# Phase 1 second exported-session inspection — 2026-10-09

Source: user-supplied exported CSV named clippingchecks-1cd1ed64-0d67-4933-a429-c707ab02513b.csv. The raw user fixture is not committed here.

## Purpose

This second fixture validates the timing-provenance correction made after the first independent export inspection.

## Session result

- label: clippingchecks
- device: Samsung SM-J810M / Android 10
- source: VOICE_RECOGNITION
- sample format: PCM16 mono
- sample rate: 48,000 Hz
- outcome: COMPLETED
- rows: 552 measurement frames
- sequence: contiguous 0 through 551
- captured duration: 55,120 ms
- elapsed duration: 55,284 ms
- elapsed minus captured: 164 ms
- intervals: 551 × 100 ms plus one final 20 ms partial interval
- timing quality: AUDIO_TIMESTAMP_MONOTONIC on all 552 rows
- signal state: NONZERO on all rows

The sum of interval durations, final interval end, and exported session_captured_ms all agree at exactly 55,120 ms.

## Timing provenance correction

The corrected draft export now includes and consistently populates:

- session_start_utc = 2026-10-09T10:26:48.959Z
- session_timezone_offset = -03:00
- interval_start_utc_estimate

Every interval_start_utc_estimate equals session_start_utc + elapsed_start_ms exactly for this fixture.

This closes the timing-provenance omission found in the first exported fixture.

## Numerical consistency

Independent recalculation found:

- rms_fs squared agrees with mean_square_fs to floating-point precision;
- rms_dbfs agrees with 10 * log10(mean_square_fs);
- sample_peak_dbfs agrees with 20 * log10(sample_peak_fs);
- every interval duration agrees exactly with sample_count / sample_rate;
- unavailable reference-adjustment / SPL fields remain empty rather than being fabricated as zero.

No sequence gap or malformed row was observed.

## Clipping evidence

This session contains:

- 15 clipped frames;
- 3,593 clipped endpoint samples;
- 14 frames reaching exactly 0.0 dBFS sample peak;
- maximum 980 clipped samples in one 100 ms frame.

The exported clipped_sample_count and CLIPPED quality flag agree on all affected rows.

## Visual-evidence note

The screenshots/video supplied alongside this CSV show the earlier first-session UI state and the old latest-100-ms blinking warning. They are not used as evidence that the cumulative clipping-status UI patch is visible on-device.

## Decision

**Second normal-stop export fixture: PASS.**

**Timing provenance correction: PASS.**

**Schema 1: NOT YET FROZEN.**

The next required fixture is an interrupted/recovered session from the forced-process-death experiment. That fixture should exercise the recovery outcome and interruption fields before schema 1 is frozen.
