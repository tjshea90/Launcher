# CHECKPOINT 1649 — read me first, then TASKS.md

**Written:** 2026-11-23T04:52:13Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/dazzling-hawking-b7qz0b` · **builds on:** `40cd6da9` (this checkpoint is the commit after it)

## Just done
CX1 RAW VERDICT hm #772 id 45112677186 job 137488357728 (head 64dda30f keep-window): 12 PASS 7 FAIL (hmApp1 shot 170fr/2833ms flashA5, hmApp2 170fr, hmDrawer1 13fr/217ms, hmShade1 7fr, hmShade2 flashA28 2fr, hmOff1 124fr/2067ms, hmOff2 117fr/1950ms). baseline #771: 4 FAIL (App1/2, Shade1/2). hp #773 id 45112680139 not yet read

## Do this next
analyse #772 logs: hmApp now 'fast skip: already open' (no more 'no window yet') but long shot spans; hmOff1/2: 'scan menuOpen=true found=false -> menu closed' ~0.5-0.7 s after fast open then PATCH removed/window re-created (premature close); read hp; heartbeat trig_01GoGsAi9y4N2PpYgaAmbk6A

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  0d28ad66 ckpt 1648: CX1c: in flight on 64dda30f: hm #772 id 45112677186, hp #773 id 45112680139 
  64dda30f ckpt 1647: CX1c-H1: home window KEPT hidden through transient reasons (app/drawer/folde
  a2c1e498 ckpt 1646: CX1: gm baseline 2 #770 id 45109356329 job 137477900783 ran > 25 min (emulat
  e03bdd35 ckpt 1645: CX1 RAW VERDICT hm #771 id 45109794817 job 137479292087 (head==tag): 15 PASS
  a9b431f6 ckpt 1644: CX1: BASELINE 2 in flight on product code == tag v0.9.18: gm #770 id 4510935
  6ff6b527 ckpt 1643: CX1d/CX1e written into TASKS (baseline findings + fix plan); gm baseline 2 a
  6ca77abe ckpt 1642: CX1b: hm baseline #769 id 45107548707 = geometry bug only (core box empty: h
  bd1d674e ckpt 1641: CX1b: fl_analyze --from (menu judged only from the press; selftest 4b), flfu
  c5dbb2a9 ckpt 1640: CX1b RAW VERDICT gm #768 id 45106026788 (head==tag v0.9.18): 18 PASS 5 FAIL.
  0e3ffaed ckpt 1639: CX1b: BASELINES in flight: gm #768 id 45106026788 (03:26Z), hm #769 id 45107
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
