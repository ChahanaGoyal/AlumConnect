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
import com.alumconnect.service.dto.LoginResult;
import com.alumconnect.service.dto.MentorshipRequestDto;
import com.alumconnect.service.dto.MentorshipRequestResult;
import com.alumconnect.service.dto.RegistrationRequest;
import com.alumconnect.service.dto.RegistrationResult;
import com.alumconnect.service.dto.StudentProfileRequest;
import com.alumconnect.service.dto.StudentProfileResult;
import com.alumconnect.util.PasswordUtil;
import com.alumconnect.util.UserSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Prompt 20 - Validation & Exception Handling Tests")
class ValidationAndExceptionHandlingTest {

    private FakeUserRepo userRepo;
    private FakeStudentProfileRepo studentRepo;
    private FakeAlumniProfileRepo alumniRepo;
    private FakeMentorshipRequestRepo requestRepo;

    private UserService userService;
    private StudentProfileService studentProfileService;
    private AlumniProfileService alumniProfileService;
    private MentorshipRequestService requestService;
    private AlumniDirectoryService directoryService;

    private Student testStudent;
    private Alumni testAlumni;

    @BeforeEach
    void setUp() {
        UserSession.getInstance().logout();

        userRepo = new FakeUserRepo();
        studentRepo = new FakeStudentProfileRepo();
        alumniRepo = new FakeAlumniProfileRepo();
        requestRepo = new FakeMentorshipRequestRepo();

        userService = new UserService(userRepo);
        studentProfileService = new StudentProfileService(studentRepo);
        alumniProfileService = new AlumniProfileService(alumniRepo);
        requestService = new MentorshipRequestService(requestRepo, userRepo, alumniRepo);
        directoryService = new AlumniDirectoryService(alumniRepo);

        testStudent = new Student("student@example.com", PasswordUtil.hashPassword("ValidPass@123"), "Student One", "1234567890", "CS", 2026);
        testStudent.setId(10);
        testStudent.setActive(true);
        testStudent.setCurrentSemester(6);
        testStudent.setEnrollmentNumber("EN1001");
        userRepo.users.put(10, testStudent);
        studentRepo.students.put(10, testStudent);

        testAlumni = new Alumni("alumni@example.com", PasswordUtil.hashPassword("ValidPass@123"), "Alumni One", "0987654321", "CS", 2020);
        testAlumni.setId(20);
        testAlumni.setActive(true);
        testAlumni.setAvailableForMentorship(true);
        testAlumni.setMaxMentees(3);
        userRepo.users.put(20, testAlumni);
        alumniRepo.alumni.put(20, testAlumni);
    }

    @AfterEach
    void tearDown() {
        UserSession.getInstance().logout();
    }

    // ── 1. Registration & Authentication Validation ──────────────────────────

    @Test
    @DisplayName("Registration rejects null request and missing required fields")
    void testRegistrationMissingRequiredFields() {
        RegistrationResult nullResult = userService.register(null);
        assertFalse(nullResult.isSuccess());
        assertTrue(nullResult.getMessage().contains("null"));

        RegistrationRequest req = new RegistrationRequest();
        RegistrationResult emptyResult = userService.register(req);
        assertFalse(emptyResult.isSuccess());
        assertTrue(emptyResult.getErrors().size() >= 4);
    }

    @Test
    @DisplayName("Registration rejects invalid email format and duplicate email")
    void testRegistrationEmailValidation() {
        RegistrationRequest req = RegistrationRequest.forStudent("Student Name", "invalid-email", "ValidPass@123", "CS", 2026);
        RegistrationResult result = userService.register(req);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.toLowerCase().contains("email")));

        RegistrationRequest dupReq = RegistrationRequest.forStudent("Another Student", "student@example.com", "ValidPass@123", "CS", 2026);
        RegistrationResult dupResult = userService.register(dupReq);
        assertFalse(dupResult.isSuccess());
        assertTrue(dupResult.getMessage().toLowerCase().contains("already exists"));
    }

    @Test
    @DisplayName("Registration rejects passwords that exceed 128 chars or violate complexity")
    void testRegistrationPasswordRequirements() {
        RegistrationRequest longPwd = RegistrationRequest.forStudent("Student", "valid@example.com", "Aa1!" + "x".repeat(130), "CS", 2026);
        RegistrationResult longResult = userService.register(longPwd);
        assertFalse(longResult.isSuccess());
        assertTrue(longResult.getErrors().stream().anyMatch(e -> e.contains("128 characters")));

        RegistrationRequest weakPwd = RegistrationRequest.forStudent("Student", "valid@example.com", "weakpassword", "CS", 2026);
        RegistrationResult weakResult = userService.register(weakPwd);
        assertFalse(weakResult.isSuccess());
    }

    @Test
    @DisplayName("Registration validates student and alumni profile constraints including LinkedIn and bounds")
    void testRegistrationFieldBounds() {
        RegistrationRequest stuReq = RegistrationRequest.forStudent("Student", "stu@example.com", "ValidPass@123", "CS", 2026);
        stuReq.setLinkedinUrl("invalid-linkedin-format");
        RegistrationResult stuRes = userService.register(stuReq);
        assertFalse(stuRes.isSuccess());
        assertTrue(stuRes.getErrors().stream().anyMatch(e -> e.toLowerCase().contains("linkedin")));

        RegistrationRequest alumReq = RegistrationRequest.forAlumni("Alum", "alum@example.com", "ValidPass@123", "CS", 2020);
        alumReq.setYearsOfExperience(75); // exceeds 70
        alumReq.setMaxMentees(150); // exceeds 100
        RegistrationResult alumRes = userService.register(alumReq);
        assertFalse(alumRes.isSuccess());
        assertTrue(alumRes.getErrors().stream().anyMatch(e -> e.contains("70")));
        assertTrue(alumRes.getErrors().stream().anyMatch(e -> e.contains("100")));
    }

    @Test
    @DisplayName("Login rejects empty inputs and handles invalid credentials safely")
    void testLoginValidation() {
        LoginResult emptyEmail = userService.login("", "pass");
        assertFalse(emptyEmail.isSuccess());
        assertTrue(emptyEmail.getMessage().contains("Email is required"));

        LoginResult emptyPass = userService.login("user@example.com", "");
        assertFalse(emptyPass.isSuccess());
        assertTrue(emptyPass.getMessage().contains("Password is required"));

        LoginResult invalidCreds = userService.login("student@example.com", "WrongPassword!1");
        assertFalse(invalidCreds.isSuccess());
        assertEquals("Invalid email or password.", invalidCreds.getMessage());
    }

    @Test
    @DisplayName("User lookup handles invalid IDs and emails safely")
    void testUserLookupSafely() {
        assertTrue(userService.getUserById(0).isEmpty());
        assertTrue(userService.getUserById(-1).isEmpty());
        assertTrue(userService.getUserByEmail(null).isEmpty());
        assertTrue(userService.getUserByEmail("   ").isEmpty());
    }

    // ── 2. Student Profile Validation & Exceptions ────────────────────────────

    @Test
    @DisplayName("Student profile operations enforce authentication and role separation")
    void testStudentProfileAccessControl() {
        // Unauthenticated
        StudentProfileResult viewUnauth = studentProfileService.viewMyProfile();
        assertFalse(viewUnauth.isSuccess());
        assertTrue(viewUnauth.getMessage().contains("Access denied"));

        // Alumni accessing student profile service
        UserSession.getInstance().login(testAlumni);
        StudentProfileResult viewAlumni = studentProfileService.viewMyProfile();
        assertFalse(viewAlumni.isSuccess());
        assertTrue(viewAlumni.getMessage().contains("Only students"));
    }

    @Test
    @DisplayName("Student profile update validates LinkedIn format and required fields")
    void testStudentProfileUpdateValidation() {
        UserSession.getInstance().login(testStudent);

        StudentProfileResult nullReq = studentProfileService.updateMyProfile(null);
        assertFalse(nullReq.isSuccess());

        StudentProfileRequest badReq = new StudentProfileRequest();
        badReq.setDepartment(""); // missing required
        badReq.setGraduationYear(1900); // invalid
        badReq.setCurrentSemester(15); // invalid
        badReq.setLinkedinUrl("https://facebook.com/fake"); // invalid linkedin
        StudentProfileResult badRes = studentProfileService.updateMyProfile(badReq);
        assertFalse(badRes.isSuccess());
        assertTrue(badRes.getMessage().contains("Department is required"));
        assertTrue(badRes.getMessage().contains("Graduation year"));
        assertTrue(badRes.getMessage().contains("Current semester"));
        assertTrue(badRes.getMessage().contains("LinkedIn"));
    }

    @Test
    @DisplayName("Student profile create rejects if profile already exists")
    void testStudentProfileCreateAlreadyExists() {
        UserSession.getInstance().login(testStudent);
        StudentProfileRequest req = new StudentProfileRequest();
        req.setDepartment("CS");
        req.setGraduationYear(2026);

        StudentProfileResult res = studentProfileService.createMyProfile(req);
        assertFalse(res.isSuccess());
        assertTrue(res.getMessage().contains("already exists"));
    }

    // ── 3. Alumni Profile Validation & Exceptions ─────────────────────────────

    @Test
    @DisplayName("Alumni profile operations enforce authentication and role separation")
    void testAlumniProfileAccessControl() {
        AlumniProfileResult viewUnauth = alumniProfileService.viewMyProfile();
        assertFalse(viewUnauth.isSuccess());

        UserSession.getInstance().login(testStudent);
        AlumniProfileResult viewStudent = alumniProfileService.viewMyProfile();
        assertFalse(viewStudent.isSuccess());
        assertTrue(viewStudent.getMessage().contains("Only alumni"));
    }

    @Test
    @DisplayName("Alumni profile update validates experience, mentees, and LinkedIn bounds")
    void testAlumniProfileUpdateValidation() {
        UserSession.getInstance().login(testAlumni);

        AlumniProfileRequest req = new AlumniProfileRequest();
        req.setDepartment("CS");
        req.setGraduationYear(2020);
        req.setYearsOfExperience(99); // > 70
        req.setMaxMentees(200); // > 100
        req.setLinkedinUrl("not-a-linkedin-link");

        AlumniProfileResult res = alumniProfileService.updateMyProfile(req);
        assertFalse(res.isSuccess());
        assertTrue(res.getMessage().contains("70"));
        assertTrue(res.getMessage().contains("100"));
        assertTrue(res.getMessage().contains("LinkedIn"));
    }

    // ── 4. Directory & Filter Validation ──────────────────────────────────────

    @Test
    @DisplayName("Alumni search rejects empty or overly long keywords")
    void testAlumniSearchValidation() {
        UserSession.getInstance().login(testStudent);

        AlumniDirectoryResult emptyRes = directoryService.searchAlumni("   ");
        assertFalse(emptyRes.isSuccess());
        assertTrue(emptyRes.getMessage().contains("empty"));

        AlumniDirectoryResult longRes = directoryService.searchAlumni("x".repeat(101));
        assertFalse(longRes.isSuccess());
        assertTrue(longRes.getMessage().contains("100 characters"));
    }

    @Test
    @DisplayName("Alumni filter validates graduation year, experience, and field lengths")
    void testAlumniFilterValidation() {
        UserSession.getInstance().login(testStudent);

        AlumniFilterCriteria criteria = new AlumniFilterCriteria();
        criteria.setGraduationYear(1940); // invalid
        criteria.setMinExperience(80); // > 70
        criteria.setIndustry("x".repeat(105)); // > 100

        AlumniDirectoryResult res = directoryService.filterAlumni(criteria);
        assertFalse(res.isSuccess());
        assertTrue(res.getMessage().contains("Graduation year"));
        assertTrue(res.getMessage().contains("70"));
        assertTrue(res.getMessage().contains("Industry"));
    }

    @Test
    @DisplayName("Alumni view profile validates ID and not-found records")
    void testViewAlumniProfileValidation() {
        UserSession.getInstance().login(testStudent);

        AlumniDirectoryResult badId = directoryService.viewAlumniProfile(0);
        assertFalse(badId.isSuccess());
        assertTrue(badId.getMessage().contains("positive integer"));

        AlumniDirectoryResult notFound = directoryService.viewAlumniProfile(9999);
        assertFalse(notFound.isSuccess());
        assertTrue(notFound.getMessage().contains("not found"));
    }

    // ── 5. Mentorship Requests Validation & Lifecycle ─────────────────────────

    @Test
    @DisplayName("Mentorship request sending rejects invalid IDs, empty fields, and self-requests")
    void testSendMentorshipRequestValidation() {
        UserSession.getInstance().login(testStudent);

        // Invalid alumni ID
        MentorshipRequestResult badId = requestService.sendRequest(0, "Topic", "Msg");
        assertFalse(badId.isSuccess());
        assertTrue(badId.getMessage().contains("Invalid alumni"));

        // Empty topic
        MentorshipRequestResult emptyTopic = requestService.sendRequest(20, "", "Msg");
        assertFalse(emptyTopic.isSuccess());
        assertTrue(emptyTopic.getMessage().contains("topic is required"));

        // Overly long message
        MentorshipRequestResult longMsg = requestService.sendRequest(20, "Topic", "m".repeat(2001));
        assertFalse(longMsg.isSuccess());
        assertTrue(longMsg.getMessage().contains("2000 characters"));

        // Self request
        MentorshipRequestResult selfReq = requestService.sendRequest(testStudent.getId(), "Topic", "Msg");
        assertFalse(selfReq.isSuccess());
        assertTrue(selfReq.getMessage().contains("yourself"));
    }

    @Test
    @DisplayName("Mentorship request response validates ownership, status transition, and repeated actions")
    void testRespondToRequestValidation() {
        UserSession.getInstance().login(testAlumni);

        // Invalid request ID
        MentorshipRequestResult badId = requestService.acceptRequest(0);
        assertFalse(badId.isSuccess());
        assertTrue(badId.getMessage().contains("positive integer"));

        // Not found
        MentorshipRequestResult notFound = requestService.acceptRequest(9999);
        assertFalse(notFound.isSuccess());
        assertTrue(notFound.getMessage().contains("not found"));

        // Request belonging to another alumnus
        MentorshipRequest otherReq = new MentorshipRequest(10, 999, "Topic", "Msg");
        otherReq.setId(50);
        otherReq.setStatus(MentorshipStatus.PENDING);
        requestRepo.requests.put(50, otherReq);

        MentorshipRequestResult unauth = requestService.acceptRequest(50);
        assertFalse(unauth.isSuccess());
        assertTrue(unauth.getMessage().contains("Access denied"));

        // Already accepted (repeated attempt)
        MentorshipRequest myReq = new MentorshipRequest(10, 20, "Topic", "Msg");
        myReq.setId(51);
        myReq.setStatus(MentorshipStatus.ACCEPTED);
        requestRepo.requests.put(51, myReq);

        MentorshipRequestResult repeatAcc = requestService.acceptRequest(51);
        assertFalse(repeatAcc.isSuccess());
        assertTrue(repeatAcc.getMessage().contains("Request is not pending"));
    }

    @Test
    @DisplayName("Mentorship connections enforce student and alumni role separation")
    void testMentorshipConnectionsAccessControl() {
        // Unauthenticated
        MentorshipRequestResult unauth = requestService.getConnectionsForStudent(10);
        assertFalse(unauth.isSuccess());

        // Alumni accessing student connections
        UserSession.getInstance().login(testAlumni);
        MentorshipRequestResult alumAccStudent = requestService.getConnectionsForStudent(10);
        assertFalse(alumAccStudent.isSuccess());
        assertTrue(alumAccStudent.getMessage().contains("Only students"));

        // Student accessing alumni connections
        UserSession.getInstance().login(testStudent);
        MentorshipRequestResult stuAccAlum = requestService.getConnectionsForAlumni(20);
        assertFalse(stuAccAlum.isSuccess());
        assertTrue(stuAccAlum.getMessage().contains("Only alumni"));
    }

    // ── 6. Database Connection Failure & Exception Handling ───────────────────

    @Test
    @DisplayName("Database connection failures show user-friendly unavailable message")
    void testDatabaseConnectionFailureHandling() {
        userRepo.throwConnectionFailure = true;

        RegistrationRequest req = RegistrationRequest.forStudent("New Student", "new@example.com", "ValidPass@123", "CS", 2026);
        RegistrationResult regRes = userService.register(req);
        assertFalse(regRes.isSuccess());
        assertTrue(regRes.getMessage().contains("Database is currently unavailable. Please try again later."));

        LoginResult loginRes = userService.login("new@example.com", "ValidPass@123");
        assertFalse(loginRes.isSuccess());
        assertTrue(loginRes.getMessage().contains("Database is currently unavailable. Please try again later."));

        studentRepo.throwConnectionFailure = true;
        UserSession.getInstance().login(testStudent);
        StudentProfileResult stuRes = studentProfileService.viewMyProfile();
        assertFalse(stuRes.isSuccess());
        assertTrue(stuRes.getMessage().contains("Database is currently unavailable. Please try again later."));

        requestRepo.throwConnectionFailure = true;
        MentorshipRequestResult reqRes = requestService.sendRequest(20, "Topic", "Msg");
        assertFalse(reqRes.isSuccess());
        assertTrue(reqRes.getMessage().contains("Database is currently unavailable. Please try again later."));
    }

    @Test
    @DisplayName("UserSession security invariants")
    void testUserSessionInvariants() {
        assertThrows(IllegalArgumentException.class, () -> UserSession.getInstance().login(null));
        assertThrows(IllegalArgumentException.class, () -> UserSession.getInstance().requireRole(null));
        assertThrows(SecurityException.class, () -> UserSession.getInstance().requireRole(Role.STUDENT));

        UserSession.getInstance().login(testStudent);
        assertDoesNotThrow(() -> UserSession.getInstance().requireRole(Role.STUDENT));
        assertThrows(SecurityException.class, () -> UserSession.getInstance().requireRole(Role.ALUMNI));
    }

    // ── Fake Repositories for Unit Testing ────────────────────────────────────

    private static class FakeUserRepo implements UserRepository {
        final Map<Integer, User> users = new HashMap<>();
        boolean throwConnectionFailure = false;

        @Override
        public boolean existsByEmail(String email) throws SQLException {
            if (throwConnectionFailure) {
                throw new SQLException("Communications link failure", "08S01");
            }
            return users.values().stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(email));
        }

        @Override
        public Optional<User> findById(int id) throws SQLException {
            if (throwConnectionFailure) throw new SQLException("Communications link failure", "08S01");
            return Optional.ofNullable(users.get(id));
        }

        @Override
        public Optional<User> findByEmail(String email) throws SQLException {
            if (throwConnectionFailure) throw new SQLException("Communications link failure", "08S01");
            return users.values().stream().filter(u -> u.getEmail().equalsIgnoreCase(email)).findFirst();
        }

        @Override
        public Optional<Student> findStudentById(int userId) throws SQLException {
            User u = users.get(userId);
            return (u instanceof Student s) ? Optional.of(s) : Optional.empty();
        }

        @Override
        public Optional<Alumni> findAlumniById(int userId) throws SQLException {
            User u = users.get(userId);
            return (u instanceof Alumni a) ? Optional.of(a) : Optional.empty();
        }

        @Override
        public Student saveStudent(Student student) throws SQLException {
            if (throwConnectionFailure) throw new SQLException("Communications link failure", "08S01");
            student.setId(100);
            users.put(100, student);
            return student;
        }

        @Override
        public Alumni saveAlumni(Alumni alumni) throws SQLException {
            if (throwConnectionFailure) throw new SQLException("Communications link failure", "08S01");
            alumni.setId(200);
            users.put(200, alumni);
            return alumni;
        }

        @Override
        public boolean deleteUserById(int userId) {
            return users.remove(userId) != null;
        }
    }

    private static class FakeStudentProfileRepo implements StudentProfileRepository {
        final Map<Integer, Student> students = new HashMap<>();
        boolean throwConnectionFailure = false;

        @Override
        public Optional<Student> findProfileByUserId(int userId) throws SQLException {
            if (throwConnectionFailure) throw new SQLException("Communications link failure", "08S01");
            return Optional.ofNullable(students.get(userId));
        }

        @Override
        public boolean updateProfile(Student student) throws SQLException {
            if (throwConnectionFailure) throw new SQLException("Communications link failure", "08S01");
            students.put(student.getId(), student);
            return true;
        }

        @Override
        public boolean createProfile(Student student) throws SQLException {
            if (throwConnectionFailure) throw new SQLException("Communications link failure", "08S01");
            students.put(student.getId(), student);
            return true;
        }

        @Override
        public boolean profileExistsForUser(int userId) throws SQLException {
            if (throwConnectionFailure) throw new SQLException("Communications link failure", "08S01");
            return students.containsKey(userId);
        }
    }

    private static class FakeAlumniProfileRepo implements AlumniProfileRepository {
        final Map<Integer, Alumni> alumni = new HashMap<>();
        boolean throwConnectionFailure = false;

        @Override
        public Optional<Alumni> findProfileByUserId(int userId) throws SQLException {
            if (throwConnectionFailure) throw new SQLException("Communications link failure", "08S01");
            return Optional.ofNullable(alumni.get(userId));
        }

        @Override
        public List<Alumni> findAllAlumni() throws SQLException {
            if (throwConnectionFailure) throw new SQLException("Communications link failure", "08S01");
            return new ArrayList<>(alumni.values());
        }

        @Override
        public List<Alumni> searchAlumni(String keyword) throws SQLException {
            if (throwConnectionFailure) throw new SQLException("Communications link failure", "08S01");
            return alumni.values().stream()
                    .filter(a -> a.getFullName().toLowerCase().contains(keyword.toLowerCase()))
                    .toList();
        }

        @Override
        public List<Alumni> filterAlumni(AlumniFilterCriteria criteria) throws SQLException {
            if (throwConnectionFailure) throw new SQLException("Communications link failure", "08S01");
            return alumni.values().stream()
                    .filter(a -> criteria == null || criteria.matches(a))
                    .toList();
        }

        @Override
        public Optional<Alumni> findPublicProfileByUserId(int userId) throws SQLException {
            if (throwConnectionFailure) throw new SQLException("Communications link failure", "08S01");
            return Optional.ofNullable(alumni.get(userId));
        }

        @Override
        public boolean updateProfile(Alumni alum) throws SQLException {
            if (throwConnectionFailure) throw new SQLException("Communications link failure", "08S01");
            alumni.put(alum.getId(), alum);
            return true;
        }

        @Override
        public boolean createProfile(Alumni alum) throws SQLException {
            if (throwConnectionFailure) throw new SQLException("Communications link failure", "08S01");
            alumni.put(alum.getId(), alum);
            return true;
        }

        @Override
        public boolean profileExistsForUser(int userId) throws SQLException {
            if (throwConnectionFailure) throw new SQLException("Communications link failure", "08S01");
            return alumni.containsKey(userId);
        }
    }

    private static class FakeMentorshipRequestRepo implements MentorshipRequestRepository {
        final Map<Integer, MentorshipRequest> requests = new HashMap<>();
        boolean throwConnectionFailure = false;

        @Override
        public int createRequest(MentorshipRequestDto dto) throws SQLException {
            if (throwConnectionFailure) throw new SQLException("Communications link failure", "08S01");
            int id = requests.size() + 1;
            MentorshipRequest req = new MentorshipRequest(dto.getStudentId(), dto.getAlumniId(), dto.getTopic(), dto.getMessage());
            req.setId(id);
            req.setStatus(MentorshipStatus.PENDING);
            requests.put(id, req);
            return id;
        }

        @Override
        public boolean existsPending(int studentId, int alumniId) throws SQLException {
            if (throwConnectionFailure) throw new SQLException("Communications link failure", "08S01");
            return requests.values().stream().anyMatch(r -> r.getStudentId() == studentId && r.getAlumniId() == alumniId && r.getStatus() == MentorshipStatus.PENDING);
        }

        @Override
        public boolean existsAccepted(int studentId, int alumniId) throws SQLException {
            if (throwConnectionFailure) throw new SQLException("Communications link failure", "08S01");
            return requests.values().stream().anyMatch(r -> r.getStudentId() == studentId && r.getAlumniId() == alumniId && r.getStatus() == MentorshipStatus.ACCEPTED);
        }

        @Override
        public int countActiveMentees(int alumniId) throws SQLException {
            if (throwConnectionFailure) throw new SQLException("Communications link failure", "08S01");
            return (int) requests.values().stream().filter(r -> r.getAlumniId() == alumniId && (r.getStatus() == MentorshipStatus.PENDING || r.getStatus() == MentorshipStatus.ACCEPTED)).count();
        }

        @Override
        public boolean isAlumniAvailable(int alumniId) throws SQLException {
            if (throwConnectionFailure) throw new SQLException("Communications link failure", "08S01");
            return true;
        }

        @Override
        public List<MentorshipRequest> findRequestsByAlumniId(int alumniId) throws SQLException {
            if (throwConnectionFailure) throw new SQLException("Communications link failure", "08S01");
            return requests.values().stream().filter(r -> r.getAlumniId() == alumniId).toList();
        }

        @Override
        public List<MentorshipRequest> findRequestsByStudentId(int studentId) throws SQLException {
            if (throwConnectionFailure) throw new SQLException("Communications link failure", "08S01");
            return requests.values().stream().filter(r -> r.getStudentId() == studentId).toList();
        }

        @Override
        public List<MentorshipRequest> findAcceptedConnectionsByStudentId(int studentId) throws SQLException {
            if (throwConnectionFailure) throw new SQLException("Communications link failure", "08S01");
            return requests.values().stream().filter(r -> r.getStudentId() == studentId && r.getStatus() == MentorshipStatus.ACCEPTED).toList();
        }

        @Override
        public List<MentorshipRequest> findAcceptedConnectionsByAlumniId(int alumniId) throws SQLException {
            if (throwConnectionFailure) throw new SQLException("Communications link failure", "08S01");
            return requests.values().stream().filter(r -> r.getAlumniId() == alumniId && r.getStatus() == MentorshipStatus.ACCEPTED).toList();
        }

        @Override
        public Optional<MentorshipRequest> findRequestById(int requestId) throws SQLException {
            if (throwConnectionFailure) throw new SQLException("Communications link failure", "08S01");
            return Optional.ofNullable(requests.get(requestId));
        }

        @Override
        public boolean updateRequestStatus(int requestId, MentorshipStatus newStatus, LocalDateTime respondedAt) throws SQLException {
            if (throwConnectionFailure) throw new SQLException("Communications link failure", "08S01");
            MentorshipRequest req = requests.get(requestId);
            if (req != null) {
                req.setStatus(newStatus);
                req.setRespondedAt(respondedAt);
                return true;
            }
            return false;
        }
    }
}

