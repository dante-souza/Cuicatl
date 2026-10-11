# Phase 2D.1 — Meter usability validation

Date: 2026-10-11  
Device: Samsung Galaxy J8 (SM-J810M), Android 10  
Input used for device review: P3/TRRS wired microphone (`h2w`)  
Branch: `feature/phase-2d1-meter-usability`

## First on-device presentation review

The first Phase 2D.1 build was exercised during an active P3/TRRS recording.

Observed live capture provenance:

```text
Capture: VOICE_RECOGNITION · 48 kHz
Input: Auto → Wired microphone · h2w
```

The revised measurement hierarchy was visually successful on the J8:

- current weighted digital level is the dominant value;
- minimum and maximum are paired;
- Leq and sample peak are paired;
- elapsed/captured time is condensed;
- source/sample rate and actual input route remain visible;
- clipping is reduced to one concise status line.

## Usability issues exposed by the first device pass

The screenshots also exposed two issues that were not apparent from code review alone:

1. The normal statement `Device measurement · absolute SPL not calibrated`
   appeared both in the application subtitle and again in the Meter card.
2. While recording, the lower configuration card continued to show disabled
   controls, raw processing diagnostics, and the idle-readiness message
   `Ready. Recording starts only when you press Start.`

The latter was especially misleading because recording was already active.

## Refinement decision

The normal uncalibrated statement remains in the application subtitle and is
removed from the Meter card. Exceptional calibration states may still appear
inside the card.

While a session is active, the full setup form is replaced with a compact
active-session summary:

```text
Active session
<session label>
<weighting> weighting · AGC <request>
Measurement configuration is fixed until this session ends.
Stop
```

The complete frequency-weighting and AGC controls remain available while idle.

Raw processing diagnostics remain preserved in saved-session detail rather than
occupying the live Meter setup card.

## Evidence hashes

The three supplied first-pass UI photographs are bound to this review by these
SHA-256 hashes:

```text
31485d954e5df336bdff7fb42e54f6293aedc6e2ef3e9368ad58e616e04c31b8
3fa8f4898a9dc703000a38ace7b05a1474ca783c0516384c117f0c893e4d4ada
fb62c53cd90aa8db57f72f0225bfe6a5226f1676bf87164977fadac2055d2554
```

The images themselves were supplied as project-conversation validation
evidence.

## Status

First-pass Meter hierarchy: **PASS with usability refinements required**.

The compact active-session refinement must pass build and J8 visual validation
before Phase 2D.1 is considered complete.
