# Draft answer to Tj for CB1 / CB2 / CB2e (written BEFORE the gate; check every sentence against the runs before sending)

Tj, v0.9.10 is out: the launcher now turns your VPN off whenever the screen goes off or the lock screen is up, in both launchers.

https://github.com/tjshea90/Launcher/releases/tag/v0.9.10

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

It cannot work if your VPN is set as "Always-on VPN" in Android's settings (Settings > Network & internet > Advanced > VPN > the gear next to your VPN): Android does not let any
other app turn that one off. Launcher control then says so.

What was proven: 1051 unit tests and lint; the new emulator test with a real VPN, 24 of 24 checks (#595, again in the gate run #598); the release gate (14 sections in 3 runs, #596-#598; 4 failed checks in two long runs passed when run alone, on this version AND on v0.9.9, #599-#602). The emulator used a real test VPN: the row appeared, and the screen going off turned it off.
What only your tablet can show: your VPN app's behaviour (some apps reconnect or warn when another app takes the VPN over).

Still waiting on you: the Allow step and a lock test with your VPN (CB2g), the v0.9.9 tablet check and the battery-saver question (CA1d), the v0.9.8 flicker checks (BY1g), the Gboard check (BX1e), the Eleven questions and tablet check (BV1i/j/k), and the older tablet checks (BT1h, BS2e, BR1c, BN7a, BN8c/d, BQ1b, BH1c, BK1e, BM1).
