# Phase 1 — usable session, preservation, and export

Status: **implementation started** on feature/phase-1-usable-session-export.

Planning gate: capture a labeled event on the J8, inspect its history, stop and reopen it, export/share a CSV, and interpret it externally. Finalize schema 1 only after independent inspection of the real export fixture.

## First runnable increment

Implemented in the first Phase 1 slice:

- user-started Start/Stop measurement sessions;
- capture ownership moved out of Compose into a started/bound microphone foreground service;
- one fixed input configuration per session using the Phase 0 source/rate negotiation;
- 100 ms persisted measurement frames;
- RMS and sample peak in dBFS only;
- elapsed and captured durations kept distinct;
- clipping count and CLIPPED quality flag from the Phase 0 J8 finding;
- live Meter/History page switching without using the page as capture owner;
- app-private append-oriented session candidate;
- readable-prefix recovery of interrupted sessions;
- saved-session list/detail/reopen path;
- directly shareable phase1-draft CSV;
- spreadsheet formula-prefix protection for user-provided session labels.

## Deliberately not claimed yet

- schema version 1 is not frozen;
- append storage has not yet passed the J8 process-kill recovery experiment;
- the required SQLite/Room comparison is not yet closed;
- modern Android service/permission coverage is not established by the J8;
- no SPL, calibration, A/Z weighting, Leq, Fast, or Slow values are displayed;
- no acoustic accuracy claim is made.

## Phase 1 gate checklist

- [ ] Permission denial produces no capture.
- [ ] Repeated Start does not create competing sessions.
- [ ] Repeated Stop is harmless.
- [ ] Meter ↔ History switching preserves session ID, frame sequence, and capture.
- [ ] Activity recreation preserves the service-owned session.
- [ ] Normal Stop produces a reopenable saved session.
- [ ] Saved detail history agrees with persisted frames.
- [ ] CSV share works through Android content URI permissions.
- [ ] Exported row count and sequence are independently checked.
- [ ] Exported RMS/peak values are independently spot-checked.
- [ ] Clipping count/flag is visible in a deliberately clipped fixture.
- [ ] Process death produces a recovered/interrupted record, never a false continuous session.
- [ ] Observed preservation/loss bound is recorded.
- [ ] Append vs SQLite/Room comparison is closed with evidence.
- [ ] Storage failure behavior is exercised or explicitly recorded as unexercised.
- [ ] Input change/interruption behavior is recorded.
- [ ] Screen-off behavior is tested for the implemented foreground-service path.
- [ ] First acoustic task/setting is recorded before final user acceptance.
- [ ] Real exported fixture is inspected externally.
- [ ] CSV schema 1 is finalized only after that inspection.

Original-dimension screenshots, logs, exported fixtures, APK identity, and checksums belong under this validation directory when the J8 run is performed.
