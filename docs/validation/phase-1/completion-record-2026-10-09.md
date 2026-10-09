# Phase 1 completion record

Date: 2026-10-09

Phase: **Phase 1 — usable session + preservation + export**

Status: **complete**

## Validated executable candidate

The accepted Phase 1 APK was built and checked from:

- branch: `feature/phase-1-usable-session-export`
- validated application commit: `527a4bd3056d4797fa33c6439e7a2a9d8dd173a2`
- APK: `app/build/outputs/apk/debug/app-debug.apk`
- APK size: 18,379,050 bytes
- APK SHA-256: `9F19D4F38BE50C68AFA8F9375F50C1BB1EA6AFEB8C3DEE628168B9A2D35480BD`

The local freeze quality gate ran `testDebugUnitTest`, `lintDebug`, and `assembleDebug` and completed successfully.

Later Phase 1 branch commits after the validated executable candidate contain documentation / host-side validation-helper closure and do not intentionally change the accepted application behavior.

## Device identity

Primary validation device:

- model: Samsung Galaxy J8 / SM-J810M
- device: `j8y18lte`
- Android: 10 / API 29
- security patch: 2021-09-01

## Final lifecycle gate

The final Meter / History navigation check used session:

`ba299b28-8bf4-429c-9e40-db7b12c01fa3`

Observed persisted rows:

- before navigation: 3587
- on History: 3662
- back on Meter: 3753
- same active session on History: true
- same active session on Meter: true

Result: **PASS**.

Capture and persistence continued while the visible analysis page changed, and the active session identity remained stable.

## Phase 1 accepted behavior

Phase 1 establishes:

- explicit user-started microphone capture;
- session-owned foreground-service capture independent from Compose page lifecycle;
- fixed source / sample rate / route policy per session;
- 100 ms persisted measurement frames;
- RMS and sample peak in digital dBFS;
- clipping counts and quality flags;
- distinct captured and elapsed duration semantics;
- durable app-private session storage;
- normal-stop save and reopen;
- readable-prefix process-death recovery;
- explicit `RECOVERED` / `process_recovery` provenance;
- unknown recovered elapsed duration represented as unavailable, not fabricated;
- Meter / History continuity;
- screen-off continuity;
- Activity recreation continuity;
- permission denial with no capture;
- direct CSV sharing;
- frozen CSV Schema 1;
- pre-session standard Android AGC request model:
  - DEFAULT
  - FORCE_OFF
  - FORCE_ON
- actual standard Android AGC / NS / AEC state preserved in session provenance where inspectable.

## Persistence decision

The append-oriented app-private session format is retained for the initial Cuicatl baseline.

Room / SQLite is deferred until a concrete requirement justifies it, such as indexed relational session queries, transactional multi-record editing, or demonstrated file-library performance limits.

The future backup direction remains local-first: finalized immutable sessions may later be copied/imported through Android Storage Access Framework / DocumentsProvider destinations without introducing a Cuicatl cloud/account backend.

## Recovery evidence

The instrumented process-death test observed:

- complete rows visible immediately before helper-issued force-stop: 446
- complete rows recovered: 449
- known loss of complete rows from the measured pre-kill snapshot: 0

The three additional rows completed during the small interval between the helper snapshot and the actual force-stop command.

This result applies to the tested Android process-death path. It is not a guarantee against sudden power removal, storage corruption, or loss of an in-flight partial write.

## CSV Schema 1

Schema 1 is frozen after independent inspection of completed and recovered J8 fixtures.

Validated properties include:

- contiguous sequence reconstruction;
- sample-count / sample-rate / interval-duration agreement;
- independently reproducible RMS / peak dBFS math;
- explicit UTC anchor and original timezone offset;
- interval UTC estimate derived from monotonic/sample-relative timing;
- empty unavailable SPL/reference fields rather than fabricated zeros;
- correct recovered-session outcome / interruption semantics;
- empty recovered elapsed duration when unknowable.

Breaking changes require a new schema version.

## J8 processing-path finding

The J8 `VOICE_RECOGNITION` path reports:

- standard Android AGC unavailable;
- standard Android NoiseSuppressor available and controllably disabled;
- standard Android AcousticEchoCanceler available and controllably disabled;
- vendor / HAL / analog processing unknown.

In the controlled fan/background fixture, the delivered PCM background level rose temporarily by about 1.7 dB after a loud foreground event and later returned near baseline.

Phase 1 therefore records truthful digital PCM / dBFS observations, but does **not** establish that the J8 has a completely stable absolute acoustic gain path.

## Explicit boundaries

Phase 1 does not claim:

- calibrated SPL;
- acoustic accuracy certification;
- stable absolute microphone gain on the J8;
- A or Z frequency weighting;
- Fast / Slow time weighting;
- Leq;
- SPL min / max / current;
- physical input hot-swap validation;
- modern Android service/permission coverage beyond the Android 10 J8 validation campaign;
- zero-loss behavior under sudden power loss or arbitrary storage failure.

## Phase 1 decision

**PASS — Phase 1 is complete.**

The next phase may rely on capture, preservation, recovery, session reopening, and CSV Schema 1 as established foundations.

Phase 2 must treat reference adjustment / calibration and the J8 input-path stability uncertainty explicitly rather than silently converting dBFS into SPL.
