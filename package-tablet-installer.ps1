param(
    [switch]$Release,
    [switch]$Legacy
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
        if ($Legacy) {
            & .\gradlew.bat clean :adhd-focus-app:assembleLegacyRelease --no-daemon
            $apk = Join-Path $appDir "build\outputs\apk\legacy\release\adhd-focus-app-legacy-release.apk"
        }
        else {
            & .\gradlew.bat clean :adhd-focus-app:assembleModernRelease --no-daemon
            $apk = Join-Path $appDir "build\outputs\apk\modern\release\adhd-focus-app-modern-release.apk"
        }
        if (!(Test-Path $apk)) {
            throw "Release APK was not generated at $apk. Release signing may not be configured."
        }
        $target = Join-Path $distDir $(if ($Legacy) { "KinspaceTablet-legacy-release.apk" } else { "KinspaceTablet-release.apk" })
    }
    else {
        if ($Legacy) {
            Write-Host "Building legacy debug APK..."
            & .\gradlew.bat :adhd-focus-app:assembleLegacyDebug
            $apk = Join-Path $appDir "build\outputs\apk\legacy\debug\adhd-focus-app-legacy-debug.apk"
        }
        else {
            Write-Host "Building modern debug APK..."
            & .\gradlew.bat :adhd-focus-app:assembleModernDebug
            $apk = Join-Path $appDir "build\outputs\apk\modern\debug\adhd-focus-app-modern-debug.apk"
        }
        if (!(Test-Path $apk)) {
            throw "Debug APK was not generated at $apk."
        }
        $target = Join-Path $distDir $(if ($Legacy) { "KinspaceTablet-legacy-debug.apk" } else { "KinspaceTablet-debug.apk" })
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
