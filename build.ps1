param([string]$Task = "assembleDebug")

# Notes:
# This machine sets ANDROID_SDK_HOME=D:\AndroidAVD, which lives outside this
# project directory. The local file sandbox cannot write there (same reason
# ~/.gradle is unavailable), so this script redirects the Gradle user home and
# the Android preferences/AVD home into hidden folders inside the project.
#
# You do NOT need this script when opening the project in Android Studio.
# Usage:  .\build.ps1                 # assembleDebug
#         .\build.ps1 assembleRelease

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$androidHome = Join-Path $root ".androidhome"
$prefsRoot = Join-Path $androidHome ".android"

$env:GRADLE_USER_HOME = Join-Path $root ".gradlehome"
$env:ANDROID_SDK_ROOT = "D:\AndroidSDK"
$env:ANDROID_HOME = "D:\AndroidSDK"
# Only ONE prefs-root variable may be set, otherwise AGP aborts with
# "Several environment variables ... contain different paths to the
# Android Preferences folder". ANDROID_SDK_HOME keeps the legacy layout
# (<home>/.android), which is where debug.keystore lives.
$env:ANDROID_SDK_HOME = $androidHome
Remove-Item Env:\ANDROID_USER_HOME -ErrorAction SilentlyContinue
$env:ANDROID_AVD_HOME = Join-Path $androidHome "avd"
$env:ANDROID_EMULATOR_HOME = $androidHome

New-Item -ItemType Directory -Force -Path $prefsRoot | Out-Null

# Seed debug.keystore from the machine-wide one so the build can sign.
$keystore = Join-Path $prefsRoot "debug.keystore"
if (-not (Test-Path $keystore)) {
    $source = "D:\AndroidAVD\.android\debug.keystore"
    if (Test-Path $source) { Copy-Item $source $keystore -Force }
}

$gradle = Get-ChildItem "$env:USERPROFILE\.gradle\wrapper\dists\gradle-8.13-bin" -Recurse -Filter gradle.bat -ErrorAction SilentlyContinue |
    Select-Object -First 1 -ExpandProperty FullName
if (-not $gradle) {
    $wrapper = Join-Path $root "gradlew.bat"
    if (Test-Path $wrapper) { $gradle = $wrapper } else { throw "Gradle not found. Sync the project in Android Studio first." }
}

# Gradle writes benign warnings to stderr; do not let that abort the script.
$ErrorActionPreference = "Continue"
& $gradle --no-daemon $Task
exit $LASTEXITCODE
