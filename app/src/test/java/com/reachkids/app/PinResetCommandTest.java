package com.reachkids.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PinResetCommandTest {
    @Test
    public void exactCommandIsRecognised() {
        assertTrue(PinResetCommand.isPinResetRequest("#ResetPin"));
    }

    @Test
    public void commandIsRecognisedRegardlessOfCaseAndPadding() {
        assertTrue(PinResetCommand.isPinResetRequest("  #resetpin  "));
        assertTrue(PinResetCommand.isPinResetRequest("#RESETPIN"));
        assertTrue(PinResetCommand.isPinResetRequest("\n#ResetPin\t"));
    }

    @Test
    public void commandInsideASentenceIsRecognised() {
        assertTrue(PinResetCommand.isPinResetRequest("Please #ResetPin"));
        assertTrue(PinResetCommand.isPinResetRequest("#ResetPin now"));
    }

    @Test
    public void otherMessagesAreNotPinResetRequests() {
        assertFalse(PinResetCommand.isPinResetRequest("Hello"));
        assertFalse(PinResetCommand.isPinResetRequest("ResetPin"));
        assertFalse(PinResetCommand.isPinResetRequest("#"));
        assertFalse(PinResetCommand.isPinResetRequest(""));
        assertFalse(PinResetCommand.isPinResetRequest(null));
    }

    @Test
    public void longerWordsStartingWithTheCommandAreNotRequests() {
        assertFalse(PinResetCommand.isPinResetRequest("#ResetPinXYZ"));
        assertFalse(PinResetCommand.isPinResetRequest("x#ResetPin"));
    }

    @Test
    public void locationCommandIsNotTreatedAsAPinReset() {
        assertFalse(PinResetCommand.isPinResetRequest("#Location"));
    }
}
