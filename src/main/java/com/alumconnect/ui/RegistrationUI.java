package com.alumconnect.ui;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.Role;
import com.alumconnect.model.Student;
import com.alumconnect.service.UserService;
import com.alumconnect.service.dto.RegistrationRequest;
import com.alumconnect.service.dto.RegistrationResult;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

/**
 * Console-based user interface for registering new Student and Alumni accounts.
 *
 * <p>Handles user prompts, interactive role selection, input capture,
 * validation feedback display, and error handling.
 */
public class RegistrationUI {

    private final UserService userService;
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
        this.scanner = new Scanner(in);
        this.out = out;
    }

    /**
     * Guides the user through interactive account registration and returns the result.
     *
     * @return the {@link RegistrationResult}
     */
    public RegistrationResult promptAndRegister() {
        out.println("\n==================================================");
        out.println("          AlumConnect User Registration           ");
        out.println("==================================================");

        // 1. Role Selection
        Role role = promptRole();
        if (role == null) {
            RegistrationResult failure = RegistrationResult.failure("Registration cancelled: Invalid role selection.");
            displayResult(failure);
            return failure;
        }

        // 2. Common Information
        out.println("\n--- Basic Account Details ---");
        String fullName = promptRequired("Full Name: ");
        String email = promptRequired("Email: ");
        String password = promptRequired("Password: ");
        String confirmPassword = promptRequired("Confirm Password: ");
        String phone = promptOptional("Phone number (optional): ");

        out.println("\n--- Academic Information ---");
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
        out.print("Enter choice (1 or 2): ");
        String choice = scanner.hasNextLine() ? scanner.nextLine().trim() : "";

        if ("1".equals(choice) || "student".equalsIgnoreCase(choice)) {
            return Role.STUDENT;
        } else if ("2".equals(choice) || "alumni".equalsIgnoreCase(choice)) {
            return Role.ALUMNI;
        } else {
            out.println("[Error] Invalid selection. Please enter 1 for Student or 2 for Alumni.");
            return null;
        }
    }

    // ── Field Prompts ─────────────────────────────────────────────────────────

    private void promptStudentFields(RegistrationRequest request) {
        out.println("\n--- Student Profile Details ---");
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
        out.println("\n--- Alumni Profile Details ---");
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

    // ── Result Display ───────────────────────────────────────────────────────

    public void displayResult(RegistrationResult result) {
        out.println("\n--------------------------------------------------");
        if (result.isSuccess()) {
            out.println("[SUCCESS] " + result.getMessage());
            if (result.getUser() != null) {
                out.println("  • User ID:  " + result.getUser().getId());
                out.println("  • Name:     " + result.getUser().getFullName());
                out.println("  • Email:    " + result.getUser().getEmail());
                out.println("  • Role:     " + result.getUser().getRole());
            }
        } else {
            out.println("[ERROR] " + result.getMessage());
            if (!result.getErrors().isEmpty()) {
                out.println("Details:");
                for (String err : result.getErrors()) {
                    out.println("  - " + err);
                }
            }
        }
        out.println("--------------------------------------------------\n");
    }
}
