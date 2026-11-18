# CH1 — the answer to Tj (draft, written BEFORE the release; check every sentence against the code and the run list, then fill the links)

Tj, your pocket-return request and the keyboard choice are built.

**Launcher: <RELEASE LINK, plain text, own line>**
**LinkMeter: <RELEASE LINK, plain text, own line>** (install BOTH: the Launcher asks LinkMeter, and only the new LinkMeter answers)

## What you get (Launcher control, both default as before)
1. **"Pocket return (tile and icon)"**, OFF by default. OFF = the launcher behaves exactly as it did (the lock preparation still runs at 0.7 s and 6.5 s, nothing else is new).
   ON, and you return with the **"Back to custom" tile or icon** (not double Home, not a plain PIN, not the key codes):
   - LinkMeter is asked (beside the return, nothing waits for it) to put the phone to rest: phone Wi-Fi off, LinkMeter's saver on, hotspot off + airplane mode on + screen off (the existing "deep sleep"), then Android's own Battery saver, then the screen off again if it is not off.
   - The tablet's Wi-Fi goes off as soon as the phone has accepted its order (at the latest 25 s after the start; at once if LinkMeter does not answer).
   - The VPN is looked at at once, the stock keyboard is set again, every open app is force-stopped except LinkMeter, Gboard / SwiftKey / Swoop, system apps and the Google infrastructure apps, notifications and Recents are cleared, the custom launcher comes up (no stand-in app), and the patch / pencil layers and the Settings look-alike are refreshed.
   - At the **side key** the lock preparation starts at once (0 ms and 250 ms, then the old 0.7 s and 6.5 s): patch window, pencil cover, number, notifications closed (apps with a notification are force-stopped, but NOT while music plays), Wi-Fi / Bluetooth symbols hidden, no VPN.
2. **"Keyboard in Quickstep"**: Gboard (as before, still the default) / Stock Android keyboard / Swoop (package `app.swoop.keyboard`). The custom launcher always keeps the stock keyboard. In the Settings look-alike Swoop is hidden only when it is the Quickstep choice.

## MEASURED (emulator, CI) and tested
- Unit tests: 1167, 0 failures (the switch OFF is never "active", only the tile / icon count, which apps may be stopped, the Wi-Fi-off timing, the bridge verdicts, the keyboard choice, LinkMeter's own PocketRulesTest 35,580 checks).
- Section `pk` (run #658): OFF = no pocket line at all and the old single lock preparation; ON via the tile = the steps in order in the lock log, the tablet's Wi-Fi really off, no stand-in app, double Home NOT a pocket return, the lock preparation starts at the key press.
- The full gate (the runs named in BUILDLOG).

## NOT seen (only reasoned, or untestable here): please check on the tablet and phone
- The phone half: the real phone going to sleep through LinkMeter (LinkMeter's tests use a simulated phone). The CI emulator has no LinkMeter, so there the log says "LinkMeter did not answer ... phone steps are skipped".
- That the Launcher's call really reaches LinkMeter's door on the tablet (the signing-key check is proven by tests, not by a device).
- Switching to Gboard / Swoop (the CI image has neither). Swoop must be enabled in Settings, Languages and input.
- Whether the preload makes the next lock / wake faster: unmeasured.

## Things to know
- The phone ends in airplane mode with the screen off; "Hotspot on" on the tablet wakes it over Bluetooth, as before. Bluetooth on the tablet is left on while the phone goes to sleep.
- The pocket step "Android's Battery saver" stores LinkMeter's option "Also switch on Android's Battery saver" as ON until you press "Leave alone".
- Apps force-stopped by the side key or the return stay "stopped" (no pushes) until you open them once.
