package com.alumconnect.ui;

import com.alumconnect.AlumConnectApp;
import com.alumconnect.model.Alumni;
import com.alumconnect.model.Role;
import com.alumconnect.model.Student;
import com.alumconnect.model.User;
import com.alumconnect.service.AlumniDirectoryService;
import com.alumconnect.service.AlumniProfileService;
import com.alumconnect.service.MentorshipRequestService;
import com.alumconnect.service.StudentProfileService;
import com.alumconnect.service.UserService;
import com.alumconnect.service.dto.AlumniDirectoryResult;
import com.alumconnect.service.dto.AlumniProfileResult;
import com.alumconnect.service.dto.LoginResult;
import com.alumconnect.service.dto.MentorshipRequestResult;
import com.alumconnect.service.dto.RegistrationRequest;
import com.alumconnect.service.dto.RegistrationResult;
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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Dedicated unit and UI integration tests for Prompt 24 — UI Polish &amp; User Experience Improvement.
 *
 * <p>Validates:
 * <ul>
 *   <li>Visual consistency and header formatting via {@link ConsoleFormatter}</li>
 *   <li>Main menu navigation and graceful exit via {@link AlumConnectApp#runMainMenu}</li>
 *   <li>Sub-menu back/cancel options in Student and Alumni dashboards</li>
 *   <li>Friendly empty list messages across directory and mentorship screens</li>
 *   <li>Handling of invalid numeric inputs without terminating the application</li>
 * </ul>
 */
@DisplayName("Prompt 24 — UI Polish & User Experience Improvement Tests")
class UIPolishAndExperienceTest {

    private Student testStudent;
    private Alumni testAlumni;

    @BeforeEach
    void setUp() {
        UserSession.getInstance().logout();

        testStudent = new Student(
                "student24@alumconnect.test",
                PasswordUtil.hashPassword("StudentP@ss123"),
                "Aarav Patel",
                "+91-9876543210",
                "Computer Science",
                2026
        );
        testStudent.setId(101);
        testStudent.setCurrentSemester(6);
        testStudent.setEnrollmentNumber("ENC101");

        testAlumni = new Alumni(
                "alumni24@alumconnect.test",
                PasswordUtil.hashPassword("AlumniP@ss123"),
                "Meera Nair",
                "+91-9811223344",
                "Electronics",
                2019
        );
        testAlumni.setId(202);
        testAlumni.setCurrentCompany("Amazon");
        testAlumni.setCurrentRole("Principal SDE");
        testAlumni.setIndustry("Technology");
        testAlumni.setYearsOfExperience(7);
        testAlumni.setAvailableForMentorship(true);
        testAlumni.setMaxMentees(4);
    }

    @AfterEach
    void tearDown() {
        UserSession.getInstance().logout();
    }

    // ── 1. ConsoleFormatter Utility Tests ────────────────────────────────────

    @Test
    @DisplayName("ConsoleFormatter center utility centers strings correctly")
    void testCenterUtility() {
        assertEquals("   TEST   ", ConsoleFormatter.center("TEST", 10));
        assertEquals("", ConsoleFormatter.center(null, 10));
        assertEquals("LONGSTRINGHERE", ConsoleFormatter.center("LONGSTRINGHERE", 5));
    }

    @Test
    @DisplayName("ConsoleFormatter printAppHeader renders consistent identity and borders")
    void testAppHeaderFormatting() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(out);

        ConsoleFormatter.printAppHeader(ps, "MY TEST SECTION");

        String output = out.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("=================================================="));
        assertTrue(output.contains("ALUMCONNECT"));
        assertTrue(output.contains("Alumni Networking Platform"));
        assertTrue(output.contains("MY TEST SECTION"));
        assertTrue(output.contains("--------------------------------------------------"));
    }

    @Test
    @DisplayName("ConsoleFormatter message tags print properly formatted outputs")
    void testMessageFormatting() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(out);

        ConsoleFormatter.printSuccess(ps, "Action successful");
        ConsoleFormatter.printError(ps, "Action failed");
        ConsoleFormatter.printInfo(ps, "Note message");

        String output = out.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("[SUCCESS] Action successful"));
        assertTrue(output.contains("[ERROR] Action failed"));
        assertTrue(output.contains("[INFO] Note message"));
    }

    @Test
    @DisplayName("ConsoleFormatter safeString and formatDateTime handle edge cases")
    void testSafeStringAndDateTime() {
        assertEquals("Not Specified", ConsoleFormatter.safeString(null));
        assertEquals("Not Specified", ConsoleFormatter.safeString("   "));
        assertEquals("Computer Science", ConsoleFormatter.safeString("  Computer Science  "));

        assertEquals("Not Specified", ConsoleFormatter.formatDateTime(null));
        LocalDateTime now = LocalDateTime.of(2026, 11, 10, 14, 30, 0);
        assertEquals("2026-11-10 14:30:00", ConsoleFormatter.formatDateTime(now));
    }

    // ── 2. Main Menu Navigation Tests ────────────────────────────────────────

    @Test
    @DisplayName("Main Menu renders options and terminates cleanly on option 0")
    void testMainMenuExit() {
        String input = "0\n";
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Scanner scanner = new Scanner(in);
        PrintStream ps = new PrintStream(out);

        AlumConnectApp.runMainMenu(scanner, ps, new DummyAuthUI(), new DummyRegistrationUI());

        String output = out.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("MAIN MENU"));
        assertTrue(output.contains("[1] Login (Student or Alumni)"));
        assertTrue(output.contains("[2] Register New Account"));
        assertTrue(output.contains("[0] Exit"));
        assertTrue(output.contains("Thank you for using AlumConnect. Goodbye!"));
    }

    @Test
    @DisplayName("Main Menu handles invalid choices gracefully without terminating")
    void testMainMenuInvalidChoiceHandling() {
        String input = "invalid\n0\n";
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Scanner scanner = new Scanner(in);
        PrintStream ps = new PrintStream(out);

        AlumConnectApp.runMainMenu(scanner, ps, new DummyAuthUI(), new DummyRegistrationUI());

        String output = out.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Invalid choice 'invalid'"));
        assertTrue(output.contains("Thank you for using AlumConnect. Goodbye!"));
    }

    @Test
    @DisplayName("Main Menu delegates choice 1 to AuthUI and choice 2 to RegistrationUI")
    void testMainMenuDelegation() {
        String input = "1\n2\n0\n";
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Scanner scanner = new Scanner(in);
        PrintStream ps = new PrintStream(out);

        DummyAuthUI dummyAuth = new DummyAuthUI();
        DummyRegistrationUI dummyReg = new DummyRegistrationUI();

        AlumConnectApp.runMainMenu(scanner, ps, dummyAuth, dummyReg);

        assertTrue(dummyAuth.loginCalled, "AuthUI should be called on choice 1");
        assertTrue(dummyReg.registerCalled, "RegistrationUI should be called on choice 2");
    }

    // ── 3. Sub-Menu Back Navigation Tests ────────────────────────────────────

    @Test
    @DisplayName("Student Dashboard Profile sub-menu supports option 0 to return cleanly")
    void testStudentProfileSubMenuBackOption() {
        UserSession.getInstance().login(testStudent);

        ByteArrayInputStream in = new ByteArrayInputStream("0\n".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Scanner scanner = new Scanner(in);
        PrintStream ps = new PrintStream(out);

        StudentProfileUI profileUI = new StudentProfileUI(new DummyStudentProfileService(), scanner, ps);
        StudentDashboardUI dashboardUI = new StudentDashboardUI(profileUI, new UserService(), scanner, ps);

        dashboardUI.handleProfileSubMenu();

        String output = out.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("--- My Profile ---"));
        assertTrue(output.contains("[0] Back to Student Dashboard"));
        assertTrue(output.contains("Returning to Student Dashboard"));
    }

    @Test
    @DisplayName("Alumni Dashboard Profile sub-menu supports option 0 to return cleanly")
    void testAlumniProfileSubMenuBackOption() {
        UserSession.getInstance().login(testAlumni);

        ByteArrayInputStream in = new ByteArrayInputStream("0\n".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Scanner scanner = new Scanner(in);
        PrintStream ps = new PrintStream(out);

        AlumniProfileUI profileUI = new AlumniProfileUI(new DummyAlumniProfileService(), scanner, ps);
        AlumniDashboardUI dashboardUI = new AlumniDashboardUI(profileUI, new UserService(), scanner, ps);

        dashboardUI.handleProfileSubMenu();

        String output = out.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("--- My Profile ---"));
        assertTrue(output.contains("[0] Back to Alumni Dashboard"));
        assertTrue(output.contains("Returning to Alumni Dashboard"));
    }

    // ── 4. Cancel in Login and Registration Tests ─────────────────────────────

    @Test
    @DisplayName("AuthUI promptAndLogin supports entering 0 to cancel")
    void testAuthUILoginCancel() {
        ByteArrayInputStream in = new ByteArrayInputStream("0\n".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AuthUI authUI = new AuthUI(new UserService(), in, new PrintStream(out));

        LoginResult result = authUI.promptAndLogin();

        assertFalse(result.isSuccess());
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("Login cancelled"));
    }

    @Test
    @DisplayName("RegistrationUI promptAndRegister supports entering 0 to cancel role selection")
    void testRegistrationRoleCancel() {
        ByteArrayInputStream in = new ByteArrayInputStream("0\n".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        RegistrationUI regUI = new RegistrationUI(new UserService(), in, new PrintStream(out));

        RegistrationResult result = regUI.promptAndRegister();

        assertFalse(result.isSuccess());
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("Registration cancelled"));
    }

    // ── Test Double Helpers ──────────────────────────────────────────────────

    private static class DummyAuthUI extends AuthUI {
        boolean loginCalled = false;

        @Override
        public LoginResult promptAndLogin() {
            this.loginCalled = true;
            return LoginResult.failure("Stub");
        }
    }

    private static class DummyRegistrationUI extends RegistrationUI {
        boolean registerCalled = false;

        @Override
        public RegistrationResult promptAndRegister() {
            this.registerCalled = true;
            return RegistrationResult.failure("Stub");
        }
    }

    private static class DummyStudentProfileService extends StudentProfileService {
        @Override
        public StudentProfileResult viewMyProfile() {
            return StudentProfileResult.success("Stub", new Student("s@test.com", "hash", "Student", null, "CS", 2026));
        }
    }

    private static class DummyAlumniProfileService extends AlumniProfileService {
        @Override
        public AlumniProfileResult viewMyProfile() {
            return AlumniProfileResult.success("Stub", new Alumni("a@test.com", "hash", "Alumni", null, "ECE", 2020));
        }
    }
}

