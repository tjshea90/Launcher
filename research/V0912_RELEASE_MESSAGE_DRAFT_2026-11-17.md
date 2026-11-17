# v0.9.12 answer to Tj (DRAFT, session A, 2026-11-17 22:40Z) — finish it, check EVERY sentence against a run or a test, then send it as the LAST message

Rules for this message (CLAUDE.md, BP3b): the Release link first as plain text on its own line, then the short answers, then what was MEASURED vs only REASONED, then what Tj has to do on the tablet. No sentence about a check that did not run.
Fill the `[PENDING ...]` markers from the run list (`list_workflow_runs`, `get_release_by_tag`), never from memory.

## Release
[PENDING] Release link (plain text, own line): https://github.com/tjshea90/Launcher/releases/tag/v0.9.12

## The six asks, one line each
1. **Custom launcher showing up in Quickstep (CG1a).** Found one real cause (proven): lock the tablet with an app open in Quickstep, unlock with the gesture, the app opens, and when you leave it with Back the custom home was underneath (it was pre-armed for the lock screen). Nothing sent you back. v0.9.12 has a guard: when the custom home comes to the front while Quickstep is your mode, it sends you to the stock home (switch `stray_custom_guard`, on in the release build). Emulator: run #619 reproduced it, run #620 (id 44164339591) 13 PASS 0 FAIL with the guard. If it still happens, the lock log now has a `STRAYCUSTOM` line that says why.
2. **Audit for CPU / RAM / speed (CG1b).** Six read-only reviews. Shipped only the changes that are safe without tablet measurements: three accessibility hot-path savings (one text search less per power-menu scan, a preference write off the main thread, counters in the tablet report that say which trigger starts how many scans). NOT shipped (behaviour changes, no tablet numbers): E1 / G1 / G3 in `research/CG1B_*`.
3. **Compile everything at install (CG1c).** Android 10 gives an app no switch for it. Shipped: a baseline profile (the idle compile starts with a profile), the real compile state in Launcher control and the tablet report (`DexoptWatch`). Not possible without the cable: `cmd package compile -m speed -f com.tj.launcher` (shell only). Nothing here is measured on the tablet.
4. **Wallpaper patch ready at once (CG1d).** Coded: time the screen was dark counts toward the patch settling (switch `patch_dark_settle`, on), a finger / PIN-pad hold that was running when the screen turned off is void after the wake, lock-log lines say why the patch was resting or away. [PENDING lg run: what the gesture matrix showed, with run id and PASS/FAIL/INFO.] Honest limit: a window that is created while the screen is lit still waits 1.2 s.
5. **Pencil icon without root (CG1e).** No. Run #618 (id 44159446669, 9 PASS): an overlay APK is refused or its resources are refused by the system; accessibility cannot repaint another app's icon. The cover window stays.
6. **Why Quickstep RAM grows (CG1f).** REASONED from the Android source, not measured on the tablet: the Go recents view registers a listener in a process-wide object and nothing removes it, so every re-creation of the stock launcher's screen stays in memory until that process dies; the process cannot be killed by an app. What re-creates it: font size, display size, language and dark-theme changes (they are not in the stock launcher's `configChanges`). Prime suspect from our code: the battery-saver flips (on lock, on gesture unlock, when the custom launcher takes over), because the saver flips the dark theme. Measured: the emulator shows the same pattern (stock launcher Activities 1 -> 4, PSS 49 -> 64 MB after churn, runs #621-#623). Shipped: a recorder (Launcher control + tablet report) that reads the stock launcher's memory through the shell helper every 25 minutes while the screen is in use; without the helper it says so in plain words. `research/CG1F_CONFIG_CHANGES_2026-11-17.md` has the measurement plan.

## What I need from Tj
- Start the adb helper once (as before) so the recorder can read the stock launcher's memory; send the tablet report after a day.
- Lock screen: the first side-key hold after waking the lock screen, and a pull-up of the PIN pad: tell me if the patch still shows.

## Measured / reasoned / not tested (fill the last one honestly)
- MEASURED: #618, #619, #620, #621-#623, the JVM unit tests (QsMemoryTest, PatchRestTest 46, StrayCustomGuardTest, ModeFlipsTest, DexoptStateTest), [PENDING lg, gate runs].
- REASONED only: R1 leak size on the tablet, the battery-saver-flip theory, H3 on the tablet, everything about AOT speed.
- NOT in this release (and why): CG1b E1 / G1 / G3; the opt-in idle restart of the stock launcher.

## Other session
Session B (account B) was silent from 21:48Z to 22:25Z with no CI in flight; A took over (merge `aa4fd5e2`), nothing of B's was lost. If B wakes it must merge `main` first.
