package com.alumconnect.ui;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.Role;
import com.alumconnect.model.User;
import com.alumconnect.service.AlumniProfileService;
import com.alumconnect.service.UserService;
import com.alumconnect.util.UserSession;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

/**
 * Console UI for the Alumni Dashboard (Prompt 11).
 *
 * <p>Displays a role-gated menu to an authenticated Alumni and routes input
 * to the appropriate sub-feature UIs. The dashboard can only be entered by a
 * user whose session role is {@code ALUMNI}; any other access (unauthenticated,
 * wrong role such as Student) is rejected immediately with a clear error message.
 *
 * <p>Only features that are fully implemented in the project are exposed here.
 * Placeholder menu items for future prompts are intentionally omitted to
 * prevent alumni from reaching unimplemented functionality.
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
 *   <li>Logout</li>
 *   <li>Exit</li>
 * </ol>
 *
 * <p>Business logic is never duplicated here; the dashboard delegates entirely
 * to {@link AlumniProfileService} via {@link AlumniProfileUI}, and to
 * {@link UserService} for session management. Future mentorship request
 * management and connection features (Prompts 17-19) can be integrated cleanly.
 */
public class AlumniDashboardUI {

    // ── Dashboard menu option constants ──────────────────────────────────────
    /** Menu option: show the profile sub-menu (view / create / edit). */
    public static final String OPTION_PROFILE  = "1";
    /** Menu option: log the alumni out. */
    public static final String OPTION_LOGOUT   = "9";
    /** Menu option: exit the application. */
    public static final String OPTION_EXIT     = "0";

    private final AlumniProfileUI alumniProfileUI;
    private final UserService userService;
    private final Scanner scanner;
    private final PrintStream out;

    // ── Constructors ─────────────────────────────────────────────────────────

    /**
     * Production constructor — wires real services and uses {@code System.in/out}.
     */
    public AlumniDashboardUI() {
        this(new AlumniProfileUI(), new UserService(), System.in, System.out);
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
        this.alumniProfileUI = alumniProfileUI;
        this.userService = userService;
        this.scanner = new Scanner(in);
        this.out = out;
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
        this.alumniProfileUI = alumniProfileUI;
        this.userService = userService;
        this.scanner = scanner;
        this.out = out;
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
        // ── 1. Authentication guard ───────────────────────────────────────────
        UserSession session = UserSession.getInstance();

        if (!session.isLoggedIn()) {
            out.println("[AlumConnect] Access denied: You must be logged in to view the Alumni Dashboard.");
            throw new SecurityException("Unauthorized: No user is currently logged in.");
        }

        if (!session.isAlumni()) {
            out.println("[AlumConnect] Access denied: The Alumni Dashboard is only available to Alumni.");
            throw new SecurityException(
                    "Access Denied: role [" + session.getCurrentRole().orElse(null) +
                    "] cannot access the Alumni Dashboard.");
        }

        // ── 2. Retrieve the current alumni from the session ───────────────────
        User currentUser = session.getCurrentUser().orElseThrow(
                () -> new SecurityException("Unauthorized: session user is missing."));

        if (!(currentUser instanceof Alumni alumni)) {
            out.println("[AlumConnect] Access denied: User is not an Alumni instance.");
            throw new SecurityException("Unexpected user type; expected Alumni.");
        }

        // ── 3. Print welcome header and menu ─────────────────────────────────
        printHeader(alumni);

        // ── 4. Read and dispatch one menu selection ───────────────────────────
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
        out.println("  [" + OPTION_PROFILE  + "] My Profile");
        out.println("  [" + OPTION_LOGOUT   + "] Logout");
        out.println("  [" + OPTION_EXIT     + "] Exit");
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
            case OPTION_LOGOUT, "2", "4", "5" -> handleLogout();
            case OPTION_EXIT, "3", "6", "10" -> {
                out.println("\n[AlumConnect] Goodbye! Thank you for using AlumConnect.");
            }
            default -> out.println("[AlumConnect] Invalid choice '" + choice
                    + "'. Please enter one of the listed options.");
        }
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
}
