package com.jasavast.core.util;

import org.apache.commons.lang3.RandomStringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

public final class RandomUtil {

    private static final int KEY_LENGTH = 20; // ~119 bits of entropy (62^20)
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private RandomUtil() {
    }

    /** Random alphanumeric key for activation / password reset links. */
    public static String generateKey() {
        return RandomStringUtils.random(KEY_LENGTH, 0, 0, true, true, null, SECURE_RANDOM);
    }

    /** Short random reference (6 chars). Not suitable as a secret. */
    public static String getRandomRef() {
        return RandomStringUtils.random(6, 0, 0, true, true, null, SECURE_RANDOM);
    }

    /**
     * SHA-256 hex digest. One-time keys are stored hashed so a database read does not
     * yield usable activation/reset links. A plain hash (no salt/KDF) is sufficient because
     * the keys are high-entropy random values, not user-chosen passwords.
     */
    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
