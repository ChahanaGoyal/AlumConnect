package com.alumconnect.ui;

import com.alumconnect.model.MentorshipRequest;
import com.alumconnect.model.Role;
import com.alumconnect.model.Student;
import com.alumconnect.model.User;
import com.alumconnect.service.AlumniDirectoryService;
import com.alumconnect.service.MentorshipRequestService;
import com.alumconnect.service.StudentProfileService;
import com.alumconnect.service.UserService;
import com.alumconnect.service.dto.MentorshipRequestResult;
import com.alumconnect.util.UserSession;

import java.io.InputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

/**
 * Console UI for the Student Dashboard (Prompt 10, Prompt 12, Prompt 13, Prompt 14 &amp; Prompt 18).
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
 *   <li>Filter Alumni: filter alumni by structured criteria (Prompt 14)</li>
 *   <li>My Mentorship Requests: view sent requests and status tracking (Prompt 18)</li>
 *   <li>Logout</li>
 *   <li>Exit</li>
 * </ol>
 *
 * <p>Business logic is never duplicated here; the dashboard delegates to
 * {@link StudentProfileService} via {@link StudentProfileUI},
 * {@link AlumniDirectoryService} via {@link AlumniDirectoryUI},
 * {@link MentorshipRequestService} for request tracking,
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
    /** Menu option: filter alumni directory using structured criteria. */
    public static final String OPTION_FILTER_ALUMNI = "4";
    /** Menu option: view sent mentorship requests and status tracking (Prompt 18). */
    public static final String OPTION_MY_REQUESTS   = "5";
    /** Menu option: view accepted mentorship connections (Prompt 19). */
    public static final String OPTION_MY_CONNECTIONS = "6";
    /** Menu option: log the student out. */
    public static final String OPTION_LOGOUT        = "9";
    /** Menu option: exit the application. */
    public static final String OPTION_EXIT          = "0";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StudentProfileUI studentProfileUI;
    private final AlumniDirectoryUI alumniDirectoryUI;
    private final MentorshipRequestService mentorshipRequestService;
    private final UserService userService;
    private final Scanner scanner;
    private final PrintStream out;

    // ── Constructors ─────────────────────────────────────────────────────────

    /**
     * Production constructor — wires real services and uses {@code System.in/out}.
     */
    public StudentDashboardUI() {
        this(new StudentProfileUI(), new AlumniDirectoryUI(), new MentorshipRequestService(), new UserService(), System.in, System.out);
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
             new MentorshipRequestService(),
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
        this(studentProfileUI, alumniDirectoryUI, new MentorshipRequestService(), userService, new Scanner(in), out);
    }

    /**
     * Constructor allowing injection of profile and directory UIs with shared scanner.
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
        this(studentProfileUI, alumniDirectoryUI, new MentorshipRequestService(), userService, scanner, out);
    }

    /**
     * Constructor allowing injection of profile, directory, and mentorship request services with streams.
     */
    public StudentDashboardUI(StudentProfileUI studentProfileUI,
                              AlumniDirectoryUI alumniDirectoryUI,
                              MentorshipRequestService mentorshipRequestService,
                              UserService userService,
                              InputStream in,
                              PrintStream out) {
        this(studentProfileUI, alumniDirectoryUI, mentorshipRequestService, userService, new Scanner(in), out);
    }

    /**
     * Full dependency-injection constructor with shared Scanner.
     *
     * @param studentProfileUI         the student profile UI
     * @param alumniDirectoryUI        the alumni directory UI
     * @param mentorshipRequestService the mentorship request service
     * @param userService              the user service
     * @param scanner                  a pre-created, shared Scanner
     * @param out                      output stream
     */
    public StudentDashboardUI(StudentProfileUI studentProfileUI,
                              AlumniDirectoryUI alumniDirectoryUI,
                              MentorshipRequestService mentorshipRequestService,
                              UserService userService,
                              Scanner scanner,
                              PrintStream out) {
        this.studentProfileUI = studentProfileUI;
        this.alumniDirectoryUI = alumniDirectoryUI;
        this.mentorshipRequestService = mentorshipRequestService;
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
        out.println("  [" + OPTION_FILTER_ALUMNI + "] Filter Alumni");
        out.println("  [" + OPTION_MY_REQUESTS   + "] My Mentorship Requests");
        out.println("  [" + OPTION_MY_CONNECTIONS+ "] My Mentorship Connections");
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
            case OPTION_FILTER_ALUMNI -> handleFilterAlumni();
            case OPTION_MY_REQUESTS, "M", "REQUESTS" -> handleMyMentorshipRequests(student);
            case OPTION_MY_CONNECTIONS, "C", "CONNECTIONS" -> handleMyMentorshipConnections(student);
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

    // ── Filter Alumni (Prompt 14) ─────────────────────────────────────────────

    /**
     * Opens the Alumni Filter prompt for the student to narrow down alumni by structured criteria.
     */
    public void handleFilterAlumni() {
        try {
            alumniDirectoryUI.promptAndFilterAlumni();
        } catch (SecurityException e) {
            out.println("[AlumConnect] Access denied: " + e.getMessage());
        } catch (Exception e) {
            out.println("[AlumConnect] An unexpected error occurred: " + e.getMessage());
        }
    }

    // ── My Mentorship Requests (Prompt 18) ────────────────────────────────────

    /**
     * Retrieves and displays all mentorship requests sent by the current student.
     *
     * @param student the currently logged-in Student
     */
    public void handleMyMentorshipRequests(Student student) {
        validateStudentSession();

        out.println("\n==================================================");
        out.println("             MY MENTORSHIP REQUESTS               ");
        out.println("==================================================");

        MentorshipRequestResult result = mentorshipRequestService.getRequestsForStudent(student.getId());
        if (!result.isSuccess()) {
            out.println("[ERROR] " + result.getMessage());
            out.println("==================================================");
            return;
        }

        List<MentorshipRequest> requests = result.getRequests();
        if (requests.isEmpty()) {
            out.println("[INFO] You have not sent any mentorship requests yet.");
            out.println("==================================================");
            out.println("[AlumConnect] Returning to Student Dashboard.");
            return;
        }

        out.println("Displaying " + requests.size() + " sent mentorship request(s):");
        out.println("--------------------------------------------------");

        for (int i = 0; i < requests.size(); i++) {
            printStudentRequestCard(i + 1, requests.get(i));
        }

        out.println("==================================================");
        out.println("[INFO] End of Mentorship Requests.");
        out.println("[AlumConnect] Returning to Student Dashboard.");
    }

    /**
     * Convenience overload retrieving the session student automatically.
     */
    public void handleMyMentorshipRequests() {
        validateStudentSession();
        UserSession session = UserSession.getInstance();
        User currentUser = session.getCurrentUser().orElseThrow(
                () -> new SecurityException("Unauthorized: session user is missing."));
        if (currentUser instanceof Student s) {
            handleMyMentorshipRequests(s);
        }
    }

    private void printStudentRequestCard(int index, MentorshipRequest req) {
        out.println(index + ". Request ID   : " + req.getId());
        out.println("   Alumni Name  : " + safeString(req.getAlumniName())
                + (req.getAlumniEmail() != null ? " (" + req.getAlumniEmail() + ")" : ""));
        if (req.getAlumniCompany() != null || req.getAlumniRole() != null) {
            String company = req.getAlumniCompany() != null ? req.getAlumniCompany() : "N/A";
            String role = req.getAlumniRole() != null ? req.getAlumniRole() : "N/A";
            out.println("   Professional : " + company + " | " + role);
        }
        if (req.getAlumniDegree() != null || req.getAlumniDepartment() != null) {
            out.println("   Academic Info: " + safeString(req.getAlumniDegree()) + " - " + safeString(req.getAlumniDepartment()));
        }
        out.println("   Topic        : " + safeString(req.getTopic()));
        out.println("   Message      : " + safeString(req.getMessage()));
        out.println("   Status       : " + req.getStatus());
        out.println("   Requested At : " + formatDateTime(req.getRequestedAt()));
        if (req.getRespondedAt() != null) {
            out.println("   Responded At : " + formatDateTime(req.getRespondedAt()));
        }
        if (req.getResponseNotes() != null && !req.getResponseNotes().isBlank()) {
            out.println("   Response Note: " + req.getResponseNotes());
        }
        out.println("--------------------------------------------------");
    }

    // ── My Mentorship Connections (Prompt 19) ─────────────────────────────────

    /**
     * Programmatic entry point for viewing accepted mentorship connections.
     *
     * @throws SecurityException if active session is unauthenticated or not a Student
     */
    public void viewMentorshipConnections() {
        validateStudentSession();
        User currentUser = UserSession.getInstance().getCurrentUser().orElseThrow(
                () -> new SecurityException("Unauthorized: session user is missing."));
        if (currentUser instanceof Student student) {
            handleMyMentorshipConnections(student);
        } else {
            throw new SecurityException("Access Denied: Current user is not a Student.");
        }
    }

    /**
     * Retrieves and displays accepted mentorship connections for the current student.
     *
     * @param student the currently logged-in Student
     */
    public void handleMyMentorshipConnections(Student student) {
        validateStudentSession();

        out.println("\n==================================================");
        out.println("            MY MENTORSHIP CONNECTIONS             ");
        out.println("==================================================");

        MentorshipRequestResult result = mentorshipRequestService.getConnectionsForStudent(student.getId());
        if (!result.isSuccess()) {
            out.println("[ERROR] " + result.getMessage());
            out.println("==================================================");
            return;
        }

        List<MentorshipRequest> connections = result.getRequests();
        if (connections.isEmpty()) {
            out.println("[INFO] You have no active mentorship connections yet.");
            out.println("==================================================");
            out.println("[AlumConnect] Returning to Student Dashboard.");
            return;
        }

        out.println("Displaying " + connections.size() + " active mentorship connection(s):");
        out.println("--------------------------------------------------");

        for (int i = 0; i < connections.size(); i++) {
            printStudentConnectionCard(i + 1, connections.get(i));
        }

        out.println("==================================================");
        out.println("[INFO] End of Mentorship Connections.");
        out.println("[AlumConnect] Returning to Student Dashboard.");
    }

    /**
     * Convenience overload retrieving the session student automatically.
     */
    public void handleMyMentorshipConnections() {
        validateStudentSession();
        UserSession session = UserSession.getInstance();
        User currentUser = session.getCurrentUser().orElseThrow(
                () -> new SecurityException("Unauthorized: session user is missing."));
        if (currentUser instanceof Student s) {
            handleMyMentorshipConnections(s);
        }
    }

    private void printStudentConnectionCard(int index, MentorshipRequest conn) {
        out.println(index + ". Connection ID: " + conn.getId());
        out.println("   Mentor Name  : " + safeString(conn.getAlumniName())
                + (conn.getAlumniEmail() != null ? " (" + conn.getAlumniEmail() + ")" : ""));
        if (conn.getAlumniCompany() != null || conn.getAlumniRole() != null) {
            String company = conn.getAlumniCompany() != null ? conn.getAlumniCompany() : "N/A";
            String role = conn.getAlumniRole() != null ? conn.getAlumniRole() : "N/A";
            out.println("   Professional : " + company + " | " + role);
        }
        if (conn.getAlumniDegree() != null || conn.getAlumniDepartment() != null) {
            out.println("   Academic Info: " + safeString(conn.getAlumniDegree()) + " - " + safeString(conn.getAlumniDepartment()));
        }
        out.println("   Topic        : " + safeString(conn.getTopic()));
        out.println("   Status       : " + conn.getStatus());
        if (conn.getRespondedAt() != null) {
            out.println("   Connected On : " + formatDateTime(conn.getRespondedAt()));
        } else if (conn.getRequestedAt() != null) {
            out.println("   Requested At : " + formatDateTime(conn.getRequestedAt()));
        }
        out.println("--------------------------------------------------");
    }

    private void validateStudentSession() {
        UserSession session = UserSession.getInstance();
        if (!session.isLoggedIn()) {
            out.println("[AlumConnect] Access denied: You must be logged in to view your requests.");
            throw new SecurityException("Unauthorized: No user is currently logged in.");
        }
        if (!session.isStudent()) {
            out.println("[AlumConnect] Access denied: Mentorship requests can only be viewed by Students.");
            throw new SecurityException("Access Denied: Current user is not a Student.");
        }
    }

    private String formatDateTime(LocalDateTime ldt) {
        if (ldt == null) return "Not Specified";
        return ldt.format(DATE_TIME_FORMATTER);
    }

    private String safeString(String value) {
        return (value != null && !value.isBlank()) ? value.trim() : "Not Specified";
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
