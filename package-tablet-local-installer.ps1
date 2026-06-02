param(
    [switch]$Release,
    [switch]$Legacy,
    [switch]$Audit
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
        if ($Audit) {
            throw "Audit builds are debug-only. Remove -Release when using -Audit."
        }
        if ($Legacy) {
            Write-Host "Building local legacy release APK..."
            & .\gradlew.bat clean :adhd-focus-app:assembleLegacyRelease --no-daemon
            $apk = Join-Path $appDir "build\outputs\apk\legacy\release\adhd-focus-app-legacy-release.apk"
            $target = Join-Path $distDir "KinspaceTabletLocal-legacy-release.apk"
        } else {
            Write-Host "Building local modern release APK..."
            & .\gradlew.bat clean :adhd-focus-app:assembleModernRelease --no-daemon
            $apk = Join-Path $appDir "build\outputs\apk\modern\release\adhd-focus-app-modern-release.apk"
            $target = Join-Path $distDir "KinspaceTabletLocal-release.apk"
        }
    } else {
        if ($Audit -and $Legacy) {
            Write-Host "Building local legacy audit APK..."
            & .\gradlew.bat :adhd-focus-app:assembleLegacyAuditDebug --no-daemon
            $apk = Join-Path $appDir "build\outputs\apk\legacyAudit\debug\adhd-focus-app-legacy-audit-debug.apk"
            $target = Join-Path $distDir "KinspaceTabletLocal-legacy-audit.apk"
        } elseif ($Audit) {
            Write-Host "Building local modern audit APK..."
            & .\gradlew.bat :adhd-focus-app:assembleModernAuditDebug --no-daemon
            $apk = Join-Path $appDir "build\outputs\apk\modernAudit\debug\adhd-focus-app-modern-audit-debug.apk"
            $target = Join-Path $distDir "KinspaceTabletLocal-audit.apk"
        } elseif ($Legacy) {
            Write-Host "Building local legacy debug APK..."
            & .\gradlew.bat :adhd-focus-app:assembleLegacyProductionDebug --no-daemon
            $apk = Join-Path $appDir "build\outputs\apk\legacyProduction\debug\adhd-focus-app-legacy-production-debug.apk"
            $target = Join-Path $distDir "KinspaceTabletLocal-legacy-debug.apk"
        } else {
            Write-Host "Building local modern debug APK..."
            & .\gradlew.bat :adhd-focus-app:assembleModernProductionDebug --no-daemon
            $apk = Join-Path $appDir "build\outputs\apk\modernProduction\debug\adhd-focus-app-modern-production-debug.apk"
            $target = Join-Path $distDir "KinspaceTabletLocal-debug.apk"
        }
    }

    if (!(Test-Path $apk)) {
        throw "APK was not generated at $apk"
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
