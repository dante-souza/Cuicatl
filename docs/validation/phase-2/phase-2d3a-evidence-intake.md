# Phase 2D.3a — J8 disconnect outcome and laboratory evidence intake

**Recorded:** 2026-10-12  
**Status:** IMPLEMENTATION BUILT AND INSTALLED; physical disconnect **outcome unverified**  
**Branch:** `feature/phase-2d3-bluetooth-robustness`  
**Tracking:** [Draft PR #9](https://github.com/dante-souza/Cuicatl/pull/9), [evidence custody issue #10](https://github.com/dante-souza/Cuicatl/issues/10)

## Evidence already established

- Local Windows `make check` completed successfully (Gradle unit tests, lint, debug build).
- `make install-j8` installed the debug APK on `38c19745` / SM-J810M.
- `make open-j8` launched Cuicatl.
- User reported turning Bluetooth off during the capture experiment. **The saved session outcome and interruption reason have not yet been observed here**, so the device gate is **not PASS**.
- GitHub Actions for the prior Phase 2D.3a implementation baseline passed. The two new evidence scripts are separate changes and need their own review.
- `startBluetoothSco()` and `stopBluetoothSco()` emitted deprecation warnings but did not fail the Android build.

## Laboratory archive — do not mutate or publish blindly

The owner preserved the formerly untracked evidence in the sibling directory
`Cuicatl-lab-evidence-20261012-132137`, verified all SHA-256 digests against
a pre-move `SHA256SUMS.csv`, backed up `gradle.properties` and its diff, and
confirmed the original worktree was clean.

**Archive bytes are not accessible through GitHub, and neither the archive nor
its contents are claimed to have been committed.** Raw J8 audio/system dumps
may include identifiers, so they need a privacy review before public Git upload.

### 1. Read-only archive integrity and inventory

In Windows PowerShell 5.1, from the local Cuicatl repository:

```powershell
git pull --ff-only

$archive = 'M:\ATOL\GitProjects\Cuicatl-lab-evidence-20261012-132137'
.\scripts\phase2-evidence-audit.ps1 -ArchivePath $archive
```

The script reads the original archive and checks every manifest entry against
the stored file's current SHA-256. It lists counts by evidence class and
reports `PASS` only if all digests match and there are no unexpected files.
It does **not** copy, modify, sanitize, stage or upload anything.

For an optional inventory outside the immutable archive:

```powershell
.\scripts\phase2-evidence-audit.ps1 -ArchivePath $archive `
    -ReportDirectory 'M:\ATOL\GitProjects\Cuicatl-evidence-review'
```

The optional inventory contains only relative paths, file sizes, hashes and
risk-review categories. Even this inventory should be reviewed before sharing.

### 2. Read-only J8 session outcome verification

The installed debug APK permits `adb run-as` to read Cuicatl's own session
metadata on a connected, authorized J8. No device root is needed.

```powershell
.\scripts\phase2d3a-session-outcomes-j8.ps1
```

The output lists recent session start times, abbreviated IDs, requested
selection, actual route category/identity, frame count, outcome and reason.
It does **not** read the audio frame payload or alter the session.

**Acceptance rule:** the Bluetooth-off recording must show
`Selection=BLUETOOTH_SCO`, `Outcome=INTERRUPTED`, and a Bluetooth transport
or route-change reason. `COMPLETED`, an empty outcome, a missing saved
session, or an unrelated source is **not** a passing disconnect test.
If `run-as` is unavailable, use the app History detail and exported CSV.

### 3. Manual privacy and evidence publication

Only after review:

1. Select the original CSV captures that correspond to the 2D.2 report.
2. Compare their exact SHA-256 digests with the archive manifest.
3. Inspect label and provenance fields for personal or device-specific identifiers.
4. Copy (never move) approved source-identical CSVs into a phase-scoped
   `artifacts/phase2d2-j8-bluetooth/` evidence directory, record SHA-256
   digests, and submit those changes for review.
5. Keep raw device diagnostics in the private immutable archive; publish
   sanitized extracts only if useful, with a documented transformation
   and hashes linking to the private originals.

An archive moved off the main working tree remains evidence; moving it did not
make the experiment, microphone isolation or acoustic calibration more certain.

## Gate status

| Gate | Status |
| --- | --- |
| Android unit tests/lint/debug build on J8 host | PASS |
| Debug installation and launch | PASS |
| Bluetooth-off physical stimulus attempted | REPORTED |
| Session outcome + interruption reason | NOT YET OBSERVED |
| No automatic fallback to built-in microphone | NOT YET OBSERVED |
| Auto built-in / P3 regression | NOT YET OBSERVED |
| Archive integrity before move | PASS (owner command log) |
| Archive privacy review + Git inclusion | OPEN — issue #10 |
| Draft PR #9 ready to merge | NO |

No merge into `dev` or promotion into `main` is authorized by these
observations alone.
