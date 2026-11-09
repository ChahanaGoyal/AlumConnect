package com.alumconnect.service;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.MentorshipRequest;
import com.alumconnect.model.MentorshipStatus;
import com.alumconnect.model.Role;
import com.alumconnect.model.User;
import com.alumconnect.repository.AlumniProfileRepository;
import com.alumconnect.repository.JdbcAlumniProfileRepository;
import com.alumconnect.repository.JdbcMentorshipRequestRepository;
import com.alumconnect.repository.JdbcUserRepository;
import com.alumconnect.repository.MentorshipRequestRepository;
import com.alumconnect.repository.UserRepository;
import com.alumconnect.service.dto.MentorshipRequestDto;
import com.alumconnect.service.dto.MentorshipRequestResult;
import com.alumconnect.util.UserSession;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service handling mentorship request operations across student and alumni lifecycles.
 *
 * <p>Supports:
 * <ul>
 *   <li>Sending mentorship requests (Student role - Prompt 16)</li>
 *   <li>Retrieving received mentorship requests (Alumni role - Prompt 17)</li>
 *   <li>Accepting pending mentorship requests (Alumni role - Prompt 17)</li>
 *   <li>Rejecting pending mentorship requests (Alumni role - Prompt 17)</li>
 *   <li>Retrieving sent mentorship requests (Student role - Prompt 18)</li>
 *   <li>Retrieving accepted mentorship connections (Student & Alumni roles - Prompt 19)</li>
 * </ul>
 */
public class MentorshipRequestService {

    private final MentorshipRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final AlumniProfileRepository alumniProfileRepository;

    /** Default constructor backed by the real JDBC repositories. */
    public MentorshipRequestService() {
        this(new JdbcMentorshipRequestRepository(), new JdbcUserRepository(), new JdbcAlumniProfileRepository());
    }

    /** Constructor allowing injection of request and user repositories. */
    public MentorshipRequestService(MentorshipRequestRepository requestRepository,
                                    UserRepository userRepository) {
        this(requestRepository, userRepository, new JdbcAlumniProfileRepository());
    }

    /** Dependency‑injection constructor for unit testing. */
    public MentorshipRequestService(MentorshipRequestRepository requestRepository,
                                    UserRepository userRepository,
                                    AlumniProfileRepository alumniProfileRepository) {
        this.requestRepository = Objects.requireNonNull(requestRepository, "requestRepository must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.alumniProfileRepository = Objects.requireNonNull(alumniProfileRepository, "alumniProfileRepository must not be null");
    }

    /**
     * Sends a mentorship request from the currently logged‑in student to the selected alumni.
     *
     * @param alumniId target alumni user id
     * @param topic    mentorship request topic
     * @param message  mentorship request message
     * @return result containing success/failure information and request id when successful
     */
    public MentorshipRequestResult sendRequest(int alumniId, String topic, String message) {
        // 1. Authentication check
        Optional<User> sessionUser = UserSession.getInstance().getCurrentUser();
        if (sessionUser.isEmpty()) {
            return MentorshipRequestResult.failure("Access denied: You must be logged in to send a mentorship request.");
        }
        // 2. Role check
        User user = sessionUser.get();
        if (user.getRole() != Role.STUDENT) {
            return MentorshipRequestResult.failure("Access denied: Only students can send mentorship requests.");
        }
        int studentId = user.getId();
        // 3. Basic input validation
        if (alumniId <= 0) {
            return MentorshipRequestResult.failure("Invalid alumni selected.");
        }
        if (topic == null || topic.isBlank()) {
            return MentorshipRequestResult.failure("Mentorship topic is required.");
        }
        if (message == null || message.isBlank()) {
            return MentorshipRequestResult.failure("Mentorship message is required.");
        }
        topic = topic.trim();
        message = message.trim();
        if (topic.length() > 255) {
            return MentorshipRequestResult.failure("Mentorship topic cannot exceed 255 characters.");
        }
        if (message.length() > 2000) {
            return MentorshipRequestResult.failure("Mentorship message cannot exceed 2000 characters.");
        }
        // 4. Self‑request check
        if (studentId == alumniId) {
            return MentorshipRequestResult.failure("You cannot send a mentorship request to yourself.");
        }

        try {
            // 5. Verify target exists and is an alumni
            Optional<Alumni> alumniOpt = userRepository.findAlumniById(alumniId);
            if (alumniOpt.isEmpty()) {
                return MentorshipRequestResult.failure("Target user does not exist or is not an alumni.");
            }
            Alumni alumni = alumniOpt.get();

            // 6. Duplicate pending request
            if (requestRepository.existsPending(studentId, alumniId)) {
                return MentorshipRequestResult.failure("A pending mentorship request already exists.");
            }

            // 7. Duplicate accepted request
            if (requestRepository.existsAccepted(studentId, alumniId)) {
                return MentorshipRequestResult.failure("You already have an accepted mentorship request with this alumni.");
            }

            // 8. Alumni mentorship availability
            if (!requestRepository.isAlumniAvailable(alumniId)) {
                return MentorshipRequestResult.failure("This alumni is currently not available for mentorship.");
            }

            // 9. Capacity check against max mentees
            int activeMentees = requestRepository.countActiveMentees(alumniId);
            int maxMentees = alumni.getMaxMentees();
            if (activeMentees >= maxMentees) {
                return MentorshipRequestResult.failure("Alumni has reached the maximum number of mentees.");
            }

            // 10. Create request
            MentorshipRequestDto dto = new MentorshipRequestDto(studentId, alumniId, topic, message);
            int requestId = requestRepository.createRequest(dto);
            if (requestId <= 0) {
                return MentorshipRequestResult.failure("Failed to create mentorship request.");
            }
            return MentorshipRequestResult.success(requestId, "Mentorship request sent successfully.");

        } catch (SQLException e) {
            return MentorshipRequestResult.failure("Database error while sending mentorship request: " + e.getMessage());
        }
    }

    /**
     * Retrieves all mentorship requests received by the specified alumnus.
     *
     * <p>Security invariants:
     * <ul>
     *   <li>User must be authenticated.</li>
     *   <li>User must have the {@link Role#ALUMNI} role.</li>
     *   <li>User can only view requests sent to themselves (alumniId must match session user ID).</li>
     * </ul>
     *
     * @param alumniId target alumni user ID
     * @return {@link MentorshipRequestResult} carrying the list of requests on success, or an error message on failure
     */
    public MentorshipRequestResult getRequestsForAlumni(int alumniId) {
        // 1. Authentication check
        Optional<User> sessionUser = UserSession.getInstance().getCurrentUser();
        if (sessionUser.isEmpty()) {
            return MentorshipRequestResult.failure("Access denied: You must be logged in to view mentorship requests.");
        }

        // 2. Role check
        User user = sessionUser.get();
        if (user.getRole() != Role.ALUMNI) {
            return MentorshipRequestResult.failure("Access denied: Only alumni can view received mentorship requests.");
        }

        // 3. ID validation
        if (alumniId <= 0) {
            return MentorshipRequestResult.failure("Invalid alumni ID: ID must be a positive integer.");
        }

        // 4. Ownership check: alumnus can only view their own received requests
        if (user.getId() == null || user.getId() != alumniId) {
            return MentorshipRequestResult.failure("Access denied: You cannot view mentorship requests for another alumnus.");
        }

        // 5. Query repository
        try {
            List<MentorshipRequest> requests = requestRepository.findRequestsByAlumniId(alumniId);
            return MentorshipRequestResult.success(requests, "Retrieved " + requests.size() + " mentorship request(s).");
        } catch (SQLException e) {
            return MentorshipRequestResult.failure("Database error while retrieving mentorship requests: " + e.getMessage());
        }
    }

    /**
     * Convenience method to retrieve received requests for the currently logged-in alumnus.
     *
     * @return result containing list of requests on success, or error message on failure
     */
    public MentorshipRequestResult getReceivedRequests() {
        Optional<User> sessionUser = UserSession.getInstance().getCurrentUser();
        if (sessionUser.isEmpty() || sessionUser.get().getRole() != Role.ALUMNI || sessionUser.get().getId() == null) {
            return getRequestsForAlumni(-1); // will trigger proper auth/role failure
        }
        return getRequestsForAlumni(sessionUser.get().getId());
    }

    /**
     * Direct list-returning method for convenience.
     *
     * @param alumniId target alumni user ID
     * @return list of MentorshipRequest
     * @throws SecurityException if unauthorized
     * @throws IllegalArgumentException if ID is invalid
     */
    public List<MentorshipRequest> getRequests(int alumniId) {
        MentorshipRequestResult result = getRequestsForAlumni(alumniId);
        if (!result.isSuccess()) {
            if (result.getMessage().startsWith("Access denied")) {
                throw new SecurityException(result.getMessage());
            }
            throw new IllegalArgumentException(result.getMessage());
        }
        return result.getRequests();
    }

    /**
     * Retrieves all mentorship requests sent by the specified student.
     *
     * <p>Security invariants:
     * <ul>
     *   <li>User must be authenticated.</li>
     *   <li>User must have the {@link Role#STUDENT} role.</li>
     *   <li>User can only view requests sent by themselves (studentId must match session user ID).</li>
     * </ul>
     *
     * @param studentId target student user ID
     * @return {@link MentorshipRequestResult} carrying the list of requests on success, or an error message on failure
     */
    public MentorshipRequestResult getRequestsForStudent(int studentId) {
        // 1. Authentication check
        Optional<User> sessionUser = UserSession.getInstance().getCurrentUser();
        if (sessionUser.isEmpty()) {
            return MentorshipRequestResult.failure("Access denied: You must be logged in to view your mentorship requests.");
        }

        // 2. Role check
        User user = sessionUser.get();
        if (user.getRole() != Role.STUDENT) {
            return MentorshipRequestResult.failure("Access denied: Only students can view sent mentorship requests.");
        }

        // 3. ID validation
        if (studentId <= 0) {
            return MentorshipRequestResult.failure("Invalid student ID: ID must be a positive integer.");
        }

        // 4. Ownership check: student can only view their own sent requests
        if (user.getId() == null || user.getId() != studentId) {
            return MentorshipRequestResult.failure("Access denied: You cannot view mentorship requests for another student.");
        }

        // 5. Query repository
        try {
            List<MentorshipRequest> requests = requestRepository.findRequestsByStudentId(studentId);
            return MentorshipRequestResult.success(requests, "Retrieved " + requests.size() + " mentorship request(s).");
        } catch (SQLException e) {
            return MentorshipRequestResult.failure("Database error while retrieving mentorship requests: " + e.getMessage());
        }
    }

    /**
     * Convenience method to retrieve sent requests for the currently logged-in student.
     *
     * @return result containing list of requests on success, or error message on failure
     */
    public MentorshipRequestResult getMyRequests() {
        Optional<User> sessionUser = UserSession.getInstance().getCurrentUser();
        if (sessionUser.isEmpty() || sessionUser.get().getRole() != Role.STUDENT || sessionUser.get().getId() == null) {
            return getRequestsForStudent(-1); // will trigger proper auth/role failure
        }
        return getRequestsForStudent(sessionUser.get().getId());
    }

    /**
     * Direct list-returning method for student requests.
     *
     * @param studentId target student user ID
     * @return list of MentorshipRequest
     * @throws SecurityException if unauthorized
     * @throws IllegalArgumentException if ID is invalid
     */
    public List<MentorshipRequest> getStudentRequests(int studentId) {
        MentorshipRequestResult result = getRequestsForStudent(studentId);
        if (!result.isSuccess()) {
            if (result.getMessage().startsWith("Access denied")) {
                throw new SecurityException(result.getMessage());
            }
            throw new IllegalArgumentException(result.getMessage());
        }
        return result.getRequests();
    }

    /**
     * Retrieves accepted mentorship connections for the specified student.
     *
     * <p>Security invariants:
     * <ul>
     *   <li>User must be authenticated.</li>
     *   <li>User must have the {@link Role#STUDENT} role.</li>
     *   <li>User can only view their own connections (studentId must match session user ID).</li>
     * </ul>
     *
     * @param studentId target student user ID
     * @return {@link MentorshipRequestResult} carrying the list of accepted connections on success, or an error message on failure
     */
    public MentorshipRequestResult getConnectionsForStudent(int studentId) {
        // 1. Authentication check
        Optional<User> sessionUser = UserSession.getInstance().getCurrentUser();
        if (sessionUser.isEmpty()) {
            return MentorshipRequestResult.failure("Access denied: You must be logged in to view mentorship connections.");
        }

        // 2. Role check
        User user = sessionUser.get();
        if (user.getRole() != Role.STUDENT) {
            return MentorshipRequestResult.failure("Access denied: Only students can view student mentorship connections.");
        }

        // 3. ID validation
        if (studentId <= 0) {
            return MentorshipRequestResult.failure("Invalid student ID: ID must be a positive integer.");
        }

        // 4. Ownership check: student can only view their own connections
        if (user.getId() == null || user.getId() != studentId) {
            return MentorshipRequestResult.failure("Access denied: You cannot view mentorship connections for another student.");
        }

        // 5. Query repository
        try {
            List<MentorshipRequest> connections = requestRepository.findAcceptedConnectionsByStudentId(studentId);
            return MentorshipRequestResult.success(connections, "Retrieved " + connections.size() + " active mentorship connection(s).");
        } catch (SQLException e) {
            return MentorshipRequestResult.failure("Database error while retrieving mentorship connections: " + e.getMessage());
        }
    }

    /**
     * Retrieves accepted mentorship connections for the specified alumnus.
     *
     * <p>Security invariants:
     * <ul>
     *   <li>User must be authenticated.</li>
     *   <li>User must have the {@link Role#ALUMNI} role.</li>
     *   <li>User can only view their own connections (alumniId must match session user ID).</li>
     * </ul>
     *
     * @param alumniId target alumni user ID
     * @return {@link MentorshipRequestResult} carrying the list of accepted connections on success, or an error message on failure
     */
    public MentorshipRequestResult getConnectionsForAlumni(int alumniId) {
        // 1. Authentication check
        Optional<User> sessionUser = UserSession.getInstance().getCurrentUser();
        if (sessionUser.isEmpty()) {
            return MentorshipRequestResult.failure("Access denied: You must be logged in to view mentorship connections.");
        }

        // 2. Role check
        User user = sessionUser.get();
        if (user.getRole() != Role.ALUMNI) {
            return MentorshipRequestResult.failure("Access denied: Only alumni can view alumni mentorship connections.");
        }

        // 3. ID validation
        if (alumniId <= 0) {
            return MentorshipRequestResult.failure("Invalid alumni ID: ID must be a positive integer.");
        }

        // 4. Ownership check: alumnus can only view their own connections
        if (user.getId() == null || user.getId() != alumniId) {
            return MentorshipRequestResult.failure("Access denied: You cannot view mentorship connections for another alumnus.");
        }

        // 5. Query repository
        try {
            List<MentorshipRequest> connections = requestRepository.findAcceptedConnectionsByAlumniId(alumniId);
            return MentorshipRequestResult.success(connections, "Retrieved " + connections.size() + " active mentorship connection(s).");
        } catch (SQLException e) {
            return MentorshipRequestResult.failure("Database error while retrieving mentorship connections: " + e.getMessage());
        }
    }

    /**
     * Convenience method to retrieve accepted mentorship connections for the currently logged-in user (Student or Alumni).
     *
     * @return result containing list of accepted connections on success, or error message on failure
     */
    public MentorshipRequestResult getMyMentorshipConnections() {
        Optional<User> sessionUser = UserSession.getInstance().getCurrentUser();
        if (sessionUser.isEmpty()) {
            return MentorshipRequestResult.failure("Access denied: You must be logged in to view your mentorship connections.");
        }

        User user = sessionUser.get();
        if (user.getId() == null || user.getId() <= 0) {
            return MentorshipRequestResult.failure("Invalid user ID in session.");
        }

        if (user.getRole() == Role.STUDENT) {
            return getConnectionsForStudent(user.getId());
        } else if (user.getRole() == Role.ALUMNI) {
            return getConnectionsForAlumni(user.getId());
        } else {
            return MentorshipRequestResult.failure("Access denied: Unauthorized role for mentorship connections.");
        }
    }

    /**
     * Direct list-returning method for student mentorship connections.
     *
     * @param studentId target student user ID
     * @return list of MentorshipRequest objects
     * @throws SecurityException if unauthorized
     * @throws IllegalArgumentException if ID is invalid
     */
    public List<MentorshipRequest> getStudentConnections(int studentId) {
        MentorshipRequestResult result = getConnectionsForStudent(studentId);
        if (!result.isSuccess()) {
            if (result.getMessage().startsWith("Access denied")) {
                throw new SecurityException(result.getMessage());
            }
            throw new IllegalArgumentException(result.getMessage());
        }
        return result.getRequests();
    }

    /**
     * Direct list-returning method for alumni mentorship connections.
     *
     * @param alumniId target alumni user ID
     * @return list of MentorshipRequest objects
     * @throws SecurityException if unauthorized
     * @throws IllegalArgumentException if ID is invalid
     */
    public List<MentorshipRequest> getAlumniConnections(int alumniId) {
        MentorshipRequestResult result = getConnectionsForAlumni(alumniId);
        if (!result.isSuccess()) {
            if (result.getMessage().startsWith("Access denied")) {
                throw new SecurityException(result.getMessage());
            }
            throw new IllegalArgumentException(result.getMessage());
        }
        return result.getRequests();
    }

    /**
     * Accepts a pending mentorship request.
     *
     * <p>Security invariants:
     * <ul>
     *   <li>Must be an authenticated alumnus.</li>
     *   <li>Request ID must be positive and exist.</li>
     *   <li>Request must belong to the authenticated alumnus.</li>
     *   <li>Request must currently be in {@link MentorshipStatus#PENDING} status.</li>
     * </ul>
     *
     * @param requestId ID of the mentorship request to accept
     * @return result containing success/failure status and the request ID
     */
    public MentorshipRequestResult acceptRequest(int requestId) {
        return respondToRequest(requestId, MentorshipStatus.ACCEPTED);
    }

    /**
     * Rejects a pending mentorship request.
     *
     * <p>Security invariants:
     * <ul>
     *   <li>Must be an authenticated alumnus.</li>
     *   <li>Request ID must be positive and exist.</li>
     *   <li>Request must belong to the authenticated alumnus.</li>
     *   <li>Request must currently be in {@link MentorshipStatus#PENDING} status.</li>
     * </ul>
     *
     * @param requestId ID of the mentorship request to reject
     * @return result containing success/failure status and the request ID
     */
    public MentorshipRequestResult rejectRequest(int requestId) {
        return respondToRequest(requestId, MentorshipStatus.REJECTED);
    }

    /**
     * Accepts a pending mentorship request verifying the given alumniId.
     */
    public MentorshipRequestResult acceptRequest(int requestId, int alumniId) {
        Optional<User> sessionUser = UserSession.getInstance().getCurrentUser();
        if (sessionUser.isPresent() && (sessionUser.get().getId() == null || sessionUser.get().getId() != alumniId)) {
            return MentorshipRequestResult.failure("Access denied: You can only respond to mentorship requests sent to you.");
        }
        return acceptRequest(requestId);
    }

    /**
     * Rejects a pending mentorship request verifying the given alumniId.
     */
    public MentorshipRequestResult rejectRequest(int requestId, int alumniId) {
        Optional<User> sessionUser = UserSession.getInstance().getCurrentUser();
        if (sessionUser.isPresent() && (sessionUser.get().getId() == null || sessionUser.get().getId() != alumniId)) {
            return MentorshipRequestResult.failure("Access denied: You can only respond to mentorship requests sent to you.");
        }
        return rejectRequest(requestId);
    }

    private MentorshipRequestResult respondToRequest(int requestId, MentorshipStatus newStatus) {
        // 1. Authentication check
        Optional<User> sessionUser = UserSession.getInstance().getCurrentUser();
        if (sessionUser.isEmpty()) {
            return MentorshipRequestResult.failure("Access denied: You must be logged in to respond to mentorship requests.");
        }

        // 2. Role check
        User user = sessionUser.get();
        if (user.getRole() != Role.ALUMNI) {
            return MentorshipRequestResult.failure("Access denied: Only alumni can respond to mentorship requests.");
        }

        // 3. ID validation
        if (requestId <= 0) {
            return MentorshipRequestResult.failure("Invalid request ID: Request ID must be a positive integer.");
        }

        // 4. Status validation argument check
        if (newStatus != MentorshipStatus.ACCEPTED && newStatus != MentorshipStatus.REJECTED) {
            return MentorshipRequestResult.failure("Invalid status transition: Can only transition to ACCEPTED or REJECTED.");
        }

        try {
            // 5. Look up existing request
            Optional<MentorshipRequest> reqOpt = requestRepository.findRequestById(requestId);
            if (reqOpt.isEmpty()) {
                return MentorshipRequestResult.failure("Mentorship request not found with ID: " + requestId);
            }

            MentorshipRequest request = reqOpt.get();

            // 6. Ownership check: alumni_id must match authenticated alumnus
            if (user.getId() == null || request.getAlumniId() != user.getId()) {
                return MentorshipRequestResult.failure("Access denied: You can only respond to mentorship requests sent to you.");
            }

            // 7. Non-pending check: cannot respond to requests that are not PENDING
            if (request.getStatus() != MentorshipStatus.PENDING) {
                return MentorshipRequestResult.failure(
                        "Cannot respond to request: Request is not pending (current status: " + request.getStatus() + ").");
            }

            // 8. Update status in database
            LocalDateTime respondedAt = LocalDateTime.now();
            boolean updated = requestRepository.updateRequestStatus(requestId, newStatus, respondedAt);
            if (!updated) {
                return MentorshipRequestResult.failure("Failed to update mentorship request status. Please try again.");
            }

            // 9. Update model instance
            request.setStatus(newStatus);
            request.setRespondedAt(respondedAt);

            String actionWord = newStatus == MentorshipStatus.ACCEPTED ? "accepted" : "rejected";
            return MentorshipRequestResult.success(requestId, "Mentorship request " + actionWord + " successfully.");

        } catch (SQLException e) {
            return MentorshipRequestResult.failure("Database error while responding to mentorship request: " + e.getMessage());
        }
    }
}
