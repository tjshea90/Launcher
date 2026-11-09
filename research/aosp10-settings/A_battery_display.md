# AOSP 10 Settings facts: Battery page + five Display entries (pixel-identical re-creation in a third-party app)

Tree android-10.0.0_r47. Source names: settings10/res/..., settings10/src/com/android/settings/... (Settings app); fwbase/packages/SettingsLib/... (incl. sub-modules AppPreference/res,
LayoutPreference/res, ActionBarShadow/src); fwbase/core/res/res/values/*.xml (framework resources). Facts only, no design proposals.
Tags: [SRC] read in the checkout. [MEM] from memory; the file is NOT in the checkout (framework Java, SettingsProvider, WindowManagerService, SystemUI, androidx preference/appcompat,
core/res color/ and drawable/ dirs). NOT FOUND = could not be established. At 170 dpi and font scale 1.2: 1 dp = 1.0625 px, 1 sp = 1.275 px (sizes written in dp do not follow font scale).
"Tablet" = Tj's measurements: research/SETTINGS_SPOOF_2026-11-09.md (sec. 1, 4b, 4c, 6) and research/tablet/2026-11-05/tablet-report.txt (600x973 window, 170 dpi, font scale 1.1999817).

## 0. Where AOSP and the real tablet disagree (D) or are unknown
D1 Display page: display_settings.xml has initialExpandedChildrenCount=5 [SRC] (rows after the 5th fold behind an "Advanced" row). The tablet shows ten flat rows and no expander
   (Brightness level, Night Light, Adaptive brightness, Wallpaper, Dark theme, Screen timeout, Auto-rotate screen, Font size, Display size, Lock screen display). The device build differs.
D2 Font size label: AOSP entryvalues_font_size = 0.85 / 1.0 / 1.15 / 1.30 = Small / Default / Large / Largest [SRC]; fontSizeValueToIndex(1.2) = index 2 = "Large".
   Tablet: font scale 1.1999817 yet the row reads "Default" -> the device's arrays differ. Device values and labels: NOT FOUND (the Font size page was not photographed).
D3 Display size: AOSP table for this tablet = 144 / 170 / 198 / 226 / 254 dpi (sec. 4.5); the tablet row reads "Default" at 170 dpi: agrees.
D4 Accent: AOSP colorAccent #008577 (accent_material_light = material_deep_teal_500) [SRC]; PIL measurement on the tablet #008577: agrees (the notes' by-eye "about #00897B" is not right).
D5 Brightness limits 10 / 255 / default 102 are framework defaults (core config.xml); a device overlay is NOT FOUND. Tablet row reads "25%".
D6 Screen timeout list = AOSP list (sec. 4.2); tablet shows "After 1 minute of inactivity" (value 60000, in the list). A device overlay is NOT FOUND.
D7 Battery page: AOSP row set and strings agree with the screenshots (tip card, Battery Saver "Off", Battery Manager "On / Detecting when apps drain battery", Battery percentage switch).

## 1. Shared theme, toolbar and row structure
THEME [SRC] Theme.Settings (settings10/res/values/themes.xml) -> Theme.SettingsBase -> android:Theme.DeviceDefault.Settings (public API since 21) -> Theme.DeviceDefault.Light.
  Theme.Settings adds: listPreferredItemHeight 72dip, homeAsUpIndicator @drawable/ic_arrow_back (24 dp vector, path M20,11H7.83l5.59-5.59L12,4l-8,8l8,8l1.41-1.41L7.83,13H20V11z, tint colorControlNormal),
  navigationBarColor transparent, preferenceTheme PreferenceTheme, actionBarStyle Widget.ActionBar (contentInsetStart 16 dp). DeviceDefault.Settings itself: windowLightStatusBar, solid action bar
  (Widget.DeviceDefault.Light.ActionBar.Solid), white nav bar + divider #1F000000, colorListDivider, dialogCornerRadius 2 dp (config_dialogCornerRadius).
  Settings$PowerUsageSummaryActivity and Settings$DisplaySettingsActivity use the application theme Theme.Settings. Sub-pages (Battery usage, Font size, Display size) run in SubSettings with
  Theme.SubSettings: actionBarStyle Widget.ActionBar.SubSettings = contentInsetStart 72 dp (toolbar title at x 72 dp = 76.5 px).
COLOURS [SRC colors_material.xml, colors_device_defaults.xml]: colorPrimary (toolbar) #F5F5F5 | colorPrimaryDark (status bar, battery header) #E0E0E0 | colorBackground #FAFAFA | colorBackgroundFloating #FFFFFF |
  colorAccent #008577 | colorError #FF5722 | colorListDivider #FFDADCE0 | secondaryContentAlpha 0.54, primaryContentAlpha 0.87, disabledAlpha 0.26. textColorPrimary / textColorSecondary are selectors in
  core/res color/ (NOT FOUND); their base colours are primary_text_default_material_light #DE000000 and secondary_text_default_material_light #8A000000; textColorTertiary = @color/secondary_text_material_light.
TEXT [SRC styles_material.xml, styles_device_defaults.xml, donottranslate_material.xml]: TextAppearance.Material = 14 sp, sans-serif, textColorPrimary. ListItem = Subhead 16 sp textColorPrimary; ListItemSecondary
  = Body1 14 sp; Material.Small = 14 sp textColorTertiary; Caption 12 sp textColorSecondary; Title 20 sp (sans-serif-medium); Headline 24 sp. DeviceDefault.* adds fontFamily config_bodyFontFamily (sans-serif).
  DeviceDefault.Widget.ActionBar.Title = config_headlineFontFamilyMedium (= font_family_button_material = sans-serif-medium), 20 dp (text_size_title_material_toolbar), textColorPrimary.
ROWS: listPreferredItemPaddingStart/End = list_item_padding_*_material = action_bar_content_inset_material = 16 dp [SRC]; listPreferredItemHeightSmall 48 dip; Settings sets listPreferredItemHeight 72 dip
  (preference_footer.xml uses it; the rows below use ...Small). A plain Preference row is androidx preference_material.xml (NOT FOUND); SettingsLib preference_two_target.xml / preference_app.xml copy it: LinearLayout horizontal,
  minHeight 48 dp, paddingStart/End 16 dp, icon_frame minWidth 56 dp (paddingTop/Bottom 4 dp), text block paddingTop/Bottom 16 dp = title (singleLine, marquee, textAppearanceListItem) above summary
  (textAppearanceListItemSecondary, textColorSecondary, maxLines 10), widget_frame minWidth 64 dp. androidx Preference.Material sets iconSpaceReserved=true [MEM], so a row without icon keeps the 56 dp
  frame INVISIBLE: title x = 16 + 56 = 72 dp = 76.5 px (tablet 77 px). Tablet row pitch with that structure: one-line 62 px, two-line 87 px.
DIVIDERS: SettingsPreferenceFragmentStyle sets android:divider = @*android:drawable/list_divider_material (drawable NOT FOUND; tablet: 1 px #DADCE0). androidx DividerDecoration draws a line below a
  row only if that row allows a divider below (default true) AND the next row allows one above (default false) [MEM]: lines appear only before rows with settings:allowDividerAbove="true"
  (Last full charge, the footer) and categories. The Display page has none between its rows.
SWITCH rows (SwitchPreference): androidx widget layout preference_widget_switch (platform Switch, Material style) inside widget_frame [MEM]; track/thumb drawables NOT FOUND (core/res drawable/ absent).
SCROLL SHADOW [SRC fwbase/packages/SettingsLib/ActionBarShadow/.../ActionBarShadowController.java]: ELEVATION_HIGH = 8 (px), ELEVATION_LOW = 0; setElevation(8) on the action bar while the list canScrollVertically(-1).

## 2. BATTERY page (Settings$PowerUsageSummaryActivity -> fragment PowerUsageSummary)
ENTRY [SRC AndroidManifest.xml]: action android.intent.action.POWER_USAGE_SUMMARY; label power_usage_summary_title "Battery"; fragment com.android.settings.fuelgauge.PowerUsageSummary
  (PowerUsageBase > DashboardFragment); screen settings10/res/xml/power_usage_summary.xml (key power_usage_summary_screen). Toolbar title "Battery".
REFRESH [SRC BatteryBroadcastReceiver.java, PowerUsageSummary.java]: registers for ACTION_BATTERY_CHANGED + ACTION_POWER_SAVE_MODE_CHANGED; update type MANUAL(0) on the sticky first call, then BATTERY_LEVEL(1) if the
  level text changed, else BATTERY_STATUS(3) if the status text changed; each triggers reload of BatteryInfo (loader) and of the two PowerGauge rows. The header percent/icon are ALSO set at once from the
  sticky broadcast (quickUpdateHeaderPreference): level = EXTRA_LEVEL*100/EXTRA_SCALE (integer division), charging = EXTRA_PLUGGED != 0, power save = PowerManager.isPowerSaveMode().
TOOLBAR [SRC EntityHeaderController.java]: BatteryHeaderPreferenceController.onStart -> styleActionBar: action bar background ColorDrawable(colorPrimaryDark #E0E0E0), elevation 0 (toolbar and header read as ONE
  grey block), ActionBarShadowController attached to the list (elevation 8 px once scrolled). Overflow menu [SRC PowerUsageSummary]: ONE item, id MENU_ADVANCED_BATTERY, title advanced_battery_title
  "Battery usage", no icon, not shown as action (a DEBUG-only stats item is compiled out; no help item: help_url_battery is empty). Overflow glyph = theme attr ic_menu_moreoverflow ->
  @*android:drawable/ic_menu_moreoverflow_holo_dark (drawable NOT FOUND). Click: SubSettingLauncher(ctx).setDestination(PowerUsageAdvanced).setTitleRes(advanced_battery_title).launch() = SubSettings, sec. 3.
ROWS in order [SRC power_usage_summary.xml]:
 1 battery_header     LayoutPreference (fwbase/packages/SettingsLib/LayoutPreference), layout battery_header, selectable=false, no dividers around (allowDivider* default false); frame = FrameLayout match x wrap.
 2 battery_tip        CardPreference (layout card_preference_layout.xml, NOT READ); default tip: battery_tip_summary_title "Apps are running normally" / battery_tip_summary_summary[product=tablet]
                      "Tablet has typical background battery usage". Not wanted by Tj.
 3 battery_saver_summary  Preference, title battery_saver "Battery Saver", fragment BatterySaverSettings; summary (BatterySaverController.getSummary): isPowerSaveMode -> battery_saver_on_summary "On"; else if
                      Global AUTOMATIC_POWER_SAVE_MODE == POWER_SAVE_MODE_TRIGGER_PERCENTAGE (default) -> Global LOW_POWER_MODE_TRIGGER_LEVEL != 0 ? "Will turn on at <n%>" : battery_saver_off_summary "Off";
                      other mode -> battery_saver_auto_routine "Based on your routine". Not wanted by Tj.
 4 smart_battery_manager  Preference, title smart_battery_manager_title "Battery Manager", fragment SmartBatterySettings; summary: restricted apps n > 0 -> plural battery_manager_app_restricted, else feature on
                      (Global ADAPTIVE_BATTERY_MANAGEMENT_ENABLED or APP_AUTO_RESTRICTION_ENABLED, default 1) -> battery_manager_on "On / Detecting when apps drain battery", else "Off". Not wanted.
 5 battery_percentage SwitchPreference, title battery_percentage "Battery percentage", summary battery_percentage_description "Show battery percentage in status bar"; Settings.System SHOW_BATTERY_PERCENT
                      (read/write putInt 0/1; the constant is hidden [MEM]). Not wanted.
 6 last_full_charge   PowerGaugePreference, selectable=false, allowDividerAbove=true (sec. 2.4)          WANTED
 7 screen_usage       PowerGaugePreference, selectable=false (sec. 2.4)                                    WANTED
 8 footer_preference  FooterPreference built in code (sec. 2.5)                                            WANTED

### 2.1 Header [SRC settings10/res/layout/battery_header.xml; fwbase/packages/SettingsLib/LayoutPreference/res/values/styles.xml]
root LinearLayout id battery_entity_header, horizontal, match x match, style EntityHeader = background ?colorPrimaryDark (#E0E0E0), paddingTop 24 dp, paddingBottom 16 dp, paddingEnd 16 dp, paddingStart 0.
  A: vertical LinearLayout 170 dp wide, wrap high, marginStart 72 dp, marginEnd 8 dp:
     battery_percent TextView match x wrap, marginTop 12 dp, textAppearance EntityHeaderTitle (parent Material.Subhead; fontFamily config_headlineFontFamilyMedium = sans-serif-medium; textColorPrimary;
       textSize 20sp) but the layout sets android:textSize="36sp", which wins: 36 sp = 45.9 px.
     summary1 TextView match x wrap, marginTop 8 dp, minLines 2, textAppearance android:style/TextAppearance.DeviceDefault.Small (14 sp, sans-serif, colour textColorTertiary = 54% grey).
     summary2 TextView match x wrap, same textAppearance, text "" (set by the controller).
     SOURCE BUG: two stray lines 'android:textColor="?android:attr/textColorPrimary"/>' follow summary1 and summary2 OUTSIDE any tag: never applied, so both stay textColorTertiary.
  B: com.android.settings.fuelgauge.BatteryMeterView id battery_header_icon, width match_parent, height @dimen/battery_meter_height (100 dp), layout_gravity center.
Tablet check: header height 197 px (grey block y 26..283 = 56 dp toolbar + header); icon: left edge = 250 dp + (298.7 - 66) / 2 dp = 366.4 dp = 389 px (measured 390), 66 x 100 dp = 70 x 106 px
  (measured 70 x 106), top = header top (y 86) + 25.5 + (154.5 - 106.25) / 2 px = y 136 (measured 136; 154.5 px = 197 - 25.5 - 17 = height of the text column); percent text x = 72 dp = 76.5 px.
Percent text: string battery_header_title_alternate = '^1<small> <font size="20">%</font></small>' [SRC settings10/res/values/strings.xml], filled with TextUtils.expandTemplate(getText(...),
  android.icu NumberFormat.getIntegerInstance().format(level)) -> "84" + small "%". Span meaning [MEM, StringBlock]: <small> = RelativeSizeSpan 0.8, <font size="20"> = AbsoluteSizeSpan(20, dip=true):
  the inner span wins, so "%" is about 20 dp (21 px), not scaled by font scale; the space before it is 0.8 x 36 sp.
summary1 = info.remainingLabel, or info.statusLabel when that is null (sec. 2.3). Icon = BatteryMeterView.setBatteryLevel(level) / setCharging(!info.discharging) / setPowerSave(isPowerSaveMode()).

### 2.2 Big battery icon [SRC BatteryMeterView.java; fwbase/packages/SettingsLib/src/com/android/settingslib/graph/ThemedBatteryDrawable.kt; BatteryMeterDrawableBase is legacy and NOT used here]
Drawable intrinsic size = battery_meter_width 66 dp x battery_meter_height 100 dp [SRC settings10/res/values/dimens.xml]. The ImageView (FIT_CENTER) is 298.7 dp wide on the tablet, so the drawable is drawn 1:1, centred.
Paths live on a 12 x 20 grid, scaled by (boundsRight/12, boundsBottom/20) = 5.84 x 5.31 px per unit: the icon is about 10% wider than the grid aspect. Path data [SRC core config.xml lines 4053-4069]:
  perimeter (also the error perimeter): M3.5,2 v0 H1.33 C0.6,2 0,2.6 0,3.33 V13v5.67 C0,19.4 0.6,20 1.33,20 h9.33 C11.4,20 12,19.4 12,18.67 V13V3.33 C12,2.6 11.4,2 10.67,2 H8.5 V0 H3.5 z M2,18v-7V4h8v9v5H2L2,18z
  fill mask: M2,18 v-14 h8 v14 z (interior x 2..10, y 4..18) | bolt: M5,17.5 V12 H3 L7,4.5 V10 h2 L5,17.5 z | power-save plus: M9,10l-2,0l0,-2l-2,0l0,2l-2,0l0,2l2,0l0,2l2,0l0,-2l2,0z | config_batterymeterDualTone false.
Drawing (draw()): shape = perimeter UNION level rectangle; level rectangle = fill-mask bounds with top = floor(fillTop), fillTop = maskTop if level >= 95 else maskTop + maskHeight * (1 - level/100).
  ONE colour for everything: the drawable's colour filter replaces the colour (PorterDuff SRC), so the shape is a flat accent #008577: ring outline plus the solid fill, the unfilled part stays transparent.
  Filter (BatteryMeterView.updateColorFilter): power save on -> colorForeground (SRC); else level < criticalLevel (config_criticalBatteryWarningLevel = 5) -> color/battery_icon_color_error
  (colorError at secondaryContentAlpha = #FF5722 at 54%, SRC_IN); else accent. Power save additionally paints the error perimeter and the plus with errorPaint = colorError #FF5722 (unfiltered, SRC).
  Charging: the bolt is cut out of the shape (DIFFERENCE). invertFillIcon = true when level >= 67, false when level <= 33, unchanged between. Not inverted: bolt also painted solid; inverted: stays a hole.
  Then clipOut(bolt) and a stroke of width max(boundsRight/12 * 3, 6) px (= 17.5 px here) around the bolt: CLEAR blend (transparent ring) when not inverted, frame colour when inverted.
  Level <= 15 (companion CRITICAL_LEVEL) and not charging: the level rectangle is painted a second time inside the fill mask (no change with the opaque accent; with the 54% error filter it doubles the
  fill's opacity to about 79%). Tablet: icon colour #008577, 70 x 106 px at x 390-459, y 136-241.

### 2.3 Summary text logic [SRC BatteryInfo.java; fwbase/packages/SettingsLib/src/com/android/settingslib/utils/PowerUtil.java, StringUtil.java; Utils.java]
discharging = EXTRA_PLUGGED == 0. batteryPercentString = NumberFormat.getPercentInstance().format(level/100). Estimate source = BatteryStats.computeBatteryTimeRemaining (hidden); the "based on your usage"
  variants need a PowerUsageFeatureProvider that enables enhanced prediction (AOSP provider: off), so they never appear. Discharging with drain time ms > 0 -> remainingLabel =
  PowerUtil.getBatteryRemainingStringFormatted(ctx, ms, null, false):
  ms <= 7 min                -> power_remaining_duration_only_shutdown_imminent[product=tablet] "Tablet may shutdown soon"
  7 min < ms <= 15 min       -> power_remaining_less_than_duration_only "Less than 15 min remaining" (time text = formatElapsedTime(15 min))
  ms >= 2 days               -> power_remaining_only_more_than_subtext "More than 2 days remaining" (ICU MeasureFormat SHORT of 2 days)
  1 day <= ms < 2 days       -> power_remaining_duration_only "About 1 day, 3 hr left" (ms rounded to the nearest hour, then formatElapsedTime SHORT)
  otherwise (15 min..1 day)  -> power_discharge_by_only "Should last until about 9:15 AM": now + ms rounded to the NEAREST 15 min of wall-clock (remainder < 7.5 min rounds down), formatted by
                                ICU DateFormat.getInstanceForSkeleton(android.text.format.DateFormat.getTimeFormatString(ctx)) = "9:15 AM" (12 h) or "09:15" (24 h) per the user's setting.
  ms <= 0: remainingLabel null -> header shows statusLabel = Utils.getBatteryStatus(EXTRA_STATUS): battery_info_status_charging "Charging", _discharging "Not charging", _not_charging
  "Plugged in, can't charge right now", _full "Full", else _unknown "Unknown".
Charging: chargeTime = BatteryStats.computeChargeTimeRemaining(); if > 0 and status != FULL: remainingLabel = power_remaining_charging_duration_only "<time> left until fully charged" (formatElapsedTime SHORT);
  else remainingLabel null (statusLabel shown).
StringUtil.formatElapsedTime(ms, withSeconds=false): seconds = floor(ms/1000) + 30; split into days / hours / minutes, zero parts dropped, all zero = "0 min"; ICU MeasureFormat SHORT, en: "59 min",
  "1 hr, 3 min", "1 hr, 16 min", "2 hr", "1 day, 3 hr" (comma + space between units).
StringUtil.formatRelativeTime(ms, withSeconds=false, LONG): s = floor(ms/1000); s < 2 h -> minutes = (s + 30) / 60 ("109 minutes ago"); s < 2 days -> hours = (s + 1800) / 3600 ("2 hours ago");
  else days = (s + 43200) / 86400 ("3 days ago"); RelativeDateTimeFormatter LONG, Direction.LAST, CAPITALIZATION_FOR_MIDDLE_OF_SENTENCE.

### 2.4 Last full charge / Screen usage rows [SRC PowerGaugePreference.java, AppPreference (fwbase/packages/SettingsLib/AppPreference), preference_app.xml, preference_widget_summary.xml, BatteryUtils.java]
Class PowerGaugePreference extends AppPreference (layout preference_app, widget layout preference_widget_summary), selectable=false. preference_app.xml: LinearLayout horizontal, minHeight
  listPreferredItemHeightSmall (48 dp), paddingStart/End 16 dp, selectableItemBackground; icon_frame (LinearLayout minWidth 56 dp, paddingEnd 8 dp, paddingTop/Bottom 4 dp, start|center_vertical; ImageView
  secondary_app_icon_size 32 dp; invisible but reserved when no icon [MEM]); text column weight 1, paddingTop/Bottom 16 dp: title (ellipsize marquee, singleLine, textAppearanceListItem 16 sp), summary
  (textAppearanceSmall 14 sp, textColorSecondary; empty here), appendix (gone), ProgressBar (gone, marginTop 4 dp, max 100); widget_frame (wrap x match, minWidth 64 dp, gravity center_vertical|end)
  holding TextView widget_summary: match x wrap, drawablePadding 8 dp, gravity center_vertical|end, textAlignment viewEnd, textAppearanceSmall 14 sp, textColorSecondary, start drawable ic_warning_24dp
  only for an anomaly (off). Result: title at x 72 dp, value right-aligned to 16 dp from the right edge.
last_full_charge: title battery_last_full_charge "Last full charge"; value = StringUtil.formatRelativeTime(now - BatteryStats.getStartClockTime(), false) -> "109 minutes ago", "2 hours ago"
  (BatteryUtils.calculateLastFullChargeTime = currentTimeMs - stats.getStartClockTime()). Alternate only when a provider supplies averageTimeToDischarge: title battery_full_charge_last "Full charge lasts about",
  value formatElapsedTime(averageTimeToDischarge). allowDividerAbove=true.
screen_usage: title device_screen_usage "Screen usage since full charge"; value = formatElapsedTime(BatteryUtils.calculateScreenUsageTime(), false) -> "59 min", "1 hr, 3 min", "1 hr, 16 min";
  calculateScreenUsageTime = usageTimeMs of the SCREEN BatterySipper in BatteryStatsHelper.getUsageList() (0 if none) [= screen-on time since the stats reset, ScreenPowerCalculator, MEM].
"Full charge" reset rule [MEM BatteryStatsImpl.setBatteryStateLocked, detail unverified]: stats and start clock reset on unplug when the previous status was FULL, or level >= 90, or (discharge start < 20% and level >= 80).
Both values are set in refreshUi() and in the BatteryInfo loader callback; plain text, no animation.

### 2.5 Footer [SRC FooterPreference.java, preference_footer.xml, ic_info_outline_24.xml, styles_support_preference.xml, PowerUsageSummary.java]
mFooterPreferenceMixin.createFooterPreference().setTitle(battery_footer_summary) = "Battery usage data is approximate and can change based on usage". key footer_preference, order Integer.MAX_VALUE - 1,
  icon @drawable/ic_info_outline_24 (24 dp vector, viewport 24, tint colorControlNormal = 54% grey; path M11,17h2v-6h-2v6zM12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM12,20c-4.41,0
  -8,-3.59 -8,-8s3.59,-8 8,-8 8,3.59 8,8 -3.59,8 -8,8zM11,9h2L13,7h-2v2z), style Preference.FooterPreference.SettingsBase (layout preference_footer, allowDividerAbove=true).
preference_footer.xml: LinearLayout horizontal, minHeight ?listPreferredItemHeight (72 dip in Settings), paddingStart/End 16 dp, selectableItemBackground; icon_frame minWidth 56 dp, gravity start|top, paddingEnd 12 dp,
  paddingTop 16 dp, paddingBottom 4 dp (icon centre x = 28 dp = 29.75 px, tablet 30 px); title = LinkTextView, paddingTop/Bottom 16 dp, maxLines 10, ellipsize marquee, textColor textColorSecondary,
  size = default TextView appearance (TextAppearance.Material 14 sp); text starts at x 72 dp; not clickable.

## 3. BATTERY USAGE page ("Battery usage", overflow item of sec. 2) [SRC PowerUsageAdvanced.java, power_usage_advanced.xml, BatteryHistoryPreference.java, battery_usage_graph.xml, BatteryAppListPreferenceController.java, BatteryEntry.java]
Hosted in SubSettings (Theme.SubSettings; toolbar title advanced_battery_title "Battery usage", contentInsetStart 72 dp). Fragment PowerUsageAdvanced. Screen = battery_graph + PreferenceCategory app_list.
Overflow: ONE item (MENU_TOGGLE_APPS): show_all_apps "Show full device usage" <-> hide_extra_apps "Show app usage" (toggles mShowAllApps, state saved; reloads the list).
battery_graph = BatteryHistoryPreference (layout battery_usage_graph.xml, selectable=false): LinearLayout vertical, paddingTop 16 dp, paddingStart 72 dp (preference_no_icon_padding_start), paddingEnd 16 dp:
  charge TextView (marginBottom 16 dp, fontFamily config_headlineFontFamily = "" default, textAppearanceLarge 22 sp but textSize 36 sp, textColor colorAccent; text = batteryPercentString, e.g. "84%");
  UsageView battery_usage (height 141 dp, marginBottom 16 dp, sideLabels @array/battery_labels = "0%","50%","100%", textColor textColorSecondary, gravity end; its label_group alpha 0.7);
  bottom_summary TextView (marginBottom 16 dp, textAppearanceSmall), visible only if a provider enables enhanced prediction and the device is not plugged (AOSP: hidden).
  UsageView dimens [SRC fwbase/packages/SettingsLib/res/values/dimens.xml]: graph area height 122 dp, margin top/bottom 9 dp, labels width 56 dp, labels padding 16 dp, divider 1 dp, line width 3 dp,
  line corner radius 6 dp, dot size .75 dp, dot interval 7 dp; line colour accent. Data = BatteryStats history (BatteryInfo.parse -> bindHistory): hidden classes, sec. 5.
app_list = PreferenceCategory, title power_usage_list_summary "Battery usage since full charge" (category title style TextAppearance.CategoryTitle: DeviceDefault.Body2, all caps, 11 sp, letter spacing 0.0727).
  Rows (refreshAppListGroup) = PowerGaugePreference, layout of sec. 2.4 but WITH icon (32 dp in the 56 dp frame): icon = app icon badged for the user (IconDrawableFactory), title = app label,
  widget_summary = Utils.formatPercentage(percentOfTotal, true) e.g. "12%", summary = battery_used_for "Used for 1 hr, 3 min" (APP type) or the bare formatElapsedTime (system rows) only when usageTimeMs >= 1 min.
  Order = totalPowerMah descending; percent = power / (total - hidden) * dischargeAmount; rows with rounded percent < 1 dropped; at most 20 (+1) rows; none -> Preference "not_available" (selectable=false)
  title power_usage_not_available "Battery usage data isn't available." Hidden unless "Show full device usage": IDLE, CELL, SCREEN, UNACCOUNTED, OVERCOUNTED, BLUETOOTH, WIFI, power*3600 < 5, service/system types,
  hidden system modules (OVERCOUNTED, UNACCOUNTED and hidden modules are never shown). Row click -> AdvancedPowerUsageDetail (app battery details; not covered here).
  System rows (BatteryEntry): IDLE "Tablet idle" ic_settings_phone_idle | CELL "Mobile network standby" ic_cellular_1_bar | PHONE "Voice calls" ic_settings_voice_calls | WIFI "Wi-Fi" (U+2011)
  ic_settings_wireless | BLUETOOTH "Bluetooth" @*android:drawable/ic_settings_bluetooth | SCREEN "Screen" ic_settings_display | FLASHLIGHT "Flashlight" ic_settings_display | CAMERA "Camera" ic_settings_camera |
  AMBIENT_DISPLAY "Ambient display" ic_settings_aod | UNACCOUNTED "Miscellaneous", OVERCOUNTED "Over-counted" ic_android | USER = user icon + user label.

## 4. DISPLAY page (Settings$DisplaySettingsActivity -> fragment DisplaySettings) [SRC AndroidManifest.xml, DisplaySettings.java, display_settings.xml]
ENTRY: actions android.settings.DISPLAY_SETTINGS / com.android.settings.DISPLAY_SETTINGS; label display_settings "Display"; screen key display_settings_screen; initialExpandedChildrenCount=5 (see D1).
AOSP XML order of all rows: brightness, night_display, auto_brightness_entry, wallpaper, dark_ui_mode, screen_timeout, adaptive_sleep_entry, auto_rotate, color_mode, display_white_balance, font_size,
  display_settings_screen_zoom, show_operator_name, screensaver, lockscreen_from_display_settings, camera_gesture, lift_to_wake, tap_to_wake, theme, vr_display_pref (most are availability-gated).
The five entries keep this relative order: Brightness level, Screen timeout, Auto-rotate screen, Font size, Display size. No row has an icon (icon space reserved: title x 72 dp).

### 4.1 Brightness level
key brightness; class com.android.settingslib.RestrictedPreference (extends TwoTargetPreference: layout preference_two_target.xml, ripple only on the left block, widget frame and its 1 dp divider hidden);
  title brightness "Brightness level"; summary set by BrightnessLevelPreferenceController (AbstractPreferenceController; always available); useAdminDisabledSummary, userRestriction no_config_brightness.
preference_two_target.xml: LinearLayout minHeight 48 dp; left block paddingStart/End 16 dp (icon_frame minWidth 56 dp, paddingTop/Bottom 4 dp, icon max 48 dp) holding a RelativeLayout paddingTop/Bottom 16 dp:
  title (singleLine, marquee, textAppearanceListItem 16 sp) and summary below it (textAppearanceListItemSecondary 14 sp, textColorSecondary, maxLines 10).
Summary = NumberFormat.getPercentInstance().format(p), p = clamp(convertLinearToGamma(Settings.System.SCREEN_BRIGHTNESS [default when unset = min], min, max) / 1023, 0, 1). HLG [SRC
  fwbase/packages/SettingsLib/src/com/android/settingslib/display/BrightnessUtils.java]: n = (value - min) / (max - min) * 12; ret = n <= 1 ? sqrt(n) * 0.5 : 0.17883277 * ln(n - 0.28466892) + 0.55991073;
  gamma = round(ret * 1023). Inverse (slider -> setting): n = gamma / 1023; ret = n <= 0.5 ? (n / 0.5)^2 : exp((n - 0.55991073) / 0.17883277) + 0.28466892; value = round(min + (max - min) * ret / 12).
  min / max = PowerManager.getMinimumScreenBrightnessSetting() / getMaximumScreenBrightnessSetting() (hidden [MEM]) = config_screenBrightnessSettingMinimum 10 / Maximum 255, default 102 [SRC core config.xml].
  Computed for 10..255: raw 10 -> "0%"; 15 -> "25%" (24.7); 20 -> "35%"; 30 -> "49%"; 40 -> "59%"; 102 (default) -> "82%"; 255 -> "100%". The tablet's "25%" therefore means a raw value of about 15.
  VR mode would read SCREEN_BRIGHTNESS_FOR_VR with VR limits (IVrManager, hidden): irrelevant here. Observers while started: Settings.System SCREEN_BRIGHTNESS, SCREEN_BRIGHTNESS_FOR_VR, SCREEN_AUTO_BRIGHTNESS_ADJ.
Click: the preference's intent action com.android.intent.action.SHOW_BRIGHTNESS_DIALOG (no component) -> SystemUI's BrightnessDialog (translucent dialog with the slider; an "Auto" toggle only when
  config_automatic_brightness_available, false in the AOSP core config [SRC]). SystemUI sources and layouts: NOT FOUND. [MEM] slider range 0..1023 (gamma space) and the dialog writes
  Settings.System.SCREEN_BRIGHTNESS (setTemporaryBrightness while dragging); in auto mode (SCREEN_BRIGHTNESS_MODE = 1) it changes SCREEN_AUTO_BRIGHTNESS_ADJ instead; PowerManagerService applies the setting.

### 4.2 Screen timeout
key screen_timeout; class com.android.settings.display.TimeoutListPreference (RestrictedListPreference > CustomListPreference > androidx ListPreference; widget layout restricted_icon = admin padlock, hidden here);
  title screen_timeout "Screen timeout"; summary starts as summary_placeholder; controller TimeoutPreferenceController. Entries [SRC settings10/res/values/arrays.xml screen_timeout_entries / _values]:
  "15 seconds"=15000, "30 seconds"=30000, "1 minute"=60000, "2 minutes"=120000, "5 minutes"=300000, "10 minutes"=600000, "30 minutes"=1800000 (no "Never").
Summary: string screen_timeout_summary "After %1$s of inactivity" with the matching entry text, e.g. "After 1 minute of inactivity"; a value not in the list -> ""; admin-disabled ->
  disabled_by_policy_title "Action not allowed". Value read: Settings.System.getLong(SCREEN_OFF_TIMEOUT, 30000 fallback).
Click: single-choice dialog (CustomListPreferenceDialogFragment > ListPreferenceDialogFragmentCompat; androidx AlertDialog, Theme.AlertDialog = AppCompat.DayNight.Dialog.Alert [MEM] with colorAccent #008577,
  window colorBackground = background_device_default_light #FAFAFA, dialogCornerRadius 2 dp, textAppearanceMedium = TextAppearance.DialogMessage 16 sp): title "Screen timeout"; rows from
  restricted_dialog_singlechoice.xml = CheckableLinearLayout (paddingStart 20 dp, paddingEnd ?dialogPreferredPadding = 24 dp, minHeight 48 dp) with CheckedTextView text1 (textAppearanceMedium,
  textColor textColorAlertDialogListItem, gravity center_vertical, drawableStart listChoiceIndicatorSingle = radio, drawablePadding 20 dp, marquee) and a hidden restricted_lock_icon; the current value is checked.
  isAutoClosePreference() = true: tapping a row stores the value and dismisses (no OK button); negative button "Cancel" (android:string/cancel) [MEM]. A footer view is added only for admin-limited timeouts.
Write: onPreferenceChange -> Settings.System.putInt(SCREEN_OFF_TIMEOUT, int) (public constants; permissions sec. 5). Applied by PowerManagerService observing the setting [MEM].

### 4.3 Auto-rotate screen
key auto_rotate; androidx SwitchPreference; title accelerometer_title "Auto-rotate screen"; no summary; controller AutoRotatePreferenceController (TogglePreferenceController): a tap on the row toggles the switch.
isChecked = !RotationPolicy.isRotationLocked(ctx); setChecked(c) = RotationPolicy.setRotationLock(ctx, !c); available only if RotationPolicy.isRotationLockToggleVisible(ctx); a RotationPolicyListener
  (registered in onResume, unregistered in onPause) re-reads the state on every rotation-setting change. RotationPolicy (com.android.internal.view) is NOT in the checkout [MEM]: locked == Settings.System ACCELEROMETER_ROTATION == 0; setRotationLock clears
  HIDE_ROTATION_LOCK_TOGGLE_FOR_ACCESSIBILITY and runs an AsyncTask calling IWindowManager.freezeRotation(rotation) when locking / thawRotation() when unlocking (hidden); freezeRotation writes
  ACCELEROMETER_ROTATION = 0 and USER_ROTATION. config_supportAutoRotation = true [SRC core config.xml]. The tablet report lists accelerometer_rotation and user_rotation among its Settings.System names.

### 4.4 Font size
key font_size; androidx Preference; title title_font_size "Font size"; fragment ToggleFontSizePreferenceFragment; controller FontSizePreferenceController (AVAILABLE_UNSEARCHABLE).
Summary = entries_font_size[fontSizeValueToIndex(Settings.System.FONT_SCALE (default 1.0f), entryvalues_font_size)] [SRC arrays.xml]: entries "Small", "Default", "Large", "Largest"; values 0.85, 1.0, 1.15, 1.30.
  fontSizeValueToIndex: for i = 1..n-1 return i-1 at the first i with val < prev + (cur - prev) * 0.5 (boundaries 0.925, 1.075, 1.225), else n-1. See D2 for the tablet.
Click opens the fragment in SubSettings (toolbar title "Font size"). Page layout font_size_activity.xml replaces the list container: ScrollView (fillViewport) > LinearLayout vertical { include preview_seek_bar_view_pager (sec. 4.6);
  LinearLayout vertical, paddingTop 16 dp, paddingStart/End ?listPreferredItemPaddingStart/End 16 dp: DotsPageIndicator page_indicator (padding 6 dp, centred; GONE: one preview page);
  TextView current_label (wrap, padding 6 dp, centred, TextAppearance.DeviceDefault.Widget.TextView, elevation 2 dp) = selected entry text; row (center_vertical, paddingTop 8 dp):
  ImageView smaller 48 x 48 dp (@drawable/ic_font_size_16dp = 16 dp "A" glyph, viewport 48, path M33.3,36.8H14.7L10.8,48H2.1L20.2,0h7.5l18.2,48h-8.7L33.3,36.8z M17,30.1h13.9l-7-19.9L17,30.1z; tint
  textColorPrimary src_in; scaleType center; borderless ripple; contentDescription font_size_make_smaller_desc "Make smaller") | LabeledSeekBar seek_bar (0 dp weight 1 x 48 dp, style
  android:Widget.Material.SeekBar.Discrete, max = entries - 1 = 3; Material seek bar drawables NOT FOUND) | ImageView larger 48 x 48 dp (ic_font_size_24dp = same glyph at 24 dp, "Make larger");
  then TextView "Make the text on screen smaller or larger." (font_size_summary, TextAppearance.DeviceDefault.Subhead 16 sp, marginBottom 16 dp) }.
Behaviour [SRC PreviewSeekBarPreferenceFragment.java]: smaller / larger = setProgress(+-1, animate), each disabled at its end. Every progress change updates the label and the preview layer (setPreviewLayer is
  called with animate=false everywhere in this checkout, so the 400 ms cross-fade code of PreviewPagerAdapter is never reached). commit() runs on every progress change that is NOT a touch drag (buttons, keys,
  accessibility) and once at onStopTrackingTouch of a drag (after a running preview animation ends). The initial setProgress in onStart happens before the listener is attached: opening the page writes nothing.
Apply [SRC ToggleFontSizePreferenceFragment.commit]: Settings.System.putFloat(resolver, FONT_SCALE, values[index]) and nothing else (no IActivityManager call in this version); the system picks the new
  configuration up by observing the setting [MEM: ActivityTaskManagerService font-scale observer].

### 4.5 Display size
key display_settings_screen_zoom; class com.android.settings.display.ScreenZoomPreference (androidx Preference); title screen_zoom_title "Display size"; fragment ScreenZoomSettings; searchable=false.
Summary: when the XML summary is empty -> DisplayDensityUtils.getEntries()[getCurrentIndex()]; the row is hidden and disabled when getCurrentIndex() < 0. Entry strings [SRC SettingsLib strings.xml]:
  screen_zoom_summary_small "Small", _default "Default", _large "Large", _very_large "Larger", _extremely_large "Largest", _custom "Custom (%d)" (current density matches no stop).
Table [SRC fwbase/packages/SettingsLib/src/com/android/settingslib/display/DisplayDensityUtils.java]: default = IWindowManager.getInitialDisplayDensity(0) (hidden); current = getRealMetrics().densityDpi;
  maxDensity = 160 * min(widthPx, heightPx) / 320; maxScale = min(1.5, maxDensity / default); minScale 0.85; interval 0.09; numLarger = (int) clamp((maxScale - 1) / 0.09, 0, 3);
  numSmaller = (int) clamp((1 - 0.85) / 0.09, 0, 1). Smaller: ((int)(default * (1 - 0.15))) & ~1. Larger i = 0..numLarger-1: ((int)(default * (1 + (i+1) * (maxScale - 1) / numLarger))) & ~1 (all even).
  Tablet (default 170, short side 600 px -> maxDensity 300, maxScale 1.5, numLarger 3, numSmaller 1): 144 "Small", 170 "Default", 198 "Large", 226 "Larger", 254 "Largest" (5 stops, seek bar max 4).
Page layout screen_zoom_activity.xml: same skeleton as font_size_activity.xml except: no paddingTop on the control row; buttons ic_remove_24dp (24 dp "-", viewport 48, path M38 26H10v-4h28v4z) and
  ic_add_24dp (24 dp "+", viewport 24, path M20,13h-7v7h-2v-7H4v-2h7V4h2v7h7V13z) with descriptions screen_zoom_make_smaller_desc / _larger_desc ("Make smaller" / "Make larger"); page indicator visible
  (3 preview pages, config_enable_extra_screen_zoom_preview = true); footer text screen_zoom_summary "Make the items on your screen smaller or larger. Some apps on your screen may change position."
  Page title "Display size". With no table (index < 0): one entry "Default", seek bar disabled. Behaviour and commit timing as in sec. 4.4.
Apply: commit() = density == default ? DisplayDensityUtils.clearForcedDisplayDensity(0) : setForcedDisplayDensity(0, density); both run in an AsyncTask and call the hidden IWindowManager
  clearForcedDisplayDensityForUser / setForcedDisplayDensityForUser(displayId, density, userId) (WMS demands WRITE_SECURE_SETTINGS [MEM]). Each preview layer is inflated in a context whose
  Configuration has densityDpi = values[index] (createConfigurationContext), so previews render at that real density.

### 4.6 Shared preview pager and preview content [SRC preview_seek_bar_view_pager.xml, preview_seek_bar_outline.xml, PreviewPagerAdapter.java, DotsPageIndicator.java, styles.xml, strings.xml]
preview_seek_bar_view_pager.xml: LinearLayout vertical, match x 0 dp (weight 1: takes the rest of the page height), padding preview_pager_padding 8 dp, background @drawable/preview_seek_bar_outline
  (layer-list: rectangle with an 8 dp stroke in colorPrimary #F5F5F5 around the area + a solid colorPrimary rectangle at the bottom, height ?actionBarSize 56 dp); child ViewPager preview_pager (match x 100 dp,
  weight 1, background colorBackgroundFloating #FFFFFF, contentDescription "Preview"); child TextView "Preview" (screen_zoom_preview_title; match x 56 dp, paddingStart 32 dp, gravity start|center,
  TextAppearance.DeviceDefault.Widget.ActionBar.Title 20 dp sans-serif-medium, importantForAccessibility no).
Pages are FrameLayouts (clipToPadding, clipChildren) with one ViewStub per entry, inflated lazily in a context with that entry's fontScale or densityDpi; only the current layer is visible (alpha 1), the others
  INVISIBLE. DotsPageIndicator: defaults dot 8 dp, gap 12 dp, animation 400 ms; style PreviewPagerPageIndicator sets dotGap 8 dp, pageIndicatorColor ?colorControlNormal (54% grey),
  currentPageIndicatorColor ?colorControlActivated (accent). preview_page_indicator_content_description "Preview, page %1$d of %2$d".
FONT preview (font_size_preview.xml): ScrollView > TouchBlockingFrameLayout > LinearLayout vertical, padding top/start/end 16 dp, four TextViews (match x wrap): font_size_preview_text_headline "Sample text"
  (DeviceDefault.Headline 24 sp) | _title "The Wonderful Wizard of Oz" (Title 20 sp) | _subtitle "Chapter 11: The Wonderful Emerald City of Oz" (Subhead 16 sp) | _body (Body1 14 sp) =
  "Even with eyes protected by the green spectacles Dorothy and her friends were at first dazzled by the brilliancy of the wonderful City. The streets were lined with beautiful houses all built of green marble
  and studded everywhere with sparkling emeralds. They walked over a pavement of the same green marble, and where the blocks were joined together were rows of emeralds, set closely, and glittering in the
  brightness of the sun. The window panes were of green glass; even the sky above the City had a green tint, and the rays of the sun were green. [paragraph break] There were many people, men, women and
  children, walking about, and these were all dressed in green clothes and had greenish skins. They looked at Dorothy and her strangely assorted company with wondering eyes, and the children all ran away and
  hid behind their mothers when they saw the Lion; but no one spoke to them. Many shops stood in the street, and Dorothy saw that everything in them was green. Green candy and green pop-corn were offered for
  sale, as well as green shoes, green hats and green clothes of all sorts. At one place a man was selling green lemonade, and when the children bought it Dorothy could see that they paid for it with green
  pennies. [paragraph break] There seemed to be no horses nor animals of any kind; the men carried things around in little green carts, which they pushed before them. Everyone seeemed happy and contented and
  prosperous." (sic "seeemed"; source lines are joined by single spaces, "\n\n" is a blank line.)
ZOOM preview pages (all importantForAccessibility noHideDescendants):
  1 screen_zoom_preview_1: ScrollView > LinearLayout vertical, background colorBackgroundFloating, padding top/start/end conversation_message_list_padding 10 dp; four ConversationMessageView rows:
    out "Hi Pete!" Tue 6:00PM icon "A" | in "Hey, want to grab coffee and catch up today?" Tue 6:01PM "P" | out "Sounds great. I know of a good place not too far from here." Tue 6:02PM "A" | in "Perfect!" Tue 6:03PM "P".
    Colours: outgoing icon background #4285F4, incoming #689F38, icon text white (message_icon_*); icon letter 32 sp bold sans-serif, message text 16 sp, status 12 sp, bubble width snap 20 dp, bubble drawables
    msg_bubble_incoming / msg_bubble_outgoing (geometry: settings10/src/com/android/settings/display/ConversationMessageView.java, MessageBubbleBackground.java; not re-read).
  2 screen_zoom_preview_2: AppGridView (GridView numColumns 3, padding 32 dp, gravity center): the FIRST 6 launcher activities returned by PackageManager.queryIntentActivities(MAIN + LAUNCHER) that have a label,
    then sorted by label (compareToIgnoreCase); item screen_zoom_preview_app_icon: vertical, centred top, paddingTop 8 dp, icon 64 x 64 dp (marginBottom 4 dp, fitCenter), label DeviceDefault.Caption 12 sp, single line, ellipsize end.
  3 screen_zoom_preview_settings: five rows (paddingTop 16 dp, paddingStart 8 dp): 48 x 48 dp icon (scaleType center, tint colorAccent) + text column (paddingStart 16 dp, title DeviceDefault.Subhead 16 sp
    textColorPrimary, summary DeviceDefault.Body1 14 sp textColorSecondary): ic_settings_wireless "Wi-Fi" / "Unavailable" | ic_settings_data_usage "Data usage" / "No data used" | ic_settings_display "Display" /
    "Sleep after 10 minutes of inactivity" | ic_settings_sound "Sound" / "Ring volume at 80%" | ic_settings_applications "App info" / "24 apps installed" (Wi-Fi uses U+2011).

## 5. Gotchas for a non-system app (Java, API 29, no AndroidX; the tablet report shows WRITE_SECURE_SETTINGS, DUMP and CHANGE_CONFIGURATION granted to the launcher)
P = public SDK, H = hidden / non-SDK, S = signature-level.
 Battery level / status / plugged / power save: P. registerReceiver(null, IntentFilter(ACTION_BATTERY_CHANGED)) gives sticky EXTRA_LEVEL, EXTRA_SCALE, EXTRA_STATUS, EXTRA_PLUGGED; PowerManager.isPowerSaveMode().
 Time-remaining estimate, "Last full charge" (stats start clock), "Screen usage since full charge", the graph and the app list: all from BatteryStats / BatteryStatsHelper / BatterySipper / PowerProfile = H classes
   (com.android.internal.os); BATTERY_STATS is signature|privileged|development [MEM], the same class as WRITE_SECURE_SETTINGS and DUMP; dumpsys batterystats (DUMP) prints the same data as text [MEM].
   Public data that exist: BatteryManager properties, UsageStatsManager.queryEvents (PACKAGE_USAGE_STATS; SCREEN_INTERACTIVE 15 / SCREEN_NON_INTERACTIVE 16 events public since API 28 [MEM]).
 Settings.System SCREEN_BRIGHTNESS, SCREEN_OFF_TIMEOUT, FONT_SCALE, ACCELEROMETER_ROTATION, USER_ROTATION, SCREEN_BRIGHTNESS_MODE, SCREEN_AUTO_BRIGHTNESS_ADJ: P constants. Writing needs WRITE_SETTINGS
   (appop, Settings.System.canWrite(ctx)); SettingsProvider skips that appop check when the caller holds WRITE_SECURE_SETTINGS [MEM]. The platform does the HLG conversion nowhere: the raw value is 0..255 (limits H).
 Settings.System.SHOW_BATTERY_PERCENT: H constant (string "status_bar_show_battery_percent" [MEM]).
 Brightness limits: PowerManager.getMinimum/MaximumScreenBrightnessSetting() are H; the numbers 10 / 255 / 102 are core config.xml values (Resources.getIdentifier on package "android" reads non-public config_* names
   in the usual status_bar_height way [MEM, not tried here]; the tablet's overlay NOT FOUND).
 Font size: the whole AOSP apply path is Settings.System.putFloat(FONT_SCALE). Reading the live value: getResources().getConfiguration().fontScale (tablet 1.1999817).
 Display size: IWindowManager.setForcedDisplayDensityForUser / clearForcedDisplayDensityForUser / getInitialDisplayDensity through WindowManagerGlobal.getWindowManagerService() = H (hidden-API list status NOT FOUND);
   shell equivalent: wm density <dpi> / wm density reset. Settings.Secure display_density_forced is a name on the tablet; a plain write is not known to apply live (no observer known [MEM]). Public read: DisplayMetrics.densityDpi
   (current); the initial density is ro.sf.lcd_density (170 on the tablet report); the table also needs real width and height (getRealMetrics).
 Auto-rotate: P read and write of ACCELEROMETER_ROTATION and USER_ROTATION; IWindowManager.freezeRotation / thawRotation are H and need SET_ORIENTATION (S) [MEM]. A bare ACCELEROMETER_ROTATION = 0 write locks at the
   stored USER_ROTATION, which may differ from the current rotation [MEM].
 SHOW_BRIGHTNESS_DIALOG: startActivity of the action to SystemUI's exported BrightnessDialog [MEM]; its look is the system's.
 Theme: android:Theme.DeviceDefault.Settings is P since API 21 (toolbar, light status bar, nav bar, dialogs); the Settings-only attributes (listPreferredItemHeight 72dip, homeAsUpIndicator, preferenceTheme) are not part of it.
 Not in any SDK, so nothing to call: BatteryMeterView / ThemedBatteryDrawable / EntityHeader / LayoutPreference / FooterPreference / PowerGaugePreference / ActionBarShadowController / UsageView / LabeledSeekBar /
   DotsPageIndicator / AppGridView; androidx.preference classes (rows, dividers, switch, dialog fragments) are absent from the checkout and from the app.
 Framework resources by name: battery path strings (config_batterymeter*), config_criticalBatteryWarningLevel and config_dialogCornerRadius are non-public; reading them by getIdentifier(name, type, "android") is unverified [MEM].
 Another app's resources: PackageManager.getResourcesForApplication("com.android.settings") could expose its strings and drawables; whether that package name and its resources are readable on the tablet: NOT FOUND.
 product variants: strings marked product="tablet" are chosen at run time from ro.build.characteristics [MEM]; the screenshots confirm the tablet variant ("Tablet has typical background battery usage").
 ICU: android.icu.* is public from API 24 (MeasureFormat, RelativeDateTimeFormatter, NumberFormat, DateFormat.getInstanceForSkeleton) and is what produces "1 hr, 3 min", "109 minutes ago", "25%" and "9:15 AM".
