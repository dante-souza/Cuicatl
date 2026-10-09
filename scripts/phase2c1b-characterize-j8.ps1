# SPDX-License-Identifier: AGPL-3.0-or-later
$ErrorActionPreference = 'Stop'

$ExpectedSerial = '38c19745'
$ExpectedModel = 'SM-J810M'
$ExpectedProduct = 'j8y18lte'

& "$PSScriptRoot/preflight-j8.ps1"

$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$out = Join-Path (Join-Path $PSScriptRoot '..') "artifacts/phase2c-j8-audio/characterization-$stamp"
New-Item -ItemType Directory -Force $out | Out-Null

function Capture-Adb {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$ShellCommand
    )

    $target = Join-Path $out $Name
    # Use cmd.exe so Android stderr is captured into the evidence file too.
    & adb -s $ExpectedSerial shell "$ShellCommand 2>&1" |
        Out-File -FilePath $target -Encoding utf8
}

$model = (& adb -s $ExpectedSerial shell getprop ro.product.model).Trim()
$product = (& adb -s $ExpectedSerial shell getprop ro.product.device).Trim()

@(
    "collected_at_local=$(Get-Date -Format o)"
    "serial=$ExpectedSerial"
    "model=$model"
    "product=$product"
    "collection_mode=read_only"
    "expected_state=Cuicatl actively recording during collection"
) | Set-Content -Path (Join-Path $out 'collection-metadata.txt') -Encoding utf8

Capture-Adb 'audio-policy-live.txt' 'dumpsys media.audio_policy'
Capture-Adb 'audio-flinger-live.txt' 'dumpsys media.audio_flinger'
Capture-Adb 'getprop-live.txt' 'getprop'
Capture-Adb 'tinymix-live.txt' 'command -v tinymix >/dev/null && tinymix || echo tinymix_not_available'
Capture-Adb 'asound-cards-live.txt' 'cat /proc/asound/cards'
Capture-Adb 'asound-pcm-live.txt' 'cat /proc/asound/pcm'
Capture-Adb 'asoc-debugfs-live.txt' 'ls -la /sys/kernel/debug/asoc; echo ---codecs---; cat /sys/kernel/debug/asoc/codecs; echo ---dais---; cat /sys/kernel/debug/asoc/dais; echo ---platforms---; cat /sys/kernel/debug/asoc/platforms'
Capture-Adb 'sysfs-audio-devices-live.txt' 'for d in /sys/bus/platform/devices/* /sys/bus/spmi/devices/*; do n=$(basename "$d"); echo "$n"; done | grep -Ei "audio|codec|wcd|pm8953|msm8953|sound|snd|qcom"'
Capture-Adb 'proc-device-tree-compatible.txt' 'cat /proc/device-tree/compatible'
Capture-Adb 'vendor-audio-files-live.txt' 'ls -la /vendor/etc/*audio* /vendor/etc/*mixer* 2>/dev/null'

$hashLines = Get-ChildItem -File $out |
    Where-Object { $_.Name -ne 'SHA256SUMS.txt' } |
    Sort-Object Name |
    ForEach-Object {
        $hash = Get-FileHash $_.FullName -Algorithm SHA256
        "$($hash.Hash.ToLowerInvariant())  $($_.Name)"
    }
$hashLines | Set-Content -Path (Join-Path $out 'SHA256SUMS.txt') -Encoding ascii

Write-Host ""
Write-Host "Phase 2C.1b read-only characterization captured:"
Write-Host "  $out"
Write-Host ""
Write-Host "Keep Cuicatl recording until this command finishes."
Write-Host "Commit the new characterization-* directory after inspection."
