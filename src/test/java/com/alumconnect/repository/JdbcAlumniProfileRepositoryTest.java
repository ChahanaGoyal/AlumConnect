package com.alumconnect.repository;

import com.alumconnect.model.Alumni;
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

@DisplayName("JDBC Alumni Profile Repository Integration Tests")
class JdbcAlumniProfileRepositoryTest {

    private static boolean dbAvailable = false;
    private static JdbcUserRepository userRepo;
    private static JdbcAlumniProfileRepository profileRepo;
    private final List<Integer> createdUserIds = new ArrayList<>();

    @BeforeAll
    static void init() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            dbAvailable = conn != null && conn.isValid(2);
            if (dbAvailable) {
                userRepo = new JdbcUserRepository();
                profileRepo = new JdbcAlumniProfileRepository();
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
    @DisplayName("Find, update, and check existence of alumni profile via JDBC")
    void testFindAndUpdateAlumniProfile() throws SQLException {
        assumeTrue(dbAvailable, "Skipping: MySQL is not reachable");

        String email = "alumni.profile." + System.currentTimeMillis() + "@test.org";
        Alumni alumni = new Alumni(email, "hashed_pw", "Profile Alumni", "+1234567890", "Computer Science", 2020);
        alumni.setCurrentCompany("Tech Corp");
        alumni.setCurrentRole("Software Engineer");
        alumni.setIndustry("Technology");
        alumni.setYearsOfExperience(4);
        alumni.setExpertiseAreas("Java, Cloud, Spring");
        alumni.setAvailableForMentorship(true);
        alumni.setMaxMentees(3);

        Alumni saved = userRepo.saveAlumni(alumni);
        assertNotNull(saved.getId());
        createdUserIds.add(saved.getId());

        // Test profile existence
        assertTrue(profileRepo.profileExistsForUser(saved.getId()));

        // Test findProfileByUserId
        Optional<Alumni> found = profileRepo.findProfileByUserId(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Computer Science", found.get().getDepartment());
        assertEquals(2020, found.get().getGraduationYear());
        assertEquals("Tech Corp", found.get().getCurrentCompany());
        assertEquals("Software Engineer", found.get().getCurrentRole());
        assertEquals(4, found.get().getYearsOfExperience());

        // Test updateProfile
        found.get().setCurrentCompany("Innovate Inc");
        found.get().setCurrentRole("Principal Engineer");
        found.get().setYearsOfExperience(6);
        found.get().setBio("Updated alumni bio");
        found.get().setLinkedinUrl("https://linkedin.com/in/alumnitest");
        found.get().setMaxMentees(5);

        boolean updated = profileRepo.updateProfile(found.get());
        assertTrue(updated);

        // Verify updated values persisted
        Optional<Alumni> refreshed = profileRepo.findProfileByUserId(saved.getId());
        assertTrue(refreshed.isPresent());
        assertEquals("Innovate Inc", refreshed.get().getCurrentCompany());
        assertEquals("Principal Engineer", refreshed.get().getCurrentRole());
        assertEquals(6, refreshed.get().getYearsOfExperience());
        assertEquals("Updated alumni bio", refreshed.get().getBio());
        assertEquals(5, refreshed.get().getMaxMentees());
    }
}
