# Kinspace Tablet Local

Local-only Android tablet app for family organization, ADHD-friendly routines, timers, achievements, and caregiver reporting.

This fork is designed for dedicated household devices that should keep working even without internet or cloud sign-in.

## What It Does

- runs fully on-device with Room local storage
- supports local household setup and family member management
- tracks todos, timer sessions, streaks, achievements, and puzzle progress
- provides per-person reporting for caregivers
- includes modern and legacy Android build flavors
- includes kiosk-friendly startup behavior for dedicated tablets

## Key Local Features

- **Local setup**: create a household and family members directly on the tablet
- **Daily focus view**: streamlined home screen for the active family member
- **Timer analytics**: pauses, resets, cancels, stopped-early sessions, and completion timing
- **Achievements**: badges, streaks, and puzzle progress
- **Reports**: per-person stats, breakdowns, 7-day trends, and suggested next moves
- **Backups**: export local database backups from Settings

## Build Variants

- `modernDebug`
- `legacyDebug`
- `modernRelease`
- `legacyRelease`

## Packaged Installers

Packaged APKs are written to:

- [KinspaceTabletLocal-debug.apk](C:/Users/allen/kinspace-tablet-local/dist/KinspaceTabletLocal-debug.apk)
- [KinspaceTabletLocal-legacy-debug.apk](C:/Users/allen/kinspace-tablet-local/dist/KinspaceTabletLocal-legacy-debug.apk)

## Build Commands

```powershell
./gradlew.bat :adhd-focus-app:assembleModernDebug
./gradlew.bat :adhd-focus-app:assembleLegacyDebug
```

To package the installers into `dist/`:

```powershell
./package-tablet-local-installer.ps1
./package-tablet-local-installer.ps1 -Legacy
```

If the tablet exposes multiple Android users, install and launch the APK as user `0`:

```powershell
adb install --user 0 -r "C:\Users\allen\kinspace-tablet-local\dist\KinspaceTabletLocal-legacy-debug.apk"
adb shell am start --user 0 -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n com.adhdfocus.app/.MainActivity
```

## Install On A Device

```powershell
adb install --user 0 -r "C:\Users\allen\kinspace-tablet-local\dist\KinspaceTabletLocal-debug.apk"
adb install --user 0 -r "C:\Users\allen\kinspace-tablet-local\dist\KinspaceTabletLocal-legacy-debug.apk"
```

## Architecture

See:

- [PACKAGE_STRUCTURE.md](C:/Users/allen/kinspace-tablet-local/adhd-focus-app/src/main/kotlin/com/adhdfocus/app/PACKAGE_STRUCTURE.md)
- [INSTALL.md](C:/Users/allen/kinspace-tablet-local/INSTALL.md)
- [KIOSK_DEPLOYMENT.md](C:/Users/allen/kinspace-tablet-local/KIOSK_DEPLOYMENT.md)

## Current Direction

This repo is intentionally separate from the cloud tablet app. The goal is a resilient family hub that:

- starts directly on the device
- keeps family data local
- offers caregiver reporting without cloud dependence
- can be deployed as a dedicated Android tablet experience
