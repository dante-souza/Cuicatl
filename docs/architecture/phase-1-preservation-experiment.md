# Phase 1 preservation / recovery experiment

Date: 2026-10-08.

Status: **closed for Phase 1**. Candidate A passed the tested J8 recovery gate and was selected. The original requirement to implement a Room/SQLite candidate was revised after evidence showed no unmet Phase 1 requirement that justified a second persistence stack.

## Why this experiment exists

Phase 1 must preserve a useful measurement record when the UI changes pages, the Activity is recreated, or the process is interrupted. The UI is not the authoritative data store. CSV remains a portable export, not the live persistence mechanism.

The original planning revision required an implemented append-vs-Room/SQLite comparison before freezing storage. Phase 1 evidence changed that decision: the append candidate passed the demonstrated normal-stop, reopen, export, process-recovery, and preservation requirements. The storage decision record therefore selects Candidate A and defers Room/SQLite until a concrete adoption trigger appears.

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

### Candidate-A observed result

The J8 evidence established:

- normal Stop produces a reopenable saved session;
- completed and recovered exports reconstruct contiguous measurement rows;
- a forced process death produces RECOVERED / process_recovery rather than a false completed session;
- recovered data form a readable prefix with valid numerical/timing semantics;
- an instrumented recovery run observed 446 complete rows immediately before the helper issued force-stop and 449 complete rows after recovery, with 0 known loss of complete rows from the measured pre-kill snapshot.

The three additional rows completed between the helper snapshot and the actual force-stop command. This is not negative loss.

The result is specific to the tested Android process-death path. Sudden power loss, arbitrary filesystem failure, and an in-flight partial write remain separate failure modes.

## Candidate B — SQLite / Room disposition

A Room/SQLite implementation was **not built** for Phase 1.

After Candidate A passed the required user workflow and process-recovery tests, there was no remaining Phase 1 requirement that needed relational persistence. Building Candidate B at that point would have introduced a second storage stack, migration/schema lifecycle work, and another recovery implementation without resolving an observed problem.

Room/SQLite remains a deferred alternative rather than a rejected technology. Reconsider it if Cuicatl later requires indexed multidimensional queries, relational cross-session metadata, frequent in-place updates, transactionally coupled record types, or file-library performance that cannot meet measured needs.

The accepted decision and future backup direction are recorded in `phase-1-storage-decision-2026-10-09.md`.

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

Room/SQLite is deferred under the accepted storage decision; no duplicate recovery run is required until a concrete database adoption trigger reopens the decision.

## Evidence required to close the experiment

- exact Cuicatl commit / APK hash;
- J8 model / Android version;
- normal-stop session fixture;
- process-kill recovery fixture;
- exported CSV fixture;
- observed preservation/loss bound;
- Candidate A vs Candidate B disposition with rationale.

Storage-write failure was not deliberately induced during this experiment because no safe, reproducible J8 method was established that would avoid unrelated device-state damage. This remains explicitly unexercised rather than being represented as a passed case.

Schema version 1 was subsequently frozen only after multiple real Phase 1 exports—including a RECOVERED fixture—were independently inspected. See `../architecture/csv-schema-v1.md`.
