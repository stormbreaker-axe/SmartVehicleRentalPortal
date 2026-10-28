package com.svrp.util;

import java.io.*;
import java.sql.*;
import java.util.*;

public final class DatabaseConnection {
    private static final Properties P = new Properties();

    static {
        InputStream in = DatabaseConnection.class.getClassLoader().getResourceAsStream("db.properties");
        if (in == null) in = DatabaseConnection.class.getResourceAsStream("/db.properties");
        if (in == null) in = DatabaseConnection.class.getResourceAsStream("/resources/db.properties");
        try {
            if (in != null) {
                try (InputStream stream = in) {
                    P.load(stream);
                }
            } else {
                File f = new File("src/resources/db.properties");
                if (!f.exists()) f = new File("src/db.properties");
                if (!f.exists()) f = new File("bin/db.properties");
                if (f.exists()) {
                    try (FileInputStream fis = new FileInputStream(f)) {
                        P.load(fis);
                    }
                } else {
                    throw new IllegalStateException("db.properties missing");
                }
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private DatabaseConnection() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(P.getProperty("db.url"), P.getProperty("db.username"), P.getProperty("db.password"));
    }

    public static void test() throws SQLException {
        try (Connection c = getConnection()) {
            System.out.println("Database connection successful.");
        }
    }
}
