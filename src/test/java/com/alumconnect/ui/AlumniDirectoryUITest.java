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
 * Unit tests for {@link AlumniDirectoryUI} — Prompt 12.
 */
@DisplayName("Alumni Directory UI Tests (Prompt 12)")
class AlumniDirectoryUITest {

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
        testAlumni.setDepartment("Electronics");
        testAlumni.setCurrentCompany("Google");
        testAlumni.setCurrentRole("Senior Software Engineer");
        testAlumni.setIndustry("Technology");
        testAlumni.setYearsOfExperience(5);
        testAlumni.setExpertiseAreas("Distributed Systems, Go");
        testAlumni.setAvailableForMentorship(true);
    }

    @AfterEach
    void tearDown() {
        UserSession.getInstance().logout();
    }

    private AlumniDirectoryUI buildUI(ByteArrayOutputStream capture) {
        ByteArrayInputStream in = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
        return new AlumniDirectoryUI(directoryService, in, new PrintStream(capture));
    }

    @Test
    @DisplayName("Authenticated student can view alumni directory with all fields displayed")
    void testShowDirectoryDisplaysAllFields() {
        UserSession.getInstance().login(testStudent);
        fakeRepo.addAlumni(testAlumni);

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        AlumniDirectoryUI ui = buildUI(capture);

        assertDoesNotThrow(ui::showDirectory);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("ALUMNI DIRECTORY"), "Header must appear");
        assertTrue(output.contains("Priya Verma"), "Alumni Name must appear");
        assertTrue(output.contains("2020"), "Graduation year must appear");
        assertTrue(output.contains("Bachelor of Engineering"), "Degree must appear");
        assertTrue(output.contains("Electronics"), "Department must appear");
        assertTrue(output.contains("Google"), "Company must appear");
        assertTrue(output.contains("Senior Software Engineer"), "Role must appear");
        assertTrue(output.contains("Technology"), "Industry must appear");
        assertTrue(output.contains("5 years"), "Experience must appear");
        assertTrue(output.contains("Distributed Systems, Go"), "Skills must appear");
        assertTrue(output.contains("Mentorship Available: Yes"), "Mentorship availability must appear");
    }

    @Test
    @DisplayName("Multiple alumni are displayed separately with 1-based indexing")
    void testMultipleAlumniDisplayedSeparately() {
        UserSession.getInstance().login(testStudent);

        Alumni secondAlumni = new Alumni(
                "rahul@thapar.edu",
                PasswordUtil.hashPassword("AlumniP@ss2"),
                "Rahul Mehta",
                "+91-9877665544",
                "Mechanical",
                2019
        );
        secondAlumni.setId(3);
        secondAlumni.setProfileId(11);
        secondAlumni.setCurrentCompany("Amazon");
        secondAlumni.setCurrentRole("SDE II");
        secondAlumni.setYearsOfExperience(3);
        secondAlumni.setAvailableForMentorship(false);

        fakeRepo.addAlumni(testAlumni);
        fakeRepo.addAlumni(secondAlumni);

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        AlumniDirectoryUI ui = buildUI(capture);

        ui.showDirectory();

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("1. Alumni Name         : Priya Verma"));
        assertTrue(output.contains("2. Alumni Name         : Rahul Mehta"));
        assertTrue(output.contains("Mentorship Available: Yes"));
        assertTrue(output.contains("Mentorship Available: No"));
        assertTrue(output.contains("Displaying 2 registered alumni:"));
    }

    @Test
    @DisplayName("Empty directory displays friendly no alumni message")
    void testEmptyDirectoryMessage() {
        UserSession.getInstance().login(testStudent);

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        AlumniDirectoryUI ui = buildUI(capture);

        ui.showDirectory();

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("No registered alumni found"));
    }

    @Test
    @DisplayName("Unauthenticated user is denied access to Alumni Directory")
    void testUnauthenticatedAccessThrowsSecurityException() {
        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        AlumniDirectoryUI ui = buildUI(capture);

        assertThrows(SecurityException.class, ui::showDirectory);
        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Access denied"));
    }

    @Test
    @DisplayName("Alumni role user is denied access to Alumni Directory")
    void testAlumniRoleAccessThrowsSecurityException() {
        UserSession.getInstance().login(testAlumni);

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        AlumniDirectoryUI ui = buildUI(capture);

        assertThrows(SecurityException.class, ui::showDirectory);
        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Access denied"));
    }

    @Test
    @DisplayName("Logged-out session cannot access Alumni Directory")
    void testLoggedOutSessionThrowsSecurityException() {
        UserSession.getInstance().login(testStudent);
        UserSession.getInstance().logout();

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        AlumniDirectoryUI ui = buildUI(capture);

        assertThrows(SecurityException.class, ui::showDirectory);
    }

    @Test
    @DisplayName("Missing and incomplete profile details display Not Specified safely")
    void testIncompleteProfileDisplaysNotSpecified() {
        UserSession.getInstance().login(testStudent);

        Alumni incomplete = new Alumni(
                "incomplete@thapar.edu",
                PasswordUtil.hashPassword("Pass#123"),
                "Sparse Alumni",
                null,
                "Electrical",
                2022
        );
        incomplete.setId(4);
        // Do not set currentCompany, currentRole, industry, expertise, etc.
        fakeRepo.addAlumni(incomplete);

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        AlumniDirectoryUI ui = buildUI(capture);

        assertDoesNotThrow(ui::showDirectory);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Sparse Alumni"));
        assertTrue(output.contains("Not Specified"));
    }

    @Test
    @DisplayName("Database error is surfaced clearly to user")
    void testDatabaseErrorPrintedClearly() {
        UserSession.getInstance().login(testStudent);
        fakeRepo.setThrowException(true);

        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        AlumniDirectoryUI ui = buildUI(capture);

        assertDoesNotThrow(ui::showDirectory);

        String output = capture.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("[ERROR]"));
        assertTrue(output.contains("Database error"));
    }

    // ── In-Memory Fake ────────────────────────────────────────────────────────

    private static class FakeAlumniProfileRepository implements AlumniProfileRepository {
        private final List<Alumni> list = new ArrayList<>();
        private boolean throwException = false;

        public void addAlumni(Alumni a) { list.add(a); }
        public void setThrowException(boolean b) { this.throwException = b; }

        @Override
        public List<Alumni> findAllAlumni() throws SQLException {
            if (throwException) throw new SQLException("Simulated database timeout");
            return new ArrayList<>(list);
        }

        @Override
        public Optional<Alumni> findProfileByUserId(int userId) {
            return list.stream().filter(a -> a.getId().equals(userId)).findFirst();
        }

        @Override public boolean updateProfile(Alumni a) { return true; }
        @Override public boolean createProfile(Alumni a) { list.add(a); return true; }
        @Override public boolean profileExistsForUser(int userId) { return true; }
    }
}

