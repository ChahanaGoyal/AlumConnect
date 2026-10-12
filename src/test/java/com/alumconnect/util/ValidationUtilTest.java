package com.alumconnect.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
