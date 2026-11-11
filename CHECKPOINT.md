# CHECKPOINT 1130 — read me first, then TASKS.md

**Written:** 2026-11-11T07:38:58Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-3e66a8c0-ruc8aa` · **builds on:** `34a9fd11` (this checkpoint is the commit after it)

## Just done
BA1 numbers (pt #535): measured end level -0.0087 vs generic -0.0023; check 2 needs |m| <= |g|+0.005 = 0.0073 so it missed by 0.0014; check 1 (within 1.2%) passed; fit 0.7997/0.7999/0.7999 off 0.0303/0.0027/0.0033 = same as v0.8.2 run 272. Earlier runs: -0.0012, -0.0024. Dispatched 07:37Z: rerun of #535 (same commit 980012c, run id 42978130132 attempt 2) and pt on tag v0.9.4

## Do this next
Heartbeat trig_01GVcBRvTAzqf97UhMTJszab. Read at ~07:50Z: #533 id 42978120944, #534 id 42978125724 (get_job_logs run_id + failed_only tail 45), the pt rerun (id 42978130132, attempt 2: list_workflow_jobs shows the new job id) and the pt tag run (emulator.yml, ref v0.9.4, workflow_dispatch: list_workflow_runs resource_id emulator.yml event workflow_dispatch perPage 3). Compare BA1 lines (PT/BA1: end level) on head x2 vs tag. If BA1 differs only by noise and everything else is green: ship; write the evidence in TASKS BT1

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  6b073f40 ckpt 1129: pt #535 (id 42978130132, job 130381928660, head 980012c): 30 PASS 1 FAIL 124
  e14c58b4 ckpt 1128: rg #536 (id 42978135973) GREEN (job 130381945885, scenario step success). Ve
  80ae726b ckpt 1127: Tier-0 local green (990 unit tests 0 fail, lint ran, abortOnError false by d
  dce6378e ckpt 1126: gate dispatched 07:15Z on 980012c: #533 id 42978120944 (base,x1,n,v,ps,hp,hf
  980012c1 ckpt 1125: TAKEOVER (branch ccr-3e66a8c0-ruc8aa) on Tj's order: other account hit its u
  11c1c313 ckpt 1124: review A fixes in (focus tolerance 450 + launcherFocus rule, warm card short
  9671f217 ckpt 1123: review B (HIGH fixed): lock patch return = one level-triggered timer (PatchR
  cef15801 ckpt 1122: review C done (7 LOW): fixed #1 #2 (service label + AppLabelTest 3 tests gre
  323f69fd ckpt 1121: BT1b boxed [~], BT1 findings written to research/BT1_ANALYSIS; review slices
  9a8b660d ckpt 1120: fix compile error (comment swallowed addPatch tail); unit suite green; BT1b:
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
