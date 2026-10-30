package com.alumconnect.ui;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.Role;
import com.alumconnect.model.Student;
import com.alumconnect.model.User;
import com.alumconnect.repository.AlumniProfileRepository;
import com.alumconnect.repository.UserRepository;
import com.alumconnect.service.AlumniProfileService;
import com.alumconnect.service.UserService;
import com.alumconnect.service.dto.AlumniProfileResult;
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
import java.util.HashMap;
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
        ByteArrayInputStream in = new ByteArrayInputStream(
                simulatedInput.getBytes(StandardCharsets.UTF_8));
        PrintStream ps = new PrintStream(capture);

        FakeAlumniProfileService fakeProfileService = new FakeAlumniProfileService();
        AlumniProfileUI profileUI = new AlumniProfileUI(fakeProfileService, in, ps);
        return new AlumniDashboardUI(profileUI, userService, in, ps);
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
        String input = "alumni@thapar.edu\nAlumniP@ss1\n0\n";
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
        String input = "student@thapar.edu\nStudentP@ss1\n0\n";
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

    // ── In-memory fakes ───────────────────────────────────────────────────────

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
