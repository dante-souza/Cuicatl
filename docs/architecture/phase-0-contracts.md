# Phase 0 — foundation contracts

Date: 2026-10-08

This document turns the Phase 0 planning baseline into implementation contracts. It does not claim microphone capture or acoustic accuracy.

## Android identity and compatibility

- Application ID / namespace: `io.github.dante_souza.cuicatl`
- Minimum Android API: 26
- Compile / target API: 36
- JDK: 17
- Gradle: 8.13
- Android Gradle Plugin: 8.13.2
- Kotlin: 2.3.21
- Compose BOM: 2026.09.00
- Original Cuicatl source files use `SPDX-License-Identifier: AGPL-3.0-or-later`.

The Samsung Galaxy J8 (Android 10 / API 29) remains the Phase 0 physical-device gate.

## Session contract

A session has one identity and moves through `READY → STARTING → RUNNING → FINALIZING → SAVED`; the persisted model also admits `PAUSED` for the later pause/resume increment.

A saved session outcome is explicit: `COMPLETED`, `INTERRUPTED`, or `RECOVERED`. A lifecycle interruption must never be presented as a clean completion.

The UI is not the capture owner or authoritative data store. Page changes and recomposition must not create or reset sessions.

## Readiness contract

Readiness is a reasoned domain state rather than a UI boolean. Phase 0 defines reasons for permission, unavailable microphone, unsupported configuration, platform restrictions, and unknown failure. Phase 1 will provide the Android adapter that resolves these states.

Opening Cuicatl does not begin microphone capture. The Phase 0 Start control is intentionally disabled.

## Digital measurement convention

For normalized PCM samples `x[n]`:

- `mean_square_fs = sum(x[n]^2) / N`
- `rms_fs = sqrt(mean_square_fs)`
- `rms_dbfs = 10 * log10(mean_square_fs)`
- exact digital zero has no finite dBFS value and is represented by an empty logarithmic field plus `DIGITAL_ZERO`
- missing input is `MISSING`; it is not converted to zero
- invalid data is `INVALID`

Under this convention, a full-scale sine is approximately −3.0103 dBFS RMS. The executable numerical tests freeze that behavior.

For calibrated levels, equivalent level must use duration-weighted energy:

`Leq = 10 * log10(sum(t_i * 10^(L_i/10)) / sum(t_i))`

An equal-duration 60 dB + 80 dB fixture therefore yields approximately 77.0329 dB, not 70 dB.

## Timing contract

Phase 1 will use monotonic timing for capture duration and sequence ordering, plus an independent UTC session anchor for export. A measurement frame carries an elapsed start, actual duration, sample count, and sample rate. Missing or interrupted time becomes a gap/event rather than a fabricated frame.

## Export contract

CSV schema version 1 is frozen by `CsvSchema` and the planning document. Essential fields include session/segment identity, timing, configured sample rate, digital energy/RMS/peak, calibration/weighting context, validity, clipping count, and quality flags.

Phase 0 freezes names and semantics. Phase 1 implements serialization, saved sessions, Android sharing, and independent reconstruction fixtures.

## Presentation contract

Phase 0 exposes only Meter and History. Neither screen displays fake sensor data. The shell explicitly states that no measurement exists and that automatic capture is disabled. Later analysis pages are added only when their underlying capability exists.
