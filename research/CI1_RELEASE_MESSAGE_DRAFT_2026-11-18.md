# v0.9.14 answer to Tj (DRAFT, 2026-11-18; check every sentence against the gate runs before sending)

Answers CI1 (Tj 22:50 UTC: full pass on efficiency, RAM / CPU / battery, speed, the lock-screen and home patch, and the Quickstep RAM problem).

**The short version**
- Quickstep RAM: fixed as far as an app can fix it. When the stock launcher has grown big (200 MB or more, or 6+ screens kept alive), the custom home is
  on screen, the screen is on and unlocked, and the shell helper runs, the launcher now restarts the stock launcher process once (at most every 6 hours).
  That is the same cure as your own force stops (450 -> 280 -> 130 MB), done for you at the one moment nobody uses that process. A healthy stock launcher
  (50-130 MB) is never touched. Without the helper nothing happens.
- Speed / battery: one real saving (the Settings look-alike no longer rebuilds the whole home + drawer after every return to the custom launcher when
  the drawer is already right). The rest of the audit found nothing running while the screen is dark (no alarm, job, timer or loop in the app).
- The Screenshot patch on the lock screen and the home: I did not change it. See below for why.

**What was measured (CI emulator) and what is only reasoned**
- MEASURED, emulator section `sr` (new): restart works through the helper (stock launcher 38 MB / 1 Activity -> 11 MB / 0 after the restart), the custom home stays,
  Recents still opens, a Quickstep session still shows the stock home, the cooldown holds, a healthy launcher is left alone, no crash.
- MEASURED, emulator section `ss`: the Settings look-alike still works (73 checks green); the new "no full reload" path is read from the log line.
- MEASURED, unit tests: the new rules (when to restart, cooldown, retry after a failed try, never behind the lock screen) and the reload rule.
- GATE: the emulator gate for the diff (core set + sr, ss, bs, bl, an, rt, cs2) ended green on identical product code after re-running the sections that had single failures (a native crash of the emulator app, a timing check in base that now waits longer); no section failed twice.
- REASONED, not measured: why your stock launcher grows. The stock (MediaTek Go) launcher keeps every NEW screen of itself alive for the life of its process;
  every battery-saver flip re-creates the visible one. The lock flips the saver around every lock, so a lock + gesture unlock plausibly adds up to two kept
  screens. The emulator's stock launcher is plain Android and does not leak, so no emulator can show the growth, and no emulator can show the memory you
  get back on the tablet. Only your tablet can.
- The patch (first side-key press): on the emulator the cover already reacts 2-23 ms after the power menu announces itself, which is as fast as it can
  be measured there. The two things that could still cause a flash on the tablet (the launcher busy at the moment of the press, a cold first draw) cannot be
  seen on the emulator; the v0.9.12 lock log on your tablet is the instrument. Changing the lock-safety rules without a number would be a guess, so
  I made no change. If you still see a flash on the first press, send the tablet report right after it.

**What I need from you**
1. Start the adb helper once, as before (nothing here works without it; it dies at a reboot).
2. Install v0.9.14. Launcher control has a new switch, "Give the stock launcher's memory back (needs the shell helper)", ON by default in this build.
3. After a day or two, send the tablet report: the line "stock launcher memory refresh" says whether and when it acted and how big the stock launcher
   was before and after; the "Memory used by apps" Quickstep row (3 hours / 1 day) is the number to compare with your 445 MB.

(Release link on its own line at the end.)
