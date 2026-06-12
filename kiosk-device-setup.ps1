param(
    [string]$DeviceId,
    [switch]$Legacy,
    [switch]$Launch,
    [switch]$OpenHomeChooser,
    [switch]$OpenAppSettings,
    [switch]$ShowCommands
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$packageName = "com.adhdfocus.app"
$modernApk = Join-Path $repoRoot "dist\KinspaceTabletLocal-debug.apk"
$legacyApk = Join-Path $repoRoot "dist\KinspaceTabletLocal-legacy-debug.apk"
$apkPath = if ($Legacy) { $legacyApk } else { $modernApk }

function Get-AdbPrefix {
    param([string]$TargetDeviceId)
    if ([string]::IsNullOrWhiteSpace($TargetDeviceId)) {
        return @("adb")
    }
    return @("adb", "-s", $TargetDeviceId)
}

function Invoke-Adb {
    param(
        [string[]]$Args,
        [string]$TargetDeviceId
    )
    $prefix = Get-AdbPrefix -TargetDeviceId $TargetDeviceId
    & $prefix[0] $prefix[1..($prefix.Length - 1)] @Args
}

Write-Host "Kinspace Tablet Local kiosk helper"
Write-Host ""
Write-Host "Package: $packageName"
Write-Host "APK: $apkPath"
Write-Host ""

if (!(Test-Path $apkPath)) {
    throw "APK not found at $apkPath"
}

Write-Host "Connected devices:"
& adb devices
Write-Host ""

Write-Host "Installing APK..."
if ([string]::IsNullOrWhiteSpace($DeviceId)) {
    & adb install -r $apkPath
} else {
    & adb -s $DeviceId install -r $apkPath
}
Write-Host ""

if ($Launch) {
    Write-Host "Launching Kinspace Tablet Local..."
    if ([string]::IsNullOrWhiteSpace($DeviceId)) {
        & adb shell monkey -p $packageName -c android.intent.category.LAUNCHER 1
    } else {
        & adb -s $DeviceId shell monkey -p $packageName -c android.intent.category.LAUNCHER 1
    }
    Write-Host ""
}

if ($OpenHomeChooser) {
    Write-Host "Opening Home chooser..."
    if ([string]::IsNullOrWhiteSpace($DeviceId)) {
        & adb shell am start -a android.intent.action.MAIN -c android.intent.category.HOME
    } else {
        & adb -s $DeviceId shell am start -a android.intent.action.MAIN -c android.intent.category.HOME
    }
    Write-Host ""
}

if ($OpenAppSettings) {
    Write-Host "Opening app settings..."
    if ([string]::IsNullOrWhiteSpace($DeviceId)) {
        & adb shell am start -a android.settings.APPLICATION_DETAILS_SETTINGS -d "package:$packageName"
    } else {
        & adb -s $DeviceId shell am start -a android.settings.APPLICATION_DETAILS_SETTINGS -d "package:$packageName"
    }
    Write-Host ""
}

if ($ShowCommands) {
    Write-Host "Useful follow-up commands:"
    Write-Host ""
    Write-Host "Launch app:"
    Write-Host "adb shell monkey -p $packageName -c android.intent.category.LAUNCHER 1"
    Write-Host ""
    Write-Host "Open Home chooser:"
    Write-Host "adb shell am start -a android.intent.action.MAIN -c android.intent.category.HOME"
    Write-Host ""
    Write-Host "Open app settings:"
    Write-Host "adb shell am start -a android.settings.APPLICATION_DETAILS_SETTINGS -d package:$packageName"
    Write-Host ""
    Write-Host "Set device owner on a freshly reset device:"
    Write-Host "adb shell dpm set-device-owner $packageName/com.adhdfocus.app.admin.KinspaceDeviceAdminReceiver"
    Write-Host ""
    Write-Host "Remove active admin on a test device:"
    Write-Host "adb shell dpm remove-active-admin $packageName/com.adhdfocus.app.admin.KinspaceDeviceAdminReceiver"
    Write-Host ""
    Write-Host "Use KIOSK_DEPLOYMENT.md as the main deployment reference."
}
