# CHECKPOINT 1213 — read me first, then TASKS.md

**Written:** 2026-11-12T20:07:24Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-91599ec3-za40s7` · **builds on:** `82fcc76d` (this checkpoint is the commit after it)

## Just done
#564 id 43294929647 (fl, fixed analyzer, b3b0c4d7): 17 PASS 2 FAIL. FAIL FL1 shade flashA=140 (2.33 s, A3.03-5.37: patch back over the OPEN shade = F1 proven). FAIL FL2 menuP1 shot=10 (167 ms, S0.72-0.88: menu 1.5 s after the PIN pad left = F3; menuP2 shot 0). Diag menuPU (menu over the OPEN pad): shot=172 = Screenshot visible the whole menu (pin hide after fast open). wake films flashA 0 (F2 not seen on emu), tap/pad/menuL/W/S all 0. shadeX too-few-frames

## Do this next
BY1b/c: git fetch origin ci-shots, LOOK at fl-sheet-shade/menuP1/menuPU.png; design fixes: F1 shade-open hold (events: SUI 0x800 FrameLayout x10 in shade film), F3 fastDecline after pad, menuPU pin hide. Heartbeat trig_01JYEUXs7JosHyg85f2T1oCK

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  9577dc4d ckpt 1212: BY1a: 4 read-only perf reviewers launched 20:00Z (A startup+drawer, B a11y s
  3f825827 ckpt 1211: BY1b: run #564 id 43294929647 (fl, shots=true) dispatched 19:54Z on b3b0c4d7
  b3b0c4d7 ckpt 1210: BY1b: fl_analyze fixed (2 test bugs that made #563 unreadable): (1) VFR film
  ca1a23e2 ckpt 1209: acct-2 heartbeat ARMED trig_01JYEUXs7JosHyg85f2T1oCK (cron 44 */2, bound to 
  6e3af8cb ckpt 1208: TAKEOVER (account 2, branch ccr-91599ec3-za40s7; acct-1 session cut off, las
  861a3526 ckpt 1207: BY1b: fl BEFORE run #563 id 43273214570 dispatched 17:48Z on fcc27808; fl go
  fcc27808 ckpt 1206: BY1b: section fl written (films: wakeH1/2 wakeL shade shadeX pad tap + menuL
  59478275 ckpt 1205: BY1b: tools/fl_analyze.py (selftest OK) + section fl design in research/BY1_
  4c088a33 ckpt 1204: BY1-F: 4 code findings (lock patch over open shade, dark-rest at wake, fastD
  b83f8177 ckpt 1203: BY1 written into TASKS (Tj 17:26Z: perf review + lock/home patch flicker fil
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
