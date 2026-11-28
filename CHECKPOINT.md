# CHECKPOINT 1705 — read me first, then TASKS.md

**Written:** 2026-11-28T00:02:27Z · **tests:** all 12 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/wonderful-franklin-vlm90o` · **builds on:** `c3a5ca9d` (this checkpoint is the commit after it)

## Just done
DA1: heartbeat armed (trig_01TAXAYoGyEPAhuzh7PzBQm8, cron 1 */2 * * *, first 02:01Z); profgen test (scratchpad pg/): class wildcard rules WORK (current baseline rules = 14784 methods HSP, 0 classes; plus Lcom/tj/launcher/** and Lcom/android/launcher3/** = +1762 classes: 1061 launcher3, 701 tj); profgen needs ABSOLUTE paths

## Do this next
wait for ci-shots commit naming run 46084969954 (watcher bxtwnvipl), read pr-summary.txt / pr-summary2.txt / pr-profile.txt (git show origin/ci-shots:<f>); decide class list size (all 1762 may be too big for an app image: use the runtime profile's classes); heartbeat trig_01TAXAYoGyEPAhuzh7PzBQm8 must be deleted (delete_trigger) when DA1 is shipped or only Tj-blocked items remain

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  c3a5ca9d ckpt 1704: DA1b: findings of run #796 written (research/DA1_PERF_PROBE_2026-11-27.md, T
  aefb878e ckpt 1703: DA1b: scenario pr gets the PIN-unlock and power-menu phases; NEW section pr2
  622ba1fb ckpt 1702: DA1 facts so far (all [V] by hand): debug APK resources.arsc + dex are STORE
  44e5756c ckpt 1701: DA1b: dispatched emulator.yml sections=pr shots=true: run #796 id 4608351418
  618215c8 ckpt 1700: DA1b: perf probe built (tools/trace_top.py ART-trace reader + selftest, emul
  35cc3cef ckpt 1699: DA1 written into TASKS (Tj's speed / RAM / CPU request, sixth pass); debug A
  6e22778a ckpt 1698: CZ1 tidy: CLAUDE.md 81K to 31K (lessons to LESSONS.md), dup kotlin skill + o
  05373f92 ckpt 1697: CY1: draft wording fixed (two emulator runs, second green); job complete
  a3301a93 ckpt 1696: CY1: answer draft finalised (link, gate verdict #795/#794); heartbeat trig_0
  2c7b35b0 ckpt 1695: CY1 SHIPPED v0.9.20: Release published 15:13Z (android.yml run 61 id 4542247
```
