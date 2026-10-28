package com.reachkids.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/** Updates the locally displayed result of an outgoing SMS send request. */
public final class SentSmsStatusReceiver extends BroadcastReceiver {
    public static final String ACTION_SENT_STATUS = "com.reachkids.app.SMS_SENT_STATUS";
    public static final String EXTRA_MESSAGE_ID = "message_id";
    public static final String EXTRA_PART_INDEX = "part_index";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_SENT_STATUS.equals(intent.getAction())) {
            return;
        }
        long messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1L);
        if (messageId == -1L) {
            return;
        }
        boolean sent = getResultCode() == android.app.Activity.RESULT_OK;
        new SmsDatabase(context).updateStatus(messageId, sent ? "sent" : "failed");
        // Multipart SMS requests produce one result per part; notify once for the first part.
        if (intent.getIntExtra(EXTRA_PART_INDEX, 0) == 0) {
            Toast.makeText(context, sent ? "SMS sent." : "SMS could not be sent.",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
