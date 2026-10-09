# Phase 2B.3 — weighted history consistency

Date: 2026-10-09. Status: **committed; local build and J8 validation pending**.

## Implemented

- Shared pure-Kotlin MeterProjection selects the appropriate energy for A or Z.
- A sessions use persisted A-weighted mean-square; absent weighted samples remain unavailable, rather than being replaced with raw levels.
- Z sessions use raw digital mean-square, including original Phase 1 15-field records.
- Live Meter and History charts display the session's selected frequency weighting.
- Saved session details display weighting and reconstructed current/minimum/maximum/Leq from persisted interval energy and durations.
- The chart retains null positions rather than connecting nonadjacent valid points across missing or digital-zero intervals.
- Added deterministic projection and aggregate tests.

## Intentional limits

- Digital dBFS only; no SPL claim or reference offset enabled.
- Frozen CSV Schema 1 continues exporting raw digital RMS/peak without a new A-weighted digital column. A-weighted digital export requires an explicit schema evolution decision.
- A/Z is fixed for a running session. Filter-state persistence across process death is not attempted; recovered sessions are finalized as interrupted.
- Existing presentation decimation remains; waveform/audio samples are not stored.

## Validation request

Run `make check`, `make install-j8`, and `make open-j8` on the dedicated Galaxy J8. In separate A and Z sessions: confirm graph labels, live levels, saved levels after reopening, unchanged session IDs during Meter/History navigation, old Phase 1 session viewing, CSV Schema 1 export, and interruption/recovery. Preserve screenshots at original resolution and record APK SHA256 and current commit.

**Not yet verified:** CI/Gradle outcome of this commit, on-device behavior and physical acoustic accuracy.

## Usability defect found on J8 — share/detail return navigation

Device evidence on 2026-10-09 showed that opening a saved session for sharing could preserve the parent scroll position, leaving the in-app Back control above the visible area. Android system Back then fell through to the Activity and could close Cuicatl instead of returning to the saved-session list.

Patched in commit `793e1d0887330af347afa553f9c836939f27f722`:

- selected saved-session ID is now saveable across Activity recreation;
- opening/closing a saved session resets the main scroll position to the top;
- Android Back is intercepted while a saved-session detail is open and returns to the History list;
- the existing explicit Back control remains available at the top of the detail view.

Validation pending on the J8: open a saved session, share CSV, return from the external target/chooser, and verify both in-app Back and Android system Back return to History without exiting Cuicatl.
