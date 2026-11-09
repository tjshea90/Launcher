# CHECKPOINT 817 — read me first, then TASKS.md

**Written:** 2026-11-09T17:49:57Z · **tests:** all 8 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `ca45344` (this checkpoint is the commit after it)

## Just done
Run 461 (42666684837, 3ae7454) = 67 PASS 0 FAIL: Display size reads Default everywhere (SS28g), chat = Tj Default (SS28f); shots in scratchpad shots461. BN1m-1 ticked. BO2 (Tj: TTS page NOT greyed) is next; reviews A a69a3c15e2219f5c7, B a052e4a9a97c20628 still running

## Do this next
1) SsPagesTts.java rewrite tts(): no TextToSpeech, no writes, fixed texts 'Pico TTS' + inert gear widget (new res/drawable/ss_ic_gear.xml, Material settings path), 'Use system language', sliders new SeekBar(a) enabled no listener progress rateProgress(100)/pitchProgress(100), PLAY/RESET enabled no-op; drop dead code after grep of users; 2) tools/emulator_scenario.sh ~3989: replace SS26d greyed pixel check by SS26d-h (texts, enabled nodes, drag moves thumb + settings get secure tts_default_rate unchanged, thumb colour = Sound's, gear content-desc); 3) triage reviews A/B notifications; 4) ss run on new head (shots=true), save ci-shots at once; 5) full gradle test, gate.sh edbf66d, ship v0.9.0. Heartbeat id: trig_01DhC2W8FUPFeqdGFQCt7yWe

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md
     M TASKS.md

## Last ten checkpoints
```
  ca45344 ckpt 816: BO2 recorded (Tj: TTS page not greyed: Pico TTS + inert gear, Use system langu
  b363c86 ckpt 815: BO1a answered; SettingsSpoof.defaultOn flipped to !debuggable (+test releaseDe
  58bc177 ckpt 814: ss+shots dispatched 17:39 on 3ae7454 (run id: list emulator.yml main workflow_
  3ae7454 ckpt 813: Run 460 (42578077589) = 65 PASS 0 FAIL, chat = Tj Default to the pixel (SS28f 
  b7821b6 ckpt 812: BO1c done: CLAUDE.md Auto-resume rule, HANDOFF Rung 5, resume.sh line (briefin
  a9f1202 ckpt 811: BN1m-1 code+tests done (SsDisplayRulesTest 8 green; SS28g pending CI). BO1: he
  35e93e9 ckpt 810: BN1m recorded (Tj 07:16: Display size says Custom, must look identical). Revie
  74fd1d5 ckpt 809: BN1: build log decision 15 (pixel proof, chat font padding), CLAUDE.md BN1 les
  986698f ckpt 808: BN1l: tools/ss_chat_probe.py + test (SS28f pins chat = Tj Default: bubbles 75 
  c3e90a4 ckpt 807: BN1l: run 458 = 62 PASS 0 FAIL. Chat bubbles were 8 px too tall vs Tj (83 vs 7
```
