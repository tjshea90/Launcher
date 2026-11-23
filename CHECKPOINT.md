# CHECKPOINT 1645 — read me first, then TASKS.md

**Written:** 2026-11-23T04:22:40Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/dazzling-hawking-b7qz0b` · **builds on:** `f51a7d20` (this checkpoint is the commit after it)

## Just done
CX1 RAW VERDICT hm #771 id 45109794817 job 137479292087 (head==tag): 15 PASS 4 FAIL. FAIL hmApp1 (shot 81 fr/1350 ms), hmApp2 (205 fr/3417 ms), hmShade1 (shot 121 fr/2017 ms flashA 1), hmShade2 (flashA 16 flashB 6, shot 0). Control PASS. Others pass (hmDrawer/Page/Off/Tap/Twice). hmApp log: no patch window at the press (0.2 and 0.8 s after Home from an app), slow path card lag 343 ms, then 'PATCH removed' + 'menu closed' while the stock menu was STILL OPEN (scans 90-244 ms), re-created 0.3 s later: entry visible 1.35-3.4 s. gm baseline 2 #770 id 45109356329 still running/unread

## Do this next
read gm #770 (job via list_workflow_jobs), then fix: never remove the patch/card window while a menu is open for a transient reason (TAS refreshLayers ~1467-1483, removePatch ~1641, home whyNot), see hmApp1 log; heartbeat trig_01GoGsAi9y4N2PpYgaAmbk6A

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  a9b431f6 ckpt 1644: CX1: BASELINE 2 in flight on product code == tag v0.9.18: gm #770 id 4510935
  6ff6b527 ckpt 1643: CX1d/CX1e written into TASKS (baseline findings + fix plan); gm baseline 2 a
  6ca77abe ckpt 1642: CX1b: hm baseline #769 id 45107548707 = geometry bug only (core box empty: h
  bd1d674e ckpt 1641: CX1b: fl_analyze --from (menu judged only from the press; selftest 4b), flfu
  c5dbb2a9 ckpt 1640: CX1b RAW VERDICT gm #768 id 45106026788 (head==tag v0.9.18): 18 PASS 5 FAIL.
  0e3ffaed ckpt 1639: CX1b: BASELINES in flight: gm #768 id 45106026788 (03:26Z), hm #769 id 45107
  36da7375 ckpt 1638: CX1b: section hm (home matrix) written (7 events x 0.2/0.8 s then power menu
  f274afbc ckpt 1637: CX1b: BASELINE gm run #768 id 45106026788 dispatched 03:26Z main ce7c7be7 (p
  ce7c7be7 ckpt 1636: CX1b: section gm (gesture matrix) written: 6 flash cases (pad slow/half/hold
  8c11a150 ckpt 1635: CX1a: LOCK + HOME + PENCIL audits all saved (research/CX1_AUDIT_*). Lock G3 
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
