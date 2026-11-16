---
date: 2026-11-16T01:21:50.234Z
project: Launcher
session: bd515d82
schema_version: 1
next_steps_count: 4
has_blockers: false
keywords: vpn row lock screen, fast wi-fi pause, tj_launcher_world, vpnoff, linkmeter, ce1, tablet restart no adb, launcher v0.9.11
headline: Goal: stop VPN row "Device connected to ..." on lock screen + custom launcher; Fast Wi-Fi (LinkMeter VPN) stays on in un
---

# Session Handoff Snapshot
> **Generated:** 11/16/2026, 1:21:50 AM

## 🎯 High-Level Objective
* **Goal:** Goal: stop VPN row "Device connected to ..." on lock screen + custom launcher; Fast Wi-Fi (LinkMeter VPN) stays on in unlocked Quickstep, pauses on lock/screen-off/custom launcher, resumes by itself in Quickstep. Why: Launcher VpnOff (revoke at screen off) and LinkMeter v0.2.183 (re-connect 2 s after revoke, max 3 per 10 min) fought; after 3 rounds both gave up, VPN stayed up, row visible. Follow-up CF1: review what survives tablet restart without adb cable. Both repos: tjshea90/Launcher, tjshea90/LinkMeter; session branch ccr-9028296e-kjveqx.

## 📌 Current State & Next Steps
* **Status:** Code, tests, gate, releases, BUILDLOG, TASKS (CE1a-c, CF1, CF1a ticked), HANDOFF 0y, research notes all pushed; Launcher main == branch tip. Remaining: only user tablet check CE1d (no Claude work open). No heartbeat trigger created.
* **Next Action:** Nothing Claude-owned open. Wait for user tablet check CE1d: install Launcher v0.9.11 and LinkMeter v0.2.184 on tablet; verify lock screen has no VPN row, lock then Quickstep unlock brings Fast Wi-Fi back in seconds, LinkMeter Settings > Fast Wi-Fi > What changed it lately shows Paused and came back by itself lines.

### Remaining Queue
- [ ] If user reports problem: read that history list and Launcher control VPN line (VpnOff.describe) first; check word via Launcher control; nothing yet seen on real tablet.
- [ ] Resume protocol per repo: Launcher bash tools/resume.sh, CHECKPOINT.md, TASKS.md CE1d; LinkMeter HANDOFF.md RESUME HERE; git fetch --all and ff-merge session branch first.
- [ ] Older open TASKS items (CD1/CD3 memory questions) wait on user; unrelated to this job.

## 🛠️ Modified Files Delta
* Launcher app/src/main/java/com/tj/launcher/VpnOff.java: rewritten, follows quiet world via syncNow, publishes word on worker, poll idempotent, lookWanted flag, world re-read before prepare, refusal expires 10 min, own worker lock, async network-callback registration
* Launcher VpnOffRules.java: why() takes quietWorld, worldValue/isQuiet, WORLD_KEY constants
* Launcher AnimationSpeedup.java: VpnOff.sync call in syncNow, tick condition includes VPN switch
* Launcher TjAccessibilityService.java: removed direct VPN call sites, watch + onUnlocked kept
* Launcher ControlActivity.java: toggle label/text, toggle and Allow result trigger pipeline; RestoreStore.java retract word; ReportState.java label
* Launcher VpnOffRulesTest.java: 13 tests incl. world word
* Launcher tools/emulator_scenario.sh section vo: rewritten phases B C E F H, grant in setup; tools/gate-map.txt text
* Launcher app/build.gradle.kts: versionCode 50, versionName 0.9.11
* Launcher TASKS.md CE1/CF1, HANDOFF.md block 0y, BUILDLOG.md, research/VPN_FOOTER_2026-11-13.md CE1 section + review verdicts, research/RESTART_NO_CABLE_2026-11-15.md, research/CE1_RELEASE_MESSAGE_DRAFT_2026-11-15.md
* LinkMeter FastRules.java: PAUSED state, pauseWhy, pausedWords, card branch, HISTORY 10
* LinkMeter FastVpn.java: pause gate, receiver + observer + 30 s look, no prepare while paused, history lines
* LinkMeter FastTile.java: Paused until Quickstep
* LinkMeter tools tests: FastRulesTest, FastPreviewTest (32 scenarios, idleNow helper), mutate_fast.py, mutate_fast_keep_glue.py (23 breakages)
* LinkMeter docs/FAST_WIFI.md, docs/USER_GUIDE.md, HANDOFF.md RESUME HERE block

## 🚫 Failed Approaches (DO NOT RETRY)
* Approach: LinkMeter glue breakage run with long wait loops advancing Robolectric clock ~100 s → Result: 3 of 23 breakages MISSED, 30 s level look healed service masking event paths → Lesson: wait loops in event tests must not move pretend clock; added idleNow helper and userPresentAloneWakesIt test
* Approach: reuse old vo scenario phases B/C/E unchanged → Result: 6 FAIL in run #610 → Lesson: new design stops VPN in custom launcher at first look; log line 'has not allowed' written once per world; stop counts differ
* Approach: solo vo run without WRITE_SECURE_SETTINGS grant → Result: world word null, 4 FAIL in #613 → Lesson: only base section grants it; vo setup now grants
* Approach: phase E cap check assuming fixed earlier stop count → Result: VO5b/VO5c FAIL in #615, launcher process changed between C and D → Lesson: reconnect up to 5 times, left-alone reconnect must be #1-3, earlier ones stopped
* Approach: helper named settle() in FastPreviewTest → Result: compile clash with static import PreviewTest.settle → Lesson: renamed idleNow
* Approach: read ss SS7 FAIL as regression → Result: ss solo #614 green → Lesson: known flake, rerun solo before touching code

## Summary
DONE + RELEASED + ANSWERED to user. Launcher v0.9.11 (code 50, android.yml run #52 id 43772718433, commit dceaa412, BUILDLOG recorded). LinkMeter v0.2.184 (run #184, commit a5b2837, main equal). Design: one shared "world" rule. Launcher VpnOff now consumer of AnimationSpeedup.syncNow pipeline using NotifQuietPolicy quiet world (custom launcher, screen dark, lock screen any launcher, plain-PIN undecided = quiet = VPN off; unlocked decided Quickstep = VPN may run). Launcher publishes Settings.Global tj_launcher_world = quickstep | quiet:why BEFORE revoking (needs WRITE_SECURE_SETTINGS adb grant, survives reboot). LinkMeter reads word plus own screen/keyguard facts (word missing = fail open; unknown word = may run); State.PAUSED keeps choice, never calls VpnService.prepare while paused, pauses not counted as revokes; resumes via screen/unlock receiver, ContentObserver on word, 30 s level look. Read-only subagent review of Launcher diff: 8 findings, fixed 1,3,4,5,6,8; not changed 2 (LinkMeter has own lock facts) and 7 (rare failed word write). Cap 3 stops per 10 min stays as documented limit for third-party VPN apps. Proof: Launcher 1054 unit tests + lint; LinkMeter 32 preview scenarios, pure checks, 16 pure + 23 glue deliberate breakages all caught; emulator gate on product code c0d29aae (identical at release): #611 green, #610 base/ac/rb/last pass, #612 pass except SS7 flake (ss solo #614 green), vo solo #616 green. Nothing seen on real tablet. Restart/no-cable audit saved research/RESTART_NO_CABLE_2026-11-15.md and answered. Draft message research/CE1_RELEASE_MESSAGE_DRAFT_2026-11-15.md. Deliberately NOT passed keyDecisions/implicitRules to avoid overwriting existing auto-managed learnings block in CLAUDE.md.

---
*A short hint surfaces on session start; full context loads only if your next prompt matches a keyword above, or via manual `/handoff-resume`. The same record is available as a sibling `.json` file for external tools.*