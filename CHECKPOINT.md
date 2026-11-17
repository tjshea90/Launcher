# CHECKPOINT 1388 — read me first, then TASKS.md

**Written:** 2026-11-17T19:40:05Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-16772798-h13w43` · **builds on:** `4ed8eca5` (this checkpoint is the commit after it)

## Just done
CG1: TASKS holds the reviewer findings (CG1A B1 stray-custom, CG1B x2, CG1D H1-H10, CG1F IconRecentsView listener leak); CG1e ticked (RRO impossible, run #618); android.yml baseline-profile step

## Do this next
implement CG1a P0+P1: Mode.set MODE log + mode_flips, TjHooks LAUNCHER resumed line, StrayCustomGuard (pure + JVM test) + guardStrayCustom in TjAccessibilityService next to guardStrayHome (TAS:3060), switch default ON release/OFF debug, scenario ad check (Back after AD5 resume). heartbeat trig_01EzvMtE44ag8oMav4DqoqHZ

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  50cb0c71 ckpt 1387: CG1e VERDICT run #618 id 44159446669 (9 PASS 0 FAIL): RRO impossible w/o roo
  efc7fe50 ckpt 1386: CG1e: ro v2 DISPATCHED run #618 id 44159446669 (19:31Z, head 3239688c, shots
  3239688c ckpt 1385: CG1e: section ro v2 = 5 overlay variants (plain / targetName / sdk28; androi
  bccc1f1e ckpt 1384: CG1c: DexoptState/DexoptWatch wired (Control, report, Setup, intake), unit t
  35dd7fe1 ckpt 1383: CG1: ro run = #617 id 44156308504 (started 19:13Z, head 0da1d72b, shots=true
  d6a7c074 ckpt 1382: CG1: emulator.yml sections=ro (shots=true) DISPATCHED on ccr-16772798-h13w43
  0da1d72b ckpt 1381: CG1e: section ro (RRO acceptance experiment: tools/make_overlay_apk.sh + emu
  7361a436 ckpt 1380: CG1: heartbeat armed trig_01EzvMtE44ag8oMav4DqoqHZ; 7 read-only reviewers la
  dc10ff72 ckpt 1379: CG1 written into TASKS (Tj's 6 asks, verbatim); prior research read (CA1 NEX
  7617cde9 ckpt 1378: handoff manifest saved (.handoff/), CE1/CF1 fully done and answered; nothing
```

(13 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
