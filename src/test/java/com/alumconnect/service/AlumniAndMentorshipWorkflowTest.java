package com.alumconnect.service;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.MentorshipRequest;
import com.alumconnect.model.MentorshipStatus;
import com.alumconnect.model.Role;
import com.alumconnect.model.Student;
import com.alumconnect.model.User;
import com.alumconnect.repository.AlumniProfileRepository;
import com.alumconnect.repository.MentorshipRequestRepository;
import com.alumconnect.repository.UserRepository;
import com.alumconnect.service.dto.AlumniProfileRequest;
import com.alumconnect.service.dto.AlumniProfileResult;
import com.alumconnect.service.dto.MentorshipRequestDto;
import com.alumconnect.service.dto.MentorshipRequestResult;
import com.alumconnect.util.PasswordUtil;
import com.alumconnect.util.UserSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive alumni and mentorship request lifecycle unit tests for Prompt 23C.
 *
 * <p>Covers:
 * <ul>
 *   <li>Creating and updating an alumni's own profile</li>
 *   <li>Viewing incoming mentorship requests and request details</li>
 *   <li>Accepting and rejecting pending mentorship requests</li>
 *   <li>Viewing accepted mentorship connections for both users</li>
 *   <li>Complete end-to-end 5-step mentorship request lifecycle</li>
 *   <li>Verification that pending and rejected requests are excluded from connections</li>
 *   <li>Duplicate request prevention</li>
 *   <li>Invalid status transition rejection</li>
 *   <li>Ownership enforcement: alumni can only manage requests addressed to their own account</li>
 * </ul>
 */
@DisplayName("Alumni & Mentorship Workflow Tests (Prompt 23C)")
class AlumniAndMentorshipWorkflowTest {

    private InMemoryUserRepository userRepository;
    private InMemoryAlumniProfileRepository alumniProfileRepository;
    private InMemoryMentorshipRequestRepository requestRepository;

    private AlumniProfileService alumniProfileService;
    private MentorshipRequestService mentorshipService;

    private Alumni alumni1;
    private Alumni alumni2;
    private Student student1;
    private Student student2;

    @BeforeEach
    void setUp() throws SQLException {
        UserSession.getInstance().logout();

        userRepository = new InMemoryUserRepository();
        alumniProfileRepository = new InMemoryAlumniProfileRepository();
        requestRepository = new InMemoryMentorshipRequestRepository();

        alumniProfileService = new AlumniProfileService(alumniProfileRepository);
        mentorshipService = new MentorshipRequestService(requestRepository, userRepository);

        // Seed Alumni 1: Google Staff Software Engineer
        alumni1 = new Alumni(
                "alumni1@thapar.edu",
                PasswordUtil.hashPassword("AlumniP@ss1"),
                "Dr. Vikram Malhotra",
                "+91-9811001122",
                "Computer Science",
                2018
        );
        alumni1.setCurrentCompany("Google");
        alumni1.setCurrentRole("Staff Software Engineer");
        alumni1.setIndustry("Technology");
        alumni1.setYearsOfExperience(8);
        alumni1.setDegree("B.Tech Computer Science");
        alumni1.setExpertiseAreas("Distributed Systems, Cloud Architecture, Java");
        alumni1.setBio("Passionate mentor with extensive cloud engineering experience.");
        alumni1.setLinkedinUrl("https://linkedin.com/in/vikrammalhotra");
        alumni1.setAvailableForMentorship(true);
        alumni1.setMaxMentees(3);
        userRepository.saveAlumni(alumni1);
        alumniProfileRepository.save(alumni1);

        // Seed Alumni 2: Amazon Engineering Manager
        alumni2 = new Alumni(
                "alumni2@thapar.edu",
                PasswordUtil.hashPassword("AlumniP@ss2"),
                "Ananya Sharma",
                "+91-9811003344",
                "Electronics",
                2016
        );
        alumni2.setCurrentCompany("Amazon");
        alumni2.setCurrentRole("Software Development Manager");
        alumni2.setIndustry("E-Commerce");
        alumni2.setYearsOfExperience(10);
        alumni2.setDegree("B.Tech Electronics");
        alumni2.setExpertiseAreas("Leadership, System Design, AWS");
        alumni2.setAvailableForMentorship(true);
        alumni2.setMaxMentees(2);
        userRepository.saveAlumni(alumni2);
        alumniProfileRepository.save(alumni2);

        // Seed Student 1
        student1 = new Student(
                "student1@thapar.edu",
                PasswordUtil.hashPassword("StudentP@ss1"),
                "Aarav Mehta",
                "+91-9876543210",
                "Computer Science",
                2026
        );
        student1.setCurrentSemester(6);
        student1.setEnrollmentNumber("102203111");
        userRepository.saveStudent(student1);

        // Seed Student 2
        student2 = new Student(
                "student2@thapar.edu",
                PasswordUtil.hashPassword("StudentP@ss2"),
                "Diya Sengupta",
                "+91-9876543211",
                "Electronics",
                2027
        );
        student2.setCurrentSemester(4);
        student2.setEnrollmentNumber("102203222");
        userRepository.saveStudent(student2);
    }

    @AfterEach
    void tearDown() {
        UserSession.getInstance().logout();
    }

    // =========================================================================
    // 1. Alumni Profile Workflows
    // =========================================================================

    @Nested
    @DisplayName("Alumni Profile Creation & Updating Workflows")
    class AlumniProfileWorkflows {

        @Test
        @DisplayName("Alumni can view their own profile successfully")
        void testAlumniCanViewOwnProfile() {
            UserSession.getInstance().login(alumni1);

            AlumniProfileResult result = alumniProfileService.viewMyProfile();

            assertTrue(result.isSuccess(), "Profile viewing should succeed");
            assertNotNull(result.getAlumni());
            assertEquals("Dr. Vikram Malhotra", result.getAlumni().getFullName());
            assertEquals("Google", result.getAlumni().getCurrentCompany());
            assertEquals("Staff Software Engineer", result.getAlumni().getCurrentRole());
            assertEquals(8, result.getAlumni().getYearsOfExperience());
        }

        @Test
        @DisplayName("Alumni can update their own profile with valid data")
        void testAlumniCanUpdateOwnProfile() {
            UserSession.getInstance().login(alumni1);

            AlumniProfileRequest req = new AlumniProfileRequest();
            req.setDepartment("Computer Science");
            req.setGraduationYear(2018);
            req.setCurrentCompany("Google Cloud");
            req.setCurrentRole("Principal Architect");
            req.setIndustry("Cloud Computing");
            req.setYearsOfExperience(10);
            req.setExpertiseAreas("Kubernetes, Distributed Databases, Go, Java");
            req.setBio("Updated bio: 10+ years designing enterprise cloud infrastructure.");
            req.setLinkedinUrl("https://linkedin.com/in/vikram-updated");
            req.setAvailableForMentorship(true);
            req.setMaxMentees(4);

            AlumniProfileResult result = alumniProfileService.updateMyProfile(req);

            assertTrue(result.isSuccess(), "Profile update should succeed: " + result.getMessage());
            assertNotNull(result.getAlumni());
            assertEquals("Google Cloud", result.getAlumni().getCurrentCompany());
            assertEquals("Principal Architect", result.getAlumni().getCurrentRole());
            assertEquals(10, result.getAlumni().getYearsOfExperience());
            assertEquals(4, result.getAlumni().getMaxMentees());
        }

        @Test
        @DisplayName("Alumni cannot update another alumni's profile (Ownership enforcement)")
        void testAlumniCannotUpdateAnotherAlumniProfile() {
            UserSession.getInstance().login(alumni1); // Logged in as alumni1

            AlumniProfileRequest req = new AlumniProfileRequest();
            req.setDepartment("Electronics");
            req.setGraduationYear(2016);
            req.setCurrentCompany("Compromised Corp");

            // Attempting to update alumni2's profile
            AlumniProfileResult result = alumniProfileService.updateProfile(alumni2.getId(), req);

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().toLowerCase().contains("cannot modify another alumni's profile"));
        }

        @Test
        @DisplayName("Alumni profile update fails when experience is negative")
        void testUpdateProfileNegativeExperienceFails() {
            UserSession.getInstance().login(alumni1);

            AlumniProfileRequest req = new AlumniProfileRequest();
            req.setDepartment("Computer Science");
            req.setGraduationYear(2018);
            req.setYearsOfExperience(-2); // Invalid: negative experience

            AlumniProfileResult result = alumniProfileService.updateMyProfile(req);

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().toLowerCase().contains("cannot be negative"));
        }

        @Test
        @DisplayName("Alumni profile update fails when max mentees is negative")
        void testUpdateProfileNegativeMaxMenteesFails() {
            UserSession.getInstance().login(alumni1);

            AlumniProfileRequest req = new AlumniProfileRequest();
            req.setDepartment("Computer Science");
            req.setGraduationYear(2018);
            req.setMaxMentees(-1); // Invalid: negative mentees

            AlumniProfileResult result = alumniProfileService.updateMyProfile(req);

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().toLowerCase().contains("cannot be negative"));
        }

        @Test
        @DisplayName("Alumni cannot create profile if one already exists")
        void testCreateProfileFailsIfAlreadyExists() {
            UserSession.getInstance().login(alumni1);

            AlumniProfileRequest req = new AlumniProfileRequest();
            req.setDepartment("Computer Science");
            req.setGraduationYear(2018);

            AlumniProfileResult result = alumniProfileService.createMyProfile(req);

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().toLowerCase().contains("already exists"));
        }

        @Test
        @DisplayName("Unauthenticated access to alumni profile operations is rejected")
        void testUnauthenticatedProfileAccessRejected() {
            AlumniProfileResult viewResult = alumniProfileService.viewMyProfile();
            assertFalse(viewResult.isSuccess());
            assertTrue(viewResult.getMessage().toLowerCase().contains("must be logged in"));

            AlumniProfileResult updateResult = alumniProfileService.updateMyProfile(new AlumniProfileRequest());
            assertFalse(updateResult.isSuccess());
            assertTrue(updateResult.getMessage().toLowerCase().contains("must be logged in"));
        }

        @Test
        @DisplayName("Student role cannot access alumni profile operations")
        void testStudentRoleCannotAccessAlumniProfile() {
            UserSession.getInstance().login(student1);

            AlumniProfileResult result = alumniProfileService.viewMyProfile();

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().toLowerCase().contains("only alumni"));
        }
    }

    // =========================================================================
    // 2. Incoming Mentorship Requests Management
    // =========================================================================

    @Nested
    @DisplayName("Incoming Mentorship Request Management")
    class IncomingRequestsManagement {

        @Test
        @DisplayName("Alumni views incoming mentorship requests with student details")
        void testAlumniViewsIncomingRequests() {
            // Seed a request from student1 to alumni1
            MentorshipRequest req = new MentorshipRequest();
            req.setId(1);
            req.setStudentId(student1.getId());
            req.setAlumniId(alumni1.getId());
            req.setTopic("Cloud Architecture Guidance");
            req.setMessage("I would like advice on distributed system design.");
            req.setStatus(MentorshipStatus.PENDING);
            req.setStudentName("Aarav Mehta");
            req.setStudentEmail("student1@thapar.edu");
            req.setStudentDepartment("Computer Science");
            req.setStudentGraduationYear(2026);
            requestRepository.save(req);

            UserSession.getInstance().login(alumni1);

            MentorshipRequestResult result = mentorshipService.getReceivedRequests();

            assertTrue(result.isSuccess());
            assertNotNull(result.getRequests());
            assertEquals(1, result.getRequests().size());
            MentorshipRequest retrieved = result.getRequests().get(0);
            assertEquals("Cloud Architecture Guidance", retrieved.getTopic());
            assertEquals("Aarav Mehta", retrieved.getStudentName());
            assertEquals(MentorshipStatus.PENDING, retrieved.getStatus());
        }

        @Test
        @DisplayName("Alumni with no incoming requests receives empty list gracefully")
        void testAlumniWithNoRequestsReceivesEmptyList() {
            UserSession.getInstance().login(alumni2); // No requests sent to alumni2

            MentorshipRequestResult result = mentorshipService.getReceivedRequests();

            assertTrue(result.isSuccess());
            assertNotNull(result.getRequests());
            assertTrue(result.getRequests().isEmpty());
        }

        @Test
        @DisplayName("Alumni can accept a pending mentorship request")
        void testAlumniCanAcceptPendingRequest() {
            MentorshipRequest req = new MentorshipRequest();
            req.setId(10);
            req.setStudentId(student1.getId());
            req.setAlumniId(alumni1.getId());
            req.setTopic("Microservices Deep Dive");
            req.setMessage("Looking for mentorship.");
            req.setStatus(MentorshipStatus.PENDING);
            requestRepository.save(req);

            UserSession.getInstance().login(alumni1);

            MentorshipRequestResult result = mentorshipService.acceptRequest(10);

            assertTrue(result.isSuccess(), "Accepting request should succeed: " + result.getMessage());
            assertEquals(10, result.getRequestId());

            // Verify in repository
            Optional<MentorshipRequest> updated = requestRepository.findRequestById(10);
            assertTrue(updated.isPresent());
            assertEquals(MentorshipStatus.ACCEPTED, updated.get().getStatus());
            assertNotNull(updated.get().getRespondedAt());
        }

        @Test
        @DisplayName("Alumni can reject a pending mentorship request")
        void testAlumniCanRejectPendingRequest() {
            MentorshipRequest req = new MentorshipRequest();
            req.setId(11);
            req.setStudentId(student2.getId());
            req.setAlumniId(alumni1.getId());
            req.setTopic("Hardware Design");
            req.setMessage("Advice needed.");
            req.setStatus(MentorshipStatus.PENDING);
            requestRepository.save(req);

            UserSession.getInstance().login(alumni1);

            MentorshipRequestResult result = mentorshipService.rejectRequest(11);

            assertTrue(result.isSuccess(), "Rejecting request should succeed: " + result.getMessage());
            assertEquals(11, result.getRequestId());

            // Verify in repository
            Optional<MentorshipRequest> updated = requestRepository.findRequestById(11);
            assertTrue(updated.isPresent());
            assertEquals(MentorshipStatus.REJECTED, updated.get().getStatus());
            assertNotNull(updated.get().getRespondedAt());
        }

        @Test
        @DisplayName("Alumni cannot accept or reject a request addressed to another alumnus (Ownership enforcement)")
        void testAlumniCannotRespondToAnotherAlumniRequest() {
            // Seed a request addressed to alumni1
            MentorshipRequest req = new MentorshipRequest();
            req.setId(12);
            req.setStudentId(student1.getId());
            req.setAlumniId(alumni1.getId()); // Belongs to alumni1
            req.setTopic("Algorithms");
            req.setMessage("Help with graphs.");
            req.setStatus(MentorshipStatus.PENDING);
            requestRepository.save(req);

            // Log in as alumni2
            UserSession.getInstance().login(alumni2);

            // Attempting to accept alumni1's request
            MentorshipRequestResult acceptResult = mentorshipService.acceptRequest(12);
            assertFalse(acceptResult.isSuccess());
            assertTrue(acceptResult.getMessage().toLowerCase().contains("requests sent to you"));

            // Attempting to reject alumni1's request
            MentorshipRequestResult rejectResult = mentorshipService.rejectRequest(12);
            assertFalse(rejectResult.isSuccess());
            assertTrue(rejectResult.getMessage().toLowerCase().contains("requests sent to you"));
        }

        @Test
        @DisplayName("Student cannot accept or reject mentorship requests")
        void testStudentCannotAcceptOrRejectRequests() {
            MentorshipRequest req = new MentorshipRequest();
            req.setId(13);
            req.setStudentId(student1.getId());
            req.setAlumniId(alumni1.getId());
            req.setStatus(MentorshipStatus.PENDING);
            requestRepository.save(req);

            UserSession.getInstance().login(student1);

            MentorshipRequestResult acceptResult = mentorshipService.acceptRequest(13);
            assertFalse(acceptResult.isSuccess());
            assertTrue(acceptResult.getMessage().toLowerCase().contains("only alumni"));

            MentorshipRequestResult rejectResult = mentorshipService.rejectRequest(13);
            assertFalse(rejectResult.isSuccess());
            assertTrue(rejectResult.getMessage().toLowerCase().contains("only alumni"));
        }

        @Test
        @DisplayName("Alumni cannot view incoming requests of another alumnus")
        void testAlumniCannotViewAnotherAlumniRequests() {
            UserSession.getInstance().login(alumni1);

            // Alumni 1 attempts to query Alumni 2's received requests
            MentorshipRequestResult result = mentorshipService.getRequestsForAlumni(alumni2.getId());

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().toLowerCase().contains("cannot view mentorship requests for another alumnus"));
        }
    }

    // =========================================================================
    // 3. Mentorship Connections
    // =========================================================================

    @Nested
    @DisplayName("Mentorship Connections")
    class MentorshipConnectionsTests {

        @Test
        @DisplayName("Alumni views accepted mentorship connections")
        void testAlumniViewsAcceptedConnections() {
            // Seed ACCEPTED connection
            MentorshipRequest accepted = new MentorshipRequest();
            accepted.setId(21);
            accepted.setStudentId(student1.getId());
            accepted.setAlumniId(alumni1.getId());
            accepted.setTopic("Active Mentorship on Distributed Systems");
            accepted.setMessage("Let's connect.");
            accepted.setStatus(MentorshipStatus.ACCEPTED);
            accepted.setStudentName("Aarav Mehta");
            requestRepository.save(accepted);

            // Seed PENDING request (should not appear in connections)
            MentorshipRequest pending = new MentorshipRequest();
            pending.setId(22);
            pending.setStudentId(student2.getId());
            pending.setAlumniId(alumni1.getId());
            pending.setTopic("Pending Topic");
            pending.setMessage("Waiting.");
            pending.setStatus(MentorshipStatus.PENDING);
            requestRepository.save(pending);

            // Seed REJECTED request (should not appear in connections)
            MentorshipRequest rejected = new MentorshipRequest();
            rejected.setId(23);
            rejected.setStudentId(student2.getId());
            rejected.setAlumniId(alumni1.getId());
            rejected.setTopic("Rejected Topic");
            rejected.setMessage("Not available.");
            rejected.setStatus(MentorshipStatus.REJECTED);
            requestRepository.save(rejected);

            UserSession.getInstance().login(alumni1);

            MentorshipRequestResult result = mentorshipService.getMyMentorshipConnections();

            assertTrue(result.isSuccess());
            assertNotNull(result.getRequests());
            assertEquals(1, result.getRequests().size(), "Only ACCEPTED requests must appear as connections");
            assertEquals(21, result.getRequests().get(0).getId());
            assertEquals(MentorshipStatus.ACCEPTED, result.getRequests().get(0).getStatus());
        }

        @Test
        @DisplayName("Alumni cannot view another alumnus's accepted connections")
        void testAlumniCannotViewAnotherAlumniConnections() {
            UserSession.getInstance().login(alumni1);

            // Alumni 1 attempts to query Alumni 2's connections
            MentorshipRequestResult result = mentorshipService.getConnectionsForAlumni(alumni2.getId());

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().toLowerCase().contains("cannot view mentorship connections for another alumnus"));
        }
    }

    // =========================================================================
    // 4. Complete End-to-End Mentorship Request Lifecycle
    // =========================================================================

    @Nested
    @DisplayName("Complete End-to-End Mentorship Request Lifecycle")
    class CompleteLifecycleTests {

        @Test
        @DisplayName("Complete Lifecycle 1: Send -> View -> Accept -> Verify Status & Connections")
        void testCompleteAcceptanceLifecycle() {
            // Step 1: Student 1 sends a mentorship request to Alumni 1
            UserSession.getInstance().login(student1);
            MentorshipRequestResult sendResult = mentorshipService.sendRequest(
                    alumni1.getId(),
                    "Distributed Systems Roadmap",
                    "Hi Vikram, I would love your mentorship on building distributed consensus engines."
            );
            assertTrue(sendResult.isSuccess(), "Step 1: Student sends request");
            int requestId = sendResult.getRequestId();
            assertTrue(requestId > 0);

            // Verify: At this point, neither student nor alumni has an active connection
            MentorshipRequestResult studentPreConn = mentorshipService.getMyMentorshipConnections();
            assertTrue(studentPreConn.getRequests().isEmpty(), "Pending request must NOT appear in student connections");

            // Step 2: Alumni 1 logs in and views incoming request
            UserSession.getInstance().login(alumni1);
            MentorshipRequestResult alumniIncoming = mentorshipService.getReceivedRequests();
            assertTrue(alumniIncoming.isSuccess(), "Step 2: Alumni retrieves incoming requests");
            assertEquals(1, alumniIncoming.getRequests().size());
            MentorshipRequest incomingReq = alumniIncoming.getRequests().get(0);
            assertEquals(requestId, incomingReq.getId());
            assertEquals(MentorshipStatus.PENDING, incomingReq.getStatus());
            assertEquals("Distributed Systems Roadmap", incomingReq.getTopic());

            // Verify: Alumni connections list does not yet have this pending request
            MentorshipRequestResult alumniPreConn = mentorshipService.getMyMentorshipConnections();
            assertTrue(alumniPreConn.getRequests().isEmpty(), "Pending request must NOT appear in alumni connections");

            // Step 3: Alumni 1 accepts the request
            MentorshipRequestResult acceptResult = mentorshipService.acceptRequest(requestId);
            assertTrue(acceptResult.isSuccess(), "Step 3: Alumni accepts request");
            assertEquals(requestId, acceptResult.getRequestId());

            // Step 4: Student 1 logs back in and verifies the request status is now ACCEPTED
            UserSession.getInstance().login(student1);
            MentorshipRequestResult studentRequests = mentorshipService.getMyRequests();
            assertTrue(studentRequests.isSuccess(), "Step 4: Student views updated request status");
            assertEquals(1, studentRequests.getRequests().size());
            MentorshipRequest studentViewReq = studentRequests.getRequests().get(0);
            assertEquals(MentorshipStatus.ACCEPTED, studentViewReq.getStatus());
            assertNotNull(studentViewReq.getRespondedAt());

            // Step 5: Accepted request appears as a connection for BOTH users
            // 5a: Student connection verification
            MentorshipRequestResult studentConnections = mentorshipService.getMyMentorshipConnections();
            assertTrue(studentConnections.isSuccess());
            assertEquals(1, studentConnections.getRequests().size(), "Step 5a: Student has active connection");
            assertEquals(requestId, studentConnections.getRequests().get(0).getId());

            // 5b: Alumni connection verification
            UserSession.getInstance().login(alumni1);
            MentorshipRequestResult alumniConnections = mentorshipService.getMyMentorshipConnections();
            assertTrue(alumniConnections.isSuccess());
            assertEquals(1, alumniConnections.getRequests().size(), "Step 5b: Alumni has active connection");
            assertEquals(requestId, alumniConnections.getRequests().get(0).getId());
        }

        @Test
        @DisplayName("Complete Lifecycle 2: Send -> View -> Reject -> Verify Excluded from Connections")
        void testCompleteRejectionLifecycle() {
            // Step 1: Student 2 sends request to Alumni 2
            UserSession.getInstance().login(student2);
            MentorshipRequestResult sendResult = mentorshipService.sendRequest(
                    alumni2.getId(),
                    "System Design Consultation",
                    "Hi Ananya, advice needed for system design."
            );
            assertTrue(sendResult.isSuccess());
            int requestId = sendResult.getRequestId();

            // Step 2: Alumni 2 logs in and rejects the request
            UserSession.getInstance().login(alumni2);
            MentorshipRequestResult rejectResult = mentorshipService.rejectRequest(requestId);
            assertTrue(rejectResult.isSuccess());

            // Step 3: Student 2 logs in and sees status is REJECTED
            UserSession.getInstance().login(student2);
            MentorshipRequestResult studentRequests = mentorshipService.getMyRequests();
            assertTrue(studentRequests.isSuccess());
            assertEquals(1, studentRequests.getRequests().size());
            assertEquals(MentorshipStatus.REJECTED, studentRequests.getRequests().get(0).getStatus());

            // Step 4: Verify REJECTED request does NOT appear as connection for student
            MentorshipRequestResult studentConn = mentorshipService.getMyMentorshipConnections();
            assertTrue(studentConn.getRequests().isEmpty(), "Rejected request must not appear in student connections");

            // Step 5: Verify REJECTED request does NOT appear as connection for alumni
            UserSession.getInstance().login(alumni2);
            MentorshipRequestResult alumniConn = mentorshipService.getMyMentorshipConnections();
            assertTrue(alumniConn.getRequests().isEmpty(), "Rejected request must not appear in alumni connections");
        }

        @Test
        @DisplayName("Invalid status transitions: Cannot accept or reject non-pending requests")
        void testInvalidStatusTransitions() {
            // Create a request and accept it
            MentorshipRequest req = new MentorshipRequest();
            req.setId(30);
            req.setStudentId(student1.getId());
            req.setAlumniId(alumni1.getId());
            req.setStatus(MentorshipStatus.PENDING);
            requestRepository.save(req);

            UserSession.getInstance().login(alumni1);

            // First accept succeeds
            assertTrue(mentorshipService.acceptRequest(30).isSuccess());

            // Second accept on already ACCEPTED request must fail
            MentorshipRequestResult doubleAccept = mentorshipService.acceptRequest(30);
            assertFalse(doubleAccept.isSuccess());
            assertTrue(doubleAccept.getMessage().toLowerCase().contains("not pending"));

            // Rejecting an already ACCEPTED request must fail
            MentorshipRequestResult rejectAccepted = mentorshipService.rejectRequest(30);
            assertFalse(rejectAccepted.isSuccess());
            assertTrue(rejectAccepted.getMessage().toLowerCase().contains("not pending"));
        }

        @Test
        @DisplayName("Duplicate request prevention: Student cannot send second request while one is pending")
        void testDuplicateRequestHandling() {
            UserSession.getInstance().login(student1);

            // Send first request
            MentorshipRequestResult r1 = mentorshipService.sendRequest(
                    alumni1.getId(), "Topic 1", "Message 1"
            );
            assertTrue(r1.isSuccess());

            // Send duplicate request to same alumni
            MentorshipRequestResult r2 = mentorshipService.sendRequest(
                    alumni1.getId(), "Topic 2", "Message 2"
            );
            assertFalse(r2.isSuccess());
            assertTrue(r2.getMessage().toLowerCase().contains("pending mentorship request already exists"));
        }
    }

    // =========================================================================
    // In-Memory Test Fakes
    // =========================================================================

    private static class InMemoryUserRepository implements UserRepository {
        private final Map<Integer, User> users = new HashMap<>();
        private int seq = 1;

        @Override public boolean existsByEmail(String email) {
            return users.values().stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(email));
        }
        @Override public Optional<User> findById(int id) { return Optional.ofNullable(users.get(id)); }
        @Override public Optional<User> findByEmail(String email) {
            return users.values().stream().filter(u -> u.getEmail().equalsIgnoreCase(email)).findFirst();
        }
        @Override public Optional<Student> findStudentById(int userId) {
            User u = users.get(userId);
            return (u instanceof Student s) ? Optional.of(s) : Optional.empty();
        }
        @Override public Optional<Alumni> findAlumniById(int userId) {
            User u = users.get(userId);
            return (u instanceof Alumni a) ? Optional.of(a) : Optional.empty();
        }
        @Override public Student saveStudent(Student s) {
            if (s.getId() == null) s.setId(seq++);
            users.put(s.getId(), s);
            return s;
        }
        @Override public Alumni saveAlumni(Alumni a) {
            if (a.getId() == null) a.setId(seq++);
            users.put(a.getId(), a);
            return a;
        }
        @Override public boolean deleteUserById(int userId) { return users.remove(userId) != null; }
    }

    private static class InMemoryAlumniProfileRepository implements AlumniProfileRepository {
        private final Map<Integer, Alumni> profiles = new HashMap<>();

        void save(Alumni a) { profiles.put(a.getId(), a); }

        @Override public Optional<Alumni> findProfileByUserId(int userId) {
            return Optional.ofNullable(profiles.get(userId));
        }
        @Override public boolean updateProfile(Alumni alumni) {
            profiles.put(alumni.getId(), alumni);
            return true;
        }
        @Override public boolean createProfile(Alumni alumni) {
            profiles.put(alumni.getId(), alumni);
            return true;
        }
        @Override public boolean profileExistsForUser(int userId) {
            return profiles.containsKey(userId);
        }
    }

    private static class InMemoryMentorshipRequestRepository implements MentorshipRequestRepository {
        private final Map<Integer, MentorshipRequest> requests = new HashMap<>();
        private int seq = 1;

        void save(MentorshipRequest r) { requests.put(r.getId(), r); }

        @Override public int createRequest(MentorshipRequestDto dto) {
            int id = seq++;
            MentorshipRequest r = new MentorshipRequest();
            r.setId(id);
            r.setStudentId(dto.getStudentId());
            r.setAlumniId(dto.getAlumniId());
            r.setTopic(dto.getTopic());
            r.setMessage(dto.getMessage());
            r.setStatus(MentorshipStatus.PENDING);
            r.setRequestedAt(LocalDateTime.now());
            requests.put(id, r);
            return id;
        }

        @Override public boolean existsPending(int studentId, int alumniId) {
            return requests.values().stream().anyMatch(r ->
                    r.getStudentId() == studentId && r.getAlumniId() == alumniId && r.getStatus() == MentorshipStatus.PENDING);
        }

        @Override public boolean existsAccepted(int studentId, int alumniId) {
            return requests.values().stream().anyMatch(r ->
                    r.getStudentId() == studentId && r.getAlumniId() == alumniId && r.getStatus() == MentorshipStatus.ACCEPTED);
        }

        @Override public int countActiveMentees(int alumniId) {
            return (int) requests.values().stream()
                    .filter(r -> r.getAlumniId() == alumniId && r.getStatus() == MentorshipStatus.ACCEPTED)
                    .count();
        }

        @Override public boolean isAlumniAvailable(int alumniId) {
            return true;
        }

        @Override public Optional<MentorshipRequest> findRequestById(int requestId) {
            return Optional.ofNullable(requests.get(requestId));
        }

        @Override public boolean updateRequestStatus(int requestId, MentorshipStatus newStatus, LocalDateTime respondedAt) {
            MentorshipRequest req = requests.get(requestId);
            if (req != null) {
                req.setStatus(newStatus);
                req.setRespondedAt(respondedAt);
                return true;
            }
            return false;
        }

        @Override public List<MentorshipRequest> findRequestsByAlumniId(int alumniId) {
            return requests.values().stream().filter(r -> r.getAlumniId() == alumniId).toList();
        }

        @Override public List<MentorshipRequest> findRequestsByStudentId(int studentId) {
            return requests.values().stream().filter(r -> r.getStudentId() == studentId).toList();
        }

        @Override public List<MentorshipRequest> findAcceptedConnectionsByStudentId(int studentId) {
            return requests.values().stream()
                    .filter(r -> r.getStudentId() == studentId && r.getStatus() == MentorshipStatus.ACCEPTED)
                    .toList();
        }

        @Override public List<MentorshipRequest> findAcceptedConnectionsByAlumniId(int alumniId) {
            return requests.values().stream()
                    .filter(r -> r.getAlumniId() == alumniId && r.getStatus() == MentorshipStatus.ACCEPTED)
                    .toList();
        }
    }
}
