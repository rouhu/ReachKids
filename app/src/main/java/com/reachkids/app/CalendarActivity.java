package com.reachkids.app;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Child-safe read-only view of upcoming entries from the phone's system calendar. */
public final class CalendarActivity extends Activity {
    private static final int REQUEST_CALENDAR_PERMISSION = 400;
    private static final int UPCOMING_DAYS = 30;

    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private CalendarRepository calendarRepository;
    private LinearLayout eventContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        calendarRepository = new CalendarRepository(this);
        showCalendar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (eventContainer != null && hasCalendarPermission()) {
            loadEvents();
        }
    }

    private void showCalendar() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        root.setPadding(padding, padding, padding, padding);

        TextView title = new TextView(this);
        title.setText("Calendar");
        title.setTextSize(27);
        root.addView(title, fullWidthParams(0, dp(6)));

        TextView description = new TextView(this);
        description.setText("Upcoming events for the next " + UPCOMING_DAYS
                + " days. This page is view-only.");
        description.setTextSize(17);
        root.addView(description, fullWidthParams(0, dp(16)));

        ScrollView scrollView = new ScrollView(this);
        eventContainer = new LinearLayout(this);
        eventContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(eventContainer);
        root.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        Button backButton = button("Back");
        backButton.setOnClickListener(view -> finish());
        root.addView(backButton, fullWidthParams(dp(12), 0));
        setContentView(root);

        if (!hasCalendarPermission() && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.READ_CALENDAR},
                    REQUEST_CALENDAR_PERMISSION);
        } else if (!hasCalendarPermission()) {
            showMessage("Calendar permission is required to view events.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CALENDAR_PERMISSION) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadEvents();
            } else {
                showMessage("Calendar permission was not granted. Ask a parent to allow it.");
            }
        }
    }

    private void loadEvents() {
        eventContainer.removeAllViews();
        showMessage("Loading calendar events...");
        long start = System.currentTimeMillis();
        Calendar end = Calendar.getInstance();
        end.setTimeInMillis(start);
        end.add(Calendar.DAY_OF_YEAR, UPCOMING_DAYS);
        backgroundExecutor.execute(() -> {
            List<CalendarEvent> events = calendarRepository.loadUpcomingEvents(start, end.getTimeInMillis());
            runOnUiThread(() -> showEvents(events));
        });
    }

    private void showEvents(List<CalendarEvent> events) {
        if (isFinishing() || eventContainer == null) {
            return;
        }
        eventContainer.removeAllViews();
        if (events.isEmpty()) {
            showMessage("No upcoming calendar events.");
            return;
        }
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
        DateFormat allDayDateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
        for (CalendarEvent event : events) {
            TextView eventView = new TextView(this);
            String when = event.allDay
                    ? allDayDateFormat.format(new Date(event.startMillis)) + " · All day"
                    : dateFormat.format(new Date(event.startMillis));
            String location = event.location == null || event.location.trim().isEmpty()
                    ? ""
                    : "\n" + event.location;
            eventView.setText(event.title + "\n" + when + location);
            eventView.setTextSize(18);
            int sidePadding = dp(12);
            int verticalPadding = dp(10);
            eventView.setPadding(sidePadding, verticalPadding, sidePadding, verticalPadding);
            eventView.setBackgroundColor(0xFFF1F1F1);
            eventContainer.addView(eventView, fullWidthParams(0, dp(8)));
        }
    }

    private boolean hasCalendarPermission() {
        return android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M
                || checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED;
    }

    private void showMessage(String message) {
        TextView view = new TextView(this);
        view.setText(message);
        view.setTextSize(17);
        eventContainer.addView(view, fullWidthParams(0, 0));
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(18);
        button.setMinHeight(dp(54));
        return button;
    }

    private LinearLayout.LayoutParams fullWidthParams(int topMargin, int bottomMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = topMargin;
        params.bottomMargin = bottomMargin;
        return params;
    }

    @Override
    protected void onDestroy() {
        backgroundExecutor.shutdownNow();
        super.onDestroy();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}