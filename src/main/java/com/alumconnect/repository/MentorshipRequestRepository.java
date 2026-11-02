package com.alumconnect.repository;

import com.alumconnect.model.MentorshipRequest;
import com.alumconnect.model.MentorshipStatus;
import com.alumconnect.service.dto.MentorshipRequestDto;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Repository for mentorship request persistence and query operations.
 */
public interface MentorshipRequestRepository {
    /**
     * Creates a new mentorship request and returns the generated request id.
     */
    int createRequest(MentorshipRequestDto dto) throws SQLException;

    /**
     * Checks if a pending request already exists between the given student and alumni.
     */
    boolean existsPending(int studentId, int alumniId) throws SQLException;

    /**
     * Checks if an accepted request already exists between the given student and alumni.
     */
    boolean existsAccepted(int studentId, int alumniId) throws SQLException;

    /**
     * Returns the number of active mentees (status PENDING or ACCEPTED) for the alumni.
     */
    int countActiveMentees(int alumniId) throws SQLException;

    /**
     * Checks if the alumni has set availability for mentorship.
     */
    boolean isAlumniAvailable(int alumniId) throws SQLException;

    /**
     * Retrieves all mentorship requests received by the specified alumnus,
     * joined with student and student_profiles details.
     *
     * @param alumniId the target alumnus user ID
     * @return list of MentorshipRequest objects
     * @throws SQLException on database error
     */
    default List<MentorshipRequest> findRequestsByAlumniId(int alumniId) throws SQLException {
        return Collections.emptyList();
    }

    /**
     * Finds a single mentorship request by its primary key ID.
     *
     * @param requestId the mentorship request ID
     * @return Optional containing the MentorshipRequest if found
     * @throws SQLException on database error
     */
    default Optional<MentorshipRequest> findRequestById(int requestId) throws SQLException {
        return Optional.empty();
    }

    /**
     * Updates the status and responded_at timestamp for a mentorship request.
     *
     * @param requestId   the request ID to update
     * @param newStatus   the new MentorshipStatus (ACCEPTED or REJECTED)
     * @param respondedAt the timestamp when the response was made
     * @return true if updated, false otherwise
     * @throws SQLException on database error
     */
    default boolean updateRequestStatus(int requestId, MentorshipStatus newStatus, LocalDateTime respondedAt) throws SQLException {
        return false;
    }

    // Default aliases for flexibility
    default List<MentorshipRequest> findByAlumniId(int alumniId) throws SQLException {
        return findRequestsByAlumniId(alumniId);
    }

    default Optional<MentorshipRequest> findById(int requestId) throws SQLException {
        return findRequestById(requestId);
    }

    default boolean updateStatus(int requestId, MentorshipStatus newStatus, LocalDateTime respondedAt) throws SQLException {
        return updateRequestStatus(requestId, newStatus, respondedAt);
    }
}
