package com.reachkids.app;

import java.util.Calendar;

/** Evaluates a weekly child-home block schedule, including ranges that cross midnight. */
public final class BlockedHoursSchedule {
    public static final int MINUTES_PER_DAY = 24 * 60;

    private BlockedHoursSchedule() {
    }

    public static boolean isBlocked(Calendar now, ParentPreferences preferences) {
        int currentDay = now.get(Calendar.DAY_OF_WEEK);
        int currentMinute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        if (isBlockedOnDay(currentDay, currentMinute, preferences)) {
            return true;
        }
        int previousDay = currentDay == Calendar.SUNDAY ? Calendar.SATURDAY : currentDay - 1;
        return crossesMidnight(previousDay, preferences)
                && currentMinute < preferences.getBlockedEndMinute(previousDay);
    }

    private static boolean isBlockedOnDay(int day, int currentMinute, ParentPreferences preferences) {
        if (!preferences.isBlockedHoursEnabled(day)) {
            return false;
        }
        int startMinute = preferences.getBlockedStartMinute(day);
        int endMinute = preferences.getBlockedEndMinute(day);
        return isWithinRangeOnStartDay(currentMinute, startMinute, endMinute);
    }

    private static boolean crossesMidnight(int day, ParentPreferences preferences) {
        return preferences.isBlockedHoursEnabled(day)
                && preferences.getBlockedStartMinute(day) > preferences.getBlockedEndMinute(day);
    }

    static boolean isWithinRangeOnStartDay(int currentMinute, int startMinute, int endMinute) {
        if (startMinute < endMinute) {
            return currentMinute >= startMinute && currentMinute < endMinute;
        }
        return startMinute > endMinute && currentMinute >= startMinute;
    }

    public static int parseTime(String value) {
        if (value == null || !value.matches("([01]?[0-9]|2[0-3]):[0-5][0-9]")) {
            return -1;
        }
        String[] parts = value.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    public static String formatTime(int minutes) {
        if (minutes < 0 || minutes >= MINUTES_PER_DAY) {
            return "";
        }
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes / 60, minutes % 60);
    }
}