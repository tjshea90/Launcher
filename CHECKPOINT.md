# CHECKPOINT 1059 — read me first, then TASKS.md

**Written:** 2026-11-11T00:27:41Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `ce40965` (this checkpoint is the commit after it)

## Just done
BS2 review fixes IN PROGRESS: done = NotifQuietPolicy.WHY_NO_HELPER, RadioIconHold.radiosGoOffWhenHoldEnds, RadioIconHide.sync F2 trigger (LockQuiet.onLockAsync when the hold ends with WHY_NO_HELPER on a dark/locked screen). Run #520 pictures confirm the Wi-Fi symbol gone on lock + custom (strips in scratchpad)

## Do this next
REMAINING in this order: (F1) LockQuiet.shouldForgetPrev(keyguardLocked,screenOn,prev)+forgetStalePrevAsync(c) (worker thread, PowerManager.isInteractive + TileProfile.keyguardLocked), call it in TjAccessibilityService.onServiceConnected after LockQuiet.applyNotificationsAsync (~line 319); (F5) ControlActivity texts (restart began in Quickstep / only the entries this switch added / put-back text) + start the radios-off worker BEFORE syncNow in the switch handler; tests LockQuietTest (shouldForgetPrev) + RadioIconHoldTest (radiosGoOffWhenHoldEnds); scenario phase N plant w1b1 again + check lq_prev w1b1 gone (WF12c), new phase D2 between D and E (lockpad from Quickstep, uiautomator dump on the lock screen => wifi_on 0 WF13, list hidden again + radio off WF13b/c, gesture unlock puts Wi-Fi back WF13d); gate-map wf 130 s; docs TASKS BS2 F1-F5 + research (F3/F4 accepted); then :app:testDebugUnitTest (RadioIconHoldTest LockQuietTest NotifQuietPolicyTest NotifQuietHoldTest), tools/gate.sh --check, tools/test_handoff.sh, ckpt, dispatch emulator.yml wf shots=true on main (verify main==tip first), fetch ci-shots into a NEW scratchpad dir. Then bump versionCode 43 / versionName 0.9.4 in app/build.gradle.kts, bash tools/gate.sh a542594 (3 runs), ship.sh, android.yml full_build, record-release, answer Tj (draft: research/V094_RELEASE_MESSAGE_DRAFT_2026-11-11.md). Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  6e6b2c4 ckpt 1054: BS2d: wf run #520 (id 42930004319, main@ce7f000, code before the review fixes
  52f666f ckpt 1050: BS2d: emulator run #520 (id 42930004319, main@ce7f000, sections=wf shots=true
  ce7f000 ckpt 1049: BS2: docs (TASKS BS1b/2a/2b/2c done, HANDOFF row, research moments walked), s
  65bf8f5 ckpt 1046: BS2a code done: Radio* rename, wifi+bluetooth slots, keepsRadiosOn mirrors th
  d41f968 ckpt 1030: BS2: scenario wf v2 written (Bluetooth list, picture probe, lock control, pha
  b2cfbb6 ckpt 1026: BS1d wf run #519 (id 42926361302, job 130213726774, main@8babd57): 24 PASS 0 
  d1c4de3 ckpt 1025: BS2 (Tj: also Bluetooth, only if already on, never turn on, never show) writt
  8e2f709 ckpt 1021: BS1: TASKS BS1c ticked, HANDOFF row added; wf emulator run dispatched on main
  8babd57 ckpt 1018: BS1d: emulator section 'wf' written (scenario + gate-map + emulator.yml names
  dece682 ckpt 1015: BS1c code: WifiIconHold (24 tests green) + WifiIconHide + hooks (syncNow/tick
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
