package com.alumconnect.util;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.Student;

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
}
