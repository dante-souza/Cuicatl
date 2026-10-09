# Phase 1 first exported-session inspection — 2026-10-09

Source: user-supplied exported CSV from the first J8 Phase 1 session. The raw user fixture is not committed here.

## Session result

- device: Samsung SM-J810M / Android 10
- source: VOICE_RECOGNITION
- sample format: PCM16 mono
- sample rate: 48,000 Hz
- outcome: COMPLETED
- rows: 1,547 measurement frames
- sequence: contiguous 0 through 1,546
- captured duration: 154,660 ms
- elapsed duration: 154,832 ms
- elapsed minus captured: 172 ms
- intervals: 1,546 × 100 ms plus one final 60 ms partial interval
- timing quality: AUDIO_TIMESTAMP_MONOTONIC on all 1,547 rows
- signal state: NONZERO on all rows

The sum of interval durations, the final interval end, and exported session_captured_ms all agree at exactly 154,660 ms.

## Numerical consistency

Independent recalculation found:

- rms_fs squared agrees with mean_square_fs to floating-point precision;
- rms_dbfs agrees with 10 * log10(mean_square_fs);
- sample_peak_dbfs agrees with 20 * log10(sample_peak_fs);
- every interval duration agrees exactly with sample_count / sample_rate;
- interval UTC progression agrees with elapsed_start_ms;
- unavailable reference-adjustment / SPL fields remain empty rather than being fabricated as zero.

No sequence gap or malformed measurement row was observed.

## Clipping evidence

Clipping was not a cosmetic false positive:

- 130 frames contained one or more endpoint samples;
- 83,349 clipped samples were recorded in total;
- 27 clipping bursts were observed;
- 108 frames reached exactly 0.0 dBFS sample peak;
- the largest single 100 ms frame contained 1,845 clipped samples.

The first implementation changed the status message every 100 ms based only on the latest frame, which made the warning blink rapidly for isolated clipped frames. This is a presentation defect, not a measurement defect.

Correction: keep the latest-frame clipped-sample count, add cumulative session clipping counts, and keep the ordinary capture status stable. Saved-session detail also reports cumulative clipping derived from the persisted frames.

## Draft-schema inspection result

The first real export validates the general row model and serialization, but schema 1 should not be frozen from this fixture unchanged.

A timing-provenance omission was found: the CSV carried derived interval UTC timestamps but did not explicitly export the independent session UTC anchor or the original timezone offset required by the planning contract.

The next draft adds:

- session_start_utc
- session_timezone_offset
- interval_start_utc_estimate

The prior interval_start_utc name is intentionally revised while the schema is still marked phase1-draft.

## Decision

**Normal-stop capture / reopen / CSV export path: PASS for this fixture.**

**Schema 1: NOT YET FROZEN.**

Remaining Phase 1 blockers include the forced-process-death recovery experiment, observed preservation/loss bound, append-vs-SQLite/Room decision, and a second exported fixture after the timing-provenance correction.
