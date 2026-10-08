# Cuicatl

Cuicatl is an Android sound-analysis platform in the CATL family: capture an acoustic event, inspect changes over time, preserve the session, and export/share the data with enough context to interpret it outside the app.

## Project status

**Planning Revision 2 is the active baseline. Phase 0 is now “Foundation and capture probe.”**

The existing Android/Compose bootstrap is retained. Phase 0A covers the minimum runnable foundation; Phase 0B adds an explicit-start J8 PCM capture/timing probe. The export layout remains provisional until a real Phase 1 capture/export fixture is inspected independently.

- [Detailed development plan — Revision 2](docs/planning/cuicatl-development-plan-2026-10-08.md)
- [Planning review disposition](docs/planning/cuicatl-planning-review-disposition-2026-10-08.md)
- [Phase 0 implementation contracts](docs/architecture/phase-0-contracts.md)
- [Phase 0 validation record](docs/validation/phase-0/README.md)
- [Approved Cuicatl icon](docs/assets/branding/cuicatl-app-icon.png)
- [CATL family visual identity](docs/assets/branding/catl-family-visual-identity.png)

Microphone capture never starts automatically. The Phase 0B diagnostic path starts only after an explicit user action and runtime permission.

## Android baseline

- Application ID: `io.github.dante_souza.cuicatl`
- minSdk: 26
- compileSdk / targetSdk: 36
- JDK 17
- Gradle 8.13
- Android Gradle Plugin 8.13.2
- Kotlin 2.3.21
- Compose BOM 2026.09.00

The Samsung Galaxy J8 (Android 10 / API 29) is the initial physical capture-probe target.

## Development

Run the software gate:

```sh
make check
```

Build the debug APK:

```sh
make build-debug
```

On the Windows lab host, J8 device actions validate the known lab device before installation/open:

```powershell
make j8-preflight
make install-j8
make open-j8
```

The expected debug APK is `app/build/outputs/apk/debug/app-debug.apk`.

## Product direction

- Measurement sessions are the central unit of work.
- Session CSV export/share is mandatory, but schema 1 is finalized only from a real Phase 1 export fixture.
- Initial sessions use Start/Stop and one fixed capture/analysis configuration.
- Route, weighting, or reference-adjustment changes start a new session.
- Pause/resume and multi-segment aggregation are later capabilities.
- Missing observations are gaps, never fabricated silence.
- Estimated SPL is blocked until a reference procedure and evidence path are chosen.
- A bounded basic FFT experiment may happen early if it helps the first acoustic task, but it does not replace the session/export milestone.

## Contribution workflow

Work proceeds through `feature/phase-*` branches into `dev`, then into `main`. Preserve published history with merge commits; do not squash phase archaeology. Device evidence and artifacts accompany completed milestones.

## License

The repository contains the GNU Affero General Public License. Original Cuicatl application source files are declared with `SPDX-License-Identifier: AGPL-3.0-or-later`.
