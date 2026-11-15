package com.reachkids.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

/**
 * Captures the phone position for a {@code #Location} request and texts the coordinates back
 * to the default contact.
 *
 * <p>The service runs in the foreground because Android only lets a background app keep working
 * for a short time after a broadcast, and a GPS fix can take a while. It stops itself as soon
 * as the reply has been sent.
 */
public final class LocationRequestService extends Service {
    public static final String ACTION_SEND_LOCATION = "com.reachkids.app.action.SEND_LOCATION";
    public static final String EXTRA_ADDRESS = "address";
    public static final String EXTRA_CONTACT_NAME = "contact_name";
    private static final String CHANNEL_ID = "reach_kids_location";
    private static final int NOTIFICATION_ID = 4711;
    private static final String NOTIFICATION_TITLE = "Reach Kids location request";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private LocationProviderController locationController;
    private SmsSender smsSender;
    private String destinationAddress;
    private String contactName;
    private Location lastKnownLocation;
    private boolean replySent;
    private boolean started;

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            deliverLocation(location);
        }

        @Override
        public void onProviderDisabled(String provider) {
            // Nothing to do; the timeout below still sends the best fix available.
        }

        @Override
        public void onProviderEnabled(String provider) {
            // The provider is live; the fix arrives through onLocationChanged.
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // Deprecated on modern Android and never called on the supported releases.
        }
    };

    /**
     * Starts the location capture for a verified default-contact request. When Android refuses
     * the service, the parent still gets a short explanation by SMS.
     */
    public static void start(Context context, String address, String contactName) {
        if (address == null || address.trim().isEmpty()) {
            return;
        }
        if (!new LocationProviderController(context).hasLocationPermission()) {
            sendDirectReply(context, address,
                    "Reach Kids could not report a location: location permission was not granted.");
            return;
        }
        Intent intent = new Intent(context, LocationRequestService.class)
                .setAction(ACTION_SEND_LOCATION)
                .putExtra(EXTRA_ADDRESS, address)
                .putExtra(EXTRA_CONTACT_NAME, contactName);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (IllegalStateException | SecurityException exception) {
            // Android refused to start the service, so the parent is told why.
            sendDirectReply(context, address,
                    "Reach Kids could not report a location: Android would not start the "
                            + "location lookup.");
        }
    }

    /** Sends a short explanation without the foreground service. */
    private static void sendDirectReply(Context context, String address, String body) {
        try {
            new SmsSender(context).send(address, body);
        } catch (SecurityException | IllegalArgumentException exception) {
            // Android refused the reply, so nothing further can be done here.
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        locationController = new LocationProviderController(this);
        smsSender = new SmsSender(this);
        showNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || !ACTION_SEND_LOCATION.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        destinationAddress = intent.getStringExtra(EXTRA_ADDRESS);
        contactName = intent.getStringExtra(EXTRA_CONTACT_NAME);
        if (destinationAddress == null || destinationAddress.trim().isEmpty()) {
            stopSelf();
            return START_NOT_STICKY;
        }
        updateNotification();
        if (started) {
            return START_NOT_STICKY;
        }
        started = true;
        captureLocation();
        return START_NOT_STICKY;
    }

    private void captureLocation() {
        if (!locationController.hasLocationPermission()) {
            sendReply(buildUnavailableMessage("location permission was not granted"));
            return;
        }
        boolean listening = locationController.enableGps(locationListener);
        if (!listening) {
            sendReply(buildUnavailableMessage("Android did not allow the GPS to be switched on"));
            return;
        }
        // The cached fix is only used when the GPS does not deliver a live fix in time.
        lastKnownLocation = locationController.lastKnownLocation();
        handler.postDelayed(timeoutRunnable, LocationProviderController.FIX_TIMEOUT_MILLIS);
    }

    private final Runnable timeoutRunnable = new Runnable() {
        @Override
        public void run() {
            // No live fix arrived, so report the cached position when there is one.
            sendReply(buildLocationMessage(lastKnownLocation));
        }
    };

    private void deliverLocation(Location location) {
        if (replySent || location == null) {
            return;
        }
        // A live GPS fix arrived, so the reply does not wait any longer.
        handler.removeCallbacks(timeoutRunnable);
        sendReply(buildLocationMessage(location));
    }

    private String buildLocationMessage(Location location) {
        if (location == null) {
            return buildUnavailableMessage("no GPS fix was available");
        }
        long now = System.currentTimeMillis();
        boolean fresh = LocationProviderController.isFresh(location, now);
        String provider = location.getProvider() == null ? "unknown" : location.getProvider();
        String message = LocationReplyText.buildMessage(location.getLatitude(),
                location.getLongitude(), provider, location.getAccuracy(), !fresh,
                location.getTime());
        if (!fresh) {
            message = LocationReplyText.withWarning(message,
                    "Note: this is a cached position. The GPS did not deliver a fresh fix.");
        }
        return message;
    }

    private String buildUnavailableMessage(String reason) {
        return "Reach Kids could not report a location: " + reason + ".";
    }

    private void sendReply(String body) {
        if (replySent) {
            return;
        }
        replySent = true;
        handler.removeCallbacks(timeoutRunnable);
        locationController.removeUpdates(locationListener);
        try {
            smsSender.send(destinationAddress, body);
        } catch (SecurityException | IllegalArgumentException exception) {
            // Android refused the reply, so nothing further can be done here.
        }
        stopSelf();
    }

    private void showNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Location requests", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Shows while Reach Kids finds the phone position.");
            manager.createNotificationChannel(channel);
        }
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        builder.setContentTitle(NOTIFICATION_TITLE)
                .setContentText(describeWork())
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true);
        try {
            startForeground(NOTIFICATION_ID, builder.build());
        } catch (IllegalStateException | SecurityException exception) {
            // Android refused the foreground call; the service still finishes its work.
        }
    }

    /** Refreshes the notification once the recipient of the reply is known. */
    private void updateNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        builder.setContentTitle(NOTIFICATION_TITLE)
                .setContentText(describeWork())
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true);
        try {
            manager.notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException exception) {
            // The notification is only a hint; the reply does not depend on it.
        }
    }

    private String describeWork() {
        if (contactName == null || contactName.trim().isEmpty()) {
            return "Finding the phone position.";
        }
        return "Finding the phone position for " + contactName.trim() + ".";
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(timeoutRunnable);
        locationController.removeUpdates(locationListener);
        super.onDestroy();
    }
}
