# Phase 0 J8 capture-probe checklist

Target: Samsung Galaxy J8 / SM-J810M / Android 10 (API 29).

## 0A — foundation

- [ ] Confirm ADB target is exactly the lab J8 before installation.
- [ ] Install the candidate debug APK.
- [ ] Confirm the approved Cuicatl launcher icon is visible.
- [ ] Launch and confirm Cuicatl splash branding appears.
- [ ] Confirm opening the app does **not** request microphone permission.
- [ ] Confirm opening the app does **not** activate microphone capture.
- [ ] Switch Meter ↔ History and confirm the shell remains stable.

## 0B — explicit-start PCM/timing probe

- [ ] Press **Start probe** and confirm the runtime permission request appears only now if permission was not already granted.
- [ ] Deny once if practical; confirm no capture starts and the app reports denial.
- [ ] Grant permission and start the probe.
- [ ] Let one run complete for approximately 10 seconds.
- [ ] Record the selected source and actual sample rate.
- [ ] Record all attempted source/rate combinations.
- [ ] Record routed-device information.
- [ ] Record AudioTimestamp availability/fallback text.
- [ ] Record reported AEC/AGC/NS platform availability, without treating it as proof of active processing.
- [ ] Observe RMS and peak dBFS change with sound level; confirm the UI never labels these values SPL.
- [ ] Confirm nonzero sample count and plausible sample-count/duration relationship.
- [ ] Stop a second run manually and confirm a clean stop is reported.
- [ ] Switch Meter ↔ History during a run; confirm the probe does not restart automatically.
- [ ] Capture `CuicatlAudioProbe` logcat output for the evidence record.

## Evidence

Preserve:

- original-size launcher/splash and diagnostic-screen screenshots;
- candidate commit SHA;
- APK SHA256;
- J8 model, Android/API, and serial-safe device identity record;
- selected source/rate/route/timing findings;
- logcat probe output;
- concise **proceed / fallback investigation / blocker** decision and known limitations.

Phase 0 is not complete merely because the shell launches. Its gate is the runnable diagnostic APK plus actual J8 capture/timing findings and a decision record.
