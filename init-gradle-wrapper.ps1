# Initialize Gradle Wrapper
# This script downloads and sets up the Gradle wrapper for the project

$GRADLE_VERSION = "8.5"
$GRADLE_URL = "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
$TEMP_DIR = [System.IO.Path]::GetTempPath()
$ZIP_FILE = Join-Path $TEMP_DIR "gradle-$GRADLE_VERSION-bin.zip"
$EXTRACT_DIR = Join-Path $TEMP_DIR "gradle-extract"

Write-Host "Downloading Gradle $GRADLE_VERSION..."
$ProgressPreference = 'SilentlyContinue'
Invoke-WebRequest -Uri $GRADLE_URL -OutFile $ZIP_FILE

Write-Host "Extracting Gradle..."
Expand-Archive -Path $ZIP_FILE -DestinationPath $EXTRACT_DIR -Force

Write-Host "Setting up wrapper JAR..."
$sourceJar = Join-Path -Path $EXTRACT_DIR -ChildPath "gradle-$GRADLE_VERSION" | Join-Path -ChildPath "lib" | Join-Path -ChildPath "gradle-$GRADLE_VERSION-all.jar"
$destDir = "gradle/wrapper"
$destJar = Join-Path -Path $destDir -ChildPath "gradle-wrapper.jar"

if (-not (Test-Path $destDir)) {
    New-Item -ItemType Directory -Path $destDir -Force | Out-Null
}

if (Test-Path $sourceJar) {
    Copy-Item -Path $sourceJar -Destination $destJar -Force
    Write-Host "Wrapper JAR copied successfully"
} else {
    Write-Host "Warning: Could not find source JAR at $sourceJar"
}

Write-Host "Cleaning up..."
Remove-Item -Path $ZIP_FILE -Force
Remove-Item -Path $EXTRACT_DIR -Recurse -Force

Write-Host "Gradle wrapper initialized successfully!"
