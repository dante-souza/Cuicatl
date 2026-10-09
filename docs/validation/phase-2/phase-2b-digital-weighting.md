# Phase 2B — streaming A/Z digital weighting implementation

Date: 2026-10-09  
Status: **implementation committed; tests and J8 evidence pending**

## Scope

Introduces a pure-Kotlin `StreamingFrequencyWeighting` processor, with one persistent state per audio capture session. The Z path is a digital identity function. The A path uses three stable second-order IIR sections with 1 kHz gain normalization, defined separately for the actual Phase 1 negotiated sample rates of 48 kHz and 44.1 kHz.

The analogue prototype has two real poles at 20.598997 Hz, poles at 107.65265 and 737.86223 Hz, and two poles at 12194.217 Hz, plus four zeroes at DC. The digital sections use a bilinear transform and a normalization at 1 kHz. The finite-rate bilinear transform warps higher frequencies; no IEC compliance class or flat microphone response is claimed.

## Deterministic test criteria

Tests cover the exact Z identity, nominal A response from 31.5 Hz to 4 kHz on both sample rates, state continuity across buffer boundaries, reset, and rejection of unverified sample rates. These are code fixtures, **not** observed successful CI results or acoustic validation. Expected response has tolerance ±0.35 dB against rounded nominal target values in the specified frequencies. This finite test band does not establish class-compliant weighting.

## Important boundary

Phase 1 records unweighted digital RMS as `mean_square_fs` / `rms_dbfs`. This initial 2B commit does **not** silently reinterpret those frozen fields, nor does it claim filtered values are present in a saved session. Connecting the A/Z processor to capture requires an explicit selected-weighting snapshot, persistence design, and export mapping, while protecting the frozen Schema 1 semantics. Until that integration is done, the app's current Meter display remains Phase 1 digital Z-like observation, not an A-weighted meter.

Reference-adjusted SPL remains disabled until a real documented physical reference procedure passes its device gate.

## Validation status

- [x] Pure streaming filter implementation added to Phase 2 feature branch
- [x] Deterministic test source added for both negotiated rates
- [ ] Gradle test/lint/assemble pass recorded for exact commit
- [ ] Streaming filter connected to the fixed session configuration
- [ ] A/Z values persisted and reconciled against CSV Schema 1
- [ ] J8 capture/performance validation
- [ ] Independent numerical review and physical reference-selection gate
