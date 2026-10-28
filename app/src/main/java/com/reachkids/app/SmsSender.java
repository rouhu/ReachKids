package com.reachkids.app;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;

import java.util.ArrayList;

/** Sends SMS messages after the caller has validated the current Contacts-provider recipient. */
public final class SmsSender {
    private final Context context;
    private final SmsDatabase database;

    public SmsSender(Context context) {
        this.context = context.getApplicationContext();
        database = new SmsDatabase(context);
    }

    public long send(String destinationAddress, String body) {
        long messageId = database.insertOutgoingPending(destinationAddress, body,
                System.currentTimeMillis());
        SmsManager manager = SmsManager.getDefault();
        ArrayList<String> parts = manager.divideMessage(body);
        ArrayList<PendingIntent> sentIntents = new ArrayList<>();
        ArrayList<PendingIntent> deliveredIntents = new ArrayList<>();
        for (int index = 0; index < parts.size(); index++) {
            sentIntents.add(statusIntent(SentSmsStatusReceiver.ACTION_SENT_STATUS,
                    SentSmsStatusReceiver.class, SentSmsStatusReceiver.EXTRA_MESSAGE_ID, messageId, index));
            deliveredIntents.add(statusIntent(DeliveredSmsStatusReceiver.ACTION_DELIVERED_STATUS,
                    DeliveredSmsStatusReceiver.class, DeliveredSmsStatusReceiver.EXTRA_MESSAGE_ID, messageId, index));
        }
        if (parts.size() == 1) {
            manager.sendTextMessage(destinationAddress, null, body,
                    sentIntents.get(0), deliveredIntents.get(0));
        } else {
            manager.sendMultipartTextMessage(destinationAddress, null, parts,
                    sentIntents, deliveredIntents);
        }
        return messageId;
    }

    private PendingIntent statusIntent(String action, Class<?> receiverClass, String idExtra,
            long messageId, int partIndex) {
        Intent intent = new Intent(context, receiverClass)
                .setAction(action)
                .putExtra(idExtra, messageId)
                .putExtra(SentSmsStatusReceiver.EXTRA_PART_INDEX, partIndex);
        int requestCode = (int) (messageId ^ (messageId >>> 32) ^ partIndex ^ action.hashCode());
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
