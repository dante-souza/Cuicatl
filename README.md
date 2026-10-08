# Cuicatl

Cuicatl is an Android sound-analysis platform in the CATL family: capture a measurement session, inspect it through focused analysis views, preserve the results, and export/share the data.

## Project status

**Phase 0 — Foundation and contracts is in progress.** The Android/Compose project, launcher/splash resources, Meter/History shell, domain contracts, schema-1 export contract, numerical fixtures, and CI baseline now live on `feature/phase-0-foundation-contracts`.

Microphone capture is intentionally not implemented in Phase 0 and never starts automatically.

- [Detailed development plan and technical report](docs/planning/cuicatl-development-plan-2026-10-08.md)
- [Phase 0 implementation contracts](docs/architecture/phase-0-contracts.md)
- [Phase 0 validation record](docs/validation/phase-0/README.md)
- [Approved Cuicatl icon](docs/assets/branding/cuicatl-app-icon.png)
- [CATL family visual identity](docs/assets/branding/catl-family-visual-identity.png)

## Android baseline

- Application ID: `io.github.dante_souza.cuicatl`
- minSdk: 26
- compileSdk / targetSdk: 36
- JDK 17
- Gradle 8.13
- Android Gradle Plugin 8.13.2
- Kotlin 2.3.21
- Compose BOM 2026.09.00

The Samsung Galaxy J8 (Android 10 / API 29) is the initial physical validation target.

## Development

Run the complete local software gate:

```sh
make check
```

Build only the debug APK:

```sh
make build-debug
```

On the Windows lab host, J8 device actions first validate the known lab device:

```powershell
make j8-preflight
make install-j8
make open-j8
```

The expected Phase 0 debug APK is `app/build/outputs/apk/debug/app-debug.apk`.

## Product direction

- Measurement sessions are the central unit of work.
- Session CSV export/share is a core requirement.
- Meter and History are the first analysis pages.
- Capture ownership is independent of presentation.
- Calibration, units, missing data, and input limitations remain explicit.
- Processing and session storage are local by default.
- Missing observations are gaps, never fabricated silence.

The proposed first public release includes capture, a sound meter, saved sessions, history, and CSV export/share. FFT and additional analysis views follow in separate releases.

## Contribution workflow

Work proceeds through `feature/phase-*` branches into `dev`, then into `main`. Preserve published history; use merge commits rather than squash merges. Phase snapshots and device evidence accompany completed milestones. Generic scaffolding and orchestration remain outside this repository.

## License

The repository contains the GNU Affero General Public License. Original Cuicatl application source files are declared with `SPDX-License-Identifier: AGPL-3.0-or-later`.
