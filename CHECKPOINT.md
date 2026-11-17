# CHECKPOINT 1392 — read me first, then TASKS.md

**Written:** 2026-11-17T20:03:56Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-16772798-h13w43` · **builds on:** `68e0b18d` (this checkpoint is the commit after it)

## Just done
CG1f: QsMemory recorder (dumpsys meminfo of the stock launcher via DUMP grant, ring of 96 samples, reading R1/R2/R3) wired into wake/Control/report; QsMemoryTest 15 green. CG1a: guard now also called from Launcher.onResume (run #619 had no window event), AD5b INFO rewritten. ad run #620 id 44164339591 (head 9d37c438, shots) in progress

## Do this next
read run 44164339591 job 134326264819 (AD5b STRAYCUSTOM INFO + check); then section qm (recorder proof), CG1b batch a, section lg. heartbeat trig_01EzvMtE44ag8oMav4DqoqHZ

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  5ff672be ckpt 1391: CG1a: ad run #619 id 44161930288: 12 PASS 1 FAIL=AD5b (custom home in front 
  12775bb7 ckpt 1390: CG1a: ad run #619 id 44161930288 started 19:45Z head 5d936924 shots=true (un
  5d936924 ckpt 1389: CG1a P0+P1: StrayCustomGuard (pure, 13 tests) + guardStrayCustom in the serv
  7b0f5893 ckpt 1388: CG1: TASKS holds the reviewer findings (CG1A B1 stray-custom, CG1B x2, CG1D 
  50cb0c71 ckpt 1387: CG1e VERDICT run #618 id 44159446669 (9 PASS 0 FAIL): RRO impossible w/o roo
  efc7fe50 ckpt 1386: CG1e: ro v2 DISPATCHED run #618 id 44159446669 (19:31Z, head 3239688c, shots
  3239688c ckpt 1385: CG1e: section ro v2 = 5 overlay variants (plain / targetName / sdk28; androi
  bccc1f1e ckpt 1384: CG1c: DexoptState/DexoptWatch wired (Control, report, Setup, intake), unit t
  35dd7fe1 ckpt 1383: CG1: ro run = #617 id 44156308504 (started 19:13Z, head 0da1d72b, shots=true
  d6a7c074 ckpt 1382: CG1: emulator.yml sections=ro (shots=true) DISPATCHED on ccr-16772798-h13w43
```

(9 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
