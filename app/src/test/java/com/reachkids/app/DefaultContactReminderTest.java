package com.reachkids.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DefaultContactReminderTest {
    @Test
    public void reminderIsNeededWhenNoDefaultContactIsSet() {
        assertTrue(DefaultContactReminder.isNeeded(false));
    }

    @Test
    public void reminderIsNotNeededWhenADefaultContactIsSet() {
        assertFalse(DefaultContactReminder.isNeeded(true));
    }

    @Test
    public void reminderTextNamesBothCommands() {
        String text = DefaultContactReminder.text();
        assertTrue(text.contains("#Location"));
        assertTrue(text.contains("#ResetPin"));
        assertTrue(text.contains("default contact"));
    }
}
