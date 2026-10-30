package com.alumconnect.ui;

import com.alumconnect.model.Role;
import com.alumconnect.model.Student;
import com.alumconnect.model.User;
import com.alumconnect.service.AlumniDirectoryService;
import com.alumconnect.service.StudentProfileService;
import com.alumconnect.service.UserService;
import com.alumconnect.util.UserSession;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

/**
 * Console UI for the Student Dashboard (Prompt 10, Prompt 12 &amp; Prompt 13).
 *
 * <p>Displays a role-gated menu to an authenticated Student and routes input
 * to the appropriate sub-feature UIs. The dashboard can only be entered by a
 * user whose session role is {@code STUDENT}; any other access (unauthenticated,
 * wrong role) is rejected immediately with a clear error message.
 *
 * <p>Only features that are fully implemented in the project are exposed here.
 * Placeholder menu items for future prompts are intentionally omitted to
 * prevent students from reaching unimplemented functionality.
 *
 * <p>Implemented features accessible from this dashboard:
 * <ol>
 *   <li>My Profile: view, create, edit student profile</li>
 *   <li>Browse Alumni: explore registered alumni directory (Prompt 12)</li>
 *   <li>Search Alumni: search alumni by name, company, role, industry, skills (Prompt 13)</li>
 *   <li>Logout</li>
 *   <li>Exit</li>
 * </ol>
 *
 * <p>Business logic is never duplicated here; the dashboard delegates to
 * {@link StudentProfileService} via {@link StudentProfileUI},
 * {@link AlumniDirectoryService} via {@link AlumniDirectoryUI},
 * and {@link UserService} for session management.
 */
public class StudentDashboardUI {

    // ── Dashboard menu option constants ──────────────────────────────────────
    /** Menu option: show the profile sub-menu (view / create / edit). */
    public static final String OPTION_PROFILE       = "1";
    /** Menu option: browse the alumni directory. */
    public static final String OPTION_BROWSE_ALUMNI = "2";
    /** Menu option: search alumni directory. */
    public static final String OPTION_SEARCH_ALUMNI = "3";
    /** Menu option: log the student out. */
    public static final String OPTION_LOGOUT        = "9";
    /** Menu option: exit the application. */
    public static final String OPTION_EXIT          = "0";

    private final StudentProfileUI studentProfileUI;
    private final AlumniDirectoryUI alumniDirectoryUI;
    private final UserService userService;
    private final Scanner scanner;
    private final PrintStream out;

    // ── Constructors ─────────────────────────────────────────────────────────

    /**
     * Production constructor — wires real services and uses {@code System.in/out}.
     */
    public StudentDashboardUI() {
        this(new StudentProfileUI(), new AlumniDirectoryUI(), new UserService(), System.in, System.out);
    }

    /**
     * Convenience constructor for callers that already own a {@link StudentProfileUI}.
     *
     * @param studentProfileUI the student profile UI instance to delegate to
     * @param userService      the user service for logout operations
     * @param in               input stream
     * @param out              output stream
     */
    public StudentDashboardUI(StudentProfileUI studentProfileUI,
                              UserService userService,
                              InputStream in,
                              PrintStream out) {
        this(studentProfileUI, userService, new Scanner(in), out);
    }

    /**
     * Constructor accepting a shared {@link Scanner} so the dashboard and its
     * parent UI (e.g. {@link AuthUI}) read from the same stream without competing.
     *
     * @param studentProfileUI the student profile UI instance
     * @param userService      the user service
     * @param scanner          a pre-created, shared Scanner
     * @param out              output stream
     */
    public StudentDashboardUI(StudentProfileUI studentProfileUI,
                              UserService userService,
                              Scanner scanner,
                              PrintStream out) {
        this(studentProfileUI,
             new AlumniDirectoryUI(new AlumniDirectoryService(), scanner, out),
             userService,
             scanner,
             out);
    }

    /**
     * Constructor allowing injection of both profile and alumni directory UIs.
     *
     * @param studentProfileUI  the student profile UI
     * @param alumniDirectoryUI the alumni directory UI
     * @param userService       the user service
     * @param in                input stream
     * @param out               output stream
     */
    public StudentDashboardUI(StudentProfileUI studentProfileUI,
                              AlumniDirectoryUI alumniDirectoryUI,
                              UserService userService,
                              InputStream in,
                              PrintStream out) {
        this(studentProfileUI, alumniDirectoryUI, userService, new Scanner(in), out);
    }

    /**
     * Full dependency-injection constructor with shared Scanner.
     *
     * @param studentProfileUI  the student profile UI
     * @param alumniDirectoryUI the alumni directory UI
     * @param userService       the user service
     * @param scanner           a pre-created, shared Scanner
     * @param out               output stream
     */
    public StudentDashboardUI(StudentProfileUI studentProfileUI,
                              AlumniDirectoryUI alumniDirectoryUI,
                              UserService userService,
                              Scanner scanner,
                              PrintStream out) {
        this.studentProfileUI = studentProfileUI;
        this.alumniDirectoryUI = alumniDirectoryUI;
        this.userService = userService;
        this.scanner = scanner;
        this.out = out;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Entry point for the Student Dashboard.
     *
     * <p>Validates that the current session belongs to an authenticated Student;
     * displays the welcome header and menu; then reads and dispatches exactly
     * one menu selection. Returns immediately after dispatching — the looping
     * behaviour (if any) is the responsibility of the caller (e.g. {@link AuthUI}).
     *
     * @throws SecurityException if no user is logged in or the logged-in user
     *                           is not a Student
     */
    public void show() {
        // ── 1. Authentication guard ───────────────────────────────────────────
        UserSession session = UserSession.getInstance();

        if (!session.isLoggedIn()) {
            out.println("[AlumConnect] Access denied: You must be logged in to view the Student Dashboard.");
            throw new SecurityException("Unauthorized: No user is currently logged in.");
        }

        if (!session.isStudent()) {
            out.println("[AlumConnect] Access denied: The Student Dashboard is only available to Students.");
            throw new SecurityException(
                    "Access Denied: role [" + session.getCurrentRole().orElse(null) +
                    "] cannot access the Student Dashboard.");
        }

        // ── 2. Retrieve the current student from the session ─────────────────
        User currentUser = session.getCurrentUser().orElseThrow(
                () -> new SecurityException("Unauthorized: session user is missing."));

        if (!(currentUser instanceof Student student)) {
            out.println("[AlumConnect] Access denied: User is not a Student instance.");
            throw new SecurityException("Unexpected user type; expected Student.");
        }

        // ── 3. Print welcome header and menu ─────────────────────────────────
        printHeader(student);

        // ── 4. Read and dispatch one menu selection ───────────────────────────
        handleMenuSelection(student);
    }

    // ── Menu rendering ────────────────────────────────────────────────────────

    /**
     * Prints the dashboard header and the main menu.
     *
     * @param student the currently logged-in Student
     */
    private void printHeader(Student student) {
        out.println("\n==================================================");
        out.println("               STUDENT DASHBOARD                  ");
        out.println("==================================================");
        out.println("Welcome, " + student.getFullName() + "!");
        out.println("Department : " + student.getDepartment()
                + " | Semester: " + student.getCurrentSemester()
                + " | Grad: " + student.getGraduationYear());
        if (student.getEnrollmentNumber() != null && !student.getEnrollmentNumber().isBlank()) {
            out.println("Enrollment : " + student.getEnrollmentNumber());
        }
        if (student.getSkills() != null && !student.getSkills().isBlank()) {
            out.println("Skills     : " + student.getSkills());
        }
        out.println("--------------------------------------------------");
        out.println("Student Menu:");
        out.println("  [" + OPTION_PROFILE       + "] My Profile");
        out.println("  [" + OPTION_BROWSE_ALUMNI + "] Browse Alumni");
        out.println("  [" + OPTION_SEARCH_ALUMNI + "] Search Alumni");
        out.println("  [" + OPTION_LOGOUT        + "] Logout");
        out.println("  [" + OPTION_EXIT          + "] Exit");
        out.println("--------------------------------------------------");
    }

    // ── Menu dispatch ─────────────────────────────────────────────────────────

    /**
     * Reads one menu selection from the input stream and dispatches it.
     * Invalid or unrecognised input is handled gracefully with a clear message.
     *
     * @param student the currently logged-in Student (passed to sub-menus)
     */
    public void handleMenuSelection(Student student) {
        if (!scanner.hasNextLine()) {
            // Input stream is exhausted (e.g., piped test input) — treat as no-op.
            return;
        }

        out.print("Enter your choice: ");
        String choice = scanner.nextLine().trim();

        switch (choice) {
            case OPTION_PROFILE       -> handleProfileSubMenu();
            case OPTION_BROWSE_ALUMNI -> handleBrowseAlumni();
            case OPTION_SEARCH_ALUMNI -> handleSearchAlumni();
            case OPTION_LOGOUT        -> handleLogout();
            case OPTION_EXIT          -> {
                out.println("\n[AlumConnect] Goodbye! Thank you for using AlumConnect.");
            }
            default -> out.println("[AlumConnect] Invalid choice '" + choice
                    + "'. Please enter one of the listed options.");
        }
    }

    // ── Browse Alumni ─────────────────────────────────────────────────────────

    /**
     * Opens the Alumni Directory UI for the student to browse available alumni.
     */
    public void handleBrowseAlumni() {
        try {
            alumniDirectoryUI.showDirectory();
        } catch (SecurityException e) {
            out.println("[AlumConnect] Access denied: " + e.getMessage());
        } catch (Exception e) {
            out.println("[AlumConnect] An unexpected error occurred: " + e.getMessage());
        }
    }

    // ── Search Alumni ─────────────────────────────────────────────────────────

    /**
     * Opens the Alumni Search prompt for the student to search available alumni.
     */
    public void handleSearchAlumni() {
        try {
            alumniDirectoryUI.promptAndSearchAlumni();
        } catch (SecurityException e) {
            out.println("[AlumConnect] Access denied: " + e.getMessage());
        } catch (Exception e) {
            out.println("[AlumConnect] An unexpected error occurred: " + e.getMessage());
        }
    }

    // ── Profile sub-menu ──────────────────────────────────────────────────────

    /**
     * Shows the Profile sub-menu and delegates to {@link StudentProfileUI}.
     * Handles missing profile and database errors with user-friendly messages.
     */
    public void handleProfileSubMenu() {
        out.println("\n--- My Profile ---");
        out.println("  [V] View my profile");
        out.println("  [C] Create my profile");
        out.println("  [E] Edit / Update my profile");
        out.print("Select: ");

        if (!scanner.hasNextLine()) {
            return;
        }
        String sub = scanner.nextLine().trim().toUpperCase();

        try {
            switch (sub) {
                case "V" -> studentProfileUI.displayMyProfile();
                case "C" -> studentProfileUI.promptAndCreateProfile();
                case "E" -> studentProfileUI.promptAndEditProfile();
                default  -> out.println("[AlumConnect] Invalid sub-option '" + sub + "'.");
            }
        } catch (SecurityException e) {
            out.println("[AlumConnect] Access denied: " + e.getMessage());
        } catch (Exception e) {
            out.println("[AlumConnect] An unexpected error occurred: " + e.getMessage());
        }
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    /**
     * Logs the current student out, clears the session, and prints a confirmation.
     */
    public void handleLogout() {
        if (!UserSession.getInstance().isLoggedIn()) {
            out.println("[AlumConnect] No active session to log out from.");
            return;
        }

        String name = UserSession.getInstance()
                .getCurrentUser()
                .map(User::getFullName)
                .orElse("Student");

        userService.logout();
        out.println("\n[AlumConnect] " + name + " has been successfully logged out.");
        out.println("Session closed.\n");
    }
}
