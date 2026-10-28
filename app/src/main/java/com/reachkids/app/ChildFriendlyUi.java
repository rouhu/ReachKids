package com.reachkids.app;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.Button;

/** Shared lightweight visual styling for Reach Kids' programmatic screens. */
public final class ChildFriendlyUi {
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