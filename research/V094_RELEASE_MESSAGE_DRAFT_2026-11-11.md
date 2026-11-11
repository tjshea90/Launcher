# v0.9.4 release answer to Tj (DRAFT, written 2026-11-11 00:25 UTC before the gate result is known)

Why this file exists: if the session that ships v0.9.4 is cut off, the next one can send this answer as it is (after checking the facts marked CHECK
against the run and the Release). The answer goes out as the LAST message of the turn; the Release link stands alone on its own line (plain text, not a
code block). Re-list the open items at the end. OUTBOX.md must show an entry that starts with this answer (BP3b).

## The message (fill the CHECK items from the real runs)

Tj, both of your Wi-Fi / Bluetooth requests are in v0.9.4:

https://github.com/tjshea90/Launcher/releases/tag/v0.9.4

**Is it possible?** Yes. Android lets an app hide individual status-bar symbols (the same list the system itself uses). The launcher now keeps that list in step with where you are: in the custom launcher, and on the lock screen from any launcher (also while the screen is dark), the Wi-Fi and Bluetooth symbols are hidden. In an unlocked Quickstep session the list is put back exactly as it was, so Quickstep looks like stock.

**What changed**
- ONE new switch in Launcher control, "Keep Wi-Fi and Bluetooth on, hide their symbols" (ON by default). While it is on, the lock screen rule and the custom launcher's radio guard no longer switch Wi-Fi or Bluetooth OFF. Switch it off and everything is exactly as in v0.9.3 (radios off on the lock screen and in the custom launcher); switching it off in the custom launcher switches the radios off at once.
- "Only keep them on if they are already on. Don't turn them on automatically.": nothing in this feature turns a radio on. A radio that is off stays off; Wi-Fi OFF before the lock is still OFF after a gesture unlock and after a plain PIN (CHECK: emulator checks WF12a, WF12, WF12b).
- One exception that already existed before: if the lock screen comes up BEFORE the launcher's helper is running (right after a restart that began in Quickstep), the symbols cannot be hidden yet, so the radios are switched off like in v0.9.3, and a gesture unlock puts back what was on. Nothing is turned on that was off.
- The symbols never come back before the radio is off: if the helper stops while the lock screen is up or the screen is dark (a crash, a restart), the symbols stay hidden until the radios have been switched off (213 ms in the emulator run, read from the lock log), and only then does the list go back.
- It needs the adb permission the launcher already has; without it the old behaviour applies (radios off), because a visible symbol on the lock screen would be worse.

**What is proven and what is not**
- Wi-Fi: proven on the emulator with pictures (section wf, run #523, 38 of 38 checks): radio stays on across the lock, the Wi-Fi symbol (102 white pixels in its spot of the status bar) is gone in the custom launcher (0) and on the lock screen (the icon band has 370 white pixels without the hold and 268 with it, the missing 102 are the symbol), it is back in Quickstep (102), and the original list is back after the switch is turned off and after "Clear data".
- Bluetooth: the emulator has no Bluetooth hardware, so Bluetooth is proven only by unit tests and by reading Android's source. It uses the same mechanism (a slot named `bluetooth`).
- NOT proven: your tablet's own lock screen (MediaTek's SystemUI) may not obey the hide. A flash of the symbols at the very first moment after a restart is possible (the launcher starts a few seconds after the lock screen first draws).
- If you uninstall the launcher, switch this off first, otherwise the symbols stay hidden everywhere until the list is reset.

**What I need from you:** lock the tablet with Wi-Fi and Bluetooth on and tell me whether either symbol is still visible on the lock screen or in the custom launcher. If you still see one, send a tablet report right then.

## Still waiting on Tj (re-list at the end of the reply, unchanged)
- BR1c: does the settings gear in the custom notification panel open the look-alike?
- BN7a: start the shell helper and tap "Do everything that is missing" (usage access).
- BN8c / BN8d: tablet reports, one right after the first side-key press on the custom home, one before you open more Quickstep apps.
- BQ1b: a tablet report right after any lock-screen screenshot or white box.
- BH1c: where you saw a pop-up notification, and which app.
- BK1e: which unlock is slow, what is on screen, how many seconds.
- NEW: the Wi-Fi / Bluetooth symbol check above.
