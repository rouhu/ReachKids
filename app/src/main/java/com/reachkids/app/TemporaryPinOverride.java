package com.reachkids.app;

import android.content.Context;

import java.util.Arrays;

/**
 * A short-lived fallback PIN that the default contact can activate by SMS.
 *
 * <p>The override lets a parent who has forgotten the parent PIN still reach the parent area.
 * It is deliberately limited:
 *
 * <ul>
 *     <li>Only the default contact can activate it.</li>
 *     <li>It lasts a few minutes and then stops working on its own.</li>
 *     <li>The real parent PIN always keeps working and is never changed.</li>
 *     <li>The fallback PIN is stored as a salted verifier, never as plain text.</li>
 * </ul>
 */
public final class TemporaryPinOverride {
    /** The PIN that works while an override is active. */
    public static final String FALLBACK_PIN = "1234";
    /** How long an activated override stays usable. */
    public static final long DURATION_MILLIS = 5 * 60 * 1000L;

    private TemporaryPinOverride() {
    }

    /** Activates the fallback PIN for the default contact and returns its expiry time. */
    public static long activate(Context context) {
        ParentPreferences preferences = new ParentPreferences(context);
        byte[] salt = PinSecurity.createSalt();
        byte[] hash = PinSecurity.derive(FALLBACK_PIN.toCharArray(), salt);
        long expiresAt = System.currentTimeMillis() + DURATION_MILLIS;
        preferences.saveTemporaryPin(salt, hash, expiresAt);
        Arrays.fill(hash, (byte) 0);
        return expiresAt;
    }

    /** True when the fallback PIN may be used right now. */
    public static boolean isActive(ParentPreferences preferences, long nowMillis) {
        return preferences.hasTemporaryPin() && preferences.temporaryPinExpiresAt() > nowMillis;
    }

    /** True when the candidate matches the active fallback PIN. */
    public static boolean matches(ParentPreferences preferences, char[] candidate,
            long nowMillis) {
        if (!isActive(preferences, nowMillis) || candidate == null) {
            return false;
        }
        return preferences.verifyTemporaryPin(candidate);
    }

    /** Removes the fallback PIN so it can no longer be used. */
    public static void clear(ParentPreferences preferences) {
        preferences.clearTemporaryPin();
    }

    /** A short description of how long the override still lasts. */
    public static String remainingText(ParentPreferences preferences, long nowMillis) {
        long remaining = preferences.temporaryPinExpiresAt() - nowMillis;
        if (remaining <= 0L) {
            return "expired";
        }
        long minutes = (remaining + 59999L) / 60000L;
        return minutes + " minute(s)";
    }
}
