package com.alumconnect.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Validation Utility Tests")
class ValidationUtilTest {

    @Test
    @DisplayName("Email format validation")
    void testEmailValidation() {
        // Valid emails
        assertTrue(ValidationUtil.isValidEmail("student@thapar.edu"));
        assertTrue(ValidationUtil.isValidEmail("john.doe@example.com"));
        assertTrue(ValidationUtil.isValidEmail("user_name+tag@sub.domain.org"));

        // Invalid emails
        assertFalse(ValidationUtil.isValidEmail(null));
        assertFalse(ValidationUtil.isValidEmail(""));
        assertFalse(ValidationUtil.isValidEmail("   "));
        assertFalse(ValidationUtil.isValidEmail("plainaddress"));
        assertFalse(ValidationUtil.isValidEmail("@missingusername.com"));
        assertFalse(ValidationUtil.isValidEmail("missingdomain@"));
        assertFalse(ValidationUtil.isValidEmail("missingdot@com"));
        assertFalse(ValidationUtil.isValidEmail("a@b.c")); // fails %_@__%.__% check
        assertFalse(ValidationUtil.isValidEmail("user@domain..com"));
    }

    @Test
    @DisplayName("Graduation year boundary checks")
    void testGraduationYearValidation() {
        assertTrue(ValidationUtil.isValidGraduationYear(1950));
        assertTrue(ValidationUtil.isValidGraduationYear(2026));
        assertTrue(ValidationUtil.isValidGraduationYear(2100));

        assertFalse(ValidationUtil.isValidGraduationYear(1949));
        assertFalse(ValidationUtil.isValidGraduationYear(2101));
        assertFalse(ValidationUtil.isValidGraduationYear(0));
    }

    @Test
    @DisplayName("Semester boundary checks")
    void testSemesterValidation() {
        assertTrue(ValidationUtil.isValidSemester(1));
        assertTrue(ValidationUtil.isValidSemester(6));
        assertTrue(ValidationUtil.isValidSemester(12));

        assertFalse(ValidationUtil.isValidSemester(0));
        assertFalse(ValidationUtil.isValidSemester(13));
        assertFalse(ValidationUtil.isValidSemester(-1));
    }

    @Test
    @DisplayName("Phone number validation")
    void testPhoneValidation() {
        assertTrue(ValidationUtil.isValidPhone(null));
        assertTrue(ValidationUtil.isValidPhone(""));
        assertTrue(ValidationUtil.isValidPhone("+1-555-123-4567"));
        assertTrue(ValidationUtil.isValidPhone("9876543210"));
        assertTrue(ValidationUtil.isValidPhone("(123) 456-7890"));

        assertFalse(ValidationUtil.isValidPhone("abc-def-ghij"));
        assertFalse(ValidationUtil.isValidPhone("+12345678901234567890123")); // too long
    }

    @Test
    @DisplayName("LinkedIn URL validation")
    void testLinkedInUrlValidation() {
        // Optional / blank
        assertTrue(ValidationUtil.isValidLinkedInUrl(null));
        assertTrue(ValidationUtil.isValidLinkedInUrl(""));
        assertTrue(ValidationUtil.isValidLinkedInUrl("   "));

        // Valid formats
        assertTrue(ValidationUtil.isValidLinkedInUrl("https://linkedin.com/in/johndoe"));
        assertTrue(ValidationUtil.isValidLinkedInUrl("https://www.linkedin.com/in/johndoe"));
        assertTrue(ValidationUtil.isValidLinkedInUrl("http://linkedin.com/in/alice-smith"));
        assertTrue(ValidationUtil.isValidLinkedInUrl("linkedin.com/in/fresh"));
        assertTrue(ValidationUtil.isValidLinkedInUrl("https://in.linkedin.com/in/vikram"));

        // Invalid formats
        assertFalse(ValidationUtil.isValidLinkedInUrl("not-a-url"));
        assertFalse(ValidationUtil.isValidLinkedInUrl("https://facebook.com/johndoe"));
        assertFalse(ValidationUtil.isValidLinkedInUrl("https://google.com"));
        assertFalse(ValidationUtil.isValidLinkedInUrl("https://linkedin.com/" + "a".repeat(250))); // exceeds 255
    }

    @Test
    @DisplayName("Years of experience and max mentees validation")
    void testExperienceAndMenteeValidation() {
        assertTrue(ValidationUtil.isValidYearsOfExperience(0));
        assertTrue(ValidationUtil.isValidYearsOfExperience(15));
        assertTrue(ValidationUtil.isValidYearsOfExperience(70));
        assertFalse(ValidationUtil.isValidYearsOfExperience(-1));
        assertFalse(ValidationUtil.isValidYearsOfExperience(71));
        assertFalse(ValidationUtil.isValidYearsOfExperience(null));

        assertTrue(ValidationUtil.isValidMaxMentees(0));
        assertTrue(ValidationUtil.isValidMaxMentees(5));
        assertTrue(ValidationUtil.isValidMaxMentees(100));
        assertFalse(ValidationUtil.isValidMaxMentees(-1));
        assertFalse(ValidationUtil.isValidMaxMentees(101));
        assertFalse(ValidationUtil.isValidMaxMentees(null));
    }

    @Test
    @DisplayName("ID, Topic, and Message validation")
    void testIdTopicMessageValidation() {
        assertTrue(ValidationUtil.isValidId(1));
        assertTrue(ValidationUtil.isValidId(9999));
        assertFalse(ValidationUtil.isValidId(0));
        assertFalse(ValidationUtil.isValidId(-5));
        assertFalse(ValidationUtil.isValidId(null));

        assertTrue(ValidationUtil.isValidTopic("Career Guidance"));
        assertFalse(ValidationUtil.isValidTopic(null));
        assertFalse(ValidationUtil.isValidTopic(""));
        assertFalse(ValidationUtil.isValidTopic("   "));
        assertFalse(ValidationUtil.isValidTopic("x".repeat(256)));

        assertTrue(ValidationUtil.isValidMessage("I would like advice on software roles."));
        assertFalse(ValidationUtil.isValidMessage(null));
        assertFalse(ValidationUtil.isValidMessage(""));
        assertFalse(ValidationUtil.isValidMessage("   "));
        assertFalse(ValidationUtil.isValidMessage("m".repeat(2001)));
    }

    @Test
    @DisplayName("Database connection failure detection and error formatting")
    void testDatabaseErrorFormatting() {
        // Normal error
        SQLException queryError = new SQLException("Syntax error in query", "42000");
        assertFalse(ValidationUtil.isConnectionFailure(queryError));
        String formatted = ValidationUtil.formatDatabaseErrorMessage(queryError, "Database error while querying");
        assertTrue(formatted.contains("Database error while querying"));
        assertTrue(formatted.contains("Syntax error in query"));

        // Connection failure
        SQLException connError = new SQLException("Communications link failure", "08S01");
        assertTrue(ValidationUtil.isConnectionFailure(connError));
        String connMsg = ValidationUtil.formatDatabaseErrorMessage(connError, "Database error");
        assertTrue(connMsg.contains("Database is currently unavailable. Please try again later."));

        // Sanitization of password in message
        SQLException leakError = new SQLException("Connection failed: password=secret123&user=root");
        String sanitized = ValidationUtil.formatDatabaseErrorMessage(leakError, "Database error");
        assertFalse(sanitized.contains("secret123"));
        assertTrue(sanitized.contains("******"));

        // Null exception
        String nullMsg = ValidationUtil.formatDatabaseErrorMessage(null, "Database error");
        assertTrue(nullMsg.contains("Database error"));
    }
}
