# Phase 2C.1b — Galaxy J8 nominal hardware characterization

Date: 2026-10-09  
Device target: Samsung Galaxy J8 SM-J810M (`j8y18lte`)  
Status: **round 1 analyzed — active integrated codec identified; gain/microphone sensitivity characterization still incomplete; no nominal SPL estimate enabled**

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

## Codec identity — round 1 resolves the main ambiguity

The same `audio_platform_info.xml` declares two possible built-in-microphone interfaces:

- `SLIMBUS_0`, `codec_type="external"`;
- `TERT_MI2S`, `codec_type="internal"`.

That common configuration alone was ambiguous. The first 2C.1b live characterization removed most of that ambiguity.

During an active Cuicatl capture, the readable ASoC debugfs files reported these codec components:

- `msm8x16_wcd_codec-11`;
- `tfa98xx.5-0034`;
- `soc:qcom,msm-stub-codec`;
- `snd-soc-dummy`.

No WCD9306/WCD9326/WCD9330/WCD9335 codec component was registered in that ASoC snapshot.

For the Qualcomm MSM8953 platform, Qualcomm's reference kernel device tree instantiates the PM8953 `8953_wcd_codec` nodes with compatible string `qcom,msm8x16_wcd_codec`. The driver itself identifies its codec name as `msm8x16_wcd_codec`.

Therefore the evidence now supports:

- Qualcomm MSM8953 platform: **confirmed** by device properties;
- bottom built-in main mic: **confirmed** by live Android routing;
- integrated `msm8x16_wcd_codec` component: **confirmed present at runtime**;
- PM8953 integrated WCD codec path: **strongly confirmed by runtime component name + MSM8953 platform mapping**;
- external WCD93xx codec populated for the active path: **not supported by the runtime ASoC snapshot**;
- `tfa98xx.5-0034`: separately present as the NXP/TFA98xx speaker-amplifier codec component and not evidence for the microphone ADC path.

Reference sources used for hardware interpretation:

- Qualcomm/Android MSM8953 audio device tree: https://android.googlesource.com/kernel/msm/+/android-7.1.0_r0.2/arch/arm/boot/dts/qcom/msm8953-audio.dtsi
- Qualcomm `msm8x16_wcd` codec driver/header: https://android.googlesource.com/kernel/msm/+/866b728388bc6a1d774e8433482a08f11e8245d4/sound/soc/codecs/msm8x16-wcd.h

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

## Round 1 characterization result — 2026-10-09 15:48

Evidence directory:

`artifacts/phase2c-j8-audio/characterization-20261009-154858/`

The collection metadata confirms it was taken in read-only mode while Cuicatl was actively recording.

### Successful evidence

`audio-flinger-live.txt` again confirms an active RECORD thread with:

- `AUDIO_DEVICE_IN_BUILTIN_MIC`;
- `AUDIO_SOURCE_VOICE_RECOGNITION`;
- PCM16;
- mono;
- 48 kHz.

`asoc-debugfs-live.txt` is the key new artifact. Although directory listing of `/sys/kernel/debug/asoc` is restricted, the known files themselves are readable and disclose codec/DAI/platform names. The codec list contains `msm8x16_wcd_codec-11`, which materially strengthens the PM8953 integrated-codec conclusion.

### Restricted evidence

The stock Samsung build denies:

- `/proc/asound/cards`;
- `/proc/asound/pcm`;
- `/proc/device-tree/compatible`.

Those restrictions are now evidence themselves and are preserved verbatim.

### TinyALSA result

The first collector printed `Failed to open mixer` to the host terminal but stored `tinymix_not_available` because the original shell expression conflated "binary missing" with "binary present but mixer open failed".

The collector has been corrected to distinguish:

- `tinymix_not_installed`;
- `tinymix_present` plus its actual exit code/error.

At present, direct live mixer-state confirmation remains blocked on the stock build.

### Collector defect found and fixed

The first sysfs inventory command used an on-device grep expression containing pipe characters. Through the adb/shell quoting layers, those alternations were parsed as shell pipelines, producing errors such as `pm8953: inaccessible or not found`.

The collector now preserves full platform/SPMI device inventories without on-device grep and filters during analysis instead.

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
| Runtime codec component = msm8x16_wcd_codec-11 | ASoC debugfs during active capture | Confirmed present |
| PM8953 integrated WCD codec path | MSM8953 platform + runtime msm8x16_wcd component + Qualcomm MSM8953 DT mapping | Strongly confirmed |
| External WCD93xx codec populated for active path | not present in runtime ASoC codec list | No supporting runtime evidence |
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

The first run showed that `tinymix` cannot currently open the mixer on the stock build, but ASoC known files are readable. The hardened collector now records exact TinyALSA failure semantics plus ASoC card names, full platform/SPMI device inventories and `/sys/class/sound` identity files.

The next question is no longer primarily "which codec?". The higher-value unresolved questions are:

- whether the Samsung stock kernel exposes enough control metadata to convert `ADC1 Volume=6` and `DEC1 Volume=91` into defensible electrical gain;
- whether the exact microphone capsule or FPCB microphone part can be identified;
- whether any physical sensitivity can be established without an external acoustic reference.

Generic Qualcomm kernels show that these controls are TLV gain controls, but the exact dB mapping varies by codec/driver revision. We therefore will not convert the Samsung raw values to dB until the matching J8 kernel implementation is identified.

## Gate to finish 2C.1b

2C.1b can be frozen when we have:

- a reproducible live mixer snapshot;
- a documented conclusion on what codec identity can/cannot be proved from stock Android;
- a documented conclusion on whether raw mixer values can be converted to gain;
- microphone capsule identification status;
- an explicit decision whether a defensible nominal SPL estimate is possible.

Until then Cuicatl remains a digital A/Z meter with reference-adjusted SPL physically blocked.
