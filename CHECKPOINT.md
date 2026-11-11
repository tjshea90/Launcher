# CHECKPOINT 1127 — read me first, then TASKS.md

**Written:** 2026-11-11T07:19:51Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-3e66a8c0-ruc8aa` · **builds on:** `dce6378e` (this checkpoint is the commit after it)

## Just done
Tier-0 local green (990 unit tests 0 fail, lint ran, abortOnError false by design). Read the post-review production delta (~75 lines): no defect found. Looked at run 532 pictures: ok. v0.9.5 answer draft already exists

## Do this next
Heartbeat trig_01GVcBRvTAzqf97UhMTJszab. Gate runs on 980012c dispatched 07:15Z: #533 id 42978120944 (base,x1,n,v,ps,hp,hf,fp), #534 id 42978125724 (e1,o,ac,ae,pa,rb,ss,bt,last), #535 id 42978130132 (pt), #536 id 42978135973 (rg). rg done ~07:21, pt ~07:33, others ~07:48. Read each with get_job_logs run_id + failed_only + tail_lines 45; ckpt the verdict BEFORE analysing. Then bump app/build.gradle.kts to 0.9.5 / code 44, bash ship.sh, android.yml full_build=true on ccr-3e66a8c0-ruc8aa, get_release_by_tag, tools/record-release.sh, send research/V095_RELEASE_MESSAGE_DRAFT (verify its Home-chooser claim in item 6: review says chooser shows RouterActivity label Launcher)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  dce6378e ckpt 1126: gate dispatched 07:15Z on 980012c: #533 id 42978120944 (base,x1,n,v,ps,hp,hf
  980012c1 ckpt 1125: TAKEOVER (branch ccr-3e66a8c0-ruc8aa) on Tj's order: other account hit its u
  11c1c313 ckpt 1124: review A fixes in (focus tolerance 450 + launcherFocus rule, warm card short
  9671f217 ckpt 1123: review B (HIGH fixed): lock patch return = one level-triggered timer (PatchR
  cef15801 ckpt 1122: review C done (7 LOW): fixed #1 #2 (service label + AppLabelTest 3 tests gre
  323f69fd ckpt 1121: BT1b boxed [~], BT1 findings written to research/BT1_ANALYSIS; review slices
  9a8b660d ckpt 1120: fix compile error (comment swallowed addPatch tail); unit suite green; BT1b:
  c0c1840a ckpt 1119: bt run #532 (id 42972187857, job 130362986310, head eae2cb4) GREEN: ctl/g04/
  eae2cb44 ckpt 1118: hp run #531 (id 42970809372, job 130358595632) GREEN on 79c74bf (0 FAIL). bt
  0565d017 ckpt 1117: run #530 (id 42970805392, job 130358583086, bt on 79c74bf): 7 FAIL (ctl, g04
```
