# CHECKPOINT 1389 — read me first, then TASKS.md

**Written:** 2026-11-17T19:44:57Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-16772798-h13w43` · **builds on:** `9ff7d0d8` (this checkpoint is the commit after it)

## Just done
CG1a P0+P1: StrayCustomGuard (pure, 13 tests) + guardStrayCustom in the service + Control toggle (default ON release / OFF debug) + MODE flip log + mode_flips key (5 tests) + LAUNCHER resumed/created lines with the mode; scenario ad: guard on + AD5b (Back after the AD5 resume)

## Do this next
dispatch emulator.yml sections=ad shots=true on ccr-16772798-h13w43; read AD5 / AD5b INFO + FAILs (STRAYCUSTOM line = B1 seen on the emulator); fetch ci-shots 152-ad-back-after-resume BEFORE any other shots=true run. Then CG1d logging L1-L4 + H3. heartbeat trig_01EzvMtE44ag8oMav4DqoqHZ

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  7b0f5893 ckpt 1388: CG1: TASKS holds the reviewer findings (CG1A B1 stray-custom, CG1B x2, CG1D 
  50cb0c71 ckpt 1387: CG1e VERDICT run #618 id 44159446669 (9 PASS 0 FAIL): RRO impossible w/o roo
  efc7fe50 ckpt 1386: CG1e: ro v2 DISPATCHED run #618 id 44159446669 (19:31Z, head 3239688c, shots
  3239688c ckpt 1385: CG1e: section ro v2 = 5 overlay variants (plain / targetName / sdk28; androi
  bccc1f1e ckpt 1384: CG1c: DexoptState/DexoptWatch wired (Control, report, Setup, intake), unit t
  35dd7fe1 ckpt 1383: CG1: ro run = #617 id 44156308504 (started 19:13Z, head 0da1d72b, shots=true
  d6a7c074 ckpt 1382: CG1: emulator.yml sections=ro (shots=true) DISPATCHED on ccr-16772798-h13w43
  0da1d72b ckpt 1381: CG1e: section ro (RRO acceptance experiment: tools/make_overlay_apk.sh + emu
  7361a436 ckpt 1380: CG1: heartbeat armed trig_01EzvMtE44ag8oMav4DqoqHZ; 7 read-only reviewers la
  dc10ff72 ckpt 1379: CG1 written into TASKS (Tj's 6 asks, verbatim); prior research read (CA1 NEX
```

(7 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
