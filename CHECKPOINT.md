# CHECKPOINT 1401 — read me first, then TASKS.md

**Written:** 2026-11-17T22:21:41Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-16772798-h13w43` · **builds on:** `ba0d6586` (this checkpoint is the commit after it)

## Just done
TAKEOVER done 22:25Z (CG3b): B silent since 21:48Z (31 min), no CI in flight; merged origin/main (B ckpt 1400, tip 69453515) into A in one call (aa4fd5e2; only CHECKPOINT.md/OUTBOX.md conflicted); release diff vs v0.9.11 = 19 files +1317; gate.sh v0.9.11 asks 4 runs (x1,bs,bl,se,ps,pa,rb,ss,last | base,e1,b6,n,t,ae,fp,gear,fl | o,x,v,ac,uf,an,hp,cu | pt solo)

## Do this next
1) QsMemory read via ShellHelper (read ShellHelper.run + DexoptWatch.read, keep the refused status, JVM test), 2) write section lg (per research/CG1_SESSION_A_NOTE) and dispatch it alone, 3) /code-review high vs v0.9.11 with read-only slice reviewers BEFORE the gate, fix, 4) dispatch the 4 gate runs (ref = branch), triage, 5) bash ship.sh, android.yml full_build=true, get_release_by_tag, record-release.sh, answer Tj (measured vs reasoned; battery-saver flip theory in research/CG1F_CONFIG_CHANGES). Scope: skip CG1b E1/G1/G3. Delete the 22:28Z backup wake trig_01BMccPN2w2Qh3tb3n942Ft9 effect: ignore it when it fires. heartbeat trig_01EzvMtE44ag8oMav4DqoqHZ

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  63330b2e ckpt 1398: takeover prepared (read-only): git merge-tree dry run of origin/main into my
  497a0215 ckpt 1397: CG3d done: research/CG1F_CONFIG_CHANGES_2026-11-17.md = prime suspect for th
  7336b488 ckpt 1396: CG3 (Tj 22:06Z: monitor the other session, finish and build the release) wri
  c6e2ebb8 ckpt 1395: heartbeat 22:04Z: stood down again (rule 2): session B ckpt 1400 (21:47Z, br
  6214dc56 ckpt 1394: heartbeat 21:55Z: STOOD DOWN (rule 2): session B (account B, branch ccr-b6bb
  9d635b14 ckpt 1400: CG1f verdict: app-uid dumpsys meminfo + activity processes refused (qm runs 
  45d8a44d ckpt 1399: QsMemory fallback: dumpsys activity processes -> lastPss+pid of stock launch
  0919635b ckpt 1398: qm run #622 id 44180402866: app-uid 'dumpsys meminfo' = Can't find service: 
  cf88ec38 ckpt 1397: QsMemory: store why a look gave nothing (qs_mem_status, shown in describe) +
  c5042815 ckpt 1396: qm run #621 id 44178666208 (head f6b552cc): 5 PASS 2 FAIL (QM2 no sample sto
```

(9 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
