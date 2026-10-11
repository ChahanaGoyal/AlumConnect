package com.alumconnect.model;

/**
 * Represents the role of a registered user on the AlumConnect platform.
 *
 * <p>Maps directly to the {@code role} column {@code ENUM('STUDENT','ALUMNI','ADMIN')}
 * in the {@code users} table. Each role determines which profile table is associated:
 * <ul>
 *   <li>{@link #STUDENT} — linked to {@code student_profiles}</li>
 *   <li>{@link #ALUMNI}  — linked to {@code alumni_profiles}</li>
 *   <li>{@link #ADMIN}   — platform administrator; no separate profile table</li>
 * </ul>
 */
public enum Role {

    /** An enrolled student who can browse alumni and request mentorship. */
    STUDENT,

    /** A graduated alumnus who can offer mentorship to students. */
    ALUMNI,

    /** A platform administrator with elevated privileges. */
    ADMIN;

    /**
     * Returns the {@code Role} whose name matches {@code value} (case-insensitive).
     *
     * @param value the string representation, e.g. {@code "student"} or {@code "ALUMNI"}
     * @return the matching {@code Role}
     * @throws IllegalArgumentException if no matching role exists
     */
    public static Role fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Role value must not be null or blank");
        }
        return Role.valueOf(value.trim().toUpperCase());
    }
}
