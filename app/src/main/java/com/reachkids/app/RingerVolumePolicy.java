package com.reachkids.app;

/** Keeps ringer restoration audible while respecting a device's valid volume range. */
public final class RingerVolumePolicy {
    private RingerVolumePolicy() {
    }

    public static int restoredVolume(int savedVolume, int maximumVolume) {
        if (maximumVolume <= 0) {
            return 0;
        }
        if (savedVolume <= 0) {
            return 1;
        }
        return Math.min(savedVolume, maximumVolume);
    }
}