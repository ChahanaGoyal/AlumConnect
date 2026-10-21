package com.alumconnect.service.dto;

/**
 * Data Transfer Object encapsulating the fields a student may update on
 * their own profile after registration.
 *
 * <p>All fields that exist in the {@code student_profiles} table and are
 * mutable post-registration are included here. Required fields are:
 * {@code department} and {@code graduationYear}. All other fields are optional.
 */
public class StudentProfileRequest {

    // Required
    private String department;
    private Integer graduationYear;

    // Optional — default to existing values if not provided
    private String degree;
    private Integer currentSemester;
    private String enrollmentNumber;
    private String bio;
    private String skills;
    private String interests;
    private String linkedinUrl;
    private String githubUrl;

    public StudentProfileRequest() {
    }

    // ── Getters and Setters ───────────────────────────────────────────────────

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public Integer getGraduationYear() { return graduationYear; }
    public void setGraduationYear(Integer graduationYear) { this.graduationYear = graduationYear; }

    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }

    public Integer getCurrentSemester() { return currentSemester; }
    public void setCurrentSemester(Integer currentSemester) { this.currentSemester = currentSemester; }

    public String getEnrollmentNumber() { return enrollmentNumber; }
    public void setEnrollmentNumber(String enrollmentNumber) { this.enrollmentNumber = enrollmentNumber; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }

    public String getInterests() { return interests; }
    public void setInterests(String interests) { this.interests = interests; }

    public String getLinkedinUrl() { return linkedinUrl; }
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }

    public String getGithubUrl() { return githubUrl; }
    public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }
}
