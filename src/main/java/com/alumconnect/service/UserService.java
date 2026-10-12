package com.alumconnect.service;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.Role;
import com.alumconnect.model.Student;
import com.alumconnect.model.User;
import com.alumconnect.repository.JdbcUserRepository;
import com.alumconnect.repository.UserRepository;
import com.alumconnect.service.dto.RegistrationRequest;
import com.alumconnect.service.dto.RegistrationResult;
import com.alumconnect.util.PasswordUtil;
import com.alumconnect.util.ValidationUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service orchestrating user registration, validation, password security,
 * and profile management for Student and Alumni accounts.
 */
public class UserService {

    private final UserRepository userRepository;

    /**
     * Default constructor creating a UserService backed by {@link JdbcUserRepository}.
     */
    public UserService() {
        this(new JdbcUserRepository());
    }

    /**
     * Constructor allowing dependency injection of {@link UserRepository} (e.g. for unit testing).
     *
     * @param userRepository the user repository implementation
     */
    public UserService(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
    }

    /**
     * Registers a new platform user as either a {@link Student} or {@link Alumni}.
     *
     * <p>Execution lifecycle:
     * <ol>
     *   <li>Validates all required fields, email format, and password security rules.</li>
     *   <li>Checks for duplicate email addresses.</li>
     *   <li>Hashes the plaintext password using salted PBKDF2.</li>
     *   <li>Maps request data into the appropriate model entity.</li>
     *   <li>Persists both authentication and profile records within an atomic transaction.</li>
     *   <li>Returns a descriptive {@link RegistrationResult} with success or failure details.</li>
     * </ol>
     *
     * @param request the registration details
     * @return {@link RegistrationResult} indicating the outcome
     */
    public RegistrationResult register(RegistrationRequest request) {
        if (request == null) {
            return RegistrationResult.failure("Registration request must not be null.");
        }

        // 1. Validation
        List<String> validationErrors = validateRegistrationRequest(request);
        if (!validationErrors.isEmpty()) {
            return RegistrationResult.failure("Registration validation failed.", validationErrors);
        }

        // 2. Duplicate email check
        try {
            if (userRepository.existsByEmail(request.getEmail())) {
                return RegistrationResult.failure("An account with email '" + request.getEmail().trim() + "' already exists.");
            }
        } catch (SQLException e) {
            return RegistrationResult.failure("Database error while checking email availability: " + e.getMessage());
        }

        // 3. Password hashing
        String passwordHash = PasswordUtil.hashPassword(request.getPassword());

        // 4. Persistence based on role
        try {
            if (request.getRole() == Role.STUDENT) {
                Student student = buildStudent(request, passwordHash);
                Student savedStudent = userRepository.saveStudent(student);
                return RegistrationResult.success(
                        "Registration successful! Welcome to AlumConnect, " + savedStudent.getFullName() + " (Student).",
                        savedStudent
                );
            } else if (request.getRole() == Role.ALUMNI) {
                Alumni alumni = buildAlumni(request, passwordHash);
                Alumni savedAlumni = userRepository.saveAlumni(alumni);
                return RegistrationResult.success(
                        "Registration successful! Welcome to AlumConnect, " + savedAlumni.getFullName() + " (Alumni).",
                        savedAlumni
                );
            } else {
                return RegistrationResult.failure("Registration is only supported for STUDENT and ALUMNI roles.");
            }
        } catch (SQLException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("duplicate") || msg.contains("unique")) {
                if (msg.contains("email")) {
                    return RegistrationResult.failure("An account with email '" + request.getEmail().trim() + "' already exists.");
                } else if (msg.contains("enrollment_number")) {
                    return RegistrationResult.failure("Enrollment number '" + request.getEnrollmentNumber() + "' is already registered.");
                }
                return RegistrationResult.failure("A record with these unique details already exists.");
            }
            return RegistrationResult.failure("Database error during registration: " + e.getMessage());
        } catch (Exception e) {
            return RegistrationResult.failure("Unexpected error during registration: " + e.getMessage());
        }
    }

    /**
     * Checks if an email is available for registration.
     *
     * @param email email to check
     * @return {@code true} if email is available; {@code false} if already taken or DB error
     */
    public boolean isEmailAvailable(String email) {
        if (!ValidationUtil.isValidEmail(email)) {
            return false;
        }
        try {
            return !userRepository.existsByEmail(email);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Finds a user by ID.
     */
    public Optional<User> getUserById(int id) {
        try {
            return userRepository.findById(id);
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

    /**
     * Finds a user by email.
     */
    public Optional<User> getUserByEmail(String email) {
        try {
            return userRepository.findByEmail(email);
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

    // ── Validation Helpers ───────────────────────────────────────────────────

    public List<String> validateRegistrationRequest(RegistrationRequest request) {
        List<String> errors = new ArrayList<>();

        // Role validation
        if (request.getRole() == null) {
            errors.add("Role selection is required (STUDENT or ALUMNI).");
        } else if (request.getRole() != Role.STUDENT && request.getRole() != Role.ALUMNI) {
            errors.add("Registration is only permitted for STUDENT and ALUMNI roles.");
        }

        // Full Name validation
        if (request.getFullName() == null || request.getFullName().isBlank()) {
            errors.add("Full name is required.");
        } else if (request.getFullName().trim().length() > 100) {
            errors.add("Full name cannot exceed 100 characters.");
        }

        // Email validation
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            errors.add("Email is required.");
        } else if (!ValidationUtil.isValidEmail(request.getEmail())) {
            errors.add("Invalid email format (e.g. user@example.com).");
        }

        // Password validation
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            errors.add("Password is required.");
        } else {
            List<String> pwdErrors = PasswordUtil.getPasswordRequirementErrors(request.getPassword());
            errors.addAll(pwdErrors);

            if (request.getConfirmPassword() != null && !request.getPassword().equals(request.getConfirmPassword())) {
                errors.add("Passwords do not match.");
            }
        }

        // Phone validation (optional)
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            if (!ValidationUtil.isValidPhone(request.getPhone())) {
                errors.add("Phone number format is invalid (allowed: digits, spaces, hyphens, +, max 20 chars).");
            }
        }

        // Academic Department validation
        if (request.getDepartment() == null || request.getDepartment().isBlank()) {
            errors.add("Department is required.");
        } else if (request.getDepartment().trim().length() > 100) {
            errors.add("Department cannot exceed 100 characters.");
        }

        // Graduation Year validation
        if (request.getGraduationYear() == null) {
            errors.add("Graduation year is required.");
        } else if (!ValidationUtil.isValidGraduationYear(request.getGraduationYear())) {
            errors.add("Graduation year must be between " + Student.MIN_GRAD_YEAR + " and " + Student.MAX_GRAD_YEAR + ".");
        }

        // Degree length check
        if (request.getDegree() != null && request.getDegree().trim().length() > 100) {
            errors.add("Degree cannot exceed 100 characters.");
        }

        // Role-specific validation
        if (request.getRole() == Role.STUDENT) {
            validateStudentFields(request, errors);
        } else if (request.getRole() == Role.ALUMNI) {
            validateAlumniFields(request, errors);
        }

        return errors;
    }

    private void validateStudentFields(RegistrationRequest request, List<String> errors) {
        if (request.getCurrentSemester() != null && !ValidationUtil.isValidSemester(request.getCurrentSemester())) {
            errors.add("Current semester must be between " + Student.MIN_SEMESTER + " and " + Student.MAX_SEMESTER + ".");
        }
        if (request.getEnrollmentNumber() != null && request.getEnrollmentNumber().trim().length() > 50) {
            errors.add("Enrollment number cannot exceed 50 characters.");
        }
        if (request.getLinkedinUrl() != null && request.getLinkedinUrl().trim().length() > 255) {
            errors.add("LinkedIn URL cannot exceed 255 characters.");
        }
        if (request.getGithubUrl() != null && request.getGithubUrl().trim().length() > 255) {
            errors.add("GitHub URL cannot exceed 255 characters.");
        }
    }

    private void validateAlumniFields(RegistrationRequest request, List<String> errors) {
        if (request.getYearsOfExperience() != null && request.getYearsOfExperience() < 0) {
            errors.add("Years of experience cannot be negative.");
        }
        if (request.getMaxMentees() != null && request.getMaxMentees() < 0) {
            errors.add("Max mentees cannot be negative.");
        }
        if (request.getCurrentCompany() != null && request.getCurrentCompany().trim().length() > 150) {
            errors.add("Current company cannot exceed 150 characters.");
        }
        if (request.getCurrentRole() != null && request.getCurrentRole().trim().length() > 100) {
            errors.add("Current role cannot exceed 100 characters.");
        }
        if (request.getIndustry() != null && request.getIndustry().trim().length() > 100) {
            errors.add("Industry cannot exceed 100 characters.");
        }
        if (request.getLinkedinUrl() != null && request.getLinkedinUrl().trim().length() > 255) {
            errors.add("LinkedIn URL cannot exceed 255 characters.");
        }
    }

    // ── Model Builders ───────────────────────────────────────────────────────

    private Student buildStudent(RegistrationRequest request, String passwordHash) {
        Student student = new Student(
                request.getEmail().trim().toLowerCase(),
                passwordHash,
                request.getFullName().trim(),
                request.getPhone() != null && !request.getPhone().isBlank() ? request.getPhone().trim() : null,
                request.getDepartment().trim(),
                request.getGraduationYear()
        );

        if (request.getDegree() != null && !request.getDegree().isBlank()) {
            student.setDegree(request.getDegree().trim());
        }
        if (request.getCurrentSemester() != null) {
            student.setCurrentSemester(request.getCurrentSemester());
        }
        if (request.getEnrollmentNumber() != null && !request.getEnrollmentNumber().isBlank()) {
            student.setEnrollmentNumber(request.getEnrollmentNumber().trim());
        }
        if (request.getBio() != null && !request.getBio().isBlank()) {
            student.setBio(request.getBio().trim());
        }
        if (request.getSkills() != null && !request.getSkills().isBlank()) {
            student.setSkills(request.getSkills().trim());
        }
        if (request.getInterests() != null && !request.getInterests().isBlank()) {
            student.setInterests(request.getInterests().trim());
        }
        if (request.getLinkedinUrl() != null && !request.getLinkedinUrl().isBlank()) {
            student.setLinkedinUrl(request.getLinkedinUrl().trim());
        }
        if (request.getGithubUrl() != null && !request.getGithubUrl().isBlank()) {
            student.setGithubUrl(request.getGithubUrl().trim());
        }

        return student;
    }

    private Alumni buildAlumni(RegistrationRequest request, String passwordHash) {
        Alumni alumni = new Alumni(
                request.getEmail().trim().toLowerCase(),
                passwordHash,
                request.getFullName().trim(),
                request.getPhone() != null && !request.getPhone().isBlank() ? request.getPhone().trim() : null,
                request.getDepartment().trim(),
                request.getGraduationYear()
        );

        if (request.getDegree() != null && !request.getDegree().isBlank()) {
            alumni.setDegree(request.getDegree().trim());
        }
        if (request.getCurrentCompany() != null && !request.getCurrentCompany().isBlank()) {
            alumni.setCurrentCompany(request.getCurrentCompany().trim());
        }
        if (request.getCurrentRole() != null && !request.getCurrentRole().isBlank()) {
            alumni.setCurrentRole(request.getCurrentRole().trim());
        }
        if (request.getIndustry() != null && !request.getIndustry().isBlank()) {
            alumni.setIndustry(request.getIndustry().trim());
        }
        if (request.getYearsOfExperience() != null) {
            alumni.setYearsOfExperience(request.getYearsOfExperience());
        }
        if (request.getExpertiseAreas() != null && !request.getExpertiseAreas().isBlank()) {
            alumni.setExpertiseAreas(request.getExpertiseAreas().trim());
        }
        if (request.getBio() != null && !request.getBio().isBlank()) {
            alumni.setBio(request.getBio().trim());
        }
        if (request.getLinkedinUrl() != null && !request.getLinkedinUrl().isBlank()) {
            alumni.setLinkedinUrl(request.getLinkedinUrl().trim());
        }
        if (request.getAvailableForMentorship() != null) {
            alumni.setAvailableForMentorship(request.getAvailableForMentorship());
        }
        if (request.getMaxMentees() != null) {
            alumni.setMaxMentees(request.getMaxMentees());
        }

        return alumni;
    }
}
