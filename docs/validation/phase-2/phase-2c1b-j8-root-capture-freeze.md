# Phase 2C.1b — J8 rooted capture characterization (frozen findings)

Date: 2026-10-10. State: **capture pipeline characterization verified; absolute SPL calibration not established**.

## Device and method
- Samsung Galaxy J8 SM-J810M / j8y18lte; Android 10.
- ADB serial 38c19745; Magisk root confirmed via `su -c id`.
- Read-only script: `make phase2c1b-characterize-j8`. Root is optional and is used for ASoC debugfs and tinymix; ordinary-shell evidence is retained.
- Characterization evidence generated locally: `artifacts/phase2c-j8-audio/characterization-20261009-201933/` (not represented as uploaded/committed here).
- Separate Android audio-service snapshot taken during an active Cuicatl recording session; observed session IDs are transient, not stable configuration.

## Verified observation
- ALSA mixer name: `msm8952-snd-card`; `tinymix` enumerated 2365 controls.
- ASoC debugfs `/sys/kernel/debug/asoc` accessible under Magisk; directory includes `msm8952-snd-card`.
- Recording belongs to `io.github.dante_souza.cuicatl` UID 10225, session 25 at observation time.
- Audio policy: input 46, built-in microphone `0x80000004,@:bottom`, `AUDIO_SOURCE_VOICE_RECOGNITION` (source 6), active client; 48000 Hz, PCM 16-bit, mono.
- AudioFlinger: input thread `AudioIn_2E` (type RECORD), I/O handle 46, `Standby: no`, `Frames read: 631680`, last read 36 ms; active track session 25, 48000 Hz.
- Two session effects listed: Noise Suppression and Acoustic Echo Canceler, reported Disabled. Do not equate this with end-to-end unprocessed capture.
- Earlier empty input dumps were taken when no active input was visible; later live snapshots confirm working capture. Do not generalize earlier absence into application failure.

## Boundaries and open questions
- Mixer controls such as `ADC1/2/3 Volume = 4` and `DEC1/2 Volume = 84` were enumerated; **none has yet been proven to be Cuicatl's active hardware gain stage**.
- Capture paths, vendor DSP gains, microphone sensitivity and reference sound-pressure level remain unknown.
- No fixed dB offset or purported dB SPL calibration is justified from these observations.
- Full local `getprop` and Android service dumps may contain identifiers: sanitize before publication; preserve SHA256SUMS for any curated evidence set.
- A future controlled stopped-vs-recording mixer/DAPM comparison is optional research, not a release dependency.

## Product decision
**Root must not be a calibration prerequisite for ordinary Cuicatl installations.** Treat rooted hardware characterization as a laboratory-only, device-specific research aid. Standard app operation/measurements remain unprivileged. Meter should distinguish device-relative digital level (e.g. dBFS) from calibrated dB SPL until an external acoustic reference and repeatable per-device calibration are validated. No change to the app's recording source or meter reference is made by this freeze.

## Local evidence to preserve after redaction
- `root-status.txt`, `probe-privileges.tsv`, `tinymix-live.txt`, `asoc-debugfs-live.txt`
- `audio-policy-live.txt`, `audio-flinger-live.txt`, `collection-metadata.txt`, `SHA256SUMS.txt`
- Subsequent `policy-live.txt`, `flinger-live.txt`, `audio-live.txt` showing active session 25, captured separately in the repository working directory.

This freeze records only verified findings; it is not an absolute microphone calibration or a finalized Phase 2C meter-reference adjustment.
