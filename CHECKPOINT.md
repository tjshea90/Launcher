# CHECKPOINT 1287 — read me first, then TASKS.md

**Written:** 2026-11-13T03:14:05Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-6e656bc6-wl2rhu` · **builds on:** `b38c8e7a` (this checkpoint is the commit after it)

## Just done
gate FAILs: #584 MU-12 beside NATIVE CRASH libhwui (known flake); #585 SS7 (beside jemalloc crash in ss) + FL2 menuS1 5 frames/83ms + FL2 menuP1 3 frames/50ms; #587 base recents + startup beside 4 native crashes (known); #586 #588 green

## Do this next
rerun solo: head fl, TAG v0.9.8 fl (compare FL2), head ss, head base, head mu; if FL2 differs from tag -> suspect U1 (click/pencil gate while pad up) or S3. heartbeat trig_01WL1UmCwAoqLLcPFywmb7Dv

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  b38c8e7a ckpt 1286: gate read 03:10Z: #586 y2..hp GREEN, #588 rg GREEN; #584 (job 131648084482) 
  6e3e0224 ckpt 1285: CA1b status in TASKS; answer draft research/CA1_RELEASE_MESSAGE_DRAFT_2026-1
  2d37f372 ckpt 1284: gate dispatched 02:33Z on 566fc305: #584 43352934985 x1,se,nq,rt,gear,wf,mu;
  566fc305 ckpt 1283: CA1b review fixes all in (MED1 reloadOwed, MED2 grant memo 3s+forget, LOW3/5
  d283250f ckpt 1282: CA1b review fixes: MED1 S2 reloadOwed (SettingsSpoof), MED2 grant memo 3 s +
  4f356f77 ckpt 1281: CA1b verdicts written in PERF_AUDIT (FIXED / REJECTED / NEXT)
  2e4760b8 ckpt 1280: CA1b: + U12 diagnostic (TjHooks.onLauncherCreated: lock-log LAUNCHER created
  c109ba0c ckpt 1279: CA1b: read-only review subagent running on git diff 4ce6f0f0 -- app/src; gat
  c2bb6338 ckpt 1278: CA1b batch1 coded + unit suite 1040/0 green (before M1); + S3 pencil watch p
  7900bf97 ckpt 1277: CA1b batch1: + M2 lock bitmaps dropped on home target / trim, M9 TimeFmt (+T
```
