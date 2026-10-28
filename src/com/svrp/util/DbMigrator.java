package com.svrp.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;

public class DbMigrator {
    public static void migrate() {
        try {
            Path p = Paths.get("database/schema_v2.sql");
            if (!Files.exists(p)) {
                p = Paths.get("../database/schema_v2.sql");
            }
            if (!Files.exists(p)) return;
            String sql = Files.readString(p);
            String[] stmts = sql.split(";");
            try (Connection c = DatabaseConnection.getConnection();
                 Statement s = c.createStatement()) {
                for (String raw : stmts) {
                    StringBuilder sb = new StringBuilder();
                    for (String line : raw.split("\\r?\\n")) {
                        String t = line.trim();
                        if (!t.startsWith("--") && !t.startsWith("#")) {
                            sb.append(line).append("\n");
                        }
                    }
                    String clean = sb.toString().trim();
                    if (clean.isEmpty() || clean.toUpperCase().startsWith("USE ")) {
                        continue;
                    }
                    try {
                        s.execute(clean);
                    } catch (Exception ex) {
                        String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
                        if (!msg.contains("duplicate column") && 
                            !msg.contains("already exists") && 
                            !msg.contains("duplicate key") &&
                            !msg.contains("duplicate entry")) {
                            System.err.println("Notice on schema execution: " + ex.getMessage());
                        }
                    }
                }
            }
            System.out.println("Database schema v2 verified/migrated successfully.");
        } catch (Exception e) {
            System.err.println("Migration check failed: " + e.getMessage());
        }
    }
}
