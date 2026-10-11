package com.alumconnect.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User and Subclass Model Tests")
class UserModelTest {

    @Test
    @DisplayName("Role enum parsing and behavior")
    void testRoleEnum() {
        assertEquals(Role.STUDENT, Role.fromString("STUDENT"));
        assertEquals(Role.STUDENT, Role.fromString("student"));
        assertEquals(Role.ALUMNI, Role.fromString(" alumni "));
        assertEquals(Role.ADMIN, Role.fromString("Admin"));

        assertThrows(IllegalArgumentException.class, () -> Role.fromString(null));
        assertThrows(IllegalArgumentException.class, () -> Role.fromString(""));
        assertThrows(IllegalArgumentException.class, () -> Role.fromString("INVALID_ROLE"));
    }

    @Test
    @DisplayName("Student inheritance, defaults and properties")
    void testStudentModel() {
        Student student = new Student();
        assertEquals(Role.STUDENT, student.getRole());
        assertTrue(student.isStudent());
        assertFalse(student.isAlumni());
        assertFalse(student.isAdmin());
        assertTrue(student.isActive());
        assertEquals(Student.DEFAULT_DEGREE, student.getDegree());
        assertEquals(Student.MIN_SEMESTER, student.getCurrentSemester());

        student.setId(101);
        student.setEmail("student@example.com");
        student.setPasswordHash("hashed_pw_student");
        student.setFullName("John Doe");
        student.setPhone("1234567890");
        student.setProfileId(1);
        student.setEnrollmentNumber("EN12345");
        student.setDepartment("Computer Science");
        student.setGraduationYear(2027);
        student.setCurrentSemester(6);
        student.setBio("Aspiring Software Engineer");
        student.setSkills("Java, Spring, MySQL");
        student.setInterests("Web Development, Cloud");
        student.setLinkedinUrl("https://linkedin.com/in/johndoe");
        student.setGithubUrl("https://github.com/johndoe");

        LocalDateTime now = LocalDateTime.now();
        student.setCreatedAt(now);
        student.setUpdatedAt(now);

        assertEquals(101, student.getId());
        assertEquals("student@example.com", student.getEmail());
        assertEquals("hashed_pw_student", student.getPasswordHash());
        assertEquals("John Doe", student.getFullName());
        assertEquals("1234567890", student.getPhone());
        assertEquals(1, student.getProfileId());
        assertEquals("EN12345", student.getEnrollmentNumber());
        assertEquals("Computer Science", student.getDepartment());
        assertEquals(2027, student.getGraduationYear());
        assertEquals(6, student.getCurrentSemester());
        assertEquals("Aspiring Software Engineer", student.getBio());
        assertEquals("Java, Spring, MySQL", student.getSkills());
        assertEquals("Web Development, Cloud", student.getInterests());
        assertEquals("https://linkedin.com/in/johndoe", student.getLinkedinUrl());
        assertEquals("https://github.com/johndoe", student.getGithubUrl());
        assertEquals(now, student.getCreatedAt());
        assertEquals(now, student.getUpdatedAt());

        assertFalse(student.isFinalSemester());
        student.setCurrentSemester(12);
        assertTrue(student.isFinalSemester());

        assertEquals("Computer Science | Sem 12 | Graduating 2027", student.getAcademicSummary());
        assertEquals("John Doe [STUDENT]", student.getDisplayLabel());

        assertFalse(student.toString().contains("hashed_pw_student"));
    }

    @Test
    @DisplayName("Student constructor validation and range constraints")
    void testStudentValidation() {
        Student student = new Student("alice@example.com", "hash", "Alice", "9876543210", "IT", 2026);
        assertEquals("alice@example.com", student.getEmail());
        assertEquals("IT", student.getDepartment());
        assertEquals(2026, student.getGraduationYear());
        assertEquals(Role.STUDENT, student.getRole());

        assertThrows(IllegalArgumentException.class, () -> student.setGraduationYear(1940));
        assertThrows(IllegalArgumentException.class, () -> student.setGraduationYear(2105));
        assertThrows(IllegalArgumentException.class, () -> student.setCurrentSemester(0));
        assertThrows(IllegalArgumentException.class, () -> student.setCurrentSemester(13));
    }

    @Test
    @DisplayName("Alumni inheritance, defaults and properties")
    void testAlumniModel() {
        Alumni alumni = new Alumni();
        assertEquals(Role.ALUMNI, alumni.getRole());
        assertTrue(alumni.isAlumni());
        assertFalse(alumni.isStudent());
        assertFalse(alumni.isAdmin());
        assertTrue(alumni.isActive());
        assertEquals(Alumni.DEFAULT_DEGREE, alumni.getDegree());
        assertEquals(Alumni.DEFAULT_MAX_MENTEES, alumni.getMaxMentees());
        assertEquals(0, alumni.getYearsOfExperience());
        assertTrue(alumni.isAvailableForMentorship());

        alumni.setId(202);
        alumni.setEmail("alumni@example.com");
        alumni.setPasswordHash("hashed_pw_alumni");
        alumni.setFullName("Jane Smith");
        alumni.setPhone("5551234567");
        alumni.setProfileId(2);
        alumni.setDepartment("Information Technology");
        alumni.setGraduationYear(2020);
        alumni.setCurrentCompany("Acme Corp");
        alumni.setCurrentRole("Senior Architect");
        alumni.setIndustry("Technology");
        alumni.setYearsOfExperience(5);
        alumni.setExpertiseAreas("Distributed Systems, Cloud");
        alumni.setBio("Tech enthusiast and mentor");
        alumni.setLinkedinUrl("https://linkedin.com/in/janesmith");
        alumni.setAvailableForMentorship(true);
        alumni.setMaxMentees(4);

        assertEquals(202, alumni.getId());
        assertEquals("alumni@example.com", alumni.getEmail());
        assertEquals("hashed_pw_alumni", alumni.getPasswordHash());
        assertEquals("Jane Smith", alumni.getFullName());
        assertEquals("5551234567", alumni.getPhone());
        assertEquals(2, alumni.getProfileId());
        assertEquals("Information Technology", alumni.getDepartment());
        assertEquals(2020, alumni.getGraduationYear());
        assertEquals("Acme Corp", alumni.getCurrentCompany());
        assertEquals("Senior Architect", alumni.getCurrentRole());
        assertEquals("Technology", alumni.getIndustry());
        assertEquals(5, alumni.getYearsOfExperience());
        assertEquals("Distributed Systems, Cloud", alumni.getExpertiseAreas());
        assertEquals("Tech enthusiast and mentor", alumni.getBio());
        assertEquals("https://linkedin.com/in/janesmith", alumni.getLinkedinUrl());
        assertTrue(alumni.isAvailableForMentorship());
        assertEquals(4, alumni.getMaxMentees());

        assertTrue(alumni.canAcceptMentee());
        alumni.setAvailableForMentorship(false);
        assertFalse(alumni.canAcceptMentee());
        assertEquals("Not Available", alumni.getMentorshipStatus());

        alumni.setAvailableForMentorship(true);
        assertEquals("Open for Mentorship", alumni.getMentorshipStatus());

        alumni.setMaxMentees(0);
        assertFalse(alumni.canAcceptMentee());

        alumni.setMaxMentees(3);
        String summary = alumni.getProfessionalSummary();
        assertTrue(summary.contains("Senior Architect at Acme Corp"));
        assertTrue(summary.contains("5 yrs exp"));
        assertTrue(summary.contains("Information Technology"));
        assertTrue(summary.contains("Grad: 2020"));

        assertFalse(alumni.toString().contains("hashed_pw_alumni"));
    }

    @Test
    @DisplayName("Alumni validation and constraints")
    void testAlumniValidation() {
        Alumni alumni = new Alumni("bob@example.com", "hash", "Bob", "1112223333", "ECE", 2018);
        assertEquals("bob@example.com", alumni.getEmail());
        assertEquals(Role.ALUMNI, alumni.getRole());

        assertThrows(IllegalArgumentException.class, () -> alumni.setGraduationYear(1900));
        assertThrows(IllegalArgumentException.class, () -> alumni.setGraduationYear(2150));
        assertThrows(IllegalArgumentException.class, () -> alumni.setYearsOfExperience(-1));
        assertThrows(IllegalArgumentException.class, () -> alumni.setMaxMentees(-1));
    }

    @Test
    @DisplayName("Equals and hashCode identity semantics")
    void testEqualsAndHashCode() {
        Student s1 = new Student();
        s1.setId(1);
        Student s2 = new Student();
        s2.setId(1);
        Student s3 = new Student();
        s3.setId(2);
        Student sUnsaved1 = new Student();
        Student sUnsaved2 = new Student();

        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
        assertNotEquals(s1, s3);
        assertNotEquals(sUnsaved1, sUnsaved2);
    }
}
