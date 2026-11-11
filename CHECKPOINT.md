# CHECKPOINT 1171 — read me first, then TASKS.md

**Written:** 2026-11-11T17:46:14Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-3e66a8c0-ruc8aa` · **builds on:** `8fd6eb46` (this checkpoint is the commit after it)

## Just done
BV1 drawer rule hardened after the review: auto_done = pkg@firstInstallTime (pure AppVisibility.Marks/asked/handSet/removed, 15 AppVisibilityTest green, all 1025 unit tests + lint green), autoAllowIfDue apply(), SessionCommitReceiver skips a home icon for the music app; scenario mu: MU-12a real Hidden apps tap, MU-13b update in place, MU-14b missed removal (not yet run)

## Do this next
read run 546 (id 43069863937, upstream control + our tour on 0701e089) RESULTS: get_job_logs run_id failed_only tail 75; look at muc-* pictures (ci-shots -> scratchpad/shots546); then re-dispatch mu (new scenario) shots=true; then gate.sh v0.9.5; ship v0.9.6. Heartbeat trig_01GVcBRvTAzqf97UhMTJszab; never update_trigger/delete_trigger

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  ca99a8b5 ckpt 1170: Review of the drawer rule (agent a44d508, no HIGH): update in place does NOT
  0701e089 ckpt 1169: BV1e: mu control = upstream Eleven (baseline db65f024 + port fix, built by e
  67f04fc9 ckpt 1168: Run 545 (id 43066812780, head ae8cb7e1, mu shots=true): 23 PASS 1 FAIL = MU-
  ae8cb7e1 ckpt 1167: BV1: audit items 3/4/5 done (dead BitmapWithColors ctor + Palette call remov
  cde47d6c ckpt 1166: BV1: audit report saved (research/ELEVEN_AUDIT_2026-11-11.md: no HIGH; TO DO
  25bb7e5a ckpt 1165: Run 544 (id 43063412826, head ff924adf, sections=mu shots=true): 18 PASS 4 F
  ff924adf ckpt 1164: Run 543 (id 43062112175, mu on 21315c73) FAILED before the scenario: step Im
  fa3436e5 ckpt 1163: BV1: TASKS BV1a/b/c ticked with proof; mu extended (artist/album popup menus
  a6de0e7d ckpt 1162: BV1c/e/d: guard test 10 tests green + AppVisibilityTest 9 green (IoUtils dea
  6d4faaf3 ckpt 1161: BV1b/c: art code cut from eleven/ (ImageWorker/ImageFetcher placeholder-only
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
