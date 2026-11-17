# CHECKPOINT 1387 — read me first, then TASKS.md

**Written:** 2026-11-17T19:37:26Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-16772798-h13w43` · **builds on:** `930faf96` (this checkpoint is the commit after it)

## Just done
CG1e VERDICT run #618 id 44159446669 (9 PASS 0 FAIL): RRO impossible w/o root: plain overlay refused at install (different certs, lacks targetName); +targetName installs but idmap2d refuses EVERY resource (must be preinstalled or same signature) -> STATE_NO_IDMAP; sdk28 refused (must target Q+); same for SystemUI

## Do this next
write the verdict into research/PENCIL_REMOVAL_2026-11-10.md + TASKS CG1e tick; then CG1D report (research/CG1D_PATCH_LIFECYCLE_2026-11-17.md sec 7+11) -> implement L1-L4 logging + H3 fix (elapsedRealtime/books, rest at once at SCREEN_OFF) + emulator section lg; CG1F finding (IconRecentsView leak per Activity creation) -> Quickstep recorder; reviewers still running: CG1A, CG1B x2. heartbeat trig_01EzvMtE44ag8oMav4DqoqHZ

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  efc7fe50 ckpt 1386: CG1e: ro v2 DISPATCHED run #618 id 44159446669 (19:31Z, head 3239688c, shots
  3239688c ckpt 1385: CG1e: section ro v2 = 5 overlay variants (plain / targetName / sdk28; androi
  bccc1f1e ckpt 1384: CG1c: DexoptState/DexoptWatch wired (Control, report, Setup, intake), unit t
  35dd7fe1 ckpt 1383: CG1: ro run = #617 id 44156308504 (started 19:13Z, head 0da1d72b, shots=true
  d6a7c074 ckpt 1382: CG1: emulator.yml sections=ro (shots=true) DISPATCHED on ccr-16772798-h13w43
  0da1d72b ckpt 1381: CG1e: section ro (RRO acceptance experiment: tools/make_overlay_apk.sh + emu
  7361a436 ckpt 1380: CG1: heartbeat armed trig_01EzvMtE44ag8oMav4DqoqHZ; 7 read-only reviewers la
  dc10ff72 ckpt 1379: CG1 written into TASKS (Tj's 6 asks, verbatim); prior research read (CA1 NEX
  7617cde9 ckpt 1378: handoff manifest saved (.handoff/), CE1/CF1 fully done and answered; nothing
  8201dfdb ckpt 1377: CE1/CF1 answered to Tj (v0.9.11 + LinkMeter v0.2.184 released; restart/no-ca
```

(42 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
