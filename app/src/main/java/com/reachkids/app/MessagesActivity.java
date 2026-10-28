package com.reachkids.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/** Basic local SMS inbox/conversation UI. MMS and group messaging are explicitly unsupported. */
public final class MessagesActivity extends Activity {
    public static final String EXTRA_ADDRESS = "address";

    private SmsDatabase database;
    private ContactsRepository contactsRepository;
    private String conversationAddress;
    private LinearLayout messageContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = new SmsDatabase(this);
        contactsRepository = new ContactsRepository(this);
        if (isMmsIntent(getIntent())) {
            showUnsupportedMms();
            return;
        }
        conversationAddress = resolveRequestedAddress(getIntent());
        if (conversationAddress == null) {
            showInbox();
        } else {
            showConversation(conversationAddress);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (isMmsIntent(intent)) {
            conversationAddress = null;
            showUnsupportedMms();
            return;
        }
        conversationAddress = resolveRequestedAddress(intent);
        if (conversationAddress == null) {
            showInbox();
        } else {
            showConversation(conversationAddress);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (conversationAddress == null) {
            showInbox();
        } else {
            showConversation(conversationAddress);
        }
    }

    public static boolean isDefaultSmsApp(Activity activity) {
        return activity.getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(activity));
    }

    private String resolveRequestedAddress(Intent intent) {
        String extraAddress = intent.getStringExtra(EXTRA_ADDRESS);
        if (CallTargetValidator.hasCallableNumber(extraAddress)) {
            return extraAddress;
        }
        Uri data = intent.getData();
        if (data == null) {
            return null;
        }
        String address = data.getSchemeSpecificPart();
        return CallTargetValidator.hasCallableNumber(address) ? address : null;
    }

    private boolean isMmsIntent(Intent intent) {
        Uri data = intent.getData();
        return data != null && ("mms".equals(data.getScheme()) || "mmsto".equals(data.getScheme()));
    }

    private void showInbox() {
        LinearLayout root = createRoot();
        TextView title = heading("Messages");
        root.addView(title, fullWidthParams(0, dp(4)));

        TextView status = new TextView(this);
        status.setText(isDefaultSmsApp(this)
                ? "SMS only. MMS and group messages are not supported."
                : "A parent must make Reach Kids the default SMS app before messages can be sent or received here.");
        status.setTextSize(16);
        root.addView(status, fullWidthParams(0, dp(12)));

        Button newMessageButton = button("New message");
        newMessageButton.setOnClickListener(view -> startActivity(new Intent(this, MessageContactsActivity.class)));
        root.addView(newMessageButton, fullWidthParams(0, dp(10)));

        ScrollView scrollView = new ScrollView(this);
        LinearLayout threadContainer = new LinearLayout(this);
        threadContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(threadContainer);
        root.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        Button backButton = button("Back");
        backButton.setOnClickListener(view -> finish());
        root.addView(backButton, fullWidthParams(dp(12), 0));
        setContentView(root);

        List<SmsThread> threads = database.loadThreads();
        if (threads.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No SMS messages yet.");
            empty.setTextSize(17);
            threadContainer.addView(empty, fullWidthParams(0, 0));
            return;
        }
        for (SmsThread thread : threads) {
            String name = displayNameFor(thread.address);
            String unread = thread.unreadCount > 0 ? " (" + thread.unreadCount + ")" : "";
            String direction = thread.outgoing ? "Outgoing" : "Incoming";
            Button threadButton = button(name + unread + "\n" + direction + " — " + thread.preview);
            threadButton.setAllCaps(false);
            threadButton.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            threadButton.setOnClickListener(view -> {
                conversationAddress = thread.address;
                showConversation(thread.address);
            });
            threadContainer.addView(threadButton, fullWidthParams(0, dp(8)));
        }
    }

    private void showConversation(String address) {
        database.markConversationRead(address);
        LinearLayout root = createRoot();
        TextView title = heading(displayNameFor(address));
        root.addView(title, fullWidthParams(0, dp(2)));

        TextView number = new TextView(this);
        number.setText(address);
        number.setTextSize(15);
        root.addView(number, fullWidthParams(0, dp(6)));

        boolean canReply = isCurrentSystemContact(address);
        TextView conversationStatus = new TextView(this);
        conversationStatus.setText(canReply
                ? (isDefaultSmsApp(this) ? "SMS only." : "Parent setup is required before sending SMS.")
                : "This sender is not in the phone Contacts. You can read this thread, but a parent must add the sender before replying.");
        conversationStatus.setTextSize(16);
        root.addView(conversationStatus, fullWidthParams(0, dp(10)));

        ScrollView scrollView = new ScrollView(this);
        messageContainer = new LinearLayout(this);
        messageContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(messageContainer);
        root.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        EditText composer = new EditText(this);
        composer.setHint(canReply ? "Write a message" : "Reply unavailable");
        composer.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        composer.setMinLines(2);
        composer.setEnabled(canReply && isDefaultSmsApp(this) && hasSendSmsPermission());
        root.addView(composer, fullWidthParams(dp(8), dp(6)));

        Button sendButton = button("Send SMS");
        sendButton.setEnabled(canReply && isDefaultSmsApp(this) && hasSendSmsPermission());
        sendButton.setOnClickListener(view -> sendMessage(address, composer));
        root.addView(sendButton, fullWidthParams(0, dp(6)));

        Button backButton = button("Back to messages");
        backButton.setOnClickListener(view -> {
            conversationAddress = null;
            showInbox();
        });
        root.addView(backButton, fullWidthParams(0, 0));
        setContentView(root);
        loadConversation(address);
    }

    private void loadConversation(String address) {
        if (messageContainer == null) {
            return;
        }
        messageContainer.removeAllViews();
        List<SmsMessageRecord> messages = database.loadConversation(address);
        if (messages.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Start a new SMS conversation.");
            empty.setTextSize(17);
            messageContainer.addView(empty, fullWidthParams(0, 0));
            return;
        }
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        for (SmsMessageRecord message : messages) {
            TextView messageView = new TextView(this);
            String direction = message.outgoing
                    ? "Outgoing to " + displayNameFor(message.address)
                    : "Incoming from " + displayNameFor(message.address);
            String status = message.outgoing ? " · " + message.status : "";
            messageView.setText(direction + status + "\n" + message.body + "\n"
                    + dateFormat.format(new Date(message.timestamp)));
            messageView.setTextSize(17);
            int sidePadding = dp(12);
            int verticalPadding = dp(10);
            messageView.setPadding(sidePadding, verticalPadding, sidePadding, verticalPadding);
            messageView.setBackgroundColor(message.outgoing ? 0xFFE3F2FD : 0xFFF1F1F1);
            messageContainer.addView(messageView, fullWidthParams(0, dp(8)));
        }
    }

    private void sendMessage(String address, EditText composer) {
        String body = composer.getText().toString().trim();
        if (!SmsBodyValidator.hasSendableBody(body)) {
            showToast("Write a message first.");
            return;
        }
        if (!isDefaultSmsApp(this)) {
            showToast("A parent must make Reach Kids the default SMS app first.");
            return;
        }
        if (!hasSendSmsPermission()) {
            showToast("A parent must grant SMS permission first.");
            return;
        }
        // Re-check the live system Contacts provider at the send boundary.
        if (!isCurrentSystemContact(address)) {
            showToast("That recipient is no longer in the phone Contacts.");
            showConversation(address);
            return;
        }
        try {
            new SmsSender(this).send(address, body);
            composer.setText("");
            loadConversation(address);
        } catch (SecurityException exception) {
            showToast("Android did not allow this SMS.");
        } catch (IllegalArgumentException exception) {
            showToast("This SMS could not be sent.");
        }
    }

    private boolean isCurrentSystemContact(String address) {
        return hasContactsPermission() && contactsRepository.findContactByPhoneNumber(address) != null;
    }

    private String displayNameFor(String address) {
        if (hasContactsPermission()) {
            SystemContact contact = contactsRepository.findContactByPhoneNumber(address);
            if (contact != null && CallTargetValidator.hasCallableNumber(contact.displayName)) {
                return contact.displayName;
            }
        }
        return address;
    }

    private boolean hasContactsPermission() {
        return android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M
                || checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasSendSmsPermission() {
        return android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M
                || checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private LinearLayout createRoot() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        ChildFriendlyUi.stylePage(root);
        int padding = dp(20);
        root.setPadding(padding, padding, padding, padding);
        return root;
    }

    private TextView heading(String text) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setTextSize(27);
        return title;
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(18);
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

    private void showUnsupportedMms() {
        LinearLayout root = createRoot();
        root.addView(heading("MMS is not supported"), fullWidthParams(0, dp(8)));
        TextView explanation = new TextView(this);
        explanation.setText("Reach Kids currently supports SMS text messages only. MMS, photos, attachments, and group messages are unavailable.");
        explanation.setTextSize(17);
        root.addView(explanation, fullWidthParams(0, dp(16)));
        Button backButton = button("Back");
        backButton.setOnClickListener(view -> finish());
        root.addView(backButton, fullWidthParams(0, 0));
        setContentView(root);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
