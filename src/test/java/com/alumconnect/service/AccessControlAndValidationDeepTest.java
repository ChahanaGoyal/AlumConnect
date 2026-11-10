package com.alumconnect.service;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.MentorshipRequest;
import com.alumconnect.model.MentorshipStatus;
import com.alumconnect.model.Role;
import com.alumconnect.model.Student;
import com.alumconnect.model.User;
import com.alumconnect.repository.AlumniProfileRepository;
import com.alumconnect.repository.MentorshipRequestRepository;
import com.alumconnect.repository.StudentProfileRepository;
import com.alumconnect.repository.UserRepository;
import com.alumconnect.service.dto.AlumniDirectoryResult;
import com.alumconnect.service.dto.AlumniFilterCriteria;
import com.alumconnect.service.dto.AlumniProfileRequest;
import com.alumconnect.service.dto.AlumniProfileResult;
import com.alumconnect.service.dto.MentorshipRequestDto;
import com.alumconnect.service.dto.MentorshipRequestResult;
import com.alumconnect.service.dto.RegistrationRequest;
import com.alumconnect.service.dto.RegistrationResult;
import com.alumconnect.service.dto.StudentProfileRequest;
import com.alumconnect.service.dto.StudentProfileResult;
import com.alumconnect.ui.AlumniDashboardUI;
import com.alumconnect.ui.AlumniProfileUI;
import com.alumconnect.ui.StudentDashboardUI;
import com.alumconnect.ui.StudentProfileUI;
import com.alumconnect.util.PasswordUtil;
import com.alumconnect.util.UserSession;
import com.alumconnect.util.ValidationUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * In-depth access control and validation unit tests for Prompt 23D.
 *
 * <p>Covers:
 * <ul>
 *   <li>Student access restrictions (denial from alumni operations, cross-student privacy, request accept/reject)</li>
 *   <li>Alumni access restrictions (denial from student operations, cross-alumni privacy, request management)</li>
 *   <li>Unauthenticated access restrictions and stale session invalidation</li>
 *   <li>UI defense-in-depth: verification that authorization is enforced at service layer and menu options are role-filtered</li>
 *   <li>Validation testing: malformed emails, weak passwords, null/empty inputs, invalid LinkedIn URLs, graduation year boundaries, experience boundaries</li>
 *   <li>Duplicate mentorship request prevention and invalid status transitions</li>
 *   <li>Sanitization of sensitive error details</li>
 * </ul>
 */
@DisplayName("Access Control & Validation Deep Tests (Prompt 23D)")
class AccessControlAndValidationDeepTest {

    private InMemoryUserRepository userRepository;
    private InMemoryStudentProfileRepository studentProfileRepository;
    private InMemoryAlumniProfileRepository alumniProfileRepository;
    private InMemoryMentorshipRequestRepository requestRepository;

    private UserService userService;
    private StudentProfileService studentProfileService;
    private AlumniProfileService alumniProfileService;
    private AlumniDirectoryService directoryService;
    private MentorshipRequestService mentorshipService;

    private Student student1;
    private Student student2;
    private Alumni alumni1;
    private Alumni alumni2;

    @BeforeEach
    void setUp() throws SQLException {
        UserSession.getInstance().logout();

        userRepository = new InMemoryUserRepository();
        studentProfileRepository = new InMemoryStudentProfileRepository();
        alumniProfileRepository = new InMemoryAlumniProfileRepository();
        requestRepository = new InMemoryMentorshipRequestRepository();

        userService = new UserService(userRepository);
        studentProfileService = new StudentProfileService(studentProfileRepository);
        alumniProfileService = new AlumniProfileService(alumniProfileRepository);
        directoryService = new AlumniDirectoryService(alumniProfileRepository);
        mentorshipService = new MentorshipRequestService(requestRepository, userRepository);

        // Seed Student 1
        student1 = new Student(
                "student1@thapar.edu",
                PasswordUtil.hashPassword("StudentP@ss1"),
                "Kabir Sharma",
                "+91-9876543210",
                "Computer Science",
                2026
        );
        student1.setCurrentSemester(6);
        student1.setEnrollmentNumber("102203001");
        userRepository.saveStudent(student1);
        studentProfileRepository.save(student1);

        // Seed Student 2
        student2 = new Student(
                "student2@thapar.edu",
                PasswordUtil.hashPassword("StudentP@ss2"),
                "Meera Roy",
                "+91-9876543211",
                "Electronics",
                2027
        );
        student2.setCurrentSemester(4);
        student2.setEnrollmentNumber("102203002");
        userRepository.saveStudent(student2);
        studentProfileRepository.save(student2);

        // Seed Alumni 1
        alumni1 = new Alumni(
                "alumni1@thapar.edu",
                PasswordUtil.hashPassword("AlumniP@ss1"),
                "Aditya Chopra",
                "+91-9811001122",
                "Computer Science",
                2019
        );
        alumni1.setCurrentCompany("Microsoft");
        alumni1.setCurrentRole("Senior Software Engineer");
        alumni1.setYearsOfExperience(7);
        alumni1.setAvailableForMentorship(true);
        alumni1.setMaxMentees(3);
        userRepository.saveAlumni(alumni1);
        alumniProfileRepository.save(alumni1);

        // Seed Alumni 2
        alumni2 = new Alumni(
                "alumni2@thapar.edu",
                PasswordUtil.hashPassword("AlumniP@ss2"),
                "Isha Oberoi",
                "+91-9811003344",
                "Mechanical",
                2017
        );
        alumni2.setCurrentCompany("Boeing");
        alumni2.setCurrentRole("Lead Aerodynamics Engineer");
        alumni2.setYearsOfExperience(9);
        alumni2.setAvailableForMentorship(true);
        alumni2.setMaxMentees(2);
        userRepository.saveAlumni(alumni2);
        alumniProfileRepository.save(alumni2);
    }

    @AfterEach
    void tearDown() {
        UserSession.getInstance().logout();
    }

    // =========================================================================
    // 1. Student Access Restrictions
    // =========================================================================

    @Nested
    @DisplayName("Student Access Restrictions")
    class StudentAccessRestrictions {

        @Test
        @DisplayName("Student cannot invoke alumni-only profile service operations")
        void testStudentCannotInvokeAlumniProfileService() {
            UserSession.getInstance().login(student1);

            AlumniProfileResult viewRes = alumniProfileService.viewMyProfile();
            assertFalse(viewRes.isSuccess());
            assertTrue(viewRes.getMessage().toLowerCase().contains("only alumni"));

            AlumniProfileResult createRes = alumniProfileService.createMyProfile(new AlumniProfileRequest());
            assertFalse(createRes.isSuccess());
            assertTrue(createRes.getMessage().toLowerCase().contains("only alumni"));

            AlumniProfileResult updateRes = alumniProfileService.updateMyProfile(new AlumniProfileRequest());
            assertFalse(updateRes.isSuccess());
            assertTrue(updateRes.getMessage().toLowerCase().contains("only alumni"));
        }

        @Test
        @DisplayName("Student cannot view or manage another student's private profile")
        void testStudentCannotAccessAnotherStudentProfile() {
            UserSession.getInstance().login(student1);

            // Cannot view student 2's profile
            StudentProfileResult viewRes = studentProfileService.viewProfile(student2.getId());
            assertFalse(viewRes.isSuccess());
            assertTrue(viewRes.getMessage().toLowerCase().contains("cannot view another student's profile"));

            // Cannot update student 2's profile
            StudentProfileRequest updateReq = new StudentProfileRequest();
            updateReq.setDepartment("Hacked Department");
            updateReq.setGraduationYear(2027);
            StudentProfileResult updateRes = studentProfileService.updateProfile(student2.getId(), updateReq);
            assertFalse(updateRes.isSuccess());
            assertTrue(updateRes.getMessage().toLowerCase().contains("cannot modify another student's profile"));
        }

        @Test
        @DisplayName("Student cannot accept or reject mentorship requests")
        void testStudentCannotAcceptOrRejectMentorshipRequests() {
            // Seed a mentorship request
            MentorshipRequest req = new MentorshipRequest();
            req.setId(101);
            req.setStudentId(student1.getId());
            req.setAlumniId(alumni1.getId());
            req.setStatus(MentorshipStatus.PENDING);
            requestRepository.save(req);

            UserSession.getInstance().login(student1);

            MentorshipRequestResult acceptRes = mentorshipService.acceptRequest(101);
            assertFalse(acceptRes.isSuccess());
            assertTrue(acceptRes.getMessage().toLowerCase().contains("only alumni"));

            MentorshipRequestResult rejectRes = mentorshipService.rejectRequest(101);
            assertFalse(rejectRes.isSuccess());
            assertTrue(rejectRes.getMessage().toLowerCase().contains("only alumni"));
        }
    }

    // =========================================================================
    // 2. Alumni Access Restrictions
    // =========================================================================

    @Nested
    @DisplayName("Alumni Access Restrictions")
    class AlumniAccessRestrictions {

        @Test
        @DisplayName("Alumni cannot access student-only operations (directory, send request)")
        void testAlumniCannotAccessStudentOperations() {
            UserSession.getInstance().login(alumni1);

            // Cannot browse directory
            AlumniDirectoryResult dirRes = directoryService.getAlumniDirectory();
            assertFalse(dirRes.isSuccess());
            assertTrue(dirRes.getMessage().toLowerCase().contains("only students"));

            // Cannot search alumni
            AlumniDirectoryResult searchRes = directoryService.searchAlumni("Google");
            assertFalse(searchRes.isSuccess());
            assertTrue(searchRes.getMessage().toLowerCase().contains("only students"));

            // Cannot filter alumni
            AlumniDirectoryResult filterRes = directoryService.filterAlumni(new AlumniFilterCriteria());
            assertFalse(filterRes.isSuccess());
            assertTrue(filterRes.getMessage().toLowerCase().contains("only students"));

            // Cannot view individual alumni profiles via directory service
            AlumniDirectoryResult viewRes = directoryService.viewAlumniProfile(alumni2.getId());
            assertFalse(viewRes.isSuccess());
            assertTrue(viewRes.getMessage().toLowerCase().contains("only students"));

            // Cannot send mentorship requests
            MentorshipRequestResult sendRes = mentorshipService.sendRequest(alumni2.getId(), "Topic", "Msg");
            assertFalse(sendRes.isSuccess());
            assertTrue(sendRes.getMessage().toLowerCase().contains("only students"));
        }

        @Test
        @DisplayName("Alumni cannot modify another user's private profile")
        void testAlumniCannotModifyAnotherAlumniProfile() {
            UserSession.getInstance().login(alumni1);

            AlumniProfileRequest req = new AlumniProfileRequest();
            req.setDepartment("Mechanical");
            req.setGraduationYear(2017);
            req.setCurrentCompany("Unauthorized Company");

            AlumniProfileResult result = alumniProfileService.updateProfile(alumni2.getId(), req);

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().toLowerCase().contains("cannot modify another alumni's profile"));
        }

        @Test
        @DisplayName("Alumni cannot manage or respond to requests belonging to another alumnus")
        void testAlumniCannotManageAnotherAlumnusRequests() {
            // Seed request sent to alumni 2
            MentorshipRequest req = new MentorshipRequest();
            req.setId(201);
            req.setStudentId(student1.getId());
            req.setAlumniId(alumni2.getId());
            req.setStatus(MentorshipStatus.PENDING);
            requestRepository.save(req);

            UserSession.getInstance().login(alumni1);

            // Alumni 1 attempts to accept request sent to Alumni 2
            MentorshipRequestResult acceptRes = mentorshipService.acceptRequest(201);
            assertFalse(acceptRes.isSuccess());
            assertTrue(acceptRes.getMessage().toLowerCase().contains("requests sent to you"));

            // Alumni 1 attempts to reject request sent to Alumni 2
            MentorshipRequestResult rejectRes = mentorshipService.rejectRequest(201);
            assertFalse(rejectRes.isSuccess());
            assertTrue(rejectRes.getMessage().toLowerCase().contains("requests sent to you"));

            // Alumni 1 attempts to query Alumni 2's received requests
            MentorshipRequestResult queryRes = mentorshipService.getRequestsForAlumni(alumni2.getId());
            assertFalse(queryRes.isSuccess());
            assertTrue(queryRes.getMessage().toLowerCase().contains("another alumnus"));
        }
    }

    // =========================================================================
    // 3. Unauthenticated Restrictions & Session Stale Protection
    // =========================================================================

    @Nested
    @DisplayName("Unauthenticated Access & Session Invalidation")
    class UnauthenticatedRestrictions {

        @Test
        @DisplayName("Unauthenticated callers cannot access protected services")
        void testUnauthenticatedAccessRejectedAcrossServices() {
            // No login
            assertFalse(studentProfileService.viewMyProfile().isSuccess());
            assertFalse(alumniProfileService.viewMyProfile().isSuccess());
            assertFalse(directoryService.getAlumniDirectory().isSuccess());
            assertFalse(mentorshipService.getMyRequests().isSuccess());
            assertFalse(mentorshipService.getMyMentorshipConnections().isSuccess());
        }

        @Test
        @DisplayName("Logging out invalidates active session immediately")
        void testLogoutInvalidatesSession() {
            userService.login("student1@thapar.edu", "StudentP@ss1");
            assertTrue(UserSession.getInstance().isLoggedIn());

            userService.logout();

            assertFalse(UserSession.getInstance().isLoggedIn());
            assertThrows(SecurityException.class, () -> UserSession.getInstance().requireStudent());
            assertThrows(SecurityException.class, () -> UserSession.getInstance().getAuthenticatedUserId());
        }

        @Test
        @DisplayName("Stale user reference cannot be used after session logout")
        void testStaleUserReferenceCannotAccessService() {
            userService.login("student1@thapar.edu", "StudentP@ss1");
            User capturedUser = UserSession.getInstance().getCurrentUser().orElseThrow();
            assertEquals("Kabir Sharma", capturedUser.getFullName());

            // User logs out
            userService.logout();

            // Services read from UserSession singleton, so capturedUser reference cannot bypass security
            StudentProfileResult result = studentProfileService.viewMyProfile();
            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().toLowerCase().contains("must be logged in"));
        }
    }

    // =========================================================================
    // 4. UI Defense-in-Depth & Menu Restrictions
    // =========================================================================

    @Nested
    @DisplayName("UI Defense-in-Depth & Role-Specific Menus")
    class UIDefenseInDepth {

        @Test
        @DisplayName("Direct calls to StudentDashboardUI handlers throw SecurityException when unauthenticated")
        void testStudentDashboardDirectCallBypassPrevented() {
            ByteArrayOutputStream capture = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
            PrintStream ps = new PrintStream(capture);
            StudentProfileUI profileUI = new StudentProfileUI(studentProfileService, in, ps);
            StudentDashboardUI dashboard = new StudentDashboardUI(profileUI, userService, in, ps);

            assertThrows(SecurityException.class, dashboard::handleBrowseAlumni);
            assertThrows(SecurityException.class, dashboard::handleSearchAlumni);
            assertThrows(SecurityException.class, dashboard::handleFilterAlumni);
            assertThrows(SecurityException.class, dashboard::handleProfileSubMenu);
        }

        @Test
        @DisplayName("Direct call to AlumniDashboardUI profile handler throws SecurityException when unauthenticated")
        void testAlumniDashboardDirectCallBypassPrevented() {
            ByteArrayOutputStream capture = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
            PrintStream ps = new PrintStream(capture);
            AlumniProfileUI profileUI = new AlumniProfileUI(alumniProfileService, in, ps);
            AlumniDashboardUI dashboard = new AlumniDashboardUI(profileUI, mentorshipService, userService, in, ps);

            assertThrows(SecurityException.class, dashboard::handleProfileSubMenu);
        }

        @Test
        @DisplayName("Student Dashboard menu shows only student features and excludes alumni features")
        void testStudentMenuShowsOnlyAuthorizedOptions() {
            userService.login("student1@thapar.edu", "StudentP@ss1");

            ByteArrayOutputStream capture = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream("0\n".getBytes(StandardCharsets.UTF_8));
            PrintStream ps = new PrintStream(capture);
            StudentProfileUI profileUI = new StudentProfileUI(studentProfileService, in, ps);
            StudentDashboardUI dashboard = new StudentDashboardUI(profileUI, userService, in, ps);

            dashboard.show();

            String output = capture.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("STUDENT DASHBOARD"));
            assertTrue(output.contains("Browse Alumni"));
            assertTrue(output.contains("Search Alumni"));
            assertTrue(output.contains("Filter Alumni"));
            assertTrue(output.contains("My Mentorship Requests"));
            assertTrue(output.contains("My Mentorship Connections"));

            // Must NOT show alumni-specific header or prompt
            assertFalse(output.contains("ALUMNI DASHBOARD"));
            assertFalse(output.contains("View Incoming Mentorship Requests"));
        }

        @Test
        @DisplayName("Alumni Dashboard menu shows only alumni features and excludes student features")
        void testAlumniMenuShowsOnlyAuthorizedOptions() {
            userService.login("alumni1@thapar.edu", "AlumniP@ss1");

            ByteArrayOutputStream capture = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream("0\n".getBytes(StandardCharsets.UTF_8));
            PrintStream ps = new PrintStream(capture);
            AlumniProfileUI profileUI = new AlumniProfileUI(alumniProfileService, in, ps);
            AlumniDashboardUI dashboard = new AlumniDashboardUI(profileUI, mentorshipService, userService, in, ps);

            dashboard.show();

            String output = capture.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("ALUMNI DASHBOARD"));
            assertTrue(output.contains("View Mentorship Requests"));
            assertTrue(output.contains("My Mentees"));

            // Must NOT show student-specific header or directory options
            assertFalse(output.contains("STUDENT DASHBOARD"));
            assertFalse(output.contains("Browse Alumni"));
            assertFalse(output.contains("Search Alumni"));
        }
    }

    // =========================================================================
    // 5. Validation Testing & Error Understandability
    // =========================================================================

    @Nested
    @DisplayName("Validation Testing & Error Sanitization")
    class ValidationTesting {

        @Test
        @DisplayName("Invalid email formats produce clear validation error messages")
        void testInvalidEmailFormats() {
            String[] invalidEmails = {
                    "plainaddress",
                    "@missinguser.com",
                    "user@.com",
                    "user@domain",
                    "user name@thapar.edu"
            };

            for (String email : invalidEmails) {
                RegistrationRequest req = RegistrationRequest.forStudent(
                        "Test Student", email, "ValidP@ss123", "CS", 2026
                );
                req.setConfirmPassword("ValidP@ss123");
                RegistrationResult res = userService.register(req);

                assertFalse(res.isSuccess());
                assertTrue(res.getErrors().stream().anyMatch(e -> e.contains("Invalid email format")),
                        "Expected error for email: " + email);
            }
        }

        @Test
        @DisplayName("Weak passwords produce explicit, requirement-specific error messages")
        void testWeakPasswordRequirementMessages() {
            // Missing uppercase
            RegistrationRequest req1 = RegistrationRequest.forStudent("A", "a1@thapar.edu", "lowercase@1", "CS", 2026);
            req1.setConfirmPassword("lowercase@1");
            RegistrationResult res1 = userService.register(req1);
            assertTrue(res1.getErrors().stream().anyMatch(e -> e.contains("uppercase")));

            // Missing lowercase
            RegistrationRequest req2 = RegistrationRequest.forStudent("B", "a2@thapar.edu", "UPPERCASE@1", "CS", 2026);
            req2.setConfirmPassword("UPPERCASE@1");
            RegistrationResult res2 = userService.register(req2);
            assertTrue(res2.getErrors().stream().anyMatch(e -> e.contains("lowercase")));

            // Missing digit
            RegistrationRequest req3 = RegistrationRequest.forStudent("C", "a3@thapar.edu", "NoDigitsHere!", "CS", 2026);
            req3.setConfirmPassword("NoDigitsHere!");
            RegistrationResult res3 = userService.register(req3);
            assertTrue(res3.getErrors().stream().anyMatch(e -> e.contains("digit")));

            // Missing special character
            RegistrationRequest req4 = RegistrationRequest.forStudent("D", "a4@thapar.edu", "NoSpecial123", "CS", 2026);
            req4.setConfirmPassword("NoSpecial123");
            RegistrationResult res4 = userService.register(req4);
            assertTrue(res4.getErrors().stream().anyMatch(e -> e.contains("special character")));
        }

        @Test
        @DisplayName("Invalid graduation years outside 1950-2100 produce clear error")
        void testInvalidGraduationYearValidation() {
            RegistrationRequest reqLow = RegistrationRequest.forStudent("L", "low@thapar.edu", "ValidP@ss1", "CS", 1940);
            reqLow.setConfirmPassword("ValidP@ss1");
            RegistrationResult resLow = userService.register(reqLow);
            assertFalse(resLow.isSuccess());
            assertTrue(resLow.getErrors().stream().anyMatch(e -> e.contains("between 1950 and 2100")));

            RegistrationRequest reqHigh = RegistrationRequest.forStudent("H", "high@thapar.edu", "ValidP@ss1", "CS", 2150);
            reqHigh.setConfirmPassword("ValidP@ss1");
            RegistrationResult resHigh = userService.register(reqHigh);
            assertFalse(resHigh.isSuccess());
            assertTrue(resHigh.getErrors().stream().anyMatch(e -> e.contains("between 1950 and 2100")));
        }

        @Test
        @DisplayName("Invalid experience values produce clear errors")
        void testInvalidExperienceValues() {
            RegistrationRequest reqNeg = RegistrationRequest.forAlumni("N", "neg@thapar.edu", "ValidP@ss1", "CS", 2020);
            reqNeg.setConfirmPassword("ValidP@ss1");
            reqNeg.setYearsOfExperience(-5);
            RegistrationResult resNeg = userService.register(reqNeg);
            assertFalse(resNeg.isSuccess());
            assertTrue(resNeg.getErrors().stream().anyMatch(e -> e.contains("Years of experience cannot be negative")));

            RegistrationRequest reqOver = RegistrationRequest.forAlumni("O", "over@thapar.edu", "ValidP@ss1", "CS", 2020);
            reqOver.setConfirmPassword("ValidP@ss1");
            reqOver.setYearsOfExperience(75);
            RegistrationResult resOver = userService.register(reqOver);
            assertFalse(resOver.isSuccess());
            assertTrue(resOver.getErrors().stream().anyMatch(e -> e.contains("Years of experience cannot exceed 70")));
        }

        @Test
        @DisplayName("Invalid LinkedIn URLs produce clear format error without leaking internals")
        void testInvalidLinkedInUrls() {
            RegistrationRequest req = RegistrationRequest.forStudent("U", "url@thapar.edu", "ValidP@ss1", "CS", 2026);
            req.setConfirmPassword("ValidP@ss1");
            req.setLinkedinUrl("https://unrelated-domain.com/profile");

            RegistrationResult res = userService.register(req);
            assertFalse(res.isSuccess());
            assertTrue(res.getErrors().stream().anyMatch(e -> e.contains("Invalid LinkedIn URL format")));
        }

        @Test
        @DisplayName("Duplicate mentorship request rejected with understandable message")
        void testDuplicateMentorshipRequestValidation() {
            UserSession.getInstance().login(student1);

            // First request succeeds
            MentorshipRequestResult r1 = mentorshipService.sendRequest(alumni1.getId(), "Topic A", "Msg A");
            assertTrue(r1.isSuccess());

            // Duplicate pending request rejected
            MentorshipRequestResult r2 = mentorshipService.sendRequest(alumni1.getId(), "Topic A", "Msg A");
            assertFalse(r2.isSuccess());
            assertEquals("A pending mentorship request already exists.", r2.getMessage());
        }

        @Test
        @DisplayName("Invalid mentorship status transitions produce understandable error messages")
        void testInvalidStatusTransitionsProduceClearErrors() {
            MentorshipRequest req = new MentorshipRequest();
            req.setId(301);
            req.setStudentId(student1.getId());
            req.setAlumniId(alumni1.getId());
            req.setStatus(MentorshipStatus.PENDING);
            requestRepository.save(req);

            UserSession.getInstance().login(alumni1);

            // Accept request
            assertTrue(mentorshipService.acceptRequest(301).isSuccess());

            // Attempt to accept already accepted request
            MentorshipRequestResult duplicateAccept = mentorshipService.acceptRequest(301);
            assertFalse(duplicateAccept.isSuccess());
            assertTrue(duplicateAccept.getMessage().contains("Request is not pending (current status: ACCEPTED)"));

            // Attempt to reject already accepted request
            MentorshipRequestResult rejectAccepted = mentorshipService.rejectRequest(301);
            assertFalse(rejectAccepted.isSuccess());
            assertTrue(rejectAccepted.getMessage().contains("Request is not pending (current status: ACCEPTED)"));
        }
    }

    // =========================================================================
    // In-Memory Test Fakes
    // =========================================================================

    private static class InMemoryUserRepository implements UserRepository {
        private final Map<Integer, User> users = new HashMap<>();
        private int seq = 1;

        @Override public boolean existsByEmail(String email) {
            return users.values().stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(email));
        }
        @Override public Optional<User> findById(int id) { return Optional.ofNullable(users.get(id)); }
        @Override public Optional<User> findByEmail(String email) {
            return users.values().stream().filter(u -> u.getEmail().equalsIgnoreCase(email)).findFirst();
        }
        @Override public Optional<Student> findStudentById(int userId) {
            User u = users.get(userId);
            return (u instanceof Student s) ? Optional.of(s) : Optional.empty();
        }
        @Override public Optional<Alumni> findAlumniById(int userId) {
            User u = users.get(userId);
            return (u instanceof Alumni a) ? Optional.of(a) : Optional.empty();
        }
        @Override public Student saveStudent(Student s) {
            if (s.getId() == null) s.setId(seq++);
            users.put(s.getId(), s);
            return s;
        }
        @Override public Alumni saveAlumni(Alumni a) {
            if (a.getId() == null) a.setId(seq++);
            users.put(a.getId(), a);
            return a;
        }
        @Override public boolean deleteUserById(int userId) { return users.remove(userId) != null; }
    }

    private static class InMemoryStudentProfileRepository implements StudentProfileRepository {
        private final Map<Integer, Student> profiles = new HashMap<>();

        void save(Student s) { profiles.put(s.getId(), s); }

        @Override public Optional<Student> findProfileByUserId(int userId) { return Optional.ofNullable(profiles.get(userId)); }
        @Override public boolean updateProfile(Student student) { profiles.put(student.getId(), student); return true; }
        @Override public boolean createProfile(Student student) { profiles.put(student.getId(), student); return true; }
        @Override public boolean profileExistsForUser(int userId) { return profiles.containsKey(userId); }
    }

    private static class InMemoryAlumniProfileRepository implements AlumniProfileRepository {
        private final Map<Integer, Alumni> profiles = new HashMap<>();

        void save(Alumni a) { profiles.put(a.getId(), a); }

        @Override public List<Alumni> findAllAlumni() { return new ArrayList<>(profiles.values()); }
        @Override public Optional<Alumni> findProfileByUserId(int userId) { return Optional.ofNullable(profiles.get(userId)); }
        @Override public Optional<Alumni> findPublicProfileByUserId(int userId) { return Optional.ofNullable(profiles.get(userId)); }
        @Override public List<Alumni> searchAlumni(String keyword) {
            return profiles.values().stream()
                    .filter(a -> a.getFullName().toLowerCase().contains(keyword.toLowerCase())
                            || (a.getCurrentCompany() != null && a.getCurrentCompany().toLowerCase().contains(keyword.toLowerCase())))
                    .toList();
        }
        @Override public List<Alumni> filterAlumni(AlumniFilterCriteria criteria) {
            if (criteria == null) return new ArrayList<>(profiles.values());
            return profiles.values().stream().filter(criteria::matches).toList();
        }
        @Override public boolean updateProfile(Alumni a) { profiles.put(a.getId(), a); return true; }
        @Override public boolean createProfile(Alumni a) { profiles.put(a.getId(), a); return true; }
        @Override public boolean profileExistsForUser(int userId) { return profiles.containsKey(userId); }
    }

    private static class InMemoryMentorshipRequestRepository implements MentorshipRequestRepository {
        private final Map<Integer, MentorshipRequest> requests = new HashMap<>();
        private int seq = 1;

        void save(MentorshipRequest r) { requests.put(r.getId(), r); }

        @Override public int createRequest(MentorshipRequestDto dto) {
            int id = seq++;
            MentorshipRequest r = new MentorshipRequest();
            r.setId(id);
            r.setStudentId(dto.getStudentId());
            r.setAlumniId(dto.getAlumniId());
            r.setTopic(dto.getTopic());
            r.setMessage(dto.getMessage());
            r.setStatus(MentorshipStatus.PENDING);
            r.setRequestedAt(LocalDateTime.now());
            requests.put(id, r);
            return id;
        }

        @Override public boolean existsPending(int studentId, int alumniId) {
            return requests.values().stream().anyMatch(r ->
                    r.getStudentId() == studentId && r.getAlumniId() == alumniId && r.getStatus() == MentorshipStatus.PENDING);
        }

        @Override public boolean existsAccepted(int studentId, int alumniId) {
            return requests.values().stream().anyMatch(r ->
                    r.getStudentId() == studentId && r.getAlumniId() == alumniId && r.getStatus() == MentorshipStatus.ACCEPTED);
        }

        @Override public int countActiveMentees(int alumniId) {
            return (int) requests.values().stream()
                    .filter(r -> r.getAlumniId() == alumniId && r.getStatus() == MentorshipStatus.ACCEPTED)
                    .count();
        }

        @Override public boolean isAlumniAvailable(int alumniId) { return true; }

        @Override public Optional<MentorshipRequest> findRequestById(int requestId) {
            return Optional.ofNullable(requests.get(requestId));
        }

        @Override public boolean updateRequestStatus(int requestId, MentorshipStatus newStatus, LocalDateTime respondedAt) {
            MentorshipRequest req = requests.get(requestId);
            if (req != null) {
                req.setStatus(newStatus);
                req.setRespondedAt(respondedAt);
                return true;
            }
            return false;
        }

        @Override public List<MentorshipRequest> findRequestsByAlumniId(int alumniId) {
            return requests.values().stream().filter(r -> r.getAlumniId() == alumniId).toList();
        }

        @Override public List<MentorshipRequest> findRequestsByStudentId(int studentId) {
            return requests.values().stream().filter(r -> r.getStudentId() == studentId).toList();
        }

        @Override public List<MentorshipRequest> findAcceptedConnectionsByStudentId(int studentId) {
            return requests.values().stream()
                    .filter(r -> r.getStudentId() == studentId && r.getStatus() == MentorshipStatus.ACCEPTED)
                    .toList();
        }

        @Override public List<MentorshipRequest> findAcceptedConnectionsByAlumniId(int alumniId) {
            return requests.values().stream()
                    .filter(r -> r.getAlumniId() == alumniId && r.getStatus() == MentorshipStatus.ACCEPTED)
                    .toList();
        }
    }
}
