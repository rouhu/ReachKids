package com.reachkids.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Entry point for initial parent setup and the child-safe kiosk home. */
public final class MainActivity extends Activity {
    private static final int REQUEST_CONTACT_PERMISSIONS = 100;
    private static final int REQUEST_QUICK_CALL_PERMISSION = 101;
    private ParentPreferences parentPreferences;
    private DevicePolicyController devicePolicyController;
    private LinearLayout content;
    private ContactsRepository contactsRepository;
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private long pendingQuickCallPhoneDataId = -1L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parentPreferences = new ParentPreferences(this);
        devicePolicyController = new DevicePolicyController(this);
        contactsRepository = new ContactsRepository(this);
        showCurrentScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (parentPreferences != null && parentPreferences.hasPin()) {
            showChildHome();
            enterKioskIfConfigured();
        }
    }

    private void showCurrentScreen() {
        if (parentPreferences.hasPin()) {
            showChildHome();
        } else {
            showFirstRunSetup();
        }
    }

    private LinearLayout createScreen(String title, String description) {
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        int padding = dp(24);
        content.setPadding(padding, padding, padding, padding);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(28);
        titleView.setTextColor(getResources().getColor(R.color.textPrimary));
        content.addView(titleView, fullWidthParams(0, dp(12)));

        TextView descriptionView = new TextView(this);
        descriptionView.setText(description);
        descriptionView.setTextSize(17);
        content.addView(descriptionView, fullWidthParams(0, dp(24)));

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        setContentView(scrollView);
        return content;
    }

    private void showFirstRunSetup() {
        LinearLayout screen = createScreen("Parent setup",
                "Create a parent PIN before giving this phone to a child. "
                        + "Phase 1 has no PIN recovery: forgetting it requires resetting and reprovisioning the device.");
        final EditText pin = pinField("Choose a PIN (at least 4 digits)");
        final EditText confirmPin = pinField("Confirm PIN");
        screen.addView(pin, fullWidthParams(0, dp(12)));
        screen.addView(confirmPin, fullWidthParams(0, dp(20)));

        Button continueButton = button("Create parent PIN");
        continueButton.setOnClickListener(view -> {
            String first = pin.getText().toString();
            String second = confirmPin.getText().toString();
            if (!isAcceptablePin(first)) {
                showToast("Use at least four digits for the PIN.");
                return;
            }
            if (!first.equals(second)) {
                showToast("The PIN entries do not match.");
                confirmPin.setText("");
                return;
            }
            parentPreferences.savePin(first.toCharArray());
            pin.setText("");
            confirmPin.setText("");
            requestContactPermissionsThenShowHome();
        });
        screen.addView(continueButton, fullWidthParams(0, 0));
    }

    private void requestContactPermissionsThenShowHome() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                && (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.WRITE_CONTACTS
            }, REQUEST_CONTACT_PERMISSIONS);
        } else {
            showChildHome();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CONTACT_PERMISSIONS) {
            boolean granted = grantResults.length == 2
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED;
            if (!granted) {
                showToast("Contacts permission is required for parent contact management.");
            }
            showChildHome();
        } else if (requestCode == REQUEST_QUICK_CALL_PERMISSION) {
            long requestedPhoneDataId = pendingQuickCallPhoneDataId;
            pendingQuickCallPhoneDataId = -1L;
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && requestedPhoneDataId >= 0L) {
                placeQuickCall(requestedPhoneDataId);
            } else {
                showToast("Phone-call permission was not granted.");
            }
        }
    }

    private void showChildHome() {
        LinearLayout screen = createScreen("Reach Kids", "Choose what you want to do.");
        TextView status = new TextView(this);
        status.setText("Call a number saved by your parent. You cannot type a phone number.");
        status.setTextSize(18);
        screen.addView(status, fullWidthParams(0, dp(28)));

        Button messagesButton = button("Messages");
        messagesButton.setOnClickListener(view -> startActivity(new Intent(this, MessagesActivity.class)));
        screen.addView(messagesButton, fullWidthParams(0, dp(12)));

        addQuickCallButton(screen, 1);
        addQuickCallButton(screen, 2);

        Button calendarButton = button("Calendar");
        calendarButton.setOnClickListener(view -> startActivity(new Intent(this, CalendarActivity.class)));
        screen.addView(calendarButton, fullWidthParams(0, dp(12)));

        Button callButton = button("Call a contact");
        callButton.setOnClickListener(view -> startActivity(new Intent(this, CallContactsActivity.class)));
        screen.addView(callButton, fullWidthParams(0, dp(12)));

        Button parentButton = button("Parent area");
        parentButton.setOnClickListener(view -> showParentPinScreen());
        screen.addView(parentButton, fullWidthParams(0, 0));
    }

    private void addQuickCallButton(LinearLayout screen, int slot) {
        boolean configured = parentPreferences.hasQuickCall(slot);
        Button quickCallButton = button(configured
                ? parentPreferences.getQuickCallLabel(slot)
                : "Quick call " + slot + " is not set up");
        quickCallButton.setTextSize(22);
        quickCallButton.setMinHeight(dp(84));
        quickCallButton.setEnabled(configured);
        quickCallButton.setOnClickListener(view -> requestQuickCall(slot));
        screen.addView(quickCallButton, fullWidthParams(0, dp(12)));
    }

    private void requestQuickCall(int slot) {
        long phoneDataId = parentPreferences.getQuickCallPhoneDataId(slot);
        if (phoneDataId < 0L) {
            showToast("This quick-call button has not been set up.");
            return;
        }
        if (!hasPermission(Manifest.permission.READ_CONTACTS)) {
            showToast("Contacts permission is required for this call.");
            return;
        }
        if (!hasPermission(Manifest.permission.CALL_PHONE)) {
            pendingQuickCallPhoneDataId = phoneDataId;
            requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, REQUEST_QUICK_CALL_PERMISSION);
            return;
        }
        placeQuickCall(phoneDataId);
    }

    private void placeQuickCall(long phoneDataId) {
        backgroundExecutor.execute(() -> {
            SystemContact contact = contactsRepository.loadPhoneContact(phoneDataId);
            runOnUiThread(() -> {
                if (isFinishing()) {
                    return;
                }
                if (contact == null || !CallTargetValidator.hasCallableNumber(contact.phoneNumber)) {
                    showToast("That quick-call contact is no longer available. Ask a parent to set it up again.");
                    return;
                }
                try {
                    startActivity(new Intent(Intent.ACTION_CALL,
                            Uri.fromParts("tel", contact.phoneNumber, null)));
                } catch (SecurityException exception) {
                    showToast("Android did not allow this call.");
                } catch (android.content.ActivityNotFoundException exception) {
                    showToast("No phone application is available for calls.");
                }
            });
        });
    }

    private void enterKioskIfConfigured() {
        if (parentPreferences.isKioskEnabled() && devicePolicyController.canApplyKioskPolicy()) {
            try {
                // Refresh the lock-task allowlist after app updates and system-dialer changes.
                devicePolicyController.enableKioskPolicy(parentPreferences.isCameraDisabled());
                devicePolicyController.enterLockTask(this);
            } catch (IllegalArgumentException exception) {
                showToast("Kiosk policy needs to be reapplied by a parent.");
            } catch (IllegalStateException | SecurityException exception) {
                showToast("Could not refresh kiosk policy.");
            }
        }
    }

    private void showParentPinScreen() {
        if (parentPreferences.isPinEntryLocked()) {
            showToast("Try again in " + formatRemainingLockTime() + ".");
            return;
        }
        LinearLayout screen = createScreen("Parent PIN", "Enter the parent PIN to manage phone contacts and settings.");
        final EditText pin = pinField("Parent PIN");
        screen.addView(pin, fullWidthParams(0, dp(20)));

        Button unlockButton = button("Unlock parent area");
        unlockButton.setOnClickListener(view -> {
            if (parentPreferences.isPinEntryLocked()) {
                showToast("Try again in " + formatRemainingLockTime() + ".");
                pin.setText("");
                return;
            }
            char[] candidate = pin.getText().toString().toCharArray();
            pin.setText("");
            pin.setEnabled(false);
            unlockButton.setEnabled(false);
            unlockButton.setText("Unlocking...");
            backgroundExecutor.execute(() -> {
                boolean verified = parentPreferences.verifyPin(candidate);
                java.util.Arrays.fill(candidate, '\0');
                runOnUiThread(() -> {
                    if (isFinishing()) {
                        return;
                    }
                    if (verified) {
                        parentPreferences.clearFailedPinAttempts();
                        ParentSession.open();
                        startActivity(new Intent(this, ContactsActivity.class));
                        return;
                    }
                    parentPreferences.recordFailedPinAttempt();
                    pin.setEnabled(true);
                    unlockButton.setEnabled(true);
                    unlockButton.setText("Unlock parent area");
                    showToast(parentPreferences.isPinEntryLocked()
                            ? "Incorrect PIN. Try again in " + formatRemainingLockTime() + "."
                            : "Incorrect PIN.");
                });
            });
        });
        screen.addView(unlockButton, fullWidthParams(0, dp(12)));

        Button cancelButton = button("Cancel");
        cancelButton.setOnClickListener(view -> showChildHome());
        screen.addView(cancelButton, fullWidthParams(0, 0));
    }

    private EditText pinField(String hint) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        field.setSingleLine(true);
        return field;
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(18);
        button.setMinHeight(dp(56));
        return button;
    }

    private LinearLayout.LayoutParams fullWidthParams(int topMargin, int bottomMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = topMargin;
        params.bottomMargin = bottomMargin;
        return params;
    }

    private boolean isAcceptablePin(String pin) {
        return pin != null && pin.matches("[0-9]{4,}");
    }

    private boolean hasPermission(String permission) {
        return android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M
                || checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    private String formatRemainingLockTime() {
        long remainingSeconds = (parentPreferences.millisecondsUntilPinEntryAllowed() + 999L) / 1000L;
        long minutes = remainingSeconds / 60L;
        long seconds = remainingSeconds % 60L;
        return minutes > 0L ? minutes + "m " + seconds + "s" : seconds + "s";
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
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
