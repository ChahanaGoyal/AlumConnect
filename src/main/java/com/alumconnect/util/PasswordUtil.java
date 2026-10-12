package com.alumconnect.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Security utility class for password hashing, verification, and complexity validation.
 *
 * <p>Uses PBKDF2WithHmacSHA256 with a cryptographically secure random salt and
 * 10,000 iterations to generate slow-to-brute-force password hashes.
 * Password hashes are stored in the format:
 * {@code PBKDF2$<iterations>$<saltHex>$<hashHex>}.
 *
 * <p>All hash comparisons use constant-time byte comparisons to prevent timing attacks.
 */
public final class PasswordUtil {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final String HASH_PREFIX = "PBKDF2";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // Password requirements: min 8 chars, 1 uppercase, 1 lowercase, 1 digit, 1 special char
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]");

    private PasswordUtil() {
        // Utility class
    }

    /**
     * Hashes a raw plain-text password using PBKDF2 with HMAC-SHA256 and a random salt.
     *
     * @param plainPassword the plain-text password to hash
     * @return the formatted hash string {@code PBKDF2$<iterations>$<saltHex>$<hashHex>}
     * @throws IllegalArgumentException if the password is null or blank
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password to hash must not be null or empty");
        }

        byte[] salt = new byte[SALT_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(salt);

        byte[] hash = pbkdf2(plainPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS);

        return HASH_PREFIX + "$" + ITERATIONS + "$" + bytesToHex(salt) + "$" + bytesToHex(hash);
    }

    /**
     * Verifies a plain-text password against a stored PBKDF2 hash.
     *
     * @param plainPassword the plain-text password to verify
     * @param storedHash    the stored hash string
     * @return {@code true} if the password matches the hash; {@code false} otherwise
     */
    public static boolean verifyPassword(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }

        String[] parts = storedHash.split("\\$");
        if (parts.length != 4 || !HASH_PREFIX.equals(parts[0])) {
            return false;
        }

        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = hexToBytes(parts[2]);
            byte[] expectedHash = hexToBytes(parts[3]);

            byte[] computedHash = pbkdf2(plainPassword.toCharArray(), salt, iterations, expectedHash.length * 8);

            return constantTimeEquals(expectedHash, computedHash);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks whether a plain password meets platform security requirements:
     * <ul>
     *   <li>At least 8 characters long</li>
     *   <li>At least one uppercase letter (A-Z)</li>
     *   <li>At least one lowercase letter (a-z)</li>
     *   <li>At least one digit (0-9)</li>
     *   <li>At least one special character (!@#$%^&*...)</li>
     * </ul>
     *
     * @param password the password to evaluate
     * @return {@code true} if all complexity criteria are satisfied
     */
    public static boolean meetsRequirements(String password) {
        return getPasswordRequirementErrors(password).isEmpty();
    }

    /**
     * Returns a list of validation error messages describing any missing
     * requirements for the given password.
     *
     * @param password the candidate password
     * @return list of error strings; empty if password is fully compliant
     */
    public static List<String> getPasswordRequirementErrors(String password) {
        List<String> errors = new ArrayList<>();
        if (password == null || password.length() < 8) {
            errors.add("Password must be at least 8 characters long.");
            if (password == null) {
                return errors;
            }
        }
        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            errors.add("Password must contain at least one uppercase letter (A-Z).");
        }
        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            errors.add("Password must contain at least one lowercase letter (a-z).");
        }
        if (!DIGIT_PATTERN.matcher(password).find()) {
            errors.add("Password must contain at least one digit (0-9).");
        }
        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            errors.add("Password must contain at least one special character (e.g. !@#$%^&*).");
        }
        return errors;
    }

    // ── Internal crypto helpers ───────────────────────────────────────────────

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) {
        try {
            KeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Failed to calculate PBKDF2 hash", e);
        }
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have an even length");
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
