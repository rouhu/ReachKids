package com.reachkids.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/** Child call picker. It exposes phone-number rows from system Contacts, never a dial pad. */
public final class CallContactsActivity extends Activity {
    private static final int REQUEST_CALL_PERMISSION = 200;

    private ContactsRepository contactsRepository;
    private ParentPreferences parentPreferences;
    private LinearLayout contactsContainer;
    private long pendingPhoneDataId = -1L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parentPreferences = new ParentPreferences(this);
        if (finishIfCallingDisabled()) {
            return;
        }
        contactsRepository = new ContactsRepository(this);
        showCallPicker();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (finishIfCallingDisabled()) {
            return;
        }
        if (contactsContainer != null) {
            loadContacts();
        }
    }

    private void showCallPicker() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        ChildFriendlyUi.stylePage(root);
        int padding = dp(20);
        root.setPadding(padding, padding, padding, padding);

        TextView title = new TextView(this);
        title.setText("Who do you want to call?");
        title.setTextSize(27);
        root.addView(title, fullWidthParams(0, dp(6)));

        TextView description = new TextView(this);
        description.setText("Choose a contact. You cannot type a phone number here.");
        description.setTextSize(17);
        root.addView(description, fullWidthParams(0, dp(16)));

        ScrollView scrollView = new ScrollView(this);
        contactsContainer = new LinearLayout(this);
        contactsContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(contactsContainer);
        root.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        Button backButton = button("Back");
        backButton.setOnClickListener(view -> finish());
        root.addView(backButton, fullWidthParams(dp(12), 0));
        setContentView(root);
        loadContacts();
    }

    private void loadContacts() {
        contactsContainer.removeAllViews();
        if (!hasPermission(Manifest.permission.READ_CONTACTS)) {
            showMessage("Contacts permission is required before a call can be placed.");
            return;
        }

        List<SystemContact> contacts = contactsRepository.loadPhoneContacts();
        if (contacts.isEmpty()) {
            showMessage("There are no phone contacts yet. Ask a parent to add a contact.");
            return;
        }
        for (SystemContact contact : contacts) {
            Button contactButton = button(contact.displayName + "\n" + contact.phoneNumber);
            contactButton.setAllCaps(false);
            contactButton.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            contactButton.setOnClickListener(view -> confirmCall(contact.phoneDataId));
            contactsContainer.addView(contactButton, fullWidthParams(0, dp(8)));
        }
    }

    private void confirmCall(long phoneDataId) {
        SystemContact contact = contactsRepository.loadPhoneContact(phoneDataId);
        if (contact == null || !CallTargetValidator.hasCallableNumber(contact.phoneNumber)) {
            showToast("That contact is no longer available.");
            loadContacts();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Call " + contact.displayName + "?")
                .setMessage(contact.phoneNumber)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Call", (dialog, which) -> requestCall(contact.phoneDataId))
                .show();
    }

    private void requestCall(long phoneDataId) {
        if (finishIfCallingDisabled()) {
            return;
        }
        if (!hasPermission(Manifest.permission.CALL_PHONE)) {
            pendingPhoneDataId = phoneDataId;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL_PERMISSION);
            } else {
                showToast("Phone-call permission is required.");
            }
            return;
        }
        placeSelectedContactCall(phoneDataId);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CALL_PERMISSION) {
            long requestedContact = pendingPhoneDataId;
            pendingPhoneDataId = -1L;
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && requestedContact != -1L) {
                placeSelectedContactCall(requestedContact);
            } else {
                showToast("Phone-call permission was not granted.");
            }
        }
    }

    private void placeSelectedContactCall(long phoneDataId) {
        if (finishIfCallingDisabled()) {
            return;
        }
        // Re-query at the action boundary so this call can only target the current provider value.
        SystemContact contact = contactsRepository.loadPhoneContact(phoneDataId);
        if (contact == null || !CallTargetValidator.hasCallableNumber(contact.phoneNumber)) {
            showToast("That contact is no longer available.");
            loadContacts();
            return;
        }
        Intent callIntent = new Intent(Intent.ACTION_CALL,
                Uri.fromParts("tel", contact.phoneNumber, null));
        try {
            startActivity(callIntent);
        } catch (SecurityException exception) {
            showToast("Android did not allow this call.");
        } catch (android.content.ActivityNotFoundException exception) {
            showToast("No phone application is available for calls.");
        }
    }

    private boolean hasPermission(String permission) {
        return android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M
                || checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean finishIfCallingDisabled() {
        if (parentPreferences != null && parentPreferences.isCallingDisabled()) {
            Toast.makeText(this, "Calling is disabled by a parent.", Toast.LENGTH_SHORT).show();
            finish();
            return true;
        }
        return false;
    }

    private void showMessage(String message) {
        TextView view = new TextView(this);
        view.setText(message);
        view.setTextSize(17);
        contactsContainer.addView(view, fullWidthParams(0, 0));
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(18);
        button.setMinHeight(dp(58));
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
