package com.reachkids.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SmsCommandMatcherTest {
    @Test
    public void commandIsMatchedAsAWholeWord() {
        assertTrue(SmsCommandMatcher.containsCommand("#Location", "#Location"));
        assertTrue(SmsCommandMatcher.containsCommand("Please #Location now", "#Location"));
        assertTrue(SmsCommandMatcher.containsCommand("#Location", "#location"));
    }

    @Test
    public void longerWordsAreNotMatched() {
        assertFalse(SmsCommandMatcher.containsCommand("#LocationXYZ", "#Location"));
        assertFalse(SmsCommandMatcher.containsCommand("x#Location", "#Location"));
    }

    @Test
    public void unrelatedTextIsNotMatched() {
        assertFalse(SmsCommandMatcher.containsCommand("Hello", "#Location"));
        assertFalse(SmsCommandMatcher.containsCommand("", "#Location"));
    }

    @Test
    public void nullArgumentsAreHandled() {
        assertFalse(SmsCommandMatcher.containsCommand(null, "#Location"));
        assertFalse(SmsCommandMatcher.containsCommand("#Location", null));
        assertFalse(SmsCommandMatcher.containsCommand("#Location", ""));
    }
}
