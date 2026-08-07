$ErrorActionPreference = "Stop"

$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$GradleVersion = "9.5.0"
$CacheRoot = Join-Path $ProjectRoot ".gradle-local"
$GradleHome = Join-Path $CacheRoot "gradle-$GradleVersion"
$GradleExe = Join-Path $GradleHome "bin\gradle.bat"
$ZipPath = Join-Path $CacheRoot "gradle-$GradleVersion-bin.zip"

if (-not (Test-Path $GradleExe)) {
    New-Item -ItemType Directory -Force -Path $CacheRoot | Out-Null

    if (-not (Test-Path $ZipPath)) {
        Write-Host "Downloading Gradle $GradleVersion..."
        Invoke-WebRequest `
            -Uri "https://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip" `
            -OutFile $ZipPath
    }

    Write-Host "Extracting Gradle $GradleVersion..."
    Expand-Archive -Path $ZipPath -DestinationPath $CacheRoot -Force
}

Write-Host "Building Blake Labs Guitar Tuner (debug)..."
& $GradleExe -p $ProjectRoot :app:assembleDebug @args
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$Apk = Join-Path $ProjectRoot "app\build\outputs\apk\debug\app-debug.apk"
Write-Host ""
Write-Host "APK: $Apk"
