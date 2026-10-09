# Phase 1 AGC default / fan fixture — 2026-10-09

Source set supplied together by the user:

- device screenshot showing the pre-session Android AGC control during the active `agcdeffan` session;
- `agcdeffan-2bbde380-3428-4a65-9c1a-a9350941418d.csv`.

The raw user files are not committed here.

## Session identity

- label: `agcdeffan`
- session ID: `2bbde380-3428-4a65-9c1a-a9350941418d`
- device: Samsung SM-J810M / Android 10
- source: `VOICE_RECOGNITION`
- sample format: PCM16 mono
- sample rate: 48,000 Hz
- outcome: COMPLETED
- rows: 879
- sequence: contiguous 0 through 878
- captured duration: 87,880 ms
- elapsed duration: 88,029 ms
- timing quality: `AUDIO_TIMESTAMP_MONOTONIC` on all rows
- signal state: `NONZERO` on all rows

The sum of interval durations and final interval end both agree exactly with `session_captured_ms=87880`.

## Standard Android processing audit

The persisted `processing_state` is:

```text
agc_request=DEFAULT; AGC=unavailable; ns_request=FORCE_OFF; NS=available,control=true,before=false,apply_result=0,after=false; aec_request=FORCE_OFF; AEC=available,control=true,before=false,apply_result=0,after=false; vendor_or_hardware_processing=unknown
```

Interpretation:

- requested Android AGC mode: DEFAULT;
- standard Android `AutomaticGainControl`: unavailable;
- standard Android NoiseSuppressor: available, Cuicatl had control, disabled before and after the request;
- standard Android AcousticEchoCanceler: available, Cuicatl had control, disabled before and after the request;
- vendor/HAL/analog or other microphone-path processing remains unknown.

Therefore this J8 cannot execute a meaningful standard-API `FORCE_OFF` vs `FORCE_ON` AGC comparison. The disabled Off/On controls shown in the screenshot are correct behavior.

## Fan/background response

The acoustic sequence contains a stable initial fan/background region, a foreground transient region, then a return to fan/background.

A robust comparison of fixed windows found:

### Initial fan/background — 2.0 s to 15.0 s

- median RMS: **-41.37 dBFS**
- mean RMS: -41.32 dBFS
- median sample peak: -29.81 dBFS
- no clipped frames

### Early post-foreground fan/background — 25.0 s to 35.0 s

- median RMS: **-39.64 dBFS**
- mean RMS: -39.55 dBFS
- median sample peak: -28.04 dBFS
- no clipped frames

Relative to the initial fan/background, the early post-foreground window is approximately:

- **+1.73 dB RMS median**
- **+1.77 dB sample-peak median**

### Later fan/background — 40.0 s to 50.0 s

- median RMS: **-41.03 dBFS**
- mean RMS: -41.01 dBFS
- median sample peak: -29.47 dBFS

This is only about **+0.35 dB RMS median** above the initial baseline.

### Late session — 70.0 s to 85.0 s

- median RMS: **-41.50 dBFS**
- mean RMS: -41.48 dBFS
- median sample peak: -30.05 dBFS

The trace therefore shows a measurable temporary increase in the delivered digital fan/background level after the foreground event, followed by a gradual return toward the original baseline. Using a 2-second rolling-median criterion of within +0.5 dB of the initial baseline, the trace first returns to that neighborhood at approximately **38.1 s**.

The foreground activity contains clipping. Across the complete session:

- clipped frames: 12
- clipped samples: 890
- maximum sample peak: 0.0 dBFS

The post-foreground comparison windows themselves contain no clipping.

## Interpretation boundary

This fixture demonstrates **adaptive-looking behavior in the PCM level delivered to Cuicatl**, but it does not by itself prove an electronic gain change.

The standard Android AGC effect cannot explain this fixture as a controllable effect because it is reported unavailable. NS and AEC were both verified disabled through the standard effect API.

Remaining possible contributors include:

- vendor/HAL microphone processing;
- analog microphone/front-end gain behavior;
- other undocumented processing;
- physical acoustic settling or small environmental changes.

Cuicatl therefore records the observation as **effective input-level / noise-floor adaptation of unknown layer**, not as confirmed AGC.

## Phase decision

**AGC control provenance: PASS.**

**J8 standard Android AGC availability: unavailable.**

**NS/AEC standard effects verified disabled: PASS.**

**Temporary post-foreground fan/background elevation: observed and quantified.**

This does not invalidate Phase 1 digital PCM/dBFS capture. It does mean Phase 2 must not treat the J8 `VOICE_RECOGNITION` path as a proven stable absolute acoustic reference without additional characterization or a different validated input path.
