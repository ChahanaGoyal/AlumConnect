package com.alumconnect.repository;

import com.alumconnect.model.Role;
import com.alumconnect.model.Student;
import com.alumconnect.util.ConnectionPool;
import com.alumconnect.util.DatabaseConnection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("JDBC Student Profile Repository Integration Tests")
class JdbcStudentProfileRepositoryTest {

    private static boolean dbAvailable = false;
    private static JdbcUserRepository userRepo;
    private static JdbcStudentProfileRepository profileRepo;
    private final List<Integer> createdUserIds = new ArrayList<>();

    @BeforeAll
    static void init() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            dbAvailable = conn != null && conn.isValid(2);
            if (dbAvailable) {
                userRepo = new JdbcUserRepository();
                profileRepo = new JdbcStudentProfileRepository();
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
        if (dbAvailable && userRepo != null) {
            for (Integer id : createdUserIds) {
                try {
                    userRepo.deleteUserById(id);
                } catch (SQLException ignored) {
                }
            }
            createdUserIds.clear();
        }
    }

    @Test
    @DisplayName("Find, update, and check existence of student profile via JDBC")
    void testFindAndUpdateStudentProfile() throws SQLException {
        assumeTrue(dbAvailable, "Skipping: MySQL is not reachable");

        String email = "student.profile." + System.currentTimeMillis() + "@test.org";
        Student student = new Student(email, "hashed_pw", "Profile Student", "+1234567890", "Computer Science", 2026);
        student.setCurrentSemester(4);
        student.setEnrollmentNumber("STP_" + System.currentTimeMillis());

        Student saved = userRepo.saveStudent(student);
        assertNotNull(saved.getId());
        createdUserIds.add(saved.getId());

        // Test profile existence
        assertTrue(profileRepo.profileExistsForUser(saved.getId()));

        // Test findProfileByUserId
        Optional<Student> found = profileRepo.findProfileByUserId(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Computer Science", found.get().getDepartment());
        assertEquals(2026, found.get().getGraduationYear());
        assertEquals(4, found.get().getCurrentSemester());

        // Test updateProfile
        found.get().setDepartment("Information Technology");
        found.get().setCurrentSemester(5);
        found.get().setSkills("Java, Docker, Kubernetes");
        found.get().setBio("Updated student bio");
        found.get().setLinkedinUrl("https://linkedin.com/in/test");

        boolean updated = profileRepo.updateProfile(found.get());
        assertTrue(updated);

        // Verify updated values persisted
        Optional<Student> refreshed = profileRepo.findProfileByUserId(saved.getId());
        assertTrue(refreshed.isPresent());
        assertEquals("Information Technology", refreshed.get().getDepartment());
        assertEquals(5, refreshed.get().getCurrentSemester());
        assertEquals("Java, Docker, Kubernetes", refreshed.get().getSkills());
        assertEquals("Updated student bio", refreshed.get().getBio());
        assertEquals("https://linkedin.com/in/test", refreshed.get().getLinkedinUrl());
    }
}
