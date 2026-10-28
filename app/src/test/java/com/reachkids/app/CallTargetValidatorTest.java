package com.reachkids.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CallTargetValidatorTest {
    @Test
    public void callableNumberRequiresNonWhitespaceValue() {
        assertTrue(CallTargetValidator.hasCallableNumber("+1 555 0100"));
        assertFalse(CallTargetValidator.hasCallableNumber(null));
        assertFalse(CallTargetValidator.hasCallableNumber(""));
        assertFalse(CallTargetValidator.hasCallableNumber("   "));
    }
}