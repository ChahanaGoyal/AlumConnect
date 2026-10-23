package com.alumconnect.service;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.Role;
import com.alumconnect.model.Student;
import com.alumconnect.model.User;
import com.alumconnect.repository.AlumniProfileRepository;
import com.alumconnect.service.dto.AlumniProfileRequest;
import com.alumconnect.service.dto.AlumniProfileResult;
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

@DisplayName("Alumni Profile Service Tests")
class AlumniProfileServiceTest {

    private InMemoryAlumniProfileRepository fakeRepository;
    private AlumniProfileService profileService;
    private Alumni seededAlumni;

    @BeforeEach
    void setUp() {
        UserSession.getInstance().logout();
        fakeRepository = new InMemoryAlumniProfileRepository();
        profileService = new AlumniProfileService(fakeRepository);

        // Seed an alumni profile
        seededAlumni = new Alumni(
                "priya@techcorp.com",
                PasswordUtil.hashPassword("PriyaPass#1"),
                "Priya Sharma",
                "+91-9876543210",
                "Computer Science",
                2018
        );
        seededAlumni.setId(20);
        seededAlumni.setProfileId(201);
        seededAlumni.setCurrentCompany("TechCorp");
        seededAlumni.setCurrentRole("Staff Architect");
        seededAlumni.setIndustry("Software");
        seededAlumni.setYearsOfExperience(6);
        seededAlumni.setExpertiseAreas("Microservices, Cloud, Distributed Systems");
        seededAlumni.setBio("Passionate mentor and backend architect.");
        seededAlumni.setLinkedinUrl("https://linkedin.com/in/priyasharma");
        seededAlumni.setAvailableForMentorship(true);
        seededAlumni.setMaxMentees(4);

        fakeRepository.save(seededAlumni);
    }

    @AfterEach
    void tearDown() {
        UserSession.getInstance().logout();
    }

    // ── viewMyProfile ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Logged-in alumni can view their own profile")
    void testViewProfileSuccess() {
        UserSession.getInstance().login(seededAlumni);

        AlumniProfileResult result = profileService.viewMyProfile();

        assertTrue(result.isSuccess());
        assertNotNull(result.getAlumni());
        assertEquals("Priya Sharma", result.getAlumni().getFullName());
        assertEquals("TechCorp", result.getAlumni().getCurrentCompany());
        assertEquals(6, result.getAlumni().getYearsOfExperience());
        assertEquals(4, result.getAlumni().getMaxMentees());
        assertTrue(result.getAlumni().isAvailableForMentorship());
    }

    @Test
    @DisplayName("viewMyProfile: unauthenticated access fails")
    void testViewProfileUnauthenticated() {
        AlumniProfileResult result = profileService.viewMyProfile();

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("must be logged in"));
    }

    @Test
    @DisplayName("viewMyProfile: unauthorized role (Student) cannot view alumni profile")
    void testViewProfileUnauthorizedRole() {
        Student student = new Student();
        student.setId(5);
        student.setRole(Role.STUDENT);
        UserSession.getInstance().login(student);

        AlumniProfileResult result = profileService.viewMyProfile();

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Only alumni"));
    }

    @Test
    @DisplayName("viewMyProfile: profile not found error")
    void testViewProfileNotFound() {
        Alumni unseededAlumni = new Alumni();
        unseededAlumni.setId(999);
        unseededAlumni.setRole(Role.ALUMNI);
        UserSession.getInstance().login(unseededAlumni);

        AlumniProfileResult result = profileService.viewMyProfile();

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Profile not found"));
    }

    @Test
    @DisplayName("viewMyProfile: handles database error gracefully")
    void testViewProfileDatabaseError() {
        UserSession.getInstance().login(seededAlumni);
        fakeRepository.setThrowOnFind(true);

        AlumniProfileResult result = profileService.viewMyProfile();

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Database error"));
    }

    // ── updateMyProfile ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Logged-in alumni can update their profile successfully")
    void testUpdateProfileSuccess() throws SQLException {
        UserSession.getInstance().login(seededAlumni);

        AlumniProfileRequest request = new AlumniProfileRequest();
        request.setDepartment("Information Technology");
        request.setGraduationYear(2017);
        request.setCurrentCompany("Google");
        request.setCurrentRole("Principal Engineer");
        request.setIndustry("Technology & Cloud");
        request.setYearsOfExperience(8);
        request.setExpertiseAreas("Distributed Systems, Kubernetes");
        request.setBio("Updated bio for Priya.");
        request.setLinkedinUrl("https://linkedin.com/in/priyaupdated");
        request.setAvailableForMentorship(false);
        request.setMaxMentees(2);

        AlumniProfileResult result = profileService.updateMyProfile(request);

        assertTrue(result.isSuccess());
        Alumni updated = result.getAlumni();
        assertEquals("Information Technology", updated.getDepartment());
        assertEquals(2017, updated.getGraduationYear());
        assertEquals("Google", updated.getCurrentCompany());
        assertEquals("Principal Engineer", updated.getCurrentRole());
        assertEquals(8, updated.getYearsOfExperience());
        assertFalse(updated.isAvailableForMentorship());
        assertEquals(2, updated.getMaxMentees());

        // Verify persistence in fake repo
        Alumni persisted = fakeRepository.findProfileByUserId(seededAlumni.getId()).orElseThrow();
        assertEquals("Google", persisted.getCurrentCompany());
        assertEquals(8, persisted.getYearsOfExperience());
    }

    @Test
    @DisplayName("updateMyProfile: unauthenticated access fails")
    void testUpdateProfileUnauthenticated() {
        AlumniProfileRequest request = buildValidRequest();
        AlumniProfileResult result = profileService.updateMyProfile(request);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("must be logged in"));
    }

    @Test
    @DisplayName("updateMyProfile: unauthorized role (Student) fails")
    void testUpdateProfileUnauthorizedRole() {
        Student student = new Student();
        student.setId(5);
        student.setRole(Role.STUDENT);
        UserSession.getInstance().login(student);

        AlumniProfileRequest request = buildValidRequest();
        AlumniProfileResult result = profileService.updateMyProfile(request);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Only alumni"));
    }

    @Test
    @DisplayName("updateMyProfile: null request fails")
    void testUpdateProfileNullRequest() {
        UserSession.getInstance().login(seededAlumni);

        AlumniProfileResult result = profileService.updateMyProfile(null);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("must not be null"));
    }

    @Test
    @DisplayName("updateMyProfile: missing required fields fails validation")
    void testUpdateProfileMissingRequiredFields() {
        UserSession.getInstance().login(seededAlumni);

        AlumniProfileRequest request = new AlumniProfileRequest();
        // missing department and graduationYear
        AlumniProfileResult result = profileService.updateMyProfile(request);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("validation failed"));
    }

    @Test
    @DisplayName("updateMyProfile: out-of-range graduation year fails validation")
    void testUpdateProfileInvalidGradYear() {
        UserSession.getInstance().login(seededAlumni);

        AlumniProfileRequest request = buildValidRequest();
        request.setGraduationYear(1900); // invalid
        AlumniProfileResult result = profileService.updateMyProfile(request);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Graduation year must be between"));
    }

    @Test
    @DisplayName("updateMyProfile: negative experience or mentees fails validation")
    void testUpdateProfileInvalidNumbers() {
        UserSession.getInstance().login(seededAlumni);

        AlumniProfileRequest req1 = buildValidRequest();
        req1.setYearsOfExperience(-1);
        AlumniProfileResult res1 = profileService.updateMyProfile(req1);
        assertFalse(res1.isSuccess());
        assertTrue(res1.getMessage().contains("Years of experience cannot be negative"));

        AlumniProfileRequest req2 = buildValidRequest();
        req2.setMaxMentees(-5);
        AlumniProfileResult res2 = profileService.updateMyProfile(req2);
        assertFalse(res2.isSuccess());
        assertTrue(res2.getMessage().contains("Max mentees cannot be negative"));
    }

    @Test
    @DisplayName("updateMyProfile: field length bounds validation")
    void testUpdateProfileFieldLengthBounds() {
        UserSession.getInstance().login(seededAlumni);

        AlumniProfileRequest request = buildValidRequest();
        request.setDepartment("A".repeat(101));
        request.setCurrentCompany("B".repeat(151));
        request.setCurrentRole("C".repeat(101));
        request.setIndustry("D".repeat(101));
        request.setLinkedinUrl("https://" + "E".repeat(250));

        List<String> errors = profileService.validateProfileRequest(request);
        assertEquals(5, errors.size());
    }

    @Test
    @DisplayName("updateMyProfile: profile not found in repository fails")
    void testUpdateProfileNotFound() {
        Alumni unseeded = new Alumni();
        unseeded.setId(555);
        unseeded.setRole(Role.ALUMNI);
        UserSession.getInstance().login(unseeded);

        AlumniProfileRequest request = buildValidRequest();
        AlumniProfileResult result = profileService.updateMyProfile(request);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Profile not found"));
    }

    @Test
    @DisplayName("updateMyProfile: handles database update failure")
    void testUpdateProfileDatabaseError() {
        UserSession.getInstance().login(seededAlumni);
        fakeRepository.setThrowOnUpdate(true);

        AlumniProfileRequest request = buildValidRequest();
        AlumniProfileResult result = profileService.updateMyProfile(request);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Database error"));
    }

    // ── createMyProfile ───────────────────────────────────────────────────────

    @Test
    @DisplayName("createMyProfile: successfully creates profile for fresh alumni")
    void testCreateMyProfileSuccess() throws SQLException {
        Alumni freshAlumni = new Alumni();
        freshAlumni.setId(40);
        freshAlumni.setEmail("fresh@alumni.org");
        freshAlumni.setFullName("Fresh Alum");
        freshAlumni.setRole(Role.ALUMNI);
        UserSession.getInstance().login(freshAlumni);

        AlumniProfileRequest request = buildValidRequest();
        request.setCurrentCompany("Amazon");
        request.setYearsOfExperience(3);

        AlumniProfileResult result = profileService.createMyProfile(request);

        assertTrue(result.isSuccess());
        assertNotNull(result.getAlumni());
        assertEquals("Fresh Alum", result.getAlumni().getFullName());
        assertEquals("Amazon", result.getAlumni().getCurrentCompany());
        assertTrue(fakeRepository.profileExistsForUser(40));
    }

    @Test
    @DisplayName("createMyProfile: fails if profile already exists")
    void testCreateMyProfileAlreadyExists() {
        UserSession.getInstance().login(seededAlumni);

        AlumniProfileRequest request = buildValidRequest();
        AlumniProfileResult result = profileService.createMyProfile(request);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("createMyProfile: unauthenticated access fails")
    void testCreateMyProfileUnauthenticated() {
        AlumniProfileRequest request = buildValidRequest();
        AlumniProfileResult result = profileService.createMyProfile(request);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("must be logged in"));
    }

    @Test
    @DisplayName("createMyProfile: unauthorized role fails")
    void testCreateMyProfileUnauthorizedRole() {
        Student student = new Student();
        student.setId(5);
        student.setRole(Role.STUDENT);
        UserSession.getInstance().login(student);

        AlumniProfileRequest request = buildValidRequest();
        AlumniProfileResult result = profileService.createMyProfile(request);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Only alumni"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AlumniProfileRequest buildValidRequest() {
        AlumniProfileRequest req = new AlumniProfileRequest();
        req.setDepartment("Computer Science");
        req.setGraduationYear(2018);
        req.setCurrentCompany("TechCorp");
        req.setCurrentRole("Senior Engineer");
        req.setYearsOfExperience(5);
        req.setMaxMentees(3);
        req.setAvailableForMentorship(true);
        return req;
    }

    // ── In-Memory Fake Repository ─────────────────────────────────────────────

    private static class InMemoryAlumniProfileRepository implements AlumniProfileRepository {
        private final Map<Integer, Alumni> profilesByUserId = new HashMap<>();
        private boolean throwOnFind = false;
        private boolean throwOnUpdate = false;

        void save(Alumni alumni) {
            profilesByUserId.put(alumni.getId(), alumni);
        }

        void setThrowOnFind(boolean val) { this.throwOnFind = val; }
        void setThrowOnUpdate(boolean val) { this.throwOnUpdate = val; }

        @Override
        public Optional<Alumni> findProfileByUserId(int userId) throws SQLException {
            if (throwOnFind) {
                throw new SQLException("Simulated DB connection failure");
            }
            return Optional.ofNullable(profilesByUserId.get(userId));
        }

        @Override
        public boolean updateProfile(Alumni alumni) throws SQLException {
            if (throwOnUpdate) {
                throw new SQLException("Simulated DB update error");
            }
            if (!profilesByUserId.containsKey(alumni.getId())) {
                return false;
            }
            profilesByUserId.put(alumni.getId(), alumni);
            return true;
        }

        @Override
        public boolean createProfile(Alumni alumni) throws SQLException {
            profilesByUserId.put(alumni.getId(), alumni);
            return true;
        }

        @Override
        public boolean profileExistsForUser(int userId) throws SQLException {
            return profilesByUserId.containsKey(userId);
        }
    }
}
