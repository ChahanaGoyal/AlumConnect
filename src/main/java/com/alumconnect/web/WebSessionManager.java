package com.alumconnect.web;

import com.alumconnect.model.Role;
import com.alumconnect.model.User;
import com.alumconnect.util.UserSession;
import com.sun.net.httpserver.HttpExchange;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages HTTP session tracking using in-memory token mapping and cookies.
 * Also provides thread-safe execution bridge to the legacy {@link UserSession} singleton.
 */
public final class WebSessionManager {

    public static final String COOKIE_NAME = "ALUM_SESSION";
    private static final long SESSION_TIMEOUT_MS = 2 * 60 * 60 * 1000L; // 2 hours

    private static final Map<String, SessionRecord> SESSIONS = new ConcurrentHashMap<>();

    private WebSessionManager() {
        // Prevent instantiation
    }

    private static class SessionRecord {
        private final User user;
        private volatile long lastAccessTime;

        SessionRecord(User user) {
            this.user = user;
            this.lastAccessTime = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - lastAccessTime > SESSION_TIMEOUT_MS;
        }

        void touch() {
            this.lastAccessTime = System.currentTimeMillis();
        }
    }

    /**
     * Creates an active session for the user and sets the session cookie on the response.
     *
     * @param exchange HTTP exchange
     * @param user     authenticated user
     * @return session token string
     */
    public static String createSession(HttpExchange exchange, User user) {
        String token = UUID.randomUUID().toString();
        SESSIONS.put(token, new SessionRecord(user));
        HttpUtil.setCookie(exchange, COOKIE_NAME, token, (int) (SESSION_TIMEOUT_MS / 1000));
        return token;
    }

    /**
     * Resolves the authenticated user from the exchange cookie.
     *
     * @param exchange HTTP exchange
     * @return Optional containing the User if logged in and not expired
     */
    public static Optional<User> getAuthenticatedUser(HttpExchange exchange) {
        String token = HttpUtil.getCookie(exchange, COOKIE_NAME);
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        SessionRecord record = SESSIONS.get(token);
        if (record == null) {
            return Optional.empty();
        }
        if (record.isExpired()) {
            SESSIONS.remove(token);
            return Optional.empty();
        }
        record.touch();
        return Optional.of(record.user);
    }

    /**
     * Destroys the current session and clears the cookie.
     *
     * @param exchange HTTP exchange
     */
    public static void destroySession(HttpExchange exchange) {
        String token = HttpUtil.getCookie(exchange, COOKIE_NAME);
        if (token != null) {
            SESSIONS.remove(token);
        }
        HttpUtil.setCookie(exchange, COOKIE_NAME, "", 0);
    }

    /**
     * Clears all sessions (useful during test teardown).
     */
    public static void clearAllSessions() {
        SESSIONS.clear();
    }

    /**
     * Functional interface for supplier actions throwing exceptions.
     */
    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    /**
     * Functional interface for runnable actions throwing exceptions.
     */
    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    /**
     * Bridges an action to the singleton {@link UserSession} in a thread-safe synchronized manner.
     * Sets the logged in user, runs the action, and guarantees clean logout in finally block.
     *
     * @param user   the user to run as
     * @param action supplier action
     * @param <T>    return type
     * @return result of action
     * @throws Exception if action fails
     */
    public static <T> T executeAs(User user, ThrowingSupplier<T> action) throws Exception {
        synchronized (UserSession.class) {
            UserSession.getInstance().login(user);
            try {
                return action.get();
            } finally {
                UserSession.getInstance().logout();
            }
        }
    }

    /**
     * Bridges a runnable action to the singleton {@link UserSession}.
     *
     * @param user   the user to run as
     * @param action runnable action
     * @throws Exception if action fails
     */
    public static void executeAs(User user, ThrowingRunnable action) throws Exception {
        synchronized (UserSession.class) {
            UserSession.getInstance().login(user);
            try {
                action.run();
            } finally {
                UserSession.getInstance().logout();
            }
        }
    }
}

