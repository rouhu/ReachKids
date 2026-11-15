package com.reachkids.app;

/** The parent-page reminder shown while no default contact has been selected. */
public final class DefaultContactReminder {
    private DefaultContactReminder() {
    }

    /** True when the parent page has to show the reminder. */
    public static boolean isNeeded(boolean hasDefaultContact) {
        return !hasDefaultContact;
    }

    /** The reminder text shown to the parent in red. */
    public static String text() {
        return "Reminder: set a default contact. Without one, the #Location and #ResetPin "
                + "commands cannot be answered.";
    }
}
