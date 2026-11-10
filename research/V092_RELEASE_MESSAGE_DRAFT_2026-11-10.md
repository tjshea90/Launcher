# Draft of the answer to Tj for v0.9.2 (TASKS BQ1; written 2026-11-10 09:10 UTC BEFORE the gate, so a cut-off session can send it)

Send ONE message (OUTBOX.md must show it afterwards). Put the Release link alone on its own line, as plain text. Replace the two `<<...>>` markers with the facts of the finished release (gate run ids stay in the ckpt text and TASKS, not in the message).

---

Tj, v0.9.2 is out. It fixes both problems in your report.

https://github.com/tjshea90/Launcher/releases/tag/v0.9.2

**1. The lock-screen screenshots.** They were real system screenshots: the menu-dimming measurement takes two of them to learn how dark the stock power menu makes the screen. After a Quickstep session the battery saver makes the lock screen wake dark for a few seconds, and the app tried to measure that dark look as well. That measurement never finished, an interrupted try was not counted, so it started again at your next lock (the two or three screenshots you saw). Now the dimming is measured only in the normal look, once for your wallpaper; the dark look reuses it and never gets screenshots of its own. With your wallpaper unchanged there will be no more screenshots. Only a new wallpaper, a different screen size or rotation, or the "Measure again" button can start one (it waits until the lock screen has been on for 10 seconds, runs once per lock, and gives up after two tries that stored nothing).

**2. The white box when you slide the panel back up.** The cover over the pencil was only taken away after the app had looked at the panel, and each look is a slow call into the system (up to 1.3 seconds on your tablet), so the box lagged behind the panel. Now the cover goes the instant your finger touches the screen, and again as soon as the pencil is gone, and it comes back only after the panel has been still for about half a second. The price: after you touch the open panel, the pencil is visible for about half a second before it is covered again.

What is proven: the screenshot fix by the unit tests and an emulator run (no dark-look measurement starts). The white-box fix on the emulator: when the panel closes the cover is gone about 0.1 to 0.2 seconds later, in both modes, and the pictures show no white box. One check of my long emulator test was red twice (the third panel closing in the custom-mode run). I looked at the pictures: the test's swipe made the emulator show the PIN pad with the panel still open behind it, and the cover was correctly on the pencil, so the test's assumption was wrong, not the app. I fixed the test and it is re-running (CI run 517); the result goes into TASKS.md. Neither fix has run on your tablet yet.

If either happens again, send the tablet report right after it: the lock-screen log now has a timeline (`SCRIM run starts / run ends`, `PENCIL cover shown / hidden` with the reason), so I can see which visit did what.

Still waiting on you (nothing new):
- The tablet report right after the FIRST side-key press on the custom home (the Screenshot button / visible patch you saw), and one before you open more Quickstep apps (they showed in the custom Recents).
- Start the shell helper and tap "Do everything that is missing" (usage access), so the Battery page can show real numbers.
- Two answers about notifications (where you saw one: banner, status-bar icon, shade or lock screen, and which app) and about the slow unlock (which unlock, what is on screen, about how many seconds).

---

Facts behind the message (for the next session; final design after the two reviews of 2026-11-10, `research/BQ1_REVIEW_2026-11-10.md`):
- BQ2b (Tj's question 'why not measure once'): `ScrimMeasure.State.night` + the 'waiting: ... dark look' reason, `slotOrder`, `TjAccessibilityService.scrimLoad` (dark look falls back to the light slot), scenario check `BQ2` in section pt phase E, Launcher control wording. The dark look is NEVER measured; consequence: if both tries of a NEW wallpaper are interrupted, the generic dimming stays until 'Measure again' (no cooldown on purpose: Tj's rule is 'never screenshot again').
- BQ1a code: `ScrimMeasure` (SETTLE_AFTER_WAKE_MS 10 s, MAX_PER_LOCK 1, MAX_FAILURES 2 per slot, `RUN_KEY`, `runLine`, `settleFromMs`, `slotOrder`), `TjAccessibilityService.maybeMeasureScrim` (the run is COUNTED WHEN IT STARTS, commit before the first screenshot), `scrimIngest` (clears the record when it stores a value), `scrimFinish` (end line via `setStringAsync`, the last outcome wins), `scrimTeardown` (a torn-down run says so), `ReportState` (BA1 line: last run, the stored failure records with the CURRENT wallpaper signature, starts in 24 h). Tests: `ScrimMeasureTest` (34); scenario pt phase C checks the run line and the cleared record.
- BQ1b code: `PencilRules.FingerGuard` (+`waitingAt`), `PencilRules.staleLook`, `TjAccessibilityService.penFingerDown` / `updatePencil` (both guards fed with the START of the look; stale looks decide nothing; `penAfterFinger` fast watch) / `findPencil` (hide before the whole-tree search) / `pencilWanted` / `penLockLog(shown, text)`. Tests: `PencilRulesTest` (35).
- Evidence: `research/tablet/2026-11-10-2/` (the report; BA1 line at report line 643; lock log 718-818). The report could NOT show which visit started which run because only the last start was kept; that is why v0.9.2 adds the run line and the lock-log lines.
