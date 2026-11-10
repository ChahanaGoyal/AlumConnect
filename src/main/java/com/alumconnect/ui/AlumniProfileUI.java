package com.alumconnect.ui;

import com.alumconnect.model.Alumni;
import com.alumconnect.service.AlumniProfileService;
import com.alumconnect.service.MentorshipRequestService;
import com.alumconnect.service.dto.AlumniProfileRequest;
import com.alumconnect.service.dto.AlumniProfileResult;
import com.alumconnect.service.dto.MentorshipRequestResult;
import com.alumconnect.util.UserSession;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

/**
 * Console UI for the Alumni Profile feature (Prompt 8 &amp; Prompt 16).
 *
 * <p>Lets a logged-in alumni:
 * <ol>
 *   <li>View their full professional profile.</li>
 *   <li>Edit their profile fields interactively.</li>
 *   <li>Create a profile if none exists yet.</li>
 * </ol>
 *
 * <p>Lets a student viewing an alumni profile:
 * <ol>
 *   <li>View the complete alumni profile.</li>
 *   <li>Send a mentorship request (Prompt 16).</li>
 * </ol>
 *
 * <p>All access-control decisions are delegated to {@link AlumniProfileService}
 * and {@link MentorshipRequestService}; this class only handles I/O presentation.
 */
public class AlumniProfileUI {

    private final AlumniProfileService profileService;
    private final MentorshipRequestService mentorshipRequestService;
    private final Scanner scanner;
    private final PrintStream out;

    public AlumniProfileUI() {
        this(new AlumniProfileService(), new MentorshipRequestService(), new Scanner(System.in), System.out);
    }

    public AlumniProfileUI(AlumniProfileService profileService) {
        this(profileService, new MentorshipRequestService(), new Scanner(System.in), System.out);
    }

    public AlumniProfileUI(AlumniProfileService profileService,
                           InputStream in,
                           PrintStream out) {
        this(profileService, new MentorshipRequestService(), new Scanner(in), out);
    }

    public AlumniProfileUI(AlumniProfileService profileService,
                           Scanner scanner,
                           PrintStream out) {
        this(profileService, new MentorshipRequestService(), scanner, out);
    }

    public AlumniProfileUI(AlumniProfileService profileService,
                           MentorshipRequestService mentorshipRequestService,
                           Scanner scanner,
                           PrintStream out) {
        this.profileService = profileService;
        this.mentorshipRequestService = mentorshipRequestService;
        this.scanner = scanner;
        this.out = out;
    }

    // ── View Profile (Alumni Self) ────────────────────────────────────────────

    /**
     * Displays the current alumni's profile.
     *
     * @return the {@link AlumniProfileResult} from the service
     */
    public AlumniProfileResult displayMyProfile() {
        AlumniProfileResult result = profileService.viewMyProfile();

        if (result.isSuccess()) {
            printProfileDetails(result.getAlumni());
        } else {
            out.println("\n[ERROR] " + result.getMessage());
        }

        return result;
    }

    // ── View Profile & Mentorship (Student Flow) ──────────────────────────────

    /**
     * Displays an alumni's profile to a student and provides an option to send
     * a mentorship request (Prompt 16).
     *
     * @param alumni the target alumni
     */
    public void displayAlumniProfile(Alumni alumni) {
        if (alumni == null) {
            out.println("\n[ERROR] Alumni profile not found.");
            return;
        }

        ConsoleFormatter.printAppHeader(out, "ALUMNI PROFILE");
        out.println("  Full Name       : " + alumni.getFullName());
        out.println("  Email           : " + alumni.getEmail());
        if (alumni.getPhone() != null && !alumni.getPhone().isBlank()) {
            out.println("  Phone           : " + alumni.getPhone());
        }
        ConsoleFormatter.printDivider(out);
        out.println("  Degree          : " + alumni.getDegree());
        out.println("  Department      : " + alumni.getDepartment());
        out.println("  Graduation Year : " + alumni.getGraduationYear());
        if (alumni.getCurrentCompany() != null) {
            out.println("  Current Company : " + alumni.getCurrentCompany());
        }
        if (alumni.getCurrentRole() != null) {
            out.println("  Current Role    : " + alumni.getCurrentRole());
        }
        if (alumni.getIndustry() != null) {
            out.println("  Industry        : " + alumni.getIndustry());
        }
        out.println("  Experience      : " + alumni.getYearsOfExperience() + " years");
        if (alumni.getExpertiseAreas() != null) {
            out.println("  Expertise Areas : " + alumni.getExpertiseAreas());
        }
        ConsoleFormatter.printDivider(out);
        out.println("  Mentorship      : " + (alumni.isAvailableForMentorship() ? "Available" : "Not Available"));
        out.println("  Max Mentees     : " + alumni.getMaxMentees());
        if (alumni.getBio() != null) {
            out.println("  Bio             : " + alumni.getBio());
        }
        if (alumni.getLinkedinUrl() != null) {
            out.println("  LinkedIn        : " + alumni.getLinkedinUrl());
        }
        ConsoleFormatter.printDoubleDivider(out);

        if (UserSession.getInstance().isStudent()) {
            promptSendMentorshipRequest(alumni.getId());
        }
    }

    /**
     * Prompts the student to send a mentorship request to the target alumni.
     *
     * @param alumniId the target alumni's user ID
     */
    public void promptSendMentorshipRequest(int alumniId) {
        // Guard: only authenticated students may send mentorship requests.
        if (!com.alumconnect.util.UserSession.getInstance().isLoggedIn()) {
            out.println("[AlumConnect] Access denied: You must be logged in to send a mentorship request.");
            throw new SecurityException("Unauthorized: No user is currently logged in.");
        }
        if (!com.alumconnect.util.UserSession.getInstance().isStudent()) {
            out.println("[AlumConnect] Access denied: Only students can send mentorship requests.");
            throw new SecurityException("Access Denied: Only students can send mentorship requests.");
        }
        out.println("\nMentorship Options:");
        out.println("  [1] Send Mentorship Request");
        out.println("  [0] Back");
        out.print("Select: ");

        if (!scanner.hasNextLine()) {
            return;
        }

        String choice = scanner.nextLine().trim();
        if ("1".equals(choice) || "y".equalsIgnoreCase(choice) || "yes".equalsIgnoreCase(choice)) {
            out.print("Enter Mentorship Topic: ");
            if (!scanner.hasNextLine()) return;
            String topic = scanner.nextLine().trim();

            out.print("Enter Mentorship Message: ");
            if (!scanner.hasNextLine()) return;
            String message = scanner.nextLine().trim();

            MentorshipRequestResult result = mentorshipRequestService.sendRequest(alumniId, topic, message);

            if (result.isSuccess()) {
                out.println("\n[SUCCESS] " + result.getMessage());
            } else {
                out.println("\n[ERROR] " + result.getMessage());
            }
        }
    }

    // ── Edit Profile ──────────────────────────────────────────────────────────

    /**
     * Interactive edit flow: loads existing profile, prompts for each field
     * (showing current value so the alumni can press Enter to keep it),
     * then submits the update.
     *
     * @return the {@link AlumniProfileResult} from the update call
     */
    public AlumniProfileResult promptAndEditProfile() {
        ConsoleFormatter.printAppHeader(out, "Edit My Alumni Profile");
        out.println("[INFO] Press Enter to keep the current value for any field.");

        // First load existing data so we can show current values as defaults
        AlumniProfileResult loadResult = profileService.viewMyProfile();
        if (!loadResult.isSuccess()) {
            out.println("[ERROR] " + loadResult.getMessage());
            return loadResult;
        }

        Alumni current = loadResult.getAlumni();
        AlumniProfileRequest request = new AlumniProfileRequest();

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
                return AlumniProfileResult.failure("Invalid graduation year input.");
            }
            request.setGraduationYear(parsed);
        }

        // ── Optional fields ───────────────────────────────────────────────────

        // Degree
        out.print("Degree/Programme [" + current.getDegree() + "]: ");
        String degree = readLine();
        request.setDegree(degree.isBlank() ? current.getDegree() : degree.trim());

        // Current Company
        out.print("Current Employer/Company [" + nullOr(current.getCurrentCompany(), "none") + "]: ");
        String company = readLine();
        request.setCurrentCompany(company.isBlank() ? current.getCurrentCompany() : company.trim());

        // Current Role
        out.print("Current Job Title/Role [" + nullOr(current.getCurrentRole(), "none") + "]: ");
        String role = readLine();
        request.setCurrentRole(role.isBlank() ? current.getCurrentRole() : role.trim());

        // Industry
        out.print("Industry Sector [" + nullOr(current.getIndustry(), "none") + "]: ");
        String industry = readLine();
        request.setIndustry(industry.isBlank() ? current.getIndustry() : industry.trim());

        // Years of Experience
        out.print("Years of Experience [" + current.getYearsOfExperience() + "]: ");
        String expInput = readLine();
        if (expInput.isBlank()) {
            request.setYearsOfExperience(current.getYearsOfExperience());
        } else {
            Integer exp = parseIntOrNull(expInput);
            if (exp == null) {
                out.println("[ERROR] Years of experience must be a number. Profile not updated.");
                return AlumniProfileResult.failure("Invalid years of experience input.");
            }
            request.setYearsOfExperience(exp);
        }

        // Areas of Expertise
        out.print("Areas of Expertise [" + nullOr(current.getExpertiseAreas(), "none") + "]: ");
        String expertise = readLine();
        request.setExpertiseAreas(expertise.isBlank() ? current.getExpertiseAreas() : expertise.trim());

        // Bio
        out.print("Bio [" + nullOr(current.getBio(), "none") + "]: ");
        String bio = readLine();
        request.setBio(bio.isBlank() ? current.getBio() : bio.trim());

        // LinkedIn URL
        out.print("LinkedIn URL [" + nullOr(current.getLinkedinUrl(), "none") + "]: ");
        String linkedin = readLine();
        request.setLinkedinUrl(linkedin.isBlank() ? current.getLinkedinUrl() : linkedin.trim());

        // Mentorship Availability
        String currentAvailStr = current.isAvailableForMentorship() ? "Y" : "N";
        out.print("Available for Mentorship (Y/N) [" + currentAvailStr + "]: ");
        String availInput = readLine().trim();
        if (availInput.isBlank()) {
            request.setAvailableForMentorship(current.isAvailableForMentorship());
        } else if ("y".equalsIgnoreCase(availInput) || "yes".equalsIgnoreCase(availInput)) {
            request.setAvailableForMentorship(true);
        } else if ("n".equalsIgnoreCase(availInput) || "no".equalsIgnoreCase(availInput)) {
            request.setAvailableForMentorship(false);
        } else {
            request.setAvailableForMentorship(current.isAvailableForMentorship());
        }

        // Max Mentees
        out.print("Maximum Mentees [" + current.getMaxMentees() + "]: ");
        String maxMenteesInput = readLine();
        if (maxMenteesInput.isBlank()) {
            request.setMaxMentees(current.getMaxMentees());
        } else {
            Integer maxM = parseIntOrNull(maxMenteesInput);
            if (maxM == null) {
                out.println("[ERROR] Maximum mentees must be a number. Profile not updated.");
                return AlumniProfileResult.failure("Invalid maximum mentees input.");
            }
            request.setMaxMentees(maxM);
        }

        // ── Submit ────────────────────────────────────────────────────────────
        out.println("\n[AlumConnect] Saving profile changes...");
        AlumniProfileResult result = profileService.updateMyProfile(request);

        if (result.isSuccess()) {
            out.println("[SUCCESS] " + result.getMessage());
            printProfileDetails(result.getAlumni());
        } else {
            out.println("[ERROR] " + result.getMessage());
        }

        return result;
    }

    // ── Create Profile ────────────────────────────────────────────────────────

    /**
     * Interactive profile creation flow for an alumni who does not have a profile yet.
     *
     * @return the {@link AlumniProfileResult} from the creation call
     */
    public AlumniProfileResult promptAndCreateProfile() {
        ConsoleFormatter.printAppHeader(out, "Create My Alumni Profile");

        AlumniProfileRequest request = new AlumniProfileRequest();

        out.print("Department (Required): ");
        request.setDepartment(readLine().trim());

        out.print("Graduation Year (Required, e.g., 2020): ");
        String gradYearStr = readLine().trim();
        Integer gradYear = parseIntOrNull(gradYearStr);
        if (gradYear == null) {
            out.println("[ERROR] Graduation year must be a valid number.");
            return AlumniProfileResult.failure("Invalid graduation year input.");
        }
        request.setGraduationYear(gradYear);

        out.print("Degree/Programme (Optional, default B.Tech): ");
        String degree = readLine().trim();
        request.setDegree(degree.isEmpty() ? Alumni.DEFAULT_DEGREE : degree);

        out.print("Current Employer/Company (Optional): ");
        String company = readLine().trim();
        if (!company.isEmpty()) request.setCurrentCompany(company);

        out.print("Current Job Title/Role (Optional): ");
        String role = readLine().trim();
        if (!role.isEmpty()) request.setCurrentRole(role);

        out.print("Industry Sector (Optional): ");
        String industry = readLine().trim();
        if (!industry.isEmpty()) request.setIndustry(industry);

        out.print("Years of Experience (Optional, default 0): ");
        String expStr = readLine().trim();
        if (!expStr.isEmpty()) {
            Integer exp = parseIntOrNull(expStr);
            if (exp == null) {
                out.println("[ERROR] Years of experience must be a valid number.");
                return AlumniProfileResult.failure("Invalid years of experience input.");
            }
            request.setYearsOfExperience(exp);
        } else {
            request.setYearsOfExperience(0);
        }

        out.print("Areas of Expertise (Optional, comma-separated): ");
        String expertise = readLine().trim();
        if (!expertise.isEmpty()) request.setExpertiseAreas(expertise);

        out.print("Bio (Optional): ");
        String bio = readLine().trim();
        if (!bio.isEmpty()) request.setBio(bio);

        out.print("LinkedIn URL (Optional): ");
        String linkedin = readLine().trim();
        if (!linkedin.isEmpty()) request.setLinkedinUrl(linkedin);

        out.print("Available for mentorship? (Y/N, default Y): ");
        String mentorship = readLine().trim();
        if ("n".equalsIgnoreCase(mentorship) || "no".equalsIgnoreCase(mentorship)) {
            request.setAvailableForMentorship(false);
        } else {
            request.setAvailableForMentorship(true);
        }

        out.print("Maximum Mentees (Optional, default 3): ");
        String maxMenteesStr = readLine().trim();
        if (!maxMenteesStr.isEmpty()) {
            Integer maxM = parseIntOrNull(maxMenteesStr);
            if (maxM == null) {
                out.println("[ERROR] Maximum mentees must be a valid number.");
                return AlumniProfileResult.failure("Invalid maximum mentees input.");
            }
            request.setMaxMentees(maxM);
        } else {
            request.setMaxMentees(Alumni.DEFAULT_MAX_MENTEES);
        }

        out.println("\n[AlumConnect] Creating profile...");
        AlumniProfileResult result = profileService.createMyProfile(request);

        if (result.isSuccess()) {
            out.println("[SUCCESS] " + result.getMessage());
            printProfileDetails(result.getAlumni());
        } else {
            out.println("[ERROR] " + result.getMessage());
        }

        return result;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void printProfileDetails(Alumni alumni) {
        ConsoleFormatter.printAppHeader(out, "MY ALUMNI PROFILE");
        out.println("  Full Name       : " + alumni.getFullName());
        out.println("  Email           : " + alumni.getEmail());
        if (alumni.getPhone() != null && !alumni.getPhone().isBlank()) {
            out.println("  Phone           : " + alumni.getPhone());
        }
        ConsoleFormatter.printDivider(out);
        out.println("  Degree          : " + alumni.getDegree());
        out.println("  Department      : " + alumni.getDepartment());
        out.println("  Graduation Year : " + alumni.getGraduationYear());
        if (alumni.getCurrentCompany() != null) {
            out.println("  Current Company : " + alumni.getCurrentCompany());
        }
        if (alumni.getCurrentRole() != null) {
            out.println("  Current Role    : " + alumni.getCurrentRole());
        }
        if (alumni.getIndustry() != null) {
            out.println("  Industry        : " + alumni.getIndustry());
        }
        out.println("  Experience      : " + alumni.getYearsOfExperience() + " years");
        if (alumni.getExpertiseAreas() != null) {
            out.println("  Expertise Areas : " + alumni.getExpertiseAreas());
        }
        ConsoleFormatter.printDivider(out);
        out.println("  Mentorship      : " + alumni.getMentorshipStatus());
        out.println("  Max Mentees     : " + alumni.getMaxMentees());
        if (alumni.getBio() != null) {
            out.println("  Bio             : " + alumni.getBio());
        }
        if (alumni.getLinkedinUrl() != null) {
            out.println("  LinkedIn        : " + alumni.getLinkedinUrl());
        }
        ConsoleFormatter.printDoubleDivider(out);
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
