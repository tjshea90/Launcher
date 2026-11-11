# CHECKPOINT 1132 — read me first, then TASKS.md

**Written:** 2026-11-11T07:55:06Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-3e66a8c0-ruc8aa` · **builds on:** `3627dbaf` (this checkpoint is the commit after it)

## Just done
pt triage DONE: rerun of #535 on same commit = 31 PASS 0 FAIL (BA1 end level +0.0004), tag v0.9.4 pt #537 = 31/0 (+0.0003): first BA1 fail was a noisy film. HP4 triage dispatched 07:53Z on 3627dbaf (= gated 980012c + 2 version lines): #538 id 42982839002 hp,hf on branch; #539 id 42982843044 v,ps,hp on main

## Do this next
Heartbeat trig_01GVcBRvTAzqf97UhMTJszab. Read #538 and #539 at ~08:06Z (get_job_logs run_id + failed_only tail 45; No failed jobs + not in progress = green). Both green -> HP4 was a chain-state flake: ship (bash ship.sh note; android.yml full_build=true on ccr-3e66a8c0-ruc8aa; get_release_by_tag v0.9.5; tools/record-release.sh; tick TASKS BT1; send research/V095 draft). #538 red -> real: inspect hp events. #539 red only -> run v,ps,hp on tag v0.9.4 to see if pre-existing

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  60c4424c ckpt 1131: gate verdicts on 980012c: #534 (id 42978125724 e1,o,ac,ae,pa,rb,ss,bt,last) 
  c9261aa6 ckpt 1130: BA1 numbers (pt #535): measured end level -0.0087 vs generic -0.0023; check 
  6b073f40 ckpt 1129: pt #535 (id 42978130132, job 130381928660, head 980012c): 30 PASS 1 FAIL 124
  e14c58b4 ckpt 1128: rg #536 (id 42978135973) GREEN (job 130381945885, scenario step success). Ve
  80ae726b ckpt 1127: Tier-0 local green (990 unit tests 0 fail, lint ran, abortOnError false by d
  dce6378e ckpt 1126: gate dispatched 07:15Z on 980012c: #533 id 42978120944 (base,x1,n,v,ps,hp,hf
  980012c1 ckpt 1125: TAKEOVER (branch ccr-3e66a8c0-ruc8aa) on Tj's order: other account hit its u
  11c1c313 ckpt 1124: review A fixes in (focus tolerance 450 + launcherFocus rule, warm card short
  9671f217 ckpt 1123: review B (HIGH fixed): lock patch return = one level-triggered timer (PatchR
  cef15801 ckpt 1122: review C done (7 LOW): fixed #1 #2 (service label + AppLabelTest 3 tests gre
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
