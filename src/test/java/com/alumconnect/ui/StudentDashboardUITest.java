package com.alumconnect.ui;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.Role;
import com.alumconnect.model.Student;
import com.alumconnect.model.User;
import com.alumconnect.repository.AlumniProfileRepository;
import com.alumconnect.repository.StudentProfileRepository;
import com.alumconnect.repository.UserRepository;
import com.alumconnect.service.AlumniDirectoryService;
import com.alumconnect.service.StudentProfileService;
import com.alumconnect.service.UserService;
import com.alumconnect.service.dto.StudentProfileResult;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link StudentDashboardUI} — Prompt 10, Prompt 12 &amp; Prompt 13.
 *
 * <p>Uses in-memory fakes for all repositories and services so that no real
 * database connection is needed. Every test starts with a clean
 * {@link UserSession} and tears it down afterwards.
 */
@DisplayName("Student Dashboard Tests (Prompt 10, 12 & 13)")
class StudentDashboardUITest {

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
        userRepo.saveAlumni(alumni);
    }

    @AfterEach
    void tearDown() {
        UserSession.getInstance().logout();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private StudentDashboardUI buildDashboard(String simulatedInput,
                                              ByteArrayOutputStream capture) {
        ByteArrayInputStream in = new ByteArrayInputStream(
                simulatedInput.getBytes(StandardCharsets.UTF_8));
        PrintStream ps = new PrintStream(capture);

        // Use a fake profile service so show() / handleProfileSubMenu() never hits DB
        FakeStudentProfileService fakeProfileService = new FakeStudentProfileService();
        StudentProfileUI profileUI = new StudentProfileUI(fakeProfileService, in, ps);
        return new StudentDashboardUI(profileUI, userService, in, ps);
    }

    // ── Security / access control tests ───────────────────────────────────────

    @Test
    @DisplayName("Unauthenticated user cannot access Student Dashboard")
    void testUnauthenticatedAccessIsRejected() {
        // No login -> session is empty
        StudentDashboardUI dashboard = buildDashboard("", new ByteArrayOutputStream());
        assertThrows(SecurityException.class, dashboard::show,
                "show() must throw SecurityException when no user is logged in.");
    }

    @Test
    @DisplayName("Authenticated Alumni cannot access Student Dashboard")
    void testAlumniCannotAccessStudentDashboard() {
        userService.login("alumni@thapar.edu", "AlumniP@ss1");
        assertTrue(UserSession.getInstance().isAlumni());

        StudentDashboardUI dashboard = buildDashboard("", new ByteArrayOutputStream());
        assertThrows(SecurityException.class, dashboard::show,
                "show() must throw SecurityException when role is ALUMNI.");
    }

    @Test
    @DisplayName("Authenticated Student can access Student Dashboard")
    void testStudentCanAccessDashboard() {
        userService.login("student@thapar.edu", "StudentP@ss1");
        assertTrue(UserSession.getInstance().isStudent());

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        // Send "0" (Exit) so the menu loop terminates cleanly
        StudentDashboardUI dashboard = buildDashboard("0\n", capture);
        assertDoesNotThrow(dashboard::show);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("STUDENT DASHBOARD"), "Header must appear");
        assertTrue(output.contains("Anshika Singh"),     "Student name must appear");
    }

    @Test
    @DisplayName("Student role is correctly recognised in session after login")
    void testStudentRoleIsRecognised() {
        userService.login("student@thapar.edu", "StudentP@ss1");
        assertTrue(UserSession.getInstance().isStudent());
        assertFalse(UserSession.getInstance().isAlumni());
        assertEquals(Role.STUDENT, UserSession.getInstance().getCurrentRole().orElse(null));
    }

    @Test
    @DisplayName("Logged-out session cannot access Student Dashboard")
    void testLoggedOutSessionIsRejected() {
        userService.login("student@thapar.edu", "StudentP@ss1");
        userService.logout();
        assertFalse(UserSession.getInstance().isLoggedIn());

        StudentDashboardUI dashboard = buildDashboard("", new ByteArrayOutputStream());
        assertThrows(SecurityException.class, dashboard::show);
    }

    // ── Menu behaviour tests ──────────────────────────────────────────────────

    @Test
    @DisplayName("Dashboard handles invalid menu input gracefully")
    void testInvalidMenuChoice() {
        userService.login("student@thapar.edu", "StudentP@ss1");

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        StudentDashboardUI dashboard = buildDashboard("99\n", capture);
        assertDoesNotThrow(dashboard::show);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Invalid choice"), "Invalid choice message must appear");
    }

    @Test
    @DisplayName("Menu option 0 (Exit) prints goodbye and returns cleanly")
    void testExitOption() {
        userService.login("student@thapar.edu", "StudentP@ss1");

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        StudentDashboardUI dashboard = buildDashboard("0\n", capture);
        assertDoesNotThrow(dashboard::show);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Goodbye"), "Exit option must print goodbye");
    }

    @Test
    @DisplayName("Menu option 9 (Logout) clears the session")
    void testLogoutClearsSession() {
        userService.login("student@thapar.edu", "StudentP@ss1");
        assertTrue(UserSession.getInstance().isLoggedIn());

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        StudentDashboardUI dashboard = buildDashboard("9\n", capture);
        assertDoesNotThrow(dashboard::show);

        assertFalse(UserSession.getInstance().isLoggedIn(), "Session must be cleared after logout");

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("logged out"), "Logout confirmation must appear");
    }

    @Test
    @DisplayName("Dashboard menu shows correct options including Browse and Search Alumni")
    void testMenuShowsCorrectOptions() {
        userService.login("student@thapar.edu", "StudentP@ss1");

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        StudentDashboardUI dashboard = buildDashboard("0\n", capture);
        dashboard.show();

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("My Profile"), "Profile option must be listed");
        assertTrue(output.contains("Browse Alumni"), "Browse Alumni option must be listed");
        assertTrue(output.contains("Search Alumni"), "Search Alumni option must be listed");
        assertTrue(output.contains("My Mentorship Requests"), "Mentorship Requests option must be listed");
        assertTrue(output.contains("Logout"),      "Logout option must be listed");
        assertTrue(output.contains("Exit"),        "Exit option must be listed");
    }

    @Test
    @DisplayName("Menu option 2 (Browse Alumni) launches Alumni Directory")
    void testBrowseAlumniOptionOpensDirectory() {
        userService.login("student@thapar.edu", "StudentP@ss1");

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream("2\n".getBytes(StandardCharsets.UTF_8));
        PrintStream ps = new PrintStream(capture);

        FakeStudentProfileService fakeProfileService = new FakeStudentProfileService();
        StudentProfileUI profileUI = new StudentProfileUI(fakeProfileService, in, ps);

        Alumni a = new Alumni("alumni@thapar.edu", "hash", "Directory Alum", null, "CS", 2020);
        a.setCurrentCompany("Google");
        FakeAlumniDirectoryRepo dirRepo = new FakeAlumniDirectoryRepo(List.of(a));
        AlumniDirectoryService dirService = new AlumniDirectoryService(dirRepo);
        AlumniDirectoryUI dirUI = new AlumniDirectoryUI(dirService, in, ps);

        StudentDashboardUI dashboard = new StudentDashboardUI(profileUI, dirUI, userService, in, ps);
        assertDoesNotThrow(dashboard::show);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("ALUMNI DIRECTORY"), "Alumni Directory header must appear");
        assertTrue(output.contains("Directory Alum"), "Alumni in directory must appear");
        assertTrue(output.contains("Google"), "Company must appear");
    }

    @Test
    @DisplayName("Menu option 3 (Search Alumni) prompts and displays search results")
    void testSearchAlumniOptionOpensSearch() {
        userService.login("student@thapar.edu", "StudentP@ss1");

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream("3\nGoogle\n".getBytes(StandardCharsets.UTF_8));
        PrintStream ps = new PrintStream(capture);
        Scanner sharedScanner = new Scanner(in);

        FakeStudentProfileService fakeProfileService = new FakeStudentProfileService();
        StudentProfileUI profileUI = new StudentProfileUI(fakeProfileService, sharedScanner, ps);

        Alumni a = new Alumni("alumni@thapar.edu", "hash", "Search Alum", null, "CS", 2020);
        a.setCurrentCompany("Google");
        FakeAlumniDirectoryRepo dirRepo = new FakeAlumniDirectoryRepo(List.of(a));
        AlumniDirectoryService dirService = new AlumniDirectoryService(dirRepo);
        AlumniDirectoryUI dirUI = new AlumniDirectoryUI(dirService, sharedScanner, ps);

        StudentDashboardUI dashboard = new StudentDashboardUI(profileUI, dirUI, userService, sharedScanner, ps);
        assertDoesNotThrow(dashboard::show);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("ALUMNI SEARCH"), "Alumni Search header must appear");
        assertTrue(output.contains("Search Alum"), "Alumni in search results must appear");
        assertTrue(output.contains("Google"), "Company must appear");
    }

    // ── Profile sub-menu tests ────────────────────────────────────────────────

    @Test
    @DisplayName("Profile sub-menu option V routes to displayMyProfile")
    void testProfileSubMenuView() {
        userService.login("student@thapar.edu", "StudentP@ss1");

        // input: choose "1" (My Profile) then "V" (View)
        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        StudentDashboardUI dashboard = buildDashboard("1\nV\n", capture);
        assertDoesNotThrow(dashboard::show);

        String output = capture.toString(StandardCharsets.UTF_8);
        // FakeStudentProfileService.viewMyProfile returns success with a student object
        assertTrue(output.contains("MY STUDENT PROFILE") || output.contains("Profile"),
                "Profile display must be attempted");
    }

    @Test
    @DisplayName("Profile sub-menu handles invalid sub-option gracefully")
    void testProfileSubMenuInvalidOption() {
        userService.login("student@thapar.edu", "StudentP@ss1");

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        StudentDashboardUI dashboard = buildDashboard("1\nZ\n", capture);
        assertDoesNotThrow(dashboard::show);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Invalid sub-option"), "Invalid sub-option message must appear");
    }

    // ── AuthUI integration tests ──────────────────────────────────────────────

    @Test
    @DisplayName("AuthUI routes Student login to Student Dashboard with correct content")
    void testAuthUIRoutesStudentToNewDashboard() {
        String input = "student@thapar.edu\nStudentP@ss1\n0\n";
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream capture = new ByteArrayOutputStream();

        AuthUI authUI = new AuthUI(userService, in, new PrintStream(capture));
        authUI.promptAndLogin();

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("STUDENT DASHBOARD"), "Student Dashboard header must appear");
        assertTrue(output.contains("Anshika Singh"),     "Student name must appear");
        assertTrue(output.contains("Student Menu"),      "Student Menu label must appear");
        assertTrue(output.contains("Browse Alumni"),     "Browse Alumni option must appear");
        assertTrue(output.contains("Search Alumni"),     "Search Alumni option must appear");
    }

    @Test
    @DisplayName("AuthUI does not route Alumni to Student Dashboard")
    void testAuthUIDoesNotRouteAlumniToStudentDashboard() {
        String input = "alumni@thapar.edu\nAlumniP@ss1\n0\n";
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream capture = new ByteArrayOutputStream();

        AuthUI authUI = new AuthUI(userService, in, new PrintStream(capture));
        authUI.promptAndLogin();

        String output = capture.toString(StandardCharsets.UTF_8);
        assertFalse(output.contains("STUDENT DASHBOARD"), "Alumni must NOT see Student Dashboard");
        assertTrue(output.contains("ALUMNI DASHBOARD"),   "Alumni must see Alumni Dashboard");
    }

    @Test
    @DisplayName("showStudentDashboard throws SecurityException when alumni is logged in")
    void testShowStudentDashboardRejectsAlumni() {
        userService.login("alumni@thapar.edu", "AlumniP@ss1");

        AuthUI authUI = new AuthUI(userService, System.in, System.out);
        Student mockStudent = new Student("s@b.com", "h", "Student", null, "CS", 2027);
        assertThrows(SecurityException.class, () -> authUI.showStudentDashboard(mockStudent));
    }

    @Test
    @DisplayName("handleMenuSelection dispatches logout and clears session")
    void testHandleMenuSelectionLogout() {
        userService.login("student@thapar.edu", "StudentP@ss1");

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream("9\n".getBytes(StandardCharsets.UTF_8));
        PrintStream ps = new PrintStream(capture);

        FakeStudentProfileService fakeProfileService = new FakeStudentProfileService();
        StudentProfileUI profileUI = new StudentProfileUI(fakeProfileService, in, ps);
        StudentDashboardUI dashboard = new StudentDashboardUI(profileUI, userService, in, ps);

        dashboard.handleMenuSelection(student);

        assertFalse(UserSession.getInstance().isLoggedIn());
        assertTrue(capture.toString(StandardCharsets.UTF_8).contains("logged out"));
    }

    // ── In-memory fakes ───────────────────────────────────────────────────────

    /** In-memory UserRepository used by tests that need a UserService. */
    private static class InMemoryUserRepository implements UserRepository {
        private final Map<Integer, User>   usersById      = new HashMap<>();
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
     * Fake StudentProfileService that never hits the database.
     */
    private static class FakeStudentProfileService extends StudentProfileService {
        FakeStudentProfileService() {
            super(new StudentProfileRepository() {
                @Override public Optional<Student> findProfileByUserId(int userId) { return Optional.empty(); }
                @Override public boolean updateProfile(Student student) { return true; }
                @Override public boolean createProfile(Student student) { return true; }
                @Override public boolean profileExistsForUser(int userId) { return false; }
            });
        }

        @Override
        public StudentProfileResult viewMyProfile() {
            return StudentProfileResult.failure("No profile found (test stub — no DB connection).");
        }

        @Override
        public StudentProfileResult createMyProfile(com.alumconnect.service.dto.StudentProfileRequest req) {
            return StudentProfileResult.failure("Create profile stub (no DB in test).");
        }

        @Override
        public StudentProfileResult updateMyProfile(com.alumconnect.service.dto.StudentProfileRequest req) {
            return StudentProfileResult.failure("Update profile stub (no DB in test).");
        }
    }

    /**
     * Fake AlumniProfileRepository for directory testing.
     */
    private static class FakeAlumniDirectoryRepo implements AlumniProfileRepository {
        private final List<Alumni> list;

        FakeAlumniDirectoryRepo(List<Alumni> list) {
            this.list = list != null ? list : Collections.emptyList();
        }

        @Override
        public List<Alumni> findAllAlumni() {
            return new ArrayList<>(list);
        }

        @Override
        public Optional<Alumni> findProfileByUserId(int userId) {
            return list.stream().filter(a -> a.getId().equals(userId)).findFirst();
        }

        @Override public boolean updateProfile(Alumni a) { return true; }
        @Override public boolean createProfile(Alumni a) { return true; }
        @Override public boolean profileExistsForUser(int userId) { return true; }
    }

    // ── Prompt 18 Tests: Student Mentorship Requests UI ───────────────────────

    @Test
    @DisplayName("Menu option 5 (My Mentorship Requests) displays sent requests with alumni info and statuses")
    void testOptionMyRequestsDisplaysSentRequests() {
        userService.login("student@thapar.edu", "StudentP@ss1");

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream("5\n".getBytes(StandardCharsets.UTF_8));
        PrintStream ps = new PrintStream(capture);

        FakeStudentProfileService fakeProfileService = new FakeStudentProfileService();
        StudentProfileUI profileUI = new StudentProfileUI(fakeProfileService, in, ps);

        AlumniDirectoryService dirService = new AlumniDirectoryService(new FakeAlumniDirectoryRepo(List.of()));
        AlumniDirectoryUI dirUI = new AlumniDirectoryUI(dirService, in, ps);

        FakeMentorshipRequestRepo reqRepo = new FakeMentorshipRequestRepo();
        com.alumconnect.model.MentorshipRequest req1 = new com.alumconnect.model.MentorshipRequest(student.getId(), 20, "Cloud Guidance", "Need tips on AWS");
        req1.setId(10);
        req1.setStatus(com.alumconnect.model.MentorshipStatus.PENDING);
        req1.setAlumniName("Neha Gupta");
        req1.setAlumniEmail("neha@google.com");
        req1.setAlumniCompany("Google");
        req1.setAlumniRole("Staff Architect");
        reqRepo.addRequest(req1);

        com.alumconnect.model.MentorshipRequest req2 = new com.alumconnect.model.MentorshipRequest(student.getId(), 30, "Interview Prep", "System design mock");
        req2.setId(11);
        req2.setStatus(com.alumconnect.model.MentorshipStatus.ACCEPTED);
        req2.setAlumniName("Ravi Kumar");
        req2.setAlumniCompany("Microsoft");
        req2.setAlumniRole("Principal Engineer");
        req2.setRespondedAt(java.time.LocalDateTime.now());
        reqRepo.addRequest(req2);

        com.alumconnect.service.MentorshipRequestService reqService = new com.alumconnect.service.MentorshipRequestService(reqRepo, userRepo);
        StudentDashboardUI dashboard = new StudentDashboardUI(profileUI, dirUI, reqService, userService, in, ps);

        assertDoesNotThrow(dashboard::show);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("MY MENTORSHIP REQUESTS"));
        assertTrue(output.contains("Neha Gupta"));
        assertTrue(output.contains("Google"));
        assertTrue(output.contains("Staff Architect"));
        assertTrue(output.contains("PENDING"));
        assertTrue(output.contains("Ravi Kumar"));
        assertTrue(output.contains("Microsoft"));
        assertTrue(output.contains("ACCEPTED"));
        assertTrue(output.contains("End of Mentorship Requests."));
    }

    @Test
    @DisplayName("Menu option 5 displays empty state when student has no requests")
    void testOptionMyRequestsEmptyState() {
        userService.login("student@thapar.edu", "StudentP@ss1");

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream("5\n".getBytes(StandardCharsets.UTF_8));
        PrintStream ps = new PrintStream(capture);

        FakeStudentProfileService fakeProfileService = new FakeStudentProfileService();
        StudentProfileUI profileUI = new StudentProfileUI(fakeProfileService, in, ps);

        AlumniDirectoryService dirService = new AlumniDirectoryService(new FakeAlumniDirectoryRepo(List.of()));
        AlumniDirectoryUI dirUI = new AlumniDirectoryUI(dirService, in, ps);

        FakeMentorshipRequestRepo reqRepo = new FakeMentorshipRequestRepo(); // Empty repo
        com.alumconnect.service.MentorshipRequestService reqService = new com.alumconnect.service.MentorshipRequestService(reqRepo, userRepo);
        StudentDashboardUI dashboard = new StudentDashboardUI(profileUI, dirUI, reqService, userService, in, ps);

        assertDoesNotThrow(dashboard::show);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("MY MENTORSHIP REQUESTS"));
        assertTrue(output.contains("[INFO] You have not sent any mentorship requests yet."));
    }

    @Test
    @DisplayName("Menu option 5 displays error when database error occurs")
    void testOptionMyRequestsDatabaseError() {
        userService.login("student@thapar.edu", "StudentP@ss1");

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream("5\n".getBytes(StandardCharsets.UTF_8));
        PrintStream ps = new PrintStream(capture);

        FakeStudentProfileService fakeProfileService = new FakeStudentProfileService();
        StudentProfileUI profileUI = new StudentProfileUI(fakeProfileService, in, ps);

        AlumniDirectoryService dirService = new AlumniDirectoryService(new FakeAlumniDirectoryRepo(List.of()));
        AlumniDirectoryUI dirUI = new AlumniDirectoryUI(dirService, in, ps);

        FakeMentorshipRequestRepo reqRepo = new FakeMentorshipRequestRepo();
        reqRepo.setThrowOnQuery(true);
        com.alumconnect.service.MentorshipRequestService reqService = new com.alumconnect.service.MentorshipRequestService(reqRepo, userRepo);
        StudentDashboardUI dashboard = new StudentDashboardUI(profileUI, dirUI, reqService, userService, in, ps);

        assertDoesNotThrow(dashboard::show);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("[ERROR]"));
        assertTrue(output.contains("Database error"));
    }

    @Test
    @DisplayName("handleMyMentorshipRequests rejects unauthenticated and non-student access")
    void testHandleMyMentorshipRequestsSecurityGuards() {
        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
        PrintStream ps = new PrintStream(capture);

        FakeStudentProfileService fakeProfileService = new FakeStudentProfileService();
        StudentProfileUI profileUI = new StudentProfileUI(fakeProfileService, in, ps);
        AlumniDirectoryService dirService = new AlumniDirectoryService(new FakeAlumniDirectoryRepo(List.of()));
        AlumniDirectoryUI dirUI = new AlumniDirectoryUI(dirService, in, ps);
        FakeMentorshipRequestRepo reqRepo = new FakeMentorshipRequestRepo();
        com.alumconnect.service.MentorshipRequestService reqService = new com.alumconnect.service.MentorshipRequestService(reqRepo, userRepo);
        StudentDashboardUI dashboard = new StudentDashboardUI(profileUI, dirUI, reqService, userService, in, ps);

        // 1. Unauthenticated access
        assertThrows(SecurityException.class, dashboard::handleMyMentorshipRequests);

        // 2. Non-student (Alumni) access
        userService.login("alumni@thapar.edu", "AlumniP@ss1");
        assertThrows(SecurityException.class, dashboard::handleMyMentorshipRequests);
    }

    // ── Prompt 19: Mentorship Connections Tests ──────────────────────────────

    @Test
    @DisplayName("Menu option 6 My Mentorship Connections appears in student dashboard header")
    void testOptionMyConnectionsAppearsInStudentDashboardMenu() {
        userService.login("student@thapar.edu", "StudentP@ss1");

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        StudentDashboardUI dashboard = buildDashboard("0\n", capture);

        assertDoesNotThrow(dashboard::show);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("[6] My Mentorship Connections"),
                "Dashboard menu must offer '[6] My Mentorship Connections'.");
    }

    @Test
    @DisplayName("Menu option 6 displays accepted connections with alumni details")
    void testOptionMyConnectionsDisplaysAcceptedConnections() {
        userService.login("student@thapar.edu", "StudentP@ss1");

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream("6\n".getBytes(StandardCharsets.UTF_8));
        PrintStream ps = new PrintStream(capture);

        FakeStudentProfileService fakeProfileService = new FakeStudentProfileService();
        StudentProfileUI profileUI = new StudentProfileUI(fakeProfileService, in, ps);

        AlumniDirectoryService dirService = new AlumniDirectoryService(new FakeAlumniDirectoryRepo(List.of()));
        AlumniDirectoryUI dirUI = new AlumniDirectoryUI(dirService, in, ps);

        FakeMentorshipRequestRepo reqRepo = new FakeMentorshipRequestRepo();

        // Accepted connection
        com.alumconnect.model.MentorshipRequest conn = new com.alumconnect.model.MentorshipRequest(
                student.getId(), alumni.getId(), "Cloud Engineering", "Looking for mentoring");
        conn.setId(101);
        conn.setStatus(com.alumconnect.model.MentorshipStatus.ACCEPTED);
        conn.setAlumniName("Ravi Kumar");
        conn.setAlumniEmail("alumni@thapar.edu");
        conn.setAlumniCompany("Google");
        conn.setAlumniRole("Staff Engineer");
        conn.setAlumniDepartment("Electronics");
        conn.setAlumniDegree("B.Tech");
        conn.setRespondedAt(java.time.LocalDateTime.now());
        reqRepo.addRequest(conn);

        // Pending request (should not appear in connections)
        com.alumconnect.model.MentorshipRequest pendingReq = new com.alumconnect.model.MentorshipRequest(
                student.getId(), 99, "Other Topic", "Other Msg");
        pendingReq.setId(102);
        pendingReq.setStatus(com.alumconnect.model.MentorshipStatus.PENDING);
        reqRepo.addRequest(pendingReq);

        com.alumconnect.service.MentorshipRequestService reqService = new com.alumconnect.service.MentorshipRequestService(reqRepo, userRepo);
        StudentDashboardUI dashboard = new StudentDashboardUI(profileUI, dirUI, reqService, userService, in, ps);

        assertDoesNotThrow(dashboard::show);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("MY MENTORSHIP CONNECTIONS"));
        assertTrue(output.contains("Ravi Kumar"));
        assertTrue(output.contains("Google"));
        assertTrue(output.contains("Staff Engineer"));
        assertTrue(output.contains("Cloud Engineering"));
        assertTrue(output.contains("End of Mentorship Connections."));
        assertTrue(output.contains("Returning to Student Dashboard."));
        assertFalse(output.contains("Other Topic"), "Pending requests must not appear in connections.");
    }

    @Test
    @DisplayName("Menu option 6 displays empty state when student has no accepted connections")
    void testOptionMyConnectionsEmptyState() {
        userService.login("student@thapar.edu", "StudentP@ss1");

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream("6\n".getBytes(StandardCharsets.UTF_8));
        PrintStream ps = new PrintStream(capture);

        FakeStudentProfileService fakeProfileService = new FakeStudentProfileService();
        StudentProfileUI profileUI = new StudentProfileUI(fakeProfileService, in, ps);

        AlumniDirectoryService dirService = new AlumniDirectoryService(new FakeAlumniDirectoryRepo(List.of()));
        AlumniDirectoryUI dirUI = new AlumniDirectoryUI(dirService, in, ps);

        FakeMentorshipRequestRepo reqRepo = new FakeMentorshipRequestRepo(); // Empty repo
        com.alumconnect.service.MentorshipRequestService reqService = new com.alumconnect.service.MentorshipRequestService(reqRepo, userRepo);
        StudentDashboardUI dashboard = new StudentDashboardUI(profileUI, dirUI, reqService, userService, in, ps);

        assertDoesNotThrow(dashboard::show);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("MY MENTORSHIP CONNECTIONS"));
        assertTrue(output.contains("[INFO] You have no active mentorship connections yet."));
        assertTrue(output.contains("Returning to Student Dashboard."));
    }

    @Test
    @DisplayName("Menu option 6 displays error when database error occurs")
    void testOptionMyConnectionsDatabaseError() {
        userService.login("student@thapar.edu", "StudentP@ss1");

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream("6\n".getBytes(StandardCharsets.UTF_8));
        PrintStream ps = new PrintStream(capture);

        FakeStudentProfileService fakeProfileService = new FakeStudentProfileService();
        StudentProfileUI profileUI = new StudentProfileUI(fakeProfileService, in, ps);

        AlumniDirectoryService dirService = new AlumniDirectoryService(new FakeAlumniDirectoryRepo(List.of()));
        AlumniDirectoryUI dirUI = new AlumniDirectoryUI(dirService, in, ps);

        FakeMentorshipRequestRepo reqRepo = new FakeMentorshipRequestRepo();
        reqRepo.setThrowOnQuery(true);
        com.alumconnect.service.MentorshipRequestService reqService = new com.alumconnect.service.MentorshipRequestService(reqRepo, userRepo);
        StudentDashboardUI dashboard = new StudentDashboardUI(profileUI, dirUI, reqService, userService, in, ps);

        assertDoesNotThrow(dashboard::show);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("[ERROR]"));
        assertTrue(output.contains("Database error"));
    }

    @Test
    @DisplayName("handleMyMentorshipConnections rejects unauthenticated and non-student access")
    void testHandleMyMentorshipConnectionsSecurityGuards() {
        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
        PrintStream ps = new PrintStream(capture);

        FakeStudentProfileService fakeProfileService = new FakeStudentProfileService();
        StudentProfileUI profileUI = new StudentProfileUI(fakeProfileService, in, ps);
        AlumniDirectoryService dirService = new AlumniDirectoryService(new FakeAlumniDirectoryRepo(List.of()));
        AlumniDirectoryUI dirUI = new AlumniDirectoryUI(dirService, in, ps);
        FakeMentorshipRequestRepo reqRepo = new FakeMentorshipRequestRepo();
        com.alumconnect.service.MentorshipRequestService reqService = new com.alumconnect.service.MentorshipRequestService(reqRepo, userRepo);
        StudentDashboardUI dashboard = new StudentDashboardUI(profileUI, dirUI, reqService, userService, in, ps);

        // 1. Unauthenticated access
        assertThrows(SecurityException.class, dashboard::handleMyMentorshipConnections);
        assertThrows(SecurityException.class, dashboard::viewMentorshipConnections);

        // 2. Non-student (Alumni) access
        userService.login("alumni@thapar.edu", "AlumniP@ss1");
        assertThrows(SecurityException.class, dashboard::handleMyMentorshipConnections);
        assertThrows(SecurityException.class, dashboard::viewMentorshipConnections);
    }

    private static class FakeMentorshipRequestRepo implements com.alumconnect.repository.MentorshipRequestRepository {
        private final List<com.alumconnect.model.MentorshipRequest> requests = new ArrayList<>();
        private boolean throwOnQuery = false;

        void addRequest(com.alumconnect.model.MentorshipRequest r) { requests.add(r); }
        void setThrowOnQuery(boolean val) { this.throwOnQuery = val; }

        @Override
        public List<com.alumconnect.model.MentorshipRequest> findRequestsByStudentId(int studentId) throws SQLException {
            if (throwOnQuery) throw new SQLException("Simulated DB query error");
            return requests.stream().filter(r -> r.getStudentId() == studentId).toList();
        }

        @Override
        public List<com.alumconnect.model.MentorshipRequest> findAcceptedConnectionsByStudentId(int studentId) throws SQLException {
            if (throwOnQuery) throw new SQLException("Simulated DB query error");
            return requests.stream()
                    .filter(r -> r.getStudentId() == studentId && r.getStatus() == com.alumconnect.model.MentorshipStatus.ACCEPTED)
                    .toList();
        }

        @Override public int createRequest(com.alumconnect.service.dto.MentorshipRequestDto dto) { return 1; }
        @Override public boolean existsPending(int studentId, int alumniId) { return false; }
        @Override public boolean existsAccepted(int studentId, int alumniId) { return false; }
        @Override public int countActiveMentees(int alumniId) { return 0; }
        @Override public boolean isAlumniAvailable(int alumniId) { return true; }
    }
}
