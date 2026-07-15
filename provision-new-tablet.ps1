param(
    [string]$DeviceId,
    [string]$ApkPath,
    [switch]$SkipRebootVerification
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
if (!$ApkPath) {
    $besideScript = Join-Path $scriptDir "KinspaceTabletLocal-debug.apk"
    $inRepoDist = Join-Path $scriptDir "dist\KinspaceTabletLocal-debug.apk"
    $ApkPath = if (Test-Path -LiteralPath $besideScript) { $besideScript } else { $inRepoDist }
}
$apkPath = $ApkPath
$packageName = "com.adhdfocus.app"
$adminComponent = "$packageName/com.adhdfocus.app.admin.KinspaceDeviceAdminReceiver"
$script:SelectedDeviceId = $null

function Invoke-Adb {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        if ($script:SelectedDeviceId) {
            $output = & adb -s $script:SelectedDeviceId @Arguments 2>&1
        } else {
            $output = & adb @Arguments 2>&1
        }
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    if ($exitCode -ne 0) {
        $message = ($output | Out-String).Trim()
        throw "adb $($Arguments -join ' ') failed (exit $exitCode).`n$message"
    }
    return $output
}

function Select-TargetDevice {
    $lines = & adb devices 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Unable to run adb. Confirm Android platform-tools is installed and adb is on PATH."
    }

    $devices = @()
    foreach ($line in $lines) {
        if ($line -match '^([^\s]+)\s+(device|unauthorized|offline)$') {
            $devices += [PSCustomObject]@{
                Id = $Matches[1]
                State = $Matches[2]
            }
        }
    }

    if ($DeviceId) {
        $target = $devices | Where-Object { $_.Id -eq $DeviceId } | Select-Object -First 1
        if (!$target) {
            throw "Device '$DeviceId' is not connected. Run 'adb devices' to check the connection."
        }
    } else {
        if ($devices.Count -eq 0) {
            throw "No tablet detected. Connect it by USB and approve the USB debugging prompt."
        }
        if ($devices.Count -gt 1) {
            $ids = ($devices.Id -join ', ')
            throw "Multiple devices detected ($ids). Run this script again with -DeviceId <serial>."
        }
        $target = $devices[0]
    }

    if ($target.State -eq "unauthorized") {
        throw "Tablet '$($target.Id)' is unauthorized. Approve 'Allow USB debugging' on the tablet, then rerun the script."
    }
    if ($target.State -ne "device") {
        throw "Tablet '$($target.Id)' is $($target.State). Reconnect it and confirm 'adb devices' shows 'device'."
    }

    $script:SelectedDeviceId = $target.Id
}

function Assert-KioskLocked {
    $activityState = (Invoke-Adb -Arguments @("shell", "dumpsys", "activity") | Out-String)
    if ($activityState -notmatch 'mLockTaskModeState=LOCKED') {
        throw "Kinspace did not enter managed lock-task mode. The tablet is not yet locked down."
    }
    if ($activityState -notmatch 'u0:\[com\.adhdfocus\.app\]') {
        throw "Kinspace is not the only lock-task allowlisted package."
    }

    $activities = (Invoke-Adb -Arguments @("shell", "dumpsys", "activity", "activities") | Out-String)
    if ($activities -notmatch 'ResumedActivity:.*com\.adhdfocus\.app/\.MainActivity') {
        throw "Kinspace MainActivity is not the foreground activity."
    }
}

function Wait-ForBoot {
    $deadline = (Get-Date).AddMinutes(3)
    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        while ((Get-Date) -lt $deadline) {
            $state = & adb -s $script:SelectedDeviceId get-state 2>$null
            if ($LASTEXITCODE -eq 0 -and ($state | Out-String).Trim() -eq "device") {
                $bootComplete = & adb -s $script:SelectedDeviceId shell getprop dev.bootcomplete 2>$null
                if ($LASTEXITCODE -eq 0 -and ($bootComplete | Out-String).Trim() -eq "1") {
                    return
                }
            }
            Start-Sleep -Seconds 2
        }
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    throw "The tablet did not finish rebooting within 3 minutes."
}

if (!(Get-Command adb -ErrorAction SilentlyContinue)) {
    throw "adb was not found. Install Android platform-tools and add adb to PATH."
}
if (!(Test-Path -LiteralPath $apkPath)) {
    throw "Installer APK not found: $apkPath`nRepackage the installer or pass -ApkPath <path>."
}

Write-Host "Kinspace new-tablet provisioning"
Select-TargetDevice
Write-Host "Using tablet: $script:SelectedDeviceId"
Write-Host "Waiting for Android to finish booting..."
Wait-ForBoot

Write-Host "Installing Kinspace..."
Invoke-Adb -Arguments @("install", "--user", "0", "-r", $apkPath) | Write-Host

$owners = (Invoke-Adb -Arguments @("shell", "dpm", "list-owners") | Out-String)
if ($owners -match 'DeviceOwner') {
    if ($owners -notmatch [regex]::Escape($packageName)) {
        throw "This tablet already has a different device owner. Factory-reset it before provisioning Kinspace."
    }
    Write-Host "Kinspace is already the device owner."
} else {
    Write-Host "Setting Kinspace as device owner..."
    Invoke-Adb -Arguments @("shell", "dpm", "set-device-owner", $adminComponent) | Write-Host
}

Write-Host "Launching Kinspace and enabling kiosk mode..."
Invoke-Adb -Arguments @(
    "shell", "am", "start", "--user", "0",
    "-a", "android.intent.action.MAIN",
    "-c", "android.intent.category.LAUNCHER",
    "-n", "$packageName/.MainActivity"
) | Write-Host
Start-Sleep -Seconds 3
Assert-KioskLocked
Write-Host "Initial kiosk verification passed."

if (!$SkipRebootVerification) {
    Write-Host "Rebooting to verify kiosk persistence..."
    Invoke-Adb -Arguments @("reboot") | Out-Null
    Wait-ForBoot
    Start-Sleep -Seconds 5
    Assert-KioskLocked
    Write-Host "Post-reboot kiosk verification passed."
}

Write-Host ""
Write-Host "Kinspace provisioning completed successfully."
Write-Host "Device owner: $adminComponent"
Write-Host "Lock-task state: LOCKED"
