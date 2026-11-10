# Draft of the answer to Tj for v0.9.3 (TASKS BR1 / BR2; written 2026-11-10 20:10 UTC BEFORE the release, so a cut-off session can send it)

Send ONE message (OUTBOX.md must show it afterwards; a later session checks for an entry starting 'Tj, v0.9.3 is out'). Put the Release link alone on its own line, as plain text.

---

Tj, v0.9.3 is out. The gear in the notification panel now opens the Settings look-alike.

https://github.com/tjshea90/Launcher/releases/tag/v0.9.3

**1. The gear.** In the custom launcher, with the tablet unlocked and the panel open, tapping the gear closes the panel and opens the look-alike Settings (Battery, Display, Sound, Storage, Accessibility, System, About tablet) instead of the real one. It works through an invisible touch-catcher over the gear that exists only while the pencil cover is up, so it is never over a moving panel. New switch in Launcher control, "Gear opens the Settings look-alike" (on); it needs the pencil cover and the Settings look-alike switches to be on.

What I tested, as you asked without the long gate: the unit tests, and one short emulator run of just this feature: the tap opened the look-alike, the panel closed, and with the switch off the same tap opened the real Settings. It has NOT run on your tablet yet.

Two things to know: (a) a tap on the gear within about half a second after you pull the panel down or touch it still reaches the real gear, because the cover keeps out of the way while the panel may be moving; (b) the lock-screen panel keeps the real gear: it asks for your PIN first, and opening the look-alike over a locked screen would skip that. Tell me if either one bothers you.

**2. Removing the pencil instead of covering it: I found no safe way.**
- The only switch that really removes it is Android's retail-demo flag. It removes the pencil AND the gear (so it would undo what you just asked for), and I retired it on 2026-11-05: that flag can make Android create a demo user at boot, nobody has tested a reboot with it, and the way out can be a factory reset. I could not read that service's Android source today (the page was missing), so that stays the old audit.
- A resource overlay installed with adb could in theory replace the panel's footer layout. It is a separate APK built against SystemUI, and a mistake crashes the status bar and lock screen. I did not build it; I can try it on the emulator first if you want.
- Accessibility can only cover or click another app's icon, never remove it. So the cover stays, and the v0.9.2 fix for its white box is still waiting for your tablet report.
Notes: research/PENCIL_REMOVAL_2026-11-10.md.

Still waiting on you, unchanged: a tablet report right after the first side-key press on the custom home and one before you open more Quickstep apps; a report right after any lock-screen screenshot or white box; start the shell helper and tap "Do everything that is missing" (usage access); two answers, where you saw a notification (and which app) and which unlock is slow (what is on screen, about how many seconds). New: tell me if the gear ever opens the real Settings when it should not.
