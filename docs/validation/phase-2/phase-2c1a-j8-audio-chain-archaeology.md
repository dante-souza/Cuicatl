# Phase 2C.1a — Galaxy J8 live audio-chain archaeology

Date: 2026-10-09  
Device: Samsung Galaxy J8 SM-J810M  
Evidence root: `artifacts/phase2c-j8-audio/`  
Live evidence: `artifacts/phase2c-j8-audio/live-capture/`

## Confirmed while Cuicatl was recording

The live Android audio-policy and AudioFlinger dumps establish the active application-visible capture chain:

- input device: `AUDIO_DEVICE_IN_BUILTIN_MIC`;
- device address: `bottom`;
- audio source: `AUDIO_SOURCE_VOICE_RECOGNITION` (source 6);
- format: `AUDIO_FORMAT_PCM_16_BIT`;
- channel mask: mono input;
- sample rate: 48000 Hz;
- an active AudioFlinger RECORD thread was present during the dump.

This closes the earlier uncertainty about whether the static policy merely advertised a route. The route was active while Cuicatl was recording.

## Vendor topology

The Samsung/Qualcomm vendor configuration exposes distinct main-microphone routes including:

- `SND_DEVICE_IN_REC_MAIN_MIC`;
- `SND_DEVICE_IN_REC_HIGH_GAIN_MAIN_MIC`;
- `SND_DEVICE_IN_VR_MAIN_MIC`;
- mixer paths `main-mic`, `rec-main-mic`, and `vr-main-mic`.

The device properties identify the platform as Qualcomm `msm8953` / `qcom`.

The vendor image also contains mixer-path files for WCD9306/WCD9326/WCD9330/WCD9335. Their presence does not establish that any one external WCD codec is populated in this handset; common vendor images can contain multiple hardware variants. The board/service evidence for the J810 family remains more consistent with the PM8953 integrated codec path, but exact silicon should not be marked confirmed solely from these configuration filenames.

## Processing evidence

The image contains Qualcomm voice-processing libraries including Qualcomm Fluence noise suppression and acoustic echo cancellation. Their presence shows capability, not necessarily that they are active in Cuicatl's session. Cuicatl's own session audit remains the authority for effect control/state visible through Android; vendor/hardware processing not exposed through the API remains uncertain.

## /proc/asound result

Both static and live reads of:

- `/proc/asound/cards`;
- `/proc/asound/pcm`;

returned `Permission denied` on the stock J8 build. The empty files in the first attempt were therefore a stdout-only collection artifact, not evidence that ALSA card/PCM information was absent.

## Current chain model

For Cuicatl on this J8, the evidence-supported application path is:

`bottom built-in main microphone -> Samsung/Qualcomm main/VR microphone route -> Qualcomm msm8953 audio stack -> Android primary input -> PCM16 mono 48 kHz VOICE_RECOGNITION -> AudioRecord -> Cuicatl A/Z processing`

The exact microphone capsule manufacturer/model and exact codec silicon remain separate hardware-identification questions.

## Calibration consequence

This archaeology improves provenance and reference-profile matching but does not provide microphone acoustic sensitivity in V/Pa or dBV/Pa. It therefore cannot replace an acoustic reference measurement for absolute SPL. A hardware-derived nominal estimate, if implemented later, must be labeled separately from a physically reference-adjusted estimate.
