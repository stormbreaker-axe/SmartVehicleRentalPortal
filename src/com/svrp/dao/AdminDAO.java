package com.svrp.dao;

import com.svrp.util.DatabaseConnection;
import java.sql.*;

public class AdminDAO {

    public boolean authenticate(String username, String password) throws SQLException {
        String q = "SELECT COUNT(*) FROM admins WHERE username=? AND password=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(q)) {
            p.setString(1, username); p.setString(2, password);
            try (ResultSet r = p.executeQuery()) { r.next(); return r.getInt(1) > 0; }
        }
    }
}
