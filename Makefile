SHELL := /bin/sh

ifeq ($(OS),Windows_NT)
GRADLE := powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/gradle-windows.ps1
JDK_DOCTOR := powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/gradle-windows.ps1 -Doctor
else
GRADLE := ./gradlew
JDK_DOCTOR := java -version
endif

.PHONY: check build-debug jdk-doctor gradle-stop gradle-verify jdk-repair install-j8 open-j8 j8-preflight probe-log-j8 phase1-recovery-kill-j8 phase1-screenoff-j8 phase1-recreate-j8 phase1-pages-j8 phase1-freeze-info-j8

# Diagnose the launching JDK without changing the caller's environment.
jdk-doctor:
	$(JDK_DOCTOR)

# All Gradle-based targets use the JDK 17-selecting Windows launcher.
gradle-stop:
	$(GRADLE) --stop

gradle-verify:
	$(GRADLE) --version

# Ordered, fail-fast recovery: diagnose -> stop -> verify -> assemble.
# This does not modify the parent PowerShell environment; use make targets for builds.
jdk-repair:
	$(MAKE) jdk-doctor
	$(MAKE) gradle-stop
	$(MAKE) gradle-verify
	$(MAKE) build-debug

check:
	$(GRADLE) --no-daemon testDebugUnitTest lintDebug assembleDebug

build-debug:
	$(GRADLE) --no-daemon assembleDebug

j8-preflight:
	powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/preflight-j8.ps1

install-j8:
	powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/preflight-j8.ps1
	$(GRADLE) --no-daemon assembleDebug
	powershell.exe -NoProfile -Command "if (-not (Test-Path 'app/build/outputs/apk/debug/app-debug.apk')) { Write-Error 'Debug APK missing after build.'; exit 1 }"
	adb -s 38c19745 install -r app/build/outputs/apk/debug/app-debug.apk

open-j8: j8-preflight
	adb -s 38c19745 shell monkey -p io.github.dante_souza.cuicatl -c android.intent.category.LAUNCHER 1

probe-log-j8: j8-preflight
	adb -s 38c19745 logcat -v time CuicatlAudioProbe:I "*:S"


phase1-recovery-kill-j8: j8-preflight
	powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/phase1-recovery-kill-j8.ps1


phase1-screenoff-j8: j8-preflight
	powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/phase1-lifecycle-j8.ps1 -Mode ScreenOff

phase1-recreate-j8: j8-preflight
	powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/phase1-lifecycle-j8.ps1 -Mode Recreate


phase1-pages-j8: j8-preflight
	powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/phase1-lifecycle-j8.ps1 -Mode Pages


phase1-freeze-info-j8: j8-preflight
	powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/phase1-freeze-info-j8.ps1
