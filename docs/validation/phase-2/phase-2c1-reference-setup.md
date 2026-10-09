# Phase 2C.1 — reference-adjustment setup and compatibility gating

Date: 2026-10-09. Status: **implemented; local build and J8 validation pending**.

## Implemented

- Exactly one active reference adjustment is stored app-private.
- A candidate is created from a saved session plus a user-supplied known reference SPL and reference method/equipment description.
- The saved session's weighted Leq becomes the measured digital reference value.
- Candidate creation rejects:
  - non-finite reference SPL;
  - blank reference method;
  - incomplete input/source/rate identity;
  - any observed clipped samples;
  - no finite weighted Leq;
  - less than 10 seconds of valid captured data.
- The adjustment snapshot binds:
  - device model;
  - input identity;
  - Android audio source;
  - sample rate;
  - PCM format;
  - A/Z weighting;
  - method;
  - known reference level;
  - measured reference digital level;
  - correction;
  - creation time and notes.
- Saved-session detail can store or clear the active adjustment.
- Meter displays the stored adjustment's compatibility against the actual running capture configuration.
- A mismatch names the differing configuration dimensions.
- A match is informational only.

## Hard gate retained

**Reference-adjusted SPL remains disabled.**

Storing a numeric correction does not establish physical accuracy. Phase 2C.2 may enable reference-adjusted SPL only after the physical procedure is actually performed and documented with known equipment/reference source, geometry or coupling, applicable level/weighting, and before/after observations.

Until then:
- Meter remains digital dBFS;
- CSV Schema 1 SPL/reference fields remain blank or digital-only as previously frozen;
- no regulatory or IEC class claim is made.

## Deterministic tests

ReferenceAdjustmentFactoryTest covers:
- valid weighted-Leq arithmetic;
- correction arithmetic;
- clipping rejection;
- short-reference-session rejection.

Existing ReferenceAdjustmentTest continues to cover configuration mismatch suppression.

## J8 validation requested

1. Run `make check`.
2. Install/open on the dedicated Galaxy J8.
3. Open a saved A or Z session longer than 10 s with no clipping.
4. Enter a **real known reference SPL only if such a reference was actually used**; otherwise test rejection/UI only and do not fabricate a value for evidence.
5. Enter the physical method/equipment description and notes/geometry.
6. Store the active adjustment and confirm it survives app restart.
7. Start a session with the same weighting/source/input/rate and verify the Meter reports a configuration match while still saying SPL is disabled.
8. Change weighting and verify a frequency-weighting mismatch.
9. Clear the adjustment and verify the stored state disappears.
10. Preserve screenshots and build/APK identity.

## Next gate

Phase 2C.2 — physical reference procedure and reference-adjusted SPL activation — requires an actual suitable reference setup. If suitable equipment/reference is unavailable, record SPL activation as blocked and retain the digital A/Z meter.
