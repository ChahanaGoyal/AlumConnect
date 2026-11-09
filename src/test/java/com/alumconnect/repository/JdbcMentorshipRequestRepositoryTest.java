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

    @Test
    @DisplayName("Find requests by student ID with alumni info (Prompt 18)")
    void testFindRequestsByStudentId() throws SQLException {
        assumeTrue(dbAvailable, "Database must be available for integration test");

        Student student = new Student("test.std4." + System.currentTimeMillis() + "@thapar.edu",
                "hashedPass1", "Student Four", null, "CS", 2026);
        student.setRole(Role.STUDENT);
        userRepo.saveStudent(student);
        createdUserIds.add(student.getId());

        Alumni alumni = new Alumni("test.alm4." + System.currentTimeMillis() + "@thapar.edu",
                "hashedPass2", "Alumni Four", null, "CS", 2017);
        alumni.setRole(Role.ALUMNI);
        alumni.setCurrentCompany("Amazon");
        alumni.setCurrentRole("Principal SDE");
        userRepo.saveAlumni(alumni);
        createdUserIds.add(alumni.getId());

        MentorshipRequestDto dto = new MentorshipRequestDto(student.getId(), alumni.getId(),
                "Prompt 18 Topic", "Prompt 18 Message");
        int reqId = requestRepo.createRequest(dto);
        assertTrue(reqId > 0);
        createdRequestIds.add(reqId);

        List<MentorshipRequest> studentRequests = requestRepo.findRequestsByStudentId(student.getId());
        assertFalse(studentRequests.isEmpty());
        assertTrue(studentRequests.stream().anyMatch(r -> r.getId() == reqId));

        MentorshipRequest req = studentRequests.stream().filter(r -> r.getId() == reqId).findFirst().orElseThrow();
        assertEquals("Prompt 18 Topic", req.getTopic());
        assertEquals("Alumni Four", req.getAlumniName());
        assertEquals(alumni.getEmail(), req.getAlumniEmail());
        assertEquals(MentorshipStatus.PENDING, req.getStatus());
    }

    @Test
    @DisplayName("Find accepted connections by student ID (Prompt 19)")
    void testFindAcceptedConnectionsByStudentId() throws SQLException {
        assumeTrue(dbAvailable, "Database must be available for integration test");

        Student student = new Student("test.std5." + System.currentTimeMillis() + "@thapar.edu",
                "hashedPass1", "Student Five", null, "CS", 2026);
        student.setRole(Role.STUDENT);
        userRepo.saveStudent(student);
        createdUserIds.add(student.getId());

        Alumni alumni = new Alumni("test.alm5." + System.currentTimeMillis() + "@thapar.edu",
                "hashedPass2", "Alumni Five", null, "CS", 2016);
        alumni.setRole(Role.ALUMNI);
        alumni.setCurrentCompany("Microsoft");
        alumni.setCurrentRole("Senior Architect");
        userRepo.saveAlumni(alumni);
        createdUserIds.add(alumni.getId());

        // Create accepted request
        MentorshipRequestDto dto1 = new MentorshipRequestDto(student.getId(), alumni.getId(),
                "Accepted Topic", "Accepted Message");
        int reqId1 = requestRepo.createRequest(dto1);
        requestRepo.updateRequestStatus(reqId1, MentorshipStatus.ACCEPTED, LocalDateTime.now());
        createdRequestIds.add(reqId1);

        // Create pending request (should be excluded)
        MentorshipRequestDto dto2 = new MentorshipRequestDto(student.getId(), alumni.getId(),
                "Pending Topic", "Pending Message");
        int reqId2 = requestRepo.createRequest(dto2);
        createdRequestIds.add(reqId2);

        List<MentorshipRequest> connections = requestRepo.findAcceptedConnectionsByStudentId(student.getId());
        assertEquals(1, connections.size());
        MentorshipRequest conn = connections.get(0);
        assertEquals(reqId1, conn.getId());
        assertEquals(MentorshipStatus.ACCEPTED, conn.getStatus());
        assertEquals("Alumni Five", conn.getAlumniName());
        assertEquals(alumni.getEmail(), conn.getAlumniEmail());
        assertEquals("Microsoft", conn.getAlumniCompany());
        assertEquals("Senior Architect", conn.getAlumniRole());
    }

    @Test
    @DisplayName("Find accepted connections by alumni ID (Prompt 19)")
    void testFindAcceptedConnectionsByAlumniId() throws SQLException {
        assumeTrue(dbAvailable, "Database must be available for integration test");

        Student student = new Student("test.std6." + System.currentTimeMillis() + "@thapar.edu",
                "hashedPass1", "Student Six", null, "ECE", 2024);
        student.setRole(Role.STUDENT);
        userRepo.saveStudent(student);
        createdUserIds.add(student.getId());

        Alumni alumni = new Alumni("test.alm6." + System.currentTimeMillis() + "@thapar.edu",
                "hashedPass2", "Alumni Six", null, "ECE", 2015);
        alumni.setRole(Role.ALUMNI);
        userRepo.saveAlumni(alumni);
        createdUserIds.add(alumni.getId());

        // Create accepted request
        MentorshipRequestDto dto1 = new MentorshipRequestDto(student.getId(), alumni.getId(),
                "Accepted Topic 2", "Accepted Message 2");
        int reqId1 = requestRepo.createRequest(dto1);
        requestRepo.updateRequestStatus(reqId1, MentorshipStatus.ACCEPTED, LocalDateTime.now());
        createdRequestIds.add(reqId1);

        // Create rejected request (should be excluded)
        MentorshipRequestDto dto2 = new MentorshipRequestDto(student.getId(), alumni.getId(),
                "Rejected Topic", "Rejected Message");
        int reqId2 = requestRepo.createRequest(dto2);
        requestRepo.updateRequestStatus(reqId2, MentorshipStatus.REJECTED, LocalDateTime.now());
        createdRequestIds.add(reqId2);

        List<MentorshipRequest> connections = requestRepo.findAcceptedConnectionsByAlumniId(alumni.getId());
        assertEquals(1, connections.size());
        MentorshipRequest conn = connections.get(0);
        assertEquals(reqId1, conn.getId());
        assertEquals(MentorshipStatus.ACCEPTED, conn.getStatus());
        assertEquals("Student Six", conn.getStudentName());
        assertEquals(student.getEmail(), conn.getStudentEmail());
        assertEquals("ECE", conn.getStudentDepartment());
    }
}
