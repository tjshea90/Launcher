# CHECKPOINT 815 — read me first, then TASKS.md

**Written:** 2026-11-09T17:45:16Z · **tests:** all 8 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `383147b` (this checkpoint is the commit after it)

## Just done
BO1a answered; SettingsSpoof.defaultOn flipped to !debuggable (+test releaseDefaultIsOnAndDebugDefaultIsOff, not yet run). ss run dispatched 17:39 on 3ae7454 in flight, timer 17:48:30; reviews A/B running

## Do this next
1) on the timer: read the ss run (list emulator.yml main workflow_dispatch perPage 1, then jobs, then logs tail 58), SAVE ci-shots first (git fetch origin ci-shots; git archive into a fresh scratchpad dir); expect SS26d + SS28g PASS; 2) triage reviews A/B notifications; 3) bash tools/gradle.sh :app:testDebugUnitTest (full), gate.sh edbf66d, ship v0.9.0. Heartbeat id: trig_01DhC2W8FUPFeqdGFQCt7yWe

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md
     M TASKS.md

## Last ten checkpoints
```
  58bc177 ckpt 814: ss+shots dispatched 17:39 on 3ae7454 (run id: list emulator.yml main workflow_
  3ae7454 ckpt 813: Run 460 (42578077589) = 65 PASS 0 FAIL, chat = Tj Default to the pixel (SS28f 
  b7821b6 ckpt 812: BO1c done: CLAUDE.md Auto-resume rule, HANDOFF Rung 5, resume.sh line (briefin
  a9f1202 ckpt 811: BN1m-1 code+tests done (SsDisplayRulesTest 8 green; SS28g pending CI). BO1: he
  35e93e9 ckpt 810: BN1m recorded (Tj 07:16: Display size says Custom, must look identical). Revie
  74fd1d5 ckpt 809: BN1: build log decision 15 (pixel proof, chat font padding), CLAUDE.md BN1 les
  986698f ckpt 808: BN1l: tools/ss_chat_probe.py + test (SS28f pins chat = Tj Default: bubbles 75 
  c3e90a4 ckpt 807: BN1l: run 458 = 62 PASS 0 FAIL. Chat bubbles were 8 px too tall vs Tj (83 vs 7
  ab8f487 ckpt 806: BN5: 10 v0.8.13-review findings recorded (research/BN5_..., TASKS BN5a-c); 3 r
  ab80966 ckpt 805: BN1l: FONT_VALUES 1.0/1.2/1.3/1.4 measured from Tj's shots; title regular, Pre
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
