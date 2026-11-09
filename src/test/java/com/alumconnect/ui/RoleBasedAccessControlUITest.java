package com.alumconnect.ui;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.MentorshipRequest;
import com.alumconnect.model.MentorshipStatus;
import com.alumconnect.model.Role;
import com.alumconnect.model.Student;
import com.alumconnect.model.User;
import com.alumconnect.repository.AlumniProfileRepository;
import com.alumconnect.repository.MentorshipRequestRepository;
import com.alumconnect.repository.StudentProfileRepository;
import com.alumconnect.repository.UserRepository;
import com.alumconnect.service.AlumniProfileService;
import com.alumconnect.service.MentorshipRequestService;
import com.alumconnect.service.StudentProfileService;
import com.alumconnect.service.UserService;
import com.alumconnect.service.dto.AlumniProfileResult;
import com.alumconnect.service.dto.MentorshipRequestDto;
import com.alumconnect.service.dto.StudentProfileResult;
import com.alumconnect.util.PasswordUtil;
import com.alumconnect.util.UserSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
 * UI-layer role-based access control tests for Prompt 21.
 *
 * <p>Covers direct-call bypass prevention on:
 * <ul>
 *   <li>{@link StudentDashboardUI#handleBrowseAlumni()}</li>
 *   <li>{@link StudentDashboardUI#handleSearchAlumni()}</li>
 *   <li>{@link StudentDashboardUI#handleFilterAlumni()}</li>
 *   <li>{@link StudentDashboardUI#handleProfileSubMenu()}</li>
 *   <li>{@link AlumniDashboardUI#handleProfileSubMenu()}</li>
 *   <li>{@link AlumniProfileUI#promptSendMentorshipRequest(int)}</li>
 * </ul>
 *
 * <p>Also covers navigation correctness, session clearance on logout,
 * and regression checks for existing security guards.
 *
 * <p>No real database connection is used. All repositories are in-memory fakes.
 */
@DisplayName("UI-Layer Role-Based Access Control Tests (Prompt 21)")
class RoleBasedAccessControlUITest {

    // ── Shared fixtures ──────────────────────────────────────────────────────

    private Student student;
    private Alumni  alumni;
    private InMemoryUserRepository userRepo;
    private UserService            userService;

    @BeforeEach
    void setUp() throws SQLException {
        UserSession.getInstance().logout();

        userRepo     = new InMemoryUserRepository();
        userService  = new UserService(userRepo);

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

    // ── Helpers ──────────────────────────────────────────────────────────────

    private StudentDashboardUI buildStudentDashboard(String input, ByteArrayOutputStream capture) {
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        PrintStream ps = new PrintStream(capture);
        FakeStudentProfileService fakeProfileService = new FakeStudentProfileService();
        StudentProfileUI profileUI = new StudentProfileUI(fakeProfileService, in, ps);
        return new StudentDashboardUI(profileUI, userService, in, ps);
    }

    private AlumniDashboardUI buildAlumniDashboard(String input, ByteArrayOutputStream capture) {
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        PrintStream ps = new PrintStream(capture);
        FakeAlumniProfileService fakeProfileService = new FakeAlumniProfileService();
        AlumniProfileUI profileUI = new AlumniProfileUI(fakeProfileService, in, ps);
        MentorshipRequestService mentorshipService =
                new MentorshipRequestService(new FakeMentorshipRequestRepository(), userRepo);
        return new AlumniDashboardUI(profileUI, mentorshipService, userService, in, ps);
    }

    private AlumniProfileUI buildAlumniProfileUI(String input, ByteArrayOutputStream capture) {
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        PrintStream ps = new PrintStream(capture);
        FakeAlumniProfileService fakeProfileService = new FakeAlumniProfileService();
        return new AlumniProfileUI(fakeProfileService, in, ps);
    }

    // ════════════════════════════════════════════════════════════════════════
    // 1. StudentDashboardUI — direct-call bypass prevention
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("StudentDashboardUI — direct-call bypass prevention")
    class StudentDashboardBypassPrevention {

        @Test
        @DisplayName("handleBrowseAlumni() throws SecurityException when not logged in")
        void browseAlumni_unauthenticated() {
            StudentDashboardUI dashboard = buildStudentDashboard("", new ByteArrayOutputStream());
            assertThrows(SecurityException.class, dashboard::handleBrowseAlumni,
                    "handleBrowseAlumni() must reject unauthenticated callers.");
        }

        @Test
        @DisplayName("handleBrowseAlumni() throws SecurityException when logged in as Alumni")
        void browseAlumni_alumniRejected() {
            userService.login("alumni@thapar.edu", "AlumniP@ss1");
            StudentDashboardUI dashboard = buildStudentDashboard("", new ByteArrayOutputStream());
            assertThrows(SecurityException.class, dashboard::handleBrowseAlumni,
                    "handleBrowseAlumni() must reject ALUMNI role.");
        }

        @Test
        @DisplayName("handleSearchAlumni() throws SecurityException when not logged in")
        void searchAlumni_unauthenticated() {
            StudentDashboardUI dashboard = buildStudentDashboard("", new ByteArrayOutputStream());
            assertThrows(SecurityException.class, dashboard::handleSearchAlumni,
                    "handleSearchAlumni() must reject unauthenticated callers.");
        }

        @Test
        @DisplayName("handleSearchAlumni() throws SecurityException when logged in as Alumni")
        void searchAlumni_alumniRejected() {
            userService.login("alumni@thapar.edu", "AlumniP@ss1");
            StudentDashboardUI dashboard = buildStudentDashboard("", new ByteArrayOutputStream());
            assertThrows(SecurityException.class, dashboard::handleSearchAlumni,
                    "handleSearchAlumni() must reject ALUMNI role.");
        }

        @Test
        @DisplayName("handleFilterAlumni() throws SecurityException when not logged in")
        void filterAlumni_unauthenticated() {
            StudentDashboardUI dashboard = buildStudentDashboard("", new ByteArrayOutputStream());
            assertThrows(SecurityException.class, dashboard::handleFilterAlumni,
                    "handleFilterAlumni() must reject unauthenticated callers.");
        }

        @Test
        @DisplayName("handleFilterAlumni() throws SecurityException when logged in as Alumni")
        void filterAlumni_alumniRejected() {
            userService.login("alumni@thapar.edu", "AlumniP@ss1");
            StudentDashboardUI dashboard = buildStudentDashboard("", new ByteArrayOutputStream());
            assertThrows(SecurityException.class, dashboard::handleFilterAlumni,
                    "handleFilterAlumni() must reject ALUMNI role.");
        }

        @Test
        @DisplayName("handleProfileSubMenu() throws SecurityException when not logged in")
        void profileSubMenu_unauthenticated() {
            StudentDashboardUI dashboard = buildStudentDashboard("", new ByteArrayOutputStream());
            assertThrows(SecurityException.class, dashboard::handleProfileSubMenu,
                    "handleProfileSubMenu() must reject unauthenticated callers.");
        }

        @Test
        @DisplayName("handleProfileSubMenu() throws SecurityException when logged in as Alumni")
        void profileSubMenu_alumniRejected() {
            userService.login("alumni@thapar.edu", "AlumniP@ss1");
            StudentDashboardUI dashboard = buildStudentDashboard("", new ByteArrayOutputStream());
            assertThrows(SecurityException.class, dashboard::handleProfileSubMenu,
                    "handleProfileSubMenu() must reject ALUMNI role.");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 2. AlumniDashboardUI — direct-call bypass prevention
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("AlumniDashboardUI — direct-call bypass prevention")
    class AlumniDashboardBypassPrevention {

        @Test
        @DisplayName("handleProfileSubMenu() throws SecurityException when not logged in")
        void profileSubMenu_unauthenticated() {
            AlumniDashboardUI dashboard = buildAlumniDashboard("", new ByteArrayOutputStream());
            assertThrows(SecurityException.class, dashboard::handleProfileSubMenu,
                    "handleProfileSubMenu() must reject unauthenticated callers.");
        }

        @Test
        @DisplayName("handleProfileSubMenu() throws SecurityException when logged in as Student")
        void profileSubMenu_studentRejected() {
            userService.login("student@thapar.edu", "StudentP@ss1");
            AlumniDashboardUI dashboard = buildAlumniDashboard("", new ByteArrayOutputStream());
            assertThrows(SecurityException.class, dashboard::handleProfileSubMenu,
                    "handleProfileSubMenu() must reject STUDENT role.");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 3. AlumniProfileUI — direct-call bypass prevention
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("AlumniProfileUI — direct-call bypass prevention")
    class AlumniProfileBypassPrevention {

        @Test
        @DisplayName("promptSendMentorshipRequest() throws SecurityException when not logged in")
        void promptSendMentorshipRequest_unauthenticated() {
            AlumniProfileUI profileUI = buildAlumniProfileUI("", new ByteArrayOutputStream());
            assertThrows(SecurityException.class,
                    () -> profileUI.promptSendMentorshipRequest(1),
                    "promptSendMentorshipRequest() must reject unauthenticated callers.");
        }

        @Test
        @DisplayName("promptSendMentorshipRequest() throws SecurityException when logged in as Alumni")
        void promptSendMentorshipRequest_alumniRejected() {
            userService.login("alumni@thapar.edu", "AlumniP@ss1");
            AlumniProfileUI profileUI = buildAlumniProfileUI("", new ByteArrayOutputStream());
            assertThrows(SecurityException.class,
                    () -> profileUI.promptSendMentorshipRequest(1),
                    "promptSendMentorshipRequest() must reject ALUMNI role.");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 4. Navigation correctness — Student Dashboard
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Student Dashboard — navigation correctness")
    class StudentDashboardNavigation {

        @Test
        @DisplayName("Student dashboard shows correct menu headers")
        void studentDashboardShowsStudentMenu() {
            userService.login("student@thapar.edu", "StudentP@ss1");
            ByteArrayOutputStream capture = new ByteArrayOutputStream();
            // Choose Exit (0) to terminate the loop cleanly
            StudentDashboardUI dashboard = buildStudentDashboard("0\n", capture);
            assertDoesNotThrow(dashboard::show);

            String output = capture.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("STUDENT DASHBOARD"), "Student dashboard header must appear.");
            assertTrue(output.contains("Anshika Singh"),     "Logged-in student name must appear.");
        }

        @Test
        @DisplayName("Student dashboard does not show alumni-only prompt text")
        void studentDashboardExcludesAlumniFeatures() {
            userService.login("student@thapar.edu", "StudentP@ss1");
            ByteArrayOutputStream capture = new ByteArrayOutputStream();
            StudentDashboardUI dashboard = buildStudentDashboard("0\n", capture);
            assertDoesNotThrow(dashboard::show);

            String output = capture.toString(StandardCharsets.UTF_8);
            // The alumni dashboard header must never appear inside the student dashboard
            assertFalse(output.contains("ALUMNI DASHBOARD"),
                    "ALUMNI DASHBOARD header must not appear in student dashboard output.");
        }

        @Test
        @DisplayName("Student dashboard: show() rejects unauthenticated access")
        void studentDashboardRejectsUnauthenticated() {
            StudentDashboardUI dashboard = buildStudentDashboard("", new ByteArrayOutputStream());
            assertThrows(SecurityException.class, dashboard::show);
        }

        @Test
        @DisplayName("Student dashboard: show() rejects alumni")
        void studentDashboardRejectsAlumni() {
            userService.login("alumni@thapar.edu", "AlumniP@ss1");
            StudentDashboardUI dashboard = buildStudentDashboard("", new ByteArrayOutputStream());
            assertThrows(SecurityException.class, dashboard::show);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 5. Navigation correctness — Alumni Dashboard
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Alumni Dashboard — navigation correctness")
    class AlumniDashboardNavigation {

        @Test
        @DisplayName("Alumni dashboard shows correct menu headers")
        void alumniDashboardShowsAlumniMenu() {
            userService.login("alumni@thapar.edu", "AlumniP@ss1");
            ByteArrayOutputStream capture = new ByteArrayOutputStream();
            AlumniDashboardUI dashboard = buildAlumniDashboard("0\n", capture);
            assertDoesNotThrow(dashboard::show);

            String output = capture.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("ALUMNI DASHBOARD"), "Alumni dashboard header must appear.");
            assertTrue(output.contains("Ravi Kumar"),        "Logged-in alumni name must appear.");
        }

        @Test
        @DisplayName("Alumni dashboard does not show student-only prompt text")
        void alumniDashboardExcludesStudentFeatures() {
            userService.login("alumni@thapar.edu", "AlumniP@ss1");
            ByteArrayOutputStream capture = new ByteArrayOutputStream();
            AlumniDashboardUI dashboard = buildAlumniDashboard("0\n", capture);
            assertDoesNotThrow(dashboard::show);

            String output = capture.toString(StandardCharsets.UTF_8);
            assertFalse(output.contains("STUDENT DASHBOARD"),
                    "STUDENT DASHBOARD header must not appear in alumni dashboard output.");
        }

        @Test
        @DisplayName("Alumni dashboard: show() rejects unauthenticated access")
        void alumniDashboardRejectsUnauthenticated() {
            AlumniDashboardUI dashboard = buildAlumniDashboard("", new ByteArrayOutputStream());
            assertThrows(SecurityException.class, dashboard::show);
        }

        @Test
        @DisplayName("Alumni dashboard: show() rejects student")
        void alumniDashboardRejectsStudent() {
            userService.login("student@thapar.edu", "StudentP@ss1");
            AlumniDashboardUI dashboard = buildAlumniDashboard("", new ByteArrayOutputStream());
            assertThrows(SecurityException.class, dashboard::show);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 6. Session clearance on logout
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Session clearance on logout")
    class SessionClearance {

        @Test
        @DisplayName("Student logout via menu clears session")
        void studentLogoutClearsSession() {
            userService.login("student@thapar.edu", "StudentP@ss1");
            assertTrue(UserSession.getInstance().isLoggedIn());

            ByteArrayOutputStream capture = new ByteArrayOutputStream();
            // Menu option 9 = Logout in student dashboard
            StudentDashboardUI dashboard = buildStudentDashboard("9\n", capture);
            assertDoesNotThrow(dashboard::show);

            assertFalse(UserSession.getInstance().isLoggedIn(),
                    "Session must be cleared after student logout.");
        }

        @Test
        @DisplayName("Alumni logout via menu clears session")
        void alumniLogoutClearsSession() {
            userService.login("alumni@thapar.edu", "AlumniP@ss1");
            assertTrue(UserSession.getInstance().isLoggedIn());

            ByteArrayOutputStream capture = new ByteArrayOutputStream();
            // Menu option 9 = Logout in alumni dashboard
            AlumniDashboardUI dashboard = buildAlumniDashboard("9\n", capture);
            assertDoesNotThrow(dashboard::show);

            assertFalse(UserSession.getInstance().isLoggedIn(),
                    "Session must be cleared after alumni logout.");
        }

        @Test
        @DisplayName("After student logout, student dashboard show() throws SecurityException")
        void afterStudentLogout_dashboardRejectsAccess() {
            userService.login("student@thapar.edu", "StudentP@ss1");
            userService.logout();

            StudentDashboardUI dashboard = buildStudentDashboard("", new ByteArrayOutputStream());
            assertThrows(SecurityException.class, dashboard::show,
                    "Dashboard must reject access after session has been cleared.");
        }

        @Test
        @DisplayName("After alumni logout, alumni dashboard show() throws SecurityException")
        void afterAlumniLogout_dashboardRejectsAccess() {
            userService.login("alumni@thapar.edu", "AlumniP@ss1");
            userService.logout();

            AlumniDashboardUI dashboard = buildAlumniDashboard("", new ByteArrayOutputStream());
            assertThrows(SecurityException.class, dashboard::show,
                    "Dashboard must reject access after session has been cleared.");
        }

        @Test
        @DisplayName("UserSession.logout() clears authentication state")
        void userSessionLogoutClearsState() {
            userService.login("student@thapar.edu", "StudentP@ss1");
            assertTrue(UserSession.getInstance().isLoggedIn());
            assertEquals(Role.STUDENT, UserSession.getInstance().getCurrentRole().orElse(null));

            UserSession.getInstance().logout();

            assertFalse(UserSession.getInstance().isLoggedIn());
            assertFalse(UserSession.getInstance().isStudent());
            assertFalse(UserSession.getInstance().isAlumni());
            assertTrue(UserSession.getInstance().getCurrentRole().isEmpty());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 7. Cross-role access regression
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Cross-role access regression")
    class CrossRoleAccessRegression {

        @Test
        @DisplayName("Student role is correctly recognised after login")
        void studentRoleRecognised() {
            userService.login("student@thapar.edu", "StudentP@ss1");
            assertTrue(UserSession.getInstance().isStudent());
            assertFalse(UserSession.getInstance().isAlumni());
            assertEquals(Role.STUDENT, UserSession.getInstance().getCurrentRole().orElse(null));
        }

        @Test
        @DisplayName("Alumni role is correctly recognised after login")
        void alumniRoleRecognised() {
            userService.login("alumni@thapar.edu", "AlumniP@ss1");
            assertTrue(UserSession.getInstance().isAlumni());
            assertFalse(UserSession.getInstance().isStudent());
            assertEquals(Role.ALUMNI, UserSession.getInstance().getCurrentRole().orElse(null));
        }

        @Test
        @DisplayName("Student cannot access alumni dashboard show()")
        void studentCannotAccessAlumniDashboard() {
            userService.login("student@thapar.edu", "StudentP@ss1");
            AlumniDashboardUI alumniBashboard = buildAlumniDashboard("", new ByteArrayOutputStream());
            assertThrows(SecurityException.class, alumniBashboard::show);
        }

        @Test
        @DisplayName("Alumni cannot access student dashboard show()")
        void alumniCannotAccessStudentDashboard() {
            userService.login("alumni@thapar.edu", "AlumniP@ss1");
            StudentDashboardUI studentDashboard = buildStudentDashboard("", new ByteArrayOutputStream());
            assertThrows(SecurityException.class, studentDashboard::show);
        }

        @Test
        @DisplayName("Student can send mentorship request via promptSendMentorshipRequest()")
        void studentCanCallPromptSendMentorshipRequest() {
            userService.login("student@thapar.edu", "StudentP@ss1");
            // Provide "0" to immediately back out of the mentorship prompt
            ByteArrayOutputStream capture = new ByteArrayOutputStream();
            AlumniProfileUI profileUI = buildAlumniProfileUI("0\n", capture);
            // Should NOT throw — student is authorized
            assertDoesNotThrow(() -> profileUI.promptSendMentorshipRequest(alumni.getId()));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Fake inner classes — in-memory stubs (no database)
    // ════════════════════════════════════════════════════════════════════════

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

    private static class FakeStudentProfileService extends StudentProfileService {
        FakeStudentProfileService() {
            super(new StudentProfileRepository() {
                @Override public Optional<Student> findProfileByUserId(int userId) { return Optional.empty(); }
                @Override public boolean updateProfile(Student student) { return true; }
                @Override public boolean createProfile(Student student) { return true; }
                @Override public boolean profileExistsForUser(int userId) { return false; }
            });
        }

        @Override public StudentProfileResult viewMyProfile() {
            return StudentProfileResult.failure("No profile (test stub).");
        }

        @Override public StudentProfileResult createMyProfile(com.alumconnect.service.dto.StudentProfileRequest req) {
            return StudentProfileResult.failure("Create profile stub (no DB in test).");
        }

        @Override public StudentProfileResult updateMyProfile(com.alumconnect.service.dto.StudentProfileRequest req) {
            return StudentProfileResult.failure("Update profile stub (no DB in test).");
        }
    }

    private static class FakeAlumniProfileService extends AlumniProfileService {
        FakeAlumniProfileService() {
            super(new AlumniProfileRepository() {
                @Override public Optional<Alumni> findProfileByUserId(int userId) { return Optional.empty(); }
                @Override public boolean updateProfile(Alumni alumni) { return true; }
                @Override public boolean createProfile(Alumni alumni) { return true; }
                @Override public boolean profileExistsForUser(int userId) { return false; }
            });
        }

        @Override public AlumniProfileResult viewMyProfile() {
            return AlumniProfileResult.failure("No profile (test stub).");
        }

        @Override public AlumniProfileResult createMyProfile(com.alumconnect.service.dto.AlumniProfileRequest req) {
            return AlumniProfileResult.failure("Create profile stub (no DB in test).");
        }

        @Override public AlumniProfileResult updateMyProfile(com.alumconnect.service.dto.AlumniProfileRequest req) {
            return AlumniProfileResult.failure("Update profile stub (no DB in test).");
        }
    }

    private static class FakeMentorshipRequestRepository implements MentorshipRequestRepository {
        private final Map<Integer, MentorshipRequest> requests = new HashMap<>();

        @Override
        public List<MentorshipRequest> findAcceptedConnectionsByAlumniId(int alumniId) throws SQLException {
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
            if (req != null) { req.setStatus(newStatus); req.setRespondedAt(respondedAt); return true; }
            return false;
        }

        @Override public int createRequest(MentorshipRequestDto dto) { return 1; }
        @Override public boolean existsPending(int studentId, int alumniId) { return false; }
        @Override public boolean existsAccepted(int studentId, int alumniId) { return false; }
        @Override public int countActiveMentees(int alumniId) { return 0; }
        @Override public boolean isAlumniAvailable(int alumniId) { return true; }
    }
}
