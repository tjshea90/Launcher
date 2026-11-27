# CHECKPOINT 1702 — read me first, then TASKS.md

**Written:** 2026-11-27T23:44:07Z · **tests:** all 12 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/wonderful-franklin-vlm90o` · **builds on:** `44e5756c` (this checkpoint is the commit after it)

## Just done
DA1 facts so far (all [V] by hand): debug APK resources.arsc + dex are STORED (no packaging win); lint perf category clean; manifest clean (only ProfileInstallerInitializer at start); release baseline.prof binary = 791 B: ONE dex of 55203 methods, ~11.4k hot methods (our wildcards expand), but only 4 CLASSES in the class set and every method flagged HSP (no layout locality, no app image); a11y config subscribes typeWindowContentChanged from all packages (CY1 decided NO to narrow). Perf probe emulator run #796 id 46083514184 (sections pr, shots=true) dispatched 23:37Z

## Do this next
1) wait for background watcher (git fetch origin ci-shots until its commit names run 46083514184), then read: git show origin/ci-shots:pr-summary.txt > scratchpad; also pr-cold.txt pr-drawer.txt pr-applaunch.txt pr-shade.txt pr-screen.txt pr-recents.txt pr-pages.txt pr-meminfo-start.txt pr-meminfo-end.txt (git show origin/ci-shots:<file>); 2) if a trace did not parse fix tools/trace_top.py from the 'trace not readable' INFO line (hexdump of the head) and rerun sections=pr; 3) rank what the hot methods say, pick provable-neutral fixes (DA1c), JVM test or emulator section for each; possible extra idea: precise HS lines + P wildcard in baseline-prof.txt for locality (neutral by construction, unproven gain)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  44e5756c ckpt 1701: DA1b: dispatched emulator.yml sections=pr shots=true: run #796 id 4608351418
  618215c8 ckpt 1700: DA1b: perf probe built (tools/trace_top.py ART-trace reader + selftest, emul
  35cc3cef ckpt 1699: DA1 written into TASKS (Tj's speed / RAM / CPU request, sixth pass); debug A
  6e22778a ckpt 1698: CZ1 tidy: CLAUDE.md 81K to 31K (lessons to LESSONS.md), dup kotlin skill + o
  05373f92 ckpt 1697: CY1: draft wording fixed (two emulator runs, second green); job complete
  a3301a93 ckpt 1696: CY1: answer draft finalised (link, gate verdict #795/#794); heartbeat trig_0
  2c7b35b0 ckpt 1695: CY1 SHIPPED v0.9.20: Release published 15:13Z (android.yml run 61 id 4542247
  69493ceb ckpt 1694: CY1d: ship.sh OK (handoff, unit suite, lint) v0.9.20 code 59 pushed, main==h
  866c1947 ckpt 1693: gated v0.9.20 (code 59) and pushed it: the handoff self-test, the full unit 
  54223ca4 ckpt 1692: CY1c GATE #795 (id 45418554508, job 138531416509, head product==0847c754, se
```
