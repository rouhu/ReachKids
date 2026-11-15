package com.reachkids.app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Looper;

import java.lang.reflect.Method;

/**
 * Turns the phone's GPS on and captures a position fix.
 *
 * <p>Android does not let an ordinary app flip the system location switch: that needs the
 * signature-level {@code CHANGE_LOCATION_MODE} permission. Reach Kids therefore uses every
 * route the platform actually offers:
 *
 * <ol>
 *     <li>As the enrolled device owner it can call {@code DevicePolicyManager.setLocationEnabled}
 *         (API 28 and later) to switch the phone-wide location setting back on.</li>
 *     <li>Older releases expose {@code LocationManager.setProviderEnabled}, so Reach Kids tries
 *         it as a best-effort and accepts that Android normally refuses it.</li>
 *     <li>Requesting updates keeps the listener registered even while the GPS is off, so the fix
 *         arrives as soon as the provider is switched on, and
 *         {@code getLastKnownLocation} supplies the cached fix in the meantime.</li>
 * </ol>
 */
public final class LocationProviderController {
    private static final String GPS_PROVIDER = LocationManager.GPS_PROVIDER;
    private static final String NETWORK_PROVIDER = LocationManager.NETWORK_PROVIDER;
    /** How long the reply waits for a live fix before it falls back to the cached one. */
    public static final long FIX_TIMEOUT_MILLIS = 45000L;
    /** A cached fix younger than this is recent enough to report as the current position. */
    public static final long FRESH_FIX_MILLIS = 120000L;
    private static final long MINIMUM_INTERVAL_MILLIS = 0L;
    private static final float MINIMUM_DISTANCE_METERS = 0f;

    private final Context context;
    private final LocationManager locationManager;
    private final DevicePolicyController devicePolicyController;

    public LocationProviderController(Context context) {
        this(context, new DevicePolicyController(context));
    }

    public LocationProviderController(Context context, DevicePolicyController devicePolicyController) {
        this.context = context.getApplicationContext();
        this.locationManager = (LocationManager) this.context.getSystemService(Context.LOCATION_SERVICE);
        this.devicePolicyController = devicePolicyController;
    }

    /** True when the parent has granted Reach Kids access to the phone's location. */
    public boolean hasLocationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** True when only the approximate location was granted, so the fix is not a GPS fix. */
    public boolean isPreciseLocationAllowed() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** True when the platform reports that the GPS provider is switched on. */
    public boolean isGpsEnabled() {
        if (locationManager == null) {
            return false;
        }
        try {
            return locationManager.isProviderEnabled(GPS_PROVIDER);
        } catch (SecurityException | IllegalArgumentException exception) {
            return false;
        }
    }

    /**
     * Switches the GPS provider on where Android allows it and starts listening for a fix from
     * both the GPS and the network provider. The first fix that arrives wins, so a phone whose
     * GPS toggle is off still reports a position through the network provider.
     * Returns false only when the platform refused to register the listener at all.
     */
    public boolean enableGps(LocationListener listener) {
        // The device owner is the only role allowed to turn the phone-wide location setting on.
        devicePolicyController.enableLocation();
        // Older releases expose this call; it needs a signature permission, so it usually fails.
        trySetProviderEnabled(GPS_PROVIDER);
        boolean gpsListening = requestProviderUpdates(GPS_PROVIDER, listener);
        // The network provider gives a coarser fix but works when the GPS toggle is off.
        boolean networkListening = requestProviderUpdates(NETWORK_PROVIDER, listener);
        return gpsListening || networkListening;
    }

    private void trySetProviderEnabled(String provider) {
        try {
            Method method = LocationManager.class.getMethod("setProviderEnabled", String.class,
                    boolean.class);
            method.invoke(locationManager, provider, true);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Android refuses this for ordinary apps, which is expected.
        }
    }

    /**
     * Registers the listener for a provider. Android accepts the request even when the
     * provider is switched off, and then simply delivers no update until it is switched on.
     */
    private boolean requestProviderUpdates(String provider, LocationListener listener) {
        if (locationManager == null) {
            return false;
        }
        try {
            locationManager.requestLocationUpdates(provider, MINIMUM_INTERVAL_MILLIS,
                    MINIMUM_DISTANCE_METERS, listener, Looper.getMainLooper());
            return true;
        } catch (SecurityException | IllegalArgumentException exception) {
            return false;
        }
    }

    public void removeUpdates(LocationListener listener) {
        if (locationManager == null) {
            return;
        }
        try {
            locationManager.removeUpdates(listener);
        } catch (SecurityException | IllegalArgumentException ignored) {
            // The provider was already released.
        }
    }

    /** The most recent cached fix, preferring GPS over the network provider. */
    public Location lastKnownLocation() {
        if (locationManager == null) {
            return null;
        }
        Location best = null;
        for (String provider : new String[]{GPS_PROVIDER, NETWORK_PROVIDER}) {
            Location candidate = lastKnownFor(provider);
            if (candidate == null) {
                continue;
            }
            if (best == null || candidate.getTime() > best.getTime()) {
                best = candidate;
            }
        }
        return best;
    }

    private Location lastKnownFor(String provider) {
        try {
            // Android keeps the cached fix even when the provider is switched off.
            return locationManager.getLastKnownLocation(provider);
        } catch (SecurityException | IllegalArgumentException exception) {
            return null;
        }
    }

    /** True when a cached fix is recent enough to be presented as the current position. */
    public static boolean isFresh(Location location, long nowMillis) {
        return location != null && location.getTime() > 0L
                && nowMillis - location.getTime() <= FRESH_FIX_MILLIS;
    }
}