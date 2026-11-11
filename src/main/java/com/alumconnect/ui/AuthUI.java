package com.alumconnect.ui;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.Role;
import com.alumconnect.model.Student;
import com.alumconnect.model.User;
import com.alumconnect.service.AlumniProfileService;
import com.alumconnect.service.StudentProfileService;
import com.alumconnect.service.UserService;
import com.alumconnect.service.dto.LoginResult;
import com.alumconnect.util.UserSession;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

/**
 * Console UI handling login, dashboard routing based on role (Student vs. Alumni),
 * access-controlled feature menus, and logout operations.
 */
public class AuthUI {

    private final UserService userService;
    private final StudentProfileUI studentProfileUI;
    private final AlumniProfileUI alumniProfileUI;
    private final StudentDashboardUI studentDashboardUI;
    private final AlumniDashboardUI alumniDashboardUI;
    private final InputStream in;
    private final Scanner scanner;
    private final PrintStream out;

    public AuthUI() {
        this(new UserService(), System.in, System.out);
    }

    public AuthUI(UserService userService) {
        this(userService, System.in, System.out);
    }

    public AuthUI(UserService userService, InputStream in, PrintStream out) {
        this.userService = userService;
        this.in = in;
        this.scanner = new Scanner(in);
        this.out = out;
        // Share the same scanner instance so child UIs read from the same stream without competition
        this.studentProfileUI = new StudentProfileUI(new StudentProfileService(), this.scanner, out);
        this.alumniProfileUI = new AlumniProfileUI(new AlumniProfileService(), this.scanner, out);
        this.studentDashboardUI = new StudentDashboardUI(this.studentProfileUI, userService, this.scanner, out);
        this.alumniDashboardUI = new AlumniDashboardUI(this.alumniProfileUI, userService, this.scanner, out);
    }

    /**
     * Constructor allowing injection of profile UIs for unit testing.
     *
     * @param userService      the UserService
     * @param studentProfileUI the StudentProfileUI
     * @param in               input stream
     * @param out              output stream
     */
    public AuthUI(UserService userService, StudentProfileUI studentProfileUI,
                  InputStream in, PrintStream out) {
        this(userService, studentProfileUI, new AlumniProfileUI(new AlumniProfileService(), new Scanner(in), out), in, out);
    }

    /**
     * Constructor allowing injection of both Student and Alumni profile UIs for unit testing.
     *
     * @param userService      the UserService
     * @param studentProfileUI the StudentProfileUI
     * @param alumniProfileUI  the AlumniProfileUI
     * @param in               input stream
     * @param out              output stream
     */
    public AuthUI(UserService userService, StudentProfileUI studentProfileUI,
                  AlumniProfileUI alumniProfileUI, InputStream in, PrintStream out) {
        this.userService = userService;
        this.studentProfileUI = studentProfileUI;
        this.alumniProfileUI = alumniProfileUI;
        this.in = in;
        this.scanner = new Scanner(in);
        this.out = out;
        this.studentDashboardUI = new StudentDashboardUI(studentProfileUI, userService, this.scanner, out);
        this.alumniDashboardUI = new AlumniDashboardUI(alumniProfileUI, userService, this.scanner, out);
    }

    /**
     * Constructor allowing injection of all UIs including StudentDashboardUI for unit testing.
     *
     * @param userService        the UserService
     * @param studentProfileUI   the StudentProfileUI
     * @param alumniProfileUI    the AlumniProfileUI
     * @param studentDashboardUI the StudentDashboardUI
     * @param in                 input stream
     * @param out                output stream
     */
    public AuthUI(UserService userService, StudentProfileUI studentProfileUI,
                  AlumniProfileUI alumniProfileUI, StudentDashboardUI studentDashboardUI,
                  InputStream in, PrintStream out) {
        this.userService = userService;
        this.studentProfileUI = studentProfileUI;
        this.alumniProfileUI = alumniProfileUI;
        this.studentDashboardUI = studentDashboardUI;
        this.in = in;
        this.scanner = new Scanner(in);
        this.out = out;
        this.alumniDashboardUI = new AlumniDashboardUI(alumniProfileUI, userService, this.scanner, out);
    }

    /**
     * Constructor allowing injection of all UIs including both dashboards for unit testing.
     *
     * @param userService        the UserService
     * @param studentProfileUI   the StudentProfileUI
     * @param alumniProfileUI    the AlumniProfileUI
     * @param studentDashboardUI the StudentDashboardUI
     * @param alumniDashboardUI  the AlumniDashboardUI
     * @param in                 input stream
     * @param out                output stream
     */
    public AuthUI(UserService userService, StudentProfileUI studentProfileUI,
                  AlumniProfileUI alumniProfileUI, StudentDashboardUI studentDashboardUI,
                  AlumniDashboardUI alumniDashboardUI, InputStream in, PrintStream out) {
        this.userService = userService;
        this.studentProfileUI = studentProfileUI;
        this.alumniProfileUI = alumniProfileUI;
        this.studentDashboardUI = studentDashboardUI;
        this.alumniDashboardUI = alumniDashboardUI;
        this.in = in;
        this.scanner = new Scanner(in);
        this.out = out;
    }

    /**
     * Displays the SELECT LOGIN TYPE menu and executes the selected login flow:
     * [1] Student Login
     * [2] Alumni Login
     * [0] Back to Main Menu
     *
     * @return the LoginResult
     */
    public LoginResult promptAndLogin() {
        while (true) {
            out.println("\n" + ConsoleFormatter.DOUBLE_LINE);
            out.println(ConsoleFormatter.center("SELECT LOGIN TYPE", ConsoleFormatter.TERMINAL_WIDTH));
            out.println(ConsoleFormatter.DOUBLE_LINE);
            out.println("[1] Student Login");
            out.println("[2] Alumni Login");
            out.println("[0] Back to Main Menu");
            ConsoleFormatter.printDivider(out);
            out.print("Enter your choice: ");

            if (!scanner.hasNextLine()) {
                return LoginResult.failure("Login cancelled: End of input.");
            }

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> {
                    return promptStudentLogin();
                }
                case "2" -> {
                    return promptAlumniLogin();
                }
                case "0", "BACK", "CANCEL", "EXIT" -> {
                    out.println("[AlumConnect] Login cancelled.");
                    return LoginResult.failure("Login cancelled by user.");
                }
                default -> out.println("[AlumConnect] Invalid choice '" + choice + "'. Please enter 1, 2, or 0.");
            }
        }
    }

    /**
     * Prompts for Student credentials and authenticates with Student role enforcement.
     *
     * @return the LoginResult
     */
    public LoginResult promptStudentLogin() {
        return promptRoleSpecificLogin(Role.STUDENT, "Student Login");
    }

    /**
     * Prompts for Alumni credentials and authenticates with Alumni role enforcement.
     *
     * @return the LoginResult
     */
    public LoginResult promptAlumniLogin() {
        return promptRoleSpecificLogin(Role.ALUMNI, "Alumni Login");
    }

    /**
     * Prompts for credentials under a specific role, verifies role match,
     * and routes to the appropriate dashboard on success.
     *
     * @param expectedRole the required Role (STUDENT or ALUMNI)
     * @param title        the title to display on the header (e.g. "Student Login")
     * @return the LoginResult
     */
    public LoginResult promptRoleSpecificLogin(Role expectedRole, String title) {
        ConsoleFormatter.printAppHeader(out, title);
        out.println("Please enter your account credentials (or enter '0' to cancel):");

        out.print("Email: ");
        String email = scanner.hasNextLine() ? scanner.nextLine().trim() : "";
        if ("0".equals(email) || "cancel".equalsIgnoreCase(email)) {
            out.println("[AlumConnect] Login cancelled.");
            return LoginResult.failure("Login cancelled by user.");
        }

        String password = promptPassword("Password: ");
        if (password == null) {
            out.println("[AlumConnect] Login cancelled.");
            return LoginResult.failure("Login cancelled: Incomplete input.");
        }

        out.println("\n[AlumConnect] Authenticating...");
        LoginResult result = userService.login(email, password);

        if (!result.isSuccess()) {
            out.println("[ERROR] " + result.getMessage());
            return result;
        }

        User user = result.getUser();
        if (expectedRole != null && user.getRole() != expectedRole) {
            userService.logout();
            String err = "Access denied: Account role [" + user.getRole() + "] cannot log in via " + title +
                    ". Please select " + (user.getRole() == Role.STUDENT ? "Student Login" : "Alumni Login") + ".";
            out.println("[ERROR] " + err);
            return LoginResult.failure(err);
        }

        out.println("[SUCCESS] " + result.getMessage());
        routeToDashboard(user);
        return result;
    }

    private String promptPassword(String label) {
        if (System.console() != null && this.in == System.in) {
            char[] chars = System.console().readPassword("%s", label);
            return chars != null ? new String(chars) : null;
        }
        out.print(label);
        return scanner.hasNextLine() ? scanner.nextLine() : null;
    }

    /**
     * Routes the authenticated user to their role-specific dashboard.
     *
     * @param user the authenticated User
     */
    public void routeToDashboard(User user) {
        if (user == null) {
            out.println("[Error] Cannot route null user to dashboard.");
            return;
        }

        if (user.isStudent()) {
            showStudentDashboard((Student) user);
        } else if (user.isAlumni()) {
            showAlumniDashboard((Alumni) user);
        } else {
            out.println("\n--- Admin / System Dashboard ---");
            out.println("Logged in as: " + user.getDisplayLabel());
        }
    }

    /**
     * Displays the Student Dashboard with student-specific actions and enforces role separation.
     * Delegates to {@link StudentDashboardUI} which provides the full Prompt 10 implementation.
     *
     * @param student the authenticated Student
     */
    public void showStudentDashboard(Student student) {
        // Enforce access control
        UserSession.getInstance().requireRole(Role.STUDENT);

        ConsoleFormatter.printAppHeader(out, "STUDENT DASHBOARD");
        out.println("Welcome, " + student.getFullName() + "!");
        out.println("Department: " + student.getDepartment() + " | Semester: " + student.getCurrentSemester() + " | Grad: " + student.getGraduationYear());
        if (student.getEnrollmentNumber() != null && !student.getEnrollmentNumber().isBlank()) {
            out.println("Enrollment No: " + student.getEnrollmentNumber());
        }
        if (student.getSkills() != null && !student.getSkills().isBlank()) {
            out.println("Skills: " + student.getSkills());
        }
        ConsoleFormatter.printDivider(out);
        out.println("Student Menu:");
        out.println("  [1] My Profile");
        out.println("  [2] Browse Alumni");
        out.println("  [3] Search Alumni");
        out.println("  [4] Filter Alumni");
        out.println("  [5] My Mentorship Requests");
        out.println("  [6] My Mentorship Connections");
        out.println("  [9] Logout");
        out.println("  [0] Exit");
        ConsoleFormatter.printDivider(out);

        // Handle student menu selection via the dedicated dashboard UI
        studentDashboardUI.handleMenuSelection(student);
    }

    /**
     * Displays the Alumni Dashboard with alumni-specific actions and enforces role separation.
     * Delegates to {@link AlumniDashboardUI} which provides the full Prompt 11 implementation.
     *
     * @param alumni the authenticated Alumni
     */
    public void showAlumniDashboard(Alumni alumni) {
        // Enforce access control
        UserSession.getInstance().requireRole(Role.ALUMNI);

        ConsoleFormatter.printAppHeader(out, "ALUMNI DASHBOARD");
        out.println("Welcome, " + alumni.getFullName() + "!");
        String summary = alumni.getProfessionalSummary();
        if (summary != null && !summary.isBlank()) {
            out.println(summary);
        }
        out.println("Mentorship Status: " + alumni.getMentorshipStatus() + " (Max Mentees: " + alumni.getMaxMentees() + ")");
        if (alumni.getExpertiseAreas() != null && !alumni.getExpertiseAreas().isBlank()) {
            out.println("Expertise        : " + alumni.getExpertiseAreas());
        }
        ConsoleFormatter.printDivider(out);
        out.println("Alumni Menu:");
        out.println("  [1] My Profile");
        out.println("  [2] View Mentorship Requests");
        out.println("  [3] My Mentees");
        out.println("  [9] Logout");
        out.println("  [0] Exit");
        ConsoleFormatter.printDivider(out);

        // Handle alumni menu selection via the dedicated dashboard UI
        alumniDashboardUI.handleMenuSelection(alumni);
    }

    /**
     * Reads and dispatches an alumni menu selection.
     *
     * @param alumni the currently logged-in alumni
     */
    public void handleAlumniMenuSelection(Alumni alumni) {
        alumniDashboardUI.handleMenuSelection(alumni);
    }

    /**
     * Executes role-protected action for Student, throwing SecurityException if alumni tries to access.
     */
    public void executeStudentOnlyAction(Runnable action) {
        UserSession.getInstance().requireRole(Role.STUDENT);
        action.run();
    }

    /**
     * Executes role-protected action for Alumni, throwing SecurityException if student tries to access.
     */
    public void executeAlumniOnlyAction(Runnable action) {
        UserSession.getInstance().requireRole(Role.ALUMNI);
        action.run();
    }

    /**
     * Logs out the user and displays confirmation message.
     */
    public void handleLogout() {
        if (!UserSession.getInstance().isLoggedIn()) {
            out.println("[AlumConnect] No active user session.");
            return;
        }

        String name = UserSession.getInstance().getCurrentUser().map(User::getFullName).orElse("User");
        userService.logout();
        out.println("\n[AlumConnect] " + name + " has been successfully logged out.");
        out.println("Session closed.\n");
    }
}
