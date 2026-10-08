# Phase 0 validation record

Planning Revision 2 splits this milestone into **0A minimum runnable foundation** and **0B J8 PCM capture/timing probe**.

## Automated gate

Required on the exact candidate commit:

- unit tests;
- Android lint;
- debug APK assembly;
- Gradle wrapper validation.

GitHub Actions is the CI authority for the software baseline.

## Physical J8 gate

Use `j8-checklist.md`. Device installation/open actions must pass `scripts/preflight-j8.ps1`.

The physical gate now includes a user-started diagnostic capture. Merely proving that the shell launches is a 0A checkpoint, not Phase 0 completion.

Evidence must identify the actual source/rate/route/timing behavior and keep dBFS observations distinct from SPL claims.

## Completion decision

Freeze a short findings record with one of:

- **Proceed** — tested J8 path is sufficient for Phase 1 digital session work.
- **Fallback investigation** — a different source/input path needs a bounded follow-up.
- **Blocker** — reliable capture cannot proceed without resolving a documented issue.

No Phase 0 tag should be created before configured CI checks and the physical probe gate are complete.
