SHELL := /bin/sh

ifeq ($(OS),Windows_NT)
GRADLE := powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/gradle-windows.ps1
else
GRADLE := ./gradlew
endif

.PHONY: check build-debug install-j8 open-j8 j8-preflight probe-log-j8

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
