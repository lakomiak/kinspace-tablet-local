param(
    [switch]$Release
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$appDir = Join-Path $repoRoot "adhd-focus-app"
$distDir = Join-Path $repoRoot "dist"

if (!(Test-Path $distDir)) {
    New-Item -ItemType Directory -Path $distDir | Out-Null
}

Push-Location $repoRoot
try {
    if ($Release) {
        Write-Host "Building release APK..."
        & .\gradlew.bat clean :adhd-focus-app:assembleRelease --no-daemon
        $apk = Join-Path $appDir "build\outputs\apk\release\adhd-focus-app-release.apk"
        if (!(Test-Path $apk)) {
            throw "Release APK was not generated at $apk. Release signing may not be configured."
        }
        $target = Join-Path $distDir "KinspaceTablet-release.apk"
    }
    else {
        Write-Host "Building debug APK..."
        & .\gradlew.bat :adhd-focus-app:assembleDebug
        $apk = Join-Path $appDir "build\outputs\apk\debug\adhd-focus-app-debug.apk"
        if (!(Test-Path $apk)) {
            throw "Debug APK was not generated at $apk."
        }
        $target = Join-Path $distDir "KinspaceTablet-debug.apk"
    }

    Copy-Item -Force $apk $target
    Write-Host ""
    Write-Host "Packaged installer written to:"
    Write-Host $target
    Write-Host ""
    Write-Host "Install on a device with:"
    Write-Host "adb install -r `"$target`""
}
finally {
    Pop-Location
}
