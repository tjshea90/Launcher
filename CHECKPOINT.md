# CHECKPOINT 1520 — read me first, then TASKS.md

**Written:** 2026-11-21T21:20:53Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/linkmeter-keyboard-launcher-fixes-fsk78t` · **builds on:** `7059dc3e` (this checkpoint is the commit after it)

## Just done
CS2 triage: head N2 fails ONLY when a native crash hit base earlier (fail: #694 #698 #702 #707 all crash; pass: #700 #708 no crash; pre-fix #692 fail no crash); #707 N2 detail: handshake=none lead=ND (pass has lead=DD erase2); tag chain lq,uf,cs,cs2,pk #705 also FAILS (CS-A1 + crash in cs) so CS-B2/CS-D2 chain fail = env; tag base,n #699 crash+PASS; sent 4 more runs (head branch+main base,n,last; tag base,n and base,n,last); #706 tag base,n,cu,pf,rb,last still running

## Do this next
read the 4 new runs + #706 via haiku brief (header, FAILs, crash section); if head fails N2 without any crash or tag never fails with one: bisect; else flake: ship (ship.sh, android.yml full_build ONCE, record-release)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  b636f8e4 ckpt 1519: scenario only: N2 now prints mode/resumed/keyguard/a11y + lock log tail as I
  af0127bc ckpt 1518: CS2 gate on head 37b99dad-code (early-out): #702 id 44948036203 base,n,cu,pf
  37b99dad ckpt 1517: CS2 head b4485d0a: base,n #700 id 44947211442 GREEN, pm #701 id 44947217420 
  b4485d0a ckpt 1516: CS2: primePowerMenu now returns at once when the switch is off / automation 
  b47f4085 ckpt 1515: CS2 triage: cs SOLO green on head (#695 id 44944662421) and tag (#697); base
  3f13c121 ckpt 1514: CS2 gate verdict on 96d0596b: #691 GREEN (x1,e1,o,ac,hp,pm); #692 71 PASS 9 
  b90ed541 ckpt 1513: CS2 gate dispatched on 96d0596b: #691 id 44942545183 x1,e1,o,ac,hp,pm | #692
  96d0596b ckpt 1512: CS2: prime end counts toward the 1-minute rule only when the menu was really
  3f77a1df ckpt 1511: CS2: run #690 pm 20 PASS 1 FAIL (PM4): its premise was wrong (setmode force-
  154d6cf0 ckpt 1510: CS2: run #689 pm: 18 PASS 3 FAIL = test bugs only (PM2 regexes, order check 
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
