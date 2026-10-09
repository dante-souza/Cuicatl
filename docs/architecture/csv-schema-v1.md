# Cuicatl measurement CSV — Schema 1

Date frozen: 2026-10-09

Status: **frozen for Phase 1 / initial release baseline**

Schema marker: `1`

## Scope

Schema 1 is the portable row-oriented measurement export for the first usable Cuicatl session workflow.

It was frozen only after independent inspection of:

- a normal completed J8 session;
- a second completed session carrying the corrected timing provenance;
- a forced-process-death RECOVERED session;
- the corrected recovered-session null semantics for unknown elapsed duration.

Schema 1 contains measurement frames and enough repeated context to interpret the CSV without a companion metadata file. It does not preserve sample-by-sample audio and therefore cannot support later waveform replay or arbitrary FFT reanalysis.

## Serialization

- UTF-8
- comma delimiter
- header row
- decimal point
- standard CSV quoting for commas, quotes, and line breaks
- empty field means unavailable / not applicable
- numeric zero remains numeric zero
- no NaN or infinity as ordinary measurements
- user-controlled session labels receive reversible spreadsheet-formula-prefix protection before CSV quoting

Breaking changes require a new schema version.

## Timing semantics

- `session_start_utc`: independent UTC anchor recorded when the session is started.
- `session_timezone_offset`: original local UTC offset at session start.
- `elapsed_start_ms`: monotonic/sample-derived start offset of the measurement frame.
- `interval_start_utc_estimate`: `session_start_utc + elapsed_start_ms`; it is an externally useful estimate, not a replacement for monotonic capture ordering.
- `duration_ms`: actual interval coverage derived from sample count and sample rate.
- `session_captured_ms`: valid persisted capture coverage.
- `session_elapsed_ms`: observed elapsed duration when known. It is empty for process-recovered sessions because the true final elapsed time at process death was not observed.
- `timing_quality`: provenance of the capture timing path, such as `AUDIO_TIMESTAMP_MONOTONIC`.

Missing time is never converted to zero duration.

## Digital measurement semantics

For normalized PCM16 samples under the established Phase 0/1 convention:

- `mean_square_fs` = mean of squared normalized samples;
- `rms_fs` = square root of mean square;
- `rms_dbfs` = `10 * log10(mean_square_fs)` when energy is nonzero;
- `sample_peak_fs` = maximum absolute normalized sample;
- `sample_peak_dbfs` = `20 * log10(sample_peak_fs)` when peak is nonzero.

Exact digital zero retains zero linear energy with `signal_state=DIGITAL_ZERO`; unavailable logarithmic values remain empty.

`clipped_sample_count` counts PCM16 endpoint samples in the interval. A nonzero count is accompanied by the `CLIPPED` quality flag.

## Frequency weighting and SPL fields

Phase 1 is digital-only.

- `frequency_weighting` is `NONE_DIGITAL`.
- reference-adjustment fields are empty.
- estimated SPL fields are empty.
- cumulative Leq SPL estimate is empty.

These columns exist so a later schema-compatible producer may populate them only when the Phase 2 reference and numerical prerequisites are satisfied. Their presence in Schema 1 does not imply that Phase 1 measures calibrated SPL.

## Session outcomes

Expected initial outcomes include:

- `COMPLETED` — normal finalization;
- `RECOVERED` — readable persisted prefix recovered after process interruption.

For a recovered process-death record:

- `interruption_reason=process_recovery`;
- `session_captured_ms` reports the recovered readable coverage;
- `session_elapsed_ms` is empty unless independently known.

A recovered record must never imply that capture continued while the process was absent.

## Schema 1 columns

In order:

1. schema_version
2. session_id
3. sequence
4. session_label
5. app_version
6. device_model
7. android_version
8. session_outcome
9. session_start_utc
10. session_timezone_offset
11. interval_start_utc_estimate
12. elapsed_start_ms
13. duration_ms
14. timing_quality
15. input_identity
16. audio_source
17. sample_rate_hz
18. sample_format
19. sample_count
20. processing_state
21. mean_square_fs
22. rms_fs
23. sample_peak_fs
24. rms_dbfs
25. sample_peak_dbfs
26. frequency_weighting
27. reference_adjustment_id
28. reference_adjustment_version
29. reference_method
30. reference_level_db
31. reference_correction_db
32. interval_level_db_spl_est
33. cumulative_leq_db_spl_est
34. clipped_sample_count
35. signal_state
36. quality_flags
37. session_elapsed_ms
38. session_captured_ms
39. interruption_reason

## Validation evidence

Phase 1 validation established:

- contiguous sequence and row reconstruction for completed and recovered sessions;
- exact agreement between sample count, sample rate, and interval duration;
- independent recalculation of RMS and peak dBFS values;
- exact interval UTC-estimate progression from the recorded session anchor;
- matching clipping counts/flags between live/saved presentation and exported frames;
- recovered-session outcome and interruption provenance;
- empty recovered elapsed duration after the semantic correction.

The validation evidence remains under `docs/validation/phase-1/`.
