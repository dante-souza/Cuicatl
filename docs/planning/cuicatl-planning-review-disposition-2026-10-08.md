# Cuicatl planning review — disposition for Revision 2

Date: 2026-10-08 (America/Sao_Paulo).

Plan: [development report, Revision 2](cuicatl-development-plan-2026-10-08.md).

Original baseline: [Revision 1 at published commit 7a96706](https://github.com/dante-souza/Cuicatl/blob/7a96706f2114f411bfc5f59fcaff57733557da2d/docs/planning/cuicatl-development-plan-2026-10-08.md).

## Review input and status

Input: the user-supplied third-party document titled “Cuicatl Development Plan Critical Review.md”, dated 2026-10-08. This record summarizes its findings and the subsequent author reassessment; it does not claim another independent review of Revision 2 has occurred or that any implementation/hardware gate has passed.

The user reviewed the proposed changes in conversation and authorized their commit/push. Revision 2 preserves the accepted product direction and changes the execution order, initial scope, and decision timing.

Android foundation work already underway remains useful. This documentation change does not merge, replace, or validate that implementation branch.

## Findings and disposition

| Finding | Disposition | Revision 2 response and rationale |
| --- | --- | --- |
| Audience/use case is vague | Accepted with qualification | Add a proposed first-use-case statement and Dante's capture/inspect/share scenario. Choose the actual acoustic task before acceptance testing. A personally useful/open-source tool need not prove a unique commercial market before experimentation; broader demand remains unvalidated. |
| Capture/timing probe is scheduled after excessive foundation work | Accepted | Move the probe to Phase 0B after only the minimum runnable foundation. Retain existing bootstrap and do essential CI/branding alongside it. |
| Export schema is frozen before real data | Accepted | Fix meanings and integrity rules early; finalize schema 1 after real capture/export inspection in Phase 1. |
| First release includes too much session/export machinery | Accepted in part | Start/Stop, fixed configuration, one active reference adjustment, and one self-contained CSV. Defer pause/resume, multiple segments, profile collections, and rich export bundles. Preserve interruption outcomes and configuration snapshots. |
| Flat-file persistence is automatically simpler and recovery is just truncating a line | Not adopted as a conclusion | Files are a candidate. Compare framing, buffered writes, metadata consistency, interruption recovery, and preservation bounds with a minimal database approach before selecting. Low row count does not settle recovery correctness. |
| J8 cannot validate modern foreground-service rules | Accepted | Add an explicit modern Android lifecycle matrix. Use emulator platform checks and separately authorized modern hardware before broad hardware-support claims. |
| Stable-device policy should be discarded | Rejected | Keep J8 as the initial hardware lab. Moto G41 and Redmi 10A remain unchanged. Cover the platform gap without treating stable phones as automatically available test devices. |
| Calibration procedure is unresolved despite being central to SPL | Accepted | Decide the actual reference/equipment/input procedure before estimated SPL implementation. Consider reference-meter comparison and external microphone/calibrator workflows distinctly. Missing reference equipment is an explicit blocker for SPL, not a reason to fabricate it. |
| Documentary process is excessive and effort needs accountability | Accepted in part | Add a bounded 1–2 focused development-day probe investigation, concrete outputs, and next-increment budgets. Keep the report as a reference and use short checklists/disposition records. A budget expiry is not acceptance. |
| Drop archaeology and original evidence preservation | Rejected | Preserve the user's established history/tag/evidence practice with proportional detail. Avoid paperwork that adds no useful evidence. |
| Two-page lateral paging should be replaced | Rejected | Retain the requested swipe direction and visible selectors. Defer graph gestures until basic history is useful and gesture ownership can be tested. |
| FFT should be explored in the first release | Conditional | Permit an early bounded basic FFT experiment. Inclusion depends on usefulness, numerical validation, and J8 cost. It is not assumed cheaper than lifecycle work or required to finish the full Phase 4 spectrum workflow. |

## Requirements preserved

- Approved Cuicatl/CATL identity and independent application responsibilities.
- Mandatory portable session CSV export/share.
- Measurement honesty: digital levels before unsupported SPL claims, missing versus digital zero, energy aggregation, clear units/reference conventions.
- One capture owner independent of page selection.
- Immutable saved measurement/configuration context.
- Local processing/storage direction and explicit capture start.
- J8-first hardware validation and unchanged stable devices.
- Feature → dev → main, published history, merge commits, phase archaeology, original-dimension evidence, and release checksums.
- Project-specific tooling only; generic scaffolding/orchestration stays outside Cuicatl.

## Decisions dependent on evidence

| Decision | Evidence/gate | Current planning treatment |
| --- | --- | --- |
| Exact first acoustic task | Dante's Phase 1 acceptance scenario | Proposed workflow; setting/event still to be chosen |
| J8 source/rate/processing/timing path | Phase 0B diagnostic data | Defaults remain provisional |
| Reference procedure and external-input scope | Equipment/setup decision before Phase 2 SPL implementation | No equipment availability or accuracy outcome assumed |
| Persistence format and recoverable loss bound | Phase 1 preservation/recovery experiment | Files and Room/SQLite are candidates |
| Schema 1 field layout | Real CSV opened/recalculated independently | Provisional until that gate |
| Basic FFT in v0.1.0 | Early experiment usefulness/numerical/performance findings | Conditional; full spectrum stays Phase 4 |
| Modern Android coverage | Explicit platform matrix and authorized physical evidence where needed | J8 does not establish modern behavior |
| Public sound-meter scope | Probe and chosen reference-workflow findings | Intended meter capability; earlier digital-only builds are clearly labeled test milestones |

## Review closure and next deliverable

Close the planning review as direction accepted with revisions and identified evidence dependencies. The next bounded deliverable is the runnable J8 capture probe plus findings. Broader roadmap items remain a reference; they do not all become prerequisites for that experiment.

Before each substantial later phase, perform author review, independent review, finding disposition, and a bounded implementation checklist. Reopen decisions when material evidence changes an assumption. Preserve earlier planning states in Git history.
