# CHECKPOINT 1129 — read me first, then TASKS.md

**Written:** 2026-11-11T07:35:59Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-3e66a8c0-ruc8aa` · **builds on:** `3dd7d85f` (this checkpoint is the commit after it)

## Just done
pt #535 (id 42978130132, job 130381928660, head 980012c): 30 PASS 1 FAIL 124 INFO, no NATIVE CRASH. FAIL = BA1 (patch end level not further from stock than generic look +0.5%). Known-noisy check (failed on tag v0.8.8 too, CLAUDE.md v0.8.9 lessons). Triage next: compare on tag v0.9.4

## Do this next
Heartbeat trig_01GVcBRvTAzqf97UhMTJszab. Run1 #533 id 42978120944 and run2 #534 id 42978125724 still running (~07:48Z). Triage BA1: read its full FAIL line + PT/BB2 fit INFO (light slot offsets 0.0303,0.0027,0.0033 look odd), grep tools/emulator_scenario.sh BA1; dispatch pt on ref v0.9.4 AND on main side by side; a regression is only real if the tag passes and head fails twice

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  e14c58b4 ckpt 1128: rg #536 (id 42978135973) GREEN (job 130381945885, scenario step success). Ve
  80ae726b ckpt 1127: Tier-0 local green (990 unit tests 0 fail, lint ran, abortOnError false by d
  dce6378e ckpt 1126: gate dispatched 07:15Z on 980012c: #533 id 42978120944 (base,x1,n,v,ps,hp,hf
  980012c1 ckpt 1125: TAKEOVER (branch ccr-3e66a8c0-ruc8aa) on Tj's order: other account hit its u
  11c1c313 ckpt 1124: review A fixes in (focus tolerance 450 + launcherFocus rule, warm card short
  9671f217 ckpt 1123: review B (HIGH fixed): lock patch return = one level-triggered timer (PatchR
  cef15801 ckpt 1122: review C done (7 LOW): fixed #1 #2 (service label + AppLabelTest 3 tests gre
  323f69fd ckpt 1121: BT1b boxed [~], BT1 findings written to research/BT1_ANALYSIS; review slices
  9a8b660d ckpt 1120: fix compile error (comment swallowed addPatch tail); unit suite green; BT1b:
  c0c1840a ckpt 1119: bt run #532 (id 42972187857, job 130362986310, head eae2cb4) GREEN: ctl/g04/
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
