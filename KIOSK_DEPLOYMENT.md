# Kinspace Tablet Local - Kiosk Deployment Guide

This guide helps turn an Android tablet into a dedicated Kinspace family hub.

The local app already includes:

- HOME intent filter in [AndroidManifest.xml](C:/Users/allen/kinspace-tablet-local/adhd-focus-app/src/main/AndroidManifest.xml)
- boot relaunch behavior in [KioskBootReceiver.kt](C:/Users/allen/kinspace-tablet-local/adhd-focus-app/src/main/kotlin/com/adhdfocus/app/KioskBootReceiver.kt)
- lock-task attempt in [MainActivity.kt](C:/Users/allen/kinspace-tablet-local/adhd-focus-app/src/main/kotlin/com/adhdfocus/app/MainActivity.kt)

That means the app can behave like a launcher and reopen after reboot. The strongest “only app on the tablet” setup still depends on Android device-owner / managed-device provisioning.

## Package Name

The app package is:

- `com.adhdfocus.app`

## Install The App

Modern:

```powershell
adb install -r "C:\Users\allen\kinspace-tablet-local\dist\KinspaceTabletLocal-debug.apk"
```

Legacy:

```powershell
adb install -r "C:\Users\allen\kinspace-tablet-local\dist\KinspaceTabletLocal-legacy-debug.apk"
```

## Option 1: Simple Home App Setup

This is the easier path for personal household tablets.

1. Install the APK.
2. Press the Home button on the device.
3. When Android asks which Home app to use, choose `Kinspace Tablet Local`.
4. Set it as `Always`.

This gives you:

- app launches from Home
- app can relaunch after boot
- app feels like the main surface

This does **not** fully prevent leaving the app if the device still exposes standard Android navigation and settings.

## Option 2: Lock Task / Pinned Experience

The app already attempts `startLockTask()` when Android allows it.

This works best when:

- the device is in a managed setup, or
- the app is allowlisted for lock task by a device-owner policy

Without device-owner policy, Android behavior varies by device and OS version.

## Option 3: Device Owner / Managed Tablet

This is the strongest deployment path for a true family hub device.

With device-owner mode, you can:

- allowlist the app for lock task
- hide or restrict system navigation
- reduce access to other apps
- make Kinspace the practical single-purpose surface

Important:

- device-owner setup usually must be done on a freshly reset device
- provisioning steps vary by vendor and Android version

## ADB Helper Script

Use:

- [kiosk-device-setup.ps1](C:/Users/allen/kinspace-tablet-local/kiosk-device-setup.ps1)

It helps with:

- checking connected devices
- installing the APK
- launching the app
- opening the system Home-app chooser
- opening app details
- showing example device-owner / lock-task commands

## Useful Manual Commands

Check devices:

```powershell
adb devices
```

Launch the app:

```powershell
adb shell monkey -p com.adhdfocus.app -c android.intent.category.LAUNCHER 1
```

Open app settings:

```powershell
adb shell am start -a android.settings.APPLICATION_DETAILS_SETTINGS -d package:com.adhdfocus.app
```

Open Home-app settings / chooser:

```powershell
adb shell am start -a android.intent.action.MAIN -c android.intent.category.HOME
```

## Recommended Real-World Setup

For most households:

1. Use a tablet dedicated to Kinspace
2. Install `KinspaceTabletLocal`
3. Set it as the Home app
4. Disable or hide other distracting apps if possible
5. Keep a caregiver passcode on Settings
6. Use local backups regularly from Settings

For stricter managed deployments:

1. Factory reset the tablet
2. Provision device-owner mode
3. Install Kinspace
4. Allowlist `com.adhdfocus.app` for lock task
5. Set Kinspace as launcher/home app

## Current Limits

This repo now supports kiosk-friendly behavior well, but it does not yet include:

- an in-app device-owner provisioning flow
- automatic system-policy management
- guaranteed vendor-specific navigation disabling

Those are device-management concerns rather than normal app-only features.
