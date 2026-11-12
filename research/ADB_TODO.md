# ADB TO-DO LIST — everything to do with adb, in order (written 2026-11-04 for Tj)

## FAST PATH — everything in ONE adb session (about 5 minutes, no need to read the rest first)
1. On the tablet: Settings > About tablet > tap **Build number** 7 times; Settings > System > Developer options > **USB debugging ON**. Plug in USB, tap **Allow**.
2. On the computer, in the Launcher folder, run **one** of these:
   - Mac / Linux: `bash tools/adb-all.sh`
   - Windows: `tools\adb-all.bat`
   (add `Launcher-v0.5.0.apk` after it to install/update the launcher first; add `--browser-off` to also disable the old Browser app; `--status` only shows what is granted and changes nothing.)
3. It grants every permission and background setting (including the ones for future upgrades), turns the accessibility helper on, starts the invisible clean-up helper, reads everything useful (read-only), **copies the tablet's own Settings / SystemUI / stock launcher / framework apps**, takes screenshots of the stock Settings pages, and prints a status table of every permission command. Touch nothing on the tablet while the Settings screenshots are taken (about a minute).
4. **Send me the folder `probe-out`** (zip it), or paste `probe-out/status.txt` and `probe-out/launcher-probe.txt`. (Claude saves what you send with `bash tools/tablet-intake.sh`: redacted, summarised, kept in `research/tablet/` for every later session.)
5. After every tablet RESTART run it again (or only the helper line, section C): the helper does not survive a restart.
6. Later, with the helper running, the same things (and a fresh report) are one tap away inside the launcher: **Launcher control > Tablet setup & report**.

**With a PHONE running an adb app (ADB OTG etc.) instead of a computer:** see `research/ADB_PHONE.txt`. You paste ONE line (the helper line, no "adb shell" prefix); everything else is done by the launcher itself in Launcher control > Tablet setup & report, and the results are published to the tablet's Downloads / launcher-probe folder.

~~Optional experiment E1 (hide the pencil and gear with `device_demo_mode`)~~ **REMOVED 2026-11-05 (safety audit AB1): do NOT do this.** Android's retail-demo flag makes the system create a demo user and switch to it at boot (AOSP retail-mode documentation). The launcher hides the pencil with an accessibility cover instead. If you ever switched it on, `settings put global device_demo_mode 0` puts it back (also in the phone guide, section 10).

The sections below explain every command one by one (this is what the scripts run).

Tick a box when you have done it. **Total first time: about 10 minutes.** Every command can be pasted one line at a time (Windows, Mac or Linux).
Lines starting with `#` are comments. Every change is reversible (the UNDO block at the end). Nothing here touches the lock screen or installs software.

## 0. Once: make adb work (about 2 minutes)
- [ ] On the tablet: Settings > About tablet > tap **Build number** 7 times. Then Settings > System > Developer options > **USB debugging ON**.
- [ ] Plug the tablet into the computer by USB. Tap **Allow** on the tablet (tick "always allow").
- [ ] `adb devices` — the tablet must say `device` (not `unauthorized`). If `unauthorized`: unplug, plug in, tap Allow again.

## A. The launcher's permissions — once, they survive updates (about 1 minute)
The launcher already lists every one of these in advance, so a later release never needs a reinstall.
They are lost only if you uninstall the launcher.
- [ ] `adb shell pm grant com.tj.launcher android.permission.WRITE_SECURE_SETTINGS` — keyboard switch, quick-settings tiles per launcher, animation speed-up during the reset
- [ ] `adb shell pm grant com.tj.launcher android.permission.DUMP` — read-only: what is running / in Recents
- [ ] `adb shell pm grant com.tj.launcher android.permission.CHANGE_CONFIGURATION` — for later features (font size, orientation)
- [ ] `adb shell pm grant com.tj.launcher android.permission.READ_EXTERNAL_STORAGE` — only for the optional wallpaper-patch power-menu mode
- [ ] `adb shell pm grant com.tj.launcher android.permission.PACKAGE_USAGE_STATS` — only for the Battery page of the Settings look-alike (last full charge, screen time); without it that page shows the percentage only
- [ ] `adb shell cmd appops set com.tj.launcher GET_USAGE_STATS allow` — read-only "which app was used last"
- [ ] `adb shell cmd appops set com.tj.launcher WRITE_SETTINGS allow` — for later features (brightness, screen timeout)
- [ ] Check they took: `adb shell dumpsys package com.tj.launcher | grep -E "WRITE_SECURE_SETTINGS|DUMP|CHANGE_CONFIGURATION|READ_EXTERNAL_STORAGE|PACKAGE_USAGE_STATS"` — each should show `granted=true`
- Not needed and not requested: INTERNET, VIBRATE, KILL_BACKGROUND_PROCESSES, wallpaper and the rest are "normal" permissions Android gives at install. (INTERNET is declared only for the local link to the helper in section C.)

## B. Keep the helper alive on this low-memory tablet — once (about 30 seconds)
- [ ] `adb shell dumpsys deviceidle whitelist +com.tj.launcher`
- [ ] `adb shell cmd appops set com.tj.launcher RUN_IN_BACKGROUND allow`
- [ ] `adb shell cmd appops set com.tj.launcher RUN_ANY_IN_BACKGROUND allow`
- [ ] Turn the **accessibility helper** on: tablet Settings > Accessibility > "Launcher power-menu helper" > On. (From the computer instead: `adb shell settings get secure enabled_accessibility_services` — if it prints `null`: `adb shell settings put secure enabled_accessibility_services com.tj.launcher/com.tj.launcher.TjAccessibilityService` then `adb shell settings put secure accessibility_enabled 1`.)
- [ ] After installing a NEW version: turn the helper off and on once (new key-event access for the volume / Back methods).

## C. The invisible clean-up helper — **repeat after EVERY tablet restart** (about 20 seconds)
Starts a small helper as the shell user. With it, locking or leaving Quickstep closes its apps, notifications and Recents cards with no black cover and no delay at unlock.
It does not survive a restart (Android rule for adb-started processes). Without it everything still works, with the black cover.
- [ ] Mac / Linux (one line, keep the single quotes):
  `adb shell 'CLASSPATH=$(pm path com.tj.launcher | head -1 | cut -d: -f2) setsid nohup app_process /system/bin com.tj.launcher.ShellServer >/dev/null 2>&1 &'`
  or simply: `bash tools/adb-setup.sh --helper`
- [ ] Windows (two steps): `adb shell pm path com.tj.launcher` prints `package:/data/app/.../base.apk`; copy the part after `package:`, then
  `adb shell "CLASSPATH=PASTE_THE_PATH_HERE setsid nohup app_process /system/bin com.tj.launcher.ShellServer >/dev/null 2>&1 &"`
- [ ] Check: Launcher control > Invisible clean-up says **RUNNING**.
- Stop it any time: `adb shell pkill -f com.tj.launcher.ShellServer`

## D. The "Browser" app (com.android.browser) — optional (about 10 seconds)
Nothing in Android needs it; links open in your other browser instead.
- [ ] Safest — disable it (hidden, fully reversible): `adb shell pm disable-user --user 0 com.android.browser`
- [ ] Or remove it for your user (the file stays in the system image): `adb shell pm uninstall -k --user 0 com.android.browser`
- Bring it back: `adb shell pm enable com.android.browser`  or  `adb shell pm install-existing --user 0 com.android.browser`

## E. Check the user profiles — decides whether a REAL second Android user is possible (about 10 seconds)
- [ ] `adb shell pm get-max-users` — prints `Maximum supported users: N`. **N = 1 means a second user is impossible on this tablet; 2 or more means it is possible** (a user switch ends on the other user's own lock screen, so it is never seamless: see research/IDEAS_2026-11-04.md).
- [ ] `adb shell pm list users` — lists the users that exist now (normally only `UserInfo{0:...}`).
- [ ] `adb shell settings get global user_switcher_enabled`
- **Send me the output of these three.**

## F. The read-only probe of the tablet — for the power menu, restarts and the Settings clone (about 2 minutes)
Changes nothing. Prints no accounts, serial number or Wi-Fi names.
- [ ] Mac / Linux: `bash tools/adb-probe.sh` — writes `probe-out/launcher-probe.txt`. **Paste that file to me.**
- [ ] For the Settings clone also: `bash tools/adb-probe.sh --pull` — copies the tablet's own Settings app and framework (the files I need to match the look and find the power-menu list) into `probe-out/` (about 50-100 MB). Send the `.apk` files if you can (or just the Settings one).
- [ ] Windows without bash: paste the lines of SECTION 12 in `research/ADB_COMMANDS.txt` one at a time and send me the output.
- Why each fact matters: `ro.crypto.*` = can the helper run before the first unlock after a restart; `pm get-max-users` = second user; `service.adb.tcp.port` / `persist.adb.tcp.port` = could the launcher start its own helper after every restart with no computer; `power_menu_actions` / `lineage` = does the ROM have a native power-menu setting (then Power off + Restart only needs no overlay at all); `pm path com.android.settings` = the app to copy for the Settings look.

## G. If Android ever shows "Choose a Home app" (after a launcher crash)
- [ ] `adb shell cmd package set-home-activity com.tj.launcher/com.tj.launcher.RouterActivity`

## H. Keyboard switch (Gboard in Quickstep, stock keyboard in custom; SwiftKey until 2026-11-12) — check once
- [ ] `adb shell ime list -s` — shows the enabled keyboards. Both the stock one and Gboard must be listed (the launcher only switches between keyboards that are already enabled).
- [ ] If Gboard is installed but missing: `adb shell ime enable com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME` (or Settings > System > Languages & input > Virtual keyboard > Manage keyboards)

## I. Experiments I may ask you to run later (do NOT run these yet)
- Power-menu and pencil tests that need the tablet (see research/IDEAS_2026-11-04.md); I will give you the exact lines when they are proven safe on the emulator.

## UNDO (any time)
- `adb shell pm revoke com.tj.launcher android.permission.WRITE_SECURE_SETTINGS` (and the same for DUMP, CHANGE_CONFIGURATION, READ_EXTERNAL_STORAGE)
- `adb shell cmd appops set com.tj.launcher GET_USAGE_STATS default` and `... WRITE_SETTINGS default`
- `adb shell dumpsys deviceidle whitelist -com.tj.launcher`
- If animations ever stay off (they are put back automatically): `adb shell settings put global window_animation_scale 1.0` and the same for `transition_animation_scale` and `animator_duration_scale`
- Browser: see D. Helper: `adb shell pkill -f com.tj.launcher.ShellServer`
- Everything the launcher changed: Launcher control > Restore everything.

## After every restart: only section C (and turn the helper off/on once after installing a new version).
