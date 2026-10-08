# Phase 0 J8 validation checklist

Target: Samsung Galaxy J8 / SM-J810M / Android 10 (API 29).

This checklist is for the Phase 0 gate after CI succeeds and a debug APK is built.

- [ ] Confirm ADB target is exactly the lab J8 before installation.
- [ ] Install the Phase 0 debug APK.
- [ ] Confirm the approved Cuicatl launcher icon is visible.
- [ ] Launch from the launcher and confirm Cuicatl splash branding appears.
- [ ] Confirm Meter opens without requesting or activating microphone capture.
- [ ] Confirm Meter says no measurement exists.
- [ ] Switch Meter ↔ History repeatedly; shell remains stable.
- [ ] Rotate/recreate the activity; selected page may restore, but no capture starts.
- [ ] Confirm the disabled Phase 1 Start control cannot begin recording.
- [ ] Capture original-size screenshots for launcher/splash, Meter, and History.
- [ ] Record commit SHA, APK SHA256, device model, Android version, and test result.

Phase 1 probe additions: permission paths, supported sample rates, AudioRecord source, routing, reported processing support, capture timestamp quality, screen-off behavior, and storage/timing continuity.
