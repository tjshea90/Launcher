# CHECKPOINT 787 — read me first, then TASKS.md

**Written:** 2026-11-09T04:49:32Z · **tests:** all 7 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `a6e29b4` (this checkpoint is the commit after it)

## Just done
v0.8.13 RELEASED: android.yml #40 id 42563477283 SUCCESS on edbf66d, Release published 04:46:45Z, BUILDLOG line written, TASKS updated. Waiting on Tj: tablet check (research/TABLET_CHECKLIST_v0.8.13.md)

## Do this next
give Tj the Release link as plain text (done in the reply); then START BN1 M0 per research/SETTINGS_BUILD_PLAN_2026-11-09.md: read research/SETTINGS_SPOOF_2026-11-09.md + aosp10-settings/*.md + icons/INDEX.md as needed; first files: AndroidManifest alias, SsPage/SsPageActivity, theme, settings_spoof switch (policy class KEY + defaultOn, WIP default OFF), AppVisibility.ALWAYS_SHOWN_OWN_COMPONENTS, gate-map entry for section ss

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md
     M TASKS.md

## Last ten checkpoints
```
  0c8443a ckpt 786: GATE v0.8.13 GREEN: 450 pt solo on main = SUCCESS (id 42562320225); every othe
  295dc13 ckpt 785: ship.sh v0.8.13 (code 38) DONE: handoff test, unit suite green, pushed; HEAD=b
  3ea693a ckpt 784: gated v0.8.13 (code 38) and pushed it: the handoff self-test, the full unit su
  b466126 ckpt 783: 444 pt solo cancelled at 20 min (emulator died in phase H: 3 FAIL = PT timing 
  53ea1f0 ckpt 782: GATE v0.8.13: ALL GREEN except 444 still running: 445 last, 447 e1,n,ac (id 42
  d6bdfd6 ckpt 781: 448 hp SOLO = SUCCESS (HP14 green). 449 fp,last (fixed scenario, head 4b0cbfc)
  55e5668 ckpt 780: TASKS: gate story v0.8.13 written (441/442/443 triage, fixes: gate.sh solo pt+
  7adb7fa ckpt 779: 446 fp,last FAILED = TEST order problem (picture: lock screen + PIN pad up, st
  437946a ckpt 778: gate triage: 445 last SOLO = SUCCESS (443's last FAIL was chain-dependent); 44
  ae3a6e2 ckpt 777: tools/gate.sh: pt and rg now SOLO runs (+ bins grow when a run would pass 40 m
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
