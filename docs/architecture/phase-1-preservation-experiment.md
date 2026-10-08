# Phase 1 preservation / recovery experiment

Date: 2026-10-08.

Status: **in progress**. This document defines the experiment; it does not yet claim that the selected preservation design has passed the J8 recovery gate.

## Why this experiment exists

Phase 1 must preserve a useful measurement record when the UI changes pages, the Activity is recreated, or the process is interrupted. The UI is not the authoritative data store. CSV remains a portable export, not the live persistence mechanism.

The approved plan requires an explicit comparison between an append-oriented design and a SQLite/Room design against the same recovery checks before the storage choice is frozen.

## Candidate A — append-oriented session directory

The first runnable candidate uses one app-private directory per session:

- session.properties — small session/configuration/finalization record;
- frames.tsv — one framed measurement record per line;
- 100 ms target measurement intervals;
- output is flushed after every frame;
- FileDescriptor.sync() is requested every 10 frames (nominally every 1 second) and at normal finalization;
- metadata is written through a temporary file and replaced only after the temporary copy has been synced;
- a malformed/truncated final frame line is ignored during recovery, preserving the readable prefix.

A session left in RUNNING state is converted on next service creation to a saved RECOVERED outcome with interruption reason process_recovery. This state means a persisted prefix was recovered; it must never imply that microphone capture continued while the process was absent.

### Candidate-A preservation hypothesis

- Activity/page changes: no measurement loss because the foreground service owns capture and persistence.
- Normal Stop: all completed and partial final frames are synced before metadata finalization.
- Abrupt process death: all complete readable frame lines already delivered to the kernel should remain readable; the explicit durability target for storage/power-loss testing is at most the most recent 10 frames (about 1 second) between sync() calls.
- Torn final write: the incomplete final line is rejected rather than interpreted as silence or a valid measurement.

These are hypotheses until exercised on the J8.

## Candidate B — SQLite / Room comparison

The comparison candidate is a conventional session table plus frame table, with frame batches committed transactionally. It must be judged using the same checks:

1. normal Stop and reopen;
2. forced process death during capture;
3. readable prefix / row count after recovery;
4. storage-full or write-error behavior;
5. export reconstruction;
6. implementation and migration burden;
7. measured write cost on the J8.

The database candidate is not automatically preferred because it provides transactions, and the append candidate is not automatically preferred because it is smaller. The Phase 1 decision record will select the smallest implementation that actually passes the shared acceptance checks.

## J8 execution checklist

For Candidate A:

1. Start a labeled session and capture at least 30 seconds.
2. Switch Meter ↔ History repeatedly.
3. Rotate/recreate the Activity if supported by the current device configuration.
4. Stop normally; reopen the saved session; compare visible frame count and durations.
5. Export the CSV and independently count/recalculate rows.
6. Start another session, allow at least 15 seconds of data, then kill the Cuicatl process from ADB without pressing Stop.
7. Relaunch Cuicatl. Confirm the session appears as RECOVERED, not COMPLETED.
8. Record recovered frame count, captured duration, last sequence, and the difference from the pre-kill observation.
9. Exercise a storage-write failure if a safe reproducible method is available; otherwise record it as unexercised rather than simulating success.

Run the equivalent persistence/recovery checks for the SQLite candidate before freezing the storage decision.

## Evidence required to close the experiment

- exact Cuicatl commit / APK hash;
- J8 model / Android version;
- normal-stop session fixture;
- process-kill recovery fixture;
- exported CSV fixture;
- observed preservation/loss bound;
- Candidate A vs Candidate B decision with rationale.

Schema version 1 is **not** frozen by this experiment. It is frozen only after a real Phase 1 exported CSV is independently inspected.
