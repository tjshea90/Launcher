# Draft of the answer to Tj for v0.9.6 (BV1: the music app), written BEFORE the release so a cut-off session can send it

Rules (CLAUDE.md "Messages from Tj", TASKS BP3b): the answer is the LAST message of the turn and is also appended to OUTBOX.md. Fill the markers (`<<RELEASE>>`, `<<GATE>>`, `<<MU>>`) from the real results and DELETE any sentence the results do not support (check each one against the RUN LIST in `CHECKPOINT.md` / the ckpt texts, not memory). Do not claim a tablet result. Keep it short; the Release link goes on its own line as plain text.

---

Tj, the music app is in v0.9.6, as its own app next to the launcher.

<<RELEASE>>   (the Release link on its own line; the three APKs in it: `Launcher-v0.9.6.apk` = update in place as always, `Eleven-v0.9.6.apk` = the music app, install it once, same signing key, `PhoneGuide-v0.9.6.apk` unchanged)

**What it is.** LineageOS's own music app, Eleven (package `org.lineageos.eleven`, from the `lineage-17.1` branch, pinned to one commit, Apache-2.0 notice and a list of my changes are in the repo under `eleven/`). It looks and works as upstream wrote it, with the album art removed.

**The name and the icon (your latest message).** The app is called "Music" in every language (upstream had translated its name, "Musique", "Música" and so on: all 70 translations are removed) and its icon is the blue speaker of your ATG music player. I searched the web for the icon file (three searches: no listing, APK or icon of an "ATG music player" turned up), so I redrew the speaker from your two screenshots. At your drawer's own size my drawing differs from your screenshot by about 5 of 255 levels on average, and at the size in the Files screenshot, which I did not tune to, by about 6 (method and numbers: `research/MUSIC_ICON_2026-11-11.md`). My first version came out about 13 % too small in the launcher's own drawer on the emulator (the launcher shrinks round icons of the old kind: 0.65 of the white square against your 0.715), so the icon is now the newer "adaptive" kind, which the launcher does not shrink, and a check on the emulator's drawer measures the size against yours. With it the speaker is 38 of 52 pixels wide on the emulator's drawer, against 39 of 55 on yours (0.73 and 0.715 of the white square), and the blue cone, black ring, grey rim and the light from below match the numbers I measured on your screenshot. If you send me the ATG APK file itself I can take its original icon out of it, byte for byte; say so if you want that.

**Heads-up: two "Music" entries.** Your tablet already has the ATG "Music". After you install this one, the custom drawer shows two entries called Music with the same icon until you hide or uninstall the old one (Hidden apps screen). I did not touch the old one. One side effect (reasoned from the code, not run): with the shell helper off, the Recents label closer cannot tell a hidden "Music" from a shown "Music" (the helper's sweeps work by package and are fine). Easiest: uninstall the old one; or tell me and I handle it in the code.

**In the custom drawer.** The launcher puts the music app into the custom drawer by itself, once per install, the first time it sees it. If you switch it off on the Hidden apps screen it stays off, also after an update. Uninstall and reinstall counts as new and puts it back. It does not appear on the home screen by itself (the stock "add icon to home on install" is switched off for it; reasoned, the emulator cannot test an installer install).

**No album art, ever.** I did not hide it, I removed every place that reads or shows it: the app no longer asks Android for a cover, never decodes one, keeps no picture cache, and has no internet permission, so nothing can be fetched. Artists show the coloured letter cards, albums and songs the music-note tile, as the app draws them when it finds no art. Removed with it: "Change image" on artists and albums, the photo picker, "Delete cache" in Settings. A unit test (12 checks, runs for every release) fails the build if any way to read, cache or show a cover comes back.

**What is proven** (it all ran; the last music-app run on the final code, #561: 29 checks passed, 0 failed)
- Emulator, same song with a flat bright magenta cover, same screens: upstream's app WITH its art code shows that cover on the songs tab, the albums tab, the playing screen and the notification card; our app shows none of it in 19 pictures (tabs, artist and album pages, menus, playing screen, shade, lock screen) and shows the placeholders instead. The app stayed in front through the whole tour and did not crash.
- Drawer rule on the emulator: appears by itself, a system app outside your list stays hidden, opens from the drawer, a hide on the real Hidden apps screen sticks (also across an update in place), uninstall + reinstall brings it back, also when the launcher did not see the uninstall.
- Icon on the emulator (MU-21): a check measures the speaker on the white square of the real drawer picture (size against the square, blue cone, dust cap, black ring, grey rim, shading) with numbers taken from your screenshot. The first, old-style icon failed it (0.65 of the square, run #554); the adaptive one passes (run #561), and I looked at the picture.
- The launcher code is unchanged by this release except the drawer rule for the music app and the version (`git diff` against the gated code is empty otherwise). All gate sections passed on it: base, legacy, n (#550), o and last (#559), x1 and ac (#549), e1 and cs (#548), rb (#552), ss (#557), the music-app section mu (#561). A few failed once without a code change and passed on a rerun (o: three checks and an emulator that went offline; ss: the check that opens the real Settings; rb: one check); o and ss also pass on the v0.9.5 release (runs #558 and #556) and rb passed on a solo rerun, so those were the CI emulator, not this release. The unit tests (1030: 1000 launcher, 17 music app, 13 phone guide) and lint are green.

**What only your tablet can show**
- The permission prompt on first start (tap Allow), real music files, the Go-edition memory, headset and Bluetooth buttons, and how the media card looks on the MediaTek lock screen (on the emulator the card is green with the placeholder note).
- The notification, the lock-screen card and the widgets show the PLACEHOLDER tile (not art), as Eleven does for songs without art.

**Two questions for you**
1. "Blur background" is still in Eleven's Settings; with no art it blurs nothing. Remove it?
2. Do you want the notification and lock-screen media card without any picture (no placeholder note either)?

**Still waiting on you** (unchanged):
- BR1c: does the settings gear in the custom notification panel open the look-alike?
- BN7a: start the shell helper and tap "Do everything that is missing".
- BN8c / BN8d: tablet reports, one right after the first side-key press on the custom home and one before you open more stock-launcher apps.
- BQ1b: a tablet report right after any lock-screen screenshot or white box.
- BH1c: where you saw a pop-up notification, and from which app.
- BK1e: which unlock is slow, what is on screen, and how many seconds.
- BM1: the account plugin `session-handoff`.
- BS2e: the Wi-Fi / Bluetooth symbol check from v0.9.4.
- BT1h: the five tablet checks of v0.9.5 (pull-up cover, flicker after unlock, side-key after the notification panel, home wallpaper cover, "Settings" name).
