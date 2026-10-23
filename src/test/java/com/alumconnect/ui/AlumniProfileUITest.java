package com.alumconnect.ui;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.Role;
import com.alumconnect.model.Student;
import com.alumconnect.repository.AlumniProfileRepository;
import com.alumconnect.service.AlumniProfileService;
import com.alumconnect.service.dto.AlumniProfileResult;
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

@DisplayName("Alumni Profile Console UI Tests")
class AlumniProfileUITest {

    private InMemoryAlumniProfileRepo fakeRepo;
    private AlumniProfileService profileService;
    private Alumni testAlumni;

    @BeforeEach
    void setUp() {
        UserSession.getInstance().logout();
        fakeRepo = new InMemoryAlumniProfileRepo();
        profileService = new AlumniProfileService(fakeRepo);

        testAlumni = new Alumni(
                "vikram@google.com",
                PasswordUtil.hashPassword("Pass#12345"),
                "Vikram Malhotra",
                "+91-9876543210",
                "Computer Science",
                2019
        );
        testAlumni.setId(20);
        testAlumni.setProfileId(201);
        testAlumni.setCurrentCompany("Google");
        testAlumni.setCurrentRole("Staff Software Engineer");
        testAlumni.setIndustry("Technology");
        testAlumni.setYearsOfExperience(7);
        testAlumni.setExpertiseAreas("Cloud Architecture, Java, Go");
        testAlumni.setBio("Experienced engineer helping students prepare for tech careers.");
        testAlumni.setLinkedinUrl("https://linkedin.com/in/vikram");
        testAlumni.setAvailableForMentorship(true);
        testAlumni.setMaxMentees(4);

        fakeRepo.save(testAlumni);
    }

    @AfterEach
    void tearDown() {
        UserSession.getInstance().logout();
    }

    @Test
    @DisplayName("displayMyProfile displays all profile information for logged-in alumni")
    void testDisplayMyProfileSuccess() {
        UserSession.getInstance().login(testAlumni);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AlumniProfileUI ui = new AlumniProfileUI(profileService, new ByteArrayInputStream(new byte[0]), new PrintStream(out));

        AlumniProfileResult result = ui.displayMyProfile();

        assertTrue(result.isSuccess());
        String output = out.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("MY ALUMNI PROFILE"));
        assertTrue(output.contains("Vikram Malhotra"));
        assertTrue(output.contains("vikram@google.com"));
        assertTrue(output.contains("Google"));
        assertTrue(output.contains("Staff Software Engineer"));
        assertTrue(output.contains("7 years"));
        assertTrue(output.contains("Max Mentees     : 4"));
    }

    @Test
    @DisplayName("displayMyProfile shows error when unauthenticated")
    void testDisplayMyProfileUnauthenticated() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AlumniProfileUI ui = new AlumniProfileUI(profileService, new ByteArrayInputStream(new byte[0]), new PrintStream(out));

        AlumniProfileResult result = ui.displayMyProfile();

        assertFalse(result.isSuccess());
        String output = out.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("[ERROR]"));
        assertTrue(output.contains("must be logged in"));
    }

    @Test
    @DisplayName("promptAndEditProfile: preserves values on empty inputs (press Enter to keep)")
    void testEditProfileKeepDefaults() {
        UserSession.getInstance().login(testAlumni);

        // 11 inputs: dept, gradYear, degree, company, role, industry, exp, expertise, bio, linkedin, avail, maxMentees
        String input = "\n\n\n\n\n\n\n\n\n\n\n\n";
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        AlumniProfileUI ui = new AlumniProfileUI(profileService, in, new PrintStream(out));
        AlumniProfileResult result = ui.promptAndEditProfile();

        assertTrue(result.isSuccess());
        Alumni updated = result.getAlumni();
        assertEquals("Computer Science", updated.getDepartment());
        assertEquals(2019, updated.getGraduationYear());
        assertEquals("Google", updated.getCurrentCompany());
        assertEquals("Staff Software Engineer", updated.getCurrentRole());
        assertEquals(7, updated.getYearsOfExperience());
        assertEquals(4, updated.getMaxMentees());
    }

    @Test
    @DisplayName("promptAndEditProfile: updates fields when new values provided")
    void testEditProfileNewValues() {
        UserSession.getInstance().login(testAlumni);

        // Inputs for 12 prompts
        String input = String.join("\n",
                "Information Science",       // dept
                "2018",                      // gradYear
                "Master of Technology",      // degree
                "Meta",                      // company
                "Director of Engineering",   // role
                "Social Media & AI",         // industry
                "8",                         // experience
                "AI, Leadership, Scale",     // expertise
                "Updated bio.",              // bio
                "https://linkedin.com/in/v", // linkedin
                "N",                         // mentorship availability
                "2"                          // maxMentees
        ) + "\n";

        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        AlumniProfileUI ui = new AlumniProfileUI(profileService, in, new PrintStream(out));
        AlumniProfileResult result = ui.promptAndEditProfile();

        assertTrue(result.isSuccess());
        Alumni updated = result.getAlumni();
        assertEquals("Information Science", updated.getDepartment());
        assertEquals(2018, updated.getGraduationYear());
        assertEquals("Master of Technology", updated.getDegree());
        assertEquals("Meta", updated.getCurrentCompany());
        assertEquals("Director of Engineering", updated.getCurrentRole());
        assertEquals(8, updated.getYearsOfExperience());
        assertFalse(updated.isAvailableForMentorship());
        assertEquals(2, updated.getMaxMentees());
    }

    @Test
    @DisplayName("promptAndCreateProfile: successfully creates profile for fresh alumni")
    void testCreateProfileInteractive() {
        Alumni freshAlumni = new Alumni();
        freshAlumni.setId(30);
        freshAlumni.setEmail("fresh@alumni.org");
        freshAlumni.setFullName("Fresh Alum");
        freshAlumni.setRole(Role.ALUMNI);
        UserSession.getInstance().login(freshAlumni);

        String input = String.join("\n",
                "Computer Science",
                "2021",
                "B.Tech",
                "Startup Inc",
                "Frontend Lead",
                "Web",
                "3",
                "React, TypeScript",
                "Building cool apps.",
                "https://linkedin.com/in/fresh",
                "Y",
                "3"
        ) + "\n";

        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        AlumniProfileUI ui = new AlumniProfileUI(profileService, in, new PrintStream(out));
        AlumniProfileResult result = ui.promptAndCreateProfile();

        assertTrue(result.isSuccess());
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("MY ALUMNI PROFILE"));
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("Fresh Alum"));
    }

    @Test
    @DisplayName("promptAndEditProfile: invalid number inputs fail gracefully")
    void testEditProfileInvalidNumberInput() {
        UserSession.getInstance().login(testAlumni);

        String input = "Computer Science\nnot-a-number\n";
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        AlumniProfileUI ui = new AlumniProfileUI(profileService, in, new PrintStream(out));
        AlumniProfileResult result = ui.promptAndEditProfile();

        assertFalse(result.isSuccess());
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("Graduation year must be a number"));
    }

    // ── In-Memory Repo ────────────────────────────────────────────────────────

    private static class InMemoryAlumniProfileRepo implements AlumniProfileRepository {
        private final Map<Integer, Alumni> store = new HashMap<>();

        void save(Alumni alumni) {
            store.put(alumni.getId(), alumni);
        }

        @Override
        public Optional<Alumni> findProfileByUserId(int userId) {
            return Optional.ofNullable(store.get(userId));
        }

        @Override
        public boolean updateProfile(Alumni alumni) {
            if (!store.containsKey(alumni.getId())) return false;
            store.put(alumni.getId(), alumni);
            return true;
        }

        @Override
        public boolean createProfile(Alumni alumni) {
            store.put(alumni.getId(), alumni);
            return true;
        }

        @Override
        public boolean profileExistsForUser(int userId) {
            return store.containsKey(userId);
        }
    }
}
