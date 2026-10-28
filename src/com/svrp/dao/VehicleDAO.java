package com.svrp.dao;

import com.svrp.model.Vehicle;
import com.svrp.util.DatabaseConnection;
import java.sql.*;
import java.util.*;

public class VehicleDAO {

    public List<Vehicle> findAll() throws SQLException {
        List<Vehicle> out = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT * FROM vehicles ORDER BY id");
             ResultSet r = p.executeQuery()) {
            while (r.next()) out.add(map(r));
        }
        return out;
    }

    public Vehicle findById(int id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT * FROM vehicles WHERE id=?")) {
            p.setInt(1, id);
            try (ResultSet r = p.executeQuery()) { return r.next() ? map(r) : null; }
        }
    }

    public int insert(String type, String make, String model, String plate, double rate,
                      Integer range, Integer cc, String colour, String fuelType,
                      int seats, int kms) throws SQLException {
        String q = "INSERT INTO vehicles(vehicle_type,make,model,license_plate,rental_rate," +
                   "available,battery_range_km,engine_cc,colour,fuel_type,num_seats,kms_travelled)" +
                   " VALUES(?,?,?,?,?,TRUE,?,?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(q, Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, type); p.setString(2, make); p.setString(3, model);
            p.setString(4, plate); p.setDouble(5, rate);
            if (range == null) p.setNull(6, Types.INTEGER); else p.setInt(6, range);
            if (cc == null) p.setNull(7, Types.INTEGER); else p.setInt(7, cc);
            p.setString(8, colour != null ? colour : "Unknown");
            p.setString(9, fuelType != null ? fuelType : "PETROL");
            p.setInt(10, seats); p.setInt(11, kms);
            p.executeUpdate();
            try (ResultSet k = p.getGeneratedKeys()) {
                if (k.next()) return k.getInt(1);
            }
        }
        throw new SQLException("No generated vehicle ID");
    }

    // Legacy insert (backward compat with Main.java)
    public int insert(String type, String make, String model, String plate, double rate,
                      Integer range, Integer cc) throws SQLException {
        return insert(type, make, model, plate, rate, range, cc, "Unknown", "PETROL", 2, 0);
    }

    public void updateAvailability(int id, boolean value) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("UPDATE vehicles SET available=? WHERE id=?")) {
            p.setBoolean(1, value); p.setInt(2, id);
            if (p.executeUpdate() == 0) throw new SQLException("Vehicle not found");
        }
    }

    private Vehicle map(ResultSet r) throws SQLException {
        return new Vehicle(
            r.getInt("id"),
            r.getString("vehicle_type"),
            r.getString("make"),
            r.getString("model"),
            r.getString("license_plate"),
            r.getDouble("rental_rate"),
            r.getBoolean("available"),
            getIntOrNull(r, "battery_range_km"),
            getIntOrNull(r, "engine_cc"),
            safeStr(r, "colour", "Unknown"),
            safeStr(r, "fuel_type", "PETROL"),
            safeInt(r, "num_seats", 2),
            safeInt(r, "kms_travelled", 0),
            safeStr(r, "image_url", null)
        );
    }

    private Integer getIntOrNull(ResultSet r, String col) throws SQLException {
        int v = r.getInt(col); return r.wasNull() ? null : v;
    }
    private String safeStr(ResultSet r, String col, String def) {
        try { String v = r.getString(col); return v == null ? def : v; } catch (Exception e) { return def; }
    }
    private int safeInt(ResultSet r, String col, int def) {
        try { int v = r.getInt(col); return r.wasNull() ? def : v; } catch (Exception e) { return def; }
    }
}
