# Kinspace Tablet Local Install Guide

Use this guide to install the local-only tablet app on an Android device.

## Prerequisites

- Android device with Developer Options enabled
- `USB debugging` enabled
- Android platform tools installed so `adb` is available
- One of the packaged APKs from `dist/`

## Pick The Right APK

- Modern Android tablets:
  - [KinspaceTabletLocal-debug.apk](C:/Users/allen/kinspace-tablet-local/dist/KinspaceTabletLocal-debug.apk)
- Older tablets / legacy compatibility:
  - [KinspaceTabletLocal-legacy-debug.apk](C:/Users/allen/kinspace-tablet-local/dist/KinspaceTabletLocal-legacy-debug.apk)

## Install Steps

1. Confirm the device is visible:

   ```powershell
   adb devices
   ```

2. Install the chosen APK:

   ```powershell
   adb install -r "C:\Users\allen\kinspace-tablet-local\dist\KinspaceTabletLocal-debug.apk"
   ```

   or

   ```powershell
   adb install -r "C:\Users\allen\kinspace-tablet-local\dist\KinspaceTabletLocal-legacy-debug.apk"
   ```

3. Launch `Kinspace Tablet Local`.
4. Complete local setup on the device:
   - household name
   - family members
   - default member
5. Open Settings and confirm:
   - category reminder toggles
   - reports access
   - local backup section

## Reinstalling

If you see `INSTALL_FAILED_VERSION_DOWNGRADE`, uninstall first:

```powershell
adb uninstall com.adhdfocus.app
```

Then reinstall the APK.

## Dedicated Device Notes

This app includes kiosk-friendly groundwork:

- home-app intent filter
- boot relaunch receiver
- lock-task attempt on resume

For a fully locked-down family hub device, Android device-owner or managed-device setup is still recommended.

See also:

- [KIOSK_DEPLOYMENT.md](C:/Users/allen/kinspace-tablet-local/KIOSK_DEPLOYMENT.md)
- [kiosk-device-setup.ps1](C:/Users/allen/kinspace-tablet-local/kiosk-device-setup.ps1)

## Local Backup Notes

Backups are created from the Settings screen and stored in the app's Documents area under:

- `kinspace_backups`

These backups are intended to help move the household setup and history between devices without cloud sync.
