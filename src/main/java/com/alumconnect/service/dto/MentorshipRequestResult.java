package com.alumconnect.service.dto;

import com.alumconnect.model.MentorshipRequest;

import java.util.Collections;
import java.util.List;

/**
 * Result wrapper for mentorship request operations.
 */
public class MentorshipRequestResult {
    private final boolean success;
    private final String message;
    private final Integer requestId; // nullable, present on single-request success
    private final List<MentorshipRequest> requests; // nullable, present on query success

    private MentorshipRequestResult(boolean success, String message, Integer requestId, List<MentorshipRequest> requests) {
        this.success = success;
        this.message = message;
        this.requestId = requestId;
        this.requests = requests != null ? List.copyOf(requests) : Collections.emptyList();
    }

    public static MentorshipRequestResult success(int requestId, String message) {
        return new MentorshipRequestResult(true, message, requestId, null);
    }

    public static MentorshipRequestResult success(List<MentorshipRequest> requests, String message) {
        return new MentorshipRequestResult(true, message, null, requests);
    }

    public static MentorshipRequestResult failure(String message) {
        return new MentorshipRequestResult(false, message, null, null);
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

    public List<MentorshipRequest> getRequests() {
        return requests != null ? requests : Collections.emptyList();
    }
}
