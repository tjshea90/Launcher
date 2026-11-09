# BN1 build log (Settings look-alike) — newest facts first; read with SETTINGS_BUILD_PLAN_2026-11-09.md

## Where things are (so a cold session does not search)
- Code (all `app/src/main/java/com/tj/launcher/`): `SettingsSpoof` (switch `settings_spoof`, alias gating, `syncSoon` / `syncNow`, real Settings kept out of the custom drawer), `SsPage` (enum of pages),
  `SsPageActivity` (ONE activity, extra `ss_page`), `SsPages` (page builders; M0: home only, others say "not built yet"), `SsRows` (row / tile / divider factory). Resources: `res/values/ss_styles.xml`
  (theme `SsTheme`, `SsTheme.Home`), `ss_strings.xml`, `res/drawable/ss_ic_launcher.xml` (adaptive icon) + `res/mipmap-*/ss_ic_launcher_foreground.png` (AOSP gear PNGs, unchanged), `ss_glyph_*.xml` (the seven
  home glyphs), `ss_ic_arrow_back.xml`. Manifest: `SsPageActivity` + `activity-alias com.tj.launcher.SettingsAlias` (DISABLED in the manifest; the code enables it). Wiring: `AppVisibility.ALWAYS_SHOWN_OWN_COMPONENTS`,
  `HiddenApps.isShown/isPackageShown` (real Settings hidden), `Mode.set`, `TjApplication.onCreate`, `RestoreStore`, `RecentsGuard` (the look-alike's task stays in Recents), `ControlActivity` (switch row), `ReportState`.
- Tests: `SettingsSpoofTest` (10 JVM tests: gating, entry set, page table). Emulator section `ss` (scenario + `tools/gate-map.txt`): SS1-SS7 = alias on/off, ONE Settings in the drawer, opens `SsPageActivity`, seven rows in order,
  no search. First CI run of `ss` with `shots=true`: emulator.yml run #451 (id 42565458939, head 2ba5988).
- The switch is WORK IN PROGRESS: default OFF everywhere (`SettingsSpoof.defaultOn` returns false; `SettingsSpoofTest.wipDefaultIsOff` pins it). The finishing commit makes release builds ON (`return !debuggable`) and changes the test.

## Tj's screenshots (still in `/root/.claude/uploads/<session>/`; NEVER copy into the repo). File name prefix -> page
07401db6 Battery 84% | d6bbbc77 Battery 86% | e895669e Battery 86% | 0b8dab8c Sound | 4cd04033 Sound (scrolled) | 1eeec128 Display | 20aea9d7 Display | 2a288d48 Accessibility (top) | 3c150c6a Accessibility (middle) |
3d280fbe Accessibility (lower) | 52ef06ee Storage | 623c7986 Languages & input | 64660f88 About tablet (top, Owner) | a39abad9 About tablet (scrolled) | 8052f043 Touch & hold delay dialog |
8d14938a Text-to-speech output | aa9db61d Legal information | d9d18032 Magnification | cc4fb931 System | be456e72 Settings home (top) | df7cd971 Settings home (bottom) | afaadb68, bd381c3e = chat screenshots (not Settings).
A contact sheet of all of them can be made in a minute with PIL (scratchpad only).

## Decisions taken while building (Claude; Tj can veto) — rule: ANYTHING not in Tj's list of pages or screenshots does nothing when tapped (he said "only the following settings pages available"; "Model & hardware" is inert on the stock tablet too)
1. Inert rows: Languages & input -> Languages, Virtual keyboard, Physical keyboard, Spell checker (their pages are not in his list); About -> Model & hardware, Android version (its page shows the Build number), Custom build version;
   Battery usage -> the Solitaire row. Controls that live ON an allowed page work (e.g. Pointer speed dialog, brightness, timeout, volumes).
2. Legal information rows open the real read-only licence screens through the public intents (`android.settings.LICENSE` etc.) when a handler exists.
3. Sound ringtone rows use `RingtoneManager.ACTION_RINGTONE_PICKER` (public; what the real Settings does).

4. Battery numbers (estimate, "Last full charge", "Screen usage since full charge") come from Android's own statistics, read the way an adb shell reads them: `dumpsys batterystats --charged` run by the app (it holds DUMP; `SsBatteryStats`,
   format from AOSP 10 `BatteryStats.dumpLocked`: "Battery time remaining:", "Charge time remaining:", "Start clock time:", "Screen on:"). The status of the last read is in the tablet report (`ss_battery_status`). Unknown value = blank, never a guess.
5. Category headings are 24 dp above / 8 dp below the text (measured on Tj's Languages screenshot: the glyphs sit exactly 8 px lower than the first guess of 16 / 16 dp); the footer text ends 16 dp before the row padding (Tj's footer wraps "... based on / usage").
6. Battery usage: ONE permanent row, Solitaire with its real icon (a generic icon if the app is not installed), no percentage, never anything else (Tj: "one permanent app solitaire in the list that never changes and nothing ever adds to the list"). If Tj wants a number on it, say so.
7. The Settings icon is the AOSP gear on teal; if the tablet's vendor icon differs, Tj tells us. Nav bar: the page theme sets the same window attributes as AOSP Settings (transparent bar colour, light icons, 12 % divider); it cannot be compared on the CI emulator (no 3-button bar in its screenshots).
8. Font size table is PROVISIONAL: AOSP 10 has 0.85 / 1.0 / 1.15 / 1.30 ("Default" = 1.0) but Tj's tablet shows "Default" at its own scale 1.2, so its table is shifted; `SsDisplayRules.FONT_VALUES` = 1.05 / 1.2 / 1.35 / 1.5 until Tj sends the page.

9. (run #455, id 42570101882, head 48b3aa0, ss: 40 PASS 2 FAIL) FACTS: (a) `pm grant PACKAGE_USAGE_STATS` WORKS and then the app's own `dumpsys batterystats --charged` works (status `ok start=true screen=true`), so Battery
   "Last full charge" / "Screen usage" need ONE extra adb grant for Tj: `adb shell pm grant com.tj.launcher android.permission.PACKAGE_USAGE_STATS` (TO DO: add to tools/adb-setup.sh, tools/adb-all.sh status, research/ADB_COMMANDS.txt,
   the in-app guide, the tablet checklist). (b) `master_mono` and `master_balance` are in `Settings.System.PRIVATE_SETTINGS`: a non-system app gets IllegalArgumentException("You cannot change private system settings.") EVEN with
   WRITE_SECURE_SETTINGS (SettingsProvider.enforceRestrictedSystemSettingsMutationForCallingPackage; only system / shell / root may write them). So Mono audio and Audio balance cannot be set by the app itself: plan = try the write,
   on failure ask the shell helper (new verb SETSYS, allow-list master_mono 0|1 and master_balance -1.0..1.0 only; helper is unproven on the tablet, HLP1), and if neither works REVERT the control and toast (never lie). The same
   restriction would hit any other private key. (c) Accessibility page render matches Tj's screenshots (rows, headings, Magnification icon, Touch & hold dialog, balance bar with centre tick; Left/Right labels are the SECONDARY
   colour, measured #737373). (d) scenario bug: `settings put system K "$VAR"` with VAR=null (absent key) writes the literal string "null": restore with `settings delete` when the old value was null.

## Open questions for Tj (ask ONCE, batched, only if his answer changes the work)
- Font size page of his tablet (Settings > Display > Font size): a screenshot of the page with the slider at each of the 4 positions, or at least the labels and the preview, so the steps (1.05 / 1.2 / 1.35 / 1.5 are guesses) and the page layout can be copied.
- Display size page of his tablet (the same, 5 stops expected: Small, Default, Large, Larger, Largest).
- Should the Solitaire row in Battery usage show a percentage? (Built without one.)
