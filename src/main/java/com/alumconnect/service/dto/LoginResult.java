package com.alumconnect.service.dto;

import com.alumconnect.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the result of an authentication attempt.
 */
public class LoginResult {

    private final boolean success;
    private final String message;
    private final User user;
    private final List<String> errors;

    private LoginResult(boolean success, String message, User user, List<String> errors) {
        this.success = success;
        this.message = message != null ? message : "";
        this.user = user;
        this.errors = errors != null ? Collections.unmodifiableList(new ArrayList<>(errors)) : Collections.emptyList();
    }

    /**
     * Creates a successful login result.
     *
     * @param message informative success message
     * @param user the authenticated User instance
     * @return successful LoginResult
     */
    public static LoginResult success(String message, User user) {
        return new LoginResult(true, message, user, Collections.emptyList());
    }

    /**
     * Creates a failed login result with a single error message.
     *
     * @param message description of the failure reason
     * @return failed LoginResult
     */
    public static LoginResult failure(String message) {
        List<String> errors = new ArrayList<>();
        if (message != null && !message.isBlank()) {
            errors.add(message);
        }
        return new LoginResult(false, message, null, errors);
    }

    /**
     * Creates a failed login result with detailed error messages.
     *
     * @param message general failure summary
     * @param errors list of error strings
     * @return failed LoginResult
     */
    public static LoginResult failure(String message, List<String> errors) {
        return new LoginResult(false, message, null, errors);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public User getUser() {
        return user;
    }

    public List<String> getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        return "LoginResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", user=" + (user != null ? user.getDisplayLabel() : "null") +
                ", errors=" + errors +
                '}';
    }
}
