package com.reachkids.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PinAttemptLimiterTest {
    @Test
    public void firstTwoFailuresHaveNoDelay() {
        assertEquals(0L, PinAttemptLimiter.delayAfterFailureMilliseconds(1));
        assertEquals(0L, PinAttemptLimiter.delayAfterFailureMilliseconds(2));
    }

    @Test
    public void failuresUseIncreasingCappedBackoff() {
        assertEquals(30_000L, PinAttemptLimiter.delayAfterFailureMilliseconds(3));
        assertEquals(60_000L, PinAttemptLimiter.delayAfterFailureMilliseconds(4));
        assertEquals(900_000L, PinAttemptLimiter.delayAfterFailureMilliseconds(20));
        assertTrue(PinAttemptLimiter.delayAfterFailureMilliseconds(20)
                >= PinAttemptLimiter.delayAfterFailureMilliseconds(4));
    }
}
