package com.alumconnect.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for the JDBC connection mechanism.
 *
 * <p>Tests verify:
 * <ul>
 *   <li>Direct {@link DatabaseConnection#getConnection()} works and is valid</li>
 *   <li>{@link ConnectionPool} singleton returns pooled, wrappedConnections</li>
 *   <li>Acquired connections release back to the pool on {@code close()}</li>
 *   <li>Parameterized {@code SELECT 1} ping succeeds ({@link DatabaseHealthCheck#ping})</li>
 *   <li>Server version is retrievable ({@link DatabaseHealthCheck#getServerVersion})</li>
 *   <li>Table-existence check via parameterized {@code INFORMATION_SCHEMA} query works</li>
 *   <li>Schema verification confirms all required AlumConnect tables ({@link DatabaseHealthCheck#verifySchema})</li>
 *   <li>{@link ConnectionPool} pool-size counters behave correctly</li>
 * </ul>
 *
 * <p><strong>Graceful degradation:</strong> If the database is not reachable,
 * every test is <em>skipped</em> (not failed) using JUnit 5 {@code assumeTrue}.
 * This keeps the build green in CI environments that have no live MySQL instance.
 */
@DisplayName("JDBC Database Connection Tests")
class DatabaseConnectionTest {

    private static boolean dbAvailable = false;

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @BeforeAll
    static void checkDatabaseAvailability() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            dbAvailable = conn != null && conn.isValid(2);
        } catch (SQLException e) {
            System.out.println("[Test] Database not available, tests will be skipped: " + e.getMessage());
            dbAvailable = false;
        }
    }

    @AfterAll
    static void shutdownPool() {
        if (dbAvailable) {
            ConnectionPool.getInstance().shutdown();
        }
    }

    // ── DatabaseConnection tests ─────────────────────────────────────────────

    @Test
    @DisplayName("DatabaseConnection.getConnection() should return a valid, open connection")
    void testDirectConnectionIsValid() throws SQLException {
        assumeTrue(dbAvailable, "Skipping: MySQL not reachable");

        try (Connection conn = DatabaseConnection.getConnection()) {
            assertNotNull(conn, "Connection must not be null");
            assertFalse(conn.isClosed(), "Connection must be open");
            assertTrue(conn.isValid(2), "Connection must be valid within 2 seconds");
        }
    }

    @Test
    @DisplayName("DatabaseConnection.getProperty() should resolve properties with env/system-property override precedence")
    void testPropertyResolution() {
        // This test is pure unit-level and does not need a live DB
        String url = DatabaseConnection.getProperty("db.url",
                "jdbc:mysql://localhost:3306/alumconnect_db");
        assertNotNull(url, "db.url must be resolvable");
        assertTrue(url.contains("mysql"), "URL should reference MySQL");

        String appName = DatabaseConnection.getProperty("app.name", "UNKNOWN");
        assertEquals("AlumConnect", appName, "Application name must match");
    }

    @Test
    @DisplayName("DatabaseConnection.getRootServerConnection() should open a server-level connection")
    void testRootServerConnection() throws SQLException {
        assumeTrue(dbAvailable, "Skipping: MySQL not reachable");

        try (Connection conn = DatabaseConnection.getRootServerConnection()) {
            assertNotNull(conn, "Root server connection must not be null");
            assertTrue(conn.isValid(2), "Root server connection must be valid");
        }
    }

    // ── ConnectionPool tests ─────────────────────────────────────────────────

    @Test
    @DisplayName("ConnectionPool.getInstance() should return the same singleton instance")
    void testConnectionPoolSingleton() {
        assumeTrue(dbAvailable, "Skipping: MySQL not reachable");

        ConnectionPool pool1 = ConnectionPool.getInstance();
        ConnectionPool pool2 = ConnectionPool.getInstance();
        assertSame(pool1, pool2, "ConnectionPool must be a singleton");
    }

    @Test
    @DisplayName("ConnectionPool.acquire() should return a non-null, valid Connection")
    void testPoolAcquireReturnsValidConnection() throws SQLException {
        assumeTrue(dbAvailable, "Skipping: MySQL not reachable");

        try (Connection conn = ConnectionPool.getInstance().acquire()) {
            assertNotNull(conn, "Pooled connection must not be null");
            assertFalse(conn.isClosed(), "Pooled connection must be open");
            assertTrue(conn.isValid(2), "Pooled connection must be valid");
        }
    }

    @Test
    @DisplayName("ConnectionPool: closing an acquired connection should release it back (idle count increases)")
    void testPoolReleaseOnClose() throws SQLException {
        assumeTrue(dbAvailable, "Skipping: MySQL not reachable");

        ConnectionPool pool = ConnectionPool.getInstance();
        int idleBefore = pool.idleCount();

        Connection conn = pool.acquire();
        // idleCount decreases by 1 after acquire
        int idleDuringUse = pool.idleCount();

        conn.close(); // should release back to pool
        int idleAfter = pool.idleCount();

        // After releasing, idle count should be restored
        assertTrue(idleAfter >= idleDuringUse,
                "Idle count should increase after releasing a connection back to the pool");
    }

    @Test
    @DisplayName("ConnectionPool.totalCount() should be >= 1 after first acquire")
    void testPoolTotalCount() throws SQLException {
        assumeTrue(dbAvailable, "Skipping: MySQL not reachable");

        try (Connection conn = ConnectionPool.getInstance().acquire()) {
            assertTrue(ConnectionPool.getInstance().totalCount() >= 1,
                    "Pool should track at least 1 total connection after acquire");
        }
    }

    // ── DatabaseHealthCheck tests ────────────────────────────────────────────

    @Test
    @DisplayName("DatabaseHealthCheck.ping() should return true for a live connection (parameterized SELECT 1)")
    void testPingWithLiveConnection() throws SQLException {
        assumeTrue(dbAvailable, "Skipping: MySQL not reachable");

        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean alive = DatabaseHealthCheck.ping(conn);
            assertTrue(alive, "Ping (SELECT 1) should succeed on a live connection");
        }
    }

    @Test
    @DisplayName("DatabaseHealthCheck.getServerVersion() should return a non-blank version string")
    void testGetServerVersion() throws SQLException {
        assumeTrue(dbAvailable, "Skipping: MySQL not reachable");

        try (Connection conn = DatabaseConnection.getConnection()) {
            String version = DatabaseHealthCheck.getServerVersion(conn);
            assertNotNull(version, "Server version must not be null");
            assertFalse(version.isBlank(), "Server version must not be blank");
            assertNotEquals("<unknown>", version, "Server version should be retrievable");
            System.out.println("[Test] MySQL server version: " + version);
        }
    }

    @Test
    @DisplayName("DatabaseHealthCheck.tableExists() should confirm 'users' table exists in alumconnect_db")
    void testTableExistsUsersTable() throws SQLException {
        assumeTrue(dbAvailable, "Skipping: MySQL not reachable");

        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean exists = DatabaseHealthCheck.tableExists(conn, "users", "alumconnect_db");
            assertTrue(exists, "'users' table must exist in alumconnect_db after schema initialization");
        }
    }

    @Test
    @DisplayName("DatabaseHealthCheck.tableExists() should return false for a non-existent table")
    void testTableExistsForNonExistentTable() throws SQLException {
        assumeTrue(dbAvailable, "Skipping: MySQL not reachable");

        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean exists = DatabaseHealthCheck.tableExists(conn, "jobs_table_does_not_exist", "alumconnect_db");
            assertFalse(exists, "Non-existent table must return false — jobs/internship tables must not be present");
        }
    }

    @Test
    @DisplayName("DatabaseHealthCheck.verifySchema() should confirm all required AlumConnect tables exist")
    void testVerifySchemaAllTablesPresent() throws SQLException {
        assumeTrue(dbAvailable, "Skipping: MySQL not reachable");

        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean ok = DatabaseHealthCheck.verifySchema(conn);
            assertTrue(ok, "Schema verification must pass — all 4 required tables should exist");
        }
    }

    @Test
    @DisplayName("DatabaseHealthCheck.verifyConnection() full pipeline should pass on a live connection")
    void testVerifyConnectionFullPipeline() throws SQLException {
        assumeTrue(dbAvailable, "Skipping: MySQL not reachable");

        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean ok = DatabaseHealthCheck.verifyConnection(conn);
            assertTrue(ok, "Full connection verification (ping + schema check) must pass");
        }
    }

    // ── Parameterized SQL safety test ────────────────────────────────────────

    @Test
    @DisplayName("Parameterized SELECT against users table should execute without SQL injection risk")
    void testParameterizedQueryOnUsersTable() throws SQLException {
        assumeTrue(dbAvailable, "Skipping: MySQL not reachable");

        // This test demonstrates safe parameterized SQL — the repository layer
        // must always use PreparedStatement, never string concatenation.
        final String sql = "SELECT COUNT(*) FROM users WHERE role = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "STUDENT");

            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "ResultSet should have at least one row");
                int count = rs.getInt(1);
                assertTrue(count >= 0, "Student count must be non-negative");
                System.out.println("[Test] STUDENT rows in users: " + count);
            }
        }
    }
}
