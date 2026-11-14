# CD1 emulator experiment: does OUR setup inflate the stock launcher? (2026-11-14)

Run 43549429509 (#603), job 132308199323, head f1283967, section `cd1` (research, not in the gate), 401 s, 7 PASS / 0 FAIL / 17 INFO.
Question (TASKS CD1 / CD1i): Tj's tablet shows the STOCK launcher (`com.android.launcher3`, "Quickstep") at about 400 MB; a friend's identical tablet shows 48 MB (there the stock launcher is the real home). On Tj's tablet our `RouterActivity` is the only HOME in both modes, so the stock launcher only hosts Recents and the Quickstep session. Does that setup make the stock process big?

Method (tools/emulator_scenario.sh, section `cd1`): the stock process is killed before each phase, three apps are started, then Recents is opened and closed (Recents key, Home) 5 + 10 times, then (B, C) a Quickstep session is started and ended (double Home) 3 times. `dumpsys meminfo com.android.launcher3` after each step.
A = stock launcher is the REAL home, our accessibility service off, our app stopped. B = our router is the home, no helper. C = B plus the shell helper.

## Raw numbers (PSS in KB, App Summary; Objects; RSS from /proc status)

| step | TOTAL | Java | Native | System | Graphics | Views | ViewRootImpl | Activities | RSS |
| :- | -: | -: | -: | -: | -: | -: | -: | -: | -: |
| a0 | 38823 | 4004 | 21356 | 9851 | 0 | 97 | 1 | 1 | 144252 |
| a5 | 43091 | 4324 | 23340 | 11679 | 0 | 97 | 1 | 1 | 144668 |
| a15 | 40631 | 4296 | 22700 | 9963 | 0 | 97 | 1 | 1 | 145692 |
| b0 | 15120 | 1296 | 6352 | 5136 | 0 | 0 | 0 | 0 | 104420 |
| b5 | 27260 | 2652 | 13848 | 7864 | 0 | 40 | 1 | 1 | 123984 |
| b15 | 24823 | 3568 | 11160 | 7183 | 0 | 40 | 1 | 1 | 124624 |
| b15r3 | 39250 | 6048 | 18332 | 11098 | 0 | 137 | 2 | 2 | 144096 |
| c0 | 18602 | 1304 | 9608 | 5346 | 0 | 0 | 0 | 0 | 108740 |
| c5 | 27837 | 2716 | 13512 | 8697 | 0 | 49 | 1 | 1 | 127420 |
| c15 | 26041 | 2956 | 12684 | 7361 | 0 | 49 | 1 | 1 | 128144 |
| c15r3 | 34841 | 4208 | 15696 | 11189 | 0 | 146 | 2 | 2 | 142144 |

Display of the CI emulator: physical 320x640 / 160 dpi, override 600x1024 / 170 dpi. Stock home component: `com.android.launcher3/.Launcher` (Q Quickstep keeps the old name). Helper phase C: `INVISIBLE: force-stopped 11 of 11 apps`; phase B: the app-only path (`RESET recents result=3 +2.2 s`, cover shown / removed).

## Reading

- The stock process is SMALL in every phase: 15-43 MB PSS. No growth with the 15 Recents cycles (A 43 -> 41, B 27 -> 25, C 28 -> 26 MB), no leak signature (Activities 0-2, Views <= 146, ViewRootImpl <= 2).
- A (stock launcher as the REAL home) is not smaller than B / C: with our router as the only home the stock process is the same size or smaller. **Our setup does not inflate the stock launcher on the emulator.** The +12-15 MB after the three Quickstep sessions is the stock Launcher activity being created (Activities 2, Views 137-146).
- So the ~400 MB on Tj's tablet is NOT produced by the flows tested here (Recents open / close, Quickstep session start / end, helper on / off).

## What this does NOT cover (say so in every answer to Tj)

1. The emulator image has no MediaTek Go build (`MtkLauncher3GoIconRecents`): a different Recents (`IconRecentsView`) and MediaTek changes.
2. Software GL: the **Graphics row is 0**, so GPU / gralloc memory cannot be measured here. On the tablet that row may be the big one.
3. 15 Recents cycles, not months of uptime; no dark-mode flips (`cmd uimode night` does nothing on this image, so the BA2 battery-saver theme flips cannot be reproduced), no real notifications, no wallpaper changes, a handful of apps.
4. 3 sessions of the return reset, not the hundreds a tablet sees.

## Next (TASKS CD1e / CD1k)

The tablet dump decides: `dumpsys meminfo com.android.launcher3 | head -62` (App Summary incl. Graphics, Activities / Views) and `dumpsys procstats --hours 24 com.android.launcher3 | head -40`. The probe lines for the next tablet report are already in `app/src/main/resources/probe-commands.txt` (CD1c) and the intake flags >= 250 MB (`tools/tablet_intake.py`); they need a helper started from the NEW APK, so the cable dump does not wait for a release.
