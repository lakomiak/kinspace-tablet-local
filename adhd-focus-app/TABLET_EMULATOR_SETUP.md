# ADHD Focus App - 7" Tablet Emulator Setup Guide

This guide explains how to run the ADHD Focus App in a 7" tablet emulator (1024x600 resolution).

## Prerequisites

Before running the emulator, ensure you have:

1. **Android SDK** installed with:
   - Android SDK Platform 34 (API level 34)
   - Google APIs system image for Android 34
   - Android Emulator
   - Android Platform Tools (ADB)

2. **Java Development Kit (JDK)** 17 or higher

3. **Gradle** (included via wrapper)

### Installation Steps

#### macOS/Linux

```bash
# Install Android SDK (if not already installed)
# Using Homebrew on macOS:
brew install android-sdk

# Or download from: https://developer.android.com/studio/install

# Set ANDROID_SDK_ROOT environment variable
export ANDROID_SDK_ROOT=$HOME/Android/Sdk

# Add to your shell profile (~/.zshrc, ~/.bash_profile, etc.)
echo 'export ANDROID_SDK_ROOT=$HOME/Android/Sdk' >> ~/.zshrc
```

#### Windows

```powershell
# Set ANDROID_SDK_ROOT environment variable
[Environment]::SetEnvironmentVariable("ANDROID_SDK_ROOT", "$env:USERPROFILE\AppData\Local\Android\Sdk", "User")

# Or manually:
# 1. Right-click "This PC" or "My Computer"
# 2. Click "Properties"
# 3. Click "Advanced system settings"
# 4. Click "Environment Variables"
# 5. Add new User variable: ANDROID_SDK_ROOT = C:\Users\[YourUsername]\AppData\Local\Android\Sdk
```

## Running the Emulator

### Option 1: Bash Script (macOS/Linux)

```bash
cd calendar-tablet-adhd/adhd-focus-app
chmod +x run-tablet-emulator.sh
./run-tablet-emulator.sh
```

### Option 2: Batch Script (Windows CMD)

```cmd
cd calendar-tablet-adhd\adhd-focus-app
run-tablet-emulator.bat
```

### Option 3: PowerShell Script (Windows PowerShell)

```powershell
cd calendar-tablet-adhd\adhd-focus-app
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
.\run-tablet-emulator.ps1
```

**PowerShell Options:**
- `-SkipBuild`: Skip the build step (use if already built)
- `-SkipInstall`: Skip the install step (use if already installed)
- `-LogcatFollow`: Follow logcat output after launch

Example:
```powershell
.\run-tablet-emulator.ps1 -SkipBuild -LogcatFollow
```

## What the Scripts Do

1. **Check Prerequisites**: Verifies Android SDK, emulator, and ADB are installed
2. **Create AVD**: Creates a 7" tablet Android Virtual Device (if not exists)
3. **Start Emulator**: Launches the emulator with tablet specifications
4. **Build App**: Compiles the app using Gradle
5. **Install App**: Installs the APK on the emulator
6. **Launch App**: Starts the app on the emulator
7. **Display Info**: Shows device information and usage tips

## Emulator Specifications

- **Screen Size**: 7 inches
- **Resolution**: 1024x600 pixels
- **DPI**: 160 (mdpi)
- **Memory**: 2048 MB
- **CPU Cores**: 4
- **GPU**: Enabled
- **Android Version**: 34 (API level 34)

## Useful ADB Commands

### View Logs
```bash
adb logcat
adb logcat | grep "adhdfocus"  # Filter for app logs
```

### Take Screenshots
```bash
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png
```

### App Management
```bash
adb shell pm clear com.adhdfocus.app          # Clear app data
adb uninstall com.adhdfocus.app               # Uninstall app
adb shell pm list packages | grep adhdfocus   # List app packages
```

### Device Control
```bash
adb shell input keyevent 26                   # Power button
adb shell input keyevent 24                   # Volume up
adb shell input keyevent 25                   # Volume down
adb shell input text "Hello"                  # Type text
adb shell input tap 512 300                   # Tap at coordinates
```

### Emulator Control
```bash
adb emu kill                                  # Stop emulator
adb emu avd name                              # Show AVD name
adb shell getprop ro.build.version.release    # Android version
```

## Emulator Controls

| Action | Key |
|--------|-----|
| Rotate device | Ctrl+F11 (Windows/Linux) or Cmd+F11 (Mac) |
| Volume up | Keypad + |
| Volume down | Keypad - |
| Power button | F7 |
| Home | Home key |
| Back | Esc |
| Menu | F2 |
| Minimize | Ctrl+M |

## Troubleshooting

### Emulator Won't Start

**Problem**: "Emulator failed to start within timeout"

**Solutions**:
1. Check if Android SDK is properly installed
2. Verify ANDROID_SDK_ROOT environment variable is set correctly
3. Ensure you have enough disk space (at least 5GB)
4. Try manually creating the AVD:
   ```bash
   $ANDROID_SDK_ROOT/cmdline-tools/latest/bin/avdmanager create avd \
     -n tablet_7inch_avd \
     -k "system-images;android;34;google_apis" \
     -d "7in WSVGA"
   ```

### Build Fails

**Problem**: "Build failed"

**Solutions**:
1. Clean build cache:
   ```bash
   ./gradlew clean
   ```
2. Check Java version (must be 17+):
   ```bash
   java -version
   ```
3. Check for missing dependencies:
   ```bash
   ./gradlew dependencies
   ```

### App Won't Install

**Problem**: "Installation failed"

**Solutions**:
1. Clear previous installation:
   ```bash
   adb uninstall com.adhdfocus.app
   ```
2. Check emulator storage:
   ```bash
   adb shell df /data
   ```
3. Verify APK exists:
   ```bash
   ls -la app/build/outputs/apk/debug/app-debug.apk
   ```

### App Crashes on Launch

**Problem**: App starts but immediately crashes

**Solutions**:
1. Check logcat for errors:
   ```bash
   adb logcat | grep "adhdfocus"
   ```
2. Clear app data:
   ```bash
   adb shell pm clear com.adhdfocus.app
   ```
3. Reinstall app:
   ```bash
   adb uninstall com.adhdfocus.app
   ./run-tablet-emulator.sh  # or .bat/.ps1
   ```

### Slow Emulator Performance

**Solutions**:
1. Enable GPU acceleration (already enabled in scripts)
2. Increase allocated memory:
   ```bash
   emulator -avd tablet_7inch_avd -memory 4096
   ```
3. Use hardware acceleration (KVM on Linux, HAXM on Windows)
4. Close other applications

## Performance Tips

1. **First Run**: Initial startup takes 2-3 minutes. Subsequent runs are faster.
2. **Snapshots**: Scripts disable snapshots for clean state. Enable for faster restarts:
   ```bash
   emulator -avd tablet_7inch_avd -snapshot-save
   ```
3. **Headless Mode**: Run without GUI for better performance:
   ```bash
   emulator -avd tablet_7inch_avd -no-window
   ```

## Testing on Emulator

### Run Unit Tests
```bash
./gradlew test
```

### Run Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

### Run Specific Test
```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.adhdfocus.app.ui.auth.SignInScreenIntegrationTest
```

## Accessing App Data

### View App Database
```bash
adb shell
cd /data/data/com.adhdfocus.app/databases
sqlite3 adhd_focus.db
```

### View Shared Preferences
```bash
adb shell
cat /data/data/com.adhdfocus.app/shared_prefs/user_preferences.xml
```

## Stopping the Emulator

### Graceful Shutdown
```bash
adb emu kill
```

### Force Quit
```bash
pkill emulator  # macOS/Linux
taskkill /IM emulator.exe  # Windows
```

## Advanced Configuration

### Create Custom AVD Profile

```bash
$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/avdmanager create avd \
  -n custom_tablet \
  -k "system-images;android;34;google_apis" \
  -c 512M \
  -d "7in WSVGA"
```

### Modify AVD Configuration

Edit `~/.android/avd/tablet_7inch_avd/config.ini`:

```ini
hw.device.name=7in WSVGA
hw.lcd.density=160
hw.lcd.height=600
hw.lcd.width=1024
hw.ram.size=2048
hw.cpu.cores=4
hw.gpu.enabled=yes
```

## Additional Resources

- [Android Emulator Documentation](https://developer.android.com/studio/run/emulator)
- [ADB Command Reference](https://developer.android.com/studio/command-line/adb)
- [Android Virtual Devices](https://developer.android.com/studio/run/managing-avds)
- [Jetpack Compose Testing](https://developer.android.com/jetpack/compose/testing)

## Support

For issues or questions:
1. Check the troubleshooting section above
2. Review logcat output: `adb logcat`
3. Check Android Studio's Logcat window
4. Consult Android documentation

---

**Last Updated**: March 2026
**App Version**: 1.0.0
**Target Android**: API 34 (Android 14)
