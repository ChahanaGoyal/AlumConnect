package com.alumconnect.service;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.Role;
import com.alumconnect.model.Student;
import com.alumconnect.model.User;
import com.alumconnect.repository.UserRepository;
import com.alumconnect.service.dto.RegistrationRequest;
import com.alumconnect.service.dto.RegistrationResult;
import com.alumconnect.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User Registration and Service Tests")
class UserServiceTest {

    private InMemoryUserRepository fakeRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        fakeRepository = new InMemoryUserRepository();
        userService = new UserService(fakeRepository);
    }

    @Test
    @DisplayName("Successful Student Registration")
    void testSuccessfulStudentRegistration() {
        RegistrationRequest request = RegistrationRequest.forStudent(
                "John Doe",
                "john.doe@thapar.edu",
                "StrongP@ssw0rd",
                "Computer Science",
                2027
        );
        request.setConfirmPassword("StrongP@ssw0rd");
        request.setPhone("+91-9876543210");
        request.setCurrentSemester(5);
        request.setEnrollmentNumber("102103456");
        request.setSkills("Java, SQL, Git");
        request.setInterests("Backend Architecture");

        RegistrationResult result = userService.register(request);

        assertTrue(result.isSuccess(), "Registration should succeed: " + result.getMessage());
        assertNotNull(result.getUser());
        assertTrue(result.getUser() instanceof Student);

        Student student = (Student) result.getUser();
        assertNotNull(student.getId(), "User ID should be assigned");
        assertNotNull(student.getProfileId(), "Profile ID should be assigned");
        assertEquals("john.doe@thapar.edu", student.getEmail());
        assertEquals("John Doe", student.getFullName());
        assertEquals(Role.STUDENT, student.getRole());
        assertEquals("Computer Science", student.getDepartment());
        assertEquals(2027, student.getGraduationYear());
        assertEquals(5, student.getCurrentSemester());
        assertEquals("102103456", student.getEnrollmentNumber());
        assertEquals("Java, SQL, Git", student.getSkills());

        // Verify password hashing
        assertNotEquals("StrongP@ssw0rd", student.getPasswordHash(), "Password must not be stored plain-text");
        assertTrue(PasswordUtil.verifyPassword("StrongP@ssw0rd", student.getPasswordHash()), "Hash must verify successfully");
    }

    @Test
    @DisplayName("Successful Alumni Registration")
    void testSuccessfulAlumniRegistration() {
        RegistrationRequest request = RegistrationRequest.forAlumni(
                "Sarah Connor",
                "sarah.c@techcorp.com",
                "SecureAlum#2026",
                "Electronics and Communication",
                2019
        );
        request.setConfirmPassword("SecureAlum#2026");
        request.setPhone("+1-555-987-6543");
        request.setCurrentCompany("TechCorp");
        request.setCurrentRole("Lead Architect");
        request.setIndustry("Technology");
        request.setYearsOfExperience(7);
        request.setExpertiseAreas("Cloud Infrastructure, DevOps");
        request.setAvailableForMentorship(true);
        request.setMaxMentees(4);

        RegistrationResult result = userService.register(request);

        assertTrue(result.isSuccess(), "Registration should succeed: " + result.getMessage());
        assertNotNull(result.getUser());
        assertTrue(result.getUser() instanceof Alumni);

        Alumni alumni = (Alumni) result.getUser();
        assertNotNull(alumni.getId());
        assertNotNull(alumni.getProfileId());
        assertEquals("sarah.c@techcorp.com", alumni.getEmail());
        assertEquals("Sarah Connor", alumni.getFullName());
        assertEquals(Role.ALUMNI, alumni.getRole());
        assertEquals("TechCorp", alumni.getCurrentCompany());
        assertEquals("Lead Architect", alumni.getCurrentRole());
        assertEquals(7, alumni.getYearsOfExperience());
        assertTrue(alumni.isAvailableForMentorship());
        assertEquals(4, alumni.getMaxMentees());

        // Verify password hashing
        assertTrue(PasswordUtil.verifyPassword("SecureAlum#2026", alumni.getPasswordHash()));
    }

    @Test
    @DisplayName("Duplicate Email Registration should be rejected")
    void testDuplicateEmailRegistration() {
        RegistrationRequest request1 = RegistrationRequest.forStudent(
                "User One", "shared.email@example.com", "Password@123", "IT", 2026
        );
        RegistrationResult result1 = userService.register(request1);
        assertTrue(result1.isSuccess());

        // Attempt second registration with same email
        RegistrationRequest request2 = RegistrationRequest.forStudent(
                "User Two", "shared.email@example.com", "Password@123", "CSE", 2027
        );
        RegistrationResult result2 = userService.register(request2);

        assertFalse(result2.isSuccess(), "Second registration with duplicate email must fail");
        assertTrue(result2.getMessage().toLowerCase().contains("already exists"),
                "Message should state email already exists: " + result2.getMessage());

        // Check with different casing or leading/trailing space
        RegistrationRequest request3 = RegistrationRequest.forAlumni(
                "User Three", "  SHARED.EMAIL@EXAMPLE.COM  ", "Password@123", "ECE", 2020
        );
        RegistrationResult result3 = userService.register(request3);
        assertFalse(result3.isSuccess(), "Duplicate email with different casing/spaces must fail");
    }

    @Test
    @DisplayName("Validation failure for missing required fields")
    void testValidationMissingRequiredFields() {
        RegistrationRequest request = new RegistrationRequest();

        RegistrationResult result = userService.register(request);
        assertFalse(result.isSuccess());
        assertFalse(result.getErrors().isEmpty());

        List<String> errors = result.getErrors();
        assertTrue(errors.stream().anyMatch(e -> e.contains("Role")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("Full name")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("Email")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("Password")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("Department")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("Graduation year")));
    }

    @Test
    @DisplayName("Validation failure for invalid email format")
    void testValidationInvalidEmail() {
        RegistrationRequest request = RegistrationRequest.forStudent(
                "Jane Doe", "invalid-email-format", "ValidP@ss123", "CS", 2026
        );
        RegistrationResult result = userService.register(request);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.toLowerCase().contains("email format")));
    }

    @Test
    @DisplayName("Validation failure for weak password")
    void testValidationWeakPassword() {
        // Missing special character
        RegistrationRequest request = RegistrationRequest.forStudent(
                "Jane Doe", "jane@example.com", "WeakPass123", "CS", 2026
        );
        RegistrationResult result = userService.register(request);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.toLowerCase().contains("special character")));
    }

    @Test
    @DisplayName("Validation failure for password mismatch")
    void testValidationPasswordMismatch() {
        RegistrationRequest request = RegistrationRequest.forStudent(
                "Jane Doe", "jane@example.com", "ValidP@ss123", "CS", 2026
        );
        request.setConfirmPassword("DifferentP@ss123");

        RegistrationResult result = userService.register(request);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.toLowerCase().contains("passwords do not match")));
    }

    @Test
    @DisplayName("Validation failure for out-of-range graduation year")
    void testValidationGraduationYearRange() {
        RegistrationRequest request = RegistrationRequest.forStudent(
                "Jane Doe", "jane@example.com", "ValidP@ss123", "CS", 1940
        );
        RegistrationResult result = userService.register(request);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.toLowerCase().contains("graduation year")));
    }

    @Test
    @DisplayName("Validation failure for out-of-range student semester")
    void testValidationSemesterRange() {
        RegistrationRequest request = RegistrationRequest.forStudent(
                "Jane Doe", "jane@example.com", "ValidP@ss123", "CS", 2026
        );
        request.setCurrentSemester(15);

        RegistrationResult result = userService.register(request);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.toLowerCase().contains("semester")));
    }

    @Test
    @DisplayName("Validation failure for negative alumni experience or max mentees")
    void testValidationAlumniConstraints() {
        RegistrationRequest request = RegistrationRequest.forAlumni(
                "Alum One", "alum@example.com", "ValidP@ss123", "CS", 2020
        );
        request.setYearsOfExperience(-2);
        request.setMaxMentees(-1);

        RegistrationResult result = userService.register(request);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.toLowerCase().contains("experience")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.toLowerCase().contains("mentees")));
    }

    @Test
    @DisplayName("Graceful handling of database exceptions during registration")
    void testDatabaseErrorHandling() {
        fakeRepository.setThrowExceptionOnSave(true);

        RegistrationRequest request = RegistrationRequest.forStudent(
                "John Error", "john.error@example.com", "ValidP@ss123", "CS", 2026
        );
        RegistrationResult result = userService.register(request);

        assertFalse(result.isSuccess(), "Registration should fail gracefully on database error");
        assertTrue(result.getMessage().contains("Database error"), "Message should mention database error: " + result.getMessage());
    }

    @Test
    @DisplayName("Email availability check behavior")
    void testEmailAvailabilityCheck() {
        assertTrue(userService.isEmailAvailable("available@example.com"));

        RegistrationRequest request = RegistrationRequest.forStudent(
                "User Available", "taken@example.com", "ValidP@ss123", "CS", 2026
        );
        userService.register(request);

        assertFalse(userService.isEmailAvailable("taken@example.com"));
        assertFalse(userService.isEmailAvailable("not-an-email"));
    }

    // ── In-Memory Repository Fake for Deterministic Unit Tests ─────────────────

    private static class InMemoryUserRepository implements UserRepository {
        private final Map<Integer, User> usersById = new HashMap<>();
        private final Map<String, Integer> userIdsByEmail = new HashMap<>();
        private int sequence = 1;
        private boolean throwExceptionOnSave = false;

        void setThrowExceptionOnSave(boolean val) {
            this.throwExceptionOnSave = val;
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
            if (throwExceptionOnSave) {
                throw new SQLException("Simulated connection timeout to MySQL database");
            }
            int id = sequence++;
            student.setId(id);
            student.setProfileId(sequence++);
            usersById.put(id, student);
            userIdsByEmail.put(student.getEmail().trim().toLowerCase(), id);
            return student;
        }

        @Override
        public Alumni saveAlumni(Alumni alumni) throws SQLException {
            if (throwExceptionOnSave) {
                throw new SQLException("Simulated connection timeout to MySQL database");
            }
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
