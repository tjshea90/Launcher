# CHECKPOINT 1198 — read me first, then TASKS.md

**Written:** 2026-11-12T01:54:01Z · **tests:** all 10 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-393c88ba-k4wifl` · **builds on:** `bf161d6a` (this checkpoint is the commit after it)

## Just done
BX1 code done: Quickstep -> Gboard (KeyboardPolicy.pickGboard/noTargetStatus), custom untouched, look-alike hides Gboard unless only one, strings+docs, KeyboardPolicyTest 18 green, version 0.9.7 code 46

## Do this next
1) dispatch emulator.yml sections base,legacy,x on ref ccr-393c88ba-k4wifl (smoke only, Tj wants light testing); 2) bash ship.sh 'v0.9.7 note' (handoff test, FULL unit suite incl new status test, lint); 3) android.yml full_build=true; get_release_by_tag v0.9.7; record-release.sh; 4) tick BX1a-d in TASKS, HANDOFF 0t, answer Tj (last message, link on own line)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  89ac6020 ckpt 1197: BX1 written into TASKS (Tj: Quickstep keyboard Gboard instead of SwiftKey, c
  c4d6429b ckpt 1196: heartbeat 20:13Z: no other session working (newest main commit = own OUTBOX 
  ff35152f ckpt 1195: HANDOFF 0t rewritten as v0.9.6 RELEASED (run #47, BUILDLOG 09546785); answer
  46c47738 ckpt 1194: v0.9.6 (code 45) RELEASED+RECORDED: android.yml #47 id 43084983118 green on 
  bd61fbea ckpt 1193: SHIPPED v0.9.6 (code 45) gate green (e2302114, main == tip); android.yml ful
  e2302114 ckpt 1192: gated v0.9.6 (code 45) and pushed it: the handoff self-test, the full unit s
```

(23 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
