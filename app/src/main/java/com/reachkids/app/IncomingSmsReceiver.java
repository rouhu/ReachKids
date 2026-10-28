package com.reachkids.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;

/** Receives SMS delivery only when Android routes it to Reach Kids as the default SMS app. */
public final class IncomingSmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_DELIVER_ACTION.equals(intent.getAction())) {
            return;
        }
        SmsDatabase database = new SmsDatabase(context);
        for (SmsMessage message : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
            String address = message.getDisplayOriginatingAddress();
            String body = message.getDisplayMessageBody();
            if (address != null && body != null) {
                database.insertIncoming(address, body, message.getTimestampMillis());
            }
        }
    }
}
