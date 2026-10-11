package com.alumconnect.model;

import java.util.Objects;

/**
 * Represents an enrolled student on the AlumConnect platform.
 *
 * <p>Extends {@link User} (role = {@link Role#STUDENT}) and maps the additional
 * profile fields from the {@code student_profiles} table:
 * <pre>
 *  profileId        ← student_profiles.id               (INT PK)
 *  enrollmentNumber ← student_profiles.enrollment_number (VARCHAR 50, nullable/unique)
 *  degree           ← student_profiles.degree            (VARCHAR 100, default 'Bachelor of Technology')
 *  department       ← student_profiles.department        (VARCHAR 100)
 *  graduationYear   ← student_profiles.graduation_year   (INT, 1950–2100)
 *  currentSemester  ← student_profiles.current_semester  (INT, 1–12, default 1)
 *  bio              ← student_profiles.bio               (TEXT, nullable)
 *  skills           ← student_profiles.skills            (TEXT, nullable)
 *  interests        ← student_profiles.interests         (TEXT, nullable)
 *  linkedinUrl      ← student_profiles.linkedin_url      (VARCHAR 255, nullable)
 *  githubUrl        ← student_profiles.github_url        (VARCHAR 255, nullable)
 * </pre>
 *
 * <p>The FK {@code student_profiles.user_id} is represented by the inherited
 * {@link User#getId()} — no separate {@code userId} field is needed in the model.
 *
 * <p>Invariants enforced by setters:
 * <ul>
 *   <li>{@code currentSemester} must be in [1, 12]</li>
 *   <li>{@code graduationYear} must be in [1950, 2100]</li>
 * </ul>
 */
public class Student extends User {

    // ── Constants matching DB constraints ────────────────────────────────────

    /** Default degree as per DB {@code DEFAULT 'Bachelor of Technology'}. */
    public static final String DEFAULT_DEGREE = "Bachelor of Technology";

    /** Minimum allowed semester value — matches DB CHECK constraint. */
    public static final int MIN_SEMESTER = 1;

    /** Maximum allowed semester value — matches DB CHECK constraint. */
    public static final int MAX_SEMESTER = 12;

    /** Minimum allowed graduation year — matches DB CHECK constraint. */
    public static final int MIN_GRAD_YEAR = 1950;

    /** Maximum allowed graduation year — matches DB CHECK constraint. */
    public static final int MAX_GRAD_YEAR = 2100;

    // ── student_profiles fields ───────────────────────────────────────────────

    /** PK of the {@code student_profiles} row. Null until persisted. */
    private Integer profileId;

    /** Optional unique enrollment number — nullable in DB. */
    private String enrollmentNumber;

    /** Degree programme, e.g. "Bachelor of Technology". */
    private String degree;

    /** Academic department, e.g. "Computer Science". */
    private String department;

    /** Expected graduation year. */
    private int graduationYear;

    /** Current semester (1–12). */
    private int currentSemester;

    /** Free-text biography — nullable. */
    private String bio;

    /** Comma-separated or free-text skills list — nullable. */
    private String skills;

    /** Comma-separated or free-text interests — nullable. */
    private String interests;

    /** LinkedIn profile URL — nullable. */
    private String linkedinUrl;

    /** GitHub profile URL — nullable. */
    private String githubUrl;

    // ── Constructors ──────────────────────────────────────────────────────────

    /**
     * No-arg constructor for JDBC row-mapping.
     * Sets role to {@link Role#STUDENT} and applies DB defaults.
     */
    public Student() {
        super();
        setRole(Role.STUDENT);
        this.degree          = DEFAULT_DEGREE;
        this.currentSemester = MIN_SEMESTER;
    }

    /**
     * Convenience constructor for creating a new student account before persistence.
     * Profile-specific nullable fields (bio, skills, interests, URLs, enrollmentNumber)
     * default to {@code null} and can be set via setters after construction.
     *
     * @param email          unique email
     * @param passwordHash   pre-computed hash — never a raw password
     * @param fullName       full display name
     * @param phone          optional phone (may be {@code null})
     * @param department     academic department (required)
     * @param graduationYear expected graduation year
     */
    public Student(String email, String passwordHash, String fullName,
                   String phone, String department, int graduationYear) {
        super(email, passwordHash, fullName, Role.STUDENT, phone);
        this.department      = Objects.requireNonNull(department, "department must not be null");
        setGraduationYear(graduationYear);
        this.degree          = DEFAULT_DEGREE;
        this.currentSemester = MIN_SEMESTER;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** @return the {@code student_profiles.id} PK, or {@code null} if not yet persisted */
    public Integer getProfileId() { return profileId; }

    /** @return the optional enrollment number, or {@code null} */
    public String getEnrollmentNumber() { return enrollmentNumber; }

    /** @return the degree programme, e.g. "Bachelor of Technology" */
    public String getDegree() { return degree; }

    /** @return the academic department */
    public String getDepartment() { return department; }

    /** @return the expected graduation year */
    public int getGraduationYear() { return graduationYear; }

    /** @return the current semester (1–12) */
    public int getCurrentSemester() { return currentSemester; }

    /** @return the biography text, or {@code null} */
    public String getBio() { return bio; }

    /** @return the skills text, or {@code null} */
    public String getSkills() { return skills; }

    /** @return the interests text, or {@code null} */
    public String getInterests() { return interests; }

    /** @return the LinkedIn URL, or {@code null} */
    public String getLinkedinUrl() { return linkedinUrl; }

    /** @return the GitHub URL, or {@code null} */
    public String getGithubUrl() { return githubUrl; }

    // ── Setters ───────────────────────────────────────────────────────────────

    /** @param profileId set by the repository after INSERT into {@code student_profiles} */
    public void setProfileId(Integer profileId) { this.profileId = profileId; }

    /** @param enrollmentNumber optional enrollment number; {@code null} is permitted */
    public void setEnrollmentNumber(String enrollmentNumber) {
        this.enrollmentNumber = enrollmentNumber;
    }

    /**
     * @param degree degree programme name; must not be null
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

    /**
     * Sets the expected graduation year.
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
     * Sets the current semester.
     *
     * @param currentSemester must be in [{@value #MIN_SEMESTER}, {@value #MAX_SEMESTER}]
     * @throws IllegalArgumentException if out of range
     */
    public void setCurrentSemester(int currentSemester) {
        if (currentSemester < MIN_SEMESTER || currentSemester > MAX_SEMESTER) {
            throw new IllegalArgumentException(
                "currentSemester must be between " + MIN_SEMESTER + " and " + MAX_SEMESTER
                + ", got: " + currentSemester);
        }
        this.currentSemester = currentSemester;
    }

    /** @param bio optional biography text; {@code null} is permitted */
    public void setBio(String bio) { this.bio = bio; }

    /** @param skills optional skills text; {@code null} is permitted */
    public void setSkills(String skills) { this.skills = skills; }

    /** @param interests optional interests text; {@code null} is permitted */
    public void setInterests(String interests) { this.interests = interests; }

    /** @param linkedinUrl optional LinkedIn profile URL; {@code null} is permitted */
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }

    /** @param githubUrl optional GitHub profile URL; {@code null} is permitted */
    public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }

    // ── Business methods ──────────────────────────────────────────────────────

    /**
     * Returns {@code true} if this student is in their final semester
     * (semester == {@value #MAX_SEMESTER}).
     *
     * @return {@code true} for final-semester students
     */
    public boolean isFinalSemester() {
        return this.currentSemester == MAX_SEMESTER;
    }

    /**
     * Returns a formatted academic summary suitable for display.
     *
     * @return e.g. {@code "Computer Science | Sem 3 | Graduating 2027"}
     */
    public String getAcademicSummary() {
        return department + " | Sem " + currentSemester + " | Graduating " + graduationYear;
    }

    // ── Object overrides ──────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "Student{" +
               "id=" + getId() +
               ", email='" + getEmail() + '\'' +
               ", fullName='" + getFullName() + '\'' +
               ", department='" + department + '\'' +
               ", graduationYear=" + graduationYear +
               ", currentSemester=" + currentSemester +
               ", active=" + isActive() +
               '}';
    }
}
