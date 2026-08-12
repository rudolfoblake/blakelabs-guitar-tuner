$ErrorActionPreference = "Stop"

$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$GradleWrapper = Join-Path $ProjectRoot "gradlew.bat"

Write-Host "Building Blake Labs Guitar Tuner (debug)..."
& $GradleWrapper -p $ProjectRoot --no-daemon :app:assembleDebug @args
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$Apk = Join-Path $ProjectRoot "app\build\outputs\apk\debug\app-debug.apk"
Write-Host ""
Write-Host "APK: $Apk"
