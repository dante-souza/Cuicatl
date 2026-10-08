# SPDX-License-Identifier: AGPL-3.0-or-later
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $GradleArgs
)

$ErrorActionPreference = 'Stop'

function Get-JavaMajor([string] $JavaExe) {
    if (-not (Test-Path $JavaExe)) {
        return $null
    }

    $firstLine = (& $JavaExe -version 2>&1 | Select-Object -First 1).ToString()
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
        (& $pathJava.Source -version 2>&1 | Select-Object -First 1).ToString()
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

$javaVersion = (& (Join-Path $selectedHome 'bin\java.exe') -version 2>&1 | Select-Object -First 1).ToString()
Write-Host "Cuicatl build JDK: $selectedHome"
Write-Host "Cuicatl Java: $javaVersion"

$wrapper = Join-Path (Split-Path -Parent $PSScriptRoot) 'gradlew.bat'
& $wrapper @GradleArgs
$exitCode = $LASTEXITCODE

if ($null -eq $exitCode) {
    $exitCode = if ($?) { 0 } else { 1 }
}

exit $exitCode
