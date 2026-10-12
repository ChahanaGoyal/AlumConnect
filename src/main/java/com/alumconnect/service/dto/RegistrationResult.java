package com.alumconnect.service.dto;

import com.alumconnect.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the outcome of a user registration attempt, including success status,
 * human-readable feedback messages, optional validation error lists, and the registered {@link User}.
 */
public class RegistrationResult {

    private final boolean success;
    private final String message;
    private final User user;
    private final List<String> errors;

    private RegistrationResult(boolean success, String message, User user, List<String> errors) {
        this.success = success;
        this.message = message != null ? message : "";
        this.user = user;
        this.errors = errors != null ? Collections.unmodifiableList(new ArrayList<>(errors)) : Collections.emptyList();
    }

    /**
     * Creates a successful registration result.
     *
     * @param message informative success message
     * @param user    the persisted User instance
     * @return successful RegistrationResult
     */
    public static RegistrationResult success(String message, User user) {
        return new RegistrationResult(true, message, user, Collections.emptyList());
    }

    /**
     * Creates a failed registration result with a single error message.
     *
     * @param message description of the failure reason
     * @return failed RegistrationResult
     */
    public static RegistrationResult failure(String message) {
        List<String> errors = new ArrayList<>();
        if (message != null && !message.isBlank()) {
            errors.add(message);
        }
        return new RegistrationResult(false, message, null, errors);
    }

    /**
     * Creates a failed registration result with multiple detailed validation errors.
     *
     * @param message general failure summary message
     * @param errors  list of granular validation error strings
     * @return failed RegistrationResult
     */
    public static RegistrationResult failure(String message, List<String> errors) {
        return new RegistrationResult(false, message, null, errors);
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
        return "RegistrationResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", user=" + (user != null ? user.getDisplayLabel() : "null") +
                ", errors=" + errors +
                '}';
    }
}
