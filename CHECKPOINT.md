# CHECKPOINT 807 — read me first, then TASKS.md

**Written:** 2026-11-09T07:09:47Z · **tests:** all 7 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `0b74d03` (this checkpoint is the commit after it)

## Just done
BN1l: run 458 = 62 PASS 0 FAIL. Chat bubbles were 8 px too tall vs Tj (83 vs 75 px @170): setIncludeFontPadding(false) on msg+time fits all 4 single-line stops (63/75/88/100). next run (ss+shots) in flight on older head

## Do this next
1) wait run on main (dispatched ~07:07) -> read, save ci-shots (ss-19b/c/d, ss-20b) BEFORE next shots run; 2) triage agents A/B/C; 3) re-dispatch ss+shots with the chat fix, compare ss-20 vs Tj 8a1f9456 (bubble 75 px, text top +20, time top +47); 4) gate, ship

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  ab8f487 ckpt 806: BN5: 10 v0.8.13-review findings recorded (research/BN5_..., TASKS BN5a-c); 3 r
  ab80966 ckpt 805: BN1l: FONT_VALUES 1.0/1.2/1.3/1.4 measured from Tj's shots; title regular, Pre
  001ed92 ckpt 804: BN1l: Tj wants LOOK identical, function optional (hard / non-functional settin
  2015db9 ckpt 803: BN1: Storage agent done (SsPagesStorage/SsDonutView/SsStorageRules, 25 tests),
  fd1acac ckpt 802: BN1: case STORAGE + Home Storage summary wired (SsHomeSummary.loadStorage -> S
  9d560b7 ckpt 801: BN1: Home Battery summary (SsHomeSummary), case SOUND wired, TTS spacing fixed
  f98f160 ckpt 800: BN1: SETSYS for private mono/balance; PACKAGE_USAGE_STATS grant in every guide
  f0e2bbb ckpt 799: BN1: run #455 ss = 40 PASS 2 FAIL (mono/balance are PRIVATE system settings, a
  75be91c ckpt 798: BN1 M3b: Font size + Display size pages (SsPagesSize, 5 vector icons, discrete
  48b3aa0 ckpt 797: BN1 M3: Accessibility+TTS+Magnification+Touch&hold pages (SsPagesAccessibility
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
