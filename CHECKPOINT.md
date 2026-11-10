# CHECKPOINT 997 — read me first, then TASKS.md

**Written:** 2026-11-10T20:03:09Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `73ff790` (this checkpoint is the commit after it)

## Just done
gated v0.9.3 (code 42) and pushed it: the handoff self-test, the full unit suite + lint
and the versionCode check all passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow
android.yml, ref main, inputs {"full_build": "true"}. When that run is green,
confirm the Release is published (get_release_by_tag is enough) and then run:
  bash tools/record-release.sh v0.9.3 "BR1: the gear in the custom notification panel opens the Settings look-alike (Tj 2026-11-10); fast tier only, emulator section gear green"

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  634ed77 ckpt 994: BR1 emu run #518 (id 42889694843, job 130087240598, main@5360869, sections=gea
  44a6455 ckpt 992: BR1 emu run #518 id 42889694843 (sections=gear, shots, main@5360869) DISPATCHE
  5360869 ckpt 990: BR1: emulator section 'gear' written (scenario + gate-map + emulator.yml names
  ce9981c ckpt 987: BR1 code: GearRules(+13 tests green) + gear overlay in TjAccessibilityService 
  dcf71e3 ckpt 974: BR1 (gear -> look-alike) + BR2 (pencil removal research) written into TASKS in
  4f00f29 ckpt 971: Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE DISABLED 16:34Z (update_trigger enable
  57d90ce ckpt 970: Heartbeat firing 16:22Z: rule 2 no other session working, rule 3 no unanswered
  afe5fc4 ckpt 969: Heartbeat firing 14:24Z: rule 2 no other session working; v0.9.2 shipped/answe
  fbac8ea ckpt 967: b6 rerun #517 (id 42811195897, head 52093c7) GREEN: 21 PASS 0 FAIL; custom clo
  02daec4 ckpt 965: Run in flight: b6 rerun #517 id 42811195897 head 52093c7 since 11:53:51Z (done
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
