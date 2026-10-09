# Phase 1 lifecycle closure evidence — 2026-10-09

Target: Samsung Galaxy J8 / SM-J810M / Android 10 (API 29)

## Build / install

Local Windows quality gate:

- make check: PASS
- testDebugUnitTest + lintDebug + assembleDebug: BUILD SUCCESSFUL
- make install-j8: PASS
- adb install -r: Success
- make open-j8: PASS

## Screen-off foreground-service check

Session: 1f6b4bd9-5123-453a-8552-92ef5b0bfe65

Observed helper output:

- complete rows before screen off: 1592
- complete rows while screen off: 1755
- complete rows after wake: 1792
- rows added during the measured off interval: 163
- same active session: true

Result: **PASS**. Capture/persistence continued while the J8 screen was off, with session identity preserved.

The 163-row increase is not interpreted as an exact 16.3-second screen-off duration because helper/ADB command overhead surrounds the nominal 15-second sleep. It is used only as positive evidence of continued capture/persistence.

## Activity recreation check

Same session: 1f6b4bd9-5123-453a-8552-92ef5b0bfe65

Observed helper output:

- rows before rotation: 2015
- rows in landscape: 2067
- rows after portrait: 2112
- rows at final check: 2133
- same active session: true

Result: **PASS**. Service-owned capture continued across forced landscape/portrait Activity recreation without changing the active session ID.

## Permission-denial check

The app was force-stopped, RECORD_AUDIO permission was revoked through ADB, and Cuicatl was relaunched.

After Start and user denial, the Meter remained inactive and displayed:

> Microphone permission denied. No capture was started.

The supplied device screenshot shows no active digital level/source/sample-rate capture and retains the Grant + Start action.

Result: **PASS**. Permission denial does not begin a measurement session.

## Background-noise / apparent gain adaptation observation

During the active lifecycle session, after the nearby foreground sound became quiet, the user observed the microphone path become more sensitive to a stationary room fan/background noise. The supplied short video captures the post-transition state, with the live RMS level around the low -40 dBFS range and a visibly moving background trace.

This behavior is **not** sufficient to prove that Android's AutomaticGainControl AudioEffect is active. Phase 0 reported:

- AGC platform availability: false
- NS platform availability: true
- AEC platform availability: true
- active source: VOICE_RECOGNITION

The observation is therefore recorded as **effective input-gain / noise-floor adaptation of unknown layer** until the actual AudioRecord session preprocessors are inspected.

This does not invalidate Phase 1 digital dBFS capture: Cuicatl is recording the samples Android provides and does not claim SPL. It is, however, a blocker for treating this J8 path as a stable absolute acoustic reference in Phase 2 until gain/processing stability is characterized.

Follow-up instrumentation now attaches to the active AudioRecord session, records actual controllable AGC/NS/AEC enabled state, requests controllable preprocessors disabled, and preserves that state in processing_state. Vendor/HAL/analog processing remains explicitly unknown.

## First acoustic task / setting

The first concrete acoustic stability setting is recorded as:

**Stationary room fan/background after a foreground sound falls silent.**

Purpose: observe whether the digital input floor remains stable after a large change in acoustic scene.

This is a capture-path stability task, not an SPL validation or calibration task.
