# Draft answer to Tj for CA1 (v0.9.9), written BEFORE the gate result; finalise after the release

Tj, v0.9.9 is out: a second performance pass (yesterday's v0.9.8 was the first).

<Release link here, plain text on its own line>

What I looked at: three separate reviewers read the unlock path (lock screen -> custom home), the steady CPU / battery use and the memory, using
the numbers from your last tablet report (2 hours: 69 MB total, about 1,800 tree searches on the main thread, the Enter tap reaching the launcher
224 ms late on a plain unlock and 2.2 s late on a Quickstep -> custom unlock). A fourth reviewer checked my changes for bugs (2 medium, 6 small
problems found, all fixed but one rare case that keeps today's plain black cover).

What changed (no function and no look removed):
1. Typing the PIN: each key used to start two whole-screen searches (power menu + pencil) on the thread that also draws the launcher and
   that SystemUI uses for the next key. A PIN key cannot open either, so they no longer run while the PIN pad is up (the power menu opening over
   the pad is still caught by its own window event). This is the main lag fix for "loading from the lock screen".
2. Memory: a 4.9 MB wallpaper copy (up to 12 MB with a separate lock photo) stayed in memory on the custom home screen; a 2.4 MB copy of the home
   screen stayed in memory in the custom launcher where nothing can show it. Both are now dropped there and read back only where needed.
3. Switching to Quickstep no longer rebuilds the whole custom app drawer in the background while Quickstep is starting (it is rebuilt when you
   come back, as before).
4. Battery / CPU: fewer system calls per event (a permission check that ran 4 times per tick is asked once every 3 s), status lines written in
   the background instead of with a disk flush on the main thread, the Solitaire lookup only when it matters, the pencil cover's 150 ms watch
   paused while the screen is dark, one panel check per lock instead of two, less garbage per log line.
5. The unlock summary now measures from your actual Enter TAP (it used to start when the launcher got round to it), so the numbers in the next
   report may look larger: they are the honest ones.
6. A new lock-log line ("LAUNCHER created") shows whether the custom home is rebuilt during the wake; that tells me whether the slow
   Quickstep -> custom unlock needs the next fix.

What was proven: the full unit suite (1041 tests) and the emulator gate <runs>. What only your tablet can show: how much faster the PIN entry and
the Quickstep -> custom unlock feel, and the memory number (Launcher control / tablet report: "total PSS").

Not done (each needs its own test first, details in research/PERF_AUDIT_2026-11-13.md): the Recents clean-up loop on the Quickstep -> custom
unlock, closing background apps after the cover instead of during it, moving the mode-switch profile changes off the main thread.

One question (your rule, not a bug): the lock screen always switches battery saver OFF (BA2). That also loses the saver's standby savings while
the tablet is locked. Do you want to keep that rule?

Still waiting on you: <the open list>.
