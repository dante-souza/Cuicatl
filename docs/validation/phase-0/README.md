# Phase 0 validation record

Planning Revision 2 splits this milestone into **0A minimum runnable foundation** and **0B J8 PCM capture/timing probe**.

## Automated gate

The Phase 0 candidate passed the configured GitHub Actions software gate:

- unit tests;
- Android lint;
- debug APK assembly;
- Gradle wrapper validation.

## Physical J8 gate

Physical validation was performed on the Samsung Galaxy J8 / SM-J810M / Android 10 (API 29).

The tested capture path is documented in `j8-findings-2026-10-08.md`.

Result: **PROCEED**

The probe established a stable 48 kHz `VOICE_RECOGNITION` PCM path, coherent sample/timestamp progression, bounded 10-second completion, and explicit manual stop.

The physical evidence also exposed repeated full-scale sample peaks at 0.0 dBFS under loud transients. Phase 1 therefore treats clipping as explicit measurement-quality state.

## Non-blocking unexercised checks

- permission denial after permission had already been granted;
- Meter ↔ History switching during an active probe.

Neither changes the Phase 0 capture-path decision.

## Final artifact bookkeeping

Candidate debug APK SHA256 was verified from the local J8-tested artifact. Fingerprint: `8CAC5DB5…A913D87B`.

Phase 0 archaeology is complete.

No calibrated SPL accuracy claim is made by Phase 0.
