package com.alumconnect.service.dto;

import com.alumconnect.model.Student;

/**
 * Data Transfer Object carrying the result of a student profile operation
 * (view or update).
 *
 * <p>Mirrors the pattern of {@link RegistrationResult} and {@link LoginResult}.
 */
public class StudentProfileResult {

    private final boolean success;
    private final String message;
    private final Student student;

    private StudentProfileResult(boolean success, String message, Student student) {
        this.success = success;
        this.message = message != null ? message : "";
        this.student = student;
    }

    // ── Factory methods ───────────────────────────────────────────────────────

    /**
     * Creates a successful result carrying the Student profile.
     *
     * @param message informative success message
     * @param student the Student whose profile was operated on
     * @return successful StudentProfileResult
     */
    public static StudentProfileResult success(String message, Student student) {
        return new StudentProfileResult(true, message, student);
    }

    /**
     * Creates a failed result with a descriptive error message.
     *
     * @param message description of the failure reason
     * @return failed StudentProfileResult
     */
    public static StudentProfileResult failure(String message) {
        return new StudentProfileResult(false, message, null);
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Student getStudent() { return student; }

    @Override
    public String toString() {
        return "StudentProfileResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", student=" + (student != null ? student.getFullName() : "null") +
                '}';
    }
}
