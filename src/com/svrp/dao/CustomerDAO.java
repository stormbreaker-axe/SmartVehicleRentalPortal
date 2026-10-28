package com.svrp.dao;

import com.svrp.model.Customer;
import com.svrp.util.DatabaseConnection;
import java.sql.*;
import java.util.*;

public class CustomerDAO {

    public int insert(Customer x) throws SQLException {
        String q = "INSERT INTO customers(name,email,phone,license_number,password_hash,otp_verified) VALUES(?,?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(q, Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, x.getName()); p.setString(2, x.getEmail());
            p.setString(3, x.getPhone()); p.setString(4, x.getLicenseNumber());
            p.setString(5, x.getPasswordHash()); p.setBoolean(6, x.isOtpVerified());
            p.executeUpdate();
            try (ResultSet k = p.getGeneratedKeys()) { if (k.next()) return k.getInt(1); }
        }
        throw new SQLException("No generated customer ID");
    }

    public void markOtpVerified(int id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("UPDATE customers SET otp_verified=1 WHERE id=?")) {
            p.setInt(1, id); p.executeUpdate();
        }
    }

    public Customer findById(int id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT * FROM customers WHERE id=?")) {
            p.setInt(1, id);
            try (ResultSet r = p.executeQuery()) { return r.next() ? map(r) : null; }
        }
    }

    public Customer findByPhone(String phone) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT * FROM customers WHERE phone=?")) {
            p.setString(1, phone);
            try (ResultSet r = p.executeQuery()) { return r.next() ? map(r) : null; }
        }
    }

    public Customer findByEmail(String email) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT * FROM customers WHERE email=?")) {
            p.setString(1, email);
            try (ResultSet r = p.executeQuery()) { return r.next() ? map(r) : null; }
        }
    }

    public List<Customer> findAll() throws SQLException {
        List<Customer> a = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT * FROM customers ORDER BY id");
             ResultSet r = p.executeQuery()) {
            while (r.next()) a.add(map(r));
        }
        return a;
    }

    private Customer map(ResultSet r) throws SQLException {
        String pw = null; boolean ov = false;
        try { pw = r.getString("password_hash"); } catch (Exception ignored) {}
        try { ov = r.getBoolean("otp_verified"); } catch (Exception ignored) {}
        return new Customer(r.getInt("id"), r.getString("name"), r.getString("email"),
                r.getString("phone"), r.getString("license_number"), pw, ov);
    }
}
