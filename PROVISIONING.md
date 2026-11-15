# Device-owner provisioning and kiosk test procedure

This procedure is for the target **Samsung Galaxy A3 (2016) on Android 7**. It is a development/test procedure, not a claim that the phone is tamper-proof. Record the exact model identifier, firmware build number, carrier, and SIM behavior with the test results.

## Prerequisites

- A factory-reset phone that has not completed normal user/account setup.
- A computer with Android Platform Tools (`adb`), a compatible JDK, Android SDK, and a built Reach Kids APK.
- USB debugging enabled during initial device setup, if the Samsung setup flow permits it.
- The APK package ID remains `com.reachkids.app` and its device-admin receiver remains `com.reachkids.app/.ReachKidsDeviceAdminReceiver`.

## Install and enroll

1. Factory-reset the phone. Do not add a Google account or create a normal device user before enrollment.
2. Connect the phone by USB and verify it is visible:

   ```text
   adb devices
   ```

3. Install the APK:

   ```text
   adb install app-debug.apk
   ```

4. Set Reach Kids as device owner:

   ```text
   adb shell dpm set-device-owner com.reachkids.app/.ReachKidsDeviceAdminReceiver
   ```

5. Confirm that Android reports success. If this command fails because the device already has users/accounts or has progressed too far through setup, factory-reset again and repeat the procedure. Do not attempt to work around the device-owner restriction on a phone intended for child use.
6. Launch Reach Kids, create the parent PIN, grant Contacts permission, open **Parent area**, and verify that the device diagnostic says device owner is active.
7. In **Parent area**, select **Set up SMS (parent only)**. Grant the requested SMS permissions, then confirm Android's default-SMS-app prompt for Reach Kids.
8. In **Parent area**, select **Set default contact** and choose the contact that is allowed to ask for the phone location.
9. In **Parent area**, select **Set up location access (parent only)** and grant the location permission. Confirm that the phone-wide location setting is on; Reach Kids can switch it on itself only when the phone is enrolled as device owner on Android 9 or later.
10. Configure contacts and the camera preference, then select **Enable kiosk mode**.
11. Disconnect the USB cable and turn off the developer access on the phone:
    - **Settings → Developer options → USB debugging**: off.
    - **Settings → Developer options → Revoke USB debugging authorizations**: tap it, so the setup computer is no longer trusted.
    - **Settings → Developer options**: turn the master switch off. On Android 7 the screen is then hidden from normal use.
    - On the computer, confirm `adb devices` no longer lists the phone.

    Do this only after the enrollment commands above have succeeded. The device-owner enrollment stays active when USB debugging is switched off, so kiosk mode, the camera policy, and the persistent Home setting keep working. Keep the APK and these instructions, because the phone will need re-provisioning after a factory reset.

    Note that this raises the effort needed to bypass kiosk mode; it does not make the phone tamper-proof, since a factory reset or a recovery-mode boot can still restore developer access.

## Required test checklist

Run and record every test on the target handset before relying on it:

- Lock task starts when kiosk mode is enabled.
- Pressing Home and Recents does not leave Reach Kids.
- Restarting the phone returns to Reach Kids as Home and re-enters lock task after app launch.
- The camera is unavailable when the parent setting is enabled and returns when disabled.
- Parent area remains accessible only after correct PIN entry.
- The child can call only from **Call a contact** and is never shown a Reach Kids keypad or manual-number field.
- `CALL_PHONE` permission is requested/granted, and selecting a system contact starts the intended call.
- Reach Kids is confirmed as the default SMS app after parent setup; incoming SMS reaches the Reach Kids inbox.
- A child can start a new SMS only by selecting a system Contact; there is no number-entry field.
- An incoming SMS from an unknown number is readable, but its composer and send button are disabled until a parent adds that number to system Contacts.
- Verify short and multipart SMS sending, failed send/no-signal handling, delivery reports where the carrier supports them, SIM removal, and reboot behavior.
- Verify MMS, attachment, and group-message attempts show an unsupported state rather than a compose flow.
- Verify a `#Location` request from the default contact switches the GPS on, returns the phone position by SMS, and that a `#Location` request from any other sender is ignored.
- Verify the location reply when the phone-wide location setting is off, when no fix is available, and when the cached fix is reported.
- Verify a `#ResetPin` message from the default contact lets the PIN `1234` open the parent area, that the same message from any other sender does nothing, and that the temporary PIN stops working after it is used once and when it expires after five minutes.
- While lock task is active, test the Samsung phone UI carefully: verify whether the child can reach keypad, recents, contacts, or other apps from the system call screen. Do not rely on this call flow until the result is acceptable on the exact handset firmware.
- **Exit kiosk mode (parent only)** stops lock task and removes Reach Kids as the persistent Home application.
- Re-enabling kiosk restores the same restrictions.
- Verify that after USB debugging and Developer options are switched off, the phone no longer appears in `adb devices`, that kiosk mode still starts on reboot, and that Reach Kids still receives SMS.
- Recovery-mode and factory-reset behavior are documented as out-of-app escape/recovery paths.

## Current limitations

Phase 4 provides an SMS-only inbox, contact-only compose flow, and default-SMS setup. It does not provide MMS, attachments, group messaging, emergency-dialer integration, or a signed release APK.

Lock task and persistent Home settings restrict ordinary navigation only. They do not make a device physically tamper-proof, and Samsung firmware behavior must be validated on the actual device.