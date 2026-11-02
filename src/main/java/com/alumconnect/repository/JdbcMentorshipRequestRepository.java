package com.alumconnect.repository;

import com.alumconnect.service.dto.MentorshipRequestDto;
import com.alumconnect.util.ConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * JDBC implementation of {@link MentorshipRequestRepository}.
 *
 * Mirrors the patterns used in other repository implementations (e.g.,
 * {@code JdbcUserRepository}, {@code JdbcAlumniProfileRepository}). All SQL is
 * executed via {@link PreparedStatement} to avoid injection, and resources are
 * managed with try‑with‑resources.
 */
public class JdbcMentorshipRequestRepository implements MentorshipRequestRepository {

    @FunctionalInterface
    public interface ConnectionSupplier {
        Connection get() throws SQLException;
    }

    private final ConnectionSupplier connectionSupplier;

    /** Default constructor using the shared {@link ConnectionPool}. */
    public JdbcMentorshipRequestRepository() {
        this(() -> ConnectionPool.getInstance().acquire());
    }

    /** Constructor allowing a custom {@link ConnectionSupplier} (used in tests). */
    public JdbcMentorshipRequestRepository(ConnectionSupplier connectionSupplier) {
        this.connectionSupplier = connectionSupplier;
    }

    @Override
    public int createRequest(MentorshipRequestDto dto) throws SQLException {
        final String sql = "INSERT INTO mentorship_requests (student_id, alumni_id, topic, message, status) " +
                "VALUES (?, ?, ?, ?, 'PENDING')";
        try (Connection conn = connectionSupplier.get();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, dto.getStudentId());
            ps.setInt(2, dto.getAlumniId());
            ps.setString(3, dto.getTopic());
            ps.setString(4, dto.getMessage());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1; // indicates failure to retrieve generated id
    }

    @Override
    public boolean existsPending(int studentId, int alumniId) throws SQLException {
        final String sql = "SELECT 1 FROM mentorship_requests WHERE student_id = ? AND alumni_id = ? AND status = 'PENDING' LIMIT 1";
        try (Connection conn = connectionSupplier.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, alumniId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public boolean existsAccepted(int studentId, int alumniId) throws SQLException {
        final String sql = "SELECT 1 FROM mentorship_requests WHERE student_id = ? AND alumni_id = ? AND status = 'ACCEPTED' LIMIT 1";
        try (Connection conn = connectionSupplier.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, alumniId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public int countActiveMentees(int alumniId) throws SQLException {
        final String sql = "SELECT COUNT(*) FROM mentorship_requests WHERE alumni_id = ? AND status IN ('PENDING', 'ACCEPTED')";
        try (Connection conn = connectionSupplier.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, alumniId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    @Override
    public boolean isAlumniAvailable(int alumniId) throws SQLException {
        final String sql = "SELECT is_available_for_mentorship FROM alumni_profiles WHERE user_id = ?";
        try (Connection conn = connectionSupplier.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, alumniId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean(1);
                }
            }
        }
        return false;
    }
}

