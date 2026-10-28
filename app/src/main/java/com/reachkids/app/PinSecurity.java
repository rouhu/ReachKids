package com.reachkids.app;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Creates and verifies non-reversible PIN verifiers. */
public final class PinSecurity {
    public static final int SALT_BYTES = 16;
    public static final int HASH_BYTES = 32;
    public static final int ITERATIONS = 120000;

    private PinSecurity() {
    }

    public static byte[] createSalt() {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    public static byte[] derive(char[] pin, byte[] salt) {
        if (pin == null || salt == null) {
            throw new IllegalArgumentException("PIN and salt are required");
        }

        PBEKeySpec keySpec = new PBEKeySpec(pin, salt, ITERATIONS, HASH_BYTES * 8);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
                    .generateSecret(keySpec)
                    .getEncoded();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("PBKDF2 is unavailable on this device", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not derive PIN verifier", exception);
        } finally {
            keySpec.clearPassword();
        }
    }

    public static boolean verify(char[] candidate, byte[] salt, byte[] expectedHash) {
        if (candidate == null || salt == null || expectedHash == null) {
            return false;
        }
        return constantTimeEquals(derive(candidate, salt), expectedHash);
    }

    static boolean constantTimeEquals(byte[] first, byte[] second) {
        if (first == null || second == null) {
            return false;
        }
        int difference = first.length ^ second.length;
        int maximum = Math.max(first.length, second.length);
        for (int i = 0; i < maximum; i++) {
            byte firstByte = i < first.length ? first[i] : 0;
            byte secondByte = i < second.length ? second[i] : 0;
            difference |= firstByte ^ secondByte;
        }
        return difference == 0;
    }
}
