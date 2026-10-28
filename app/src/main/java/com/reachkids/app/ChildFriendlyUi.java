package com.reachkids.app;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

/** Shared lightweight visual styling for Reach Kids' programmatic screens. */
public final class ChildFriendlyUi {
    private static final int SWIPE_CONFIRM_DISTANCE_DP = 96;

    private ChildFriendlyUi() {
    }

    public static void stylePage(View view) {
        view.setBackgroundResource(R.color.pageBlue);
    }

    public static void styleButton(Button button) {
        button.setAllCaps(false);
        button.setTextColor(Color.rgb(61, 44, 0));
        button.setBackgroundResource(R.drawable.child_button);
        button.setElevation(dp(button, 4));
        button.setStateListAnimator(null);
        button.setPadding(dp(button, 14), dp(button, 5), dp(button, 14), dp(button, 5));
        int icon = iconFor(button.getText() == null ? "" : button.getText().toString());
        if (icon != 0) {
            button.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0);
            button.setCompoundDrawablePadding(dp(button, 10));
        }
    }

    /** Requires a deliberate right swipe so high-impact child actions cannot be triggered by a tap. */
    public static void requireSwipeRight(Button button, Runnable action) {
        final float minimumDistance = dp(button, SWIPE_CONFIRM_DISTANCE_DP);
        final float[] startX = new float[1];
        final float[] startY = new float[1];
        CharSequence label = button.getText();
        button.setText(label + "\nSwipe right ->");
        button.setOnTouchListener((view, event) -> {
            if (!button.isEnabled()) {
                return true;
            }
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    startX[0] = event.getX();
                    startY[0] = event.getY();
                    button.setPressed(true);
                    return true;
                case MotionEvent.ACTION_UP:
                    button.setPressed(false);
                    float horizontalDistance = event.getX() - startX[0];
                    float verticalDistance = Math.abs(event.getY() - startY[0]);
                    if (horizontalDistance >= minimumDistance && verticalDistance <= minimumDistance) {
                        action.run();
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    button.setPressed(false);
                    return true;
                default:
                    return true;
            }
        });
    }

    private static int iconFor(String text) {
        String label = text.toUpperCase();
        if (label.contains("MESSAGE") || label.contains("SMS")) {
            return R.drawable.ic_message;
        }
        if (label.contains("CALENDAR")) {
            return R.drawable.ic_calendar;
        }
        if (label.contains("CALL") || label.contains("CONTACT")) {
            return R.drawable.ic_phone;
        }
        if (label.contains("PARENT") || label.contains("PIN") || label.contains("LOCK")) {
            return R.drawable.ic_lock;
        }
        if (label.contains("ADD") || label.contains("NEW")) {
            return R.drawable.ic_add;
        }
        if (label.equals("BACK") || label.equals("CANCEL")) {
            return R.drawable.ic_back;
        }
        return 0;
    }

    private static int dp(View view, int value) {
        return Math.round(value * view.getResources().getDisplayMetrics().density);
    }
}