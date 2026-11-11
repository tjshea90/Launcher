# CHECKPOINT 1139 — read me first, then TASKS.md

**Written:** 2026-11-11T08:27:37Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-3e66a8c0-ruc8aa` · **builds on:** `6172610b` (this checkpoint is the commit after it)

## Just done
android.yml full_build dispatched 08:26Z: Build APK run #46 id 42987042430 on 6172610b (ship.sh passed: self-test, unit suite, version 44; product diff vs gated 980012c = version lines only)

## Do this next
Heartbeat trig_01GVcBRvTAzqf97UhMTJszab. Read run 42987042430 at ~08:30Z (list_workflow_jobs / get_job_logs failed_only; android.yml done ~2.5-3 min after dispatch). Green -> get_release_by_tag v0.9.5 (target must be 6172610b), then: bash tools/record-release.sh v0.9.5 'v0.9.5: BT1 six reports (lock patch hold after PIN pad, home cover never half-shown after the shade, SwiftKey text, Recents title Settings); android.yml run 46 id 42987042430 on 6172610; gate on 980012c: e1..last #534 rg #536 hp,hf #538 green, pt #535 BA1 flake (rerun 31/0, tag 31/0), HP4 in chain v,ps,hp fails identically on tag v0.9.4 (#533 #539 #540, TASKS BT2)'. Then tick TASKS BT1c/d/e/f, send research/V095 draft

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  6172610b ckpt 1138: gated v0.9.5 (code 44) and pushed it: the handoff self-test, the full unit s
  6f1d0a9d ckpt 1137: tag v0.9.4 chain #540 (id 42984659617, job 130402860592): 47 PASS 1 FAIL = H
  0df075f1 ckpt 1136: tag chain #540 (id 42984659617, job 130402860592, v0.9.4 v,ps,hp) FAILED; FA
  4e8bb812 ckpt 1135: heartbeat 08:14Z: no other session working (other account last commit 70 min
  a691789e ckpt 1134: GATE STATE on 3627dbaf (= gated 980012c + 2 version lines, ship candidate). 
  49a5d419 ckpt 1133: HP4 triage on 3627dbaf (= gated 980012c + version lines): #538 (id 429828390
  31e8d72b ckpt 1132: pt triage DONE: rerun of #535 on same commit = 31 PASS 0 FAIL (BA1 end level
  60c4424c ckpt 1131: gate verdicts on 980012c: #534 (id 42978125724 e1,o,ac,ae,pa,rb,ss,bt,last) 
  c9261aa6 ckpt 1130: BA1 numbers (pt #535): measured end level -0.0087 vs generic -0.0023; check 
  6b073f40 ckpt 1129: pt #535 (id 42978130132, job 130381928660, head 980012c): 30 PASS 1 FAIL 124
```
