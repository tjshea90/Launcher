# The answer to Tj for the one-launcher release (TASKS CV1; written 2026-11-22 BEFORE the release exists)

**Before sending:** (1) the Release exists (`get_release_by_tag`) and is recorded (BUILDLOG); (2) replace `<VERSION>`, `<LINK>` (alone on its own line, plain text), `<GATE>`; (3) check EVERY sentence against the run list in the newest ckpt, not memory (BT lesson). Everything below the line is the message.

---

<VERSION> is released: the unlocked home is now THIS launcher, so there is one launcher instead of two.

<LINK>

**What changed**
- After the secret handshake you land in this launcher in its unlocked form: every app in the drawer, the real Settings, Launcher control and Back to custom in the drawer, nothing hidden, no look-alike. Locked down it is the custom launcher as before. Double Home, the lock, a plain PIN and the other ways back work as before.
- The old launcher stays installed for two jobs only: it is Android's own Recents screen (the Recents key is unchanged), and it is the emergency exit: three crashes in two minutes put it back as the unlocked home.
- One switch in Launcher control, "One launcher for both worlds": on = this release, off = exactly the old two-launcher behaviour. "Restore everything" switches it off.

**One difference you will see:** the unlocked home has its own icon layout and it starts as a COPY of the custom home's layout (widgets and pinned shortcuts are not copied), not as your old stock arrangement (the folders "Connect" and "Games"): the stock launcher keeps that private and the app cannot read it. You arrange it once; after that it keeps its own layout.

**What the emulator proved** (<GATE>): in the unlocked world our launcher is in front and loaded its own layout file; the drawer shows all apps (Contacts, Launcher control, Back to custom); after double Home the custom drawer hides them again; an icon added in the unlocked home never appears in the custom home; the copy of the custom layout (app, folder with two apps; widgets and pinned shortcuts left out; ids moved to a separate range) was checked row by row; the Recents key still opens Android's own Recents; Launcher control opens; a tripped crash breaker brings the stock launcher back, double Home ends it, the switch off gives the old behaviour; the lock-screen sections ran once with the switch off and once with it on.
**What the emulator could not show, only your tablet can:** how the switch between the two worlds looks on the real screen (a short cover bridges the reload), whether the Recents opening animation differs a little, how your tablet's own system screens behave in front of this launcher, and the three-Home-press failsafe when the helper is off (the emulator's injected key presses never triggered it, the rule itself is covered by unit tests).

**Please check on the tablet:** (1) handshake unlock: this launcher with all apps; (2) double Home back: custom, hidden apps hidden again, no flash of the full list; (3) lock and unlock a few times; (4) the Recents key; (5) an app installed while unlocked appears on the unlocked home only. If anything looks wrong, the switch in Launcher control brings the old behaviour back at once.
