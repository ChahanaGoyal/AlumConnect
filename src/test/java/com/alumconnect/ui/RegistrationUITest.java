package com.alumconnect.ui;

import com.alumconnect.model.Role;
import com.alumconnect.model.Student;
import com.alumconnect.repository.UserRepository;
import com.alumconnect.service.UserService;
import com.alumconnect.service.dto.RegistrationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Registration Console UI Tests")
class RegistrationUITest {

    @Test
    @DisplayName("Interactive Student Registration Flow via Console UI")
    void testStudentConsoleRegistrationFlow() {
        // Simulated user inputs:
        // 1 (Student role)
        // Full Name
        // Email
        // Password
        // Confirm Password
        // Phone
        // Department
        // Graduation Year
        // Degree (Enter for default)
        // Semester (5)
        // Enrollment number
        // Skills
        // Interests
        // LinkedIn
        // GitHub
        // Bio
        String simulatedInput = String.join("\n",
                "1",
                "Alice Smith",
                "alice.smith@university.edu",
                "SecretP@ssword1",
                "SecretP@ssword1",
                "+1234567890",
                "Computer Science",
                "2026",
                "", // default degree
                "5",
                "EN998877",
                "Java, Spring",
                "AI, Distributed Systems",
                "https://linkedin.com/in/alicesmith",
                "https://github.com/alicesmith",
                "Student developer"
        ) + "\n";

        ByteArrayInputStream in = new ByteArrayInputStream(simulatedInput.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outBytes, true, StandardCharsets.UTF_8);

        StubUserRepository stubRepo = new StubUserRepository();
        UserService userService = new UserService(stubRepo);
        RegistrationUI ui = new RegistrationUI(userService, in, out);

        RegistrationResult result = ui.promptAndRegister();

        assertTrue(result.isSuccess(), "Registration should succeed");
        assertNotNull(result.getUser());
        assertEquals("alice.smith@university.edu", result.getUser().getEmail());
        assertEquals(Role.STUDENT, result.getUser().getRole());

        String output = outBytes.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("[SUCCESS]"));
        assertTrue(output.contains("Alice Smith"));
    }

    @Test
    @DisplayName("Interactive Alumni Registration Flow via Console UI")
    void testAlumniConsoleRegistrationFlow() {
        String simulatedInput = String.join("\n",
                "2", // Alumni role
                "Bob Johnson",
                "bob.johnson@corporate.org",
                "AlumP@ssword2026",
                "AlumP@ssword2026",
                "+1987654321",
                "Electrical Engineering",
                "2018",
                "", // default degree
                "Global Tech Inc",
                "Principal Architect",
                "Technology",
                "8",
                "Hardware, Microcontrollers",
                "Y", // mentorship available
                "5", // max mentees
                "https://linkedin.com/in/bobjohnson",
                "Senior engineering mentor"
        ) + "\n";

        ByteArrayInputStream in = new ByteArrayInputStream(simulatedInput.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outBytes, true, StandardCharsets.UTF_8);

        StubUserRepository stubRepo = new StubUserRepository();
        UserService userService = new UserService(stubRepo);
        RegistrationUI ui = new RegistrationUI(userService, in, out);

        RegistrationResult result = ui.promptAndRegister();

        assertTrue(result.isSuccess(), "Registration should succeed");
        assertEquals(Role.ALUMNI, result.getUser().getRole());

        String output = outBytes.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("[SUCCESS]"));
        assertTrue(output.contains("Bob Johnson"));
    }

    @Test
    @DisplayName("Console UI handles invalid role selection")
    void testInvalidRoleSelection() {
        String simulatedInput = "9\n";

        ByteArrayInputStream in = new ByteArrayInputStream(simulatedInput.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outBytes, true, StandardCharsets.UTF_8);

        StubUserRepository stubRepo = new StubUserRepository();
        UserService userService = new UserService(stubRepo);
        RegistrationUI ui = new RegistrationUI(userService, in, out);

        RegistrationResult result = ui.promptAndRegister();

        assertFalse(result.isSuccess());
        String output = outBytes.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("[ERROR]"));
        assertTrue(output.contains("Invalid role selection"));
    }

    // ── Stub Repository ───────────────────────────────────────────────────────

    private static class StubUserRepository implements UserRepository {
        private int seq = 1;
        private final Map<String, Integer> emails = new HashMap<>();

        @Override
        public boolean existsByEmail(String email) {
            return emails.containsKey(email.trim().toLowerCase());
        }

        @Override
        public Optional<com.alumconnect.model.User> findById(int id) { return Optional.empty(); }

        @Override
        public Optional<com.alumconnect.model.User> findByEmail(String email) { return Optional.empty(); }

        @Override
        public Optional<Student> findStudentById(int userId) { return Optional.empty(); }

        @Override
        public Optional<com.alumconnect.model.Alumni> findAlumniById(int userId) { return Optional.empty(); }

        @Override
        public Student saveStudent(Student student) throws SQLException {
            student.setId(seq++);
            student.setProfileId(seq++);
            emails.put(student.getEmail().trim().toLowerCase(), student.getId());
            return student;
        }

        @Override
        public com.alumconnect.model.Alumni saveAlumni(com.alumconnect.model.Alumni alumni) throws SQLException {
            alumni.setId(seq++);
            alumni.setProfileId(seq++);
            emails.put(alumni.getEmail().trim().toLowerCase(), alumni.getId());
            return alumni;
        }

        @Override
        public boolean deleteUserById(int userId) { return true; }
    }
}
