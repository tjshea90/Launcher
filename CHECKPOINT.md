# CHECKPOINT 1382 — read me first, then TASKS.md

**Written:** 2026-11-17T19:14:49Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-16772798-h13w43` · **builds on:** `0da1d72b` (this checkpoint is the commit after it)

## Just done
CG1: emulator.yml sections=ro (shots=true) DISPATCHED on ccr-16772798-h13w43 head 0da1d72b; 6 reviewers still running, CG1E overlay-source report done

## Do this next
(1) find the ro run id: actions_list list_workflow_runs owner tjshea90 repo Launcher resource_id emulator.yml workflow_runs_filter {status:in_progress} (then list_workflow_jobs for the job id); it ends ~8-12 min after 19:30Z; read get_job_logs tail_lines ~80 (RESULTS block: INFO 'RO ...' lines = does Android 10 accept a user-installed overlay for android / com.android.systemui, pencil box stddev before/after); BEFORE the next shots dispatch fetch branch ci-shots into the scratchpad (git archive) - ro-base.png, ro-afterfw.png. Record the raw verdict in a ckpt FIRST, then analyse. (2) reviewers' reports are written by the subagents to research/CG1A_MODE_PATHS, CG1D_PATCH_LIFECYCLE, CG1B_LAUNCHER3_PERF, CG1B_PROCESS_PERF, CG1B_A11Y_PERF, CG1F_QUICKSTEP_AOSP_ANGLE (all _2026-11-17.md); CG1E_OVERLAY_AOSP10_SOURCE is DONE (WebFetch gave summaries only: framework-res has no overlayable block on android10-release, default policy mask lacks PUBLIC -> an android: overlay should be refused, medium confidence; SystemUI unverified). A missing report after a cut-off = rerun that slice with a neutral read-only brief. (3) CG1c baseline profile: app/build.gradle.kts add androidx.profileinstaller (1.4.1) + app/src/main/baseline-prof.txt with wildcard rules HSPLcom/tj/launcher/**;->**(**)** and HSPLcom/android/launcher3/**;->**(**)**, validate locally with bash tools/gradle.sh :app:compileReleaseArtProfile (one Gradle at a time), then a pure DexoptState parser + JVM test, show state in Launcher control + tablet report. (4) heartbeat trig_01EzvMtE44ag8oMav4DqoqHZ (fires :03 every 2 h; delete when CG1 is shipped).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  0da1d72b ckpt 1381: CG1e: section ro (RRO acceptance experiment: tools/make_overlay_apk.sh + emu
  7361a436 ckpt 1380: CG1: heartbeat armed trig_01EzvMtE44ag8oMav4DqoqHZ; 7 read-only reviewers la
  dc10ff72 ckpt 1379: CG1 written into TASKS (Tj's 6 asks, verbatim); prior research read (CA1 NEX
  7617cde9 ckpt 1378: handoff manifest saved (.handoff/), CE1/CF1 fully done and answered; nothing
  8201dfdb ckpt 1377: CE1/CF1 answered to Tj (v0.9.11 + LinkMeter v0.2.184 released; restart/no-ca
  659d20ad ckpt 1376: CE1: v0.9.11 RELEASED and RECORDED (android.yml #52 id 43772718433, publishe
  39dc3055 ckpt 1375: CE1a-c ticked, HANDOFF 0y updated; android.yml full_build dispatched on dcea
  dceaa412 ckpt 1374: gated v0.9.11 (code 50) and pushed it: the handoff self-test, the full unit 
  858ec985 ckpt 1373: pre-ship: v0.9.11: no VPN on the lock screen, with the screen off or in the 
  5c176218 ckpt 1372: CE1 GATE COMPLETE on product code c0d29aae (identical at HEAD): #611 id 4376
```
