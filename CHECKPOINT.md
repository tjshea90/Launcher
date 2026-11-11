# CHECKPOINT 1133 — read me first, then TASKS.md

**Written:** 2026-11-11T08:07:48Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-3e66a8c0-ruc8aa` · **builds on:** `89ceefe0` (this checkpoint is the commit after it)

## Just done
HP4 triage on 3627dbaf (= gated 980012c + version lines): #538 (id 42982839002, hp,hf SOLO) GREEN; #539 (id 42982843044, v,ps,hp chain, job 130397053607) FAILED again (names being read). So hp passes solo, fails in the chain after v,ps twice (also #533)

## Do this next
Heartbeat trig_01GVcBRvTAzqf97UhMTJszab. Read the FAIL lines of #539 (subagent aeeba4a3141c85628 is fetching them). If HP4 with stock scale 0.750: make HP4 tolerant of the emulator scrim state (scenario only, not product code), then run hp after v,ps again; ship otherwise unchanged (product diff gated..head = version lines only)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  31e8d72b ckpt 1132: pt triage DONE: rerun of #535 on same commit = 31 PASS 0 FAIL (BA1 end level
  60c4424c ckpt 1131: gate verdicts on 980012c: #534 (id 42978125724 e1,o,ac,ae,pa,rb,ss,bt,last) 
  c9261aa6 ckpt 1130: BA1 numbers (pt #535): measured end level -0.0087 vs generic -0.0023; check 
  6b073f40 ckpt 1129: pt #535 (id 42978130132, job 130381928660, head 980012c): 30 PASS 1 FAIL 124
  e14c58b4 ckpt 1128: rg #536 (id 42978135973) GREEN (job 130381945885, scenario step success). Ve
  80ae726b ckpt 1127: Tier-0 local green (990 unit tests 0 fail, lint ran, abortOnError false by d
  dce6378e ckpt 1126: gate dispatched 07:15Z on 980012c: #533 id 42978120944 (base,x1,n,v,ps,hp,hf
  980012c1 ckpt 1125: TAKEOVER (branch ccr-3e66a8c0-ruc8aa) on Tj's order: other account hit its u
  11c1c313 ckpt 1124: review A fixes in (focus tolerance 450 + launcherFocus rule, warm card short
  9671f217 ckpt 1123: review B (HIGH fixed): lock patch return = one level-triggered timer (PatchR
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
