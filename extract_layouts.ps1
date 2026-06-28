# PowerShell script to extract edited .lang layout templates from the debug APK on a connected device
# and copy them into the repository's assets directory.

$ErrorActionPreference = "Stop"

# 1. Check if ADB is installed and available
if (!(Get-Command adb -ErrorAction SilentlyContinue)) {
    Write-Error "ADB is not installed or not in your PATH. Please install Android Platform Tools."
    exit 1
}

# 2. Check if a device is connected
$devices = adb devices | Select-String -Pattern "device$"
if ($devices.Count -eq 0) {
    Write-Host "No connected Android devices found. Please connect your phone with USB debugging enabled." -ForegroundColor Red
    exit 1
}

Write-Host "Found connected device(s). Connecting to GridType keyboard..." -ForegroundColor Green

$packageName = "com.gridtype.keyboard"
$targetDir = "app/src/main/assets/layouts"

# Ensure target directory exists in the repository
if (!(Test-Path $targetDir)) {
    New-Item -ItemType Directory -Force -Path $targetDir | Out-Null
}

# 3. List all .lang files in the app's private layouts directory on the device
Write-Host "Scanning for edited base language (.lang) files on device..." -ForegroundColor Cyan
$cmd = "run-as $packageName sh -c 'ls files/layouts/*.lang 2>/dev/null'"
$files = adb shell $cmd

# Clean up ADB output line endings
$files = $files | ForEach-Object { $_.Trim() } | Where-Object { $_ -ne "" -and $_ -notlike "*No such file*" -and $_ -notlike "*Permission denied*" }

if ($files.Count -eq 0) {
    Write-Host "No edited .lang files found in the app's private storage (files/layouts/)." -ForegroundColor Yellow
    exit 0
}

Write-Host "Found $($files.Count) layout file(s) to extract." -ForegroundColor Green

# 4. Extract each file using binary-safe adb exec-out and save it to the repository
foreach ($file in $files) {
    $filename = Split-Path $file -Leaf
    $destinationPath = Join-Path $targetDir $filename
    
    Write-Host "Extracting $filename -> $destinationPath" -ForegroundColor Cyan
    
    # Use exec-out to prevent line ending translation (preserves original format)
    $execCmd = "run-as $packageName cat $file"
    
    try {
        # Execute adb exec-out and redirect output to the local file
        $bytes = adb exec-out $execCmd
        [System.IO.File]::WriteAllBytes($destinationPath, $bytes)
        Write-Host "Successfully extracted $filename." -ForegroundColor Green
    }
    catch {
        Write-Host "Failed to extract $($filename): $_" -ForegroundColor Red
    }
}

Write-Host "Extraction complete!" -ForegroundColor Green
