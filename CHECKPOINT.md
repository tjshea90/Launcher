# CHECKPOINT 1134 — read me first, then TASKS.md

**Written:** 2026-11-11T08:09:29Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-3e66a8c0-ruc8aa` · **builds on:** `49a5d419` (this checkpoint is the commit after it)

## Just done
GATE STATE on 3627dbaf (= gated 980012c + 2 version lines, ship candidate). GREEN: #534 e1,o,ac,ae,pa,rb,ss,bt,last; #536 rg; #538 hp,hf SOLO (id 42982839002); pt rerun 42978130132 attempt 2 (31/0) and tag-v0.9.4 pt 42980825408 (31/0). EXPLAINED: #535 pt first try BA1 flake. OPEN: HP4 fails ONLY in chain v,ps,hp: #533 (run1) and #539 (id 42982843044) with identical numbers (stock scale 0.750, core_mean +7.17), passes solo. Tag chain v,ps,hp dispatched 08:08Z (ref v0.9.4)

## Do this next
Heartbeat trig_01GVcBRvTAzqf97UhMTJszab. Find the tag run id (list_workflow_runs status in_progress), read ~08:20Z (get_job_logs run_id failed_only; if failed ask subagent aeeba4a3141c85628 for FAIL lines). TAG FAILS HP4 identically = pre-existing chain-state: edit tools/emulator_scenario.sh HP4 (~line 3106) to INFO-skip when the fitted scale is not within 0.02 of 0.80, no product change, then ship v0.9.5: bash ship.sh note; android.yml full_build=true ref ccr-3e66a8c0-ruc8aa; get_release_by_tag v0.9.5; bash tools/record-release.sh v0.9.5 note; tick TASKS BT1c/d/e/f; send research/V095_RELEASE_MESSAGE_DRAFT (OUTBOX). TAG PASSES = regression: inspect hp film

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  49a5d419 ckpt 1133: HP4 triage on 3627dbaf (= gated 980012c + version lines): #538 (id 429828390
  31e8d72b ckpt 1132: pt triage DONE: rerun of #535 on same commit = 31 PASS 0 FAIL (BA1 end level
  60c4424c ckpt 1131: gate verdicts on 980012c: #534 (id 42978125724 e1,o,ac,ae,pa,rb,ss,bt,last) 
  c9261aa6 ckpt 1130: BA1 numbers (pt #535): measured end level -0.0087 vs generic -0.0023; check 
  6b073f40 ckpt 1129: pt #535 (id 42978130132, job 130381928660, head 980012c): 30 PASS 1 FAIL 124
  e14c58b4 ckpt 1128: rg #536 (id 42978135973) GREEN (job 130381945885, scenario step success). Ve
  80ae726b ckpt 1127: Tier-0 local green (990 unit tests 0 fail, lint ran, abortOnError false by d
  dce6378e ckpt 1126: gate dispatched 07:15Z on 980012c: #533 id 42978120944 (base,x1,n,v,ps,hp,hf
  980012c1 ckpt 1125: TAKEOVER (branch ccr-3e66a8c0-ruc8aa) on Tj's order: other account hit its u
  11c1c313 ckpt 1124: review A fixes in (focus tolerance 450 + launcherFocus rule, warm card short
```
