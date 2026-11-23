# CHECKPOINT 1640 — read me first, then TASKS.md

**Written:** 2026-11-23T03:58:07Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/dazzling-hawking-b7qz0b` · **builds on:** `dfa2dd2e` (this checkpoint is the commit after it)

## Just done
CX1b RAW VERDICT gm #768 id 45106026788 (head==tag v0.9.18): 18 PASS 5 FAIL. GM1 all 6 flash cases PASS (flashA=0 flashB=0: the patch never lights up on the emulator, slow/half/held/fast pad pulls, shade half, QS). GM3 control PASS (shot=172: analyser sees the entry). GM2 FAIL: gmSlow shot 10 fr/167ms (fast open age=115ms: main thread busy), gmFling 9 fr/150ms, gmTap 6 fr/100ms, gmQS 257 fr + gmMarathon 254 fr = analyser counts the OPEN SHADE as a menu (span = shade-open time) = false positive; pass: gmHalf gmHold gmShadeHalf gmPadOff gmTwice

## Do this next
(1) fl_analyze --from S (judge menu/shot only after the menu press) so shade cases are not false positives; (2) read seq lines of gmSlow/gmFling/gmTap in /root/.claude/projects/-home-user-Launcher/6d9fd5b5-e294-505d-9050-d139e9c305b9/tool-results/mcp-github-get_job_logs-1795405942394-tjan8w.txt; (3) hm run 45107548707 verdict at ~04:05Z; heartbeat trig_01GoGsAi9y4N2PpYgaAmbk6A

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  0e3ffaed ckpt 1639: CX1b: BASELINES in flight: gm #768 id 45106026788 (03:26Z), hm #769 id 45107
  36da7375 ckpt 1638: CX1b: section hm (home matrix) written (7 events x 0.2/0.8 s then power menu
  f274afbc ckpt 1637: CX1b: BASELINE gm run #768 id 45106026788 dispatched 03:26Z main ce7c7be7 (p
  ce7c7be7 ckpt 1636: CX1b: section gm (gesture matrix) written: 6 flash cases (pad slow/half/hold
  8c11a150 ckpt 1635: CX1a: LOCK + HOME + PENCIL audits all saved (research/CX1_AUDIT_*). Lock G3 
  53d517b3 ckpt 1634: CX1a: PENCIL audit saved research/CX1_AUDIT_PENCIL_2026-11-23.md. Key facts:
  5733f97b ckpt 1633: CX1a: usage cap (reset 03:10Z) killed the 3 audit subagents at ~02:57Z; Tj t
  d8282c26 ckpt 1632: CX1a: 3 read-only audit subagents launched 02:55Z (lock a7429c983f586954f, h
  c0a80247 ckpt 1631: CX1: heartbeat armed trig_01GoGsAi9y4N2PpYgaAmbk6A (every 2 h at :48)
  42045723 ckpt 1630: CX1 request written into TASKS (Tj 02:45Z: lock + home screenshot-button pat
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
