package com.reachkids.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Covers the fallback PIN rules that do not need Android framework classes. */
public class TemporaryPinOverrideTest {
    @Test
    public void fallbackPinIsFourDigits() {
        assertEquals("1234", TemporaryPinOverride.FALLBACK_PIN);
        assertTrue(TemporaryPinOverride.FALLBACK_PIN.matches("[0-9]{4,}"));
    }

    @Test
    public void overrideLastsFiveMinutes() {
        assertEquals(5 * 60 * 1000L, TemporaryPinOverride.DURATION_MILLIS);
    }

    @Test
    public void saltedVerifierAcceptsOnlyTheFallbackPin() {
        byte[] salt = PinSecurity.createSalt();
        byte[] hash = PinSecurity.derive(TemporaryPinOverride.FALLBACK_PIN.toCharArray(), salt);

        assertTrue(PinSecurity.verify("1234".toCharArray(), salt, hash));
        assertFalse(PinSecurity.verify("1235".toCharArray(), salt, hash));
        assertFalse(PinSecurity.verify("4321".toCharArray(), salt, hash));
    }

    @Test
    public void eachActivationUsesAFreshSalt() {
        byte[] first = PinSecurity.createSalt();
        byte[] second = PinSecurity.createSalt();
        assertFalse(java.util.Arrays.equals(first, second));
    }
}
