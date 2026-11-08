# Tablet checklist for v0.8.12 (written 2026-11-08 night; NOTHING in this release has run on the real tablet yet, only on the CI emulator)

What changed since v0.8.11 and what to do. Install: Release page, `Launcher-v0.8.12.apk`, install over the old one (same key: permissions, Home choice, accessibility stay). The adb helper does not survive an install. The accessibility service restarts by itself after the update; if Android asks again, switch it on in Accessibility settings.

## What changed (your message of 2026-11-08 21:11 UTC: "Make the home looking curtain instead of the black screen. An identical screenshot of the current home screen")
1. **The black screen during the unlock clean-up is now a picture of your home screen.** After a Quickstep session, with the plain PIN and no adb helper, the app clears Recents and the notifications behind a cover for about 1 to 4 seconds. That cover used to be black. Now it shows your real home screen: your own wallpaper (a real screenshot, taken by the home calibration) with the home screen's own icons laid over it, made by the Launcher itself, and re-made by itself whenever the home changes (NO flash for that). On the emulator the picture matches the real home to 0.09 % (a screenshot taken while the curtain was up, compared with the bare home).
2. **ONE extra screenshot flash after the update, once per home page you use.** The picture needs your whole plain wallpaper, which the app used to throw away after cutting a small crop. So the next time the custom home is quiet (no finger, a few seconds after the screen came on) the home goes blank for about a second and the screen flashes ONCE, exactly like the first calibration in v0.8.9. It happens by itself. Until then (and whenever there is no picture, another rotation, a new wallpaper) you get the black cover as before: it is never worse than v0.8.11.
3. **Taps on the picture are swallowed, but only once the PIN is accepted.** Before that the picture does not block touches, so a wrong PIN can be retyped at once. (The old black cover never blocked touches at all; tapping a picture of an icon must not hit Recents underneath.)

## Do this
**A. Install and let it settle.** Use the custom home for a minute. Wait for the ONE flash (the home goes blank about a second). Then open Launcher control (from Quickstep) and read the line "Home curtain (BL1)": it should say "ready: a picture of your home screen taken N min ago". If it says why not, send me that line.
**B. The real test.** Open any app from the STOCK launcher (Quickstep), lock the tablet, wake it, and unlock with the plain PIN. Watch the 1 to 4 seconds after the last PIN tap: it should look like your home screen the whole time (no black, no Wi-Fi, no Quickstep apps, no notifications). Does the end (the cover going away) show any jump?
**C. Things that WILL differ (known, tell me if they bother you):** (1) the status bar row at the very top (clock, battery, icons) is BLANK in the picture and appears when it ends; a frozen clock would have been wrong. (2) If you moved icons while the app was not running, or the home comes back on another page than the picture shows, the picture can jump at the end.
**D. Tap on the picture** right after the PIN (on an icon): nothing should happen, and the app must not open.
**E. A WRONG PIN:** you can retype at once, nothing blocks the pad.

## Send back
* The tablet report: the lines `Home curtain (BL1)` (also in Launcher control), `home_curtain_state` (what the cover was last: the curtain, or why black), `home_curtain_capture` (the last picture capture), and `Last unlock, start to finish (BK1c)`.
* One word for B: identical / slightly different (what) / black / something else. A photo of anything that differs (view-only for me, it never goes into the repo).
* The answer to the open question from v0.8.11: which unlock was slow, what you saw, about how many seconds.

## If something is wrong
* Launcher control > "Home-looking curtain instead of the black cover" OFF puts back the black cover at once. "Calibrate the home patch again" forgets the pictures (the next quiet moment takes them again, one flash). "Restore everything" resets all switches.
* Nothing else changed: the lock screen, the power-menu patches, the drawer, the Recents and notification rules of v0.8.11 are as they were.
* Details and every number: `research/BL1_HOME_CURTAIN_2026-11-08.md`.
