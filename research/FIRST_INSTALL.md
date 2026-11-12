# First install on the tablet — checklist (v0.2.x "safe core")

What v0.2 is: Launcher3 Go port (home, drawer, folders, search, wallpapers) + the safety core. It is NOT yet the finished product: the look is AOSP Go defaults
(4x4 home with search bar and dock, 4-column drawer) until the grid is matched to your stock layout, and the special features (hide apps, clone Settings,
power-menu screenshot hide, kill switch, keyboard switch) are not in yet. It changes NO system setting, needs NO permission beyond launcher basics
(call shortcuts, wallpaper, widgets), has no boot receiver and no lock-screen code.
Proven so far (Android 10 emulator, GitHub Actions run, every commit): HOME goes through an invisible router; custom<->Quickstep routing; 3 crashes in 2 minutes
switch itself back to Quickstep; uninstall leaves the stock launcher as Home; the manifest contains no device-admin/overlay/keyguard/boot/secure-settings permissions.
NOT proven (needs your tablet): the recents button with our app as Home (R3), vendor quirks, the real home layout/density.

Before installing: have a PC with `adb` ready if you can (not required): `bash tools/adb-restore.sh` puts everything back in one command.
1. Download the APK from the GitHub Release and install it (Settings may ask to allow "install unknown apps" for your browser/file manager).
2. Settings > Apps & notifications > Default apps > Home app > choose **Launcher**. Press Home: you should see the new launcher. (If you ever see a chooser, pick "Launcher" > Always.)
3. Test: Home button; long-press empty space (Home settings / Wallpapers); swipe up for the drawer; open an app; press Recents (square button): the recent apps screen should appear.
   Two-finger CLOCKWISE twist on the home screen -> goes to the stock launcher. Open the app icon **Launcher control** (in the stock launcher's drawer) -> "Use custom launcher".
4. If anything is wrong: Launcher control > **Restore everything** > or Settings > Apps > Default apps > Home app > Quickstep > or uninstall Launcher.
   Safe mode (hold power, long-press "Power off", OK) disables all user-installed apps and shows the stock launcher.
5. Tell Claude what you see (screenshots help): the home layout, the drawer, and whether Recents works.

## v0.2.1 additions (2026-11-02)
- Look: 5-column grid + 5-column drawer, Go icon size (60 dp), drawer scrim = 50% white (measured from your stock photos). Row pitch is ~7% taller than stock (needs your answer: does the stock home have a dock?).
- **Power-menu helper (optional, OFF by default).** Open the "Launcher control" app icon > choose "Power menu: hide Screenshot button" (or "block it completely") > "Open Accessibility settings"
  > find "Launcher power-menu helper" > turn it on. It only acts while the custom launcher is active and never on the lock screen; switch back to Quickstep (or choose "leave alone") and the normal power menu returns.
  Proven on an Android 10 emulator (Screenshot entry fully hidden). Your tablet's panel colour may differ a little: send a photo if you can see a patch.
- If Android ever shows "Select a Home app": choose Launcher > Always (or Quickstep to go back). It is only a chooser.

## Getting back to the custom launcher from Quickstep (v0.2.2)
- **Press Home twice quickly** (within ~0.7 s) while the stock launcher is showing: you are back on the custom launcher. No settings or permissions needed.
- Or open the "Launcher control" app icon (in the stock drawer) > "Use custom launcher".

## v0.2.2 (2026-11-02)
- Home screen: 8 rows, NO dock, no "^" caret (matches your stock photos). Drawer unchanged from v0.2.1 (it matched).
- Power menu: "remove Screenshot (two-button menu)" now covers the whole system card with Power off + Restart (no gray gap); tapping them presses the real buttons. If a press ever fails the original menu is revealed.
- **Double-press Home** in the stock launcher = back to the custom launcher.

## v0.2.3 (2026-11-02)
- **Return reset.** When you double-press Home to come back from the stock launcher, the launcher now: asks Android to close every app (apps with their own running service may stay), clears all notifications,
  turns Wi-Fi and Bluetooth off, and opens your Solitaire app (found by name; Launcher control shows which app it found). Master switch: Launcher control > "Return reset: ON / OFF".
  For notifications to clear, turn on **Notification access** for "Launcher" (Launcher control has a button that opens that page). Recents thumbnails can remain (clearing the Recents list needs a system permission).
- **Twist gesture** is more responsive: the home screen no longer scrolls while two fingers are down, and it fires after 30 degrees (was 40).
- **Faster power-menu cover**: acts on the first event and remembers where the card is (a very brief flash may remain; Android creates the menu before we can react).

## v0.3.0 (2026-11-02) — hide apps, lock, Recents, reset fixes
- **Hide apps / new installs.** Quickstep mode > Launcher control > "Choose apps shown in the custom launcher": tick = shown in the custom drawer AND its search. Everything you already have is ticked the first time the new launcher runs;
  apps installed (or uninstalled and reinstalled) later start UNticked and stay hidden (also kept off the home screen) until you tick them. Quickstep always shows everything.
- **Lock.** Launcher control and the app chooser do not open and have no icon while the custom launcher is on; use the twist (or Home-Home is for the other direction) to reach Quickstep first.
- **Reset on double-Home** (Quickstep -> custom): no more Home press (that caused the Solitaire glitch: Solitaire was in Recents but the home screen covered it). Solitaire is started last and re-started up to 3 times until it is actually in front; Launcher control > "Last reset" says what happened.
- **Three independent switches** (Launcher control, Quickstep mode): A = apps launched from the custom launcher stay out of Recents except Solitaire; B1 = press "Clear all" in the notification shade; B2 = press "Clear all" in Recents (closes those apps). B1/B2 need the accessibility helper. Try them alone and together and tell me which works best.
- Notification access does not exist on Android Go, so the old notification route is gone.

## v0.3.1 (2026-11-02)
- **Twist gesture** fires after 18 degrees (was 30), with looser pinch tolerance: more responsive. **Double-press Home** window is now 0.9 s (was 0.7 s).
- **Solitaire in front whenever the custom launcher starts** (boot, or if Android restarts it) and on return from Quickstep. Switch: Launcher control > "Open the launcher at start". Normal Home presses stay stock.
- **Our launcher never appears in Recents** (it lost its home status when the router took over; fixed with an exclude flag).
- **Faster reset:** polls every 40 ms for the real "Clear all" buttons instead of waiting; an empty shade/Recents ends instantly; "Last reset" shows how many ms it took.
- **Keyboard switch (needs one adb command):** `adb shell pm grant com.tj.launcher android.permission.WRITE_SECURE_SETTINGS`, then Launcher control > "Stock keyboard in custom, Gboard in Quickstep" ON (Gboard, not SwiftKey, since 2026-11-12). It only switches to a keyboard that is already enabled, and "Restore everything" puts your original back.

## v0.3.2 (2026-11-03)
- **After installing, turn the helper off and on once** (Settings > Accessibility > "Launcher power-menu helper"): it now also asks to see the side (power) key, and Android re-reads that when the service restarts.
- **Quickstep lock-out (W5).** In custom mode, if the stock home screen shows up without your twist (e.g. after closing the last app) the helper presses Home and the router sends you back to the custom launcher. Needs the helper ON. It stands down if the custom launcher cannot start, and after 4 redirects in 8 s (no loops). Launcher control > "Quickstep lock-out" switch + a counter of how many times it fired. The twist still works.
- **Recents (W2).** Our launcher and its screens are now marked "excluded from Recents" at runtime (the old flag only worked on newly created tasks, so an old Launcher task kept showing). Fallback: if a card still appears it is named and iconed like Solitaire.
- **Faster double-Home reset (W1).** Closing apps / Wi-Fi / Bluetooth now run in the background, Clear all is polled every 20 ms from the first instant, shorter waits, Solitaire checked sooner. "Last reset" in Launcher control lists the ms for each step: send me that line.
- **Power menu (W3/W4).** The replacement card is now kept ready (invisible, untouchable) and just made visible when the menu opens; and when the side key goes DOWN the helper shows it at once, before the long-press menu exists. Launcher control shows "Side key seen by the helper: N times": if it stays 0 after you press the key, the hardware does not pass that key to accessibility and the early show cannot work on this tablet.
- **Lock-screen number (X1).** `71125011907547` is drawn at the bottom of the lock screen by the helper and vanishes when the PIN pad is pulled up. Look measured from your photo. Launcher control > "Lock-screen number: ON / OFF". "Set the lock-screen wallpaper to the ORIGINAL leaf photo" sets the original (the one you sent, no number in it) as your lock wallpaper. If the lock screen's own "Charging" text sits at the same spot the two will overlap: tell me and I will nudge the number.

## v0.3.3 (2026-11-03)
- **Power-menu cover now also works on the lock screen** (custom mode). Blocking the menu still stays off the lock screen.
- **Lock-screen number matches your photo (5.png):** 6% larger, Light weight (the photo's strokes carry about 19 % less ink than Regular), nudged 1 px right and 1 px down. Measured from 5.png vs your tablet screenshot 6.png. Send a new screenshot if anything is still off.
- **Faster double-Home reset:**
  - It looks at the status bar first: no notification icons = the whole shade step is skipped. If it cannot tell, it does the step as before.
  - **Animations off during the reset** (put back exactly as they were; four failsafes). Needs the one-time adb grant `adb shell pm grant com.tj.launcher android.permission.WRITE_SECURE_SETTINGS` (the same one the keyboard switch uses). Without it nothing changes. Switch in Launcher control.
  - Shorter waits everywhere; "Last reset" lists the ms for each step.
- **New power mode "wallpaper cover"** (Launcher control > Power menu): while the custom launcher is active, a permanent patch of the wallpaper sits over the power-menu area, so the real menu is never seen; your Power off / Restart card shows on top. Needs: (1) tap "Allow wallpaper access", (2) open the power menu once so the helper learns where it sits (per screen orientation). It also covers that spot over apps. Not on the lock screen. Launcher control shows a "Wallpaper patch:" status line. If the patch is slightly off on your wallpaper, send me a screenshot with the mode on and one with it off.
- **Lock wallpaper (v0.3.3):** the bundled photo is now your ORIGINAL leaf wallpaper (960x853, no number), replacing the AI-edited one. Launcher control > "Set the lock-screen wallpaper to the ORIGINAL leaf photo". Android centre-crops it to the portrait screen, so the framing differs from the AI portrait version.

## v0.3.4 (2026-11-03)
- **Launcher never in Recents (B1).** Why it still showed: Android always lists the MOST RECENT task even when it is excluded, so opening Recents from the custom home screen listed "Launcher". Fix (your choice, default ON): the custom launcher is now a HOME-type task (Android never lists those). COST: if the launcher process ever crashes, Android clears our default-home setting and the next Home press shows 'Choose a Home app': pick Launcher > Always. Switch: Launcher control > Recents; "Restore everything" turns it off. Backup: any card of ours that ever shows says "solitaire" with the Solitaire icon (v0.3.2 set that only once per process; now on every resume). Also fixes the root of the "Quickstep appears by itself" problem (Android now resumes OUR home task).
- **Launcher control scrolls (B2).**
- **Quick-settings pencil hidden and disabled (B6):** a white cover over the pencil while the panel is open (your photo: the panel is pure white); taps on it do nothing. Switch in Launcher control.
- **Double-Home, shortest path (B4):** looks first. No notification icons in the status bar = no shade step. No other app opened since Recents was last cleared = no Recents step. If both are skipped, Solitaire appears at once with nothing else on screen. When something must be cleared, a black cover hides the shade / Recents flicker, animations are off (adb grant needed), Solitaire starts the moment clearing ends and the cover goes as soon as Solitaire is verified in front (4.5 s failsafe). Switch for the cover in Launcher control.
- **Wallpaper (B5):** NOT changed yet. The image did not arrive with the message: please send it again.

## v0.3.5 (2026-11-04)
- **Lock-screen number is editable (C1):** Launcher control > "Lock-screen number" box > Save (same font, colour, position and behaviour; up to 24 letters/digits/space - : . / _). "Use the original number again" puts 71125011907547 back.
- **adb setup guide in the app (C2):** Launcher control > "adb setup guide": every command step by step, a tick for what is already granted, Copy buttons. The full list is also in research/ADB_COMMANDS.txt and `bash tools/adb-setup.sh` runs the safe ones. v0.3.5 DECLARES extra safe permissions in advance (DUMP, CHANGE_CONFIGURATION, usage access, modify system settings) so you can grant them later without reinstalling. Nothing is granted until you run the adb line.
- **"Quickstep" never in Recents (C5):** the router now starts the stock launcher as a HOME-type task (it used to start it with an explicit component, which Android listed as an ordinary app named "Quickstep").

## v0.3.6 (2026-11-04)
- **New Launcher control (D2):** sections, a real on/off switch for every option (green = on), one-line explanations, a status card (active launcher, accessibility helper, adb permission), tick boxes for tiles, a confirmation before "Restore everything".
- **Separate Recents (D5):** apps you open from the custom launcher now stay in Recents like normal apps (the old "only Solitaire" flag is OFF by default; switch in Recents). Apps opened from Quickstep are closed when you come back to the custom launcher (their Recents cards are dismissed one by one; the others stay). "Switch to the custom launcher" in Launcher control does the same.
- **Quick-settings tiles per launcher (D6):** ONE tile (rotation) in the custom launcher, three (wifi, bt, rotation, pick your own) in Quickstep. Needs the adb permission WRITE_SECURE_SETTINGS (step 2 of the adb guide). Original list restored by "Restore everything".
- **Power menu (D3):** measured, not guessed: Launcher control shows how many ms the card needed after the real menu window appeared ("Speed of the last menu"). There is NO earlier signal on Android 10 (the side key never reaches an accessibility service, and the system no longer broadcasts anything before it opens the power menu: checked in the Android 10 source), so the speed comes from the pre-built (warm) card. That "Side key seen" counter will stay 0.
- **Wallpaper patch (D0):** now only on the plain home screen (never over apps, not in All Apps or during transitions), with nudges (left/right, up/down, size) if it is slightly off on your wallpaper. A live copy of the screen is not possible on Android 10 (see the notes).
- **Pencil (D4):** the cover window is kept ready (hidden), so it appears faster. A permanent cover is impossible: it would sit over whatever app is under that spot.

## v0.3.7 (2026-11-04)
- **Choose the launcher when you unlock (E1, opt-in, default OFF):** Launcher control > Lock screen > "Choose the launcher when I unlock". Tap the **Delete key twice on the empty PIN pad**, then type your normal PIN and Enter = you land in **Quickstep**. A plain PIN (or fingerprint) = the **custom launcher**. Why not two PINs: Android keeps ONE credential and compares the whole entry, so "10001" for a PIN of "1000" is just a wrong attempt (and counts toward the lock-out). The helper only recognises WHICH TYPE of key was tapped (by its id); it never reads, stores or logs a digit. It acts only after the unlock has succeeded. Needs the accessibility helper ON; turn it off and on once after installing (new event type).
- **Less access for the helper:** the hardware-key capability (added for the side key, which Android never passes on) is removed again.
