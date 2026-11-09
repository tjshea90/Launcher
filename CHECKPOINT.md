# CHECKPOINT 799 — read me first, then TASKS.md

**Written:** 2026-11-09T06:04:42Z · **tests:** all 7 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `42cacb5` (this checkpoint is the commit after it)

## Just done
BN1: run #455 ss = 40 PASS 2 FAIL (mono/balance are PRIVATE system settings, app cannot write them); battery dump works with pm grant PACKAGE_USAGE_STATS; log updated

## Do this next
1) fix Mono/Balance: SsPagesAccessibility write fallback -> helper verb SETSYS (ShellProtocol + ShellServer + ShellHelper client, allow-list master_mono 0/1, master_balance -1..1) else revert UI+toast; fix scenario restore (delete when null) 2) add pm grant PACKAGE_USAGE_STATS to tools/adb-setup.sh adb-all.sh ADB_COMMANDS.txt in-app guide + checklist 3) dispatch ss shots again (size pages SS27-28 + probes) 4) integrate Sound/Storage agents on notice 5) home summaries Battery/Storage 6) questions to Tj 7) review+gate+ship

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md
     M research/SETTINGS_BUILD_LOG_2026-11-09.md

## Last ten checkpoints
```
  75be91c ckpt 798: BN1 M3b: Font size + Display size pages (SsPagesSize, 5 vector icons, discrete
  48b3aa0 ckpt 797: BN1 M3: Accessibility+TTS+Magnification+Touch&hold pages (SsPagesAccessibility
  8bdaf8e ckpt 796: BN1: Sound + Storage pages delegated to 2 background subagents (new files only
  9fae60b ckpt 795: BN1 M2: Display page (SsPagesDisplay, SsDisplayRules+7 tests, switchRow), run 
  57bdfd4 ckpt 794: BN1 M2: SsDisplayRules (+Test) written, not yet run; CI ss shots run dispatche
  993c798 ckpt 793: BN1 M2: scenario ss steps SS13-SS14b (Battery page rows, overflow Battery usag
  17b92a3 ckpt 792: BN1 M2 part 1: Battery page code (SsBatteryText/Stats/Icon, SsPagesBattery, Li
  87715f1 ckpt 791: BN1: M1 ss run dispatched on main at 05:14Z (head f74b715; find id: list_workf
  f74b715 ckpt 790: BN1 M1: pages System, Languages & input, About tablet, Legal (SsPagesSystem, S
  dbe1092 ckpt 789: BN1 M0: CI ss run #451 id 42565458939 in flight (head 2ba5988); build log rese
```

(8 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
