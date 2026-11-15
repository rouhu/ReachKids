package com.reachkids.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LocationReplyTextTest {
    @Test
    public void messageContainsCoordinatesAndMapLink() {
        String message = LocationReplyText.buildMessage(51.507351, -0.127758,
                "gps", 8.5f, false, 1700000000000L);
        assertTrue(message.contains("51.507351"));
        assertTrue(message.contains("-0.127758"));
        assertTrue(message.contains("https://www.google.com/maps?q=51.507351,-0.127758"));
        assertTrue(message.contains("GPS"));
    }

    @Test
    public void staleFixIsMarkedAsLastKnown() {
        String message = LocationReplyText.buildMessage(1.0, 2.0, "network", 1200f,
                true, 1700000000000L);
        assertTrue(message.contains("last known fix"));
    }

    @Test
    public void freshFixIsNotMarkedAsStale() {
        String message = LocationReplyText.buildMessage(1.0, 2.0, "gps", 5f,
                false, 1700000000000L);
        assertFalse(message.contains("last known fix"));
    }

    @Test
    public void unknownAccuracyAndFixTimeAreReported() {
        String message = LocationReplyText.buildMessage(1.0, 2.0, "gps", 0f, false, 0L);
        assertTrue(message.contains("unknown"));
    }

    @Test
    public void coordinatesUseDecimalPointSeparator() {
        assertTrue(LocationReplyText.coordinates(51.5, -0.25).contains("51.500000"));
        assertTrue(LocationReplyText.coordinates(51.5, -0.25).contains("-0.250000"));
    }

    @Test
    public void providerNameIsMadeReadable() {
        assertTrue(LocationReplyText.describeProvider("gps").contains("GPS"));
        assertTrue(LocationReplyText.describeProvider("network").contains("network"));
        assertTrue(LocationReplyText.describeProvider("fused").contains("fused"));
        assertTrue(LocationReplyText.describeProvider(null).contains("unknown"));
        assertTrue(LocationReplyText.describeProvider("  ").contains("unknown"));
    }

    @Test
    public void mapLinkUsesTheRequestedGoogleMapsFormat() {
        assertEquals("https://www.google.com/maps?q=48.858400,2.294500",
                LocationReplyText.mapLink(48.8584, 2.2945));
    }

    @Test
    public void mapLinkHasNoSpaceSoItStaysAValidUrl() {
        String link = LocationReplyText.mapLink(51.507351, -0.127758);
        assertFalse(link.contains(" "));
        assertFalse(link.contains("%20"));
    }

    @Test
    public void readableCoordinatesKeepTheSpace() {
        assertEquals("51.507351, -0.127758", LocationReplyText.coordinates(51.507351, -0.127758));
    }
}
