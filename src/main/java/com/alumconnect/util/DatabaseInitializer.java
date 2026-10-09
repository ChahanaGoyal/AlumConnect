package com.alumconnect.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Initializes the database schema using the schema.sql script.
 */
public class DatabaseInitializer {

    public static void initializeDatabase() {
        System.out.println("Initializing AlumConnect database schema...");

        try (InputStream is = DatabaseInitializer.class.getClassLoader().getResourceAsStream("schema.sql")) {
            if (is == null) {
                System.err.println("Error: schema.sql resource not found in classpath.");
                return;
            }

            try (Connection conn = DatabaseConnection.getRootServerConnection();
                 Statement stmt = conn.createStatement();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

                StringBuilder sqlBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("--") || trimmed.isEmpty()) {
                        continue;
                    }
                    sqlBuilder.append(line).append("\n");
                    if (trimmed.endsWith(";")) {
                        String sql = sqlBuilder.toString().trim();
                        if (!sql.isEmpty()) {
                            stmt.execute(sql);
                        }
                        sqlBuilder.setLength(0);
                    }
                }
                System.out.println("Database schema initialized successfully.");
            }
        } catch (Exception e) {
            System.err.println("Error initializing database schema: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        initializeDatabase();
    }
}
