# Phase 2C.2 — rootless calibration validation gate

Date: 2026-10-10  
Status: **domain architecture locally validated; Phase 2C.2a persistence/migration implemented and awaiting local build**

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
- add procedure + verification UI;
- snapshot validated profile identity into each session at Start;
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
