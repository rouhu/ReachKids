package com.reachkids.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Required for default-SMS eligibility. MMS is intentionally unsupported in Phase 4. */
public final class MmsDeliveryReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Phase 4 intentionally does not store, display, or compose MMS.
    }
}
