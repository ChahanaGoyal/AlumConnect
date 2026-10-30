package com.alumconnect.service;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.Role;
import com.alumconnect.model.User;
import com.alumconnect.repository.AlumniProfileRepository;
import com.alumconnect.repository.JdbcAlumniProfileRepository;
import com.alumconnect.service.dto.AlumniDirectoryResult;
import com.alumconnect.util.UserSession;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service providing Alumni Directory browsing functionality for authenticated students (Prompt 12).
 *
 * <p>Enforces the following security invariants:
 * <ul>
 *   <li>Only authenticated students (role = {@link Role#STUDENT}) may view the alumni directory.</li>
 *   <li>Alumni, admin, unauthenticated, and logged-out users are denied access.</li>
 * </ul>
 *
 * <p>Database interactions are delegated to {@link AlumniProfileRepository} using parameter-safe
 * JDBC queries against the existing {@code users} and {@code alumni_profiles} tables.
 */
public class AlumniDirectoryService {

    private final AlumniProfileRepository profileRepository;

    /**
     * Default constructor backed by the real JDBC repository.
     */
    public AlumniDirectoryService() {
        this(new JdbcAlumniProfileRepository());
    }

    /**
     * Dependency-injection constructor for unit testing.
     *
     * @param profileRepository the repository implementation
     */
    public AlumniDirectoryService(AlumniProfileRepository profileRepository) {
        this.profileRepository = Objects.requireNonNull(profileRepository,
                "profileRepository must not be null");
    }

    /**
     * Retrieves all registered active alumni records for directory display.
     *
     * <p>Validates that the active session belongs to a logged-in student before querying.
     *
     * @return {@link AlumniDirectoryResult} carrying the alumni list on success, or an error message on failure
     */
    public AlumniDirectoryResult getAlumniDirectory() {
        // 1. Authentication check
        Optional<User> sessionUser = UserSession.getInstance().getCurrentUser();
        if (sessionUser.isEmpty()) {
            return AlumniDirectoryResult.failure(
                    "Access denied: You must be logged in to view the alumni directory.");
        }

        // 2. Role check - directory is student-accessible only
        User user = sessionUser.get();
        if (user.getRole() != Role.STUDENT) {
            return AlumniDirectoryResult.failure(
                    "Access denied: Only students can access the alumni directory.");
        }

        // 3. Database retrieval
        try {
            List<Alumni> list = profileRepository.findAllAlumni();
            if (list == null || list.isEmpty()) {
                return AlumniDirectoryResult.success(
                        "No registered alumni found in the directory.", Collections.emptyList());
            }
            return AlumniDirectoryResult.success(
                    "Alumni directory retrieved successfully.", list);
        } catch (SQLException e) {
            return AlumniDirectoryResult.failure(
                    "Database error while retrieving alumni directory: " + e.getMessage());
        } catch (Exception e) {
            return AlumniDirectoryResult.failure(
                    "Unexpected error while retrieving alumni directory: " + e.getMessage());
        }
    }
}

