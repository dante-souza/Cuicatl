# SPDX-License-Identifier: AGPL-3.0-or-later
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $GradleArgs
)

$ErrorActionPreference = 'Stop'

function Get-JavaVersionLine([string] $JavaExe) {
    if (-not (Test-Path $JavaExe)) {
        return $null
    }

    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $JavaExe
    $startInfo.Arguments = '-version'
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardError = $true
    $startInfo.RedirectStandardOutput = $true

    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $startInfo

    try {
        [void] $process.Start()
        $stderr = $process.StandardError.ReadToEnd()
        $stdout = $process.StandardOutput.ReadToEnd()
        $process.WaitForExit()
    } finally {
        $process.Dispose()
    }

    $combined = (($stderr + [Environment]::NewLine + $stdout) -split "\r?\n") |
        Where-Object { $_ -and $_.Trim() } |
        Select-Object -First 1

    if ($combined) {
        return $combined.Trim()
    }

    return $null
}

function Get-JavaMajor([string] $JavaExe) {
    $firstLine = Get-JavaVersionLine $JavaExe
    if (-not $firstLine) {
        return $null
    }

    if ($firstLine -match 'version "(?<major>\d+)') {
        return [int]$Matches.major
    }

    if ($firstLine -match '(?<major>\d+)(?:\.\d+)*') {
        return [int]$Matches.major
    }

    return $null
}

$candidateHomes = New-Object System.Collections.Generic.List[string]

if ($env:JAVA_HOME) {
    $candidateHomes.Add($env:JAVA_HOME.Trim('"'))
}

$searchRoots = @(
    (Join-Path $env:ProgramFiles 'Eclipse Adoptium'),
    (Join-Path $env:ProgramFiles 'Java'),
    (Join-Path $env:LOCALAPPDATA 'Programs\Eclipse Adoptium'),
    (Join-Path $env:USERPROFILE '.jdks')
) | Where-Object { $_ -and (Test-Path $_) }

foreach ($root in $searchRoots) {
    Get-ChildItem -Path $root -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match '(^|[-_])17([._-]|$)|jdk-17|temurin-17' } |
        Sort-Object Name -Descending |
        ForEach-Object { $candidateHomes.Add($_.FullName) }
}

$selectedHome = $null
foreach ($candidateHome in $candidateHomes | Select-Object -Unique) {
    $javaExe = Join-Path $candidateHome 'bin\java.exe'
    if ((Get-JavaMajor $javaExe) -eq 17) {
        $selectedHome = $candidateHome
        break
    }
}

if (-not $selectedHome) {
    $pathJava = Get-Command java.exe -ErrorAction SilentlyContinue
    $pathVersion = if ($pathJava) {
        Get-JavaVersionLine $pathJava.Source
    } else {
        'java.exe not found in PATH'
    }

    throw @"
Cuicatl requires JDK 17 to run Gradle 8.13 / AGP 8.13.2.
No installed JDK 17 was found in the standard Windows locations checked.
Current PATH Java: $pathVersion

Install or expose a JDK 17 (Temurin recommended), or set JAVA_HOME to an existing JDK 17 and rerun.
"@
}

$env:JAVA_HOME = $selectedHome
$env:Path = "$(Join-Path $selectedHome 'bin');$env:Path"

$javaVersion = Get-JavaVersionLine (Join-Path $selectedHome 'bin\java.exe')
Write-Host "Cuicatl build JDK: $selectedHome"
Write-Host "Cuicatl Java: $javaVersion"

$wrapper = Join-Path (Split-Path -Parent $PSScriptRoot) 'gradlew.bat'
& $wrapper @GradleArgs
$exitCode = $LASTEXITCODE

if ($null -eq $exitCode) {
    $exitCode = if ($?) { 0 } else { 1 }
}

exit $exitCode
