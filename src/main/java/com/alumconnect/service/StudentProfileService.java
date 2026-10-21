package com.alumconnect.service;

import com.alumconnect.model.Role;
import com.alumconnect.model.Student;
import com.alumconnect.repository.JdbcStudentProfileRepository;
import com.alumconnect.repository.StudentProfileRepository;
import com.alumconnect.service.dto.StudentProfileRequest;
import com.alumconnect.service.dto.StudentProfileResult;
import com.alumconnect.util.UserSession;
import com.alumconnect.util.ValidationUtil;
import com.alumconnect.model.User;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service handling student profile viewing and editing operations.
 *
 * <p>Enforces the following security invariants:
 * <ul>
 *   <li>Only a logged-in user with role {@code STUDENT} may call these operations.</li>
 *   <li>A student may only view or modify their own profile — the {@code userId} used
 *       in every repository call is always taken from the active session, never from
 *       untrusted caller-supplied input.</li>
 * </ul>
 *
 * <p>Validation reuses {@link ValidationUtil} for graduation year and semester ranges,
 * consistent with registration validation in {@link UserService}.
 */
public class StudentProfileService {

    private final StudentProfileRepository profileRepository;

    /**
     * Default constructor backed by the real JDBC repository.
     */
    public StudentProfileService() {
        this(new JdbcStudentProfileRepository());
    }

    /**
     * Dependency-injection constructor for unit testing.
     *
     * @param profileRepository the profile repository implementation
     */
    public StudentProfileService(StudentProfileRepository profileRepository) {
        this.profileRepository = Objects.requireNonNull(profileRepository,
                "profileRepository must not be null");
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns the complete student profile for the currently logged-in student.
     *
     * <p>Access control is checked first: the caller must be a logged-in student.
     * The profile is loaded by the session's {@code userId}; a student cannot
     * view another student's profile through this method.
     *
     * @return {@link StudentProfileResult} — success with Student data, or failure
     *         with a user-friendly message
     */
    public StudentProfileResult viewMyProfile() {
        // 1. Authentication and role guard
        Optional<User> sessionUser = UserSession.getInstance().getCurrentUser();
        if (sessionUser.isEmpty()) {
            return StudentProfileResult.failure(
                    "Access denied: You must be logged in to view your profile.");
        }
        User user = sessionUser.get();
        if (user.getRole() != Role.STUDENT) {
            return StudentProfileResult.failure(
                    "Access denied: Only students can access student profiles.");
        }

        // 2. Load from DB using session userId (ownership guaranteed)
        try {
            Optional<Student> studentOpt = profileRepository.findProfileByUserId(user.getId());
            if (studentOpt.isEmpty()) {
                return StudentProfileResult.failure(
                        "Profile not found. Your account may not have a student profile yet. " +
                        "Please contact support.");
            }
            return StudentProfileResult.success("Profile loaded successfully.", studentOpt.get());
        } catch (SQLException e) {
            return StudentProfileResult.failure(
                    "Database error while loading your profile: " + e.getMessage());
        }
    }

    /**
     * Creates a new profile for the currently logged-in student.
     *
     * <p>If the student already has a profile, this returns a failure suggesting
     * to update instead.
     *
     * @param request the profile creation data
     * @return {@link StudentProfileResult} indicating success or failure
     */
    public StudentProfileResult createMyProfile(StudentProfileRequest request) {
        if (request == null) {
            return StudentProfileResult.failure("Profile request must not be null.");
        }

        // 1. Authentication and role guard
        Optional<User> sessionUser = UserSession.getInstance().getCurrentUser();
        if (sessionUser.isEmpty()) {
            return StudentProfileResult.failure(
                    "Access denied: You must be logged in to create your profile.");
        }
        User user = sessionUser.get();
        if (user.getRole() != Role.STUDENT) {
            return StudentProfileResult.failure(
                    "Access denied: Only students can create student profiles.");
        }

        // 2. Validation
        List<String> errors = validateProfileRequest(request);
        if (!errors.isEmpty()) {
            return StudentProfileResult.failure("Profile creation validation failed: " +
                    String.join("; ", errors));
        }

        // 3. Verify that a profile does not already exist
        try {
            if (profileRepository.profileExistsForUser(user.getId())) {
                return StudentProfileResult.failure(
                        "A profile already exists for your account. Please update it instead.");
            }
        } catch (SQLException e) {
            return StudentProfileResult.failure(
                    "Database error while checking profile status: " + e.getMessage());
        }

        // 4. Create Student entity and populate
        Student student = new Student();
        student.setId(user.getId());
        student.setEmail(user.getEmail());
        student.setFullName(user.getFullName());
        student.setRole(Role.STUDENT);
        student.setPhone(user.getPhone());
        student.setActive(user.isActive());
        applyRequestToStudent(request, student);

        // 5. Persist
        try {
            boolean created = profileRepository.createProfile(student);
            if (!created) {
                return StudentProfileResult.failure("Could not create profile. Please try again.");
            }
            return StudentProfileResult.success(
                    "Your profile has been created successfully.", student);
        } catch (SQLException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("duplicate") || msg.contains("unique")) {
                if (msg.contains("enrollment_number")) {
                    return StudentProfileResult.failure(
                            "Enrollment number '" + request.getEnrollmentNumber() +
                            "' is already registered to another account.");
                }
                return StudentProfileResult.failure(
                        "A record with these unique details already exists.");
            }
            return StudentProfileResult.failure(
                    "Database error while creating your profile: " + e.getMessage());
        }
    }

    /**
     * Updates the profile of the currently logged-in student using the provided request.
     *
     * <p>Validates all input fields before persisting. A student may only update their
     * own profile — the {@code userId} is always taken from the active session.
     *
     * @param request the profile update data from the UI
     * @return {@link StudentProfileResult} — success with updated Student, or failure
     *         with a user-friendly validation or database error message
     */
    public StudentProfileResult updateMyProfile(StudentProfileRequest request) {
        if (request == null) {
            return StudentProfileResult.failure("Profile update request must not be null.");
        }

        // 1. Authentication and role guard
        Optional<User> sessionUser = UserSession.getInstance().getCurrentUser();
        if (sessionUser.isEmpty()) {
            return StudentProfileResult.failure(
                    "Access denied: You must be logged in to update your profile.");
        }
        User user = sessionUser.get();
        if (user.getRole() != Role.STUDENT) {
            return StudentProfileResult.failure(
                    "Access denied: Only students can update student profiles.");
        }

        // 2. Validation
        List<String> errors = validateProfileRequest(request);
        if (!errors.isEmpty()) {
            return StudentProfileResult.failure("Profile update validation failed: " +
                    String.join("; ", errors));
        }

        // 3. Load existing profile to verify existence (and to carry over unset fields)
        Student existing;
        try {
            Optional<Student> existingOpt = profileRepository.findProfileByUserId(user.getId());
            if (existingOpt.isEmpty()) {
                return StudentProfileResult.failure(
                        "Profile not found. Cannot update a profile that does not exist.");
            }
            existing = existingOpt.get();
        } catch (SQLException e) {
            return StudentProfileResult.failure(
                    "Database error while loading your profile: " + e.getMessage());
        }

        // 4. Apply validated request fields onto the existing model
        applyRequestToStudent(request, existing);

        // 5. Persist
        try {
            boolean updated = profileRepository.updateProfile(existing);
            if (!updated) {
                return StudentProfileResult.failure(
                        "Profile could not be updated. No matching profile row was found.");
            }
            return StudentProfileResult.success(
                    "Your profile has been updated successfully.", existing);
        } catch (SQLException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("duplicate") || msg.contains("unique")) {
                if (msg.contains("enrollment_number")) {
                    return StudentProfileResult.failure(
                            "Enrollment number '" + request.getEnrollmentNumber() +
                            "' is already registered to another account.");
                }
                return StudentProfileResult.failure(
                        "A record with these unique details already exists.");
            }
            return StudentProfileResult.failure(
                    "Database error while updating your profile: " + e.getMessage());
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────

    /**
     * Validates a {@link StudentProfileRequest}.
     * Reuses {@link ValidationUtil} for graduation year and semester, consistent
     * with registration validation.
     *
     * @param request the request to validate
     * @return list of human-readable error messages; empty means valid
     */
    public List<String> validateProfileRequest(StudentProfileRequest request) {
        List<String> errors = new ArrayList<>();

        // Department (required)
        if (request.getDepartment() == null || request.getDepartment().isBlank()) {
            errors.add("Department is required.");
        } else if (request.getDepartment().trim().length() > 100) {
            errors.add("Department cannot exceed 100 characters.");
        }

        // Graduation year (required)
        if (request.getGraduationYear() == null) {
            errors.add("Graduation year is required.");
        } else if (!ValidationUtil.isValidGraduationYear(request.getGraduationYear())) {
            errors.add("Graduation year must be between " +
                    Student.MIN_GRAD_YEAR + " and " + Student.MAX_GRAD_YEAR + ".");
        }

        // Degree (optional, length check)
        if (request.getDegree() != null && !request.getDegree().isBlank()
                && request.getDegree().trim().length() > 100) {
            errors.add("Degree cannot exceed 100 characters.");
        }

        // Semester (optional, range check)
        if (request.getCurrentSemester() != null &&
                !ValidationUtil.isValidSemester(request.getCurrentSemester())) {
            errors.add("Current semester must be between " +
                    Student.MIN_SEMESTER + " and " + Student.MAX_SEMESTER + ".");
        }

        // Enrollment number (optional, length check)
        if (request.getEnrollmentNumber() != null &&
                !request.getEnrollmentNumber().isBlank() &&
                request.getEnrollmentNumber().trim().length() > 50) {
            errors.add("Enrollment number cannot exceed 50 characters.");
        }

        // LinkedIn URL (optional, length check)
        if (request.getLinkedinUrl() != null &&
                !request.getLinkedinUrl().isBlank() &&
                request.getLinkedinUrl().trim().length() > 255) {
            errors.add("LinkedIn URL cannot exceed 255 characters.");
        }

        // GitHub URL (optional, length check)
        if (request.getGithubUrl() != null &&
                !request.getGithubUrl().isBlank() &&
                request.getGithubUrl().trim().length() > 255) {
            errors.add("GitHub URL cannot exceed 255 characters.");
        }

        return errors;
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Copies validated request data onto the existing Student model before persistence.
     * Required fields always overwrite. Optional fields only overwrite if non-blank in
     * the request; a blank/null optional means "leave unchanged" is not enforced at
     * this layer (the UI prompts with current values), but null/blank still clears.
     */
    private void applyRequestToStudent(StudentProfileRequest request, Student student) {
        student.setDepartment(request.getDepartment().trim());
        student.setGraduationYear(request.getGraduationYear());

        if (request.getDegree() != null && !request.getDegree().isBlank()) {
            student.setDegree(request.getDegree().trim());
        }
        if (request.getCurrentSemester() != null) {
            student.setCurrentSemester(request.getCurrentSemester());
        }

        // Nullable/clearable fields
        student.setEnrollmentNumber(
                request.getEnrollmentNumber() != null && !request.getEnrollmentNumber().isBlank()
                        ? request.getEnrollmentNumber().trim() : null);
        student.setBio(
                request.getBio() != null && !request.getBio().isBlank()
                        ? request.getBio().trim() : null);
        student.setSkills(
                request.getSkills() != null && !request.getSkills().isBlank()
                        ? request.getSkills().trim() : null);
        student.setInterests(
                request.getInterests() != null && !request.getInterests().isBlank()
                        ? request.getInterests().trim() : null);
        student.setLinkedinUrl(
                request.getLinkedinUrl() != null && !request.getLinkedinUrl().isBlank()
                        ? request.getLinkedinUrl().trim() : null);
        student.setGithubUrl(
                request.getGithubUrl() != null && !request.getGithubUrl().isBlank()
                        ? request.getGithubUrl().trim() : null);
    }
}
