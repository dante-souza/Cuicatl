# Phase 2 validation — Meter + chosen reference adjustment

Status: **IN PROGRESS**  
Started: 2026-10-09  
Branch: `feature/phase-2-meter-reference-adjustment`  
Baseline: Phase 1 merge on `dev` at `5aa7d157d42ff035cc0ae32f16db241b4e4fa6d9`

## Phase gate

Phase 2 is complete only when all applicable items below have evidence:

- [ ] Z-weighted digital path is deterministic and preserves the Phase 1 level convention.
- [ ] A-weighting implementation passes deterministic synthetic response checks.
- [ ] Current/minimum/maximum/Leq definitions are implemented and unit tested.
- [ ] Partial intervals use actual duration and sample coverage.
- [ ] Digital zero is not converted into an invented logarithmic floor.
- [ ] One active reference-adjustment configuration is supported.
- [ ] The reference-adjustment snapshot is immutable per applicable session.
- [ ] Input/configuration mismatch disables reference-adjusted SPL.
- [ ] CSV Schema 1 reserved weighting/reference/SPL fields match displayed values.
- [ ] The selected physical reference procedure is documented.
- [ ] The selected physical reference procedure is performed on the J8 before device SPL estimates are enabled.
- [ ] Device evidence records commit SHA, APK SHA256, input configuration, procedure, limitations, and original screenshots.
- [ ] v0.1.0 meter-scope decision is recorded.

## Increment 2A — contract and deterministic level arithmetic

Initial scope:

- frequency-weighting domain contract;
- immutable configuration-bound reference-adjustment model;
- energy/duration based current/min/max/Leq accumulator;
- mismatch suppression;
- unit tests;
- architecture record.

This increment intentionally does **not** enable SPL estimates in the app. The real reference procedure has not yet been recorded/performed.

## Next increment

Implement the streaming Z/A analysis path and deterministic frequency-response fixtures. Keep weighting fixed at Start and preserve it through persistence/export. Only after that numerical gate should the UI be promoted from the Phase 1 digital presentation to the Phase 2 meter presentation.

## Reference-procedure blocker

The roadmap requires the actual reference equipment and procedure to be chosen before estimated SPL is shown on-device. Until that decision/evidence exists, the correct state is **Uncalibrated**, not an assumed correction.
