package com.svrp.dao;

import com.svrp.model.Reservation;
import com.svrp.util.DatabaseConnection;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

public class ReservationDAO {

    public boolean conflict(int vehicleId, LocalDate s, LocalDate e) throws SQLException {
        String q = "SELECT COUNT(*) FROM reservations WHERE vehicle_id=? AND status='CONFIRMED' AND start_date<=? AND end_date>=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(q)) {
            p.setInt(1, vehicleId); p.setDate(2, Date.valueOf(e)); p.setDate(3, Date.valueOf(s));
            try (ResultSet r = p.executeQuery()) { r.next(); return r.getInt(1) > 0; }
        }
    }

    public int insert(Reservation x) throws SQLException {
        String q = "INSERT INTO reservations(customer_id,vehicle_id,start_date,end_date,total_cost,status) VALUES(?,?,?,?,?,'CONFIRMED')";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(q, Statement.RETURN_GENERATED_KEYS)) {
            p.setInt(1, x.getCustomerId()); p.setInt(2, x.getVehicleId());
            p.setDate(3, Date.valueOf(x.getStartDate())); p.setDate(4, Date.valueOf(x.getEndDate()));
            p.setDouble(5, x.getTotalCost());
            p.executeUpdate();
            try (ResultSet k = p.getGeneratedKeys()) { if (k.next()) return k.getInt(1); }
        }
        throw new SQLException("No generated reservation ID");
    }

    public Reservation findById(int id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT * FROM reservations WHERE id=?")) {
            p.setInt(1, id);
            try (ResultSet r = p.executeQuery()) { return r.next() ? map(r) : null; }
        }
    }

    public List<Reservation> byCustomer(int id) throws SQLException {
        return list("SELECT * FROM reservations WHERE customer_id=? ORDER BY start_date DESC", id);
    }

    public List<Reservation> all() throws SQLException {
        return list("SELECT * FROM reservations ORDER BY id DESC", null);
    }

    /** Reservations that are CONFIRMED, past end_date, and not returned */
    public List<Reservation> overdueNotFlagged() throws SQLException {
        String q = "SELECT * FROM reservations WHERE status='CONFIRMED' AND end_date < CURDATE() AND actual_return_date IS NULL AND overdue_flagged=0 ORDER BY end_date ASC";
        return list(q, null);
    }

    /** Reservations CONFIRMED and end_date+7 days past and not returned */
    public List<Reservation> severelyOverdue() throws SQLException {
        String q = "SELECT * FROM reservations WHERE status='CONFIRMED' AND end_date <= DATE_SUB(CURDATE(),INTERVAL 7 DAY) AND actual_return_date IS NULL ORDER BY end_date ASC";
        return list(q, null);
    }

    public void cancel(int id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("UPDATE reservations SET status='CANCELLED' WHERE id=? AND status='CONFIRMED'")) {
            p.setInt(1, id);
            if (p.executeUpdate() == 0) throw new SQLException("Reservation not found or already cancelled");
        }
    }

    public void markReturn(int id, LocalDate returnDate, double fine, double ratePerDay) throws SQLException {
        String q = "UPDATE reservations SET actual_return_date=?, fine_amount=?, fine_rate_per_day=?, status='COMPLETED' WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(q)) {
            p.setDate(1, Date.valueOf(returnDate)); p.setDouble(2, fine);
            p.setDouble(3, ratePerDay); p.setInt(4, id);
            p.executeUpdate();
        }
    }

    public void flagOverdue(int id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("UPDATE reservations SET overdue_flagged=1 WHERE id=?")) {
            p.setInt(1, id); p.executeUpdate();
        }
    }

    private List<Reservation> list(String q, Integer id) throws SQLException {
        List<Reservation> a = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(q)) {
            if (id != null) p.setInt(1, id);
            try (ResultSet r = p.executeQuery()) { while (r.next()) a.add(map(r)); }
        }
        return a;
    }

    private Reservation map(ResultSet r) throws SQLException {
        LocalDate returnDate = null;
        double fine = 0, fineRate = 0;
        boolean flagged = false;
        try { Date d = r.getDate("actual_return_date"); if (!r.wasNull() && d != null) returnDate = d.toLocalDate(); } catch (Exception ignored) {}
        try { fine = r.getDouble("fine_amount"); } catch (Exception ignored) {}
        try { flagged = r.getBoolean("overdue_flagged"); } catch (Exception ignored) {}
        try { fineRate = r.getDouble("fine_rate_per_day"); } catch (Exception ignored) {}
        return new Reservation(r.getInt("id"), r.getInt("customer_id"), r.getInt("vehicle_id"),
                r.getDate("start_date").toLocalDate(), r.getDate("end_date").toLocalDate(),
                r.getDouble("total_cost"), r.getString("status"),
                returnDate, fine, flagged, fineRate);
    }
}
