package com.alumconnect.repository;

import com.alumconnect.model.MentorshipRequest;
import com.alumconnect.model.MentorshipStatus;
import com.alumconnect.service.dto.MentorshipRequestDto;
import com.alumconnect.util.ConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    @Override
    public List<MentorshipRequest> findRequestsByAlumniId(int alumniId) throws SQLException {
        final String sql = "SELECT mr.id, mr.student_id, mr.alumni_id, mr.topic, mr.message, mr.status, " +
                "mr.response_notes, mr.requested_at, mr.responded_at, mr.completed_at, mr.updated_at, " +
                "u.full_name AS student_name, u.email AS student_email, " +
                "sp.degree AS student_degree, sp.department AS student_department, sp.graduation_year AS student_graduation_year " +
                "FROM mentorship_requests mr " +
                "JOIN users u ON mr.student_id = u.id " +
                "LEFT JOIN student_profiles sp ON mr.student_id = sp.user_id " +
                "WHERE mr.alumni_id = ? " +
                "ORDER BY mr.requested_at DESC, mr.id DESC";

        List<MentorshipRequest> list = new ArrayList<>();
        try (Connection conn = connectionSupplier.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, alumniId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToMentorshipRequest(rs));
                }
            }
        }
        return list;
    }

    @Override
    public List<MentorshipRequest> findRequestsByStudentId(int studentId) throws SQLException {
        final String sql = "SELECT mr.id, mr.student_id, mr.alumni_id, mr.topic, mr.message, mr.status, " +
                "mr.response_notes, mr.requested_at, mr.responded_at, mr.completed_at, mr.updated_at, " +
                "u.full_name AS alumni_name, u.email AS alumni_email, " +
                "ap.degree AS alumni_degree, ap.department AS alumni_department, " +
                "ap.current_company AS alumni_company, ap.current_role AS alumni_role " +
                "FROM mentorship_requests mr " +
                "JOIN users u ON mr.alumni_id = u.id " +
                "LEFT JOIN alumni_profiles ap ON mr.alumni_id = ap.user_id " +
                "WHERE mr.student_id = ? " +
                "ORDER BY mr.requested_at DESC, mr.id DESC";

        List<MentorshipRequest> list = new ArrayList<>();
        try (Connection conn = connectionSupplier.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToStudentMentorshipRequest(rs));
                }
            }
        }
        return list;
    }

    @Override
    public Optional<MentorshipRequest> findRequestById(int requestId) throws SQLException {
        final String sql = "SELECT mr.id, mr.student_id, mr.alumni_id, mr.topic, mr.message, mr.status, " +
                "mr.response_notes, mr.requested_at, mr.responded_at, mr.completed_at, mr.updated_at, " +
                "u.full_name AS student_name, u.email AS student_email, " +
                "sp.degree AS student_degree, sp.department AS student_department, sp.graduation_year AS student_graduation_year " +
                "FROM mentorship_requests mr " +
                "JOIN users u ON mr.student_id = u.id " +
                "LEFT JOIN student_profiles sp ON mr.student_id = sp.user_id " +
                "WHERE mr.id = ?";

        try (Connection conn = connectionSupplier.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToMentorshipRequest(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean updateRequestStatus(int requestId, MentorshipStatus newStatus, LocalDateTime respondedAt) throws SQLException {
        final String sql = "UPDATE mentorship_requests SET status = ?, responded_at = ? WHERE id = ?";
        try (Connection conn = connectionSupplier.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setTimestamp(2, respondedAt != null ? Timestamp.valueOf(respondedAt) : Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(3, requestId);
            return ps.executeUpdate() > 0;
        }
    }

    private MentorshipRequest mapRowToMentorshipRequest(ResultSet rs) throws SQLException {
        MentorshipRequest request = new MentorshipRequest();
        request.setId(rs.getInt("id"));
        request.setStudentId(rs.getInt("student_id"));
        request.setAlumniId(rs.getInt("alumni_id"));
        request.setTopic(rs.getString("topic"));
        request.setMessage(rs.getString("message"));

        String statusStr = rs.getString("status");
        if (statusStr != null) {
            request.setStatus(MentorshipStatus.valueOf(statusStr));
        }

        request.setResponseNotes(rs.getString("response_notes"));

        Timestamp reqAt = rs.getTimestamp("requested_at");
        if (reqAt != null) {
            request.setRequestedAt(reqAt.toLocalDateTime());
        }
        Timestamp respAt = rs.getTimestamp("responded_at");
        if (respAt != null) {
            request.setRespondedAt(respAt.toLocalDateTime());
        }
        Timestamp compAt = rs.getTimestamp("completed_at");
        if (compAt != null) {
            request.setCompletedAt(compAt.toLocalDateTime());
        }
        Timestamp upAt = rs.getTimestamp("updated_at");
        if (upAt != null) {
            request.setUpdatedAt(upAt.toLocalDateTime());
        }

        // Student details from JOIN
        request.setStudentName(rs.getString("student_name"));
        request.setStudentEmail(rs.getString("student_email"));
        request.setStudentDegree(rs.getString("student_degree"));
        request.setStudentDepartment(rs.getString("student_department"));
        int gradYear = rs.getInt("student_graduation_year");
        if (!rs.wasNull()) {
            request.setStudentGraduationYear(gradYear);
        }

        return request;
    }

    private MentorshipRequest mapRowToStudentMentorshipRequest(ResultSet rs) throws SQLException {
        MentorshipRequest request = new MentorshipRequest();
        request.setId(rs.getInt("id"));
        request.setStudentId(rs.getInt("student_id"));
        request.setAlumniId(rs.getInt("alumni_id"));
        request.setTopic(rs.getString("topic"));
        request.setMessage(rs.getString("message"));

        String statusStr = rs.getString("status");
        if (statusStr != null) {
            request.setStatus(MentorshipStatus.valueOf(statusStr));
        }

        request.setResponseNotes(rs.getString("response_notes"));

        Timestamp reqAt = rs.getTimestamp("requested_at");
        if (reqAt != null) {
            request.setRequestedAt(reqAt.toLocalDateTime());
        }
        Timestamp respAt = rs.getTimestamp("responded_at");
        if (respAt != null) {
            request.setRespondedAt(respAt.toLocalDateTime());
        }
        Timestamp compAt = rs.getTimestamp("completed_at");
        if (compAt != null) {
            request.setCompletedAt(compAt.toLocalDateTime());
        }
        Timestamp upAt = rs.getTimestamp("updated_at");
        if (upAt != null) {
            request.setUpdatedAt(upAt.toLocalDateTime());
        }

        // Alumni details from JOIN
        request.setAlumniName(rs.getString("alumni_name"));
        request.setAlumniEmail(rs.getString("alumni_email"));
        request.setAlumniDegree(rs.getString("alumni_degree"));
        request.setAlumniDepartment(rs.getString("alumni_department"));
        request.setAlumniCompany(rs.getString("alumni_company"));
        request.setAlumniRole(rs.getString("alumni_role"));

        return request;
    }
}
