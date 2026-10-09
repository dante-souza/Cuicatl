# SPDX-License-Identifier: AGPL-3.0-or-later
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('ScreenOff', 'Recreate', 'Pages')]
    [string]$Mode
)

$ErrorActionPreference = 'Stop'

$Serial = '38c19745'
$PackageName = 'io.github.dante_souza.cuicatl'

& "$PSScriptRoot/preflight-j8.ps1"

function Invoke-AdbText {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    $output = @(& adb @Arguments)
    if ($LASTEXITCODE -ne 0) {
        throw "adb failed: adb $($Arguments -join ' ')"
    }

    return ($output -join "`n").Trim()
}

function Get-SessionMetadata {
    param(
        [Parameter(Mandatory = $true)]
        [string]$SessionId
    )

    return Invoke-AdbText @(
        '-s', $Serial,
        'shell', 'run-as', $PackageName,
        'cat', "files/sessions/$SessionId/session.properties"
    )
}

function Get-FrameCount {
    param(
        [Parameter(Mandatory = $true)]
        [string]$SessionId
    )

    $wc = Invoke-AdbText @(
        '-s', $Serial,
        'shell', 'run-as', $PackageName,
        'wc', '-l', "files/sessions/$SessionId/frames.tsv"
    )

    if ($wc -notmatch '^(\d+)') {
        throw "Could not parse frame-line count: $wc"
    }

    return [Math]::Max(0, ([int]$Matches[1]) - 1)
}

function Find-RunningSessionId {
    $sessionIdsText = Invoke-AdbText @(
        '-s', $Serial,
        'shell', 'run-as', $PackageName,
        'ls', 'files/sessions'
    )

    $sessionIds = @(
        $sessionIdsText -split '\s+' |
            Where-Object { $_ -match '\S' }
    )

    $running = @()

    foreach ($sessionId in $sessionIds) {
        $metadata = Get-SessionMetadata -SessionId $sessionId
        if ($metadata -match '(?m)^state=RUNNING\r?$') {
            $running += $sessionId
        }
    }

    if ($running.Count -ne 1) {
        throw "Expected exactly one RUNNING Cuicatl session; found $($running.Count)."
    }

    return $running[0]
}

function Invoke-TapText {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Text
    )

    & adb -s $Serial shell uiautomator dump /sdcard/cuicatl-window.xml | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not dump Android UI hierarchy.'
    }

    $xmlText = Invoke-AdbText @(
        '-s', $Serial,
        'shell', 'cat', '/sdcard/cuicatl-window.xml'
    )

    [xml]$xml = $xmlText
    $escapedText = $Text.Replace("'", "&apos;")
    $node = $xml.SelectSingleNode("//*[@text='$escapedText']")

    if ($null -eq $node) {
        throw "Could not find visible UI node with text '$Text'."
    }

    $bounds = [string]$node.bounds
    if ($bounds -notmatch '^\[(\d+),(\d+)\]\[(\d+),(\d+)\]$') {
        throw "Could not parse bounds for '$Text': $bounds"
    }

    $x = [int](([int]$Matches[1] + [int]$Matches[3]) / 2)
    $y = [int](([int]$Matches[2] + [int]$Matches[4]) / 2)

    & adb -s $Serial shell input tap $x $y | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Could not tap '$Text'."
    }
}

$sessionId = Find-RunningSessionId
$preFrames = Get-FrameCount -SessionId $sessionId

Write-Host ''
Write-Host "Phase 1 lifecycle check: $Mode"
Write-Host "Session: $sessionId"
Write-Host "Pre-check complete rows: $preFrames"

if ($Mode -eq 'Pages') {
    Write-Host 'Switching Meter -> History -> Meter through the visible navigation labels...'

    Invoke-TapText -Text 'History'
    Start-Sleep -Seconds 3

    $historyFrames = Get-FrameCount -SessionId $sessionId
    $historySessionId = Find-RunningSessionId

    Invoke-TapText -Text 'Meter'
    Start-Sleep -Seconds 3

    $meterFrames = Get-FrameCount -SessionId $sessionId
    $postSessionId = Find-RunningSessionId

    Write-Host ''
    Write-Host 'Meter/History navigation result'
    Write-Host '-------------------------------'
    Write-Host "Rows before navigation: $preFrames"
    Write-Host "Rows on History page:    $historyFrames"
    Write-Host "Rows back on Meter:      $meterFrames"
    Write-Host "Same session on History: $($historySessionId -eq $sessionId)"
    Write-Host "Same session on Meter:   $($postSessionId -eq $sessionId)"

    if ($historyFrames -le $preFrames) {
        throw 'Capture/persistence did not advance after switching to History.'
    }

    if ($meterFrames -le $historyFrames) {
        throw 'Capture/persistence did not advance after switching back to Meter.'
    }

    if ($historySessionId -ne $sessionId -or $postSessionId -ne $sessionId) {
        throw 'Active session identity changed across Meter/History navigation.'
    }

    Write-Host 'PASS: Meter/History switching preserved the same active session while frames advanced.'
    exit 0
}

if ($Mode -eq 'ScreenOff') {
    Write-Host 'Turning the screen off for 15 seconds...'
    & adb -s $Serial shell input keyevent 26 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not send power keyevent.'
    }

    try {
        Start-Sleep -Seconds 15
        $offFrames = Get-FrameCount -SessionId $sessionId
    }
    finally {
        & adb -s $Serial shell input keyevent 26 | Out-Null
        Start-Sleep -Seconds 2
    }

    $postFrames = Get-FrameCount -SessionId $sessionId
    $postSessionId = Find-RunningSessionId

    Write-Host ''
    Write-Host 'Screen-off result'
    Write-Host '-----------------'
    Write-Host "Rows before screen off: $preFrames"
    Write-Host "Rows while screen off:  $offFrames"
    Write-Host "Rows after wake:         $postFrames"
    Write-Host "Rows added while off:    $($offFrames - $preFrames)"
    Write-Host "Same active session:     $($postSessionId -eq $sessionId)"

    if ($offFrames -le $preFrames) {
        throw 'No persisted measurement progress was observed while the screen was off.'
    }

    if ($postSessionId -ne $sessionId) {
        throw 'Active session identity changed across screen-off check.'
    }

    Write-Host 'PASS: capture/persistence continued while the J8 screen was off.'
    exit 0
}

$originalAccelerometer = Invoke-AdbText @(
    '-s', $Serial,
    'shell', 'settings', 'get', 'system', 'accelerometer_rotation'
)

$originalRotation = Invoke-AdbText @(
    '-s', $Serial,
    'shell', 'settings', 'get', 'system', 'user_rotation'
)

Write-Host 'Forcing landscape then portrait to recreate the Activity...'

try {
    & adb -s $Serial shell settings put system accelerometer_rotation 0 | Out-Null
    & adb -s $Serial shell settings put system user_rotation 1 | Out-Null

    Start-Sleep -Seconds 4
    $landscapeFrames = Get-FrameCount -SessionId $sessionId

    & adb -s $Serial shell settings put system user_rotation 0 | Out-Null

    Start-Sleep -Seconds 4
    $portraitFrames = Get-FrameCount -SessionId $sessionId
}
finally {
    if ($originalAccelerometer -match '^\d+$') {
        & adb -s $Serial shell settings put system accelerometer_rotation $originalAccelerometer | Out-Null
    }

    if ($originalRotation -match '^\d+$') {
        & adb -s $Serial shell settings put system user_rotation $originalRotation | Out-Null
    }
}

$postSessionId = Find-RunningSessionId
$postFrames = Get-FrameCount -SessionId $sessionId

Write-Host ''
Write-Host 'Activity-recreation result'
Write-Host '--------------------------'
Write-Host "Rows before rotation: $preFrames"
Write-Host "Rows in landscape:     $landscapeFrames"
Write-Host "Rows after portrait:   $portraitFrames"
Write-Host "Rows at final check:   $postFrames"
Write-Host "Same active session:   $($postSessionId -eq $sessionId)"

if ($postFrames -le $preFrames) {
    throw 'No persisted measurement progress was observed across Activity recreation.'
}

if ($postSessionId -ne $sessionId) {
    throw 'Active session identity changed across Activity recreation.'
}

Write-Host 'PASS: service-owned capture survived Activity recreation with the same session.'
