# CHECKPOINT 1397 — read me first, then TASKS.md

**Written:** 2026-11-17T21:29:44Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b6bbb365-txbcep` · **builds on:** `f7f43a9a` (this checkpoint is the commit after it)

## Just done
QsMemory: store why a look gave nothing (qs_mem_status, shown in describe) + qm prints it with DUMP count and avc lines; compiles, QsMemoryTest green

## Do this next
next: dispatch emulator.yml sections=qm again, read QM status INFO: if the dump is refused (SELinux) the recorder cannot work app-only: then design the fallback (ShellHelper when running, or ActivityManager.getProcessMemoryInfo for stock pid via UsageStats? no) and say so plainly to Tj. then CG1d logging, lg, review, gate. heartbeat trig_01EzvMtE44ag8oMav4DqoqHZ (unchecked)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  c5042815 ckpt 1396: qm run #621 id 44178666208 (head f6b552cc): 5 PASS 2 FAIL (QM2 no sample sto
  2a86eb80 ckpt 1395: CG1b batch a unit tests green; dispatched emulator.yml sections=qm on main a
  f6b552cc ckpt 1394: CG1b batch a (E3 E4 E5 G2: scan-by-trigger counters, Screenshot lookup only 
  81fd7345 ckpt 1393: CG1a PROVEN on emulator: ad run #620 id 44164339591 (head 9d37c438): 13 PASS
  38a7ec46 ckpt 1392: CG1f: QsMemory recorder (dumpsys meminfo of the stock launcher via DUMP gran
  5ff672be ckpt 1391: CG1a: ad run #619 id 44161930288: 12 PASS 1 FAIL=AD5b (custom home in front 
  12775bb7 ckpt 1390: CG1a: ad run #619 id 44161930288 started 19:45Z head 5d936924 shots=true (un
  5d936924 ckpt 1389: CG1a P0+P1: StrayCustomGuard (pure, 13 tests) + guardStrayCustom in the serv
  7b0f5893 ckpt 1388: CG1: TASKS holds the reviewer findings (CG1A B1 stray-custom, CG1B x2, CG1D 
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
