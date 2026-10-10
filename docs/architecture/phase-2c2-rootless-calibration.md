# Phase 2C.2 — Rootless Calibration Architecture

Date: 2026-10-10  
Branch: `feature/phase-2-meter-reference-adjustment`  
Status: **architecture + domain gate implemented; persistence/UI activation and physical validation pending**

## Objective

Cuicatl must be able to become reference-adjusted on ordinary non-rooted Android devices.

Root access may be useful for laboratory archaeology, but it is not part of the runtime calibration contract and must never be required to:

- create a calibration;
- apply a calibration;
- verify compatibility;
- calculate a reference-adjusted estimate;
- reopen/export a calibrated session.

The calibration authority is an external physical reference plus Android-observable capture identity, not privileged knowledge of mixer registers or microphone vendor data.

## Layered model

Phase 2 now separates three concepts.

### 1. Digital measurement

Cuicatl produces deterministic A- or Z-weighted digital levels in dBFS.

This layer requires no acoustic assumptions.

### 2. Reference adjustment

A `ReferenceAdjustment` is the arithmetic relationship:

`correction_dB = known_reference_SPL - measured_reference_dBFS`

This object is deliberately insufficient to enable SPL on its own.

### 3. Rootless calibration profile

A `RootlessCalibrationProfile` wraps the adjustment with physical-procedure provenance and repeatability verification.

Only a **validated**, configuration-matching profile may unlock the evidence state:

**Reference-adjusted estimate**

Everything else remains either:

- **Uncalibrated**; or
- **Profile mismatch**.

## Rootless reference methods

The first architecture supports:

- acoustic calibrator;
- external reference sound-level meter;
- documented comparison source.

These methods have different evidence quality but use the same application contract. Cuicatl does not infer that a comparison source is traceable or standards-compliant merely because it was entered.

## Required physical provenance

A rootless reference procedure records:

- method category;
- equipment/source description;
- optional identifier/serial;
- known reference SPL;
- optional reference frequency;
- optional stated reference uncertainty;
- geometry/coupling;
- environment notes;
- procedure notes.

Geometry is mandatory because a side-by-side comparison and a direct acoustic coupler are physically different procedures.

## Repeatability gate

A single numeric offset is not enough to activate SPL.

The architecture requires at least two reference observations, represented as a before/after measured digital level, plus a declared maximum acceptable drift.

`observed_drift_dB = abs(after_dBFS - before_dBFS)`

The profile can only enter `VALIDATED` when:

- verification exists;
- at least two observations were recorded;
- observed drift is within the declared procedure tolerance;
- a validation timestamp exists.

The drift threshold is procedure metadata, **not an IEC tolerance claim**. A later UI policy may provide conservative defaults, but the domain model does not pretend a project threshold defines instrument class.

## Exact capture compatibility

The validated profile delegates matching to the existing Phase 2 reference contract. All must remain equal:

- device model;
- input identity;
- Android audio source;
- sample rate;
- sample format;
- frequency weighting.

A mismatch suppresses the SPL estimate rather than silently applying an offset to a different signal path.

## What root is allowed to do

Rooted evidence may help us understand:

- codec identity;
- mixer topology;
- hardware gain;
- vendor processing;
- microphone routing.

That information may improve documentation and diagnostics.

It **must not become a required key** for a calibration profile. If Cuicatl cannot reproduce a necessary input identity through ordinary Android APIs on another non-rooted handset, that property cannot be part of the portable runtime gate.

## What firmware metadata cannot do

Vendor microphone sensitivity tables, codec gain guesses, board schematics, or nominal hardware specifications cannot by themselves create a `VALIDATED` rootless profile.

They may support a separately labeled **Nominal hardware-derived estimate** in a future phase, but that evidence class is not equivalent to a physical reference adjustment.

## Failure behavior

Cuicatl fails closed:

- no profile -> Uncalibrated;
- draft profile -> Uncalibrated;
- invalidated profile -> Uncalibrated;
- failed repeatability verification -> Uncalibrated;
- validated profile + configuration mismatch -> Profile mismatch;
- validated profile + matching configuration -> Reference-adjusted estimate.

No fallback offset is applied.

## Persistence architecture

The next implementation increment should persist the complete rootless profile, not only the old numeric adjustment.

The profile record should be versioned and immutable once validated. Recalibration creates a new version/profile; it should not rewrite the historical profile referenced by old sessions.

An applicable measurement session must snapshot at least:

- calibration profile ID/version;
- reference method;
- reference level;
- correction;
- evidence state;
- validation timestamp.

That snapshot is necessary so a later active-profile change cannot rewrite the interpretation of a historical session.

## Export architecture

CSV Schema 1 remains frozen.

Reference-adjusted Phase 2 data therefore needs an explicit schema evolution decision before release. The export must distinguish:

- raw digital dBFS;
- weighted digital dBFS;
- reference-adjusted estimated SPL;
- calibration/profile identifier and version;
- evidence state and method.

No A-weighted dBFS value may be placed into an SPL column.

## Current activation state

**SPL display remains disabled.**

The code added in this increment is a domain-level gate and testable architecture. It does not authorize the current 2C.1 stored adjustment to become validated automatically.

Activation requires:

1. persistence of the full profile;
2. UI/procedure capture;
3. immutable session snapshot;
4. export schema decision;
5. an actual external physical reference procedure on the J8;
6. before/after verification evidence.

If no external reference is available, Cuicatl remains a fully functional digital A/Z meter and reports calibration as blocked rather than inventing evidence.
