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
import com.alumconnect.service.dto.AlumniProfileRequest;
import com.alumconnect.service.dto.AlumniProfileResult;
import com.alumconnect.service.dto.MentorshipRequestDto;
import com.alumconnect.service.dto.MentorshipRequestResult;
import com.alumconnect.service.dto.StudentProfileRequest;
import com.alumconnect.service.dto.StudentProfileResult;
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
 * Comprehensive service-layer Role-Based Access Control tests for Prompt 21.
 *
 * <p>Covers:
 * <ol>
 *   <li>Unauthenticated access to all protected service operations.</li>
 *   <li>Students accessing student-permitted operations (profile, directory, mentorship).</li>
 *   <li>Students denied alumni-only operations.</li>
 *   <li>Alumni accessing alumni-permitted operations (profile, request management).</li>
 *   <li>Alumni denied student-only operations.</li>
 *   <li>Cross-user profile access and modification.</li>
 *   <li>Cross-user mentorship request and connection access.</li>
 *   <li>Unauthorized mentorship status changes.</li>
 *   <li>Session logout and invalid session state.</li>
 * </ol>
 */
@DisplayName("Role-Based Access Control — Service Layer Tests (Prompt 21)")
class RoleBasedAccessControlTest {

    // ── Test fixtures ──────────────────────────────────────────────────────────

    private Student student1;
    private Student student2;
    private Alumni  alumni1;
    private Alumni  alumni2;

    private FakeStudentProfileRepo    studentProfileRepo;
    private FakeAlumniProfileRepo     alumniProfileRepo;
    private FakeMentorshipRepo        mentorshipRepo;
    private FakeUserRepo              userRepo;

    private StudentProfileService     studentProfileService;
    private AlumniProfileService      alumniProfileService;
    private AlumniDirectoryService    directoryService;
    private MentorshipRequestService  mentorshipService;

    // ── Setup / Teardown ──────────────────────────────────────────────────────

    @BeforeEach
    void setUp() throws SQLException {
        UserSession.getInstance().logout();

        // Students
        student1 = new Student();
        student1.setId(101);
        student1.setEmail("student1@thapar.edu");
        student1.setFullName("Alice Student");
        student1.setRole(Role.STUDENT);
        student1.setDepartment("Computer Science");
        student1.setGraduationYear(2026);
        student1.setCurrentSemester(6);
        student1.setActive(true);

        student2 = new Student();
        student2.setId(102);
        student2.setEmail("student2@thapar.edu");
        student2.setFullName("Bob Student");
        student2.setRole(Role.STUDENT);
        student2.setDepartment("Electronics");
        student2.setGraduationYear(2027);
        student2.setActive(true);

        // Alumni
        alumni1 = new Alumni();
        alumni1.setId(201);
        alumni1.setEmail("alumni1@google.com");
        alumni1.setFullName("Carol Alumni");
        alumni1.setRole(Role.ALUMNI);
        alumni1.setDepartment("Computer Science");
        alumni1.setGraduationYear(2020);
        alumni1.setAvailableForMentorship(true);
        alumni1.setMaxMentees(5);
        alumni1.setActive(true);

        alumni2 = new Alumni();
        alumni2.setId(202);
        alumni2.setEmail("alumni2@amazon.com");
        alumni2.setFullName("Dave Alumni");
        alumni2.setRole(Role.ALUMNI);
        alumni2.setDepartment("Electronics");
        alumni2.setGraduationYear(2019);
        alumni2.setAvailableForMentorship(true);
        alumni2.setMaxMentees(3);
        alumni2.setActive(true);

        // Repositories
        studentProfileRepo = new FakeStudentProfileRepo();
        studentProfileRepo.save(student1);
        studentProfileRepo.save(student2);

        alumniProfileRepo  = new FakeAlumniProfileRepo();
        alumniProfileRepo.save(alumni1);
        alumniProfileRepo.save(alumni2);

        userRepo = new FakeUserRepo();
        userRepo.save(student1);
        userRepo.save(student2);
        userRepo.save(alumni1);
        userRepo.save(alumni2);

        mentorshipRepo = new FakeMentorshipRepo();
        mentorshipRepo.setAlumniAvailable(201, true);
        mentorshipRepo.setAlumniAvailable(202, true);
        mentorshipRepo.setActiveMenteeCount(201, 0);
        mentorshipRepo.setActiveMenteeCount(202, 0);

        // Services
        studentProfileService = new StudentProfileService(studentProfileRepo);
        alumniProfileService  = new AlumniProfileService(alumniProfileRepo);
        directoryService      = new AlumniDirectoryService(alumniProfileRepo);
        mentorshipService     = new MentorshipRequestService(mentorshipRepo, userRepo);
    }

    @AfterEach
    void tearDown() {
        UserSession.getInstance().logout();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 1. Unauthenticated access
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("1. Unauthenticated access is denied for all protected operations")
    class UnauthenticatedAccess {

        @Test
        @DisplayName("viewMyProfile (student) — denied when not logged in")
        void studentViewMyProfile_notLoggedIn_denied() {
            StudentProfileResult result = studentProfileService.viewMyProfile();
            assertFalse(result.isSuccess());
            assertContains(result.getMessage(), "logged in");
        }

        @Test
        @DisplayName("viewProfile(userId) (student) — denied when not logged in")
        void studentViewProfile_notLoggedIn_denied() {
            StudentProfileResult result = studentProfileService.viewProfile(101);
            assertFalse(result.isSuccess());
            assertContains(result.getMessage(), "logged in");
        }

        @Test
        @DisplayName("updateProfile(userId, request) (student) — denied when not logged in")
        void studentUpdateProfile_notLoggedIn_denied() {
            StudentProfileResult result = studentProfileService.updateProfile(101, validStudentRequest());
            assertFalse(result.isSuccess());
            assertContains(result.getMessage(), "logged in");
        }

        @Test
        @DisplayName("viewMyProfile (alumni) — denied when not logged in")
        void alumniViewMyProfile_notLoggedIn_denied() {
            AlumniProfileResult result = alumniProfileService.viewMyProfile();
            assertFalse(result.isSuccess());
            assertContains(result.getMessage(), "logged in");
        }

        @Test
        @DisplayName("viewProfile(userId) (alumni) — denied when not logged in")
        void alumniViewProfile_notLoggedIn_denied() {
            AlumniProfileResult result = alumniProfileService.viewProfile(201);
            assertFalse(result.isSuccess());
            assertContains(result.getMessage(), "logged in");
        }

        @Test
        @DisplayName("updateProfile(userId, request) (alumni) — denied when not logged in")
        void alumniUpdateProfile_notLoggedIn_denied() {
            AlumniProfileResult result = alumniProfileService.updateProfile(201, validAlumniRequest());
            assertFalse(result.isSuccess());
            assertContains(result.getMessage(), "logged in");
        }

        @Test
        @DisplayName("getAlumniDirectory — denied when not logged in")
        void getAlumniDirectory_notLoggedIn_denied() {
            AlumniDirectoryResult result = directoryService.getAlumniDirectory();
            assertFalse(result.isSuccess());
            assertContains(result.getMessage(), "logged in");
        }

        @Test
        @DisplayName("searchAlumni — denied when not logged in")
        void searchAlumni_notLoggedIn_denied() {
            AlumniDirectoryResult result = directoryService.searchAlumni("java");
            assertFalse(result.isSuccess());
            assertContains(result.getMessage(), "logged in");
        }

        @Test
        @DisplayName("filterAlumni — denied when not logged in")
        void filterAlumni_notLoggedIn_denied() {
            AlumniDirectoryResult result = directoryService.filterAlumni(null);
            assertFalse(result.isSuccess());
            assertContains(result.getMessage(), "logged in");
        }

        @Test
        @DisplayName("sendMentorshipRequest — denied when not logged in")
        void sendRequest_notLoggedIn_denied() {
            MentorshipRequestResult result = mentorshipService.sendRequest(201, "Career", "Help me");
            assertFalse(result.isSuccess());
            assertContains(result.getMessage(), "logged in");
        }

        @Test
        @DisplayName("getRequestsForStudent — denied when not logged in")
        void getRequestsForStudent_notLoggedIn_denied() {
            MentorshipRequestResult result = mentorshipService.getRequestsForStudent(101);
            assertFalse(result.isSuccess());
            assertContains(result.getMessage(), "logged in");
        }

        @Test
        @DisplayName("getRequestsForAlumni — denied when not logged in")
        void getRequestsForAlumni_notLoggedIn_denied() {
            MentorshipRequestResult result = mentorshipService.getRequestsForAlumni(201);
            assertFalse(result.isSuccess());
            assertContains(result.getMessage(), "logged in");
        }

        @Test
        @DisplayName("getConnectionsForStudent — denied when not logged in")
        void getConnectionsForStudent_notLoggedIn_denied() {
            MentorshipRequestResult result = mentorshipService.getConnectionsForStudent(101);
            assertFalse(result.isSuccess());
            assertContains(result.getMessage(), "logged in");
        }

        @Test
        @DisplayName("getConnectionsForAlumni — denied when not logged in")
        void getConnectionsForAlumni_notLoggedIn_denied() {
            MentorshipRequestResult result = mentorshipService.getConnectionsForAlumni(201);
            assertFalse(result.isSuccess());
            assertContains(result.getMessage(), "logged in");
        }

        @Test
        @DisplayName("acceptRequest — denied when not logged in")
        void acceptRequest_notLoggedIn_denied() {
            MentorshipRequestResult result = mentorshipService.acceptRequest(1);
            assertFalse(result.isSuccess());
            assertContains(result.getMessage(), "logged in");
        }

        @Test
        @DisplayName("rejectRequest — denied when not logged in")
        void rejectRequest_notLoggedIn_denied() {
            MentorshipRequestResult result = mentorshipService.rejectRequest(1);
            assertFalse(result.isSuccess());
            assertContains(result.getMessage(), "logged in");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. Students accessing student-permitted operations
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("2. Students can access their own permitted operations")
    class StudentPermittedOperations {

        @BeforeEach
        void loginAsStudent1() {
            UserSession.getInstance().login(student1);
        }

        @Test
        @DisplayName("Student can view their own profile via viewMyProfile()")
        void student_viewMyProfile_succeeds() {
            StudentProfileResult result = studentProfileService.viewMyProfile();
            assertTrue(result.isSuccess(), result.getMessage());
            assertEquals("Alice Student", result.getStudent().getFullName());
        }

        @Test
        @DisplayName("Student can view their own profile via viewProfile(ownId)")
        void student_viewProfile_ownId_succeeds() {
            StudentProfileResult result = studentProfileService.viewProfile(101);
            assertTrue(result.isSuccess(), result.getMessage());
        }

        @Test
        @DisplayName("Student can browse the alumni directory")
        void student_browseAlumniDirectory_succeeds() {
            AlumniDirectoryResult result = directoryService.getAlumniDirectory();
            assertTrue(result.isSuccess(), result.getMessage());
            assertFalse(result.getAlumniList().isEmpty());
        }

        @Test
        @DisplayName("Student can search alumni")
        void student_searchAlumni_succeeds() {
            AlumniDirectoryResult result = directoryService.searchAlumni("carol");
            assertTrue(result.isSuccess(), result.getMessage());
        }

        @Test
        @DisplayName("Student can send a mentorship request")
        void student_sendMentorshipRequest_succeeds() {
            MentorshipRequestResult result = mentorshipService.sendRequest(201, "Career guidance", "Please mentor me");
            assertTrue(result.isSuccess(), result.getMessage());
        }

        @Test
        @DisplayName("Student can view their own sent requests")
        void student_getRequestsForStudent_ownId_succeeds() {
            MentorshipRequestResult result = mentorshipService.getRequestsForStudent(101);
            assertTrue(result.isSuccess(), result.getMessage());
        }

        @Test
        @DisplayName("Student can view their own accepted connections")
        void student_getConnectionsForStudent_ownId_succeeds() {
            // Seed an accepted connection
            mentorshipRepo.seedAcceptedConnection(101, 201);
            MentorshipRequestResult result = mentorshipService.getConnectionsForStudent(101);
            assertTrue(result.isSuccess(), result.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. Students denied alumni-only operations
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("3. Students are denied alumni-only operations")
    class StudentDeniedAlumniOperations {

        @BeforeEach
        void loginAsStudent1() {
            UserSession.getInstance().login(student1);
        }

        @Test
        @DisplayName("Student cannot access alumni profile service (viewMyProfile)")
        void student_alumniViewMyProfile_denied() {
            AlumniProfileResult result = alumniProfileService.viewMyProfile();
            assertFalse(result.isSuccess());
            assertAccessDenied(result.getMessage());
        }

        @Test
        @DisplayName("Student cannot view an alumni profile via viewProfile(alumniId)")
        void student_alumniViewProfile_denied() {
            AlumniProfileResult result = alumniProfileService.viewProfile(201);
            assertFalse(result.isSuccess());
            assertAccessDenied(result.getMessage());
        }

        @Test
        @DisplayName("Student cannot update an alumni profile")
        void student_alumniUpdateProfile_denied() {
            AlumniProfileResult result = alumniProfileService.updateProfile(201, validAlumniRequest());
            assertFalse(result.isSuccess());
            assertAccessDenied(result.getMessage());
        }

        @Test
        @DisplayName("Student cannot view received mentorship requests (alumni operation)")
        void student_getRequestsForAlumni_denied() {
            MentorshipRequestResult result = mentorshipService.getRequestsForAlumni(201);
            assertFalse(result.isSuccess());
            assertAccessDenied(result.getMessage());
        }

        @Test
        @DisplayName("Student cannot view alumni mentorship connections")
        void student_getConnectionsForAlumni_denied() {
            MentorshipRequestResult result = mentorshipService.getConnectionsForAlumni(201);
            assertFalse(result.isSuccess());
            assertAccessDenied(result.getMessage());
        }

        @Test
        @DisplayName("Student cannot accept a mentorship request")
        void student_acceptRequest_denied() {
            MentorshipRequestResult result = mentorshipService.acceptRequest(1);
            assertFalse(result.isSuccess());
            assertAccessDenied(result.getMessage());
        }

        @Test
        @DisplayName("Student cannot reject a mentorship request")
        void student_rejectRequest_denied() {
            MentorshipRequestResult result = mentorshipService.rejectRequest(1);
            assertFalse(result.isSuccess());
            assertAccessDenied(result.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. Alumni accessing alumni-permitted operations
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("4. Alumni can access their own permitted operations")
    class AlumniPermittedOperations {

        @BeforeEach
        void loginAsAlumni1() {
            UserSession.getInstance().login(alumni1);
        }

        @Test
        @DisplayName("Alumni can view their own profile via viewMyProfile()")
        void alumni_viewMyProfile_succeeds() {
            AlumniProfileResult result = alumniProfileService.viewMyProfile();
            assertTrue(result.isSuccess(), result.getMessage());
            assertEquals("Carol Alumni", result.getAlumni().getFullName());
        }

        @Test
        @DisplayName("Alumni can view their own profile via viewProfile(ownId)")
        void alumni_viewProfile_ownId_succeeds() {
            AlumniProfileResult result = alumniProfileService.viewProfile(201);
            assertTrue(result.isSuccess(), result.getMessage());
        }

        @Test
        @DisplayName("Alumni can view received mentorship requests addressed to them")
        void alumni_getRequestsForAlumni_ownId_succeeds() {
            MentorshipRequestResult result = mentorshipService.getRequestsForAlumni(201);
            assertTrue(result.isSuccess(), result.getMessage());
        }

        @Test
        @DisplayName("Alumni can view their own accepted connections")
        void alumni_getConnectionsForAlumni_ownId_succeeds() {
            mentorshipRepo.seedAcceptedConnection(101, 201);
            MentorshipRequestResult result = mentorshipService.getConnectionsForAlumni(201);
            assertTrue(result.isSuccess(), result.getMessage());
        }

        @Test
        @DisplayName("Alumni can accept a pending request addressed to them")
        void alumni_acceptPendingRequest_ownRequest_succeeds() {
            int requestId = mentorshipRepo.seedPendingRequest(101, 201);
            MentorshipRequestResult result = mentorshipService.acceptRequest(requestId);
            assertTrue(result.isSuccess(), result.getMessage());
        }

        @Test
        @DisplayName("Alumni can reject a pending request addressed to them")
        void alumni_rejectPendingRequest_ownRequest_succeeds() {
            int requestId = mentorshipRepo.seedPendingRequest(101, 201);
            MentorshipRequestResult result = mentorshipService.rejectRequest(requestId);
            assertTrue(result.isSuccess(), result.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. Alumni denied student-only operations
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("5. Alumni are denied student-only operations")
    class AlumniDeniedStudentOperations {

        @BeforeEach
        void loginAsAlumni1() {
            UserSession.getInstance().login(alumni1);
        }

        @Test
        @DisplayName("Alumni cannot access student profile service (viewMyProfile)")
        void alumni_studentViewMyProfile_denied() {
            StudentProfileResult result = studentProfileService.viewMyProfile();
            assertFalse(result.isSuccess());
            assertAccessDenied(result.getMessage());
        }

        @Test
        @DisplayName("Alumni cannot view a student profile via viewProfile(studentId)")
        void alumni_studentViewProfile_denied() {
            StudentProfileResult result = studentProfileService.viewProfile(101);
            assertFalse(result.isSuccess());
            assertAccessDenied(result.getMessage());
        }

        @Test
        @DisplayName("Alumni cannot update a student profile")
        void alumni_studentUpdateProfile_denied() {
            StudentProfileResult result = studentProfileService.updateProfile(101, validStudentRequest());
            assertFalse(result.isSuccess());
            assertAccessDenied(result.getMessage());
        }

        @Test
        @DisplayName("Alumni cannot browse the alumni directory (student-only)")
        void alumni_browseAlumniDirectory_denied() {
            AlumniDirectoryResult result = directoryService.getAlumniDirectory();
            assertFalse(result.isSuccess());
            assertAccessDenied(result.getMessage());
        }

        @Test
        @DisplayName("Alumni cannot search alumni (student-only)")
        void alumni_searchAlumni_denied() {
            AlumniDirectoryResult result = directoryService.searchAlumni("carol");
            assertFalse(result.isSuccess());
            assertAccessDenied(result.getMessage());
        }

        @Test
        @DisplayName("Alumni cannot send a mentorship request (student-only)")
        void alumni_sendMentorshipRequest_denied() {
            MentorshipRequestResult result = mentorshipService.sendRequest(202, "Help", "Please");
            assertFalse(result.isSuccess());
            assertAccessDenied(result.getMessage());
        }

        @Test
        @DisplayName("Alumni cannot view student's sent requests")
        void alumni_getRequestsForStudent_denied() {
            MentorshipRequestResult result = mentorshipService.getRequestsForStudent(101);
            assertFalse(result.isSuccess());
            assertAccessDenied(result.getMessage());
        }

        @Test
        @DisplayName("Alumni cannot view student's connections")
        void alumni_getConnectionsForStudent_denied() {
            MentorshipRequestResult result = mentorshipService.getConnectionsForStudent(101);
            assertFalse(result.isSuccess());
            assertAccessDenied(result.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 6. Cross-user profile access
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("6. Cross-user profile access and modification is denied")
    class CrossUserProfileAccess {

        @Test
        @DisplayName("Student cannot view another student's profile via viewProfile(otherId)")
        void student_viewProfile_otherStudentId_denied() {
            UserSession.getInstance().login(student1);
            StudentProfileResult result = studentProfileService.viewProfile(102); // student2's ID
            assertFalse(result.isSuccess());
            assertAccessDenied(result.getMessage());
        }

        @Test
        @DisplayName("Student cannot update another student's profile via updateProfile(otherId)")
        void student_updateProfile_otherStudentId_denied() {
            UserSession.getInstance().login(student1);
            StudentProfileResult result = studentProfileService.updateProfile(102, validStudentRequest());
            assertFalse(result.isSuccess());
            assertAccessDenied(result.getMessage());
        }

        @Test
        @DisplayName("Alumni cannot view another alumni's profile via viewProfile(otherId)")
        void alumni_viewProfile_otherAlumniId_denied() {
            UserSession.getInstance().login(alumni1);
            AlumniProfileResult result = alumniProfileService.viewProfile(202); // alumni2's ID
            assertFalse(result.isSuccess());
            assertAccessDenied(result.getMessage());
        }

        @Test
        @DisplayName("Alumni cannot update another alumni's profile via updateProfile(otherId)")
        void alumni_updateProfile_otherAlumniId_denied() {
            UserSession.getInstance().login(alumni1);
            AlumniProfileResult result = alumniProfileService.updateProfile(202, validAlumniRequest());
            assertFalse(result.isSuccess());
            assertAccessDenied(result.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 7. Cross-user mentorship access
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("7. Cross-user mentorship request and connection access is denied")
    class CrossUserMentorshipAccess {

        @Test
        @DisplayName("Student cannot view another student's sent requests")
        void student_getRequestsForOtherStudent_denied() {
            UserSession.getInstance().login(student1);
            MentorshipRequestResult result = mentorshipService.getRequestsForStudent(102); // student2's ID
            assertFalse(result.isSuccess());
            assertAccessDenied(result.getMessage());
        }

        @Test
        @DisplayName("Student cannot view another student's connections")
        void student_getConnectionsForOtherStudent_denied() {
            UserSession.getInstance().login(student1);
            MentorshipRequestResult result = mentorshipService.getConnectionsForStudent(102);
            assertFalse(result.isSuccess());
            assertAccessDenied(result.getMessage());
        }

        @Test
        @DisplayName("Alumni cannot view another alumni's received requests")
        void alumni_getRequestsForOtherAlumni_denied() {
            UserSession.getInstance().login(alumni1);
            MentorshipRequestResult result = mentorshipService.getRequestsForAlumni(202); // alumni2's ID
            assertFalse(result.isSuccess());
            assertAccessDenied(result.getMessage());
        }

        @Test
        @DisplayName("Alumni cannot view another alumni's connections")
        void alumni_getConnectionsForOtherAlumni_denied() {
            UserSession.getInstance().login(alumni1);
            MentorshipRequestResult result = mentorshipService.getConnectionsForAlumni(202);
            assertFalse(result.isSuccess());
            assertAccessDenied(result.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 8. Unauthorized mentorship status changes
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("8. Unauthorized mentorship status changes are rejected")
    class UnauthorizedStatusChanges {

        @Test
        @DisplayName("Alumni cannot accept a request addressed to another alumni")
        void alumni_acceptRequest_notOwner_denied() {
            int requestId = mentorshipRepo.seedPendingRequest(101, 202); // request is for alumni2
            UserSession.getInstance().login(alumni1); // alumni1 tries to act
            MentorshipRequestResult result = mentorshipService.acceptRequest(requestId);
            assertFalse(result.isSuccess());
            assertAccessDenied(result.getMessage());
        }

        @Test
        @DisplayName("Alumni cannot reject a request addressed to another alumni")
        void alumni_rejectRequest_notOwner_denied() {
            int requestId = mentorshipRepo.seedPendingRequest(101, 202);
            UserSession.getInstance().login(alumni1);
            MentorshipRequestResult result = mentorshipService.rejectRequest(requestId);
            assertFalse(result.isSuccess());
            assertAccessDenied(result.getMessage());
        }

        @Test
        @DisplayName("Alumni cannot accept a request that is already accepted (non-pending)")
        void alumni_acceptAlreadyAccepted_denied() {
            int requestId = mentorshipRepo.seedAcceptedConnection(101, 201);
            UserSession.getInstance().login(alumni1);
            MentorshipRequestResult result = mentorshipService.acceptRequest(requestId);
            assertFalse(result.isSuccess());
            // Should mention non-pending status or access denied
            assertTrue(result.getMessage().toLowerCase().contains("not pending")
                    || result.getMessage().toLowerCase().contains("cannot respond"),
                    "Expected non-pending rejection: " + result.getMessage());
        }

        @Test
        @DisplayName("Alumni cannot reject a request that is already rejected (non-pending)")
        void alumni_rejectAlreadyRejected_denied() {
            int requestId = mentorshipRepo.seedRejectedRequest(101, 201);
            UserSession.getInstance().login(alumni1);
            MentorshipRequestResult result = mentorshipService.rejectRequest(requestId);
            assertFalse(result.isSuccess());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 9. Session logout clears access
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("9. Session logout correctly revokes all access")
    class SessionLogout {

        @Test
        @DisplayName("After logout, student can no longer view their profile")
        void afterLogout_studentCannotViewProfile() {
            UserSession.getInstance().login(student1);
            assertTrue(studentProfileService.viewMyProfile().isSuccess());

            UserSession.getInstance().logout();

            StudentProfileResult result = studentProfileService.viewMyProfile();
            assertFalse(result.isSuccess());
            assertContains(result.getMessage(), "logged in");
        }

        @Test
        @DisplayName("After logout, alumni can no longer view received requests")
        void afterLogout_alumniCannotViewRequests() {
            UserSession.getInstance().login(alumni1);
            assertTrue(mentorshipService.getRequestsForAlumni(201).isSuccess());

            UserSession.getInstance().logout();

            MentorshipRequestResult result = mentorshipService.getRequestsForAlumni(201);
            assertFalse(result.isSuccess());
            assertContains(result.getMessage(), "logged in");
        }

        @Test
        @DisplayName("UserSession helpers correctly reflect logged-out state")
        void loggedOut_sessionHelpers_returnFalse() {
            UserSession.getInstance().login(student1);
            assertTrue(UserSession.getInstance().isLoggedIn());
            assertTrue(UserSession.getInstance().isStudent());

            UserSession.getInstance().logout();

            assertFalse(UserSession.getInstance().isLoggedIn());
            assertFalse(UserSession.getInstance().isStudent());
            assertFalse(UserSession.getInstance().isAlumni());
            assertTrue(UserSession.getInstance().getCurrentUser().isEmpty());
        }

        @Test
        @DisplayName("requireStudent() throws SecurityException when not logged in")
        void requireStudent_notLoggedIn_throwsSecurityException() {
            assertThrows(SecurityException.class,
                    () -> UserSession.getInstance().requireStudent());
        }

        @Test
        @DisplayName("requireAlumni() throws SecurityException when logged in as student")
        void requireAlumni_loggedInAsStudent_throwsSecurityException() {
            UserSession.getInstance().login(student1);
            assertThrows(SecurityException.class,
                    () -> UserSession.getInstance().requireAlumni());
        }

        @Test
        @DisplayName("requireStudent() throws SecurityException when logged in as alumni")
        void requireStudent_loggedInAsAlumni_throwsSecurityException() {
            UserSession.getInstance().login(alumni1);
            assertThrows(SecurityException.class,
                    () -> UserSession.getInstance().requireStudent());
        }

        @Test
        @DisplayName("requireUserId() throws SecurityException when ID does not match")
        void requireUserId_wrongId_throwsSecurityException() {
            UserSession.getInstance().login(student1);
            assertThrows(SecurityException.class,
                    () -> UserSession.getInstance().requireUserId(999));
        }

        @Test
        @DisplayName("requireUserId() succeeds when ID matches")
        void requireUserId_correctId_noException() {
            UserSession.getInstance().login(student1);
            assertDoesNotThrow(() -> UserSession.getInstance().requireUserId(101));
        }

        @Test
        @DisplayName("getAuthenticatedUserId() throws SecurityException when not logged in")
        void getAuthenticatedUserId_notLoggedIn_throws() {
            assertThrows(SecurityException.class,
                    () -> UserSession.getInstance().getAuthenticatedUserId());
        }

        @Test
        @DisplayName("getAuthenticatedUserId() returns correct ID when logged in")
        void getAuthenticatedUserId_loggedIn_returnsId() {
            UserSession.getInstance().login(student1);
            assertEquals(101, UserSession.getInstance().getAuthenticatedUserId());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helper assertions
    // ══════════════════════════════════════════════════════════════════════════

    private void assertContains(String message, String keyword) {
        assertTrue(message.toLowerCase().contains(keyword.toLowerCase()),
                "Expected message to contain '" + keyword + "' but was: " + message);
    }

    private void assertAccessDenied(String message) {
        assertTrue(message.toLowerCase().contains("access denied")
                        || message.toLowerCase().contains("only students")
                        || message.toLowerCase().contains("only alumni")
                        || message.toLowerCase().contains("cannot view")
                        || message.toLowerCase().contains("cannot modify")
                        || message.toLowerCase().contains("cannot access"),
                "Expected access-denied message, but was: " + message);
    }

    private StudentProfileRequest validStudentRequest() {
        StudentProfileRequest req = new StudentProfileRequest();
        req.setDepartment("Computer Science");
        req.setGraduationYear(2026);
        req.setCurrentSemester(6);
        return req;
    }

    private AlumniProfileRequest validAlumniRequest() {
        AlumniProfileRequest req = new AlumniProfileRequest();
        req.setDepartment("Computer Science");
        req.setGraduationYear(2020);
        return req;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Fake Repositories
    // ══════════════════════════════════════════════════════════════════════════

    // ── StudentProfileRepository ──────────────────────────────────────────────

    static class FakeStudentProfileRepo implements StudentProfileRepository {
        private final Map<Integer, Student> store = new HashMap<>();

        public void save(Student s) {
            store.put(s.getId(), s);
        }

        @Override
        public Optional<Student> findProfileByUserId(int userId) throws SQLException {
            return Optional.ofNullable(store.get(userId));
        }

        @Override
        public boolean profileExistsForUser(int userId) throws SQLException {
            return store.containsKey(userId);
        }

        @Override
        public boolean createProfile(Student student) throws SQLException {
            store.put(student.getId(), student);
            return true;
        }

        @Override
        public boolean updateProfile(Student student) throws SQLException {
            if (!store.containsKey(student.getId())) return false;
            store.put(student.getId(), student);
            return true;
        }
    }

    // ── AlumniProfileRepository ───────────────────────────────────────────────

    static class FakeAlumniProfileRepo implements AlumniProfileRepository {
        private final Map<Integer, Alumni> store = new HashMap<>();

        public void save(Alumni a) {
            store.put(a.getId(), a);
        }

        @Override
        public Optional<Alumni> findProfileByUserId(int userId) throws SQLException {
            return Optional.ofNullable(store.get(userId));
        }

        @Override
        public boolean profileExistsForUser(int userId) throws SQLException {
            return store.containsKey(userId);
        }

        @Override
        public boolean createProfile(Alumni alumni) throws SQLException {
            store.put(alumni.getId(), alumni);
            return true;
        }

        @Override
        public boolean updateProfile(Alumni alumni) throws SQLException {
            if (!store.containsKey(alumni.getId())) return false;
            store.put(alumni.getId(), alumni);
            return true;
        }

        @Override
        public List<Alumni> findAllAlumni() throws SQLException {
            return new ArrayList<>(store.values());
        }

        @Override
        public List<Alumni> searchAlumni(String keyword) throws SQLException {
            String kw = keyword.toLowerCase();
            List<Alumni> result = new ArrayList<>();
            for (Alumni a : store.values()) {
                if (a.getFullName() != null && a.getFullName().toLowerCase().contains(kw)) {
                    result.add(a);
                }
            }
            return result;
        }

        @Override
        public List<Alumni> filterAlumni(com.alumconnect.service.dto.AlumniFilterCriteria criteria) throws SQLException {
            return new ArrayList<>(store.values());
        }

        @Override
        public Optional<Alumni> findPublicProfileByUserId(int userId) throws SQLException {
            return Optional.ofNullable(store.get(userId));
        }
    }

    // ── MentorshipRequestRepository ───────────────────────────────────────────

    static class FakeMentorshipRepo implements MentorshipRequestRepository {
        private final Map<Integer, MentorshipRequest> requests = new HashMap<>();
        private final Map<Integer, Boolean>           available = new HashMap<>();
        private final Map<Integer, Integer>           menteeCount = new HashMap<>();
        private int nextId = 1;

        public void setAlumniAvailable(int alumniId, boolean available) {
            this.available.put(alumniId, available);
        }

        public void setActiveMenteeCount(int alumniId, int count) {
            this.menteeCount.put(alumniId, count);
        }

        /** Seeds a PENDING request and returns its ID. */
        public int seedPendingRequest(int studentId, int alumniId) {
            MentorshipRequest r = new MentorshipRequest();
            r.setId(nextId++);
            r.setStudentId(studentId);
            r.setAlumniId(alumniId);
            r.setStatus(MentorshipStatus.PENDING);
            r.setTopic("Test Topic");
            r.setMessage("Test Message");
            r.setRequestedAt(LocalDateTime.now());
            requests.put(r.getId(), r);
            return r.getId();
        }

        /** Seeds an ACCEPTED connection and returns its ID. */
        public int seedAcceptedConnection(int studentId, int alumniId) {
            MentorshipRequest r = new MentorshipRequest();
            r.setId(nextId++);
            r.setStudentId(studentId);
            r.setAlumniId(alumniId);
            r.setStatus(MentorshipStatus.ACCEPTED);
            r.setTopic("Mentorship");
            r.setRequestedAt(LocalDateTime.now());
            r.setRespondedAt(LocalDateTime.now());
            requests.put(r.getId(), r);
            return r.getId();
        }

        /** Seeds a REJECTED request and returns its ID. */
        public int seedRejectedRequest(int studentId, int alumniId) {
            MentorshipRequest r = new MentorshipRequest();
            r.setId(nextId++);
            r.setStudentId(studentId);
            r.setAlumniId(alumniId);
            r.setStatus(MentorshipStatus.REJECTED);
            r.setTopic("Rejected Topic");
            r.setRequestedAt(LocalDateTime.now());
            r.setRespondedAt(LocalDateTime.now());
            requests.put(r.getId(), r);
            return r.getId();
        }

        @Override
        public int createRequest(MentorshipRequestDto dto) throws SQLException {
            MentorshipRequest r = new MentorshipRequest();
            r.setId(nextId++);
            r.setStudentId(dto.getStudentId());
            r.setAlumniId(dto.getAlumniId());
            r.setTopic(dto.getTopic());
            r.setMessage(dto.getMessage());
            r.setStatus(MentorshipStatus.PENDING);
            r.setRequestedAt(LocalDateTime.now());
            requests.put(r.getId(), r);
            return r.getId();
        }

        @Override
        public boolean existsPending(int studentId, int alumniId) throws SQLException {
            return requests.values().stream()
                    .anyMatch(r -> r.getStudentId() == studentId
                            && r.getAlumniId() == alumniId
                            && r.getStatus() == MentorshipStatus.PENDING);
        }

        @Override
        public boolean existsAccepted(int studentId, int alumniId) throws SQLException {
            return requests.values().stream()
                    .anyMatch(r -> r.getStudentId() == studentId
                            && r.getAlumniId() == alumniId
                            && r.getStatus() == MentorshipStatus.ACCEPTED);
        }

        @Override
        public int countActiveMentees(int alumniId) throws SQLException {
            return menteeCount.getOrDefault(alumniId, 0);
        }

        @Override
        public boolean isAlumniAvailable(int alumniId) throws SQLException {
            return available.getOrDefault(alumniId, false);
        }

        @Override
        public Optional<MentorshipRequest> findRequestById(int requestId) throws SQLException {
            return Optional.ofNullable(requests.get(requestId));
        }

        @Override
        public List<MentorshipRequest> findRequestsByAlumniId(int alumniId) throws SQLException {
            List<MentorshipRequest> result = new ArrayList<>();
            for (MentorshipRequest r : requests.values()) {
                if (r.getAlumniId() == alumniId) result.add(r);
            }
            return result;
        }

        @Override
        public List<MentorshipRequest> findRequestsByStudentId(int studentId) throws SQLException {
            List<MentorshipRequest> result = new ArrayList<>();
            for (MentorshipRequest r : requests.values()) {
                if (r.getStudentId() == studentId) result.add(r);
            }
            return result;
        }

        @Override
        public List<MentorshipRequest> findAcceptedConnectionsByStudentId(int studentId) throws SQLException {
            List<MentorshipRequest> result = new ArrayList<>();
            for (MentorshipRequest r : requests.values()) {
                if (r.getStudentId() == studentId && r.getStatus() == MentorshipStatus.ACCEPTED) result.add(r);
            }
            return result;
        }

        @Override
        public List<MentorshipRequest> findAcceptedConnectionsByAlumniId(int alumniId) throws SQLException {
            List<MentorshipRequest> result = new ArrayList<>();
            for (MentorshipRequest r : requests.values()) {
                if (r.getAlumniId() == alumniId && r.getStatus() == MentorshipStatus.ACCEPTED) result.add(r);
            }
            return result;
        }

        @Override
        public boolean updateRequestStatus(int requestId, MentorshipStatus status,
                                           LocalDateTime respondedAt) throws SQLException {
            MentorshipRequest r = requests.get(requestId);
            if (r == null) return false;
            r.setStatus(status);
            r.setRespondedAt(respondedAt);
            return true;
        }
    }

    // ── UserRepository ────────────────────────────────────────────────────────

    static class FakeUserRepo implements UserRepository {
        private final Map<Integer, User> byId    = new HashMap<>();
        private final Map<String, User>  byEmail = new HashMap<>();

        public void save(User u) {
            byId.put(u.getId(), u);
            byEmail.put(u.getEmail(), u);
        }

        @Override
        public Optional<User> findById(int id) throws SQLException {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public Optional<User> findByEmail(String email) throws SQLException {
            return Optional.ofNullable(byEmail.get(email));
        }

        @Override
        public boolean existsByEmail(String email) throws SQLException {
            return byEmail.containsKey(email);
        }

        @Override
        public Optional<Student> findStudentById(int userId) throws SQLException {
            User u = byId.get(userId);
            return (u instanceof Student s) ? Optional.of(s) : Optional.empty();
        }

        @Override
        public Optional<Alumni> findAlumniById(int userId) throws SQLException {
            User u = byId.get(userId);
            return (u instanceof Alumni a) ? Optional.of(a) : Optional.empty();
        }

        @Override
        public Student saveStudent(Student student) throws SQLException {
            save(student);
            return student;
        }

        @Override
        public Alumni saveAlumni(Alumni alumni) throws SQLException {
            save(alumni);
            return alumni;
        }

        @Override
        public boolean deleteUserById(int userId) throws SQLException {
            User removed = byId.remove(userId);
            if (removed != null) byEmail.remove(removed.getEmail());
            return removed != null;
        }
    }
}
