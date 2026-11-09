# AOSP 10 Settings (android-10.0.0_r47): Accessibility, System, About tablet (TASKS BN1 notes)

Facts only. Paths: `settings10/res/...`, `settings10/src/...` (Java, package com.android.settings), `fwbase/packages/SettingsLib/res/...`,
`fwbase/core/res/res/...` (framework resources). 1 dp = 1.0625 px; sp at font scale 1.2 = x1.275 px.
Markers: **NF** = NOT FOUND in the checkout. **DIFF** = AOSP source disagrees with the real tablet screens (`research/SETTINGS_SPOOF_2026-11-09.md`
sections 1 and 6). **(memory)** = written from memory of androidx/AOSP, not checkable in the checkout.
Missing from the checkout: the androidx.preference layouts/styles (`preference_material`, `preference_category_material`, `Preference.Material`,
`Preference.Category.Material`), framework `color/` state lists, most framework `drawable/`. The platform copies
`fwbase/core/res/res/layout/preference_*_material.xml` are used below as stand-ins (same numbers as androidx 1.1, memory).

## 0. Shared machinery (rows, headings, dividers)

- Preference theme (`settings10/res/values/styles_preference.xml`): `PreferenceTheme` -> `preferenceCategoryTitleTextAppearance` =
  `TextAppearance.CategoryTitle`; `seekBarPreferenceStyle` = `SettingsSeekBarPreference` (layout `preference_widget_seekbar_settings`);
  fragment style `SettingsPreferenceFragmentStyle` sets `android:divider` = `@*android:drawable/list_divider_material`.
- Standard row (`fwbase/core/res/res/layout/preference_material.xml`): LinearLayout, minHeight `listPreferredItemHeightSmall` (48 dp), paddingStart/End
  `listPreferredItemPaddingStart/End` (16 dp), background `activatedBackgroundIndicator`, gravity center_vertical.
  icon_frame: marginStart -4 dp, minWidth 60 dp, paddingEnd 12 dp, paddingTop/Bottom 4 dp, gravity start; icon `PreferenceImageView` maxWidth/maxHeight 48 dp.
  Text block (RelativeLayout, weight 1): paddingTop/Bottom 16 dp. Title: `textAppearanceListItem` = `TextAppearance.Material.Subhead` = 16 sp, `sans-serif`,
  colour `textColorPrimary` (#de000000), singleLine, ellipsize marquee. Summary: below title, `textAppearanceListItemSecondary` = `Body1` = 14 sp `sans-serif`,
  colour `textColorSecondary` (#8a000000), maxLines 10, ellipsize end. widget_frame: paddingStart 16 dp, gravity end|center_vertical.
- Title x = 16 - 4 + 60 = 72 dp = 76.5 px (measured 77 px): so a row WITHOUT an icon still reserves the icon frame (androidx `Preference.Material`
  `iconSpaceReserved` = true, memory; NF in checkout). Evidence in source: `setIconSpaceReserved(false)` is called only in `tts/TtsEnginePreference.java` and
  `widget/RadioButtonPreference.java`; `preference_no_icon_padding_start` = 72 dp (`fwbase/packages/SettingsLib/res/values/styles_support_preference.xml`) is
  used by custom layouts to align with titles; `accessibility/ToggleFeaturePreferenceFragment.java` calls `setIconSpaceReserved(true)` explicitly.
- A row WITH an icon: the icon sits at the frame start: x = 16 - 4 = 12 dp; a 32 dp icon spans 12..44 dp (centre 28 dp = 29.75 px); a 24 dp icon centre 24 dp = 25.5 px.
- Category heading (platform copy `preference_category_material.xml`): TextView, paddingStart/End 16 dp, paddingTop 16 dp, marginBottom 16 dp, colour
  `colorAccent`. Settings replaces the appearance by `TextAppearance.CategoryTitle` (`settings10/res/values/styles.xml` line 239): parent
  `TextAppearance.DeviceDefault.Body2` (14 sp `sans-serif-medium`) + `textAllCaps` true + `textSize` 11 sp + `letterSpacing` 0.072727 (0.8/11).
  At font scale 1.2: 13.2 sp = 14.0 px (measured "about 13 sp"). Heading strings are stored in mixed case ("Screen readers") and shown capitalised.
  Heading x: NF for androidx; proxy `settings10/res/layout/time_zone_search_header.xml` = paddingStart 72 dp, marginTop/Bottom 16 dp, same appearance.
  A category with an empty title shows no heading text (memory).
- Dividers (androidx `DividerDecoration`, memory): a hairline is drawn below item A only if A.allowDividerBelow AND the next item B.allowDividerAbove.
  `Preference.Material`: above = false, below = true. `Preference.Category.Material`: above = true, below = false. So every category heading has a divider ABOVE it
  (not above the very first item of a page); `settings:allowDividerAbove="true"` in XML adds one above a plain row. Footer style also has above = true
  (`styles_support_preference.xml`). Divider spans the full width. Colour `colorListDivider` = `list_divider_color_light` #dadce0
  (`fwbase/core/res/res/values/colors_device_defaults.xml`; opacity attr = white = no alpha); measured 1 px #DADCE0. Drawable `list_divider_material`: NF.
- Colours: accent #008577 (SPOOF 4c), page #FAFAFA, text #de000000 / #8a000000 (`colors_material.xml`). Disabled/hint text colours: NF (`color/` lists absent).
- Settings theme `Theme.Settings` overrides `android:listPreferredItemHeight` to 72 dip; `listPreferredItemHeightSmall` stays 48 dp; used by custom slider rows.

## 1. Accessibility page

Screen: `settings10/res/xml/accessibility_settings.xml`, key `accessibility_settings_screen`, title `accessibility_settings` "Accessibility",
class `accessibility/AccessibilitySettings.java`. XML order (a `PreferenceCategory` is a heading + its rows; keys in backticks):
1. (no heading) `accessibility_shortcut_preference` "Volume key shortcut" (REMOVE).
2. `user_installed_services_category` heading "Downloaded services" (`user_installed_services_category_title`; rows = installed accessibility services; empty -> not shown).
3. `screen_reader_category` heading "Screen readers": `tts_settings_preference`.
4. `display_category` heading "Display": `font_size_preference_screen`, `accessibility_settings_screen_zoom`, `dark_ui_mode_accessibility` (SwitchPreference "Dark theme"),
   `magnification_preference_screen`, `toggle_large_pointer_icon`, `toggle_disable_animations`.
5. `interaction_control_category` heading "Interaction controls": `autoclick_preference`, `toggle_power_button_ends_call_preference`,
   `toggle_lock_screen_rotation_preference` ("Auto-rotate screen"), `select_long_press_timeout_preference`, `accessibility_control_timeout_preference_fragment`,
   `vibration_preference_screen`.
6. `audio_and_captions_category` heading "Audio & on-screen text" (`audio_and_captions_category_title`): `live_caption`, `toggle_master_mono`,
   `seekbar_master_balance`, `hearing_aid_preference`, `captioning_preference_screen`.
7. `experimental_category` heading "Experimental": `toggle_high_text_contrast_preference`, `daltonizer_preference` ("Color correction"), `toggle_inversion_preference`.
- Real tablet DISPLAY order (Font size, Display size, Dark theme, Magnification, Color correction, Color inversion, Large mouse pointer, Remove animations) is
  explained by `AccessibilitySettings.updateSystemPreferences()`: when `ColorDisplayManager.isColorTransformAccelerated(ctx)` it moves daltonizer + inversion out of
  EXPERIMENTAL into DISPLAY right after Magnification (`setOrder(magnification.order + 1)` chain, then large pointer, then animations). XML alone would give
  Font size, Display size, Dark theme, Magnification, Large pointer, Remove animations. The wanted subset has the same relative order either way.
- Wanted rows and headings, in order: SCREEN READERS (Text-to-speech output); DISPLAY (Font size, Display size, Magnification); INTERACTION CONTROLS
  (Touch & hold delay); AUDIO & ON-SCREEN TEXT (Mono audio, Audio balance). Heading strings: "Screen readers", "Display", "Interaction controls",
  "Audio & on-screen text" (`strings.xml` lines 4815-4821).
- DIFF: r47 title of `accessibility_control_timeout_preference_fragment` is "Time to take action (Accessibility timeout)" (`accessibility_setting_item_control_timeout_title`);
  the tablet shows "Time to take action" (row not wanted).

### 1.1 Text-to-speech output
- Row: key `tts_settings_preference`; Preference; title `tts_settings_title` "Text-to-speech output" (defined in `fwbase/packages/SettingsLib/res/values/strings.xml` line 399,
  NOT in settings10 strings); summary: none (no `android:summary`, nothing sets one; matches the tablet); no icon; click = fragment
  `com.android.settings.tts.TextToSpeechSettings` (`settings:searchable=false`). The same page is also a row in Languages & input (key `tts_settings_summary`, section 2).
- Page `settings10/res/xml/tts_settings.xml` (key `tts_settings_screen`, title "Text-to-speech output"), rows in order:
  1. `tts_engine_preference` `GearPreference`, title `tts_engine_preference_title` "Preferred engine" (settings10 strings line 5942); summary = engine label
     (`TtsEngines.getEngineInfo(engine).label`); click = fragment `TtsEnginePreferenceFragment` (`res/xml/tts_engine_picker.xml`, title "Preferred engine", one radio row per
     engine via `TtsEnginePreference`, `iconSpaceReserved` false); the gear widget (`res/layout/preference_widget_gear.xml`) starts the engine's own settings activity.
  2. `tts_default_lang` `ListPreference`, title `tts_default_lang_title` "Language"; XML summary `tts_default_lang_summary` "Sets the language-specific voice for the spoken text",
     replaced by the chosen entry name (`updateLanguageTo`); entries include "Use system language" (`tts_lang_use_system`); `persistent=false`; hidden/disabled when the
     engine reports no languages; data `TtsEngines.updateLocalePrefForEngine` (hidden class; backing Secure key NOT READ).
  3. Category `tts_general_section` (no title): `tts_default_rate` (`widget/SeekBarPreference`) title `tts_default_rate_title` "Speech rate", summary `tts_default_rate_summary`
     "Speed at which the text is spoken", XML defaultValue 50, max 600; `tts_default_pitch` title "Pitch", summary "Affects the tone of the synthesized speech",
     defaultValue 100, max 400; `action_buttons` `ActionButtonsPreference`: button 1 `tts_play` "Play" (speaks the engine's sample text, else `tts_default_sample_string`
     "This is an example of speech synthesis"; `TextToSpeech.speak(.., QUEUE_FLUSH, utteranceId "Sample")`; network-only languages -> dialog `tts_engine_network_required`),
     button 2 `tts_reset` "Reset" (rate and pitch back to 100).
  - Slider mapping in code (`TextToSpeechSettings.java`): rate progress = value - 10, max 590 (value 10..600); pitch progress = value - 25, max 375 (value 25..400).
    Write on every change: `Settings.Secure.putInt("tts_default_rate", value)` / `"tts_default_pitch"`; read default 100 (`TextToSpeech.Engine.DEFAULT_RATE/PITCH`).
    Engine key `tts_default_synth` (String, engine package) is read through `TtsEngines.getDefaultEngine` (hidden class; the TextToSpeech instance's `getDefaultEngine()` is public).
  - DIFF with the wording of the brief: strings "Listen to an example" (`tts_play_example_title`) and "Reset speech pitch" exist but tts_settings.xml in r47 does NOT use them
    (Play / Reset buttons instead).
  - Slider row layout: `SettingsSeekBarPreference` -> `settings10/res/layout/preference_widget_seekbar_settings.xml`: RelativeLayout, marginTop/Bottom 6 dp, minHeight
    `listPreferredItemHeight` (72 dip under `Theme.Settings`), paddingStart/End 16 dp; title paddingStart 56 dp / paddingEnd 8 dp, `TextAppearance.DeviceDefault.Subhead`;
    summary below, same paddings, maxLines 4, `textAppearanceListItemSecondary`, `textColorSecondary`; `DefaultIndicatorSeekBar` style `Widget.Material.SeekBar.Discrete`
    (tick marks), height 48 dp, paddingStart 56 dp, paddingEnd 12 dp, layout_alignParentEnd, below summary. `SeekBarPreference` is NOT selectable by default.
  - Action buttons (`fwbase/packages/SettingsLib/ActionButtonsPreference/res/layout/settings_action_buttons.xml`): horizontal row of 4 equal-weight Buttons (paddingStart 8 dp),
    only the used ones visible; style `SettingsActionButton` (`.../res/values/styles.xml`): parent `Widget.DeviceDefault.Button.Borderless.Colored`, drawablePadding 4 dp,
    marginEnd 8 dp, paddingTop/Bottom 20 dp.
- Gotchas: `android.speech.tts.TextToSpeech` is public (`new TextToSpeech(ctx, listener)`, `speak`, `setSpeechRate`, `setPitch`, `getEngines()`, `getDefaultEngine()`,
  `getDefaultVoice()`); `TtsEngines` and `EngineInfo` locale preference helpers are hidden; Intents `TextToSpeech.Engine.ACTION_CHECK_TTS_DATA` / `ACTION_GET_SAMPLE_TEXT` are public.
  The Secure keys can be read without permission and written with WRITE_SECURE_SETTINGS (held). `Settings.Secure.TTS_DEFAULT_*` constants: visibility not checkable here (use the strings).

### 1.2 Font size
- Row: key `font_size_preference_screen`; title `title_font_size` "Font size" (settings10 strings line 2957); summary `entries_font_size[index]` where
  `index = ToggleFontSizePreferenceFragment.fontSizeValueToIndex(Settings.System.font_scale (default 1.0), entryvalues_font_size)`.
  Arrays (`settings10/res/values/arrays.xml` 139-151): Small / Default / Large / Largest = 0.85 / 1.0 / 1.15 / 1.30. Rule: nearest by midpoint, i.e. < 0.925 Small, < 1.075 Default,
  < 1.225 Large, else Largest. Click = fragment `com.android.settings.display.ToggleFontSizePreferenceFragment` (`PreviewSeekBarPreferenceFragment`, 4 stops; `commit()` =
  `Settings.System.putFloat(FONT_SCALE, values[index])`). No icon. `settings:searchable=false`.
- **DIFF**: the tablet report says font scale 1.2, yet the row shows "Default". r47 arrays would give "Large" for 1.2 (1.2 < 1.225). So the tablet build uses other
  `entryvalues_font_size` (probably Default = 1.2); the real values are NF here. Read them from the installed Settings app at run time or measure the page.
- Gotchas: `Settings.System.FONT_SCALE` and `Settings.System.putFloat` are public; writing needs WRITE_SETTINGS (appop) or WRITE_SECURE_SETTINGS (held). The activity manager observes the
  System key `font_scale` and pushes the new Configuration to all apps (memory); no extra call is needed.

### 1.3 Display size
- Row: key `accessibility_settings_screen_zoom`; class `display/ScreenZoomPreference` (extends Preference); title `screen_zoom_title` "Display size" (strings line 9441);
  summary (set in the constructor only if the XML has none) = `DisplayDensityUtils(ctx).getEntries()[getCurrentIndex()]`; row hidden and disabled when `getCurrentIndex() < 0`.
  Entries (`fwbase/packages/SettingsLib/res/values/strings.xml` 1073-1084): Small, Default, Large, Larger, Largest, or `Custom (%d)` when the density matches no step.
  Steps (`fwbase/packages/SettingsLib/src/com/android/settingslib/display/DisplayDensityUtils.java`): scale interval 0.09, min 0.85, max 1.50, larger steps limited so the
  smallest screen side stays >= 320 dp (`maxDensity = DENSITY_MEDIUM * minDimensionPx / 320`). Default entry = the display's initial density.
- Click = fragment `display/ScreenZoomSettings` (`PreviewSeekBarPreferenceFragment`; layout `screen_zoom_activity`; previews `screen_zoom_preview_1`...). `commit()` =
  `DisplayDensityUtils.setForcedDisplayDensity(DEFAULT_DISPLAY, dpi)` or `clearForcedDisplayDensity` when dpi == default (both call hidden `IWindowManager.set/clearForcedDisplayDensityForUser`).
- Gotchas: no public API changes the density; `wm density N` / `wm density reset` through the adb shell helper is the equivalent; `DisplayMetrics.densityDpi` (current) is public.
  The Launcher's own window follows the new density only after the configuration change.

### 1.4 Magnification
- Row: key `magnification_preference_screen`; title `accessibility_screen_magnification_title` "Magnification"; icon `@drawable/ic_accessibility_magnification`
  (`settings10/res/drawable/ic_accessibility_magnification.xml`): vector 32 x 32 dp, viewport 192 x 192, three fills `#F50057` (a pentagon with a round hole, plus a circle) and
  shadow fills `#3E2723` alpha 0.2 / gradients `#333E2723` -> `#003E2723`; click = fragment `MagnificationPreferenceFragment`.
- Summary (`AccessibilitySettings.updateMagnificationSummary`): reads Secure `accessibility_display_magnification_enabled` (triple-tap, `==1`) and
  `accessibility_display_magnification_navbar_enabled` (button/shortcut, `==1`):
  both off -> `accessibility_feature_state_off` "Off"; navbar only -> `accessibility_screen_magnification_navbar_title` "Magnify with shortcut";
  triple-tap only -> `accessibility_screen_magnification_gestures_title` "Magnify with triple-tap"; both -> `accessibility_screen_magnification_state_navbar_gesture`
  "Magnify with shortcut & triple-tap". (Tablet shows "Off".) Strings `settings10/res/values/strings.xml` 4837-4843, 5042-5044.
- Redirect rule (`configureMagnificationPreferenceIfNeeded`): if `!MagnificationPreferenceFragment.isApplicable(res)` the row opens `ToggleScreenMagnificationPreferenceFragment`
  directly. `isApplicable` = `com.android.internal.R.bool.config_showNavigationBar` (framework default false, `fwbase/core/res/res/values/config.xml` 2216; the tablet has an
  on-screen navigation bar so its overlay presumably sets true: overlay NF). When true the row opens the intermediate page below.
- Intermediate page (`settings10/res/xml/accessibility_magnification_settings.xml`, title "Magnification"): two rows, both open `ToggleScreenMagnificationPreferenceFragment`:
  `screen_magnification_gestures_preference_screen` "Magnify with triple-tap" (controller `MagnificationGesturesPreferenceController`) and
  `screen_magnification_navbar_preference_screen` "Magnify with shortcut" (`MagnificationNavbarPreferenceController`, available only if `isApplicable`). Row summary = "On"/"Off"
  (`accessibility_feature_state_on/off`) from the same Secure key; extras: key, title res, summary res, video `R.raw.accessibility_screen_magnification` (gestures only), checked.
- Toggle page (`ToggleScreenMagnificationPreferenceFragment` over `ToggleFeaturePreferenceFragment`): top SwitchBar (see below) bound to the key; below it a video row
  (`settings10/res/layout/magnification_video_preference.xml`: RelativeLayout, ImageView `accessibility_screen_magnification_background` fitXY adjustViewBounds + `VideoView`
  centred horizontally, aligned top; `dimens.xml`: background 750 x 383 dp, video 170 x 311 dp, margin top 40 dp; video file `settings10/res/raw/accessibility_screen_magnification.mp4`),
  then the summary text `accessibility_screen_magnification_summary` (triple-tap) or `..._navbar_summary` (shortcut) with bold/list HTML, and a warning row (icon `ic_warning_24dp`)
  when the accessibility button points to another service (`accessibility_screen_magnification_navbar_configuration_warning`). Turning the shortcut ON shows the tutorial dialog
  (`DIALOG_ID_GESTURE_NAVIGATION_TUTORIAL`). Writes `Settings.Secure.putInt(key, 1|0)`.
- SwitchBar (`settings10/src/.../widget/SwitchBar.java`, layout `res/layout/switch_bar.xml`): LinearLayout; label `switch_text` weight 1, maxLines 2, `TextAppearance.Switch`
  (parent `DeviceDefault.Widget.ActionBar.Title`, 16 sp), colour `textColorPrimaryInverse`; text "On"/"Off" (`switch_on_text` / `switch_off_text`); `ToggleSwitch` at the end with theme
  `Widget.SwitchBar.Switch` (trackTint #BFFFFFFF, thumbTint white); theme overlay `ThemeOverlay.SwitchBar.Settings`: margin start 72 dp (`switchbar_subsettings_margin_start`),
  margin end 16 dp, background `switchbar_background_color` = `material_grey_600` (#757575). Whole bar clickable.
- Gotchas: both Secure keys readable by any app; writes need WRITE_SECURE_SETTINGS (held). Magnification takes effect through `AccessibilityManagerService` observing the key.
  The video and its background drawable are in the checkout; `config_showNavigationBar` can be read as a non-public framework resource by name (`Resources.getSystem().getIdentifier`).

### 1.5 Touch & hold delay
- Row: key `select_long_press_timeout_preference`; `ListPreference`, `persistent=false`; title `accessibility_long_press_timeout_preference_title` "Touch & hold delay"
  (XML `&amp;`, shown with an ampersand); entries `long_press_timeout_selector_titles` Short / Medium / Long (`arrays.xml` 705-712); values
  `long_press_timeout_selector_values` 400 / 1000 / 1500 (ms). Summary = the entry title of the current value (set in `updateSystemPreferences` and in
  `handleLongPressTimeoutPreferenceChange`). Click = the list dialog (single choice; title = row title; androidx dialog layout NF).
- Data: `Settings.Secure.putInt("long_press_timeout", Integer.parseInt(value))` (`Settings.Secure.LONG_PRESS_TIMEOUT`, constant visibility not checkable: use the string); read default =
  first array value (400). A stored value not in {400, 1000, 1500} selects nothing and gives a blank summary. Consumers: `ViewConfiguration.getLongPressTimeout()` (public) reads the setting.
- Gotchas: write needs WRITE_SECURE_SETTINGS (held). Our own long-press logic must call `ViewConfiguration.getLongPressTimeout()` to follow the value.

### 1.6 Mono audio
- Row: key `toggle_master_mono`; `SwitchPreference`, `persistent=false`; title `accessibility_toggle_master_mono_title` "Mono audio"; summary (static, both states)
  `accessibility_toggle_master_mono_summary` "Combine channels when playing audio". Standard two-line row + switch in widget_frame (`preference_widget_switch.xml` platform copy).
- Click: `Settings.System.putIntForUser(cr, MASTER_MONO, checked ? 1 : 0, USER_CURRENT)`. State: `Settings.System.getIntForUser(cr, MASTER_MONO, 0, USER_CURRENT) == 1`.
  Key string `master_mono` (Settings.System). Gotchas: `MASTER_MONO` is hidden (use the string); `putIntForUser` is hidden: use `Settings.System.putInt(cr, "master_mono", v)`
  (current user); writing the System table needs WRITE_SETTINGS (appop) or WRITE_SECURE_SETTINGS (held, the provider accepts it).

### 1.7 Audio balance
- Row: key `seekbar_master_balance`; class `accessibility/BalanceSeekBarPreference` (extends `widget/SeekBarPreference`, not selectable); layout set in the constructor =
  `settings10/res/layout/preference_balance_slider.xml`; title `accessibility_toggle_master_balance_title` "Audio balance"; no summary; labels `..._left_label` "Left",
  `..._right_label` "Right".
- Layout (whole file): LinearLayout horizontal, width match, height wrap, minHeight `listPreferredItemHeightSmall` (48 dp), gravity center_vertical, paddingStart/End 16 dp, clickable false.
  (a) `icon_frame` LinearLayout: minWidth 56 dp, paddingEnd 12 dp, paddingTop/Bottom 4 dp; `PreferenceImageView` 24 x 24 dp (invisible, space reserved).
  (b) vertical LinearLayout, width match_parent, marginTop 8 dp, marginBottom 8 dp:
      row 1 (horizontal): title TextView weight 1, singleLine, marquee, `TextAppearance.Material.Subhead` (16 sp), `textColorPrimary`, NO paddingStart; `widget_frame` wrap.
      row 2 (vertical): `BalanceSeekBar` (id `@*android:id/seekbar`), layout_gravity center_vertical, width match_parent, height wrap_content, NO style and NO padding attributes
      -> theme default `seekBarStyle` = `Widget.DeviceDefault.SeekBar` -> `Widget.Material.SeekBar` (`fwbase/core/res/res/values/styles_material.xml` 802): `paddingStart` 16 dip,
      `paddingEnd` 16 dip, `splitTrack` true, thumb `seekbar_thumb_material_anim`, track `seekbar_track_material`, background `control_background_32dp_material` (drawables NF);
      then a horizontal LinearLayout (layout_gravity center_vertical) with `left_text` and `right_text`: wrap_content, weight 1 each, maxLines 1, `textAlignment` viewStart / viewEnd,
      NO textAppearance or colour set -> `TextAppearance.DeviceDefault` = 14 sp `sans-serif`, `textColorPrimary` (not secondary).
- Horizontal geometry in dp (page width 564.7 dp): title x = 16 + 56 = 72; "Left" text x = 72; "Right" text ends at width - 16 = 548.7 (583 px); SeekBar paddings 16 + 16, so the
  track starts at 16 + 56 + 16 = **88 dp (93.5 px)** and ends 32 dp from the right edge (531.8 dp = 565 px) before the thumb inset.
  Volume slider for comparison (`settings10/res/layout/preference_volume_slider.xml`): icon_frame minWidth 44, title paddingStart 12, SeekBar height 48 dp with
  explicit paddingStart 12 dp and style paddingEnd 16 dip -> track starts 16 + 44 + 12 = 72 dp (measured x 77 px) and ends 32 dp from the right (measured 565 px). Both rows have marginTop/Bottom 8 dp.
- **DIFF**: SPOOF says the balance slider is "inset 16 dp more at BOTH ends than the volume sliders". The source gives +16 dp at the START only (88 vs 72 dp); the end is the same
  (32 dp from the right). Re-measure the end on the screenshot before building. SeekBar wrap_content height: NF (thumb drawable absent); volume slider fixes 48 dp.
- `BalanceSeekBar.java`: progress 0..200 (`BALANCE_MAX_VALUE` 200, centre 100); stored float = (progress - 100) * 0.01 in -1.0..1.0; snaps to the centre when within
  `SNAP_TO_PERCENTAGE` 0.03 of the range; draws a centre marker, 14 dp tall and 1 dp wide (`balance_seekbar_center_marker_height/width`, `settings10/res/values/dimens.xml`);
  `setProgressTintList(TRANSPARENT)` (no coloured fill; only the thumb shows the accent). Read: `Settings.System.getFloatForUser(MASTER_BALANCE, 0f, USER_CURRENT)`,
  progress = (int)(balance * 100) + 100. Write: `Settings.System.putFloatForUser(MASTER_BALANCE, value, USER_CURRENT)`. Key `master_balance` (Settings.System, float); the provider
  validator range -1..1 (memory).
- Gotchas: constants hidden: use `Settings.System.putFloat(cr, "master_balance", v)` (public method, current user; needs WRITE_SETTINGS appop or WRITE_SECURE_SETTINGS).

## 2. System page and Languages & input

### 2.1 System page
- Screen `settings10/res/xml/system_dashboard_fragment.xml` (key `system_dashboard_screen`, title `header_category_system` "System", `SystemDashboardFragment`,
  `settings:initialExpandedChildrenCount="4"`). Its rows are the XML preferences MERGED by `order` with dashboard tiles from `AndroidManifest.xml` (category
  `com.android.settings.category.ia.system`, meta-data `com.android.settings.order`). Ascending order:
  | order | row | title (resource) | summary | icon | opens |
  | -260 | Languages & input | `language_settings` "Languages & input" (a no-break space before "&", strings 4590) | label of the default keyboard (below) | `ic_settings_language` | `LanguageAndInputSettings` |
  | -250 | Gestures (`gesture_settings`) | `gesture_preference_title` "Gestures" | NF (controller `GesturesSettingPreferenceController` not read) | `ic_settings_gestures` | `GestureSettings` |
  | -240 | Date & time | `date_and_time` "Date & time" | NF here (tablet: "GMT-05:00 Eastern Standard Time") | `ic_settings_date_time` | `DateTimeSettings` |
  | -60 | Backup | `privacy_settings_title` "Backup" | from content provider (`summary_uri` `content://com.android.settings.backup.BackupSettingsContentProvider/summary`); tablet "Off" | `ic_settings_backup` | `UserBackupSettingsActivity` |
  | -50 | Reset options (`reset_dashboard`) | `reset_dashboard_title` "Reset options" | `reset_dashboard_summary` "Network, apps, or device can be reset" | `ic_restore` | `ResetDashboardFragment` |
  | -45 | Multiple users | `user_settings_title` | NF | `ic_settings_multiuser` | `UserSettings` (hidden on the tablet) |
  | -40 | Developer options | `development_settings_title` | empty | `ic_settings_development` | component `enabled=false` until Build number is tapped 7 times |
  | -31 / -30 | additional system update / System updates (`system_update_settings`) | `system_update_settings_list_item_title` "System updates" | placeholder, set by `SystemUpdatePreferenceController` (not read) | `ic_system_update` | intent `android.settings.SYSTEM_UPDATE_SETTINGS` |
- "Advanced" expander: `SystemDashboardFragment.onCreate`: if the visible preference count == initialExpandedChildrenCount + 1 (5 rows), the count becomes MAX_VALUE, so
  no "Advanced" row appears. This explains the real tablet showing exactly five rows (Languages & input, Gestures, Date & time, Backup, Reset options).
- Row summary for "Languages & input" (`language/LanguageAndInputSettings.SummaryProvider`): `Settings.Secure.getString("default_input_method")` (a flattened ComponentName)
  -> package name -> first `InputMethodManager.getInputMethodList()` entry with that package -> `imi.loadLabel(pm)`; empty string if none. Tablet: "Android Keyboard (AOSP)".
  All of these calls are public API; reading the Secure key needs no permission.

### 2.2 Languages & input (`settings10/res/xml/language_and_input.xml`, key `language_and_input_settings_screen`, title `language_settings`)
`initialExpandedChildrenCount` = 3. androidx counts leaf rows (memory), so the first three rows (Languages, Virtual keyboard, Physical keyboard) show and the rest hide behind an
"Advanced" expander row. Children in XML order:
1. `phone_language` title `phone_language` "Languages", icon `ic_translate_24dp`, fragment `localepicker/LocaleListEditor` (launched with title `language_picker_title` "Languages").
   Summary (`PhoneLanguagePreferenceController` -> `LocaleFeatureProviderImpl.getLocaleNames()`): `LocalePicker.getLocales()` (hidden) display names of up to TWO locales joined by a
   list formatter, sentence case, e.g. "English (United States)". Shown only if `config_show_phone_language` (true) and `AssetManager.getLocales().length > 1`.
2. Category `keyboards_category` heading `keyboard_and_input_methods_category` "Keyboards":
   - `virtual_keyboard_pref` title `virtual_keyboard_category` "Virtual keyboard"; summary = labels of all enabled input methods (`getEnabledInputMethodList()`, filtered by the
     device policy permitted list), each `BidiFormatter.unicodeWrap`, joined by `ListFormatter` ("A, B and C"); none -> `summary_empty`; shown if `config_show_virtual_keyboard_pref` (true);
     opens `inputmethod/VirtualKeyboardFragment`.
   - `physical_keyboard_pref` title "Physical keyboard", summary placeholder replaced by `PhysicalKeyboardPreferenceController` (NOT READ), fragment `PhysicalKeyboardFragment`;
     `config_show_physical_keyboard_pref` (true).
3. Category `input_assistance_category` heading `input_assistance` "Tools": `spellcheckers_settings` "Spell checker" (`config_show_spellcheckers_settings` true; summary
   set by `SpellCheckerPreferenceController`, NOT READ), `default_autofill` "Autofill service" (`AutofillPicker`), `key_user_dictionary_settings` "Personal dictionary" (`UserDictionaryList`).
4. Category `pointer_and_tts_category` (layout `@layout/preference_category_no_label`, no heading): `pointer_speed` "Pointer speed" (`PointerSpeedPreference`, dialog; shown only when a
   pointer device exists / `config_show_pointer_speed`, controller NOT READ), `tts_settings_summary` "Text-to-speech output" -> `TextToSpeechSettings` (`config_show_tts_settings_summary` true).
5. `vibrate_input_devices` SwitchPreference "Redirect vibration" / "Send vibration to game controller when connected" (only with a game controller, `GameControllerPreferenceController`).
6. `WorkOnlyCategory` "Work profile keyboards & tools" (three rows; only with a managed profile).
- Phone-only items: none by name; none of the strings above has a `product=` variant. Items that need extra hardware: Physical keyboard (summary), Pointer speed, Redirect vibration, work category.
- `config_show_*` booleans: `settings10/res/values/config.xml` lines 332 (`config_show_phone_language`), 335 (`..._virtual_keyboard_pref`), 338, 341, 344, 347.

### 2.3 Languages sub-page (`LocaleListEditor`, a `RestrictedSettingsFragment`; layout `settings10/res/layout/locale_order_list.xml`)
- Action bar: title "Languages"; options-menu item `locale_remove_menu` "Remove" (icon `ic_delete`, overflow normally, always shown in remove mode, visible only with > 1 language and not
  restricted). Remove mode: checkboxes appear, the add row is invisible; confirm dialog title plural `dlg_remove_locales_title`, message `dlg_remove_locales_message` "Text will be displayed in
  another language."; removing all -> "Can't remove all languages" / "Keep at least one preferred language". Restricted users see `language_empty_list_user_restricted`.
- Body: ScrollView > vertical LinearLayout, paddingTop/Bottom 8 dp: `dragList` RecyclerView (`LocaleRecyclerView`, vertical scrollbars) then Button `add_language`: height `listPreferredItemHeight`,
  paddingStart/End 16 dp, `drawableStart` `ic_add_24dp`, drawablePadding 24 dp, text "Add a language" (`add_a_language`), `textAlignment` start, style `Base.Widget.AppCompat.Button.Borderless`
  (AppCompat: NF), `textAppearanceListItem`.
- List row (`locale_drag_cell.xml`): minHeight `listPreferredItemHeight`, background `colorBackground`; `checkbox` paddingStart 16 dp (style `LanguageCheckboxAndLabel`, remove mode only);
  `label` paddingStart 48 dp; `miniLabel` (position number 1, 2, 3 in `colorAccent`, minWidth 24 sp, centred); `l10nWarn` (`locale_not_translated`, second line, paddingStart 48 dp, marginTop -28 dp,
  `textAppearanceListItemSecondary`, `textColorSecondary`; its visibility rule was NOT READ); `dragHandle` (`drag_handle`) at the end with paddingStart/End 16 dp; 1 dp bottom divider `?android:attr/listDivider`.
  Drag-and-drop reorders; the first row is the system language.
- Add flow: `LocalePickerWithRegionActivity` (internal `LocalePickerWithRegion` fragment; a search icon in its action bar, memory): rows `fwbase/core/res/res/layout/language_picker_item.xml` (TextView, minHeight
  `listPreferredItemHeight`, paddingStart/End 16 dp, paddingTop/Bottom 8 dp, `textAppearanceListItem`) and section headers `language_picker_section_header.xml` (36 dp high, `colorAccent`, bold, style
  `preferenceCategoryStyle`).
- Apply: `LocaleDragAndDropAdapter.doTheUpdate()` -> `LocalePicker.updateLocales(LocaleList)` after the drag animation stops (hidden class `com.android.internal.app.LocalePicker`, which calls
  `ActivityManager.getService().updatePersistentConfiguration`; needs `CHANGE_CONFIGURATION`, protection `signature|privileged|development` in `fwbase/core/res/AndroidManifest.xml` 2565, the same
  protection as WRITE_SECURE_SETTINGS, so `pm grant` from adb is possible: NOT TESTED).
  Public fallback: Intent action `Settings.ACTION_LOCALE_SETTINGS` ("android.settings.LOCALE_SETTINGS") opens the real page.

### 2.4 Virtual keyboard page (`settings10/res/xml/virtual_keyboard_settings.xml`, key `virtual_keyboard_category`, title "Virtual keyboard", `VirtualKeyboardFragment`)
- Rows are built in code (`onResume`): one `InputMethodPreference` (no switch) per ENABLED input method: key = IME id, title = `imi.loadLabel`, icon = `imi.loadIcon`, summary =
  `InputMethodAndSubtypeUtilCompat.getSubtypeLocaleNameListAsSentence(enabled subtypes)` (subtype `getDisplayName`, joined by `ListFormatter`, sentence case; empty list -> ""); sorted: system +
  ASCII-capable keyboards first, then `Collator` on the title. Click = start the IME's settings activity (`Intent.ACTION_MAIN`, component from `imi.getSettingsActivity()`); no activity -> no
  intent; `ActivityNotFoundException` -> toast `failed_to_open_app_settings_toast` "Failed to open settings for %1$s".
- Last row `add_virtual_keyboard_screen`, title `add_virtual_keyboard` "Manage keyboards", icon `ic_add_24dp`, opens `AvailableVirtualKeyboardFragment` (title `available_virtual_keyboard_category`
  "Available virtual keyboard"): every installed IME with a switch; enabling a non-system IME shows the `ime_security_warning` dialog, a non-direct-boot-aware IME the
  `direct_boot_unaware_dialog_message` dialog; an always-checked IME has a disabled switch. Enabling/disabling writes `ENABLED_INPUT_METHODS` (Secure).
- Current keyboard: there is NO "Current keyboard" row on these pages in r47. String `current_input_method` "Current Keyboard" exists (strings 4740) but is not used by them.
  The only "current keyboard" summary is the System-page one in 2.1 (default IME label).
- Gotchas: `InputMethodManager.getEnabledInputMethodList()`, `getInputMethodList()`, `getEnabledInputMethodSubtypeList(imi, true)`, `InputMethodInfo.loadLabel/loadIcon/getSettingsActivity`,
  `InputMethodSubtype.getDisplayName(ctx, pkg, appInfo)` and `android.icu.text.ListFormatter` are public. `DevicePolicyManager.getPermittedInputMethodsForCurrentUser` is hidden (null on an unmanaged tablet).
  `LocaleHelper` and `LocalePicker` are hidden (use `Locale.getDisplayName`, `LocaleList.getAdjustedDefault()` public). Switching the default keyboard needs WRITE_SECURE_SETTINGS.

## 3. About tablet

Page file: r47 uses `settings10/res/xml/my_device_info.xml` (key `my_device_info_pref_screen`, title `about_settings`: product tablet "About tablet", default "About phone"),
class `deviceinfo/aboutphone/MyDeviceInfoFragment.java`. DIFF with the brief's file name: there is no `device_info_settings.xml` in r47.
Rows by `order` (key; title res; notes). Hidden on the tablet per SPOOF: owner header, Device name, Emergency information, IP/MAC/BT/Up time, Build number, Software updates.
 0 `my_device_info_header` (`settings_entity_header`, selectable false, initially invisible) | 1 `device_name` "Device name" | 2 `branded_account` "Account" | 3 `phone_number` (tablet: "MDN", selectable false)
 14 `emergency_info` "Emergency information" | **15 `legal_container` "Legal information"** (fragment `LegalSettings`, `allowDividerAbove`) | 16 `regulatory_info` (hidden: `config_show_regulatory_info` false)
 17 `safety_info` (hidden) | 18 `sim_status` "SIM status" (`allowDividerAbove`; hidden on Wi-Fi only) | **31 `device_model` "Model & hardware"** | 32 `imei_info` "IMEI" | **42 `firmware_version` "Android version"**
 44 `wifi_ip_address` "IP address" (selectable false, `allowDividerAbove`) | 45 `wifi_mac_address` "Wi-Fi MAC address" (`Wi‑Fi`) | 46 `bt_address` "Bluetooth address" | 47 `up_time` "Up time" (all selectable false)
 50 `manual` (hidden: `config_show_manual` false) | 51 `device_feedback` (hidden when no OEM feedback reporter) | 52 `fcc_equipment_id` | 53 `build_number` "Build number" (`allowDividerAbove`).
- Divider logic and the tablet: dividers appear above rows 15, 18, 44, 53 (`allowDividerAbove`); the tablet's visible groups [Device name, Emergency] | [Legal, Model & hardware, Android version] |
  [IP, MAC, Bluetooth, Up time] | [Custom build version, Build number, Software updates] match order 14 | 15-42 | 44-47 | 53. The two vendor rows (Custom build version, Software updates) are NF in r47.
- Gray rows: r47 only sets `android:selectable="false"` on `phone_number`, `wifi_ip_address`, `wifi_mac_address`, `bt_address`, `up_time`. A non-selectable androidx row has no ripple/click but keeps the normal
  text colours (memory); nothing in settings10 or SettingsLib greys it (`isSelectable`/`setSelectable` grep: no styling). **DIFF**: the tablet draws these four rows gray (disabled style): the
  colour is NF in the source; measure it on the screenshots. Disabled text colours: NF (`color/` lists absent).
- Strings: `firmware_version` "Android version" (3059), `hardware_info` "Model & hardware" (3067), `model_info` "Model" (3063), `model_summary` "Model: %1$s" (3065), `build_number` "Build number" (3077),
  `legal_information` "Legal information" (3959), `summary_placeholder` = U+00A0 (2643); `about_settings_summary` (Settings home row) "View legal info, status, software version".

### 3.1 Legal information (`settings10/src/.../LegalSettings.java`, `res/xml/about_legal.xml`, key `legal_screen`, title "Legal information")
- Rows in order: `copyright` "Copyright", `license` "License", `terms` "Terms and conditions", `module_license` "Google Play system update licenses", `webview_license` "System WebView License",
  `wallpaper_attributions` "Wallpapers" (summary `wallpaper_attributions_values` "Satellite imagery providers:\n©2014 CNES / Astrium, DigitalGlobe, Bluesky"; no intent or fragment, so it does nothing;
  shown if `config_show_wallpaper_attribution`, true in r47).
- Behaviour (`deviceinfo/legal/LegalPreferenceController`): the first four rows are visible only if `PackageManager.queryIntentActivities(Intent(action))` finds an activity of a SYSTEM app
  (`FLAG_SYSTEM`). The row's intent becomes that explicit component and its TITLE becomes that activity's label, so the XML titles above are NOT shown when a handler exists. Actions:
  `android.settings.COPYRIGHT`, `android.settings.LICENSE`, `android.settings.TERMS`, `android.settings.WEBVIEW_LICENSE`. `module_license` opens fragment `ModuleLicensesDashboard`
  and is shown only if an installed mainline module has a license provider (`PackageManager.getInstalledModules`).
- "Open source licenses": the string does NOT exist in r47 (grep: NF). The `LICENSE` handler is Settings' own `SettingsLicenseActivity` (`AndroidManifest.xml` 976-987, label
  `settings_license_activity_title` = "Third-party licenses", strings 3995; DIFF if the tablet shows "Open source licenses", a vendor or Go label). It shows text like this: if
  `/system/etc/NOTICE.html.gz` exists and is non-empty -> `ACTION_VIEW` with that `file:` URI, type `text/html`, `EXTRA_TITLE` "Third-party licenses", package pinned to `com.android.htmlviewer`
  (HTMLViewer decompresses the .gz itself); otherwise `LicenseHtmlLoaderCompat` builds `NOTICE.html` in the app cache from the NOTICE xml.gz files in `/system/etc`, `/vendor/etc`, `/odm/etc`,
  `/oem/etc`, `/product/etc`, `/product_services/etc` (`fwbase/packages/SettingsLib/src/com/android/settingslib/license/LicenseHtmlLoaderCompat.java`) and opens that through a FileProvider URI.
  Failure: toast `settings_license_activity_unavailable` "There is a problem loading the licenses."
- Gotchas: whether the tablet has `/system/etc/NOTICE.html.gz` and `com.android.htmlviewer`: NOT KNOWN. A non-system app can read world-readable files under `/system/etc` and render the
  gunzipped HTML in its own WebView (`loadDataWithBaseURL`); the real file can be several MB. `Intent("android.settings.LICENSE")` can be resolved and started by any app (the activity is exported
  by its intent filter), which opens the real page.

### 3.2 Model & hardware
- Row `device_model` (order 31), controller `deviceinfo/HardwareInfoPreferenceController` (shown if `config_show_device_model`, true): summary = `model_summary` "Model: %1$s" with
  `Build.MODEL + DeviceInfoUtils.getMsvSuffix()`; msv suffix = " (ENGINEERING)" if `/sys/board_properties/soc/msv` parses as hex 0, else "" (file absent -> ""). Tablet: the Settings home About row shows "711" = `Build.MODEL` (SPOOF section 6).
  Click = fragment `deviceinfo/hardwareinfo/HardwareInfoFragment` (`res/xml/hardware_info.xml`, key `hardware_info_screen`, title "Model & hardware"), rows (all `enableCopying`, placeholder summary):
  `hardware_info_device_model` "Model" = `Build.MODEL` + msv suffix; `hardware_info_device_serial` "Serial number" (`status_serial_number`) = `Build.getSerial()`;
  `hardware_info_device_revision` "Hardware version" (`hardware_revision`) = `SystemProperties.get("ro.boot.hardware.revision")` (shown with an empty summary when unset).
- Gotchas: `Build.MODEL` public. `Build.getSerial()` throws SecurityException for an app targeting API 29 without READ_PRIVILEGED_PHONE_STATE (`signature|privileged`, `fwbase/core/res/AndroidManifest.xml`
  2014; not adb-grantable) or carrier / device-owner status; `Build.SERIAL` is "unknown": the serial row has NO usable source (NF). `SystemProperties` is hidden (reflection is greylist at API 29; the SELinux
  read right of `ro.boot.hardware.revision` for an untrusted app is NOT KNOWN). The shell helper can run `getprop`.

### 3.3 Android version
- Row `firmware_version` (order 42), controller `firmwareversion/FirmwareVersionPreferenceController`: summary `Build.VERSION.RELEASE` ("10"); click = fragment `FirmwareVersionSettings`
  (`res/xml/firmware_version.xml`, key `firmware_version_screen`, title "Android version"). Rows in order (all copyable, placeholder summary):
  1. `os_firmware_version` "Android version" = `Build.VERSION.RELEASE`; three taps within 500 ms (array of 3 `uptimeMillis` stamps) start `com.android.internal.app.PlatLogoActivity` in package `android`
     (blocked by `DISALLOW_FUN`).
  2. `security_key` "Android security patch level" (`security_patch`) = `DeviceInfoUtils.getSecurityPatch()`: `Build.VERSION.SECURITY_PATCH` parsed as `yyyy-MM-dd`, shown with the best pattern for "dMMMMyyyy"
     (en-US: "November 5, 2019"); raw string if unparsable; row hidden if empty. Click = `ACTION_VIEW https://source.android.com/security/bulletin/` if a handler exists.
  3. `module_version` "Google Play system update" (`module_version`) = `versionName` of the package in `config_defaultModuleMetadataProvider`; hidden if empty.
  4. `base_band` "Baseband version" = `SystemProperties gsm.version.baseband` (default `device_info_default`); hidden on Wi-Fi-only devices (`Utils.isWifiOnly`).
  5. `kernel_version` "Kernel version" = `Os.uname()` formatted as `release` + newline + `#N date`: regex `(#\d+) (?:.*?)?((Sun|Mon|Tue|Wed|Thu|Fri|Sat).+)`; no match -> `status_unavailable`.
  6. `os_build_number` "Build number" = `BidiFormatter.unicodeWrap(Build.DISPLAY)`; NO tap counter on this page.
- Build number row on the About page (key `build_number`, controller `deviceinfo/BuildNumberPreferenceController`): summary `BidiFormatter.unicodeWrap(Build.DISPLAY)`. Tap counter: `TAPS_TO_BE_A_DEVELOPER` 7;
  counter starts at 7, or -1 if developer options are already on (`Settings.Global.development_settings_enabled`); each tap decrements; toasts with the remaining steps (plural `show_dev_countdown`) only while
  remaining is 4..1; at 0 it asks for the device credential (`ChooseLockSettingsHelper`), or enables at once when no lock is set; success toast `show_dev_on` "You are now a developer!"; later taps
  `show_dev_already` "No need, you are already a developer." (long). Ignored for monkey, secondary non-demo users, an unprovisioned device, `DISALLOW_DEBUGGING_FEATURES`. Enabling =
  `DevelopmentSettingsEnabler.setDevelopmentSettingsEnabled(ctx, true)` (Global `development_settings_enabled` = 1, and the Developer options component is enabled).
- "Custom build version" (tablet text "vnd_tb8168p1_bsp 1779111601"): NOT FOUND in r47 (vendor row; its source property is unknown; candidates only from the shape of the string).
- Gotchas: `Build.VERSION.RELEASE`, `Build.VERSION.SECURITY_PATCH`, `Build.DISPLAY`, `Build.MODEL`, `android.system.Os.uname()`, `PackageManager.getPackageInfo` are public. Whether `PlatLogoActivity` can be started by another app: NOT CHECKED.
  Writing `development_settings_enabled` needs WRITE_SECURE_SETTINGS (held) but also the component switch; leave the counter out unless Tj wants it (Build number is REMOVE on the tablet spoof).

## 4. Summary of every DIFF and NF
- DIFF Font size: r47 maps font scale 1.2 to "Large"; the tablet shows "Default" at 1.2. Read the tablet's own array values.
- DIFF Audio balance slider inset: source = +16 dp at the start only (track from 88 dp); SPOOF says both ends.
- DIFF TTS buttons: r47 uses "Play" and "Reset" (not "Listen to an example").
- DIFF Time to take action title; About file name (`my_device_info.xml`); "Third-party licenses" vs "Open source licenses"; gray disabled rows (no gray in r47).
- NF: Custom build version, Software updates, owner header details, vendor array overlays, `config_showNavigationBar` of the tablet, androidx layouts/styles, `color/` lists, thumb/track drawables,
  Gestures / Date & time summaries, Physical keyboard / Spell checker / Pointer speed controllers, serial-number source for a normal app.
- Runtime shortcut that removes most DIFF/NF (NOT TESTED on the tablet): a non-system app can open the installed Settings package's resources by name
  (`createPackageContext("com.android.settings", 0)` + `getIdentifier(name, "string"|"array"|"dimen", "com.android.settings")`), which yields the tablet's real titles, arrays and overlays.
