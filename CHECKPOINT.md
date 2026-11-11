# CHECKPOINT 1125 — read me first, then TASKS.md

**Written:** 2026-11-11T07:14:44Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-3e66a8c0-ruc8aa` · **builds on:** `9beffd52` (this checkpoint is the commit after it)

## Just done
TAKEOVER (branch ccr-3e66a8c0-ruc8aa) on Tj's order: other account hit its usage limit. Checked: no CI run in flight, v0.9.4 latest, final gate NOT yet dispatched, product tree = ckpt 1124 (990 unit tests green). Armed heartbeat. Tier-0 unit+lint running locally

## Do this next
Heartbeat trig_01GVcBRvTAzqf97UhMTJszab (this session; the OTHER account has trig_01DZ15y7gsi1bXpJ7P2KBghc, cannot be read from here). NEXT: read scratchpad tier0.log (GRADLE_EXIT=0?), then dispatch the 4 gate runs from 'bash tools/gate.sh 9676905' on ref ccr-3e66a8c0-ruc8aa (base,x1,n,v,ps,hp,hf,fp | e1,o,ac,ae,pa,rb,ss,bt,last | pt SOLO | rg SOLO); list runs first; ckpt run ids; read each RESULTS; ship v0.9.5 only if green

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  11c1c313 ckpt 1124: review A fixes in (focus tolerance 450 + launcherFocus rule, warm card short
  9671f217 ckpt 1123: review B (HIGH fixed): lock patch return = one level-triggered timer (PatchR
  cef15801 ckpt 1122: review C done (7 LOW): fixed #1 #2 (service label + AppLabelTest 3 tests gre
  323f69fd ckpt 1121: BT1b boxed [~], BT1 findings written to research/BT1_ANALYSIS; review slices
  9a8b660d ckpt 1120: fix compile error (comment swallowed addPatch tail); unit suite green; BT1b:
  c0c1840a ckpt 1119: bt run #532 (id 42972187857, job 130362986310, head eae2cb4) GREEN: ctl/g04/
  eae2cb44 ckpt 1118: hp run #531 (id 42970809372, job 130358595632) GREEN on 79c74bf (0 FAIL). bt
  0565d017 ckpt 1117: run #530 (id 42970805392, job 130358583086, bt on 79c74bf): 7 FAIL (ctl, g04
  fb838402 ckpt 1116: dispatched at 06:12Z on head 79c74bf: bt run #530 (id 42970805392, main, sho
  79c74bf6 ckpt 1115: BT1c/BT1d code: HomePatchRules.openTreatment (fresh/stale/full card) + notFr
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
