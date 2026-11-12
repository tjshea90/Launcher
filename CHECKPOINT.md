# CHECKPOINT 1250 — read me first, then TASKS.md

**Written:** 2026-11-12T22:34:41Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-91599ec3-za40s7` · **builds on:** `b51fe2b4` (this checkpoint is the commit after it)

## Just done
GATE RAW: #569 109P/6F all bt (BT1c-ctl, g04-never, g12-never, g12, g4-never, g4), fl GREEN, no crash; #570 91P/58F (SE-F2,R2,C2,L + ~54 SS*) + NATIVE CRASH jemalloc/libziparchive; #571 112P/1F CS-B2 no crash; #572 168P/2F ('switching back to custom works', 'recents: Messaging NOT in Recents') + 3 NATIVE CRASH (GrallocUploadTh EGL, tj-target-scan ziparchive, main mallopt); #573 pt hung, cancelled; #574 rg GREEN

## Do this next
read bt INFO lines of #569 job 131521356338 + pt RESULTS job 131521431600 (haiku subagent); suspect F1 shadeLevel 'Notification shade.' = OPEN when collapsed & not KEYGUARD (pane title default) -> patch away; then solo reruns bt / se,ss / cs / base,legacy,x on head AND tag v0.9.7. Heartbeat trig_01JYEUXs7JosHyg85f2T1oCK

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  b51fe2b4 ckpt 1249: gate: #569-#572 COMPLETED (verdicts being read by a haiku subagent); #573 pt
  aa965083 ckpt 1248: gate: #574 rg GREEN; #573 pt (id 43315242706, job 131521431600) still in_pro
  f9e64bea ckpt 1247: HANDOFF 0v written (v0.9.8 gate state, run ids, ship steps)
  cb9db1ab ckpt 1246: gate #574 id 43315248372 (rg) GREEN (success, 21:57-22:01Z)
  34ec2ea2 ckpt 1245: Tj answer drafted: research/V098_RELEASE_MESSAGE_DRAFT_2026-11-12.md (facts 
  cb28da0c ckpt 1244: GATE dispatched 21:57Z on 72f529a1 (branch): #569 id 43315221820 r1 b6..fl; 
  72f529a1 ckpt 1243: review fixes: suite 1036/0, lint OK; self-review of the delta OK (no showWhe
  43435134 ckpt 1242: BY1 review fixes coded: MED1 shade look at service connect; LOW2 wake skips 
  0ef5c7fc ckpt 1241: #568 id 43310928575 (fl, 6381a278): 18 PASS 1 FAIL (FL2 menuW1 flashA 2 = 33
  ea8d0d3b ckpt 1240: lock reviewer DONE (BY1c diff): 1 MED + 5 LOW. MED1 onServiceConnected never
```
