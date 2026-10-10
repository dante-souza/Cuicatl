# Phase 2C.2 — rootless calibration validation gate

Date: 2026-10-10  
Status: **domain architecture committed; local build pending**

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
