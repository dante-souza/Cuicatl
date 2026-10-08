# Cuicatl

Cuicatl is an Android sound-analysis platform in the CATL family: capture an acoustic event, inspect changes over time, preserve the session, and export/share the data with enough context to interpret it outside the app.

## Project status

**Planning Revision 2 is active. Phase 0 — Foundation and capture probe is in progress.**

Phase 0A retains the Android/Compose bootstrap, branding, toolchain, CI, and J8-safe developer commands. Phase 0B now contains an explicit-start, bounded diagnostic `AudioRecord` path for the J8. It reports source/rate attempts, selected stream configuration, route, monotonic timestamp availability, sample counts, and digital RMS/peak levels.

The diagnostic values are **dBFS, not SPL**. The public export layout remains provisional until a real Phase 1 session is captured/exported and inspected independently.

- [Detailed development plan — Revision 2](docs/planning/cuicatl-development-plan-2026-10-08.md)
- [Planning review disposition](docs/planning/cuicatl-planning-review-disposition-2026-10-08.md)
- [Phase 0 implementation contracts](docs/architecture/phase-0-contracts.md)
- [Phase 0 J8 checklist](docs/validation/phase-0/j8-checklist.md)
- [Approved Cuicatl icon](docs/assets/branding/cuicatl-app-icon.png)

Microphone capture never starts automatically. Pressing **Start probe** is required; if permission is missing, the runtime permission prompt is triggered by that explicit action.

## Android baseline

- Application ID: `io.github.dante_souza.cuicatl`
- minSdk: 26
- compileSdk / targetSdk: 36
- JDK 17
- Gradle 8.13
- Android Gradle Plugin 8.13.2
- Kotlin 2.3.21
- Compose BOM 2026.09.00

The Samsung Galaxy J8 (Android 10 / API 29) is the Phase 0 physical capture-probe target.

## Development

```sh
make check
make build-debug
```

On the Windows lab host:

```powershell
make j8-preflight
make install-j8
make open-j8
```

To preserve the diagnostic stream from the device:

```powershell
make probe-log-j8
```

The expected debug APK is `app/build/outputs/apk/debug/app-debug.apk`.

## Current Phase 0B probe

The bounded probe tries mono PCM16 using:

1. `UNPROCESSED` at 48 kHz / 44.1 kHz when Android advertises support;
2. `VOICE_RECOGNITION` at 48 kHz / 44.1 kHz;
3. `MIC` at 48 kHz / 44.1 kHz.

The first initialized path is used for a maximum 10-second diagnostic run. Initialization proves only that the digital path opened; it does not establish physical microphone bandwidth, flatness, calibration, or accuracy.

## Product direction

- Initial sessions use Start/Stop and one fixed configuration.
- Pause/resume and multi-segment aggregation are deferred.
- Missing observations are never fabricated as silence.
- Persistence is selected in Phase 1 from a bounded recovery experiment.
- Schema 1 is finalized only from a real export fixture.
- Estimated SPL waits for a chosen and documented reference procedure.
- A bounded FFT experiment may happen early if useful, but it cannot displace the session/export milestone.

## Contribution workflow

Work proceeds through `feature/phase-*` → `dev` → `main`. Preserve published history with merge commits; do not squash phase archaeology.

## License

The repository contains the GNU Affero General Public License. Original Cuicatl application source files are declared with `SPDX-License-Identifier: AGPL-3.0-or-later`.
