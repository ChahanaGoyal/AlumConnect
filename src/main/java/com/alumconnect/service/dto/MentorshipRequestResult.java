package com.alumconnect.service.dto;

/**
 * Result wrapper for mentorship request operations.
 */
public class MentorshipRequestResult {
    private boolean success;
    private String message;
    private Integer requestId; // nullable, present on success

    private MentorshipRequestResult(boolean success, String message, Integer requestId) {
        this.success = success;
        this.message = message;
        this.requestId = requestId;
    }

    public static MentorshipRequestResult success(int requestId, String message) {
        return new MentorshipRequestResult(true, message, requestId);
    }

    public static MentorshipRequestResult failure(String message) {
        return new MentorshipRequestResult(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Integer getRequestId() {
        return requestId;
    }
}

