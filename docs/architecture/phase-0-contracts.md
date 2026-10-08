# Phase 0 — foundation and capture-probe contracts

Date: 2026-10-08. Planning baseline: Revision 2.

This document records the implementation contract for Phase 0A/0B. It does not claim calibrated acoustic accuracy, a final export schema, or a release-ready persistence design.

## Phase 0A foundation

- Application ID / namespace: `io.github.dante_souza.cuicatl`
- Minimum Android API: 26
- Compile / target API: 36
- JDK 17
- Gradle 8.13
- Android Gradle Plugin 8.13.2
- Kotlin 2.3.21
- Compose BOM 2026.09.00
- Original Cuicatl source files use `SPDX-License-Identifier: AGPL-3.0-or-later`.

The existing Android bootstrap is retained under Planning Revision 2.

CI rejected the initially selected Compose BOM 2026.09.00 / Core 1.19.1 because those releases require compileSdk 37 and AGP 9. The foundation therefore pins Compose BOM 2026.04.01, Activity Compose 1.12.4, and Core KTX 1.17.0 while retaining compileSdk 36 / AGP 8.13.2. This is an evidence-driven compatibility correction, not a downgrade of target-device support.

## Initial session contract

The initial lifecycle is:

`READY → STARTING → RUNNING → FINALIZING → SAVED`

A failed Start returns to `READY` with an explicit readiness reason. Saved outcomes are `COMPLETED`, `INTERRUPTED`, or `RECOVERED`.

v0.1.0 begins with Start/Stop and one fixed input/analysis configuration per session. Pause/resume and multi-segment aggregation are later capabilities. An input route, weighting, or reference-adjustment change ends the current measurement and requires a new session.

The UI is not the future authoritative capture owner or persistence layer. Phase 0B is intentionally a bounded diagnostic path, not the final session service.

## Readiness and explicit start

Opening Cuicatl never requests microphone permission and never starts recording.

The user must press **Start probe**. If permission is missing, that action launches the Android runtime permission request. A granted request continues the same explicit user action into the diagnostic probe. Denial creates no capture.

## Phase 0B J8 diagnostic probe

The diagnostic probe is bounded to at most 10 seconds per run and tries mono PCM16 configurations in this order:

1. `UNPROCESSED` at 48 kHz, then 44.1 kHz, but only when Android advertises unprocessed-source support.
2. `VOICE_RECOGNITION` at 48 kHz, then 44.1 kHz.
3. `MIC` at 48 kHz, then 44.1 kHz.

The first initialized configuration is used. The probe reports:

- requested/selected source and actual `AudioRecord.sampleRate`;
- attempted source/rate combinations;
- sample count and monotonic elapsed duration;
- current block RMS and sample peak in dBFS;
- routed input reported by Android;
- `AudioTimestamp` availability using the monotonic timebase, or explicit fallback observation;
- platform availability of AEC, AGC, and noise suppression.

Processing availability is **not** evidence that a particular effect is active or inactive in the vendor microphone path. A successful source/rate initialization is **not** evidence of physical microphone bandwidth or acoustic accuracy.

Probe snapshots are also emitted to logcat with tag `CuicatlAudioProbe` so the device run can be preserved as evidence.

## Digital measurement convention

For normalized PCM16 samples `x[n]` using divisor 32768:

- `mean_square_fs = sum(x[n]^2) / N`
- `rms_fs = sqrt(mean_square_fs)`
- `rms_dbfs = 10 * log10(mean_square_fs)`
- sample peak dBFS uses `20 * log10(max(abs(x[n])))`
- exact digital zero has no finite logarithmic value
- missing input is not converted to silence

The 32768 divisor preserves the signed PCM16 negative endpoint at −1.0 while the positive endpoint is slightly below +1.0; later implementation specifications must retain or explicitly revise this convention.

A full-scale sine is approximately −3.0103 dBFS RMS under the established numerical convention.

For later calibrated levels:

`Leq = 10 * log10(sum(t_i * 10^(L_i/10)) / sum(t_i))`

An equal-duration 60 dB + 80 dB fixture is approximately 77.0329 dB, not 70 dB.

## Timing contract

Phase 0B checks whether `AudioRecord.getTimestamp(..., TIMEBASE_MONOTONIC)` is available on the actual J8 path. Monotonic `elapsedRealtime` is reported as the diagnostic fallback observation.

Phase 1 will convert the probe findings into the durable capture-time policy. No Compose delivery timestamp is treated as sample capture time.

## Export layout is provisional

There is deliberately no frozen schema version 1 in Phase 0.

`ProvisionalExportFields` records candidate context/measurement fields so the first real capture/export fixture is not designed from memory. Exact names, repeated metadata, packaging, spreadsheet-safe text rules, and schema version 1 are finalized only after Phase 1 captures, exports, and independently inspects real data.

The initial session model has no public multi-segment field.

## Persistence remains undecided

Phase 0 does not choose Room/SQLite or an append-file format. Phase 1 performs a bounded preservation/recovery experiment and selects the smallest design that passes save, reopen, export, interruption, and readable-prefix requirements.

## Presentation contract

Phase 0 exposes Meter and History navigation, but only Meter contains live diagnostic data. History explicitly states that Phase 0B does not persist the public history model.

No Phase 0 display presents dBFS as SPL.
