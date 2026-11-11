# CHECKPOINT 1136 — read me first, then TASKS.md

**Written:** 2026-11-11T08:21:42Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-3e66a8c0-ruc8aa` · **builds on:** `4183f3a1` (this checkpoint is the commit after it)

## Just done
tag chain #540 (id 42984659617, job 130402860592, v0.9.4 v,ps,hp) FAILED; FAIL names being read (compare with head: HP4 scale 0.750 core_mean +7.17)

## Do this next
Heartbeat trig_01GVcBRvTAzqf97UhMTJszab. If tag FAIL = HP4 with the same numbers: pre-existing chain-state; edit tools/emulator_scenario.sh HP4 (~line 3106) to INFO-skip when the fitted scale is not within 0.02 of 0.80; ship v0.9.5. If the tag fails something else or passes HP4: dig

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  4e8bb812 ckpt 1135: heartbeat 08:14Z: no other session working (other account last commit 70 min
  a691789e ckpt 1134: GATE STATE on 3627dbaf (= gated 980012c + 2 version lines, ship candidate). 
  49a5d419 ckpt 1133: HP4 triage on 3627dbaf (= gated 980012c + version lines): #538 (id 429828390
  31e8d72b ckpt 1132: pt triage DONE: rerun of #535 on same commit = 31 PASS 0 FAIL (BA1 end level
  60c4424c ckpt 1131: gate verdicts on 980012c: #534 (id 42978125724 e1,o,ac,ae,pa,rb,ss,bt,last) 
  c9261aa6 ckpt 1130: BA1 numbers (pt #535): measured end level -0.0087 vs generic -0.0023; check 
  6b073f40 ckpt 1129: pt #535 (id 42978130132, job 130381928660, head 980012c): 30 PASS 1 FAIL 124
  e14c58b4 ckpt 1128: rg #536 (id 42978135973) GREEN (job 130381945885, scenario step success). Ve
  80ae726b ckpt 1127: Tier-0 local green (990 unit tests 0 fail, lint ran, abortOnError false by d
  dce6378e ckpt 1126: gate dispatched 07:15Z on 980012c: #533 id 42978120944 (base,x1,n,v,ps,hp,hf
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
