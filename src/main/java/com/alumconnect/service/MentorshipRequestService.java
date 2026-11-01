package com.alumconnect.service;

import com.alumconnect.model.Alumni;
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
import java.util.Objects;
import java.util.Optional;

/**
 * Service handling mentorship request operations.
 *
 * Only authenticated students may send mentorship requests. The service validates the target alumni,
 * duplicate requests, mentorship availability, and active mentee capacity before creating a new PENDING request.
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
                return MentorshipRequestResult.failure("Could not create mentorship request. Please try again.");
            }
            return MentorshipRequestResult.success(requestId, "Mentorship request sent successfully.");
        } catch (SQLException e) {
            return MentorshipRequestResult.failure("Database error while sending mentorship request: " + e.getMessage());
        }
    }
}
