# CHECKPOINT 994 — read me first, then TASKS.md

**Written:** 2026-11-10T20:00:51Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `1ca6410` (this checkpoint is the commit after it)

## Just done
BR1 emu run #518 (id 42889694843, job 130087240598, main@5360869, sections=gear): 9 PASS 0 FAIL. pencil [4,564][55,615] gear [545,564][596,615]; tap -> com.tj.launcher/.SettingsAlias, 'shade collapsed: true'; control (switch off) opens com.android.settings. Unit suite 935 green, lint green

## Do this next
1) fetch ci-shots (git fetch origin ci-shots; git archive FETCH_HEAD into a fresh scratchpad dir) and LOOK at gear-01-panel.png + gear-02-after-tap.png (shade really gone? overlay on the gear?). 2) bash ship.sh 'BR1 gear opens the Settings look-alike'; 3) android.yml full_build=true on main; get_release_by_tag v0.9.3; bash tools/record-release.sh v0.9.3 note; 4) tick BR1/BR2 in TASKS; ONE message to Tj with the Release link alone on its own line

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  44a6455 ckpt 992: BR1 emu run #518 id 42889694843 (sections=gear, shots, main@5360869) DISPATCHE
  5360869 ckpt 990: BR1: emulator section 'gear' written (scenario + gate-map + emulator.yml names
  ce9981c ckpt 987: BR1 code: GearRules(+13 tests green) + gear overlay in TjAccessibilityService 
  dcf71e3 ckpt 974: BR1 (gear -> look-alike) + BR2 (pencil removal research) written into TASKS in
  4f00f29 ckpt 971: Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE DISABLED 16:34Z (update_trigger enable
  57d90ce ckpt 970: Heartbeat firing 16:22Z: rule 2 no other session working, rule 3 no unanswered
  afe5fc4 ckpt 969: Heartbeat firing 14:24Z: rule 2 no other session working; v0.9.2 shipped/answe
  fbac8ea ckpt 967: b6 rerun #517 (id 42811195897, head 52093c7) GREEN: 21 PASS 0 FAIL; custom clo
  02daec4 ckpt 965: Run in flight: b6 rerun #517 id 42811195897 head 52093c7 since 11:53:51Z (done
  52093c7 ckpt 963: b6 custom flow: the BN8b drag ends at the PIN pad on the CI emulator: first BA
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
