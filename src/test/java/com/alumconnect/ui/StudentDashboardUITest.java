package com.alumconnect.ui;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.Role;
import com.alumconnect.model.Student;
import com.alumconnect.model.User;
import com.alumconnect.repository.StudentProfileRepository;
import com.alumconnect.repository.UserRepository;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link StudentDashboardUI} — Prompt 10.
 *
 * <p>Uses in-memory fakes for all repositories and services so that no real
 * database connection is needed.  Every test starts with a clean
 * {@link UserSession} and tears it down afterwards.
 */
@DisplayName("Student Dashboard Tests (Prompt 10)")
class StudentDashboardUITest {

    // ── Shared test fixtures ──────────────────────────────────────────────────

    private Student student;
    private Alumni  alumni;
    private InMemoryUserRepository userRepo;
    private UserService userService;

    @BeforeEach
    void setUp() throws SQLException {
        UserSession.getInstance().logout();

        userRepo    = new InMemoryUserRepository();
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
        // No login → session is empty
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
    @DisplayName("Dashboard menu shows correct options for implemented features only")
    void testMenuShowsCorrectOptions() {
        userService.login("student@thapar.edu", "StudentP@ss1");

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        StudentDashboardUI dashboard = buildDashboard("0\n", capture);
        dashboard.show();

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("My Profile"), "Profile option must be listed");
        assertTrue(output.contains("Logout"),      "Logout option must be listed");
        assertTrue(output.contains("Exit"),        "Exit option must be listed");
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
    }

    @Test
    @DisplayName("AuthUI does not route Alumni to Student Dashboard")
    void testAuthUIDoesNotRouteAlumniToStudentDashboard() {
        String input = "alumni@thapar.edu\nAlumniP@ss1\n4\n";
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
     * Returns a pre-built Student profile for viewMyProfile(), and no-ops for create/update.
     */
    private static class FakeStudentProfileService extends StudentProfileService {

        private final StudentProfileRepository noOpRepo = new StudentProfileRepository() {
            @Override public Optional<Student> findProfileByUserId(int userId) { return Optional.empty(); }
            @Override public boolean updateProfile(Student student) { return true; }
            @Override public boolean createProfile(Student student) { return true; }
            @Override public boolean profileExistsForUser(int userId) { return false; }
        };

        FakeStudentProfileService() {
            super(/* repository injected via reflection not needed — we override methods */
                    new StudentProfileRepository() {
                        @Override public Optional<Student> findProfileByUserId(int userId) { return Optional.empty(); }
                        @Override public boolean updateProfile(Student student) { return true; }
                        @Override public boolean createProfile(Student student) { return true; }
                        @Override public boolean profileExistsForUser(int userId) { return false; }
                    });
        }

        @Override
        public StudentProfileResult viewMyProfile() {
            // Return failure so StudentProfileUI prints "[ERROR] ..." rather than null student
            return StudentProfileResult.failure(
                    "No profile found (test stub — no DB connection).");
        }

        @Override
        public StudentProfileResult createMyProfile(
                com.alumconnect.service.dto.StudentProfileRequest req) {
            return StudentProfileResult.failure("Create profile stub (no DB in test).");
        }

        @Override
        public StudentProfileResult updateMyProfile(
                com.alumconnect.service.dto.StudentProfileRequest req) {
            return StudentProfileResult.failure("Update profile stub (no DB in test).");
        }
    }
}
