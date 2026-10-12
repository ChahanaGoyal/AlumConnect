package com.alumconnect.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Password Utility & Security Tests")
class PasswordUtilTest {

    @Test
    @DisplayName("Password hashing produces salted PBKDF2 hash and verifies correctly")
    void testHashAndVerifyPassword() {
        String plain = "SecurePass123!";
        String hash = PasswordUtil.hashPassword(plain);

        assertNotNull(hash);
        assertTrue(hash.startsWith("PBKDF2$10000$"), "Hash should start with algorithm and iteration count");
        assertNotEquals(plain, hash, "Hash must not equal plain text");

        assertTrue(PasswordUtil.verifyPassword(plain, hash), "Password verification should succeed with correct password");
        assertFalse(PasswordUtil.verifyPassword("WrongPassword123!", hash), "Password verification should fail with wrong password");
        assertFalse(PasswordUtil.verifyPassword("", hash), "Empty password should fail verification");
        assertFalse(PasswordUtil.verifyPassword(null, hash), "Null password should fail verification");
        assertFalse(PasswordUtil.verifyPassword(plain, null), "Null hash should fail verification");
        assertFalse(PasswordUtil.verifyPassword(plain, "corrupted_hash_format"), "Invalid hash format should return false");
    }

    @Test
    @DisplayName("Different salts are generated for identical passwords")
    void testDifferentSaltsForSamePassword() {
        String password = "Password@2026";
        String hash1 = PasswordUtil.hashPassword(password);
        String hash2 = PasswordUtil.hashPassword(password);

        assertNotEquals(hash1, hash2, "Repeated hashes of the same password must produce different salts/hashes");
        assertTrue(PasswordUtil.verifyPassword(password, hash1));
        assertTrue(PasswordUtil.verifyPassword(password, hash2));
    }

    @Test
    @DisplayName("Password complexity validation rules")
    void testPasswordComplexityRules() {
        // Valid password
        assertTrue(PasswordUtil.meetsRequirements("StrongP@ss1"));
        assertTrue(PasswordUtil.getPasswordRequirementErrors("StrongP@ss1").isEmpty());

        // Too short (< 8 chars)
        assertFalse(PasswordUtil.meetsRequirements("Sh0rt!"));
        List<String> shortErrors = PasswordUtil.getPasswordRequirementErrors("Sh0rt!");
        assertTrue(shortErrors.stream().anyMatch(e -> e.contains("at least 8 characters")));

        // Missing uppercase
        assertFalse(PasswordUtil.meetsRequirements("lowercase123!"));
        List<String> upperErrors = PasswordUtil.getPasswordRequirementErrors("lowercase123!");
        assertTrue(upperErrors.stream().anyMatch(e -> e.contains("uppercase letter")));

        // Missing lowercase
        assertFalse(PasswordUtil.meetsRequirements("UPPERCASE123!"));
        List<String> lowerErrors = PasswordUtil.getPasswordRequirementErrors("UPPERCASE123!");
        assertTrue(lowerErrors.stream().anyMatch(e -> e.contains("lowercase letter")));

        // Missing digit
        assertFalse(PasswordUtil.meetsRequirements("NoDigitsHere!"));
        List<String> digitErrors = PasswordUtil.getPasswordRequirementErrors("NoDigitsHere!");
        assertTrue(digitErrors.stream().anyMatch(e -> e.contains("digit")));

        // Missing special character
        assertFalse(PasswordUtil.meetsRequirements("NoSpecialChar123"));
        List<String> specialErrors = PasswordUtil.getPasswordRequirementErrors("NoSpecialChar123");
        assertTrue(specialErrors.stream().anyMatch(e -> e.contains("special character")));

        // Null password
        assertFalse(PasswordUtil.meetsRequirements(null));
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hashPassword(null));
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hashPassword(""));
    }
}
