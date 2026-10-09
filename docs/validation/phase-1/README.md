# Phase 1 — usable session, preservation, and export

Status: **closure in progress** on feature/phase-1-usable-session-export.

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

- CSV schema version 1 is frozen after independent completed/recovered export inspection;
- append storage has passed process-kill recovery semantics, readable-prefix recovery, and one instrumented J8 run with 0 loss of complete rows visible at the pre-kill snapshot;
- append-oriented app-private storage is selected; Room/SQLite is deferred with explicit adoption triggers;
- modern Android service/permission coverage is not established by the J8;
- the J8 VOICE_RECOGNITION path shows an observed background-noise/effective-gain adaptation that must be characterized before Phase 2 can treat it as a stable absolute acoustic reference;
- pre-session Android AGC control is implemented as DEFAULT / FORCE_OFF / FORCE_ON where supported; requested and actual effect state are persisted in session metadata and Schema 1 processing_state; the J8 DEFAULT fan fixture confirms AGC unavailable, NS/AEC disabled, and a temporary ~1.7 dB post-foreground background elevation of unknown layer;
- no SPL, calibration, A/Z weighting, Leq, Fast, or Slow values are displayed;
- no acoustic accuracy claim is made.

## Phase 1 gate checklist

- [x] Permission denial produces no capture; explicit denial UI verified on the J8.
- [ ] Repeated Start does not create competing sessions.
- [ ] Repeated Stop is harmless.
- [ ] Meter ↔ History switching preserves session ID, frame sequence, and capture.
- [x] Activity recreation preserves the service-owned session; instrumented rotation check kept the same session ID while frames advanced.
- [x] Normal Stop produces a reopenable saved session.
- [x] Saved detail history agrees with persisted frames for the first J8 fixture.
- [x] CSV share works through Android content URI permissions on the first J8 fixture.
- [x] Exported row count and sequence are independently checked (1,547 rows; sequence 0–1,546 contiguous).
- [x] Exported RMS/peak values are independently recalculated and consistent with the digital definitions.
- [x] Clipping count/flag is present in the first J8 fixture; 130 clipped frames / 83,349 endpoint samples were independently counted.
- [x] Process death produces a RECOVERED record with process_recovery, never a false continuous session.
- [x] Observed J8 process-death preservation bound is recorded: 446 complete rows visible pre-kill, 449 recovered, 0 known complete-row loss.
- [x] Persistence decision is closed: append-oriented Candidate A selected from tested evidence; Room/SQLite deferred with explicit adoption triggers.
- [x] Storage-write failure is explicitly recorded as unexercised because no safe reproducible J8 fault-injection method was established.
- [ ] Input change/interruption behavior is recorded.
- [x] Screen-off behavior is tested: persisted frames advanced with the same active session while the J8 screen was off.
- [x] First acoustic task/setting is recorded: stationary room fan/background after foreground sound falls silent, used as an input-stability observation rather than SPL validation.
- [x] Real exported fixtures are independently inspected externally; the second fixture closes the timing-provenance correction.
- [x] CSV schema 1 is finalized after completed and RECOVERED fixtures were independently inspected.

Original-dimension screenshots, logs, exported fixtures, APK identity, and checksums belong under this validation directory when the J8 run is performed.
