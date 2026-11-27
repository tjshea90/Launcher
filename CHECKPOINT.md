# CHECKPOINT 1699 — read me first, then TASKS.md

**Written:** 2026-11-27T23:26:30Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/wonderful-franklin-vlm90o` · **builds on:** `fe962e0e` (this checkpoint is the commit after it)

## Just done
DA1 written into TASKS (Tj's speed / RAM / CPU request, sixth pass); debug APK build started to measure packaging

## Do this next
DA1b: inspect app/build/outputs/apk/debug/*.apk (unzip -v: resources.arsc compressed? dex size), then write the perf-probe emulator section (tools/emulator_scenario.sh) for PSS split / idle CPU per thread / idle frames

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md
    M  TASKS.md

## Last ten checkpoints
```
  6e22778a ckpt 1698: CZ1 tidy: CLAUDE.md 81K to 31K (lessons to LESSONS.md), dup kotlin skill + o
  05373f92 ckpt 1697: CY1: draft wording fixed (two emulator runs, second green); job complete
  a3301a93 ckpt 1696: CY1: answer draft finalised (link, gate verdict #795/#794); heartbeat trig_0
  2c7b35b0 ckpt 1695: CY1 SHIPPED v0.9.20: Release published 15:13Z (android.yml run 61 id 4542247
  69493ceb ckpt 1694: CY1d: ship.sh OK (handoff, unit suite, lint) v0.9.20 code 59 pushed, main==h
  866c1947 ckpt 1693: gated v0.9.20 (code 59) and pushed it: the handoff self-test, the full unit 
  54223ca4 ckpt 1692: CY1c GATE #795 (id 45418554508, job 138531416509, head product==0847c754, se
  dfb42c92 ckpt 1691: CY1c: RERUN dispatched ~14:50Z emulator.yml sections base,y2,b4,o,cs on bran
  212299ff ckpt 1690: CY1c gate #794 TRIAGE 1: B4 FAILs explained = MY run composition: gate.sh BI
  717d452f ckpt 1689: CY1c GATE #794 (id 45413130411, job 138512022462, head 0847c754, base,b4,o,c
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
