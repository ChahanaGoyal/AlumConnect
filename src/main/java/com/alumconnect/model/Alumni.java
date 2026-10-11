package com.alumconnect.model;

import java.util.Objects;

/**
 * Represents a graduated alumnus on the AlumConnect platform.
 *
 * <p>Extends {@link User} (role = {@link Role#ALUMNI}) and maps the additional
 * profile fields from the {@code alumni_profiles} table:
 * <pre>
 *  profileId               ← alumni_profiles.id                       (INT PK)
 *  graduationYear          ← alumni_profiles.graduation_year           (INT, 1950–2100)
 *  degree                  ← alumni_profiles.degree                    (VARCHAR 100, default 'Bachelor of Technology')
 *  department              ← alumni_profiles.department                (VARCHAR 100)
 *  currentCompany          ← alumni_profiles.current_company           (VARCHAR 150, nullable)
 *  currentRole             ← alumni_profiles.current_role              (VARCHAR 100, nullable)
 *  industry                ← alumni_profiles.industry                  (VARCHAR 100, nullable)
 *  yearsOfExperience       ← alumni_profiles.years_of_experience       (INT DEFAULT 0, >= 0)
 *  expertiseAreas          ← alumni_profiles.expertise_areas           (TEXT, nullable)
 *  bio                     ← alumni_profiles.bio                       (TEXT, nullable)
 *  linkedinUrl             ← alumni_profiles.linkedin_url              (VARCHAR 255, nullable)
 *  availableForMentorship  ← alumni_profiles.is_available_for_mentorship (BOOLEAN DEFAULT TRUE)
 *  maxMentees              ← alumni_profiles.max_mentees               (INT DEFAULT 3, >= 0)
 * </pre>
 *
 * <p>The FK {@code alumni_profiles.user_id} is represented by the inherited
 * {@link User#getId()} — no separate {@code userId} field is needed in the model.
 *
 * <p>Invariants enforced by setters:
 * <ul>
 *   <li>{@code graduationYear} must be in [1950, 2100]</li>
 *   <li>{@code yearsOfExperience} must be >= 0</li>
 *   <li>{@code maxMentees} must be >= 0</li>
 * </ul>
 */
public class Alumni extends User {

    // ── Constants matching DB constraints ────────────────────────────────────

    /** Default degree as per DB {@code DEFAULT 'Bachelor of Technology'}. */
    public static final String DEFAULT_DEGREE = "Bachelor of Technology";

    /** Default max mentees as per DB {@code DEFAULT 3}. */
    public static final int DEFAULT_MAX_MENTEES = 3;

    /** Minimum allowed graduation year — matches DB CHECK constraint. */
    public static final int MIN_GRAD_YEAR = 1950;

    /** Maximum allowed graduation year — matches DB CHECK constraint. */
    public static final int MAX_GRAD_YEAR = 2100;

    // ── alumni_profiles fields ────────────────────────────────────────────────

    /** PK of the {@code alumni_profiles} row. Null until persisted. */
    private Integer profileId;

    /** Year the alumnus graduated. */
    private int graduationYear;

    /** Degree completed, e.g. "Bachelor of Technology". */
    private String degree;

    /** Academic department, e.g. "Electronics and Communication". */
    private String department;

    /** Current employer — nullable. */
    private String currentCompany;

    /** Current job title / role — nullable. */
    private String currentRole;

    /** Industry sector, e.g. "Software", "Finance" — nullable. */
    private String industry;

    /** Total years of professional experience (>= 0). */
    private int yearsOfExperience;

    /** Free-text description of areas of expertise — nullable. */
    private String expertiseAreas;

    /** Free-text biography — nullable. */
    private String bio;

    /** LinkedIn profile URL — nullable. */
    private String linkedinUrl;

    /** Whether this alumnus is accepting mentorship requests. */
    private boolean availableForMentorship;

    /** Maximum number of concurrent mentees this alumnus will accept (>= 0). */
    private int maxMentees;

    // ── Constructors ──────────────────────────────────────────────────────────

    /**
     * No-arg constructor for JDBC row-mapping.
     * Sets role to {@link Role#ALUMNI} and applies DB defaults.
     */
    public Alumni() {
        super();
        setRole(Role.ALUMNI);
        this.degree                 = DEFAULT_DEGREE;
        this.yearsOfExperience      = 0;
        this.availableForMentorship = true;
        this.maxMentees             = DEFAULT_MAX_MENTEES;
    }

    /**
     * Convenience constructor for creating a new alumni account before persistence.
     * Optional profile fields (company, role, industry, expertise, bio, LinkedIn)
     * default to {@code null} and can be set via setters after construction.
     *
     * @param email          unique email
     * @param passwordHash   pre-computed hash — never a raw password
     * @param fullName       full display name
     * @param phone          optional phone (may be {@code null})
     * @param department     academic department (required)
     * @param graduationYear year of graduation
     */
    public Alumni(String email, String passwordHash, String fullName,
                  String phone, String department, int graduationYear) {
        super(email, passwordHash, fullName, Role.ALUMNI, phone);
        this.department             = Objects.requireNonNull(department, "department must not be null");
        setGraduationYear(graduationYear);
        this.degree                 = DEFAULT_DEGREE;
        this.yearsOfExperience      = 0;
        this.availableForMentorship = true;
        this.maxMentees             = DEFAULT_MAX_MENTEES;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** @return the {@code alumni_profiles.id} PK, or {@code null} if not yet persisted */
    public Integer getProfileId() { return profileId; }

    /** @return the graduation year */
    public int getGraduationYear() { return graduationYear; }

    /** @return the completed degree */
    public String getDegree() { return degree; }

    /** @return the academic department */
    public String getDepartment() { return department; }

    /** @return the current employer, or {@code null} */
    public String getCurrentCompany() { return currentCompany; }

    /** @return the current job title, or {@code null} */
    public String getCurrentRole() { return currentRole; }

    /** @return the industry sector, or {@code null} */
    public String getIndustry() { return industry; }

    /** @return total years of professional experience */
    public int getYearsOfExperience() { return yearsOfExperience; }

    /** @return expertise areas text, or {@code null} */
    public String getExpertiseAreas() { return expertiseAreas; }

    /** @return biography text, or {@code null} */
    public String getBio() { return bio; }

    /** @return LinkedIn profile URL, or {@code null} */
    public String getLinkedinUrl() { return linkedinUrl; }

    /** @return {@code true} if this alumnus is accepting mentorship requests */
    public boolean isAvailableForMentorship() { return availableForMentorship; }

    /** @return the maximum number of concurrent mentees accepted */
    public int getMaxMentees() { return maxMentees; }

    // ── Setters ───────────────────────────────────────────────────────────────

    /** @param profileId set by the repository after INSERT into {@code alumni_profiles} */
    public void setProfileId(Integer profileId) { this.profileId = profileId; }

    /**
     * Sets the graduation year.
     *
     * @param graduationYear must be in [{@value #MIN_GRAD_YEAR}, {@value #MAX_GRAD_YEAR}]
     * @throws IllegalArgumentException if out of range
     */
    public void setGraduationYear(int graduationYear) {
        if (graduationYear < MIN_GRAD_YEAR || graduationYear > MAX_GRAD_YEAR) {
            throw new IllegalArgumentException(
                "graduationYear must be between " + MIN_GRAD_YEAR + " and " + MAX_GRAD_YEAR
                + ", got: " + graduationYear);
        }
        this.graduationYear = graduationYear;
    }

    /**
     * @param degree degree name; must not be null
     */
    public void setDegree(String degree) {
        this.degree = Objects.requireNonNull(degree, "degree must not be null");
    }

    /**
     * @param department academic department; must not be null
     */
    public void setDepartment(String department) {
        this.department = Objects.requireNonNull(department, "department must not be null");
    }

    /** @param currentCompany optional employer; {@code null} is permitted */
    public void setCurrentCompany(String currentCompany) {
        this.currentCompany = currentCompany;
    }

    /** @param currentRole optional job title; {@code null} is permitted */
    public void setCurrentRole(String currentRole) {
        this.currentRole = currentRole;
    }

    /** @param industry optional industry sector; {@code null} is permitted */
    public void setIndustry(String industry) { this.industry = industry; }

    /**
     * Sets years of professional experience.
     *
     * @param yearsOfExperience must be >= 0
     * @throws IllegalArgumentException if negative
     */
    public void setYearsOfExperience(int yearsOfExperience) {
        if (yearsOfExperience < 0) {
            throw new IllegalArgumentException(
                "yearsOfExperience must be >= 0, got: " + yearsOfExperience);
        }
        this.yearsOfExperience = yearsOfExperience;
    }

    /** @param expertiseAreas optional expertise description; {@code null} is permitted */
    public void setExpertiseAreas(String expertiseAreas) {
        this.expertiseAreas = expertiseAreas;
    }

    /** @param bio optional biography; {@code null} is permitted */
    public void setBio(String bio) { this.bio = bio; }

    /** @param linkedinUrl optional LinkedIn URL; {@code null} is permitted */
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }

    /**
     * Toggles mentorship availability.
     *
     * @param availableForMentorship {@code false} to stop receiving new mentorship requests
     */
    public void setAvailableForMentorship(boolean availableForMentorship) {
        this.availableForMentorship = availableForMentorship;
    }

    /**
     * Sets the maximum number of mentees this alumnus can take on.
     *
     * @param maxMentees must be >= 0
     * @throws IllegalArgumentException if negative
     */
    public void setMaxMentees(int maxMentees) {
        if (maxMentees < 0) {
            throw new IllegalArgumentException(
                "maxMentees must be >= 0, got: " + maxMentees);
        }
        this.maxMentees = maxMentees;
    }

    // ── Business methods ──────────────────────────────────────────────────────

    /**
     * Returns {@code true} if this alumnus can accept at least one more mentee.
     * Both conditions must hold: {@code availableForMentorship == true} and
     * the caller must track current mentee count externally.
     *
     * <p>This model-level method checks availability flag only. The repository
     * layer is responsible for comparing against actual active mentee counts.
     *
     * @return {@code true} if the availability flag is set and {@code maxMentees > 0}
     */
    public boolean canAcceptMentee() {
        return availableForMentorship && maxMentees > 0;
    }

    /**
     * Returns a professional summary string suitable for display.
     *
     * @return e.g. {@code "Software Engineer at Google | 5 years exp | Dept: CSE | Grad: 2019"}
     */
    public String getProfessionalSummary() {
        StringBuilder sb = new StringBuilder();
        if (currentRole != null && !currentRole.isBlank()) {
            sb.append(currentRole);
        }
        if (currentCompany != null && !currentCompany.isBlank()) {
            if (!sb.isEmpty()) sb.append(" at ");
            sb.append(currentCompany);
        }
        if (!sb.isEmpty()) sb.append(" | ");
        sb.append(yearsOfExperience).append(" yrs exp");
        sb.append(" | ").append(department);
        sb.append(" | Grad: ").append(graduationYear);
        return sb.toString();
    }

    /**
     * Returns a mentorship status label for display.
     *
     * @return {@code "Open for Mentorship"} or {@code "Not Available"}
     */
    public String getMentorshipStatus() {
        return availableForMentorship ? "Open for Mentorship" : "Not Available";
    }

    // ── Object overrides ──────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "Alumni{" +
               "id=" + getId() +
               ", email='" + getEmail() + '\'' +
               ", fullName='" + getFullName() + '\'' +
               ", department='" + department + '\'' +
               ", graduationYear=" + graduationYear +
               ", currentCompany='" + currentCompany + '\'' +
               ", availableForMentorship=" + availableForMentorship +
               ", active=" + isActive() +
               '}';
    }
}
