# Kinspace Tablet Local - Package Structure

## Overview

This fork is a local-only Android tablet app for household organization, ADHD-friendly routines, timers, achievements, and caregiver reporting. It does not depend on cloud authentication or sync.

The app is designed around:

- local household setup on the device
- local family member management
- local task, timer, and completion history
- local reports and success-pattern insights
- local backup/export
- dedicated-device / kiosk-friendly behavior

## Current Architecture

The codebase uses a practical layered structure:

- `data/` for Room entities, DAOs, backup helpers, and repositories
- `domain/` for business logic and device behavior
- `ui/` for Compose screens and view models
- `di/` for Hilt wiring

## Package Map

```text
com.adhdfocus.app/
|- MainActivity.kt                      # Local app entry point and navigation host
|- KioskBootReceiver.kt                 # Relaunches the app after boot/package replace
|
|- di/
|  \- AppModule.kt                     # Room, repositories, managers, and feature providers
|
|- data/
|  |- dao/                             # Room queries
|  |- database/
|  |  |- AdhdfocusDatabase.kt
|  |  |- Converters.kt
|  |  |- DatabaseInitializer.kt
|  |  \- DatabaseBackupManager.kt
|  |- model/                           # Local entities and enums
|  \- repository/
|
|- domain/
|  |- affirmation/
|  |- audio/                           # Reminder and timer sound playback
|  |- completion/                      # Per-day completion persistence
|  |- gamification/                    # Badges, efficiency, achievements
|  |- notification/
|  |- persistence/                     # Local cleanup and task persistence helpers
|  |- progress/
|  |- puzzle/
|  |- reminder/                        # Category reminder scheduling + receivers
|  |- serialization/
|  |- setup/                           # Tablet household/member setup state
|  |- streak/
|  |- sync/                            # Local status enums/types still referenced by task state
|  |- task/
|  |- theme/
|  |- timer/
|  |- userswitching/
|  \- visibility/
|
|- ui/
|  |- achievements/
|  |- common/
|  |- family/                          # Family management and member preferences
|  |- focus/                           # Main daily view and create/edit todo flows
|  |- reports/                         # Per-person reporting and breakdowns
|  |- settings/                        # Tablet and member settings
|  |- setup/                           # Local first-run setup + member selection
|  |- theme/
|  \- timer/
|
\- util/                               # Small helpers and shared utility functions
```

## Important Local-Only Flows

### 1. First-Run Setup

- `LocalSetupScreen`
- `LocalSetupViewModel`
- `TabletSetupManager`

This flow creates the household and initial member records locally on the device.

### 2. Daily Home Flow

- `DailyFocusViewScreen`
- `FocusViewModel`
- `TaskManager`
- `TaskRepository`

This is the core "today" experience for the currently assigned member.

### 3. Timer + Session Tracking

- `TimerScreen`
- `TimerViewModel`
- `TaskSessionMetric`

This is where we capture:

- active time
- paused time
- pause count
- reset count
- completed before/after timer end
- canceled before/after timer end
- stopped before timer end

### 4. Reports

- `ReportsScreen`
- `ReportsViewModel`

Reports are person-specific and now focus on:

- completed todos
- streaks
- average completion time
- paused percentage
- reset/cancel/stopped-early patterns
- completion-after-time-ended rate
- category completion breakdown
- timer outcome breakdown
- recent 7-day trends
- suggested next moves based on local history

### 5. Local Backup

- `DatabaseBackupManager`
- `SettingsScreen`
- `SettingsViewModel`

This flow creates exportable local backup packages that can be used to move household data to another device without cloud sync.

### 6. Dedicated Device Behavior

- `MainActivity`
- `KioskBootReceiver`
- manifest HOME intent filter

This is the foundation for a family-hub device experience where the app is the primary surface on the tablet.

## Notes About `domain/sync`

This local fork no longer performs cloud synchronization.

The `domain/sync` package only remains for lightweight local status/shared types that the current app still uses, such as sync-state enums and queue models referenced by existing task state. It is no longer a network sync layer.

## What Was Removed From This Fork

Compared with the cloud tablet app, this fork no longer includes active:

- cloud auth flows
- Retrofit service wiring
- AppAuth/Cognito sign-in
- REST sync clients
- websocket/realtime update handling
- cloud custom-group sync

## Build Outputs

Current debug variants:

- `modernDebug`
- `legacyDebug`

Packaged installers are written into:

- `dist/KinspaceTabletLocal-debug.apk`
- `dist/KinspaceTabletLocal-legacy-debug.apk`

## Recommended Next Improvements

- add local restore/import flow for backups
- add device-owner / lock-task deployment guidance
- add printable/exportable caregiver summaries
- add richer charts inside reports
