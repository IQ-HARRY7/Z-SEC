package com.iq.zsec.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Handles all cryptographic operations for Z_SEC.
 * Uses SHA-256 with a salted "sandwich" scheme to resist rainbow table attacks.
 * Comparison uses constant-time equality to prevent timing side-channel attacks.
 */
public class SecurityUtils {

    private static final int SALT_BYTE_LENGTH = 16;
    private static final String ALGORITHM = "SHA-256";

    private SecurityUtils() { /* no instances */ }

    /**
     * Generates a cryptographically random hex salt.
     */
    public static String generateSalt() {
        byte[] bytes = new byte[SALT_BYTE_LENGTH];
        new SecureRandom().nextBytes(bytes);
        return bytesToHex(bytes);
    }

    /**
     * Produces a salted SHA-256 hash: SHA256(salt + password + salt)
     */
    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            String salted = salt + password + salt;
            return bytesToHex(digest.digest(salted.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 unavailable on this device.", e);
        }
    }

    /**
     * Verifies a password against a stored salt and hash.
     * Uses constant-time comparison to prevent timing attacks.
     */
    public static boolean verifyPassword(String input, String salt, String storedHash) {
        if (input == null || salt == null || storedHash == null) return false;
        String inputHash = hashPassword(input, salt);
        return constantTimeEquals(inputHash, storedHash);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= (a.charAt(i) ^ b.charAt(i));
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
}
