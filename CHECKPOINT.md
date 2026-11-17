# CHECKPOINT 1400 — read me first, then TASKS.md

**Written:** 2026-11-17T21:47:56Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b6bbb365-txbcep` · **builds on:** `9d692fe3` (this checkpoint is the commit after it)

## Just done
CG1f verdict: app-uid dumpsys meminfo + activity processes refused (qm runs 621-623); shell sees stock launcher Activities 1->4, PSS 49->64MB after churn; TASKS written

## Do this next
next: route QsMemory.read via ShellHelper when connected (look at ShellHelper.run + DexoptWatch.read), keep refused status; rerun qm; then CG1d L1-L4 logging, lg, /code-review, gate vs v0.9.11, release. heartbeat trig_01EzvMtE44ag8oMav4DqoqHZ (unchecked)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md
     M TASKS.md

## Last ten checkpoints
```
  45d8a44d ckpt 1399: QsMemory fallback: dumpsys activity processes -> lastPss+pid of stock launch
  0919635b ckpt 1398: qm run #622 id 44180402866: app-uid 'dumpsys meminfo' = Can't find service: 
  cf88ec38 ckpt 1397: QsMemory: store why a look gave nothing (qs_mem_status, shown in describe) +
  c5042815 ckpt 1396: qm run #621 id 44178666208 (head f6b552cc): 5 PASS 2 FAIL (QM2 no sample sto
  2a86eb80 ckpt 1395: CG1b batch a unit tests green; dispatched emulator.yml sections=qm on main a
  f6b552cc ckpt 1394: CG1b batch a (E3 E4 E5 G2: scan-by-trigger counters, Screenshot lookup only 
  81fd7345 ckpt 1393: CG1a PROVEN on emulator: ad run #620 id 44164339591 (head 9d37c438): 13 PASS
  38a7ec46 ckpt 1392: CG1f: QsMemory recorder (dumpsys meminfo of the stock launcher via DUMP gran
  5ff672be ckpt 1391: CG1a: ad run #619 id 44161930288: 12 PASS 1 FAIL=AD5b (custom home in front 
  12775bb7 ckpt 1390: CG1a: ad run #619 id 44161930288 started 19:45Z head 5d936924 shots=true (un
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
