package com.reachkids.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PinSecurityTest {
    @Test
    public void verifierAcceptsCorrectPinAndRejectsIncorrectPin() {
        byte[] salt = PinSecurity.createSalt();
        byte[] verifier = PinSecurity.derive("2468".toCharArray(), salt);

        assertTrue(PinSecurity.verify("2468".toCharArray(), salt, verifier));
        assertFalse(PinSecurity.verify("2469".toCharArray(), salt, verifier));
    }

    @Test
    public void constantTimeEqualsRejectsUnequalLengthsAndContent() {
        assertTrue(PinSecurity.constantTimeEquals(new byte[]{1, 2}, new byte[]{1, 2}));
        assertFalse(PinSecurity.constantTimeEquals(new byte[]{1, 2}, new byte[]{1, 2, 3}));
        assertFalse(PinSecurity.constantTimeEquals(new byte[]{1, 2}, new byte[]{1, 3}));
    }
}
