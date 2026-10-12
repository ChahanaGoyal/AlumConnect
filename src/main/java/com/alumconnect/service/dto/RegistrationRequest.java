package com.alumconnect.service.dto;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.Role;
import com.alumconnect.model.Student;

/**
 * Data Transfer Object encapsulating registration input for both Student and Alumni roles.
 */
public class RegistrationRequest {

    // Common authentication & user fields
    private Role role;
    private String fullName;
    private String email;
    private String password;
    private String confirmPassword;
    private String phone;

    // Common academic fields
    private String department;
    private Integer graduationYear;
    private String degree = Student.DEFAULT_DEGREE;

    // Student-specific profile fields
    private Integer currentSemester = Student.MIN_SEMESTER;
    private String enrollmentNumber;
    private String skills;
    private String interests;
    private String githubUrl;

    // Alumni-specific profile fields
    private String currentCompany;
    private String currentRole;
    private String industry;
    private Integer yearsOfExperience = 0;
    private String expertiseAreas;
    private Boolean availableForMentorship = true;
    private Integer maxMentees = Alumni.DEFAULT_MAX_MENTEES;

    // Common optional profile fields
    private String bio;
    private String linkedinUrl;

    public RegistrationRequest() {
    }

    // ── Factory methods ───────────────────────────────────────────────────────

    /**
     * Factory helper to create a basic Student registration request.
     */
    public static RegistrationRequest forStudent(String fullName, String email, String password,
                                                 String department, int graduationYear) {
        RegistrationRequest req = new RegistrationRequest();
        req.setRole(Role.STUDENT);
        req.setFullName(fullName);
        req.setEmail(email);
        req.setPassword(password);
        req.setDepartment(department);
        req.setGraduationYear(graduationYear);
        return req;
    }

    /**
     * Factory helper to create a basic Alumni registration request.
     */
    public static RegistrationRequest forAlumni(String fullName, String email, String password,
                                                String department, int graduationYear) {
        RegistrationRequest req = new RegistrationRequest();
        req.setRole(Role.ALUMNI);
        req.setFullName(fullName);
        req.setEmail(email);
        req.setPassword(password);
        req.setDepartment(department);
        req.setGraduationYear(graduationYear);
        return req;
    }

    // ── Getters and Setters ───────────────────────────────────────────────────

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

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

    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }

    public String getInterests() { return interests; }
    public void setInterests(String interests) { this.interests = interests; }

    public String getGithubUrl() { return githubUrl; }
    public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }

    public String getCurrentCompany() { return currentCompany; }
    public void setCurrentCompany(String currentCompany) { this.currentCompany = currentCompany; }

    public String getCurrentRole() { return currentRole; }
    public void setCurrentRole(String currentRole) { this.currentRole = currentRole; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }

    public Integer getYearsOfExperience() { return yearsOfExperience; }
    public void setYearsOfExperience(Integer yearsOfExperience) { this.yearsOfExperience = yearsOfExperience; }

    public String getExpertiseAreas() { return expertiseAreas; }
    public void setExpertiseAreas(String expertiseAreas) { this.expertiseAreas = expertiseAreas; }

    public Boolean getAvailableForMentorship() { return availableForMentorship; }
    public void setAvailableForMentorship(Boolean availableForMentorship) { this.availableForMentorship = availableForMentorship; }

    public Integer getMaxMentees() { return maxMentees; }
    public void setMaxMentees(Integer maxMentees) { this.maxMentees = maxMentees; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getLinkedinUrl() { return linkedinUrl; }
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }
}
