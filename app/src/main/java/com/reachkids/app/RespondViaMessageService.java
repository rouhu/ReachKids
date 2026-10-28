package com.reachkids.app;

import android.app.IntentService;
import android.content.Intent;

/** Required default-SMS component. Quick responses are intentionally not exposed in this phase. */
public final class RespondViaMessageService extends IntentService {
    public RespondViaMessageService() {
        super("ReachKidsRespondViaMessage");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // No quick-reply UI is provided in Phase 4.
    }
}
