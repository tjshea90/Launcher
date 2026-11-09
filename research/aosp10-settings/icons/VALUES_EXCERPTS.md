# Verbatim value excerpts behind the icon files (AOSP 10, android-10.0.0_r47)

Every block below was cut from the named source file with sed or grep -n, nothing retyped. Paths are relative to the clone roots
(`settings10/` = root of the Settings clone, `fwbase/` = root of the frameworks subset; see INDEX.md). Line numbers are those of the clone.

## A. Settings home tile: size, inset, colours, shape

### Tile sizes (36 dp square, 24 dp glyph, 6 dp inset)
Source: `settings10/res/values/dimens.xml`, lines 99-106
```xml
    <!-- Dashboard image tile size -->
    <dimen name="dashboard_tile_image_size">36dp</dimen>

    <!-- Dashboard foreground image size -->
    <dimen name="dashboard_tile_foreground_image_size">24dp</dimen>

    <!-- Dashboard foreground image inset (from background edge to foreground edge) -->
    <dimen name="dashboard_tile_foreground_image_inset">6dp</dimen>
```

### Tile background colours (all categories)
Source: `settings10/res/values/colors.xml`, lines 84-100
```xml
    <!-- Dashboard/homepage icon background colors -->
    <color name="homepage_network_background">#2196F3</color>
    <color name="homepage_connected_device_background">#72B70F</color>
    <color name="homepage_app_and_notification_background">#FF7E0F</color>
    <color name="homepage_battery_background">#258982</color>
    <color name="homepage_display_background">#FFA727</color>
    <color name="homepage_sound_background">#01B1AF</color>
    <color name="homepage_storage_background">#C14CE6</color>
    <color name="homepage_security_background">#0F9D58</color>
    <color name="homepage_accounts_background">#F15B8D</color>
    <color name="homepage_accessibility_background">#5011C1</color>
    <color name="homepage_system_background">#9E9E9E</color>
    <color name="homepage_support_background">#26459C</color>
    <color name="homepage_generic_icon_background">#1A73E8</color>
    <color name="homepage_location_background">#2EC7DC</color>
    <color name="homepage_about_background">#6F86DA</color>
    <color name="homepage_privacy_background">#5E97F6</color>
```

### Dark-theme variant (of the seven tiles only Accessibility differs, Support differs too; not used by the light tablet)
Source: `settings10/res/values-night/colors.xml`, lines 20-21
```xml
    <color name="homepage_accessibility_background">#783BE5</color>
    <color name="homepage_support_background">#3F5FBD</color>
```

### Shape mask of the tile (AdaptiveIconShapeDrawable reads this string; viewport 100 x 100)
Source: `fwbase/core/res/res/values/config.xml`, lines 3568-3569
```xml
    <!-- Specifies the path that is used by AdaptiveIconDrawable class to crop launcher icons. -->
    <string name="config_icon_mask" translatable="false">"M50,0L92,0C96.42,0 100,4.58 100 8L100,92C100, 96.42 96.42 100 92 100L8 100C4.58, 100 0 96.42 0 92L0 8 C 0 4.42 4.42 0 8 0L50 0Z"</string>
```

### Flag that makes TopLevelSettings round injected tiles (not the seven static tiles)
Source: `settings10/res/values/config.xml`, lines 201-202
```xml
    <!-- Whether or not TopLevelSettings should force rounded icon for injected tiles -->
    <bool name="config_force_rounded_icon_TopLevelSettings">true</bool>
```

## B. Colour chain used by the tints

### framework colors_material.xml: accent, text, alpha, error, greys
Source: `fwbase/core/res/res/values/colors_material.xml` (line number, then the verbatim line)
```xml
20:    <color name="foreground_material_light">@color/black</color>
23:    <color name="background_material_light">@color/material_grey_50</color>
30:    <color name="primary_material_settings_light">@color/material_grey_100</color>
35:    <color name="primary_dark_material_settings_light">@color/material_grey_300</color>
38:    <color name="secondary_material_settings_light">@color/material_grey_200</color>
42:    <color name="accent_material_light">@color/material_deep_teal_500</color>
48:    <color name="error_color_material_light">#ff5722</color><!-- deep orange 500 -->
59:    <color name="primary_text_default_material_light">#de000000</color>
61:    <color name="secondary_text_default_material_light">#8a000000</color>
74:    <item name="disabled_alpha_material_light" format="float" type="dimen">0.26</item>
77:    <item name="primary_content_alpha_material_light" format="float" type="dimen">0.87</item>
79:    <item name="secondary_content_alpha_material_light" format="float" type="dimen">0.54</item>
91:    <color name="material_grey_600">#ff757575</color>
92:    <color name="material_grey_300">#ffe0e0e0</color>
93:    <color name="material_grey_200">#ffeeeeee</color>
94:    <color name="material_grey_100">#fff5f5f5</color>
95:    <color name="material_grey_50">#fffafafa</color>
98:    <color name="material_deep_teal_200">#ff80cbc4</color>
100:    <color name="material_deep_teal_500">#ff008577</color>
```

### framework colors_device_defaults.xml
Source: `fwbase/core/res/res/values/colors_device_defaults.xml` (line number, then the verbatim line)
```xml
23:    <color name="primary_device_default_settings_light">@color/primary_material_settings_light</color>
27:    <color name="primary_dark_device_default_settings_light">@color/primary_dark_material_settings_light</color>
29:    <color name="navigation_bar_divider_device_default_settings">#1f000000</color>
32:    <color name="secondary_device_default_settings_light">@color/secondary_material_settings_light</color>
36:    <color name="accent_device_default_light">@color/accent_material_light</color>
47:    <color name="error_color_device_default_light">@color/error_color_material_light</color>
49:    <color name="list_divider_color_light">#ffdadce0</color>
```

### framework colors.xml (black, white)
Source: `fwbase/core/res/res/values/colors.xml` (line number, then the verbatim line)
```xml
37:    <color name="white">#ffffffff</color>
39:    <color name="black">#ff000000</color>
```

### Launcher icon background colour reference
Source: `settings10/res/values/colors.xml`, lines 136-136
```xml
    <color name="icon_launcher_setting_color">@*android:color/accent_device_default_light</color>
```

### Theme attributes of Theme.Material.Light: text colours
Source: `fwbase/core/res/res/values/themes_material.xml`, lines 436-439
```xml
        <item name="textColorPrimary">@color/text_color_primary</item>
        <item name="textColorPrimaryInverse">@color/primary_text_material_dark</item>
        <item name="textColorPrimaryActivated">@color/primary_text_inverse_when_activated_material</item>
        <item name="textColorSecondary">@color/text_color_secondary</item>
```

### Theme attributes of Theme.Material.Light: control colours
Source: `fwbase/core/res/res/values/themes_material.xml`, lines 781-782
```xml
        <item name="colorControlNormal">?attr/textColorSecondary</item>
        <item name="colorControlActivated">?attr/colorAccent</item>
```

### Theme.DeviceDefault.Settings (parent of the Settings theme)
Source: `fwbase/core/res/res/values/themes_device_defaults.xml`, lines 1452-1479
```xml
    <style name="Theme.DeviceDefault.Settings" parent="Theme.DeviceDefault.Light">
        <!-- From Theme.Material.Light.LightStatusBar -->
        <item name="windowLightStatusBar">true</item>

        <!-- From Theme.Material.Settings -->
        <item name="homeAsUpIndicator">@drawable/ic_ab_back_material_settings</item>
        <item name="presentationTheme">@style/Theme.Material.Settings.Dialog.Presentation</item>
        <item name="searchDialogTheme">@style/Theme.Material.Settings.SearchBar</item>
        <item name="panelMenuListTheme">@style/Theme.Material.Settings.CompactMenu</item>

        <!-- action bar -->
        <item name="actionBarStyle">@style/Widget.DeviceDefault.Light.ActionBar.Solid</item>
        <item name="actionBarTheme">@style/ThemeOverlay.DeviceDefault.ActionBar</item>
        <item name="popupTheme">@style/ThemeOverlay.DeviceDefault.Popup.Light</item>

        <!-- Color palette -->
        <item name="colorBackground">@color/background_device_default_light</item>
        <item name="colorPrimary">@color/primary_device_default_settings_light</item>
        <item name="colorPrimaryDark">@color/primary_dark_device_default_settings_light</item>
        <item name="colorSecondary">@color/secondary_device_default_settings_light</item>
        <item name="colorAccent">@color/accent_device_default_light</item>
        <item name="colorError">@color/error_color_device_default_light</item>
        <item name="colorEdgeEffect">@color/edge_effect_device_default_light</item>

        <!-- Add white nav bar with divider that matches material -->
        <item name="navigationBarDividerColor">@color/navigation_bar_divider_device_default_settings</item>
        <item name="navigationBarColor">@android:color/white</item>
        <item name="windowLightNavigationBar">true</item>
```

### Settings themes (Theme.SettingsBase, Theme.Settings: homeAsUpIndicator, listPreferredItemHeight)
Source: `settings10/res/values/themes.xml`, lines 26-40
```xml
    <style name="Theme.SettingsBase" parent="@android:style/Theme.DeviceDefault.Settings" />

    <style name="Theme.Settings" parent="Theme.SettingsBase">
        <item name="preferenceTheme">@style/PreferenceTheme</item>
        <item name="android:clipToPadding">false</item>
        <item name="android:clipChildren">false</item>
        <item name="android:listPreferredItemHeight">72dip</item>
        <item name="android:homeAsUpIndicator">@drawable/ic_arrow_back</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>

        <item name="fingerprint_layout_theme">@style/FingerprintLayoutTheme</item>
        <item name="face_layout_theme">@style/FaceLayoutTheme</item>
        <item name="ic_menu_moreoverflow">@*android:drawable/ic_menu_moreoverflow_holo_dark</item>

        <!-- For wifi icon -->
```

## C. Big battery icon (Battery page header)

### Path strings, dual-tone flag (12 x 20 grid)
Source: `fwbase/core/res/res/values/config.xml`, lines 4051-4070
```xml
    <!-- All of the paths defined for the batterymeter are defined on a 12x20 canvas, and must
     be parsable by android.utill.PathParser -->
    <string name="config_batterymeterPerimeterPath" translatable="false">
		M3.5,2 v0 H1.33 C0.6,2 0,2.6 0,3.33 V13v5.67 C0,19.4 0.6,20 1.33,20 h9.33 C11.4,20 12,19.4 12,18.67 V13V3.33 C12,2.6 11.4,2 10.67,2 H8.5 V0 H3.5 z M2,18v-7V4h8v9v5H2L2,18z
    </string>
    <string name="config_batterymeterErrorPerimeterPath" translatable="false">@string/config_batterymeterPerimeterPath</string>
    <string name="config_batterymeterFillMask" translatable="false">
        M2,18 v-14 h8 v14 z
    </string>
    <string name="config_batterymeterBoltPath" translatable="false">
        M5,17.5 V12 H3 L7,4.5 V10 h2 L5,17.5 z
    </string>
    <string name="config_batterymeterPowersavePath" translatable="false">
        M9,10l-2,0l0,-2l-2,0l0,2l-2,0l0,2l2,0l0,2l2,0l0,-2l2,0z
    </string>

    <!-- A dual tone battery meter draws the perimeter path twice - once to define the shape
     and a second time clipped to the fill level to indicate charge -->
    <bool name="config_batterymeterDualTone">false</bool>

```

### Critical battery level
Source: `fwbase/core/res/res/values/config.xml`, lines 1288-1290
```xml
    <!-- Display low battery warning when battery level dips to this value.
         Also, the battery stats are flushed to disk when we hit this level.  -->
    <integer name="config_criticalBatteryWarningLevel">5</integer>
```

### Level colour thresholds read by ThemedBatteryDrawable
Source: `fwbase/packages/SettingsLib/res/values/arrays.xml`, lines 573-581
```xml
    <!-- BatteryMeterView parameters -->
    <array name="batterymeter_color_levels">
        <item>15</item>
        <item>100</item>
    </array>
    <array name="batterymeter_color_values">
        <item>?android:attr/colorError</item>
        <item>@android:color/white</item>
    </array>
```

### Drawable size of the header meter (66 x 100 dp)
Source: `settings10/res/values/dimens.xml`, lines 321-323
```xml
    <!-- Battery meter view size -->
    <dimen name="battery_meter_width">66dp</dimen>
    <dimen name="battery_meter_height">100dp</dimen>
```

## D. Storage donut and rows

### Donut dimensions (default; the tablet uses the sw360dp overrides below for the two text sizes)
Source: `settings10/res/values/dimens.xml`, lines 308-319
```xml
    <!-- Padding between the donut and the storage summary. -->
    <dimen name="storage_summary_padding_end">16dp</dimen>
    <!-- Text size of the big number in the donut. -->
    <dimen name="storage_donut_view_percent_text_size">45sp</dimen>
    <!-- Text size of the percentage sign in the donut. -->
    <dimen name="storage_donut_view_percent_sign_size">20sp</dimen>
    <!-- Text size of the label text in the donut. -->
    <dimen name="storage_donut_view_label_text_size">21sp</dimen>
    <!-- Text size of the label text in the donut if the label text is long. -->
    <dimen name="storage_donut_view_shrunken_label_text_size">10sp</dimen>
    <!-- The width of the storage summary donut -->
    <dimen name="storage_donut_thickness">4dp</dimen>
```

### sw360dp overrides (the tablet is 564 dp wide, so sw360dp applies, sw600dp does not)
Source: `settings10/res/values-sw360dp/dimens.xml`, lines 28-31
```xml
    <!-- Text size of the big number in the donut. -->
    <dimen name="storage_donut_view_percent_text_size">30sp</dimen>
    <!-- Text size of the label text in the donut. -->
    <dimen name="storage_donut_view_label_text_size">14sp</dimen>
```

### strings used inside the donut and header
Source: `settings10/res/values/strings.xml` (line number, then the verbatim line)
```xml
3292:    <string name="storage_menu_free">Free up space</string>
5728:    <string name="battery_footer_summary">Battery usage data is approximate and can change based on usage</string>
10499:    <string name="storage_percent_full">used</string>
```

## E. Toolbar

### Settings toolbar insets and size
Source: `settings10/res/values/dimens.xml`, lines 93-97
```xml
    <dimen name="actionbar_size">56dip</dimen>

    <!-- ActionBar contentInsetStart -->
    <dimen name="actionbar_contentInsetStart">16dp</dimen>
    <dimen name="actionbar_subsettings_contentInsetStart">72dp</dimen>
```

### Settings ActionBar styles (16 dp inset on the home page, 72 dp on sub pages)
Source: `settings10/res/values/styles.xml`, lines 19-29
```xml
    <style name="Widget.ActionBar.Base"
           parent="@android:style/Widget.DeviceDefault.Light.ActionBar.Solid"/>

    <style name="Widget.ActionBar"
           parent="Widget.ActionBar.Base">
        <item name="android:contentInsetStart">@dimen/actionbar_contentInsetStart</item>
    </style>

    <style name="Widget.ActionBar.SubSettings" parent="Widget.ActionBar">
        <item name="android:contentInsetStart">@dimen/actionbar_subsettings_contentInsetStart</item>
    </style>
```

### framework toolbar dimensions
Source: `fwbase/core/res/res/values/dimens_material.xml` (line number, then the verbatim line)
```xml
39:    <dimen name="action_bar_default_height_material">56dp</dimen>
45:    <dimen name="action_bar_content_inset_material">16dp</dimen>
47:    <dimen name="action_bar_content_inset_with_nav">72dp</dimen>
62:    <dimen name="action_bar_overflow_padding_start_material">6dp</dimen>
64:    <dimen name="action_bar_overflow_padding_end_material">10dp</dimen>
65:    <dimen name="action_bar_elevation_material">4dp</dimen>
67:    <dimen name="action_button_min_width_overflow_material">36dp</dimen>
69:    <dimen name="action_button_min_height_material">48dp</dimen>
```

### Overflow button style (the glyph is the framework drawable ic_menu_moreoverflow_material, NOT in the subset)
Source: `fwbase/core/res/res/values/styles_material.xml`, lines 967-980
```xml
    <style name="Widget.Material.ActionButton.Overflow">
        <item name="src">@drawable/ic_menu_moreoverflow_material</item>
        <item name="background">?attr/actionBarItemBackground</item>
        <item name="contentDescription">@string/action_menu_overflow_description</item>
        <item name="tooltipText">@string/action_menu_overflow_description</item>
        <item name="minWidth">@dimen/action_button_min_width_overflow_material</item>
        <item name="minHeight">@dimen/action_button_min_height_material</item>
        <item name="paddingStart">@dimen/action_bar_overflow_padding_start_material</item>
        <item name="paddingEnd">@dimen/action_bar_overflow_padding_end_material</item>
    </style>

    <style name="Widget.Material.ActionBar.TabView" parent="Widget.Material">
        <item name="gravity">center_horizontal</item>
        <item name="background">@drawable/tab_indicator_material</item>
```

### ThemeOverlay.Material.ActionBar (toolbar controls use textColorPrimary)
Source: `fwbase/core/res/res/values/themes_material.xml`, lines 887-890
```xml
    <style name="ThemeOverlay.Material.ActionBar">
        <item name="colorControlNormal">?attr/textColorPrimary</item>
        <item name="searchViewStyle">@style/Widget.Material.SearchView.ActionBar</item>
    </style>
```
