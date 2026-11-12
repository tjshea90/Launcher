# v0.9.8 answer to Tj (DRAFT, 2026-11-12; check every sentence against the gate runs #569-#574 before sending)

Answers BY1 (Tj 17:26 UTC: flicker of the Screenshot cover + full speed / battery / memory review) and BZ1 (Tj 21:11 UTC: the VPN key).

**The Screenshot cover (flicker)**
- Notification panel on the lock screen: the cover came back about 1.5 s after your finger, ON TOP of the still open panel (a block of
  lock-screen wallpaper over the panel). Now it stays away while the panel is open and comes back 0.4 s after the panel has closed, also when
  the panel was left open and the screen went off.
- PIN pad: after the pad was put away (Back) without unlocking, the cover waited 2.5 s; a power menu opened in that time showed the Screenshot
  entry for a few frames. Now the cover is back 0.7 s after the pad went away.
- The first lock screen after waking: the launcher was busy searching the screen's contents at exactly that moment (your last tablet report: up
  to 1.3 s blocked at a time). Those searches are cut now (see below). On the emulator the launcher now reacts to the opening power menu within
  2-3 ms; a lag of 2-4 frames (33-67 ms) at the very start of the fade remains on the FIRST menu after a wake, and it comes from how the stock menu
  announces itself, not from the launcher.
- How it was tested: a new emulator test films the screen frame by frame (the first lock screen after waking, the panel pulled down slowly and
  fast, the PIN pad, taps, and the power menu after each of them) and measures the cover in every frame.

**Speed, battery, memory** (full review, nothing removed or slowed)
- The biggest cause of lag: on many screen events the launcher searched the contents of other apps' windows and SystemUI's whole screen. It now
  skips every window that cannot hold the power menu or the panel.
- Fewer system calls while nothing of the launcher is on screen, no disk write at every Home press, the Recents app-name list built in the
  background, a 5 MB copy of the wallpaper released after use.

**VPN key**: hidden by the same rule as the Wi-Fi and Bluetooth symbols: hidden on every lock screen and in the custom launcher, shown only in
unlocked Quickstep. The VPN itself is never switched on or off. Launcher control's switch now reads "Keep Wi-Fi and Bluetooth on, hide their
symbols and the VPN key".

**Questions**
1. Do you ever open the power menu while the PIN pad or the notification panel is still open? Then the cover cannot match the screen behind it
   (today: the Screenshot entry shows over the PIN pad). If you do, which is better: the Screenshot entry, or a wallpaper-coloured block over it?
2. Does your lock screen have a camera shortcut or an emergency-call button? (If yes, I will make sure the cover never shows over those apps.)

**Please check on the tablet**
- With the VPN on: no key in the status bar, on the lock screen and in the panel in the custom launcher; the key is back in unlocked Quickstep.
- Wake the tablet and hold the power key at once: does the cover still flash? Pull the panel down and up, open the PIN pad and put it away, then
  hold the power key. If anything flashes, send the tablet report right after it.

(Release link on its own line at the end.)
