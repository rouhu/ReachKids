package com.reachkids.app;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * The version details of the installed Reach Kids build.
 *
 * <p>The values are read from the installed package rather than from the build script, so the
 * parent area always shows what is actually running on the phone.
 */
public final class AppVersionInfo {
    private final String versionName;
    private final long versionCode;
    private final long lastUpdateTime;

    private AppVersionInfo(String versionName, long versionCode, long lastUpdateTime) {
        this.versionName = versionName;
        this.versionCode = versionCode;
        this.lastUpdateTime = lastUpdateTime;
    }

    /** Reads the version of the installed package, or an empty label when it cannot be read. */
    @SuppressWarnings("deprecation")
    public static AppVersionInfo read(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
            long code;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                code = info.getLongVersionCode();
            } else {
                code = info.versionCode;
            }
            return new AppVersionInfo(info.versionName, code, info.lastUpdateTime);
        } catch (PackageManager.NameNotFoundException | RuntimeException exception) {
            return new AppVersionInfo(null, 0L, 0L);
        }
    }

    /** The version name, for example {@code 0.2.0-phase1}. */
    public String versionName() {
        return versionName;
    }

    /** The build number, which increases with every published build. */
    public long versionCode() {
        return versionCode;
    }

    /** A single line such as {@code Version 0.2.0-phase1 (build 2)}. */
    public String versionLabel() {
        return versionLabel(versionName, versionCode);
    }

    /** A line showing when the build was installed on the phone. */
    public String updatedLabel() {
        return updatedLabel(lastUpdateTime);
    }

    /** The version line and the install date, one after the other. */
    public String summary() {
        return versionLabel() + "\n" + updatedLabel();
    }

    /** Formats the version name and build number for display. */
    public static String versionLabel(String versionName, long versionCode) {
        if (versionName == null || versionName.trim().isEmpty()) {
            return "Version unknown";
        }
        return "Version " + versionName.trim() + " (build " + versionCode + ")";
    }

    /** Formats the install date for display. */
    public static String updatedLabel(long lastUpdateTime) {
        if (lastUpdateTime <= 0L) {
            return "Installed: unknown";
        }
        String date = new SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.getDefault())
                .format(new Date(lastUpdateTime));
        return "Installed: " + date;
    }
}
