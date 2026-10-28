package com.reachkids.app;

import android.telephony.PhoneNumberUtils;

/** Matches SMS reply/send destinations to current system-contact phone rows. */
public final class SmsAddressValidator {
    private SmsAddressValidator() {
    }

    public static boolean addressesMatch(String first, String second) {
        return first != null && second != null && PhoneNumberUtils.compare(first, second);
    }
}
