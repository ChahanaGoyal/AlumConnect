package com.alumconnect.repository;

import com.alumconnect.service.dto.MentorshipRequestDto;
import java.sql.SQLException;

/**
 * Repository for mentorship request operations.
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
}

