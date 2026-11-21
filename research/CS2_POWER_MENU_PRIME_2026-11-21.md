# CS2: preparing the power-menu patch when "Back to custom" is pressed (2026-11-21)

Tj, 2026-11-21 (verbatim in TASKS CS2): "when I press the back button in either the app drawer or notification quick settings tile, the launcher in addition to
what it already does should prepare as much as possible the power menu screenshot button cover for both the home screen and lock screen, so that even on the
first press of the side key, the button is hidden with no flicker at all. This may need the launcher to automatically open then close the power menu to prep it,
i don't know."

## What the first side-key press after a return pays for (read from the code and `BY1_FLICKER_FINDINGS_2026-11-12.md`, `CG1D_PATCH_LIFECYCLE_2026-11-17.md`)
1. SystemUI inflates and draws its power dialog for the first time in this boot / after the stock launcher process was restarted (SR1 restarts it after a Quickstep
   session). The first frames are the slowest; the dialog announces itself about when its first frame is drawn, so any reaction of ours is at least a frame late (BY1:
   menuW1 flash A 2-4 frames on a first opening, 0 on the second).
2. Our patch window's first draw with the OPEN look (`warmUp()` pre-creates the windows but the open look is drawn for real only when a menu opens: CG1D H4, "cold first draw").
3. Our service's fast path (`homeOpenDecide`, `patchShow(true)`, the fade `PatchFade`) runs for the first time in this process.
4. On the home screen the composite of the patch has to be fresh (`homeCompositeFresh`); after a return it is rebuilt when the home has been quiet for 250 ms
   (`scheduleHomeBuild`), and a menu that opens before that shows a stale patch (BT1c) or the full card.

## What CS2 does (all of it in the Launcher; `PowerMenuPrime` holds the rules, `TjAccessibilityService.primePowerMenu` the glue)
`ProfileSession.returnToCustom` (the tile and the drawer icon, with or without the Pocket-return switch) asks the service to prime. The service waits until everything
is quiet (custom home in front and at rest, unlocked, screen on, no calibration / measurement / patch build / reset cover / open menu, at most one run a minute), then:
1. raises the existing reset cover (the curtain = the picture of the home screen when one is in memory, else black) so nothing of the next second can be seen;
2. asks Android for the stock power menu (`GLOBAL_ACTION_POWER_DIALOG`, the same call the scrim measurement uses);
3. lets the normal event path run on it for 700 ms (the patch window draws its open look, the fast path and the fade run, SystemUI draws the dialog);
4. presses Back, only while the menu's window is verifiably there and the screen is still unlocked and plain, up to three times; ends in at most about 4 s;
5. removes the cover and asks the layers to look again (a calibration that waited for the prime gets its turn).
Every step writes one `PRIME` line into the lock log (the tablet report prints it) and the one-line result into Launcher control. Switch: "Prepare the power menu when you
go back to the custom launcher" (release default ON, debug OFF like every new behaviour; emulator section `pm` turns it on).

## What it does NOT do, on purpose
- **No automatic menu on the lock screen.** At the moment of the return the screen is unlocked; a menu opened later over a lit lock screen would be seen, and one opened
  over a dark one is untested on the tablet (wake side effects, the keyguard's own reaction). The lock screen is prepared the way it was: its patch window is created
  when the screen goes off (`lockPrepare`, at 0 / 250 / 700 / 6500 ms with Pocket return ON) and the stock dialog it will meet is already warm from this prime (the same
  SystemUI dialog class shows on the lock screen). The lock crop and the menu's real dimming are measured on a real lock screen (calibration / `ScrimMeasure`), which
  needs the keyguard showing: nothing can measure them from the home screen.
- It does not skip the stale-patch rules: a menu that opens before the composite is fresh still follows BT1c.

## Proven / reasoned
- Proven by JVM tests (`PowerMenuPrimeTest`): when it starts, when it never starts (switch, locked, screen off, automation off, a menu already open, a calibration running, less than a
  minute since the last run), that Back is sent only to a menu it opened and only over a plain screen, at most three times, and that a run always ends within the cover's failsafe.
- Proven by the emulator section `pm` (if its run is green; see TASKS CS2): the switch OFF opens nothing; ON writes the ordered lines, leaves no menu window and no cover behind, the
  custom home in front, a second return inside a minute does nothing, a real side-key menu still opens and closes afterwards.
- **Reasoned only (not provable on the CI emulator):** that the first side-key press on the tablet is flicker-free. The emulator's dialog is plain AOSP and its timing is not the
  tablet's (BY1 / CH1i: the film tests show a 2-4 frame residue on the first opening there on identical code). If a flash is still seen after v the tablet report's `PRIME` lines and the
  `HOMEPRESS menu opened (home)` line right after the first press say which of the four costs above is left.
