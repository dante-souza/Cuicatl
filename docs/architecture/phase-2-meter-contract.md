# Phase 2 meter and reference-adjustment contract

Date: 2026-10-09  
Branch: `feature/phase-2-meter-reference-adjustment`

## Purpose

Phase 2 turns the Phase 1 digital observation pipeline into a defined meter pipeline while preserving the distinction between digital correctness and physical acoustic accuracy.

The implementation must support:

- fixed A or Z frequency weighting for one session;
- current, minimum, maximum, and energy-equivalent level (Leq);
- one active reference-adjustment configuration initially;
- an immutable snapshot of that configuration in each applicable session;
- explicit suppression of estimated SPL when the active capture configuration does not match the reference adjustment;
- the existing CSV Schema 1 reserved weighting/reference/SPL fields without a breaking schema change.

## Evidence states

The UI and export use three descriptive states:

1. **Uncalibrated** — digital weighted levels are available, but no matching reference adjustment is active.
2. **Reference-adjusted estimate** — a documented adjustment matches the fixed session input/configuration and can be applied.
3. **Profile mismatch** — an adjustment exists, but one or more required configuration fields differ. Estimated SPL is unavailable.

A numeric offset is not evidence of IEC class compliance, microphone flatness, full dynamic-range accuracy, or suitability for regulatory measurements.

## Fixed session configuration

Frequency weighting and any reference adjustment are chosen before Start and frozen for the session. A reference adjustment is compatible only when all of these match:

- device model;
- input identity;
- Android audio source;
- sample rate;
- sample format;
- frequency weighting.

A detected input route change already terminates the Phase 1 capture and remains an interruption in Phase 2.

## Reference-adjustment arithmetic

For a documented reference level and the matching measured digital level:

`correction_dB = reference_level_dB_SPL - measured_reference_level_dBFS`

For any compatible finite interval level:

`estimated_SPL_dB = weighted_interval_level_dBFS + correction_dB`

The correction is constant in dB. Therefore the same correction can shift current/minimum/maximum/Leq after those values have been correctly calculated from the selected weighted signal. It must not be used to claim frequency-response correction.

## Statistics

Statistics are calculated from valid captured intervals under one fixed weighting/configuration.

- **Current**: latest completed interval with a finite logarithmic level.
- **Minimum/maximum**: extrema of finite valid interval levels.
- **Leq**: energy-equivalent level over valid captured duration.
- **Digital zero**: contributes zero energy and valid captured duration but has no fabricated finite logarithmic value.
- **Missing/invalid intervals**: contribute neither energy nor captured duration.

Leq must be accumulated from energy and duration, not by arithmetic averaging of decibel values.

## A/Z sequencing

Z weighting is the flat digital path and does not imply a physically flat microphone.

A weighting requires a numerically validated digital filter. The filter implementation is a separate Phase 2 increment and must be checked with deterministic synthetic tones against documented target response/tolerances before its output is exposed as A-weighted.

No generic UI smoothing may be labeled Fast or Slow. Those labels require explicit exponential time-weighting behavior and tests.

## Reference-procedure gate

Estimated SPL must remain disabled until the actual reference procedure is documented with:

- reference equipment or comparison source;
- geometry/coupling;
- reference level and applicable weighting;
- selected device/input/source/rate/format;
- before/after checks;
- limitations and repeatability observations.

If no suitable reference is available, Phase 2 continues as a digital A/Z meter and records reference-adjusted SPL as blocked rather than inventing calibration evidence.
