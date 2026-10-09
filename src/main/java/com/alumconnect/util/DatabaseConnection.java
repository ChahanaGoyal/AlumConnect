package com.alumconnect.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Utility class for managing MySQL Database connections.
 */
public class DatabaseConnection {

    private static final Properties properties = new Properties();

    static {
        loadProperties();
    }

    private static void loadProperties() {
        try (InputStream input = DatabaseConnection.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load application.properties: " + e.getMessage());
        }
    }

    public static String getProperty(String key, String defaultValue) {
        String systemVal = System.getProperty(key);
        if (systemVal != null && !systemVal.isEmpty()) {
            return systemVal;
        }
        String envKey = key.toUpperCase().replace('.', '_');
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.isEmpty()) {
            return envVal;
        }
        return properties.getProperty(key, defaultValue);
    }

    public static Connection getConnection() throws SQLException {
        String url = getProperty("db.url", "jdbc:mysql://localhost:3306/alumconnect_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        String user = getProperty("db.user", "root");
        String password = getProperty("db.password", "");

        return DriverManager.getConnection(url, user, password);
    }

    public static Connection getRootServerConnection() throws SQLException {
        String url = getProperty("db.server.url", "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        String user = getProperty("db.user", "root");
        String password = getProperty("db.password", "");

        return DriverManager.getConnection(url, user, password);
    }

    private DatabaseConnection() {
        // Private constructor to prevent instantiation
    }
}
