package com.alumconnect.service.dto;

import com.alumconnect.model.Alumni;

/**
 * Data Transfer Object encapsulating the fields an alumni may update or create on
 * their own professional profile after registration.
 */
public class AlumniProfileRequest {

    // Required fields
    private String department;
    private Integer graduationYear;

    // Optional fields
    private String degree;
    private String currentCompany;
    private String currentRole;
    private String industry;
    private Integer yearsOfExperience;
    private String expertiseAreas;
    private String bio;
    private String linkedinUrl;
    private Boolean availableForMentorship;
    private Integer maxMentees;

    public AlumniProfileRequest() {
    }

    // ── Getters and Setters ───────────────────────────────────────────────────

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public Integer getGraduationYear() { return graduationYear; }
    public void setGraduationYear(Integer graduationYear) { this.graduationYear = graduationYear; }

    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }

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

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getLinkedinUrl() { return linkedinUrl; }
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }

    public Boolean getAvailableForMentorship() { return availableForMentorship; }
    public void setAvailableForMentorship(Boolean availableForMentorship) { this.availableForMentorship = availableForMentorship; }

    public Integer getMaxMentees() { return maxMentees; }
    public void setMaxMentees(Integer maxMentees) { this.maxMentees = maxMentees; }
}
