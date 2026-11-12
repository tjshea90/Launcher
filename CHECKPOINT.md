# CHECKPOINT 1254 — read me first, then TASKS.md

**Written:** 2026-11-12T23:00:43Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-91599ec3-za40s7` · **builds on:** `8c438a3a` (this checkpoint is the commit after it)

## Just done
reruns GREEN on 17e10ad2: #575 ps,bt 27P/0F (BT1c guard: scale 0.750, 12-level INFO); #577 cs 19P/0F; #578 base 29P/0F (1 native crash libandroid_runtime draw, no FAIL); #579 pt 31P/0F

## Do this next
~23:13Z read #576 id 43322018134 se,ss; green -> ship.sh v0.9.8, android.yml full_build; red -> se,ss on tag v0.9.7. Heartbeat trig_01JYEUXs7JosHyg85f2T1oCK

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  ee2378fd ckpt 1253: solo reruns dispatched 22:40Z on 17e10ad2: #575 id 43322012415 ps,bt; #576 i
  17e10ad2 ckpt 1252: BT1c scale guard added (bttol: fit 0.78-0.82 -> 2 levels, else 12 + INFO; of
  970ab845 ckpt 1251: triage: bt = ENVIRONMENT (core_mean +7.17 in ctl AND all gaps = the known ps
  207b3e84 ckpt 1250: GATE RAW: #569 109P/6F all bt (BT1c-ctl, g04-never, g12-never, g12, g4-never
  b51fe2b4 ckpt 1249: gate: #569-#572 COMPLETED (verdicts being read by a haiku subagent); #573 pt
  aa965083 ckpt 1248: gate: #574 rg GREEN; #573 pt (id 43315242706, job 131521431600) still in_pro
  f9e64bea ckpt 1247: HANDOFF 0v written (v0.9.8 gate state, run ids, ship steps)
  cb9db1ab ckpt 1246: gate #574 id 43315248372 (rg) GREEN (success, 21:57-22:01Z)
  34ec2ea2 ckpt 1245: Tj answer drafted: research/V098_RELEASE_MESSAGE_DRAFT_2026-11-12.md (facts 
  cb28da0c ckpt 1244: GATE dispatched 21:57Z on 72f529a1 (branch): #569 id 43315221820 r1 b6..fl; 
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
