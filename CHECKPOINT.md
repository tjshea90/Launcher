# CHECKPOINT 1644 — read me first, then TASKS.md

**Written:** 2026-11-23T04:09:57Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/dazzling-hawking-b7qz0b` · **builds on:** `6ff6b527` (this checkpoint is the commit after it)

## Just done
CX1: BASELINE 2 in flight on product code == tag v0.9.18: gm #770 id 45109356329 (04:01Z, bd1d674e, expect done ~04:17Z), hm #771 id 45109794817 (04:06Z, 6ca77abe, expect ~04:17Z). Findings so far in TASKS CX1d/CX1e + research/CX1_AUDIT_*

## Do this next
at ~04:18Z: job ids (list_workflow_jobs resource_id=run id), haiku subagent reads each (header, FAILs, 'INFO  FL gm*/hm* log:' lines of cases with shot>0, HM2 path lines); ckpt RAW verdict; then implement CX1e fixes in TjAccessibilityService.java: (1) gate pencil look at TAS ~2555 schedulePencil on lock screen unless finger<3s/shade/cover; throttle bouncerShownScan callers (updateLockNumber ~3088) while pad known up; (2) premature menu close (see gmFling log); (3) home keep window+composite across pause; (4) pencil flicker; heartbeat trig_01GoGsAi9y4N2PpYgaAmbk6A

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  6ff6b527 ckpt 1643: CX1d/CX1e written into TASKS (baseline findings + fix plan); gm baseline 2 a
  6ca77abe ckpt 1642: CX1b: hm baseline #769 id 45107548707 = geometry bug only (core box empty: h
  bd1d674e ckpt 1641: CX1b: fl_analyze --from (menu judged only from the press; selftest 4b), flfu
  c5dbb2a9 ckpt 1640: CX1b RAW VERDICT gm #768 id 45106026788 (head==tag v0.9.18): 18 PASS 5 FAIL.
  0e3ffaed ckpt 1639: CX1b: BASELINES in flight: gm #768 id 45106026788 (03:26Z), hm #769 id 45107
  36da7375 ckpt 1638: CX1b: section hm (home matrix) written (7 events x 0.2/0.8 s then power menu
  f274afbc ckpt 1637: CX1b: BASELINE gm run #768 id 45106026788 dispatched 03:26Z main ce7c7be7 (p
  ce7c7be7 ckpt 1636: CX1b: section gm (gesture matrix) written: 6 flash cases (pad slow/half/hold
  8c11a150 ckpt 1635: CX1a: LOCK + HOME + PENCIL audits all saved (research/CX1_AUDIT_*). Lock G3 
  53d517b3 ckpt 1634: CX1a: PENCIL audit saved research/CX1_AUDIT_PENCIL_2026-11-23.md. Key facts:
```
