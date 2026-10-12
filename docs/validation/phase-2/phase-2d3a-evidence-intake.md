# Phase 2D.3a — J8 disconnect outcome and laboratory evidence intake

**Recorded:** 2026-10-12  
**Status:** implementation and J8 Bluetooth-off disconnect gate **PASS (bounded)**; Auto/P3 regressions and raw Git archival still open  
**Branch:** `feature/phase-2d3-bluetooth-robustness`  
**Tracking:** [Draft PR #9](https://github.com/dante-souza/Cuicatl/pull/9), [evidence custody issue #10](https://github.com/dante-souza/Cuicatl/issues/10)

## Evidence already established

- Local Windows `make check` completed successfully (Gradle unit tests, lint, debug build).
- `make install-j8` installed the debug APK on `38c19745` / SM-J810M.
- `make open-j8` launched Cuicatl.
- The original Bluetooth-off attempt was reported without its final outcome. Subsequent read-only J8 session inspection on 2026-10-12 confirmed that its persisted outcome was **INTERRUPTED** with a Bluetooth SCO disconnect reason; see the dated evidence checkpoint below.
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
| Bluetooth-off physical stimulus | REPORTED; outcome corroborated by saved metadata |
| Session outcome + interruption reason | **PASS — read-only J8 metadata** |
| No silent successful fallback on observed disconnect | **PASS for terminal outcome**; in-flight routing trace still unverified |
| Auto built-in / P3 regression | NOT YET OBSERVED |
| Archive integrity at 2026-10-12 audit | **PASS — all 56 manifest entries** |
| Archive privacy review + Git inclusion | OPEN — issue #10 |
| Draft PR #9 ready to merge | **NO** — other device controls and evidence gates remain |

No merge into `dev` or promotion into `main` is authorized by these
observations alone.

## Verified checkpoint — J8 Bluetooth-off test, 2026-10-12

The owner supplied the console output from the **read-only, non-root**
`scripts/phase2d3a-session-outcomes-j8.ps1` inspection after turning
Bluetooth off mid-recording. Recorded device session metadata:

| Field | Observed value |
| --- | --- |
| Started UTC | `2026-10-12 16:16:46` |
| Abbreviated session ID | `74fab196` |
| Session outcome | **`INTERRUPTED`** |
| Requested selection | `BLUETOOTH_SCO` |
| Routed input | `BLUETOOTH`, `type=7 · EDIFIER GM3` |
| Frames persisted | `207` |
| Captured duration | `20700 ms` (20.7 s) |
| Interruption reason | `Bluetooth SCO disconnected during capture; session interrupted` |

The reader exposed Android Java `Properties` escaping (`type\\=7 \\u00B7 ...`),
which represents `type=7 · ...`; the provenance is not an extra device.
The session was **not** marked COMPLETED. This confirms the expected terminal
fail-closed outcome for the tested Bluetooth-off event and means the disconnect
acceptance case is **PASS, within the scope of this one run**.

This observation **does not prove** every possible disconnect race is handled,
that the physical headset microphone was the exclusive signal source, or that
there was never a transient intermediate route. Review additional platform
traces and Auto/P3 controls before promoting the entire increment.

### Verified archive inventory, 2026-10-12

The owner also supplied a successful output from the read-only
`phase2-evidence-audit.ps1` run on the preserved archive
`Cuicatl-lab-evidence-20261012-132137`:

| Classification | Files |
| --- | ---: |
| BLUETOOTH_CSV_REVIEW | 1 |
| BLUETOOTH_OTHER_REVIEW | 9 |
| CHARACTERIZATION_PRIVATE | 41 |
| SYSTEM_DIAGNOSTIC_PRIVATE | 5 |
| **Total original payloads verified** | **56** |

All 56 files matched the pre-move SHA-256 manifest. No originals were modified,
staged or published by that audit. **Only one Bluetooth `.csv` file** is in the
Bluetooth-evidence classification. The frozen Phase 2D.2 report cites **two**
named CSVs; determine which is present and locate the other before declaring
source-complete archival. The manifest values and original payloads are not
present in this repository, so no individual digest is asserted here.


## Source identification checkpoint — 2026-10-12 (local archive)

After pulling feature commit `df173568`, the owner queried the preserved
archive's `SHA256SUMS.csv` for CSV files. Exactly one row was returned:

```text
artifacts\phase2d2-j8-bluetooth\20261012-104544\bt_handshk1-923cbd17-5a88-410d-bd54-38fb1fc5127b.csv
```

The console displayed a SHA-256 digest beginning
`5E441461996CC0097BD1FE4CDDF6EC6A90DCEDF10E673B29E4F77437`,
but **PowerShell's table formatting truncated the remainder**. The earlier
read-only archive audit verified the complete digest internally, but a
fully legible 64-digit digest has not been supplied in this checkpoint.
**Do not reconstruct or infer missing hex digits.**

The other Phase 2D.2 session referenced in the freeze,
`bt_test-d99f70ae-13e7-4d6e-973d-57102c491377.csv`,
**is not listed in this archive's CSV manifest**. It may be outside this
archived set; its current local availability is unknown. No raw CSV has
been added to Git, and the Phase 2D.2 freeze claim remains unchanged.

A fresh invocation of the J8 metadata inspector showed **only the prior
Bluetooth-off session** (`74fab196`, INTERRUPTED, 207 frames). Thus the
Auto built-in and Auto/P3 regression tests are **still not observed**.

To display the complete original CSV hash without modifying the archive:

```powershell
Import-Csv "$archive\SHA256SUMS.csv" |
    Where-Object { $_.Path -match 'bt_handshk1-.*\.csv$' } |
    Format-List Path,SHA256
```

To locate the other source CSV in Downloads without altering files:

```powershell
Get-ChildItem -LiteralPath (Join-Path $HOME 'Downloads') -Recurse -File `
    -Filter 'bt_test*.csv' -ErrorAction SilentlyContinue |
    Select-Object FullName,Length
```

Do not stage exports or full Android platform diagnostics before examining
the contents for sensitive identifiers.

## Custody clarification — `bt_test` sanitized intentionally

**Owner clarification (2026-10-12):** the longer Phase 2D.2
`bt_test-d99f70ae-13e7-4d6e-973d-57102c491377.csv` was **sanitized**.
Therefore its absence from the immutable 56-file archive must **not** be
described as evidence of accidental loss or an unexplained missing export.
The archive's `SHA256SUMS.csv` contains only `bt_handshk1`.

The sanitization operation, location/availability of any resulting derivative,
and retention status of the pre-sanitization source have **not been specified**.
Do not assume that sanitization means deletion, that a source-identical original
remains, or that an altered file has the SHA-256 hash of its original.

If a sanitized derivative is retained and later selected for publication,
record its own SHA-256, the transformation applied (for example, redacted
metadata or rows, if applicable), which fields remain suitable for analysis,
and a clear **SANITIZED DERIVATIVE — not original raw CSV** designation.
Preserve the frozen Phase 2D.2 experimental interpretation as historical
conversation evidence; do not overstate repository-level raw reproducibility.
Intentionally withheld sensitive raw exports need not be made public solely
to close this development increment.

The **Phase 2D.3a Bluetooth-off PASS** and the pending Auto built-in/P3
regression checks are unaffected by this archival clarification.

## Mail-attachment recovery lead — 2026-10-12

The owner subsequently clarified that the `bt_test` CSV **was sent by
email**, and it is unknown whether a copy remains on the Android device
after sanitization.

Code review confirms the Cuicatl in-app `sanitizeSavedSessions()` operation
deletes saved app sessions via `SessionRepository.deleteAllSavedSessions()`,
deletes cached `.csv` exports via `MeasurementCsvExporter.clearCachedExports()`,
and clears calibration/reference records. **This local-store action does not
itself redact or transform previously emailed CSV bytes.** The prior
description of `bt_test` as a potentially *sanitized CSV derivative* is a
precaution, **not an established alteration of that CSV's contents**.
Distinguish an app-store wipe from field-level sanitization of a file.

Recovery priority: search the sender's **Sent** mail (and Drafts, if needed)
for the `bt_test-d99f70ae-13e7-4d6e-973d-57102c491377.csv` attachment.
If found, download it unchanged into a **new local review directory outside
the immutable archive and outside the repository**, record its SHA-256 and
bytes, and review headers/provenance for sensitive information before
deciding whether to publish. Do not stage or modify the original attachment
as part of recovery. The sent attachment may preserve the original export,
but **attachment existence and byte identity remain unverified** until
the mail copy has been inspected.

The local phone cache may no longer contain `bt_test` after the sanitize
action. Do not attempt to recover it by overwriting app data, rooting a
stable device, or rerunning an acoustic test as though it were the same
historical capture.

Archive SHA-256 status (56/56) and the Phase 2D.3a Bluetooth-off
INTERRUPTED outcome are unaffected.
