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
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Mentorship Request Service Tests (Prompt 16, 17, 18 & 19)")
class MentorshipRequestServiceTest {

    private InMemoryMentorshipRequestRepository fakeRequestRepository;
    private InMemoryUserRepository fakeUserRepository;
    private MentorshipRequestService requestService;

    private Student loggedInStudent;
    private Alumni targetAlumni;
    private Alumni otherAlumni;

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

        // Set up another alumni
        otherAlumni = new Alumni();
        otherAlumni.setId(303);
        otherAlumni.setEmail("otheralumni@microsoft.com");
        otherAlumni.setFullName("Rohit Sharma");
        otherAlumni.setRole(Role.ALUMNI);
        otherAlumni.setAvailableForMentorship(true);
        otherAlumni.setMaxMentees(3);
        fakeUserRepository.saveUser(otherAlumni);

        // Configure repository state
        fakeRequestRepository.setAlumniAvailable(202, true);
        fakeRequestRepository.setActiveMenteeCount(202, 1);
    }

    @AfterEach
    void tearDown() {
        UserSession.getInstance().logout();
    }

    // ── Prompt 16 Tests: Sending Mentorship Requests ──────────────────────────

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
    @DisplayName("6. Duplicate pending request rejected")
    void testDuplicatePendingRequestRejected() {
        UserSession.getInstance().login(loggedInStudent);
        fakeRequestRepository.setPendingExists(101, 202, true);

        MentorshipRequestResult result = requestService.sendRequest(
                202,
                "Topic",
                "Message"
        );

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("A pending mentorship request already exists"));
    }

    @Test
    @DisplayName("7. Duplicate accepted request rejected")
    void testDuplicateAcceptedRequestRejected() {
        UserSession.getInstance().login(loggedInStudent);
        fakeRequestRepository.setAcceptedExists(101, 202, true);

        MentorshipRequestResult result = requestService.sendRequest(
                202,
                "Topic",
                "Message"
        );

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("You already have an accepted mentorship request"));
    }

    @Test
    @DisplayName("8. Alumni not available for mentorship rejected")
    void testAlumniNotAvailableRejected() {
        UserSession.getInstance().login(loggedInStudent);
        fakeRequestRepository.setAlumniAvailable(202, false);

        MentorshipRequestResult result = requestService.sendRequest(
                202,
                "Topic",
                "Message"
        );

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("currently not available for mentorship"));
    }

    @Test
    @DisplayName("9. Alumni at maximum mentee capacity rejected")
    void testAlumniMaxMenteeCapacityRejected() {
        UserSession.getInstance().login(loggedInStudent);
        fakeRequestRepository.setActiveMenteeCount(202, 3); // targetAlumni max is 3

        MentorshipRequestResult result = requestService.sendRequest(
                202,
                "Topic",
                "Message"
        );

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("maximum number of mentees"));
    }

    @Test
    @DisplayName("10. Self-request rejected")
    void testSelfRequestRejected() {
        // Create an alumni user who somehow logs in
        UserSession.getInstance().login(targetAlumni);

        // However, role check fails first if not STUDENT. Let's make a student with id 202
        Student selfStudent = new Student();
        selfStudent.setId(202);
        selfStudent.setRole(Role.STUDENT);
        UserSession.getInstance().login(selfStudent);

        MentorshipRequestResult result = requestService.sendRequest(
                202,
                "Topic",
                "Message"
        );

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("cannot send a mentorship request to yourself"));
    }

    @Test
    @DisplayName("11. Database failure handled gracefully")
    void testDatabaseFailureHandledGracefully() {
        UserSession.getInstance().login(loggedInStudent);
        fakeRequestRepository.setThrowOnCreate(true);

        MentorshipRequestResult result = requestService.sendRequest(
                202,
                "Topic",
                "Message"
        );

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Database error"));
    }

    @Test
    @DisplayName("12. Invalid alumni ID rejected")
    void testInvalidAlumniIdRejected() {
        UserSession.getInstance().login(loggedInStudent);

        MentorshipRequestResult res1 = requestService.sendRequest(0, "Topic", "Message");
        assertFalse(res1.isSuccess());
        assertTrue(res1.getMessage().contains("Invalid alumni selected"));

        MentorshipRequestResult res2 = requestService.sendRequest(-5, "Topic", "Message");
        assertFalse(res2.isSuccess());
        assertTrue(res2.getMessage().contains("Invalid alumni selected"));
    }

    @Test
    @DisplayName("13. Topic and message validation")
    void testTopicAndMessageValidation() {
        UserSession.getInstance().login(loggedInStudent);

        // Null / empty topic
        MentorshipRequestResult res1 = requestService.sendRequest(202, null, "Message");
        assertFalse(res1.isSuccess());
        assertTrue(res1.getMessage().contains("Mentorship topic is required"));

        MentorshipRequestResult res2 = requestService.sendRequest(202, "   ", "Message");
        assertFalse(res2.isSuccess());
        assertTrue(res2.getMessage().contains("Mentorship topic is required"));

        // Null / empty message
        MentorshipRequestResult res3 = requestService.sendRequest(202, "Topic", null);
        assertFalse(res3.isSuccess());
        assertTrue(res3.getMessage().contains("Mentorship message is required"));

        MentorshipRequestResult res4 = requestService.sendRequest(202, "Topic", "   ");
        assertFalse(res4.isSuccess());
        assertTrue(res4.getMessage().contains("Mentorship message is required"));

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

    // ── Prompt 17 Tests: Alumni Mentorship Request Management ─────────────────

    @Test
    @DisplayName("14. Authenticated alumnus can retrieve their received requests with student details")
    void testRetrieveRequestsForAlumniSuccessfully() {
        UserSession.getInstance().login(targetAlumni); // Alumni ID 202

        // Create request 1 for alumni 202
        MentorshipRequest req1 = new MentorshipRequest(101, 202, "Distributed Systems", "Need guidance on Raft");
        req1.setId(1);
        req1.setStatus(MentorshipStatus.PENDING);
        req1.setStudentName("Aman Verma");
        req1.setStudentEmail("student@thapar.edu");
        req1.setStudentDegree("Bachelor of Technology");
        req1.setStudentDepartment("Computer Science");
        req1.setStudentGraduationYear(2026);
        fakeRequestRepository.addRequest(req1);

        // Create request 2 for alumni 202
        MentorshipRequest req2 = new MentorshipRequest(102, 202, "Career Advice", "Tips for tech interviews");
        req2.setId(2);
        req2.setStatus(MentorshipStatus.PENDING);
        req2.setStudentName("Priya Singh");
        req2.setStudentEmail("priya@thapar.edu");
        fakeRequestRepository.addRequest(req2);

        // Create request 3 for otherAlumni (303) — should NOT be included in alumni 202's list
        MentorshipRequest req3 = new MentorshipRequest(101, 303, "Cloud Dev", "AWS vs GCP");
        req3.setId(3);
        fakeRequestRepository.addRequest(req3);

        MentorshipRequestResult result = requestService.getRequestsForAlumni(202);

        assertTrue(result.isSuccess(), "Retrieval should succeed: " + result.getMessage());
        List<MentorshipRequest> requests = result.getRequests();
        assertEquals(2, requests.size(), "Should only retrieve requests received by alumni 202");

        MentorshipRequest first = requests.get(0);
        assertEquals("Aman Verma", first.getStudentName());
        assertEquals("student@thapar.edu", first.getStudentEmail());
        assertEquals("Distributed Systems", first.getTopic());
        assertEquals("Need guidance on Raft", first.getMessage());
        assertEquals(MentorshipStatus.PENDING, first.getStatus());
        assertNotNull(first.getRequestedAt());

        // Test convenience methods
        MentorshipRequestResult receivedResult = requestService.getReceivedRequests();
        assertTrue(receivedResult.isSuccess());
        assertEquals(2, receivedResult.getRequests().size());

        List<MentorshipRequest> directList = requestService.getRequests(202);
        assertEquals(2, directList.size());
    }

    @Test
    @DisplayName("15. Unauthenticated user cannot retrieve alumni requests")
    void testRetrieveRequestsUnauthenticatedFails() {
        // No session active
        MentorshipRequestResult result = requestService.getRequestsForAlumni(202);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().toLowerCase().contains("access denied") ||
                result.getMessage().toLowerCase().contains("logged in"));
        assertTrue(result.getRequests().isEmpty());
    }

    @Test
    @DisplayName("16. Student role cannot retrieve alumni requests")
    void testRetrieveRequestsByStudentFails() {
        UserSession.getInstance().login(loggedInStudent); // Student role

        MentorshipRequestResult result = requestService.getRequestsForAlumni(202);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Only alumni can view received mentorship requests"));
        assertTrue(result.getRequests().isEmpty());
    }

    @Test
    @DisplayName("17. Alumnus cannot retrieve requests belonging to another alumnus")
    void testRetrieveRequestsForAnotherAlumnusFails() {
        UserSession.getInstance().login(targetAlumni); // ID 202

        // Try to retrieve requests for alumni 303
        MentorshipRequestResult result = requestService.getRequestsForAlumni(303);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("cannot view mentorship requests for another alumnus"));
        assertTrue(result.getRequests().isEmpty());
    }

    @Test
    @DisplayName("18. Retrieve requests with invalid alumni ID rejected")
    void testRetrieveRequestsInvalidIdFails() {
        UserSession.getInstance().login(targetAlumni);

        MentorshipRequestResult resZero = requestService.getRequestsForAlumni(0);
        assertFalse(resZero.isSuccess());
        assertTrue(resZero.getMessage().contains("Invalid alumni ID"));

        MentorshipRequestResult resNeg = requestService.getRequestsForAlumni(-1);
        assertFalse(resNeg.isSuccess());
        assertTrue(resNeg.getMessage().contains("Invalid alumni ID"));
    }

    @Test
    @DisplayName("19. Authenticated alumnus can accept a pending request successfully")
    void testAcceptPendingRequestSuccessfully() throws Exception {
        UserSession.getInstance().login(targetAlumni); // ID 202

        MentorshipRequest req = new MentorshipRequest(101, 202, "System Design", "Guidance request");
        req.setId(10);
        req.setStatus(MentorshipStatus.PENDING);
        fakeRequestRepository.addRequest(req);

        MentorshipRequestResult result = requestService.acceptRequest(10);

        assertTrue(result.isSuccess(), "Accepting request should succeed: " + result.getMessage());
        assertEquals(10, result.getRequestId());
        assertTrue(result.getMessage().contains("accepted successfully"));

        // Verify status and responded_at in repository
        Optional<MentorshipRequest> updatedOpt = fakeRequestRepository.findRequestById(10);
        assertTrue(updatedOpt.isPresent());
        MentorshipRequest updated = updatedOpt.get();
        assertEquals(MentorshipStatus.ACCEPTED, updated.getStatus());
        assertNotNull(updated.getRespondedAt());
    }

    @Test
    @DisplayName("20. Authenticated alumnus can reject a pending request successfully")
    void testRejectPendingRequestSuccessfully() throws Exception {
        UserSession.getInstance().login(targetAlumni); // ID 202

        MentorshipRequest req = new MentorshipRequest(101, 202, "Machine Learning", "ML guidance");
        req.setId(11);
        req.setStatus(MentorshipStatus.PENDING);
        fakeRequestRepository.addRequest(req);

        MentorshipRequestResult result = requestService.rejectRequest(11);

        assertTrue(result.isSuccess(), "Rejecting request should succeed: " + result.getMessage());
        assertEquals(11, result.getRequestId());
        assertTrue(result.getMessage().contains("rejected successfully"));

        // Verify status and responded_at in repository
        Optional<MentorshipRequest> updatedOpt = fakeRequestRepository.findRequestById(11);
        assertTrue(updatedOpt.isPresent());
        MentorshipRequest updated = updatedOpt.get();
        assertEquals(MentorshipStatus.REJECTED, updated.getStatus());
        assertNotNull(updated.getRespondedAt());
    }

    @Test
    @DisplayName("21. Accept and reject with invalid request ID rejected")
    void testAcceptRejectInvalidRequestIdRejected() {
        UserSession.getInstance().login(targetAlumni);

        MentorshipRequestResult accZero = requestService.acceptRequest(0);
        assertFalse(accZero.isSuccess());
        assertTrue(accZero.getMessage().contains("Invalid request ID"));

        MentorshipRequestResult accNeg = requestService.acceptRequest(-5);
        assertFalse(accNeg.isSuccess());
        assertTrue(accNeg.getMessage().contains("Invalid request ID"));

        MentorshipRequestResult rejZero = requestService.rejectRequest(0);
        assertFalse(rejZero.isSuccess());
        assertTrue(rejZero.getMessage().contains("Invalid request ID"));

        MentorshipRequestResult rejNeg = requestService.rejectRequest(-2);
        assertFalse(rejNeg.isSuccess());
        assertTrue(rejNeg.getMessage().contains("Invalid request ID"));
    }

    @Test
    @DisplayName("22. Accept and reject on non-existent request rejected")
    void testAcceptRejectNonExistentRequestRejected() {
        UserSession.getInstance().login(targetAlumni);

        MentorshipRequestResult accResult = requestService.acceptRequest(9999);
        assertFalse(accResult.isSuccess());
        assertTrue(accResult.getMessage().contains("Mentorship request not found"));

        MentorshipRequestResult rejResult = requestService.rejectRequest(9999);
        assertFalse(rejResult.isSuccess());
        assertTrue(rejResult.getMessage().contains("Mentorship request not found"));
    }

    @Test
    @DisplayName("23. Alumnus cannot respond to a request owned by another alumnus (incorrect ownership)")
    void testAcceptRejectIncorrectOwnershipRejected() throws Exception {
        UserSession.getInstance().login(targetAlumni); // Logged in as 202

        // Request sent to otherAlumni (303)
        MentorshipRequest req = new MentorshipRequest(101, 303, "Topic", "Message");
        req.setId(25);
        req.setStatus(MentorshipStatus.PENDING);
        fakeRequestRepository.addRequest(req);

        // Attempt to accept
        MentorshipRequestResult accResult = requestService.acceptRequest(25);
        assertFalse(accResult.isSuccess());
        assertTrue(accResult.getMessage().contains("You can only respond to mentorship requests sent to you"));

        // Attempt to reject
        MentorshipRequestResult rejResult = requestService.rejectRequest(25);
        assertFalse(rejResult.isSuccess());
        assertTrue(rejResult.getMessage().contains("You can only respond to mentorship requests sent to you"));

        // Verify request in repository remained PENDING
        assertEquals(MentorshipStatus.PENDING, fakeRequestRepository.findRequestById(25).get().getStatus());
    }

    @Test
    @DisplayName("24. Attempt to accept a non-pending request rejected")
    void testAcceptNonPendingRequestRejected() {
        UserSession.getInstance().login(targetAlumni); // ID 202

        // Already ACCEPTED request
        MentorshipRequest reqAccepted = new MentorshipRequest(101, 202, "Topic", "Message");
        reqAccepted.setId(31);
        reqAccepted.setStatus(MentorshipStatus.ACCEPTED);
        fakeRequestRepository.addRequest(reqAccepted);

        MentorshipRequestResult result1 = requestService.acceptRequest(31);
        assertFalse(result1.isSuccess());
        assertTrue(result1.getMessage().contains("Request is not pending"));

        // Already REJECTED request
        MentorshipRequest reqRejected = new MentorshipRequest(101, 202, "Topic", "Message");
        reqRejected.setId(32);
        reqRejected.setStatus(MentorshipStatus.REJECTED);
        fakeRequestRepository.addRequest(reqRejected);

        MentorshipRequestResult result2 = requestService.acceptRequest(32);
        assertFalse(result2.isSuccess());
        assertTrue(result2.getMessage().contains("Request is not pending"));

        // Already COMPLETED request
        MentorshipRequest reqCompleted = new MentorshipRequest(101, 202, "Topic", "Message");
        reqCompleted.setId(33);
        reqCompleted.setStatus(MentorshipStatus.COMPLETED);
        fakeRequestRepository.addRequest(reqCompleted);

        MentorshipRequestResult result3 = requestService.acceptRequest(33);
        assertFalse(result3.isSuccess());
        assertTrue(result3.getMessage().contains("Request is not pending"));

        // Already CANCELLED request
        MentorshipRequest reqCancelled = new MentorshipRequest(101, 202, "Topic", "Message");
        reqCancelled.setId(34);
        reqCancelled.setStatus(MentorshipStatus.CANCELLED);
        fakeRequestRepository.addRequest(reqCancelled);

        MentorshipRequestResult result4 = requestService.acceptRequest(34);
        assertFalse(result4.isSuccess());
        assertTrue(result4.getMessage().contains("Request is not pending"));
    }

    @Test
    @DisplayName("25. Attempt to reject a non-pending request rejected")
    void testRejectNonPendingRequestRejected() {
        UserSession.getInstance().login(targetAlumni); // ID 202

        // Already ACCEPTED request
        MentorshipRequest reqAccepted = new MentorshipRequest(101, 202, "Topic", "Message");
        reqAccepted.setId(41);
        reqAccepted.setStatus(MentorshipStatus.ACCEPTED);
        fakeRequestRepository.addRequest(reqAccepted);

        MentorshipRequestResult result1 = requestService.rejectRequest(41);
        assertFalse(result1.isSuccess());
        assertTrue(result1.getMessage().contains("Request is not pending"));

        // Already REJECTED request
        MentorshipRequest reqRejected = new MentorshipRequest(101, 202, "Topic", "Message");
        reqRejected.setId(42);
        reqRejected.setStatus(MentorshipStatus.REJECTED);
        fakeRequestRepository.addRequest(reqRejected);

        MentorshipRequestResult result2 = requestService.rejectRequest(42);
        assertFalse(result2.isSuccess());
        assertTrue(result2.getMessage().contains("Request is not pending"));

        // Already COMPLETED request
        MentorshipRequest reqCompleted = new MentorshipRequest(101, 202, "Topic", "Message");
        reqCompleted.setId(43);
        reqCompleted.setStatus(MentorshipStatus.COMPLETED);
        fakeRequestRepository.addRequest(reqCompleted);

        MentorshipRequestResult result3 = requestService.rejectRequest(43);
        assertFalse(result3.isSuccess());
        assertTrue(result3.getMessage().contains("Request is not pending"));

        // Already CANCELLED request
        MentorshipRequest reqCancelled = new MentorshipRequest(101, 202, "Topic", "Message");
        reqCancelled.setId(44);
        reqCancelled.setStatus(MentorshipStatus.CANCELLED);
        fakeRequestRepository.addRequest(reqCancelled);

        MentorshipRequestResult result4 = requestService.rejectRequest(44);
        assertFalse(result4.isSuccess());
        assertTrue(result4.getMessage().contains("Request is not pending"));
    }

    @Test
    @DisplayName("26. Unauthenticated user cannot accept or reject requests")
    void testAcceptRejectUnauthenticatedRejected() {
        // No session active
        MentorshipRequestResult accResult = requestService.acceptRequest(1);
        assertFalse(accResult.isSuccess());
        assertTrue(accResult.getMessage().contains("must be logged in"));

        MentorshipRequestResult rejResult = requestService.rejectRequest(1);
        assertFalse(rejResult.isSuccess());
        assertTrue(rejResult.getMessage().contains("must be logged in"));
    }

    @Test
    @DisplayName("27. Student user cannot accept or reject requests")
    void testAcceptRejectByStudentRejected() {
        UserSession.getInstance().login(loggedInStudent); // Student role

        MentorshipRequestResult accResult = requestService.acceptRequest(1);
        assertFalse(accResult.isSuccess());
        assertTrue(accResult.getMessage().contains("Only alumni can respond to mentorship requests"));

        MentorshipRequestResult rejResult = requestService.rejectRequest(1);
        assertFalse(rejResult.isSuccess());
        assertTrue(rejResult.getMessage().contains("Only alumni can respond to mentorship requests"));
    }

    @Test
    @DisplayName("28. Database exceptions on retrieval and update handled gracefully")
    void testDatabaseExceptionsHandledGracefully() {
        UserSession.getInstance().login(targetAlumni);

        MentorshipRequest req = new MentorshipRequest(101, 202, "Topic", "Message");
        req.setId(50);
        req.setStatus(MentorshipStatus.PENDING);
        fakeRequestRepository.addRequest(req);

        // Error on query
        fakeRequestRepository.setThrowOnQuery(true);
        MentorshipRequestResult queryRes = requestService.getRequestsForAlumni(202);
        assertFalse(queryRes.isSuccess());
        assertTrue(queryRes.getMessage().contains("Database error"));

        fakeRequestRepository.setThrowOnQuery(false);

        // Error on update
        fakeRequestRepository.setThrowOnUpdate(true);
        MentorshipRequestResult updateRes = requestService.acceptRequest(50);
        assertFalse(updateRes.isSuccess());
        assertTrue(updateRes.getMessage().contains("Database error"));
    }

    // ── Prompt 18 Tests: Student Mentorship Request Status ────────────────────

    @Test
    @DisplayName("29. Authenticated student can retrieve their sent requests with alumni details")
    void testRetrieveRequestsForStudentSuccessfully() {
        UserSession.getInstance().login(loggedInStudent); // Student ID 101

        MentorshipRequest req1 = new MentorshipRequest(101, 202, "Distributed Systems", "Need guidance on Raft");
        req1.setId(101);
        req1.setStatus(MentorshipStatus.PENDING);
        req1.setAlumniName("Neha Gupta");
        req1.setAlumniEmail("alumni@google.com");
        req1.setAlumniCompany("Google");
        req1.setAlumniRole("Staff Engineer");
        req1.setAlumniDegree("Bachelor of Technology");
        req1.setAlumniDepartment("Computer Science");
        fakeRequestRepository.addRequest(req1);

        MentorshipRequestResult result = requestService.getRequestsForStudent(101);
        assertTrue(result.isSuccess());
        assertEquals(1, result.getRequests().size());

        MentorshipRequest retrieved = result.getRequests().get(0);
        assertEquals(101, retrieved.getId());
        assertEquals("Neha Gupta", retrieved.getAlumniName());
        assertEquals("alumni@google.com", retrieved.getAlumniEmail());
        assertEquals("Google", retrieved.getAlumniCompany());
        assertEquals("Staff Engineer", retrieved.getAlumniRole());
        assertEquals("Bachelor of Technology", retrieved.getAlumniDegree());
        assertEquals("Computer Science", retrieved.getAlumniDepartment());
        assertEquals(MentorshipStatus.PENDING, retrieved.getStatus());
    }

    @Test
    @DisplayName("30. Student sees multiple requests with different statuses")
    void testStudentSeesMultipleRequestsWithDifferentStatuses() {
        UserSession.getInstance().login(loggedInStudent); // Student ID 101

        MentorshipRequest r1 = new MentorshipRequest(101, 202, "Topic 1", "Msg 1");
        r1.setId(1);
        r1.setStatus(MentorshipStatus.PENDING);
        fakeRequestRepository.addRequest(r1);

        MentorshipRequest r2 = new MentorshipRequest(101, 303, "Topic 2", "Msg 2");
        r2.setId(2);
        r2.setStatus(MentorshipStatus.ACCEPTED);
        fakeRequestRepository.addRequest(r2);

        MentorshipRequest r3 = new MentorshipRequest(101, 202, "Topic 3", "Msg 3");
        r3.setId(3);
        r3.setStatus(MentorshipStatus.REJECTED);
        fakeRequestRepository.addRequest(r3);

        MentorshipRequest r4 = new MentorshipRequest(101, 303, "Topic 4", "Msg 4");
        r4.setId(4);
        r4.setStatus(MentorshipStatus.COMPLETED);
        fakeRequestRepository.addRequest(r4);

        MentorshipRequest r5 = new MentorshipRequest(101, 202, "Topic 5", "Msg 5");
        r5.setId(5);
        r5.setStatus(MentorshipStatus.CANCELLED);
        fakeRequestRepository.addRequest(r5);

        MentorshipRequestResult result = requestService.getRequestsForStudent(101);
        assertTrue(result.isSuccess());
        assertEquals(5, result.getRequests().size());
        assertTrue(result.getRequests().stream().anyMatch(r -> r.getStatus() == MentorshipStatus.PENDING));
        assertTrue(result.getRequests().stream().anyMatch(r -> r.getStatus() == MentorshipStatus.ACCEPTED));
        assertTrue(result.getRequests().stream().anyMatch(r -> r.getStatus() == MentorshipStatus.REJECTED));
        assertTrue(result.getRequests().stream().anyMatch(r -> r.getStatus() == MentorshipStatus.COMPLETED));
        assertTrue(result.getRequests().stream().anyMatch(r -> r.getStatus() == MentorshipStatus.CANCELLED));
    }

    @Test
    @DisplayName("31. Student with no requests gets an empty result successfully")
    void testStudentWithNoRequestsGetsEmptyResult() {
        UserSession.getInstance().login(loggedInStudent); // Student ID 101

        MentorshipRequestResult result = requestService.getRequestsForStudent(101);
        assertTrue(result.isSuccess());
        assertNotNull(result.getRequests());
        assertTrue(result.getRequests().isEmpty());
    }

    @Test
    @DisplayName("32. Unauthenticated user cannot view student requests")
    void testUnauthenticatedUserCannotViewStudentRequests() {
        // No session
        MentorshipRequestResult result = requestService.getRequestsForStudent(101);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("must be logged in"));
    }

    @Test
    @DisplayName("33. Alumni user cannot view student requests")
    void testAlumniUserCannotViewStudentRequests() {
        UserSession.getInstance().login(targetAlumni); // Alumni ID 202

        MentorshipRequestResult result = requestService.getRequestsForStudent(101);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Only students can view sent mentorship requests"));
    }

    @Test
    @DisplayName("34. Invalid student ID is rejected")
    void testInvalidStudentIdRejected() {
        UserSession.getInstance().login(loggedInStudent);

        MentorshipRequestResult res1 = requestService.getRequestsForStudent(0);
        assertFalse(res1.isSuccess());
        assertTrue(res1.getMessage().contains("Invalid student ID"));

        MentorshipRequestResult res2 = requestService.getRequestsForStudent(-1);
        assertFalse(res2.isSuccess());
        assertTrue(res2.getMessage().contains("Invalid student ID"));
    }

    @Test
    @DisplayName("35. Student cannot view another student's requests")
    void testStudentCannotViewAnotherStudentsRequests() {
        UserSession.getInstance().login(loggedInStudent); // Student ID 101

        MentorshipRequestResult result = requestService.getRequestsForStudent(102); // Student ID 102
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("cannot view mentorship requests for another student"));
    }

    @Test
    @DisplayName("36. Database failure on student request query handled gracefully")
    void testDatabaseFailureOnStudentQueryHandledGracefully() {
        UserSession.getInstance().login(loggedInStudent);
        fakeRequestRepository.setThrowOnQuery(true);

        MentorshipRequestResult result = requestService.getRequestsForStudent(101);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Database error while retrieving mentorship requests"));
    }

    @Test
    @DisplayName("37. getMyRequests works for logged-in student")
    void testGetMyRequestsWorksForLoggedInStudent() {
        UserSession.getInstance().login(loggedInStudent); // Student ID 101

        MentorshipRequest req = new MentorshipRequest(101, 202, "Topic", "Msg");
        req.setId(77);
        fakeRequestRepository.addRequest(req);

        MentorshipRequestResult result = requestService.getMyRequests();
        assertTrue(result.isSuccess());
        assertEquals(1, result.getRequests().size());
        assertEquals(77, result.getRequests().get(0).getId());

        // When logged out
        UserSession.getInstance().logout();
        MentorshipRequestResult unauthRes = requestService.getMyRequests();
        assertFalse(unauthRes.isSuccess());
    }

    @Test
    @DisplayName("38. getStudentRequests returns list and throws on unauthorized access")
    void testGetStudentRequestsDirectMethod() {
        UserSession.getInstance().login(loggedInStudent);

        MentorshipRequest req = new MentorshipRequest(101, 202, "Topic", "Msg");
        req.setId(88);
        fakeRequestRepository.addRequest(req);

        List<MentorshipRequest> list = requestService.getStudentRequests(101);
        assertEquals(1, list.size());

        // Unauthorized access throws SecurityException
        assertThrows(SecurityException.class, () -> requestService.getStudentRequests(999));
        // Invalid ID throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            Student invalidStudent = new Student();
            invalidStudent.setId(-1);
            invalidStudent.setRole(Role.STUDENT);
            UserSession.getInstance().login(invalidStudent);
            requestService.getStudentRequests(-1);
        });
    }

    // ── Prompt 19 Tests: Mentorship Connections ──────────────────────────

    @Test
    @DisplayName("39. Student retrieves accepted mentorship connections successfully")
    void testStudentRetrievesAcceptedConnections() {
        UserSession.getInstance().login(loggedInStudent);

        MentorshipRequest acceptedReq = new MentorshipRequest(101, 202, "Career Guidance", "Looking forward to working together");
        acceptedReq.setId(1);
        acceptedReq.setStatus(MentorshipStatus.ACCEPTED);
        acceptedReq.setAlumniName("Neha Gupta");
        acceptedReq.setAlumniEmail("alumni@google.com");
        acceptedReq.setAlumniCompany("Google");
        acceptedReq.setAlumniRole("Staff Engineer");
        acceptedReq.setAlumniDepartment("Computer Science");
        fakeRequestRepository.addRequest(acceptedReq);

        MentorshipRequestResult result = requestService.getConnectionsForStudent(101);
        assertTrue(result.isSuccess(), "Should successfully retrieve connections: " + result.getMessage());
        assertEquals(1, result.getRequests().size());

        MentorshipRequest conn = result.getRequests().get(0);
        assertEquals(MentorshipStatus.ACCEPTED, conn.getStatus());
        assertEquals("Neha Gupta", conn.getAlumniName());
        assertEquals("alumni@google.com", conn.getAlumniEmail());
        assertEquals("Google", conn.getAlumniCompany());
        assertEquals("Staff Engineer", conn.getAlumniRole());
        assertEquals("Computer Science", conn.getAlumniDepartment());
    }

    @Test
    @DisplayName("40. Alumni retrieves accepted mentorship connections successfully")
    void testAlumniRetrievesAcceptedConnections() {
        UserSession.getInstance().login(targetAlumni);

        MentorshipRequest acceptedReq = new MentorshipRequest(101, 202, "System Design", "Need help with distributed systems");
        acceptedReq.setId(2);
        acceptedReq.setStatus(MentorshipStatus.ACCEPTED);
        acceptedReq.setStudentName("Aman Verma");
        acceptedReq.setStudentEmail("student@thapar.edu");
        acceptedReq.setStudentDegree("B.Tech");
        acceptedReq.setStudentDepartment("Computer Science");
        acceptedReq.setStudentGraduationYear(2026);
        fakeRequestRepository.addRequest(acceptedReq);

        MentorshipRequestResult result = requestService.getConnectionsForAlumni(202);
        assertTrue(result.isSuccess(), "Should successfully retrieve connections: " + result.getMessage());
        assertEquals(1, result.getRequests().size());

        MentorshipRequest conn = result.getRequests().get(0);
        assertEquals(MentorshipStatus.ACCEPTED, conn.getStatus());
        assertEquals("Aman Verma", conn.getStudentName());
        assertEquals("student@thapar.edu", conn.getStudentEmail());
        assertEquals("B.Tech", conn.getStudentDegree());
        assertEquals("Computer Science", conn.getStudentDepartment());
        assertEquals(2026, conn.getStudentGraduationYear());
    }

    @Test
    @DisplayName("41. Connections exclude PENDING, REJECTED, COMPLETED, and CANCELLED requests for both roles")
    void testConnectionsExcludeNonAcceptedStatuses() {
        // Create requests in all different statuses
        MentorshipRequest pendingReq = new MentorshipRequest(101, 202, "Topic 1", "Msg 1");
        pendingReq.setId(10);
        pendingReq.setStatus(MentorshipStatus.PENDING);
        fakeRequestRepository.addRequest(pendingReq);

        MentorshipRequest rejectedReq = new MentorshipRequest(101, 202, "Topic 2", "Msg 2");
        rejectedReq.setId(11);
        rejectedReq.setStatus(MentorshipStatus.REJECTED);
        fakeRequestRepository.addRequest(rejectedReq);

        MentorshipRequest completedReq = new MentorshipRequest(101, 202, "Topic 3", "Msg 3");
        completedReq.setId(12);
        completedReq.setStatus(MentorshipStatus.COMPLETED);
        fakeRequestRepository.addRequest(completedReq);

        MentorshipRequest cancelledReq = new MentorshipRequest(101, 202, "Topic 4", "Msg 4");
        cancelledReq.setId(13);
        cancelledReq.setStatus(MentorshipStatus.CANCELLED);
        fakeRequestRepository.addRequest(cancelledReq);

        MentorshipRequest acceptedReq = new MentorshipRequest(101, 202, "Topic 5", "Msg 5");
        acceptedReq.setId(14);
        acceptedReq.setStatus(MentorshipStatus.ACCEPTED);
        fakeRequestRepository.addRequest(acceptedReq);

        // Verify Student view: only ACCEPTED returned
        UserSession.getInstance().login(loggedInStudent);
        MentorshipRequestResult studentRes = requestService.getConnectionsForStudent(101);
        assertTrue(studentRes.isSuccess());
        assertEquals(1, studentRes.getRequests().size());
        assertEquals(14, studentRes.getRequests().get(0).getId());
        assertEquals(MentorshipStatus.ACCEPTED, studentRes.getRequests().get(0).getStatus());

        // Verify Alumni view: only ACCEPTED returned
        UserSession.getInstance().login(targetAlumni);
        MentorshipRequestResult alumniRes = requestService.getConnectionsForAlumni(202);
        assertTrue(alumniRes.isSuccess());
        assertEquals(1, alumniRes.getRequests().size());
        assertEquals(14, alumniRes.getRequests().get(0).getId());
        assertEquals(MentorshipStatus.ACCEPTED, alumniRes.getRequests().get(0).getStatus());
    }

    @Test
    @DisplayName("42. Empty results return successful empty list for student and alumni")
    void testEmptyConnectionsResults() {
        // No requests in repository
        UserSession.getInstance().login(loggedInStudent);
        MentorshipRequestResult studentRes = requestService.getConnectionsForStudent(101);
        assertTrue(studentRes.isSuccess());
        assertTrue(studentRes.getRequests().isEmpty());

        UserSession.getInstance().login(targetAlumni);
        MentorshipRequestResult alumniRes = requestService.getConnectionsForAlumni(202);
        assertTrue(alumniRes.isSuccess());
        assertTrue(alumniRes.getRequests().isEmpty());
    }

    @Test
    @DisplayName("43. Incorrect user ownership rejected for student and alumni connections")
    void testIncorrectOwnershipRejected() {
        // Student attempting to access another student's connections
        UserSession.getInstance().login(loggedInStudent); // ID 101
        MentorshipRequestResult studentOtherRes = requestService.getConnectionsForStudent(999);
        assertFalse(studentOtherRes.isSuccess());
        assertTrue(studentOtherRes.getMessage().contains("Access denied"));

        // Alumni attempting to access another alumni's connections
        UserSession.getInstance().login(targetAlumni); // ID 202
        MentorshipRequestResult alumniOtherRes = requestService.getConnectionsForAlumni(303);
        assertFalse(alumniOtherRes.isSuccess());
        assertTrue(alumniOtherRes.getMessage().contains("Access denied"));
    }

    @Test
    @DisplayName("44. Unauthenticated access rejected for all connection methods")
    void testUnauthenticatedAccessRejected() {
        UserSession.getInstance().logout();

        MentorshipRequestResult res1 = requestService.getConnectionsForStudent(101);
        assertFalse(res1.isSuccess());
        assertTrue(res1.getMessage().contains("Access denied"));

        MentorshipRequestResult res2 = requestService.getConnectionsForAlumni(202);
        assertFalse(res2.isSuccess());
        assertTrue(res2.getMessage().contains("Access denied"));

        MentorshipRequestResult res3 = requestService.getMyMentorshipConnections();
        assertFalse(res3.isSuccess());
        assertTrue(res3.getMessage().contains("Access denied"));
    }

    @Test
    @DisplayName("45. Incorrect role access rejected (Student calling alumni method, Alumni calling student method)")
    void testIncorrectRoleAccessRejected() {
        // Student attempting to call getConnectionsForAlumni
        UserSession.getInstance().login(loggedInStudent);
        MentorshipRequestResult studAsAlumni = requestService.getConnectionsForAlumni(101);
        assertFalse(studAsAlumni.isSuccess());
        assertTrue(studAsAlumni.getMessage().contains("Only alumni"));

        // Alumni attempting to call getConnectionsForStudent
        UserSession.getInstance().login(targetAlumni);
        MentorshipRequestResult alumniAsStud = requestService.getConnectionsForStudent(202);
        assertFalse(alumniAsStud.isSuccess());
        assertTrue(alumniAsStud.getMessage().contains("Only students"));
    }

    @Test
    @DisplayName("46. Invalid user IDs rejected with proper error messages")
    void testInvalidUserIdsRejected() {
        UserSession.getInstance().login(loggedInStudent);
        MentorshipRequestResult zeroStudent = requestService.getConnectionsForStudent(0);
        assertFalse(zeroStudent.isSuccess());
        assertTrue(zeroStudent.getMessage().contains("Invalid student ID"));

        MentorshipRequestResult negStudent = requestService.getConnectionsForStudent(-5);
        assertFalse(negStudent.isSuccess());
        assertTrue(negStudent.getMessage().contains("Invalid student ID"));

        UserSession.getInstance().login(targetAlumni);
        MentorshipRequestResult zeroAlumni = requestService.getConnectionsForAlumni(0);
        assertFalse(zeroAlumni.isSuccess());
        assertTrue(zeroAlumni.getMessage().contains("Invalid alumni ID"));

        MentorshipRequestResult negAlumni = requestService.getConnectionsForAlumni(-5);
        assertFalse(negAlumni.isSuccess());
        assertTrue(negAlumni.getMessage().contains("Invalid alumni ID"));
    }

    @Test
    @DisplayName("47. getMyMentorshipConnections resolves correctly based on logged-in role")
    void testGetMyMentorshipConnectionsDynamicResolution() {
        MentorshipRequest acceptedReq = new MentorshipRequest(101, 202, "Topic", "Msg");
        acceptedReq.setId(50);
        acceptedReq.setStatus(MentorshipStatus.ACCEPTED);
        fakeRequestRepository.addRequest(acceptedReq);

        // As Student
        UserSession.getInstance().login(loggedInStudent);
        MentorshipRequestResult studentMyRes = requestService.getMyMentorshipConnections();
        assertTrue(studentMyRes.isSuccess());
        assertEquals(1, studentMyRes.getRequests().size());
        assertEquals(50, studentMyRes.getRequests().get(0).getId());

        // As Alumni
        UserSession.getInstance().login(targetAlumni);
        MentorshipRequestResult alumniMyRes = requestService.getMyMentorshipConnections();
        assertTrue(alumniMyRes.isSuccess());
        assertEquals(1, alumniMyRes.getRequests().size());
        assertEquals(50, alumniMyRes.getRequests().get(0).getId());
    }

    @Test
    @DisplayName("48. Database errors during connection retrieval handled gracefully")
    void testDatabaseErrorHandling() {
        fakeRequestRepository.setThrowOnQuery(true);

        UserSession.getInstance().login(loggedInStudent);
        MentorshipRequestResult studentRes = requestService.getConnectionsForStudent(101);
        assertFalse(studentRes.isSuccess());
        assertTrue(studentRes.getMessage().contains("Database error"));

        UserSession.getInstance().login(targetAlumni);
        MentorshipRequestResult alumniRes = requestService.getConnectionsForAlumni(202);
        assertFalse(alumniRes.isSuccess());
        assertTrue(alumniRes.getMessage().contains("Database error"));
    }

    @Test
    @DisplayName("49. Direct list methods getStudentConnections and getAlumniConnections return lists and throw on errors")
    void testDirectListMethods() {
        MentorshipRequest acceptedReq = new MentorshipRequest(101, 202, "Topic", "Msg");
        acceptedReq.setId(60);
        acceptedReq.setStatus(MentorshipStatus.ACCEPTED);
        fakeRequestRepository.addRequest(acceptedReq);

        // Student direct
        UserSession.getInstance().login(loggedInStudent);
        List<MentorshipRequest> studentList = requestService.getStudentConnections(101);
        assertEquals(1, studentList.size());
        assertThrows(SecurityException.class, () -> requestService.getStudentConnections(999));

        // Alumni direct
        UserSession.getInstance().login(targetAlumni);
        List<MentorshipRequest> alumniList = requestService.getAlumniConnections(202);
        assertEquals(1, alumniList.size());
        assertThrows(SecurityException.class, () -> requestService.getAlumniConnections(999));
    }

    // ── In-Memory MentorshipRequestRepository Fake ──────────────────────────

    private static class InMemoryMentorshipRequestRepository implements MentorshipRequestRepository {
        private int sequence = 1;
        private MentorshipRequestDto lastCreatedDto;
        private final Map<String, Boolean> pendingRequests = new HashMap<>();
        private final Map<String, Boolean> acceptedRequests = new HashMap<>();
        private final Map<Integer, Integer> activeMenteesCount = new HashMap<>();
        private final Map<Integer, Boolean> alumniAvailability = new HashMap<>();
        private final Map<Integer, MentorshipRequest> requests = new HashMap<>();

        private boolean throwOnCreate = false;
        private boolean throwOnQuery = false;
        private boolean throwOnUpdate = false;

        void setThrowOnCreate(boolean val) { this.throwOnCreate = val; }
        void setThrowOnQuery(boolean val) { this.throwOnQuery = val; }
        void setThrowOnUpdate(boolean val) { this.throwOnUpdate = val; }

        void addRequest(MentorshipRequest req) {
            requests.put(req.getId(), req);
        }

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
            int id = sequence++;
            MentorshipRequest req = new MentorshipRequest(dto.getStudentId(), dto.getAlumniId(), dto.getTopic(), dto.getMessage());
            req.setId(id);
            requests.put(id, req);
            return id;
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

        @Override
        public List<MentorshipRequest> findRequestsByAlumniId(int alumniId) throws SQLException {
            if (throwOnQuery) {
                throw new SQLException("Simulated DB query error");
            }
            return requests.values().stream()
                    .filter(r -> r.getAlumniId() == alumniId)
                    .toList();
        }

        @Override
        public List<MentorshipRequest> findRequestsByStudentId(int studentId) throws SQLException {
            if (throwOnQuery) {
                throw new SQLException("Simulated DB query error");
            }
            return requests.values().stream()
                    .filter(r -> r.getStudentId() == studentId)
                    .toList();
        }

        @Override
        public List<MentorshipRequest> findAcceptedConnectionsByStudentId(int studentId) throws SQLException {
            if (throwOnQuery) {
                throw new SQLException("Simulated DB query error");
            }
            return requests.values().stream()
                    .filter(r -> r.getStudentId() == studentId && r.getStatus() == MentorshipStatus.ACCEPTED)
                    .toList();
        }

        @Override
        public List<MentorshipRequest> findAcceptedConnectionsByAlumniId(int alumniId) throws SQLException {
            if (throwOnQuery) {
                throw new SQLException("Simulated DB query error");
            }
            return requests.values().stream()
                    .filter(r -> r.getAlumniId() == alumniId && r.getStatus() == MentorshipStatus.ACCEPTED)
                    .toList();
        }

        @Override
        public Optional<MentorshipRequest> findRequestById(int requestId) throws SQLException {
            if (throwOnQuery) {
                throw new SQLException("Simulated DB query error");
            }
            return Optional.ofNullable(requests.get(requestId));
        }

        @Override
        public boolean updateRequestStatus(int requestId, MentorshipStatus newStatus, LocalDateTime respondedAt) throws SQLException {
            if (throwOnUpdate) {
                throw new SQLException("Simulated DB update error");
            }
            MentorshipRequest req = requests.get(requestId);
            if (req != null) {
                req.setStatus(newStatus);
                req.setRespondedAt(respondedAt);
                return true;
            }
            return false;
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
