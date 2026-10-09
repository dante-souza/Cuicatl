# Phase 1 fixed-input interruption policy

Date: 2026-10-09

Status: **implemented; physical route-change fault injection unexercised**

## Requirement

A Phase 1 measurement session must use one fixed input configuration. Cuicatl must not silently continue one session across two different routed microphone devices.

## Implemented behavior

At capture start Cuicatl records the active Android audio route identity.

During the AudioRecord loop, before consuming each newly read PCM block, Cuicatl compares the current routed-device ID with the session's expected routed-device ID.

If Android reports a different routed-device ID, Cuicatl raises a capture failure with an interruption message describing the old and new route. The service then finalizes the session as INTERRUPTED rather than silently appending samples from the new route to the same session.

If Android initially reports no routed device and later provides one, the first concrete route becomes the fixed expected route for that session.

## What has not been exercised

No controlled wired-headset, USB-audio, or Bluetooth microphone hot-swap fixture was supplied during the J8 Phase 1 closure run.

Therefore the physical route-change path is **not claimed as device-validated**.

The acceptance status is:

- fixed-input policy: implemented;
- silent cross-route continuation: explicitly prevented in code;
- physical hot-swap behavior on the J8: unexercised;
- future device validation: perform when a suitable alternate input route is available.

This limitation does not block the current built-in-microphone Phase 1 workflow, but it remains part of the device-coverage boundary.
