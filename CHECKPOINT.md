# CHECKPOINT 923 — read me first, then TASKS.md

**Written:** 2026-11-10T10:23:04Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `3ec729c` (this checkpoint is the commit after it)

## Just done
BQ3 round 2 read (#511 42797400107: 16 PASS 4 FAIL; BQ3 custom+quickstep FAIL = stuck cover after collapse). Facts in research/BQ3_STUCK_COVER_2026-11-10.md: pencil node+ancestors read V with open bounds while panel closed; tree sketch open state shows keyguard clock still V. Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

## Do this next
1) grep the saved job log /root/.claude/projects/-home-user-Launcher/f57c5b9c-bf62-5be7-8f0c-bd772cf12bbe/tool-results/mcp-github-get_job_logs-1794306018529.txt for 'BQ3 (custom) C closed' + 'B open' tree lines and compare V/h per id (open vs closed) to find a discriminator; 2) implement it in updatePencil/findPencil (+ PencilRules pure fn + JVM test), fallback to old behaviour if node missing; 3) fix bq3_pick awk (stop after 1 probe); 4) rerun b6,n (+ad); 5) then release v0.9.2. Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md
    ?? research/BQ3_STUCK_COVER_2026-11-10.md

## Last ten checkpoints
```
  3ec729c ckpt 922: BQ3 round 2: run #510 b6 on head (42795642177, 14 PASS 6 FAIL): BQ3 closed-pan
  135053e ckpt 913: BQ3 diagnostics coded (debug-only PENCIL probe in service; b6: closed-panel sa
  9c1b18d ckpt 904: Triage 09:50Z: b6 FAILS on head #507 (42792069234) AND tag v0.9.1 #509 (427920
  ad4c424 ckpt 901: Triage 09:41Z: base SOLO #506 (42790941782) GREEN = native-crash flake; n SOLO
  a837b79 ckpt 900: Gate 6878395 raw verdict: #504 b6,n,o,x,ac,ad = 98 PASS 4 FAIL (BK1b no-cover 
  4f7d798 ckpt 898: Gate 6878395 verdict 09:27Z: #503 pt GREEN (0 FAIL; BQ1a/BQ2 pass, dark look 0
  bd81eab ckpt 894: Gate dispatched on 6878395 09:10Z: #503 pt (run 42788494486), #504 b6,n,o,x,ac
  6878395 ckpt 893: Review fixes coded: scrim counted at run start (+async end, report w/ sig), pe
  c33b797 ckpt 882: Both reviews read; verdicts saved in research/BQ1_REVIEW_2026-11-10.md. Scrim:
  a04e129 ckpt 881: BQ2a answered in chat (09:00Z); BQ2b coded, not released. Heartbeat trig_013YU
```
