# Settings spoof app (TASKS BN1) — notes, facts, measurements

Tj, 2026-11-09 02:06 UTC: build a look-alike of the stock Settings app (screens 600 x 1024, about 170 dpi, 1 dp = 1.0625 px) with only the
pages Battery, Display, Sound, Storage, Accessibility, System, About tablet; no search bar or icon anywhere. His 16 screenshots stay in the
session uploads folder (never in the repo: they show an IP, a MAC, an uptime, the model and build strings). THIS file keeps words and numbers only.
If a new session needs the pictures it must ask Tj to send them again; everything needed to START is below.

## 1. What each screenshot showed (read 2026-11-09; the last five screenshots are in section 6)

Common chrome: status bar 0-26 px light gray with the clock at the left; toolbar y 26-86 px (about 56 dp), slightly lighter gray, thin shadow under it;
back arrow at x 30 px; page title at x 77 px (72 dp) in a medium weight, about 20 sp; search icon at the right (TO REMOVE on every page); page background
near-white (#FAFAFA); navigation bar y 974-1024 px, white, three buttons (back, home, recents). Row text starts at x 77 px (72 dp), rows WITH an icon have the
icon centred at x 30 px (24 dp icon, 16 dp start margin). Teal accent (about #00897B) for headings, switches, slider thumbs, progress bars. Section headings are
teal capitals about 13 sp ("DISPLAY"). Hairline divider (1 px) full width between groups. Two-line row pitch about 87 px, one-line row pitch about 62 px.
Switch: track light, thumb round; ON = teal thumb with a pale teal track.

- **Battery** (3 variants seen in the batch; one read so far): gray header (#E0E0E0) 26..283 px: big "84" with a small "%" at the right of it, under it
  "Should last until about 9:15 AM" in gray (two lines), a big teal battery icon at the right (about 70 x 105 px, x 390-460). Then (all of it NOT wanted by
  Tj, except as noted) a bordered card "Apps are running normally / Tablet has typical background battery usage", "Battery Saver / Off", "Battery Manager /
  On / Detecting when apps drain battery", "Battery percentage" switch. WANTED: a divider, "Last full charge" with the value right-aligned ("2 hours ago"),
  "Screen usage since full charge" with the value right-aligned ("1 hr, 16 min"), a divider, an info icon (circle with i) at x 30 and the note "Battery usage
  data is approximate and can change based on usage" (gray, two lines). Toolbar: back arrow, title, search icon (remove), overflow menu (three dots, KEEP, one
  item "Battery usage").
- **Display**: rows (top to bottom): Brightness level (summary "25%"), Night Light, Adaptive brightness, Wallpaper, Dark theme (switch), Screen timeout ("After 1
  minute of inactivity"), Auto-rotate screen (switch), Font size ("Default"), Display size ("Default"), Lock screen display. WANTED only: Brightness level,
  Screen timeout, Auto-rotate screen, Font size, Display size, in that order. No icons on this page; title x 77 px.
- **Sound**: four volume rows with a slider each and an icon at the left (music note: "Media volume"; phone: "Call volume"; alarm clock: "Alarm volume"; bell:
  "Notification volume"); slider thumb teal, active track teal, inactive track light gray; divider; Do Not Disturb (REMOVE), Shortcut to prevent ringing
  (REMOVE), "Default notification sound" (summary = the sound's title, e.g. On The Hunt), "Default alarm sound" (e.g. Cesium); divider; heading "OTHER SOUNDS AND
  VIBRATIONS" (Tj: drop the words "and vibrations", so "OTHER SOUNDS"); switches "Screen locking sounds" (on), "Charging sounds and vibration" (on; Tj named only
  "the and vibrations text", so keep this row as shown and tell him), "Touch sounds" (on); "Sound enhancement" (REMOVE).
- **Storage**: gray header, big "30.28" (gray, large) with a small "GB", "Used of 32 GB" under it, a teal "FREE UP SPACE" button (REMOVE), a teal ring at the right
  with "95%" and "used" inside (ring about 115 px, teal arc for the used part, pale teal for the rest). Divider. Then seven rows, each with an icon at the left, the
  name, the size right-aligned in gray and a thin progress bar under the name (teal fill over a light gray track, full width from x 77 to 583): Photos & videos
  3.3 GB, Music & audio 0.54 GB, Games 3.0 GB, Movie & TV apps 0.00 GB, Other apps 3.7 GB, Files 15 GB, System 4.3 GB. Row pitch 84 px.
- **Accessibility** (headings in teal capitals): Volume key shortcut (REMOVE); DOWNLOADED SERVICES with our own accessibility helper (REMOVE); SCREEN READERS:
  "Text-to-speech output" (no summary); DISPLAY: "Font size / Default", "Display size / Default", "Dark theme" (switch, REMOVE), "Magnification / Off" (a pink pentagon
  icon), "Color correction" (REMOVE), "Color inversion" (REMOVE), "Large mouse pointer" (REMOVE), "Remove animations" (REMOVE); INTERACTION CONTROLS: "Dwell timing"
  (REMOVE), "Auto-rotate screen" (REMOVE), "Touch & hold delay / Short" (the text on screen has an ampersand), "Time to take action" (REMOVE), "Vibration & haptic strength"
  (REMOVE); AUDIO & ON-SCREEN TEXT: "Mono audio / Combine channels when playing audio" (switch), "Audio balance" (slider with the labels Left and Right under it, thumb
  in the middle, the slider is inset 16 dp more at both ends than the volume sliders), "Caption preferences" (REMOVE); EXPERIMENTAL: "High contrast text" (REMOVE).
  WANTED, with their headings and in this order: SCREEN READERS (Text-to-speech output), DISPLAY (Font size, Display size, Magnification), INTERACTION CONTROLS (Touch &
  hold delay), AUDIO & ON-SCREEN TEXT (Mono audio, Audio balance).
- **About tablet**: a gray owner header (a teal round user icon and the word "Owner": REMOVE, Tj said so), then groups: [Device name, Emergency information] (REMOVE),
  divider, [Legal information; Model & hardware / "Model: <Build.MODEL>"; Android version / "10"], divider, [IP address, Wi-Fi MAC address, Bluetooth address, Up time]
  (REMOVE; those four are shown in gray = disabled style), divider, [Custom build version (on the real device: the vendor build string, Tj wants the TEXT
  "vnd_tb8168p1_bsp 1779111601"); Build number (REMOVE); Software updates (REMOVE)]. WANTED: Legal information, Model & hardware, Android version, Custom build version.
- Two of the 18 uploaded files are NOT Settings (a chat screenshot of the dead session, twice).

## 2. Where the stock source is (Tj allowed the download)

AOSP Settings, Android 10: `https://github.com/aosp-mirror/platform_packages_apps_settings`, tag `android-10.0.0_r47` (git over the proxy works; a plain
`git clone --depth 1 --branch android-10.0.0_r47 <url> <new-empty-dir>`; a Bash command with `rm -rf` in it is BLOCKED by a plugin hook: use a fresh directory name).
2026-11-09: cloned into the session scratchpad (`aosp/settings10/src`); the scratchpad does not survive a new session, clone again (about 1 minute).
Useful tags to read first: `res/layout/preference_*`, `res/values/dimens.xml`, `styles.xml`, `res/xml/{power_usage_summary,display_settings,sound_settings,storage_dashboard_fragment,accessibility_settings,system_dashboard_fragment,device_info_settings}.xml`.

## 3. The launcher map (BN1a, read-only subagent, 2026-11-09; paths relative to the repo; T = app/src/main/java/com/tj/launcher, L3 = launcher3/src/com/android/launcher3)

- **Drawer list**: `L3/model/LoaderTask.java:800-818` (`getActivityList`) -> `L3/AllAppsList.java:77` `mAppFilter.shouldShowApp` -> `L3/AppFilter.java:20` -> `T/HiddenApps.isShown` -> `T/AppVisibility.java:27-28`:
  own package: `className != null && ALWAYS_SHOWN_OWN_COMPONENTS.contains(className)`; any other package: shown iff it is in the allowed set (prefs `tj_apps`). The set
  `ALWAYS_SHOWN_OWN_COMPONENTS` (`AppVisibility.java:13`) is EMPTY and its comment says "later: the Settings clone": the hook for this job already exists.
  The real Settings (`com.android.settings`) has no special case today (shown iff chosen in the Quickstep-only app chooser; the emulator seeds it allowed, `tools/emulator_scenario.sh:175`).
- **Launch**: `L3/AppInfo.java:96-100` builds `ACTION_MAIN` + `CATEGORY_LAUNCHER` + component + `NEW_TASK | RESET_TASK_IF_NEEDED`; click path `L3/touch/ItemClickHandler.java:106` -> `BaseDraggingActivity.java:151-170`.
  Drawer entries come ONLY from `LauncherActivityInfo` (a component with a LAUNCHER intent filter): so the spoof needs one, and the STOCK Quickstep drawer would list it too.
- **Manifest / build**: compileSdk 36, minSdk 29, **targetSdk 28**, R8 off, application theme `AppTheme`; permissions already declared: WRITE_SECURE_SETTINGS, WRITE_SETTINGS, CHANGE_CONFIGURATION,
  PACKAGE_USAGE_STATS, DUMP, KILL_BACKGROUND_PROCESSES, SET_WALLPAPER, READ_EXTERNAL_STORAGE ... (adb grants documented in `research/ADB_COMMANDS.txt` lines 24, 52, 54; in-app guide
  `T/AdbGuideActivity.java`, Quickstep only). Dependencies already in the APK: `androidx.preference:preference:1.1.1` (pulls appcompat 1.1.0), recyclerview 1.3.2, dynamicanimation,
  `:iconloaderlib` (androidx.core): **no new dependency is needed** (BRIEF.md line 13 "Kotlin stdlib only" is stale). `app/src/main/res` has only `values/tj_strings.xml`, `xml/tj_accessibility.xml`
  and two vectors (no layouts or styles yet); the pool in `launcher3/res` has `BaseLauncherTheme` (parent `Theme.DeviceDefault.Light`). Own screens so far are code-built plain views.
- **Settings.System / Secure writers today**: only `T/EntranceWatcher.java:79` writes Settings.System (ACCELEROMETER_ROTATION, needs the WRITE_SETTINGS appop); the Secure / Global writers
  (`BatterySaverGuard`, `LockQuiet`, `NotifQuiet`, `AnimationSpeedup`, `KeyboardSwitch`, ...) each check `checkSelfPermission(WRITE_SECURE_SETTINGS)` themselves. No code exists for brightness,
  screen timeout, font scale, volume or storage. Grant checks to copy: `AdbGuideActivity.java:66-88`, `SetupActivity.java:135-155`.
- **Recents / sweep**: tasks ROOTED in `com.tj.launcher` are never swept (`ResetPolicy.shouldKill` returns false for the own package; `RecentsSweep.java:83`); own windows count as "home"
  (`TjAccessibilityService.java:2086`); the manifest `excludeFromRecents` plus `RecentsGuard.excludeOurTasks` keep Control / Setup / HiddenApps out of Recents today. A spoof activity inside this APK
  is therefore safe from the sweep; whether it SHOULD show in Recents like the real Settings does is a design choice (decision below).
- **Emulator scenario**: `tools/emulator_scenario.sh`, 49 sections written as `if want <name>; then # section <name>` (`want()` line 72); `emulator.yml` takes `sections`; each section is
  listed in `tools/gate-map.txt` (`section :: fixed waits :: tier :: path regex :: content regex :: protects`); `tools/gate.sh --check` and `tools/test_handoff.sh` keep both in sync.
  Measuring helpers: `tools/uinode.py` (uiautomator dump -> bounds), `pb_ui.py` (text + bounds), `pb_probe.py` (edges in 600x1024 PNGs). CI screenshots are an artifact (`emulator-screenshots`,
  14 days) that a hook stops us from downloading: so far only text results reach Claude.
- **Tests**: JVM only (JUnit 4.13.2, no Robolectric); pattern = a final class with a private constructor and static pure methods (e.g. `HomeDoublePress` + `HomeDoublePressTest`).
- **Older plan** (`research/SETTINGS_CLONE_2026-11-02.md`): the REAL Settings cannot be cloned (system UID, platform key, 2,917 files, hidden APIs): a "front door" that opens real pages was the
  earlier idea. Tj's BN1 request is different and feasible: re-implement ONLY seven pages with public APIs. `HANDOFF.md` line 212 held SET1 back "until his go-ahead and his inputs": BN1 is both.

## 3b. Decisions taken for BN1 (Claude, 2026-11-09; Tj can veto any of them)

1. **Delivery: activities inside the Launcher APK**, one theme, no new dependency, no second APK.
2. **Entry in the drawer**: an `activity-alias` named like the real app (label "Settings", the real icon) carrying the LAUNCHER filter; its class name goes into `ALWAYS_SHOWN_OWN_COMPONENTS`; the real
   `com.android.settings` stays out of the custom drawer. The alias is ENABLED only in custom mode and DISABLED in Quickstep mode (`PackageManager.setComponentEnabledSetting`, DONT_KILL_APP), so
   the stock drawer keeps its single real Settings. The flip is level-triggered (process start + `Mode.set`) and runs OFF the unlock path, a few seconds late (the unlock path must stay fast, BK1).
3. **One page activity** (`SsPageActivity`, extras = page id) on `Theme.DeviceDefault.Settings` + the Settings overlay (`androidx.preference` rows as in the real app) so rows, headings, switches,
   dialogs and the toolbar are the platform's own; the special rows (battery header, volume / balance sliders, storage header, home icons) are small custom views.
4. **Strings**: exact English strings from the AOSP source (or Tj's screenshots where the tablet differs, e.g. "Touch & hold delay", "Screen usage since full charge").
5. **Recents**: the spoof appears in Recents like the real Settings (task label "Settings", the real icon via TaskDescription); it is never swept (own package).
6. **Seeing the result**: add a CI step that pushes the emulator screenshots of section `ss` to an orphan branch (`ci-shots`) so Claude can fetch and LOOK at them next to Tj's screenshots
   (numbers alone cannot show a missing icon). No personal data is on the emulator.

## 3c. Still open / to ask Tj (only if the answer changes the work)

- Languages & input page content, Magnification, Touch & hold delay, Text-to-speech, Legal information, Model & hardware and Battery usage pages were not in his screenshots: built from the
  AOSP 10 source; he gets the list to compare and can send screenshots.
- "Charging sounds and vibration" on the Sound page keeps its words (he asked to drop only the words "and vibrations" = the heading).
- The home page summaries ("Wallpaper, sleep, font size" ...) stay as stock; he can change them.

## 4. Measured numbers to match (px at 170 dpi unless noted) — to be completed from the AOSP source (dp values) and the CI emulator

| Item | Tj's screenshot | AOSP 10 source (dp) | Spoof on emulator |
| :- | :- | :- | :- |
| status bar height | 26 px | | |
| toolbar height | 60 px (26..86) | | |
| title x | 77 px | | |
| two-line row pitch | 87 px | | |
| one-line row pitch | 62 px | | |
| Battery header height | 257 px (26..283) | | |
| Storage row pitch | 84 px | | |

### 4b. Numbers measured with PIL from Tj's screenshots (2026-11-09; px at 170 dpi, font scale 1.2 = tablet report "600x973 @170 dpi, font scale 1.2")

- Colours: status bar #E0E0E0, toolbar #F5F5F5, page #FAFAFA, navigation bar #FCFCFC with a #DCDCDC line on its top edge (y 973); toolbar shadow y 86 #E3E3E3, 87 #EEEEEE, 88 #F3F3F3, 89 #F6F6F6, 90 #F8F8F8, 91 #F9F9F9, 92 #FAFAFA (a 4 dp elevation shadow).
- Geometry: status bar y 0-25 (26 px), toolbar y 26-85 (60 px = 56 dp), app window y 26-972 (947 px = the tablet report's 973 incl. status bar), nav bar y 973-1023.
- Display page rows (the standard preference row): rows start at y 87; two-line row = 87 px (title ascender top at row+23, summary digit top at row+51); one-line row = 62 px;
  title text 19 px from ascender to descender (about 20.4 px = 16 sp x 1.275), summary digits 13 px high (about 17.85 px = 14 sp x 1.275). This equals the AndroidX / framework
  preference row: padding 16 dp top and bottom, title 16 sp, summary 14 sp directly below it, `includeFontPadding` default (Roboto top -1.056, bottom +0.271 em).
- Settings home icons: 38 x 38 px squares with small rounded corners at x 17-54, one per row, pitch 87 px; fills: Battery #258982, Display #FFA727, Sound #01B1AF,
  Storage #C14CE6, Accessibility #5011C1, System #9E9E9E, About tablet #6F86DA (others, not needed: Network #2196F3, Connected devices #72B70F, Apps #FF7E0F, Privacy #5E97F6,
  Location #2EC7DC, Security #0F9D58, Accounts #F15B8D, DuraSpeed #1A73E8).
- The platform theme that gives all of this for free: `Theme.DeviceDefault.Settings` (framework `themes_device_defaults.xml` line 1452: light status bar, `ic_ab_back_material_settings`,
  solid action bar, white navigation bar with divider, `colorListDivider`). Its resource id can be looked up by name at run time (`getIdentifier("Theme.DeviceDefault.Settings", "style", "android")`).

### 4c. Theme facts read from the AOSP 10 source (r47)

- `android.R.style.Theme_DeviceDefault_Settings` is PUBLIC API since level 21 (`api-versions.xml`), so `parent="@android:style/Theme.DeviceDefault.Settings"` compiles and gives the
  tablet's own colours / toolbar / switch / dialogs. Settings' own `Theme.Settings` adds on top: `android:listPreferredItemHeight` 72dip, `android:homeAsUpIndicator` = its own
  24 dp vector `ic_arrow_back` (path `M20,11H7.83l5.59-5.59L12,4l-8,8l8,8l1.41-1.41L7.83,13H20V11z`, tint `colorControlNormal`, autoMirrored), `android:navigationBarColor` transparent,
  `android:actionBarStyle` = `Widget.ActionBar` = `Widget.DeviceDefault.Light.ActionBar.Solid` + `contentInsetStart` 16 dp.
- Page frame (`settings_base_layout.xml`): a vertical LinearLayout with `fitsSystemWindows`, a `Toolbar` (`style=?attr/actionBarStyle`, `theme=?attr/actionBarTheme`), then the content.
- Category heading: `TextAppearance.CategoryTitle` = `TextAppearance.DeviceDefault.Body2` + all caps + 11 sp + letter spacing 0.0727 (0.8 / 11).
- Battery header icon colour measured #008577 (the DeviceDefault light accent); the big icon is 70 x 106 px at x 390-459, y 136-241; "84" digits 36 sp (`battery_header.xml`:
  a 170 dp wide text column with 72 dp start margin, percent text `textSize` 36 sp, margin top 12 dp, `summary1` min 2 lines margin top 8 dp).
- Volume slider row (`preference_volume_slider.xml`): icon frame min width 44 dp, icon 24 dp, title padding start 12 dp, row margins top/bottom 8 dp; measured: track 2 px
  thick, spans x 77-565, thumb 11 px, inactive track #D7D7D7, active #008577; dividers on the Battery page are 1 px #DADCE0.
- Storage row (`storage_item.xml`): padding top/bottom 14 dp, icon frame min width 56 dp (icon 24 dp), title + the size on the right (`layout_alignParentEnd`), progress bar 8 dp
  high with top margin 12 dp and bottom margin 4 dp under the title (the screenshot shows the visible bar about 5 px thick: the theme's progress drawable).

## 5. Hard facts about this container (so nobody re-derives them)

- No `/dev/kvm`, no emulator, no system image here: the look can only be checked on the CI emulator (`emulator.yml`) and ONLY through numbers (RESULTS block), no pictures.
- `rm -rf` inside a Bash command is refused by a plugin hook.

## 6. The last five screenshots (read 2026-11-09; ALL 18 uploads are now read: 16 Settings + 2 chat screenshots)

- **Settings home** (2 screenshots, top and bottom of the scroll): a search card at the top (white card y 43-95 px, teal magnifier at x 44, hint "Search settings": REMOVE),
  then rows with a 38 px rounded-square coloured icon (x 17-55) and title at x 77, pitch 87 px, in this stock order and with these stock summaries:
  Network & internet (Wi-Fi, data usage, and hotspot; blue), Connected devices (Bluetooth; green), Apps & notifications (Recent apps, default apps; orange),
  **Battery** ("90% - Should last until about 2:00 PM"; dark teal), **Display** ("Wallpaper, sleep, font size"; amber), **Sound** ("Volume, vibration, Do Not Disturb";
  teal), **Storage** ("95% used - 1.57 GB free"; purple), Privacy (Permissions, account activity, personal data; periwinkle), Location (On - 4 apps have access to
  location; cyan), Security (Screen lock; green), Accounts (e.g. Telegram; pink), **Accessibility** ("Screen readers, display, interaction controls"; deep purple),
  DuraSpeed (an OEM row with no summary; blue), **System** ("Languages, gestures, time, backup"; gray), **About tablet** ("711" = the model; indigo). Bold = the seven
  Tj wants, in his order (the stock order already is Battery, Display, Sound, Storage, Accessibility, System, About tablet). No title bar on the home page (the search card
  is where it would be); the spoof has NO search card, so the first row starts right under the status bar area (the exact top padding is in the AOSP layout).
- **System page** (stock): Languages & input (summary "Android Keyboard (AOSP)", globe icon), Gestures, Date & time (GMT-05:00 Eastern Standard Time, clock icon),
  Backup (Off, cloud icon), Reset options (Network, apps, or device can be reset, history icon); small gray 24 dp line icons at x 30. WANTED: only Languages & input.
  **Not shown by Tj: the Languages & input page itself** (and none of Magnification, Touch & hold delay, Text-to-speech, Font size, Display size, Battery usage, Legal
  information, Model & hardware): those are built from the AOSP 10 layouts and strings, and Tj gets the list of them to compare.
- **Battery**, two more screenshots of the same page with other values: "86 %" "Should last until about 9:45 AM" / "10:00 AM"; "Last full charge" shows
  "109 minutes ago" / "113 minutes ago" / "2 hours ago"; "Screen usage since full charge" shows "59 min" / "1 hr, 3 min" / "1 hr, 16 min". (The estimate and the
  history figures come from Android's own battery statistics.) The row label on screen is "Screen usage since full charge" (Tj wrote "since last full charge").

## 8. The SECOND batch of screenshots (Tj, 2026-11-09 02:47 UTC; 5 more files; words + numbers only)

Tj's words: "The model and hardware setting should look like the photo, but it can't be opened. The languages and input page has everything except autofill service and personal
dictionary, and there is a last setting on the bottom called Text-to-speech output". (Claude's reading is in TASKS BN1i: Model & hardware = the About-page row as shown, a tap does nothing.)

- **Languages & input** (stock, 9:41): rows: "Languages" (a translate icon at x 30, summary "English (United States)"), divider, heading KEYBOARDS: "Virtual keyboard / Android Keyboard (AOSP)",
  "Physical keyboard / Not connected"; divider; heading TOOLS: "Spell checker / Android Spell Checker (AOSP)", "Autofill service" (REMOVE), "Personal dictionary" (REMOVE); divider; "Pointer speed";
  and (Tj) a LAST row "Text-to-speech output" below it. Measured y (px): Languages title 110-129, summary 136-154, divider 173, heading KEYBOARDS cap 204-214, Virtual keyboard 251-270 / 277-295,
  Physical keyboard 338-357 / 365-379, divider 401, TOOLS 432-442, Spell checker 479-497 / 505-523, Autofill 565-581, Personal dictionary 628-647, divider 666, Pointer speed 690-708.
  Dividers are 1 px #DADCE0 full width. (In the spoof, after removing the two rows the same rhythm continues: Spell checker, then the divider, Pointer speed, Text-to-speech output.)
- **Text-to-speech output** (stock, state with NO engine installed: "Preferred engine" has no summary, the rest is disabled): rows: "Preferred engine" (enabled, #202020, title only, y 109-129);
  "Language" (disabled #B9B9B9, summary "Sets the language-specific voice for the spoken text", y 173-190 / 199-216); divider y 235; "Speech rate" (disabled, title y 274-292) with a slider under it
  (y 315-323, track #D7D7D7 from x 77 to 569, DISABLED thumb = a hollow ring about 9 px at x 152); "Pitch" (title y 365-379, slider y 406-414, hollow thumb at x 175); divider y 437; a two-button bar
  at y 471-484: "PLAY" (disabled #D7D7D7, centred at x 150) and "RESET" (enabled, teal #008577, centred at x 447). Search icon in the toolbar (REMOVE).
- **Touch & hold delay** dialog (over the Accessibility page): the whole window is dimmed 60 % black (page #FAFAFA -> #646464, toolbar #F5F5F5 -> #626262, status bar #E0E0E0 -> #5A5A5A; the
  navigation bar stays #FFFFFF); dialog box x 77-521 (445 px), y 359-638 (280 px), colour #FAFAFA; title "Touch & hold delay" (text y 386-410); three radio rows "Short" (selected, teal #008577
  ring + dot), "Medium", "Long" (text y 435-456, 486-507, 537-560: pitch 51 px = 48 dp); a "CANCEL" text button (teal, y 601-614, right aligned). Summary of the row on the Accessibility page: "Short".
- **Magnification**: two rows "Magnify with triple-tap / Off" and "Magnify with shortcut / Off" (standard 87 px rows, titles y 109-129 and 196-216). Search icon (REMOVE).
- **Legal information**: "Third-party licenses" (one line, y 110-129), "System WebView licenses" (one line, y 172-191), "Wallpapers" (title y 234-252, a two-line summary "Satellite imagery
  providers:" / "(c)2014 CNES / Astrium, DigitalGlobe, Bluesky", y 261-299). Search icon (REMOVE).
- **Model & hardware**: no new picture; the About screenshot shows "Model & hardware / Model: 711". Tj: it "can't be opened" on the stock tablet, so the spoof row looks the same and is not clickable.

## 7. Where each stock number comes from (to implement "functions identically")

Filled in as the AOSP source is read (`PowerUsageSummary`, `BatteryUtils.calculateLastFullChargeTime`, `StorageSummaryDonutPreference`, `AudioSummaryPreference`, ...).
