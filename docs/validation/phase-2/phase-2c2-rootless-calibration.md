# Phase 2C.2 — rootless calibration validation gate

Date: 2026-10-10  
Status: **domain architecture locally validated; Phase 2C.2a persistence and Phase 2C.2b procedure/verification UI implemented; combined local build pending**

## Deterministic gate tests

The pure Kotlin domain tests require:

- a draft profile never enables estimated SPL;
- a validated, repeatable, configuration-matching profile enables the arithmetic estimate;
- any configuration mismatch suppresses the estimate and reports the mismatch;
- repeatability drift is calculated in dB and can fail the declared procedure tolerance;
- profile construction refuses to mark a calibration VALIDATED without a passing verification and validation timestamp.

## Non-root invariant

No class in the rootless calibration domain references:

- `su`;
- debugfs;
- ALSA control nodes;
- TinyALSA;
- vendor mixer files;
- root/Magisk state.

Those may exist in lab evidence only.

## Pending implementation gates

Before SPL is visible in the app:

- persist `RootlessCalibrationProfile`;
- migrate/interpret existing 2C.1 adjustment storage without silently validating it;
- ~~add procedure + verification UI;~~ implemented in Phase 2C.2b;
- ~~snapshot validated profile identity into each session at Start;~~ implemented in Phase 2C.2a;
- decide and implement the Phase 2 export schema;
- run physical J8 reference procedure;
- retain screenshots, CSV, profile record, APK hash and procedure notes.

Until all gates pass, the Meter remains digital dBFS.


## Local validation — 2026-10-10

The Phase 2C.2 domain architecture at branch head `a1624a20b15eba50e7f7c8496de5fc3f230d5f7d` passed:

`make check`

Result:

- **BUILD SUCCESSFUL**;
- 53 actionable tasks;
- 1 executed;
- 52 up-to-date;
- elapsed time: 23 s;
- JDK: Eclipse Adoptium 17.0.20.1;
- Android platform: API 36.

The developer working tree already contained an unrelated local modification to `gradle.properties`; it was not part of the Phase 2C.2 commits.

## Phase 2C.2a — persistence and migration

Implemented after the successful domain build:

- versioned app-private `RootlessCalibrationProfile` files;
- separate active-profile pointer;
- legacy Phase 2C.1 `active-reference-adjustment.properties` migration;
- migrated legacy adjustment is always `DRAFT`, never `VALIDATED`;
- legacy pointer is retired only after the new draft profile and active pointer are durable;
- current 2C.1 UI writes new draft rootless profiles instead of restoring numeric adjustment authority;
- active-profile identity/evidence/reference values are snapshotted into session metadata only after the actual Android capture configuration is known;
- old sessions remain readable because all calibration snapshot keys are optional;
- deleting a source session removes profiles derived from that session;
- global sanitization removes all rootless calibration profiles and the active pointer.

SPL display remains disabled. A draft profile snapshots as `UNCALIBRATED`; a mismatching validated profile would snapshot as `PROFILE_MISMATCH`; only a future physically validated matching profile may snapshot as `REFERENCE_ADJUSTED_ESTIMATE`.

### Next local gate

Run `make check` again against the Phase 2C.2a head before device installation.


## Phase 2C.2b — procedure and verification UI

Implemented:

- full physical-reference provenance form in saved-session detail;
- reference-method selector for acoustic calibrator, reference SLM, or documented comparison source;
- equipment/source description and optional identifier;
- reference SPL, optional frequency and uncertainty;
- mandatory geometry/coupling;
- environment/procedure notes;
- explicit physical-reference-present confirmation;
- immutable observation #1 draft profile;
- observation #2 based on a different saved session;
- exact capture-profile matching before verification;
- user-declared maximum drift plus Cuicatl-computed before/after weighted Leq drift;
- explicit same-reference/geometry/conditions confirmation;
- persisted verification-session ID and timestamp;
- failed verification retained as a new DRAFT profile version;
- passing verification retained as a new VALIDATED profile version;
- Meter displays the session's frozen calibration snapshot rather than reinterpreting the session from the current active profile.

The service rejects missing physical-reference confirmations independently of the UI.

Numeric SPL display remains disabled.

See `docs/validation/phase-2/phase-2c2b-calibration-procedure-ui.md` for the device workflow and the no-fabricated-calibration rule.
