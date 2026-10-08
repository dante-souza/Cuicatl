# Phase 0 J8 capture-probe checklist

Target: Samsung Galaxy J8 / SM-J810M / Android 10 (API 29).

## 0A — foundation

- [x] Confirm ADB target is exactly the lab J8 before installation.
- [x] Install the candidate debug APK.
- [x] Confirm the approved Cuicatl launcher/app presentation is usable on the J8.
- [x] Launch Cuicatl successfully on the physical device.
- [x] Confirm opening the app does **not** activate microphone capture automatically.
- [x] Meter shell is stable on the J8.
- [ ] Meter ↔ History switching during active capture is not evidenced in the supplied validation set.

## 0B — explicit-start PCM/timing probe

- [x] Start capture only through the explicit **Start probe** action.
- [ ] Permission-denial path not re-exercised after permission had already been granted.
- [x] Grant permission and start the probe.
- [x] Let a run complete for approximately 10 seconds.
- [x] Record the selected source and actual sample rate.
- [x] Record all attempted source/rate combinations.
- [x] Record routed-device information.
- [x] Record AudioTimestamp availability/fallback behavior.
- [x] Record reported AEC/AGC/NS platform availability without treating it as proof of active processing.
- [x] Observe RMS and peak dBFS response and keep the UI explicitly non-SPL.
- [x] Confirm nonzero sample count and plausible sample-count/duration relationship.
- [x] Stop a second run manually and confirm a clean stop is reported.
- [x] Capture `CuicatlAudioProbe` logcat output for the evidence record.

## Findings

Decision: **PROCEED**

See `j8-findings-2026-10-08.md`.

Key tested path:

- `VOICE_RECOGNITION`
- 48,000 Hz
- mono PCM16
- route type 15 / `SM-J810M`
- monotonic `AudioTimestamp` available after startup
- exact 480,000 samples in the completed 10-second PCM run
- manual-stop path clean

Known limitation carried to Phase 1:

- loud transients reached 0.0 dBFS, so clipping/sample-quality flags are mandatory.

## Artifact bookkeeping still to freeze

- [ ] candidate APK SHA256

The missing checksum is bookkeeping, not a capture-path blocker. No Phase 0 release tag should be created until the final APK checksum is attached to the archaeology record.
