# CHECKPOINT 1605 — read me first, then TASKS.md

**Written:** 2026-11-22T22:44:46Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/dazzling-hawking-b7qz0b` · **builds on:** `72b1be79` (this checkpoint is the commit after it)

## Just done
CW1 takeover: A1 built (LauncherModel.isAppsBound/markAppsBound, BaseLoaderResults.afterBindAllApps, LoaderTask marks after bindAllApps, HomeProfile.ready uses UnifiedRules.ready 4-arg) + test; Cw1RulesTest 22 tests + UnifiedRulesTest GREEN. DECIDED NOT TO BUILD: C4 (API 29 VPN callback is blind to VPNs that exclude the launcher: the 30 s poll is function), B10/B11, B4/B2/B5-B9/B13, A8 (marginal vs lock/card risk; to be listed in the answer)

## Do this next
write HANDOFF D2 text (never disable stock HOME activity / TouchInteractionService; stock cannot sleep, adj 100), update TASKS CW1c/d/e, full suite+lint, /code-review high of git diff e2b45cc4~..HEAD (app launcher3), fix, tools/gate.sh v0.9.17, ship.sh, android.yml, record. heartbeat trig_01DN2e1WG9kDhpHMtsFee1Lb

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  24576f80 ckpt 1604: CW1 takeover: B3 (pencil deferral: pencilDeferred + schedulePencil(boolean,i
  b8e7e556 ckpt 1603: CW1 takeover: full suite+lint GREEN on finished tree (GRADLE_EXIT=0). Read a
  e8231163 ckpt 1602: CW1 takeover: heartbeat armed trig_01DN2e1WG9kDhpHMtsFee1Lb (every 2h at :34
  537920d8 ckpt 1601: CW1 takeover: D4 signatures finished (recentsClearAll/finishRecentsEmpty tak
  9d92e814 ckpt 1600: CW1 takeover (new session): old session dead since 18:41 UTC (only worker-re
  e2b45cc4 ckpt 1599: CW1c: 3 implementers launched in background ~18:50 UTC (X speed+resources, Y
  7dca56f8 ckpt 1598: CW1b: research/CW1_FINDINGS_2026-11-22.md written (decisions per finding; pa
  1a6d6cff ckpt 1597: CW1a: audit D done (research/CW1_AUDIT_D_STOCK_SLEEP_2026-11-22.md): stock p
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
