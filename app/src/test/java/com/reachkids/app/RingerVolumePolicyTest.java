package com.reachkids.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RingerVolumePolicyTest {
    @Test
    public void restoresSavedVolumeWhenItIsWithinTheDeviceRange() {
        assertEquals(7, RingerVolumePolicy.restoredVolume(7, 15));
    }

    @Test
    public void restoresAnAudibleVolumeWhenNoPriorAudibleValueWasSaved() {
        assertEquals(1, RingerVolumePolicy.restoredVolume(-1, 15));
        assertEquals(1, RingerVolumePolicy.restoredVolume(0, 15));
    }

    @Test
    public void capsRestoredVolumeAtTheDeviceMaximum() {
        assertEquals(5, RingerVolumePolicy.restoredVolume(12, 5));
        assertEquals(0, RingerVolumePolicy.restoredVolume(4, 0));
    }
}