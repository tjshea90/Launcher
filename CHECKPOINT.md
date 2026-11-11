# CHECKPOINT 1131 — read me first, then TASKS.md

**Written:** 2026-11-11T07:53:24Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-3e66a8c0-ruc8aa` · **builds on:** `c401bda6` (this checkpoint is the commit after it)

## Just done
gate verdicts on 980012c: #534 (id 42978125724 e1,o,ac,ae,pa,rb,ss,bt,last) GREEN; rg #536 GREEN; #533 (id 42978120944 job 130381903606 base,x1,n,v,ps,hp,hf,fp) 95 PASS 1 FAIL 108 INFO: FAIL=HP4 (open slot vs dimmed home, +7.17 levels, stock scrim fitted 0.750), INFO NATIVE CRASH #1 pid 5076 (libz inflateEnd -> je_free, launcher-loader) long before hp (hp pids 9103+); pt #535 FAIL=BA1 only (-0.0087 vs -0.0023). Repeats pt rerun + tag v0.9.4 pt finished, numbers pending

## Do this next
Heartbeat trig_01GVcBRvTAzqf97UhMTJszab. Reading: the subagent (id aeeba4a3141c85628) is fetching the pt rerun + tag results (run ids 42978130132 attempt 2, 42980825408). Then dispatch hp,hf SOLO on head (branch) and v,ps,hp on tag v0.9.4 and on main side by side to classify HP4 (theory: emulator stock scrim 0.75 vs patch 0.80 = state flake, tablet is 0.80 measured). Ship only if explained

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  c9261aa6 ckpt 1130: BA1 numbers (pt #535): measured end level -0.0087 vs generic -0.0023; check 
  6b073f40 ckpt 1129: pt #535 (id 42978130132, job 130381928660, head 980012c): 30 PASS 1 FAIL 124
  e14c58b4 ckpt 1128: rg #536 (id 42978135973) GREEN (job 130381945885, scenario step success). Ve
  80ae726b ckpt 1127: Tier-0 local green (990 unit tests 0 fail, lint ran, abortOnError false by d
  dce6378e ckpt 1126: gate dispatched 07:15Z on 980012c: #533 id 42978120944 (base,x1,n,v,ps,hp,hf
  980012c1 ckpt 1125: TAKEOVER (branch ccr-3e66a8c0-ruc8aa) on Tj's order: other account hit its u
  11c1c313 ckpt 1124: review A fixes in (focus tolerance 450 + launcherFocus rule, warm card short
  9671f217 ckpt 1123: review B (HIGH fixed): lock patch return = one level-triggered timer (PatchR
  cef15801 ckpt 1122: review C done (7 LOW): fixed #1 #2 (service label + AppLabelTest 3 tests gre
  323f69fd ckpt 1121: BT1b boxed [~], BT1 findings written to research/BT1_ANALYSIS; review slices
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
