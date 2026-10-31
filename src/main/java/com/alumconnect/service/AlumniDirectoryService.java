package com.alumconnect.service;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.Role;
import com.alumconnect.model.User;
import com.alumconnect.repository.AlumniProfileRepository;
import com.alumconnect.repository.JdbcAlumniProfileRepository;
import com.alumconnect.service.dto.AlumniDirectoryResult;
import com.alumconnect.service.dto.AlumniFilterCriteria;
import com.alumconnect.util.UserSession;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service providing Alumni Directory browsing, searching, and structured filtering
 * functionality for authenticated students (Prompt 12, Prompt 13 &amp; Prompt 14).
 *
 * <p>Enforces the following security invariants:
 * <ul>
 *   <li>Only authenticated students (role = {@link Role#STUDENT}) may view, search, or filter the alumni directory.</li>
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

    /**
     * Searches registered active alumni records matching a keyword across name, company,
     * role, industry, skills, and department (Prompt 13).
     *
     * <p>Validates that the active session belongs to a logged-in student and that
     * the keyword is not empty before querying.
     *
     * @param keyword search keyword (case-insensitive)
     * @return {@link AlumniDirectoryResult} carrying matching alumni on success, or an error message on failure
     */
    public AlumniDirectoryResult searchAlumni(String keyword) {
        // 1. Authentication check
        Optional<User> sessionUser = UserSession.getInstance().getCurrentUser();
        if (sessionUser.isEmpty()) {
            return AlumniDirectoryResult.failure(
                    "Access denied: You must be logged in to search alumni.");
        }

        // 2. Role check - search is student-accessible only
        User user = sessionUser.get();
        if (user.getRole() != Role.STUDENT) {
            return AlumniDirectoryResult.failure(
                    "Access denied: Only students can search the alumni directory.");
        }

        // 3. Keyword validation
        if (keyword == null || keyword.trim().isEmpty()) {
            return AlumniDirectoryResult.failure(
                    "Search keyword cannot be empty.");
        }

        String trimmedKeyword = keyword.trim();

        // 4. Database search
        try {
            List<Alumni> list = profileRepository.searchAlumni(trimmedKeyword);
            if (list == null || list.isEmpty()) {
                return AlumniDirectoryResult.success(
                        "No alumni found matching '" + trimmedKeyword + "'.", Collections.emptyList());
            }
            return AlumniDirectoryResult.success(
                    "Found " + list.size() + " alumni matching '" + trimmedKeyword + "'.", list);
        } catch (SQLException e) {
            return AlumniDirectoryResult.failure(
                    "Database error while searching alumni: " + e.getMessage());
        } catch (Exception e) {
            return AlumniDirectoryResult.failure(
                    "Unexpected error while searching alumni: " + e.getMessage());
        }
    }

    /**
     * Filters registered active alumni records using structured criteria (Prompt 14).
     *
     * <p>Enforces student role security and validates input ranges before executing
     * parameterized database queries.
     *
     * @param criteria structured filter criteria
     * @return {@link AlumniDirectoryResult} carrying matching alumni on success, or error message on failure
     */
    public AlumniDirectoryResult filterAlumni(AlumniFilterCriteria criteria) {
        // 1. Authentication check
        Optional<User> sessionUser = UserSession.getInstance().getCurrentUser();
        if (sessionUser.isEmpty()) {
            return AlumniDirectoryResult.failure(
                    "Access denied: You must be logged in to filter alumni.");
        }

        // 2. Role check - filtering is student-accessible only
        User user = sessionUser.get();
        if (user.getRole() != Role.STUDENT) {
            return AlumniDirectoryResult.failure(
                    "Access denied: Only students can access alumni filtering.");
        }

        // 3. Criteria validation
        if (criteria != null) {
            List<String> errors = criteria.validate();
            if (!errors.isEmpty()) {
                return AlumniDirectoryResult.failure(String.join("; ", errors));
            }
        }

        // 4. Database query
        try {
            List<Alumni> list = profileRepository.filterAlumni(criteria);
            if (list == null || list.isEmpty()) {
                return AlumniDirectoryResult.success(
                        "No alumni found matching the selected filters.", Collections.emptyList());
            }
            return AlumniDirectoryResult.success(
                    "Found " + list.size() + " matching alumni for selected filters.", list);
        } catch (SQLException e) {
            return AlumniDirectoryResult.failure(
                    "Database error while filtering alumni: " + e.getMessage());
        } catch (Exception e) {
            return AlumniDirectoryResult.failure(
                    "Unexpected error while filtering alumni: " + e.getMessage());
        }
    }
}
