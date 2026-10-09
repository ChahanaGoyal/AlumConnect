package com.alumconnect.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread-safe singleton connection pool for the AlumConnect application.
 *
 * <p>Manages a bounded pool of JDBC {@link Connection} objects that can be
 * acquired and released by the repository layer. All configuration is read
 * from {@code application.properties} (or environment variables / system
 * properties) via {@link DatabaseConnection}:
 * <ul>
 *   <li>{@code db.pool.maxSize}            — total pool capacity (default 10)</li>
 *   <li>{@code db.pool.minIdle}            — connections created at startup (default 2)</li>
 *   <li>{@code db.pool.connectionTimeoutMs}— max wait on {@link #acquire()} in ms (default 3000)</li>
 *   <li>{@code db.pool.validationQuery}    — SQL used to validate live connections (default "SELECT 1")</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * ConnectionPool pool = ConnectionPool.getInstance();
 * try (Connection conn = pool.acquire()) {
 *     // use conn with PreparedStatements ...
 * }  // auto-released back to pool via wrapper
 * }</pre>
 */
public final class ConnectionPool {

    private static final Logger LOGGER = Logger.getLogger(ConnectionPool.class.getName());

    // --- Singleton -----------------------------------------------------------

    private static volatile ConnectionPool instance;

    /**
     * Returns the shared {@code ConnectionPool} instance, creating it on
     * first call (double-checked locking).
     *
     * @return the singleton pool
     * @throws IllegalStateException if the pool cannot be initialised
     */
    public static ConnectionPool getInstance() {
        if (instance == null) {
            synchronized (ConnectionPool.class) {
                if (instance == null) {
                    instance = new ConnectionPool();
                }
            }
        }
        return instance;
    }

    // --- Configuration -------------------------------------------------------

    private final int maxSize;
    private final long connectionTimeoutMs;
    private final String validationQuery;

    // --- State ---------------------------------------------------------------

    /** Idle connections available for acquisition. */
    private final BlockingQueue<Connection> idlePool;

    /** All connections ever created by this pool (for orderly shutdown). */
    private final List<Connection> allConnections;

    private volatile boolean closed = false;

    // --- Constructor ---------------------------------------------------------

    private ConnectionPool() {
        this.maxSize = parseIntProperty("db.pool.maxSize", 10);
        int minIdle   = parseIntProperty("db.pool.minIdle", 2);
        this.connectionTimeoutMs = parseLongProperty("db.pool.connectionTimeoutMs", 3000L);
        this.validationQuery = DatabaseConnection.getProperty("db.pool.validationQuery", "SELECT 1");

        this.idlePool       = new ArrayBlockingQueue<>(maxSize);
        this.allConnections = new ArrayList<>(maxSize);

        // Pre-warm minIdle connections
        int warmed = 0;
        for (int i = 0; i < minIdle; i++) {
            try {
                Connection conn = DatabaseConnection.getConnection();
                idlePool.add(conn);
                allConnections.add(conn);
                warmed++;
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING,
                        "ConnectionPool: could not pre-warm connection {0}: {1}",
                        new Object[]{i + 1, e.getMessage()});
            }
        }
        LOGGER.log(Level.INFO,
                "ConnectionPool initialised: maxSize={0}, pre-warmed={1}",
                new Object[]{maxSize, warmed});
    }

    // --- Public API ----------------------------------------------------------

    /**
     * Acquires a live {@link Connection} from the pool.
     *
     * <p>If an idle connection is available it is validated before being
     * returned. If validation fails or the connection is closed it is replaced
     * with a fresh one. If no idle connection is available and the pool has
     * not reached {@code maxSize}, a new connection is created on demand.
     * Otherwise this call blocks for up to {@code connectionTimeoutMs}
     * milliseconds before throwing {@link SQLException}.
     *
     * <p>The returned {@code Connection} is wrapped so that calling
     * {@link Connection#close()} releases it back to the pool rather than
     * closing the underlying JDBC connection.
     *
     * @return a pooled, validated connection
     * @throws SQLException if no connection could be obtained within the timeout
     */
    public Connection acquire() throws SQLException {
        ensureOpen();

        // Try to get an idle connection immediately
        Connection conn = idlePool.poll();

        if (conn == null) {
            // Pool not exhausted — create on demand if possible
            synchronized (this) {
                if (allConnections.size() < maxSize) {
                    conn = DatabaseConnection.getConnection();
                    allConnections.add(conn);
                }
            }
        }

        if (conn == null) {
            // Wait for one to become available
            try {
                conn = idlePool.poll(connectionTimeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SQLException("Interrupted while waiting for a pooled connection", e);
            }
        }

        if (conn == null) {
            throw new SQLException(
                    "ConnectionPool: timed out after " + connectionTimeoutMs
                    + " ms waiting for an available connection (pool size=" + maxSize + ")");
        }

        // Validate — replace if stale
        conn = ensureValid(conn);

        return wrapForAutoRelease(conn);
    }

    /**
     * Returns the number of idle connections currently in the pool.
     *
     * @return idle connection count
     */
    public int idleCount() {
        return idlePool.size();
    }

    /**
     * Returns the total number of connections created by this pool
     * (idle + currently acquired).
     *
     * @return total connection count
     */
    public int totalCount() {
        return allConnections.size();
    }

    /**
     * Shuts down the pool, closing all managed connections.
     * After this call {@link #acquire()} will throw {@link SQLException}.
     */
    public void shutdown() {
        closed = true;
        synchronized (this) {
            for (Connection conn : allConnections) {
                closeQuietly(conn);
            }
            allConnections.clear();
            idlePool.clear();
        }
        LOGGER.info("ConnectionPool shut down.");
    }

    // --- Internal helpers ----------------------------------------------------

    /**
     * Releases a connection back to the idle pool.
     * Called by the wrapper's {@code close()} method.
     */
    void release(Connection underlying) {
        if (closed) {
            closeQuietly(underlying);
            return;
        }
        if (!idlePool.offer(underlying)) {
            // Pool is full (e.g. more connections returned than created) — close excess
            closeQuietly(underlying);
        }
    }

    /**
     * Validates {@code conn} using the configured validation query.
     * If stale, creates and registers a fresh replacement.
     */
    private Connection ensureValid(Connection conn) throws SQLException {
        try {
            if (!conn.isValid(2)) {
                throw new SQLException("Connection failed isValid() check");
            }
            try (var stmt = conn.prepareStatement(validationQuery)) {
                stmt.execute();
            }
            return conn;
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Stale connection detected; replacing: {0}", e.getMessage());
            closeQuietly(conn);
            synchronized (this) {
                allConnections.remove(conn);
                Connection fresh = DatabaseConnection.getConnection();
                allConnections.add(fresh);
                return fresh;
            }
        }
    }

    /**
     * Wraps a raw connection so that {@code close()} releases back to the pool.
     */
    private Connection wrapForAutoRelease(Connection underlying) {
        return new PooledConnectionWrapper(underlying, this);
    }

    private void ensureOpen() throws SQLException {
        if (closed) {
            throw new SQLException("ConnectionPool has been shut down");
        }
    }

    private static void closeQuietly(Connection conn) {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException ignored) {
            // nothing we can do
        }
    }

    // --- Config helpers ------------------------------------------------------

    private static int parseIntProperty(String key, int defaultVal) {
        try {
            return Integer.parseInt(DatabaseConnection.getProperty(key, String.valueOf(defaultVal)));
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private static long parseLongProperty(String key, long defaultVal) {
        try {
            return Long.parseLong(DatabaseConnection.getProperty(key, String.valueOf(defaultVal)));
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}
