# CHECKPOINT 1394 — read me first, then TASKS.md

**Written:** 2026-11-17T21:55:59Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-16772798-h13w43` · **builds on:** `7d4bbd3b` (this checkpoint is the commit after it)

## Just done
heartbeat 21:55Z: STOOD DOWN (rule 2): session B (account B, branch ccr-b6bbb365-txbcep, ckpt 1400 at 21:47Z) works CG1 now; A changed no product code; wrote research/CG1_SESSION_A_NOTE_2026-11-17.md (CG1d is ALREADY coded: restableSinceFor + L1/L2 logging; lg design; config-audit brief; CG1b leftovers)

## Do this next
next firing (+2h) or any wake: git fetch; if origin/main has NO real work under 60 min old: git merge origin/main (CHECKPOINT.md conflict: take theirs), read its Do this next, continue (lg per my note, CG1f config audit, CG1b W4/G4/E1/G1/G3, /code-review, gate vs v0.9.11, release); else stand down again. heartbeat trig_01EzvMtE44ag8oMav4DqoqHZ (account A, alive)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  81fd7345 ckpt 1393: CG1a PROVEN on emulator: ad run #620 id 44164339591 (head 9d37c438): 13 PASS
  38a7ec46 ckpt 1392: CG1f: QsMemory recorder (dumpsys meminfo of the stock launcher via DUMP gran
  5ff672be ckpt 1391: CG1a: ad run #619 id 44161930288: 12 PASS 1 FAIL=AD5b (custom home in front 
  12775bb7 ckpt 1390: CG1a: ad run #619 id 44161930288 started 19:45Z head 5d936924 shots=true (un
  5d936924 ckpt 1389: CG1a P0+P1: StrayCustomGuard (pure, 13 tests) + guardStrayCustom in the serv
  7b0f5893 ckpt 1388: CG1: TASKS holds the reviewer findings (CG1A B1 stray-custom, CG1B x2, CG1D 
  50cb0c71 ckpt 1387: CG1e VERDICT run #618 id 44159446669 (9 PASS 0 FAIL): RRO impossible w/o roo
  efc7fe50 ckpt 1386: CG1e: ro v2 DISPATCHED run #618 id 44159446669 (19:31Z, head 3239688c, shots
  3239688c ckpt 1385: CG1e: section ro v2 = 5 overlay variants (plain / targetName / sdk28; androi
  bccc1f1e ckpt 1384: CG1c: DexoptState/DexoptWatch wired (Control, report, Setup, intake), unit t
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
