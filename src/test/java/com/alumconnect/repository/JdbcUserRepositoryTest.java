package com.alumconnect.repository;

import com.alumconnect.model.Alumni;
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

@DisplayName("JDBC User Repository Integration Tests")
class JdbcUserRepositoryTest {

    private static boolean dbAvailable = false;
    private static JdbcUserRepository repository;
    private final List<Integer> createdUserIds = new ArrayList<>();

    @BeforeAll
    static void init() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            dbAvailable = conn != null && conn.isValid(2);
            if (dbAvailable) {
                repository = new JdbcUserRepository();
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
        if (dbAvailable && repository != null) {
            for (Integer id : createdUserIds) {
                try {
                    repository.deleteUserById(id);
                } catch (SQLException ignored) {
                }
            }
            createdUserIds.clear();
        }
    }

    @Test
    @DisplayName("Save and retrieve Student via JDBC")
    void testSaveAndRetrieveStudent() throws SQLException {
        assumeTrue(dbAvailable, "Skipping: MySQL is not reachable");

        String email = "test.student." + System.currentTimeMillis() + "@test.org";
        Student student = new Student(email, "hashed_secret_pw", "Test Student", "+1234567890", "Computer Science", 2026);
        student.setCurrentSemester(6);
        student.setEnrollmentNumber("TST_" + System.currentTimeMillis());

        Student saved = repository.saveStudent(student);
        assertNotNull(saved.getId());
        assertNotNull(saved.getProfileId());
        createdUserIds.add(saved.getId());

        assertTrue(repository.existsByEmail(email));

        Optional<Student> fetched = repository.findStudentById(saved.getId());
        assertTrue(fetched.isPresent());
        assertEquals("Test Student", fetched.get().getFullName());
        assertEquals(Role.STUDENT, fetched.get().getRole());
        assertEquals("Computer Science", fetched.get().getDepartment());
        assertEquals(2026, fetched.get().getGraduationYear());
    }

    @Test
    @DisplayName("Save and retrieve Alumni via JDBC")
    void testSaveAndRetrieveAlumni() throws SQLException {
        assumeTrue(dbAvailable, "Skipping: MySQL is not reachable");

        String email = "test.alumni." + System.currentTimeMillis() + "@test.org";
        Alumni alumni = new Alumni(email, "hashed_secret_pw", "Test Alumni", "+1234567890", "Electronics", 2020);
        alumni.setCurrentCompany("Test Corp");
        alumni.setCurrentRole("Tech Lead");
        alumni.setYearsOfExperience(6);

        Alumni saved = repository.saveAlumni(alumni);
        assertNotNull(saved.getId());
        assertNotNull(saved.getProfileId());
        createdUserIds.add(saved.getId());

        assertTrue(repository.existsByEmail(email));

        Optional<Alumni> fetched = repository.findAlumniById(saved.getId());
        assertTrue(fetched.isPresent());
        assertEquals("Test Alumni", fetched.get().getFullName());
        assertEquals(Role.ALUMNI, fetched.get().getRole());
        assertEquals("Test Corp", fetched.get().getCurrentCompany());
    }
}
