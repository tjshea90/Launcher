# v0.9.19 answer to Tj (draft, written BEFORE the release; every sentence below is checked against a run id or a test, 2026-11-23)

Tj, v0.9.19 is built (link below). It is the answer to your 2026-11-23 02:45 message about the Screenshot-button patch (CX1).

## What I did
I filmed the stock power menu frame by frame on the emulator (animations ON, the same method as the earlier lock-screen films) after many gestures: PIN pad pulled slowly, half way and back, held, flicked; notification panel half-pulled; quick settings; taps; screen off with the pad up; one long run of all of them in one lock cycle. I did the same on the custom home screen: after an app and Home, after the drawer, a page flip, the notification panel, screen off and on, a tap. A control run with the cover switched off shows that the film really sees the Screenshot entry when it is there (lock: GM3; home: the hm control).

## What the films showed
- **The patch never lights up on the emulator**, in any of the six lock-screen gestures (gm GM1: flashA = flashB = 0, run on v0.9.18's own code, #768, and again on the new code, #781). That does not prove your tablet: you see it light up, the emulator does not. Your tablet report after this version (below) is how I find it.
- **The Screenshot entry did show for a moment in a power menu opened right after a gesture:** on v0.9.18 for 167 ms (after a slow PIN-pad pull), 150 ms (after a flick) and 100 ms (after a tap), in the very first frames of the menu (run #768). Cause: the patch was still "away" (the 1.5 s finger / pad-left hold) when the stock dialog opened, and our side handled the dialog's event 82-115 ms late because the main thread was busy.
- **On the home screen the patch was often missing completely right after an app, the drawer or a page flip** ("no patch window yet" at 0.2 s and 0.8 s after Home, hm #769).

## What changed in v0.9.19
1. The home patch window now stays (hidden) while an app, the drawer, a folder, a drag or a page flip is in front, so the first power menu after coming back finds it already built (H1).
2. A menu scan that finds both menu texts but cannot locate the card no longer takes the patch away while the stock menu is open, and the patch window is not swapped under an open menu for a one-pixel wobble (H2).
3. After the notification panel closes the resting home patch waits 350 ms (the panel is still closing), so it does not flash with the panel (H3).
4. A second stock-dialog event before the Launcher has its focus back is a re-opening, not a shade (H5, hm #777: the card came 335 ms late).
5. The pencil's cover watches faster for 600 ms right after the screen turns on.

## Proven vs not
Proven on the emulator (run ids in TASKS CX1d / CX1h): items 1-4 each by their film section (gm, hm), `hf`, `hm`, `pt`, `ul`, `bt` green on the same code; `unit tests` for every pure rule.
**Not fully fixed:** on the emulator, in about one of two films of "PIN pad pulled, menu opened a moment later" the Screenshot entry still shows for 4-7 frames (67-117 ms), on the new code AND on v0.9.18 (fl menuP1, runs #784 head / #785 tag). The cause is the same: the dialog's event is handled 116-149 ms late in that film while our process and SystemUI compete for the emulator's CPU. The new code leaks less often in the gesture matrix (gm: 1 of 10 cases, was 3 of 10) but not never. I do not know whether your tablet has that delay.
**Not done:** the pencil's white square: only the faster watch above, no change of its colour on a dark panel (BK1b2 still needs your answer below).

## What I need from you on the tablet (after installing v0.9.19)
1. Pull the PIN pad up and down 5-10 times, slowly and fast, then open the power menu right after some of them. Then send the tablet report: the lock-log lines "resting patch hidden at the finger's DOWN ... ms after the touch event" and "patch visible again (touchBack)" give the real delay.
2. Battery saver ON (dark panel): does the pencil's white cover stand out?
3. Open the power menu 0.2 s after Home from an app and right after the notification panel closes: any Screenshot entry or light block?
