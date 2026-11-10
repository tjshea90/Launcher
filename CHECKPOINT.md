# CHECKPOINT 894 — read me first, then TASKS.md

**Written:** 2026-11-10T09:12:02Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `6878395` (this checkpoint is the commit after it)

## Just done
Gate dispatched on 6878395 09:10Z: #503 pt (run 42788494486), #504 b6,n,o,x,ac,ad (42788499938), #505 base,x1,e1,pa,rb,last (42788505648). Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

## Do this next
Read the 3 runs at about 09:26Z (get_job_logs run_id + failed_only tail 60: 'No failed jobs' = green; else list_workflow_jobs); ckpt raw verdict (ids, PASS/FAIL, FAIL names) BEFORE analysing; triage fails SOLO vs tag v0.9.1; then bump 0.9.2/41, ship.sh, android.yml full_build, record-release, ONE message to Tj (draft research/V092_RELEASE_MESSAGE_DRAFT_2026-11-10.md). Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  6878395 ckpt 893: Review fixes coded: scrim counted at run start (+async end, report w/ sig), pe
  c33b797 ckpt 882: Both reviews read; verdicts saved in research/BQ1_REVIEW_2026-11-10.md. Scrim:
  a04e129 ckpt 881: BQ2a answered in chat (09:00Z); BQ2b coded, not released. Heartbeat trig_013YU
  8d587e9 ckpt 880: BQ2b coded (Tj: measure once, never screenshot again): dark look never measure
  15f72bb ckpt 879: Tj asked (BQ2): why not measure once and never screenshot again (wallpaper unc
  b016ab1 ckpt 878: BQ1a: a freshly (re)connected service settles from its connect (ScrimMeasure.s
  1479de8 ckpt 877: v0.9.2 answer draft saved (research/V092_RELEASE_MESSAGE_DRAFT_2026-11-10.md).
  b295cfe ckpt 876: BQ1b coded: PencilRules.FingerGuard + service wiring (penFingerDown, both guar
  1be727b ckpt 875: BQ1b design written (TASKS BQ1b-DESIGN): FingerGuard = hide cover at finger DO
  c934b14 ckpt 874: BQ1a fix coded: ScrimMeasure settle 10 s, 1 run/lock, EVERY run that stored no
```
