package com.reachkids.app;

import java.util.regex.Pattern;

/** Matches a hash command in an SMS body as a whole word. */
public final class SmsCommandMatcher {
    private SmsCommandMatcher() {
    }

    /**
     * True when the body carries the command as a whole word, so {@code #Location} is
     * recognised but {@code #LocationXYZ} is not. The match ignores letter case.
     */
    public static boolean containsCommand(String body, String command) {
        if (body == null || command == null || command.isEmpty()) {
            return false;
        }
        Pattern pattern = Pattern.compile(
                "(^|[^A-Za-z0-9])" + Pattern.quote(command) + "([^A-Za-z0-9]|$)",
                Pattern.CASE_INSENSITIVE);
        return pattern.matcher(body).find();
    }
}
