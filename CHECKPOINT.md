# CHECKPOINT 766 — read me first, then TASKS.md

**Written:** 2026-11-09T03:18:03Z · **tests:** all 7 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `e7705b1` (this checkpoint is the commit after it)

## Just done
BN2/BN4: pure rules + JVM tests written (HomePatchRules.foreignTreatment/UNLOCK_GRACE_MS, PowerMenuRule.dyingDialog, new PencilRules + 3 test files); NOT yet wired into TjAccessibilityService.java, not yet run

## Do this next
wire in TjAccessibilityService.java (read design file research/BN2_BN3_BN4_DESIGN_2026-11-09.md): (1) homeForeignWindow l.667 use HomePatchRules.foreignTreatment with a new field lastKeyguardUpMs set inside keyguardLockedFast l.1405 when locked; (2) menuClosedAtMs set in patchShow l.3775 + PowerMenuRule.dyingDialog in evaluateScan l.2989 and reset in fastMenuOpen l.131; (3) findPencil l.2162 use PencilRules.follow + penWatch 150ms; (4) touchBack l.392 fresh pad look + TOUCH_AWAY_MS 1500 + lock-log lines. then: bash tools/gradle.sh :app:testDebugUnitTest --tests 'com.tj.launcher.*Rules*' and full testDebugUnitTest :app:lintDebug

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  b284914 ckpt 765: BN2/BN3/BN4 causes read from the 03:02Z tablet report; design in research/BN2_
  aeb91cb ckpt 764: BN2d/BN4: Tj 03:02Z tablet report intaken (research/tablet/2026-11-09); log sh
  82cb5cc ckpt 763: BN1: agent A done -> research/aosp10-settings/A_battery_display.md (287 lines)
  006243a ckpt 762: BN1: agent C done -> research/aosp10-settings/C_access_system_about.md (341 li
  e94e889 ckpt 761: BN1i: Tj 02:47Z message + 5 more screenshots (TTS output, Touch&hold dialog, M
  f5590cc ckpt 760: BN1: CI screenshot channel: emulator.yml input shots=true + tools/ci_publish_s
  650d7d3 ckpt 759: BN1a map done: research/SETTINGS_SPOOF_2026-11-09.md sections 3, 3b (decisions
  d9f4115 ckpt 758: BN2 (home patch first press) + BN3 (lock patch flash on pin pull) written into
  12a5591 ckpt 757: BN1b: theme + measurement facts added to research/SETTINGS_SPOOF_2026-11-09.md
  814aa3b ckpt 756: BN1: AOSP10 Settings + frameworks/base partial clone in scratchpad aosp/{setti
```

(7 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
