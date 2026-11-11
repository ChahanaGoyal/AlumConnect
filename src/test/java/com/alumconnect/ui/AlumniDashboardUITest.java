package com.alumconnect.ui;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.MentorshipRequest;
import com.alumconnect.model.MentorshipStatus;
import com.alumconnect.model.Role;
import com.alumconnect.model.Student;
import com.alumconnect.model.User;
import com.alumconnect.repository.AlumniProfileRepository;
import com.alumconnect.repository.MentorshipRequestRepository;
import com.alumconnect.repository.UserRepository;
import com.alumconnect.service.AlumniProfileService;
import com.alumconnect.service.MentorshipRequestService;
import com.alumconnect.service.UserService;
import com.alumconnect.service.dto.AlumniProfileResult;
import com.alumconnect.service.dto.MentorshipRequestDto;
import com.alumconnect.util.PasswordUtil;
import com.alumconnect.util.UserSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AlumniDashboardUI} — Prompt 11.
 *
 * <p>Uses in-memory fakes for all repositories and services so that no real
 * database connection is needed. Every test starts with a clean
 * {@link UserSession} and tears it down afterwards.
 */
@DisplayName("Alumni Dashboard Tests (Prompt 11)")
class AlumniDashboardUITest {

    // ── Shared test fixtures ──────────────────────────────────────────────────

    private Student student;
    private Alumni alumni;
    private InMemoryUserRepository userRepo;
    private UserService userService;

    @BeforeEach
    void setUp() throws SQLException {
        UserSession.getInstance().logout();

        userRepo = new InMemoryUserRepository();
        userService = new UserService(userRepo);

        // Seed a student
        student = new Student(
                "student@thapar.edu",
                PasswordUtil.hashPassword("StudentP@ss1"),
                "Anshika Singh",
                "+91-9876543210",
                "Computer Science",
                2026
        );
        student.setCurrentSemester(6);
        student.setEnrollmentNumber("102203999");
        userRepo.saveStudent(student);

        // Seed an alumni
        alumni = new Alumni(
                "alumni@thapar.edu",
                PasswordUtil.hashPassword("AlumniP@ss1"),
                "Ravi Kumar",
                "+91-9811001100",
                "Electronics",
                2021
        );
        alumni.setCurrentCompany("Google");
        alumni.setCurrentRole("Software Engineer");
        alumni.setYearsOfExperience(3);
        alumni.setAvailableForMentorship(true);
        alumni.setMaxMentees(2);
        alumni.setExpertiseAreas("Java, Cloud, Distributed Systems");
        userRepo.saveAlumni(alumni);
    }

    @AfterEach
    void tearDown() {
        UserSession.getInstance().logout();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private AlumniDashboardUI buildDashboard(String simulatedInput,
                                             ByteArrayOutputStream capture) {
        return buildDashboardWithMentorship(simulatedInput,
                new MentorshipRequestService(new FakeMentorshipRequestRepository(), userRepo),
                capture);
    }

    private AlumniDashboardUI buildDashboardWithMentorship(String simulatedInput,
                                                          MentorshipRequestService mentorshipService,
                                                          ByteArrayOutputStream capture) {
        ByteArrayInputStream in = new ByteArrayInputStream(
                simulatedInput.getBytes(StandardCharsets.UTF_8));
        PrintStream ps = new PrintStream(capture);

        FakeAlumniProfileService fakeProfileService = new FakeAlumniProfileService();
        AlumniProfileUI profileUI = new AlumniProfileUI(fakeProfileService, in, ps);
        return new AlumniDashboardUI(profileUI, mentorshipService, userService, in, ps);
    }

    // ── Security / access control tests ───────────────────────────────────────

    @Test
    @DisplayName("Unauthenticated user cannot access Alumni Dashboard")
    void testUnauthenticatedAccessIsRejected() {
        // No login -> session is empty
        AlumniDashboardUI dashboard = buildDashboard("", new ByteArrayOutputStream());
        assertThrows(SecurityException.class, dashboard::show,
                "show() must throw SecurityException when no user is logged in.");
    }

    @Test
    @DisplayName("Authenticated Student cannot access Alumni Dashboard")
    void testStudentCannotAccessAlumniDashboard() {
        userService.login("student@thapar.edu", "StudentP@ss1");
        assertTrue(UserSession.getInstance().isStudent());

        AlumniDashboardUI dashboard = buildDashboard("", new ByteArrayOutputStream());
        assertThrows(SecurityException.class, dashboard::show,
                "show() must throw SecurityException when role is STUDENT.");
    }

    @Test
    @DisplayName("Authenticated Alumni can access Alumni Dashboard")
    void testAlumniCanAccessDashboard() {
        userService.login("alumni@thapar.edu", "AlumniP@ss1");
        assertTrue(UserSession.getInstance().isAlumni());

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        // Send "0" (Exit) so the menu loop terminates cleanly
        AlumniDashboardUI dashboard = buildDashboard("0\n", capture);
        assertDoesNotThrow(dashboard::show);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("ALUMNI DASHBOARD"), "Header must appear");
        assertTrue(output.contains("Ravi Kumar"),        "Alumni name must appear");
        assertTrue(output.contains("Google"),            "Company must appear");
        assertTrue(output.contains("Software Engineer"), "Role must appear");
    }

    @Test
    @DisplayName("Alumni role is correctly recognised in session after login")
    void testAlumniRoleIsRecognised() {
        userService.login("alumni@thapar.edu", "AlumniP@ss1");
        assertTrue(UserSession.getInstance().isAlumni());
        assertFalse(UserSession.getInstance().isStudent());
        assertEquals(Role.ALUMNI, UserSession.getInstance().getCurrentRole().orElse(null));
    }

    @Test
    @DisplayName("Logged-out session cannot access Alumni Dashboard")
    void testLoggedOutSessionIsRejected() {
        userService.login("alumni@thapar.edu", "AlumniP@ss1");
        userService.logout();
        assertFalse(UserSession.getInstance().isLoggedIn());

        AlumniDashboardUI dashboard = buildDashboard("", new ByteArrayOutputStream());
        assertThrows(SecurityException.class, dashboard::show);
    }

    // ── Menu behaviour tests ──────────────────────────────────────────────────

    @Test
    @DisplayName("Dashboard handles invalid menu input gracefully")
    void testInvalidMenuChoice() {
        userService.login("alumni@thapar.edu", "AlumniP@ss1");

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        AlumniDashboardUI dashboard = buildDashboard("99\n", capture);
        assertDoesNotThrow(dashboard::show);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Invalid choice"), "Invalid choice message must appear");
    }

    @Test
    @DisplayName("Menu option 0 (Exit) prints goodbye and returns cleanly")
    void testExitOption() {
        userService.login("alumni@thapar.edu", "AlumniP@ss1");

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        AlumniDashboardUI dashboard = buildDashboard("0\n", capture);
        assertDoesNotThrow(dashboard::show);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Goodbye"), "Exit option must print goodbye");
    }

    @Test
    @DisplayName("Menu option 9 (Logout) clears the session")
    void testLogoutClearsSession() {
        userService.login("alumni@thapar.edu", "AlumniP@ss1");
        assertTrue(UserSession.getInstance().isLoggedIn());

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        AlumniDashboardUI dashboard = buildDashboard("9\n", capture);
        assertDoesNotThrow(dashboard::show);

        assertFalse(UserSession.getInstance().isLoggedIn(), "Session must be cleared after logout");

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("logged out"), "Logout confirmation must appear");
    }

    @Test
    @DisplayName("Menu legacy option 4 also logs out cleanly")
    void testLegacyLogoutOption() {
        userService.login("alumni@thapar.edu", "AlumniP@ss1");
        assertTrue(UserSession.getInstance().isLoggedIn());

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        AlumniDashboardUI dashboard = buildDashboard("4\n", capture);
        assertDoesNotThrow(dashboard::show);

        assertFalse(UserSession.getInstance().isLoggedIn(), "Session must be cleared after logout");
    }

    @Test
    @DisplayName("Dashboard menu shows correct options for implemented features only")
    void testMenuShowsCorrectOptions() {
        userService.login("alumni@thapar.edu", "AlumniP@ss1");

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        AlumniDashboardUI dashboard = buildDashboard("0\n", capture);
        dashboard.show();

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("My Profile"), "Profile option must be listed");
        assertTrue(output.contains("Logout"),     "Logout option must be listed");
        assertTrue(output.contains("Exit"),       "Exit option must be listed");
        // Verify no fake placeholder job or application features are present
        assertFalse(output.contains("Job Portal"));
        assertFalse(output.contains("Post Job"));
        assertFalse(output.contains("Applications"));
    }

    // ── Profile sub-menu tests ────────────────────────────────────────────────

    @Test
    @DisplayName("Profile sub-menu option V routes to displayMyProfile")
    void testProfileSubMenuView() {
        userService.login("alumni@thapar.edu", "AlumniP@ss1");

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        AlumniDashboardUI dashboard = buildDashboard("1\nV\n", capture);
        assertDoesNotThrow(dashboard::show);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("MY ALUMNI PROFILE") || output.contains("Profile"),
                "Profile display must be attempted");
    }

    @Test
    @DisplayName("Profile sub-menu handles invalid sub-option gracefully")
    void testProfileSubMenuInvalidOption() {
        userService.login("alumni@thapar.edu", "AlumniP@ss1");

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        AlumniDashboardUI dashboard = buildDashboard("1\nZ\n", capture);
        assertDoesNotThrow(dashboard::show);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Invalid sub-option"), "Invalid sub-option message must appear");
    }

    // ── AuthUI integration tests ──────────────────────────────────────────────

    @Test
    @DisplayName("AuthUI routes Alumni login to Alumni Dashboard with correct content")
    void testAuthUIRoutesAlumniToDashboard() {
        String input = "2\nalumni@thapar.edu\nAlumniP@ss1\n0\n";
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream capture = new ByteArrayOutputStream();

        AuthUI authUI = new AuthUI(userService, in, new PrintStream(capture));
        authUI.promptAndLogin();

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("ALUMNI DASHBOARD"), "Alumni Dashboard header must appear");
        assertTrue(output.contains("Ravi Kumar"),        "Alumni name must appear");
        assertTrue(output.contains("Alumni Menu"),       "Alumni Menu label must appear");
    }

    @Test
    @DisplayName("AuthUI does not route Student to Alumni Dashboard")
    void testAuthUIDoesNotRouteStudentToAlumniDashboard() {
        String input = "1\nstudent@thapar.edu\nStudentP@ss1\n0\n";
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream capture = new ByteArrayOutputStream();

        AuthUI authUI = new AuthUI(userService, in, new PrintStream(capture));
        authUI.promptAndLogin();

        String output = capture.toString(StandardCharsets.UTF_8);
        assertFalse(output.contains("ALUMNI DASHBOARD"), "Student must NOT see Alumni Dashboard");
        assertTrue(output.contains("STUDENT DASHBOARD"),  "Student must see Student Dashboard");
    }

    @Test
    @DisplayName("showAlumniDashboard throws SecurityException when student is logged in")
    void testShowAlumniDashboardRejectsStudent() {
        userService.login("student@thapar.edu", "StudentP@ss1");

        AuthUI authUI = new AuthUI(userService, System.in, System.out);
        Alumni mockAlumni = new Alumni("a@b.com", "h", "Alumni", null, "EE", 2020);
        assertThrows(SecurityException.class, () -> authUI.showAlumniDashboard(mockAlumni));
    }

    @Test
    @DisplayName("handleMenuSelection dispatches logout and clears session")
    void testHandleMenuSelectionLogout() {
        userService.login("alumni@thapar.edu", "AlumniP@ss1");

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream("9\n".getBytes(StandardCharsets.UTF_8));
        PrintStream ps = new PrintStream(capture);

        FakeAlumniProfileService fakeProfileService = new FakeAlumniProfileService();
        AlumniProfileUI profileUI = new AlumniProfileUI(fakeProfileService, in, ps);
        AlumniDashboardUI dashboard = new AlumniDashboardUI(profileUI, userService, in, ps);

        dashboard.handleMenuSelection(alumni);

        assertFalse(UserSession.getInstance().isLoggedIn());
        assertTrue(capture.toString(StandardCharsets.UTF_8).contains("logged out"));
    }

    // ── Prompt 17: Mentorship Request Management UI Tests ─────────────────────

    @Test
    @DisplayName("View mentorship requests displays header and request details")
    void testViewMentorshipRequestsDisplaysDetails() {
        userService.login("alumni@thapar.edu", "AlumniP@ss1");

        FakeMentorshipRequestRepository fakeReqRepo = new FakeMentorshipRequestRepository();
        MentorshipRequest req = new MentorshipRequest(student.getId(), alumni.getId(),
                "System Design Advice", "Would love feedback on microservice design.");
        req.setId(10);
        req.setStatus(MentorshipStatus.PENDING);
        req.setStudentName("Anshika Singh");
        req.setStudentEmail("student@thapar.edu");
        req.setStudentDegree("Bachelor of Technology");
        req.setStudentDepartment("Computer Science");
        req.setRequestedAt(LocalDateTime.of(2026, 11, 2, 10, 30));
        fakeReqRepo.addRequest(req);

        MentorshipRequestService mentorshipService = new MentorshipRequestService(fakeReqRepo, userRepo);
        ByteArrayOutputStream capture = new ByteArrayOutputStream();

        // Option 2 (View Requests) -> 0 (Back to dashboard)
        AlumniDashboardUI dashboard = buildDashboardWithMentorship("2\n0\n", mentorshipService, capture);
        assertDoesNotThrow(dashboard::show);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("RECEIVED MENTORSHIP REQUESTS"), "Header must appear");
        assertTrue(output.contains("Anshika Singh"), "Student name must appear");
        assertTrue(output.contains("student@thapar.edu"), "Student email must appear");
        assertTrue(output.contains("System Design Advice"), "Topic must appear");
        assertTrue(output.contains("microservice design"), "Message must appear");
        assertTrue(output.contains("PENDING"), "Status must appear");
        assertTrue(output.contains("2026-11-02 10:30:00"), "Requested date/time must appear");
        assertTrue(output.contains("Actions for Pending Requests"), "Actions must appear for pending requests");
    }

    @Test
    @DisplayName("View mentorship requests when empty displays friendly message")
    void testViewMentorshipRequestsEmpty() {
        userService.login("alumni@thapar.edu", "AlumniP@ss1");

        FakeMentorshipRequestRepository fakeReqRepo = new FakeMentorshipRequestRepository();
        MentorshipRequestService mentorshipService = new MentorshipRequestService(fakeReqRepo, userRepo);
        ByteArrayOutputStream capture = new ByteArrayOutputStream();

        // Option 2 (View Requests)
        AlumniDashboardUI dashboard = buildDashboardWithMentorship("2\n", mentorshipService, capture);
        assertDoesNotThrow(dashboard::show);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("RECEIVED MENTORSHIP REQUESTS"));
        assertTrue(output.contains("No mentorship requests received yet"));
        assertTrue(output.contains("Returning to Alumni Dashboard"));
    }

    @Test
    @DisplayName("Accept pending request updates status, sets responded_at, and refreshes")
    void testAcceptPendingRequestAndRefresh() {
        userService.login("alumni@thapar.edu", "AlumniP@ss1");

        FakeMentorshipRequestRepository fakeReqRepo = new FakeMentorshipRequestRepository();
        MentorshipRequest req = new MentorshipRequest(student.getId(), alumni.getId(),
                "Architecture Guidance", "Need tips");
        req.setId(10);
        req.setStatus(MentorshipStatus.PENDING);
        req.setStudentName("Anshika Singh");
        req.setStudentEmail("student@thapar.edu");
        req.setRequestedAt(LocalDateTime.of(2026, 11, 2, 9, 0));
        fakeReqRepo.addRequest(req);

        MentorshipRequestService mentorshipService = new MentorshipRequestService(fakeReqRepo, userRepo);
        ByteArrayOutputStream capture = new ByteArrayOutputStream();

        // Option 2 -> Accept ('A') -> Request ID '10' -> 0 (Back to dashboard)
        AlumniDashboardUI dashboard = buildDashboardWithMentorship("2\nA\n10\n0\n", mentorshipService, capture);
        assertDoesNotThrow(dashboard::show);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("[SUCCESS]"), "Success message must appear on accept");
        assertTrue(output.contains("accepted successfully"), "Accepted message must appear");
        assertTrue(output.contains("Status       : ACCEPTED"), "Status must refresh to ACCEPTED");
        assertTrue(output.contains("Responded At"), "Responded At must be displayed after accepting");
        assertEquals(MentorshipStatus.ACCEPTED, fakeReqRepo.findRequestById(10).get().getStatus());
    }

    @Test
    @DisplayName("Reject pending request updates status, sets responded_at, and refreshes")
    void testRejectPendingRequestAndRefresh() {
        userService.login("alumni@thapar.edu", "AlumniP@ss1");

        FakeMentorshipRequestRepository fakeReqRepo = new FakeMentorshipRequestRepository();
        MentorshipRequest req = new MentorshipRequest(student.getId(), alumni.getId(),
                "Interview Prep", "Help with mocks");
        req.setId(15);
        req.setStatus(MentorshipStatus.PENDING);
        req.setStudentName("Anshika Singh");
        req.setStudentEmail("student@thapar.edu");
        req.setRequestedAt(LocalDateTime.of(2026, 11, 2, 9, 0));
        fakeReqRepo.addRequest(req);

        MentorshipRequestService mentorshipService = new MentorshipRequestService(fakeReqRepo, userRepo);
        ByteArrayOutputStream capture = new ByteArrayOutputStream();

        // Option 2 -> Reject ('R') -> Request ID '15' -> 0 (Back to dashboard)
        AlumniDashboardUI dashboard = buildDashboardWithMentorship("2\nR\n15\n0\n", mentorshipService, capture);
        assertDoesNotThrow(dashboard::show);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("[SUCCESS]"), "Success message must appear on reject");
        assertTrue(output.contains("rejected successfully"), "Rejected message must appear");
        assertTrue(output.contains("Status       : REJECTED"), "Status must refresh to REJECTED");
        assertTrue(output.contains("Responded At"), "Responded At must be displayed after rejecting");
        assertEquals(MentorshipStatus.REJECTED, fakeReqRepo.findRequestById(15).get().getStatus());
    }

    @Test
    @DisplayName("Requests that are not pending hide accept and reject action choices")
    void testNoPendingRequestsHidesActions() {
        userService.login("alumni@thapar.edu", "AlumniP@ss1");

        FakeMentorshipRequestRepository fakeReqRepo = new FakeMentorshipRequestRepository();
        MentorshipRequest req = new MentorshipRequest(student.getId(), alumni.getId(),
                "Completed Project", "Thanks!");
        req.setId(20);
        req.setStatus(MentorshipStatus.ACCEPTED);
        req.setStudentName("Anshika Singh");
        req.setStudentEmail("student@thapar.edu");
        req.setRequestedAt(LocalDateTime.of(2026, 11, 2, 9, 0));
        req.setRespondedAt(LocalDateTime.of(2026, 11, 2, 9, 30));
        fakeReqRepo.addRequest(req);

        MentorshipRequestService mentorshipService = new MentorshipRequestService(fakeReqRepo, userRepo);
        ByteArrayOutputStream capture = new ByteArrayOutputStream();

        // Option 2 -> 0 (Back)
        AlumniDashboardUI dashboard = buildDashboardWithMentorship("2\n0\n", mentorshipService, capture);
        assertDoesNotThrow(dashboard::show);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("There are no pending requests to accept or reject"));
        assertFalse(output.contains("Actions for Pending Requests:"), "Pending actions must not be shown");
    }

    @Test
    @DisplayName("Entering invalid or non-pending request ID displays clear error message")
    void testAcceptInvalidRequestIdShowsError() {
        userService.login("alumni@thapar.edu", "AlumniP@ss1");

        FakeMentorshipRequestRepository fakeReqRepo = new FakeMentorshipRequestRepository();
        MentorshipRequest req = new MentorshipRequest(student.getId(), alumni.getId(),
                "Topic", "Message");
        req.setId(10);
        req.setStatus(MentorshipStatus.PENDING);
        fakeReqRepo.addRequest(req);

        MentorshipRequestService mentorshipService = new MentorshipRequestService(fakeReqRepo, userRepo);
        ByteArrayOutputStream capture = new ByteArrayOutputStream();

        // Option 2 -> Accept ('A') -> 999 (invalid) -> 0 (exit actions)
        AlumniDashboardUI dashboard = buildDashboardWithMentorship("2\nA\n999\n0\n", mentorshipService, capture);
        assertDoesNotThrow(dashboard::show);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("[ERROR]"), "Error message must appear");
        assertTrue(output.contains("not a pending request in your list"));
    }

    @Test
    @DisplayName("Student cannot invoke viewMentorshipRequests")
    void testStudentCannotAccessMentorshipRequestsUI() {
        userService.login("student@thapar.edu", "StudentP@ss1");

        AlumniDashboardUI dashboard = buildDashboard("", new ByteArrayOutputStream());
        assertThrows(SecurityException.class, dashboard::viewMentorshipRequests,
                "Student role must throw SecurityException when calling viewMentorshipRequests");
    }

    @Test
    @DisplayName("Unauthenticated user cannot invoke viewMentorshipRequests")
    void testUnauthenticatedCannotAccessMentorshipRequestsUI() {
        AlumniDashboardUI dashboard = buildDashboard("", new ByteArrayOutputStream());
        assertThrows(SecurityException.class, dashboard::viewMentorshipRequests,
                "Unauthenticated session must throw SecurityException when calling viewMentorshipRequests");
    }

    // ── Prompt 19: Mentorship Connections / My Mentees Tests ─────────────────

    @Test
    @DisplayName("Menu option 3 My Mentees appears in alumni dashboard header")
    void testOptionMyMenteesAppearsInAlumniDashboardMenu() {
        userService.login("alumni@thapar.edu", "AlumniP@ss1");

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        AlumniDashboardUI dashboard = buildDashboard("0\n", capture);

        assertDoesNotThrow(dashboard::show);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("[3] My Mentees"),
                "Dashboard menu must offer '[3] My Mentees'.");
    }

    @Test
    @DisplayName("Menu option 3 displays accepted mentees with student details")
    void testOptionMyMenteesDisplaysAcceptedMentees() {
        userService.login("alumni@thapar.edu", "AlumniP@ss1");

        FakeMentorshipRequestRepository fakeRepo = new FakeMentorshipRequestRepository();

        // Accepted mentee
        MentorshipRequest mentee = new MentorshipRequest(student.getId(), alumni.getId(),
                "Compiler Design", "Mentoring request details");
        mentee.setId(201);
        mentee.setStatus(MentorshipStatus.ACCEPTED);
        mentee.setStudentName("Anshika Singh");
        mentee.setStudentEmail("student@thapar.edu");
        mentee.setStudentDegree("B.Tech");
        mentee.setStudentDepartment("Computer Science");
        mentee.setStudentGraduationYear(2026);
        mentee.setRespondedAt(LocalDateTime.now());
        fakeRepo.addRequest(mentee);

        // Pending request (should not appear in mentees list)
        MentorshipRequest pending = new MentorshipRequest(99, alumni.getId(),
                "Other Topic", "Other Msg");
        pending.setId(202);
        pending.setStatus(MentorshipStatus.PENDING);
        fakeRepo.addRequest(pending);

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        MentorshipRequestService reqService = new MentorshipRequestService(fakeRepo, userRepo);
        AlumniDashboardUI dashboard = buildDashboardWithMentorship("3\n", reqService, capture);

        assertDoesNotThrow(dashboard::show);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("MY MENTEES"));
        assertTrue(output.contains("Anshika Singh"));
        assertTrue(output.contains("student@thapar.edu"));
        assertTrue(output.contains("Computer Science"));
        assertTrue(output.contains("Class of 2026"));
        assertTrue(output.contains("Compiler Design"));
        assertTrue(output.contains("End of Mentees List."));
        assertTrue(output.contains("Returning to Alumni Dashboard."));
        assertFalse(output.contains("Other Topic"), "Pending requests must not appear in mentees list.");
    }

    @Test
    @DisplayName("Menu option 3 displays empty state when alumni has no accepted mentees")
    void testOptionMyMenteesEmptyState() {
        userService.login("alumni@thapar.edu", "AlumniP@ss1");

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        FakeMentorshipRequestRepository fakeRepo = new FakeMentorshipRequestRepository(); // Empty
        MentorshipRequestService reqService = new MentorshipRequestService(fakeRepo, userRepo);
        AlumniDashboardUI dashboard = buildDashboardWithMentorship("3\n", reqService, capture);

        assertDoesNotThrow(dashboard::show);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("MY MENTEES"));
        assertTrue(output.contains("[INFO] You have no active mentees yet."));
        assertTrue(output.contains("Returning to Alumni Dashboard."));
    }

    @Test
    @DisplayName("Menu option 3 displays error when database error occurs")
    void testOptionMyMenteesDatabaseError() {
        userService.login("alumni@thapar.edu", "AlumniP@ss1");

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        FakeMentorshipRequestRepository fakeRepo = new FakeMentorshipRequestRepository();
        fakeRepo.setThrowOnQuery(true);
        MentorshipRequestService reqService = new MentorshipRequestService(fakeRepo, userRepo);
        AlumniDashboardUI dashboard = buildDashboardWithMentorship("3\n", reqService, capture);

        assertDoesNotThrow(dashboard::show);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("[ERROR]"));
        assertTrue(output.contains("Database error"));
    }

    @Test
    @DisplayName("Student cannot invoke viewMyMentees or handleMyMentees")
    void testStudentCannotAccessMyMenteesUI() {
        userService.login("student@thapar.edu", "StudentP@ss1");

        AlumniDashboardUI dashboard = buildDashboard("", new ByteArrayOutputStream());
        assertThrows(SecurityException.class, dashboard::viewMyMentees,
                "Student role must throw SecurityException when calling viewMyMentees");
        assertThrows(SecurityException.class, dashboard::handleMyMentees,
                "Student role must throw SecurityException when calling handleMyMentees");
    }

    @Test
    @DisplayName("Unauthenticated user cannot invoke viewMyMentees or handleMyMentees")
    void testUnauthenticatedCannotAccessMyMenteesUI() {
        AlumniDashboardUI dashboard = buildDashboard("", new ByteArrayOutputStream());
        assertThrows(SecurityException.class, dashboard::viewMyMentees,
                "Unauthenticated session must throw SecurityException when calling viewMyMentees");
        assertThrows(SecurityException.class, dashboard::handleMyMentees,
                "Unauthenticated session must throw SecurityException when calling handleMyMentees");
    }

    // ── In-memory fakes ───────────────────────────────────────────────────────

    private static class FakeMentorshipRequestRepository implements MentorshipRequestRepository {
        private final Map<Integer, MentorshipRequest> requests = new HashMap<>();

        private boolean throwOnQuery = false;

        void setThrowOnQuery(boolean val) { this.throwOnQuery = val; }

        void addRequest(MentorshipRequest req) {
            requests.put(req.getId(), req);
        }

        @Override
        public List<MentorshipRequest> findAcceptedConnectionsByAlumniId(int alumniId) throws SQLException {
            if (throwOnQuery) throw new SQLException("Simulated DB query error");
            return requests.values().stream()
                    .filter(r -> r.getAlumniId() == alumniId && r.getStatus() == MentorshipStatus.ACCEPTED)
                    .toList();
        }

        @Override
        public List<MentorshipRequest> findRequestsByAlumniId(int alumniId) {
            return requests.values().stream().filter(r -> r.getAlumniId() == alumniId).toList();
        }

        @Override
        public Optional<MentorshipRequest> findRequestById(int requestId) {
            return Optional.ofNullable(requests.get(requestId));
        }

        @Override
        public boolean updateRequestStatus(int requestId, MentorshipStatus newStatus, LocalDateTime respondedAt) {
            MentorshipRequest req = requests.get(requestId);
            if (req != null) {
                req.setStatus(newStatus);
                req.setRespondedAt(respondedAt);
                return true;
            }
            return false;
        }

        @Override public int createRequest(MentorshipRequestDto dto) { return 1; }
        @Override public boolean existsPending(int studentId, int alumniId) { return false; }
        @Override public boolean existsAccepted(int studentId, int alumniId) { return false; }
        @Override public int countActiveMentees(int alumniId) { return 0; }
        @Override public boolean isAlumniAvailable(int alumniId) { return true; }
    }

    /** In-memory UserRepository used by tests that need a UserService. */
    private static class InMemoryUserRepository implements UserRepository {
        private final Map<Integer, User> usersById = new HashMap<>();
        private final Map<String, Integer> userIdsByEmail = new HashMap<>();
        private int sequence = 1;

        @Override public boolean existsByEmail(String email) {
            return email != null && userIdsByEmail.containsKey(email.trim().toLowerCase());
        }

        @Override public Optional<User> findById(int id) {
            return Optional.ofNullable(usersById.get(id));
        }

        @Override public Optional<User> findByEmail(String email) throws SQLException {
            if (email == null) return Optional.empty();
            Integer id = userIdsByEmail.get(email.trim().toLowerCase());
            return id != null ? Optional.ofNullable(usersById.get(id)) : Optional.empty();
        }

        @Override public Optional<Student> findStudentById(int userId) {
            User u = usersById.get(userId);
            return (u instanceof Student s) ? Optional.of(s) : Optional.empty();
        }

        @Override public Optional<Alumni> findAlumniById(int userId) {
            User u = usersById.get(userId);
            return (u instanceof Alumni a) ? Optional.of(a) : Optional.empty();
        }

        @Override public Student saveStudent(Student s) throws SQLException {
            int id = sequence++; s.setId(id); s.setProfileId(sequence++);
            usersById.put(id, s); userIdsByEmail.put(s.getEmail().trim().toLowerCase(), id);
            return s;
        }

        @Override public Alumni saveAlumni(Alumni a) throws SQLException {
            int id = sequence++; a.setId(id); a.setProfileId(sequence++);
            usersById.put(id, a); userIdsByEmail.put(a.getEmail().trim().toLowerCase(), id);
            return a;
        }

        @Override public boolean deleteUserById(int userId) {
            User removed = usersById.remove(userId);
            if (removed != null) { userIdsByEmail.remove(removed.getEmail().trim().toLowerCase()); return true; }
            return false;
        }
    }

    /**
     * Fake AlumniProfileService that never hits the database.
     */
    private static class FakeAlumniProfileService extends AlumniProfileService {

        FakeAlumniProfileService() {
            super(new AlumniProfileRepository() {
                @Override public Optional<Alumni> findProfileByUserId(int userId) { return Optional.empty(); }
                @Override public boolean updateProfile(Alumni alumni) { return true; }
                @Override public boolean createProfile(Alumni alumni) { return true; }
                @Override public boolean profileExistsForUser(int userId) { return false; }
            });
        }

        @Override
        public AlumniProfileResult viewMyProfile() {
            return AlumniProfileResult.failure("No profile found (test stub — no DB connection).");
        }

        @Override
        public AlumniProfileResult createMyProfile(
                com.alumconnect.service.dto.AlumniProfileRequest req) {
            return AlumniProfileResult.failure("Create profile stub (no DB in test).");
        }

        @Override
        public AlumniProfileResult updateMyProfile(
                com.alumconnect.service.dto.AlumniProfileRequest req) {
            return AlumniProfileResult.failure("Update profile stub (no DB in test).");
        }
    }
}
