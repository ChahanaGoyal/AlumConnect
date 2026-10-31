package com.alumconnect.service;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.Role;
import com.alumconnect.model.Student;
import com.alumconnect.model.User;
import com.alumconnect.repository.AlumniProfileRepository;
import com.alumconnect.service.dto.AlumniDirectoryResult;
import com.alumconnect.util.PasswordUtil;
import com.alumconnect.util.UserSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AlumniDirectoryService} — Prompt 12, Prompt 13, Prompt 14 &amp; Prompt 15.
 */
@DisplayName("Alumni Directory, Search, Filter & Profile Viewing Service Tests (Prompt 12–15)")
class AlumniDirectoryServiceTest {

    private FakeAlumniProfileRepository fakeRepository;
    private AlumniDirectoryService directoryService;
    private Student testStudent;
    private Alumni testAlumni;

    @BeforeEach
    void setUp() {
        UserSession.getInstance().logout();
        fakeRepository = new FakeAlumniProfileRepository();
        directoryService = new AlumniDirectoryService(fakeRepository);

        testStudent = new Student(
                "student@thapar.edu",
                PasswordUtil.hashPassword("StudentP@ss1"),
                "Aman Sharma",
                "+91-9876543210",
                "Computer Science",
                2026
        );
        testStudent.setId(1);

        testAlumni = new Alumni(
                "alumni@thapar.edu",
                PasswordUtil.hashPassword("AlumniP@ss1"),
                "Priya Verma",
                "+91-9811223344",
                "Electronics",
                2020
        );
        testAlumni.setId(2);
        testAlumni.setProfileId(20);
        testAlumni.setCurrentCompany("Microsoft");
        testAlumni.setCurrentRole("Staff Engineer");
        testAlumni.setIndustry("Software");
        testAlumni.setYearsOfExperience(4);
        testAlumni.setExpertiseAreas("Cloud Architecture, Java");
        testAlumni.setAvailableForMentorship(true);
    }

    @AfterEach
    void tearDown() {
        UserSession.getInstance().logout();
    }

    // ── Directory browsing tests (Prompt 12) ──────────────────────────────────

    @Test
    @DisplayName("Authenticated student can retrieve alumni directory successfully")
    void testStudentCanRetrieveAlumniDirectory() {
        UserSession.getInstance().login(testStudent);
        fakeRepository.addAlumni(testAlumni);

        AlumniDirectoryResult result = directoryService.getAlumniDirectory();

        assertTrue(result.isSuccess(), "Directory retrieval should succeed for student");
        assertEquals(1, result.getAlumniList().size());
        Alumni retrieved = result.getAlumniList().get(0);
        assertEquals("Priya Verma", retrieved.getFullName());
        assertEquals("Microsoft", retrieved.getCurrentCompany());
        assertEquals("Staff Engineer", retrieved.getCurrentRole());
        assertEquals("Software", retrieved.getIndustry());
        assertEquals(4, retrieved.getYearsOfExperience());
        assertEquals("Cloud Architecture, Java", retrieved.getExpertiseAreas());
        assertTrue(retrieved.isAvailableForMentorship());
    }

    @Test
    @DisplayName("Multiple alumni are correctly retrieved from repository")
    void testMultipleAlumniRetrieved() {
        UserSession.getInstance().login(testStudent);

        Alumni alumni2 = new Alumni(
                "rahul@thapar.edu",
                PasswordUtil.hashPassword("RahulP@ss1"),
                "Rahul Mehta",
                "+91-9800112233",
                "Mechanical",
                2018
        );
        alumni2.setId(3);
        alumni2.setCurrentCompany("Tesla");
        alumni2.setCurrentRole("Design Engineer");

        fakeRepository.addAlumni(testAlumni);
        fakeRepository.addAlumni(alumni2);

        AlumniDirectoryResult result = directoryService.getAlumniDirectory();

        assertTrue(result.isSuccess());
        assertEquals(2, result.getAlumniList().size());
        assertEquals("Priya Verma", result.getAlumniList().get(0).getFullName());
        assertEquals("Rahul Mehta", result.getAlumniList().get(1).getFullName());
    }

    @Test
    @DisplayName("Empty repository returns success with informative no alumni message")
    void testEmptyAlumniDirectoryHandledGracefully() {
        UserSession.getInstance().login(testStudent);

        AlumniDirectoryResult result = directoryService.getAlumniDirectory();

        assertTrue(result.isSuccess());
        assertTrue(result.getAlumniList().isEmpty());
        assertTrue(result.getMessage().toLowerCase().contains("no registered alumni") ||
                   result.getMessage().toLowerCase().contains("no alumni"));
    }

    @Test
    @DisplayName("Unauthenticated user cannot access alumni directory")
    void testUnauthenticatedAccessRejected() {
        // No login session active
        AlumniDirectoryResult result = directoryService.getAlumniDirectory();

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().toLowerCase().contains("logged in"));
        assertTrue(result.getAlumniList().isEmpty());
    }

    @Test
    @DisplayName("Alumni role user cannot access the alumni directory")
    void testAlumniCannotAccessDirectory() {
        UserSession.getInstance().login(testAlumni);

        AlumniDirectoryResult result = directoryService.getAlumniDirectory();

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().toLowerCase().contains("access denied") ||
                   result.getMessage().toLowerCase().contains("only students"));
        assertTrue(result.getAlumniList().isEmpty());
    }

    @Test
    @DisplayName("Logged-out session cannot access the alumni directory")
    void testLoggedOutSessionRejected() {
        UserSession.getInstance().login(testStudent);
        UserSession.getInstance().logout();

        AlumniDirectoryResult result = directoryService.getAlumniDirectory();

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().toLowerCase().contains("logged in"));
        assertTrue(result.getAlumniList().isEmpty());
    }

    @Test
    @DisplayName("Database SQLException is handled gracefully with clear error message")
    void testDatabaseErrorHandledGracefully() {
        UserSession.getInstance().login(testStudent);
        fakeRepository.setThrowSqlException(true);

        AlumniDirectoryResult result = directoryService.getAlumniDirectory();

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().toLowerCase().contains("database error"));
        assertTrue(result.getAlumniList().isEmpty());
    }

    @Test
    @DisplayName("Missing or incomplete profile fields in alumni model are preserved safely")
    void testIncompleteProfileHandledSafely() {
        UserSession.getInstance().login(testStudent);

        // Alumni without completed profile details (null company, null industry, etc.)
        Alumni incompleteAlumni = new Alumni(
                "incomplete@thapar.edu",
                PasswordUtil.hashPassword("Pass#12345"),
                "New Alumni",
                null,
                "Electrical",
                2022
        );
        incompleteAlumni.setId(4);
        fakeRepository.addAlumni(incompleteAlumni);

        AlumniDirectoryResult result = directoryService.getAlumniDirectory();

        assertTrue(result.isSuccess());
        assertEquals(1, result.getAlumniList().size());
        Alumni retrieved = result.getAlumniList().get(0);
        assertEquals("New Alumni", retrieved.getFullName());
        assertNull(retrieved.getCurrentCompany());
        assertNull(retrieved.getCurrentRole());
        assertNull(retrieved.getIndustry());
    }

    // ── Search functionality tests (Prompt 13) ────────────────────────────────

    @Test
    @DisplayName("Search alumni by company name is case-insensitive")
    void testSearchAlumniByCompanyCaseInsensitive() {
        UserSession.getInstance().login(testStudent);
        fakeRepository.addAlumni(testAlumni);

        Alumni googleAlum = new Alumni("g@t.edu", "hash", "Google Guy", null, "CS", 2019);
        googleAlum.setId(10);
        googleAlum.setCurrentCompany("Google LLC");
        googleAlum.setCurrentRole("Software Engineer");
        fakeRepository.addAlumni(googleAlum);

        // lowercase search "google"
        AlumniDirectoryResult resultLower = directoryService.searchAlumni("google");
        assertTrue(resultLower.isSuccess());
        assertEquals(1, resultLower.getAlumniList().size());
        assertEquals("Google Guy", resultLower.getAlumniList().get(0).getFullName());

        // uppercase search "GOOGLE"
        AlumniDirectoryResult resultUpper = directoryService.searchAlumni("GOOGLE");
        assertTrue(resultUpper.isSuccess());
        assertEquals(1, resultUpper.getAlumniList().size());
        assertEquals("Google Guy", resultUpper.getAlumniList().get(0).getFullName());
    }

    @Test
    @DisplayName("Search alumni by skills / expertise is case-insensitive")
    void testSearchAlumniBySkillsCaseInsensitive() {
        UserSession.getInstance().login(testStudent);
        fakeRepository.addAlumni(testAlumni); // has skills: "Cloud Architecture, Java"

        AlumniDirectoryResult result = directoryService.searchAlumni("java");
        assertTrue(result.isSuccess());
        assertEquals(1, result.getAlumniList().size());
        assertEquals("Priya Verma", result.getAlumniList().get(0).getFullName());

        AlumniDirectoryResult resultUpper = directoryService.searchAlumni("JAVA");
        assertTrue(resultUpper.isSuccess());
        assertEquals(1, resultUpper.getAlumniList().size());
    }

    @Test
    @DisplayName("Search alumni by name, role, or industry")
    void testSearchAlumniByNameRoleIndustry() {
        UserSession.getInstance().login(testStudent);
        fakeRepository.addAlumni(testAlumni); // Name: Priya Verma, Role: Staff Engineer, Industry: Software

        // Name
        AlumniDirectoryResult nameRes = directoryService.searchAlumni("priya");
        assertTrue(nameRes.isSuccess());
        assertEquals(1, nameRes.getAlumniList().size());

        // Role
        AlumniDirectoryResult roleRes = directoryService.searchAlumni("staff engineer");
        assertTrue(roleRes.isSuccess());
        assertEquals(1, roleRes.getAlumniList().size());

        // Industry
        AlumniDirectoryResult indRes = directoryService.searchAlumni("software");
        assertTrue(indRes.isSuccess());
        assertEquals(1, indRes.getAlumniList().size());
    }

    @Test
    @DisplayName("Search with no matching alumni returns success with zero results and message")
    void testSearchAlumniNoMatches() {
        UserSession.getInstance().login(testStudent);
        fakeRepository.addAlumni(testAlumni);

        AlumniDirectoryResult result = directoryService.searchAlumni("NonexistentKeywordXYZ");
        assertTrue(result.isSuccess());
        assertTrue(result.getAlumniList().isEmpty());
        assertTrue(result.getMessage().contains("No alumni found matching 'NonexistentKeywordXYZ'"));
    }

    @Test
    @DisplayName("Search with empty or whitespace-only keyword fails gracefully")
    void testSearchAlumniEmptyKeywordFails() {
        UserSession.getInstance().login(testStudent);

        AlumniDirectoryResult nullRes = directoryService.searchAlumni(null);
        assertFalse(nullRes.isSuccess());
        assertTrue(nullRes.getMessage().contains("cannot be empty"));

        AlumniDirectoryResult emptyRes = directoryService.searchAlumni("");
        assertFalse(emptyRes.isSuccess());
        assertTrue(emptyRes.getMessage().contains("cannot be empty"));

        AlumniDirectoryResult blankRes = directoryService.searchAlumni("   ");
        assertFalse(blankRes.isSuccess());
        assertTrue(blankRes.getMessage().contains("cannot be empty"));
    }

    @Test
    @DisplayName("Search alumni by unauthenticated user is rejected")
    void testSearchAlumniUnauthenticatedRejected() {
        AlumniDirectoryResult result = directoryService.searchAlumni("google");
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().toLowerCase().contains("logged in"));
    }

    @Test
    @DisplayName("Search alumni by alumni user is rejected")
    void testSearchAlumniByAlumniRoleRejected() {
        UserSession.getInstance().login(testAlumni);

        AlumniDirectoryResult result = directoryService.searchAlumni("google");
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().toLowerCase().contains("only students"));
    }

    @Test
    @DisplayName("Search alumni handles database error gracefully")
    void testSearchAlumniDatabaseErrorHandledGracefully() {
        UserSession.getInstance().login(testStudent);
        fakeRepository.setThrowSqlException(true);

        AlumniDirectoryResult result = directoryService.searchAlumni("google");
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().toLowerCase().contains("database error"));
    }

    // ── View Alumni Profile tests (Prompt 15) ────────────────────────────────

    @Test
    @DisplayName("Authenticated student can view a specific alumni profile by ID")
    void testStudentCanViewAlumniProfile() {
        UserSession.getInstance().login(testStudent);
        fakeRepository.addAlumni(testAlumni); // id = 2

        AlumniDirectoryResult result = directoryService.viewAlumniProfile(2);

        assertTrue(result.isSuccess(), "Profile viewing should succeed for student");
        assertEquals(1, result.getAlumniList().size());
        Alumni retrieved = result.getAlumniList().get(0);
        assertEquals("Priya Verma", retrieved.getFullName());
        assertEquals("Microsoft", retrieved.getCurrentCompany());
        assertEquals("Staff Engineer", retrieved.getCurrentRole());
        assertEquals("Software", retrieved.getIndustry());
        assertEquals(4, retrieved.getYearsOfExperience());
        assertEquals("Cloud Architecture, Java", retrieved.getExpertiseAreas());
    }

    @Test
    @DisplayName("Unauthenticated user cannot view alumni profile")
    void testViewAlumniProfileUnauthenticatedRejected() {
        // No login
        AlumniDirectoryResult result = directoryService.viewAlumniProfile(2);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().toLowerCase().contains("logged in"));
        assertTrue(result.getAlumniList().isEmpty());
    }

    @Test
    @DisplayName("Alumni role user cannot view alumni profile via student flow")
    void testViewAlumniProfileAlumniRoleRejected() {
        UserSession.getInstance().login(testAlumni);

        AlumniDirectoryResult result = directoryService.viewAlumniProfile(2);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().toLowerCase().contains("only students") ||
                   result.getMessage().toLowerCase().contains("access denied"));
        assertTrue(result.getAlumniList().isEmpty());
    }

    @Test
    @DisplayName("Invalid alumni ID (zero or negative) returns failure")
    void testViewAlumniProfileInvalidIdRejected() {
        UserSession.getInstance().login(testStudent);

        AlumniDirectoryResult zeroResult = directoryService.viewAlumniProfile(0);
        assertFalse(zeroResult.isSuccess());
        assertTrue(zeroResult.getMessage().toLowerCase().contains("invalid alumni id") ||
                   zeroResult.getMessage().toLowerCase().contains("positive"));

        AlumniDirectoryResult negResult = directoryService.viewAlumniProfile(-5);
        assertFalse(negResult.isSuccess());
    }

    @Test
    @DisplayName("Alumni ID not found in repository returns failure")
    void testViewAlumniProfileNotFound() {
        UserSession.getInstance().login(testStudent);
        // Repository is empty — no alumni with ID 999

        AlumniDirectoryResult result = directoryService.viewAlumniProfile(999);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().toLowerCase().contains("not found") ||
                   result.getMessage().contains("999"));
        assertTrue(result.getAlumniList().isEmpty());
    }

    @Test
    @DisplayName("Database SQLException is handled gracefully when viewing alumni profile")
    void testViewAlumniProfileDatabaseErrorHandled() {
        UserSession.getInstance().login(testStudent);
        fakeRepository.setThrowSqlException(true);

        AlumniDirectoryResult result = directoryService.viewAlumniProfile(2);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().toLowerCase().contains("database error"));
        assertTrue(result.getAlumniList().isEmpty());
    }

    // ── In-Memory Fake Repository ─────────────────────────────────────────────
    private static class FakeAlumniProfileRepository implements AlumniProfileRepository {
        private final List<Alumni> alumniList = new ArrayList<>();
        private boolean throwSqlException = false;

        public void addAlumni(Alumni alumni) {
            alumniList.add(alumni);
        }

        public void setThrowSqlException(boolean throwSqlException) {
            this.throwSqlException = throwSqlException;
        }

        @Override
        public List<Alumni> findAllAlumni() throws SQLException {
            if (throwSqlException) {
                throw new SQLException("Connection to MySQL lost");
            }
            return new ArrayList<>(alumniList);
        }

        @Override
        public List<Alumni> searchAlumni(String keyword) throws SQLException {
            if (throwSqlException) {
                throw new SQLException("Connection to MySQL lost");
            }
            return AlumniProfileRepository.super.searchAlumni(keyword);
        }

        @Override
        public Optional<Alumni> findProfileByUserId(int userId) throws SQLException {
            if (throwSqlException) {
                throw new SQLException("Connection to MySQL lost");
            }
            return alumniList.stream().filter(a -> a.getId().equals(userId)).findFirst();
        }        @Override
        public boolean updateProfile(Alumni alumni) {
            return true;
        }

        @Override
        public boolean createProfile(Alumni alumni) {
            alumniList.add(alumni);
            return true;
        }

        @Override
        public boolean profileExistsForUser(int userId) {
            return alumniList.stream().anyMatch(a -> a.getId().equals(userId));
        }
    }
}
