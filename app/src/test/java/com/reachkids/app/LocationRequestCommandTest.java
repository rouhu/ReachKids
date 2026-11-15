package com.reachkids.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LocationRequestCommandTest {
    @Test
    public void exactCommandIsRecognised() {
        assertTrue(LocationRequestCommand.isLocationRequest("#Location"));
    }

    @Test
    public void commandIsRecognisedRegardlessOfCaseAndPadding() {
        assertTrue(LocationRequestCommand.isLocationRequest("  #location  "));
        assertTrue(LocationRequestCommand.isLocationRequest("#LOCATION"));
        assertTrue(LocationRequestCommand.isLocationRequest("\n#Location\t"));
    }

    @Test
    public void otherMessagesAreNotLocationRequests() {
        assertFalse(LocationRequestCommand.isLocationRequest("Hello"));
        assertFalse(LocationRequestCommand.isLocationRequest("Location"));
        assertFalse(LocationRequestCommand.isLocationRequest("#"));
        assertFalse(LocationRequestCommand.isLocationRequest(""));
        assertFalse(LocationRequestCommand.isLocationRequest(null));
    }

    @Test
    public void commandInsideASentenceIsRecognised() {
        assertTrue(LocationRequestCommand.isLocationRequest("Please #Location"));
        assertTrue(LocationRequestCommand.isLocationRequest("Where are you? #Location"));
        assertTrue(LocationRequestCommand.isLocationRequest("#Location now"));
    }

    @Test
    public void longerWordsStartingWithTheCommandAreNotRequests() {
        assertFalse(LocationRequestCommand.isLocationRequest("#LocationXYZ"));
        assertFalse(LocationRequestCommand.isLocationRequest("#Locations"));
        assertFalse(LocationRequestCommand.isLocationRequest("x#Location"));
    }
}
