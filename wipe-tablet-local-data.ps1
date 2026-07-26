param(
    [string]$DeviceId,
    [int]$User = 0,
    [string]$PackageName,
    [switch]$Audit,
    [switch]$Launch,
    [switch]$WhatIf
)

$ErrorActionPreference = "Stop"

$defaultPackageName = if ($Audit) { "com.adhdfocus.app.audit" } else { "com.adhdfocus.app" }
$targetPackageName = if ([string]::IsNullOrWhiteSpace($PackageName)) { $defaultPackageName } else { $PackageName.Trim() }
$script:SelectedDeviceId = $null

function Invoke-Adb {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments,
        [switch]$AllowFailure
    )

    if ($WhatIf) {
        $prefix = if ($script:SelectedDeviceId) { "adb -s $script:SelectedDeviceId" } else { "adb" }
        Write-Host "[what-if] $prefix $($Arguments -join ' ')"
        return @()
    }

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

    if ($exitCode -ne 0 -and !$AllowFailure) {
        $message = ($output | Out-String).Trim()
        throw "adb $($Arguments -join ' ') failed (exit $exitCode).`n$message"
    }

    return $output
}

function Select-TargetDevice {
    if ($WhatIf) {
        $script:SelectedDeviceId = $DeviceId
        return
    }

    if (!(Get-Command adb -ErrorAction SilentlyContinue)) {
        throw "adb was not found. Install Android platform-tools and add adb to PATH."
    }

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

function Invoke-RunAsDataWipe {
    $credentialPath = "/data/user/$User/$targetPackageName"
    $devicePath = "/data/user_de/$User/$targetPackageName"
    $credentialTargets = "databases shared_prefs files cache code_cache no_backup"
    $deviceTargets = "$devicePath/shared_prefs $devicePath/files $devicePath/cache $devicePath/code_cache $devicePath/no_backup"

    Write-Host "Android blocked pm clear. Falling back to run-as sandbox wipe..."
    $wipeCommand = "cd $credentialPath && rm -rf $credentialTargets $deviceTargets"
    $wipeOutput = Invoke-Adb -Arguments @(
        "shell",
        "run-as",
        $targetPackageName,
        "sh",
        "-c",
        $wipeCommand
    ) -AllowFailure

    if ($WhatIf) {
        return
    }

    $databaseCheck = Invoke-Adb -Arguments @(
        "shell",
        "run-as",
        $targetPackageName,
        "sh",
        "-c",
        "cd $credentialPath && ls databases/adhdfocus_database databases/adhdfocus_database-wal databases/adhdfocus_database-shm databases/adhdfocus.db databases/adhdfocus.db-wal databases/adhdfocus.db-shm 2>&1"
    ) -AllowFailure
    $setupCheck = Invoke-Adb -Arguments @(
        "shell",
        "run-as",
        $targetPackageName,
        "sh",
        "-c",
        "ls $devicePath/shared_prefs/tablet_setup.xml 2>&1"
    ) -AllowFailure
    $databaseText = ($databaseCheck | Out-String).Trim()
    $setupText = ($setupCheck | Out-String).Trim()
    $databaseMissing = $databaseText -match "No such file" -or $databaseText -match "not found" -or [string]::IsNullOrWhiteSpace($databaseText)
    $setupMissing = $setupText -match "No such file" -or $setupText -match "not found"
    if (!$databaseMissing -or !$setupMissing) {
        $wipeText = ($wipeOutput | Out-String).Trim()
        throw "run-as fallback did not confirm the wipe. This usually means the installed APK is not debuggable, Android blocked run-as for this package, or device-protected storage could not be removed.`n$wipeText`nDatabase check: $databaseText`nSetup check: $setupText"
    }
}

function Invoke-AppDataWipeBroadcast {
    Write-Host "Requesting in-app local data wipe..."
    $wipeOutput = Invoke-Adb -Arguments @(
        "shell",
        "am",
        "broadcast",
        "--user",
        "$User",
        "-a",
        "com.adhdfocus.app.action.WIPE_LOCAL_DATA",
        "-n",
        "$targetPackageName/.LocalDataWipeReceiver"
    ) -AllowFailure

    if ($WhatIf) {
        return $true
    }

    $wipeText = ($wipeOutput | Out-String).Trim()
    if ($wipeText -match "KINSPACE_WIPE_COMPLETE") {
        return $true
    }

    Write-Host "In-app wipe was not available for this installed APK. Falling back to Android data clear..."
    if (![string]::IsNullOrWhiteSpace($wipeText)) {
        Write-Host $wipeText
    }
    return $false
}

Write-Host "Kinspace Tablet Local data wipe"
Write-Host "Package: $targetPackageName"
Write-Host "Android user: $User"
Write-Host ""

Select-TargetDevice
if ($script:SelectedDeviceId) {
    Write-Host "Using tablet: $script:SelectedDeviceId"
}

if (!$WhatIf) {
    $installed = (Invoke-Adb -Arguments @("shell", "cmd", "package", "list", "packages", "--user", "$User", $targetPackageName) | Out-String)
    if ($installed -notmatch [regex]::Escape($targetPackageName)) {
        throw "Package '$targetPackageName' is not installed for Android user $User."
    }
}

Write-Host "Stopping app..."
Invoke-Adb -Arguments @("shell", "am", "force-stop", "--user", "$User", $targetPackageName) -AllowFailure | Out-Null

$inAppWipeComplete = Invoke-AppDataWipeBroadcast
if (!$inAppWipeComplete) {
    Write-Host "Clearing app data..."
    $clearOutput = Invoke-Adb -Arguments @("shell", "pm", "clear", "--user", "$User", $targetPackageName) -AllowFailure
    if (!$WhatIf) {
        $clearText = ($clearOutput | Out-String).Trim()
        if ($clearText -notmatch "Success") {
            Invoke-RunAsDataWipe
        }
    }
}

Write-Host "Data wipe complete. The app will open like a fresh install."

if ($Launch) {
    Write-Host "Restarting app..."
    Invoke-Adb -Arguments @("shell", "am", "force-stop", "--user", "$User", $targetPackageName) -AllowFailure | Out-Null
    Start-Sleep -Seconds 1
    Invoke-Adb -Arguments @(
        "shell", "am", "start", "-S", "--user", "$User",
        "-a", "android.intent.action.MAIN",
        "-c", "android.intent.category.LAUNCHER",
        "-n", "$targetPackageName/.MainActivity"
    ) | Write-Host
}

Write-Host ""
Write-Host "Done."
