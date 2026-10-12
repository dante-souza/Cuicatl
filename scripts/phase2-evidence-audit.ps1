# SPDX-License-Identifier: AGPL-3.0-or-later
<#
.SYNOPSIS
    Verify the preserved Cuicatl laboratory archive without modifying it.
.DESCRIPTION
    Checks every archived payload against the SHA256SUMS.csv recorded before
    files were moved out of the development worktree. Produces an inventory
    grouped by review risk. Does not copy, stage, publish or sanitize files.
    Android system dumps may contain device identifiers or personal data;
    do not put them in the public repository without human review.
.EXAMPLE
    .\scripts\phase2-evidence-audit.ps1 -ArchivePath '..\Cuicatl-lab-evidence-20261012-132137'
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$ArchivePath,
    [string]$ReportDirectory
)

$ErrorActionPreference = 'Stop'

$root = (Resolve-Path -LiteralPath $ArchivePath).ProviderPath.TrimEnd('\', '/')
$manifestFile = Join-Path $root 'SHA256SUMS.csv'
if (-not (Test-Path -LiteralPath $manifestFile -PathType Leaf)) {
    throw "Archive manifest not found: $manifestFile"
}

$entries = @(Import-Csv -LiteralPath $manifestFile)
if ($entries.Count -eq 0) { throw 'Evidence manifest has no records.' }
if (-not ($entries[0].PSObject.Properties.Name -contains 'Path') -or
    -not ($entries[0].PSObject.Properties.Name -contains 'SHA256')) {
    throw 'Expected Path and SHA256 manifest columns.'
}

# Original archive metadata, not part of the pre-move payload manifest.
$metadataNames = @(
    'SHA256SUMS.csv',
    'gradle.properties.local',
    'gradle.properties.patch'
)
$seen = @{}
$inventory = @()
$rootPrefix = $root + [IO.Path]::DirectorySeparatorChar

foreach ($entry in $entries) {
    $relative = ([string]$entry.Path).Replace('/', '\').Trim()
    $expected = ([string]$entry.SHA256).Trim().ToUpperInvariant()
    if ([IO.Path]::IsPathRooted($relative) -or
        $relative -match '(^|[\\/])\.\.([\\/]|$)' -or
        [string]::IsNullOrWhiteSpace($relative)) {
        throw "Unsafe path in manifest: $relative"
    }
    if ($expected -notmatch '^[0-9A-F]{64}$') {
        throw "Invalid SHA256 digest in manifest for $relative"
    }
    $file = [IO.Path]::GetFullPath((Join-Path $root $relative))
    if (-not $file.StartsWith($rootPrefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Manifest path leaves archive: $relative"
    }
    if ($seen.ContainsKey($relative.ToLowerInvariant())) {
        throw "Duplicate archive manifest entry: $relative"
    }
    $seen[$relative.ToLowerInvariant()] = $true

    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) {
        throw "Archived file missing: $relative"
    }
    $actual = (Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash.ToUpperInvariant()
    if ($actual -ne $expected) {
        throw "SHA256 mismatch: $relative"
    }

    # Categories are *review dispositions*, NOT permission to publish.
    $category = if ($relative -match '^artifacts\\phase2d2-j8-bluetooth\\.*\.csv$') {
        'BLUETOOTH_CSV_REVIEW'
    } elseif ($relative -match '^artifacts\\phase2d2-j8-bluetooth\\') {
        'BLUETOOTH_OTHER_REVIEW'
    } elseif ($relative -match '^artifacts\\phase2c-j8-audio\\') {
        'CHARACTERIZATION_PRIVATE'
    } else {
        'SYSTEM_DIAGNOSTIC_PRIVATE'
    }
    $inventory += [pscustomobject]@{
        Path = $relative
        Bytes = (Get-Item -LiteralPath $file).Length
        SHA256 = $actual
        ReviewClass = $category
        Integrity = 'PASS'
    }
}

$unexpected = @(
    Get-ChildItem -LiteralPath $root -File -Recurse |
        Where-Object {
            $relative = $_.FullName.Substring($rootPrefix.Length).Replace('/', '\')
            -not $seen.ContainsKey($relative.ToLowerInvariant()) -and
            -not ($metadataNames -contains $relative)
        }
)
if ($unexpected.Count -gt 0) {
    throw ("Unmanifested file(s) in preserved archive: " +
        (($unexpected | ForEach-Object { $_.FullName }) -join ', '))
}

Write-Host ('Archive: ' + $root)
Write-Host ('SHA256 verified: ' + $inventory.Count + ' original evidence file(s).')
$inventory | Group-Object ReviewClass |
    Sort-Object Name |
    Select-Object Name, Count |
    Format-Table -AutoSize | Out-Host
Write-Host 'No originals were modified. No files were staged or published.'

if ($ReportDirectory) {
    $reportRoot = [IO.Path]::GetFullPath($ReportDirectory)
    if ($reportRoot.TrimEnd('\', '/') -eq $root -or
        $reportRoot.StartsWith($rootPrefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'Report directory must be OUTSIDE the immutable archive.'
    }
    New-Item -ItemType Directory -Force -Path $reportRoot | Out-Null
    $reportPath = Join-Path $reportRoot 'phase2-evidence-inventory.csv'
    $inventory | Export-Csv -LiteralPath $reportPath -NoTypeInformation -Encoding UTF8
    Write-Host ('Local inventory (review before sharing): ' + $reportPath)
}

Write-Host 'PASS: the archived payload matches its pre-move SHA256 manifest.'
