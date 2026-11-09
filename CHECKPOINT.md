# CHECKPOINT 810 — read me first, then TASKS.md

**Written:** 2026-11-09T07:19:12Z · **tests:** all 8 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `a0559e2` (this checkpoint is the commit after it)

## Just done
BN1m recorded (Tj 07:16: Display size says Custom, must look identical). Review C done (6 findings, not yet triaged); review A+B still running; run 460 (ss+shots, chat fix+SS28f) in flight, due ~07:27

## Do this next
1) BN1m-1: SsDisplayRules.densityStops rebuilds stops around the CURRENT density when it is no stop (never Custom) + tests + scenario no-Custom check; 2) BN1m-2: compare every CI shot with Tj's, TTS page greyed on emulator; 3) triage review C (home shortcut alias, Recents closer, crash process, pageAllowed in onCreate, lock, gate-map) + A + B when they land; 4) run 460 read, save ci-shots first; 5) gate, ship v0.9.0

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  74fd1d5 ckpt 809: BN1: build log decision 15 (pixel proof, chat font padding), CLAUDE.md BN1 les
  986698f ckpt 808: BN1l: tools/ss_chat_probe.py + test (SS28f pins chat = Tj Default: bubbles 75 
  c3e90a4 ckpt 807: BN1l: run 458 = 62 PASS 0 FAIL. Chat bubbles were 8 px too tall vs Tj (83 vs 7
  ab8f487 ckpt 806: BN5: 10 v0.8.13-review findings recorded (research/BN5_..., TASKS BN5a-c); 3 r
  ab80966 ckpt 805: BN1l: FONT_VALUES 1.0/1.2/1.3/1.4 measured from Tj's shots; title regular, Pre
  001ed92 ckpt 804: BN1l: Tj wants LOOK identical, function optional (hard / non-functional settin
  2015db9 ckpt 803: BN1: Storage agent done (SsPagesStorage/SsDonutView/SsStorageRules, 25 tests),
  fd1acac ckpt 802: BN1: case STORAGE + Home Storage summary wired (SsHomeSummary.loadStorage -> S
  9d560b7 ckpt 801: BN1: Home Battery summary (SsHomeSummary), case SOUND wired, TTS spacing fixed
  f98f160 ckpt 800: BN1: SETSYS for private mono/balance; PACKAGE_USAGE_STATS grant in every guide
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
