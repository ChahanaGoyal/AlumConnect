package com.alumconnect.ui;

import com.alumconnect.model.Alumni;
import com.alumconnect.service.AlumniDirectoryService;
import com.alumconnect.service.dto.AlumniDirectoryResult;
import com.alumconnect.util.UserSession;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

/**
 * Console UI for the Alumni Directory feature (Prompt 12).
 *
 * <p>Allows an authenticated Student to browse a directory of registered alumni.
 * Access by unauthorized users (unauthenticated, logged-out, or Alumni role) is
 * rejected with a security error.
 *
 * <p>Each alumni card presents:
 * <ul>
 *   <li>Full Name</li>
 *   <li>Graduation Year</li>
 *   <li>Degree / Course</li>
 *   <li>Department</li>
 *   <li>Current Company</li>
 *   <li>Current Role</li>
 *   <li>Industry</li>
 *   <li>Years of Experience</li>
 *   <li>Expertise / Skills</li>
 *   <li>Mentorship Availability</li>
 * </ul>
 */
public class AlumniDirectoryUI {

    private final AlumniDirectoryService directoryService;
    private final Scanner scanner;
    private final PrintStream out;

    public AlumniDirectoryUI() {
        this(new AlumniDirectoryService(), new Scanner(System.in), System.out);
    }

    public AlumniDirectoryUI(AlumniDirectoryService directoryService) {
        this(directoryService, new Scanner(System.in), System.out);
    }

    public AlumniDirectoryUI(AlumniDirectoryService directoryService,
                             InputStream in,
                             PrintStream out) {
        this(directoryService, new Scanner(in), out);
    }

    public AlumniDirectoryUI(AlumniDirectoryService directoryService,
                             Scanner scanner,
                             PrintStream out) {
        this.directoryService = Objects.requireNonNull(directoryService, "directoryService must not be null");
        this.scanner = Objects.requireNonNull(scanner, "scanner must not be null");
        this.out = Objects.requireNonNull(out, "out must not be null");
    }

    /**
     * Displays the Alumni Directory to the authenticated student.
     *
     * @throws SecurityException if the active session is unauthenticated or not a Student
     */
    public void showDirectory() {
        // Enforce access control at the UI boundary
        UserSession session = UserSession.getInstance();
        if (!session.isLoggedIn()) {
            out.println("\n[AlumConnect] Access denied: You must be logged in to view the Alumni Directory.");
            throw new SecurityException("Unauthorized: No user is currently logged in.");
        }
        if (!session.isStudent()) {
            out.println("\n[AlumConnect] Access denied: The Alumni Directory is only available to Students.");
            throw new SecurityException(
                    "Access Denied: Current user role [" + session.getCurrentRole().orElse(null) +
                    "] cannot access the Alumni Directory.");
        }

        out.println("\n==================================================");
        out.println("                 ALUMNI DIRECTORY                 ");
        out.println("==================================================");

        AlumniDirectoryResult result = directoryService.getAlumniDirectory();
        if (!result.isSuccess()) {
            out.println("[ERROR] " + result.getMessage());
            out.println("==================================================");
            return;
        }

        List<Alumni> alumniList = result.getAlumniList();
        if (alumniList.isEmpty()) {
            out.println("No registered alumni found in the directory.");
            out.println("==================================================");
            return;
        }

        out.println("Displaying " + alumniList.size() + " registered alumni:");
        out.println("--------------------------------------------------");

        for (int i = 0; i < alumniList.size(); i++) {
            printAlumniCard(i + 1, alumniList.get(i));
        }

        out.println("==================================================");
        out.println("[INFO] End of Alumni Directory.");
    }

    /**
     * Formats and prints a single alumni entry card. Handles missing or null fields safely.
     *
     * @param index 1-based index in the directory
     * @param a     the Alumni instance
     */
    private void printAlumniCard(int index, Alumni a) {
        String gradYearStr = a.getGraduationYear() > 0 ? String.valueOf(a.getGraduationYear()) : "Not Specified";
        String expStr;
        if (a.getProfileId() != null && a.getYearsOfExperience() >= 0) {
            expStr = a.getYearsOfExperience() + " years";
        } else {
            expStr = "Not Specified";
        }

        String mentorshipStr = a.isAvailableForMentorship() ? "Yes" : "No";

        out.println(index + ". Alumni Name         : " + safeString(a.getFullName()));
        out.println("   Graduation Year     : " + gradYearStr);
        out.println("   Degree              : " + safeString(a.getDegree()));
        out.println("   Department          : " + safeString(a.getDepartment()));
        out.println("   Company             : " + safeString(a.getCurrentCompany()));
        out.println("   Role                : " + safeString(a.getCurrentRole()));
        out.println("   Industry            : " + safeString(a.getIndustry()));
        out.println("   Experience          : " + expStr);
        out.println("   Skills              : " + safeString(a.getExpertiseAreas()));
        out.println("   Mentorship Available: " + mentorshipStr);
        out.println("--------------------------------------------------");
    }

    /**
     * Helper converting null or blank strings to user-friendly fallback.
     */
    private String safeString(String value) {
        return (value != null && !value.isBlank()) ? value.trim() : "Not Specified";
    }
}

