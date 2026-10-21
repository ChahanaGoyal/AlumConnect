package com.alumconnect.ui;

import com.alumconnect.model.Student;
import com.alumconnect.service.StudentProfileService;
import com.alumconnect.service.dto.StudentProfileRequest;
import com.alumconnect.service.dto.StudentProfileResult;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

/**
 * Console UI for the Student Profile feature (Prompt 7).
 *
 * <p>Lets a logged-in student:
 * <ol>
 *   <li>View their full profile.</li>
 *   <li>Edit their profile fields interactively.</li>
 * </ol>
 *
 * <p>All access-control decisions are delegated to {@link StudentProfileService};
 * this class only handles I/O presentation.
 */
public class StudentProfileUI {

    private final StudentProfileService profileService;
    private final Scanner scanner;
    private final PrintStream out;

    public StudentProfileUI() {
        this(new StudentProfileService(), new Scanner(System.in), System.out);
    }

    public StudentProfileUI(StudentProfileService profileService) {
        this(profileService, new Scanner(System.in), System.out);
    }

    public StudentProfileUI(StudentProfileService profileService,
                             InputStream in,
                             PrintStream out) {
        this.profileService = profileService;
        this.scanner = new Scanner(in);
        this.out = out;
    }

    /**
     * Constructor accepting an already-created Scanner for sharing with a parent UI
     * (e.g. {@link AuthUI}) to avoid competing reads on the same InputStream.
     *
     * @param profileService the profile service
     * @param scanner        the shared Scanner
     * @param out            output stream
     */
    public StudentProfileUI(StudentProfileService profileService,
                             Scanner scanner,
                             PrintStream out) {
        this.profileService = profileService;
        this.scanner = scanner;
        this.out = out;
    }

    // ── View Profile ──────────────────────────────────────────────────────────

    /**
     * Displays the current student's profile.
     *
     * @return the {@link StudentProfileResult} from the service
     */
    public StudentProfileResult displayMyProfile() {
        StudentProfileResult result = profileService.viewMyProfile();

        if (result.isSuccess()) {
            printProfileDetails(result.getStudent());
        } else {
            out.println("\n[ERROR] " + result.getMessage());
        }

        return result;
    }

    // ── Edit Profile ──────────────────────────────────────────────────────────

    /**
     * Interactive edit flow: loads existing profile, prompts for each field
     * (showing current value so the student can press Enter to keep it),
     * then submits the update.
     *
     * @return the {@link StudentProfileResult} from the update call
     */
    public StudentProfileResult promptAndEditProfile() {
        out.println("\n==================================================");
        out.println("             Edit My Student Profile              ");
        out.println("==================================================");
        out.println("[INFO] Press Enter to keep the current value for any field.");

        // First load existing data so we can show current values as defaults
        StudentProfileResult loadResult = profileService.viewMyProfile();
        if (!loadResult.isSuccess()) {
            out.println("[ERROR] " + loadResult.getMessage());
            return loadResult;
        }

        Student current = loadResult.getStudent();
        StudentProfileRequest request = new StudentProfileRequest();

        // ── Required fields ───────────────────────────────────────────────────

        // Department
        out.print("Department [" + current.getDepartment() + "]: ");
        String dept = readLine();
        request.setDepartment(dept.isBlank() ? current.getDepartment() : dept.trim());

        // Graduation Year
        out.print("Graduation Year [" + current.getGraduationYear() + "]: ");
        String gradYearInput = readLine();
        if (gradYearInput.isBlank()) {
            request.setGraduationYear(current.getGraduationYear());
        } else {
            Integer parsed = parseIntOrNull(gradYearInput);
            if (parsed == null) {
                out.println("[ERROR] Graduation year must be a number. Profile not updated.");
                return StudentProfileResult.failure("Invalid graduation year input.");
            }
            request.setGraduationYear(parsed);
        }

        // ── Optional fields ───────────────────────────────────────────────────

        // Degree
        out.print("Degree/Programme [" + current.getDegree() + "]: ");
        String degree = readLine();
        request.setDegree(degree.isBlank() ? current.getDegree() : degree.trim());

        // Semester
        out.print("Current Semester [" + current.getCurrentSemester() + "]: ");
        String semInput = readLine();
        if (semInput.isBlank()) {
            request.setCurrentSemester(current.getCurrentSemester());
        } else {
            Integer sem = parseIntOrNull(semInput);
            if (sem == null) {
                out.println("[ERROR] Semester must be a number. Profile not updated.");
                return StudentProfileResult.failure("Invalid semester input.");
            }
            request.setCurrentSemester(sem);
        }

        // Enrollment Number
        out.print("Enrollment Number [" + nullOr(current.getEnrollmentNumber(), "none") + "]: ");
        String enroll = readLine();
        request.setEnrollmentNumber(enroll.isBlank() ? current.getEnrollmentNumber() : enroll.trim());

        // Skills
        out.print("Skills [" + nullOr(current.getSkills(), "none") + "]: ");
        String skills = readLine();
        request.setSkills(skills.isBlank() ? current.getSkills() : skills.trim());

        // Interests
        out.print("Interests [" + nullOr(current.getInterests(), "none") + "]: ");
        String interests = readLine();
        request.setInterests(interests.isBlank() ? current.getInterests() : interests.trim());

        // Bio
        out.print("Bio [" + nullOr(current.getBio(), "none") + "]: ");
        String bio = readLine();
        request.setBio(bio.isBlank() ? current.getBio() : bio.trim());

        // LinkedIn URL
        out.print("LinkedIn URL [" + nullOr(current.getLinkedinUrl(), "none") + "]: ");
        String linkedin = readLine();
        request.setLinkedinUrl(linkedin.isBlank() ? current.getLinkedinUrl() : linkedin.trim());

        // GitHub URL
        out.print("GitHub URL [" + nullOr(current.getGithubUrl(), "none") + "]: ");
        String github = readLine();
        request.setGithubUrl(github.isBlank() ? current.getGithubUrl() : github.trim());

        // ── Submit ────────────────────────────────────────────────────────────
        out.println("\n[AlumConnect] Saving profile changes...");
        StudentProfileResult result = profileService.updateMyProfile(request);

        if (result.isSuccess()) {
            out.println("[SUCCESS] " + result.getMessage());
            printProfileDetails(result.getStudent());
        } else {
            out.println("[ERROR] " + result.getMessage());
        }

        return result;
    }

    // ── Create Profile ────────────────────────────────────────────────────────

    /**
     * Interactive profile creation flow for a student who does not have a profile yet.
     *
     * @return the {@link StudentProfileResult} from the creation call
     */
    public StudentProfileResult promptAndCreateProfile() {
        out.println("\n==================================================");
        out.println("             Create My Student Profile            ");
        out.println("==================================================");

        StudentProfileRequest request = new StudentProfileRequest();

        out.print("Department (Required): ");
        request.setDepartment(readLine().trim());

        out.print("Graduation Year (Required, e.g., 2026): ");
        String gradYearStr = readLine().trim();
        Integer gradYear = parseIntOrNull(gradYearStr);
        if (gradYear == null) {
            out.println("[ERROR] Graduation year must be a valid number.");
            return StudentProfileResult.failure("Invalid graduation year input.");
        }
        request.setGraduationYear(gradYear);

        out.print("Degree/Programme (Optional, default B.Tech): ");
        String degree = readLine().trim();
        request.setDegree(degree.isEmpty() ? Student.DEFAULT_DEGREE : degree);

        out.print("Current Semester (Optional, 1-10): ");
        String semStr = readLine().trim();
        if (!semStr.isEmpty()) {
            Integer sem = parseIntOrNull(semStr);
            if (sem == null) {
                out.println("[ERROR] Semester must be a valid number.");
                return StudentProfileResult.failure("Invalid semester input.");
            }
            request.setCurrentSemester(sem);
        }

        out.print("Enrollment Number (Optional): ");
        String enroll = readLine().trim();
        if (!enroll.isEmpty()) request.setEnrollmentNumber(enroll);

        out.print("Skills (Optional, comma-separated): ");
        String skills = readLine().trim();
        if (!skills.isEmpty()) request.setSkills(skills);

        out.print("Interests (Optional, comma-separated): ");
        String interests = readLine().trim();
        if (!interests.isEmpty()) request.setInterests(interests);

        out.print("Bio (Optional): ");
        String bio = readLine().trim();
        if (!bio.isEmpty()) request.setBio(bio);

        out.print("LinkedIn URL (Optional): ");
        String linkedin = readLine().trim();
        if (!linkedin.isEmpty()) request.setLinkedinUrl(linkedin);

        out.print("GitHub URL (Optional): ");
        String github = readLine().trim();
        if (!github.isEmpty()) request.setGithubUrl(github);

        out.println("\n[AlumConnect] Creating profile...");
        StudentProfileResult result = profileService.createMyProfile(request);

        if (result.isSuccess()) {
            out.println("[SUCCESS] " + result.getMessage());
            printProfileDetails(result.getStudent());
        } else {
            out.println("[ERROR] " + result.getMessage());
        }

        return result;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void printProfileDetails(Student student) {
        out.println("\n==================================================");
        out.println("               MY STUDENT PROFILE                ");
        out.println("==================================================");
        out.println("  Full Name       : " + student.getFullName());
        out.println("  Email           : " + student.getEmail());
        if (student.getPhone() != null) {
            out.println("  Phone           : " + student.getPhone());
        }
        out.println("--------------------------------------------------");
        out.println("  Degree          : " + student.getDegree());
        out.println("  Department      : " + student.getDepartment());
        out.println("  Graduation Year : " + student.getGraduationYear());
        out.println("  Current Semester: " + student.getCurrentSemester());
        if (student.getEnrollmentNumber() != null) {
            out.println("  Enrollment No   : " + student.getEnrollmentNumber());
        }
        out.println("--------------------------------------------------");
        if (student.getBio() != null) {
            out.println("  Bio             : " + student.getBio());
        }
        if (student.getSkills() != null) {
            out.println("  Skills          : " + student.getSkills());
        }
        if (student.getInterests() != null) {
            out.println("  Interests       : " + student.getInterests());
        }
        if (student.getLinkedinUrl() != null) {
            out.println("  LinkedIn        : " + student.getLinkedinUrl());
        }
        if (student.getGithubUrl() != null) {
            out.println("  GitHub          : " + student.getGithubUrl());
        }
        out.println("==================================================");
    }

    private String readLine() {
        return scanner.hasNextLine() ? scanner.nextLine() : "";
    }

    private Integer parseIntOrNull(String input) {
        try {
            return Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String nullOr(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }
}
