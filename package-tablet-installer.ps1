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
            Write-Host "Building legacy production release APK..."
            & .\gradlew.bat clean :adhd-focus-app:assembleLegacyRelease --no-daemon
            $apk = Join-Path $appDir "build\outputs\apk\legacy\release\adhd-focus-app-legacy-release.apk"
        }
        else {
            Write-Host "Building modern production release APK..."
            & .\gradlew.bat clean :adhd-focus-app:assembleModernRelease --no-daemon
            $apk = Join-Path $appDir "build\outputs\apk\modern\release\adhd-focus-app-modern-release.apk"
        }
        if (!(Test-Path $apk)) {
            throw "Release APK was not generated at $apk. Release signing may not be configured."
        }
        $target = Join-Path $distDir $(if ($Legacy) { "KinspaceTablet-legacy-release.apk" } else { "KinspaceTablet-release.apk" })
    }
    else {
        if ($Audit -and $Legacy) {
            Write-Host "Building legacy audit debug APK..."
            & .\gradlew.bat :adhd-focus-app:assembleLegacyAuditDebug --no-daemon
            $apk = Join-Path $appDir "build\outputs\apk\legacyAudit\debug\adhd-focus-app-legacy-audit-debug.apk"
        }
        elseif ($Audit) {
            Write-Host "Building modern audit debug APK..."
            & .\gradlew.bat :adhd-focus-app:assembleModernAuditDebug --no-daemon
            $apk = Join-Path $appDir "build\outputs\apk\modernAudit\debug\adhd-focus-app-modern-audit-debug.apk"
        }
        elseif ($Legacy) {
            Write-Host "Building legacy production debug APK..."
            & .\gradlew.bat :adhd-focus-app:assembleLegacyProductionDebug --no-daemon
            $apk = Join-Path $appDir "build\outputs\apk\legacyProduction\debug\adhd-focus-app-legacy-production-debug.apk"
        }
        else {
            Write-Host "Building modern production debug APK..."
            & .\gradlew.bat :adhd-focus-app:assembleModernProductionDebug --no-daemon
            $apk = Join-Path $appDir "build\outputs\apk\modernProduction\debug\adhd-focus-app-modern-production-debug.apk"
        }
        if (!(Test-Path $apk)) {
            throw "Debug APK was not generated at $apk."
        }
        $target = Join-Path $distDir $(
            if ($Audit -and $Legacy) { "KinspaceTablet-legacy-audit.apk" }
            elseif ($Audit) { "KinspaceTablet-audit.apk" }
            elseif ($Legacy) { "KinspaceTablet-legacy-debug.apk" }
            else { "KinspaceTablet-debug.apk" }
        )
    }

    Copy-Item -Force $apk $target
    Write-Host ""
    Write-Host "Packaged installer written to:"
    Write-Host $target
    Write-Host ""
    Write-Host "Install on a device with:"
    Write-Host "adb install --user 0 -r `"$target`""
}
finally {
    Pop-Location
}
