package com.alumconnect.service;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.Role;
import com.alumconnect.model.Student;
import com.alumconnect.model.User;
import com.alumconnect.repository.StudentProfileRepository;
import com.alumconnect.service.dto.StudentProfileRequest;
import com.alumconnect.service.dto.StudentProfileResult;
import com.alumconnect.util.PasswordUtil;
import com.alumconnect.util.UserSession;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link StudentProfileService} covering:
 * - Profile creation (profile exists via registration; service handles view)
 * - Profile viewing (own profile, missing profile, unauthenticated, unauthorized)
 * - Profile updating (valid update, validation errors, duplicate enrollment, DB error)
 * - Persistence verification
 * - Unauthorized access (no session, alumni role)
 */
@DisplayName("Student Profile Service Tests")
class StudentProfileServiceTest {

    private InMemoryStudentProfileRepository fakeRepository;
    private StudentProfileService profileService;
    private Student seededStudent;

    @BeforeEach
    void setUp() throws SQLException {
        UserSession.getInstance().logout();
        fakeRepository = new InMemoryStudentProfileRepository();
        profileService = new StudentProfileService(fakeRepository);

        // Seed a student profile
        seededStudent = new Student(
                "alice@college.edu",
                PasswordUtil.hashPassword("AlicePass#1"),
                "Alice Smith",
                "+91-9876543210",
                "Computer Science",
                2027
        );
        seededStudent.setId(10);
        seededStudent.setProfileId(101);
        seededStudent.setCurrentSemester(5);
        seededStudent.setEnrollmentNumber("CS2021001");
        seededStudent.setSkills("Java, Python");
        seededStudent.setInterests("AI, Cloud Computing");
        seededStudent.setBio("Passionate about software engineering.");
        seededStudent.setLinkedinUrl("https://linkedin.com/in/alice");
        seededStudent.setGithubUrl("https://github.com/alice");
        fakeRepository.save(seededStudent);
    }

    @AfterEach
    void tearDown() {
        UserSession.getInstance().logout();
    }

    // ── viewMyProfile ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Logged-in student can view their own profile")
    void testViewProfileSuccess() {
        // Simulate logged-in student session
        UserSession.getInstance().login(seededStudent);

        StudentProfileResult result = profileService.viewMyProfile();

        assertTrue(result.isSuccess(), "View should succeed: " + result.getMessage());
        assertNotNull(result.getStudent());
        assertEquals("Alice Smith", result.getStudent().getFullName());
        assertEquals("Computer Science", result.getStudent().getDepartment());
        assertEquals(2027, result.getStudent().getGraduationYear());
        assertEquals(5, result.getStudent().getCurrentSemester());
        assertEquals("CS2021001", result.getStudent().getEnrollmentNumber());
        assertEquals("Java, Python", result.getStudent().getSkills());
    }

    @Test
    @DisplayName("Unauthenticated access to viewMyProfile is rejected")
    void testViewProfileUnauthenticated() {
        // No session active
        StudentProfileResult result = profileService.viewMyProfile();

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().toLowerCase().contains("logged in"),
                "Message should mention login requirement: " + result.getMessage());
        assertNull(result.getStudent());
    }

    @Test
    @DisplayName("Non-student role cannot view student profile")
    void testViewProfileUnauthorizedAlumniRole() {
        // Fake an alumni user with role ALUMNI
        User alumniUser = new com.alumconnect.model.Alumni(
                "bob@college.edu", "hash", "Bob Alumni", null, "Electrical", 2019
        );
        UserSession.getInstance().login(alumniUser);

        StudentProfileResult result = profileService.viewMyProfile();

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().toLowerCase().contains("access denied")
                || result.getMessage().toLowerCase().contains("only students"),
                "Message should indicate access denied: " + result.getMessage());
        assertNull(result.getStudent());
    }

    @Test
    @DisplayName("View profile returns failure when profile not found in DB")
    void testViewProfileNotFound() {
        // Create a student user without a profile row in repository
        Student noProfileStudent = new Student(
                "ghost@college.edu", "hash", "Ghost User", null, "Physics", 2026
        );
        noProfileStudent.setId(99);
        UserSession.getInstance().login(noProfileStudent);

        StudentProfileResult result = profileService.viewMyProfile();

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().toLowerCase().contains("not found") ||
                result.getMessage().toLowerCase().contains("profile"),
                "Message should indicate profile not found: " + result.getMessage());
    }

    @Test
    @DisplayName("Database error during view is handled gracefully")
    void testViewProfileDatabaseError() {
        fakeRepository.setThrowOnFind(true);
        UserSession.getInstance().login(seededStudent);

        StudentProfileResult result = profileService.viewMyProfile();

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().toLowerCase().contains("database"),
                "Message should mention database error: " + result.getMessage());
    }

    // ── updateMyProfile ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Student can successfully update their own profile")
    void testUpdateProfileSuccess() {
        UserSession.getInstance().login(seededStudent);

        StudentProfileRequest request = new StudentProfileRequest();
        request.setDepartment("Information Technology");
        request.setGraduationYear(2028);
        request.setDegree("Bachelor of Engineering");
        request.setCurrentSemester(6);
        request.setEnrollmentNumber("IT2021002");
        request.setSkills("Java, Spring Boot, AWS");
        request.setInterests("Cloud, DevOps");
        request.setBio("Updated bio.");
        request.setLinkedinUrl("https://linkedin.com/in/alice-updated");
        request.setGithubUrl("https://github.com/alice-updated");

        StudentProfileResult result = profileService.updateMyProfile(request);

        assertTrue(result.isSuccess(), "Update should succeed: " + result.getMessage());
        assertNotNull(result.getStudent());
        assertEquals("Information Technology", result.getStudent().getDepartment());
        assertEquals(2028, result.getStudent().getGraduationYear());
        assertEquals(6, result.getStudent().getCurrentSemester());
        assertEquals("IT2021002", result.getStudent().getEnrollmentNumber());
        assertEquals("Java, Spring Boot, AWS", result.getStudent().getSkills());
        assertEquals("Updated bio.", result.getStudent().getBio());
    }

    @Test
    @DisplayName("Unauthenticated update is rejected")
    void testUpdateProfileUnauthenticated() {
        StudentProfileRequest request = buildValidRequest();

        StudentProfileResult result = profileService.updateMyProfile(request);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().toLowerCase().contains("logged in"),
                "Message should mention login: " + result.getMessage());
    }

    @Test
    @DisplayName("Non-student cannot update student profile")
    void testUpdateProfileUnauthorized() {
        com.alumconnect.model.Alumni alumni = new com.alumconnect.model.Alumni(
                "x@y.com", "h", "X", null, "Mech", 2018
        );
        UserSession.getInstance().login(alumni);
        StudentProfileRequest request = buildValidRequest();

        StudentProfileResult result = profileService.updateMyProfile(request);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().toLowerCase().contains("access denied") ||
                result.getMessage().toLowerCase().contains("only students"),
                "Message should indicate unauthorized: " + result.getMessage());
    }

    @Test
    @DisplayName("Update with null request fails gracefully")
    void testUpdateProfileNullRequest() {
        UserSession.getInstance().login(seededStudent);

        StudentProfileResult result = profileService.updateMyProfile(null);

        assertFalse(result.isSuccess());
        assertFalse(result.getMessage().isBlank());
    }

    @Test
    @DisplayName("Validation: missing department is rejected")
    void testUpdateProfileMissingDepartment() {
        UserSession.getInstance().login(seededStudent);

        StudentProfileRequest request = buildValidRequest();
        request.setDepartment("");

        StudentProfileResult result = profileService.updateMyProfile(request);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().toLowerCase().contains("department"),
                "Message should mention department: " + result.getMessage());
    }

    @Test
    @DisplayName("Validation: missing graduation year is rejected")
    void testUpdateProfileMissingGraduationYear() {
        UserSession.getInstance().login(seededStudent);

        StudentProfileRequest request = buildValidRequest();
        request.setGraduationYear(null);

        StudentProfileResult result = profileService.updateMyProfile(request);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().toLowerCase().contains("graduation year"),
                "Message should mention graduation year: " + result.getMessage());
    }

    @Test
    @DisplayName("Validation: invalid graduation year is rejected")
    void testUpdateProfileInvalidGraduationYear() {
        UserSession.getInstance().login(seededStudent);

        StudentProfileRequest request = buildValidRequest();
        request.setGraduationYear(1900); // below MIN_GRAD_YEAR = 1950

        StudentProfileResult result = profileService.updateMyProfile(request);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().toLowerCase().contains("graduation year"),
                "Message should mention graduation year: " + result.getMessage());
    }

    @Test
    @DisplayName("Validation: invalid semester is rejected")
    void testUpdateProfileInvalidSemester() {
        UserSession.getInstance().login(seededStudent);

        StudentProfileRequest request = buildValidRequest();
        request.setCurrentSemester(15); // above MAX_SEMESTER = 12

        StudentProfileResult result = profileService.updateMyProfile(request);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().toLowerCase().contains("semester"),
                "Message should mention semester: " + result.getMessage());
    }

    @Test
    @DisplayName("Validation: department over 100 chars is rejected")
    void testUpdateProfileDepartmentTooLong() {
        UserSession.getInstance().login(seededStudent);

        StudentProfileRequest request = buildValidRequest();
        request.setDepartment("A".repeat(101));

        StudentProfileResult result = profileService.updateMyProfile(request);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().toLowerCase().contains("department"),
                "Message should mention department: " + result.getMessage());
    }

    @Test
    @DisplayName("Validation: LinkedIn URL over 255 chars is rejected")
    void testUpdateProfileLinkedInUrlTooLong() {
        UserSession.getInstance().login(seededStudent);

        StudentProfileRequest request = buildValidRequest();
        request.setLinkedinUrl("https://linkedin.com/" + "x".repeat(250));

        StudentProfileResult result = profileService.updateMyProfile(request);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().toLowerCase().contains("linkedin"),
                "Message should mention LinkedIn URL: " + result.getMessage());
    }

    @Test
    @DisplayName("Duplicate enrollment number is reported with friendly message")
    void testUpdateProfileDuplicateEnrollmentNumber() {
        UserSession.getInstance().login(seededStudent);
        fakeRepository.setThrowDuplicateEnrollmentNumber(true);

        StudentProfileRequest request = buildValidRequest();
        request.setEnrollmentNumber("DUPLICATE_ENROLL");

        StudentProfileResult result = profileService.updateMyProfile(request);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().toLowerCase().contains("enrollment number") ||
                result.getMessage().toLowerCase().contains("already registered"),
                "Message should mention enrollment number conflict: " + result.getMessage());
    }

    @Test
    @DisplayName("Database error during update is handled gracefully")
    void testUpdateProfileDatabaseError() {
        UserSession.getInstance().login(seededStudent);
        fakeRepository.setThrowOnUpdate(true);

        StudentProfileResult result = profileService.updateMyProfile(buildValidRequest());

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().toLowerCase().contains("database"),
                "Message should mention database error: " + result.getMessage());
    }

    @Test
    @DisplayName("Update profile persists changes — findProfile returns updated values")
    void testUpdateProfilePersistence() throws SQLException {
        UserSession.getInstance().login(seededStudent);

        StudentProfileRequest request = buildValidRequest();
        request.setSkills("Kotlin, Android");
        request.setBio("Mobile developer.");

        StudentProfileResult result = profileService.updateMyProfile(request);
        assertTrue(result.isSuccess());

        // Verify persisted via repository
        Optional<Student> persisted = fakeRepository.findProfileByUserId(seededStudent.getId());
        assertTrue(persisted.isPresent());
        assertEquals("Kotlin, Android", persisted.get().getSkills());
        assertEquals("Mobile developer.", persisted.get().getBio());
    }

    @Test
    @DisplayName("validateProfileRequest returns empty list for valid request")
    void testValidateProfileRequestValid() {
        List<String> errors = profileService.validateProfileRequest(buildValidRequest());
        assertTrue(errors.isEmpty(), "Expected no errors: " + errors);
    }

    @Test
    @DisplayName("validateProfileRequest collects multiple errors")
    void testValidateProfileRequestMultipleErrors() {
        StudentProfileRequest request = new StudentProfileRequest();
        request.setDepartment("");
        request.setGraduationYear(null);
        request.setCurrentSemester(0); // invalid
        request.setLinkedinUrl("x".repeat(256)); // too long

        List<String> errors = profileService.validateProfileRequest(request);
        assertTrue(errors.size() >= 3, "Expected multiple errors, got: " + errors);
    }

    // ── Create Profile Tests ──────────────────────────────────────────────────

    @Test
    @DisplayName("createMyProfile: successfully creates profile when none exists")
    void testCreateMyProfileSuccess() throws SQLException {
        Student freshStudent = new Student(
                "fresh@college.edu",
                PasswordUtil.hashPassword("Pass123!"),
                "Fresh Student",
                "+91-9999999999",
                "IT",
                2028
        );
        freshStudent.setId(20);
        UserSession.getInstance().login(freshStudent);

        StudentProfileRequest request = new StudentProfileRequest();
        request.setDepartment("Information Technology");
        request.setGraduationYear(2028);
        request.setCurrentSemester(1);
        request.setSkills("C++, HTML");

        StudentProfileResult result = profileService.createMyProfile(request);

        assertTrue(result.isSuccess());
        assertEquals("Your profile has been created successfully.", result.getMessage());
        assertNotNull(result.getStudent());
        assertEquals("Information Technology", result.getStudent().getDepartment());
        assertEquals(2028, result.getStudent().getGraduationYear());
        assertTrue(fakeRepository.profileExistsForUser(20));
    }

    @Test
    @DisplayName("createMyProfile: fails if profile already exists")
    void testCreateMyProfileAlreadyExists() {
        UserSession.getInstance().login(seededStudent);

        StudentProfileRequest request = buildValidRequest();
        StudentProfileResult result = profileService.createMyProfile(request);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("createMyProfile: fails when unauthenticated")
    void testCreateMyProfileUnauthenticated() {
        StudentProfileRequest request = buildValidRequest();
        StudentProfileResult result = profileService.createMyProfile(request);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Access denied"));
    }

    @Test
    @DisplayName("createMyProfile: fails when role is not STUDENT")
    void testCreateMyProfileUnauthorizedRole() {
        Alumni alumni = new Alumni();
        alumni.setId(99);
        alumni.setRole(Role.ALUMNI);
        alumni.setEmail("alum@college.edu");
        UserSession.getInstance().login(alumni);

        StudentProfileRequest request = buildValidRequest();
        StudentProfileResult result = profileService.createMyProfile(request);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Only students"));
    }

    @Test
    @DisplayName("createMyProfile: fails when request validation fails")
    void testCreateMyProfileValidationFailure() {
        Student freshStudent = new Student();
        freshStudent.setId(30);
        freshStudent.setRole(Role.STUDENT);
        UserSession.getInstance().login(freshStudent);

        StudentProfileRequest request = new StudentProfileRequest(); // empty department, null gradYear
        StudentProfileResult result = profileService.createMyProfile(request);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("validation failed"));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private StudentProfileRequest buildValidRequest() {
        StudentProfileRequest req = new StudentProfileRequest();
        req.setDepartment("Computer Science");
        req.setGraduationYear(2027);
        req.setCurrentSemester(6);
        req.setSkills("Java, Python");
        return req;
    }

    // ── In-Memory Repository Fake ─────────────────────────────────────────────

    private static class InMemoryStudentProfileRepository implements StudentProfileRepository {
        private final Map<Integer, Student> profilesByUserId = new HashMap<>();
        private boolean throwOnFind = false;
        private boolean throwOnUpdate = false;
        private boolean throwDuplicateEnrollmentNumber = false;

        void save(Student student) {
            profilesByUserId.put(student.getId(), student);
        }

        void setThrowOnFind(boolean val) { this.throwOnFind = val; }
        void setThrowOnUpdate(boolean val) { this.throwOnUpdate = val; }
        void setThrowDuplicateEnrollmentNumber(boolean val) {
            this.throwDuplicateEnrollmentNumber = val;
        }

        @Override
        public Optional<Student> findProfileByUserId(int userId) throws SQLException {
            if (throwOnFind) {
                throw new SQLException("Simulated DB connection failure");
            }
            return Optional.ofNullable(profilesByUserId.get(userId));
        }

        @Override
        public boolean updateProfile(Student student) throws SQLException {
            if (throwOnUpdate) {
                throw new SQLException("Simulated DB update error");
            }
            if (throwDuplicateEnrollmentNumber) {
                throw new SQLException(
                        "Duplicate entry for key 'enrollment_number' unique constraint violation");
            }
            if (!profilesByUserId.containsKey(student.getId())) {
                return false;
            }
            profilesByUserId.put(student.getId(), student);
            return true;
        }

        @Override
        public boolean createProfile(Student student) throws SQLException {
            if (throwDuplicateEnrollmentNumber) {
                throw new SQLException(
                        "Duplicate entry for key 'enrollment_number' unique constraint violation");
            }
            profilesByUserId.put(student.getId(), student);
            return true;
        }

        @Override
        public boolean profileExistsForUser(int userId) throws SQLException {
            return profilesByUserId.containsKey(userId);
        }
    }
}
