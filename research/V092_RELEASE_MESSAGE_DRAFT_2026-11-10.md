# Draft of the answer to Tj for v0.9.2 (TASKS BQ1; written 2026-11-10 09:10 UTC BEFORE the gate, so a cut-off session can send it)

Send ONE message (OUTBOX.md must show it afterwards). Put the Release link alone on its own line, as plain text. Replace the two `<<...>>` markers with the facts of the finished release (gate run ids stay in the ckpt text and TASKS, not in the message).

---

Tj, I found both causes in your v0.9.1 report and fixed them in v0.9.2. <<If a later session sends this: the Release is built from <<commit>>, gate runs <<ids>> green.>>

**1. The screenshots of the lock screen.** They were real screenshots: the "menu dimming measurement" takes two of them (one of the lock screen, one with the power menu opening by itself) to learn how dark the stock power menu makes the screen. It measures once for the light lock screen and once for the dark one. After you use Quickstep the battery saver is on, so the lock screen wakes in the DARK look for a few seconds before the app switches the saver off. The measurement started inside that moment (it waited only 3.5 seconds), your swipe or the colour change then interrupted it, and an interrupted try was never counted, so the next lock from Quickstep started it again, up to four times a day. That is why it happened on each of your last locks.
Now: it waits 10 seconds after the screen wakes, runs at most once per lock, and EVERY try that does not finish counts: after two tries for the same lock screen and look it stops for good (the "Measure again" button in Launcher control still works). The tablet report now says what the last try was and how often it ran.

**2. The white box when you slide the notification panel back up.** The cover over the pencil could only be taken away after the app had looked at the panel, and each look is a slow call into the system (up to 1.3 seconds on your tablet, 834 of them in your report), so the box stayed behind. Now the cover is taken away the instant your finger touches the screen, with no look at all, and comes back only when the panel has been still for about half a second. The price: after you touch the open panel the pencil is visible for about half a second before it is covered again.

If either happens again, send the report right after it: the lock-screen log now has a timeline (`SCRIM run starts / run ends`, `PENCIL cover shown / hidden (why)`), so I can see which visit did what.

https://github.com/tjshea90/Launcher/releases/tag/v0.9.2

---

Facts behind the message (for the next session):
- BQ1a code: `ScrimMeasure` (SETTLE_AFTER_WAKE_MS 10 s, MAX_PER_LOCK 1, `measurementFailure` = every outcome except `MEASURED...`, `MAX_FAILURES` 2 per slot, `RUN_KEY`, `runLine`, `storedFailureCount`), `TjAccessibilityService.scrimFinish` (once per run via `scrimCounted`), `ReportState` (BA1 line: last run, runs that stored nothing, starts in 24 h). Tests: `ScrimMeasureTest` (28).
- BQ1b code: `PencilRules.FingerGuard`, `TjAccessibilityService.penFingerDown` / `updatePencil` (both guards fed at every look) / `penLockLog`. Tests: `PencilRulesTest` (31).
- Evidence: `research/tablet/2026-11-10-2/` (the report; BA1 line at report line 643; lock log 718-818). The report could NOT show which visit started which run because only the last start was kept; that is why v0.9.2 adds the run line and the lock-log lines.
