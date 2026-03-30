# ADHD Focus App - 7" Tablet Emulator Runner (PowerShell)
# This script sets up and runs the app in a 7" tablet emulator (1024x600 resolution)

param(
    [switch]$SkipBuild = $false,
    [switch]$SkipInstall = $false,
    [switch]$LogcatFollow = $false
)

# Configuration
$EMULATOR_NAME = "tablet_7inch"
$AVD_NAME = "tablet_7inch_avd"
$ANDROID_SDK_ROOT = if ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { "$env:USERPROFILE\AppData\Local\Android\Sdk" }
$EMULATOR_PATH = "$ANDROID_SDK_ROOT\emulator\emulator.exe"
$ADB_PATH = "$ANDROID_SDK_ROOT\platform-tools\adb.exe"
$GRADLE_WRAPPER = ".\gradlew.bat"

# Tablet specifications (7" tablet - 1024x600)
$TABLET_WIDTH = 1024
$TABLET_HEIGHT = 600
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
        Write-ColorOutput "✗ Android SDK not found at $ANDROID_SDK_ROOT" $colors['Red']
        Write-ColorOutput "Please set ANDROID_SDK_ROOT environment variable or install Android SDK" $colors['Red']
        exit 1
    }
    Write-ColorOutput "✓ Android SDK found" $colors['Green']
    
    if (-not (Test-Path $EMULATOR_PATH)) {
        Write-ColorOutput "✗ Emulator not found at $EMULATOR_PATH" $colors['Red']
        exit 1
    }
    Write-ColorOutput "✓ Emulator found" $colors['Green']
    
    if (-not (Test-Path $ADB_PATH)) {
        Write-ColorOutput "✗ ADB not found at $ADB_PATH" $colors['Red']
        exit 1
    }
    Write-ColorOutput "✓ ADB found" $colors['Green']
}

function Create-AVD-IfNeeded {
    Write-ColorOutput "Step 2: Setting up AVD..." $colors['Yellow']
    
    $avdPath = "$env:USERPROFILE\.android\avd\$AVD_NAME.avd"
    
    if (Test-Path $avdPath) {
        Write-ColorOutput "✓ AVD already exists" $colors['Green']
        return
    }
    
    Write-ColorOutput "Creating new 7`` tablet AVD..." $colors['Yellow']
    
    $avdManagerPath = "$ANDROID_SDK_ROOT\cmdline-tools\latest\bin\avdmanager.bat"
    
    # Try to create with specific device profile
    $process = Start-Process -FilePath $avdManagerPath -ArgumentList @(
        "create", "avd",
        "-n", $AVD_NAME,
        "-k", "system-images;android;34;google_apis",
        "-d", "7in WSVGA",
        "-f"
    ) -NoNewWindow -PassThru -RedirectStandardInput ([System.IO.StreamWriter]::Null)
    
    $process.WaitForExit()
    
    if ($process.ExitCode -ne 0) {
        Write-ColorOutput "[WARNING] Failed to create AVD with specific device profile" $colors['Yellow']
        Write-ColorOutput "Trying alternative method..." $colors['Yellow']
        
        $process = Start-Process -FilePath $avdManagerPath -ArgumentList @(
            "create", "avd",
            "-n", $AVD_NAME,
            "-k", "system-images;android;34;google_apis",
            "-f"
        ) -NoNewWindow -PassThru -RedirectStandardInput ([System.IO.StreamWriter]::Null)
        
        $process.WaitForExit()
        
        if ($process.ExitCode -ne 0) {
            Write-ColorOutput "✗ Could not create AVD" $colors['Red']
            exit 1
        }
    }
    
    Write-ColorOutput "✓ AVD created successfully" $colors['Green']
}

function Start-Emulator {
    Write-ColorOutput "Step 3: Starting emulator..." $colors['Yellow']
    
    # Check if emulator is already running
    $devices = & $ADB_PATH devices
    if ($devices -match $EMULATOR_NAME) {
        Write-ColorOutput "✓ Emulator already running" $colors['Green']
        return
    }
    
    Write-ColorOutput "Launching emulator..." $colors['Yellow']
    
    $arguments = @(
        "-avd", $AVD_NAME,
        "-skin", "$TABLET_WIDTH`x$TABLET_HEIGHT",
        "-dpi-device", $TABLET_DPI,
        "-memory", "2048",
        "-cores", "4",
        "-gpu", "on",
        "-no-snapshot-load",
        "-no-snapshot-save",
        "-no-audio"
    )
    
    Start-Process -FilePath $EMULATOR_PATH -ArgumentList $arguments -NoNewWindow
    Write-ColorOutput "✓ Emulator started" $colors['Green']
    
    # Wait for emulator to be ready
    Write-ColorOutput "Waiting for emulator to be ready..." $colors['Yellow']
    $timeout = 0
    $maxTimeout = 120
    
    while ($timeout -lt $maxTimeout) {
        try {
            $bootCompleted = & $ADB_PATH shell getprop sys.boot_completed 2>$null
            if ($bootCompleted -match "1") {
                Write-ColorOutput "✓ Emulator is ready" $colors['Green']
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
    Write-ColorOutput "✗ Emulator failed to start within timeout" $colors['Red']
    exit 1
}

function Build-App {
    if ($SkipBuild) {
        Write-ColorOutput "Skipping build (--SkipBuild flag set)" $colors['Yellow']
        return
    }
    
    Write-ColorOutput "Step 4: Building app..." $colors['Yellow']
    
    if (-not (Test-Path $GRADLE_WRAPPER)) {
        Write-ColorOutput "✗ Gradle wrapper not found" $colors['Red']
        exit 1
    }
    
    & $GRADLE_WRAPPER clean assembleDebug
    if ($LASTEXITCODE -ne 0) {
        Write-ColorOutput "✗ Build failed" $colors['Red']
        exit 1
    }
    Write-ColorOutput "✓ Build successful" $colors['Green']
}

function Install-And-Run-App {
    if ($SkipInstall) {
        Write-ColorOutput "Skipping install (--SkipInstall flag set)" $colors['Yellow']
        return
    }
    
    Write-ColorOutput "Step 5: Installing and running app..." $colors['Yellow']
    
    $apkPath = "app\build\outputs\apk\debug\app-debug.apk"
    
    if (-not (Test-Path $apkPath)) {
        Write-ColorOutput "✗ APK not found at $apkPath" $colors['Red']
        exit 1
    }
    
    Write-ColorOutput "Installing app on emulator..." $colors['Yellow']
    & $ADB_PATH install -r $apkPath
    if ($LASTEXITCODE -ne 0) {
        Write-ColorOutput "✗ Installation failed" $colors['Red']
        exit 1
    }
    Write-ColorOutput "✓ App installed successfully" $colors['Green']
    
    Write-ColorOutput "Launching app..." $colors['Yellow']
    & $ADB_PATH shell am start -n "com.adhdfocus.app/.MainActivity"
    if ($LASTEXITCODE -ne 0) {
        Write-ColorOutput "✗ Failed to launch app" $colors['Red']
        exit 1
    }
    Write-ColorOutput "✓ App launched" $colors['Green']
}

function Show-Device-Info {
    Write-Header "Emulator Information"
    
    Write-ColorOutput "Device:" $colors['Yellow']
    & $ADB_PATH shell getprop ro.product.model
    
    Write-ColorOutput "Android Version:" $colors['Yellow']
    & $ADB_PATH shell getprop ro.build.version.release
    
    Write-ColorOutput "Screen Resolution:" $colors['Yellow']
    Write-Host "$TABLET_WIDTH`x$TABLET_HEIGHT @ $TABLET_DPI`dpi"
    
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
Write-ColorOutput "✓ App is running on the 7`` tablet emulator" $colors['Green']

Show-Logcat
