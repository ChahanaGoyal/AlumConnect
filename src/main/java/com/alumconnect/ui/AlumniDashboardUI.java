package com.alumconnect.ui;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.MentorshipRequest;
import com.alumconnect.model.MentorshipStatus;
import com.alumconnect.model.Role;
import com.alumconnect.model.User;
import com.alumconnect.service.AlumniProfileService;
import com.alumconnect.service.MentorshipRequestService;
import com.alumconnect.service.UserService;
import com.alumconnect.service.dto.MentorshipRequestResult;
import com.alumconnect.util.UserSession;

import java.io.InputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

/**
 * Console UI for the Alumni Dashboard (Prompt 11 &amp; Prompt 17).
 *
 * <p>Displays a role-gated menu to an authenticated Alumni and routes input
 * to the appropriate sub-feature UIs. The dashboard can only be entered by a
 * user whose session role is {@code ALUMNI}; any other access (unauthenticated,
 * wrong role such as Student) is rejected immediately with a clear error message.
 *
 * <p>Implemented features accessible from this dashboard:
 * <ol>
 *   <li>My Profile:
 *     <ul>
 *       <li>View my alumni profile</li>
 *       <li>Create/Complete my alumni profile (if none exists)</li>
 *       <li>Update/Edit my alumni profile</li>
 *     </ul>
 *   </li>
 *   <li>View Mentorship Requests:
 *     <ul>
 *       <li>View incoming mentorship requests received from students (Prompt 17)</li>
 *       <li>Accept pending mentorship requests</li>
 *       <li>Reject pending mentorship requests</li>
 *     </ul>
 *   </li>
 *   <li>Logout</li>
 *   <li>Exit</li>
 * </ol>
 *
 * <p>Business logic is never duplicated here; the dashboard delegates to
 * {@link AlumniProfileService} via {@link AlumniProfileUI}, to
 * {@link MentorshipRequestService} for request lifecycle operations, and to
 * {@link UserService} for session management.
 */
public class AlumniDashboardUI {

    // ── Dashboard menu option constants ──────────────────────────────────────
    /** Menu option: show the profile sub-menu (view / create / edit). */
    public static final String OPTION_PROFILE       = "1";
    /** Menu option: view and manage received mentorship requests. */
    public static final String OPTION_VIEW_REQUESTS = "2";
    /** Menu option: view accepted mentees (Prompt 19). */
    public static final String OPTION_MY_MENTEES    = "3";
    /** Menu option: log the alumni out. */
    public static final String OPTION_LOGOUT        = "9";
    /** Menu option: exit the application. */
    public static final String OPTION_EXIT          = "0";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AlumniProfileUI alumniProfileUI;
    private final MentorshipRequestService mentorshipRequestService;
    private final UserService userService;
    private final Scanner scanner;
    private final PrintStream out;

    // ── Constructors ─────────────────────────────────────────────────────────

    /**
     * Production constructor — wires real services and uses {@code System.in/out}.
     */
    public AlumniDashboardUI() {
        this(new AlumniProfileUI(), new MentorshipRequestService(), new UserService(), System.in, System.out);
    }

    /**
     * Convenience constructor for callers that already own an {@link AlumniProfileUI}.
     *
     * @param alumniProfileUI the alumni profile UI instance to delegate to
     * @param userService     the user service for logout operations
     * @param in              input stream
     * @param out             output stream
     */
    public AlumniDashboardUI(AlumniProfileUI alumniProfileUI,
                             UserService userService,
                             InputStream in,
                             PrintStream out) {
        this(alumniProfileUI, new MentorshipRequestService(), userService, in, out);
    }

    /**
     * Constructor accepting a shared {@link Scanner} so the dashboard and its
     * parent UI (e.g. {@link AuthUI}) read from the same stream without competing.
     *
     * @param alumniProfileUI the alumni profile UI instance
     * @param userService     the user service
     * @param scanner         a pre-created, shared Scanner
     * @param out             output stream
     */
    public AlumniDashboardUI(AlumniProfileUI alumniProfileUI,
                             UserService userService,
                             Scanner scanner,
                             PrintStream out) {
        this(alumniProfileUI, new MentorshipRequestService(), userService, scanner, out);
    }

    /**
     * Constructor allowing injection of {@link MentorshipRequestService} with InputStream.
     */
    public AlumniDashboardUI(AlumniProfileUI alumniProfileUI,
                             MentorshipRequestService mentorshipRequestService,
                             UserService userService,
                             InputStream in,
                             PrintStream out) {
        this(alumniProfileUI, mentorshipRequestService, userService, new Scanner(in), out);
    }

    /**
     * Full dependency-injection constructor with Scanner.
     */
    public AlumniDashboardUI(AlumniProfileUI alumniProfileUI,
                             MentorshipRequestService mentorshipRequestService,
                             UserService userService,
                             Scanner scanner,
                             PrintStream out) {
        this.alumniProfileUI = Objects.requireNonNull(alumniProfileUI, "alumniProfileUI must not be null");
        this.mentorshipRequestService = Objects.requireNonNull(mentorshipRequestService, "mentorshipRequestService must not be null");
        this.userService = Objects.requireNonNull(userService, "userService must not be null");
        this.scanner = Objects.requireNonNull(scanner, "scanner must not be null");
        this.out = Objects.requireNonNull(out, "out must not be null");
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Entry point for the Alumni Dashboard.
     *
     * <p>Validates that the current session belongs to an authenticated Alumni;
     * displays the welcome header and menu; then reads and dispatches exactly
     * one menu selection.
     *
     * @throws SecurityException if no user is logged in or the logged-in user
     *                           is not an Alumni
     */
    public void show() {
        validateAlumniSession();

        User currentUser = UserSession.getInstance().getCurrentUser().orElseThrow(
                () -> new SecurityException("Unauthorized: session user is missing."));

        if (!(currentUser instanceof Alumni alumni)) {
            out.println("[AlumConnect] Access denied: User is not an Alumni instance.");
            throw new SecurityException("Unexpected user type; expected Alumni.");
        }

        printHeader(alumni);
        handleMenuSelection(alumni);
    }

    // ── Menu rendering ────────────────────────────────────────────────────────

    /**
     * Prints the dashboard header and the main menu.
     *
     * @param alumni the currently logged-in Alumni
     */
    private void printHeader(Alumni alumni) {
        out.println("\n==================================================");
        out.println("               ALUMNI DASHBOARD                   ");
        out.println("==================================================");
        out.println("Welcome, " + alumni.getFullName() + "!");
        String summary = alumni.getProfessionalSummary();
        if (summary != null && !summary.isBlank()) {
            out.println(summary);
        }
        out.println("Mentorship Status: " + alumni.getMentorshipStatus()
                + " (Max Mentees: " + alumni.getMaxMentees() + ")");
        if (alumni.getExpertiseAreas() != null && !alumni.getExpertiseAreas().isBlank()) {
            out.println("Expertise        : " + alumni.getExpertiseAreas());
        }
        out.println("--------------------------------------------------");
        out.println("Alumni Menu:");
        out.println("  [" + OPTION_PROFILE       + "] My Profile");
        out.println("  [" + OPTION_VIEW_REQUESTS + "] View Mentorship Requests");
        out.println("  [" + OPTION_MY_MENTEES    + "] My Mentees");
        out.println("  [" + OPTION_LOGOUT        + "] Logout");
        out.println("  [" + OPTION_EXIT          + "] Exit");
        out.println("--------------------------------------------------");
    }

    // ── Menu dispatch ─────────────────────────────────────────────────────────

    /**
     * Reads one menu selection from the input stream and dispatches it.
     * Invalid or unrecognised input is handled gracefully with a clear message.
     *
     * @param alumni the currently logged-in Alumni (passed to sub-menus)
     */
    public void handleMenuSelection(Alumni alumni) {
        if (!scanner.hasNextLine()) {
            return;
        }

        out.print("Enter your choice: ");
        String choice = scanner.nextLine().trim();

        switch (choice) {
            case OPTION_PROFILE -> handleProfileSubMenu();
            case OPTION_VIEW_REQUESTS, "REQUESTS", "5" -> handleMentorshipRequests(alumni);
            case OPTION_MY_MENTEES, "MENTEES", "M" -> handleMyMentees(alumni);
            case OPTION_LOGOUT, "4" -> handleLogout();
            case OPTION_EXIT, "6", "10" -> {
                out.println("\n[AlumConnect] Goodbye! Thank you for using AlumConnect.");
            }
            default -> out.println("[AlumConnect] Invalid choice '" + choice
                    + "'. Please enter one of the listed options.");
        }
    }

    // ── Mentorship Requests (Prompt 17) ───────────────────────────────────────

    /**
     * Direct programmatic entry point for viewing mentorship requests.
     *
     * @throws SecurityException if active session is unauthenticated or not an Alumni
     */
    public void viewMentorshipRequests() {
        validateAlumniSession();
        User currentUser = UserSession.getInstance().getCurrentUser().orElseThrow(
                () -> new SecurityException("Unauthorized: session user is missing."));
        if (currentUser instanceof Alumni alumni) {
            handleMentorshipRequests(alumni);
        } else {
            throw new SecurityException("Access Denied: Current user is not an Alumni.");
        }
    }

    /**
     * Displays all received mentorship requests for the given alumni and provides
     * actions to accept or reject pending requests.
     *
     * <p>Refreshes the request list after an accept or reject action, and allows returning
     * to the Alumni Dashboard at any time.
     *
     * @param alumni the currently logged-in Alumni
     */
    public void handleMentorshipRequests(Alumni alumni) {
        validateAlumniSession();

        while (true) {
            out.println("\n==================================================");
            out.println("           RECEIVED MENTORSHIP REQUESTS           ");
            out.println("==================================================");

            MentorshipRequestResult result = mentorshipRequestService.getRequestsForAlumni(alumni.getId());
            if (!result.isSuccess()) {
                out.println("[ERROR] " + result.getMessage());
                out.println("==================================================");
                return;
            }

            List<MentorshipRequest> requests = result.getRequests();
            if (requests.isEmpty()) {
                out.println("No mentorship requests received yet.");
                out.println("==================================================");
                out.println("[AlumConnect] Returning to Alumni Dashboard.");
                return;
            }

            out.println("Displaying " + requests.size() + " received mentorship request(s):");
            out.println("--------------------------------------------------");

            for (int i = 0; i < requests.size(); i++) {
                printRequestCard(i + 1, requests.get(i));
            }
            out.println("==================================================");

            List<MentorshipRequest> pendingRequests = requests.stream()
                    .filter(r -> r.getStatus() == MentorshipStatus.PENDING)
                    .toList();

            // Requirement 5: Disable or hide actions if no pending requests exist
            if (pendingRequests.isEmpty()) {
                out.println("[INFO] There are no pending requests to accept or reject.");
                out.println("  [0] Back to Alumni Dashboard");
                out.print("Enter your choice: ");
                if (!scanner.hasNextLine()) {
                    return;
                }
                scanner.nextLine();
                out.println("[AlumConnect] Returning to Alumni Dashboard.");
                return;
            }

            // Requirement 4: Provide Accept and Reject for pending requests
            out.println("Actions for Pending Requests:");
            out.println("  [A] Accept Request");
            out.println("  [R] Reject Request");
            out.println("  [0] Back to Alumni Dashboard");
            out.print("Enter your choice: ");

            if (!scanner.hasNextLine()) {
                return;
            }

            String action = scanner.nextLine().trim().toUpperCase();

            switch (action) {
                case "A", "ACCEPT", "1" -> handleAcceptRequest(alumni, pendingRequests);
                case "R", "REJECT", "2" -> handleRejectRequest(alumni, pendingRequests);
                case "0", "B", "BACK" -> {
                    out.println("[AlumConnect] Returning to Alumni Dashboard.");
                    return;
                }
                default -> out.println("[ERROR] Invalid choice '" + action + "'. Please choose A, R, or 0.");
            }
        }
    }

    private void handleAcceptRequest(Alumni alumni, List<MentorshipRequest> pendingRequests) {
        out.print("Enter Request ID to accept (or 0 to cancel): ");
        if (!scanner.hasNextLine()) {
            return;
        }
        String input = scanner.nextLine().trim();
        if ("0".equals(input) || input.equalsIgnoreCase("cancel")) {
            out.println("[AlumConnect] Accept request cancelled.");
            return;
        }

        int parsedInput;
        try {
            parsedInput = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            out.println("[ERROR] Invalid request ID: Please enter a valid numeric ID.");
            return;
        }

        // Support either exact request ID or 1-based index from the pending list
        int targetRequestId = -1;
        for (MentorshipRequest r : pendingRequests) {
            if (r.getId() != null && r.getId() == parsedInput) {
                targetRequestId = r.getId();
                break;
            }
        }
        if (targetRequestId == -1 && parsedInput >= 1 && parsedInput <= pendingRequests.size()) {
            targetRequestId = pendingRequests.get(parsedInput - 1).getId();
        }

        if (targetRequestId == -1) {
            out.println("[ERROR] Request ID " + parsedInput + " is not a pending request in your list.");
            return;
        }

        MentorshipRequestResult result = mentorshipRequestService.acceptRequest(targetRequestId, alumni.getId());
        if (result.isSuccess()) {
            out.println("\n[SUCCESS] " + result.getMessage());
        } else {
            out.println("\n[ERROR] " + result.getMessage());
        }
    }

    private void handleRejectRequest(Alumni alumni, List<MentorshipRequest> pendingRequests) {
        out.print("Enter Request ID to reject (or 0 to cancel): ");
        if (!scanner.hasNextLine()) {
            return;
        }
        String input = scanner.nextLine().trim();
        if ("0".equals(input) || input.equalsIgnoreCase("cancel")) {
            out.println("[AlumConnect] Reject request cancelled.");
            return;
        }

        int parsedInput;
        try {
            parsedInput = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            out.println("[ERROR] Invalid request ID: Please enter a valid numeric ID.");
            return;
        }

        // Support either exact request ID or 1-based index from the pending list
        int targetRequestId = -1;
        for (MentorshipRequest r : pendingRequests) {
            if (r.getId() != null && r.getId() == parsedInput) {
                targetRequestId = r.getId();
                break;
            }
        }
        if (targetRequestId == -1 && parsedInput >= 1 && parsedInput <= pendingRequests.size()) {
            targetRequestId = pendingRequests.get(parsedInput - 1).getId();
        }

        if (targetRequestId == -1) {
            out.println("[ERROR] Request ID " + parsedInput + " is not a pending request in your list.");
            return;
        }

        MentorshipRequestResult result = mentorshipRequestService.rejectRequest(targetRequestId, alumni.getId());
        if (result.isSuccess()) {
            out.println("\n[SUCCESS] " + result.getMessage());
        } else {
            out.println("\n[ERROR] " + result.getMessage());
        }
    }

    private void printRequestCard(int index, MentorshipRequest req) {
        out.println(index + ". Request ID   : " + req.getId());
        out.println("   Student Name : " + safeString(req.getStudentName())
                + (req.getStudentEmail() != null ? " (" + req.getStudentEmail() + ")" : ""));
        if (req.getStudentDegree() != null || req.getStudentDepartment() != null) {
            out.println("   Academic Info: " + safeString(req.getStudentDegree()) + " - " + safeString(req.getStudentDepartment()));
        }
        out.println("   Topic        : " + safeString(req.getTopic()));
        out.println("   Message      : " + safeString(req.getMessage()));
        out.println("   Status       : " + req.getStatus());
        out.println("   Requested At : " + formatDateTime(req.getRequestedAt()));
        if (req.getRespondedAt() != null) {
            out.println("   Responded At : " + formatDateTime(req.getRespondedAt()));
        }
        out.println("--------------------------------------------------");
    }

    private String formatDateTime(LocalDateTime ldt) {
        if (ldt == null) return "Not Specified";
        return ldt.format(DATE_TIME_FORMATTER);
    }

    private String safeString(String value) {
        return (value != null && !value.isBlank()) ? value.trim() : "Not Specified";
    }

    // ── My Mentees (Prompt 19) ────────────────────────────────────────────────

    /**
     * Direct programmatic entry point for viewing accepted mentees.
     *
     * @throws SecurityException if active session is unauthenticated or not an Alumni
     */
    public void viewMyMentees() {
        validateAlumniSession();
        User currentUser = UserSession.getInstance().getCurrentUser().orElseThrow(
                () -> new SecurityException("Unauthorized: session user is missing."));
        if (currentUser instanceof Alumni alumni) {
            handleMyMentees(alumni);
        } else {
            throw new SecurityException("Access Denied: Current user is not an Alumni.");
        }
    }

    /**
     * Retrieves and displays accepted mentees for the current alumni.
     *
     * @param alumni the currently logged-in Alumni
     */
    public void handleMyMentees(Alumni alumni) {
        validateAlumniSession();

        out.println("\n==================================================");
        out.println("                   MY MENTEES                     ");
        out.println("==================================================");

        MentorshipRequestResult result = mentorshipRequestService.getConnectionsForAlumni(alumni.getId());
        if (!result.isSuccess()) {
            out.println("[ERROR] " + result.getMessage());
            out.println("==================================================");
            return;
        }

        List<MentorshipRequest> mentees = result.getRequests();
        if (mentees.isEmpty()) {
            out.println("[INFO] You have no active mentees yet.");
            out.println("==================================================");
            out.println("[AlumConnect] Returning to Alumni Dashboard.");
            return;
        }

        out.println("Displaying " + mentees.size() + " active mentee(s):");
        out.println("--------------------------------------------------");

        for (int i = 0; i < mentees.size(); i++) {
            printMenteeCard(i + 1, mentees.get(i));
        }

        out.println("==================================================");
        out.println("[INFO] End of Mentees List.");
        out.println("[AlumConnect] Returning to Alumni Dashboard.");
    }

    /**
     * Convenience overload retrieving the session alumni automatically.
     */
    public void handleMyMentees() {
        validateAlumniSession();
        UserSession session = UserSession.getInstance();
        User currentUser = session.getCurrentUser().orElseThrow(
                () -> new SecurityException("Unauthorized: session user is missing."));
        if (currentUser instanceof Alumni a) {
            handleMyMentees(a);
        }
    }

    private void printMenteeCard(int index, MentorshipRequest conn) {
        out.println(index + ". Connection ID: " + conn.getId());
        out.println("   Student Name : " + safeString(conn.getStudentName())
                + (conn.getStudentEmail() != null ? " (" + conn.getStudentEmail() + ")" : ""));
        if (conn.getStudentDegree() != null || conn.getStudentDepartment() != null) {
            out.println("   Academic Info: " + safeString(conn.getStudentDegree()) + " - " + safeString(conn.getStudentDepartment()));
        }
        if (conn.getStudentGraduationYear() != null && conn.getStudentGraduationYear() > 0) {
            out.println("   Graduation   : Class of " + conn.getStudentGraduationYear());
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

    // ── Profile sub-menu ──────────────────────────────────────────────────────

    /**
     * Shows the Profile sub-menu and delegates to {@link AlumniProfileUI}.
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
                case "V", "1", "VIEW" -> alumniProfileUI.displayMyProfile();
                case "C", "2", "CREATE" -> alumniProfileUI.promptAndCreateProfile();
                case "E", "3", "EDIT", "UPDATE" -> alumniProfileUI.promptAndEditProfile();
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
     * Logs the current alumni out, clears the session, and prints a confirmation.
     */
    public void handleLogout() {
        if (!UserSession.getInstance().isLoggedIn()) {
            out.println("[AlumConnect] No active session to log out from.");
            return;
        }

        String name = UserSession.getInstance()
                .getCurrentUser()
                .map(User::getFullName)
                .orElse("Alumni");

        userService.logout();
        out.println("\n[AlumConnect] " + name + " has been successfully logged out.");
        out.println("Session closed.\n");
    }

    // ── Validation Helpers ────────────────────────────────────────────────────

    private void validateAlumniSession() {
        UserSession session = UserSession.getInstance();
        if (!session.isLoggedIn()) {
            out.println("[AlumConnect] Access denied: You must be logged in to view the Alumni Dashboard.");
            throw new SecurityException("Unauthorized: No user is currently logged in.");
        }
        if (!session.isAlumni()) {
            out.println("[AlumConnect] Access denied: The Alumni Dashboard is only available to Alumni.");
            throw new SecurityException(
                    "Access Denied: Current user role [" + session.getCurrentRole().orElse(null) +
                    "] cannot access the Alumni Dashboard.");
        }
    }
}
