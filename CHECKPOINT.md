# CHECKPOINT 821 — read me first, then TASKS.md

**Written:** 2026-11-09T18:12:08Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `8023aba` (this checkpoint is the commit after it)

## Just done
Review A+B fixes done (B1 B4/A3 B5 B6 B7 B8 A1 A2 A4 A5), full unit tests 864 green (5 new); lint running. BN1o ticked. Run 462 SS7 = native-crash flake

## Do this next
1) read lint (SP g/g16.log GRADLE_EXIT=); 2) bash tools/gate.sh edbf66d150cd65208d22d3e5344dc02164c7471c, dispatch its groups on main, read each (failed_only tail ~48), rerun failing sections SOLO; 3) bash ship.sh v0.9.0 note, android.yml full_build=true on main, get_release_by_tag, record-release.sh only after green, Release link plain text; 4) finalize research/TABLET_CHECKLIST_v0.9.0.md, final message to Tj with batched questions + BN1m-3 answer. Heartbeat id: trig_01DhC2W8FUPFeqdGFQCt7yWe

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md
     M TASKS.md

## Last ten checkpoints
```
  2241b0e ckpt 820: BO2 proven: run 462 (42669335036, 0d95241, ss) 69 PASS 1 FAIL; SS26d-g PASS, p
  11c5b53 ckpt 819: Review B done: 8 LOW, no medium/high (research/BN1_REVIEW_B_2026-11-09.md). BO
  0d95241 ckpt 818: BO2 code done: SsPagesTts inert (Pico TTS + gear, Use system language, enabled
  e83b256 ckpt 817: Run 461 (42666684837, 3ae7454) = 67 PASS 0 FAIL: Display size reads Default ev
  ca45344 ckpt 816: BO2 recorded (Tj: TTS page not greyed: Pico TTS + inert gear, Use system langu
  b363c86 ckpt 815: BO1a answered; SettingsSpoof.defaultOn flipped to !debuggable (+test releaseDe
  58bc177 ckpt 814: ss+shots dispatched 17:39 on 3ae7454 (run id: list emulator.yml main workflow_
  3ae7454 ckpt 813: Run 460 (42578077589) = 65 PASS 0 FAIL, chat = Tj Default to the pixel (SS28f 
  b7821b6 ckpt 812: BO1c done: CLAUDE.md Auto-resume rule, HANDOFF Rung 5, resume.sh line (briefin
  a9f1202 ckpt 811: BN1m-1 code+tests done (SsDisplayRulesTest 8 green; SS28g pending CI). BO1: he
```

(15 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
