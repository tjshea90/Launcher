# CHECKPOINT 1260 — read me first, then TASKS.md

**Written:** 2026-11-12T23:23:09Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-91599ec3-za40s7` · **builds on:** `d5017a2c` (this checkpoint is the commit after it)

## Just done
dispatched 23:21Z se,ss on ref main + on branch (head d5017a2c, product == 17e10ad2)

## Do this next
~23:36Z read both runs (list_workflow_runs branch main / branch ccr-91599ec3-za40s7, perPage 1 -> jobs -> logs tail 60 + header); 2 green -> flaky, ship v0.9.8; red -> read the SE-C2 / SS7 diag INFO. Heartbeat trig_01JYEUXs7JosHyg85f2T1oCK

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  d5017a2c ckpt 1259: SS7 diag INFO added (dump size, packages, texts when no Settings entry); dry
  1e7cce30 ckpt 1258: #580 tag v0.9.7 se,ss GREEN; #581 id 43325521656 head se GREEN (SE-C2 interm
  a2a371a0 ckpt 1257: SE-C2 diag INFO added to the scenario (mode, kg, battery, tj_crash, power br
  72b67893 ckpt 1256: tag run #580 id 43325361700 (se,ss on v0.9.7) started 23:02Z; head se rerun 
  99256c2d ckpt 1255: #576 se,ss RED on 17e10ad2 (solo, no native crash): 81P/3F SE-C2 (charger fl
  9e4ba92f ckpt 1254: reruns GREEN on 17e10ad2: #575 ps,bt 27P/0F (BT1c guard: scale 0.750, 12-lev
  ee2378fd ckpt 1253: solo reruns dispatched 22:40Z on 17e10ad2: #575 id 43322012415 ps,bt; #576 i
  17e10ad2 ckpt 1252: BT1c scale guard added (bttol: fit 0.78-0.82 -> 2 levels, else 12 + INFO; of
  970ab845 ckpt 1251: triage: bt = ENVIRONMENT (core_mean +7.17 in ctl AND all gaps = the known ps
  207b3e84 ckpt 1250: GATE RAW: #569 109P/6F all bt (BT1c-ctl, g04-never, g12-never, g12, g4-never
```
