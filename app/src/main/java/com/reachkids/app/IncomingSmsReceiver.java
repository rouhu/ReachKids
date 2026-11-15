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
            if (address == null || body == null) {
                continue;
            }
            database.insertIncoming(address, body, message.getTimestampMillis());
            if (LocationRequestCommand.isLocationRequest(body)) {
                handleLocationRequest(context, address);
            } else if (PinResetCommand.isPinResetRequest(body)) {
                handlePinResetRequest(context, address);
            }
        }
    }

    /**
     * Answers a location request only when it comes from the parent-selected default contact,
     * so no other sender can make the phone reveal its position.
     */
    private void handleLocationRequest(Context context, String address) {
        SystemContact defaultContact = findDefaultContact(context, address);
        if (defaultContact == null) {
            return;
        }
        LocationRequestService.start(context, defaultContact.phoneNumber,
                defaultContact.displayName);
    }

    /**
     * Opens the temporary parent PIN only for the default contact, so no other sender can
     * weaken the protection of the parent area.
     */
    private void handlePinResetRequest(Context context, String address) {
        SystemContact defaultContact = findDefaultContact(context, address);
        if (defaultContact == null) {
            return;
        }
        TemporaryPinOverride.activate(context);
    }

    /** The default contact when it is set and the message came from that number. */
    private SystemContact findDefaultContact(Context context, String address) {
        try {
            ParentPreferences preferences = new ParentPreferences(context);
            if (!preferences.hasDefaultContact()) {
                return null;
            }
            SystemContact defaultContact = new ContactsRepository(context)
                    .loadPhoneContact(preferences.getDefaultContactPhoneDataId());
            if (defaultContact == null) {
                return null;
            }
            if (!SmsAddressValidator.addressesMatch(defaultContact.phoneNumber, address)) {
                return null;
            }
            return defaultContact;
        } catch (RuntimeException exception) {
            // A failed lookup must never take the whole SMS delivery down.
            return null;
        }
    }
}
