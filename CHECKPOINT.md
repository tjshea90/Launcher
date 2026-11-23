# CHECKPOINT 1672 — read me first, then TASKS.md

**Written:** 2026-11-23T07:36:35Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/dazzling-gates-hbco93` · **builds on:** `614edae0` (this checkpoint is the commit after it)

## Just done
gated v0.9.19 (code 58) and pushed it: the handoff self-test, the full unit suite + lint
and the versionCode check all passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow
android.yml, ref main, inputs {"full_build": "true"}. When that run is green,
confirm the Release is published (get_release_by_tag is enough) and then run:
  bash tools/record-release.sh v0.9.19 "CX1: lock + home power-menu patch hardening: the home patch window stays hidden through apps / drawer / folders / page flips, a menu scan that finds the texts but not the card no longer drops the patch, the resting patch waits for the closing shade, a second stock dialog is a re-opening not a shade, no window swap under an open menu, pencil fast watch at screen on"

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  70cfbcd6 ckpt 1671: CX1 release prep: version bumped to 0.9.19 code 58; gm solo #793 cancelled a
  f152f218 ckpt 1670: CX1h: bt solo main #792 id 45127351694 GREEN (BT1f was a flake); gm solo bra
  2450629a ckpt 1669: CX1h: TASKS box written (gate triage state, 8 runs); bt solo main + gm solo 
  b4d94d57 ckpt 1668: CX1 takeover 06:55Z (new session, branch dazzling-gates-hbco93, old session 
  8e2389c4 ckpt 1667: CX1 triage RAW 06:46Z: gate group #781 id 45121482111 ENDED 06:33 FAILED (de
  19582175 ckpt 1666: CX1 triage RAW 06:42Z: ul solo main #786 GREEN (UL24 = native-crash flake); 
  4f8ce7b1 ckpt 1665: CX1 gate 8dc58abf RAW FAILS: #779 fl 86P 2F: FL2 menuW1 flashA 5>4, FL2 menu
  30eb9413 ckpt 1664: CX1 gate 8dc58abf RAW: pt #783 id 45121488481 GREEN; #779 (ac,ad,se,ps,cu,fl
  9b9c664d ckpt 1663: removed stray off-topic message (Tj: wrong repo) from TASKS/INBOX/OUTBOX. Ga
  fe546733 ckpt 1662: CY1: Tj's parlay message written into TASKS (not a Launcher job, waits on Tj
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
