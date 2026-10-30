package com.alumconnect.repository;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.Role;
import com.alumconnect.util.ConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of {@link AlumniProfileRepository}.
 *
 * <p>Reuses the same {@link ConnectionPool} as {@link JdbcUserRepository} so all
 * database access goes through the shared connection pool configuration.
 */
public class JdbcAlumniProfileRepository implements AlumniProfileRepository {

    @FunctionalInterface
    public interface ConnectionSupplier {
        Connection get() throws SQLException;
    }

    private final ConnectionSupplier connectionSupplier;

    /**
     * Default constructor using the shared {@link ConnectionPool}.
     */
    public JdbcAlumniProfileRepository() {
        this(() -> ConnectionPool.getInstance().acquire());
    }

    /**
     * Constructor allowing custom connection supply (e.g. for testing).
     *
     * @param connectionSupplier provider of JDBC connections
     */
    public JdbcAlumniProfileRepository(ConnectionSupplier connectionSupplier) {
        this.connectionSupplier = connectionSupplier;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Executes an INNER JOIN query on {@code users} and {@code alumni_profiles} matching
     * {@code alumni_profiles.user_id = ?}. Returns empty if no profile row exists yet.
     */
    @Override
    public Optional<Alumni> findProfileByUserId(int userId) throws SQLException {
        final String sql =
                "SELECT u.id AS u_id, u.email, u.password_hash, u.full_name, u.role, u.phone, " +
                "u.is_active, u.created_at AS u_created_at, u.updated_at AS u_updated_at, " +
                "ap.id AS ap_id, ap.graduation_year, ap.degree, ap.department, ap.current_company, " +
                "ap.current_role, ap.industry, ap.years_of_experience, ap.expertise_areas, ap.bio, " +
                "ap.linkedin_url, ap.is_available_for_mentorship, ap.max_mentees " +
                "FROM users u " +
                "INNER JOIN alumni_profiles ap ON u.id = ap.user_id " +
                "WHERE u.id = ? AND u.role = 'ALUMNI'";

        try (Connection conn = connectionSupplier.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(mapRowToAlumni(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Executes a query joining {@code users} and {@code alumni_profiles} to return all
     * registered active alumni on the platform, ordered alphabetically by full name.
     */
    @Override
    public List<Alumni> findAllAlumni() throws SQLException {
        final String sql =
                "SELECT u.id AS u_id, u.email, u.password_hash, u.full_name, u.role, u.phone, " +
                "u.is_active, u.created_at AS u_created_at, u.updated_at AS u_updated_at, " +
                "ap.id AS ap_id, ap.graduation_year, ap.degree, ap.department, ap.current_company, " +
                "ap.current_role, ap.industry, ap.years_of_experience, ap.expertise_areas, ap.bio, " +
                "ap.linkedin_url, ap.is_available_for_mentorship, ap.max_mentees " +
                "FROM users u " +
                "LEFT JOIN alumni_profiles ap ON u.id = ap.user_id " +
                "WHERE u.role = 'ALUMNI' AND u.is_active = TRUE " +
                "ORDER BY u.full_name ASC, u.id ASC";

        List<Alumni> list = new ArrayList<>();
        try (Connection conn = connectionSupplier.get();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Alumni alumni = mapRowToAlumni(rs);
                if (alumni != null) {
                    list.add(alumni);
                }
            }
        }
        return list;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Executes a parameterized query joining {@code users} and {@code alumni_profiles}
     * filtering by keyword across full_name, current_company, current_role, industry,
     * expertise_areas, and department (case-insensitive substring match).
     */
    @Override
    public List<Alumni> searchAlumni(String keyword) throws SQLException {
        if (keyword == null || keyword.isBlank()) {
            return Collections.emptyList();
        }
        final String pattern = "%" + keyword.trim().toLowerCase() + "%";
        final String sql =
                "SELECT u.id AS u_id, u.email, u.password_hash, u.full_name, u.role, u.phone, " +
                "u.is_active, u.created_at AS u_created_at, u.updated_at AS u_updated_at, " +
                "ap.id AS ap_id, ap.graduation_year, ap.degree, ap.department, ap.current_company, " +
                "ap.current_role, ap.industry, ap.years_of_experience, ap.expertise_areas, ap.bio, " +
                "ap.linkedin_url, ap.is_available_for_mentorship, ap.max_mentees " +
                "FROM users u " +
                "LEFT JOIN alumni_profiles ap ON u.id = ap.user_id " +
                "WHERE u.role = 'ALUMNI' AND u.is_active = TRUE AND (" +
                "  LOWER(u.full_name) LIKE ? OR " +
                "  LOWER(COALESCE(ap.current_company, '')) LIKE ? OR " +
                "  LOWER(COALESCE(ap.current_role, '')) LIKE ? OR " +
                "  LOWER(COALESCE(ap.industry, '')) LIKE ? OR " +
                "  LOWER(COALESCE(ap.expertise_areas, '')) LIKE ? OR " +
                "  LOWER(COALESCE(ap.department, '')) LIKE ?" +
                ") " +
                "ORDER BY u.full_name ASC, u.id ASC";

        List<Alumni> list = new ArrayList<>();
        try (Connection conn = connectionSupplier.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i <= 6; i++) {
                ps.setString(i, pattern);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Alumni alumni = mapRowToAlumni(rs);
                    if (alumni != null) {
                        list.add(alumni);
                    }
                }
            }
        }
        return list;
    }

    /**
     * Maps a ResultSet row joining {@code users} and optionally {@code alumni_profiles}
     * into a fully populated {@link Alumni} model. Handles missing profile columns gracefully.
     *
     * @param rs the ResultSet cursor positioned at a valid row
     * @return populated Alumni object, or null if essential user identification is invalid
     * @throws SQLException if a database error occurs during extraction
     */
    private Alumni mapRowToAlumni(ResultSet rs) throws SQLException {
        int userId = rs.getInt("u_id");
        if (userId <= 0) {
            return null;
        }

        Alumni alumni = new Alumni();
        alumni.setId(userId);
        alumni.setEmail(rs.getString("email"));
        alumni.setPasswordHash(rs.getString("password_hash"));
        String fullName = rs.getString("full_name");
        alumni.setFullName(fullName != null ? fullName : "Unknown Alumni");
        alumni.setRole(Role.ALUMNI);
        alumni.setPhone(rs.getString("phone"));
        alumni.setActive(rs.getBoolean("is_active"));

        Timestamp createdTs = rs.getTimestamp("u_created_at");
        if (createdTs != null) alumni.setCreatedAt(createdTs.toLocalDateTime());
        Timestamp updatedTs = rs.getTimestamp("u_updated_at");
        if (updatedTs != null) alumni.setUpdatedAt(updatedTs.toLocalDateTime());

        // Map alumni_profiles columns if row exists (LEFT JOIN check)
        Object apIdObj = rs.getObject("ap_id");
        if (apIdObj != null) {
            alumni.setProfileId(rs.getInt("ap_id"));

            int gradYear = rs.getInt("graduation_year");
            if (gradYear >= Alumni.MIN_GRAD_YEAR && gradYear <= Alumni.MAX_GRAD_YEAR) {
                alumni.setGraduationYear(gradYear);
            }

            String degree = rs.getString("degree");
            if (degree != null && !degree.isBlank()) {
                alumni.setDegree(degree);
            }

            alumni.setDepartment(rs.getString("department"));
            alumni.setCurrentCompany(rs.getString("current_company"));
            alumni.setCurrentRole(rs.getString("current_role"));
            alumni.setIndustry(rs.getString("industry"));

            int exp = rs.getInt("years_of_experience");
            if (exp >= 0) {
                alumni.setYearsOfExperience(exp);
            }

            alumni.setExpertiseAreas(rs.getString("expertise_areas"));
            alumni.setBio(rs.getString("bio"));
            alumni.setLinkedinUrl(rs.getString("linkedin_url"));
            alumni.setAvailableForMentorship(rs.getBoolean("is_available_for_mentorship"));
            alumni.setMaxMentees(rs.getInt("max_mentees"));
        }

        return alumni;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Executes an UPDATE on {@code alumni_profiles} WHERE {@code user_id = ?}.
     */
    @Override
    public boolean updateProfile(Alumni alumni) throws SQLException {
        final String sql =
                "UPDATE alumni_profiles SET " +
                "graduation_year = ?, degree = ?, department = ?, current_company = ?, " +
                "current_role = ?, industry = ?, years_of_experience = ?, " +
                "expertise_areas = ?, bio = ?, linkedin_url = ?, " +
                "is_available_for_mentorship = ?, max_mentees = ? " +
                "WHERE user_id = ?";

        try (Connection conn = connectionSupplier.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, alumni.getGraduationYear());
            ps.setString(2, alumni.getDegree() != null ? alumni.getDegree() : Alumni.DEFAULT_DEGREE);
            ps.setString(3, alumni.getDepartment());
            ps.setString(4, alumni.getCurrentCompany());
            ps.setString(5, alumni.getCurrentRole());
            ps.setString(6, alumni.getIndustry());
            ps.setInt(7, alumni.getYearsOfExperience());
            ps.setString(8, alumni.getExpertiseAreas());
            ps.setString(9, alumni.getBio());
            ps.setString(10, alumni.getLinkedinUrl());
            ps.setBoolean(11, alumni.isAvailableForMentorship());
            ps.setInt(12, alumni.getMaxMentees());
            ps.setInt(13, alumni.getId());

            return ps.executeUpdate() > 0;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Inserts a new row into {@code alumni_profiles} with the given alumni's user_id as FK.
     */
    @Override
    public boolean createProfile(Alumni alumni) throws SQLException {
        final String sql =
                "INSERT INTO alumni_profiles " +
                "(user_id, graduation_year, degree, department, current_company, current_role, " +
                "industry, years_of_experience, expertise_areas, bio, linkedin_url, " +
                "is_available_for_mentorship, max_mentees) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = connectionSupplier.get();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, alumni.getId());
            ps.setInt(2, alumni.getGraduationYear());
            ps.setString(3, alumni.getDegree() != null ? alumni.getDegree() : Alumni.DEFAULT_DEGREE);
            ps.setString(4, alumni.getDepartment());
            ps.setString(5, alumni.getCurrentCompany());
            ps.setString(6, alumni.getCurrentRole());
            ps.setString(7, alumni.getIndustry());
            ps.setInt(8, alumni.getYearsOfExperience());
            ps.setString(9, alumni.getExpertiseAreas());
            ps.setString(10, alumni.getBio());
            ps.setString(11, alumni.getLinkedinUrl());
            ps.setBoolean(12, alumni.isAvailableForMentorship());
            ps.setInt(13, alumni.getMaxMentees());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        alumni.setProfileId(rs.getInt(1));
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
     * <p>Uses a lightweight {@code SELECT 1} existence query against {@code alumni_profiles}.
     */
    @Override
    public boolean profileExistsForUser(int userId) throws SQLException {
        final String sql = "SELECT 1 FROM alumni_profiles WHERE user_id = ? LIMIT 1";
        try (Connection conn = connectionSupplier.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
