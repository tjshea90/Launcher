# CHECKPOINT 1257 — read me first, then TASKS.md

**Written:** 2026-11-12T23:08:17Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-91599ec3-za40s7` · **builds on:** `4d210d5e` (this checkpoint is the commit after it)

## Just done
SE-C2 diag INFO added to the scenario (mode, kg, battery, tj_crash, power broadcasts, TjEntrance non-ENTRANCE lines); syntax ok

## Do this next
23:16:30 timer: read tag #580 id 43325361700 + head se run (ref main, ~23:05Z); then decide env vs regression. Heartbeat trig_01JYEUXs7JosHyg85f2T1oCK

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  72b67893 ckpt 1256: tag run #580 id 43325361700 (se,ss on v0.9.7) started 23:02Z; head se rerun 
  99256c2d ckpt 1255: #576 se,ss RED on 17e10ad2 (solo, no native crash): 81P/3F SE-C2 (charger fl
  9e4ba92f ckpt 1254: reruns GREEN on 17e10ad2: #575 ps,bt 27P/0F (BT1c guard: scale 0.750, 12-lev
  ee2378fd ckpt 1253: solo reruns dispatched 22:40Z on 17e10ad2: #575 id 43322012415 ps,bt; #576 i
  17e10ad2 ckpt 1252: BT1c scale guard added (bttol: fit 0.78-0.82 -> 2 levels, else 12 + INFO; of
  970ab845 ckpt 1251: triage: bt = ENVIRONMENT (core_mean +7.17 in ctl AND all gaps = the known ps
  207b3e84 ckpt 1250: GATE RAW: #569 109P/6F all bt (BT1c-ctl, g04-never, g12-never, g12, g4-never
  b51fe2b4 ckpt 1249: gate: #569-#572 COMPLETED (verdicts being read by a haiku subagent); #573 pt
  aa965083 ckpt 1248: gate: #574 rg GREEN; #573 pt (id 43315242706, job 131521431600) still in_pro
  f9e64bea ckpt 1247: HANDOFF 0v written (v0.9.8 gate state, run ids, ship steps)
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
