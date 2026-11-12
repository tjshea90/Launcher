# BY1 flicker films: section `fl` design (2026-11-12, Claude; Tj's request TASKS BY1)

Analyzer: `tools/fl_analyze.py` (written, `--selftest` OK, ffmpeg + stdlib). Boxes in display px from the learned panel
(`CARD area=... full=[X0,Y0][X1,Y1]`, card bottom CB = 4th number of `area=`):
- CORE = `X0+6,CB+6,X1-6,Y1-6`; RING = `X0-50,CB,X0-20,Y1;X0,Y1+45,X1,Y1+60` (outside the patch window: side margin ~11 px, bottom ~32 px at 170 dpi);
  TOP = `X0+10,Y0+10,X1-10,CB-10` (menu films only; NOT for films with the notification panel in them: a light QS panel over TOP would read as "menu").
- REF = `--ref last` (every film ends with >= 2 s of settled lock screen). flashA = core_d <= 6 and ring_d >= 15; flashB = the reverse; shot = TOP up >= 40 and CORE up >= 35.

Section `fl` (insert before `if want last;` in tools/emulator_scenario.sh), setup like section `ps` (lines ~2099-2160):
pm grant READ_EXTERNAL_STORAGE; `lock_screen_show_notifications 0`; rm `shared_prefs/tj_restore.xml`; pkill ShellServer; flfeat 0 (= psfeat WITHOUT
`patch_test_skew`, with `rm -f files/patchshot_r0.png files/patchshot_r1.png`); force-stop; setmode quickstep; rebind; home; SetupActivity
`--ez test_lock_wallpaper true`; sleep 6; setmode custom; rebind; home; `locksettings set-pin 1234`; lock+wake; logcat -c; long-press power (learn the
panel), BACK; parse CARD area; then calibration window (`patch_cal_until` = now+240 s via force-stop + sed), setmode custom, rebind, home, lock+wake,
wait for `patch_shot_status` CALIBRATED (fallback screencap + MEDIA_SCANNER broadcast), check FL0; then close the window (cal_until 0, force-stop,
setmode custom, rebind, home). Animations ON for the films (`flanim 1`), restored at the end (`FL_ANIM0`).
Helpers: `flrec <secs> [pre-sleep, default 1.4]` (rm /sdcard/fl.mp4, background `screenrecord --time-limit`), `flend <label> <flash|menu>` (wait for
`pidof screenrecord` to end, pull, analyse into `$OUT/fl-metrics.txt`, keep `PATCH` logcat lines in `$OUT/fl-log-<label>.txt`, `info` the FLMETRIC line,
delete the mp4 when analysed), `flget <label> <key>`, `flpath <label>` (counts of PATCH fast open / fast skip / pin hide / touch hide / rest on|off / visible again).
Unlock: wake, swipe 300 900 -> 300 300, `input text 1234`, ENTER, until `kg_state` = showing=false.
Films (logcat -c at the START of each case):
1. wakeH1 / wakeH2 (mode flash): unlock, home, flrec 10, POWER, sleep 3.5, WAKEUP, sleep 5 (the FIRST lock screen after the custom home).
2. wakeL: the same from the lock screen.
3. shade: flrec 10, `input swipe 300 2 300 700 300`, sleep 3.2 (F1: the patch returns over the OPEN panel at +1.5 s?), BACK, sleep 4.5.
4. shadeX (diagnostic, INFO only): `cmd statusbar expand-notifications`, sleep 3, BACK, sleep 4.
5. pad: swipe 300 900 -> 300 300, sleep 2.5, BACK, sleep 5.   6. tap: tap 150 700, sleep 3, tap 150 700, sleep 4.
7. menuL (mode menu): settled lock screen, flrec 8, long-press POWER, sleep 2.5, BACK, sleep 3.
8. menuW1 / menuW2: unlock, home, POWER, sleep 3.5, WAKEUP, flrec 8 0.3, long-press POWER ... (menu about 0.8-1 s after the wake).
9. menuS1 / menuS2: panel down (swipe), sleep 1.5, BACK, flrec 8 0.3, long-press POWER ... (menu about 0.8-1 s after the close).
10. menuP1 / menuP2: PIN pad up (swipe), sleep 1.5, BACK, flrec 8 0.3, long-press POWER ... (F3: inside padSignFresh's 1.5 s).
Checks: FL1 <film> flashA <= 1 and flashB <= 1 frame for wakeH1 wakeH2 wakeL shade pad tap; FL2 <film> menu >= 10 frames and shot == 0 for the 7 menu
films; INFO the path counts. Clean-up: flanim back, BACK, b6unlock (clears the PIN), lock notifications 1, logcat -c, setfeat, setmode custom,
force-stop, ensurehome, rebind, home.
Also: `tools/gate-map.txt` line `fl :: 280 :: opt :: PatchRest|fl_analyze :: patchResting|onOutsideTouch|touchAwayUntilMs|padBack|touchBack|settleRun|pinPadCameUp|padLeft|fastDecline|lockPrepare|restableSinceMs :: BY1 ...`;
`.github/workflows/emulator.yml`: add `fl` to the sections description (line 11, after `bt,mu`) and to the ffmpeg step's `if:` (pt / hf / fp).
First dispatch = BEFORE numbers on the current code (FAILs expected), `ref` main, sections `fl`, shots off.
