# Phase 2D.0a — J8 P3/TRRS automatic input routing

Date: 2026-10-11  
Device: Samsung Galaxy J8 (SM-J810M), Android 10  
Branch: `feature/phase-2d0-input-routing`

## Purpose

Establish the real Android/J8 recording-route behavior before adding any manual
input selector. The test intentionally relies on Android's normal automatic
routing policy.

## Results

| Session | Outcome | Actual input identity | Source | Format | Captured |
| --- | --- | --- | --- | --- | --- |
| `routing_buitin` | COMPLETED | `type=15 · SM-J810M` | VOICE_RECOGNITION | 48000 Hz PCM16 mono | 40020 ms |
| `routing_p3` | COMPLETED | `type=3 · h2w` | VOICE_RECOGNITION | 48000 Hz PCM16 mono | 47740 ms |
| `routing_p3_unplugg` | INTERRUPTED | started `type=3 · h2w` | VOICE_RECOGNITION | 48000 Hz PCM16 mono | 9600 ms |

Android `AudioDeviceInfo` device type 15 is the built-in microphone and type 3
is a wired headset input.

### Route-change protection

The P3 unplug test was interrupted rather than silently continuing on the
built-in microphone. The persisted interruption reason is:

```text
Input route changed during capture: type=3 · h2w -> type=15 · SM-J810M
```

This demonstrates that the current capture path freezes the routed input for a
session and fails closed when Android changes it.

## Evidence integrity

The supplied CSV exports have these SHA-256 digests:

```text
3ec563f17688371421c6793c6663be2d73696a375b7f6537e10a102fd177f76d  routing_buitin
a146926f31eb18193ba77e9c27139ff3038b5e09922b2f360fe25bf06fb2a7d4  routing_p3
751ab309363d3f3cec15974b274d538c172629ab9e38a23ba205e96f70d9c396  routing_p3_unplugg
```

Raw CSVs were supplied as validation evidence in the project conversation.
The hashes above bind this report to those exact exports.

## Product conclusion

For P3/TRRS on the J8:

1. Android automatically changes the recording route when the wired microphone
   is connected.
2. Cuicatl does not need to override that normal behavior.
3. The UI should describe the selected mode as **Auto** and the actual routed
   device separately.
4. Manual preferred-device selection should remain a later increment, only if
   testing with multiple simultaneous inputs demonstrates a useful need.
5. An input-route change during capture remains a session-integrity boundary
   and must interrupt the session.

Phase 2D.0a can therefore proceed with **Auto-first input discovery and
human-readable actual-route presentation**, without a manual selector.
