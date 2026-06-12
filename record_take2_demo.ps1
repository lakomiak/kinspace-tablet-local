$ErrorActionPreference = "Stop"

$repoRoot = "C:\Users\allen\kinspace-tablet-local"
$adb = "adb"
$packageName = "com.adhdfocus.app"
$activityName = "com.adhdfocus.app/.MainActivity"
$deviceDataRoot = "/data/data/com.adhdfocus.app"
$deviceDbPath = "$deviceDataRoot/databases/adhdfocus_database"
$devicePrefsPath = "$deviceDataRoot/shared_prefs/tablet_setup.xml"
$demoDb = Join-Path $repoRoot "adhdfocus_database_copy.db"
$jimmyPrefs = Join-Path $repoRoot "jimmy_tablet_setup.xml"
$sallyPrefs = Join-Path $repoRoot "sally_tablet_setup.xml"
$videoToolsDir = Join-Path $repoRoot "video-tools"
$segmentsDir = Join-Path $repoRoot "demo-segments"
$outputVideo = Join-Path $repoRoot "tablet_local_demo_take2.mp4"

function Invoke-Adb {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Args
    )

    & $adb @Args
}

function Wait-Short([int]$Milliseconds) {
    Start-Sleep -Milliseconds $Milliseconds
}

function Ensure-Files {
    New-Item -ItemType Directory -Force -Path $segmentsDir | Out-Null

    if (-not (Test-Path $videoToolsDir)) {
        throw "Expected video tools directory at $videoToolsDir"
    }

    if (-not (Test-Path $demoDb)) {
        throw "Missing seeded demo DB at $demoDb"
    }

    @"
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <string name="household_name">Kinspace</string>
    <string name="assigned_member_id">b96fc44e-ebd0-48b5-b7bc-933b93f8f17f</string>
    <string name="current_focus_date">2026-06-04</string>
    <string name="household_id">local-687e54a3-c978-4df7-bcc3-4ad88fd424d1</string>
    <string name="assigned_member_name">Jimmy</string>
</map>
"@ | Set-Content -Path $jimmyPrefs -Encoding UTF8

    @"
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <string name="household_name">Kinspace</string>
    <string name="assigned_member_id">member-sally</string>
    <string name="current_focus_date">2026-06-04</string>
    <string name="household_id">local-687e54a3-c978-4df7-bcc3-4ad88fd424d1</string>
    <string name="assigned_member_name">Sally</string>
</map>
"@ | Set-Content -Path $sallyPrefs -Encoding UTF8
}

function Start-App {
    Invoke-Adb @("shell", "am", "start", "-n", $activityName) | Out-Null
    Wait-Short 1400
}

function Force-Stop-App {
    Invoke-Adb @("shell", "am", "force-stop", $packageName) | Out-Null
    Wait-Short 600
}

function Clear-App {
    Invoke-Adb @("shell", "pm", "clear", $packageName) | Out-Null
    Wait-Short 900
}

function Restore-SeededState {
    param(
        [Parameter(Mandatory = $true)]
        [string]$PrefsFile
    )

    Force-Stop-App
    Invoke-Adb @("root") | Out-Null
    Wait-Short 500

    Invoke-Adb @("push", $demoDb, "/sdcard/adhdfocus_database_copy.db") | Out-Null
    Invoke-Adb @("push", $PrefsFile, "/sdcard/tablet_setup.xml") | Out-Null

    Invoke-Adb @("shell", "su", "0", "mkdir", "-p", "$deviceDataRoot/databases") | Out-Null
    Invoke-Adb @("shell", "su", "0", "mkdir", "-p", "$deviceDataRoot/shared_prefs") | Out-Null
    Invoke-Adb @("shell", "su", "0", "cp", "/sdcard/adhdfocus_database_copy.db", $deviceDbPath) | Out-Null
    Invoke-Adb @("shell", "su", "0", "cp", "/sdcard/tablet_setup.xml", $devicePrefsPath) | Out-Null
    Invoke-Adb @("shell", "su", "0", "chown", "u0_a219:u0_a219", $deviceDbPath) | Out-Null
    Invoke-Adb @("shell", "su", "0", "chown", "u0_a219:u0_a219", $devicePrefsPath) | Out-Null
    Invoke-Adb @("shell", "su", "0", "chmod", "660", $deviceDbPath) | Out-Null
    Invoke-Adb @("shell", "su", "0", "chmod", "660", $devicePrefsPath) | Out-Null

    Start-App
}

function Start-ScreenRecord {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RemoteName,
        [Parameter(Mandatory = $true)]
        [int]$Seconds
    )

    $args = @(
        "shell",
        "screenrecord",
        "/sdcard/$RemoteName",
        "--time-limit",
        "$Seconds"
    )
    return Start-Process -FilePath $adb -ArgumentList $args -PassThru -WindowStyle Hidden
}

function Pull-Segment {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RemoteName,
        [Parameter(Mandatory = $true)]
        [string]$LocalName
    )

    $localPath = Join-Path $segmentsDir $LocalName
    Invoke-Adb @("pull", "/sdcard/$RemoteName", $localPath) | Out-Null
    return $localPath
}

function Get-XmlNodeCenter {
    param(
        [Parameter(Mandatory = $true)]
        [string]$XmlText,
        [Parameter(Mandatory = $true)]
        [string]$Text
    )

    $pattern = 'text="' + [Regex]::Escape($Text) + '".+?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'
    $match = [Regex]::Match($XmlText, $pattern)
    if (-not $match.Success) {
        return $null
    }

    $x1 = [int]$match.Groups[1].Value
    $y1 = [int]$match.Groups[2].Value
    $x2 = [int]$match.Groups[3].Value
    $y2 = [int]$match.Groups[4].Value
    return [pscustomobject]@{
        X = [int](($x1 + $x2) / 2)
        Y = [int](($y1 + $y2) / 2)
    }
}

function Record-OnboardingSegment {
    Clear-App
    Start-App

    $proc = Start-ScreenRecord -RemoteName "segment_onboarding.mp4" -Seconds 9
    Wait-Short 700

    Invoke-Adb @("shell", "input", "tap", "160", "248") | Out-Null
    Wait-Short 250
    Invoke-Adb @("shell", "input", "text", "Kinspace") | Out-Null
    Wait-Short 300
    Invoke-Adb @("shell", "input", "tap", "160", "378") | Out-Null
    Wait-Short 250
    Invoke-Adb @("shell", "input", "text", "Jimmy") | Out-Null
    Wait-Short 250
    Invoke-Adb @("shell", "input", "tap", "160", "454") | Out-Null
    Wait-Short 250
    Invoke-Adb @("shell", "input", "text", "2015-06-15") | Out-Null
    Wait-Short 250
    Invoke-Adb @("shell", "input", "tap", "160", "522") | Out-Null
    Wait-Short 300
    Invoke-Adb @("shell", "input", "tap", "160", "378") | Out-Null
    Wait-Short 250
    Invoke-Adb @("shell", "input", "text", "Sally") | Out-Null
    Wait-Short 250
    Invoke-Adb @("shell", "input", "tap", "160", "454") | Out-Null
    Wait-Short 250
    Invoke-Adb @("shell", "input", "text", "2013-04-20") | Out-Null
    Wait-Short 250
    Invoke-Adb @("shell", "input", "tap", "160", "522") | Out-Null
    Wait-Short 350
    Invoke-Adb @("shell", "input", "tap", "160", "566") | Out-Null

    Wait-Process -InputObject $proc
    return Pull-Segment -RemoteName "segment_onboarding.mp4" -LocalName "segment_onboarding.mp4"
}

function Record-CreateTodoSegment {
    Restore-SeededState -PrefsFile $jimmyPrefs

    $proc = Start-ScreenRecord -RemoteName "segment_create_jimmy.mp4" -Seconds 5
    Wait-Short 700
    Invoke-Adb @("shell", "input", "tap", "276", "492") | Out-Null
    Wait-Short 900
    Invoke-Adb @("shell", "input", "tap", "170", "230") | Out-Null
    Wait-Short 400
    Invoke-Adb @("shell", "input", "text", "Read2") | Out-Null

    Wait-Process -InputObject $proc
    return Pull-Segment -RemoteName "segment_create_jimmy.mp4" -LocalName "segment_create_jimmy.mp4"
}

function Record-JimmyTimerAchievementsSegment {
    Restore-SeededState -PrefsFile $jimmyPrefs

    $proc = Start-ScreenRecord -RemoteName "segment_jimmy_timer.mp4" -Seconds 8
    Wait-Short 700
    Invoke-Adb @("shell", "input", "tap", "248", "399") | Out-Null
    Wait-Short 1300
    Invoke-Adb @("shell", "input", "tap", "225", "355") | Out-Null
    Wait-Short 1200
    Invoke-Adb @("shell", "input", "tap", "160", "580") | Out-Null
    Wait-Short 1400
    Invoke-Adb @("shell", "input", "tap", "44", "80") | Out-Null
    Wait-Short 1000
    Invoke-Adb @("shell", "input", "tap", "196", "80") | Out-Null

    Wait-Process -InputObject $proc
    return Pull-Segment -RemoteName "segment_jimmy_timer.mp4" -LocalName "segment_jimmy_timer.mp4"
}

function Record-SallyCompleteSegment {
    $proc = Start-ScreenRecord -RemoteName "segment_sally_complete.mp4" -Seconds 6
    Wait-Short 600
    Invoke-Adb @("shell", "input", "tap", "160", "410") | Out-Null
    Wait-Short 1200
    Invoke-Adb @("shell", "input", "tap", "46", "384") | Out-Null

    Wait-Process -InputObject $proc
    return Pull-Segment -RemoteName "segment_sally_complete.mp4" -LocalName "segment_sally_complete.mp4"
}

function Record-ReportsSegment {
    Restore-SeededState -PrefsFile $sallyPrefs

    Invoke-Adb @("shell", "input", "tap", "270", "585") | Out-Null
    Wait-Short 1800
    1..12 | ForEach-Object {
        Invoke-Adb @("shell", "input", "swipe", "310", "500", "310", "120", "300") | Out-Null
        Wait-Short 300
    }
    Wait-Short 900

    $proc = Start-ScreenRecord -RemoteName "segment_reports.mp4" -Seconds 5
    Wait-Short 700
    Invoke-Adb @("shell", "input", "tap", "160", "332") | Out-Null

    Wait-Process -InputObject $proc
    return Pull-Segment -RemoteName "segment_reports.mp4" -LocalName "segment_reports.mp4"
}

function Get-FfmpegPath {
    $ffmpegPath = & node -p "require('ffmpeg-static') || ''" 2>$null
    if (-not $ffmpegPath) {
        throw "ffmpeg-static path could not be resolved."
    }
    return $ffmpegPath.Trim()
}

function Stitch-Segments {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Segments
    )

    $concatFile = Join-Path $segmentsDir "concat.txt"
    $concatLines = $Segments | ForEach-Object {
        $escaped = $_.Replace("'", "''")
        "file '$escaped'"
    }
    Set-Content -Path $concatFile -Value $concatLines -Encoding ASCII

    $ffmpeg = Get-FfmpegPath
    & $ffmpeg -y -f concat -safe 0 -i $concatFile -c copy $outputVideo
}

Ensure-Files

$segments = @()
$segments += Record-OnboardingSegment
$segments += Record-CreateTodoSegment
$segments += Record-JimmyTimerAchievementsSegment
$segments += Record-SallyCompleteSegment
$segments += Record-ReportsSegment

Stitch-Segments -Segments $segments

Write-Host "Created demo video at $outputVideo"
