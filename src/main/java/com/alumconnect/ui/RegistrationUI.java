package com.alumconnect.ui;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.Role;
import com.alumconnect.model.Student;
import com.alumconnect.service.UserService;
import com.alumconnect.service.dto.RegistrationRequest;
import com.alumconnect.service.dto.RegistrationResult;
import com.alumconnect.util.PasswordUtil;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

/**
 * Console-based user interface for registering new Student and Alumni accounts.
 *
 * <p>Handles user prompts, interactive role selection, input capture,
 * validation feedback display, and error handling.
 */
public class RegistrationUI {

    private final UserService userService;
    private final InputStream in;
    private final Scanner scanner;
    private final PrintStream out;

    public RegistrationUI() {
        this(new UserService(), System.in, System.out);
    }

    public RegistrationUI(UserService userService) {
        this(userService, System.in, System.out);
    }

    public RegistrationUI(UserService userService, InputStream in, PrintStream out) {
        this.userService = userService;
        this.in = in;
        this.scanner = new Scanner(in);
        this.out = out;
    }

    /**
     * Guides the user through interactive account registration and returns the result.
     *
     * @return the {@link RegistrationResult}
     */
    public RegistrationResult promptAndRegister() {
        ConsoleFormatter.printAppHeader(out, "AlumConnect User Registration");

        // 1. Role Selection
        Role role = promptRole();
        if (role == null) {
            RegistrationResult failure = RegistrationResult.failure("Registration cancelled: Invalid role selection.");
            displayResult(failure);
            return failure;
        }

        // 2. Common Information
        ConsoleFormatter.printSubHeader(out, "Basic Account Details");
        String fullName = promptRequired("Full Name: ");
        String email = promptRequired("Email: ");

        // Immediate password validation and retry loop
        String password = promptValidatedPassword();
        if (password == null) {
            RegistrationResult failure = RegistrationResult.failure("Registration cancelled: Incomplete input.");
            displayResult(failure);
            return failure;
        }

        // Confirm password prompt with immediate mismatch check
        String confirmPassword = promptMatchingConfirmPassword(password);
        if (confirmPassword == null) {
            RegistrationResult failure = RegistrationResult.failure("Registration cancelled: Incomplete input.");
            displayResult(failure);
            return failure;
        }

        // Only after successful password validation and confirmation, proceed to remaining details
        String phone = promptOptional("Phone number (optional): ");

        ConsoleFormatter.printSubHeader(out, "Academic Information");
        String department = promptRequired("Department (e.g. Computer Science): ");
        Integer graduationYear = promptInteger("Graduation Year (1950 - 2100): ");

        RegistrationRequest request = new RegistrationRequest();
        request.setRole(role);
        request.setFullName(fullName);
        request.setEmail(email);
        request.setPassword(password);
        request.setConfirmPassword(confirmPassword);
        request.setPhone(phone);
        request.setDepartment(department);
        request.setGraduationYear(graduationYear);

        // 3. Role-Specific Profile Information
        if (role == Role.STUDENT) {
            promptStudentFields(request);
        } else if (role == Role.ALUMNI) {
            promptAlumniFields(request);
        }

        // 4. Submit Registration
        out.println("\n[AlumConnect] Submitting registration request...");
        RegistrationResult result = userService.register(request);

        // 5. Present Results
        displayResult(result);
        return result;
    }

    // ── Role Prompts ──────────────────────────────────────────────────────────

    private Role promptRole() {
        out.println("Select Account Type:");
        out.println("  [1] Student (Enrolled student seeking mentorship & networking)");
        out.println("  [2] Alumni  (Graduated alumnus offering mentorship)");
        out.println("  [0] Cancel  (Return to previous menu)");
        out.print("Enter choice (1 or 2): ");
        String choice = scanner.hasNextLine() ? scanner.nextLine().trim() : "";

        if ("1".equals(choice) || "student".equalsIgnoreCase(choice)) {
            return Role.STUDENT;
        } else if ("2".equals(choice) || "alumni".equalsIgnoreCase(choice)) {
            return Role.ALUMNI;
        } else if ("0".equals(choice) || "cancel".equalsIgnoreCase(choice)) {
            out.println("[AlumConnect] Registration cancelled by user.");
            return null;
        } else {
            out.println("[Error] Invalid selection. Please enter 1 for Student or 2 for Alumni.");
            return null;
        }
    }

    // ── Field Prompts ─────────────────────────────────────────────────────────

    private void promptStudentFields(RegistrationRequest request) {
        ConsoleFormatter.printSubHeader(out, "Student Profile Details");
        String degree = promptOptional("Degree programme (press Enter for '" + Student.DEFAULT_DEGREE + "'): ");
        if (!degree.isEmpty()) request.setDegree(degree);

        Integer semester = promptOptionalInteger("Current Semester (1 - 12, default 1): ", Student.MIN_SEMESTER);
        request.setCurrentSemester(semester);

        String enrollmentNo = promptOptional("Enrollment Number (optional): ");
        if (!enrollmentNo.isEmpty()) request.setEnrollmentNumber(enrollmentNo);

        String skills = promptOptional("Skills (e.g. Java, Python, SQL) (optional): ");
        if (!skills.isEmpty()) request.setSkills(skills);

        String interests = promptOptional("Interests (optional): ");
        if (!interests.isEmpty()) request.setInterests(interests);

        String linkedin = promptOptional("LinkedIn Profile URL (optional): ");
        if (!linkedin.isEmpty()) request.setLinkedinUrl(linkedin);

        String github = promptOptional("GitHub Profile URL (optional): ");
        if (!github.isEmpty()) request.setGithubUrl(github);

        String bio = promptOptional("Short Bio (optional): ");
        if (!bio.isEmpty()) request.setBio(bio);
    }

    private void promptAlumniFields(RegistrationRequest request) {
        ConsoleFormatter.printSubHeader(out, "Alumni Profile Details");
        String degree = promptOptional("Degree completed (press Enter for '" + Alumni.DEFAULT_DEGREE + "'): ");
        if (!degree.isEmpty()) request.setDegree(degree);

        String company = promptOptional("Current Employer/Company (optional): ");
        if (!company.isEmpty()) request.setCurrentCompany(company);

        String role = promptOptional("Current Job Title/Role (optional): ");
        if (!role.isEmpty()) request.setCurrentRole(role);

        String industry = promptOptional("Industry Sector (optional): ");
        if (!industry.isEmpty()) request.setIndustry(industry);

        Integer experience = promptOptionalInteger("Years of Experience (default 0): ", 0);
        request.setYearsOfExperience(experience);

        String expertise = promptOptional("Areas of Expertise (optional): ");
        if (!expertise.isEmpty()) request.setExpertiseAreas(expertise);

        String mentorship = promptOptional("Available for mentorship? (Y/N, default Y): ");
        if ("n".equalsIgnoreCase(mentorship) || "no".equalsIgnoreCase(mentorship)) {
            request.setAvailableForMentorship(false);
        } else {
            request.setAvailableForMentorship(true);
        }

        Integer maxMentees = promptOptionalInteger("Maximum Mentees (default 3): ", Alumni.DEFAULT_MAX_MENTEES);
        request.setMaxMentees(maxMentees);

        String linkedin = promptOptional("LinkedIn Profile URL (optional): ");
        if (!linkedin.isEmpty()) request.setLinkedinUrl(linkedin);

        String bio = promptOptional("Short Bio (optional): ");
        if (!bio.isEmpty()) request.setBio(bio);
    }

    // ── Input Helpers ─────────────────────────────────────────────────────────

    private String promptRequired(String label) {
        out.print(label);
        return scanner.hasNextLine() ? scanner.nextLine().trim() : "";
    }

    private String promptOptional(String label) {
        out.print(label);
        return scanner.hasNextLine() ? scanner.nextLine().trim() : "";
    }

    private Integer promptInteger(String label) {
        out.print(label);
        String val = scanner.hasNextLine() ? scanner.nextLine().trim() : "";
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer promptOptionalInteger(String label, int defaultValue) {
        out.print(label);
        String val = scanner.hasNextLine() ? scanner.nextLine().trim() : "";
        if (val.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Prompts the user for a password and immediately validates it against all security rules.
     * If invalid, displays the exact validation errors immediately and prompts again.
     *
     * @return a valid password string, or {@code null} if input is exhausted
     */
    private String promptValidatedPassword() {
        while (true) {
            String password = promptPassword("Password: ");
            if (password == null) {
                return null;
            }
            List<String> errors = PasswordUtil.getPasswordRequirementErrors(password);
            if (errors.isEmpty()) {
                return password;
            }
            ConsoleFormatter.printError(out, "Password does not meet requirements:");
            for (String err : errors) {
                out.println("  - " + err);
            }
            out.println("Please enter a new password that satisfies all requirements.\n");
        }
    }

    /**
     * Prompts the user for confirm password and validates that it matches the entered password.
     * If they do not match, displays an error immediately and prompts to confirm again.
     *
     * @param validPassword the previously validated password
     * @return matching confirm password string, or {@code null} if input is exhausted
     */
    private String promptMatchingConfirmPassword(String validPassword) {
        while (true) {
            String confirmPassword = promptPassword("Confirm Password: ");
            if (confirmPassword == null) {
                return null;
            }
            if (validPassword.equals(confirmPassword)) {
                return confirmPassword;
            }
            ConsoleFormatter.printError(out, "Passwords do not match.");
            out.println("Please confirm your password again.\n");
        }
    }

    /**
     * Reads a password from the console, masking input when running attached to an interactive console.
     * Never prints or logs the entered password.
     *
     * @param label prompt label
     * @return entered password string, or {@code null} if input is exhausted
     */
    private String promptPassword(String label) {
        if (System.console() != null && this.in == System.in) {
            char[] chars = System.console().readPassword("%s", label);
            return chars != null ? new String(chars) : null;
        }
        out.print(label);
        return scanner.hasNextLine() ? scanner.nextLine() : null;
    }

    // ── Result Display ───────────────────────────────────────────────────────

    public void displayResult(RegistrationResult result) {
        ConsoleFormatter.printDivider(out);
        if (result.isSuccess()) {
            ConsoleFormatter.printSuccess(out, result.getMessage());
            if (result.getUser() != null) {
                out.println("  • User ID:  " + result.getUser().getId());
                out.println("  • Name:     " + result.getUser().getFullName());
                out.println("  • Email:    " + result.getUser().getEmail());
                out.println("  • Role:     " + result.getUser().getRole());
            }
        } else {
            ConsoleFormatter.printError(out, result.getMessage());
            if (!result.getErrors().isEmpty()) {
                out.println("Details:");
                for (String err : result.getErrors()) {
                    out.println("  - " + err);
                }
            }
        }
        ConsoleFormatter.printDivider(out);
        out.println();
    }
}
