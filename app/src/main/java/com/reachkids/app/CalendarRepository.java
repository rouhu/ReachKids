package com.reachkids.app;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

import java.util.ArrayList;
import java.util.List;

/** Reads upcoming system-calendar instances without modifying calendar data. */
public final class CalendarRepository {
    private final Context context;

    public CalendarRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public List<CalendarEvent> loadUpcomingEvents(long beginMillis, long endMillis) {
        List<CalendarEvent> events = new ArrayList<>();
        Uri uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
                .appendPath(String.valueOf(beginMillis))
                .appendPath(String.valueOf(endMillis))
                .build();
        String[] projection = {
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.EVENT_LOCATION,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY
        };
        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null,
                CalendarContract.Instances.BEGIN + " ASC")) {
            if (cursor == null) {
                return events;
            }
            while (cursor.moveToNext()) {
                String title = cursor.getString(0);
                events.add(new CalendarEvent(
                        title == null || title.trim().isEmpty() ? "Untitled event" : title,
                        cursor.getString(1), cursor.getLong(2), cursor.getLong(3),
                        cursor.getInt(4) != 0));
            }
        }
        return events;
    }
}