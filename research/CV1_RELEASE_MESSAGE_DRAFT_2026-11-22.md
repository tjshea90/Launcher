# The answer to Tj for the one-launcher release (TASKS CV1; written 2026-11-22 BEFORE the release exists, claims re-checked against run #734 / #732 / #721 output)

**Before sending:** (1) the Release exists (`get_release_by_tag`) and is recorded (BUILDLOG); (2) replace `<VERSION>`, `<LINK>` (alone on its own line, plain text), `<GATE>` (run ids + PASS/FAIL counts from the newest ckpt, and the honest list of failures that were reruns); (3) check EVERY sentence against the run list in the newest ckpt, not memory (BT lesson). Everything below the line is the message.

**What the runs did NOT prove (do not claim it):** UL10 ("an icon added in the unlocked home is not in the custom layout") was VACUOUS on the CI emulator: the image never sent the install broadcast, the scenario printed `rows ... in launcher_unlocked.db: 0`. "Restore everything switches the new launcher off" is set in code (RestoreStore) and was not run. The layout copy was proven with six rows INJECTED by the test, not with Tj's real home. The failsafe of three quick Home presses never triggered on the emulator (the rule is a unit test).

---

v0.9.17 is released: the unlocked home is now THIS launcher, so there is one launcher instead of two.

https://github.com/tjshea90/Launcher/releases/tag/v0.9.17

**What changed**
- After the secret handshake you land in this launcher in its unlocked form: the drawer has every app, with Launcher control and Back to custom in it, nothing hidden. Locked down it is the custom launcher as before. Double Home, the lock and a plain PIN work as before.
- The old launcher stays installed for two jobs only: it is Android's own Recents screen (the Recents key is unchanged), and it is the emergency exit: three crashes in two minutes put it back as the unlocked home.
- One switch in Launcher control, "One launcher for both worlds": on = this release, off = the old two-launcher behaviour. "Restore everything" should switch it off too (that part is written in the code but I did not run it).

**One difference you will see:** the unlocked home has its own icon layout and it starts as a COPY of the custom home's layout (widgets and pinned shortcuts are not copied), not as your old stock arrangement (the folders "Connect" and "Games"): the stock launcher keeps that private and the app cannot read it. You arrange it once; after that it keeps its own layout.

**What the emulator showed** (runs #721 and #734 for the new tests, #735 for the whole lock-screen set with the switch on): in the unlocked world our launcher is in front and loads its own layout file; the unlocked drawer shows Contacts, Launcher control and Back to custom; a double Home, also pressed from inside an app (three tries), brings back the custom mode and the custom drawer hides them again; the layout copy was checked row by row on a test layout (an app, a folder with two apps, nothing from widgets or pinned shortcuts, ids in a separate range); the Recents key still opens Android's own Recents; Launcher control opens; a tripped crash breaker brings the stock launcher back, a double Home ends it, and the switch off gives the old behaviour; the lock-screen tests ran with the switch on. **Honest note about the tests:** the lock-screen set was red once (six checks: the lock-time clean-up and the battery-saver return) and I could not make it fail again in five further runs of the same code, the last of which, the full set in one go, was <FULL>. I could not find a cause, so I call it a flake that I did not explain.
**What the emulator could not show, only your tablet can:** how the switch between the two worlds looks on the real screen (a short cover bridges the reload), whether the Recents opening animation differs a little, that an app installed while unlocked appears on the unlocked home only (the test had nothing to check: the emulator never reported the install), how your tablet's own system screens behave in front of this launcher, and the three-Home-press failsafe when the helper is off.

**Please check on the tablet:** (1) handshake unlock: this launcher with all apps; (2) double Home back, also from inside an app: custom, hidden apps hidden again, no flash of the full list or of the other layout; if a double Home sometimes does not register, tell me (the window is 0.9 s and the launcher now does the work that another process did before); (3) lock and unlock a few times; (4) the Recents key; (5) install an app while unlocked: it must show on the unlocked home only. If anything looks wrong, the switch in Launcher control brings the old behaviour back at once.
