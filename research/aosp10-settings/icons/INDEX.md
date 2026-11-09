# AOSP 10 Settings icons: index (android-10.0.0_r47)

Read-only lookup for Launcher task BN1 (re-create the stock Settings look). Every `.xml` in this folder is a byte-for-byte copy
(`cp -p`, checked with `cmp`) of the file in the "Source path" column; nothing was retyped. File names are the originals; each name
exists exactly once across both clones, so no module prefix was needed. Anything that is not in the clones is marked **NOT FOUND**, never guessed.
PNG, bitmap-wrapper and state-list drawables are marked as such; PNGs are given by path only (not copied).

Other files here: `VALUES_EXCERPTS.md` (verbatim dimen, colour, string and style excerpts with line numbers: tile sizes and colours, theme colour chain,
battery strings, donut dimens, toolbar dimens) and `BATTERY_METER.md` (the big battery icon: literal path strings and drawing rules).

## Path roots (all paths in this folder are relative on purpose)

- `settings10/` = root of the AOSP 10 Settings clone (`res/`, `src/`, `AndroidManifest.xml`).
- `fwbase/` = root of the frameworks subset (`packages/SettingsLib/...`, `core/res/res/layout`, `core/res/res/values`).
  The framework drawables (`core/res/res/drawable*`) and colour state lists (`core/res/res/color`) are **not** in it.

## Colour legend (light tablet, Theme.Settings)

Theme chain: Theme.Settings -> Theme.SettingsBase -> android:Theme.DeviceDefault.Settings -> Theme.DeviceDefault.Light -> Theme.Material.Light
(excerpts in `VALUES_EXCERPTS.md`, section B).

| Name | Resolves to | Literal | Source |
| :- | :- | :- | :- |
| colorControlNormal in page content (CN) | `?textColorSecondary` = `secondary_text_default_material_light` | **#8A000000** (disabled, alpha 0.26: #42000000) | `fwbase/core/res/res/values/colors_material.xml` line 61; `themes_material.xml` line 781 |
| colorControlNormal inside the toolbar (CN-bar) | `?textColorPrimary` = `primary_text_default_material_light` (toolbar theme overlay) | **#DE000000** | `colors_material.xml` line 59; `themes_material.xml` lines 887-890 |
| colorAccent (ACC) | accent_device_default_light -> accent_material_light -> material_deep_teal_500 | **#FF008577** | `colors_material.xml` line 100 |
| colorError | error_color_device_default_light -> error_color_material_light | **#FFFF5722** | `colors_material.xml` line 48 |
| colorForeground | foreground_material_light = black | **#FF000000** | `colors_material.xml` line 20 |
| white / black | `@android:color/white` / `@android:color/black` | #FFFFFFFF / #FF000000 | `fwbase/core/res/res/values/colors.xml` lines 37, 39 |
| page background (colorBackground) | background_device_default_light -> background_material_light -> material_grey_50 | #FFFAFAFA | `colors_device_defaults.xml` line 41 |
| toolbar background (colorPrimary) / status bar (colorPrimaryDark) | material_grey_100 / material_grey_300 | #FFF5F5F5 / #FFE0E0E0 | `colors_material.xml` lines 30, 35, 92-95 |

Tint rule: a vector's `android:tint` is applied SRC_IN, so with an opaque fill the drawn colour equals the tint colour. Over the page background CN
composites to about #737373 and #DE000000 to about #202020 (matches the text colours measured on the tablet). The state-list files behind
`textColorPrimary` / `textColorSecondary` (`core/res/res/color`) are not in the subset: the enabled colours above come from the base colours in
`colors_material.xml` (lines 59, 61), the disabled alpha from line 74.

dp to px at the tablet (600 x 1024 px, 170 dpi, scale 1.0625; rounded): 16 dp = 17 px, 24 dp = 26 px, 32 dp = 34 px, 36 dp = 38 px,
48 dp = 51 px, 66 dp = 70 px, 100 dp = 106 px.

Column "Fill / tint": **fill** = the vector's own `fillColor`; **tint** = the `android:tint` the file carries; **drawn** = the result on the light tablet.

---

## 1. Home page tiles (Battery, Display, Sound, Storage, Accessibility, System, About tablet)

**Composition.** Every tile is a layer-list (XML copied):
- Item 1 = `com.android.settingslib.widget.AdaptiveIconShapeDrawable` (class: `fwbase/packages/SettingsLib/AdaptiveIcon/src/com/android/settingslib/widget/AdaptiveIconShapeDrawable.java`,
  lines 53-58): a ShapeDrawable whose PathShape is built from the string `config_icon_mask` on a 100 x 100 viewport (the path coordinates run 0..100). Size
  `@dimen/dashboard_tile_image_size` = **36 dp**; fill = the item's `android:color` (`@color/homepage_*_background`).
- Item 2 = the glyph, **24 dp** (`dashboard_tile_foreground_image_size`), start and top inset **6 dp** (`dashboard_tile_foreground_image_inset`).
  Dimens: `settings10/res/values/dimens.xml` lines 100, 103, 106. The glyphs are white and are shown untinted.
- Mask string (`fwbase/core/res/res/values/config.xml` line 3569; the XML quotes are consumed by aapt):
  `M50,0L92,0C96.42,0 100,4.58 100 8L100,92C100, 96.42 96.42 100 92 100L8 100C4.58, 100 0 96.42 0 92L0 8 C 0 4.42 4.42 0 8 0L50 0Z`
  = a rounded square, corner radius 8 of 100 units = 2.88 dp at 36 dp (about 3.06 px at 170 dpi). The tile measured on the tablet is 38 x 38 px = 36 dp, which fits the default
  mask (a vendor overlay of `config_icon_mask` on the tablet cannot be excluded from the clones alone).
- `TopLevelSettings.shouldForceRoundedIcon()` reads `config_force_rounded_icon_TopLevelSettings` (true, `settings10/res/values/config.xml` line 202); it only rounds tiles injected by
  other packages, not these seven static ones.
- Colours: `settings10/res/values/colors.xml` lines 84-100 (all categories in `VALUES_EXCERPTS.md`, section A). `values-night` changes only Accessibility (#783BE5) and Support (#3F5FBD): not used by the light tablet.

**AOSP colour against the tablet colour: identical for all seven.**

| Tile | Glyph | Background colour in AOSP | Tablet colour | Same |
| :- | :- | :- | :- | :- |
| Battery | `ic_settings_battery_white` | #FF258982 (`homepage_battery_background`) | #258982 | yes |
| Display | `ic_settings_display_white` | #FFFFA727 (`homepage_display_background`) | #FFA727 | yes |
| Sound | `ic_volume_up_24dp` | #FF01B1AF (`homepage_sound_background`) | #01B1AF | yes |
| Storage | `ic_storage_white` | #FFC14CE6 (`homepage_storage_background`) | #C14CE6 | yes |
| Accessibility | `ic_settings_accessibility` | #FF5011C1 (`homepage_accessibility_background`) | #5011C1 | yes |
| System | `ic_settings_system_dashboard_white` | #FF9E9E9E (`homepage_system_background`) | #9E9E9E | yes |
| About tablet | `ic_phone_info` | #FF6F86DA (`homepage_about_background`) | #6F86DA | yes |

| Purpose | File | Source path | Size (dp) | Viewport | Fill / tint (resolved) | Notes |
| :- | :- | :- | :- | :- | :- | :- |
| Tile Battery: shape + glyph | `ic_homepage_battery.xml` | `settings10/res/drawable/ic_homepage_battery.xml` | shape 36 x 36; glyph 24 x 24 at start/top 6 | shape: 100 x 100 mask; glyph: 24 x 24 | shape fill #FF258982; tablet #FF258982 (same); no tint | layer-list (XML copied). Row `top_level_battery`, `settings10/res/xml/top_level_settings.xml` line 53 |
| Tile Display: shape + glyph | `ic_homepage_display.xml` | `settings10/res/drawable/ic_homepage_display.xml` | shape 36 x 36; glyph 24 x 24 at 6 / 6 | 100 x 100 mask; 24 x 24 | shape fill #FFFFA727; tablet #FFFFA727 (same); no tint | layer-list. `top_level_settings.xml` line 62 |
| Tile Sound: shape + glyph | `ic_homepage_sound.xml` | `settings10/res/drawable/ic_homepage_sound.xml` | shape 36 x 36; glyph 24 x 24 at 6 / 6 | 100 x 100 mask; 24 x 24 | shape fill #FF01B1AF; tablet #FF01B1AF (same); no tint | layer-list. `top_level_settings.xml` line 71 |
| Tile Storage: shape + glyph | `ic_homepage_storage.xml` | `settings10/res/drawable/ic_homepage_storage.xml` | shape 36 x 36; glyph 24 x 24 at 6 / 6 | 100 x 100 mask; 24 x 24 | shape fill #FFC14CE6; tablet #FFC14CE6 (same); no tint | layer-list. `top_level_settings.xml` line 79 |
| Tile Accessibility: shape + glyph | `ic_homepage_accessibility.xml` | `settings10/res/drawable/ic_homepage_accessibility.xml` | shape 36 x 36; glyph 24 x 24 at 6 / 6 | 100 x 100 mask; 24 x 24 | shape fill #FF5011C1; tablet #FF5011C1 (same); no tint | layer-list. `top_level_settings.xml` line 123 |
| Tile System: shape + glyph | `ic_homepage_system_dashboard.xml` | `settings10/res/drawable/ic_homepage_system_dashboard.xml` | shape 36 x 36; glyph 24 x 24 at 6 / 6 | 100 x 100 mask; 24 x 24 | shape fill #FF9E9E9E; tablet #FF9E9E9E (same); no tint | layer-list. `top_level_settings.xml` line 132 |
| Tile About tablet: shape + glyph | `ic_homepage_about.xml` | `settings10/res/drawable/ic_homepage_about.xml` | shape 36 x 36; glyph 24 x 24 at 6 / 6 | 100 x 100 mask; 24 x 24 | shape fill #FF6F86DA; tablet #FF6F86DA (same); no tint | layer-list. `top_level_settings.xml` line 140 |
| Battery tile glyph | `ic_settings_battery_white.xml` | `settings10/res/drawable/ic_settings_battery_white.xml` | 24 x 24 | 24 x 24 | fill #FFFFFFFF; no tint; drawn #FFFFFFFF | 1 path |
| Display tile glyph | `ic_settings_display_white.xml` | `settings10/res/drawable/ic_settings_display_white.xml` | 24 x 24 | 24 x 24 | fill #FFFFFFFF; no tint; drawn #FFFFFFFF | 1 path (not the same file as `ic_settings_display.xml`, section 10) |
| Sound tile glyph | `ic_volume_up_24dp.xml` | `settings10/res/drawable/ic_volume_up_24dp.xml` | 24 x 24 | 24 x 24 | fill #FFFFFFFF; no tint; drawn #FFFFFFFF | 3 paths |
| Storage tile glyph | `ic_storage_white.xml` | `settings10/res/drawable/ic_storage_white.xml` | 24 x 24 | 24 x 24 | fill #FFFFFFFF; no tint; drawn #FFFFFFFF | 1 path (not the same file as `ic_storage.xml`, section 4) |
| Accessibility tile glyph | `ic_settings_accessibility.xml` | `settings10/res/drawable/ic_settings_accessibility.xml` | 24 x 24 | 24 x 24 | fill #FFFFFFFF; no tint; drawn #FFFFFFFF | 2 paths |
| System tile glyph | `ic_settings_system_dashboard_white.xml` | `settings10/res/drawable/ic_settings_system_dashboard_white.xml` | 24 x 24 | 24 x 24 | fill #FFFFFFFF; no tint; drawn #FFFFFFFF | 3 paths (a circle with an i) |
| About tablet tile glyph | `ic_phone_info.xml` | `settings10/res/drawable/ic_phone_info.xml` | 24 x 24 | 24 x 24 | fill #FFFFFFFF; no tint; drawn #FFFFFFFF | 1 path; the layer-list names this one glyph for every device (no tablet variant in the clone) |

---

## 2. Battery page

Page layout facts for the header and toolbar are in `BATTERY_METER.md` (section 5) and section 7 below.

| Purpose | File | Source path | Size (dp) | Viewport | Fill / tint (resolved) | Notes |
| :- | :- | :- | :- | :- | :- | :- |
| Footer info icon (left of "Battery usage data is approximate ...") | `ic_info_outline_24.xml` | `fwbase/packages/SettingsLib/res/drawable/ic_info_outline_24.xml` | 24 x 24 | 24 x 24 | fill #FF000000; tint CN #8A000000; drawn #8A000000 | Set by `FooterPreference.init()`, `fwbase/packages/SettingsLib/src/com/android/settingslib/widget/FooterPreference.java` line 59. NOT the Settings-app file `ic_info_outline_24dp` (different file, not used here). Layout `fwbase/packages/SettingsLib/res/layout/preference_footer.xml`: icon_frame minWidth 56 dp, gravity start/top, paddingEnd 12 dp, paddingTop 16 dp, paddingBottom 4 dp; title (LinkTextView) padding 16 dp top and bottom, colour textColorSecondary #8A000000. Text set at `settings10/src/com/android/settings/fuelgauge/PowerUsageSummary.java` line 233 |
| Toolbar overflow (three dots) | **NOT FOUND** (framework drawable `ic_menu_moreoverflow_material`) | none in the clones: the framework `core/res/res/drawable*` tree is not in the subset | not in clones | not in clones | not available; the toolbar's CN-bar is #DE000000 | Named by `Widget.Material.ActionButton.Overflow`, `fwbase/core/res/res/values/styles_material.xml` line 967 (`src`). Chain: `actionOverflowButtonStyle` (`themes_device_defaults.xml` line 910) -> `Widget.DeviceDefault.Light.ActionButton.Overflow` (`styles_device_defaults.xml` line 220) -> `Widget.Material.Light.ActionButton.Overflow` (`styles_material.xml` line 1201) -> line 967. Button: minWidth 36 dp, minHeight 48 dp, paddingStart 6 dp, paddingEnd 10 dp (`dimens_material.xml` lines 62-69). Settings' own attr `ic_menu_moreoverflow` (`settings10/res/values/themes.xml` line 38 = framework `ic_menu_moreoverflow_holo_dark`) is defined but not used. A grep for the three-dot path found only `ic_storage_apps.xml`, a 3 x 3 dots icon |
| Big battery icon (header) | none (drawn in code) | `fwbase/packages/SettingsLib/src/com/android/settingslib/graph/ThemedBatteryDrawable.kt`; `settings10/src/com/android/settings/fuelgauge/BatteryMeterView.java`; strings `fwbase/core/res/res/values/config.xml` lines 4051-4070 | 66 x 100 intrinsic (view: match_parent x 100) | 12 x 20 grid | normal #FF008577; level < 5: #8AFF5722; power save: #FF000000 with #FFFF5722 ring and plus | The four literal path strings and the drawing rules are in `BATTERY_METER.md`. Tablet: 70 x 106 px at x 390-459, y 136-241 |
| Colour list for the level < 5 filter | `battery_icon_color_error.xml` | `settings10/res/color/battery_icon_color_error.xml` | n/a | n/a | colorError #FFFF5722 at `secondaryContentAlpha` 0.54: #8AFF5722 | state list (XML copied). Used by `BatteryMeterView` with SRC_IN |
| Colour list for the power-save ring and plus | `batterymeter_plus_color.xml` | `fwbase/packages/SettingsLib/res/color/batterymeter_plus_color.xml` | n/a | n/a | colorError: #FFFF5722 | state list. Read by `ThemedBatteryDrawable` for `errorPaint` |
| Colour list for the meter frame (and the donut ring) | `meter_background_color.xml` | `fwbase/packages/SettingsLib/res/color/meter_background_color.xml` | n/a | n/a | colorForeground at alpha 0.3: #4D000000 | state list. In the battery drawable it is only the initial paint colour (overridden by the filter); in the Storage donut it is the ring colour (section 4) |
| Colour list for the donut arc | `meter_consumed_color.xml` | `fwbase/packages/SettingsLib/res/color/meter_consumed_color.xml` | n/a | n/a | colorForeground: #FF000000 | state list. Storage donut (section 4) |

---

## 3. Sound page (volume rows)

Rows are in `settings10/res/xml/sound_settings.xml`. Row layout `settings10/res/layout/preference_volume_slider.xml`: icon frame minWidth 44 dp, icon ImageView 24 x 24 dp, SeekBar height 48 dp (no tint attribute in the layout, so each icon keeps its own tint).
Icon choice per row: `VolumeSeekBarPreference.updateIconView` order is: `showIcon` override, then the mute icon if muted (and not zen-muted), then the XML icon.
On a tablet that is not voice-capable the Ring row is hidden and the Notification row is shown.

| Purpose | File | Source path | Size (dp) | Viewport | Fill / tint (resolved) | Notes |
| :- | :- | :- | :- | :- | :- | :- |
| Media volume (music note) | `ic_media_stream.xml` | `settings10/res/drawable/ic_media_stream.xml` | 24 x 24 | 24 x 24 | fill #FF000000; tint CN #8A000000; drawn #8A000000 | `sound_settings.xml` line 36. Also the Storage "Music & audio" row (section 4) |
| Media volume, muted | `ic_media_stream_off.xml` | `settings10/res/drawable/ic_media_stream_off.xml` | 24 x 24 | 24 x 24 | fill #FF000000 (2 paths); tint CN; drawn #8A000000 | Swapped in by the mute logic |
| Call volume (phone) | `ic_local_phone_24_lib.xml` | `settings10/res/drawable/ic_local_phone_24_lib.xml` | 24 x 24 | 24 x 24 | fill #FF000000; tint CN; drawn #8A000000 | `sound_settings.xml` line 52. Muted variant: none (the same icon stays; a code comment says the call volume can never be fully muted) |
| Alarm volume (alarm clock) | **NOT FOUND** (`@*android:drawable/ic_audio_alarm`) | none in the clones (framework drawable) | not in clones | not in clones | not available | `sound_settings.xml` line 77. Idea from the earlier notes, not verified: `Resources.getSystem().getIdentifier("ic_audio_alarm", "drawable", "android")` at run time |
| Alarm volume, muted | **NOT FOUND** (`com.android.internal.R.drawable.ic_audio_alarm_mute`) | none in the clones | not in clones | not in clones | not available | Named in `settings10/src/com/android/settings/notification/AlarmVolumePreferenceController.java` line 61 |
| Ring volume and Notification volume (bell) | `ic_notifications.xml` | `settings10/res/drawable/ic_notifications.xml` | 24 x 24 | 24 x 24 | fill #FFFFFFFF (2 paths); tint CN #8A000000; drawn #8A000000 | `sound_settings.xml` lines 68 (ring) and 85 (notification). `NotificationVolumePreferenceController` extends `RingVolumePreferenceController`; after resume `updatePreferenceIcon` always calls `showIcon`, so the three ringer icons below are the ones shown and the plain "muted" branch is never reached |
| Bell, ringer mode silent | `ic_notifications_off_24dp.xml` | `settings10/res/drawable/ic_notifications_off_24dp.xml` | 24 x 24 | 24 x 24 | fill #FF000000 (3 paths); tint CN; drawn #8A000000 | autoMirrored = true |
| Bell, ringer mode vibrate | `ic_volume_ringer_vibrate.xml` | `settings10/res/drawable/ic_volume_ringer_vibrate.xml` | 24 x 24 | 24 x 24 | fill #FFFFFFFF; tint CN; drawn #8A000000 | Ringer NORMAL shows `ic_notifications`, VIBRATE this one, SILENT `ic_notifications_off_24dp` |
| Remote volume (only while a remote session exists; extra) | `ic_volume_remote.xml` | `fwbase/packages/SettingsLib/res/drawable/ic_volume_remote.xml` | 24 x 24 | 48 x 48 | fill #FFFFFFFF (written as `#FFFFFF`); tint CN; drawn #8A000000 | `sound_settings.xml` line 28 names `@drawable/ic_volume_remote`; the file is in SettingsLib, not in the Settings clone |
| Remote volume, muted (extra) | `ic_volume_remote_mute.xml` | `fwbase/packages/SettingsLib/res/drawable/ic_volume_remote_mute.xml` | 24 x 24 | 24 x 24 | fill #FFFFFFFF (5 paths); tint CN; drawn #8A000000 | SettingsLib |

Sound seek bar, switch and radio drawables: section 8 (names only).

---

## 4. Storage page

Rows are in `settings10/res/xml/storage_dashboard_fragment.xml`. `StorageItemPreferenceController.applyTint` (`settings10/src/com/android/settings/deviceinfo/storage/StorageItemPreferenceController.java`,
lines 220-230) sets `colorControlNormal` as an explicit tint on every row icon, so each row is drawn #8A000000 whatever the vector's own tint.
Row layout `settings10/res/layout/storage_item.xml`: icon frame minWidth 56 dp, padding 14 dp top and bottom; ProgressBar 8 dp high, margins 12 dp top and 4 dp bottom (its progress drawable is the framework `progress_horizontal_material`, section 8).

| Purpose | File | Source path | Size (dp) | Viewport | Fill / tint (resolved) | Notes |
| :- | :- | :- | :- | :- | :- | :- |
| Photos & videos | `ic_photo_library.xml` | `settings10/res/drawable/ic_photo_library.xml` | 24 x 24 | 24 x 24 | fill #FF000000; tint CN; drawn #8A000000 | `storage_dashboard_fragment.xml` line 38 |
| Music & audio | `ic_media_stream.xml` | `settings10/res/drawable/ic_media_stream.xml` | 24 x 24 | 24 x 24 | fill #FF000000; tint CN; drawn #8A000000 | line 44; the same file as the Sound media row (section 3) |
| Games | `ic_videogame_vd_theme_24.xml` | `settings10/res/drawable/ic_videogame_vd_theme_24.xml` | 24 x 24 | 24 x 24 | fill #FFFFFFFF (`@android:color/white`, 4 paths); tint CN; drawn #8A000000 | line 49 |
| Movies & TV apps | `ic_local_movies.xml` | `settings10/res/drawable/ic_local_movies.xml` | 24 x 24 | 24 x 24 | fill #FF000000; tint CN; drawn #8A000000 | line 54 |
| Other apps | `ic_storage_apps.xml` | `settings10/res/drawable/ic_storage_apps.xml` | 24 x 24 | 24 x 24 | fill #FF000000; tint CN; drawn #8A000000 | line 59; a 3 x 3 grid of dots |
| Files | `ic_folder_vd_theme_24.xml` | `settings10/res/drawable/ic_folder_vd_theme_24.xml` | 24 x 24 | 24 x 24 | fill #FFFFFFFF (`@android:color/white`); tint CN; drawn #8A000000 | line 64 |
| System | `ic_system_update.xml` | `fwbase/packages/SettingsLib/res/drawable/ic_system_update.xml` | 24 x 24 | 24 x 24 | fill #FFFFFFFF; tint CN; drawn #8A000000 | line 70. The Settings clone has no file of this name: the row uses the SettingsLib one. Also the System page "System update" row (section 6) |
| Storage manager toggle (extra) | `ic_storage.xml` | `settings10/res/drawable/ic_storage.xml` | 24 x 24 | 24 x 24 | fill #FF000000; tint CN; drawn #8A000000 | `toggle_asm`, line 30 |
| Audio files header on the Music & audio sub-page (extra) | `ic_headset_24dp.xml` | `settings10/res/drawable/ic_headset_24dp.xml` | 24 x 24 | 24 x 24 | fill #FF000000; tint CN; drawn #8A000000 | `settings10/src/com/android/settings/applications/manageapplications/MusicViewHolderController.java` line 75 |
| Volume low-storage warning (extra) | `ic_warning_24dp.xml` | `settings10/res/drawable/ic_warning_24dp.xml` | 24 x 24 | 24 x 24 | fill #FF000000 (3 paths); no tint in the file | `settings10/src/com/android/settings/deviceinfo/StorageVolumePreference.java` line 111 tints it colorError #FFFF5722 in code when free space is below the low-storage limit. Also used by the magnification config warning row (section 5) and the battery-usage anomaly widget |

**Donut (no drawable; `DonutView` draws in code).** Tablet layout is `settings10/res/layout-sw360dp/storage_summary_donut.xml` (the tablet is 564 dp wide: sw360dp applies, sw600dp does not): horizontal, left column weight 1,
paddingStart 72 dp, paddingEnd 16 dp, padding 32 dp top and bottom; title 36 sp (Display1), summary (Body1), "Free up space" button (style ActionPrimaryButton = `android:Widget.DeviceDefault.Button.Colored`) and a `DonutView` of
112 x 112 dp with marginEnd 32 dp. (The default layout is vertical with a 168 dp donut.)
- Stroke `storage_donut_thickness` = 4 dp; the arc rectangle is inset by 4 dp, so the outer diameter is 108 dp (115 px). Start angle -90, sweep 360 x percent, cap BUTT.
- Background ring = `meter_background_color` (colorForeground at alpha 0.3) with an accent SRC_IN filter: **#4D008577**. Consumed arc = `meter_consumed_color` with the same filter: **#FF008577**. Text paints are accent #FF008577.
- Text sizes on the tablet (`settings10/res/values-sw360dp/dimens.xml` lines 28-31): big number 30 sp (default 45 sp), percent sign 20 sp (RelativeSizeSpan 20/30), label "used" 14 sp (default 21 sp; 10 sp if the label is longer than 10 characters).
- Source: `settings10/src/com/android/settings/deviceinfo/StorageSummaryDonutPreference.java` and `fwbase/packages/SettingsLib` DonutView; dimens and strings in `VALUES_EXCERPTS.md`, section D.
- Toolbar on this page: entity style (section 7).

---

## 5. Accessibility page

Only three rows of `settings10/res/xml/accessibility_settings.xml` carry an icon; the other rows have none.

| Purpose | File | Source path | Size (dp) | Viewport | Fill / tint (resolved) | Notes |
| :- | :- | :- | :- | :- | :- | :- |
| Magnification row | `ic_accessibility_magnification.xml` | `settings10/res/drawable/ic_accessibility_magnification.xml` | 32 x 32 | 192 x 192 | no tint. Paths 1-3 fill #FFF50057 (path 1 = lens disc r 24 at 96,104; 2 and 3 = body); path 13 #FFF8BBD0 (plus); path 14 #FFFFFFFF; shadow paths 4, 5, 8, 9, 10, 11 = #FF3E2723 at fillAlpha 0.2 (= #333E2723); path 12 = #FFFFFFFF at fillAlpha 0.2 (= #33FFFFFF); gradients: paths 6 and 7 linear #333E2723 -> #003E2723, path 15 linear #19FFFFFF -> #00FFFFFF | 15 paths, 3 inline gradients (`aapt:attr`, needs `xmlns:aapt`). `accessibility_settings.xml` line 70. 32 dp spans x 12..44 dp in the 56 dp icon column |
| Color correction row (extra) | `ic_daltonizer.xml` | `settings10/res/drawable/ic_daltonizer.xml` | 32 x 32 | 192 x 192 | no tint. Body #FF00BCD4; darker #FF0097A7; #FF263238 at 0.2, 0.1; #FFFFFFFF at 0.2 and solid; gradients linear #19263238 -> #00212121 and radial #19FFFFFF -> #00FFFFFF | 11 paths, 1 clip-path, 2 gradients. `accessibility_settings.xml` line 165 |
| Color inversion row (extra) | `ic_color_inversion.xml` | `settings10/res/drawable/ic_color_inversion.xml` | 32 x 32 | 192 x 192 | no tint. Body #FF546E7A; #FF263238 at 0.2; #FFFFFFFF at 0.2 and solid; #FF000000 at 0.1; gradients linear #19263238 -> #00212121 and radial #19FFFFFF -> #00FFFFFF | 10 paths, 2 gradients. `accessibility_settings.xml` line 173 |
| Warning row on the Magnification sub-page (extra) | `ic_warning_24dp.xml` | `settings10/res/drawable/ic_warning_24dp.xml` | 24 x 24 | 24 x 24 | fill #FF000000; no tint in the file or in code | `settings10/src/com/android/settings/accessibility/ToggleScreenMagnificationPreferenceFragment.java` line 151 (row hidden until a config warning applies) |

---

## 6. System page and Languages & input

System page rows come from `settings10/res/xml/system_dashboard_fragment.xml` (static rows) and from manifest tiles with category `com.android.settings.category.ia.system`
(shown ordered by their `com.android.settings.order`). Manifest tiles have no `icon_tintable` metadata, so only the vector's own tint applies.

| Purpose | File | Source path | Size (dp) | Viewport | Fill / tint (resolved) | Notes |
| :- | :- | :- | :- | :- | :- | :- |
| System page row "Languages & input" | `ic_settings_language.xml` | `settings10/res/drawable/ic_settings_language.xml` | 24 x 24 | 24 x 24 | fill #FFFFFFFF; tint CN #8A000000; drawn #8A000000 | `settings10/AndroidManifest.xml` line 618 (`Settings$LanguageAndInputSettingsActivity`, order -260). The same icon is on `Settings$LocalePickerActivity` (line 592) |
| Row "Languages" on the Languages & input page | `ic_translate_24dp.xml` | `settings10/res/drawable/ic_translate_24dp.xml` | 24 x 24 | 24 x 24 | fill #FF000000; tint CN; drawn #8A000000 | `settings10/res/xml/language_and_input.xml` line 28. No other row on this page has an icon |
| System page row Gestures | `ic_settings_gestures.xml` | `settings10/res/drawable/ic_settings_gestures.xml` | 24 x 24 | 32 x 32 | fill #FFFFFFFF (`@android:color/white`); tint CN; drawn #8A000000 | `system_dashboard_fragment.xml` line 27 |
| System page row Reset options | `ic_restore.xml` | `settings10/res/drawable/ic_restore.xml` | 24 x 24 | 24 x 24 | fill #FFFFFFFF; tint CN; drawn #8A000000 | `system_dashboard_fragment.xml` line 36 |
| System page row System update | `ic_system_update.xml` | `fwbase/packages/SettingsLib/res/drawable/ic_system_update.xml` | 24 x 24 | 24 x 24 | fill #FFFFFFFF; tint CN; drawn #8A000000 | `system_dashboard_fragment.xml` line 46 (same file as the Storage "System" row) |
| System page row Date & time | `ic_settings_date_time.xml` | `settings10/res/drawable/ic_settings_date_time.xml` | 24 x 24 | 24 x 24 | fill #FFFFFFFF (`@android:color/white`, 2 paths); tint CN; drawn #8A000000 | `settings10/AndroidManifest.xml` line 565 (`Settings$DateTimeSettingsActivity`, order -240, category line 581) |
| System page row Backup | `ic_settings_backup.xml` | `settings10/res/drawable/ic_settings_backup.xml` | 24 x 24 | 24 x 24 | fill #FFFFFFFF; tint CN; drawn #8A000000 | `settings10/AndroidManifest.xml` line 2912 (`.backup.UserBackupSettingsActivity`, order -60, category line 2924). The activity at line 1295 (`Settings$PrivacySettingsActivity`) uses the same icon for the Backup page itself |
| System page row Developer options (only when enabled; extra) | `ic_settings_development.xml` | `settings10/res/drawable/ic_settings_development.xml` | not set (PNG size) | none | bitmap wrapper, tint CN #8A000000 | **bitmap-wrapper XML (copied) around a PNG**: `android:src="@drawable/ic_settings_development_alpha"`. PNGs (gray + alpha, not copied): `settings10/res/drawable-mdpi/ic_settings_development_alpha.png` 24 x 24, `drawable-hdpi` 36, `drawable-xhdpi` 48, `drawable-xxhdpi` 72, `drawable-xxxhdpi` 96 (same file name in each). `settings10/AndroidManifest.xml` line 1908 (order -40, category line 1921) |
| System page row Multiple users (only when supported; extra) | `ic_settings_multiuser.xml` | `settings10/res/drawable/ic_settings_multiuser.xml` | 24 x 24 | 24 x 24 | fill #FFFFFFFF; tint CN; drawn #8A000000 | `settings10/AndroidManifest.xml` line 2367 (order -45, category line 2378) |
| Gear widget on "Preferred engine" (Text-to-speech page; extra) | `ic_settings_accent.xml` | `settings10/res/drawable/ic_settings_accent.xml` | 24 x 24 | 24 x 24 | fill #FFFFFFFF (2 paths); tint ACC; drawn #FF008577 | `settings10/res/layout/preference_widget_gear.xml` line 28, via `GearPreference` (`settings10/src/com/android/settings/widget/GearPreference.java` line 46). Shown on "Preferred engine" only when an engine exists (`TextToSpeechSettings.java` line 329). The other rows of that page, and its Play and Reset buttons, have no icons |
| "Add a language" button (Languages sub-page; extra) | `ic_add_24dp.xml` | `settings10/res/drawable/ic_add_24dp.xml` | 24 x 24 | 24 x 24 | fill #FFFFFFFF (`@android:color/white`); tint CN; drawn #8A000000 | `settings10/res/layout/locale_order_list.xml` line 42, `drawableStart`, drawablePadding 24 dp |
| Drag handle (Languages sub-page; extra) | `drag_handle.xml` | `settings10/res/drawable/drag_handle.xml` | 24 x 24 | 24 x 24 | fill #FFFFFFFF (`@android:color/white`); tint CN; drawn #8A000000 | `settings10/res/layout/locale_drag_cell.xml` line 70 |
| "Remove" menu item (Languages sub-page; extra) | `ic_delete.xml` | `settings10/res/drawable/ic_delete.xml` | 24 x 24 | 24 x 24 | fill #FF000000 (3 paths); tint CN; drawn #DE000000 in the toolbar | `settings10/src/com/android/settings/localepicker/LocaleListEditor.java` line 255 (`menuItem.setIcon`); a toolbar menu icon, so CN-bar applies |
| Font size page, smaller "A" (extra) | `ic_font_size_16dp.xml` | `settings10/res/drawable/ic_font_size_16dp.xml` | 16 x 16 | 48 x 48 | fill #FFFFFFFF (`@android:color/white`); own tint ACC #FF008577, but the ImageView sets `android:tint=?android:attr/textColorPrimary`, tintMode src_in, which replaces it: drawn #DE000000 | `settings10/res/layout/font_size_activity.xml` lines 63-68 (ImageView 48 x 48 dp, scaleType center) |
| Font size page, larger "A" (extra) | `ic_font_size_24dp.xml` | `settings10/res/drawable/ic_font_size_24dp.xml` | 24 x 24 | 48 x 48 | same as above: drawn #DE000000 | `font_size_activity.xml` lines 82-87 |
| Display size page, minus (extra) | `ic_remove_24dp.xml` | `settings10/res/drawable/ic_remove_24dp.xml` | 24 x 24 | 48 x 48 | fill #FFFFFFFF (`@android:color/white`); own tint ACC, replaced by the ImageView tint `textColorPrimary` src_in: drawn #DE000000 | `settings10/res/layout/screen_zoom_activity.xml` lines 59-68 |
| Display size page, plus (extra) | `ic_add_24dp.xml` | `settings10/res/drawable/ic_add_24dp.xml` | 24 x 24 | 24 x 24 | own tint CN, replaced by the ImageView tint `textColorPrimary` src_in: drawn #DE000000 | `screen_zoom_activity.xml` lines 77-86 (the same file as the "Add a language" row above) |

---

## 7. Action bar (toolbar)

Layout `settings10/res/layout/settings_base_layout.xml`: a Toolbar with `style="?android:attr/actionBarStyle"` and `android:theme="?android:attr/actionBarTheme"`
(`ThemeOverlay.DeviceDefault.ActionBar`, which gives the toolbar's controls `colorControlNormal` = `textColorPrimary` = #DE000000).

| Purpose | File | Source path | Size (dp) | Viewport | Fill / tint (resolved) | Notes |
| :- | :- | :- | :- | :- | :- | :- |
| Back arrow (up indicator) | `ic_arrow_back.xml` | `settings10/res/drawable/ic_arrow_back.xml` | 24 x 24 | 24 x 24 | fill #FFFFFFFF (`@android:color/white`); tint CN; drawn #DE000000 inside the toolbar | autoMirrored = true. Set as `android:homeAsUpIndicator` in Theme.Settings, `settings10/res/values/themes.xml` line 33. The #DE000000 follows from the toolbar's theme overlay (resolved from the style chain, not measured here) |
| Overflow three dots | **NOT FOUND** | see section 2 | not in clones | not in clones | not available | the toolbar draws nothing else as an icon |
| Search icon (kept for completeness; Tj removes search) | `ic_search_24dp.xml` | `fwbase/packages/SettingsLib/SearchWidget/res/drawable/ic_search_24dp.xml` | 24 x 24 | 24 x 24 | fill #FF000000; tint CN; drawn #8A000000 | the home page search bar. `ic_homepage_search` (the home search card) was not copied |

Toolbar facts (values with line numbers in `VALUES_EXCERPTS.md`, section E):
- Height 56 dp (`action_bar_default_height_material`); elevation 4 dp (`action_bar_elevation_material`), static on normal pages. Background colorPrimary #FFF5F5F5; status bar colorPrimaryDark #FFE0E0E0 (`windowLightStatusBar`).
- Content inset start: 16 dp on the home page (`Widget.ActionBar`), 72 dp on sub pages (`Widget.ActionBar.SubSettings`, `actionbar_subsettings_contentInsetStart`).
- Title style `TextAppearance.DeviceDefault.Widget.ActionBar.Title`: 20 sp, textColorPrimary (#DE000000), font `config_headlineFontFamilyMedium`.
- Entity-header pages (Battery, Storage): `EntityHeaderController.styleActionBar` (`settings10/src/com/android/settings/widget/EntityHeaderController.java` lines 310-327) sets the toolbar background to colorPrimaryDark #FFE0E0E0 and elevation 0,
  then `ActionBarShadowController` (`fwbase/packages/SettingsLib/ActionBarShadow/src/com/android/settingslib/widget/ActionBarShadowController.java`) sets elevation 8 (passed to `setElevation`, which takes pixels) while the list can scroll up, else 0.
  Header style `EntityHeader` (`fwbase/packages/SettingsLib/LayoutPreference/res/values/styles.xml` lines 18-24): background colorPrimaryDark, paddingTop 24 dp, paddingBottom 16 dp, paddingEnd 16 dp.
- `settings10/res/drawable/action_bar_dropshadow.xml` exists but has no reference (unused).

---

## 8. Radio, switch, seek bar, progress bar: framework drawables, NAMED only (none is in the clones)

All of these come from `fwbase/core/res/res/values/*.xml` styles; the drawables live in the framework `core/res/res/drawable*` tree: **NOT FOUND** as files.

| Widget | Theme attribute (Theme.DeviceDefault.Light chain) | Style chain | Drawables and flags named by the style (files NOT FOUND) |
| :- | :- | :- | :- |
| SeekBar | `seekBarStyle` = `Widget.DeviceDefault.Light.SeekBar` (`themes_device_defaults.xml` line 866) | -> `Widget.Material.Light.SeekBar` (`styles_material.xml` line 1175) -> `Widget.Material.SeekBar` (line 802) | `progressDrawable` and `indeterminateDrawable` = `seekbar_track_material`; `thumb` = `seekbar_thumb_material_anim`; `background` = `control_background_32dp_material`; `splitTrack` true; `useDisabledAlpha` false; padding 16 dip each side; `mirrorForRtl` true. Volume rows use a plain SeekBar of height 48 dp; the balance row uses BalanceSeekBar |
| SeekBar, discrete | style `Widget.Material.SeekBar.Discrete` (`styles_material.xml` line 817) | adds `tickMark` = `seekbar_tick_mark_material` | used by `settings10/res/layout/preference_widget_seekbar_settings.xml` (DefaultIndicatorSeekBar: Text-to-speech rate and pitch) and by the font size and display size sliders (LabeledSeekBar) |
| Switch | `switchStyle` inherited from Theme.Material.Light = `Widget.Material.Light.CompoundButton.Switch` (`themes_material.xml` line 486; Theme.DeviceDefault.Light does not override it) | -> `Widget.Material.CompoundButton.Switch` (`styles_material.xml` line 653) | `track` = `switch_track_material`; `thumb` = `switch_thumb_material_anim`; `background` = `control_background_40dp_material`; `showText` false; `switchTextAppearance` = `TextAppearance.Material.Widget.Switch`. The androidx SwitchPreference layout is not in the clones |
| RadioButton | `radioButtonStyle` = `Widget.DeviceDefault.Light.CompoundButton.RadioButton` (`themes_device_defaults.xml` line 870) | -> `Widget.Material.Light.CompoundButton.RadioButton` (`styles_material.xml` line 1133) -> `Widget.Material.CompoundButton.RadioButton` (line 644) -> `Widget.CompoundButton.RadioButton` (`styles.xml` line 380) | `background` = `control_background_40dp_material`; `button` = `?attr/listChoiceIndicatorSingle` = `btn_radio_material_anim` (`themes_material.xml` line 512). In a Settings row: `settings10/res/layout/preference_widget_radiobutton.xml` (a `RadioButton` with id `@android:id/checkbox`, background `@null`). List-preference dialogs use Theme.AlertDialog (parent Theme.AppCompat.DayNight.Dialog.Alert); the AppCompat layouts are not in the clones. `settings10/res/drawable/radio_button_check.xml` is used only by slices |
| ProgressBar (Storage row bar) | `progressBarStyleHorizontal` = `Widget.DeviceDefault.Light.ProgressBar.Horizontal` (`themes_device_defaults.xml` line 857) | -> `Widget.Material.Light.ProgressBar.Horizontal` -> `Widget.Material.ProgressBar.Horizontal` (`styles_material.xml` line 774) | `progressDrawable` = `progress_horizontal_material`; `indeterminateDrawable` = `progress_indeterminate_horizontal_material`; `minHeight` and `maxHeight` 16 dip (the Storage rows override the height to 8 dp in `storage_item.xml`) |

---

## 9. Settings launcher icon (adaptive layers)

| Purpose | File | Source path | Size (dp) | Viewport | Fill / tint (resolved) | Notes |
| :- | :- | :- | :- | :- | :- | :- |
| Settings app icon (adaptive icon XML) | `ic_launcher_settings.xml` | `settings10/res/drawable/ic_launcher_settings.xml` | 108 x 108 layers (adaptive icon; the file sets no size) | n/a | background `@color/icon_launcher_setting_color` -> `@*android:color/accent_device_default_light` = **#FF008577**; foreground = the PNG mipmap below | `<adaptive-icon>` (XML copied). Set by `<application android:icon="@drawable/ic_launcher_settings">`, `settings10/AndroidManifest.xml` line 106; there is no `roundIcon`. Colour reference: `settings10/res/values/colors.xml` line 136 |
| Foreground layer (PNG, not copied) | `ic_launcher_settings.png` | `settings10/res/mipmap-mdpi/ic_launcher_settings.png` 108 x 108 px; `mipmap-hdpi` 162; `mipmap-xhdpi` 216; `mipmap-xxhdpi` 324; `mipmap-xxxhdpi` 432 (same file name in each folder, all RGBA) | 108 x 108 (one layer) | n/a | measured at xxxhdpi: a light-grey gear, 19,108 of its 19,782 fully opaque pixels are exactly #FFF5F5F5; opaque gear bounding box (122,117)-(309,314) px = 30.5..77.3 dp by 29.3..78.5 dp of the 108 dp layer; a faint navy shadow (colour near #002060, mean alpha about 7 %) runs from the gear to the bottom-right, bounding box (104,104)-(431,431); corners transparent | PNG. At 170 dpi the density rule picks `mipmap-mdpi` (108 px) and scales it by 1.0625. Visible part of an adaptive icon is the central 72 dp, and the gear lies inside it |

---

## 10. Extras (not asked for; collected because they sit on pages near the asked ones)

Battery usage page rows (`settings10/src/com/android/settings/fuelgauge/BatteryEntry.java`; these are the system rows of the usage list, 32 dp icon frame in `preference_app.xml`):

| Purpose | File | Source path | Size (dp) | Viewport | Fill / tint (resolved) | Notes |
| :- | :- | :- | :- | :- | :- | :- |
| Idle | `ic_settings_phone_idle.xml` | `settings10/res/drawable/ic_settings_phone_idle.xml` | 48 x 48 | 48 x 48 | fill #FF000000; tint CN; drawn #8A000000 | `BatteryEntry.java` line 144 |
| Cell standby | `ic_cellular_1_bar.xml` | `settings10/res/drawable/ic_cellular_1_bar.xml` | 24 x 24 | 24 x 24 | fill #FF000000 (2 paths); tint CN; drawn #8A000000 | line 148 |
| Voice calls | `ic_settings_voice_calls.xml` | `settings10/res/drawable/ic_settings_voice_calls.xml` | 48 x 48 | 48 x 48 | fill #FF000000; tint CN; drawn #8A000000 | line 152 |
| Wi-Fi | `ic_settings_wireless.xml` | `settings10/res/drawable/ic_settings_wireless.xml` | 24 x 24 | 24 x 24 | fill #FFFFFFFF; tint CN; drawn #8A000000 | line 156 |
| Bluetooth | **NOT FOUND** (`@*android:drawable/ic_settings_bluetooth`, `com.android.internal.R.drawable`) | none in the clones | not in clones | not in clones | not available | line 160 |
| Screen and flashlight | `ic_settings_display.xml` | `settings10/res/drawable/ic_settings_display.xml` | 24 x 24 | 24 x 24 | fill #FFFFFFFF; tint CN; drawn #8A000000 | lines 164 and 168 |
| Android OS, unaccounted, over-counted | `ic_android.xml` | `settings10/res/drawable/ic_android.xml` | 24 x 24 | 24 x 24 | fill #FFFFFFFF; tint CN; drawn #8A000000 | lines 202 and 206 |
| Camera | `ic_settings_camera.xml` | `settings10/res/drawable/ic_settings_camera.xml` | 24 x 24 | 48 x 48 | fill #FF000000; tint CN; drawn #8A000000 | autoMirrored = true. Line 210 |
| Ambient display | `ic_settings_aod.xml` | `settings10/res/drawable/ic_settings_aod.xml` | 24 x 24 | 24 x 24 | fill #FF000000 (4 paths); tint CN; drawn #8A000000 | line 214 |
| System | `ic_power_system.png` | `settings10/res/drawable-mdpi/ic_power_system.png` 24 x 24 px; `drawable-hdpi` 36; `drawable-xhdpi` 48; `drawable-xxhdpi` 72; `drawable-xxxhdpi` 96 (8-bit gray + alpha at xxxhdpi, RGBA in the others) | 24 x 24 | n/a | PNG, not copied | `BatteryEntry.java` line 262 |
| Anomaly warning widget on a usage row | `ic_warning_24dp.xml` | `settings10/res/drawable/ic_warning_24dp.xml` | 24 x 24 | 24 x 24 | fill #FF000000; no tint in the file | `PowerGaugePreference.java` line 112 (start drawable of `widget_summary`, only for an anomaly) |

---

## 11. NOT FOUND (complete list)

1. Toolbar overflow three dots: framework `ic_menu_moreoverflow_material` (style chain and dimensions verified, glyph not in the clones).
2. Alarm volume icon: `@*android:drawable/ic_audio_alarm`.
3. Alarm volume muted icon: `com.android.internal.R.drawable.ic_audio_alarm_mute`.
4. Bluetooth battery-usage row icon: `com.android.internal.R.drawable.ic_settings_bluetooth` (extra).
5. Framework widget drawables, named in section 8 only: `seekbar_track_material`, `seekbar_thumb_material_anim`, `seekbar_tick_mark_material`, `switch_track_material`, `switch_thumb_material_anim`,
   `btn_radio_material_anim`, `progress_horizontal_material`, `progress_indeterminate_horizontal_material`, `control_background_32dp_material`, `control_background_40dp_material`.
6. The text colour state lists `text_color_primary` and `text_color_secondary` (framework `core/res/res/color`): the base colours #DE000000 and #8A000000 are given instead (`colors_material.xml` lines 59 and 61).
7. The androidx SwitchPreference layout and the AppCompat dialog layouts (not in the clones).
