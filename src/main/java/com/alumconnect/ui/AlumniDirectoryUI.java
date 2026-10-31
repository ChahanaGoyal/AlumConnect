package com.alumconnect.ui;

import com.alumconnect.model.Alumni;
import com.alumconnect.service.AlumniDirectoryService;
import com.alumconnect.service.dto.AlumniDirectoryResult;
import com.alumconnect.service.dto.AlumniFilterCriteria;
import com.alumconnect.util.UserSession;
import com.alumconnect.util.ValidationUtil;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

/**
 * Console UI for the Alumni Directory, Alumni Search, Alumni Filtering, and
 * individual Alumni Profile Viewing features (Prompt 12, 13, 14 &amp; 15).
 *
 * <p>Allows an authenticated Student to:
 * <ul>
 *   <li>Browse the full directory of registered alumni</li>
 *   <li>Search for alumni using keyword matching (name, company, role, industry, skills)</li>
 *   <li>Filter alumni using structured criteria (graduation year, industry, experience,
 *       mentorship availability, department, degree, and multi-filter combinations)</li>
 *   <li>View the detailed public profile of a selected alumni</li>
 * </ul>
 *
 * <p>Access by unauthorized users (unauthenticated, logged-out, or Alumni role) is
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
 *
 * <p>The detailed profile view additionally shows:
 * <ul>
 *   <li>Bio</li>
 *   <li>LinkedIn Profile URL</li>
 *   <li>Maximum Mentees (when mentorship is available)</li>
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

    // ── Directory (Prompt 12) ─────────────────────────────────────────────────

    /**
     * Displays the full Alumni Directory to the authenticated student, then offers
     * an option to view any listed alumni's detailed profile (Prompt 15).
     *
     * @throws SecurityException if the active session is unauthenticated or not a Student
     */
    public void showDirectory() {
        validateStudentSession();

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

        promptViewAlumniFromList(alumniList);
    }

    // ── Search (Prompt 13) ────────────────────────────────────────────────────

    /**
     * Prompts the student to enter a search keyword and displays matching alumni (Prompt 13).
     *
     * @throws SecurityException if the active session is unauthenticated or not a Student
     */
    public void promptAndSearchAlumni() {
        validateStudentSession();

        out.println("\n==================================================");
        out.println("                 ALUMNI SEARCH                    ");
        out.println("==================================================");
        out.print("Enter search keyword (name, company, role, industry, skills): ");

        if (!scanner.hasNextLine()) {
            return;
        }

        String keyword = scanner.nextLine().trim();
        searchAlumni(keyword);
    }

    /**
     * Interactive search alias for callers without arguments.
     *
     * @throws SecurityException if the active session is unauthenticated or not a Student
     */
    public void searchAlumni() {
        promptAndSearchAlumni();
    }

    /**
     * Executes a search for the given keyword, prints matching alumni cards, then offers
     * an option to view any listed alumni's detailed profile (Prompt 13 &amp; 15).
     *
     * @param keyword search keyword (case-insensitive)
     * @throws SecurityException if the active session is unauthenticated or not a Student
     */
    public void searchAlumni(String keyword) {
        validateStudentSession();

        if (keyword == null || keyword.isBlank()) {
            out.println("[ERROR] Search keyword cannot be empty.");
            out.println("==================================================");
            return;
        }

        String trimmedKeyword = keyword.trim();
        AlumniDirectoryResult result = directoryService.searchAlumni(trimmedKeyword);
        if (!result.isSuccess()) {
            out.println("[ERROR] " + result.getMessage());
            out.println("==================================================");
            return;
        }

        List<Alumni> alumniList = result.getAlumniList();
        if (alumniList.isEmpty()) {
            out.println("No alumni found matching '" + trimmedKeyword + "'.");
            out.println("==================================================");
            return;
        }

        out.println("Displaying " + alumniList.size() + " matching alumni for '" + trimmedKeyword + "':");
        out.println("--------------------------------------------------");

        for (int i = 0; i < alumniList.size(); i++) {
            printAlumniCard(i + 1, alumniList.get(i));
        }

        out.println("==================================================");
        out.println("[INFO] End of Alumni Search Results.");

        promptViewAlumniFromList(alumniList);
    }

    // ── Filtering (Prompt 14) ─────────────────────────────────────────────────

    /**
     * Displays the Alumni Filters menu, reads the student's choice, and dispatches to the
     * appropriate filter handler (Prompt 14).
     *
     * @throws SecurityException if the active session is unauthenticated or not a Student
     */
    public void promptAndFilterAlumni() {
        validateStudentSession();

        out.println("\n==================================================");
        out.println("                 ALUMNI FILTERS                   ");
        out.println("==================================================");
        out.println("  [1] Filter by Graduation Year");
        out.println("  [2] Filter by Industry");
        out.println("  [3] Filter by Experience");
        out.println("  [4] Filter by Mentorship Availability");
        out.println("  [5] Filter by Department");
        out.println("  [6] Filter by Degree/Course");
        out.println("  [7] Apply Multiple Filters");
        out.println("  [0] Back");
        out.println("--------------------------------------------------");
        out.print("Enter your choice: ");

        if (!scanner.hasNextLine()) {
            return;
        }

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1" -> handleFilterByGraduationYear();
            case "2" -> handleFilterByIndustry();
            case "3" -> handleFilterByExperience();
            case "4" -> handleFilterByMentorship();
            case "5" -> handleFilterByDepartment();
            case "6" -> handleFilterByDegree();
            case "7" -> handleApplyMultipleFilters();
            case "0" -> out.println("[AlumConnect] Returning to previous menu.");
            default  -> out.println("[AlumConnect] Invalid choice '" + choice + "'. Please enter one of the listed options.");
        }
    }

    /**
     * Interactive filter alias.
     *
     * @throws SecurityException if the active session is unauthenticated or not a Student
     */
    public void filterAlumni() {
        promptAndFilterAlumni();
    }

    /**
     * Executes filtering for programmatic callers using the supplied {@link AlumniFilterCriteria}.
     *
     * @param criteria the filter criteria to apply
     * @throws SecurityException if the active session is unauthenticated or not a Student
     */
    public void filterAlumni(AlumniFilterCriteria criteria) {
        validateStudentSession();
        displayFilteredResults(criteria);
    }

    // ── Filter Handlers ───────────────────────────────────────────────────────

    private void handleFilterByGraduationYear() {
        out.print("Enter graduation year: ");
        if (!scanner.hasNextLine()) return;
        String input = scanner.nextLine().trim();

        if (input.isEmpty()) {
            out.println("[ERROR] Graduation year cannot be empty.");
            return;
        }

        int year;
        try {
            year = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            out.println("[ERROR] Invalid graduation year. Please enter a valid number.");
            return;
        }

        if (!ValidationUtil.isValidGraduationYear(year)) {
            out.println("[ERROR] Graduation year must be between " +
                    Alumni.MIN_GRAD_YEAR + " and " + Alumni.MAX_GRAD_YEAR + ".");
            return;
        }

        AlumniFilterCriteria criteria = new AlumniFilterCriteria();
        criteria.setGraduationYear(year);
        displayFilteredResults(criteria);
    }

    private void handleFilterByIndustry() {
        out.print("Enter industry: ");
        if (!scanner.hasNextLine()) return;
        String input = scanner.nextLine().trim();

        if (input.isEmpty()) {
            out.println("[ERROR] Industry filter cannot be empty.");
            return;
        }

        AlumniFilterCriteria criteria = new AlumniFilterCriteria();
        criteria.setIndustry(input);
        displayFilteredResults(criteria);
    }

    private void handleFilterByExperience() {
        out.print("Minimum years of experience: ");
        if (!scanner.hasNextLine()) return;
        String input = scanner.nextLine().trim();

        if (input.isEmpty()) {
            out.println("[ERROR] Experience cannot be empty.");
            return;
        }

        int exp;
        try {
            exp = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            out.println("[ERROR] Invalid experience. Please enter a valid non-negative number.");
            return;
        }

        if (exp < 0) {
            out.println("[ERROR] Years of experience cannot be negative.");
            return;
        }

        AlumniFilterCriteria criteria = new AlumniFilterCriteria();
        criteria.setMinExperience(exp);
        displayFilteredResults(criteria);
    }

    private void handleFilterByMentorship() {
        out.println("Mentorship Availability:");
        out.println("  [1] Available for mentorship");
        out.println("  [2] Not available for mentorship");
        out.print("Select: ");
        if (!scanner.hasNextLine()) return;
        String input = scanner.nextLine().trim();

        Boolean available;
        if ("1".equals(input)) {
            available = true;
        } else if ("2".equals(input)) {
            available = false;
        } else {
            out.println("[ERROR] Invalid choice for mentorship availability. Please select 1 or 2.");
            return;
        }

        AlumniFilterCriteria criteria = new AlumniFilterCriteria();
        criteria.setMentorshipAvailable(available);
        displayFilteredResults(criteria);
    }

    private void handleFilterByDepartment() {
        out.print("Enter department: ");
        if (!scanner.hasNextLine()) return;
        String input = scanner.nextLine().trim();

        if (input.isEmpty()) {
            out.println("[ERROR] Department cannot be empty.");
            return;
        }

        AlumniFilterCriteria criteria = new AlumniFilterCriteria();
        criteria.setDepartment(input);
        displayFilteredResults(criteria);
    }

    private void handleFilterByDegree() {
        out.print("Enter degree/course: ");
        if (!scanner.hasNextLine()) return;
        String input = scanner.nextLine().trim();

        if (input.isEmpty()) {
            out.println("[ERROR] Degree cannot be empty.");
            return;
        }

        AlumniFilterCriteria criteria = new AlumniFilterCriteria();
        criteria.setDegree(input);
        displayFilteredResults(criteria);
    }

    private void handleApplyMultipleFilters() {
        out.println("\n[AlumConnect] Configure Multiple Filters (press Enter to skip any filter):");
        AlumniFilterCriteria criteria = new AlumniFilterCriteria();

        // 1. Graduation year
        out.print("Enter graduation year (or press Enter to skip): ");
        if (scanner.hasNextLine()) {
            String gradInput = scanner.nextLine().trim();
            if (!gradInput.isEmpty()) {
                try {
                    int year = Integer.parseInt(gradInput);
                    if (!ValidationUtil.isValidGraduationYear(year)) {
                        out.println("[ERROR] Graduation year must be between " +
                                Alumni.MIN_GRAD_YEAR + " and " + Alumni.MAX_GRAD_YEAR + ".");
                        return;
                    }
                    criteria.setGraduationYear(year);
                } catch (NumberFormatException e) {
                    out.println("[ERROR] Invalid graduation year. Please enter a valid number.");
                    return;
                }
            }
        }

        // 2. Industry
        out.print("Enter industry (or press Enter to skip): ");
        if (scanner.hasNextLine()) {
            String indInput = scanner.nextLine().trim();
            if (!indInput.isEmpty()) {
                criteria.setIndustry(indInput);
            }
        }

        // 3. Experience
        out.print("Minimum years of experience (or press Enter to skip): ");
        if (scanner.hasNextLine()) {
            String expInput = scanner.nextLine().trim();
            if (!expInput.isEmpty()) {
                try {
                    int exp = Integer.parseInt(expInput);
                    if (exp < 0) {
                        out.println("[ERROR] Years of experience cannot be negative.");
                        return;
                    }
                    criteria.setMinExperience(exp);
                } catch (NumberFormatException e) {
                    out.println("[ERROR] Invalid experience. Please enter a valid non-negative number.");
                    return;
                }
            }
        }

        // 4. Mentorship availability
        out.print("Mentorship availability ([1] Available, [2] Not available, Enter to skip): ");
        if (scanner.hasNextLine()) {
            String mInput = scanner.nextLine().trim();
            if ("1".equals(mInput)) {
                criteria.setMentorshipAvailable(true);
            } else if ("2".equals(mInput)) {
                criteria.setMentorshipAvailable(false);
            } else if (!mInput.isEmpty()) {
                out.println("[ERROR] Invalid choice for mentorship availability. Please select 1 or 2.");
                return;
            }
        }

        // 5. Department
        out.print("Enter department (or press Enter to skip): ");
        if (scanner.hasNextLine()) {
            String deptInput = scanner.nextLine().trim();
            if (!deptInput.isEmpty()) {
                criteria.setDepartment(deptInput);
            }
        }

        // 6. Degree
        out.print("Enter degree/course (or press Enter to skip): ");
        if (scanner.hasNextLine()) {
            String degInput = scanner.nextLine().trim();
            if (!degInput.isEmpty()) {
                criteria.setDegree(degInput);
            }
        }

        displayFilteredResults(criteria);
    }

    /**
     * Executes filtering for the given criteria and displays matching alumni cards or a friendly message.
     * After listing results, offers an option to view any alumni's detailed profile (Prompt 15).
     */
    private void displayFilteredResults(AlumniFilterCriteria criteria) {
        out.println("\n==================================================");
        out.println("                 FILTERED RESULTS                 ");
        out.println("==================================================");

        AlumniDirectoryResult result = directoryService.filterAlumni(criteria);
        if (!result.isSuccess()) {
            out.println("[ERROR] " + result.getMessage());
            out.println("==================================================");
            return;
        }

        List<Alumni> list = result.getAlumniList();
        if (list.isEmpty()) {
            out.println("No alumni found matching the selected filters.");
            out.println("==================================================");
            return;
        }

        out.println("Displaying " + list.size() + " matching alumni for selected filters:");
        out.println("--------------------------------------------------");

        for (int i = 0; i < list.size(); i++) {
            printAlumniCard(i + 1, list.get(i));
        }

        out.println("==================================================");
        out.println("[INFO] End of Filtered Alumni Results.");

        promptViewAlumniFromList(list);
    }

    // ── Profile Viewing (Prompt 15) ───────────────────────────────────────────

    /**
     * Fetches and displays the detailed public profile of a specific alumni by their user ID.
     *
     * <p>Displays all public fields including Bio, LinkedIn URL, and mentorship details.
     * Never exposes password hashes, salts, or session information.
     *
     * @param alumniId the {@code users.id} PK of the target alumni
     * @throws SecurityException if the active session is unauthenticated or not a Student
     */
    public void viewAlumniProfile(int alumniId) {
        validateStudentSession();

        AlumniDirectoryResult result = directoryService.viewAlumniProfile(alumniId);
        if (!result.isSuccess()) {
            out.println("[ERROR] " + result.getMessage());
            return;
        }

        List<Alumni> list = result.getAlumniList();
        if (list.isEmpty()) {
            out.println("[AlumConnect] Alumni profile not found.");
            return;
        }

        printDetailedAlumniProfile(list.get(0));
    }

    /**
     * After listing an alumni result set, prompts the student to choose a number to view
     * a detailed profile, or 0 to return to the previous menu.
     *
     * @param alumniList the list of alumni currently on screen (1-based selection)
     */
    private void promptViewAlumniFromList(List<Alumni> alumniList) {
        if (alumniList == null || alumniList.isEmpty()) {
            return;
        }

        out.println("\nEnter the number to view an alumni's profile, or 0 to go back: ");
        if (!scanner.hasNextLine()) {
            return;
        }

        String input = scanner.nextLine().trim();
        if ("0".equals(input)) {
            out.println("[AlumConnect] Returning to previous menu.");
            return;
        }

        int index;
        try {
            index = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            out.println("[AlumConnect] Invalid input. Please enter a number.");
            return;
        }

        if (index < 1 || index > alumniList.size()) {
            out.println("[AlumConnect] Invalid selection. Please enter a number between 1 and " + alumniList.size() + ".");
            return;
        }

        // Use the alumni's user ID (getId()) for the service lookup
        Alumni selected = alumniList.get(index - 1);
        if (selected.getId() == null || selected.getId() <= 0) {
            out.println("[AlumConnect] Cannot view profile: alumni ID is missing.");
            return;
        }

        viewAlumniProfile(selected.getId());
    }

    /**
     * Prints the full detailed profile of an alumni, including all public fields
     * (Bio, LinkedIn, mentorship details) not shown in the compact card.
     *
     * @param a the Alumni instance
     */
    private void printDetailedAlumniProfile(Alumni a) {
        String gradYearStr = a.getGraduationYear() > 0 ? String.valueOf(a.getGraduationYear()) : "Not Specified";
        String expStr = (a.getProfileId() != null && a.getYearsOfExperience() >= 0)
                ? a.getYearsOfExperience() + " years"
                : "Not Specified";
        String mentorshipStr = a.isAvailableForMentorship() ? "Yes" : "No";
        String maxMenteesStr = a.isAvailableForMentorship() && a.getMaxMentees() > 0
                ? String.valueOf(a.getMaxMentees())
                : "N/A";

        out.println("\n==================================================");
        out.println("               ALUMNI PROFILE DETAILS             ");
        out.println("==================================================");
        out.println("  Full Name            : " + safeString(a.getFullName()));
        out.println("  Graduation Year      : " + gradYearStr);
        out.println("  Degree               : " + safeString(a.getDegree()));
        out.println("  Department           : " + safeString(a.getDepartment()));
        out.println("  Current Company      : " + safeString(a.getCurrentCompany()));
        out.println("  Current Role         : " + safeString(a.getCurrentRole()));
        out.println("  Industry             : " + safeString(a.getIndustry()));
        out.println("  Experience           : " + expStr);
        out.println("  Skills / Expertise   : " + safeString(a.getExpertiseAreas()));
        out.println("  Bio                  : " + safeString(a.getBio()));
        out.println("  LinkedIn             : " + safeString(a.getLinkedinUrl()));
        out.println("  Mentorship Available : " + mentorshipStr);
        out.println("  Max Mentees          : " + maxMenteesStr);
        out.println("==================================================");
    }

    // ── Session Validation & Display Helpers ──────────────────────────────────

    /**
     * Validates that the active session belongs to an authenticated student.
     *
     * @throws SecurityException if unauthenticated or wrong role
     */
    private void validateStudentSession() {
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
