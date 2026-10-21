package com.alumconnect.repository;

import com.alumconnect.model.Role;
import com.alumconnect.model.Student;
import com.alumconnect.util.ConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;

/**
 * JDBC implementation of {@link StudentProfileRepository}.
 *
 * <p>Reuses the same {@link ConnectionPool} as {@link JdbcUserRepository} so all
 * database access goes through the same connection pool configuration.
 */
public class JdbcStudentProfileRepository implements StudentProfileRepository {

    @FunctionalInterface
    public interface ConnectionSupplier {
        Connection get() throws SQLException;
    }

    private final ConnectionSupplier connectionSupplier;

    /**
     * Default constructor using the shared {@link ConnectionPool}.
     */
    public JdbcStudentProfileRepository() {
        this(() -> ConnectionPool.getInstance().acquire());
    }

    /**
     * Constructor allowing custom connection supply (e.g. for testing).
     *
     * @param connectionSupplier provider of JDBC connections
     */
    public JdbcStudentProfileRepository(ConnectionSupplier connectionSupplier) {
        this.connectionSupplier = connectionSupplier;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Executes an INNER JOIN query on {@code users} and {@code student_profiles} matching
     * {@code student_profiles.user_id = ?}. Returns empty if no profile row exists yet.
     */
    @Override
    public Optional<Student> findProfileByUserId(int userId) throws SQLException {
        final String sql =
                "SELECT u.id AS u_id, u.email, u.password_hash, u.full_name, u.role, u.phone, " +
                "u.is_active, u.created_at AS u_created_at, u.updated_at AS u_updated_at, " +
                "sp.id AS sp_id, sp.enrollment_number, sp.degree, sp.department, sp.graduation_year, " +
                "sp.current_semester, sp.bio, sp.skills, sp.interests, sp.linkedin_url, sp.github_url " +
                "FROM users u " +
                "INNER JOIN student_profiles sp ON u.id = sp.user_id " +
                "WHERE u.id = ? AND u.role = 'STUDENT'";

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

    /**
     * {@inheritDoc}
     *
     * <p>Executes an UPDATE on {@code student_profiles} WHERE {@code user_id = ?}.
     * All mutable profile columns are overwritten in one statement.
     */
    @Override
    public boolean updateProfile(Student student) throws SQLException {
        final String sql =
                "UPDATE student_profiles SET " +
                "enrollment_number = ?, degree = ?, department = ?, graduation_year = ?, " +
                "current_semester = ?, bio = ?, skills = ?, interests = ?, " +
                "linkedin_url = ?, github_url = ? " +
                "WHERE user_id = ?";

        try (Connection conn = connectionSupplier.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, student.getEnrollmentNumber());
            ps.setString(2, student.getDegree() != null ? student.getDegree() : Student.DEFAULT_DEGREE);
            ps.setString(3, student.getDepartment());
            ps.setInt(4, student.getGraduationYear());
            ps.setInt(5, student.getCurrentSemester());
            ps.setString(6, student.getBio());
            ps.setString(7, student.getSkills());
            ps.setString(8, student.getInterests());
            ps.setString(9, student.getLinkedinUrl());
            ps.setString(10, student.getGithubUrl());
            ps.setInt(11, student.getId());

            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean createProfile(Student student) throws SQLException {
        final String sql =
                "INSERT INTO student_profiles " +
                "(user_id, enrollment_number, degree, department, graduation_year, current_semester, " +
                "bio, skills, interests, linkedin_url, github_url) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = connectionSupplier.get();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, student.getId());
            ps.setString(2, student.getEnrollmentNumber());
            ps.setString(3, student.getDegree() != null ? student.getDegree() : Student.DEFAULT_DEGREE);
            ps.setString(4, student.getDepartment());
            ps.setInt(5, student.getGraduationYear());
            ps.setInt(6, student.getCurrentSemester());
            ps.setString(7, student.getBio());
            ps.setString(8, student.getSkills());
            ps.setString(9, student.getInterests());
            ps.setString(10, student.getLinkedinUrl());
            ps.setString(11, student.getGithubUrl());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        student.setProfileId(rs.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Uses a lightweight {@code SELECT 1} existence query against {@code student_profiles}.
     */
    @Override
    public boolean profileExistsForUser(int userId) throws SQLException {
        final String sql = "SELECT 1 FROM student_profiles WHERE user_id = ? LIMIT 1";
        try (Connection conn = connectionSupplier.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
