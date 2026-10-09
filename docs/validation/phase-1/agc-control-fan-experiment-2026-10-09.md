# Phase 1 AGC control / fan-background experiment

Date: 2026-10-09

Status: **ready for J8/device execution**

## Purpose

Cuicatl observed an apparent rise in effective microphone sensitivity/noise-floor response after a foreground sound became quiet while a stationary room fan remained unchanged.

This experiment distinguishes what can be attributed to the standard Android `AutomaticGainControl` AudioEffect from processing that remains outside Cuicatl's control or visibility.

It does **not** assume that Android AGC is the only possible source of adaptive gain.

## Pre-session control

Each new measurement session now freezes one AGC request:

- `DEFAULT` — observe the Android AGC effect state without changing it;
- `FORCE_OFF` — request the standard Android AGC effect disabled;
- `FORCE_ON` — request the standard Android AGC effect enabled.

Off/On are offered only when Android reports `AutomaticGainControl.isAvailable() == true`.

If the standard Android AGC effect is unavailable, the UI keeps Default available and explains that Off/On cannot be applied on that device.

Older sessions created before this control retain unknown/legacy AGC-request provenance rather than being retroactively labeled Default.

## Actual-state audit

At session start, Cuicatl attaches to the actual AudioRecord audio-session ID and records:

- `agc_request`;
- AGC availability / attach result;
- whether Cuicatl has effect control;
- enabled state before the request;
- apply result;
- enabled state after the request;
- NS request/result (Phase 1 requests Off);
- AEC request/result (Phase 1 requests Off);
- `vendor_or_hardware_processing=unknown`.

The full snapshot is persisted in app-private session metadata as `processing_state` and exported in CSV Schema 1's existing `processing_state` column. No Schema 1 column change is required.

Example:

```text
agc_request=FORCE_OFF; AGC=available,control=true,before=true,apply_result=0,after=false; ns_request=FORCE_OFF; NS=available,control=true,before=false,apply_result=0,after=false; aec_request=FORCE_OFF; AEC=available,control=true,before=false,apply_result=0,after=false; vendor_or_hardware_processing=unknown
```

If AGC is unavailable:

```text
agc_request=DEFAULT; AGC=unavailable; ...
```

## Acoustic sequence

Keep the fan and phone position unchanged for all comparable runs.

For each supported AGC mode:

1. 10 s — stationary fan/background only;
2. 5 s — foreground voice/claps near the phone;
3. 20 s — become quiet again while the fan remains unchanged;
4. Stop normally;
5. reopen the session and export CSV.

Suggested labels:

- `agc_default_fan`
- `agc_off_fan`
- `agc_on_fan`

Do not move the phone or change fan speed between runs.

## Interpretation

### AGC unavailable

If Android reports AGC unavailable, Off/On are not meaningful standard-API experiments on that device. Record the unavailable result and run Default once to capture the actual processing-state audit.

If the fan/background level still adapts, the observation cannot be attributed to a controllable standard Android AGC AudioEffect from this evidence. Vendor/HAL/analog or other processing remains possible.

### AGC available and controllable

Run all three modes.

- Default establishes the platform-selected state.
- Force Off must show `after=false` to count as a successful Off run.
- Force On must show `after=true` to count as a successful On run.

Compare the initial fan-only region with the post-foreground quiet region in each CSV.

If adaptation disappears only with Force Off, standard Android AGC is implicated.

If adaptation remains with verified `after=false`, the standard Android AGC effect is not sufficient to explain the behavior; other processing/gain behavior remains.

If Force On materially changes the trace relative to Force Off, Cuicatl has demonstrated meaningful control of the exposed Android AGC path.

## Phase 2 implication

A stable, calibrated SPL claim requires a sufficiently stable and characterized input path.

Phase 1 remains valid as a digital PCM/dBFS recorder even if adaptive vendor/hardware behavior exists. Phase 2 must carry the processing/gain uncertainty forward rather than calibrating through an uncontrolled adaptive path.
