package com.alumconnect.util;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.Student;

import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * Validation utilities for user input and registration fields.
 *
 * <p>Enforces formatting constraints aligned with the database schema
 * and business rules:
 * <ul>
 *   <li>Email RFC 5322 simplified regex and DB {@code chk_email_format} constraint</li>
 *   <li>Graduation year range [1950, 2100]</li>
 *   <li>Student semester range [1, 12]</li>
 *   <li>Field length bounds</li>
 *   <li>LinkedIn profile URL validation</li>
 *   <li>Years of experience and mentee capacity checks</li>
 *   <li>Sanitized, safe database error formatting</li>
 * </ul>
 */
public final class ValidationUtil {

    /**
     * Regex matching standard email addresses while satisfying MySQL's
     * {@code email LIKE '%_@__%.__%'} CHECK constraint.
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    /**
     * Optional phone pattern allowing digits, +, spaces, hyphens, and parentheses.
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^[+]?[0-9\\s\\-().]{7,20}$"
    );

    /**
     * Regex matching LinkedIn profile URL prefixes and formats.
     */
    private static final Pattern LINKEDIN_PATTERN = Pattern.compile(
            "^(https?://)?([a-zA-Z0-9-]+\\.)?linkedin\\.com/.*$",
            Pattern.CASE_INSENSITIVE
    );

    private ValidationUtil() {
        // Utility class
    }

    /**
     * Validates whether an email string is well-formed.
     *
     * @param email candidate email
     * @return {@code true} if valid and non-blank; {@code false} otherwise
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank() || email.length() > 150) {
            return false;
        }
        String trimmed = email.trim();
        // Check regex and confirm minimum MySQL check constraint structure '%_@__%.__%'
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            return false;
        }
        int atIdx = trimmed.indexOf('@');
        int lastDot = trimmed.lastIndexOf('.');
        return atIdx >= 1 && (lastDot - atIdx) >= 3 && (trimmed.length() - lastDot) >= 3;
    }

    /**
     * Validates an optional phone number.
     *
     * @param phone candidate phone (can be null or blank)
     * @return {@code true} if valid or absent; {@code false} if non-blank but malformed
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return true;
        }
        String trimmed = phone.trim();
        return trimmed.length() <= 20 && PHONE_PATTERN.matcher(trimmed).matches();
    }

    /**
     * Validates graduation year against DB constraint [{@value Student#MIN_GRAD_YEAR}, {@value Student#MAX_GRAD_YEAR}].
     *
     * @param year graduation year
     * @return {@code true} if valid
     */
    public static boolean isValidGraduationYear(int year) {
        return year >= Student.MIN_GRAD_YEAR && year <= Student.MAX_GRAD_YEAR;
    }

    /**
     * Validates semester against DB constraint [{@value Student#MIN_SEMESTER}, {@value Student#MAX_SEMESTER}].
     *
     * @param semester semester number
     * @return {@code true} if valid
     */
    public static boolean isValidSemester(int semester) {
        return semester >= Student.MIN_SEMESTER && semester <= Student.MAX_SEMESTER;
    }

    /**
     * Validates an optional LinkedIn URL.
     *
     * @param url candidate LinkedIn URL (null or blank is considered valid as it is optional)
     * @return {@code true} if valid or absent; {@code false} if non-blank but invalid
     */
    public static boolean isValidLinkedInUrl(String url) {
        if (url == null || url.isBlank()) {
            return true;
        }
        String trimmed = url.trim();
        if (trimmed.length() > 255) {
            return false;
        }
        return LINKEDIN_PATTERN.matcher(trimmed).matches();
    }

    /**
     * Validates years of professional experience (0 to 70 years).
     *
     * @param years years of experience
     * @return {@code true} if valid; {@code false} if negative or exceeding maximum
     */
    public static boolean isValidYearsOfExperience(Integer years) {
        return years != null && years >= 0 && years <= 70;
    }

    /**
     * Validates maximum allowed mentees capacity (0 to 100 mentees).
     *
     * @param maxMentees maximum mentee capacity
     * @return {@code true} if valid; {@code false} if negative or exceeding maximum
     */
    public static boolean isValidMaxMentees(Integer maxMentees) {
        return maxMentees != null && maxMentees >= 0 && maxMentees <= 100;
    }

    /**
     * Validates that an entity ID is positive.
     *
     * @param id candidate ID
     * @return {@code true} if non-null and > 0
     */
    public static boolean isValidId(Integer id) {
        return id != null && id > 0;
    }

    /**
     * Validates mentorship request topic.
     *
     * @param topic request topic
     * @return {@code true} if non-blank and length <= 255
     */
    public static boolean isValidTopic(String topic) {
        return topic != null && !topic.isBlank() && topic.trim().length() <= 255;
    }

    /**
     * Validates mentorship request message.
     *
     * @param message request message
     * @return {@code true} if non-blank and length <= 2000
     */
    public static boolean isValidMessage(String message) {
        return message != null && !message.isBlank() && message.trim().length() <= 2000;
    }

    /**
     * Detects whether an SQL exception indicates a database connectivity or server availability issue.
     *
     * @param e the SQLException to evaluate
     * @return {@code true} if the exception is a connection failure
     */
    public static boolean isConnectionFailure(SQLException e) {
        if (e == null) {
            return false;
        }
        String sqlState = e.getSQLState() != null ? e.getSQLState() : "";
        if (sqlState.startsWith("08")) {
            return true;
        }
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return msg.contains("communications link failure")
                || msg.contains("connection refused")
                || msg.contains("connectionpool: timed out")
                || msg.contains("timed out waiting for an available connection")
                || msg.contains("cannot create poolableconnectionfactory")
                || msg.contains("connection reset")
                || msg.contains("connection has been closed")
                || msg.contains("database is currently unavailable")
                || msg.contains("unable to connect to database");
    }

    /**
     * Formats a safe, user-friendly database error message without exposing sensitive credentials or stack traces.
     *
     * @param e             the SQLException that occurred
     * @param defaultPrefix contextual prefix, e.g. "Database error while loading profile"
     * @return friendly message containing the prefix and safe description
     */
    public static String formatDatabaseErrorMessage(SQLException e, String defaultPrefix) {
        String prefix = (defaultPrefix != null && !defaultPrefix.isBlank()) ? defaultPrefix : "Database error";
        if (e == null) {
            return prefix + ": An unknown database error occurred.";
        }
        if (isConnectionFailure(e)) {
            return prefix + ": Database is currently unavailable. Please try again later.";
        }
        String rawMsg = e.getMessage() != null ? e.getMessage().trim() : "Unknown database error";
        String sanitized = sanitizeSensitiveDetails(rawMsg);
        return prefix + ": " + sanitized;
    }

    private static String sanitizeSensitiveDetails(String msg) {
        if (msg == null) return "";
        return msg.replaceAll("(?i)(password\\s*=\\s*['\"]?)([^'\"\\s&]+)(['\"]?)", "$1******$3");
    }
}
