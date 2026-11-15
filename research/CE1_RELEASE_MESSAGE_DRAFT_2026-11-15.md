# CE1 answer to Tj (DRAFT, 2026-11-15; check every sentence against the runs before sending; fill the <> parts)

The VPN line on the lock screen was my fault in the sense that the two apps were fighting: LinkMeter put its VPN back 2 seconds after the launcher turned it off, three times, then both gave up and the VPN stayed on. The launcher also never turned the VPN off in the custom launcher, which you said it must.

What changed (you must install BOTH updates on the tablet):

- Launcher <v0.9.11>: one rule for the VPN. In the lock screen (either launcher), with the screen off, in the custom launcher, and while a plain PIN is still being decided, a VPN is turned off. In an unlocked Quickstep session the launcher never touches it. The launcher also tells LinkMeter which of the two it is (a small setting called tj_launcher_world, needs the adb permission the launcher already has).
- LinkMeter <v0.2.184>: if you turned Fast Wi-Fi on, it stays on in Quickstep (nothing in LinkMeter or the launcher turns it off there). On the lock screen, with the screen off, or in the custom launcher it PAUSES instead of fighting: the choice is kept, the VPN stays down, no "Device connected" line. When you unlock into Quickstep it starts again by itself, normally within a few seconds. The card says "Fast Wi-Fi is paused", the tile says "Paused until Quickstep".

What is proven and what is not:
- <PROVEN: JVM tests for both rules; LinkMeter 32 screen scenarios and 33 deliberate breakages all caught; emulator runs #610 #611 #612 for the launcher half: <fill: PASS/FAIL counts and what the section vo showed>.>
- NOT proven (needs your tablet): that the lock screen really shows no VPN line, how fast Fast Wi-Fi returns after your PIN, and Samsung's lock screen reading. The emulator has no LinkMeter, so the two apps together were tested only by reading both codes against the same setting name.
- Limits: another VPN app that reconnects by itself still wins after three tries in ten minutes. In the custom launcher Fast Wi-Fi is paused (the tablet uses the normal hotspot speed there), as you asked.

Please check: (1) lock screen: no "Device connected to ..." line; (2) lock, then unlock into Quickstep: Fast Wi-Fi comes back within seconds; (3) LinkMeter > Settings > Fast Wi-Fi > "What changed it lately" shows "Paused (...)" and "came back by itself". If it does not come back, tell me what that list says.

Releases: https://github.com/tjshea90/Launcher/releases and https://github.com/tjshea90/LinkMeter/releases
