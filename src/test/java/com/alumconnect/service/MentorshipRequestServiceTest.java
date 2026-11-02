package com.alumconnect.service;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.MentorshipRequest;
import com.alumconnect.model.MentorshipStatus;
import com.alumconnect.model.Role;
import com.alumconnect.model.Student;
import com.alumconnect.model.User;
import com.alumconnect.repository.MentorshipRequestRepository;
import com.alumconnect.repository.UserRepository;
import com.alumconnect.service.dto.MentorshipRequestDto;
import com.alumconnect.service.dto.MentorshipRequestResult;
import com.alumconnect.util.UserSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Mentorship Request Service Tests (Prompt 16)")
class MentorshipRequestServiceTest {

    private InMemoryMentorshipRequestRepository fakeRequestRepository;
    private InMemoryUserRepository fakeUserRepository;
    private MentorshipRequestService requestService;

    private Student loggedInStudent;
    private Alumni targetAlumni;

    @BeforeEach
    void setUp() {
        UserSession.getInstance().logout();

        fakeRequestRepository = new InMemoryMentorshipRequestRepository();
        fakeUserRepository = new InMemoryUserRepository();
        requestService = new MentorshipRequestService(fakeRequestRepository, fakeUserRepository);

        // Set up student
        loggedInStudent = new Student();
        loggedInStudent.setId(101);
        loggedInStudent.setEmail("student@thapar.edu");
        loggedInStudent.setFullName("Aman Verma");
        loggedInStudent.setRole(Role.STUDENT);
        fakeUserRepository.saveUser(loggedInStudent);

        // Set up alumni
        targetAlumni = new Alumni();
        targetAlumni.setId(202);
        targetAlumni.setEmail("alumni@google.com");
        targetAlumni.setFullName("Neha Gupta");
        targetAlumni.setRole(Role.ALUMNI);
        targetAlumni.setAvailableForMentorship(true);
        targetAlumni.setMaxMentees(3);
        fakeUserRepository.saveUser(targetAlumni);

        // Configure repository state
        fakeRequestRepository.setAlumniAvailable(202, true);
        fakeRequestRepository.setActiveMenteeCount(202, 1);
    }

    @AfterEach
    void tearDown() {
        UserSession.getInstance().logout();
    }

    @Test
    @DisplayName("1. Successful mentorship request creation")
    void testSuccessfulMentorshipRequest() {
        UserSession.getInstance().login(loggedInStudent);

        MentorshipRequestResult result = requestService.sendRequest(
                202,
                "Backend Architecture Guidance",
                "I would love guidance on designing scalable microservices."
        );

        assertTrue(result.isSuccess(), "Request should succeed: " + result.getMessage());
        assertNotNull(result.getRequestId(), "Request ID should be returned");
        assertEquals(1, result.getRequestId());
        assertTrue(result.getMessage().contains("Mentorship request sent successfully"));

        // Verify DTO recorded
        MentorshipRequestDto lastDto = fakeRequestRepository.getLastCreatedDto();
        assertNotNull(lastDto);
        assertEquals(101, lastDto.getStudentId());
        assertEquals(202, lastDto.getAlumniId());
        assertEquals("Backend Architecture Guidance", lastDto.getTopic());
        assertEquals("I would love guidance on designing scalable microservices.", lastDto.getMessage());
    }

    @Test
    @DisplayName("2. Unauthenticated user cannot send request")
    void testUnauthenticatedUserCannotSendRequest() {
        // No session login
        MentorshipRequestResult result = requestService.sendRequest(
                202,
                "Topic",
                "Message"
        );

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("must be logged in"));
    }

    @Test
    @DisplayName("3. Non-STUDENT user cannot send request")
    void testNonStudentCannotSendRequest() {
        UserSession.getInstance().login(targetAlumni); // Alumni session

        MentorshipRequestResult result = requestService.sendRequest(
                202,
                "Topic",
                "Message"
        );

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Only students can send mentorship requests"));
    }

    @Test
    @DisplayName("4. Target user does not exist")
    void testTargetUserDoesNotExist() {
        UserSession.getInstance().login(loggedInStudent);

        MentorshipRequestResult result = requestService.sendRequest(
                9999, // Non-existent user ID
                "Topic",
                "Message"
        );

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Target user does not exist or is not an alumni"));
    }

    @Test
    @DisplayName("5. Target user exists but is not ALUMNI")
    void testTargetUserExistsButNotAlumni() {
        UserSession.getInstance().login(loggedInStudent);

        Student otherStudent = new Student();
        otherStudent.setId(102);
        otherStudent.setEmail("other@thapar.edu");
        otherStudent.setFullName("Other Student");
        otherStudent.setRole(Role.STUDENT);
        fakeUserRepository.saveUser(otherStudent);

        MentorshipRequestResult result = requestService.sendRequest(
                102,
                "Topic",
                "Message"
        );

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Target user does not exist or is not an alumni"));
    }

    @Test
    @DisplayName("6. Student cannot send request to themselves")
    void testStudentCannotSendRequestToSelf() {
        UserSession.getInstance().login(loggedInStudent);

        MentorshipRequestResult result = requestService.sendRequest(
                101, // same ID as logged-in student
                "Topic",
                "Message"
        );

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("cannot send a mentorship request to yourself"));
    }

    @Test
    @DisplayName("7. Duplicate PENDING request is rejected")
    void testDuplicatePendingRequestRejected() {
        UserSession.getInstance().login(loggedInStudent);
        fakeRequestRepository.setPendingExists(101, 202, true);

        MentorshipRequestResult result = requestService.sendRequest(
                202,
                "Topic",
                "Message"
        );

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("pending mentorship request already exists"));
    }

    @Test
    @DisplayName("8. Duplicate ACCEPTED request is rejected")
    void testDuplicateAcceptedRequestRejected() {
        UserSession.getInstance().login(loggedInStudent);
        fakeRequestRepository.setAcceptedExists(101, 202, true);

        MentorshipRequestResult result = requestService.sendRequest(
                202,
                "Topic",
                "Message"
        );

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("already have an accepted mentorship request"));
    }

    @Test
    @DisplayName("9. Alumni unavailable for mentorship is rejected")
    void testAlumniUnavailableRejected() {
        UserSession.getInstance().login(loggedInStudent);
        fakeRequestRepository.setAlumniAvailable(202, false);

        MentorshipRequestResult result = requestService.sendRequest(
                202,
                "Topic",
                "Message"
        );

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("not available for mentorship"));
    }

    @Test
    @DisplayName("10. Alumni at max_mentees capacity is rejected")
    void testAlumniAtMaxMenteesCapacityRejected() {
        UserSession.getInstance().login(loggedInStudent);
        // targetAlumni maxMentees = 3
        fakeRequestRepository.setActiveMenteeCount(202, 3); // capacity reached

        MentorshipRequestResult result = requestService.sendRequest(
                202,
                "Topic",
                "Message"
        );

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("maximum number of mentees"));
    }

    @Test
    @DisplayName("11. Request model defaults to PENDING status")
    void testRequestModelDefaultsToPendingStatus() {
        MentorshipRequest request = new MentorshipRequest(101, 202, "Topic", "Message");
        assertEquals(MentorshipStatus.PENDING, request.getStatus());
        assertNotNull(request.getRequestedAt());
    }

    @Test
    @DisplayName("12. Database/repository SQLException is handled gracefully")
    void testDatabaseSQLExceptionHandled() {
        UserSession.getInstance().login(loggedInStudent);
        fakeRequestRepository.setThrowOnCreate(true);

        MentorshipRequestResult result = requestService.sendRequest(
                202,
                "Topic",
                "Message"
        );

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Database error while sending mentorship request"));
    }

    @Test
    @DisplayName("13. Input validation: topic and message checks")
    void testInputValidation() {
        UserSession.getInstance().login(loggedInStudent);

        // Invalid alumni ID
        MentorshipRequestResult res1 = requestService.sendRequest(0, "Topic", "Message");
        assertFalse(res1.isSuccess());
        assertTrue(res1.getMessage().contains("Invalid alumni"));

        // Null topic
        MentorshipRequestResult res2 = requestService.sendRequest(202, null, "Message");
        assertFalse(res2.isSuccess());
        assertTrue(res2.getMessage().contains("topic is required"));

        // Blank topic
        MentorshipRequestResult res3 = requestService.sendRequest(202, "   ", "Message");
        assertFalse(res3.isSuccess());
        assertTrue(res3.getMessage().contains("topic is required"));

        // Null message
        MentorshipRequestResult res4 = requestService.sendRequest(202, "Topic", null);
        assertFalse(res4.isSuccess());
        assertTrue(res4.getMessage().contains("message is required"));

        // Blank message
        MentorshipRequestResult res5 = requestService.sendRequest(202, "Topic", "   ");
        assertFalse(res5.isSuccess());
        assertTrue(res5.getMessage().contains("message is required"));

        // Topic exceeds 255 chars
        String longTopic = "A".repeat(256);
        MentorshipRequestResult res6 = requestService.sendRequest(202, longTopic, "Message");
        assertFalse(res6.isSuccess());
        assertTrue(res6.getMessage().contains("cannot exceed 255 characters"));

        // Message exceeds 2000 chars
        String longMessage = "A".repeat(2001);
        MentorshipRequestResult res7 = requestService.sendRequest(202, "Topic", longMessage);
        assertFalse(res7.isSuccess());
        assertTrue(res7.getMessage().contains("cannot exceed 2000 characters"));
    }

    // ── In-Memory MentorshipRequestRepository Fake ──────────────────────────

    private static class InMemoryMentorshipRequestRepository implements MentorshipRequestRepository {
        private int sequence = 1;
        private MentorshipRequestDto lastCreatedDto;
        private final Map<String, Boolean> pendingRequests = new HashMap<>();
        private final Map<String, Boolean> acceptedRequests = new HashMap<>();
        private final Map<Integer, Integer> activeMenteesCount = new HashMap<>();
        private final Map<Integer, Boolean> alumniAvailability = new HashMap<>();
        private boolean throwOnCreate = false;

        void setThrowOnCreate(boolean val) { this.throwOnCreate = val; }

        void setPendingExists(int studentId, int alumniId, boolean exists) {
            pendingRequests.put(studentId + "_" + alumniId, exists);
        }

        void setAcceptedExists(int studentId, int alumniId, boolean exists) {
            acceptedRequests.put(studentId + "_" + alumniId, exists);
        }

        void setActiveMenteeCount(int alumniId, int count) {
            activeMenteesCount.put(alumniId, count);
        }

        void setAlumniAvailable(int alumniId, boolean available) {
            alumniAvailability.put(alumniId, available);
        }

        MentorshipRequestDto getLastCreatedDto() {
            return lastCreatedDto;
        }

        @Override
        public int createRequest(MentorshipRequestDto dto) throws SQLException {
            if (throwOnCreate) {
                throw new SQLException("Simulated DB INSERT failure");
            }
            this.lastCreatedDto = dto;
            return sequence++;
        }

        @Override
        public boolean existsPending(int studentId, int alumniId) throws SQLException {
            return pendingRequests.getOrDefault(studentId + "_" + alumniId, false);
        }

        @Override
        public boolean existsAccepted(int studentId, int alumniId) throws SQLException {
            return acceptedRequests.getOrDefault(studentId + "_" + alumniId, false);
        }

        @Override
        public int countActiveMentees(int alumniId) throws SQLException {
            return activeMenteesCount.getOrDefault(alumniId, 0);
        }

        @Override
        public boolean isAlumniAvailable(int alumniId) throws SQLException {
            return alumniAvailability.getOrDefault(alumniId, false);
        }
    }

    // ── In-Memory UserRepository Fake ───────────────────────────────────────

    private static class InMemoryUserRepository implements UserRepository {
        private final Map<Integer, User> users = new HashMap<>();

        void saveUser(User user) {
            users.put(user.getId(), user);
        }

        @Override
        public boolean existsByEmail(String email) {
            return users.values().stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(email));
        }

        @Override
        public Optional<User> findById(int id) {
            return Optional.ofNullable(users.get(id));
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return users.values().stream()
                    .filter(u -> u.getEmail().equalsIgnoreCase(email))
                    .findFirst();
        }

        @Override
        public Optional<Student> findStudentById(int userId) {
            User u = users.get(userId);
            return (u instanceof Student s) ? Optional.of(s) : Optional.empty();
        }

        @Override
        public Optional<Alumni> findAlumniById(int userId) {
            User u = users.get(userId);
            return (u instanceof Alumni a) ? Optional.of(a) : Optional.empty();
        }

        @Override
        public Student saveStudent(Student student) {
            users.put(student.getId(), student);
            return student;
        }

        @Override
        public Alumni saveAlumni(Alumni alumni) {
            users.put(alumni.getId(), alumni);
            return alumni;
        }

        @Override
        public boolean deleteUserById(int userId) {
            return users.remove(userId) != null;
        }
    }
}

