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

10. (done 2026-11-09 06:20 UTC, ckpt 800+) The two TO-DOs of 9 are in: `PACKAGE_USAGE_STATS` is in tools/adb-setup.sh (grant + --check), tools/adb-all.sh / .bat (loop; status grep had it), research/ADB_COMMANDS.txt, ADB_TODO.md,
    ADB_PHONE.txt, the phone guide (`phoneguide/.../steps.txt` = `research/ADB_PHONE_COMMANDS.txt`, GuideTest now pins FIVE grants), the in-app adb guide (`AdbGuideActivity` step 5 + status tick), the helper's
    "Do everything" job list (`ShellProtocol.RUN` id GRANT_USAGE, `SetupActivity` item) and the tablet report's permission list. Mono audio / Audio balance: `SsPagesAccessibility.applyPrivate` = own write, else the helper verb
    SETSYS (`ShellProtocol.setSystem`: master_mono 0|1, master_balance -1..1 with at most 3 decimals, nothing else), else revert + toast. After an UPDATE the helper process must be restarted (an old helper does not know SETSYS:
    it answers an error and the control goes back). Scenario: SS22/SS22b (no helper: nothing stored, switch back), SS22d/e + SS23b (helper: stored 1 / negative), restore by `settings delete` when the value was absent.
11. Home page summaries: Battery = "NN% - <header sentence>" (`SsHomeSummary`, same sources as the Battery page; scenario SS5b), Storage = "NN% used - X GB free" (AOSP TopLevelStoragePreferenceController; wired from the Storage page code).

12. Font size table (BN1l, from Tj's four Font size shots): the tablet's steps are Small 1.0 / Default 1.2 / Large 1.3 / Largest 1.4 (`SsDisplayRules.FONT_VALUES`; the AOSP table 0.85 / 1.0 / 1.15 / 1.30 and my
    first guess 1.05 / 1.2 / 1.35 / 1.5 are both wrong). Evidence: the body text line pitch is 18 / 21 / 23 / 24 px, which fits ascent + descent rounding only for those four scales; the 16 sp lines (footer, subtitle) are
    0.846 / 1 / 1.078 / 1.194 times as wide as at Default (the tablet's text engine rounds advances, so widths alone say 1.015 / 1.2 / 1.294 / 1.433). On his page the page text (footer, label) rescales at every stop (the
    activity is recreated by the commit) while the toolbar title and the "Preview" label do NOT (they are dp texts): the label now uses the toolbar title's own text appearance (regular weight, not 20 sp medium), and the
    sample title "The Wonderful Wizard of Oz" is regular (it was medium). The tablet's text is wider than the CI emulator's by 0 to 5 % depending on the size (engine, not code): never calibrate widths against the emulator.
13. Display size chat preview (BN1l, from Tj's five Display size shots, 144 / 170 / 198 / 226 / 254 dpi; `SsConversation`): outgoing = NO bubble (text #323232, time #8380AD, blue #4285F4 52 dp disc "A" at the right);
    incoming = green #689F38 bubble with a tail (10 x 12 dp, tip at the top-left corner, radius 4), white text, time #375049, green disc "P". Letter 32 sp bold, text 16 sp, time 12 sp. Row top margin 18 dp, list padding 10 dp,
    bubble padding top 16 / bottom 12, 3 dp between text and time, incoming start 20.4 / end 15, outgoing start 14.3 / end 21.6, 4 dp of room after the time, outgoing time starts 3.2 dp further in, bubble 1.5 dp from its
    disc, widest bubble = list width - 2 x (52 + 6) dp. Reproduces Tj's wraps at every density (Default: "... not too / far from here."; Largest: "Hey, want to grab / coffee and catch up / today?"). The CI emulator's stable
    density is 160 (forced to 170 for the run), so its Display size page reads "Custom (170)": compare geometry in dp, not px.
14. Look first, function second (Tj, 2026-11-09 06:28, BN1l): a refused write is SILENT everywhere (no "This setting can't be changed from here." / Do Not Disturb toasts); the control stays where Tj put it. Mono audio /
    Audio balance keep a `Look` (what the control shows + the stored value it was set against) until the stored value changes behind its back; the Sound page's switches and sliders do not snap back; the helper write is still
    tried quietly. Nothing new is requested for function (no new adb grants, no DND access).

## Open questions for Tj (ask ONCE, batched, only if his answer changes the work)
- (ANSWERED 2026-11-09 06:28, decision 12) Font size page of his tablet: four screenshots, one per position.
- (ANSWERED 2026-11-09 06:28, decision 13) Display size page of his tablet: five screenshots, one per stop.
- Should the Solitaire row in Battery usage show a percentage? (Built without one.)
