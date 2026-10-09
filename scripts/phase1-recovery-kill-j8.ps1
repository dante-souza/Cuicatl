# SPDX-License-Identifier: AGPL-3.0-or-later
$ErrorActionPreference = 'Stop'

$Serial = '38c19745'
$PackageName = 'io.github.dante_souza.cuicatl'

& "$PSScriptRoot/preflight-j8.ps1"

function Invoke-AdbText {
    param([Parameter(Mandatory = $true)][string[]]$Arguments)

    $output = @(& adb @Arguments)
    if ($LASTEXITCODE -ne 0) {
        throw "adb failed: adb $($Arguments -join ' ')"
    }
    return ($output -join "`n").Trim()
}

function Get-SessionMetadata {
    param([Parameter(Mandatory = $true)][string]$SessionId)

    return Invoke-AdbText @(
        '-s', $Serial,
        'shell', 'run-as', $PackageName,
        'cat', "files/sessions/$SessionId/session.properties"
    )
}

function Get-FrameCount {
    param([Parameter(Mandatory = $true)][string]$SessionId)

    $wc = Invoke-AdbText @(
        '-s', $Serial,
        'shell', 'run-as', $PackageName,
        'wc', '-l', "files/sessions/$SessionId/frames.tsv"
    )

    if ($wc -notmatch '^(\d+)') {
        throw "Could not parse frame-line count: $wc"
    }

    $lineCount = [int]$Matches[1]
    return [Math]::Max(0, $lineCount - 1)
}

$sessionIdsText = Invoke-AdbText @(
    '-s', $Serial,
    'shell', 'run-as', $PackageName,
    'ls', 'files/sessions'
)

$sessionIds = @($sessionIdsText -split '\s+' | Where-Object { $_ -match '\S' })
$runningSessionId = $null

foreach ($sessionId in $sessionIds) {
    $metadata = Get-SessionMetadata $sessionId
    if ($metadata -match '(?m)^state=RUNNING\r?$') {
        if ($null -ne $runningSessionId) {
            throw "More than one RUNNING session found. Refusing recovery test."
        }
        $runningSessionId = $sessionId
    }
}

if ($null -eq $runningSessionId) {
    throw "No RUNNING Cuicatl session found. Start a Phase 1 measurement first."
}

$preMetadata = Get-SessionMetadata $runningSessionId
$preFrames = Get-FrameCount $runningSessionId

Write-Host ""
Write-Host "Phase 1 recovery test target: $runningSessionId"
Write-Host "Complete persisted frames immediately before force-stop snapshot: $preFrames"
Write-Host "Force-stopping Cuicatl now..."

& adb -s $Serial shell am force-stop $PackageName
if ($LASTEXITCODE -ne 0) {
    throw 'adb force-stop failed.'
}

Start-Sleep -Milliseconds 750

& adb -s $Serial shell monkey -p $PackageName -c android.intent.category.LAUNCHER 1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw 'Cuicatl relaunch failed.'
}

$postMetadata = ''
for ($attempt = 1; $attempt -le 8; $attempt++) {
    Start-Sleep -Seconds 1
    $postMetadata = Get-SessionMetadata $runningSessionId
    if ($postMetadata -notmatch '(?m)^state=RUNNING\r?$') {
        break
    }
}

$postFrames = Get-FrameCount $runningSessionId
$knownFrameLoss = [Math]::Max(0, $preFrames - $postFrames)

$outcome = if ($postMetadata -match '(?m)^outcome=([^\r\n]+)') { $Matches[1] } else { '<missing>' }
$reason = if ($postMetadata -match '(?m)^interruption_reason=([^\r\n]+)') { $Matches[1] } else { '<missing>' }
$state = if ($postMetadata -match '(?m)^state=([^\r\n]+)') { $Matches[1] } else { '<missing>' }

Write-Host ""
Write-Host "Recovery result"
Write-Host "---------------"
Write-Host "State:                  $state"
Write-Host "Outcome:                $outcome"
Write-Host "Interruption reason:    $reason"
Write-Host "Pre-kill complete rows: $preFrames"
Write-Host "Recovered complete rows:$postFrames"
Write-Host "Known complete-row loss:$knownFrameLoss"

if ($postFrames -ge $preFrames) {
    Write-Host "Preservation result: all complete frame rows visible at the pre-kill snapshot survived."
} else {
    Write-Warning "$knownFrameLoss complete frame row(s) visible before force-stop were not recovered."
}

if ($state -ne 'SAVED' -or $outcome -ne 'RECOVERED' -or $reason -ne 'process_recovery') {
    throw "Recovery metadata did not reach the required SAVED / RECOVERED / process_recovery state."
}

Write-Host ""
Write-Host "Open History and export the recovered CSV for independent inspection."
