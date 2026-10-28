package com.reachkids.app;

/** Prevents blank SMS bodies from entering the send path. */
public final class SmsBodyValidator {
    private SmsBodyValidator() {
    }

    public static boolean hasSendableBody(String body) {
        return body != null && !body.trim().isEmpty();
    }
}
