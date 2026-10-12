# Phase 2D.2 — Bluetooth routing stabilization and evidence freeze

**Frozen:** 2026-10-12  
**Status:** **FROZEN — PASS for Bluetooth-associated routed digital capture; physical microphone-source exclusivity UNVERIFIED**  
**Implementation branch:** `feature/phase-2d2-bluetooth-routing`  
**J8 implementation baseline:** `45a31ca96fe361b6096c9691b3047fb29649dff6` (before this evidence-only documentation commit)  
**Device under test:** Samsung Galaxy J8, SM-J810M, Android 10 (API 29)  
**External headset:** EDIFIER GM3 Bluetooth headset  
**Measurement units:** digital dBFS, **not calibrated SPL**.

## Scope and evidence hierarchy

This increment validates the application's handling of an explicitly requested Bluetooth SCO input on the Galaxy J8. It distinguishes four claims which must not be conflated:

1. Android reports a Bluetooth SCO input route associated with EDIFIER GM3.
2. The AudioRecord stream contains actual non-zero digital PCM observations.
3. Acoustic sound reaches the stream **exclusively through the remote headset microphone**.
4. The resulting digital data has absolute sound-pressure calibration.

The available observations support **(1) and (2)**. They do **not independently establish (3) or (4)**. A displayed route label, a connected SCO state, and non-zero PCM are insufficient on their own to prove exclusive physical source isolation.

## Implemented safeguards reviewed

The 2D.2 development sequence added:

- explicit Bluetooth SCO input selection, separate from ordinary Auto routing;
- SCO connection wait prior to capture and an 8-second connection deadline;
- a preferred-device request and refusal when the actual AudioRecord route cannot be confirmed as the selected Bluetooth SCO input;
- presentation of **requested input** separately from **actual routed capture input**;
- interruption if Android changes the capture route within a session;
- refusal of a sustained all-zero Bluetooth stream after approximately five seconds without a non-zero sample;
- Bluetooth SCO cleanup on capture shutdown;
- Android audio-settings permission declaration after the J8 logged a permission denial when attempting to start/stop SCO.

These safeguards protect session provenance; they are not physical microphone-calibration or source-isolation proofs.

## Recorded J8 experiment results

| Evidence file | Duration | Digital frames | Non-zero frames | Reported route | Result |
| --- | ---: | ---: | ---: | --- | --- |
| `bt_handshk1-923cbd17-5a88-410d-bd54-38fb1fc5127b.csv` | ~18.8 s | 188 | 188 | `type=7 · EDIFIER GM3` | PASS — Bluetooth-associated non-zero PCM |
| `bt_test-d99f70ae-13e7-4d6e-973d-57102c491377.csv` | ~133.4 s | 1,334 | 1,334 | `type=7 · EDIFIER GM3` | PASS — longer Bluetooth-associated non-zero PCM |

The second file records **33 clipped samples spread across 3 frames**; retain this as a quality observation, not a sign of SPL calibration.

Both tests used physically handled EDIFIER headset/microphone audio events. During the longer experiment, the tester backed away, covered/muffled the headset microphone by hand, introduced intervals of clicks/noises, and turned on a fan near the Android phone. The fan was reported as operating at some point in the **approximate 30–80 second region**, not as a sharply logged timestamp.

The recordings establish that the Android-reported Bluetooth input route was not merely a permanently zero-valued digital stream. They do not establish isolation of the headset microphone from the handset microphone: the hand/fan manipulation was not a controlled, independently synchronized two-source experiment. The fan observations remain **inconclusive**.

### Earlier failed capture is retained

One preceding Bluetooth trial returned **697/697 digital-zero frames** and therefore could not establish an active acoustic signal even where a route or SCO-related state was reported. Android also logged an **Audio Settings Permission Denial** for `startBluetoothSco()`/`stopBluetoothSco()` in earlier testing. This failure is **not erased** by the two later successful captures; it motivated the connection-state, permission, and zero-stream safeguards. Silent transport or muted source must never be treated as measured environmental silence.

## Evidence custody and limitations

The two named CSV exports were supplied in the project conversation during J8 testing. **They are referenced here but are not committed to this repository by this freeze.** Their exact bytes, hashes, and metadata should be preserved outside this Markdown report; a source-identical archive is still required before claiming repository-level raw-evidence reproducibility.

The SHA-256 digests of the two CSVs have **not been independently verified in this repository freeze**; no digest is asserted here.

The SHA-256 digest of `bt_test-d99f70ae-13e7-4d6e-973d-57102c491377.csv` has **not been independently recovered here**; it must **not** be fabricated. Do not claim that the two raw CSV exports are independently available from this git tree.

No calibrated SPL reference procedure was carried out. No waveform source-isolation proof was performed. Do not describe EDIFIER GM3 capture as acoustically calibrated, exclusive, or a validated absolute SPL path.

## Verification and gate

- GitHub Actions **Android CI** completed successfully at implementation SHA `45a31ca96fe361b6096c9691b3047fb29649dff6`: [run 38177551949](https://github.com/dante-souza/Cuicatl/actions/runs/38177551949).
- J8 validation: **PASS** for a routed, non-zero Bluetooth-associated digital capture in the two recorded sessions.
- J8 physical source-isolation validation: **UNVERIFIED**.
- Absolute sound-pressure calibration: **NOT PERFORMED**.
- Raw CSV Git archival/hashes: **INCOMPLETE** as described above.

**Freeze decision:** close Phase 2D.2 at this bounded evidence level. Preserve its caveats and implementation history; carry robustness and source-validation work forward to Phase 2D.3 rather than retroactively strengthening the 2D.2 claim.

## Next phase — Phase 2D.3

Harden behavior under device disconnect, SCO drops, mute/zero transport, reconnect and competing audio; provide explicit, distinct failure reasons in Meter/History and CSV provenance where the schema permits. Add deterministic tests for state-machine decisions; revalidate on the J8 without requiring root. If physical source isolation is investigated further, use a controlled and time-annotated stimulus at each microphone, and keep the result separate from absolute calibration.

**Release policy:** do not merge unverified measurement claims into the UX. Auto (built-in/wired) and explicit Bluetooth request must remain distinguishable, session configuration must stay fixed once started, and missing frames must never be invented.
