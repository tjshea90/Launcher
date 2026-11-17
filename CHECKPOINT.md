# CHECKPOINT 1395 — read me first, then TASKS.md

**Written:** 2026-11-17T22:04:57Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-16772798-h13w43` · **builds on:** `4fccd572` (this checkpoint is the commit after it)

## Just done
heartbeat 22:04Z: stood down again (rule 2): session B ckpt 1400 (21:47Z, branch ccr-b6bbb365-txbcep) is 16 min old; no code touched, trigger left enabled

## Do this next
next firing 00:03Z: git fetch; if origin/main has no REAL work under 60 min old: git merge origin/main (CHECKPOINT.md conflict: take theirs), read its Do this next, continue (lg per research/CG1_SESSION_A_NOTE_2026-11-17.md, CG1f config audit, CG1b W4/G4/E1/G1/G3, /code-review, gate vs v0.9.11, release); else stand down. heartbeat trig_01EzvMtE44ag8oMav4DqoqHZ (account A, alive, next 00:03Z)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  6214dc56 ckpt 1394: heartbeat 21:55Z: STOOD DOWN (rule 2): session B (account B, branch ccr-b6bb
  81fd7345 ckpt 1393: CG1a PROVEN on emulator: ad run #620 id 44164339591 (head 9d37c438): 13 PASS
  38a7ec46 ckpt 1392: CG1f: QsMemory recorder (dumpsys meminfo of the stock launcher via DUMP gran
  5ff672be ckpt 1391: CG1a: ad run #619 id 44161930288: 12 PASS 1 FAIL=AD5b (custom home in front 
  12775bb7 ckpt 1390: CG1a: ad run #619 id 44161930288 started 19:45Z head 5d936924 shots=true (un
  5d936924 ckpt 1389: CG1a P0+P1: StrayCustomGuard (pure, 13 tests) + guardStrayCustom in the serv
  7b0f5893 ckpt 1388: CG1: TASKS holds the reviewer findings (CG1A B1 stray-custom, CG1B x2, CG1D 
  50cb0c71 ckpt 1387: CG1e VERDICT run #618 id 44159446669 (9 PASS 0 FAIL): RRO impossible w/o roo
  efc7fe50 ckpt 1386: CG1e: ro v2 DISPATCHED run #618 id 44159446669 (19:31Z, head 3239688c, shots
  3239688c ckpt 1385: CG1e: section ro v2 = 5 overlay variants (plain / targetName / sdk28; androi
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
