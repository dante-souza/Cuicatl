# Phase 1 storage decision — append-oriented sessions retained

Date: 2026-10-09

Decision status: **accepted for Phase 1 / v0.1.0 baseline**

## Decision

Retain the current append-oriented, app-private session store for Cuicatl Phase 1 and the initial release baseline.

Do **not** add Room/SQLite now.

The original planning document required an implementation comparison between an append-oriented candidate and a Room/SQLite candidate. Phase 1 evidence changed the decision boundary: Candidate A already satisfies the currently demonstrated preservation, recovery, reopen, and export requirements, while no current product requirement needs relational querying, in-place record editing, multi-entity joins, or database migrations.

Rather than build and benchmark a second persistence stack solely to confirm that it is not presently needed, Cuicatl records Room/SQLite as a deferred alternative with explicit adoption triggers.

This is an evidence-driven plan revision, not a claim that Room/SQLite is unsuitable for Cuicatl forever.

## Evidence supporting Candidate A

The append-oriented design has demonstrated on the Samsung Galaxy J8:

- normal Start/Stop and durable saved-session reopening;
- 100 ms measurement-frame persistence;
- independent CSV reconstruction and numerical verification;
- readable-prefix recovery after forced Android process death;
- correct RECOVERED / process_recovery semantics;
- explicit unavailable elapsed duration after recovery rather than fabricated timing;
- an instrumented process-death run with:
  - 446 complete rows visible immediately before the helper issued force-stop;
  - 449 complete rows recovered;
  - 0 complete rows from the measured pre-kill snapshot lost.

The three extra recovered rows completed in the race window between the helper's row-count snapshot and the actual force-stop command. They are valid additional persisted frames, not negative loss.

This evidence applies to the tested Android process-death path. It is not a zero-loss claim for sudden power removal, storage corruption, or arbitrary write failure.

## Why Room/SQLite is deferred

Room/SQLite would add useful capabilities if Cuicatl later needs them, but it currently adds:

- a second persistence representation or a migration away from the proven one;
- schema/migration lifecycle work;
- additional recovery/transaction behavior to validate;
- more code and dependencies without an unmet Phase 1 requirement.

Current Cuicatl sessions are naturally append-heavy while recording and immutable after finalization. The existing file-oriented shape matches that lifecycle directly.

Room/SQLite should be reconsidered when one or more concrete triggers appear, such as:

- large session libraries requiring indexed multidimensional queries;
- editable relational metadata with cross-session joins;
- frequent partial updates to saved records;
- query performance that the file index/list implementation cannot meet;
- a future feature whose correctness is materially simpler with transactions across multiple record types.

## Backup / synchronization direction

Cuicatl should remain local-first. App-private storage stays authoritative while a measurement is active.

A future backup/export capability may allow the user to choose a destination through Android's Storage Access Framework / DocumentsProvider ecosystem. Depending on installed providers, that destination may be local storage, removable storage, a cloud-drive provider, or another document-backed location.

The intended model is:

1. capture into app-private authoritative storage;
2. finalize an immutable session;
3. package/copy the finalized session to a user-selected backup destination;
4. optionally import a compatible session bundle later.

The initial backup design is **not** bidirectional live synchronization. Cuicatl does not need an account service, custom cloud API, conflict-resolution backend, or always-on synchronization infrastructure for this capability.

Provider permissions, unavailable destinations, duplicate imports, bundle versioning, integrity checks, and restore semantics must be designed when backup/import enters active scope.

## Relationship to Yeyecatl

This decision is specific to Cuicatl's current session lifecycle. It does not establish a family-wide rule that CATL applications must or must not use databases.

Yeyecatl and Cuicatl should each adopt persistence mechanisms only when their demonstrated requirements justify them.

## Consequence for Phase 1

The persistence-selection item is closed:

**Candidate A selected; Room/SQLite deferred with explicit reconsideration triggers.**

Remaining Phase 1 closure work is lifecycle/error validation, Schema 1 freeze, and archaeology/artifact preservation.
