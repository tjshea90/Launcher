# CHECKPOINT 1170 — read me first, then TASKS.md

**Written:** 2026-11-11T17:41:49Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-3e66a8c0-ruc8aa` · **builds on:** `231a5f14` (this checkpoint is the commit after it)

## Just done
Review of the drawer rule (agent a44d508, no HIGH): update in place does NOT call onRemoved (AOSP PackageMonitor) ok; M1 missed removal event leaves a stale auto_done (hand-hidden Eleven stays hidden after uninstall+reinstall); M2 glue untested; L3 SessionCommitReceiver would add a HOME icon for Eleven; L4 commit() on main thread. Run 546 (id 43069863937) mu with upstream control still running

## Do this next
implement: (1) auto_done entries = pkg@firstInstallTime (AppVisibility pure fns + AppVisibilityTest; HiddenApps stamp via PackageManager, autoAllowIfDue apply(), setShown/onRemoved); (2) SessionCommitReceiver.queueAppIconAddition skips AUTO_ALLOWED packages; (3) scenario: MU-6/12/13/14 grep ^pkg@, muprefs takes the exact done entry, MU-14b uninstall while Launcher force-stopped; read run 546 RESULTS first (get_job_logs run_id 43069863937 failed_only tail 75). Heartbeat trig_01GVcBRvTAzqf97UhMTJszab; never update_trigger/delete_trigger

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  0701e089 ckpt 1169: BV1e: mu control = upstream Eleven (baseline db65f024 + port fix, built by e
  67f04fc9 ckpt 1168: Run 545 (id 43066812780, head ae8cb7e1, mu shots=true): 23 PASS 1 FAIL = MU-
  ae8cb7e1 ckpt 1167: BV1: audit items 3/4/5 done (dead BitmapWithColors ctor + Palette call remov
  cde47d6c ckpt 1166: BV1: audit report saved (research/ELEVEN_AUDIT_2026-11-11.md: no HIGH; TO DO
  25bb7e5a ckpt 1165: Run 544 (id 43063412826, head ff924adf, sections=mu shots=true): 18 PASS 4 F
  ff924adf ckpt 1164: Run 543 (id 43062112175, mu on 21315c73) FAILED before the scenario: step Im
  fa3436e5 ckpt 1163: BV1: TASKS BV1a/b/c ticked with proof; mu extended (artist/album popup menus
  a6de0e7d ckpt 1162: BV1c/e/d: guard test 10 tests green + AppVisibilityTest 9 green (IoUtils dea
  6d4faaf3 ckpt 1161: BV1b/c: art code cut from eleven/ (ImageWorker/ImageFetcher placeholder-only
  d65b3668 ckpt 1160: BV1: exact eleven patch list written (research note section 6); drawer findi
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
