package com.reachkids.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

/** App-local security and preference state. Contacts remain in the system provider. */
public final class ParentPreferences {
    private static final String PREFERENCES_NAME = "parent_settings";
    private static final String KEY_PIN_SALT = "pin_salt";
    private static final String KEY_PIN_HASH = "pin_hash";
    private static final String KEY_FAILED_ATTEMPTS = "failed_attempts";
    private static final String KEY_LOCKED_UNTIL = "locked_until";
    private static final String KEY_CAMERA_DISABLED = "camera_disabled";
    private static final String KEY_RINGER_MUTED = "ringer_muted";
    private static final String KEY_PREVIOUS_RING_VOLUME = "previous_ring_volume";
    private static final String KEY_KIOSK_ENABLED = "kiosk_enabled";
    private static final String KEY_CALLING_DISABLED = "calling_disabled";
    private static final String KEY_MESSAGES_DISABLED = "messages_disabled";
    private static final String KEY_BLOCKED_HOURS_ENABLED_PREFIX = "blocked_hours_enabled_";
    private static final String KEY_BLOCKED_HOURS_START_PREFIX = "blocked_hours_start_";
    private static final String KEY_BLOCKED_HOURS_END_PREFIX = "blocked_hours_end_";
    private static final String KEY_QUICK_CALL_1_LABEL = "quick_call_1_label";
    private static final String KEY_QUICK_CALL_1_PHONE_DATA_ID = "quick_call_1_phone_data_id";
    private static final String KEY_QUICK_CALL_2_LABEL = "quick_call_2_label";
    private static final String KEY_QUICK_CALL_2_PHONE_DATA_ID = "quick_call_2_phone_data_id";

    private final SharedPreferences preferences;

    public ParentPreferences(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public boolean hasPin() {
        return preferences.contains(KEY_PIN_SALT) && preferences.contains(KEY_PIN_HASH);
    }

    public void savePin(char[] pin) {
        byte[] salt = PinSecurity.createSalt();
        byte[] hash = PinSecurity.derive(pin, salt);
        preferences.edit()
                .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString(KEY_PIN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKED_UNTIL, 0L)
                .apply();
    }

    public boolean verifyPin(char[] candidate) {
        String savedSalt = preferences.getString(KEY_PIN_SALT, null);
        String savedHash = preferences.getString(KEY_PIN_HASH, null);
        if (savedSalt == null || savedHash == null) {
            return false;
        }
        return PinSecurity.verify(candidate,
                Base64.decode(savedSalt, Base64.NO_WRAP),
                Base64.decode(savedHash, Base64.NO_WRAP));
    }

    public boolean isPinEntryLocked() {
        return System.currentTimeMillis() < preferences.getLong(KEY_LOCKED_UNTIL, 0L);
    }

    public long millisecondsUntilPinEntryAllowed() {
        return Math.max(0L, preferences.getLong(KEY_LOCKED_UNTIL, 0L) - System.currentTimeMillis());
    }

    public void recordFailedPinAttempt() {
        int failures = preferences.getInt(KEY_FAILED_ATTEMPTS, 0) + 1;
        long delay = PinAttemptLimiter.delayAfterFailureMilliseconds(failures);
        preferences.edit()
                .putInt(KEY_FAILED_ATTEMPTS, failures)
                .putLong(KEY_LOCKED_UNTIL, System.currentTimeMillis() + delay)
                .apply();
    }

    public void clearFailedPinAttempts() {
        preferences.edit()
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKED_UNTIL, 0L)
                .apply();
    }

    public boolean isCameraDisabled() {
        return preferences.getBoolean(KEY_CAMERA_DISABLED, false);
    }

    public void setCameraDisabled(boolean disabled) {
        preferences.edit().putBoolean(KEY_CAMERA_DISABLED, disabled).apply();
    }

    public boolean isRingerMuted() {
        return preferences.getBoolean(KEY_RINGER_MUTED, false);
    }

    public void setRingerMuted(boolean muted) {
        preferences.edit().putBoolean(KEY_RINGER_MUTED, muted).apply();
    }

    public int getPreviousRingVolume() {
        return preferences.getInt(KEY_PREVIOUS_RING_VOLUME, -1);
    }

    public void setPreviousRingVolume(int volume) {
        preferences.edit().putInt(KEY_PREVIOUS_RING_VOLUME, volume).apply();
    }

    public boolean isKioskEnabled() {
        return preferences.getBoolean(KEY_KIOSK_ENABLED, false);
    }

    public void setKioskEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_KIOSK_ENABLED, enabled).apply();
    }

    public boolean isCallingDisabled() {
        return preferences.getBoolean(KEY_CALLING_DISABLED, false);
    }

    public void setCallingDisabled(boolean disabled) {
        preferences.edit().putBoolean(KEY_CALLING_DISABLED, disabled).apply();
    }

    public boolean areMessagesDisabled() {
        return preferences.getBoolean(KEY_MESSAGES_DISABLED, false);
    }

    public void setMessagesDisabled(boolean disabled) {
        preferences.edit().putBoolean(KEY_MESSAGES_DISABLED, disabled).apply();
    }

    public boolean isBlockedHoursEnabled(int calendarDay) {
        return preferences.getBoolean(blockedHoursEnabledKey(calendarDay), false);
    }

    public int getBlockedStartMinute(int calendarDay) {
        return preferences.getInt(blockedHoursStartKey(calendarDay), 0);
    }

    public int getBlockedEndMinute(int calendarDay) {
        return preferences.getInt(blockedHoursEndKey(calendarDay), 0);
    }

    public void saveBlockedHours(int calendarDay, boolean enabled, int startMinute, int endMinute) {
        if (startMinute < 0 || startMinute >= BlockedHoursSchedule.MINUTES_PER_DAY
                || endMinute < 0 || endMinute >= BlockedHoursSchedule.MINUTES_PER_DAY) {
            throw new IllegalArgumentException("Blocked-hours times must be within one day");
        }
        preferences.edit()
                .putBoolean(blockedHoursEnabledKey(calendarDay), enabled)
                .putInt(blockedHoursStartKey(calendarDay), startMinute)
                .putInt(blockedHoursEndKey(calendarDay), endMinute)
                .apply();
    }

    /** Stores only a parent-provided label and the selected system Contacts phone-row ID. */
    public void saveQuickCall(int slot, String label, long phoneDataId) {
        if (phoneDataId < 0L || label == null || label.trim().isEmpty()) {
            throw new IllegalArgumentException("A label and system contact are required");
        }
        preferences.edit()
                .putString(quickCallLabelKey(slot), label.trim())
                .putLong(quickCallPhoneDataIdKey(slot), phoneDataId)
                .apply();
    }

    public boolean hasQuickCall(int slot) {
        return preferences.contains(quickCallLabelKey(slot))
                && preferences.contains(quickCallPhoneDataIdKey(slot));
    }

    public String getQuickCallLabel(int slot) {
        return preferences.getString(quickCallLabelKey(slot), "");
    }

    public long getQuickCallPhoneDataId(int slot) {
        return preferences.getLong(quickCallPhoneDataIdKey(slot), -1L);
    }

    public void clearQuickCall(int slot) {
        preferences.edit()
                .remove(quickCallLabelKey(slot))
                .remove(quickCallPhoneDataIdKey(slot))
                .apply();
    }

    private String quickCallLabelKey(int slot) {
        switch (slot) {
            case 1:
                return KEY_QUICK_CALL_1_LABEL;
            case 2:
                return KEY_QUICK_CALL_2_LABEL;
            default:
                throw new IllegalArgumentException("Quick-call slot must be 1 or 2");
        }
    }

    private String blockedHoursEnabledKey(int calendarDay) {
        return KEY_BLOCKED_HOURS_ENABLED_PREFIX + validatedCalendarDay(calendarDay);
    }

    private String blockedHoursStartKey(int calendarDay) {
        return KEY_BLOCKED_HOURS_START_PREFIX + validatedCalendarDay(calendarDay);
    }

    private String blockedHoursEndKey(int calendarDay) {
        return KEY_BLOCKED_HOURS_END_PREFIX + validatedCalendarDay(calendarDay);
    }

    private int validatedCalendarDay(int calendarDay) {
        if (calendarDay < java.util.Calendar.SUNDAY || calendarDay > java.util.Calendar.SATURDAY) {
            throw new IllegalArgumentException("Calendar day must be Sunday through Saturday");
        }
        return calendarDay;
    }

    private String quickCallPhoneDataIdKey(int slot) {
        switch (slot) {
            case 1:
                return KEY_QUICK_CALL_1_PHONE_DATA_ID;
            case 2:
                return KEY_QUICK_CALL_2_PHONE_DATA_ID;
            default:
                throw new IllegalArgumentException("Quick-call slot must be 1 or 2");
        }
    }
}
