package com.reachkids.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SmsBodyValidatorTest {
    @Test
    public void sendableBodyRequiresNonWhitespaceText() {
        assertTrue(SmsBodyValidator.hasSendableBody("Hello"));
        assertFalse(SmsBodyValidator.hasSendableBody(null));
        assertFalse(SmsBodyValidator.hasSendableBody(""));
        assertFalse(SmsBodyValidator.hasSendableBody("   \n\t"));
    }
}
