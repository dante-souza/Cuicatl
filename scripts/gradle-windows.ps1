# SPDX-License-Identifier: AGPL-3.0-or-later
param(
    [switch] $Doctor,
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

if ($Doctor) {
    Write-Host 'Cuicatl JDK doctor (read-only; scoped to this process)'
    Write-Host "JAVA_HOME: $env:JAVA_HOME"
    $active = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($active) {
        Write-Host "PATH java: $($active.Source)"
        Write-Host "PATH version: $(Get-JavaVersionLine $active.Source)"
    } else {
        Write-Host 'PATH java: not found'
    }
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

if ($Doctor) {
    if ($selectedHome) {
        Write-Host "Selected build JDK: $selectedHome"
        Write-Host "Selected version: $(Get-JavaVersionLine (Join-Path $selectedHome 'bin\\java.exe'))"
        Write-Host 'Result: JDK 17 available; run make jdk-repair to stop daemons, verify and build.'
        exit 0
    }
    Write-Error 'Result: JDK 17 not found; install Temurin 17 or set JAVA_HOME to an existing JDK 17.'
    exit 1
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

$sdkCandidates = New-Object System.Collections.Generic.List[string]

if ($env:ANDROID_HOME) {
    $sdkCandidates.Add($env:ANDROID_HOME.Trim('"'))
}

if ($env:ANDROID_SDK_ROOT) {
    $sdkCandidates.Add($env:ANDROID_SDK_ROOT.Trim('"'))
}

if ($env:LOCALAPPDATA) {
    $sdkCandidates.Add((Join-Path $env:LOCALAPPDATA 'Android\Sdk'))
}

$sdkCandidates.Add((Join-Path $env:USERPROFILE 'AppData\Local\Android\Sdk'))

$selectedSdk = $null
foreach ($candidateSdk in $sdkCandidates | Select-Object -Unique) {
    if (-not (Test-Path $candidateSdk)) {
        continue
    }

    $platformPath = Join-Path $candidateSdk 'platforms\android-36'
    if (Test-Path $platformPath) {
        $selectedSdk = $candidateSdk
        break
    }
}

if (-not $selectedSdk) {
    $existingSdks = @(
        $sdkCandidates |
            Select-Object -Unique |
            Where-Object { Test-Path $_ }
    )

    if ($existingSdks.Count -gt 0) {
        $details = 'Android SDK directories were found, but none contains platforms\android-36:' +
            [Environment]::NewLine + ' - ' +
            ($existingSdks -join ([Environment]::NewLine + ' - '))
    } else {
        $details = 'No Android SDK directory was found in ANDROID_HOME, ANDROID_SDK_ROOT, or the standard Windows user location.'
    }

    throw @"
Cuicatl requires Android SDK Platform 36 for compileSdk 36.
$details

Install Android SDK Platform 36 in Android Studio SDK Manager, or expose an existing SDK through ANDROID_HOME / ANDROID_SDK_ROOT.
"@
}

$env:ANDROID_HOME = $selectedSdk
$env:ANDROID_SDK_ROOT = $selectedSdk

Write-Host "Cuicatl Android SDK: $selectedSdk"
Write-Host "Cuicatl Android platform: $(Join-Path $selectedSdk 'platforms\android-36')"

$wrapper = Join-Path (Split-Path -Parent $PSScriptRoot) 'gradlew.bat'
& $wrapper @GradleArgs
$exitCode = $LASTEXITCODE

if ($null -eq $exitCode) {
    $exitCode = if ($?) { 0 } else { 1 }
}

exit $exitCode
