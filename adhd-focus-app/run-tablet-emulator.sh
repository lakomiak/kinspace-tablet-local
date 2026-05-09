#!/bin/bash

# ADHD Focus App - 10" Tablet Emulator Runner
# This script sets up and runs the app in a 10" tablet emulator (1280x800 resolution)

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
EMULATOR_NAME="tablet_10inch"
AVD_NAME="tablet_10inch_avd"
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}"
EMULATOR_PATH="$ANDROID_SDK_ROOT/emulator/emulator"
ADB_PATH="$ANDROID_SDK_ROOT/platform-tools/adb"
GRADLE_WRAPPER="./gradlew"

# Tablet specifications (10.1" tablet - 1280x800)
TABLET_WIDTH=1280
TABLET_HEIGHT=800
TABLET_DPI=160

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}ADHD Focus App - Tablet Emulator Setup${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Function to check if Android SDK is installed
check_android_sdk() {
    if [ ! -d "$ANDROID_SDK_ROOT" ]; then
        echo -e "${RED}✗ Android SDK not found at $ANDROID_SDK_ROOT${NC}"
        echo "Please set ANDROID_SDK_ROOT environment variable or install Android SDK"
        exit 1
    fi
    echo -e "${GREEN}✓ Android SDK found${NC}"
}

# Function to check if emulator exists
check_emulator_exists() {
    if [ ! -f "$EMULATOR_PATH" ]; then
        echo -e "${RED}✗ Emulator not found at $EMULATOR_PATH${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Emulator found${NC}"
}

# Function to check if ADB is available
check_adb() {
    if [ ! -f "$ADB_PATH" ]; then
        echo -e "${RED}✗ ADB not found at $ADB_PATH${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ ADB found${NC}"
}

# Function to create AVD if it doesn't exist
create_avd_if_needed() {
    echo ""
    echo -e "${YELLOW}Checking for existing AVD: $AVD_NAME${NC}"
    
    # Check if AVD already exists
    if [ -d "$HOME/.android/avd/$AVD_NAME.avd" ]; then
        echo -e "${GREEN}✓ AVD already exists${NC}"
        return
    fi
    
    echo -e "${YELLOW}Creating new 10\" tablet AVD...${NC}"
    
    # Create AVD with tablet specifications
    echo "no" | "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/avdmanager" create avd \
        -n "$AVD_NAME" \
        -k "system-images;android-36;google_apis;x86_64" \
        -d "10.1in WXGA (Tablet)" \
        -f || {
        echo -e "${RED}✗ Failed to create AVD${NC}"
        echo "Trying alternative method..."
        
        # Alternative: use generic tablet profile
        echo "no" | "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/avdmanager" create avd \
            -n "$AVD_NAME" \
            -k "system-images;android-36;google_apis;x86_64" \
            -f || {
            echo -e "${RED}✗ Could not create AVD${NC}"
            exit 1
        }
    }
    
    echo -e "${GREEN}✓ AVD created successfully${NC}"
}

# Function to start emulator
start_emulator() {
    echo ""
    echo -e "${YELLOW}Starting 10\" tablet emulator...${NC}"
    
    # Check if emulator is already running
    if "$ADB_PATH" devices | grep -q "$EMULATOR_NAME"; then
        echo -e "${GREEN}✓ Emulator already running${NC}"
        return
    fi
    
    # Start emulator with tablet specifications
    "$EMULATOR_PATH" -avd "$AVD_NAME" \
        -skin "${TABLET_WIDTH}x${TABLET_HEIGHT}" \
        -dpi-device "$TABLET_DPI" \
        -memory 2048 \
        -cores 4 \
        -gpu on \
        -no-snapshot-load \
        -no-snapshot-save \
        -no-audio \
        &
    
    EMULATOR_PID=$!
    echo -e "${GREEN}✓ Emulator started (PID: $EMULATOR_PID)${NC}"
    
    # Wait for emulator to be ready
    echo -e "${YELLOW}Waiting for emulator to be ready...${NC}"
    timeout=0
    max_timeout=120
    
    while [ $timeout -lt $max_timeout ]; do
        if "$ADB_PATH" shell getprop sys.boot_completed 2>/dev/null | grep -q "1"; then
            echo -e "${GREEN}✓ Emulator is ready${NC}"
            return
        fi
        sleep 2
        timeout=$((timeout + 2))
        echo -n "."
    done
    
    echo ""
    echo -e "${RED}✗ Emulator failed to start within timeout${NC}"
    exit 1
}

# Function to build the app
build_app() {
    echo ""
    echo -e "${YELLOW}Building ADHD Focus App...${NC}"
    
    if [ ! -f "$GRADLE_WRAPPER" ]; then
        echo -e "${RED}✗ Gradle wrapper not found${NC}"
        exit 1
    fi
    
    chmod +x "$GRADLE_WRAPPER"
    
    "$GRADLE_WRAPPER" clean assembleDebug || {
        echo -e "${RED}✗ Build failed${NC}"
        exit 1
    }
    
    echo -e "${GREEN}✓ Build successful${NC}"
}

# Function to install and run app
install_and_run_app() {
    echo ""
    echo -e "${YELLOW}Installing app on emulator...${NC}"
    
    # Find the built APK
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
    
    if [ ! -f "$APK_PATH" ]; then
        echo -e "${RED}✗ APK not found at $APK_PATH${NC}"
        exit 1
    fi
    
    # Install APK
    "$ADB_PATH" install -r "$APK_PATH" || {
        echo -e "${RED}✗ Installation failed${NC}"
        exit 1
    }
    
    echo -e "${GREEN}✓ App installed successfully${NC}"
    
    # Launch the app
    echo ""
    echo -e "${YELLOW}Launching app...${NC}"
    
    "$ADB_PATH" shell am start -n "com.adhdfocus.app/.MainActivity" || {
        echo -e "${RED}✗ Failed to launch app${NC}"
        exit 1
    }
    
    echo -e "${GREEN}✓ App launched${NC}"
}

# Function to show device info
show_device_info() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}Emulator Information${NC}"
    echo -e "${BLUE}========================================${NC}"
    
    echo -e "${YELLOW}Device:${NC}"
    "$ADB_PATH" shell getprop ro.product.model
    
    echo -e "${YELLOW}Android Version:${NC}"
    "$ADB_PATH" shell getprop ro.build.version.release
    
    echo -e "${YELLOW}Screen Resolution:${NC}"
    echo "${TABLET_WIDTH}x${TABLET_HEIGHT} @ ${TABLET_DPI}dpi"
    
    echo -e "${YELLOW}Connected Devices:${NC}"
    "$ADB_PATH" devices
    
    echo ""
}

# Function to show usage
show_usage() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}Usage${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
    echo "The app is now running on the tablet emulator."
    echo ""
    echo -e "${YELLOW}Useful ADB Commands:${NC}"
    echo "  View logs:           adb logcat"
    echo "  Take screenshot:     adb shell screencap -p /sdcard/screenshot.png"
    echo "  Pull screenshot:     adb pull /sdcard/screenshot.png"
    echo "  Clear app data:      adb shell pm clear com.adhdfocus.app"
    echo "  Uninstall app:       adb uninstall com.adhdfocus.app"
    echo "  Stop emulator:       adb emu kill"
    echo ""
    echo -e "${YELLOW}Emulator Controls:${NC}"
    echo "  Rotate device:       Ctrl+F11 (or Cmd+F11 on Mac)"
    echo "  Volume up:           Keypad +"
    echo "  Volume down:         Keypad -"
    echo "  Power button:        F7"
    echo ""
}

# Main execution
main() {
    echo -e "${YELLOW}Step 1: Checking prerequisites...${NC}"
    check_android_sdk
    check_emulator_exists
    check_adb
    
    echo -e "${YELLOW}Step 2: Setting up AVD...${NC}"
    create_avd_if_needed
    
    echo -e "${YELLOW}Step 3: Starting emulator...${NC}"
    start_emulator
    
    echo -e "${YELLOW}Step 4: Building app...${NC}"
    build_app
    
    echo -e "${YELLOW}Step 5: Installing and running app...${NC}"
    install_and_run_app
    
    echo -e "${YELLOW}Step 6: Displaying device info...${NC}"
    show_device_info
    
    show_usage
    
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}✓ Setup complete! App is running.${NC}"
    echo -e "${GREEN}========================================${NC}"
}

# Run main function
main
