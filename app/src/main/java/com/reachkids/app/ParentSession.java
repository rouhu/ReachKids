package com.reachkids.app;

/** In-memory parent-administration session; it expires when the app is inactive. */
public final class ParentSession {
    public static final long TIMEOUT_MILLISECONDS = 5 * 60 * 1000L;
    private static long authenticatedAt;

    private ParentSession() {
    }

    public static void open() {
        authenticatedAt = System.currentTimeMillis();
    }

    public static boolean isOpen() {
        return authenticatedAt > 0L
                && System.currentTimeMillis() - authenticatedAt < TIMEOUT_MILLISECONDS;
    }

    public static void touch() {
        if (isOpen()) {
            authenticatedAt = System.currentTimeMillis();
        }
    }

    public static void close() {
        authenticatedAt = 0L;
    }
}
