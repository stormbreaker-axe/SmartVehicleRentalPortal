package com.svrp.dao;

import com.svrp.util.DatabaseConnection;
import java.sql.*;

public class OtpDAO {

    /** Save OTP (valid for 10 min) */
    public void saveOtp(String phone, String otp) throws SQLException {
        // Invalidate previous OTPs for this phone
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM otp_store WHERE phone=?")) {
            p.setString(1, phone); p.executeUpdate();
        }
        String q = "INSERT INTO otp_store(phone, otp, expires_at) VALUES(?, ?, DATE_ADD(NOW(), INTERVAL 10 MINUTE))";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(q)) {
            p.setString(1, phone); p.setString(2, otp);
            p.executeUpdate();
        }
        // Simulate: print OTP to console for demo
        System.out.println("[OTP SIM] Phone: " + phone + " OTP: " + otp);
    }

    /** Returns true if OTP matches and is not expired */
    public boolean verifyOtp(String phone, String otp) throws SQLException {
        String q = "SELECT COUNT(*) FROM otp_store WHERE phone=? AND otp=? AND expires_at > NOW() AND verified=0";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(q)) {
            p.setString(1, phone); p.setString(2, otp);
            try (ResultSet r = p.executeQuery()) {
                r.next();
                if (r.getInt(1) > 0) {
                    markVerified(phone, otp);
                    return true;
                }
                return false;
            }
        }
    }

    private void markVerified(String phone, String otp) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("UPDATE otp_store SET verified=1 WHERE phone=? AND otp=?")) {
            p.setString(1, phone); p.setString(2, otp); p.executeUpdate();
        }
    }
}
