# CHECKPOINT 1084 — read me first, then TASKS.md

**Written:** 2026-11-11T01:45:30Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `e169ed0` (this checkpoint is the commit after it)

## Just done
BS2d: wf run #522 (id 42934234064, job 130240218023, main@8fbdc7d): 37 PASS 1 FAIL 20 INFO; FAIL = WF13e only (deferral line 00:53:07.514, 'shown again' 6 ms later by a 2nd sync inside the window: window releases instead of holding back)

## Do this next
fix: RadioIconHold.releaseDecision (RELEASE/START/HOLD_BACK with state idle/running/finishing), RadioIconHide worker sets FINISHING before its final syncNow; strengthen WF13e (no 'shown again' between deferral and 'may come back'); unit tests; rerun wf. Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  81fd496 ckpt 1081: BS2d: wf run #522 (id 42934234064, main@8fbdc7d, sections=wf shots=true) disp
  8fbdc7d ckpt 1080: BS2 third-review fixes coded: deferred symbol release (RadioIconHide.deferRel
  2f6db42 ckpt 1075: BS2d: wf run #521 (id 42932077928, job 130233048653, main@8a93cbf = code BEFO
  c41a4c5 ckpt 1070: BS2d: emulator run #521 (id 42932077928, main@8a93cbf = ckpt 1068, sections=w
  8a93cbf ckpt 1068: BS2 review fixes DONE: F1 stale lq_prev forgotten at helper connect, F2 radio
  10f38f1 ckpt 1059: BS2 review fixes IN PROGRESS: done = NotifQuietPolicy.WHY_NO_HELPER, RadioIco
  6e6b2c4 ckpt 1054: BS2d: wf run #520 (id 42930004319, main@ce7f000, code before the review fixes
  52f666f ckpt 1050: BS2d: emulator run #520 (id 42930004319, main@ce7f000, sections=wf shots=true
  ce7f000 ckpt 1049: BS2: docs (TASKS BS1b/2a/2b/2c done, HANDOFF row, research moments walked), s
  65bf8f5 ckpt 1046: BS2a code done: Radio* rename, wifi+bluetooth slots, keepsRadiosOn mirrors th
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
