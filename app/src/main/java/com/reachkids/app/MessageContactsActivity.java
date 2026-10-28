package com.reachkids.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/** Child recipient picker for new SMS messages; only system Contacts are selectable. */
public final class MessageContactsActivity extends Activity {
    private ContactsRepository contactsRepository;
    private LinearLayout contactsContainer;
    private ParentPreferences parentPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parentPreferences = new ParentPreferences(this);
        if (finishIfMessagesDisabled()) {
            return;
        }
        contactsRepository = new ContactsRepository(this);
        showPicker();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (finishIfMessagesDisabled()) {
            return;
        }
        if (contactsContainer != null) {
            loadContacts();
        }
    }

    private void showPicker() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        ChildFriendlyUi.stylePage(root);
        int padding = dp(20);
        root.setPadding(padding, padding, padding, padding);

        TextView title = new TextView(this);
        title.setText("New message");
        title.setTextSize(27);
        root.addView(title, fullWidthParams(0, dp(6)));

        TextView explanation = new TextView(this);
        explanation.setText("Choose a contact. You cannot type a phone number.");
        explanation.setTextSize(17);
        root.addView(explanation, fullWidthParams(0, dp(16)));

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
        if (!hasContactsPermission()) {
            showMessage("Contacts permission is required before sending a message.");
            return;
        }
        List<SystemContact> contacts = contactsRepository.loadPhoneContacts();
        if (contacts.isEmpty()) {
            showMessage("There are no phone contacts yet. Ask a parent to add a contact.");
            return;
        }
        for (SystemContact contact : contacts) {
            Button button = button(contact.displayName + "\n" + contact.phoneNumber);
            button.setAllCaps(false);
            button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            button.setOnClickListener(view -> {
                Intent intent = new Intent(this, MessagesActivity.class);
                intent.putExtra(MessagesActivity.EXTRA_ADDRESS, contact.phoneNumber);
                startActivity(intent);
                finish();
            });
            contactsContainer.addView(button, fullWidthParams(0, dp(8)));
        }
    }

    private boolean hasContactsPermission() {
        return android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M
                || checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean finishIfMessagesDisabled() {
        if (parentPreferences != null && parentPreferences.areMessagesDisabled()) {
            finish();
            return true;
        }
        return false;
    }

    private void showMessage(String message) {
        TextView textView = new TextView(this);
        textView.setText(message);
        textView.setTextSize(17);
        contactsContainer.addView(textView, fullWidthParams(0, 0));
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
