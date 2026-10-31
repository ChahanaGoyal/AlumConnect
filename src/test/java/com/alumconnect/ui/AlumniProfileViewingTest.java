package com.alumconnect.ui;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.Student;
import com.alumconnect.repository.AlumniProfileRepository;
import com.alumconnect.service.AlumniDirectoryService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Alumni Profile Viewing feature (Prompt 15).
 *
 * <p>Covers:
 * <ul>
 *   <li>Authenticated student can view detailed alumni profile</li>
 *   <li>Correct profile information is displayed (Bio, LinkedIn, mentorship)</li>
 *   <li>Unauthenticated user is denied</li>
 *   <li>Alumni role user is denied from the student viewing flow</li>
 *   <li>Invalid alumni ID handled gracefully</li>
 *   <li>Missing/not-found alumni profile handled gracefully</li>
 *   <li>Database error handled gracefully</li>
 *   <li>Post-listing "view profile" prompt navigation works (0 to go back)</li>
 *   <li>Invalid selection in post-listing prompt handled</li>
 *   <li>Directory listing regression — existing showDirectory() still works</li>
 * </ul>
 */
@DisplayName("Alumni Profile Viewing Tests (Prompt 15)")
class AlumniProfileViewingTest {

    private FakeAlumniProfileRepository fakeRepo;
    private AlumniDirectoryService directoryService;
    private Student testStudent;
    private Alumni testAlumni;

    @BeforeEach
    void setUp() {
        UserSession.getInstance().logout();
        fakeRepo = new FakeAlumniProfileRepository();
        directoryService = new AlumniDirectoryService(fakeRepo);

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
                "priya@thapar.edu",
                PasswordUtil.hashPassword("AlumniP@ss1"),
                "Priya Verma",
                "+91-9811223344",
                "Electronics",
                2020
        );
        testAlumni.setId(2);
        testAlumni.setProfileId(10);
        testAlumni.setDegree("Bachelor of Engineering");
        testAlumni.setDepartment("Electronics and Communication");
        testAlumni.setCurrentCompany("Google");
        testAlumni.setCurrentRole("Senior Software Engineer");
        testAlumni.setIndustry("Technology");
        testAlumni.setYearsOfExperience(5);
        testAlumni.setExpertiseAreas("Distributed Systems, Go, Java");
        testAlumni.setBio("Passionate about open-source and mentoring students.");
        testAlumni.setLinkedinUrl("https://linkedin.com/in/priyaverma");
        testAlumni.setAvailableForMentorship(true);
        testAlumni.setMaxMentees(3);
    }

    @AfterEach
    void tearDown() {
        UserSession.getInstance().logout();
    }

    // ── Helper factory methods ────────────────────────────────────────────────

    private AlumniDirectoryUI buildUI(String input, ByteArrayOutputStream capture) {
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        return new AlumniDirectoryUI(directoryService, in, new PrintStream(capture));
    }

    private AlumniDirectoryUI buildUI(ByteArrayOutputStream capture) {
        return buildUI("", capture);
    }

    // ── Profile viewing via direct method ────────────────────────────────────

    @Test
    @DisplayName("Authenticated student can view a specific alumni's detailed profile")
    void testStudentCanViewAlumniProfile() {
        UserSession.getInstance().login(testStudent);
        fakeRepo.addAlumni(testAlumni);

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        AlumniDirectoryUI ui = buildUI(capture);

        assertDoesNotThrow(() -> ui.viewAlumniProfile(2));

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("ALUMNI PROFILE DETAILS"), "Profile details header must appear");
        assertTrue(output.contains("Priya Verma"), "Full name must be displayed");
    }

    @Test
    @DisplayName("Detailed profile shows all public fields including Bio and LinkedIn")
    void testDetailedProfileShowsAllPublicFields() {
        UserSession.getInstance().login(testStudent);
        fakeRepo.addAlumni(testAlumni);

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        AlumniDirectoryUI ui = buildUI(capture);

        ui.viewAlumniProfile(2);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Priya Verma"), "Name must appear");
        assertTrue(output.contains("2020"), "Graduation year must appear");
        assertTrue(output.contains("Bachelor of Engineering"), "Degree must appear");
        assertTrue(output.contains("Electronics and Communication"), "Department must appear");
        assertTrue(output.contains("Google"), "Company must appear");
        assertTrue(output.contains("Senior Software Engineer"), "Role must appear");
        assertTrue(output.contains("Technology"), "Industry must appear");
        assertTrue(output.contains("5 years"), "Experience must appear");
        assertTrue(output.contains("Distributed Systems, Go, Java"), "Skills must appear");
        assertTrue(output.contains("Passionate about open-source"), "Bio must appear");
        assertTrue(output.contains("linkedin.com/in/priyaverma"), "LinkedIn must appear");
        assertTrue(output.contains("Yes"), "Mentorship availability must appear");
        assertTrue(output.contains("3"), "Max mentees must appear");
    }

    @Test
    @DisplayName("Detailed profile does not expose password hash or salt")
    void testDetailedProfileDoesNotExposePasswordHash() {
        UserSession.getInstance().login(testStudent);
        fakeRepo.addAlumni(testAlumni);

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        AlumniDirectoryUI ui = buildUI(capture);

        ui.viewAlumniProfile(2);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertFalse(output.contains(testAlumni.getPasswordHash()),
                "Password hash must never appear in profile output");
    }

    @Test
    @DisplayName("Unauthenticated user is denied when viewing an alumni profile")
    void testUnauthenticatedUserDeniedProfileView() {
        // No session active
        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        AlumniDirectoryUI ui = buildUI(capture);

        // Should throw SecurityException because validateStudentSession checks auth
        assertThrows(SecurityException.class, () -> ui.viewAlumniProfile(2));
        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Access denied"));
    }

    @Test
    @DisplayName("Alumni role user is denied when using student profile viewing flow")
    void testAlumniRoleDeniedProfileView() {
        UserSession.getInstance().login(testAlumni);

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        AlumniDirectoryUI ui = buildUI(capture);

        assertThrows(SecurityException.class, () -> ui.viewAlumniProfile(2));
        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Access denied"));
    }

    @Test
    @DisplayName("Invalid alumni ID returns error message gracefully")
    void testInvalidAlumniIdHandledGracefully() {
        UserSession.getInstance().login(testStudent);

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        AlumniDirectoryUI ui = buildUI(capture);

        assertDoesNotThrow(() -> ui.viewAlumniProfile(0));

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("[ERROR]"), "Error indicator must appear for invalid ID");
    }

    @Test
    @DisplayName("Alumni profile not found shows friendly error message")
    void testAlumniProfileNotFoundHandledGracefully() {
        UserSession.getInstance().login(testStudent);
        // fakeRepo is empty — no alumni with ID 999

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        AlumniDirectoryUI ui = buildUI(capture);

        assertDoesNotThrow(() -> ui.viewAlumniProfile(999));

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("[ERROR]") || output.contains("not found"),
                "Error or not-found message must appear");
    }

    @Test
    @DisplayName("Database error when viewing profile is handled gracefully")
    void testDatabaseErrorHandledGracefully() {
        UserSession.getInstance().login(testStudent);
        fakeRepo.setThrowSqlException(true);

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        AlumniDirectoryUI ui = buildUI(capture);

        assertDoesNotThrow(() -> ui.viewAlumniProfile(2));

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("[ERROR]") && output.toLowerCase().contains("database error"),
                "Database error must be reported");
    }

    // ── Post-listing prompt navigation (Prompt 15 integration) ───────────────

    @Test
    @DisplayName("Post-directory listing: entering 0 returns to previous menu without error")
    void testPostListingEnterZeroGoesBack() {
        UserSession.getInstance().login(testStudent);
        fakeRepo.addAlumni(testAlumni);

        // After the directory listing, the UI prompts for a selection. "0" means go back.
        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        AlumniDirectoryUI ui = buildUI("0\n", capture);

        assertDoesNotThrow(ui::showDirectory);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Returning to previous menu"), "Back navigation message must appear");
    }

    @Test
    @DisplayName("Post-directory listing: entering valid number shows that alumni's profile")
    void testPostListingValidSelectionShowsProfile() {
        UserSession.getInstance().login(testStudent);
        fakeRepo.addAlumni(testAlumni);

        // Select alumni #1 from the directory list
        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        AlumniDirectoryUI ui = buildUI("1\n", capture);

        assertDoesNotThrow(ui::showDirectory);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("ALUMNI PROFILE DETAILS"), "Profile details section must appear");
        assertTrue(output.contains("Priya Verma"), "Correct alumni name must appear in profile");
    }

    @Test
    @DisplayName("Post-directory listing: out-of-range selection shows error gracefully")
    void testPostListingOutOfRangeSelectionHandled() {
        UserSession.getInstance().login(testStudent);
        fakeRepo.addAlumni(testAlumni);

        // Only 1 alumni in list; selecting 5 is out of range
        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        AlumniDirectoryUI ui = buildUI("5\n", capture);

        assertDoesNotThrow(ui::showDirectory);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Invalid selection"), "Invalid selection error must appear");
    }

    @Test
    @DisplayName("Post-directory listing: non-numeric input shows error gracefully")
    void testPostListingNonNumericInputHandled() {
        UserSession.getInstance().login(testStudent);
        fakeRepo.addAlumni(testAlumni);

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        AlumniDirectoryUI ui = buildUI("abc\n", capture);

        assertDoesNotThrow(ui::showDirectory);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Invalid input"), "Non-numeric input error must appear");
    }

    @Test
    @DisplayName("Post-search listing: selecting an alumni shows their profile")
    void testPostSearchListingValidSelectionShowsProfile() {
        UserSession.getInstance().login(testStudent);
        fakeRepo.addAlumni(testAlumni);

        // Input: keyword "google" then select alumni #1
        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        AlumniDirectoryUI ui = buildUI("google\n1\n", capture);

        assertDoesNotThrow(ui::promptAndSearchAlumni);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("ALUMNI PROFILE DETAILS"), "Profile must appear after search selection");
    }

    @Test
    @DisplayName("Regression: existing showDirectory still works after Prompt 15 changes")
    void testShowDirectoryStillWorksAfterPrompt15() {
        UserSession.getInstance().login(testStudent);
        fakeRepo.addAlumni(testAlumni);

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        // "0" to go back after listing
        AlumniDirectoryUI ui = buildUI("0\n", capture);

        assertDoesNotThrow(ui::showDirectory);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("ALUMNI DIRECTORY"), "Directory header must still appear");
        assertTrue(output.contains("Priya Verma"), "Alumni listing must still show");
        assertTrue(output.contains("Displaying 1 registered alumni:"));
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
            if (throwSqlException) throw new SQLException("Simulated DB error");
            return new ArrayList<>(alumniList);
        }

        @Override
        public List<Alumni> searchAlumni(String keyword) throws SQLException {
            if (throwSqlException) throw new SQLException("Simulated DB error");
            return AlumniProfileRepository.super.searchAlumni(keyword);
        }

        @Override
        public Optional<Alumni> findProfileByUserId(int userId) throws SQLException {
            if (throwSqlException) throw new SQLException("Simulated DB error");
            return alumniList.stream().filter(a -> a.getId() != null && a.getId() == userId).findFirst();
        }

        @Override
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
            return alumniList.stream().anyMatch(a -> a.getId() != null && a.getId() == userId);
        }
    }
}
