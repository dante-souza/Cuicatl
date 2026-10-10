# Phase 2C.2b — rootless calibration procedure and verification UI

Date: 2026-10-10  
Branch: `feature/phase-2-meter-reference-adjustment`  
Status: **implemented; local build and J8 UI validation pending**

## Purpose

Phase 2C.2b exposes the rootless calibration architecture through the saved-session workflow without enabling SPL display.

The UI deliberately keeps calibration work in History/session detail rather than on the live Meter page.

## Observation model

A physically referenced calibration requires two saved measurement sessions.

### Observation #1 — reference session

The user opens a saved session that was deliberately recorded while a real physical reference was present.

Cuicatl records:

- reference method:
  - acoustic calibrator;
  - reference sound-level meter;
  - documented comparison source;
- equipment/source description;
- optional equipment identifier/serial;
- known reference SPL;
- optional reference frequency;
- optional reference uncertainty;
- geometry/coupling;
- environment notes;
- procedure notes;
- explicit confirmation that the physical reference was actually present.

The saved session must still satisfy the existing reference-candidate gate:

- at least 10 seconds of valid captured measurement;
- no clipping;
- complete input/source/rate identity;
- finite weighted Leq.

The resulting profile is **DRAFT**.

### Observation #2 — verification session

A second saved session is opened while the draft profile is active.

The user supplies:

- maximum allowed drift in dB;
- verification notes;
- explicit confirmation that the same physical reference, geometry and conditions were repeated.

Cuicatl itself derives the second weighted Leq. The user does not type either dBFS observation.

Verification is rejected when:

- the same saved session is reused as observation #2;
- the second session is too short;
- the second session contains clipping;
- its Android-visible capture configuration differs by device/input/source/rate/format/weighting;
- the explicit same-reference confirmation is absent.

The observed drift is:

`abs(observation_2_dBFS - observation_1_dBFS)`

A passing verification creates an immutable new **VALIDATED** profile version. A failed verification creates an immutable new **DRAFT** version retaining the failed drift evidence.

## Provenance

A verification record persists:

- observation #1 through `sourceSessionId`;
- observation #2 through `verificationSessionId`;
- verification timestamp;
- before/after measured digital levels;
- allowed drift;
- observed drift;
- explicit same-reference confirmation;
- notes.

A validated profile cannot be constructed unless both physical-reference confirmations are true.

## Meter behavior

The Meter does not reevaluate historical sessions against the current active profile.

When capture starts, the actual Android-visible input configuration is compared with the active profile and a calibration snapshot is frozen into that session.

The Meter displays the frozen evidence state:

- Uncalibrated;
- Profile mismatch;
- Reference-adjusted estimate eligibility.

Even when the frozen state is `REFERENCE_ADJUSTED_ESTIMATE`, numeric SPL display remains disabled in 2C.2b.

## Safety against fabricated calibration

The calibration form contains an explicit confirmation:

> I confirm this session was recorded while the physical reference described above was actually present.

The verification form contains a second confirmation:

> I confirm the same physical reference, geometry and conditions were repeated.

The service independently rejects the operation when either confirmation is false.

These confirmations are provenance assertions, not proof that the equipment is certified or that the procedure is standards-compliant.

## Current physical-reference state

No external physical reference has yet been established for the J8 Phase 2 evidence set.

Therefore J8 validation of this increment must **not** create a retained VALIDATED profile from invented SPL values.

Allowed validation before reference equipment is available:

- build/unit/lint;
- visual inspection of all fields and confirmations;
- verify that unchecked confirmation blocks draft creation;
- verify that the Meter remains digital dBFS;
- verify saved-session navigation, deletion and sanitization remain functional.

Physical draft creation and successful verification are deferred until a real reference source/equipment is available.

## Local/J8 gate

1. Pull the current feature branch.
2. Run `make check`.
3. If successful, install/open on the J8.
4. Open a suitable saved session and inspect the rootless-calibration form.
5. Without a physical reference, leave the confirmation unchecked and verify draft creation is refused.
6. Confirm no SPL value appears on the Meter.
7. Preserve screenshots if useful.
8. Do not retain fabricated calibration evidence.

## Remaining gates before SPL activation

- successful physical-reference procedure;
- successful observation #2 repeatability check;
- validated-profile J8 evidence;
- final Phase 2 export schema decision and implementation;
- exported calibration/profile provenance;
- final UI labeling review.

Until those gates pass, Cuicatl remains a digital A/Z meter.
