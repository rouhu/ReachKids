package com.reachkids.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AppVersionInfoTest {
    @Test
    public void versionLabelShowsNameAndBuildNumber() {
        assertEquals("Version 0.2.0-phase1 (build 2)",
                AppVersionInfo.versionLabel("0.2.0-phase1", 2L));
    }

    @Test
    public void versionLabelHandlesAMissingName() {
        assertEquals("Version unknown", AppVersionInfo.versionLabel(null, 1L));
        assertEquals("Version unknown", AppVersionInfo.versionLabel("   ", 1L));
    }

    @Test
    public void updatedLabelShowsTheInstallDate() {
        String label = AppVersionInfo.updatedLabel(1700000000000L);
        assertTrue(label.startsWith("Installed: "));
        assertTrue(label.length() > "Installed: ".length());
    }

    @Test
    public void updatedLabelHandlesAMissingDate() {
        assertEquals("Installed: unknown", AppVersionInfo.updatedLabel(0L));
    }
}
