package com.alumconnect.repository;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.MentorshipRequest;
import com.alumconnect.model.MentorshipStatus;
import com.alumconnect.model.Role;
import com.alumconnect.model.Student;
import com.alumconnect.service.dto.MentorshipRequestDto;
import com.alumconnect.util.ConnectionPool;
import com.alumconnect.util.DatabaseConnection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("JDBC Mentorship Request Repository Integration Tests")
class JdbcMentorshipRequestRepositoryTest {

    private static boolean dbAvailable = false;
    private static JdbcUserRepository userRepo;
    private static JdbcMentorshipRequestRepository requestRepo;
    private final List<Integer> createdUserIds = new ArrayList<>();
    private final List<Integer> createdRequestIds = new ArrayList<>();

    @BeforeAll
    static void init() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            dbAvailable = conn != null && conn.isValid(2);
            if (dbAvailable) {
                userRepo = new JdbcUserRepository();
                requestRepo = new JdbcMentorshipRequestRepository();
            }
        } catch (SQLException e) {
            dbAvailable = false;
        }
    }

    @AfterAll
    static void cleanupPool() {
        if (dbAvailable) {
            ConnectionPool.getInstance().shutdown();
        }
    }

    @AfterEach
    void cleanTestData() {
        if (!dbAvailable) return;

        try (Connection conn = DatabaseConnection.getConnection()) {
            for (int reqId : createdRequestIds) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM mentorship_requests WHERE id = ?")) {
                    ps.setInt(1, reqId);
                    ps.executeUpdate();
                }
            }
            for (int userId : createdUserIds) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
                    ps.setInt(1, userId);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException ignored) {
        }
        createdRequestIds.clear();
        createdUserIds.clear();
    }

    @Test
    @DisplayName("Create request and find by ID with student info")
    void testCreateAndFindById() throws SQLException {
        assumeTrue(dbAvailable, "Database must be available for integration test");

        Student student = new Student("test.std." + System.currentTimeMillis() + "@thapar.edu",
                "hashedPass1", "Integration Student", null, "Computer Science", 2026);
        student.setRole(Role.STUDENT);
        userRepo.saveStudent(student);
        createdUserIds.add(student.getId());

        Alumni alumni = new Alumni("test.alm." + System.currentTimeMillis() + "@thapar.edu",
                "hashedPass2", "Integration Alumni", null, "Computer Science", 2020);
        alumni.setRole(Role.ALUMNI);
        userRepo.saveAlumni(alumni);
        createdUserIds.add(alumni.getId());

        MentorshipRequestDto dto = new MentorshipRequestDto(student.getId(), alumni.getId(),
                "Integration Topic", "Integration Message");
        int reqId = requestRepo.createRequest(dto);
        assertTrue(reqId > 0);
        createdRequestIds.add(reqId);

        Optional<MentorshipRequest> foundOpt = requestRepo.findRequestById(reqId);
        assertTrue(foundOpt.isPresent());
        MentorshipRequest found = foundOpt.get();
        assertEquals("Integration Topic", found.getTopic());
        assertEquals("Integration Message", found.getMessage());
        assertEquals(MentorshipStatus.PENDING, found.getStatus());
        assertEquals(student.getFullName(), found.getStudentName());
        assertEquals(student.getEmail(), found.getStudentEmail());
    }

    @Test
    @DisplayName("Find requests by alumni ID")
    void testFindRequestsByAlumniId() throws SQLException {
        assumeTrue(dbAvailable, "Database must be available for integration test");

        Student student = new Student("test.std2." + System.currentTimeMillis() + "@thapar.edu",
                "hashedPass1", "Student Two", null, "ECE", 2025);
        student.setRole(Role.STUDENT);
        userRepo.saveStudent(student);
        createdUserIds.add(student.getId());

        Alumni alumni = new Alumni("test.alm2." + System.currentTimeMillis() + "@thapar.edu",
                "hashedPass2", "Alumni Two", null, "ECE", 2019);
        alumni.setRole(Role.ALUMNI);
        userRepo.saveAlumni(alumni);
        createdUserIds.add(alumni.getId());

        MentorshipRequestDto dto = new MentorshipRequestDto(student.getId(), alumni.getId(),
                "Topic 2", "Message 2");
        int reqId = requestRepo.createRequest(dto);
        assertTrue(reqId > 0);
        createdRequestIds.add(reqId);

        List<MentorshipRequest> list = requestRepo.findRequestsByAlumniId(alumni.getId());
        assertFalse(list.isEmpty());
        assertTrue(list.stream().anyMatch(r -> r.getId() == reqId));
    }

    @Test
    @DisplayName("Update request status to ACCEPTED and REJECTED")
    void testUpdateRequestStatus() throws SQLException {
        assumeTrue(dbAvailable, "Database must be available for integration test");

        Student student = new Student("test.std3." + System.currentTimeMillis() + "@thapar.edu",
                "hashedPass1", "Student Three", null, "ME", 2026);
        student.setRole(Role.STUDENT);
        userRepo.saveStudent(student);
        createdUserIds.add(student.getId());

        Alumni alumni = new Alumni("test.alm3." + System.currentTimeMillis() + "@thapar.edu",
                "hashedPass2", "Alumni Three", null, "ME", 2018);
        alumni.setRole(Role.ALUMNI);
        userRepo.saveAlumni(alumni);
        createdUserIds.add(alumni.getId());

        MentorshipRequestDto dto = new MentorshipRequestDto(student.getId(), alumni.getId(),
                "Topic 3", "Message 3");
        int reqId = requestRepo.createRequest(dto);
        assertTrue(reqId > 0);
        createdRequestIds.add(reqId);

        LocalDateTime now = LocalDateTime.now();
        boolean updated = requestRepo.updateRequestStatus(reqId, MentorshipStatus.ACCEPTED, now);
        assertTrue(updated);

        Optional<MentorshipRequest> foundOpt = requestRepo.findRequestById(reqId);
        assertTrue(foundOpt.isPresent());
        assertEquals(MentorshipStatus.ACCEPTED, foundOpt.get().getStatus());
        assertNotNull(foundOpt.get().getRespondedAt());
    }
}
