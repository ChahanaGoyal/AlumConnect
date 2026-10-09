package com.alumconnect;

import com.alumconnect.util.ConnectionPool;
import com.alumconnect.util.DatabaseConnection;
import com.alumconnect.util.DatabaseHealthCheck;
import com.alumconnect.util.DatabaseInitializer;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Main entry point for the AlumConnect Application.
 * AlumConnect is an Alumni Networking &amp; Mentorship Platform.
 */
public class AlumConnectApp {

    public static final String APP_NAME = "AlumConnect";
    public static final String VERSION = "1.0.0";

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   Welcome to " + APP_NAME + " (v" + VERSION + ")");
        System.out.println("   Alumni Networking & Mentorship Platform");
        System.out.println("==================================================");
        System.out.println("Project foundation initialized successfully.");

        // ── Database Initialization ──────────────────────────────────────────
        System.out.println("\n[AlumConnect] Setting up database...");
        DatabaseInitializer.initializeDatabase();

        // ── JDBC Connection Health Check ─────────────────────────────────────
        System.out.println("[AlumConnect] Verifying JDBC connection...");
        try (Connection conn = ConnectionPool.getInstance().acquire()) {
            boolean ok = DatabaseHealthCheck.verifyConnection(conn);
            if (ok) {
                System.out.println("[AlumConnect] JDBC connection verified successfully.");
            } else {
                System.err.println("[AlumConnect] Database schema verification failed. Check the schema.");
            }
        } catch (SQLException e) {
            System.err.println("[AlumConnect] Could not obtain a database connection: " + e.getMessage());
            System.err.println("[AlumConnect] Application will continue without database connectivity.");
        } finally {
            // Graceful shutdown of pool resources
            ConnectionPool.getInstance().shutdown();
        }

        System.out.println("[AlumConnect] Startup complete.");
    }
}
