package com.alumconnect.service;

import com.alumconnect.model.Alumni;
import com.alumconnect.model.MentorshipRequest;
import com.alumconnect.model.MentorshipStatus;
import com.alumconnect.model.Role;
import com.alumconnect.model.Student;
import com.alumconnect.model.User;
import com.alumconnect.repository.MentorshipRequestRepository;
import com.alumconnect.repository.UserRepository;
import com.alumconnect.service.dto.MentorshipRequestDto;
import com.alumconnect.service.dto.MentorshipRequestResult;
import com.alumconnect.util.UserSession;
import com.alumconnect.web.WebViews;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Mentorship Request Timestamp & Timezone Integrity Tests")
class MentorshipRequestTimestampTest {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private FakeTimestampRequestRepository fakeRequestRepository;
    private FakeTimestampUserRepository fakeUserRepository;
    private MentorshipRequestService requestService;

    private Student student;
    private Alumni alumni;

    @BeforeEach
    void setUp() {
        UserSession.getInstance().logout();

        fakeRequestRepository = new FakeTimestampRequestRepository();
        fakeUserRepository = new FakeTimestampUserRepository();
        requestService = new MentorshipRequestService(fakeRequestRepository, fakeUserRepository);

        student = new Student();
        student.setId(101);
        student.setEmail("aman.verma@thapar.edu");
        student.setFullName("Aman Verma");
        student.setRole(Role.STUDENT);
        fakeUserRepository.saveUser(student);

        alumni = new Alumni();
        alumni.setId(202);
        alumni.setEmail("neha.gupta@google.com");
        alumni.setFullName("Neha Gupta");
        alumni.setRole(Role.ALUMNI);
        alumni.setAvailableForMentorship(true);
        alumni.setMaxMentees(5);
        fakeUserRepository.saveUser(alumni);
    }

    @AfterEach
    void tearDown() {
        UserSession.getInstance().logout();
    }

    @Test
    @DisplayName("Phase 4.1: Known timestamp matches between database, student sent view, and alumni received view")
    void testKnownTimestampMatchesBothSides() {
        LocalDateTime knownCreationTime = LocalDateTime.of(2026, 11, 12, 14, 30, 0);

        MentorshipRequest req = new MentorshipRequest(101, 202, "Cloud Architecture Guidance", "Need guidance", knownCreationTime);
        req.setId(1);
        req.setStatus(MentorshipStatus.PENDING);
        req.setStudentName("Aman Verma");
        req.setStudentEmail("aman.verma@thapar.edu");
        req.setAlumniName("Neha Gupta");
        fakeRequestRepository.save(req);

        // 1. Check Student Sent Requests
        UserSession.getInstance().login(student);
        MentorshipRequestResult studentRes = requestService.getMyRequests();
        assertTrue(studentRes.isSuccess());
        assertEquals(1, studentRes.getRequests().size());
        MentorshipRequest studentView = studentRes.getRequests().get(0);

        assertEquals(knownCreationTime, studentView.getRequestedAt(),
                "Student sent request must retain the exact creation timestamp");
        String studentFormatted = WebViews.formatDateTime(studentView.getRequestedAt());
        assertEquals("Nov 12, 2026 14:30", studentFormatted);

        // 2. Check Alumni Received Requests
        UserSession.getInstance().login(alumni);
        MentorshipRequestResult alumniRes = requestService.getReceivedRequests();
        assertTrue(alumniRes.isSuccess());
        assertEquals(1, alumniRes.getRequests().size());
        MentorshipRequest alumniView = alumniRes.getRequests().get(0);

        assertEquals(knownCreationTime, alumniView.getRequestedAt(),
                "Alumni received request must retain the exact creation timestamp");
        String alumniFormatted = WebViews.formatDateTime(alumniView.getRequestedAt());
        assertEquals("Nov 12, 2026 14:30", alumniFormatted);

        // 3. Both sides must be strictly identical
        assertEquals(studentFormatted, alumniFormatted,
                "Both student Sent Requests and alumni Received Requests must display the exact same timestamp");
    }

    @Test
    @DisplayName("Phase 4.2: Requests created shortly after midnight (00:05) must not roll back to previous day")
    void testMidnightBoundaryJustAfterMidnight() {
        // 00:05 AM IST on Nov 12
        LocalDateTime justAfterMidnight = LocalDateTime.of(2026, 11, 12, 0, 5, 0);

        MentorshipRequest req = new MentorshipRequest(101, 202, "Midnight Session", "Need guidance", justAfterMidnight);
        req.setId(2);
        req.setStatus(MentorshipStatus.PENDING);
        req.setStudentName("Aman Verma");
        req.setAlumniName("Neha Gupta");
        fakeRequestRepository.save(req);

        // Student View
        UserSession.getInstance().login(student);
        MentorshipRequest studentReq = requestService.getMyRequests().getRequests().get(0);
        String studentFormatted = WebViews.formatDateTime(studentReq.getRequestedAt());

        // Alumni View
        UserSession.getInstance().login(alumni);
        MentorshipRequest alumniReq = requestService.getReceivedRequests().getRequests().get(0);
        String alumniFormatted = WebViews.formatDateTime(alumniReq.getRequestedAt());

        assertEquals("Nov 12, 2026 00:05", studentFormatted);
        assertEquals("Nov 12, 2026 00:05", alumniFormatted);
        assertEquals(studentFormatted, alumniFormatted);
        assertTrue(studentFormatted.contains("Nov 12"),
                "Timestamp shortly after midnight must stay on Nov 12 (not roll back to Nov 11)");
    }

    @Test
    @DisplayName("Phase 4.3: Requests created shortly before midnight (23:55) must not advance to next day")
    void testMidnightBoundaryJustBeforeMidnight() {
        // 11:55 PM IST on Nov 12
        LocalDateTime justBeforeMidnight = LocalDateTime.of(2026, 11, 12, 23, 55, 0);

        MentorshipRequest req = new MentorshipRequest(101, 202, "Late Night Request", "Need guidance", justBeforeMidnight);
        req.setId(3);
        req.setStatus(MentorshipStatus.PENDING);
        req.setStudentName("Aman Verma");
        req.setAlumniName("Neha Gupta");
        fakeRequestRepository.save(req);

        // Student View
        UserSession.getInstance().login(student);
        MentorshipRequest studentReq = requestService.getMyRequests().getRequests().get(0);
        String studentFormatted = WebViews.formatDateTime(studentReq.getRequestedAt());

        // Alumni View
        UserSession.getInstance().login(alumni);
        MentorshipRequest alumniReq = requestService.getReceivedRequests().getRequests().get(0);
        String alumniFormatted = WebViews.formatDateTime(alumniReq.getRequestedAt());

        assertEquals("Nov 12, 2026 23:55", studentFormatted);
        assertEquals("Nov 12, 2026 23:55", alumniFormatted);
        assertEquals(studentFormatted, alumniFormatted);
        assertTrue(studentFormatted.contains("Nov 12"),
                "Timestamp shortly before midnight must stay on Nov 12 (not advance to Nov 13)");
    }

    @Test
    @DisplayName("Phase 4.4: Accepting or rejecting a request updates respondedAt without modifying original requestedAt")
    void testAcceptOrRejectPreservesOriginalRequestedAt() {
        LocalDateTime originalCreationTime = LocalDateTime.of(2026, 11, 12, 10, 0, 0);

        MentorshipRequest req = new MentorshipRequest(101, 202, "Interview Tips", "Help with tech interviews", originalCreationTime);
        req.setId(4);
        req.setStatus(MentorshipStatus.PENDING);
        fakeRequestRepository.save(req);

        UserSession.getInstance().login(alumni);

        // Accept request
        MentorshipRequestResult acceptRes = requestService.acceptRequest(4);
        assertTrue(acceptRes.isSuccess());

        MentorshipRequest accepted = fakeRequestRepository.requests.get(4);
        assertNotNull(accepted);
        assertEquals(MentorshipStatus.ACCEPTED, accepted.getStatus());
        assertEquals(originalCreationTime, accepted.getRequestedAt(),
                "Accepting request MUST NOT overwrite or alter original requestedAt creation timestamp");
        assertNotNull(accepted.getRespondedAt(), "respondedAt must be set upon acceptance");
    }

    @Test
    @DisplayName("Phase 4.5: sendRequest generates requestedAt timestamp once in Asia/Kolkata timezone")
    void testSendRequestGeneratesTimestampInAsiaKolkata() {
        UserSession.getInstance().login(student);

        LocalDateTime before = LocalDateTime.now(IST).minusSeconds(1);
        MentorshipRequestResult result = requestService.sendRequest(202, "Career Topic", "Hello mentor");
        LocalDateTime after = LocalDateTime.now(IST).plusSeconds(1);

        assertTrue(result.isSuccess());
        assertNotNull(result.getRequestId());

        MentorshipRequestDto dto = fakeRequestRepository.lastCreatedDto;
        assertNotNull(dto);
        assertNotNull(dto.getRequestedAt(), "MentorshipRequestDto must capture requestedAt timestamp");

        assertTrue(!dto.getRequestedAt().isBefore(before) && !dto.getRequestedAt().isAfter(after),
                "Creation timestamp must be within current Asia/Kolkata window: " + dto.getRequestedAt());
    }

    @Test
    @DisplayName("Phase 4.6: WebViews HTML rendering contains exact matching date and time strings")
    void testWebViewsHtmlRenderingMatchesOnBothDashboards() {
        LocalDateTime fixedTime = LocalDateTime.of(2026, 11, 12, 16, 45, 0);
        String expectedFormatted = "Nov 12, 2026 16:45";

        MentorshipRequest req = new MentorshipRequest(101, 202, "Distributed Systems", "Raft consensus", fixedTime);
        req.setId(10);
        req.setStatus(MentorshipStatus.PENDING);
        req.setStudentName("Aman Verma");
        req.setStudentEmail("aman.verma@thapar.edu");
        req.setAlumniName("Neha Gupta");

        // Render Student Dashboard HTML
        String studentHtml = WebViews.renderStudentDashboard(
                student, student, List.of(alumni), null, List.of(req), Collections.emptyList(), null, null
        );
        assertTrue(studentHtml.contains(expectedFormatted),
                "Student dashboard HTML must contain the formatted creation timestamp '" + expectedFormatted + "'");

        // Render Alumni Dashboard HTML
        String alumniHtml = WebViews.renderAlumniDashboard(
                alumni, alumni, List.of(req), Collections.emptyList(), null, null
        );
        assertTrue(alumniHtml.contains(expectedFormatted),
                "Alumni dashboard HTML must contain the formatted creation timestamp '" + expectedFormatted + "'");
    }

    // ── In-Memory Repository Fakes ───────────────────────────────────────────

    private static class FakeTimestampRequestRepository implements MentorshipRequestRepository {
        final Map<Integer, MentorshipRequest> requests = new HashMap<>();
        MentorshipRequestDto lastCreatedDto;
        int seq = 1;

        void save(MentorshipRequest req) {
            requests.put(req.getId(), req);
        }

        @Override
        public int createRequest(MentorshipRequestDto dto) throws SQLException {
            this.lastCreatedDto = dto;
            int id = seq++;
            LocalDateTime reqTime = dto.getRequestedAt() != null
                    ? dto.getRequestedAt()
                    : LocalDateTime.now(IST);
            MentorshipRequest req = new MentorshipRequest(dto.getStudentId(), dto.getAlumniId(), dto.getTopic(), dto.getMessage(), reqTime);
            req.setId(id);
            requests.put(id, req);
            return id;
        }

        @Override
        public boolean existsPending(int studentId, int alumniId) { return false; }
        @Override
        public boolean existsAccepted(int studentId, int alumniId) { return false; }
        @Override
        public int countActiveMentees(int alumniId) { return 0; }
        @Override
        public boolean isAlumniAvailable(int alumniId) { return true; }

        @Override
        public List<MentorshipRequest> findRequestsByAlumniId(int alumniId) {
            return requests.values().stream().filter(r -> r.getAlumniId() == alumniId).toList();
        }

        @Override
        public List<MentorshipRequest> findRequestsByStudentId(int studentId) {
            return requests.values().stream().filter(r -> r.getStudentId() == studentId).toList();
        }

        @Override
        public Optional<MentorshipRequest> findRequestById(int requestId) {
            return Optional.ofNullable(requests.get(requestId));
        }

        @Override
        public boolean updateRequestStatus(int requestId, MentorshipStatus newStatus, LocalDateTime respondedAt) {
            MentorshipRequest req = requests.get(requestId);
            if (req != null) {
                req.setStatus(newStatus);
                req.setRespondedAt(respondedAt);
                return true;
            }
            return false;
        }
    }

    private static class FakeTimestampUserRepository implements UserRepository {
        final Map<Integer, User> users = new HashMap<>();

        void saveUser(User u) { users.put(u.getId(), u); }

        @Override public boolean existsByEmail(String email) { return false; }
        @Override public Optional<User> findById(int id) { return Optional.ofNullable(users.get(id)); }
        @Override public Optional<User> findByEmail(String email) { return Optional.empty(); }
        @Override public Optional<Student> findStudentById(int userId) {
            User u = users.get(userId);
            return (u instanceof Student s) ? Optional.of(s) : Optional.empty();
        }
        @Override public Optional<Alumni> findAlumniById(int userId) {
            User u = users.get(userId);
            return (u instanceof Alumni a) ? Optional.of(a) : Optional.empty();
        }
        @Override public Student saveStudent(Student student) { users.put(student.getId(), student); return student; }
        @Override public Alumni saveAlumni(Alumni alumni) { users.put(alumni.getId(), alumni); return alumni; }
        @Override public boolean deleteUserById(int userId) { return users.remove(userId) != null; }
    }
}
