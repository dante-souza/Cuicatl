# Phase 1 process-kill recovery inspection — 2026-10-09

Source: user-supplied recovered CSV named phase1_process_kill-f908f833-5f34-439f-b05d-bfe7a62b2e3c.csv. The raw user fixture is not committed here.

## Recovery result

- label: phase1_process_kill
- session outcome: RECOVERED
- interruption reason: process_recovery
- recovered rows: 462
- sequence: contiguous 0 through 461
- recovered captured duration: 46,200 ms
- recovered elapsed duration recorded by recovery: 46,200 ms
- intervals: 462 × 100 ms
- source: VOICE_RECOGNITION
- sample format: PCM16 mono
- sample rate: 48,000 Hz
- timing quality: AUDIO_TIMESTAMP_MONOTONIC on all 462 rows
- signal state: NONZERO on all rows

The recovered CSV contains a fully readable prefix with no malformed row and no sequence gap.

## Numerical and timing integrity

Independent recalculation found:

- sum(duration_ms) = 46,200 ms;
- final elapsed_start_ms + duration_ms = 46,200 ms;
- session_captured_ms = 46,200 ms;
- every duration agrees with sample_count / sample_rate;
- rms_fs² agrees with mean_square_fs to floating-point precision;
- rms_dbfs agrees with 10 * log10(mean_square_fs);
- sample_peak_dbfs agrees with 20 * log10(sample_peak_fs);
- interval_start_utc_estimate equals session_start_utc + elapsed_start_ms exactly.

The corrected timing-provenance fields are present:

- session_start_utc = 2026-10-09T10:47:09.852Z
- session_timezone_offset = -03:00

## Clipping evidence

The recovered prefix contains:

- 23 clipped frames;
- 50 clipped endpoint samples;
- 7 frames reaching exactly 0.0 dBFS sample peak;
- maximum 4 clipped samples in one frame.

The CLIPPED quality flag agrees with clipped_sample_count on the affected rows.

## Recovery elapsed-time correction

This fixture exposed one semantic defect: recovery did not know the actual final elapsed time at process death, but the implementation copied the recovered captured duration into session_elapsed_ms.

That value is not independently observed elapsed time and therefore must not be presented as such. The implementation is corrected so a recovered session leaves elapsed duration unavailable unless it was actually observed during normal finalization. Captured duration remains the valid 46,200 ms readable-prefix coverage.

## What this fixture proves

The process-kill recovery semantics pass:

- an unfinished RUNNING record is recovered as SAVED;
- outcome is RECOVERED, not COMPLETED;
- interruption_reason is process_recovery;
- the recovered measurement prefix remains contiguous and numerically valid;
- no missing data are fabricated as silence.

## What this fixture does not prove

The exact number of complete frames present immediately before force-stop was not captured in a machine-readable pre-kill snapshot. The supplied images were mixed with earlier runs, so they are intentionally not used to infer a precise loss figure.

Therefore this fixture does **not** establish an exact process-death frame-loss bound.

A dedicated J8 helper, scripts/phase1-recovery-kill-j8.ps1 (Make target phase1-recovery-kill-j8), now snapshots the complete persisted row count immediately before force-stop and compares it with the recovered row count. The next recovery run should use that helper to close the preservation-bound item without depending on screenshots.

## Decision

**Process death → RECOVERED semantics: PASS.**

**Readable-prefix recovery: PASS.**

**Exact observed frame-loss bound: PENDING one instrumented retry.**

Schema 1 remains provisional until the recovery/preservation decision is closed.
