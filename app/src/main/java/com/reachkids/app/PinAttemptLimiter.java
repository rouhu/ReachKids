package com.reachkids.app;

/** Back-off policy for failed parent-PIN attempts. */
public final class PinAttemptLimiter {
    private static final int MAX_DELAY_SECONDS = 15 * 60;

    private PinAttemptLimiter() {
    }

    public static long delayAfterFailureMilliseconds(int failedAttempts) {
        if (failedAttempts < 3) {
            return 0L;
        }
        int exponent = Math.min(failedAttempts - 3, 9);
        long seconds = Math.min(30L << exponent, MAX_DELAY_SECONDS);
        return seconds * 1000L;
    }
}
