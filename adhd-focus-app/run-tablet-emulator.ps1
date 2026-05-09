# ADHD Focus App - 10" Tablet Emulator Runner (PowerShell)
# This script sets up and runs the app in a 10" tablet emulator (1280x800 resolution)

param(
    [switch]$SkipBuild = $false,
    [switch]$SkipInstall = $false,
    [switch]$LogcatFollow = $false
)

# Configuration
$EMULATOR_NAME = "tablet_10inch"
$AVD_NAME = "tablet_10inch_avd"
$ANDROID_SDK_ROOT = if ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { "$env:USERPROFILE\AppData\Local\Android\Sdk" }
$EMULATOR_PATH = "$ANDROID_SDK_ROOT\emulator\emulator.exe"
$ADB_PATH = "$ANDROID_SDK_ROOT\platform-tools\adb.exe"

# Get the script directory and parent directory
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$parentDir = Split-Path -Parent $scriptDir
$GRADLE_WRAPPER = "$parentDir\gradlew.bat"

# Tablet specifications (10.1 inch tablet - 1280x800)
$TABLET_WIDTH = 1280
$TABLET_HEIGHT = 800
$TABLET_DPI = 160

# Colors
$colors = @{
    'Green'  = [System.ConsoleColor]::Green
    'Red'    = [System.ConsoleColor]::Red
    'Yellow' = [System.ConsoleColor]::Yellow
    'Blue'   = [System.ConsoleColor]::Blue
    'Gray'   = [System.ConsoleColor]::Gray
}

function Write-ColorOutput {
    param(
        [string]$Message,
        [System.ConsoleColor]$Color = [System.ConsoleColor]::White
    )
    Write-Host $Message -ForegroundColor $Color
}

function Write-Header {
    param([string]$Title)
    Write-ColorOutput "========================================" $colors['Blue']
    Write-ColorOutput $Title $colors['Blue']
    Write-ColorOutput "========================================" $colors['Blue']
    Write-Host ""
}

function Check-Prerequisites {
    Write-ColorOutput "Step 1: Checking prerequisites..." $colors['Yellow']
    
    if (-not (Test-Path $ANDROID_SDK_ROOT)) {
        Write-ColorOutput "X Android SDK not found at $ANDROID_SDK_ROOT" $colors['Red']
        Write-ColorOutput "Please set ANDROID_SDK_ROOT environment variable or install Android SDK" $colors['Red']
        exit 1
    }
    Write-ColorOutput "OK Android SDK found" $colors['Green']
    
    if (-not (Test-Path $EMULATOR_PATH)) {
        Write-ColorOutput "X Emulator not found at $EMULATOR_PATH" $colors['Red']
        exit 1
    }
    Write-ColorOutput "OK Emulator found" $colors['Green']
    
    if (-not (Test-Path $ADB_PATH)) {
        Write-ColorOutput "X ADB not found at $ADB_PATH" $colors['Red']
        exit 1
    }
    Write-ColorOutput "OK ADB found" $colors['Green']
}

function Create-AVD-IfNeeded {
    Write-ColorOutput "Step 2: Setting up AVD..." $colors['Yellow']
    
    $avdPath = "$env:USERPROFILE\.android\avd\$AVD_NAME.avd"
    
    if (Test-Path $avdPath) {
        Write-ColorOutput "OK AVD already exists" $colors['Green']
        return
    }
    
    Write-ColorOutput "Creating new 10 inch tablet AVD..." $colors['Yellow']
    
    $avdManagerPath = "$ANDROID_SDK_ROOT\cmdline-tools\latest\bin\avdmanager.bat"
    
    if (-not (Test-Path $avdManagerPath)) {
        Write-ColorOutput "X AVD Manager not found at $avdManagerPath" $colors['Red']
        Write-ColorOutput "Please ensure Android SDK cmdline-tools are installed" $colors['Red']
        exit 1
    }
    
    # Create AVD using cmd to handle input properly
# Try Android 36 first (matches the existing tablet AVD image family)
$cmdScript = @"
@echo off
echo no | "$avdManagerPath" create avd -n "$AVD_NAME" -k "system-images;android-36;google_apis;x86_64" -d "10.1in WXGA (Tablet)" -f
"@
    
    $tempScript = [System.IO.Path]::GetTempFileName() -replace '\.tmp$', '.bat'
    Set-Content -Path $tempScript -Value $cmdScript
    
    $process = Start-Process -FilePath $tempScript -NoNewWindow -PassThru -Wait
    Remove-Item $tempScript -Force
    
    if ($process.ExitCode -ne 0) {
        Write-ColorOutput "[WARNING] Failed to create AVD with Android 36" $colors['Yellow']
        Write-ColorOutput "Trying with generic tablet profile..." $colors['Yellow']
        
        $cmdScript = @"
@echo off
echo no | "$avdManagerPath" create avd -n "$AVD_NAME" -k "system-images;android-36;google_apis;x86_64" -f
"@
        
        $tempScript = [System.IO.Path]::GetTempFileName() -replace '\.tmp$', '.bat'
        Set-Content -Path $tempScript -Value $cmdScript
        
        $process = Start-Process -FilePath $tempScript -NoNewWindow -PassThru -Wait
        Remove-Item $tempScript -Force
        
        if ($process.ExitCode -ne 0) {
            Write-ColorOutput "X Could not create AVD" $colors['Red']
            exit 1
        }
    }
    
    Write-ColorOutput "OK AVD created successfully" $colors['Green']
}

function Start-Emulator {
    Write-ColorOutput "Step 3: Starting emulator..." $colors['Yellow']
    
    # Check if emulator is already running
    $devices = & $ADB_PATH devices
    if ($devices -match $EMULATOR_NAME) {
        Write-ColorOutput "OK Emulator already running" $colors['Green']
        return
    }
    
    Write-ColorOutput "Launching emulator..." $colors['Yellow']
    
    $skinRes = "$TABLET_WIDTH" + "x" + "$TABLET_HEIGHT"
    $arguments = @(
        "-avd", $AVD_NAME,
        "-skin", $skinRes,
        "-dpi-device", $TABLET_DPI,
        "-memory", "2048",
        "-cores", "4",
        "-gpu", "on",
        "-read-only",
        "-no-snapshot-load",
        "-no-snapshot-save",
        "-no-audio"
    )
    
    Start-Process -FilePath $EMULATOR_PATH -ArgumentList $arguments -NoNewWindow
    Write-ColorOutput "OK Emulator started" $colors['Green']
    
    # Wait for emulator to be ready
    Write-ColorOutput "Waiting for emulator to be ready..." $colors['Yellow']
    $timeout = 0
    $maxTimeout = 120
    
    while ($timeout -lt $maxTimeout) {
        try {
            $bootCompleted = & $ADB_PATH shell getprop sys.boot_completed 2>$null
            if ($bootCompleted -match "1") {
                Write-ColorOutput "OK Emulator is ready" $colors['Green']
                return
            }
        }
        catch {
            # Emulator not ready yet
        }
        
        Start-Sleep -Seconds 2
        $timeout += 2
        Write-Host -NoNewline "."
    }
    
    Write-Host ""
    Write-ColorOutput "X Emulator failed to start within timeout" $colors['Red']
    exit 1
}

function Build-App {
    if ($SkipBuild) {
        Write-ColorOutput "Skipping build (SkipBuild flag set)" $colors['Yellow']
        return
    }
    
    Write-ColorOutput "Step 4: Building app..." $colors['Yellow']
    
    if (-not (Test-Path $GRADLE_WRAPPER)) {
        Write-ColorOutput "X Gradle wrapper not found at $GRADLE_WRAPPER" $colors['Red']
        Write-ColorOutput "Attempting to initialize gradle wrapper..." $colors['Yellow']
        
        # Try to use gradle from PATH if available
        $gradleCmd = Get-Command gradle -ErrorAction SilentlyContinue
        if ($gradleCmd) {
            Write-ColorOutput "Found gradle in PATH, using it to initialize wrapper..." $colors['Yellow']
            & gradle wrapper --gradle-version 8.5
            if ($LASTEXITCODE -ne 0) {
                Write-ColorOutput "X Failed to initialize gradle wrapper" $colors['Red']
                exit 1
            }
        } else {
            Write-ColorOutput "X Gradle not found in PATH and wrapper not initialized" $colors['Red']
            Write-ColorOutput "Please install Gradle or run: gradle wrapper --gradle-version 8.5" $colors['Red']
            exit 1
        }
    }
    
    & $GRADLE_WRAPPER -p $parentDir :adhd-focus-app:clean :adhd-focus-app:assembleDebug
    if ($LASTEXITCODE -ne 0) {
        Write-ColorOutput "X Build failed" $colors['Red']
        exit 1
    }
    Write-ColorOutput "OK Build successful" $colors['Green']
}

function Install-And-Run-App {
    if ($SkipInstall) {
        Write-ColorOutput "Skipping install (SkipInstall flag set)" $colors['Yellow']
        return
    }
    
    Write-ColorOutput "Step 5: Installing and running app..." $colors['Yellow']
    
    $apkPath = "$scriptDir\build\outputs\apk\debug\adhd-focus-app-debug.apk"
    
    if (-not (Test-Path $apkPath)) {
        Write-ColorOutput "X APK not found at $apkPath" $colors['Red']
        exit 1
    }
    
    Write-ColorOutput "Installing app on emulator..." $colors['Yellow']
    & $ADB_PATH install -r $apkPath
    if ($LASTEXITCODE -ne 0) {
        Write-ColorOutput "X Installation failed" $colors['Red']
        exit 1
    }
    Write-ColorOutput "OK App installed successfully" $colors['Green']
    
    # Wait for app to be fully installed and system to settle
    Write-ColorOutput "Waiting for system to settle..." $colors['Yellow']
    Start-Sleep -Seconds 3
    
    Write-ColorOutput "Launching app..." $colors['Yellow']
    $launchOutput = & $ADB_PATH shell am start -n "com.adhdfocus.app/.MainActivity" 2>&1
    
    if ($LASTEXITCODE -ne 0) {
        Write-ColorOutput "X Failed to launch app" $colors['Red']
        Write-ColorOutput "Launch output: $launchOutput" $colors['Red']
        exit 1
    }
    
    # Wait for app to actually start
    Write-ColorOutput "Waiting for app to start..." $colors['Yellow']
    Start-Sleep -Seconds 2
    
    # Verify app is running
    $runningApps = & $ADB_PATH shell "ps -A | grep com.adhdfocus.app" 2>&1
    if ($runningApps -match "com.adhdfocus.app") {
        Write-ColorOutput "OK App is running" $colors['Green']
    } else {
        Write-ColorOutput "[WARNING] Could not verify app is running, but launch command succeeded" $colors['Yellow']
    }
}

function Show-Device-Info {
    Write-Header "Emulator Information"
    
    Write-ColorOutput "Device:" $colors['Yellow']
    & $ADB_PATH shell getprop ro.product.model
    
    Write-ColorOutput "Android Version:" $colors['Yellow']
    & $ADB_PATH shell getprop ro.build.version.release
    
    Write-ColorOutput "Screen Resolution:" $colors['Yellow']
    Write-Host "$TABLET_WIDTH x $TABLET_HEIGHT @ $TABLET_DPI dpi"
    
    Write-ColorOutput "Connected Devices:" $colors['Yellow']
    & $ADB_PATH devices
}

function Show-Usage {
    Write-Header "Usage"
    
    Write-Host "The app is now running on the tablet emulator."
    Write-Host ""
    
    Write-ColorOutput "Useful ADB Commands:" $colors['Yellow']
    Write-Host "  View logs:           adb logcat"
    Write-Host "  Take screenshot:     adb shell screencap -p /sdcard/screenshot.png"
    Write-Host "  Pull screenshot:     adb pull /sdcard/screenshot.png"
    Write-Host "  Clear app data:      adb shell pm clear com.adhdfocus.app"
    Write-Host "  Uninstall app:       adb uninstall com.adhdfocus.app"
    Write-Host "  Stop emulator:       adb emu kill"
    Write-Host ""
    
    Write-ColorOutput "Emulator Controls:" $colors['Yellow']
    Write-Host "  Rotate device:       Ctrl+F11 (or Cmd+F11 on Mac)"
    Write-Host "  Volume up:           Keypad +"
    Write-Host "  Volume down:         Keypad -"
    Write-Host "  Power button:        F7"
    Write-Host ""
}

function Show-Logcat {
    if ($LogcatFollow) {
        Write-ColorOutput "Following logcat output (Ctrl+C to stop)..." $colors['Yellow']
        & $ADB_PATH logcat
    }
}

# Main execution
Write-Header "ADHD Focus App - Tablet Emulator Setup"

Check-Prerequisites
Create-AVD-IfNeeded
Start-Emulator
Build-App
Install-And-Run-App
Show-Device-Info
Show-Usage

Write-Header "Setup Complete"
Write-ColorOutput "OK App is running on the 10 inch tablet emulator" $colors['Green']

Show-Logcat
