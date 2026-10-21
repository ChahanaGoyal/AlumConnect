package com.alumconnect.ui;

import com.alumconnect.model.Role;
import com.alumconnect.model.Student;
import com.alumconnect.repository.StudentProfileRepository;
import com.alumconnect.service.StudentProfileService;
import com.alumconnect.service.dto.StudentProfileResult;
import com.alumconnect.util.PasswordUtil;
import com.alumconnect.util.UserSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

@DisplayName("Student Profile Console UI Tests")
class StudentProfileUITest {

    private InMemoryStudentProfileRepo fakeRepo;
    private StudentProfileService profileService;
    private Student testStudent;

    @BeforeEach
    void setUp() {
        UserSession.getInstance().logout();
        fakeRepo = new InMemoryStudentProfileRepo();
        profileService = new StudentProfileService(fakeRepo);

        testStudent = new Student(
                "rahul@college.edu",
                PasswordUtil.hashPassword("Pass#12345"),
                "Rahul Sharma",
                "+91-9876543210",
                "Computer Engineering",
                2026
        );
        testStudent.setId(10);
        testStudent.setProfileId(101);
        testStudent.setCurrentSemester(6);
        testStudent.setEnrollmentNumber("COE102203");
        testStudent.setSkills("Java, Spring, MySQL");
        testStudent.setInterests("Distributed Systems, Open Source");
        testStudent.setBio("Final year engineering student passionate about software.");
        testStudent.setLinkedinUrl("https://linkedin.com/in/rahul");
        testStudent.setGithubUrl("https://github.com/rahul");

        fakeRepo.save(testStudent);
    }

    @AfterEach
    void tearDown() {
        UserSession.getInstance().logout();
    }

    @Test
    @DisplayName("displayMyProfile displays all profile information for logged-in student")
    void testDisplayMyProfileSuccess() {
        UserSession.getInstance().login(testStudent);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StudentProfileUI ui = new StudentProfileUI(profileService, new ByteArrayInputStream(new byte[0]), new PrintStream(out));

        StudentProfileResult result = ui.displayMyProfile();

        assertTrue(result.isSuccess());
        String output = out.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("MY STUDENT PROFILE"));
        assertTrue(output.contains("Rahul Sharma"));
        assertTrue(output.contains("rahul@college.edu"));
        assertTrue(output.contains("Computer Engineering"));
        assertTrue(output.contains("2026"));
        assertTrue(output.contains("COE102203"));
        assertTrue(output.contains("Java, Spring, MySQL"));
    }

    @Test
    @DisplayName("displayMyProfile shows error message when user is not logged in")
    void testDisplayMyProfileUnauthenticated() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StudentProfileUI ui = new StudentProfileUI(profileService, new ByteArrayInputStream(new byte[0]), new PrintStream(out));

        StudentProfileResult result = ui.displayMyProfile();

        assertFalse(result.isSuccess());
        String output = out.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("[ERROR]"));
        assertTrue(output.contains("Access denied"));
    }

    @Test
    @DisplayName("promptAndEditProfile keeps existing values when user presses Enter on all fields")
    void testPromptAndEditProfileKeepDefaults() {
        UserSession.getInstance().login(testStudent);

        // Enter on all 10 prompts
        String input = "\n\n\n\n\n\n\n\n\n\n";
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        StudentProfileUI ui = new StudentProfileUI(profileService, in, new PrintStream(out));
        StudentProfileResult result = ui.promptAndEditProfile();

        assertTrue(result.isSuccess());
        assertEquals("Computer Engineering", result.getStudent().getDepartment());
        assertEquals(2026, result.getStudent().getGraduationYear());
        assertEquals(6, result.getStudent().getCurrentSemester());
        assertEquals("COE102203", result.getStudent().getEnrollmentNumber());
    }

    @Test
    @DisplayName("promptAndEditProfile updates fields when user inputs new data")
    void testPromptAndEditProfileUpdateValues() {
        UserSession.getInstance().login(testStudent);

        // Department, GradYear, Degree, Semester, Enrollment, Skills, Interests, Bio, LinkedIn, GitHub
        String input = String.join("\n",
                "Data Science",
                "2027",
                "B.Tech (Honors)",
                "7",
                "DS102209",
                "Python, PyTorch",
                "Machine Learning",
                "Aspiring AI researcher",
                "https://linkedin.com/in/rahul-ds",
                "https://github.com/rahul-ai"
        ) + "\n";

        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        StudentProfileUI ui = new StudentProfileUI(profileService, in, new PrintStream(out));
        StudentProfileResult result = ui.promptAndEditProfile();

        assertTrue(result.isSuccess());
        assertEquals("Data Science", result.getStudent().getDepartment());
        assertEquals(2027, result.getStudent().getGraduationYear());
        assertEquals("B.Tech (Honors)", result.getStudent().getDegree());
        assertEquals(7, result.getStudent().getCurrentSemester());
        assertEquals("DS102209", result.getStudent().getEnrollmentNumber());
        assertEquals("Python, PyTorch", result.getStudent().getSkills());
        assertEquals("Machine Learning", result.getStudent().getInterests());
    }

    @Test
    @DisplayName("promptAndEditProfile rejects invalid non-numeric graduation year")
    void testPromptAndEditProfileInvalidGradYear() {
        UserSession.getInstance().login(testStudent);

        String input = "Computer Science\nNotAYear\n";
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        StudentProfileUI ui = new StudentProfileUI(profileService, in, new PrintStream(out));
        StudentProfileResult result = ui.promptAndEditProfile();

        assertFalse(result.isSuccess());
        String output = out.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("[ERROR] Graduation year must be a number"));
    }

    @Test
    @DisplayName("promptAndCreateProfile creates profile successfully for fresh student")
    void testPromptAndCreateProfileSuccess() {
        Student fresh = new Student(
                "fresh@college.edu",
                PasswordUtil.hashPassword("Pass#12345"),
                "Freshman Student",
                null,
                "Civil Engineering",
                2028
        );
        fresh.setId(25);
        UserSession.getInstance().login(fresh);

        // Dept, GradYear, Degree, Semester, Enroll, Skills, Interests, Bio, LinkedIn, GitHub
        String input = String.join("\n",
                "Civil Engineering",
                "2028",
                "B.Tech",
                "2",
                "CE202401",
                "AutoCAD, Revit",
                "Structural Engineering",
                "Civil engineering student.",
                "https://linkedin.com/in/fresh",
                "https://github.com/fresh"
        ) + "\n";

        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        StudentProfileUI ui = new StudentProfileUI(profileService, in, new PrintStream(out));
        StudentProfileResult result = ui.promptAndCreateProfile();

        assertTrue(result.isSuccess());
        String output = out.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("[SUCCESS]"));
        assertTrue(output.contains("Civil Engineering"));
    }

    // ── Fake In-Memory StudentProfileRepository ───────────────────────────────

    private static class InMemoryStudentProfileRepo implements StudentProfileRepository {
        private final Map<Integer, Student> profiles = new HashMap<>();

        void save(Student s) {
            profiles.put(s.getId(), s);
        }

        @Override
        public Optional<Student> findProfileByUserId(int userId) {
            return Optional.ofNullable(profiles.get(userId));
        }

        @Override
        public boolean updateProfile(Student student) {
            if (!profiles.containsKey(student.getId())) return false;
            profiles.put(student.getId(), student);
            return true;
        }

        @Override
        public boolean createProfile(Student student) {
            profiles.put(student.getId(), student);
            return true;
        }

        @Override
        public boolean profileExistsForUser(int userId) {
            return profiles.containsKey(userId);
        }
    }
}
