# Kinspace Tablet Install Checklist

Use this checklist to install the tablet app on a physical Android device.

## Prerequisites

- Android device with Developer Options enabled
- `USB debugging` enabled
- USB cable connected to your Windows PC
- Android platform tools installed so `adb` is available

## Install Steps

1. Confirm the device is visible to ADB:

   ```powershell
   adb devices
   ```

2. Install the signed release APK:

   ```powershell
   adb install -r "C:\Users\allen\calendar-tablet-adhd\dist\KinspaceTablet-release.apk"
   ```

3. Open `Kinspace Tablet` from the device app launcher.
4. Complete onboarding and sign in to Kinspace Cloud.
5. Assign the tablet to the correct family member.
6. Verify the Home screen loads todos and the Achievements screen opens normally.

## If Install Fails

- If you see `INSTALL_FAILED_VERSION_DOWNGRADE`, uninstall the old app first:

  ```powershell
  adb uninstall com.adhdfocus.app
  ```

  Then reinstall the APK.

- If `adb devices` shows `unauthorized`, unplug and replug the cable, then approve USB debugging on the device.
- If the app opens to a blank state, confirm the device is connected to the internet and can reach Kinspace Cloud.

## Files

- Release APK: [`dist/KinspaceTablet-release.apk`](./dist/KinspaceTablet-release.apk)
- Packaging script: [`package-tablet-installer.ps1`](./package-tablet-installer.ps1)

