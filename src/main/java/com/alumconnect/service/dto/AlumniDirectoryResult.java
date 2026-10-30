package com.alumconnect.service.dto;

import com.alumconnect.model.Alumni;

import java.util.Collections;
import java.util.List;

/**
 * Data Transfer Object carrying the result of an alumni directory query.
 *
 * <p>Mirrors the result object pattern used throughout the application
 * (e.g., {@link LoginResult}, {@link StudentProfileResult}, {@link AlumniProfileResult}).
 */
public class AlumniDirectoryResult {

    private final boolean success;
    private final String message;
    private final List<Alumni> alumniList;

    private AlumniDirectoryResult(boolean success, String message, List<Alumni> alumniList) {
        this.success = success;
        this.message = message != null ? message : "";
        this.alumniList = alumniList != null ? Collections.unmodifiableList(alumniList) : Collections.emptyList();
    }

    /**
     * Creates a successful result holding the list of alumni retrieved from the directory.
     *
     * @param message    status or count message
     * @param alumniList the retrieved list of alumni
     * @return successful AlumniDirectoryResult
     */
    public static AlumniDirectoryResult success(String message, List<Alumni> alumniList) {
        return new AlumniDirectoryResult(true, message, alumniList);
    }

    /**
     * Creates a failure result with a user-friendly error description.
     *
     * @param message description of why directory access or retrieval failed
     * @return failure AlumniDirectoryResult
     */
    public static AlumniDirectoryResult failure(String message) {
        return new AlumniDirectoryResult(false, message, Collections.emptyList());
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public List<Alumni> getAlumniList() {
        return alumniList;
    }

    @Override
    public String toString() {
        return "AlumniDirectoryResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", count=" + alumniList.size() +
                '}';
    }
}

