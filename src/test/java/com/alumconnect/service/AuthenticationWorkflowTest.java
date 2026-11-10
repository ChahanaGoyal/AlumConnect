package com.alumconnect.service;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.Role;
import com.alumconnect.model.Student;
import com.alumconnect.model.User;
import com.alumconnect.repository.UserRepository;
import com.alumconnect.service.dto.LoginResult;
import com.alumconnect.service.dto.RegistrationRequest;
import com.alumconnect.service.dto.RegistrationResult;
import com.alumconnect.util.PasswordUtil;
import com.alumconnect.util.UserSession;
import com.alumconnect.util.ValidationUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive authentication workflow tests for Prompt 23B.
 *
 * <p>Covers:
 * <ul>
 *   <li>Student registration (valid, minimal, boundary, invalid)</li>
 *   <li>Alumni registration (valid, minimal, boundary, invalid)</li>
 *   <li>Login with valid and invalid credentials (case-insensitivity, trim, inactive users)</li>
 *   <li>Logout and session state transitions</li>
 *   <li>Session creation, invalidation, and role-enforced access</li>
 *   <li>Protected operations without authentication</li>
 *   <li>Invalid or empty registration inputs</li>
 *   <li>Security checks: ensuring passwords and DB credentials are never exposed in messages</li>
 * </ul>
 */
@DisplayName("Authentication Workflow Tests (Prompt 23B)")
class AuthenticationWorkflowTest {

    private InMemoryUserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() throws SQLException {
        UserSession.getInstance().logout();
        userRepository = new InMemoryUserRepository();
        userService = new UserService(userRepository);

        // Seed a pre-existing student
        Student student = new Student(
                "existing.student@thapar.edu",
                PasswordUtil.hashPassword("StudentP@ss123"),
                "Existing Student",
                "+91-9876543210",
                "Computer Science",
                2026
        );
        student.setCurrentSemester(6);
        student.setEnrollmentNumber("102203001");
        userRepository.saveStudent(student);

        // Seed a pre-existing alumni
        Alumni alumni = new Alumni(
                "existing.alumni@thapar.edu",
                PasswordUtil.hashPassword("AlumniP@ss123"),
                "Existing Alumni",
                "+91-9811223344",
                "Electronics",
                2020
        );
        alumni.setCurrentCompany("Google");
        alumni.setCurrentRole("Staff Software Engineer");
        alumni.setYearsOfExperience(6);
        alumni.setAvailableForMentorship(true);
        alumni.setMaxMentees(3);
        userRepository.saveAlumni(alumni);

        // Seed an inactive student
        Student inactive = new Student(
                "inactive.user@thapar.edu",
                PasswordUtil.hashPassword("InactiveP@ss123"),
                "Inactive User",
                null,
                "Mechanical",
                2025
        );
        inactive.setActive(false);
        userRepository.saveStudent(inactive);
    }

    @AfterEach
    void tearDown() {
        UserSession.getInstance().logout();
    }

    // =========================================================================
    // 1. Student Registration
    // =========================================================================

    @Nested
    @DisplayName("Student Registration Workflows")
    class StudentRegistrationTests {

        @Test
        @DisplayName("Valid student registration with all optional fields succeeds")
        void testValidStudentRegistrationFull() {
            RegistrationRequest req = RegistrationRequest.forStudent(
                    "Rohit Verma",
                    "rohit.verma@thapar.edu",
                    "RohitP@ss2026",
                    "Computer Engineering",
                    2026
            );
            req.setConfirmPassword("RohitP@ss2026");
            req.setPhone("+91-9876500001");
            req.setDegree("B.Tech Computer Science");
            req.setCurrentSemester(6);
            req.setEnrollmentNumber("102203888");
            req.setBio("Enthusiastic software developer");
            req.setSkills("Java, Spring, MySQL");
            req.setInterests("Distributed Systems, Cloud");
            req.setLinkedinUrl("https://linkedin.com/in/rohit-verma");
            req.setGithubUrl("https://github.com/rohitverma");

            RegistrationResult result = userService.register(req);

            assertTrue(result.isSuccess(), "Registration should succeed: " + result.getMessage());
            assertNotNull(result.getUser());
            assertTrue(result.getUser() instanceof Student);
            Student s = (Student) result.getUser();
            assertEquals("rohit.verma@thapar.edu", s.getEmail());
            assertEquals(Role.STUDENT, s.getRole());
            assertEquals("102203888", s.getEnrollmentNumber());
            assertEquals(6, s.getCurrentSemester());
            assertTrue(PasswordUtil.verifyPassword("RohitP@ss2026", s.getPasswordHash()));
        }

        @Test
        @DisplayName("Valid student registration with minimal required fields succeeds")
        void testValidStudentRegistrationMinimal() {
            RegistrationRequest req = RegistrationRequest.forStudent(
                    "Simran Kaur",
                    "simran.k@thapar.edu",
                    "Simran#Pass99",
                    "Electrical",
                    2027
            );
            req.setConfirmPassword("Simran#Pass99");

            RegistrationResult result = userService.register(req);

            assertTrue(result.isSuccess(), "Minimal registration should succeed");
            Student s = (Student) result.getUser();
            assertEquals("Simran Kaur", s.getFullName());
            assertEquals("Electrical", s.getDepartment());
            assertEquals(2027, s.getGraduationYear());
        }

        @Test
        @DisplayName("Student registration with duplicate email is rejected")
        void testDuplicateEmailRegistrationRejected() {
            RegistrationRequest req = RegistrationRequest.forStudent(
                    "Duplicate User",
                    "existing.student@thapar.edu",
                    "ValidP@ss123",
                    "Computer Science",
                    2026
            );
            req.setConfirmPassword("ValidP@ss123");

            RegistrationResult result = userService.register(req);

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().toLowerCase().contains("already exists"));
        }
    }

    // =========================================================================
    // 2. Alumni Registration
    // =========================================================================

    @Nested
    @DisplayName("Alumni Registration Workflows")
    class AlumniRegistrationTests {

        @Test
        @DisplayName("Valid alumni registration with all optional fields succeeds")
        void testValidAlumniRegistrationFull() {
            RegistrationRequest req = RegistrationRequest.forAlumni(
                    "Dr. Sunita Rao",
                    "sunita.rao@alumni.org",
                    "Sunita#Alum2024",
                    "Electronics",
                    2015
            );
            req.setConfirmPassword("Sunita#Alum2024");
            req.setPhone("+91-9811002233");
            req.setCurrentCompany("Amazon");
            req.setCurrentRole("Principal Engineer");
            req.setIndustry("Technology");
            req.setYearsOfExperience(11);
            req.setExpertiseAreas("AWS, Microservices");
            req.setBio("Passionate mentor with 11+ years in tech.");
            req.setLinkedinUrl("https://linkedin.com/in/sunitarao");
            req.setAvailableForMentorship(true);
            req.setMaxMentees(5);

            RegistrationResult result = userService.register(req);

            assertTrue(result.isSuccess());
            assertNotNull(result.getUser());
            assertTrue(result.getUser() instanceof Alumni);
            Alumni a = (Alumni) result.getUser();
            assertEquals("Dr. Sunita Rao", a.getFullName());
            assertEquals(Role.ALUMNI, a.getRole());
            assertEquals("Amazon", a.getCurrentCompany());
            assertEquals(11, a.getYearsOfExperience());
            assertEquals(5, a.getMaxMentees());
            assertTrue(a.isAvailableForMentorship());
        }

        @Test
        @DisplayName("Valid alumni registration with minimal required fields succeeds")
        void testValidAlumniRegistrationMinimal() {
            RegistrationRequest req = RegistrationRequest.forAlumni(
                    "Karan Mehta",
                    "karan.m@tech.com",
                    "KaranP@ss2021",
                    "Mechanical",
                    2021
            );
            req.setConfirmPassword("KaranP@ss2021");

            RegistrationResult result = userService.register(req);

            assertTrue(result.isSuccess());
            Alumni a = (Alumni) result.getUser();
            assertEquals("Karan Mehta", a.getFullName());
            assertEquals(Role.ALUMNI, a.getRole());
        }
    }

    // =========================================================================
    // 3. Login with Valid & Invalid Credentials
    // =========================================================================

    @Nested
    @DisplayName("Login & Credential Verification")
    class LoginTests {

        @Test
        @DisplayName("Login with valid student credentials establishes student session")
        void testValidStudentLogin() {
            LoginResult result = userService.login("existing.student@thapar.edu", "StudentP@ss123");

            assertTrue(result.isSuccess());
            assertNotNull(result.getUser());
            assertEquals(Role.STUDENT, result.getUser().getRole());
            assertTrue(UserSession.getInstance().isLoggedIn());
            assertTrue(UserSession.getInstance().isStudent());
            assertFalse(UserSession.getInstance().isAlumni());
            assertEquals("Existing Student", UserSession.getInstance().getCurrentUser().get().getFullName());
        }

        @Test
        @DisplayName("Login with valid alumni credentials establishes alumni session")
        void testValidAlumniLogin() {
            LoginResult result = userService.login("existing.alumni@thapar.edu", "AlumniP@ss123");

            assertTrue(result.isSuccess());
            assertNotNull(result.getUser());
            assertEquals(Role.ALUMNI, result.getUser().getRole());
            assertTrue(UserSession.getInstance().isLoggedIn());
            assertTrue(UserSession.getInstance().isAlumni());
            assertFalse(UserSession.getInstance().isStudent());
        }

        @Test
        @DisplayName("Login email check is case-insensitive")
        void testLoginEmailCaseInsensitive() {
            LoginResult result = userService.login("EXISTING.STUDENT@THAPAR.EDU", "StudentP@ss123");
            assertTrue(result.isSuccess(), "Login with uppercase email must succeed");
        }

        @Test
        @DisplayName("Login email with leading and trailing whitespace is trimmed")
        void testLoginEmailTrimmed() {
            LoginResult result = userService.login("  existing.student@thapar.edu  ", "StudentP@ss123");
            assertTrue(result.isSuccess(), "Login with whitespace around email must succeed");
        }

        @Test
        @DisplayName("Login with invalid password fails")
        void testLoginInvalidPassword() {
            LoginResult result = userService.login("existing.student@thapar.edu", "WrongPassword!99");

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().contains("Invalid email or password"));
            assertFalse(UserSession.getInstance().isLoggedIn());
        }

        @Test
        @DisplayName("Login with non-existent email fails")
        void testLoginNonExistentEmail() {
            LoginResult result = userService.login("ghost.user@unknown.com", "SomePass#123");

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().contains("Invalid email or password"));
            assertFalse(UserSession.getInstance().isLoggedIn());
        }

        @Test
        @DisplayName("Login with inactive user account is rejected")
        void testLoginInactiveAccountRejected() {
            LoginResult result = userService.login("inactive.user@thapar.edu", "InactiveP@ss123");

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().toLowerCase().contains("inactive"));
            assertFalse(UserSession.getInstance().isLoggedIn());
        }

        @Test
        @DisplayName("Login with null or blank email is rejected")
        void testLoginEmptyEmailRejected() {
            LoginResult nullResult = userService.login(null, "Pass123!A");
            assertFalse(nullResult.isSuccess());
            assertTrue(nullResult.getMessage().contains("Email is required"));

            LoginResult blankResult = userService.login("   ", "Pass123!A");
            assertFalse(blankResult.isSuccess());
            assertTrue(blankResult.getMessage().contains("Email is required"));
        }

        @Test
        @DisplayName("Login with null or empty password is rejected")
        void testLoginEmptyPasswordRejected() {
            LoginResult nullResult = userService.login("existing.student@thapar.edu", null);
            assertFalse(nullResult.isSuccess());
            assertTrue(nullResult.getMessage().contains("Password is required"));

            LoginResult emptyResult = userService.login("existing.student@thapar.edu", "");
            assertFalse(emptyResult.isSuccess());
            assertTrue(emptyResult.getMessage().contains("Password is required"));
        }
    }

    // =========================================================================
    // 4. Logout & Session Invalidation
    // =========================================================================

    @Nested
    @DisplayName("Logout & Session Lifecycle")
    class LogoutAndSessionLifecycleTests {

        @Test
        @DisplayName("Logout clears active session completely")
        void testLogoutClearsSession() {
            userService.login("existing.student@thapar.edu", "StudentP@ss123");
            assertTrue(UserSession.getInstance().isLoggedIn());

            userService.logout();

            assertFalse(UserSession.getInstance().isLoggedIn());
            assertFalse(UserSession.getInstance().isStudent());
            assertFalse(UserSession.getInstance().isAlumni());
            assertTrue(UserSession.getInstance().getCurrentUser().isEmpty());
            assertTrue(UserSession.getInstance().getCurrentRole().isEmpty());
        }

        @Test
        @DisplayName("getAuthenticatedUserId throws SecurityException after logout")
        void testGetAuthenticatedUserIdFailsAfterLogout() {
            userService.login("existing.student@thapar.edu", "StudentP@ss123");
            assertEquals(1, UserSession.getInstance().getAuthenticatedUserId());

            userService.logout();

            assertThrows(SecurityException.class, () -> UserSession.getInstance().getAuthenticatedUserId());
        }

        @Test
        @DisplayName("Logging in as another user replaces previous session")
        void testSessionReplacementOnRelogin() {
            userService.login("existing.student@thapar.edu", "StudentP@ss123");
            assertTrue(UserSession.getInstance().isStudent());

            userService.login("existing.alumni@thapar.edu", "AlumniP@ss123");
            assertTrue(UserSession.getInstance().isAlumni());
            assertFalse(UserSession.getInstance().isStudent());
            assertEquals("Existing Alumni", UserSession.getInstance().getCurrentUser().get().getFullName());
        }

        @Test
        @DisplayName("Protected operations requireStudent throws SecurityException when logged out")
        void testRequireStudentThrowsWhenLoggedOut() {
            assertThrows(SecurityException.class, () -> UserSession.getInstance().requireStudent());
        }

        @Test
        @DisplayName("Protected operations requireAlumni throws SecurityException when logged out")
        void testRequireAlumniThrowsWhenLoggedOut() {
            assertThrows(SecurityException.class, () -> UserSession.getInstance().requireAlumni());
        }

        @Test
        @DisplayName("Protected operations requireStudent throws SecurityException when logged in as Alumni")
        void testRequireStudentThrowsForAlumni() {
            userService.login("existing.alumni@thapar.edu", "AlumniP@ss123");
            assertThrows(SecurityException.class, () -> UserSession.getInstance().requireStudent());
        }

        @Test
        @DisplayName("Protected operations requireAlumni throws SecurityException when logged in as Student")
        void testRequireAlumniThrowsForStudent() {
            userService.login("existing.student@thapar.edu", "StudentP@ss123");
            assertThrows(SecurityException.class, () -> UserSession.getInstance().requireAlumni());
        }

        @Test
        @DisplayName("requireUserId throws SecurityException when expectedUserId does not match")
        void testRequireUserIdMismatched() {
            userService.login("existing.student@thapar.edu", "StudentP@ss123");
            int loggedInId = UserSession.getInstance().getAuthenticatedUserId();

            assertDoesNotThrow(() -> UserSession.getInstance().requireUserId(loggedInId));
            assertThrows(SecurityException.class, () -> UserSession.getInstance().requireUserId(loggedInId + 999));
        }
    }

    // =========================================================================
    // 5. Registration Input Validation & Edge Cases
    // =========================================================================

    @Nested
    @DisplayName("Registration Input Validation Edge Cases")
    class RegistrationInputValidationTests {

        @Test
        @DisplayName("Null registration request is handled gracefully")
        void testNullRegistrationRequest() {
            RegistrationResult result = userService.register(null);
            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().contains("must not be null"));
        }

        @Test
        @DisplayName("Null role is rejected")
        void testNullRoleRejected() {
            RegistrationRequest req = new RegistrationRequest();
            req.setFullName("No Role User");
            req.setEmail("norole@thapar.edu");
            req.setPassword("ValidP@ss123");
            req.setConfirmPassword("ValidP@ss123");
            req.setDepartment("Computer Science");
            req.setGraduationYear(2026);

            RegistrationResult result = userService.register(req);
            assertFalse(result.isSuccess());
            assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Role selection is required")));
        }

        @Test
        @DisplayName("Blank or whitespace-only full name is rejected")
        void testBlankFullNameRejected() {
            RegistrationRequest req = RegistrationRequest.forStudent(
                    "   ",
                    "valid.email@thapar.edu",
                    "ValidP@ss123",
                    "Computer Science",
                    2026
            );
            req.setConfirmPassword("ValidP@ss123");

            RegistrationResult result = userService.register(req);
            assertFalse(result.isSuccess());
            assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Full name is required")));
        }

        @Test
        @DisplayName("Full name exceeding 100 characters is rejected")
        void testLongFullNameRejected() {
            String longName = "A".repeat(101);
            RegistrationRequest req = RegistrationRequest.forStudent(
                    longName,
                    "valid.email@thapar.edu",
                    "ValidP@ss123",
                    "Computer Science",
                    2026
            );
            req.setConfirmPassword("ValidP@ss123");

            RegistrationResult result = userService.register(req);
            assertFalse(result.isSuccess());
            assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("cannot exceed 100 characters")));
        }

        @Test
        @DisplayName("Invalid email formats are rejected")
        void testInvalidEmailFormatsRejected() {
            String[] invalidEmails = {
                    "plainaddress",
                    "@missingusername.com",
                    "username@.com",
                    "user name@thapar.edu",
                    "user@thapar..edu"
            };

            for (String email : invalidEmails) {
                RegistrationRequest req = RegistrationRequest.forStudent(
                        "Test User", email, "ValidP@ss123", "Computer Science", 2026
                );
                req.setConfirmPassword("ValidP@ss123");
                RegistrationResult result = userService.register(req);
                assertFalse(result.isSuccess(), "Should reject invalid email: " + email);
            }
        }

        @Test
        @DisplayName("Password confirmation mismatch is rejected")
        void testPasswordMismatchRejected() {
            RegistrationRequest req = RegistrationRequest.forStudent(
                    "Test User", "mismatch@thapar.edu", "ValidP@ss123", "Computer Science", 2026
            );
            req.setConfirmPassword("DifferentP@ss123");

            RegistrationResult result = userService.register(req);
            assertFalse(result.isSuccess());
            assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Passwords do not match")));
        }

        @Test
        @DisplayName("Weak passwords failing complexity rules are rejected with details")
        void testWeakPasswordsRejected() {
            // Missing special char
            RegistrationRequest req1 = RegistrationRequest.forStudent(
                    "Test User", "pwd1@thapar.edu", "NoSpecial123", "CS", 2026
            );
            req1.setConfirmPassword("NoSpecial123");
            RegistrationResult r1 = userService.register(req1);
            assertFalse(r1.isSuccess());

            // Missing uppercase
            RegistrationRequest req2 = RegistrationRequest.forStudent(
                    "Test User", "pwd2@thapar.edu", "lowercase@123", "CS", 2026
            );
            req2.setConfirmPassword("lowercase@123");
            RegistrationResult r2 = userService.register(req2);
            assertFalse(r2.isSuccess());

            // Missing digit
            RegistrationRequest req3 = RegistrationRequest.forStudent(
                    "Test User", "pwd3@thapar.edu", "NoDigitP@ssword", "CS", 2026
            );
            req3.setConfirmPassword("NoDigitP@ssword");
            RegistrationResult r3 = userService.register(req3);
            assertFalse(r3.isSuccess());

            // Too short (< 8 chars)
            RegistrationRequest req4 = RegistrationRequest.forStudent(
                    "Test User", "pwd4@thapar.edu", "Sh@1rt", "CS", 2026
            );
            req4.setConfirmPassword("Sh@1rt");
            RegistrationResult r4 = userService.register(req4);
            assertFalse(r4.isSuccess());
        }

        @Test
        @DisplayName("Boundary graduation years are enforced (1950 - 2100)")
        void testGraduationYearBoundaries() {
            // Valid lower boundary: 1950
            RegistrationRequest req1 = RegistrationRequest.forStudent(
                    "Old Grad", "old@thapar.edu", "ValidP@ss123", "CS", 1950
            );
            req1.setConfirmPassword("ValidP@ss123");
            assertTrue(userService.register(req1).isSuccess());

            // Invalid lower boundary: 1949
            RegistrationRequest req2 = RegistrationRequest.forStudent(
                    "Too Old", "tooold@thapar.edu", "ValidP@ss123", "CS", 1949
            );
            req2.setConfirmPassword("ValidP@ss123");
            assertFalse(userService.register(req2).isSuccess());

            // Valid upper boundary: 2100
            RegistrationRequest req3 = RegistrationRequest.forStudent(
                    "Future Grad", "future@thapar.edu", "ValidP@ss123", "CS", 2100
            );
            req3.setConfirmPassword("ValidP@ss123");
            assertTrue(userService.register(req3).isSuccess());

            // Invalid upper boundary: 2101
            RegistrationRequest req4 = RegistrationRequest.forStudent(
                    "Too Future", "toofuture@thapar.edu", "ValidP@ss123", "CS", 2101
            );
            req4.setConfirmPassword("ValidP@ss123");
            assertFalse(userService.register(req4).isSuccess());
        }

        @Test
        @DisplayName("Student semester boundaries are enforced (1 - 12)")
        void testStudentSemesterBoundaries() {
            // Semester 0 is invalid
            RegistrationRequest req0 = RegistrationRequest.forStudent(
                    "Sem 0", "sem0@thapar.edu", "ValidP@ss123", "CS", 2026
            );
            req0.setConfirmPassword("ValidP@ss123");
            req0.setCurrentSemester(0);
            assertFalse(userService.register(req0).isSuccess());

            // Semester 13 is invalid
            RegistrationRequest req13 = RegistrationRequest.forStudent(
                    "Sem 13", "sem13@thapar.edu", "ValidP@ss123", "CS", 2026
            );
            req13.setConfirmPassword("ValidP@ss123");
            req13.setCurrentSemester(13);
            assertFalse(userService.register(req13).isSuccess());
        }

        @Test
        @DisplayName("Invalid LinkedIn URL format is rejected")
        void testInvalidLinkedInUrlRejected() {
            RegistrationRequest req = RegistrationRequest.forStudent(
                    "Bad URL", "badurl@thapar.edu", "ValidP@ss123", "CS", 2026
            );
            req.setConfirmPassword("ValidP@ss123");
            req.setLinkedinUrl("http://facebook.com/notlinkedin");

            RegistrationResult result = userService.register(req);
            assertFalse(result.isSuccess());
            assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("LinkedIn")));
        }
    }

    // =========================================================================
    // 6. Credential & Password Exposure Prevention
    // =========================================================================

    @Nested
    @DisplayName("Password & Database Credential Exposure Prevention")
    class SecurityLeakPreventionTests {

        @Test
        @DisplayName("Plaintext password is never stored in user model entity")
        void testPlaintextPasswordNeverStoredInModel() {
            String plainPassword = "SecretP@ssword2026";
            RegistrationRequest req = RegistrationRequest.forStudent(
                    "Secure User", "secure@thapar.edu", plainPassword, "CS", 2026
            );
            req.setConfirmPassword(plainPassword);

            RegistrationResult result = userService.register(req);
            assertTrue(result.isSuccess());

            User user = result.getUser();
            assertNotNull(user.getPasswordHash());
            assertNotEquals(plainPassword, user.getPasswordHash(), "Password must not be stored in plaintext");
            assertFalse(user.getPasswordHash().contains(plainPassword));
            assertTrue(PasswordUtil.verifyPassword(plainPassword, user.getPasswordHash()));
        }

        @Test
        @DisplayName("Plaintext password is never exposed in RegistrationResult message or errors")
        void testPasswordNeverInRegistrationResult() {
            String plainPassword = "SecretP@ssword2026";
            RegistrationRequest req = RegistrationRequest.forStudent(
                    "Secure User", "secure.dup@thapar.edu", plainPassword, "CS", 2026
            );
            req.setConfirmPassword(plainPassword);

            RegistrationResult successResult = userService.register(req);
            assertFalse(successResult.getMessage().contains(plainPassword));

            // Duplicate registration
            RegistrationResult failResult = userService.register(req);
            assertFalse(failResult.getMessage().contains(plainPassword));
            for (String err : failResult.getErrors()) {
                assertFalse(err.contains(plainPassword));
            }
        }

        @Test
        @DisplayName("Plaintext password is never exposed in LoginResult messages")
        void testPasswordNeverInLoginResult() {
            String plainPassword = "SecretP@ssword2026";
            LoginResult success = userService.login("existing.student@thapar.edu", "StudentP@ss123");
            assertFalse(success.getMessage().contains("StudentP@ss123"));

            LoginResult failed = userService.login("existing.student@thapar.edu", plainPassword);
            assertFalse(failed.getMessage().contains(plainPassword));
        }

        @Test
        @DisplayName("Database error message sanitization masks password parameters")
        void testDatabaseErrorMessageSanitization() {
            SQLException rawSqlEx = new SQLException("Access denied for user 'root'@'localhost' (using password=SuperSecretPassword123)");
            String formatted = ValidationUtil.formatDatabaseErrorMessage(rawSqlEx, "Database error");

            assertFalse(formatted.contains("SuperSecretPassword123"), "Password must be sanitized from error output");
            assertTrue(formatted.contains("******") || formatted.contains("unavailable"));
        }
    }

    // =========================================================================
    // In-Memory UserRepository Fake
    // =========================================================================

    private static class InMemoryUserRepository implements UserRepository {
        private final Map<Integer, User> usersById = new HashMap<>();
        private final Map<String, Integer> userIdsByEmail = new HashMap<>();
        private int sequence = 1;

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
        public Optional<User> findByEmail(String email) {
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
