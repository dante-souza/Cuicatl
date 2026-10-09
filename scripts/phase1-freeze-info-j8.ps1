# SPDX-License-Identifier: AGPL-3.0-or-later
$ErrorActionPreference = 'Stop'

$Serial = '38c19745'
$Apk = 'app/build/outputs/apk/debug/app-debug.apk'

& "$PSScriptRoot/preflight-j8.ps1"

$branch = (& git branch --show-current).Trim()
if ($LASTEXITCODE -ne 0) {
    throw 'Could not determine Git branch.'
}

$commit = (& git rev-parse HEAD).Trim()
if ($LASTEXITCODE -ne 0) {
    throw 'Could not determine Git commit.'
}

$dirty = @(& git status --short)
if ($LASTEXITCODE -ne 0) {
    throw 'Could not inspect Git working tree.'
}

Write-Host ""
Write-Host "Running Phase 1 freeze quality gate..."
& "$PSScriptRoot/gradle-windows.ps1" --no-daemon testDebugUnitTest lintDebug assembleDebug
if ($LASTEXITCODE -ne 0) {
    throw 'Phase 1 freeze build/check failed.'
}

if (-not (Test-Path $Apk)) {
    throw "Debug APK missing after build: $Apk"
}

$hash = Get-FileHash -Algorithm SHA256 $Apk
$apkInfo = Get-Item $Apk

function Get-AdbProp {
    param([Parameter(Mandatory = $true)][string]$Name)

    $value = (& adb -s $Serial shell getprop $Name)
    if ($LASTEXITCODE -ne 0) {
        throw "Could not read Android property: $Name"
    }
    return ($value -join '').Trim()
}

$model = Get-AdbProp 'ro.product.model'
$device = Get-AdbProp 'ro.product.device'
$android = Get-AdbProp 'ro.build.version.release'
$sdk = Get-AdbProp 'ro.build.version.sdk'
$patch = Get-AdbProp 'ro.build.version.security_patch'

Write-Host ""
Write-Host "Phase 1 freeze candidate"
Write-Host "------------------------"
Write-Host "Branch:          $branch"
Write-Host "Commit:          $commit"
Write-Host "Working tree:    $(if ($dirty.Count -eq 0) { 'clean' } else { 'DIRTY' })"
Write-Host "APK:             $Apk"
Write-Host "APK size bytes:  $($apkInfo.Length)"
Write-Host "APK SHA256:      $($hash.Hash)"
Write-Host "J8 serial:       $Serial"
Write-Host "Device:          $model / $device"
Write-Host "Android:         $android (API $sdk)"
Write-Host "Security patch:  $patch"

if ($dirty.Count -ne 0) {
    Write-Host ""
    Write-Warning "Working tree is not clean. Do not tag or freeze this candidate until the local changes are reviewed."
    $dirty | ForEach-Object { Write-Host "  $_" }
    exit 2
}

Write-Host ""
Write-Host "PASS: build/check succeeded and the freeze candidate working tree is clean."
