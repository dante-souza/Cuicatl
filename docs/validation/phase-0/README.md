# Phase 0 validation record

Phase 0 is complete only after both automated and physical gates pass.

## Automated gate

Required on the exact candidate commit:

- unit tests
- Android lint
- debug APK assembly
- Gradle wrapper validation

GitHub Actions is the CI authority for the repository baseline.

## Physical J8 gate

Use `j8-checklist.md`. Device installation is local only and must pass `scripts/preflight-j8.ps1` before an APK is installed or opened.

Evidence to freeze after the physical check:

- original-dimension screenshots
- candidate commit SHA
- APK SHA256
- J8 identity and Android version
- concise result / known limitations

No Phase 0 tag should be created before this physical gate is complete.
