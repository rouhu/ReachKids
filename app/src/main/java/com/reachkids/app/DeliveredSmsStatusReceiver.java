package com.reachkids.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Updates local delivery status when the carrier provides a delivery report. */
public final class DeliveredSmsStatusReceiver extends BroadcastReceiver {
    public static final String ACTION_DELIVERED_STATUS = "com.reachkids.app.SMS_DELIVERED_STATUS";
    public static final String EXTRA_MESSAGE_ID = "message_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_DELIVERED_STATUS.equals(intent.getAction())) {
            return;
        }
        long messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1L);
        if (messageId != -1L && getResultCode() == android.app.Activity.RESULT_OK) {
            new SmsDatabase(context).updateStatus(messageId, "delivered");
        }
    }
}
