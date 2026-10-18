package com.alumconnect.ui;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.Role;
import com.alumconnect.model.Student;
import com.alumconnect.model.User;
import com.alumconnect.repository.UserRepository;
import com.alumconnect.service.UserService;
import com.alumconnect.service.dto.LoginResult;
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
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Login, Logout, Session State & Access Control Tests")
class AuthUITest {

    private InMemoryUserRepository fakeRepository;
    private UserService userService;

    @BeforeEach
    void setUp() throws SQLException {
        UserSession.getInstance().logout();
        fakeRepository = new InMemoryUserRepository();
        userService = new UserService(fakeRepository);

        // Seed a student
        Student student = new Student(
                "student@thapar.edu",
                PasswordUtil.hashPassword("StudentP@ss123"),
                "Aman Sharma",
                "+91-9876543210",
                "Computer Science",
                2026
        );
        student.setCurrentSemester(7);
        student.setEnrollmentNumber("102203111");
        fakeRepository.saveStudent(student);

        // Seed an alumni
        Alumni alumni = new Alumni(
                "alumni@thapar.edu",
                PasswordUtil.hashPassword("AlumniP@ss123"),
                "Priya Verma",
                "+91-9811223344",
                "Electronics",
                2020
        );
        alumni.setCurrentCompany("Microsoft");
        alumni.setCurrentRole("Staff Engineer");
        fakeRepository.saveAlumni(alumni);

        // Seed an inactive user
        Student inactiveStudent = new Student(
                "inactive@thapar.edu",
                PasswordUtil.hashPassword("InactiveP@ss123"),
                "Inactive User",
                null,
                "Mechanical",
                2025
        );
        inactiveStudent.setActive(false);
        fakeRepository.saveStudent(inactiveStudent);
    }

    @AfterEach
    void tearDown() {
        UserSession.getInstance().logout();
    }

    @Test
    @DisplayName("Successful Student login authenticates and sets session")
    void testSuccessfulStudentLogin() {
        LoginResult result = userService.login("student@thapar.edu", "StudentP@ss123");

        assertTrue(result.isSuccess(), "Login should succeed: " + result.getMessage());
        assertNotNull(result.getUser());
        assertEquals("Aman Sharma", result.getUser().getFullName());
        assertEquals(Role.STUDENT, result.getUser().getRole());
        assertTrue(result.getUser() instanceof Student);

        // Verify session state
        assertTrue(UserSession.getInstance().isLoggedIn());
        assertTrue(UserSession.getInstance().isStudent());
        assertFalse(UserSession.getInstance().isAlumni());
        assertEquals(Role.STUDENT, UserSession.getInstance().getCurrentRole().orElse(null));
        assertEquals("Aman Sharma", UserSession.getInstance().getCurrentUser().map(User::getFullName).orElse(null));
    }

    @Test
    @DisplayName("Successful Alumni login authenticates and sets session")
    void testSuccessfulAlumniLogin() {
        LoginResult result = userService.login("alumni@thapar.edu", "AlumniP@ss123");

        assertTrue(result.isSuccess(), "Login should succeed: " + result.getMessage());
        assertNotNull(result.getUser());
        assertEquals("Priya Verma", result.getUser().getFullName());
        assertEquals(Role.ALUMNI, result.getUser().getRole());
        assertTrue(result.getUser() instanceof Alumni);

        // Verify session state
        assertTrue(UserSession.getInstance().isLoggedIn());
        assertTrue(UserSession.getInstance().isAlumni());
        assertFalse(UserSession.getInstance().isStudent());
        assertEquals(Role.ALUMNI, UserSession.getInstance().getCurrentRole().orElse(null));
    }

    @Test
    @DisplayName("Login with incorrect password should be rejected")
    void testLoginIncorrectPassword() {
        LoginResult result = userService.login("student@thapar.edu", "WrongPassword!1");

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Invalid email or password"));
        assertNull(result.getUser());
        assertFalse(UserSession.getInstance().isLoggedIn());
    }

    @Test
    @DisplayName("Login with nonexistent user should be rejected")
    void testLoginNonexistentUser() {
        LoginResult result = userService.login("nonexistent@thapar.edu", "SomePassword#1");

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Invalid email or password"));
        assertNull(result.getUser());
        assertFalse(UserSession.getInstance().isLoggedIn());
    }

    @Test
    @DisplayName("Login with inactive user should be rejected with friendly message")
    void testLoginInactiveUser() {
        LoginResult result = userService.login("inactive@thapar.edu", "InactiveP@ss123");

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().toLowerCase().contains("inactive"),
                "Message should mention inactive account: " + result.getMessage());
        assertNull(result.getUser());
        assertFalse(UserSession.getInstance().isLoggedIn());
    }

    @Test
    @DisplayName("Login with missing email or password should be rejected")
    void testLoginMissingInput() {
        LoginResult resultNoEmail = userService.login("", "SomePassword#1");
        assertFalse(resultNoEmail.isSuccess());
        assertTrue(resultNoEmail.getMessage().contains("Email is required"));

        LoginResult resultNoPass = userService.login("student@thapar.edu", "");
        assertFalse(resultNoPass.isSuccess());
        assertTrue(resultNoPass.getMessage().contains("Password is required"));

        LoginResult resultNullEmail = userService.login(null, "SomePassword#1");
        assertFalse(resultNullEmail.isSuccess());

        LoginResult resultNullPass = userService.login("student@thapar.edu", null);
        assertFalse(resultNullPass.isSuccess());
    }

    @Test
    @DisplayName("Database error during login is handled gracefully")
    void testLoginDatabaseError() {
        fakeRepository.setThrowExceptionOnFind(true);

        LoginResult result = userService.login("student@thapar.edu", "StudentP@ss123");

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Database error"), "Message should mention database error: " + result.getMessage());
        assertFalse(UserSession.getInstance().isLoggedIn());
    }

    @Test
    @DisplayName("Logout clears session state completely")
    void testLogoutClearsSession() {
        userService.login("student@thapar.edu", "StudentP@ss123");
        assertTrue(UserSession.getInstance().isLoggedIn());

        userService.logout();

        assertFalse(UserSession.getInstance().isLoggedIn());
        assertTrue(UserSession.getInstance().getCurrentUser().isEmpty());
        assertTrue(UserSession.getInstance().getCurrentRole().isEmpty());
        assertFalse(UserSession.getInstance().isStudent());
        assertFalse(UserSession.getInstance().isAlumni());
    }

    @Test
    @DisplayName("Unauthorized role access prevention: Student cannot access Alumni functionality")
    void testStudentCannotAccessAlumniFunctionality() {
        userService.login("student@thapar.edu", "StudentP@ss123");
        assertTrue(UserSession.getInstance().isStudent());

        AuthUI authUI = new AuthUI(userService, System.in, System.out);

        // Student accessing alumni dashboard or alumni action should throw SecurityException
        assertThrows(SecurityException.class, () -> {
            Alumni mockAlumni = new Alumni("a@b.com", "h", "Alum", null, "CS", 2020);
            authUI.showAlumniDashboard(mockAlumni);
        });

        assertThrows(SecurityException.class, () -> {
            authUI.executeAlumniOnlyAction(() -> fail("Should not execute"));
        });
    }

    @Test
    @DisplayName("Unauthorized role access prevention: Alumni cannot access Student functionality")
    void testAlumniCannotAccessStudentFunctionality() {
        userService.login("alumni@thapar.edu", "AlumniP@ss123");
        assertTrue(UserSession.getInstance().isAlumni());

        AuthUI authUI = new AuthUI(userService, System.in, System.out);

        // Alumni accessing student dashboard or student action should throw SecurityException
        assertThrows(SecurityException.class, () -> {
            Student mockStudent = new Student("s@b.com", "h", "Student", null, "CS", 2027);
            authUI.showStudentDashboard(mockStudent);
        });

        assertThrows(SecurityException.class, () -> {
            authUI.executeStudentOnlyAction(() -> fail("Should not execute"));
        });
    }

    @Test
    @DisplayName("Authorized role action execution works as expected")
    void testAuthorizedActionExecution() {
        userService.login("student@thapar.edu", "StudentP@ss123");
        AuthUI authUI = new AuthUI(userService, System.in, System.out);

        AtomicBoolean executed = new AtomicBoolean(false);
        authUI.executeStudentOnlyAction(() -> executed.set(true));
        assertTrue(executed.get());

        userService.logout();
        userService.login("alumni@thapar.edu", "AlumniP@ss123");

        AtomicBoolean alumniExecuted = new AtomicBoolean(false);
        authUI.executeAlumniOnlyAction(() -> alumniExecuted.set(true));
        assertTrue(alumniExecuted.get());
    }

    @Test
    @DisplayName("AuthUI interactive promptAndLogin simulation for Student")
    void testAuthUIInteractiveStudentLogin() {
        String input = "student@thapar.edu\nStudentP@ss123\n";
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        AuthUI authUI = new AuthUI(userService, in, new PrintStream(out));
        LoginResult result = authUI.promptAndLogin();

        assertTrue(result.isSuccess());
        String consoleOutput = out.toString(StandardCharsets.UTF_8);
        assertTrue(consoleOutput.contains("STUDENT DASHBOARD"));
        assertTrue(consoleOutput.contains("Aman Sharma"));
        assertTrue(consoleOutput.contains("Student Menu"));

        // Test handleLogout via UI
        authUI.handleLogout();
        assertFalse(UserSession.getInstance().isLoggedIn());
    }

    @Test
    @DisplayName("AuthUI interactive promptAndLogin simulation for Alumni")
    void testAuthUIInteractiveAlumniLogin() {
        String input = "alumni@thapar.edu\nAlumniP@ss123\n";
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        AuthUI authUI = new AuthUI(userService, in, new PrintStream(out));
        LoginResult result = authUI.promptAndLogin();

        assertTrue(result.isSuccess());
        String consoleOutput = out.toString(StandardCharsets.UTF_8);
        assertTrue(consoleOutput.contains("ALUMNI DASHBOARD"));
        assertTrue(consoleOutput.contains("Priya Verma"));
        assertTrue(consoleOutput.contains("Alumni Menu"));
    }

    // ── In-Memory Repository Fake ─────────────────────────────────────────────

    private static class InMemoryUserRepository implements UserRepository {
        private final Map<Integer, User> usersById = new HashMap<>();
        private final Map<String, Integer> userIdsByEmail = new HashMap<>();
        private int sequence = 1;
        private boolean throwExceptionOnFind = false;

        void setThrowExceptionOnFind(boolean val) {
            this.throwExceptionOnFind = val;
        }

        @Override
        public boolean existsByEmail(String email) {
            if (email == null) return false;
            return userIdsByEmail.containsKey(email.trim().toLowerCase());
        }

        @Override
        public Optional<User> findById(int id) {
            return Optional.ofNullable(usersById.get(id));
        }

        @Override
        public Optional<User> findByEmail(String email) throws SQLException {
            if (throwExceptionOnFind) {
                throw new SQLException("Simulated connection timeout to MySQL database");
            }
            if (email == null) return Optional.empty();
            Integer id = userIdsByEmail.get(email.trim().toLowerCase());
            return id != null ? Optional.ofNullable(usersById.get(id)) : Optional.empty();
        }

        @Override
        public Optional<Student> findStudentById(int userId) {
            User u = usersById.get(userId);
            return (u instanceof Student s) ? Optional.of(s) : Optional.empty();
        }

        @Override
        public Optional<Alumni> findAlumniById(int userId) {
            User u = usersById.get(userId);
            return (u instanceof Alumni a) ? Optional.of(a) : Optional.empty();
        }

        @Override
        public Student saveStudent(Student student) throws SQLException {
            int id = sequence++;
            student.setId(id);
            student.setProfileId(sequence++);
            usersById.put(id, student);
            userIdsByEmail.put(student.getEmail().trim().toLowerCase(), id);
            return student;
        }

        @Override
        public Alumni saveAlumni(Alumni alumni) throws SQLException {
            int id = sequence++;
            alumni.setId(id);
            alumni.setProfileId(sequence++);
            usersById.put(id, alumni);
            userIdsByEmail.put(alumni.getEmail().trim().toLowerCase(), id);
            return alumni;
        }

        @Override
        public boolean deleteUserById(int userId) {
            User removed = usersById.remove(userId);
            if (removed != null) {
                userIdsByEmail.remove(removed.getEmail().trim().toLowerCase());
                return true;
            }
            return false;
        }
    }
}
