# SPDX-License-Identifier: AGPL-3.0-or-later
<#
.SYNOPSIS
    Inspect Galaxy J8 Cuicatl session outcomes without changing app data.
.DESCRIPTION
    Uses adb run-as on the installed DEBUG build (no root) to read app-private
    session.properties metadata. Prints only selected provenance fields, never
    the stored PCM/frames or processing diagnostics. Does not write to the
    device, change Bluetooth state, or export private data to Git.
.EXAMPLE
    .\scripts\phase2d3a-session-outcomes-j8.ps1
#>
[CmdletBinding()]
param(
    [string]$Serial = '38c19745',
    [string]$Package = 'io.github.dante_souza.cuicatl',
    [ValidateRange(1, 100)]
    [int]$MaxSessions = 12
)

$ErrorActionPreference = 'Stop'
$null = Get-Command adb -ErrorAction Stop

# Android java.util.Properties.store escapes '=' and non-ASCII characters.
# Decode only for human-readable output; do not alter stored session metadata.
function ConvertFrom-JavaPropertiesDisplayValue {
    param([string]$Value)
    $decoded = [regex]::Replace(
        $Value,
        '\\u([0-9a-fA-F]{4})',
        [System.Text.RegularExpressions.MatchEvaluator] {
            param($match)
            return ([string][char]([Convert]::ToInt32($match.Groups[1].Value, 16)))
        }
    )
    return $decoded.Replace('\=', '=').Replace('\:', ':').Replace('\\', '\')
}

$sessionIds = @(& adb -s $Serial shell run-as $Package ls files/sessions 2>&1)
if ($LASTEXITCODE -ne 0) {
    throw ('Could not read the debug app session directory with adb run-as. ' +
        'Check J8 connection, installed debug build and USB authorization. ' +
        'Alternatively inspect History in the app and export the CSV. ' +
        ($sessionIds -join ' '))
}

# A session is an app-generated UUID. Ignore non-session ls output.
$ids = @($sessionIds | ForEach-Object { ([string]$_).Trim() } |
    Where-Object { $_ -match '^[0-9A-Fa-f]{8}-([0-9A-Fa-f]{4}-){3}[0-9A-Fa-f]{12}$' })
if ($ids.Count -eq 0) {
    Write-Host 'No saved session UUID directories reported by run-as.'
    return
}

$rows = @(
    foreach ($id in $ids) {
        $lines = @(& adb -s $Serial shell run-as $Package cat "files/sessions/$id/session.properties" 2>&1)
        if ($LASTEXITCODE -ne 0) {
            Write-Warning ("Could not read session metadata for $id")
            continue
        }

        $properties = @{}
        foreach ($line in $lines) {
            $s = [string]$line
            if ($s.StartsWith('#') -or $s.StartsWith('!')) { continue }
            $separator = $s.IndexOf('=')
            if ($separator -lt 1) { continue }
            $name = $s.Substring(0, $separator).Trim()
            $value = $s.Substring($separator + 1).Trim()
            $properties[$name] = $value
        }

        $epoch = 0L
        if (-not [long]::TryParse([string]$properties['started_utc_ms'], [ref]$epoch)) {
            Write-Warning ("Missing/invalid start timestamp for $id")
            continue
        }

        $utc = [DateTimeOffset]::FromUnixTimeMilliseconds($epoch).UtcDateTime.ToString('yyyy-MM-dd HH:mm:ss')
        [pscustomobject]@{
            StartedUTC = $utc
            StartEpoch = $epoch
            Session = $id.Substring(0, 8)
            State = [string]$properties['state']
            Outcome = [string]$properties['outcome']
            Selection = [string]$properties['input_selection_mode']
            Route = [string]$properties['input_route_kind']
            Input = ConvertFrom-JavaPropertiesDisplayValue ([string]$properties['input_identity'])
            Frames = [string]$properties['frame_count']
            CapturedMs = [string]$properties['captured_ms']
            Reason = [string]$properties['interruption_reason']
        }
    }
)

if ($rows.Count -eq 0) {
    Write-Warning 'No readable Cuicatl session metadata was found.'
    return
}

Write-Host 'Cuicatl J8 saved-session provenance (read-only, no root):'
$rows | Sort-Object StartEpoch -Descending | Select-Object -First $MaxSessions |
    Format-Table StartedUTC, Session, Outcome, Selection, Route, Input, Frames, CapturedMs, Reason -Wrap |
    Out-Host

Write-Host 'Review criterion: a Bluetooth-off test with selected BLUETOOTH_SCO must'
Write-Host 'end INTERRUPTED with a transport/route reason, not COMPLETED.'
Write-Host 'A missing saved session requires History/CSV review; no PASS is inferred.'
