package com.alumconnect.repository;

import com.alumconnect.model.Alumni;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Data Access interface for alumni profile persistence operations.
 *
 * <p>Handles reading, updating, and creating records in the {@code alumni_profiles}
 * table for an authenticated alumni user, as well as directory retrieval and search of all alumni.
 */
public interface AlumniProfileRepository {

    /**
     * Finds the full alumni profile (users + alumni_profiles JOIN) by the user's primary key.
     *
     * @param userId the {@code users.id} PK of the logged-in alumni
     * @return Optional containing the Alumni if a matching profile row exists
     * @throws SQLException if a database error occurs
     */
    Optional<Alumni> findProfileByUserId(int userId) throws SQLException;

    /**
     * Updates mutable profile fields in the {@code alumni_profiles} table for the given alumni user.
     *
     * <p>The fields updated include:
     * degree, department, graduation_year, current_company, current_role, industry,
     * years_of_experience, expertise_areas, bio, linkedin_url,
     * is_available_for_mentorship, max_mentees.
     *
     * @param alumni the Alumni object containing updated profile data; {@code getId()} is used
     *               as the WHERE clause key (user_id)
     * @return {@code true} if at least one row was updated; {@code false} if no matching profile found
     * @throws SQLException if a database error or constraint violation occurs
     */
    boolean updateProfile(Alumni alumni) throws SQLException;

    /**
     * Creates a new alumni_profiles row for the given alumni.
     *
     * @param alumni the Alumni model containing the profile details; {@code getId()} is used
     *               as the foreign key (user_id)
     * @return {@code true} if inserted successfully
     * @throws SQLException if a database error occurs
     */
    boolean createProfile(Alumni alumni) throws SQLException;

    /**
     * Checks whether an {@code alumni_profiles} row already exists for the given user.
     *
     * @param userId the {@code users.id} PK
     * @return {@code true} if a profile row exists
     * @throws SQLException if a database error occurs
     */
    boolean profileExistsForUser(int userId) throws SQLException;

    /**
     * Retrieves all registered active alumni records for the alumni directory, joining
     * {@code users} and {@code alumni_profiles} where {@code role = 'ALUMNI'} and {@code is_active = TRUE}.
     *
     * @return list of Alumni profiles; empty list if no alumni found
     * @throws SQLException if a database error occurs
     */
    default List<Alumni> findAllAlumni() throws SQLException {
        return Collections.emptyList();
    }

    /**
     * Searches registered active alumni records whose name, current company, role,
     * industry, skills/expertise, or department contains the specified keyword (case-insensitive).
     *
     * @param keyword the search term
     * @return list of matching Alumni profiles; empty list if no alumni match or keyword is blank
     * @throws SQLException if a database error occurs
     */
    default List<Alumni> searchAlumni(String keyword) throws SQLException {
        if (keyword == null || keyword.isBlank()) {
            return Collections.emptyList();
        }
        String lower = keyword.trim().toLowerCase();
        return findAllAlumni().stream()
                .filter(a -> (a.getFullName() != null && a.getFullName().toLowerCase().contains(lower))
                        || (a.getCurrentCompany() != null && a.getCurrentCompany().toLowerCase().contains(lower))
                        || (a.getCurrentRole() != null && a.getCurrentRole().toLowerCase().contains(lower))
                        || (a.getIndustry() != null && a.getIndustry().toLowerCase().contains(lower))
                        || (a.getExpertiseAreas() != null && a.getExpertiseAreas().toLowerCase().contains(lower))
                        || (a.getDepartment() != null && a.getDepartment().toLowerCase().contains(lower)))
                .toList();
    }
}
