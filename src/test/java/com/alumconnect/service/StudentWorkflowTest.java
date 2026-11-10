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
import com.alumconnect.service.dto.MentorshipRequestDto;
import com.alumconnect.service.dto.MentorshipRequestResult;
import com.alumconnect.service.dto.StudentProfileRequest;
import com.alumconnect.service.dto.StudentProfileResult;
import com.alumconnect.util.PasswordUtil;
import com.alumconnect.util.UserSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
 * Comprehensive student workflow unit tests for Prompt 23B.
 *
 * <p>Covers:
 * <ul>
 *   <li>Creating student profile</li>
 *   <li>Updating student's own profile</li>
 *   <li>Browsing the alumni directory</li>
 *   <li>Searching for alumni by keyword</li>
 *   <li>Applying available alumni filters</li>
 *   <li>Viewing alumni profiles</li>
 *   <li>Sending mentorship requests</li>
 *   <li>Viewing sent mentorship requests and statuses</li>
 *   <li>Viewing accepted mentorship connections</li>
 *   <li>Ownership enforcement: students can only modify their own private profile info</li>
 * </ul>
 */
@DisplayName("Student Workflow Tests (Prompt 23B)")
class StudentWorkflowTest {

    private InMemoryUserRepository userRepository;
    private InMemoryStudentProfileRepository studentProfileRepository;
    private InMemoryAlumniProfileRepository alumniProfileRepository;
    private InMemoryMentorshipRequestRepository requestRepository;

    private StudentProfileService studentProfileService;
    private AlumniDirectoryService directoryService;
    private MentorshipRequestService mentorshipService;

    private Student student1;
    private Student student2;
    private Alumni alumni1;
    private Alumni alumni2;
    private Alumni alumniFullCapacity;
    private Alumni alumniUnavailable;

    @BeforeEach
    void setUp() throws SQLException {
        UserSession.getInstance().logout();

        userRepository = new InMemoryUserRepository();
        studentProfileRepository = new InMemoryStudentProfileRepository();
        alumniProfileRepository = new InMemoryAlumniProfileRepository();
        requestRepository = new InMemoryMentorshipRequestRepository();

        studentProfileService = new StudentProfileService(studentProfileRepository);
        directoryService = new AlumniDirectoryService(alumniProfileRepository);
        mentorshipService = new MentorshipRequestService(requestRepository, userRepository);

        // Seed Student 1
        student1 = new Student(
                "student1@thapar.edu",
                PasswordUtil.hashPassword("StudentP@ss1"),
                "Aman Sharma",
                "+91-9876543210",
                "Computer Science",
                2026
        );
        student1.setCurrentSemester(6);
        student1.setEnrollmentNumber("102203111");
        student1.setSkills("Java, Spring, MySQL");
        student1.setInterests("Backend Architecture, Cloud");
        student1.setBio("Final year CS student passionate about backend systems.");
        student1.setLinkedinUrl("https://linkedin.com/in/amansharma");
        student1.setGithubUrl("https://github.com/amansharma");
        userRepository.saveStudent(student1);
        studentProfileRepository.save(student1);

        // Seed Student 2 (for cross-user testing)
        student2 = new Student(
                "student2@thapar.edu",
                PasswordUtil.hashPassword("StudentP@ss2"),
                "Pooja Verma",
                "+91-9876543211",
                "Electrical",
                2027
        );
        student2.setCurrentSemester(4);
        student2.setEnrollmentNumber("102203222");
        userRepository.saveStudent(student2);
        studentProfileRepository.save(student2);

        // Seed Alumni 1: Available, experienced Google engineer
        alumni1 = new Alumni(
                "alumni1@thapar.edu",
                PasswordUtil.hashPassword("AlumniP@ss1"),
                "Rohan Gupta",
                "+91-9811001122",
                "Computer Science",
                2019
        );
        alumni1.setCurrentCompany("Google");
        alumni1.setCurrentRole("Senior Software Engineer");
        alumni1.setIndustry("Technology");
        alumni1.setYearsOfExperience(7);
        alumni1.setDegree("B.Tech Computer Science");
        alumni1.setExpertiseAreas("Distributed Systems, Kubernetes, Go");
        alumni1.setBio("Passionate mentor with tech lead experience.");
        alumni1.setLinkedinUrl("https://linkedin.com/in/rohangupta");
        alumni1.setAvailableForMentorship(true);
        alumni1.setMaxMentees(3);
        userRepository.saveAlumni(alumni1);
        alumniProfileRepository.save(alumni1);

        // Seed Alumni 2: Available, Microsoft engineer
        alumni2 = new Alumni(
                "alumni2@thapar.edu",
                PasswordUtil.hashPassword("AlumniP@ss2"),
                "Neha Kapoor",
                "+91-9811003344",
                "Electronics",
                2021
        );
        alumni2.setCurrentCompany("Microsoft");
        alumni2.setCurrentRole("Software Engineer II");
        alumni2.setIndustry("Technology");
        alumni2.setYearsOfExperience(4);
        alumni2.setDegree("B.Tech Electronics");
        alumni2.setExpertiseAreas("Azure, C#, Microservices");
        alumni2.setAvailableForMentorship(true);
        alumni2.setMaxMentees(2);
        userRepository.saveAlumni(alumni2);
        alumniProfileRepository.save(alumni2);

        // Seed Alumni at full mentee capacity (maxMentees = 1, activeMentees = 1)
        alumniFullCapacity = new Alumni(
                "alumni.full@thapar.edu",
                PasswordUtil.hashPassword("AlumniP@ssFull"),
                "Full Capacity Alum",
                "+91-9811005566",
                "Computer Science",
                2018
        );
        alumniFullCapacity.setCurrentCompany("Meta");
        alumniFullCapacity.setYearsOfExperience(8);
        alumniFullCapacity.setAvailableForMentorship(true);
        alumniFullCapacity.setMaxMentees(1);
        userRepository.saveAlumni(alumniFullCapacity);
        alumniProfileRepository.save(alumniFullCapacity);
        requestRepository.setActiveMentees(alumniFullCapacity.getId(), 1);

        // Seed Alumni who is NOT available for mentorship
        alumniUnavailable = new Alumni(
                "alumni.unavailable@thapar.edu",
                PasswordUtil.hashPassword("AlumniP@ssUnav"),
                "Busy Alum",
                "+91-9811007788",
                "Mechanical",
                2017
        );
        alumniUnavailable.setCurrentCompany("Tesla");
        alumniUnavailable.setYearsOfExperience(9);
        alumniUnavailable.setAvailableForMentorship(false);
        userRepository.saveAlumni(alumniUnavailable);
        alumniProfileRepository.save(alumniUnavailable);
        requestRepository.setAlumniUnavailable(alumniUnavailable.getId());
    }

    @AfterEach
    void tearDown() {
        UserSession.getInstance().logout();
    }

    // =========================================================================
    // 1. Student Profile Creation & Updating
    // =========================================================================

    @Nested
    @DisplayName("Student Profile Management")
    class ProfileManagementTests {

        @Test
        @DisplayName("Student can view their own profile")
        void testStudentCanViewOwnProfile() {
            UserSession.getInstance().login(student1);

            StudentProfileResult result = studentProfileService.viewMyProfile();

            assertTrue(result.isSuccess());
            assertNotNull(result.getStudent());
            assertEquals("Aman Sharma", result.getStudent().getFullName());
            assertEquals("Computer Science", result.getStudent().getDepartment());
            assertEquals("102203111", result.getStudent().getEnrollmentNumber());
        }

        @Test
        @DisplayName("Student can update their own profile with valid data")
        void testStudentCanUpdateOwnProfile() {
            UserSession.getInstance().login(student1);

            StudentProfileRequest req = new StudentProfileRequest();
            req.setDepartment("Computer Science & Engineering");
            req.setGraduationYear(2026);
            req.setCurrentSemester(7);
            req.setEnrollmentNumber("102203111");
            req.setSkills("Java 17, Spring Boot, Docker, Kubernetes");
            req.setInterests("Distributed Systems, Cloud Architecture");
            req.setBio("Updated bio: aspiring backend architect");
            req.setLinkedinUrl("https://linkedin.com/in/amansharma-updated");
            req.setGithubUrl("https://github.com/amansharma-updated");

            StudentProfileResult result = studentProfileService.updateMyProfile(req);

            assertTrue(result.isSuccess(), "Profile update should succeed: " + result.getMessage());
            assertNotNull(result.getStudent());
            assertEquals(7, result.getStudent().getCurrentSemester());
            assertEquals("Computer Science & Engineering", result.getStudent().getDepartment());
            assertEquals("Java 17, Spring Boot, Docker, Kubernetes", result.getStudent().getSkills());
        }

        @Test
        @DisplayName("Student cannot update another student's profile (Ownership enforcement)")
        void testStudentCannotUpdateAnotherStudentProfile() {
            UserSession.getInstance().login(student1); // Logged in as student1 (id=1)

            StudentProfileRequest req = new StudentProfileRequest();
            req.setDepartment("Hacked Department");
            req.setGraduationYear(2027);

            // Attempting to update student2's profile (id=2)
            StudentProfileResult result = studentProfileService.updateProfile(student2.getId(), req);

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().toLowerCase().contains("cannot modify another student's profile"));
        }

        @Test
        @DisplayName("Student profile update with invalid semester fails validation")
        void testUpdateProfileInvalidSemester() {
            UserSession.getInstance().login(student1);

            StudentProfileRequest req = new StudentProfileRequest();
            req.setDepartment("Computer Science");
            req.setGraduationYear(2026);
            req.setCurrentSemester(15); // Invalid: semester > 12

            StudentProfileResult result = studentProfileService.updateMyProfile(req);

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().toLowerCase().contains("semester must be between 1 and 12"));
        }

        @Test
        @DisplayName("Student profile update with duplicate enrollment number is rejected")
        void testUpdateProfileDuplicateEnrollmentNumber() {
            UserSession.getInstance().login(student1);

            studentProfileRepository.setThrowDuplicateEnrollment(true);

            StudentProfileRequest req = new StudentProfileRequest();
            req.setDepartment("Computer Science");
            req.setGraduationYear(2026);
            req.setEnrollmentNumber(student2.getEnrollmentNumber());

            StudentProfileResult result = studentProfileService.updateMyProfile(req);

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().toLowerCase().contains("already registered"));
        }
    }

    // =========================================================================
    // 2. Alumni Directory Browsing, Searching & Filtering
    // =========================================================================

    @Nested
    @DisplayName("Alumni Directory Browsing, Searching & Filtering")
    class DirectoryBrowsingSearchingFilteringTests {

        @Test
        @DisplayName("Authenticated student can browse alumni directory")
        void testBrowseAlumniDirectory() {
            UserSession.getInstance().login(student1);

            AlumniDirectoryResult result = directoryService.getAlumniDirectory();

            assertTrue(result.isSuccess());
            assertNotNull(result.getAlumniList());
            assertTrue(result.getAlumniList().size() >= 2);
        }

        @Test
        @DisplayName("Alumni directory keyword search matches name, company, and skills case-insensitively")
        void testSearchAlumniKeywords() {
            UserSession.getInstance().login(student1);

            // Search by company (Google)
            AlumniDirectoryResult r1 = directoryService.searchAlumni("google");
            assertTrue(r1.isSuccess());
            assertEquals(1, r1.getAlumniList().size());
            assertEquals("Rohan Gupta", r1.getAlumniList().get(0).getFullName());

            // Search by name (Neha)
            AlumniDirectoryResult r2 = directoryService.searchAlumni("NEHA");
            assertTrue(r2.isSuccess());
            assertEquals(1, r2.getAlumniList().size());
            assertEquals("Neha Kapoor", r2.getAlumniList().get(0).getFullName());

            // Search by skill (Kubernetes)
            AlumniDirectoryResult r3 = directoryService.searchAlumni("kubernetes");
            assertTrue(r3.isSuccess());
            assertEquals(1, r3.getAlumniList().size());
            assertEquals("Rohan Gupta", r3.getAlumniList().get(0).getFullName());
        }

        @Test
        @DisplayName("Search with no matching alumni returns empty list gracefully")
        void testSearchNoMatches() {
            UserSession.getInstance().login(student1);

            AlumniDirectoryResult result = directoryService.searchAlumni("NonExistentCompanyOrSkillXYZ");

            assertTrue(result.isSuccess());
            assertTrue(result.getAlumniList().isEmpty());
            assertTrue(result.getMessage().contains("No alumni found"));
        }

        @Test
        @DisplayName("Search with empty or whitespace-only keyword is rejected")
        void testSearchEmptyKeywordRejected() {
            UserSession.getInstance().login(student1);

            AlumniDirectoryResult r1 = directoryService.searchAlumni("");
            assertFalse(r1.isSuccess());
            assertTrue(r1.getMessage().contains("cannot be empty"));

            AlumniDirectoryResult r2 = directoryService.searchAlumni("   ");
            assertFalse(r2.isSuccess());
            assertTrue(r2.getMessage().contains("cannot be empty"));
        }

        @Test
        @DisplayName("Filter alumni by department and minimum experience")
        void testFilterAlumniDepartmentAndExperience() {
            UserSession.getInstance().login(student1);

            AlumniFilterCriteria criteria = new AlumniFilterCriteria();
            criteria.setDepartment("Computer Science");
            criteria.setMinExperience(5); // Rohan Gupta (7) and Full Capacity Alum (8) match

            AlumniDirectoryResult result = directoryService.filterAlumni(criteria);

            assertTrue(result.isSuccess());
            assertEquals(2, result.getAlumniList().size());
            assertTrue(result.getAlumniList().stream().anyMatch(a -> a.getFullName().equals("Rohan Gupta")));
            assertTrue(result.getAlumniList().stream().anyMatch(a -> a.getFullName().equals("Full Capacity Alum")));
        }

        @Test
        @DisplayName("Filter alumni by mentorship availability")
        void testFilterAlumniMentorshipAvailability() {
            UserSession.getInstance().login(student1);

            AlumniFilterCriteria criteria = new AlumniFilterCriteria();
            criteria.setMentorshipAvailable(false);

            AlumniDirectoryResult result = directoryService.filterAlumni(criteria);

            assertTrue(result.isSuccess());
            assertEquals(1, result.getAlumniList().size());
            assertEquals("Busy Alum", result.getAlumniList().get(0).getFullName());
        }

        @Test
        @DisplayName("Filter criteria with invalid ranges is rejected")
        void testFilterInvalidCriteriaRejected() {
            UserSession.getInstance().login(student1);

            AlumniFilterCriteria criteria = new AlumniFilterCriteria();
            criteria.setMinExperience(-5); // Negative experience is invalid

            AlumniDirectoryResult result = directoryService.filterAlumni(criteria);

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().contains("cannot be negative"));
        }
    }

    // =========================================================================
    // 3. Viewing Alumni Profiles
    // =========================================================================

    @Nested
    @DisplayName("Viewing Individual Alumni Profiles")
    class ViewAlumniProfileTests {

        @Test
        @DisplayName("Student can view public profile of an alumni")
        void testStudentCanViewAlumniProfile() {
            UserSession.getInstance().login(student1);

            AlumniDirectoryResult result = directoryService.viewAlumniProfile(alumni1.getId());

            assertTrue(result.isSuccess());
            assertEquals(1, result.getAlumniList().size());
            Alumni profile = result.getAlumniList().get(0);
            assertEquals("Rohan Gupta", profile.getFullName());
            assertEquals("Google", profile.getCurrentCompany());
            assertEquals("Senior Software Engineer", profile.getCurrentRole());
            assertTrue(profile.isAvailableForMentorship());
        }

        @Test
        @DisplayName("Viewing alumni profile with non-existent ID fails gracefully")
        void testViewNonExistentAlumniProfile() {
            UserSession.getInstance().login(student1);

            AlumniDirectoryResult result = directoryService.viewAlumniProfile(9999);

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().contains("not found"));
        }

        @Test
        @DisplayName("Viewing alumni profile with invalid non-positive ID is rejected")
        void testViewInvalidAlumniIdRejected() {
            UserSession.getInstance().login(student1);

            AlumniDirectoryResult result = directoryService.viewAlumniProfile(-1);

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().contains("positive integer"));
        }
    }

    // =========================================================================
    // 4. Mentorship Requests (Sending, Tracking, Connections)
    // =========================================================================

    @Nested
    @DisplayName("Mentorship Request Lifecycle from Student Perspective")
    class MentorshipRequestTests {

        @Test
        @DisplayName("Student can send mentorship request to available alumni")
        void testSendMentorshipRequestSuccess() {
            UserSession.getInstance().login(student1);

            MentorshipRequestResult result = mentorshipService.sendRequest(
                    alumni1.getId(),
                    "Distributed Systems Guidance",
                    "Hi Rohan, I am looking for guidance on designing scalable microservices."
            );

            assertTrue(result.isSuccess(), "Request should succeed: " + result.getMessage());
            assertTrue(result.getRequestId() > 0);
        }

        @Test
        @DisplayName("Student cannot send mentorship request with blank topic or message")
        void testSendRequestBlankTopicOrMessage() {
            UserSession.getInstance().login(student1);

            MentorshipRequestResult r1 = mentorshipService.sendRequest(alumni1.getId(), "   ", "Valid Message");
            assertFalse(r1.isSuccess());
            assertTrue(r1.getMessage().contains("topic is required"));

            MentorshipRequestResult r2 = mentorshipService.sendRequest(alumni1.getId(), "Valid Topic", "   ");
            assertFalse(r2.isSuccess());
            assertTrue(r2.getMessage().contains("message is required"));
        }

        @Test
        @DisplayName("Student cannot send request to alumni who is not available for mentorship")
        void testSendRequestToUnavailableAlumni() {
            UserSession.getInstance().login(student1);

            MentorshipRequestResult result = mentorshipService.sendRequest(
                    alumniUnavailable.getId(),
                    "Career Guidance",
                    "I want to ask about manufacturing."
            );

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().contains("not available for mentorship"));
        }

        @Test
        @DisplayName("Student cannot send request to alumni at maximum mentee capacity")
        void testSendRequestToFullCapacityAlumni() {
            UserSession.getInstance().login(student1);

            MentorshipRequestResult result = mentorshipService.sendRequest(
                    alumniFullCapacity.getId(),
                    "Mentorship Topic",
                    "Looking for guidance."
            );

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().contains("maximum number of mentees"));
        }

        @Test
        @DisplayName("Duplicate pending mentorship request to the same alumni is rejected")
        void testDuplicatePendingRequestRejected() {
            UserSession.getInstance().login(student1);

            // First request succeeds
            MentorshipRequestResult r1 = mentorshipService.sendRequest(
                    alumni2.getId(), "Azure Cloud", "Need help with cloud certs."
            );
            assertTrue(r1.isSuccess());

            // Second request to same alumni must be rejected
            MentorshipRequestResult r2 = mentorshipService.sendRequest(
                    alumni2.getId(), "Azure Cloud Again", "Another message."
            );
            assertFalse(r2.isSuccess());
            assertTrue(r2.getMessage().contains("pending mentorship request already exists"));
        }

        @Test
        @DisplayName("Student can view their own sent mentorship requests and statuses")
        void testStudentCanViewOwnRequestsAndStatuses() {
            UserSession.getInstance().login(student1);

            // Send request to alumni1
            mentorshipService.sendRequest(alumni1.getId(), "Topic 1", "Message 1");
            // Send request to alumni2
            mentorshipService.sendRequest(alumni2.getId(), "Topic 2", "Message 2");

            MentorshipRequestResult result = mentorshipService.getMyRequests();

            assertTrue(result.isSuccess());
            assertNotNull(result.getRequests());
            assertEquals(2, result.getRequests().size());
            assertEquals(MentorshipStatus.PENDING, result.getRequests().get(0).getStatus());
            assertEquals(MentorshipStatus.PENDING, result.getRequests().get(1).getStatus());
        }

        @Test
        @DisplayName("Student cannot view another student's mentorship requests")
        void testStudentCannotViewAnotherStudentRequests() {
            UserSession.getInstance().login(student1);

            // Student 1 attempts to query student 2's requests
            MentorshipRequestResult result = mentorshipService.getRequestsForStudent(student2.getId());

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().toLowerCase().contains("cannot view mentorship requests for another student"));
        }

        @Test
        @DisplayName("Student can view only their accepted mentorship connections")
        void testStudentCanViewAcceptedConnections() {
            UserSession.getInstance().login(student1);

            // Seed accepted connection for student1 with alumni1
            MentorshipRequest acceptedReq = new MentorshipRequest();
            acceptedReq.setId(101);
            acceptedReq.setStudentId(student1.getId());
            acceptedReq.setAlumniId(alumni1.getId());
            acceptedReq.setTopic("Active Architecture Mentorship");
            acceptedReq.setMessage("Working together");
            acceptedReq.setStatus(MentorshipStatus.ACCEPTED);
            acceptedReq.setRequestedAt(LocalDateTime.now().minusDays(5));
            acceptedReq.setRespondedAt(LocalDateTime.now().minusDays(4));
            requestRepository.save(acceptedReq);

            // Seed pending request (should not appear in connections)
            MentorshipRequest pendingReq = new MentorshipRequest();
            pendingReq.setId(102);
            pendingReq.setStudentId(student1.getId());
            pendingReq.setAlumniId(alumni2.getId());
            pendingReq.setTopic("Pending Topic");
            pendingReq.setMessage("Waiting");
            pendingReq.setStatus(MentorshipStatus.PENDING);
            requestRepository.save(pendingReq);

            MentorshipRequestResult result = mentorshipService.getMyMentorshipConnections();

            assertTrue(result.isSuccess());
            assertNotNull(result.getRequests());
            assertEquals(1, result.getRequests().size());
            assertEquals(101, result.getRequests().get(0).getId());
            assertEquals(MentorshipStatus.ACCEPTED, result.getRequests().get(0).getStatus());
        }

        @Test
        @DisplayName("Student cannot view another student's accepted connections")
        void testStudentCannotViewAnotherStudentConnections() {
            UserSession.getInstance().login(student1);

            // Student 1 attempts to view student 2's connections
            MentorshipRequestResult result = mentorshipService.getConnectionsForStudent(student2.getId());

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().toLowerCase().contains("cannot view mentorship connections for another student"));
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
        private boolean throwDuplicateEnrollment = false;

        void save(Student s) { profiles.put(s.getId(), s); }
        void setThrowDuplicateEnrollment(boolean val) { this.throwDuplicateEnrollment = val; }

        @Override public Optional<Student> findProfileByUserId(int userId) {
            return Optional.ofNullable(profiles.get(userId));
        }
        @Override public boolean updateProfile(Student student) throws SQLException {
            if (throwDuplicateEnrollment) {
                throw new SQLException("Duplicate entry '102203222' for key 'student_profiles.enrollment_number'");
            }
            profiles.put(student.getId(), student);
            return true;
        }
        @Override public boolean createProfile(Student student) throws SQLException {
            profiles.put(student.getId(), student);
            return true;
        }
        @Override public boolean profileExistsForUser(int userId) {
            return profiles.containsKey(userId);
        }
    }

    private static class InMemoryAlumniProfileRepository implements AlumniProfileRepository {
        private final Map<Integer, Alumni> profiles = new HashMap<>();

        void save(Alumni a) { profiles.put(a.getId(), a); }

        @Override public List<Alumni> findAllAlumni() { return new ArrayList<>(profiles.values()); }
        @Override public Optional<Alumni> findProfileByUserId(int userId) { return Optional.ofNullable(profiles.get(userId)); }
        @Override public Optional<Alumni> findPublicProfileByUserId(int userId) { return Optional.ofNullable(profiles.get(userId)); }
        @Override public List<Alumni> searchAlumni(String keyword) {
            String lower = keyword.toLowerCase();
            return profiles.values().stream()
                    .filter(a -> (a.getFullName() != null && a.getFullName().toLowerCase().contains(lower))
                            || (a.getCurrentCompany() != null && a.getCurrentCompany().toLowerCase().contains(lower))
                            || (a.getCurrentRole() != null && a.getCurrentRole().toLowerCase().contains(lower))
                            || (a.getIndustry() != null && a.getIndustry().toLowerCase().contains(lower))
                            || (a.getExpertiseAreas() != null && a.getExpertiseAreas().toLowerCase().contains(lower))
                            || (a.getDepartment() != null && a.getDepartment().toLowerCase().contains(lower)))
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
        private final Map<Integer, Integer> activeMenteeCounts = new HashMap<>();
        private final java.util.Set<Integer> unavailableAlumni = new java.util.HashSet<>();
        private int seq = 1;

        void save(MentorshipRequest r) { requests.put(r.getId(), r); }
        void setActiveMentees(int alumniId, int count) { activeMenteeCounts.put(alumniId, count); }
        void setAlumniUnavailable(int alumniId) { unavailableAlumni.add(alumniId); }

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
            return activeMenteeCounts.getOrDefault(alumniId,
                    (int) requests.values().stream().filter(r -> r.getAlumniId() == alumniId && r.getStatus() == MentorshipStatus.ACCEPTED).count());
        }

        @Override public boolean isAlumniAvailable(int alumniId) {
            return !unavailableAlumni.contains(alumniId);
        }

        @Override public List<MentorshipRequest> findRequestsByStudentId(int studentId) {
            return requests.values().stream().filter(r -> r.getStudentId() == studentId).toList();
        }

        @Override public List<MentorshipRequest> findAcceptedConnectionsByStudentId(int studentId) {
            return requests.values().stream().filter(r -> r.getStudentId() == studentId && r.getStatus() == MentorshipStatus.ACCEPTED).toList();
        }
    }
}
