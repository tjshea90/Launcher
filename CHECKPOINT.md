# CHECKPOINT 1050 — read me first, then TASKS.md

**Written:** 2026-11-11T00:17:22Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `ce7f000` (this checkpoint is the commit after it)

## Just done
BS2d: emulator run #520 (id 42930004319, main@ce7f000, sections=wf shots=true) dispatched 00:16:09Z; read-only review of the BS2 delta started

## Do this next
when run 42930004319 ends (~00:24Z): list_workflow_jobs -> get_job_logs tail_lines ~58, ckpt the RAW verdict (PASS/FAIL counts, FAIL names) BEFORE analysing; fetch ci-shots (git archive into a fresh scratchpad dir) and look at wf pictures; fix FAILs; then ship v0.9.4 (ship.sh, android.yml full_build, record-release, answer Tj for BS1+BS2). Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  ce7f000 ckpt 1049: BS2: docs (TASKS BS1b/2a/2b/2c done, HANDOFF row, research moments walked), s
  65bf8f5 ckpt 1046: BS2a code done: Radio* rename, wifi+bluetooth slots, keepsRadiosOn mirrors th
  d41f968 ckpt 1030: BS2: scenario wf v2 written (Bluetooth list, picture probe, lock control, pha
  b2cfbb6 ckpt 1026: BS1d wf run #519 (id 42926361302, job 130213726774, main@8babd57): 24 PASS 0 
  d1c4de3 ckpt 1025: BS2 (Tj: also Bluetooth, only if already on, never turn on, never show) writt
  8e2f709 ckpt 1021: BS1: TASKS BS1c ticked, HANDOFF row added; wf emulator run dispatched on main
  8babd57 ckpt 1018: BS1d: emulator section 'wf' written (scenario + gate-map + emulator.yml names
  dece682 ckpt 1015: BS1c code: WifiIconHold (24 tests green) + WifiIconHide + hooks (syncNow/tick
  e40df78 ckpt 1006: BS1a done: AOSP10 source confirms icon_blacklist=wifi hides the symbol in sta
  1ae6eee ckpt 1004: BS1 (Tj's Wi-Fi symbol question) written into TASKS; v0.9.3 answer is in OUTB
```
