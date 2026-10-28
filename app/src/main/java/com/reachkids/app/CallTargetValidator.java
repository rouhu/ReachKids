package com.reachkids.app;

/** Keeps invalid or blank Contacts-provider phone rows out of the call action. */
public final class CallTargetValidator {
    private CallTargetValidator() {
    }

    public static boolean hasCallableNumber(String phoneNumber) {
        return phoneNumber != null && !phoneNumber.trim().isEmpty();
    }
}
