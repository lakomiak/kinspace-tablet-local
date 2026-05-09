@echo off
REM ADHD Focus App - 10" Tablet Emulator Runner (Windows)
REM This script sets up and runs the app in a 10" tablet emulator (1280x800 resolution)

setlocal enabledelayedexpansion

REM Configuration
set EMULATOR_NAME=tablet_10inch
set AVD_NAME=tablet_10inch_avd
if not defined ANDROID_SDK_ROOT (
    set ANDROID_SDK_ROOT=%USERPROFILE%\AppData\Local\Android\Sdk
)
set EMULATOR_PATH=%ANDROID_SDK_ROOT%\emulator\emulator.exe
set ADB_PATH=%ANDROID_SDK_ROOT%\platform-tools\adb.exe
set GRADLE_WRAPPER=gradlew.bat

REM Tablet specifications (10.1" tablet - 1280x800)
set TABLET_WIDTH=1280
set TABLET_HEIGHT=800
set TABLET_DPI=160

echo.
echo ========================================
echo ADHD Focus App - Tablet Emulator Setup
echo ========================================
echo.

REM Check if Android SDK is installed
if not exist "%ANDROID_SDK_ROOT%" (
    echo [ERROR] Android SDK not found at %ANDROID_SDK_ROOT%
    echo Please set ANDROID_SDK_ROOT environment variable or install Android SDK
    exit /b 1
)
echo [OK] Android SDK found

REM Check if emulator exists
if not exist "%EMULATOR_PATH%" (
    echo [ERROR] Emulator not found at %EMULATOR_PATH%
    exit /b 1
)
echo [OK] Emulator found

REM Check if ADB is available
if not exist "%ADB_PATH%" (
    echo [ERROR] ADB not found at %ADB_PATH%
    exit /b 1
)
echo [OK] ADB found

REM Check if AVD exists
echo.
echo Checking for existing AVD: %AVD_NAME%
if exist "%USERPROFILE%\.android\avd\%AVD_NAME%.avd" (
    echo [OK] AVD already exists
) else (
    echo Creating new 10" tablet AVD...
    echo no | "%ANDROID_SDK_ROOT%\cmdline-tools\latest\bin\avdmanager.bat" create avd ^
        -n "%AVD_NAME%" ^
        -k "system-images;android-36;google_apis;x86_64" ^
        -d "10.1in WXGA (Tablet)" ^
        -f
    
    if errorlevel 1 (
        echo [WARNING] Failed to create AVD with specific device profile
        echo Trying alternative method...
        echo no | "%ANDROID_SDK_ROOT%\cmdline-tools\latest\bin\avdmanager.bat" create avd ^
            -n "%AVD_NAME%" ^
            -k "system-images;android-36;google_apis;x86_64" ^
            -f
        
        if errorlevel 1 (
            echo [ERROR] Could not create AVD
            exit /b 1
        )
    )
    echo [OK] AVD created successfully
)

REM Start emulator
echo.
echo Starting 10" tablet emulator...

REM Check if emulator is already running
"%ADB_PATH%" devices | find "%EMULATOR_NAME%" >nul
if not errorlevel 1 (
    echo [OK] Emulator already running
) else (
    echo Launching emulator...
    start "" "%EMULATOR_PATH%" -avd "%AVD_NAME%" ^
        -skin "%TABLET_WIDTH%x%TABLET_HEIGHT%" ^
        -dpi-device "%TABLET_DPI%" ^
        -memory 2048 ^
        -cores 4 ^
        -gpu on ^
        -no-snapshot-load ^
        -no-snapshot-save ^
        -no-audio
    
    echo [OK] Emulator started
    
    REM Wait for emulator to be ready
    echo Waiting for emulator to be ready...
    set timeout=0
    set max_timeout=120
    
    :wait_loop
    if !timeout! geq !max_timeout! (
        echo [ERROR] Emulator failed to start within timeout
        exit /b 1
    )
    
    "%ADB_PATH%" shell getprop sys.boot_completed 2>nul | find "1" >nul
    if errorlevel 1 (
        timeout /t 2 /nobreak
        set /a timeout=!timeout!+2
        goto wait_loop
    )
    
    echo [OK] Emulator is ready
)

REM Build the app
echo.
echo Building ADHD Focus App...

if not exist "%GRADLE_WRAPPER%" (
    echo [ERROR] Gradle wrapper not found
    exit /b 1
)

call "%GRADLE_WRAPPER%" clean assembleDebug
if errorlevel 1 (
    echo [ERROR] Build failed
    exit /b 1
)
echo [OK] Build successful

REM Install and run app
echo.
echo Installing app on emulator...

set APK_PATH=app\build\outputs\apk\debug\app-debug.apk

if not exist "%APK_PATH%" (
    echo [ERROR] APK not found at %APK_PATH%
    exit /b 1
)

"%ADB_PATH%" install -r "%APK_PATH%"
if errorlevel 1 (
    echo [ERROR] Installation failed
    exit /b 1
)
echo [OK] App installed successfully

REM Launch the app
echo.
echo Launching app...

"%ADB_PATH%" shell am start -n "com.adhdfocus.app/.MainActivity"
if errorlevel 1 (
    echo [ERROR] Failed to launch app
    exit /b 1
)
echo [OK] App launched

REM Show device info
echo.
echo ========================================
echo Emulator Information
echo ========================================
echo.

echo Device:
"%ADB_PATH%" shell getprop ro.product.model

echo.
echo Android Version:
"%ADB_PATH%" shell getprop ro.build.version.release

echo.
echo Screen Resolution:
echo %TABLET_WIDTH%x%TABLET_HEIGHT% @ %TABLET_DPI%dpi

echo.
echo Connected Devices:
"%ADB_PATH%" devices

REM Show usage
echo.
echo ========================================
echo Usage
echo ========================================
echo.
echo The app is now running on the tablet emulator.
echo.
echo Useful ADB Commands:
echo   View logs:           adb logcat
echo   Take screenshot:     adb shell screencap -p /sdcard/screenshot.png
echo   Pull screenshot:     adb pull /sdcard/screenshot.png
echo   Clear app data:      adb shell pm clear com.adhdfocus.app
echo   Uninstall app:       adb uninstall com.adhdfocus.app
echo   Stop emulator:       adb emu kill
echo.
echo Emulator Controls:
echo   Rotate device:       Ctrl+F11 (or Cmd+F11 on Mac)
echo   Volume up:           Keypad +
echo   Volume down:         Keypad -
echo   Power button:        F7
echo.

echo ========================================
echo [OK] Setup complete! App is running.
echo ========================================
echo.

endlocal
