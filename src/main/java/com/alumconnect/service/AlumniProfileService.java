package com.alumconnect.service;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.Role;
import com.alumconnect.model.User;
import com.alumconnect.repository.AlumniProfileRepository;
import com.alumconnect.repository.JdbcAlumniProfileRepository;
import com.alumconnect.service.dto.AlumniProfileRequest;
import com.alumconnect.service.dto.AlumniProfileResult;
import com.alumconnect.util.UserSession;
import com.alumconnect.util.ValidationUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service handling alumni professional profile viewing, creation, and editing operations.
 *
 * <p>Enforces the following security invariants:
 * <ul>
 *   <li>Only a logged-in user with role {@code ALUMNI} may call these operations.</li>
 *   <li>An alumni may only view, create, or modify their own profile — the {@code userId} used
 *       in every repository call is always taken from the active session, never from
 *       untrusted caller-supplied input.</li>
 * </ul>
 *
 * <p>Validation reuses {@link ValidationUtil} for graduation year range and validates
 * experience, max mentees, and field lengths.
 */
public class AlumniProfileService {

    private final AlumniProfileRepository profileRepository;

    /**
     * Default constructor backed by the real JDBC repository.
     */
    public AlumniProfileService() {
        this(new JdbcAlumniProfileRepository());
    }

    /**
     * Dependency-injection constructor for unit testing.
     *
     * @param profileRepository the profile repository implementation
     */
    public AlumniProfileService(AlumniProfileRepository profileRepository) {
        this.profileRepository = Objects.requireNonNull(profileRepository,
                "profileRepository must not be null");
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns the complete alumni profile for the currently logged-in alumni.
     *
     * <p>Access control is checked first: the caller must be a logged-in alumni.
     * The profile is loaded by the session's {@code userId}; an alumni cannot
     * view another alumni's profile through this method.
     *
     * @return {@link AlumniProfileResult} — success with Alumni data, or failure
     *         with a user-friendly message
     */
    public AlumniProfileResult viewMyProfile() {
        // 1. Authentication and role guard
        Optional<User> sessionUser = UserSession.getInstance().getCurrentUser();
        if (sessionUser.isEmpty()) {
            return AlumniProfileResult.failure(
                    "Access denied: You must be logged in to view your profile.");
        }
        User user = sessionUser.get();
        if (user.getRole() != Role.ALUMNI) {
            return AlumniProfileResult.failure(
                    "Access denied: Only alumni can access alumni profiles.");
        }

        // 2. Load from DB using session userId (ownership guaranteed)
        try {
            Optional<Alumni> alumniOpt = profileRepository.findProfileByUserId(user.getId());
            if (alumniOpt.isEmpty()) {
                return AlumniProfileResult.failure(
                        "Profile not found. Your account may not have an alumni profile yet. " +
                        "Please contact support.");
            }
            return AlumniProfileResult.success("Profile loaded successfully.", alumniOpt.get());
        } catch (SQLException e) {
            return AlumniProfileResult.failure(
                    "Database error while loading your profile: " + e.getMessage());
        }
    }

    /**
     * Creates a new profile for the currently logged-in alumni.
     *
     * <p>If the alumni already has a profile, this returns a failure suggesting
     * to update instead.
     *
     * @param request the profile creation data
     * @return {@link AlumniProfileResult} indicating success or failure
     */
    public AlumniProfileResult createMyProfile(AlumniProfileRequest request) {
        if (request == null) {
            return AlumniProfileResult.failure("Profile request must not be null.");
        }

        // 1. Authentication and role guard
        Optional<User> sessionUser = UserSession.getInstance().getCurrentUser();
        if (sessionUser.isEmpty()) {
            return AlumniProfileResult.failure(
                    "Access denied: You must be logged in to create your profile.");
        }
        User user = sessionUser.get();
        if (user.getRole() != Role.ALUMNI) {
            return AlumniProfileResult.failure(
                    "Access denied: Only alumni can create alumni profiles.");
        }

        // 2. Validation
        List<String> errors = validateProfileRequest(request);
        if (!errors.isEmpty()) {
            return AlumniProfileResult.failure("Profile creation validation failed: " +
                    String.join("; ", errors));
        }

        // 3. Verify that a profile does not already exist
        try {
            if (profileRepository.profileExistsForUser(user.getId())) {
                return AlumniProfileResult.failure(
                        "A profile already exists for your account. Please update it instead.");
            }
        } catch (SQLException e) {
            return AlumniProfileResult.failure(
                    "Database error while checking profile status: " + e.getMessage());
        }

        // 4. Create Alumni entity and populate
        Alumni alumni = new Alumni();
        alumni.setId(user.getId());
        alumni.setEmail(user.getEmail());
        alumni.setFullName(user.getFullName());
        alumni.setRole(Role.ALUMNI);
        alumni.setPhone(user.getPhone());
        alumni.setActive(user.isActive());
        applyRequestToAlumni(request, alumni);

        // 5. Persist
        try {
            boolean created = profileRepository.createProfile(alumni);
            if (!created) {
                return AlumniProfileResult.failure("Could not create profile. Please try again.");
            }
            return AlumniProfileResult.success(
                    "Your profile has been created successfully.", alumni);
        } catch (SQLException e) {
            return AlumniProfileResult.failure(
                    "Database error while creating your profile: " + e.getMessage());
        }
    }

    /**
     * Updates the profile of the currently logged-in alumni using the provided request.
     *
     * <p>Validates all input fields before persisting. An alumni may only update their
     * own profile — the {@code userId} is always taken from the active session.
     *
     * @param request the profile update data from the UI
     * @return {@link AlumniProfileResult} — success with updated Alumni, or failure
     *         with a user-friendly validation or database error message
     */
    public AlumniProfileResult updateMyProfile(AlumniProfileRequest request) {
        if (request == null) {
            return AlumniProfileResult.failure("Profile update request must not be null.");
        }

        // 1. Authentication and role guard
        Optional<User> sessionUser = UserSession.getInstance().getCurrentUser();
        if (sessionUser.isEmpty()) {
            return AlumniProfileResult.failure(
                    "Access denied: You must be logged in to update your profile.");
        }
        User user = sessionUser.get();
        if (user.getRole() != Role.ALUMNI) {
            return AlumniProfileResult.failure(
                    "Access denied: Only alumni can update alumni profiles.");
        }

        // 2. Validation
        List<String> errors = validateProfileRequest(request);
        if (!errors.isEmpty()) {
            return AlumniProfileResult.failure("Profile update validation failed: " +
                    String.join("; ", errors));
        }

        // 3. Load existing profile to verify existence (and to carry over unset fields)
        Alumni existing;
        try {
            Optional<Alumni> existingOpt = profileRepository.findProfileByUserId(user.getId());
            if (existingOpt.isEmpty()) {
                return AlumniProfileResult.failure(
                        "Profile not found. Cannot update a profile that does not exist.");
            }
            existing = existingOpt.get();
        } catch (SQLException e) {
            return AlumniProfileResult.failure(
                    "Database error while loading your profile: " + e.getMessage());
        }

        // 4. Apply validated request fields onto the existing model
        applyRequestToAlumni(request, existing);

        // 5. Persist
        try {
            boolean updated = profileRepository.updateProfile(existing);
            if (!updated) {
                return AlumniProfileResult.failure(
                        "Profile could not be updated. No matching profile row was found.");
            }
            return AlumniProfileResult.success(
                    "Your profile has been updated successfully.", existing);
        } catch (SQLException e) {
            return AlumniProfileResult.failure(
                    "Database error while updating your profile: " + e.getMessage());
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────

    /**
     * Validates an {@link AlumniProfileRequest}.
     * Reuses {@link ValidationUtil} for graduation year, consistent
     * with registration and student profile validation.
     *
     * @param request the request to validate
     * @return list of human-readable error messages; empty means valid
     */
    public List<String> validateProfileRequest(AlumniProfileRequest request) {
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
                    Alumni.MIN_GRAD_YEAR + " and " + Alumni.MAX_GRAD_YEAR + ".");
        }

        // Degree (optional, length check)
        if (request.getDegree() != null && !request.getDegree().isBlank()
                && request.getDegree().trim().length() > 100) {
            errors.add("Degree cannot exceed 100 characters.");
        }

        // Current Company (optional, length check)
        if (request.getCurrentCompany() != null && !request.getCurrentCompany().isBlank()
                && request.getCurrentCompany().trim().length() > 150) {
            errors.add("Current company cannot exceed 150 characters.");
        }

        // Current Role (optional, length check)
        if (request.getCurrentRole() != null && !request.getCurrentRole().isBlank()
                && request.getCurrentRole().trim().length() > 100) {
            errors.add("Current role cannot exceed 100 characters.");
        }

        // Industry (optional, length check)
        if (request.getIndustry() != null && !request.getIndustry().isBlank()
                && request.getIndustry().trim().length() > 100) {
            errors.add("Industry cannot exceed 100 characters.");
        }

        // Years of experience (optional, non-negative)
        if (request.getYearsOfExperience() != null && request.getYearsOfExperience() < 0) {
            errors.add("Years of experience cannot be negative.");
        }

        // Max mentees (optional, non-negative)
        if (request.getMaxMentees() != null && request.getMaxMentees() < 0) {
            errors.add("Max mentees cannot be negative.");
        }

        // LinkedIn URL (optional, length check)
        if (request.getLinkedinUrl() != null && !request.getLinkedinUrl().isBlank()
                && request.getLinkedinUrl().trim().length() > 255) {
            errors.add("LinkedIn URL cannot exceed 255 characters.");
        }

        return errors;
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Copies validated request data onto the existing Alumni model before persistence.
     */
    private void applyRequestToAlumni(AlumniProfileRequest request, Alumni alumni) {
        alumni.setDepartment(request.getDepartment().trim());
        alumni.setGraduationYear(request.getGraduationYear());

        if (request.getDegree() != null && !request.getDegree().isBlank()) {
            alumni.setDegree(request.getDegree().trim());
        }
        if (request.getCurrentCompany() != null && !request.getCurrentCompany().isBlank()) {
            alumni.setCurrentCompany(request.getCurrentCompany().trim());
        } else if (request.getCurrentCompany() != null && request.getCurrentCompany().isBlank()) {
            alumni.setCurrentCompany(null);
        }

        if (request.getCurrentRole() != null && !request.getCurrentRole().isBlank()) {
            alumni.setCurrentRole(request.getCurrentRole().trim());
        } else if (request.getCurrentRole() != null && request.getCurrentRole().isBlank()) {
            alumni.setCurrentRole(null);
        }

        if (request.getIndustry() != null && !request.getIndustry().isBlank()) {
            alumni.setIndustry(request.getIndustry().trim());
        } else if (request.getIndustry() != null && request.getIndustry().isBlank()) {
            alumni.setIndustry(null);
        }

        if (request.getYearsOfExperience() != null) {
            alumni.setYearsOfExperience(request.getYearsOfExperience());
        }

        if (request.getExpertiseAreas() != null && !request.getExpertiseAreas().isBlank()) {
            alumni.setExpertiseAreas(request.getExpertiseAreas().trim());
        } else if (request.getExpertiseAreas() != null && request.getExpertiseAreas().isBlank()) {
            alumni.setExpertiseAreas(null);
        }

        if (request.getBio() != null && !request.getBio().isBlank()) {
            alumni.setBio(request.getBio().trim());
        } else if (request.getBio() != null && request.getBio().isBlank()) {
            alumni.setBio(null);
        }

        if (request.getLinkedinUrl() != null && !request.getLinkedinUrl().isBlank()) {
            alumni.setLinkedinUrl(request.getLinkedinUrl().trim());
        } else if (request.getLinkedinUrl() != null && request.getLinkedinUrl().isBlank()) {
            alumni.setLinkedinUrl(null);
        }

        if (request.getAvailableForMentorship() != null) {
            alumni.setAvailableForMentorship(request.getAvailableForMentorship());
        }

        if (request.getMaxMentees() != null) {
            alumni.setMaxMentees(request.getMaxMentees());
        }
    }
}
