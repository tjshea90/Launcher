# Draft answer to Tj for CB1 / CB2 / CB2e (written BEFORE the gate; check every sentence against the runs before sending)

Tj, v0.9.10 is out: the launcher now turns your VPN off whenever the screen goes off or the lock screen is up, in both launchers.

<Release link here, plain text on its own line>

Why not remove the row: "Device connected to ..." is Android's own quick-settings row (SystemUI's security footer). It appears whenever any VPN
is connected. Android 10 has no setting for it, and the only way to change SystemUI's layout (an overlay) must be signed by the tablet maker.
Painting over it would leave an empty strip, and the row would flash while the panel moves.

What it does now:
1. The moment the screen goes off (custom launcher or Quickstep), or whenever the lock screen is showing, a connected VPN is turned off. With no VPN,
   the row and the key are gone from the panel.
2. If your VPN app reconnects by itself while the tablet is dark or locked, it is turned off again (at most 3 times in 10 minutes, so the two
   apps can't fight all night).
3. Nothing turns the VPN back on: after unlocking, connect again in your VPN app. While the tablet is locked, its traffic goes without the VPN.
4. Unlocked, the VPN is yours: the launcher never touches it.

One step for you, once: Launcher control > Lock screen > "Allow the launcher to turn the VPN off". Android asks its own "Connection request"
question; tap OK. That wording is the same for any app that uses Android's VPN service; the launcher never sets up a VPN of its own. Tapping OK
also disconnects the VPN once. The switch above that row is on by default.

It cannot work if your VPN is set as "Always-on VPN" in Android's settings (Settings > Network & internet > VPN > gear): Android does not let any
other app turn that one off. Launcher control then says so.

What was proven: <gate sentence: runs, sections, VO checks>. The emulator used a real test VPN: the row appeared, and the screen going off turned it off.
What only your tablet can show: your VPN app's behaviour (some apps reconnect or warn when another app takes the VPN over).

Still waiting on you: <the open list>.
