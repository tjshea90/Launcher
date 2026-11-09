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

## 3. Open (fill in as it is learned; see TASKS BN1a-BN1c)

- Delivery shape chosen (unless the map shows a blocker): activities inside the Launcher APK, reached from the custom drawer.
- What the Launcher already holds (adb grants) and how the drawer treats the real Settings: BN1a map (subagent), then written here.

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

## 7. Where each stock number comes from (to implement "functions identically")

Filled in as the AOSP source is read (`PowerUsageSummary`, `BatteryUtils.calculateLastFullChargeTime`, `StorageSummaryDonutPreference`, `AudioSummaryPreference`, ...).
