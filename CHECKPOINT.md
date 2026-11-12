# CHECKPOINT 1262 — read me first, then TASKS.md

**Written:** 2026-11-12T23:37:34Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-91599ec3-za40s7` · **builds on:** `979c4d1a` (this checkpoint is the commit after it)

## Just done
#582 id 43328172010 (se,ss ref main) GREEN + #583 id 43328175947 (se,ss branch) GREEN on d5017a2c: SE-C2 / SS7 = intermittent test flakes (untouched code), diag INFO stays

## Do this next
verify git diff 72f529a1 HEAD -- app launcher3 phoneguide empty; bump versionCode 47 / 0.9.8; bash ship.sh v0.9.8; android.yml full_build=true. Heartbeat trig_01JYEUXs7JosHyg85f2T1oCK

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  8ba3cfc7 ckpt 1261: HANDOFF 0v: gate triage + rerun state written
  914160f7 ckpt 1260: dispatched 23:21Z se,ss on ref main + on branch (head d5017a2c, product == 1
  d5017a2c ckpt 1259: SS7 diag INFO added (dump size, packages, texts when no Settings entry); dry
  1e7cce30 ckpt 1258: #580 tag v0.9.7 se,ss GREEN; #581 id 43325521656 head se GREEN (SE-C2 interm
  a2a371a0 ckpt 1257: SE-C2 diag INFO added to the scenario (mode, kg, battery, tj_crash, power br
  72b67893 ckpt 1256: tag run #580 id 43325361700 (se,ss on v0.9.7) started 23:02Z; head se rerun 
  99256c2d ckpt 1255: #576 se,ss RED on 17e10ad2 (solo, no native crash): 81P/3F SE-C2 (charger fl
  9e4ba92f ckpt 1254: reruns GREEN on 17e10ad2: #575 ps,bt 27P/0F (BT1c guard: scale 0.750, 12-lev
  ee2378fd ckpt 1253: solo reruns dispatched 22:40Z on 17e10ad2: #575 id 43322012415 ps,bt; #576 i
  17e10ad2 ckpt 1252: BT1c scale guard added (bttol: fit 0.78-0.82 -> 2 levels, else 12 + INFO; of
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
