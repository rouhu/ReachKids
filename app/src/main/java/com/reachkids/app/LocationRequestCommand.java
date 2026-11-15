package com.reachkids.app;

import java.util.regex.Pattern;

/** Recognises the parent command that asks Reach Kids to report the phone location. */
public final class LocationRequestCommand {
    /** The command text a parent sends from the default contact. */
    public static final String COMMAND = "#Location";

    /**
     * The command counts when it appears as a whole word, so both a plain {@code #Location}
     * and a sentence such as {@code Please #Location} are answered, while a longer word
     * that merely starts the same way is not.
     */
    private static final Pattern COMMAND_PATTERN = Pattern.compile(
            "(^|[^A-Za-z0-9])#location([^A-Za-z0-9]|$)", Pattern.CASE_INSENSITIVE);

    private LocationRequestCommand() {
    }

    /**
     * Only a message that carries the command asks for the location. Any other text is treated
     * as an ordinary message so a normal conversation never triggers a location reply.
     */
    public static boolean isLocationRequest(String body) {
        return body != null && COMMAND_PATTERN.matcher(body).find();
    }
}