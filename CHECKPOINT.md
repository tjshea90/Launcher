# CHECKPOINT 900 — read me first, then TASKS.md

**Written:** 2026-11-10T09:37:05Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `7a3dcb7` (this checkpoint is the commit after it)

## Just done
Gate 6878395 raw verdict: #504 b6,n,o,x,ac,ad = 98 PASS 4 FAIL (BK1b no-cover x2 in b6 quickstep: P2 lock screen cover=shown + control cover=shown; N2 tap-and-erase x2 in n) + NATIVE CRASH in ac (not near them). #503 pt green. #505 1 FAIL=native crash flake (base). Dispatched 09:36Z: b6 solo, n solo (branch), b6,n on ref v0.9.1; base solo #506 id 42790941782 (head 4f7d798). Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

## Do this next
1) list emulator.yml runs (branch ccr-5ef85f7a-ngtgj1 + event workflow_dispatch, perPage 5; the v0.9.1 one may list under branch v0.9.1) and read: base solo #506, b6 solo, n solo, b6,n on tag (get_job_logs run_id+failed_only tail 20 = pass/fail probe; then a haiku subagent for header+FAIL+BK1b/N2 INFO lines). 2) If b6/n FAIL on head but not on the tag: the cause is my BQ1b pencil change - suspect a cover shown when coverWorld is false or at the lock screen without a panel (TjAccessibilityService.updatePencil/penWatch/penEval/findPencil; penWatch has no coverWorld check; check showPencil callers). N2 fails may come from a stuck touchable cover swallowing taps. 3) Then fix, unit tests, tools/gate.sh 2f5831f, re-dispatch only b6,n,ad. Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  4f7d798 ckpt 898: Gate 6878395 verdict 09:27Z: #503 pt GREEN (0 FAIL; BQ1a/BQ2 pass, dark look 0
  bd81eab ckpt 894: Gate dispatched on 6878395 09:10Z: #503 pt (run 42788494486), #504 b6,n,o,x,ac
  6878395 ckpt 893: Review fixes coded: scrim counted at run start (+async end, report w/ sig), pe
  c33b797 ckpt 882: Both reviews read; verdicts saved in research/BQ1_REVIEW_2026-11-10.md. Scrim:
  a04e129 ckpt 881: BQ2a answered in chat (09:00Z); BQ2b coded, not released. Heartbeat trig_013YU
  8d587e9 ckpt 880: BQ2b coded (Tj: measure once, never screenshot again): dark look never measure
  15f72bb ckpt 879: Tj asked (BQ2): why not measure once and never screenshot again (wallpaper unc
  b016ab1 ckpt 878: BQ1a: a freshly (re)connected service settles from its connect (ScrimMeasure.s
  1479de8 ckpt 877: v0.9.2 answer draft saved (research/V092_RELEASE_MESSAGE_DRAFT_2026-11-10.md).
  b295cfe ckpt 876: BQ1b coded: PencilRules.FingerGuard + service wiring (penFingerDown, both guar
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
