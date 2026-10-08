# Cuicatl

Cuicatl is a planned Android sound-analysis platform in the CATL family: capture a measurement session, inspect it through focused analysis views, preserve the results, and export/share the data.

## Project status

The visual identity is approved. Development planning is documented; the Android application has not yet been implemented.

- [Detailed development plan and technical report](docs/planning/cuicatl-development-plan-2026-10-08.md)
- [Approved Cuicatl icon](docs/assets/branding/cuicatl-app-icon.png)
- [Approved splash-screen direction](docs/assets/branding/cuicatl-splash-screen.png)
- [CATL family visual identity](docs/assets/branding/catl-family-visual-identity.png)
- [Branding asset checksums](docs/assets/branding/SHA256SUMS)

## Product direction

- Measurement sessions are the central unit of work.
- Session CSV export/share is a core requirement.
- Meter and History are the first analysis pages, with swipe navigation and visible page selectors.
- Capture continues independently of the selected page.
- Calibration, units, missing data, and input limitations remain explicit.
- Processing and session storage are local by default.

The proposed first public release includes capture, a sound meter, saved sessions, history, and CSV export/share. FFT and additional analysis views follow in separate releases. See the plan for phase gates, unresolved decisions, and validation requirements.

## Contribution workflow

Work proceeds through `feature/phase-*` branches into `dev`, then into `main`. Preserve published history; use merge commits rather than squash merges. Phase snapshots and device evidence accompany completed milestones. Cuicatl contains its product-specific work; generic scaffolding and orchestration belong in separate repositories.

## License

The repository includes the [GNU Affero General Public License v3](LICENSE). The development plan recommends declaring original application code as `AGPL-3.0-or-later` during Phase 0, consistent with the project's licensing direction.
