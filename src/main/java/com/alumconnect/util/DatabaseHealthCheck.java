package com.alumconnect.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides health-check and diagnostic operations for the AlumConnect database.
 *
 * <p>All SQL executed here uses <strong>parameterized {@link PreparedStatement}s</strong>
 * to prevent SQL injection. This class is the canonical example for how the
 * repository layer should perform safe database operations.
 *
 * <p>Used at application startup to:
 * <ol>
 *   <li>Verify the JDBC connection is live.</li>
 *   <li>Confirm the expected tables are present in {@code alumconnect_db}.</li>
 *   <li>Return version metadata for diagnostics.</li>
 * </ol>
 *
 * <p>All methods accept an already-acquired {@link Connection} so they compose
 * cleanly with {@link ConnectionPool#acquire()} and try-with-resources:
 * <pre>{@code
 * try (Connection conn = ConnectionPool.getInstance().acquire()) {
 *     DatabaseHealthCheck.verifyConnection(conn);
 * }
 * }</pre>
 */
public final class DatabaseHealthCheck {

    private static final Logger LOGGER = Logger.getLogger(DatabaseHealthCheck.class.getName());

    /**
     * Expected core tables in the AlumConnect schema.
     * This list is intentionally minimal and matches {@code schema.sql}.
     */
    private static final String[] REQUIRED_TABLES = {
        "users",
        "student_profiles",
        "alumni_profiles",
        "mentorship_requests"
    };

    private DatabaseHealthCheck() {
        // Utility class — no instances
    }

    // ── Public operations ────────────────────────────────────────────────────

    /**
     * Executes a lightweight {@code SELECT 1} ping to confirm the connection
     * is alive and the server responds.
     *
     * @param conn an active, non-null JDBC connection
     * @return {@code true} if the ping succeeds
     */
    public static boolean ping(Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1");
             ResultSet rs = ps.executeQuery()) {
            boolean alive = rs.next() && rs.getInt(1) == 1;
            LOGGER.log(Level.FINE, "DB ping: {0}", alive ? "OK" : "FAIL");
            return alive;
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "DB ping failed: {0}", e.getMessage());
            return false;
        }
    }

    /**
     * Returns the MySQL server version string, e.g. {@code "8.0.33"}.
     *
     * @param conn an active, non-null JDBC connection
     * @return the version string, or {@code "<unknown>"} on error
     */
    public static String getServerVersion(Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT VERSION()");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not retrieve server version: {0}", e.getMessage());
        }
        return "<unknown>";
    }

    /**
     * Checks whether a specific table exists in the given schema/catalog using
     * a parameterized query against {@code INFORMATION_SCHEMA.TABLES}.
     *
     * <p>This is the safe, injection-proof way to check table existence
     * (instead of string-concatenating table names into raw SQL).
     *
     * @param conn      an active, non-null JDBC connection
     * @param tableName the table name to look up (case-insensitive on MySQL)
     * @param schema    the schema/database name to search in
     * @return {@code true} if the table exists
     */
    public static boolean tableExists(Connection conn, String tableName, String schema) {
        final String sql =
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES "
                + "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING,
                    "Error checking existence of table ''{0}'': {1}",
                    new Object[]{tableName, e.getMessage()});
            return false;
        }
    }

    /**
     * Verifies that all required AlumConnect tables are present in the
     * {@code alumconnect_db} schema. Logs a warning for each missing table.
     *
     * @param conn an active, non-null JDBC connection to {@code alumconnect_db}
     * @return {@code true} if every required table exists
     */
    public static boolean verifySchema(Connection conn) {
        final String schema = DatabaseConnection.getProperty("db.url", "")
                .contains("alumconnect_db") ? "alumconnect_db" : conn.getClass().getName();

        // Re-read the schema name robustly from the catalog
        String catalog;
        try {
            catalog = conn.getCatalog();
            if (catalog == null || catalog.isBlank()) {
                catalog = "alumconnect_db"; // fallback
            }
        } catch (SQLException e) {
            catalog = "alumconnect_db";
        }

        boolean allPresent = true;
        for (String table : REQUIRED_TABLES) {
            boolean exists = tableExists(conn, table, catalog);
            if (!exists) {
                LOGGER.log(Level.WARNING,
                        "Schema verification: required table ''{0}'' is MISSING from ''{1}''",
                        new Object[]{table, catalog});
                allPresent = false;
            } else {
                LOGGER.log(Level.FINE,
                        "Schema verification: table ''{0}'' OK", table);
            }
        }
        return allPresent;
    }

    /**
     * Performs a full connection verification:
     * <ol>
     *   <li>Ping ({@link #ping})</li>
     *   <li>Server version log</li>
     *   <li>Schema verification ({@link #verifySchema})</li>
     * </ol>
     *
     * @param conn an active, non-null JDBC connection to {@code alumconnect_db}
     * @return {@code true} if ping and schema checks both pass
     */
    public static boolean verifyConnection(Connection conn) {
        boolean ok = ping(conn);
        if (ok) {
            String version = getServerVersion(conn);
            LOGGER.log(Level.INFO, "Connected to MySQL server version: {0}", version);
            System.out.println("[AlumConnect] MySQL server version: " + version);

            boolean schemaOk = verifySchema(conn);
            if (schemaOk) {
                System.out.println("[AlumConnect] Database schema verified — all required tables present.");
            } else {
                System.out.println("[AlumConnect] WARNING: Some required tables are missing. Run DatabaseInitializer first.");
            }
            return schemaOk;
        }
        System.err.println("[AlumConnect] ERROR: Database connection ping failed.");
        return false;
    }
}
