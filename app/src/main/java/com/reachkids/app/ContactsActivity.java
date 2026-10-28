package com.reachkids.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.provider.Telephony;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Parent-only system-contact administration. No contact data is copied into app storage. */
public final class ContactsActivity extends Activity {
    private static final int REQUEST_SMS_PERMISSIONS = 300;
    private ParentPreferences preferences;
    private DevicePolicyController devicePolicyController;
    private ContactsRepository contactsRepository;
    private LinearLayout contactsContainer;
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private boolean contactsLoadInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!ParentSession.isOpen()) {
            finish();
            return;
        }
        preferences = new ParentPreferences(this);
        devicePolicyController = new DevicePolicyController(this);
        contactsRepository = new ContactsRepository(this);
        showParentScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!ParentSession.isOpen()) {
            finish();
            return;
        }
        ParentSession.touch();
        if (contactsContainer != null) {
            reloadContacts();
        }
    }

    private void showParentScreen() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        ChildFriendlyUi.stylePage(scrollView);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        ChildFriendlyUi.stylePage(root);
        int padding = dp(20);
        root.setPadding(padding, padding, padding, padding);
        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText("Parent administration");
        title.setTextSize(26);
        root.addView(title, fullWidthParams(0, dp(6)));

        TextView explanation = new TextView(this);
        explanation.setText("These are phone system contacts. The child cannot add or edit contacts through Reach Kids.");
        explanation.setTextSize(16);
        root.addView(explanation, fullWidthParams(0, dp(12)));

        TextView deviceStatus = new TextView(this);
        deviceStatus.setText(devicePolicyController.diagnostics());
        deviceStatus.setTextSize(16);
        root.addView(deviceStatus, fullWidthParams(0, dp(10)));

        TextView smsStatus = new TextView(this);
        smsStatus.setText(MessagesActivity.isDefaultSmsApp(this)
                ? "Reach Kids is the default SMS app."
                : "Reach Kids is not the default SMS app.");
        smsStatus.setTextSize(16);
        root.addView(smsStatus, fullWidthParams(0, dp(8)));

        boolean defaultSmsApp = MessagesActivity.isDefaultSmsApp(this);
        Button smsSetupButton = button(defaultSmsApp
                ? "SMS is set up"
                : "Set up SMS (parent only)");
        smsSetupButton.setEnabled(!defaultSmsApp);
        smsSetupButton.setOnClickListener(view -> requestSmsSetup());
        root.addView(smsSetupButton, fullWidthParams(0, dp(8)));

        Button kioskButton = button(preferences.isKioskEnabled()
                ? "Kiosk mode is enabled"
                : "Enable kiosk mode");
        kioskButton.setEnabled(devicePolicyController.canApplyKioskPolicy() && !preferences.isKioskEnabled());
        kioskButton.setOnClickListener(view -> enableKiosk(kioskButton, deviceStatus));
        root.addView(kioskButton, fullWidthParams(0, dp(8)));

        Button exitKioskButton = button("Exit kiosk mode (parent only)");
        exitKioskButton.setEnabled(devicePolicyController.canApplyKioskPolicy() && preferences.isKioskEnabled());
        exitKioskButton.setOnClickListener(view -> exitKiosk(kioskButton, exitKioskButton, deviceStatus));
        root.addView(exitKioskButton, fullWidthParams(0, dp(8)));

        Button addButton = button("Add system contact");
        addButton.setOnClickListener(view -> showContactEditor(null));
        root.addView(addButton, fullWidthParams(0, dp(8)));

        Button quickCallOneButton = button(quickCallSetupLabel(1));
        quickCallOneButton.setOnClickListener(view -> showQuickCallSetup(1));
        root.addView(quickCallOneButton, fullWidthParams(0, dp(8)));

        Button quickCallTwoButton = button(quickCallSetupLabel(2));
        quickCallTwoButton.setOnClickListener(view -> showQuickCallSetup(2));
        root.addView(quickCallTwoButton, fullWidthParams(0, dp(8)));

        Button changePinButton = button("Change parent PIN");
        changePinButton.setOnClickListener(view -> showChangePinDialog());
        root.addView(changePinButton, fullWidthParams(0, dp(8)));

        CheckBox cameraCheckBox = new CheckBox(this);
        cameraCheckBox.setText("Disable camera when device policy is enabled");
        cameraCheckBox.setChecked(preferences.isCameraDisabled());
        cameraCheckBox.setOnCheckedChangeListener((buttonView, checked) -> {
            preferences.setCameraDisabled(checked);
            if (devicePolicyController.canApplyKioskPolicy()) {
                try {
                    devicePolicyController.applyCameraPolicy(checked);
                    showToast(checked ? "Camera disabled by device policy." : "Camera enabled by device policy.");
                } catch (IllegalStateException exception) {
                    showToast("Could not apply the camera policy.");
                }
            } else {
                showToast("Saved. It will be enforced after device-owner enrollment.");
            }
        });
        root.addView(cameraCheckBox, fullWidthParams(0, dp(10)));

        CheckBox ringerMutedCheckBox = new CheckBox(this);
        ringerMutedCheckBox.setText("Ringer Muted");
        configureRingerMutedCheckBox(ringerMutedCheckBox);
        root.addView(ringerMutedCheckBox, fullWidthParams(0, dp(10)));

        CheckBox disableCallingCheckBox = new CheckBox(this);
        disableCallingCheckBox.setText("Disable Calling");
        disableCallingCheckBox.setChecked(preferences.isCallingDisabled());
        disableCallingCheckBox.setOnCheckedChangeListener((buttonView, checked) -> {
            preferences.setCallingDisabled(checked);
            ParentSession.touch();
            showToast(checked ? "Calling disabled on the child home." : "Calling enabled on the child home.");
        });
        root.addView(disableCallingCheckBox, fullWidthParams(0, dp(10)));

        CheckBox disableMessagesCheckBox = new CheckBox(this);
        disableMessagesCheckBox.setText("Disable messages");
        disableMessagesCheckBox.setChecked(preferences.areMessagesDisabled());
        disableMessagesCheckBox.setOnCheckedChangeListener((buttonView, checked) -> {
            preferences.setMessagesDisabled(checked);
            ParentSession.touch();
            showToast(checked ? "Messages disabled on the child home." : "Messages enabled on the child home.");
        });
        root.addView(disableMessagesCheckBox, fullWidthParams(0, dp(10)));

        contactsContainer = new LinearLayout(this);
        contactsContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(contactsContainer, fullWidthParams(0, dp(8)));

        Button lockButton = button("Lock parent area");
        lockButton.setOnClickListener(view -> {
            ParentSession.close();
            finish();
        });
        root.addView(lockButton, fullWidthParams(dp(10), 0));
        setContentView(scrollView);
        reloadContacts();
    }

    private String quickCallSetupLabel(int slot) {
        return preferences.hasQuickCall(slot)
                ? "Set quick call " + slot + " (currently: " + preferences.getQuickCallLabel(slot) + ")"
                : "Set quick call " + slot;
    }

    private void showQuickCallSetup(int slot) {
        if (!hasContactPermissions()) {
            showToast("Contacts permission is required to set a quick-call button.");
            return;
        }
        showToast("Loading contacts...");
        backgroundExecutor.execute(() -> {
            List<SystemContact> contacts = contactsRepository.loadPhoneContacts();
            runOnUiThread(() -> showQuickCallSetupDialog(slot, contacts));
        });
    }

    private void showQuickCallSetupDialog(int slot, List<SystemContact> contacts) {
        if (isFinishing() || !ParentSession.isOpen()) {
            return;
        }
        if (contacts.isEmpty()) {
            showToast("Add a system contact before setting a quick-call button.");
            return;
        }

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        form.setPadding(padding, 0, padding, 0);

        EditText label = new EditText(this);
        label.setHint("Button label, for example CALL DAD");
        label.setSingleLine(true);
        if (preferences.hasQuickCall(slot)) {
            label.setText(preferences.getQuickCallLabel(slot));
        }
        form.addView(label);

        RadioGroup contactChoices = new RadioGroup(this);
        int selectedIndex = -1;
        long selectedPhoneDataId = preferences.getQuickCallPhoneDataId(slot);
        for (int index = 0; index < contacts.size(); index++) {
            SystemContact contact = contacts.get(index);
            RadioButton contactButton = new RadioButton(this);
            contactButton.setId(View.generateViewId());
            contactButton.setText(contact.displayName + "\n" + contact.phoneNumber);
            contactChoices.addView(contactButton);
            if (contact.phoneDataId == selectedPhoneDataId) {
                selectedIndex = index;
                contactButton.setChecked(true);
            }
        }
        form.addView(contactChoices);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Set quick call " + slot)
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Clear", null)
                .setPositiveButton("Save", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String buttonLabel = label.getText().toString().trim();
                if (buttonLabel.isEmpty()) {
                    showToast("Enter a label for the quick-call button.");
                    return;
                }
                int chosenRadioButtonId = contactChoices.getCheckedRadioButtonId();
                if (chosenRadioButtonId == -1) {
                    showToast("Choose a contact for the quick-call button.");
                    return;
                }
                int chosenIndex = contactChoices.indexOfChild(
                        contactChoices.findViewById(chosenRadioButtonId));
                preferences.saveQuickCall(slot, buttonLabel,
                        contacts.get(chosenIndex).phoneDataId);
                ParentSession.touch();
                dialog.dismiss();
                showToast("Quick call " + slot + " saved.");
                showParentScreen();
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(view -> {
                preferences.clearQuickCall(slot);
                ParentSession.touch();
                dialog.dismiss();
                showToast("Quick call " + slot + " cleared.");
                showParentScreen();
            });
        });
        dialog.show();
    }

    private void showChangePinDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        form.setPadding(padding, 0, padding, 0);

        EditText currentPin = pinField("Current PIN");
        EditText newPin = pinField("New PIN (at least 4 digits)");
        EditText confirmPin = pinField("Confirm new PIN");
        form.addView(currentPin);
        form.addView(newPin);
        form.addView(confirmPin);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Change parent PIN")
                .setMessage("Enter the current PIN before choosing a new one.")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Change PIN", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> changeParentPin(dialog, currentPin, newPin, confirmPin)));
        dialog.show();
    }

    private void changeParentPin(AlertDialog dialog, EditText currentPin, EditText newPin,
            EditText confirmPin) {
        String currentText = currentPin.getText().toString();
        String newText = newPin.getText().toString();
        String confirmText = confirmPin.getText().toString();
        if (!isAcceptablePin(newText)) {
            showToast("Use at least four digits for the new PIN.");
            return;
        }
        if (!newText.equals(confirmText)) {
            showToast("The new PIN entries do not match.");
            return;
        }
        char[] current = currentText.toCharArray();
        char[] replacement = newText.toCharArray();
        currentPin.setText("");
        newPin.setText("");
        confirmPin.setText("");
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        backgroundExecutor.execute(() -> {
            boolean verified = preferences.verifyPin(current);
            Arrays.fill(current, '\0');
            if (verified) {
                preferences.savePin(replacement);
            }
            Arrays.fill(replacement, '\0');
            runOnUiThread(() -> {
                if (isFinishing()) {
                    return;
                }
                if (verified) {
                    ParentSession.touch();
                    dialog.dismiss();
                    showToast("Parent PIN changed.");
                } else {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    showToast("The current PIN is incorrect.");
                }
            });
        });
    }

    private void enableKiosk(Button kioskButton, TextView deviceStatus) {
        try {
            devicePolicyController.enableKioskPolicy(preferences.isCameraDisabled());
            preferences.setKioskEnabled(true);
            devicePolicyController.enterLockTask(this);
            kioskButton.setText("Kiosk mode is enabled");
            kioskButton.setEnabled(false);
            deviceStatus.setText("Kiosk policy enabled. Reach Kids is set as the persistent Home app and lock task is active.");
            showToast("Kiosk mode enabled.");
        } catch (IllegalStateException | SecurityException exception) {
            showToast("Could not enable kiosk mode. Confirm device-owner enrollment.");
        }
    }

    private void requestSmsSetup() {
        if (!MessagesActivity.isDefaultSmsApp(this)) {
            requestDefaultSmsApp();
            return;
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                && (!hasPermission(Manifest.permission.SEND_SMS)
                || !hasPermission(Manifest.permission.RECEIVE_SMS)
                || !hasPermission(Manifest.permission.READ_SMS))) {
            requestPermissions(new String[]{
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS
            }, REQUEST_SMS_PERMISSIONS);
            return;
        }
        showToast("SMS setup is complete.");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_SMS_PERMISSIONS) {
            boolean allGranted = grantResults.length == 3;
            for (int result : grantResults) {
                allGranted = allGranted && result == PackageManager.PERMISSION_GRANTED;
            }
            if (allGranted) {
                showToast("SMS setup is complete.");
            } else {
                showToast("SMS permissions are required for Reach Kids messaging.");
            }
        }
    }

    private void requestDefaultSmsApp() {
        if (MessagesActivity.isDefaultSmsApp(this)) {
            showToast("Reach Kids is already the default SMS app.");
            return;
        }
        Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
        startActivity(intent);
    }

    private boolean hasPermission(String permission) {
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void exitKiosk(Button kioskButton, Button exitKioskButton, TextView deviceStatus) {
        try {
            devicePolicyController.exitLockTask(this);
            devicePolicyController.clearKioskPolicy();
            preferences.setKioskEnabled(false);
            kioskButton.setText("Enable kiosk mode");
            kioskButton.setEnabled(devicePolicyController.canApplyKioskPolicy());
            exitKioskButton.setEnabled(false);
            deviceStatus.setText("Kiosk mode disabled. Reach Kids is no longer forced as the Home app.");
            showToast("Kiosk mode disabled.");
        } catch (IllegalStateException | SecurityException exception) {
            showToast("Could not exit kiosk mode.");
        }
    }

    private void reloadContacts() {
        if (contactsLoadInProgress) {
            return;
        }
        if (!hasContactPermissions()) {
            contactsContainer.removeAllViews();
            TextView missingPermission = new TextView(this);
            missingPermission.setText("Contacts permission was not granted. Enable Contacts permission in Android settings, then return here.");
            contactsContainer.addView(missingPermission, fullWidthParams(0, 0));
            return;
        }
        contactsContainer.removeAllViews();
        TextView loading = new TextView(this);
        loading.setText("Loading system contacts...");
        loading.setTextSize(17);
        contactsContainer.addView(loading, fullWidthParams(0, 0));
        contactsLoadInProgress = true;
        backgroundExecutor.execute(() -> {
            List<SystemContact> contacts = contactsRepository.loadPhoneContacts();
            runOnUiThread(() -> {
                contactsLoadInProgress = false;
                showContacts(contacts);
            });
        });
    }

    private void showContacts(List<SystemContact> contacts) {
        if (isFinishing() || contactsContainer == null || !ParentSession.isOpen()) {
            return;
        }
        contactsContainer.removeAllViews();
        if (contacts.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No contacts with phone numbers yet.");
            empty.setTextSize(17);
            contactsContainer.addView(empty, fullWidthParams(0, 0));
            return;
        }
        for (SystemContact contact : contacts) {
            Button contactButton = button(contact.displayName + "\n" + contact.phoneNumber);
            contactButton.setAllCaps(false);
            contactButton.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            contactButton.setOnClickListener(view -> showContactEditor(contact));
            contactsContainer.addView(contactButton, fullWidthParams(0, dp(8)));
        }
    }

    private void showContactEditor(SystemContact contact) {
        if (!hasContactPermissions()) {
            showToast("Contacts permission is required.");
            return;
        }
        ParentSession.touch();
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        form.setPadding(padding, 0, padding, 0);

        EditText name = new EditText(this);
        name.setHint("Name");
        name.setSingleLine(true);
        name.setText(contact == null ? "" : contact.displayName);
        form.addView(name);

        EditText number = new EditText(this);
        number.setHint("Phone number");
        number.setInputType(InputType.TYPE_CLASS_PHONE);
        number.setSingleLine(true);
        number.setText(contact == null ? "" : contact.phoneNumber);
        form.addView(number);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(contact == null ? "Add system contact" : "Edit system contact")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null);
        if (contact != null) {
            builder.setNeutralButton("Delete", null);
        }
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String contactName = name.getText().toString().trim();
                String phoneNumber = number.getText().toString().trim();
                if (contactName.isEmpty() || phoneNumber.isEmpty()) {
                    showToast("Both a name and phone number are required.");
                    return;
                }
                try {
                    if (contact == null) {
                        contactsRepository.addPhoneContact(contactName, phoneNumber);
                    } else {
                        contactsRepository.updatePhoneContact(contact, contactName, phoneNumber);
                    }
                    ParentSession.touch();
                    dialog.dismiss();
                    reloadContacts();
                } catch (RuntimeException exception) {
                    showToast("Could not save the system contact.");
                }
            });
            if (contact != null) {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(view ->
                        new AlertDialog.Builder(this)
                                .setTitle("Delete " + contact.displayName + "?")
                                .setMessage("This deletes the selected contact from the phone's system Contacts.")
                                .setNegativeButton("Cancel", null)
                                .setPositiveButton("Delete", (confirm, which) -> {
                                    try {
                                        contactsRepository.deleteRawContact(contact);
                                        ParentSession.touch();
                                        dialog.dismiss();
                                        reloadContacts();
                                    } catch (RuntimeException exception) {
                                        showToast("Could not delete the system contact.");
                                    }
                                })
                                .show());
            }
        });
        dialog.show();
    }

    private boolean hasContactPermissions() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            return true;
        }
        return checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }

    private void configureRingerMutedCheckBox(CheckBox checkBox) {
        checkBox.setOnCheckedChangeListener(null);
        checkBox.setChecked(preferences.isRingerMuted());
        checkBox.setOnCheckedChangeListener((buttonView, checked) -> {
            if (setRingerMuted(checked)) {
                ParentSession.touch();
                showToast(checked ? "Ringer muted." : "Ringer restored.");
            } else {
                configureRingerMutedCheckBox(checkBox);
                showToast("Could not change the ringer volume.");
            }
        });
    }

    /** Mutes only the call ringer; alarms and media playback are not changed. */
    private boolean setRingerMuted(boolean muted) {
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioManager == null) {
            return false;
        }
        try {
            int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
            if (muted) {
                if (currentVolume > 0) {
                    preferences.setPreviousRingVolume(currentVolume);
                }
                audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
            } else {
                int restoredVolume = RingerVolumePolicy.restoredVolume(
                        preferences.getPreviousRingVolume(),
                        audioManager.getStreamMaxVolume(AudioManager.STREAM_RING));
                audioManager.setStreamVolume(AudioManager.STREAM_RING, restoredVolume, 0);
            }
            preferences.setRingerMuted(muted);
            return true;
        } catch (SecurityException exception) {
            return false;
        }
    }

    private EditText pinField(String hint) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        field.setSingleLine(true);
        return field;
    }

    private boolean isAcceptablePin(String pin) {
        return pin != null && pin.matches("[0-9]{4,}");
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(17);
        button.setMinHeight(dp(54));
        ChildFriendlyUi.styleButton(button);
        return button;
    }

    private LinearLayout.LayoutParams fullWidthParams(int topMargin, int bottomMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = topMargin;
        params.bottomMargin = bottomMargin;
        return params;
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
