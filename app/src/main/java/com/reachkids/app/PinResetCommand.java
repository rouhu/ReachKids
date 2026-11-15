package com.reachkids.app;

/** Recognises the parent command that opens the temporary parent-area PIN. */
public final class PinResetCommand {
    /** The command text a parent sends from the default contact. */
    public static final String COMMAND = "#ResetPin";

    private PinResetCommand() {
    }

    /**
     * Only a message that carries the command asks for the temporary PIN. Any other text is
     * treated as an ordinary message so a normal conversation never opens the parent area.
     */
    public static boolean isPinResetRequest(String body) {
        return SmsCommandMatcher.containsCommand(body, COMMAND);
    }
}
