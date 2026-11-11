package com.alumconnect.web;

import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP helper utilities for parsing requests, reading form payloads,
 * formatting cookies, and sending responses in the lightweight web server.
 */
public final class HttpUtil {

    private HttpUtil() {
        // Prevent instantiation
    }

    /**
     * Reads and parses URL-encoded form data (application/x-www-form-urlencoded).
     *
     * @param exchange the HTTP exchange
     * @return map of form fields and decoded values
     * @throws IOException on I/O error
     */
    public static Map<String, String> parseFormData(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[1024];
        int read;
        while ((read = is.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        String body = buffer.toString(StandardCharsets.UTF_8);
        return parseQueryString(body);
    }

    /**
     * Parses query parameters from a URI.
     *
     * @param uri the request URI
     * @return map of query parameters
     */
    public static Map<String, String> parseQueryParams(URI uri) {
        if (uri == null || uri.getRawQuery() == null) {
            return Collections.emptyMap();
        }
        return parseQueryString(uri.getRawQuery());
    }

    private static Map<String, String> parseQueryString(String qs) {
        if (qs == null || qs.isBlank()) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new HashMap<>();
        String[] pairs = qs.split("&");
        for (String pair : pairs) {
            if (pair.isEmpty()) continue;
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String val = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                result.put(key, val);
            } else if (idx < 0) {
                String key = URLDecoder.decode(pair, StandardCharsets.UTF_8);
                result.put(key, "");
            }
        }
        return result;
    }

    /**
     * Sends an HTML response with UTF-8 encoding.
     *
     * @param exchange   the HTTP exchange
     * @param statusCode HTTP status code (e.g. 200, 400, 403, 404)
     * @param html       HTML content string
     * @throws IOException on I/O error
     */
    public static void sendHtml(HttpExchange exchange, int statusCode, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * Sends a plain text response with UTF-8 encoding.
     *
     * @param exchange   the HTTP exchange
     * @param statusCode HTTP status code
     * @param text       plain text content
     * @throws IOException on I/O error
     */
    public static void sendText(HttpExchange exchange, int statusCode, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * Sends an HTTP 302 redirect response.
     *
     * @param exchange the HTTP exchange
     * @param location destination URL or path
     * @throws IOException on I/O error
     */
    public static void sendRedirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

    /**
     * Extracts a named cookie value from the request headers.
     *
     * @param exchange   the HTTP exchange
     * @param cookieName name of the cookie
     * @return cookie value, or null if not found
     */
    public static String getCookie(HttpExchange exchange, String cookieName) {
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader == null || cookieHeader.isBlank()) {
            return null;
        }
        String[] cookies = cookieHeader.split(";");
        for (String c : cookies) {
            String trimmed = c.trim();
            int eq = trimmed.indexOf('=');
            if (eq > 0 && trimmed.substring(0, eq).trim().equals(cookieName)) {
                return trimmed.substring(eq + 1).trim();
            }
        }
        return null;
    }

    /**
     * Sets a cookie on the response headers.
     *
     * @param exchange   the HTTP exchange
     * @param name       cookie name
     * @param value      cookie value
     * @param maxAgeSecs maximum age in seconds (0 to delete)
     */
    public static void setCookie(HttpExchange exchange, String name, String value, int maxAgeSecs) {
        String header = String.format("%s=%s; Path=/; Max-Age=%d; HttpOnly; SameSite=Lax",
                name, value, maxAgeSecs);
        exchange.getResponseHeaders().add("Set-Cookie", header);
    }

    /**
     * Escapes HTML special characters to prevent cross-site scripting (XSS).
     *
     * @param input raw string
     * @return HTML-escaped string, or empty string if input is null
     */
    public static String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

