# Phase 2B.2 — live capture integration gate

Date: 2026-10-09. Branch: `feature/phase-2-meter-reference-adjustment`.

Status: implementation design ready; integration NOT yet merged or locally validated.

## Compatibility invariants

1. Keep the existing Phase 1 `mean_square_fs`, `rms_fs`, `rms_dbfs`, and `sample_peak_dbfs` values as raw digital measurements. Never relabel them A-weighted after the fact.
2. Persist the selected A/Z weighting at session Start and refuse mid-session changes; use one filter instance across reads and 100 ms intervals.
3. Keep old 15-field `frames.tsv` sessions readable. Append new weighted energy and weighted level fields in a backward-readable record revision; verify parser recovery for incomplete trailing rows.
4. Compute weighted digital minimum/maximum on valid interval levels; calculate weighted Leq by energy and actual sample duration, not arithmetic dB averages. Missing data is not silence; digital zero is zero energy with no finite logarithmic current level.
5. Show the selected weighting, weighted current/min/max/Leq and the label **digital dBFS — uncalibrated**, not SPL.
6. Preserve the frozen CSV Schema 1 fields. Its existing raw digital columns must remain raw. The reserved SPL fields must remain empty until the physical reference-adjustment gate. Do not quietly put A-weighted dBFS into an SPL field. Before sharing A-weighted digital data, make and document a separate export schema/version decision.
7. Reopen older Z-default sessions and current A/Z sessions; compare persisted energy with displayed statistics and export.
8. Keep route-change interruption, recovery, continuous capture during navigation and notification Stop behavior unchanged.

## Required deterministic validation

- Exact Z identity at 44100 and 48000 Hz.
- A-response fixtures on both rates with explicit tolerance; independent review of filter coefficients.
- Continuity across varying read-buffer and frame boundaries.
- Weighted energy accumulation, partial-interval accounting, and zero/missing behavior.
- Start/Stop fixes one weighting for the whole session.
- Legacy session parsing and process-kill recovery.
- Schema 1 column/order/regression tests.

## Device gate

Run `make check` before `make install-j8`; on J8 capture separate A and Z sessions with the same source and AGC request, inspect levels and stored records, save and reopen both, verify History and CSV, and preserve original screenshots, APK hash, and session metadata. No calibration or physical SPL-accuracy claim follows from this digital test.
