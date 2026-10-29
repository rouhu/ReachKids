package com.reachkids.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BlockedHoursScheduleTest {
    @Test
    public void parsesAndFormatsValid24HourTimes() {
        assertEquals(0, BlockedHoursSchedule.parseTime("00:00"));
        assertEquals(9 * 60 + 5, BlockedHoursSchedule.parseTime("9:05"));
        assertEquals(23 * 60 + 59, BlockedHoursSchedule.parseTime("23:59"));
        assertEquals("09:05", BlockedHoursSchedule.formatTime(9 * 60 + 5));
    }

    @Test
    public void rejectsInvalidTimes() {
        assertEquals(-1, BlockedHoursSchedule.parseTime("24:00"));
        assertEquals(-1, BlockedHoursSchedule.parseTime("09:60"));
        assertEquals(-1, BlockedHoursSchedule.parseTime("9:5"));
        assertEquals(-1, BlockedHoursSchedule.parseTime(""));
    }

    @Test
    public void includesStartAndExcludesEndOfSameDayRange() {
        int start = 8 * 60;
        int end = 15 * 60 + 30;
        assertFalse(BlockedHoursSchedule.isWithinRangeOnStartDay(start - 1, start, end));
        assertTrue(BlockedHoursSchedule.isWithinRangeOnStartDay(start, start, end));
        assertTrue(BlockedHoursSchedule.isWithinRangeOnStartDay(end - 1, start, end));
        assertFalse(BlockedHoursSchedule.isWithinRangeOnStartDay(end, start, end));
    }

    @Test
    public void overnightRangeBlocksTheLatePartOfItsStartDay() {
        int start = 20 * 60;
        int end = 7 * 60;
        assertFalse(BlockedHoursSchedule.isWithinRangeOnStartDay(start - 1, start, end));
        assertTrue(BlockedHoursSchedule.isWithinRangeOnStartDay(start, start, end));
        assertTrue(BlockedHoursSchedule.isWithinRangeOnStartDay(23 * 60 + 59, start, end));
    }
}