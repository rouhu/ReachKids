package com.reachkids.app;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;

/** Applies device-owner-only kiosk policy. All methods are safe to call when unmanaged. */
public final class DevicePolicyController {
    private static final String TELECOM_PACKAGE = "com.android.server.telecom";
    private static final String SAMSUNG_CONTACTS_PACKAGE = "com.samsung.android.contacts";
    private static final String SAMSUNG_IN_CALL_UI_PACKAGE = "com.samsung.android.incallui";
    private final Context context;
    private final DevicePolicyManager devicePolicyManager;
    private final ComponentName adminComponent;
    private final ComponentName homeActivity;

    public DevicePolicyController(Context context) {
        this.context = context.getApplicationContext();
        devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(context, ReachKidsDeviceAdminReceiver.class);
        homeActivity = new ComponentName(context, MainActivity.class);
    }

    public boolean isDeviceOwner() {
        return devicePolicyManager != null && devicePolicyManager.isDeviceOwnerApp(context.getPackageName());
    }

    public boolean canApplyKioskPolicy() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && isDeviceOwner();
    }

    public void enableKioskPolicy(boolean cameraDisabled) {
        requireDeviceOwner();
        devicePolicyManager.setLockTaskPackages(adminComponent, lockTaskPackages());
        setPersistentHome();
        devicePolicyManager.setCameraDisabled(adminComponent, cameraDisabled);
    }

    public void applyCameraPolicy(boolean disabled) {
        requireDeviceOwner();
        devicePolicyManager.setCameraDisabled(adminComponent, disabled);
    }

    public void clearKioskPolicy() {
        requireDeviceOwner();
        devicePolicyManager.clearPackagePersistentPreferredActivities(adminComponent,
                context.getPackageName());
        devicePolicyManager.setLockTaskPackages(adminComponent, new String[0]);
    }

    public void enterLockTask(Activity activity) {
        if (canApplyKioskPolicy() && !isLockTaskActive()) {
            activity.startLockTask();
        }
    }

    private boolean isLockTaskActive() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        }
        ActivityManager activityManager = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        return activityManager != null
                && activityManager.getLockTaskModeState()
                != ActivityManager.LOCK_TASK_MODE_NONE;
    }

    public void exitLockTask(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.stopLockTask();
        }
    }

    public String diagnostics() {
        if (devicePolicyManager == null) {
            return "Device policy service is unavailable.";
        }
        if (!isDeviceOwner()) {
            return "Not device owner. Factory-reset the phone and enroll Reach Kids as device owner before enabling kiosk mode.";
        }
        return "Device owner active. Kiosk policy can be enabled from this parent area.";
    }

    private void setPersistentHome() {
        IntentFilter homeFilter = new IntentFilter(Intent.ACTION_MAIN);
        homeFilter.addCategory(Intent.CATEGORY_HOME);
        homeFilter.addCategory(Intent.CATEGORY_DEFAULT);
        devicePolicyManager.addPersistentPreferredActivity(adminComponent, homeFilter, homeActivity);
    }

    /** Allows the system dialer call flow only when those packages exist on the device. */
    private String[] lockTaskPackages() {
        List<String> packages = new ArrayList<>();
        packages.add(context.getPackageName());
        addIfInstalled(packages, TELECOM_PACKAGE);
        addIfInstalled(packages, SAMSUNG_CONTACTS_PACKAGE);
        addIfInstalled(packages, SAMSUNG_IN_CALL_UI_PACKAGE);
        return packages.toArray(new String[0]);
    }

    private void addIfInstalled(List<String> packages, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            packages.add(packageName);
        } catch (android.content.pm.PackageManager.NameNotFoundException ignored) {
            // This handset uses a different system dialer implementation.
        }
    }

    private void requireDeviceOwner() {
        if (!canApplyKioskPolicy()) {
            throw new IllegalStateException("Reach Kids is not enrolled as the device owner.");
        }
    }
}
