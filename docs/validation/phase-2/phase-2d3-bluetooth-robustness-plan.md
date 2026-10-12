# Phase 2D.3 — Bluetooth capture robustness and provenance

**Started:** 2026-10-12  
**Status:** IN PROGRESS — scope/validation contract; implementation not yet validated  
**Branch:** `feature/phase-2d3-bluetooth-robustness`  
**Baseline:** `dev` after PR #8 merge commit `37089a35adf3543ef740354c908eec163e53c644`  
**Predecessor:** [Phase 2D.2 evidence freeze](../phase-2/phase-2d2-bluetooth-routing-evidence.md).

## Goal

Make explicit Bluetooth input selection reproducible and fail-safe through startup, recording, interruptions and export. Verify the actual Android capture route and observable digital signal without claiming exclusive physical microphone identity or calibrated absolute SPL.

## Required invariants

1. The requested selection (`AUTO` or `BLUETOOTH_SCO`) is distinct from the actual Android AudioRecord input route.
2. Requested configuration is frozen per session. Never silently swap to built-in or wired input after a selected Bluetooth connection fails or drops.
3. A disconnected SCO transport, changing route, failed AudioRecord state, timed-out startup, or sustained all-zero stream is explained as an interruption or unverified capture, **not as environmental silence**.
4. Export and History preserve session outcome, observed audio configuration, quality flags and failure reason, compatible with the existing CSV schema.
5. Explicit Start is required; recording does not start as a consequence of device discovery/reconnection.
6. The app works without root and does not confuse a manufacturer profile or physical SPL calibration with routing verification.

## Implementation increments

### 2D.3a — isolate route and startup state policy

- Inspect the current Bluetooth SCO acquisition/receiver lifecycle and the failure cleanup paths for races.
- Introduce independently testable capture-route/startup policy functions or a small state machine where useful; keep platform I/O isolated.
- Prefer SCO connected + confirmed `AudioRecord.routedDevice` before declaring RUNNING.
- Differentiate errors: unavailable selected device, permission denied, SCO timeout, preferred-device rejection, actual-route mismatch, route change, disconnection, zero/unverified stream, AudioRecord read failure.
- Ensure SCO resources, receivers and AudioRecord handles are closed exactly once on stop/failure; avoid orphaned microphone sessions.

### 2D.3b — signal observability and provenance

- Inspect existing PCM zero-stream guard for its false-positive risk when the user is actually silent. Treat its error as **signal unverified**, never conclusive transport failure.
- Consider a diagnostic readiness hint or evidence flag, but do not fabricate positive acoustic activity or dBFS floor.
- Ensure Meter, History and CSV communicate the requested versus actual route and explicit interruption reason without retroactively changing old session data.

### 2D.3c — deterministic regressions and J8 evidence

- Unit-test state transitions for connect/timeout/disconnect/reconnect/stop/race and route mismatch.
- Run `make check`, with CI green.
- On the J8, test **Auto built-in**, **Auto wired/P3**, **Bluetooth GM3**, selected headset absent, headset disconnect mid-session, headset reconnect only after a fresh Start, and an intentionally silent/covered input.
- Collect original CSV exports and screenshots, metadata (commit, device, Android/API, requested/actual route, sample configuration), SHA256SUMS and an outcome matrix under an evidence directory.
- Perform a separately annotated two-microphone stimulus test if possible. Its goal is only physical-source plausibility, **not** absolute SPL calibration.

## Acceptance gate

- [ ] Existing baseline has no regressions for wired Auto and built-in Auto.
- [ ] Bluetooth starts only after transport + route validation.
- [ ] Bluetooth disconnect and route changes cannot produce a silently substituted source.
- [ ] Every failed attempt has a human-readable reason.
- [ ] Saved sessions and exports faithfully preserve observable provenance.
- [ ] Android tests and GitHub CI pass.
- [ ] J8 device evidence and hashes are recorded; no proof is claimed beyond what was tested.
- [ ] Phase 2D.2 raw CSV archival gap is addressed or carried explicitly as an open archival issue.

## Non-goals

No mandatory root, no physical calibration requirement, no invented absolute SPL claim, no assumption that a connected SCO channel implies exclusive audio from the headset microphone, no unneeded general-purpose routing selector, and no change to `main` until promotion is authorized.

## Phase 2D.3a — implementation log (2026-10-12)

**Code implemented on feature branch; J8 disconnect validation pending.** This is not a freeze or a claim of device behavior.

- Added `BluetoothScoRouteGate`, a small, Android-independent state policy for WAITING → CONNECTED → terminal LOST. SCO broadcasts received before first CONNECTED do not count as successful transport; reconnection after LOST does not silently resume the same session.
- Added unit tests for connection timeout/cancel, correct route, missing route, wrong device/type, lost transport and a fresh start after reconnection.
- Explicit Bluetooth startup now requires both the SCO handshake and actual `AudioRecord.routedDevice` confirmation before reporting RUNNING.
- During recording, an actual SCO drop or disappearing/changing routed input interrupts capture before newly read samples are processed. A lost SCO broadcast attempts to unblock `AudioRecord.READ_BLOCKING`, without being classified as a user Stop.
- AudioRecord, audio preprocessors, SCO receiver and Bluetooth SCO are cleaned up before issuing terminal callbacks; this prevents a new session being displayed as ready while the old session still holds those resources.
- Auto routing behavior for built-in/P3 is deliberately left unchanged in this increment.
- Static CI gate: `testDebugUnitTest lintDebug assembleDebug`; check the final feature-head Actions run before calling it passed.

### Manual J8 gate for this increment

Using the J8 serial `38c19745`, from Windows PowerShell in the local repository:

```powershell
git fetch origin --prune
git switch feature/phase-2d3-bluetooth-robustness
git pull --ff-only
make check
make install-j8
make open-j8
```

Capture one short control session on Auto/built-in, one Auto/P3 session, then a selected EDIFIER GM3 Bluetooth session. Disconnect GM3 while recording and verify **INTERRUPTED**, route/provenance preserved, and no fallback to built-in. Reconnect and verify a **new explicit Start** is necessary. Export the session CSVs and save the original screenshots and SHA-256 manifest. Distinguish a genuinely disconnected device from a manually silent/muffled device: the former should interrupt as a transport/route event; the latter remains acoustically **unverified** unless there is non-zero signal.

**Known limitation:** Android SCO broadcasts and `AudioRecord` behavior are vendor-dependent. The J8 regression gate is essential; unit tests cannot replace it.
