package com.alumconnect.service.dto;

import com.alumconnect.model.Alumni;
import com.alumconnect.util.ValidationUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates structured filter criteria for narrowing down the alumni directory (Prompt 14).
 *
 * <p>Supported criteria:
 * <ul>
 *   <li>Graduation year (exact match)</li>
 *   <li>Industry (case-insensitive substring match)</li>
 *   <li>Minimum years of experience (&gt;= specified value)</li>
 *   <li>Mentorship availability (boolean match)</li>
 *   <li>Department (case-insensitive substring match)</li>
 *   <li>Degree / Course (case-insensitive substring match)</li>
 * </ul>
 *
 * <p>All non-null criteria are evaluated together using conjunction (AND logic).
 * Any null or unselected criterion does not restrict the results.
 */
public class AlumniFilterCriteria {

    private Integer graduationYear;
    private String industry;
    private Integer minExperience;
    private Boolean mentorshipAvailable;
    private String department;
    private String degree;

    public AlumniFilterCriteria() {
    }

    public Integer getGraduationYear() {
        return graduationYear;
    }

    public void setGraduationYear(Integer graduationYear) {
        this.graduationYear = graduationYear;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = (industry != null && !industry.isBlank()) ? industry.trim() : null;
    }

    public Integer getMinExperience() {
        return minExperience;
    }

    public void setMinExperience(Integer minExperience) {
        this.minExperience = minExperience;
    }

    public Boolean getMentorshipAvailable() {
        return mentorshipAvailable;
    }

    public void setMentorshipAvailable(Boolean mentorshipAvailable) {
        this.mentorshipAvailable = mentorshipAvailable;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = (department != null && !department.isBlank()) ? department.trim() : null;
    }

    public String getDegree() {
        return degree;
    }

    public void setDegree(String degree) {
        this.degree = (degree != null && !degree.isBlank()) ? degree.trim() : null;
    }

    /**
     * Checks if at least one filter criterion is active.
     *
     * @return true if any filter is set, false otherwise
     */
    public boolean hasAnyFilter() {
        return graduationYear != null
                || industry != null
                || minExperience != null
                || mentorshipAvailable != null
                || department != null
                || degree != null;
    }

    /**
     * Validates the criteria values.
     *
     * @return list of error messages, empty if valid
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (graduationYear != null) {
            if (!ValidationUtil.isValidGraduationYear(graduationYear)) {
                errors.add("Graduation year must be between " +
                        Alumni.MIN_GRAD_YEAR + " and " + Alumni.MAX_GRAD_YEAR + ".");
            }
        }
        if (minExperience != null && minExperience < 0) {
            errors.add("Years of experience cannot be negative.");
        }
        return errors;
    }

    /**
     * In-memory matching logic used for test fakes and default repository implementations.
     *
     * @param alumni the alumni to test
     * @return true if all active criteria match
     */
    public boolean matches(Alumni alumni) {
        if (alumni == null) {
            return false;
        }

        if (graduationYear != null && alumni.getGraduationYear() != graduationYear) {
            return false;
        }

        if (industry != null) {
            if (alumni.getIndustry() == null ||
                    !alumni.getIndustry().toLowerCase().contains(industry.toLowerCase())) {
                return false;
            }
        }

        if (minExperience != null && alumni.getYearsOfExperience() < minExperience) {
            return false;
        }

        if (mentorshipAvailable != null && alumni.isAvailableForMentorship() != mentorshipAvailable) {
            return false;
        }

        if (department != null) {
            if (alumni.getDepartment() == null ||
                    !alumni.getDepartment().toLowerCase().contains(department.toLowerCase())) {
                return false;
            }
        }

        if (degree != null) {
            if (alumni.getDegree() == null ||
                    !alumni.getDegree().toLowerCase().contains(degree.toLowerCase())) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return "AlumniFilterCriteria{" +
                "graduationYear=" + graduationYear +
                ", industry='" + industry + '\'' +
                ", minExperience=" + minExperience +
                ", mentorshipAvailable=" + mentorshipAvailable +
                ", department='" + department + '\'' +
                ", degree='" + degree + '\'' +
                '}';
    }
}

