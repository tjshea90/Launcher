# CHECKPOINT 1255 — read me first, then TASKS.md

**Written:** 2026-11-12T23:02:26Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-91599ec3-za40s7` · **builds on:** `9e4ba92f` (this checkpoint is the commit after it)

## Just done
#576 se,ss RED on 17e10ad2 (solo, no native crash): 81P/3F SE-C2 (charger flick in/out/in), SE-L (entrance log lacks charger), SS7 (drawer Settings entry bounds none)

## Do this next
dispatch se,ss on tag v0.9.7 to compare; meanwhile read the gate's se/ss FAILs (ckpt 1250/1251) + diff v0.9.7..HEAD for charger / drawer. Heartbeat trig_01JYEUXs7JosHyg85f2T1oCK

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  9e4ba92f ckpt 1254: reruns GREEN on 17e10ad2: #575 ps,bt 27P/0F (BT1c guard: scale 0.750, 12-lev
  ee2378fd ckpt 1253: solo reruns dispatched 22:40Z on 17e10ad2: #575 id 43322012415 ps,bt; #576 i
  17e10ad2 ckpt 1252: BT1c scale guard added (bttol: fit 0.78-0.82 -> 2 levels, else 12 + INFO; of
  970ab845 ckpt 1251: triage: bt = ENVIRONMENT (core_mean +7.17 in ctl AND all gaps = the known ps
  207b3e84 ckpt 1250: GATE RAW: #569 109P/6F all bt (BT1c-ctl, g04-never, g12-never, g12, g4-never
  b51fe2b4 ckpt 1249: gate: #569-#572 COMPLETED (verdicts being read by a haiku subagent); #573 pt
  aa965083 ckpt 1248: gate: #574 rg GREEN; #573 pt (id 43315242706, job 131521431600) still in_pro
  f9e64bea ckpt 1247: HANDOFF 0v written (v0.9.8 gate state, run ids, ship steps)
  cb9db1ab ckpt 1246: gate #574 id 43315248372 (rg) GREEN (success, 21:57-22:01Z)
  34ec2ea2 ckpt 1245: Tj answer drafted: research/V098_RELEASE_MESSAGE_DRAFT_2026-11-12.md (facts 
```
