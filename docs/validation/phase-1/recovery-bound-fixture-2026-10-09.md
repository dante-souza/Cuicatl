# Phase 1 recovery-bound fixture — 2026-10-09

Source set supplied together by the user to avoid evidence overlap:

- WhatsApp Image 2026-10-09 at 08.07.46.jpeg — relaunch/recovery message
- WhatsApp Image 2026-10-09 at 08.07.58.jpeg — saved-session list
- WhatsApp Image 2026-10-09 at 08.08.14.jpeg — recovered-session detail
- phase1_recovery_bound-dc45ce81-c339-4c84-960e-8b86e8e8f852.csv — recovered export

The raw user files are not committed here.

## Visual evidence

The relaunch screen reports that one interrupted session record was recovered and explicitly states that capture did not continue while the process was absent.

The saved-session list identifies phase1_recovery_bound as:

- outcome: RECOVERED
- captured: 0:44.9
- frames: 449

The recovered-session detail identifies:

- outcome: RECOVERED
- captured: 0:44.9
- elapsed: unavailable after recovery
- source: VOICE_RECOGNITION
- sample rate: 48,000 Hz
- input: type=15 · SM-J810M
- clipping: 24 frames · 49 samples
- interruption: process_recovery

This validates the post-recovery UI correction that no longer fabricates elapsed duration from captured duration.

## CSV inspection

The recovered CSV contains:

- session label: phase1_recovery_bound
- session ID: dc45ce81-c339-4c84-960e-8b86e8e8f852
- schema marker: phase1-draft
- rows: 449
- sequence: contiguous 0 through 448
- session outcome: RECOVERED on every row
- interruption reason: process_recovery on every row
- captured duration: 44,900 ms
- session_elapsed_ms: empty on every row
- sample rate: 48,000 Hz
- sample format: PCM16_MONO
- timing quality: AUDIO_TIMESTAMP_MONOTONIC on all 449 rows
- signal state: NONZERO on all 449 rows
- intervals: 449 × 100 ms
- session_start_utc: 2026-10-09T11:05:34.856Z
- session_timezone_offset: -03:00

The sum of interval durations, final interval end, exported session_captured_ms, and the UI's 44.9-second captured value all agree exactly.

Independent numerical checks found:

- no sequence gap;
- no malformed row;
- every duration equals sample_count / sample_rate exactly;
- rms_fs squared agrees with mean_square_fs to floating-point precision;
- rms_dbfs agrees with 10 * log10(mean_square_fs);
- sample_peak_dbfs agrees with 20 * log10(sample_peak_fs);
- every interval_start_utc_estimate equals session_start_utc + elapsed_start_ms exactly.

## Clipping

The CSV independently agrees with the saved-detail UI:

- clipped frames: 24
- clipped endpoint samples: 49
- frames at exactly 0.0 dBFS sample peak: 5
- maximum clipped samples in one frame: 7

## Preservation-bound result

The instrumented J8 helper reported:

- state after recovery: SAVED
- outcome: RECOVERED
- interruption reason: process_recovery
- pre-kill complete rows visible to the helper: 446
- recovered complete rows: 449
- known complete-row loss: 0

All 446 complete rows visible at the pre-kill snapshot survived recovery.

The recovered file contains three additional complete 100 ms frames. Those frames completed during the small race window between the helper's pre-kill row-count snapshot and the subsequent ADB force-stop command. They are valid additional persisted data; they must not be described as negative loss.

Observed process-death preservation result for this run: **0 complete persisted rows lost from the measured pre-kill snapshot**.

This result applies to the tested Android process-death path. It is not a claim of zero loss under sudden power removal or arbitrary storage failure, and it does not guarantee preservation of a partially written/in-flight frame that was not yet a complete persisted row at the snapshot.

## Decision

**Recovered-state UI semantics: PASS.**

**Recovered CSV semantics: PASS.**

**Unknown elapsed-time correction: PASS on device and export.**

**Readable recovered prefix: PASS (449 contiguous frames / 44.9 s).**

**Exact observed process-death complete-row loss from the instrumented pre-kill snapshot: 0 rows in this J8 run.**
