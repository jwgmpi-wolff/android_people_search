[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

if (-not (Test-Path '.\gradlew.bat'))
{
    throw 'Gradle wrapper not found. Open the project in Android Studio or regenerate the wrapper first.'
}

& .\gradlew.bat testDebugUnitTest assembleDebug --stacktrace
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host "APK: $repoRoot\app\build\outputs\apk\debug\app-debug.apk"
