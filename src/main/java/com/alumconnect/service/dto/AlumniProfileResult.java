package com.alumconnect.service.dto;

import com.alumconnect.model.Alumni;

/**
 * Data Transfer Object carrying the result of an alumni profile operation
 * (view, create, or update).
 */
public class AlumniProfileResult {

    private final boolean success;
    private final String message;
    private final Alumni alumni;

    private AlumniProfileResult(boolean success, String message, Alumni alumni) {
        this.success = success;
        this.message = message != null ? message : "";
        this.alumni = alumni;
    }

    // ── Factory methods ───────────────────────────────────────────────────────

    /**
     * Creates a successful result carrying the Alumni profile.
     *
     * @param message informative success message
     * @param alumni the Alumni whose profile was operated on
     * @return successful AlumniProfileResult
     */
    public static AlumniProfileResult success(String message, Alumni alumni) {
        return new AlumniProfileResult(true, message, alumni);
    }

    /**
     * Creates a failed result with a descriptive error message.
     *
     * @param message description of the failure reason
     * @return failed AlumniProfileResult
     */
    public static AlumniProfileResult failure(String message) {
        return new AlumniProfileResult(false, message, null);
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Alumni getAlumni() { return alumni; }

    @Override
    public String toString() {
        return "AlumniProfileResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", alumni=" + (alumni != null ? alumni.getFullName() : "null") +
                '}';
    }
}
