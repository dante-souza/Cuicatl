# SPDX-License-Identifier: AGPL-3.0-or-later
$ErrorActionPreference = 'Stop'

$ExpectedSerial = '38c19745'
$ExpectedModel = 'SM-J810M'
$ExpectedProduct = 'j8y18lte'

& "$PSScriptRoot/preflight-j8.ps1"

$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$out = Join-Path (Join-Path $PSScriptRoot '..') "artifacts/phase2c-j8-audio/characterization-$stamp"
New-Item -ItemType Directory -Force $out | Out-Null

$script:ProbeRecords = @()
$script:RootAvailable = $false

function Test-AdbRoot {
    $probeOutput = @(& adb -s $ExpectedSerial shell su -c id 2>&1)
    $probeExitCode = $LASTEXITCODE
    $probeText = (($probeOutput | ForEach-Object { "$_" }) -join "`n").Trim()

    [pscustomobject]@{
        Available = ($probeExitCode -eq 0 -and $probeText -match 'uid=0\(root\)')
        ExitCode = $probeExitCode
        Output = $probeText
    }
}

function Capture-Adb {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$ShellCommand,
        [switch]$PreferRoot
    )

    $target = Join-Path $out $Name
    $requestedPrivilege = if ($PreferRoot) { 'root-preferred' } else { 'shell' }
    $useRoot = $PreferRoot -and $script:RootAvailable
    $effectivePrivilege = if ($useRoot) {
        'root'
    } elseif ($PreferRoot) {
        'shell-fallback'
    } else {
        'shell'
    }

    # Capture both adb stdout and stderr on the host. Keep Android-side commands read-only.
    if ($useRoot) {
        $capture = @(& adb -s $ExpectedSerial shell su -c $ShellCommand 2>&1)
    } else {
        $capture = @(& adb -s $ExpectedSerial shell $ShellCommand 2>&1)
    }
    $exitCode = $LASTEXITCODE

    if ($capture.Count -eq 0) {
        '' | Out-File -FilePath $target -Encoding utf8
    } else {
        $capture | ForEach-Object { "$_" } |
            Out-File -FilePath $target -Encoding utf8
    }

    $script:ProbeRecords += "$Name`t$requestedPrivilege`t$effectivePrivilege`t$exitCode"
}

$rootProbe = Test-AdbRoot
$script:RootAvailable = $rootProbe.Available
$rootAvailableText = $script:RootAvailable.ToString().ToLowerInvariant()

@(
    "root_available=$rootAvailableText"
    "su_probe_exit_code=$($rootProbe.ExitCode)"
    'su_probe_output_begin'
    $rootProbe.Output
    'su_probe_output_end'
) | Set-Content -Path (Join-Path $out 'root-status.txt') -Encoding utf8

$model = (& adb -s $ExpectedSerial shell getprop ro.product.model).Trim()
$product = (& adb -s $ExpectedSerial shell getprop ro.product.device).Trim()

@(
    "collected_at_local=$(Get-Date -Format o)"
    "serial=$ExpectedSerial"
    "model=$model"
    "product=$product"
    "collection_mode=read_only"
    "root_available=$rootAvailableText"
    "privileged_probe_policy=prefer_root_with_shell_fallback"
    "expected_state=Cuicatl actively recording during collection"
) | Set-Content -Path (Join-Path $out 'collection-metadata.txt') -Encoding utf8

Capture-Adb 'audio-policy-live.txt' 'dumpsys media.audio_policy'
Capture-Adb 'audio-flinger-live.txt' 'dumpsys media.audio_flinger'
Capture-Adb 'getprop-live.txt' 'getprop'

# Preserve the ordinary shell result, then use root when available for the useful mixer inventory.
Capture-Adb 'tinymix-shell-access-live.txt' 'if command -v tinymix >/dev/null 2>&1; then echo tinymix_present; command -v tinymix; tinymix; rc=$?; echo tinymix_exit_code=$rc; else echo tinymix_not_installed; fi'
Capture-Adb 'tinymix-live.txt' 'if command -v tinymix >/dev/null 2>&1; then echo tinymix_present; command -v tinymix; tinymix; rc=$?; echo tinymix_exit_code=$rc; else echo tinymix_not_installed; fi' -PreferRoot

Capture-Adb 'asound-cards-live.txt' 'cat /proc/asound/cards'
Capture-Adb 'asound-pcm-live.txt' 'cat /proc/asound/pcm'

# Preserve the stock shell restriction as evidence, then inventory ASoC debugfs with root when available.
Capture-Adb 'asoc-debugfs-shell-access-live.txt' 'ls -la /sys/kernel/debug/asoc; rc=$?; echo asoc_ls_exit_code=$rc'
Capture-Adb 'asoc-debugfs-live.txt' 'echo ---root-listing---; ls -la /sys/kernel/debug/asoc; echo ---files-depth-2---; for f in /sys/kernel/debug/asoc/* /sys/kernel/debug/asoc/*/*; do if [ -f "$f" ]; then echo "$f"; fi; done; echo ---card-directories---; for d in /sys/kernel/debug/asoc/*; do if [ -d "$d" ]; then echo ==== "$d" ====; ls -la "$d"; fi; done; echo ---codecs---; cat /sys/kernel/debug/asoc/codecs; echo ---dais---; cat /sys/kernel/debug/asoc/dais' -PreferRoot

# Do not pipe through grep on-device: adb/shell quoting can split alternation expressions.
# Preserve full device-name inventories and filter them during analysis.
Capture-Adb 'sysfs-platform-devices-live.txt' 'ls -1 /sys/bus/platform/devices'
Capture-Adb 'sysfs-spmi-devices-live.txt' 'ls -1 /sys/bus/spmi/devices'
Capture-Adb 'sys-class-sound-live.txt' 'for f in /sys/class/sound/card*/id /sys/class/sound/card*/device/uevent /sys/class/sound/card*/uevent; do echo ==== $f ====; cat $f; done'

Capture-Adb 'proc-device-tree-compatible.txt' 'cat /proc/device-tree/compatible'
Capture-Adb 'vendor-audio-files-live.txt' 'ls -la /vendor/etc/*audio* /vendor/etc/*mixer*'

@(
    "file`trequested_privilege`teffective_privilege	exit_code"
    $script:ProbeRecords
) | Set-Content -Path (Join-Path $out 'probe-privileges.tsv') -Encoding utf8

$hashLines = Get-ChildItem -File $out |
    Where-Object { $_.Name -ne 'SHA256SUMS.txt' } |
    Sort-Object Name |
    ForEach-Object {
        $hash = Get-FileHash $_.FullName -Algorithm SHA256
        "$($hash.Hash.ToLowerInvariant())  $($_.Name)"
    }
$hashLines | Set-Content -Path (Join-Path $out 'SHA256SUMS.txt') -Encoding ascii

Write-Host ""
if ($script:RootAvailable) {
    Write-Host "Root access: available via su; privileged audio probes used root."
} else {
    Write-Host "Root access: unavailable; privileged audio probes used shell fallback."
}
Write-Host ""
Write-Host "Phase 2C.1b read-only characterization captured:"
Write-Host "  $out"
Write-Host ""
Write-Host "Keep Cuicatl recording until this command finishes."
Write-Host "Inspect root-status.txt and probe-privileges.tsv before committing the evidence."
