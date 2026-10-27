package com.alumconnect.service;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.Role;
import com.alumconnect.model.Student;
import com.alumconnect.repository.AlumniProfileRepository;
import com.alumconnect.repository.StudentProfileRepository;
import com.alumconnect.service.dto.AlumniProfileRequest;
import com.alumconnect.service.dto.AlumniProfileResult;
import com.alumconnect.service.dto.StudentProfileRequest;
import com.alumconnect.service.dto.StudentProfileResult;
import com.alumconnect.util.UserSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Profile Persistence & Lifecycle Tests (Prompt 9)")
class ProfilePersistenceTest {

    private InMemoryStudentProfileStore studentDbStore;
    private InMemoryAlumniProfileStore alumniDbStore;

    @BeforeEach
    void setUp() {
        UserSession.getInstance().logout();
        studentDbStore = new InMemoryStudentProfileStore();
        alumniDbStore = new InMemoryAlumniProfileStore();
    }

    @AfterEach
    void tearDown() {
        UserSession.getInstance().logout();
    }

    @Test
    @DisplayName("Student profile persists across application restart / fresh service instances")
    void testStudentProfilePersistenceAcrossRestart() {
        // --- Session 1: Create and persist Student profile ---
        Student student = new Student();
        student.setId(101);
        student.setEmail("student101@college.edu");
        student.setFullName("Aarav Patel");
        student.setRole(Role.STUDENT);
        UserSession.getInstance().login(student);

        // First application session service instance
        StudentProfileService serviceSession1 = new StudentProfileService(studentDbStore);

        StudentProfileRequest createReq = new StudentProfileRequest();
        createReq.setDepartment("Computer Engineering");
        createReq.setGraduationYear(2027);
        createReq.setCurrentSemester(4);
        createReq.setEnrollmentNumber("COE202301");
        createReq.setSkills("Java, SQL, Git");
        createReq.setBio("First session bio");

        StudentProfileResult createRes = serviceSession1.createMyProfile(createReq);
        assertTrue(createRes.isSuccess(), "Creation should succeed: " + createRes.getMessage());

        // Update in session 1
        StudentProfileRequest updateReq = new StudentProfileRequest();
        updateReq.setDepartment("Computer Engineering");
        updateReq.setGraduationYear(2027);
        updateReq.setCurrentSemester(5);
        updateReq.setSkills("Java, SQL, Git, Docker");
        updateReq.setBio("Updated in session 1");

        StudentProfileResult updateRes = serviceSession1.updateMyProfile(updateReq);
        assertTrue(updateRes.isSuccess());

        // --- App Shutdown & Restart: clear memory, session, instantiate fresh services ---
        UserSession.getInstance().logout();
        assertFalse(UserSession.getInstance().isLoggedIn());

        // --- Session 2: User logs back in after restart ---
        Student studentAfterRestart = new Student();
        studentAfterRestart.setId(101);
        studentAfterRestart.setEmail("student101@college.edu");
        studentAfterRestart.setFullName("Aarav Patel");
        studentAfterRestart.setRole(Role.STUDENT);
        UserSession.getInstance().login(studentAfterRestart);

        // Fresh service instance pointing to the persistent storage
        StudentProfileService serviceSession2 = new StudentProfileService(studentDbStore);
        StudentProfileResult viewRes = serviceSession2.viewMyProfile();

        assertTrue(viewRes.isSuccess(), "Profile must be accessible after application restart");
        Student retrieved = viewRes.getStudent();
        assertEquals(101, retrieved.getId());
        assertEquals("Computer Engineering", retrieved.getDepartment());
        assertEquals(2027, retrieved.getGraduationYear());
        assertEquals(5, retrieved.getCurrentSemester());
        assertEquals("Java, SQL, Git, Docker", retrieved.getSkills());
        assertEquals("Updated in session 1", retrieved.getBio());
    }

    @Test
    @DisplayName("Alumni profile persists across application restart / fresh service instances")
    void testAlumniProfilePersistenceAcrossRestart() {
        // --- Session 1: Create and persist Alumni profile ---
        Alumni alumni = new Alumni();
        alumni.setId(202);
        alumni.setEmail("alumni202@techcorp.com");
        alumni.setFullName("Sneha Kapoor");
        alumni.setRole(Role.ALUMNI);
        UserSession.getInstance().login(alumni);

        AlumniProfileService serviceSession1 = new AlumniProfileService(alumniDbStore);

        AlumniProfileRequest createReq = new AlumniProfileRequest();
        createReq.setDepartment("Electronics");
        createReq.setGraduationYear(2019);
        createReq.setCurrentCompany("Intel");
        createReq.setCurrentRole("Hardware Engineer");
        createReq.setIndustry("Semiconductors");
        createReq.setYearsOfExperience(5);
        createReq.setAvailableForMentorship(true);
        createReq.setMaxMentees(3);

        AlumniProfileResult createRes = serviceSession1.createMyProfile(createReq);
        assertTrue(createRes.isSuccess(), "Alumni creation should succeed: " + createRes.getMessage());

        // Update in session 1
        AlumniProfileRequest updateReq = new AlumniProfileRequest();
        updateReq.setDepartment("Electronics");
        updateReq.setGraduationYear(2019);
        updateReq.setCurrentCompany("Qualcomm");
        updateReq.setCurrentRole("Senior SoC Architect");
        updateReq.setIndustry("Semiconductors");
        updateReq.setYearsOfExperience(7);
        updateReq.setAvailableForMentorship(true);
        updateReq.setMaxMentees(5);

        AlumniProfileResult updateRes = serviceSession1.updateMyProfile(updateReq);
        assertTrue(updateRes.isSuccess());

        // --- App Shutdown & Restart: clear memory, session, instantiate fresh services ---
        UserSession.getInstance().logout();

        // --- Session 2: User logs back in after restart ---
        Alumni alumniAfterRestart = new Alumni();
        alumniAfterRestart.setId(202);
        alumniAfterRestart.setEmail("alumni202@techcorp.com");
        alumniAfterRestart.setFullName("Sneha Kapoor");
        alumniAfterRestart.setRole(Role.ALUMNI);
        UserSession.getInstance().login(alumniAfterRestart);

        AlumniProfileService serviceSession2 = new AlumniProfileService(alumniDbStore);
        AlumniProfileResult viewRes = serviceSession2.viewMyProfile();

        assertTrue(viewRes.isSuccess(), "Alumni profile must be accessible after application restart");
        Alumni retrieved = viewRes.getAlumni();
        assertEquals(202, retrieved.getId());
        assertEquals("Qualcomm", retrieved.getCurrentCompany());
        assertEquals("Senior SoC Architect", retrieved.getCurrentRole());
        assertEquals(7, retrieved.getYearsOfExperience());
        assertEquals(5, retrieved.getMaxMentees());
    }

    @Test
    @DisplayName("Profile separation: Student cannot access or alter Alumni profiles")
    void testStudentCannotAccessAlumniProfile() {
        Student student = new Student();
        student.setId(101);
        student.setRole(Role.STUDENT);
        UserSession.getInstance().login(student);

        AlumniProfileService alumniService = new AlumniProfileService(alumniDbStore);

        AlumniProfileResult viewRes = alumniService.viewMyProfile();
        assertFalse(viewRes.isSuccess());
        assertTrue(viewRes.getMessage().contains("Only alumni"));

        AlumniProfileRequest req = new AlumniProfileRequest();
        req.setDepartment("CS");
        req.setGraduationYear(2020);

        AlumniProfileResult createRes = alumniService.createMyProfile(req);
        assertFalse(createRes.isSuccess());
        assertTrue(createRes.getMessage().contains("Only alumni"));

        AlumniProfileResult updateRes = alumniService.updateMyProfile(req);
        assertFalse(updateRes.isSuccess());
        assertTrue(updateRes.getMessage().contains("Only alumni"));
    }

    @Test
    @DisplayName("Profile separation: Alumni cannot access or alter Student profiles")
    void testAlumniCannotAccessStudentProfile() {
        Alumni alumni = new Alumni();
        alumni.setId(202);
        alumni.setRole(Role.ALUMNI);
        UserSession.getInstance().login(alumni);

        StudentProfileService studentService = new StudentProfileService(studentDbStore);

        StudentProfileResult viewRes = studentService.viewMyProfile();
        assertFalse(viewRes.isSuccess());
        assertTrue(viewRes.getMessage().contains("Only students"));

        StudentProfileRequest req = new StudentProfileRequest();
        req.setDepartment("CS");
        req.setGraduationYear(2026);

        StudentProfileResult createRes = studentService.createMyProfile(req);
        assertFalse(createRes.isSuccess());
        assertTrue(createRes.getMessage().contains("Only students"));

        StudentProfileResult updateRes = studentService.updateMyProfile(req);
        assertFalse(updateRes.isSuccess());
        assertTrue(updateRes.getMessage().contains("Only students"));
    }

    @Test
    @DisplayName("User ownership isolation: User cannot view or update another user's profile")
    void testUserOwnershipIsolation() {
        // User A creates profile
        Student studentA = new Student();
        studentA.setId(101);
        studentA.setEmail("a@college.edu");
        studentA.setFullName("Student A");
        studentA.setRole(Role.STUDENT);
        UserSession.getInstance().login(studentA);

        StudentProfileService service = new StudentProfileService(studentDbStore);
        StudentProfileRequest reqA = new StudentProfileRequest();
        reqA.setDepartment("CSE");
        reqA.setGraduationYear(2026);
        service.createMyProfile(reqA);

        // User B logs in (has no profile yet)
        Student studentB = new Student();
        studentB.setId(102);
        studentB.setEmail("b@college.edu");
        studentB.setFullName("Student B");
        studentB.setRole(Role.STUDENT);
        UserSession.getInstance().login(studentB);

        StudentProfileResult viewB = service.viewMyProfile();
        assertFalse(viewB.isSuccess(), "Student B must not see Student A's profile");
        assertTrue(viewB.getMessage().contains("Profile not found"));

        // User B updates profile - should fail because B has no profile to update
        StudentProfileRequest updateB = new StudentProfileRequest();
        updateB.setDepartment("IT");
        updateB.setGraduationYear(2027);
        StudentProfileResult updateResB = service.updateMyProfile(updateB);
        assertFalse(updateResB.isSuccess(), "Student B cannot update when profile does not exist");
    }

    @Test
    @DisplayName("Handling database connection and constraint errors gracefully")
    void testDatabaseErrorsAndConstraints() {
        Student student = new Student();
        student.setId(301);
        student.setEmail("error.test@college.edu");
        student.setFullName("Error Test Student");
        student.setRole(Role.STUDENT);
        UserSession.getInstance().login(student);

        studentDbStore.setSimulateDbError(true);
        StudentProfileService service = new StudentProfileService(studentDbStore);

        StudentProfileResult viewRes = service.viewMyProfile();
        assertFalse(viewRes.isSuccess());
        assertTrue(viewRes.getMessage().toLowerCase().contains("database error"));

        studentDbStore.setSimulateDbError(false);
        studentDbStore.setSimulateUniqueConstraintViolation(true);

        StudentProfileRequest req = new StudentProfileRequest();
        req.setDepartment("IT");
        req.setGraduationYear(2027);
        req.setEnrollmentNumber("DUPLICATE_NO");

        StudentProfileResult createRes = service.createMyProfile(req);
        assertFalse(createRes.isSuccess());
        assertTrue(createRes.getMessage().toLowerCase().contains("already registered") ||
                createRes.getMessage().toLowerCase().contains("already exists"));
    }

    // ── In-Memory Persistence Fakes ───────────────────────────────────────────

    private static class InMemoryStudentProfileStore implements StudentProfileRepository {
        private final Map<Integer, Student> table = new HashMap<>();
        private boolean simulateDbError = false;
        private boolean simulateUniqueConstraintViolation = false;

        void setSimulateDbError(boolean val) { this.simulateDbError = val; }
        void setSimulateUniqueConstraintViolation(boolean val) { this.simulateUniqueConstraintViolation = val; }

        @Override
        public Optional<Student> findProfileByUserId(int userId) throws SQLException {
            if (simulateDbError) throw new SQLException("Connection pool exhausted");
            return Optional.ofNullable(table.get(userId));
        }

        @Override
        public boolean updateProfile(Student student) throws SQLException {
            if (simulateDbError) throw new SQLException("Database connection reset");
            if (simulateUniqueConstraintViolation) {
                throw new SQLException("Duplicate entry for key 'enrollment_number'");
            }
            if (!table.containsKey(student.getId())) return false;
            table.put(student.getId(), student);
            return true;
        }

        @Override
        public boolean createProfile(Student student) throws SQLException {
            if (simulateDbError) throw new SQLException("Disk full / connection timeout");
            if (simulateUniqueConstraintViolation) {
                throw new SQLException("Duplicate entry for key 'enrollment_number'");
            }
            table.put(student.getId(), student);
            return true;
        }

        @Override
        public boolean profileExistsForUser(int userId) throws SQLException {
            if (simulateDbError) throw new SQLException("Connection failed");
            return table.containsKey(userId);
        }
    }

    private static class InMemoryAlumniProfileStore implements AlumniProfileRepository {
        private final Map<Integer, Alumni> table = new HashMap<>();
        private boolean simulateDbError = false;

        void setSimulateDbError(boolean val) { this.simulateDbError = val; }

        @Override
        public Optional<Alumni> findProfileByUserId(int userId) throws SQLException {
            if (simulateDbError) throw new SQLException("Connection pool timeout");
            return Optional.ofNullable(table.get(userId));
        }

        @Override
        public boolean updateProfile(Alumni alumni) throws SQLException {
            if (simulateDbError) throw new SQLException("Connection reset by peer");
            if (!table.containsKey(alumni.getId())) return false;
            table.put(alumni.getId(), alumni);
            return true;
        }

        @Override
        public boolean createProfile(Alumni alumni) throws SQLException {
            if (simulateDbError) throw new SQLException("Connection timeout");
            table.put(alumni.getId(), alumni);
            return true;
        }

        @Override
        public boolean profileExistsForUser(int userId) throws SQLException {
            if (simulateDbError) throw new SQLException("Connection failed");
            return table.containsKey(userId);
        }
    }
}
