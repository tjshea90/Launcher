# Tablet checklist for v0.8.13 (written 2026-11-09; NOTHING in this release has run on the real tablet yet, only JVM tests and the CI emulator)

What changed since v0.8.12 and what to do. Install: Release page, `Launcher-v0.8.13.apk`, install over the old one (same key: permissions, Home choice, accessibility stay). The adb helper does not survive an install. The accessibility service restarts by itself.

## What changed (your messages of 2026-11-09 02:33 and 03:02 UTC, with the tablet report)
1. **First side-key press on the custom home (BN2).** The report showed why: every plain unlock makes the status-bar window announce itself, and the app took that for "the notification panel is open" and kept the patch away for 2.5 seconds. Your first press came 1.2 s after the PIN, inside that time, so the stock Screenshot entry showed. Now that window starts the same 2.5 s guard only PROVISIONALLY: as soon as the home screen has the focus again (about 0.4 s later, or at once when the focus comes back) the guard is lifted and the patch comes back.
2. **The tall power menu that flashed after the Home key (BN2).** After the Home key the stock menu fades out for about 0.25 s but its window stays in the list; the app saw it 143 ms later, took it for a NEW menu and drew the old tall card over it. A menu seen within 0.45 s after a close we were told about is now ignored (a really new menu announces itself and is believed at once).
3. **The white box on the lock screen after the notification panel slides up (BN4).** The cover over the pencil is an opaque white box; it followed the pencil and was removed only after a slow whole-screen search. Losing the pencil now removes the cover AT ONCE, and a 150 ms check runs while the cover is up.
4. **The lock-screen patch that flashes while you pull up the PIN pad (BN3).** The cause is NOT proven (the report's log showed no flash). Changes: after a finger went down the patch stays away 1.5 s instead of 0.9 s (a finger that rests a moment before pulling no longer brings it back mid-pull), the return looks at the PIN pad once more first, and every return is now written to the lock log with the path that did it, so the next report names the culprit.

## Do this
**A. Install and use it for a minute.**
**B. The first press.** Lock the tablet, wake it, unlock with the plain PIN, and press the side key about 1.5 to 2 seconds after the home screen appears. The Screenshot entry must NOT show. Do it 4 or 5 times, also once at the second the home appears (under 1 s: this can still show it, it is a known limit, tell me how it looks).
**C. The Home key.** Open the power menu on the home screen, press Home: no tall card flashes.
**D. The pencil.** On the lock screen pull the notification panel down (quick settings), slide it back up: no white box.
**E. The PIN pad.** Pull the PIN pad up 10 times, some with a quick swipe, some with the finger resting a moment first: does the patch flash? Each time it does, send the report within a minute.

## Send back
* The tablet report right after anything wrong (a flash, a first-press leak, the white box). Lines to read: `Home-screen patch ... unlock window events ignored N`, the lock log lines `PATCH visible again (touchBack|padBack|restRecheck|other)` and `PENCIL cover hidden N ms after the pencil was last seen`.
* A word for B: covered every time / not covered N of M times (how soon after the home appeared).

## If something is wrong
* Launcher control > "Restore everything" resets all switches; the older behaviours are in v0.8.12 (Releases) if you need to go back.
* Details and every cause: `research/BN2_BN3_BN4_DESIGN_2026-11-09.md`; the code review: `research/BN2_BN3_BN4_REVIEW_2026-11-09.md`.
