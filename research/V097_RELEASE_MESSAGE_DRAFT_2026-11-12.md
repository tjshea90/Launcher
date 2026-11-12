# Answer to Tj for v0.9.7 (TASKS BX1), sent as the last message of the session that shipped it

If `OUTBOX.md` has no entry starting "Tj, v0.9.7 is out", send this (link on its own line, plain text, no code block):

---

Tj, v0.9.7 is out. Quickstep now switches the keyboard to Gboard instead of SwiftKey. The custom launcher is untouched: it still gets the same stock keyboard as before.

Install it over the old one:
https://github.com/tjshea90/Launcher/releases/tag/v0.9.7

Two things to know:
1. Gboard has to be turned on in the tablet's own keyboard list (Settings > System > Languages & input > Virtual keyboard > Manage keyboards), because the switch only picks a keyboard that is already enabled. It applies the next time you go into Quickstep, or right away if you flip "Stock keyboard in custom, Gboard in Quickstep" off and on in Launcher control.
2. One small call I made: the custom launcher's Settings look-alike now leaves Gboard out of the "Virtual keyboard" line, the same way it already leaves SwiftKey out, so it does not read "Android keyboard (AOSP) and Gboard". Tell me if you would rather it listed Gboard.

Testing, kept light as you asked: the full unit-test suite and lint passed, and one short emulator run (74 checks, 0 failed) showed the custom launcher still gets exactly the stock keyboard it got before. The emulator has no Gboard, so the switch to Gboard itself is only proven by unit tests; the real check is on your tablet. I skipped the long lock-screen test set because nothing in that area changed. If Gboard does not open, the status line under that switch in Launcher control now says why and lists which keyboards are enabled; send me that line.

---

Facts behind each sentence: `KeyboardPolicy.target` / `pickGboard` / `noTargetStatus` + `KeyboardPolicyTest` (19 tests); `Mode.set` always calls `KeyboardSwitch.apply` (so "next time you go into Quickstep"); `ControlActivity` toggle calls `apply` for the current mode (so "right away"); emulator run #562 id 43137670443 (`base,legacy,x` on 16b47b65, 74 PASS 0 FAIL, INFO `custom: already com.android.inputmethod.latin/.LatinIME`); `ship.sh` SHIP_EXIT=0; release run android.yml #48 id 43139795817 on 30021942.
