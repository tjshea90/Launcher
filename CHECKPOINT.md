# CHECKPOINT 1713 — read me first, then TASKS.md

**Written:** 2026-11-28T01:32:29Z · **tests:** all 12 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/wonderful-franklin-vlm90o` · **builds on:** `92146a17` (this checkpoint is the commit after it)

## Just done
DA1 FINISHED 2026-11-28: no product code changed, no release. Measured (runs #796 id 46083514184, #797 id 46084969954, #799 id 46091238924, #800 id 46092484975): idle 0.03 percent of a core / 0 frames, PIN unlock 9 ms, power menu 2 ms, drawer/pages/app launch/shade/Recents tiny, only cold start has CPU; release apk compile: verify 346 ms cold / PSS 49.7 MB, speed-profile 284 ms / 49.0 MB (anon native heap -8.2 MB), speed 322 ms / 51.6 MB; shipped baseline profile verified end to end (14.9k hot methods installed, 285 classes); P1 class lines SKIPPED (3.4 KB RAM per class, runtime adds classes itself). Built: tools/trace_top.py + test, sections pr / pr2, release-apk step in emulator.yml. TASKS DA1b-DA1e ticked, DA1f waits on Tj. Heartbeat trig_01TAXAYoGyEPAhuzh7PzBQm8 DELETED.

## Do this next
nothing Claude-owned is left; wait for Tj: (1) a fresh tablet report from v0.9.20 (PSS split with Graphics + compile state): tools/tablet-intake.sh then research/tablet/LATEST.md; judge X1 (overlay window GPU memory) and whether the nightly compile ran; (2) if Tj asks to change the Setup compile item to speed-profile, see research/DA1_PERF_PROBE_2026-11-27.md decision P4 (needs a profile-installed check first). No heartbeat is armed.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  4bf879b5 ckpt 1712: DA1b: dispatched emulator.yml sections=pr2 shots=true: run #800 id 460924849
  be0acfae ckpt 1711: DA1b: pr2 run 3 prepared (true verify baseline via compile -m verify -f, sta
  15eb44cd ckpt 1710: DA1b: run #799 id 46091238924 RESULT: release apk profile pipeline WORKS (pr
  9b11c154 ckpt 1709: DA1: LESSONS.md section for this pass (profiler method, debuggable apk caps 
  7cb3367c ckpt 1708: DA1b: run #798 id 46086858124 FAILED in the workflow step 'Build the release
  13c38769 ckpt 1707: DA1b: dispatched emulator.yml sections=pr2 shots=true: run #798 id 460868581
  12357dd7 ckpt 1706: DA1b: run #797 id 46084969954 (sections pr,pr2) read: PIN unlock = 9 ms main
  6a741e60 ckpt 1705: DA1: heartbeat armed (trig_01TAXAYoGyEPAhuzh7PzBQm8, cron 1 */2 * * *, first
  c3a5ca9d ckpt 1704: DA1b: findings of run #796 written (research/DA1_PERF_PROBE_2026-11-27.md, T
  aefb878e ckpt 1703: DA1b: scenario pr gets the PIN-unlock and power-menu phases; NEW section pr2
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
