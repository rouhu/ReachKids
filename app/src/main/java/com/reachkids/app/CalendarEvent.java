package com.reachkids.app;

/** Read-only upcoming event from the system calendar provider. */
public final class CalendarEvent {
    public final String title;
    public final String location;
    public final long startMillis;
    public final long endMillis;
    public final boolean allDay;

    public CalendarEvent(String title, String location, long startMillis, long endMillis,
            boolean allDay) {
        this.title = title;
        this.location = location;
        this.startMillis = startMillis;
        this.endMillis = endMillis;
        this.allDay = allDay;
    }
}