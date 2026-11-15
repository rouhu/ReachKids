package com.reachkids.app;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Builds the plain-text SMS body that reports the phone location to the default contact. */
public final class LocationReplyText {
    private static final String MAP_LINK_PREFIX = "https://www.google.com/maps?q=";

    private LocationReplyText() {
    }

    public static String buildMessage(double latitude, double longitude, String provider,
            float accuracyMeters, boolean stale, long fixTimeMillis) {
        StringBuilder message = new StringBuilder();
        message.append(stale
                ? "Reach Kids location (last known fix)"
                : "Reach Kids location");
        message.append("\nLatitude ").append(coordinates(latitude, longitude));
        message.append("\nAccuracy ").append(formatAccuracy(accuracyMeters))
                .append(" (").append(describeProvider(provider)).append(")");
        message.append("\nFix time ").append(formatFixTime(fixTimeMillis));
        message.append("\n").append(mapLink(latitude, longitude));
        return message.toString();
    }

    /** Names the provider that produced the fix so the parent knows how precise it is. */
    public static String describeProvider(String provider) {
        if (provider == null || provider.trim().isEmpty()) {
            return "unknown source";
        }
        String name = provider.trim();
        if ("gps".equalsIgnoreCase(name)) {
            return "GPS";
        }
        if ("network".equalsIgnoreCase(name)) {
            return "network";
        }
        if ("fused".equalsIgnoreCase(name)) {
            return "fused";
        }
        if ("passive".equalsIgnoreCase(name)) {
            return "passive";
        }
        return name;
    }

    /** Adds the reason a live fix was not available, so the parent knows what happened. */
    public static String withWarning(String message, String warning) {
        if (warning == null || warning.trim().isEmpty()) {
            return message;
        }
        return message + "\n" + warning.trim();
    }

    /** Latitude and longitude in decimal degrees with sub-metre precision. */
    public static String coordinates(double latitude, double longitude) {
        return String.format(Locale.US, "%.6f, %.6f", latitude, longitude);
    }

    /** The same coordinates without a space, so the value is a valid URL query. */
    public static String urlCoordinates(double latitude, double longitude) {
        return String.format(Locale.US, "%.6f,%.6f", latitude, longitude);
    }

    public static String mapLink(double latitude, double longitude) {
        return MAP_LINK_PREFIX + urlCoordinates(latitude, longitude);
    }

    private static String formatAccuracy(float accuracyMeters) {
        if (accuracyMeters <= 0f) {
            return "unknown";
        }
        return String.format(Locale.US, "%.0f m", accuracyMeters);
    }

    private static String formatFixTime(long fixTimeMillis) {
        if (fixTimeMillis <= 0L) {
            return "unknown";
        }
        return new SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.getDefault())
                .format(new Date(fixTimeMillis));
    }
}