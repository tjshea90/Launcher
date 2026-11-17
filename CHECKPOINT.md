# CHECKPOINT 1394 — read me first, then TASKS.md

**Written:** 2026-11-17T21:19:11Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b6bbb365-txbcep` · **builds on:** `6756778c` (this checkpoint is the commit after it)

## Just done
CG1b batch a (E3 E4 E5 G2: scan-by-trigger counters, Screenshot lookup only after Power off, async profileNote, PSS split in report) compiles; takeover after cut-off, hooks ok

## Do this next
next: unit tests (bash tools/gradle.sh :app:testDebugUnitTest), then section qm run, CG1b batch b, section lg, /code-review, gate by diff vs v0.9.11 (CG1g). heartbeat trig_01EzvMtE44ag8oMav4DqoqHZ (unchecked)

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
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
