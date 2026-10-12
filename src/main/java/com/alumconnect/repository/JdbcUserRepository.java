package com.alumconnect.repository;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.Role;
import com.alumconnect.model.Student;
import com.alumconnect.model.User;
import com.alumconnect.util.ConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * JDBC implementation of {@link UserRepository} using parameterized queries
 * and transaction management over connection-pooled connections.
 */
public class JdbcUserRepository implements UserRepository {

    @FunctionalInterface
    public interface ConnectionSupplier {
        Connection get() throws SQLException;
    }

    private final ConnectionSupplier connectionSupplier;

    /**
     * Default constructor using the shared {@link ConnectionPool}.
     */
    public JdbcUserRepository() {
        this(() -> ConnectionPool.getInstance().acquire());
    }

    /**
     * Constructor allowing custom connection supply (e.g. for testing).
     *
     * @param connectionSupplier provider of JDBC connections
     */
    public JdbcUserRepository(ConnectionSupplier connectionSupplier) {
        this.connectionSupplier = connectionSupplier;
    }

    @Override
    public boolean existsByEmail(String email) throws SQLException {
        if (email == null || email.isBlank()) {
            return false;
        }
        final String sql = "SELECT 1 FROM users WHERE LOWER(email) = LOWER(?) LIMIT 1";
        try (Connection conn = connectionSupplier.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public Optional<User> findById(int id) throws SQLException {
        final String sql = "SELECT id, email, password_hash, full_name, role, phone, is_active, created_at, updated_at " +
                "FROM users WHERE id = ?";
        try (Connection conn = connectionSupplier.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String roleStr = rs.getString("role");
                    Role role = Role.fromString(roleStr);
                    if (role == Role.STUDENT) {
                        return findStudentById(id).map(s -> (User) s);
                    } else if (role == Role.ALUMNI) {
                        return findAlumniById(id).map(a -> (User) a);
                    }
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByEmail(String email) throws SQLException {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        final String sql = "SELECT id, role FROM users WHERE LOWER(email) = LOWER(?) LIMIT 1";
        try (Connection conn = connectionSupplier.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    String roleStr = rs.getString("role");
                    Role role = Role.fromString(roleStr);
                    if (role == Role.STUDENT) {
                        return findStudentById(id).map(s -> (User) s);
                    } else if (role == Role.ALUMNI) {
                        return findAlumniById(id).map(a -> (User) a);
                    }
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Student> findStudentById(int userId) throws SQLException {
        final String sql = "SELECT u.id AS u_id, u.email, u.password_hash, u.full_name, u.role, u.phone, u.is_active, " +
                "u.created_at AS u_created_at, u.updated_at AS u_updated_at, " +
                "sp.id AS sp_id, sp.enrollment_number, sp.degree, sp.department, sp.graduation_year, " +
                "sp.current_semester, sp.bio, sp.skills, sp.interests, sp.linkedin_url, sp.github_url " +
                "FROM users u " +
                "INNER JOIN student_profiles sp ON u.id = sp.user_id " +
                "WHERE u.id = ?";

        try (Connection conn = connectionSupplier.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Student student = new Student();
                    student.setId(rs.getInt("u_id"));
                    student.setEmail(rs.getString("email"));
                    student.setPasswordHash(rs.getString("password_hash"));
                    student.setFullName(rs.getString("full_name"));
                    student.setRole(Role.STUDENT);
                    student.setPhone(rs.getString("phone"));
                    student.setActive(rs.getBoolean("is_active"));

                    Timestamp createdTs = rs.getTimestamp("u_created_at");
                    if (createdTs != null) student.setCreatedAt(createdTs.toLocalDateTime());
                    Timestamp updatedTs = rs.getTimestamp("u_updated_at");
                    if (updatedTs != null) student.setUpdatedAt(updatedTs.toLocalDateTime());

                    student.setProfileId(rs.getInt("sp_id"));
                    student.setEnrollmentNumber(rs.getString("enrollment_number"));
                    String degree = rs.getString("degree");
                    if (degree != null) student.setDegree(degree);
                    student.setDepartment(rs.getString("department"));
                    student.setGraduationYear(rs.getInt("graduation_year"));
                    student.setCurrentSemester(rs.getInt("current_semester"));
                    student.setBio(rs.getString("bio"));
                    student.setSkills(rs.getString("skills"));
                    student.setInterests(rs.getString("interests"));
                    student.setLinkedinUrl(rs.getString("linkedin_url"));
                    student.setGithubUrl(rs.getString("github_url"));

                    return Optional.of(student);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Alumni> findAlumniById(int userId) throws SQLException {
        final String sql = "SELECT u.id AS u_id, u.email, u.password_hash, u.full_name, u.role, u.phone, u.is_active, " +
                "u.created_at AS u_created_at, u.updated_at AS u_updated_at, " +
                "ap.id AS ap_id, ap.graduation_year, ap.degree, ap.department, ap.current_company, " +
                "ap.current_role, ap.industry, ap.years_of_experience, ap.expertise_areas, ap.bio, " +
                "ap.linkedin_url, ap.is_available_for_mentorship, ap.max_mentees " +
                "FROM users u " +
                "INNER JOIN alumni_profiles ap ON u.id = ap.user_id " +
                "WHERE u.id = ?";

        try (Connection conn = connectionSupplier.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Alumni alumni = new Alumni();
                    alumni.setId(rs.getInt("u_id"));
                    alumni.setEmail(rs.getString("email"));
                    alumni.setPasswordHash(rs.getString("password_hash"));
                    alumni.setFullName(rs.getString("full_name"));
                    alumni.setRole(Role.ALUMNI);
                    alumni.setPhone(rs.getString("phone"));
                    alumni.setActive(rs.getBoolean("is_active"));

                    Timestamp createdTs = rs.getTimestamp("u_created_at");
                    if (createdTs != null) alumni.setCreatedAt(createdTs.toLocalDateTime());
                    Timestamp updatedTs = rs.getTimestamp("u_updated_at");
                    if (updatedTs != null) alumni.setUpdatedAt(updatedTs.toLocalDateTime());

                    alumni.setProfileId(rs.getInt("ap_id"));
                    alumni.setGraduationYear(rs.getInt("graduation_year"));
                    String degree = rs.getString("degree");
                    if (degree != null) alumni.setDegree(degree);
                    alumni.setDepartment(rs.getString("department"));
                    alumni.setCurrentCompany(rs.getString("current_company"));
                    alumni.setCurrentRole(rs.getString("current_role"));
                    alumni.setIndustry(rs.getString("industry"));
                    alumni.setYearsOfExperience(rs.getInt("years_of_experience"));
                    alumni.setExpertiseAreas(rs.getString("expertise_areas"));
                    alumni.setBio(rs.getString("bio"));
                    alumni.setLinkedinUrl(rs.getString("linkedin_url"));
                    alumni.setAvailableForMentorship(rs.getBoolean("is_available_for_mentorship"));
                    alumni.setMaxMentees(rs.getInt("max_mentees"));

                    return Optional.of(alumni);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Student saveStudent(Student student) throws SQLException {
        final String insertUserSql = "INSERT INTO users (email, password_hash, full_name, role, phone, is_active) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        final String insertProfileSql = "INSERT INTO student_profiles " +
                "(user_id, enrollment_number, degree, department, graduation_year, current_semester, bio, skills, interests, linkedin_url, github_url) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = connectionSupplier.get()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);

                int userId;
                try (PreparedStatement psUser = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {
                    psUser.setString(1, student.getEmail().trim().toLowerCase());
                    psUser.setString(2, student.getPasswordHash());
                    psUser.setString(3, student.getFullName().trim());
                    psUser.setString(4, Role.STUDENT.name());
                    psUser.setString(5, student.getPhone());
                    psUser.setBoolean(6, student.isActive());

                    psUser.executeUpdate();
                    try (ResultSet rs = psUser.getGeneratedKeys()) {
                        if (rs.next()) {
                            userId = rs.getInt(1);
                            student.setId(userId);
                        } else {
                            throw new SQLException("Failed to retrieve generated user ID for Student");
                        }
                    }
                }

                int profileId;
                try (PreparedStatement psProfile = conn.prepareStatement(insertProfileSql, Statement.RETURN_GENERATED_KEYS)) {
                    psProfile.setInt(1, userId);
                    psProfile.setString(2, student.getEnrollmentNumber());
                    psProfile.setString(3, student.getDegree() != null ? student.getDegree() : Student.DEFAULT_DEGREE);
                    psProfile.setString(4, student.getDepartment());
                    psProfile.setInt(5, student.getGraduationYear());
                    psProfile.setInt(6, student.getCurrentSemester());
                    psProfile.setString(7, student.getBio());
                    psProfile.setString(8, student.getSkills());
                    psProfile.setString(9, student.getInterests());
                    psProfile.setString(10, student.getLinkedinUrl());
                    psProfile.setString(11, student.getGithubUrl());

                    psProfile.executeUpdate();
                    try (ResultSet rs = psProfile.getGeneratedKeys()) {
                        if (rs.next()) {
                            profileId = rs.getInt(1);
                            student.setProfileId(profileId);
                        } else {
                            throw new SQLException("Failed to retrieve generated student_profiles ID");
                        }
                    }
                }

                conn.commit();
                LocalDateTime now = LocalDateTime.now();
                student.setCreatedAt(now);
                student.setUpdatedAt(now);
                return student;
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
                throw e;
            } finally {
                try {
                    conn.setAutoCommit(originalAutoCommit);
                } catch (SQLException ignored) {
                }
            }
        }
    }

    @Override
    public Alumni saveAlumni(Alumni alumni) throws SQLException {
        final String insertUserSql = "INSERT INTO users (email, password_hash, full_name, role, phone, is_active) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        final String insertProfileSql = "INSERT INTO alumni_profiles " +
                "(user_id, graduation_year, degree, department, current_company, current_role, industry, years_of_experience, expertise_areas, bio, linkedin_url, is_available_for_mentorship, max_mentees) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = connectionSupplier.get()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);

                int userId;
                try (PreparedStatement psUser = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {
                    psUser.setString(1, alumni.getEmail().trim().toLowerCase());
                    psUser.setString(2, alumni.getPasswordHash());
                    psUser.setString(3, alumni.getFullName().trim());
                    psUser.setString(4, Role.ALUMNI.name());
                    psUser.setString(5, alumni.getPhone());
                    psUser.setBoolean(6, alumni.isActive());

                    psUser.executeUpdate();
                    try (ResultSet rs = psUser.getGeneratedKeys()) {
                        if (rs.next()) {
                            userId = rs.getInt(1);
                            alumni.setId(userId);
                        } else {
                            throw new SQLException("Failed to retrieve generated user ID for Alumni");
                        }
                    }
                }

                int profileId;
                try (PreparedStatement psProfile = conn.prepareStatement(insertProfileSql, Statement.RETURN_GENERATED_KEYS)) {
                    psProfile.setInt(1, userId);
                    psProfile.setInt(2, alumni.getGraduationYear());
                    psProfile.setString(3, alumni.getDegree() != null ? alumni.getDegree() : Alumni.DEFAULT_DEGREE);
                    psProfile.setString(4, alumni.getDepartment());
                    psProfile.setString(5, alumni.getCurrentCompany());
                    psProfile.setString(6, alumni.getCurrentRole());
                    psProfile.setString(7, alumni.getIndustry());
                    psProfile.setInt(8, alumni.getYearsOfExperience());
                    psProfile.setString(9, alumni.getExpertiseAreas());
                    psProfile.setString(10, alumni.getBio());
                    psProfile.setString(11, alumni.getLinkedinUrl());
                    psProfile.setBoolean(12, alumni.isAvailableForMentorship());
                    psProfile.setInt(13, alumni.getMaxMentees());

                    psProfile.executeUpdate();
                    try (ResultSet rs = psProfile.getGeneratedKeys()) {
                        if (rs.next()) {
                            profileId = rs.getInt(1);
                            alumni.setProfileId(profileId);
                        } else {
                            throw new SQLException("Failed to retrieve generated alumni_profiles ID");
                        }
                    }
                }

                conn.commit();
                LocalDateTime now = LocalDateTime.now();
                alumni.setCreatedAt(now);
                alumni.setUpdatedAt(now);
                return alumni;
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
                throw e;
            } finally {
                try {
                    conn.setAutoCommit(originalAutoCommit);
                } catch (SQLException ignored) {
                }
            }
        }
    }

    @Override
    public boolean deleteUserById(int userId) throws SQLException {
        final String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = connectionSupplier.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        }
    }
}
