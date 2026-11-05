package com.libraflow.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Cryptographic utility for SHA-256 password hashing and verification.
 */
public final class PasswordHasher {
    private static final String SALT = "LibraFlowPro_Salt_2026!";

    private PasswordHasher() {}

    public static String hash(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String salted = SALT + rawPassword;
            byte[] encodedHash = digest.digest(salted.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedHash.length);
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public static boolean verify(String rawPassword, String expectedHash) {
        if (rawPassword == null || expectedHash == null) return false;
        return hash(rawPassword).equals(expectedHash);
    }
}
