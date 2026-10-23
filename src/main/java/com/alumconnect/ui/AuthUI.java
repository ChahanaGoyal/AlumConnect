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
        this.scanner = new Scanner(in);
        this.out = out;
        // Share the same scanner instance so child UIs read from the same stream without competition
        this.studentProfileUI = new StudentProfileUI(new StudentProfileService(), this.scanner, out);
        this.alumniProfileUI = new AlumniProfileUI(new AlumniProfileService(), this.scanner, out);
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
        this.scanner = new Scanner(in);
        this.out = out;
    }

    /**
     * Prompts the user for email and password, executes login,
     * routes to the appropriate dashboard on success, or displays error on failure.
     *
     * @return the LoginResult
     */
    public LoginResult promptAndLogin() {
        out.println("\n==================================================");
        out.println("               AlumConnect Login                  ");
        out.println("==================================================");

        out.print("Email: ");
        String email = scanner.hasNextLine() ? scanner.nextLine().trim() : "";

        out.print("Password: ");
        String password = scanner.hasNextLine() ? scanner.nextLine() : "";

        out.println("\n[AlumConnect] Authenticating...");
        LoginResult result = userService.login(email, password);

        if (result.isSuccess()) {
            out.println("[SUCCESS] " + result.getMessage());
            routeToDashboard(result.getUser());
        } else {
            out.println("[ERROR] " + result.getMessage());
        }

        return result;
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
     *
     * @param student the authenticated Student
     */
    public void showStudentDashboard(Student student) {
        // Enforce access control
        UserSession.getInstance().requireRole(Role.STUDENT);

        out.println("\n==================================================");
        out.println("               STUDENT DASHBOARD                  ");
        out.println("==================================================");
        out.println("Welcome, " + student.getFullName() + "!");
        out.println("Department: " + student.getDepartment() + " | Semester: " + student.getCurrentSemester() + " | Grad: " + student.getGraduationYear());
        if (student.getEnrollmentNumber() != null) {
            out.println("Enrollment No: " + student.getEnrollmentNumber());
        }
        if (student.getSkills() != null) {
            out.println("Skills: " + student.getSkills());
        }
        out.println("--------------------------------------------------");
        out.println("Student Menu:");
        out.println("  [1] View / Edit My Profile");
        out.println("  [2] Browse Alumni Mentors");
        out.println("  [3] Manage Mentorship Requests");
        out.println("  [4] Logout");
        out.println("--------------------------------------------------");

        // Handle student menu selection
        handleStudentMenuSelection(student);
    }

    /**
     * Reads and dispatches a student menu selection.
     *
     * @param student the currently logged-in student
     */
    public void handleStudentMenuSelection(Student student) {
        if (!scanner.hasNextLine()) {
            return; // no more input (e.g., piped input stream exhausted)
        }
        out.print("Enter your choice: ");
        String choice = scanner.hasNextLine() ? scanner.nextLine().trim() : "";

        switch (choice) {
            case "1" -> {
                out.println("\n--- Profile Options ---");
                out.println("  [V] View profile");
                out.println("  [E] Edit profile");
                out.println("  [C] Create profile");
                out.print("Select: ");
                String sub = scanner.hasNextLine() ? scanner.nextLine().trim().toUpperCase() : "";
                if (sub.equals("E")) {
                    studentProfileUI.promptAndEditProfile();
                } else if (sub.equals("C")) {
                    studentProfileUI.promptAndCreateProfile();
                } else {
                    studentProfileUI.displayMyProfile();
                }
            }
            case "4" -> handleLogout();
            default -> out.println("[AlumConnect] Feature coming soon or invalid choice.");
        }
    }

    /**
     * Displays the Alumni Dashboard with alumni-specific actions and enforces role separation.
     *
     * @param alumni the authenticated Alumni
     */
    public void showAlumniDashboard(Alumni alumni) {
        // Enforce access control
        UserSession.getInstance().requireRole(Role.ALUMNI);

        out.println("\n==================================================");
        out.println("               ALUMNI DASHBOARD                   ");
        out.println("==================================================");
        out.println("Welcome, " + alumni.getFullName() + "!");
        out.println(alumni.getProfessionalSummary());
        out.println("Mentorship Status: " + alumni.getMentorshipStatus() + " (Max Mentees: " + alumni.getMaxMentees() + ")");
        if (alumni.getExpertiseAreas() != null) {
            out.println("Expertise: " + alumni.getExpertiseAreas());
        }
        out.println("--------------------------------------------------");
        out.println("Alumni Menu:");
        out.println("  [1] View / Edit My Profile");
        out.println("  [2] Review Mentorship Requests");
        out.println("  [3] Update Mentorship Availability");
        out.println("  [4] Logout");
        out.println("--------------------------------------------------");

        // Handle alumni menu selection
        handleAlumniMenuSelection(alumni);
    }

    /**
     * Reads and dispatches an alumni menu selection.
     *
     * @param alumni the currently logged-in alumni
     */
    public void handleAlumniMenuSelection(Alumni alumni) {
        if (!scanner.hasNextLine()) {
            return;
        }
        out.print("Enter your choice: ");
        String choice = scanner.hasNextLine() ? scanner.nextLine().trim() : "";

        switch (choice) {
            case "1" -> {
                out.println("\n--- Profile Options ---");
                out.println("  [V] View profile");
                out.println("  [E] Edit profile");
                out.println("  [C] Create profile");
                out.print("Select: ");
                String sub = scanner.hasNextLine() ? scanner.nextLine().trim().toUpperCase() : "";
                if (sub.equals("E")) {
                    alumniProfileUI.promptAndEditProfile();
                } else if (sub.equals("C")) {
                    alumniProfileUI.promptAndCreateProfile();
                } else {
                    alumniProfileUI.displayMyProfile();
                }
            }
            case "4" -> handleLogout();
            default -> out.println("[AlumConnect] Feature coming soon or invalid choice.");
        }
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
