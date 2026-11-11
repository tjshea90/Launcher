# CHECKPOINT 1138 — read me first, then TASKS.md

**Written:** 2026-11-11T08:26:09Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-3e66a8c0-ruc8aa` · **builds on:** `ad3545de` (this checkpoint is the commit after it)

## Just done
gated v0.9.5 (code 44) and pushed it: the handoff self-test, the full unit suite + lint
and the versionCode check all passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow
android.yml, ref main, inputs {"full_build": "true"}. When that run is green,
confirm the Release is published (get_release_by_tag is enough) and then run:
  bash tools/record-release.sh v0.9.5 "v0.9.5: BT1 lock patch stays away at the first pull-up and after the PIN pad leaves, power-menu cover never half-shows after the notification panel, keyboard text without SwiftKey, Recents title Settings"

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  6f1d0a9d ckpt 1137: tag v0.9.4 chain #540 (id 42984659617, job 130402860592): 47 PASS 1 FAIL = H
  0df075f1 ckpt 1136: tag chain #540 (id 42984659617, job 130402860592, v0.9.4 v,ps,hp) FAILED; FA
  4e8bb812 ckpt 1135: heartbeat 08:14Z: no other session working (other account last commit 70 min
  a691789e ckpt 1134: GATE STATE on 3627dbaf (= gated 980012c + 2 version lines, ship candidate). 
  49a5d419 ckpt 1133: HP4 triage on 3627dbaf (= gated 980012c + version lines): #538 (id 429828390
  31e8d72b ckpt 1132: pt triage DONE: rerun of #535 on same commit = 31 PASS 0 FAIL (BA1 end level
  60c4424c ckpt 1131: gate verdicts on 980012c: #534 (id 42978125724 e1,o,ac,ae,pa,rb,ss,bt,last) 
  c9261aa6 ckpt 1130: BA1 numbers (pt #535): measured end level -0.0087 vs generic -0.0023; check 
  6b073f40 ckpt 1129: pt #535 (id 42978130132, job 130381928660, head 980012c): 30 PASS 1 FAIL 124
  e14c58b4 ckpt 1128: rg #536 (id 42978135973) GREEN (job 130381945885, scenario step success). Ve
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
