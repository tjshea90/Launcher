# CHECKPOINT 1457 — read me first, then TASKS.md

**Written:** 2026-11-18T20:21:08Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-61fe5a34-fw0jax` · **builds on:** `84319472` (this checkpoint is the commit after it)

## Just done
CH1 code complete + review fixes (unit 1167/0, lint green). pk run #658 id 44389838382 head 84319472 in progress (dispatched 20:19:37Z)

## Do this next
1) read run 44389838382 (get_job_logs job via list_workflow_jobs, tail 40; expect PK1..PK4 PASS; FAIL = read INFO PK1/PK4 lines). 2) gate: bash tools/gate.sh 1c135985 -> 4 runs: {base,e1,t,x,ac,cs,fp,bt,pk} {x1,y2,b4,n,o,cs2,an,rt,ss,vo} {b6,n6,lq,uf,pf,rb,wf,fl,last} {rg}; dispatch each on main, ~32 min; triage reds solo + on tag v0.9.12 (BK1/BM1 lessons); NO new product code after the gate (git diff gated HEAD -- app launcher3 phoneguide must stay empty). 3) bash ship.sh note; android.yml full_build=true; get_release_by_tag; tools/record-release.sh. 4) tick TASKS CH1a-f with proofs (PocketRulesTest 26+, KeyboardPolicyTest, LinkMeter PocketRulesTest 35580, pk run), CH1g stays WAITING; HANDOFF 0 block. 5) LinkMeter repo (branch ccr-61fe5a34-fw0jax, last b27a212): plain tools/checkpoint.sh to publish its release + fill RELEASED AS. 6) answer Tj: MEASURED vs REASONED (phone half + Swoop/Gboard unseen; eco_android_on side effect; install both apps; enable Swoop in keyboard settings; stock-keyboard step; side key skips cleaning while music plays; Release links plain text). Heartbeat trig_01CxhtUYmpxg1H1vMi1NgFbF (delete it when shipped).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  84319472 ckpt 1456: pk run #657 (id 44388274246, head c7967b9c): 20 PASS 1 FAIL: PK1 OFF saw an 
  6c0d40b8 ckpt 1455: lint GREEN; review fixes in; pk run #657 id 44388274246 (head c7967b9c) in p
  c7967b9c ckpt 1454: pk run #656 (id 44386249311, head 4508c197): 20 PASS 1 FAIL (PK1 lock-prep c
  4d8ec662 ckpt 1453: CH1 review (code-review high, 10 findings): fixed 9 (OFF-path invisibleReady
  491be878 ckpt 1452: pk emulator run DISPATCHED on main (head 4508c197), sections=pk, at 19:59 UT
  4508c197 ckpt 1451: CH1f: emulator section pk written (OFF unchanged, ON ordered POCKET lines, W
  76b5ea01 ckpt 1450: CH1c/d: PocketReturn glue, ReturnReset pocket variants, service lock-prep 0/
  0c2c9bb3 ckpt 1449: CH1c: Launcher PocketRules (pure) + PocketRulesTest 26/0 GREEN
  3745e069 ckpt 1448: heartbeat armed trig_01CxhtUYmpxg1H1vMi1NgFbF (every 2h at :47, bound to thi
  32ea3990 ckpt 1447: CH1e: KeyboardPolicyTest+ReportStateTest GREEN (GRADLE_EXIT=0)
```
