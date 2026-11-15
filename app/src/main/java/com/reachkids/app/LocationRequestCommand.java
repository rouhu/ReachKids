package com.reachkids.app;

/** Recognises the parent command that asks Reach Kids to report the phone location. */
public final class LocationRequestCommand {
    /** The command text a parent sends from the default contact. */
    public static final String COMMAND = "#Location";

    private LocationRequestCommand() {
    }

    /**
     * Only a message that carries the command asks for the location. Any other text is treated
     * as an ordinary message so a normal conversation never triggers a location reply.
     */
    public static boolean isLocationRequest(String body) {
        return SmsCommandMatcher.containsCommand(body, COMMAND);
    }
}