# Draft of the answer to Tj for v0.9.5 (BT1a-f), written BEFORE the release so a cut-off session can send it

Rules (CLAUDE.md "Messages from Tj", TASKS BP3b): the answer is the LAST message of the turn and is also appended to OUTBOX.md. Fill in the three markers (`<<RELEASE>>`, `<<GATE>>`, `<<REVIEW>>`) from the real results; delete any sentence the results do not support. Do not claim a tablet result: none exists.

---

Tj, all six of your reports are in v0.9.5:

<<RELEASE>>

**1) First pull-up to the PIN pad shows the power-menu patch.** I could not see it on the emulator, so this one is a reasoned fix and I want your check. The cover that hides the Screenshot entry is only allowed to be visible when the stock power menu is open. Three ways it could be visible at the first pull-up are closed: a cover window that appears late (right after the screen comes on or a mode switch) now starts hidden and only shows after it has been ready for 1.2 s; your finger going down on the screen is now noted even if the cover was not yet showing, so the cover stays away for the pull-up; and the cover stays away for 1.5 s after the PIN pad leaves.

**2) The power menu flickers on and off after the first unlock.** Same family, same status. I checked every path in the unlock and nothing opens the power menu or the replacement panel by itself. The most likely cause is the lock-screen cover coming back the moment the PIN pad leaves, while the lock screen is still sliding away. That is the 1.5 s hold above. If it still happens, send a tablet report right then: the report now carries a line for every time the cover came back, with the reason.

**3) After the notification panel, the first side-key press shows the full power menu with the Screenshot button.** Reproduced on the emulator, two causes. When the home cover was "not ready" the replacement panel only covered Power off and Restart, so the Screenshot entry showed. And closing the notification panel starts a 2.5 s "another window is up" guard; on the emulator it started about 190 ms before the power menu's own event, which is exactly the "first time, then fine" you saw. Now the cover is shown as it is (and rebuilt in place 40 ms later) whenever the launcher is in front, the guard ends when the launcher has the focus again, and if nothing can be trusted the replacement panel covers the whole menu, never half of it. Emulator: new test section `bt`, the Screenshot entry stays covered at every gap after closing the panel, and the cover is ready again 0.5 s after the panel closes. Your guess that it comes from the pencil changes was right in spirit: the pencil cover's window event is one of the events that started the guard.

**4) Sometimes the home wallpaper cover doesn't work.** Same fix as 3, same reasons ("not ready" at the press). Every press on the home screen now writes one `HOMEPRESS` line saying which treatment ran (fresh, shown as it is, or full panel) and why, and it is in the tablet report. If it still misses sometimes, that line names the reason.

**5) "and Microsoft SwiftKey keyboard" under Virtual keyboard.** Gone, whether or not SwiftKey is installed or enabled; the list only names other keyboards. If SwiftKey is the default keyboard the line names the first other enabled one. Proven by unit tests; the emulator has no SwiftKey, so it cannot be seen there.

**6) Recents name "Launcher settings".** Now "Settings". Proven on the emulator (the Recents card reads "Settings"). Side effect to know about: Android takes the Recents title from the app's own name, so the app is now called "Settings" everywhere Android shows the app name: Settings > Apps, Battery and Special access (next to the real Settings, so you will see two rows), crash dialogs ("Settings keeps stopping") and the subtitle of the "Back to custom" tile. The home activity itself is still labelled "Launcher"; I did not check what the "Select a Home app" list shows. Tell me if you want any of those to say "Launcher" and I will find another way.

**What is proven and what is not**
- <<GATE>>
- <<REVIEW>>
- Proven only by reasoning plus new log lines: 1 and 2. Proven on the emulator: 3, 4, 6. Proven by unit tests: 5.

**What I need from you:** use the tablet as usual. If the cover shows at the first pull-up, or the menu flickers after an unlock, or the Screenshot entry shows at a side-key press, send a tablet report right then.

**Still waiting on you** (unchanged):
- BR1c: does the settings gear in the custom notification panel open the look-alike?
- BN7a: start the shell helper and tap "Do everything that is missing".
- BN8c / BN8d: tablet reports, one right after the first side-key press on the custom home and one before you open more stock-launcher apps.
- BQ1b: a tablet report right after any lock-screen screenshot or white box.
- BH1c: where you saw a pop-up notification, and from which app.
- BK1e: which unlock is slow, what is on screen, and how many seconds.
- BM1: the account plugin `session-handoff`.
- BS2e: the Wi-Fi / Bluetooth symbol check from v0.9.4 (lock the tablet with both on; is either symbol visible on the lock screen or in the custom launcher?).
