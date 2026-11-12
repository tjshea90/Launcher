# CHECKPOINT 1252 — read me first, then TASKS.md

**Written:** 2026-11-12T22:39:57Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-91599ec3-za40s7` · **builds on:** `24d2699f` (this checkpoint is the commit after it)

## Just done
BT1c scale guard added (bttol: fit 0.78-0.82 -> 2 levels, else 12 + INFO; offline-tested); se: SE-F2 failed but SE-F3 (Quickstep home in front) passed = default home lost (crash) not the flick; recents/switch-back checks are in base (REC1 line 319, 162)

## Do this next
dispatch solo reruns on branch: ps,bt; se,ss; cs; base; pt -> read each; if a head solo fails, rerun it on tag v0.9.7. Heartbeat trig_01JYEUXs7JosHyg85f2T1oCK

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  970ab845 ckpt 1251: triage: bt = ENVIRONMENT (core_mean +7.17 in ctl AND all gaps = the known ps
  207b3e84 ckpt 1250: GATE RAW: #569 109P/6F all bt (BT1c-ctl, g04-never, g12-never, g12, g4-never
  b51fe2b4 ckpt 1249: gate: #569-#572 COMPLETED (verdicts being read by a haiku subagent); #573 pt
  aa965083 ckpt 1248: gate: #574 rg GREEN; #573 pt (id 43315242706, job 131521431600) still in_pro
  f9e64bea ckpt 1247: HANDOFF 0v written (v0.9.8 gate state, run ids, ship steps)
  cb9db1ab ckpt 1246: gate #574 id 43315248372 (rg) GREEN (success, 21:57-22:01Z)
  34ec2ea2 ckpt 1245: Tj answer drafted: research/V098_RELEASE_MESSAGE_DRAFT_2026-11-12.md (facts 
  cb28da0c ckpt 1244: GATE dispatched 21:57Z on 72f529a1 (branch): #569 id 43315221820 r1 b6..fl; 
  72f529a1 ckpt 1243: review fixes: suite 1036/0, lint OK; self-review of the delta OK (no showWhe
  43435134 ckpt 1242: BY1 review fixes coded: MED1 shade look at service connect; LOW2 wake skips 
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
