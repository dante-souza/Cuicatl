# Phase 2C.1b — Galaxy J8 nominal hardware characterization

Date: 2026-10-09  
Device target: Samsung Galaxy J8 SM-J810M (`j8y18lte`)  
Status: **started — firmware/runtime characterization in progress; no nominal SPL estimate enabled**

## Purpose

This phase characterizes the J8 microphone capture chain as far as the stock firmware exposes it. It is intentionally separate from physical acoustic calibration.

The goal is to distinguish:

1. **runtime-confirmed facts** — observed while Cuicatl is actively recording;
2. **firmware-route facts** — explicit Samsung/Qualcomm route and mixer configuration;
3. **generic/template metadata** — present in the vendor image but explicitly not guaranteed to describe this handset;
4. **unknowns** — values that still require hardware identification or an acoustic/electrical reference.

No value from this phase may be represented as calibrated SPL.

## Runtime-confirmed capture path

The live evidence under `artifacts/phase2c-j8-audio/live-capture/` confirms while Cuicatl was recording:

- Android device: `AUDIO_DEVICE_IN_BUILTIN_MIC`;
- Android device address: `bottom`;
- source: `AUDIO_SOURCE_VOICE_RECOGNITION` (source 6);
- format: PCM 16-bit;
- one input channel;
- 48,000 Hz;
- active AudioFlinger RECORD thread.

This is the application-visible path actually used by Cuicatl on the J8.

## Firmware-route characterization

Samsung's `audio_platform_info.xml` maps both:

- `SND_DEVICE_IN_REC_MAIN_MIC`; and
- `SND_DEVICE_IN_VR_MAIN_MIC`

to `builtin_mic_1`, the bottom built-in microphone.

The vendor `mixer_paths.xml` defines:

`main-mic`:
- `DEC1 MUX = ADC1`

`vr-main-mic`:
- inherits `main-mic`;
- `DEC1 Volume = 91`;
- `ADC1 Volume = 6`.

These are raw Qualcomm/Samsung mixer-control values. They are **not automatically decibels**. Converting them to physical or electrical gain requires the exact codec/control specification and confirmation that this specific route is the active HAL route.

The same vendor file contains other scenario-specific gains, confirming that Android source/route selection materially changes the capture gain. Cuicatl therefore must continue binding any reference or nominal profile to the audio source and route identity.

## Internal-versus-external codec ambiguity

The same `audio_platform_info.xml` declares two possible built-in-microphone interfaces:

- `SLIMBUS_0`, `codec_type="external"`;
- `TERT_MI2S`, `codec_type="internal"`.

This is evidence that the common firmware supports multiple codec topologies. It is **not** evidence that both are populated on this SM-J810M.

The vendor image also contains mixer definitions for WCD9306/WCD9326/WCD9330/WCD9335. File presence is not hardware detection.

Current status:

- Qualcomm MSM8953 platform: **confirmed** by device properties.
- bottom built-in main mic: **confirmed** by live Android routing.
- VR/recognition route using main mic: **strong firmware evidence**.
- exact active codec silicon (PM8953 integrated vs external WCD-family variant): **not yet runtime-confirmed**.

## Microphone-characteristics trap

The vendor XML exposes apparently attractive values for `builtin_mic_1`, including:

- sensitivity: `-37.0`;
- max SPL: `132.5`;
- min SPL: `28.5`;
- a detailed frequency-response table.

However the file immediately labels that section:

`below values are for ref purpose to OEM, doesn't contain actual hardware info on MTP`

Therefore these values are classified as **generic/template metadata** and must not be used to derive Cuicatl SPL, a microphone correction curve, or an accuracy claim.

They can be retained as firmware archaeology only.

## Current confidence matrix

| Property | Evidence | Confidence / use |
| --- | --- | --- |
| Device/platform = SM-J810M / MSM8953 | getprop + preflight | Confirmed |
| Capture device = bottom built-in mic | live AudioPolicy/AudioFlinger | Confirmed |
| Source = VOICE_RECOGNITION | live AudioPolicy/AudioFlinger | Confirmed |
| PCM16 mono 48 kHz | live AudioPolicy/AudioFlinger + Cuicatl | Confirmed |
| Logical mic = builtin_mic_1 | Samsung audio-platform mapping | Strong |
| Recognition scenario = vr-main-mic | source semantics + Samsung route definitions | Strong, runtime mixer confirmation pending |
| main mic ADC path = DEC1 <- ADC1 | mixer_paths.xml | Strong firmware route evidence |
| vr raw controls = DEC1 91 / ADC1 6 | mixer_paths.xml | Strong firmware route evidence |
| Exact codec = PM8953 integrated | service/platform evidence | Probable, not runtime-confirmed |
| External WCD codec populated | generic files only | Unproven |
| Mic sensitivity = -37 dB | explicitly template/reference metadata | **Must not calibrate from it** |
| Exact mic capsule vendor/model | none yet | Unknown |
| Physical SPL offset | no acoustic reference | Unknown / blocked |

## Nominal-estimate policy

A future hardware-derived number may only be exposed if every term in its derivation has an evidence source and uncertainty statement.

If implemented, it must use a distinct evidence state such as:

**Nominal hardware-derived estimate**

It must never be labeled:

- calibrated;
- reference-adjusted;
- certified;
- IEC class compliant.

A nominal estimate must also remain distinct in export metadata from a physical reference adjustment.

## Next evidence collection

Run the read-only characterization collector while Cuicatl is actively recording:

`make phase2c1b-characterize-j8`

The collector records:

- live AudioPolicy and AudioFlinger state;
- all readable TinyALSA mixer controls via `tinymix`;
- ASoC/debugfs visibility or the exact permission errors;
- platform/sysfs codec-related names;
- device properties;
- SHA-256 hashes.

The highest-value next result is the live `tinymix` state. It may allow us to determine whether the active path matches `vr-main-mic` and whether the internal/external backend can be distinguished without root.

## Gate to finish 2C.1b

2C.1b can be frozen when we have:

- a reproducible live mixer snapshot;
- a documented conclusion on what codec identity can/cannot be proved from stock Android;
- a documented conclusion on whether raw mixer values can be converted to gain;
- microphone capsule identification status;
- an explicit decision whether a defensible nominal SPL estimate is possible.

Until then Cuicatl remains a digital A/Z meter with reference-adjusted SPL physically blocked.
