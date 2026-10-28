package com.svrp.web;

import com.svrp.dao.*;
import com.svrp.model.*;
import com.svrp.util.DatabaseConnection;
import com.sun.net.httpserver.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Lightweight HTTP API server — no servlet container needed.
 * Uses com.sun.net.httpserver (built into the JDK).
 */
public class ApiServer {

    private static final VehicleDAO     vd  = new VehicleDAO();
    private static final CustomerDAO    cd  = new CustomerDAO();
    private static final ReservationDAO rd  = new ReservationDAO();
    private static final PaymentDAO     pd  = new PaymentDAO();
    private static final OtpDAO         od  = new OtpDAO();
    private static final AdminDAO       ad  = new AdminDAO();

    // Simple in-memory session tokens: token -> customerId (or "ADMIN")
    private static final Map<String, String> sessions = new HashMap<>();

    public static void main(String[] args) throws Exception {
        DatabaseConnection.test();
        com.svrp.util.DbMigrator.migrate();
        int port = 8080;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); } catch (Exception ignored) {}
        }
        HttpServer server = null;
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (java.net.BindException ex) {
            System.out.println("Notice: Port " + port + " is already occupied (e.g. by Tomcat). Switching to port 8081...");
            port = 8081;
            server = HttpServer.create(new InetSocketAddress(port), 0);
        }

        server.createContext("/api/vehicles",     ApiServer::handleVehicles);
        server.createContext("/api/customer",     ApiServer::handleCustomer);
        server.createContext("/api/reservations", ApiServer::handleReservations);
        server.createContext("/api/admin",        ApiServer::handleAdmin);
        server.createContext("/api/otp",          ApiServer::handleOtp);
        server.createContext("/api/payment",      ApiServer::handlePayment);
        server.createContext("/",                 ApiServer::handleStatic);

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("=== SVRP Web Server running at http://localhost:" + port + " ===");
    }

    // ─────────────────────────── VEHICLES ──────────────────────────────────
    private static void handleVehicles(HttpExchange ex) throws IOException {
        try {
            enableCors(ex);
            if ("OPTIONS".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(204, -1); return; }
            List<Vehicle> vehicles = vd.findAll();
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < vehicles.size(); i++) {
                Vehicle v = vehicles.get(i);
                if (i > 0) sb.append(",");
                sb.append(vehicleJson(v));
            }
            sb.append("]");
            respond(ex, 200, sb.toString());
        } catch (Exception e) { respondError(ex, 500, e.getMessage()); }
    }

    // ─────────────────────────── OTP ───────────────────────────────────────
    private static void handleOtp(HttpExchange ex) throws IOException {
        try {
            enableCors(ex);
            if ("OPTIONS".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(204, -1); return; }
            Map<String,String> body = parseBody(ex);
            String action = body.getOrDefault("action", "");
            if ("send".equals(action)) {
                String phone = body.get("phone");
                if (phone == null || !phone.matches("^[6-9]\\d{9}$"))
                    throw new IllegalArgumentException("Invalid phone number");
                String otp = String.format("%06d", new Random().nextInt(999999));
                od.saveOtp(phone, otp);
                respond(ex, 200, "{\"ok\":true,\"message\":\"OTP sent to " + phone + "\",\"otp\":\"" + otp + "\"}");
            } else if ("verify".equals(action)) {
                String phone = body.get("phone");
                String otp   = body.get("otp");
                boolean ok = od.verifyOtp(phone, otp);
                if (!ok) throw new IllegalArgumentException("Invalid or expired OTP");
                respond(ex, 200, "{\"ok\":true}");
            } else {
                throw new IllegalArgumentException("Unknown OTP action");
            }
        } catch (Exception e) { respondError(ex, 400, e.getMessage()); }
    }

    // ─────────────────────────── CUSTOMER ──────────────────────────────────
    private static void handleCustomer(HttpExchange ex) throws IOException {
        try {
            enableCors(ex);
            if ("OPTIONS".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(204, -1); return; }
            String path = ex.getRequestURI().getPath();
            Map<String,String> body = parseBody(ex);
            String action = body.getOrDefault("action", "");

            if ("register".equals(action)) {
                String name    = require(body, "name");
                String email   = require(body, "email");
                String phone   = require(body, "phone");
                String license = require(body, "license");
                String pass    = require(body, "password");

                // Validations
                if (!email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$"))
                    throw new IllegalArgumentException("Invalid email format");
                if (!phone.matches("^[6-9]\\d{9}$"))
                    throw new IllegalArgumentException("Invalid phone number (10 digits starting with 6-9)");
                if (!license.matches("^[A-Z]{2}\\d{2}[A-Z]{1,2}\\d{4,7}$"))
                    throw new IllegalArgumentException("Invalid driving license format (e.g. KL01AB1234)");

                Customer existing = cd.findByEmail(email);
                if (existing != null) throw new IllegalArgumentException("Email already registered");

                // Simple hash (XOR + hex) — use BCrypt in production
                String hash = simpleHash(pass);
                int id = cd.insert(new Customer(0, name, email, phone, license, hash, true));
                respond(ex, 200, "{\"ok\":true,\"customerId\":" + id + "}");

            } else if ("login".equals(action)) {
                String phone = require(body, "phone");
                String pass  = require(body, "password");
                Customer cust = cd.findByPhone(phone);
                if (cust == null || !simpleHash(pass).equals(cust.getPasswordHash()))
                    throw new IllegalArgumentException("Invalid phone or password");
                if (!cust.isOtpVerified())
                    throw new IllegalArgumentException("Please verify your phone number first");
                String token = UUID.randomUUID().toString();
                sessions.put(token, "CUSTOMER:" + cust.getId());
                String json = "{\"ok\":true,\"token\":\"" + token + "\",\"customerId\":" + cust.getId() +
                              ",\"name\":\"" + esc(cust.getName()) + "\"}";
                respond(ex, 200, json);

            } else if ("me".equals(action)) {
                int custId = requireCustomerSession(ex, body);
                Customer cust = cd.findById(custId);
                if (cust == null) throw new IllegalArgumentException("Customer not found");
                respond(ex, 200, customerJson(cust));

            } else if ("all".equals(action)) {
                requireAdminSession(ex, body);
                List<Customer> all = cd.findAll();
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < all.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append(customerJson(all.get(i)));
                }
                sb.append("]");
                respond(ex, 200, sb.toString());

            } else if ("markVerified".equals(action)) {
                int id = Integer.parseInt(require(body, "customerId"));
                cd.markOtpVerified(id);
                respond(ex, 200, "{\"ok\":true}");

            } else {
                throw new IllegalArgumentException("Unknown action: " + action);
            }
        } catch (Exception e) { respondError(ex, 400, e.getMessage()); }
    }

    // ─────────────────────────── RESERVATIONS ──────────────────────────────
    private static void handleReservations(HttpExchange ex) throws IOException {
        try {
            enableCors(ex);
            if ("OPTIONS".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(204, -1); return; }
            Map<String,String> body = parseBody(ex);
            String action = body.getOrDefault("action", "");

            if ("reserve".equals(action)) {
                int custId  = requireCustomerSession(ex, body);
                int vehId   = Integer.parseInt(require(body, "vehicleId"));
                LocalDate s = LocalDate.parse(require(body, "startDate"));
                LocalDate e = LocalDate.parse(require(body, "endDate"));
                if (e.isBefore(s)) throw new IllegalArgumentException("End date before start");
                if (s.isBefore(LocalDate.now())) throw new IllegalArgumentException("Start date in the past");
                Vehicle v = vd.findById(vehId);
                if (v == null) throw new IllegalArgumentException("Vehicle not found");
                if (!v.isAvailable()) throw new IllegalStateException("Vehicle not available");
                if (rd.conflict(vehId, s, e)) throw new IllegalStateException("Vehicle already booked");
                int days = (int) ChronoUnit.DAYS.between(s, e) + 1;
                double cost = v.calculateRentalCost(days);
                int rid = rd.insert(new Reservation(0, custId, vehId, s, e, cost, "CONFIRMED"));
                respond(ex, 200, "{\"ok\":true,\"reservationId\":" + rid + ",\"totalCost\":" + cost + "}");

            } else if ("check".equals(action)) {
                int vehId   = Integer.parseInt(require(body, "vehicleId"));
                LocalDate s = LocalDate.parse(require(body, "startDate"));
                LocalDate e = LocalDate.parse(require(body, "endDate"));
                Vehicle v = vd.findById(vehId);
                if (v == null) throw new IllegalArgumentException("Vehicle not found");
                boolean avail = v.isAvailable() && !rd.conflict(vehId, s, e);
                int days = (int) ChronoUnit.DAYS.between(s, e) + 1;
                double cost = v.calculateRentalCost(days);
                respond(ex, 200, "{\"available\":" + avail + ",\"cost\":" + cost + "}");

            } else if ("mine".equals(action)) {
                int custId = requireCustomerSession(ex, body);
                respond(ex, 200, reservationsJson(rd.byCustomer(custId)));

            } else if ("all".equals(action)) {
                requireAdminSession(ex, body);
                respond(ex, 200, reservationsJson(rd.all()));

            } else if ("cancel".equals(action)) {
                int rid = Integer.parseInt(require(body, "reservationId"));
                // customer can cancel own, admin can cancel any
                String sess = sessions.get(body.get("token"));
                if (sess != null && sess.startsWith("CUSTOMER:")) {
                    int custId = Integer.parseInt(sess.split(":")[1]);
                    Reservation r = rd.findById(rid);
                    if (r == null || r.getCustomerId() != custId)
                        throw new IllegalArgumentException("Not your reservation");
                }
                rd.cancel(rid);
                respond(ex, 200, "{\"ok\":true}");

            } else if ("markReturn".equals(action)) {
                requireAdminSession(ex, body);
                int rid = Integer.parseInt(require(body, "reservationId"));
                LocalDate returnDate = LocalDate.parse(require(body, "returnDate"));
                double ratePerDay = Double.parseDouble(body.getOrDefault("fineRatePerDay", "0"));
                Reservation r = rd.findById(rid);
                if (r == null) throw new IllegalArgumentException("Reservation not found");
                long lateDays = ChronoUnit.DAYS.between(r.getEndDate(), returnDate);
                double fine = lateDays > 0 ? lateDays * ratePerDay : 0;
                rd.markReturn(rid, returnDate, fine, ratePerDay);
                vd.updateAvailability(r.getVehicleId(), true);
                respond(ex, 200, "{\"ok\":true,\"fine\":" + fine + ",\"lateDays\":" + lateDays + "}");

            } else if ("flagOverdue".equals(action)) {
                requireAdminSession(ex, body);
                int rid = Integer.parseInt(require(body, "reservationId"));
                String notes = body.getOrDefault("notes", "");
                rd.flagOverdue(rid);
                // Write to overdue_reports
                Reservation r = rd.findById(rid);
                if (r != null) insertOverdueReport(r, notes);
                respond(ex, 200, "{\"ok\":true}");

            } else if ("overdue".equals(action)) {
                requireAdminSession(ex, body);
                respond(ex, 200, reservationsJson(rd.overdueNotFlagged()));

            } else if ("severelyOverdue".equals(action)) {
                requireAdminSession(ex, body);
                respond(ex, 200, reservationsJson(rd.severelyOverdue()));

            } else if ("legalReport".equals(action)) {
                requireAdminSession(ex, body);
                int rid = Integer.parseInt(require(body, "reservationId"));
                respond(ex, 200, buildLegalReport(rid));

            } else {
                throw new IllegalArgumentException("Unknown action: " + action);
            }
        } catch (Exception e) { respondError(ex, 400, e.getMessage()); }
    }

    // ─────────────────────────── ADMIN ─────────────────────────────────────
    private static void handleAdmin(HttpExchange ex) throws IOException {
        try {
            enableCors(ex);
            if ("OPTIONS".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(204, -1); return; }
            Map<String,String> body = parseBody(ex);
            String action = body.getOrDefault("action", "");

            if ("login".equals(action)) {
                String user = require(body, "username");
                String pass = require(body, "password");
                if (!ad.authenticate(user, pass))
                    throw new IllegalArgumentException("Invalid admin credentials");
                String token = UUID.randomUUID().toString();
                sessions.put(token, "ADMIN");
                respond(ex, 200, "{\"ok\":true,\"token\":\"" + token + "\"}");

            } else if ("addVehicle".equals(action)) {
                requireAdminSession(ex, body);
                String type  = require(body, "vehicleType").toUpperCase();
                String make  = require(body, "make");
                String model = require(body, "model");
                String plate = require(body, "plate");
                double rate  = Double.parseDouble(require(body, "rate"));
                Integer range = body.containsKey("batteryRange") ? Integer.parseInt(body.get("batteryRange")) : null;
                Integer cc    = body.containsKey("engineCc")     ? Integer.parseInt(body.get("engineCc"))     : null;
                String colour   = body.getOrDefault("colour", "Unknown");
                String fuelType = body.getOrDefault("fuelType", "PETROL");
                int seats = Integer.parseInt(body.getOrDefault("numSeats", "2"));
                int kms   = Integer.parseInt(body.getOrDefault("kmsTravelled", "0"));
                int id = vd.insert(type, make, model, plate, rate, range, cc, colour, fuelType, seats, kms);
                respond(ex, 200, "{\"ok\":true,\"vehicleId\":" + id + "}");

            } else if ("setAvailability".equals(action)) {
                requireAdminSession(ex, body);
                int id = Integer.parseInt(require(body, "vehicleId"));
                boolean avail = Boolean.parseBoolean(require(body, "available"));
                vd.updateAvailability(id, avail);
                respond(ex, 200, "{\"ok\":true}");

            } else {
                throw new IllegalArgumentException("Unknown admin action: " + action);
            }
        } catch (Exception e) { respondError(ex, 400, e.getMessage()); }
    }

    // ─────────────────────────── PAYMENT ───────────────────────────────────
    private static void handlePayment(HttpExchange ex) throws IOException {
        try {
            enableCors(ex);
            if ("OPTIONS".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(204, -1); return; }
            Map<String,String> body = parseBody(ex);
            requireCustomerSession(ex, body);
            int rid    = Integer.parseInt(require(body, "reservationId"));
            double amt = Double.parseDouble(require(body, "amount"));
            String method = require(body, "method").toUpperCase();
            if (!List.of("CASH","CARD","UPI","NET_BANKING").contains(method))
                throw new IllegalArgumentException("Invalid payment method");
            Reservation r = rd.findById(rid);
            if (r == null) throw new IllegalArgumentException("Reservation not found");
            if (!"CONFIRMED".equals(r.getStatus())) throw new IllegalStateException("Not payable");
            if (amt < r.getTotalCost()) throw new IllegalArgumentException("Amount below total: Rs." + r.getTotalCost());
            int pid = pd.insert(new Payment(0, rid, amt, method, "PAID"));
            respond(ex, 200, "{\"ok\":true,\"paymentId\":" + pid + "}");
        } catch (Exception e) { respondError(ex, 400, e.getMessage()); }
    }

    // ─────────────────────────── STATIC FILES ──────────────────────────────
    private static void handleStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if ("/".equals(path) || path == null || path.isEmpty()) path = "/index.html";
        byte[] data = null;
        File f = new File("src/com/svrp/web" + path);
        if (!f.exists()) f = new File("bin/com/svrp/web" + path);
        if (f.exists() && f.isFile()) {
            data = java.nio.file.Files.readAllBytes(f.toPath());
        } else {
            InputStream in = ApiServer.class.getResourceAsStream(path.startsWith("/") ? path : "/" + path);
            if (in == null) in = ApiServer.class.getResourceAsStream("/com/svrp/web" + path);
            if (in == null) in = ApiServer.class.getResourceAsStream("/web" + path);
            if (in != null) {
                try (InputStream stream = in) {
                    data = stream.readAllBytes();
                }
            }
        }
        if (data == null) {
            byte[] err = ("File not found: " + path).getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(404, err.length);
            ex.getResponseBody().write(err);
            ex.getResponseBody().close();
            return;
        }
        String ct = path.endsWith(".html") ? "text/html" : path.endsWith(".css") ? "text/css"
                  : path.endsWith(".js") ? "application/javascript"
                  : path.endsWith(".png") ? "image/png"
                  : path.endsWith(".jpg") || path.endsWith(".jpeg") ? "image/jpeg"
                  : path.endsWith(".svg") ? "image/svg+xml" : "application/octet-stream";
        ex.getResponseHeaders().set("Content-Type", ct + "; charset=UTF-8");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(200, data.length);
        ex.getResponseBody().write(data);
        ex.getResponseBody().close();
    }

    // ─────────────────────────── HELPERS ───────────────────────────────────
    private static String vehicleJson(Vehicle v) {
        return "{\"id\":" + v.getId() +
               ",\"type\":\"" + esc(v.getVehicleType()) + "\"" +
               ",\"make\":\"" + esc(v.getMake()) + "\"" +
               ",\"model\":\"" + esc(v.getModel()) + "\"" +
               ",\"plate\":\"" + esc(v.getLicensePlate()) + "\"" +
               ",\"rate\":" + v.getRentalRate() +
               ",\"available\":" + v.isAvailable() +
               ",\"engineCc\":" + (v.getEngineCc() != null ? v.getEngineCc() : "null") +
               ",\"batteryRange\":" + (v.getBatteryRangeKm() != null ? v.getBatteryRangeKm() : "null") +
               ",\"colour\":\"" + esc(v.getColour()) + "\"" +
               ",\"fuelType\":\"" + esc(v.getFuelType()) + "\"" +
               ",\"numSeats\":" + v.getNumSeats() +
               ",\"kmsTravelled\":" + v.getKmsTravelled() +
               ",\"imageUrl\":" + (v.getImageUrl() != null ? "\"" + esc(v.getImageUrl()) + "\"" : "null") +
               "}";
    }

    private static String customerJson(Customer c) {
        return "{\"id\":" + c.getId() +
               ",\"name\":\"" + esc(c.getName()) + "\"" +
               ",\"email\":\"" + esc(c.getEmail()) + "\"" +
               ",\"phone\":\"" + esc(c.getPhone()) + "\"" +
               ",\"license\":\"" + esc(c.getLicenseNumber()) + "\"" +
               ",\"otpVerified\":" + c.isOtpVerified() +
               "}";
    }

    private static String reservationJson(Reservation r) {
        return "{\"id\":" + r.getId() +
               ",\"customerId\":" + r.getCustomerId() +
               ",\"vehicleId\":" + r.getVehicleId() +
               ",\"startDate\":\"" + r.getStartDate() + "\"" +
               ",\"endDate\":\"" + r.getEndDate() + "\"" +
               ",\"totalCost\":" + r.getTotalCost() +
               ",\"status\":\"" + r.getStatus() + "\"" +
               ",\"actualReturnDate\":" + (r.getActualReturnDate() != null ? "\"" + r.getActualReturnDate() + "\"" : "null") +
               ",\"fineAmount\":" + r.getFineAmount() +
               ",\"fineRatePerDay\":" + r.getFineRatePerDay() +
               ",\"overdueFlagged\":" + r.isOverdueFlagged() +
               "}";
    }

    private static String reservationsJson(List<Reservation> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(reservationJson(list.get(i)));
        }
        return sb.append("]").toString();
    }

    private static String buildLegalReport(int rid) throws SQLException {
        Reservation r = rd.findById(rid);
        if (r == null) throw new IllegalArgumentException("Reservation not found");
        Customer c = cd.findById(r.getCustomerId());
        Vehicle v  = vd.findById(r.getVehicleId());
        long overdueDays = ChronoUnit.DAYS.between(r.getEndDate(), LocalDate.now());
        return "{\"reservationId\":" + rid +
               ",\"customerName\":\"" + esc(c != null ? c.getName() : "?") + "\"" +
               ",\"customerEmail\":\"" + esc(c != null ? c.getEmail() : "?") + "\"" +
               ",\"customerPhone\":\"" + esc(c != null ? c.getPhone() : "?") + "\"" +
               ",\"licenseNumber\":\"" + esc(c != null ? c.getLicenseNumber() : "?") + "\"" +
               ",\"vehicleType\":\"" + esc(v != null ? v.getVehicleType() : "?") + "\"" +
               ",\"vehicleMake\":\"" + esc(v != null ? v.getMake() : "?") + "\"" +
               ",\"vehicleModel\":\"" + esc(v != null ? v.getModel() : "?") + "\"" +
               ",\"vehiclePlate\":\"" + esc(v != null ? v.getLicensePlate() : "?") + "\"" +
               ",\"startDate\":\"" + r.getStartDate() + "\"" +
               ",\"endDate\":\"" + r.getEndDate() + "\"" +
               ",\"overdueDays\":" + overdueDays +
               ",\"totalCost\":" + r.getTotalCost() +
               ",\"fineAmount\":" + r.getFineAmount() +
               ",\"reportDate\":\"" + LocalDate.now() + "\"" +
               "}";
    }

    private static void insertOverdueReport(Reservation r, String notes) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(
                "INSERT IGNORE INTO overdue_reports(reservation_id,customer_id,vehicle_id,notes) VALUES(?,?,?,?)")) {
            p.setInt(1, r.getId()); p.setInt(2, r.getCustomerId());
            p.setInt(3, r.getVehicleId()); p.setString(4, notes);
            p.executeUpdate();
        } catch (Exception ignored) {}
    }

    private static Map<String,String> parseBody(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).trim();
        Map<String,String> map = new HashMap<>();
        String query = ex.getRequestURI().getQuery();
        if (query != null) {
            for (String kv : query.split("&")) {
                String[] p = kv.split("=", 2);
                if (p.length == 2) {
                    try {
                        map.put(p[0], java.net.URLDecoder.decode(p[1], StandardCharsets.UTF_8));
                    } catch (Exception ignored) {}
                }
            }
        }
        if (body.isEmpty()) return map;

        // Match "key"\s*:\s*("val"|val)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "\"([^\"]+)\"\\s*:\\s*(\"[^\"]*\"|[^,\\}\\]\\s]+)"
        );
        java.util.regex.Matcher m = pattern.matcher(body);
        while (m.find()) {
            String k = m.group(1);
            String v = m.group(2).trim();
            if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) {
                v = v.substring(1, v.length() - 1);
            }
            map.put(k, v);
        }
        return map;
    }

    private static String require(Map<String,String> m, String key) {
        String v = m.get(key);
        if (v == null || v.isBlank()) throw new IllegalArgumentException("Missing field: " + key);
        return v.trim();
    }

    private static int requireCustomerSession(HttpExchange ex, Map<String,String> body) {
        String token = body.get("token");
        String sess  = token != null ? sessions.get(token) : null;
        if (sess == null || !sess.startsWith("CUSTOMER:"))
            throw new IllegalStateException("Not logged in as customer");
        return Integer.parseInt(sess.split(":")[1]);
    }

    private static void requireAdminSession(HttpExchange ex, Map<String,String> body) {
        String token = body.get("token");
        String sess  = token != null ? sessions.get(token) : null;
        if (!"ADMIN".equals(sess)) throw new IllegalStateException("Not logged in as admin");
    }

    private static void respond(HttpExchange ex, int code, String json) throws IOException {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(code, data.length);
        ex.getResponseBody().write(data);
        ex.getResponseBody().close();
    }

    private static void respondError(HttpExchange ex, int code, String msg) throws IOException {
        respond(ex, code, "{\"ok\":false,\"error\":\"" + esc(msg) + "\"}");
    }

    private static void enableCors(HttpExchange ex) {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private static String simpleHash(String pass) {
        // Simple deterministic hash for demo — use BCrypt in production
        int h = 0x12345678;
        for (char ch : pass.toCharArray()) h = h * 31 ^ ch;
        return Integer.toHexString(h);
    }
}
