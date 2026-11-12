# CHECKPOINT 1263 — read me first, then TASKS.md

**Written:** 2026-11-12T23:39:17Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-91599ec3-za40s7` · **builds on:** `1633b750` (this checkpoint is the commit after it)

## Just done
version bumped to 47 / 0.9.8; ship.sh running in background (log scratchpad/ship098.log, marker SHIP_EXIT=)

## Do this next
when SHIP_EXIT=0: android.yml full_build=true on ref main (main==tip), +3 min get_release_by_tag v0.9.8, record-release.sh v0.9.8 only if green; then answer Tj (draft research/V098_RELEASE_MESSAGE_DRAFT_2026-11-12.md), tick BY1a-d BZ1a-c, HANDOFF 0v, delete heartbeat trig_01JYEUXs7JosHyg85f2T1oCK

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  925f34f8 ckpt 1262: #582 id 43328172010 (se,ss ref main) GREEN + #583 id 43328175947 (se,ss bran
  8ba3cfc7 ckpt 1261: HANDOFF 0v: gate triage + rerun state written
  914160f7 ckpt 1260: dispatched 23:21Z se,ss on ref main + on branch (head d5017a2c, product == 1
  d5017a2c ckpt 1259: SS7 diag INFO added (dump size, packages, texts when no Settings entry); dry
  1e7cce30 ckpt 1258: #580 tag v0.9.7 se,ss GREEN; #581 id 43325521656 head se GREEN (SE-C2 interm
  a2a371a0 ckpt 1257: SE-C2 diag INFO added to the scenario (mode, kg, battery, tj_crash, power br
  72b67893 ckpt 1256: tag run #580 id 43325361700 (se,ss on v0.9.7) started 23:02Z; head se rerun 
  99256c2d ckpt 1255: #576 se,ss RED on 17e10ad2 (solo, no native crash): 81P/3F SE-C2 (charger fl
  9e4ba92f ckpt 1254: reruns GREEN on 17e10ad2: #575 ps,bt 27P/0F (BT1c guard: scale 0.750, 12-lev
  ee2378fd ckpt 1253: solo reruns dispatched 22:40Z on 17e10ad2: #575 id 43322012415 ps,bt; #576 i
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
