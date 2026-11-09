# AOSP 10 (android-10.0.0_r47) Settings: SOUND and STORAGE pages (BN1b part B, source notes)

Paths: settings10/ = packages/apps/Settings (res/..., src/com/android/settings/...). fwbase/ = frameworks/base, PARTIAL (packages/SettingsLib,
core/res/res/values, core/res/res/layout, AndroidManifest only: NO core/res/res/drawable, NO core/res/res/color, NO core/java).
Tags: [src] read in the checkout. [mem] from memory, source NOT in the checkout, verify before relying on it. [calc] my arithmetic from [src]
numbers. [tablet] Tj's screenshots / measurements (research/SETTINGS_SPOOF_2026-11-09.md). A bare statement without tag = [src].

## 0. Arithmetic and theme numbers shared by both pages
- 170 dpi: 1 dp = 1.0625 px. Font scale 1.2: 1 sp = 1.275 px. Layout dp (size, margin, padding) = round-half-up(dp x 1.0625): 4dp=4, 8dp=9 (8.5),
  12dp=13 (12.75), 14dp=15, 16dp=17, 24dp=26 (25.5), 32dp=34, 44dp=47, 48dp=51, 56dp=60 (59.5), 72dp=77 (76.5), 112dp=119.
- XML `textSize` is read by TextView as a pixel SIZE (rounded): 11sp=14 px, 14sp=18 px, 16sp=20 px, 36sp=46 px. A custom Paint using getDimension keeps the
  float (DonutView: 30sp = 38.25 px). [calc]
- Text line height [calc]: TextView default includeFontPadding=true, Roboto top -1.0566 em / bottom +0.2715 em, FontMetricsInt rounded outward:
  20 px text = 28 px line, 18 px text = 25 px line, 46 px text = 62 px line. This reproduces Tj's measured rows: one-line preference row 2x17+28 = 62 px,
  two-line row 34+28+25 = 87 px (both [tablet] exact). So: the standard row = 16dp padding top/bottom, title 16sp, summary 14sp directly below.
- Theme: activity theme Theme.Settings (settings10/res/values/themes.xml) -> @android:style/Theme.DeviceDefault.Settings (fwbase/core/res/res/values/
  themes_device_defaults.xml l.1452, parent Theme.DeviceDefault.Light). Settings adds listPreferredItemHeight 72dip and PreferenceTheme
  (settings10/res/values/styles_preference.xml: parent PreferenceThemeOverlay.SettingsBase -> androidx PreferenceThemeOverlay.v14.Material; androidx
  preference layouts/styles are NOT in the checkout; the framework twins fwbase/core/res/res/layout/preference_material.xml and
  preference_category_material.xml are quoted below and reproduce the measured heights).
- Resolved colours: colorAccent = accent_device_default_light -> accent_material_light -> material_deep_teal_500 = #FF008577. colorPrimary (toolbar) =
  material_grey_100 #F5F5F5. colorPrimaryDark (status bar) = material_grey_300 #E0E0E0. colorBackground = grey_50 #FAFAFA. textColorPrimary #DE000000
  (primary_text_default_material_light), textColorSecondary #8A000000, colorControlNormal = textColorSecondary, colorControlActivated = colorAccent,
  list divider = list_divider_color_light #FFDADCE0 (1 dp line, fwbase/packages/SettingsLib/res/layout/preference_category_divider.xml uses
  ?android:attr/dividerHorizontal), navigation bar white with divider #1F000000. All match [tablet] (#E0E0E0, #F5F5F5, #FAFAFA, #DADCE0, teal #008577).
- listPreferredItemPaddingStart/End = list_item_padding_*_material = action_bar_content_inset_material = 16dp (17 px). listPreferredItemHeightSmall 48dip,
  listPreferredItemHeight 64dip (Settings overrides to 72dip).
- Text appearances (fwbase styles_material.xml / styles_device_defaults.xml / dimens_material.xml): Subhead 16sp textColorPrimary; Body1 14sp textColorPrimary;
  Body2 14sp medium; Display1 34sp textColorSecondary; ListItem = Subhead; ListItemSecondary = Body1. DeviceDefault variants only set fontFamily
  config_bodyFontFamily = sans-serif (Body2: sans-serif-medium). config_headlineFontFamily is the EMPTY string (default typeface = Roboto Regular).
- Category heading (settings10/res/values/styles.xml l.239): TextAppearance.CategoryTitle = DeviceDefault.Body2 + textAllCaps true + textSize 11sp (14 px)
  + letterSpacing 0.072727273; colour accent teal [tablet]. Set through the androidx attr preferenceCategoryTitleTextAppearance (PreferenceTheme).
  Framework twin (preference_category_material.xml): padding start/end 16dp, paddingTop 16dip, marginBottom 16dip, colorAccent.

## 1. SOUND page

### 1.1 Page frame and row list
- Fragment settings10/src/com/android/settings/notification/SoundSettings.java (DashboardFragment, @SearchIndexable). XML settings10/res/xml/sound_settings.xml.
  Title sound_settings = "Sound". Screen attribute settings:initialExpandedChildrenCount="9" (androidx collapses rows beyond 9 behind an "Advanced" row;
  androidx source not in checkout; Tj's screenshots show NO Advanced row and every row up to "Sound enhancement": unresolved whether the OEM changed it or
  Tj had expanded it).
- Rows in XML order (order, key, class, controller: availability). Availability rules are from the controllers; [tablet] column from Tj's list.
  -185 remote_volume   RemoteVolumeSeekBarPreference  RemoteVolumePreferenceController (remote session only)            hidden
  -180 media_volume    VolumeSeekBarPreference        MediaVolumePreferenceController: config_show_media_volume (true)  SHOWN
  -175 media_output    Preference                     MediaOutputPreferenceController [mem: output switcher]            hidden
  -170 call_volume     VolumeSeekBarPreference        CallVolumePreferenceController: config_show_call_volume (true) && !AudioHelper.isSingleVolume   SHOWN
  -165 take_call_on_output ListPreference             HandsFreeProfileOutputPreferenceController [mem: BT hands-free]   hidden
  -160 ring_volume     VolumeSeekBarPreference        RingVolumePreferenceController: Utils.isVoiceCapable && !isSingleVolume   hidden (tablet is not voice capable)
  -150 alarm_volume    VolumeSeekBarPreference        AlarmVolumePreferenceController: config_show_alarm_volume (true) && !isSingleVolume   SHOWN
  -140 notification_volume VolumeSeekBarPreference    NotificationVolumePreferenceController: config_show_notification_volume (true)
                                                      && !Utils.isVoiceCapable && !isSingleVolume   SHOWN (ONLY because the tablet is not voice capable)
  -130 vibrate_when_ringing SwitchPreference ("Vibrate for calls")                                                       hidden
  -120 zen_mode        RestrictedPreference "Do Not Disturb" (settings:allowDividerAbove=true)           shown, Tj: REMOVE
  -110 gesture_prevent_ringing_sound  Preference "Shortcut to prevent ringing"                           shown, Tj: REMOVE
  -100 ringtone        DefaultRingtonePreference "Phone ringtone": PhoneRingtonePreferenceController.isAvailable = Utils.isVoiceCapable   hidden
   -90 notification_ringtone DefaultRingtonePreference   (section 1.5)                                                 SHOWN
   -80 alarm_ringtone  DefaultRingtonePreference   (section 1.5)                                                       SHOWN
   -50 other_sounds_and_vibrations_category PreferenceCategory (section 1.6)                                           SHOWN
   100 sound_work_settings_section WorkOnlyCategory (work profile only)                                                hidden
  Utils.isVoiceCapable = TelephonyManager.isVoiceCapable() [mem; public API 22]. Class names DefaultNotification/DefaultAlarmRingtonePreferenceController
  do NOT exist in this release: the controllers are NotificationRingtonePreferenceController, AlarmRingtonePreferenceController,
  PhoneRingtonePreferenceController, all extending RingtonePreferenceControllerBase (same package).
- config bools (settings10/res/values/config.xml l.296-317): config_show_media/alarm/call/notification_volume, config_show_notification_ringtone,
  config_show_screen_locking_sounds, config_show_charging_sounds, config_show_touch_sounds are all true; has_dock_settings false; has_boot_sounds false.
- Divider above zen_mode = allowDividerAbove; the heading's divider above the category comes from the category layout/theme (androidx, not in checkout);
  Settings' own twin preference_category_material_settings_with_divider = preference_category_divider (1dp, ?android:attr/dividerHorizontal) +
  preference_category_material (fwbase/packages/SettingsLib/res/layout/). [tablet] dividers 1 px #DADCE0.

### 1.2 Volume slider row (the four sliders): layout settings10/res/layout/preference_volume_slider.xml
- Classes: VolumeSeekBarPreference extends settings/widget/SeekBarPreference (extends RestrictedPreference); setLayoutResource(preference_volume_slider);
  SeekBarPreference sets selectable=false (no row click, no ripple; only the SeekBar reacts) and sets the SeekBar content description = row title.
- Root LinearLayout horizontal: minHeight ?listPreferredItemHeightSmall (48dp = 51 px), gravity center_vertical, paddingStart/End 16dp (17 px), clickable=false.
  - icon_frame LinearLayout: minWidth 44dp (47 px), gravity start|center_vertical, paddingEnd 12dp (13), paddingTop/Bottom 4dp (4);
    icon = com.android.internal.widget.PreferenceImageView @android:id/icon (internal id used by findViewById) 24dp x 24dp (26 px).
  - Vertical LinearLayout, marginTop 8dp, marginBottom 8dp (9 px each):
    - Horizontal LinearLayout: title TextView @android:id/title (weight 1, paddingStart 12dp, singleLine, DeviceDefault.Subhead 16sp = 20 px, textColorPrimary,
      ellipsize marquee, fadingEdge horizontal) + widget_frame (wrap_content x match_parent, end|center_vertical, paddingStart 12dp, vertical; EMPTY for volume rows).
    - Vertical LinearLayout: SeekBar @*android:id/seekbar (layout_height 48dp = 51 px, match_parent width, paddingStart 12dp = 13 px, layout_gravity center_vertical)
      + TextView @+id/suppression_text (visibility gone; paddingStart 12dp; singleLine; ellipsize end; DeviceDefault.Body1 14sp; textColorSecondary).
- SeekBar style = ?seekBarStyle = Widget.Material.Light.SeekBar = Widget.Material.SeekBar (fwbase styles_material.xml l.802): progressDrawable
  @drawable/seekbar_track_material, thumb @drawable/seekbar_thumb_material_anim, splitTrack true, useDisabledAlpha false, focusable true, paddingStart 16dip
  and paddingEnd 16dip (the layout overrides paddingStart only), mirrorForRtl true, background @drawable/control_background_32dp_material.
  seekbar_track_background_height_material 2dp and seekbar_track_progress_height_material 2dp (fwbase dimens_material.xml l.137-138).
  The drawables seekbar_track_material, seekbar_thumb_material_anim, control_background_32dp_material are NOT FOUND (core/res/res/drawable absent).
- Geometry [calc] against [tablet]: icon x 17-43 (centre 30 = measured); title text x = 17+47+13 = 77 (= measured 77); SeekBar left 64, track starts at 64+13 = 77
  (measured 77); SeekBar right edge 600-17 = 583, track ends 583-17 = 566 (measured 565); row height = 9+28 (title line)+51 (SeekBar)+9 = 97 px (NOT measured by
  Tj); track 2 px, thumb 11 px, inactive track #D7D7D7 = colorControlNormal #8A000000 x disabled_alpha_material_light 0.26 over #FAFAFA (calc 215 = #D7), active
  colorControlActivated = #008577 [tablet+calc].
- suppression_text (Ring/Notification rows only): text = SuppressorHelper.getSuppressionText(ctx, NotificationManager.getEffectsSuppressor()) when a suppressor
  is set [src class, text from memory: "Muted by <app>"]; empty -> view stays gone.

### 1.3 The four rows: strings, icons, streams, controllers (settings10/src/com/android/settings/notification/*VolumePreferenceController.java)
- Common base VolumeSeekBarPreferenceController -> AdjustVolumeRestrictedPreferenceController -> SliderPreferenceController. displayPreference() calls
  pref.setCallback(SoundSettings.VolumePreferenceCallback), pref.setStream(getAudioStream()), pref.setMuteIcon(getMuteIcon()). onResume/onPause forward to the pref
  (start/stop the SeekBarVolumizer). updateState enforces UserManager.DISALLOW_ADJUST_VOLUME (admin restriction; irrelevant on the tablet).
  VolumeSeekBarPreference.setStream(stream): setMax(AudioManager.getStreamMaxVolume), setMin(AudioManager.getStreamMinVolumeInt [hidden]), setProgress(getStreamVolume).
  Slider max = stream max index, step = 1 index (SeekBar integer progress, no snapping, no percentage text), range [min..max].
  Stream limits [mem, AudioService defaults, OEM builds can differ: READ THEM AT RUN TIME]: max music 15, voice call 5, alarm 7, notification 7, ring 7;
  min 0 except voice call 1 (alarm min is 1 in some releases). Public min API = AudioManager.getStreamMinVolume(stream) (API 28) (AudioHelper.getMinVolume uses it,
  falling back to STREAM_VOICE_CALL on IllegalArgumentException).
  key              string resource -> English                  icon                                         stream                 mute icon
  media_volume     media_volume_option_title "Media volume"    settings10/res/drawable/ic_media_stream      STREAM_MUSIC           ic_media_stream_off
  call_volume      call_volume_option_title "Call volume"      ic_local_phone_24_lib                        STREAM_VOICE_CALL, or STREAM_BLUETOOTH_SCO while AudioManager.isBluetoothScoOn()   ic_local_phone_24_lib (call cannot be fully muted)
  alarm_volume     alarm_volume_option_title "Alarm volume"    @*android:drawable/ic_audio_alarm (framework drawable, NOT FOUND)   STREAM_ALARM   com.android.internal.R.drawable.ic_audio_alarm_mute (NOT FOUND)
  notification_volume notification_volume_option_title "Notification volume"  ic_notifications               STREAM_NOTIFICATION    ic_notifications_off_24dp (see ringer-mode override below)
  ring_volume (hidden on tablet)  ring_volume_option_title "Ring volume"   ic_notifications                 STREAM_RING            follows ringer mode
  Order on screen: media, call, (ring), alarm, notification [tablet: Media, Call, Alarm, Notification]. Icon vectors: 24dp, viewport 24, android:tint
  ?android:attr/colorControlNormal (#8A000000): ic_media_stream, ic_media_stream_off, ic_local_phone_24_lib, ic_notifications, ic_notifications_off_24dp (autoMirrored),
  ic_volume_ringer_vibrate; path data in the named files. ic_audio_alarm / ic_audio_alarm_mute: NOT FOUND in the checkout [tablet: alarm clock glyph].
- NotificationVolumePreferenceController extends RingVolumePreferenceController: it INHERITS the ringer-mode icon logic. After onResume the pref gets showIcon():
  ringer VIBRATE -> ic_volume_ringer_vibrate, SILENT -> ic_notifications_off_24dp, NORMAL -> ic_notifications. VolumeSeekBarPreference.updateIconView() order:
  (1) showIcon override if != 0, (2) mute icon if muted && !zenMuted, (3) the XML icon. Since showIcon(ic_notifications) is also called in NORMAL mode, the
  "muted at volume 0" icon branch (2) is never reached for the Notification row once onResume ran: only the ringer mode changes the glyph. For Media/Alarm/Call
  branch (2) applies when SeekBarVolumizer reports muted (source NOT in checkout [mem]: muted = AudioManager.isStreamMute(stream); zenMuted = zen mode mutes it).
  Ringer mode source: AudioHelper.getRingerModeInternal() (hidden) + broadcast AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION (hidden) and
  NotificationManager.ACTION_EFFECTS_SUPPRESSOR_CHANGED.
- Reads/writes (AudioHelper, settings10/src/.../notification/AudioHelper.java): getStreamVolume, getStreamMaxVolume, getStreamMinVolume,
  setStreamVolume(stream, v, flags 0) (used by the Slices path setSliderPosition), isSingleVolume() = AudioSystem.isSingleVolume(ctx) [hidden],
  getRingerModeInternal, getLastAudibleStreamVolume [hidden]. The interactive writes are done by android.preference.SeekBarVolumizer (hidden class,
  source NOT FOUND). [mem] its behaviour: start() registers a ContentObserver on Settings.System volume_<stream> and a receiver for VOLUME_CHANGED_ACTION,
  STREAM_MUTE_CHANGED_ACTION, RINGER_MODE_CHANGED_ACTION, INTERNAL_RINGER_MODE_CHANGED_ACTION, STREAM_DEVICES_CHANGED_ACTION and
  NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED; onProgressChanged posts AudioManager.setStreamVolume(stream, progress, FLAG_SHOW_UI_WARNINGS) to a
  worker thread; zen-muted streams disable the SeekBar and show lastAudible progress.
- AudioHelper/Slices also listen (AdjustVolumeRestrictedPreferenceController.getIntentFilter): AudioManager.VOLUME_CHANGED_ACTION,
  STREAM_MUTE_CHANGED_ACTION, MASTER_MUTE_CHANGED_ACTION (hidden string constants "android.media.VOLUME_CHANGED_ACTION" etc. [mem]).

### 1.4 Release sample sound (when a finger lifts after dragging)
- SoundSettings.VolumePreferenceCallback (SoundSettings.java l.198-228): onSampleStarting(sbv) stops the previous volumizer's sample if another one is current,
  stores sbv, removes pending STOP_SAMPLE and posts STOP_SAMPLE after SAMPLE_CUTOFF = 2000 ms ("manually cap sample playback at 2 seconds");
  onStreamValueChanged(stream, progress) re-arms the same 2000 ms timer if a sample is current; SoundSettings.onPause() stops the sample.
- Media: SeekBarVolumizer gets sampleUri = "android.resource://<settings package>/<numeric id of R.raw.media_volume>" (VolumeSeekBarPreference.getMediaVolumeUri). File
  settings10/res/raw/media_volume.ogg: mono, 44.1 kHz Vorbis, 11,969 bytes, last granule 21,789 samples = 0.494 s (measured here). Other streams pass sampleUri =
  null and SeekBarVolumizer picks defaults [mem]: ring -> Settings.System.DEFAULT_RINGTONE_URI, notification -> DEFAULT_NOTIFICATION_URI, else
  DEFAULT_ALARM_ALERT_URI. [mem] the sample plays when the finger is released (onStopTrackingTouch), through RingtoneManager.getRingtone(...) with the stream
  type, attributes flags BYPASS_INTERRUPTION_POLICY|BYPASS_MUTE; the 2 s cutoff above stops long alarm/notification sounds.

### 1.5 "Default notification sound" and "Default alarm sound" (DefaultRingtonePreference rows)
- notification_ringtone: title notification_ringtone_title "Default notification sound", dialogTitle same, summary initially summary_placeholder (U+00A0 nbsp),
  android:ringtoneType="notification" (attrs.xml flag 2 = RingtoneManager.TYPE_NOTIFICATION), order -90. Controller NotificationRingtonePreferenceController
  (isAvailable = config_show_notification_ringtone, true).
- alarm_ringtone: title alarm_ringtone_title "Default alarm sound", dialogTitle same, summary_placeholder, android:persistent="false", ringtoneType alarm (flag 4,
  TYPE_ALARM), order -80. AlarmRingtonePreferenceController (always available). (Phone ringtone: ringtone_title "Phone ringtone", type flag 1, hidden here.)
- Widget: androidx Preference subclass (settings10/src/com/android/settings/RingtonePreference.java, DefaultRingtonePreference.java), standard preference row (title
  16sp textColorPrimary + summary 14sp textColorSecondary), NO icon, NO widget; two-line row = 87 px [tablet]. Title text x 77 [tablet].
- Summary = title of the CURRENT default sound: RingtonePreferenceControllerBase.updateState -> background thread: uri = RingtoneManager.getActualDefaultRingtoneUri(ctx,
  type); summary = Ringtone.getTitle(ctx, uri, followSettingsUri=false, allowRemote=true) (hidden static) -> posted to main thread; uri == null or title == null
  leaves the placeholder. [mem] getTitle reads MediaStore TITLE for a media Uri and falls back to framework string ringtone_unknown ("Unknown"). Refresh happens in
  updateState (onResume), i.e. right after returning from the picker. [tablet] examples: "On The Hunt", "Cesium".
- Click (SoundSettings.onPreferenceTreeClick l.114-125): rp.onPrepareRingtonePickerIntent(rp.getIntent()) then getActivity().startActivityForResultAsUser(intent,
  REQUEST_CODE 200, null, UserHandle.of(rp.getUserId())). Intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER) ("android.intent.action.RINGTONE_PICKER")
  with extras: EXTRA_RINGTONE_EXISTING_URI = getActualDefaultRingtoneUri(type) (the row is preselected), EXTRA_RINGTONE_SHOW_DEFAULT = false (DefaultRingtone
  overrides to false; so EXTRA_RINGTONE_DEFAULT_URI is not added), EXTRA_RINGTONE_SHOW_SILENT = true, EXTRA_RINGTONE_TYPE = 2 or 4, EXTRA_RINGTONE_TITLE = row title,
  EXTRA_RINGTONE_AUDIO_ATTRIBUTES_FLAGS = AudioAttributes.FLAG_BYPASS_INTERRUPTION_POLICY [mem: hidden constant].
- Result (SoundSettings.onActivityResult -> RingtonePreference.onActivityResult): resultCode is NOT checked; data == null (cancel) does nothing; otherwise uri =
  data.getParcelableExtra(EXTRA_RINGTONE_PICKED_URI) (null = "None"/silent), callChangeListener(uri string or ""), then DefaultRingtonePreference.onSaveRingtone =
  RingtoneManager.setActualDefaultRingtoneUri(userContext, type, uri).
- Storage of the choice [mem]: Settings.System keys notification_sound (NOTIFICATION_SOUND), alarm_alert (ALARM_ALERT), ringtone (RINGTONE), value = Uri string,
  null = silent. Permission [mem]: RingtoneManager.setActualDefaultRingtoneUri is @RequiresPermission(WRITE_SETTINGS) (appop; Settings.System.canWrite); a caller that
  holds WRITE_SECURE_SETTINGS is also accepted by SettingsProvider for the System table [mem, SettingsProvider.mutateSystemSetting]. Reading the current default
  (getActualDefaultRingtoneUri, Ringtone.getTitle(Context)) needs no permission.
- The picker UI itself: RingtonePickerActivity of package com.android.providers.media [mem]; fwbase core/res/res/values/config.xml has
  config_defaultRingtonePickerEnabled = true. UI NOT FOUND in the checkout [mem: AlertDialog radio list, "None" first, preview on selection, OK/Cancel].
  Framework strings (fwbase/core/res/res/values/strings.xml l.3347-3359): ringtone_default "Default ringtone", ringtone_default_with_actual "Default (%1$s)",
  ringtone_silent "None", ringtone_picker_title "Ringtones", ringtone_picker_title_alarm "Alarm sounds", ringtone_picker_title_notification "Notification
  sounds", ringtone_unknown "Unknown".

### 1.6 "Other sounds" category and the three switches
- Category other_sounds_and_vibrations_category (order -50): title other_sound_category_preference_title = "Other sounds and vibrations" (the XML string is NOT
  "Other sounds"; textAllCaps shows "OTHER SOUNDS AND VIBRATIONS" [tablet]; Tj wants the words "and vibrations" dropped). Heading style: section 0.
- Switches (SwitchPreference, androidx; row = title only, one-line row 62 px [tablet]; switch widget = framework Switch @+android:id/switch_widget in widget_frame
  paddingStart 16dp [fwbase layouts]; thumb/track drawables switch_* NOT FOUND [tablet: ON = teal thumb + pale teal track]). Each is managed by a
  SettingPrefController (SettingPref(type, key, setting, default 1 = DEFAULT_ON)); state = Settings.<table>.getInt(cr, setting, 1) != 0 (a missing value means ON);
  change = putInt(setting, checked ? 1 : 0) in onPreferenceChange; a ContentObserver registered in onResume (unregistered in onPause) on the setting Uri refreshes
  the switch. TYPE_GLOBAL=1, TYPE_SYSTEM=2, TYPE_SECURE=3.
  key                  title resource -> English                       table.constant (literal [mem])                                   availability
  screen_locking_sounds screen_locking_sounds_title "Screen locking sounds"   System.LOCKSCREEN_SOUNDS_ENABLED "lockscreen_sounds_enabled" (hidden const)  config_show_screen_locking_sounds (true)
  charging_sounds      charging_sounds_title "Charging sounds and vibration"   Secure.CHARGING_SOUNDS_ENABLED "charging_sounds_enabled" (hidden const)   config_show_charging_sounds (true)
  touch_sounds         touch_sounds_title "Touch sounds"                       System.SOUND_EFFECTS_ENABLED "sound_effects_enabled" (public const)      config_show_touch_sounds (true)
  - charging_sounds writes ONLY CHARGING_SOUNDS_ENABLED; there is no separate vibration key in this controller.
  - touch_sounds setSetting first runs AsyncTask.execute { AudioManager.loadSoundEffects() if value != 0 else unloadSoundEffects() } (both @hide) and then writes.
- Other rows of the category, hidden on the tablet: dial_pad_tones "Dial pad tones" (voice only), docking_sounds "Docking sounds" (has_dock_settings false),
  vibrate_on_touch "Touch vibration" + summary vibrate_on_touch_summary (hidden when there is no vibrator [mem]), dock_audio_media "Dock speaker plays", boot_sounds
  "Power on sounds" (has_boot_sounds false), emergency_tone "Emergency dialing signal" (summary "Set behavior when an emergency call is placed"; voice/CDMA only
  [mem]). "Sound enhancement" on the tablet is an OEM row: NOT in AOSP.

### 1.7 Gotchas for a non-system app (Sound)
- Hidden/not usable: SeekBarVolumizer (whole class), AudioManager.getStreamMinVolumeInt / getRingerModeInternal / getLastAudibleStreamVolume / loadSoundEffects /
  unloadSoundEffects, AudioSystem.isSingleVolume, Ringtone.getTitle(Context, Uri, boolean, boolean), the INTERNAL_/VOLUME_CHANGED_ACTION constants, Settings
  constants LOCKSCREEN_SOUNDS_ENABLED and Secure.CHARGING_SOUNDS_ENABLED (use the string literals). Public replacements [mem]: getStreamVolume/getStreamMaxVolume,
  getStreamMinVolume (API 28), isStreamMute (23), getRingerMode, isVolumeFixed (21), isBluetoothScoOn, TelephonyManager.isVoiceCapable (22), Ringtone.getTitle(Context),
  RingtoneManager.get/setActualDefaultRingtoneUri, Settings.System.canWrite.
- AudioManager.setStreamVolume for STREAM_RING / NOTIFICATION / SYSTEM can throw SecurityException("Not allowed to change Do Not Disturb state") for an app without
  Notification Policy (Do Not Disturb) access when the new value would flip the ringer mode to silent/vibrate [mem; documented in the public javadoc]; MUSIC, ALARM and
  VOICE_CALL are not affected. Wrap every call in try/catch. AppOps stream checks can also drop the call silently.
- The ringtone picker is reached with a normal startActivityForResult (public action and result extra); our own Activity is required to receive the result.
  Writing the default needs WRITE_SETTINGS or the adb-granted WRITE_SECURE_SETTINGS; Settings itself does it as a system app.
- The three switches write Settings.System / Settings.Secure: System needs WRITE_SETTINGS or WRITE_SECURE_SETTINGS; Secure (charging_sounds_enabled) needs
  WRITE_SECURE_SETTINGS (adb grant). Without loadSoundEffects() the effect of "Touch sounds" ON may not be audible until the audio service reloads its effects [mem, test it].
- media_volume.ogg is a resource of the Settings APK: not reachable from another package; the app must ship its own copy (the file is in settings10/res/raw/).
  Volume-slider icon for alarm: ic_audio_alarm is a framework drawable not in the checkout (use Resources.getSystem().getIdentifier("ic_audio_alarm", "drawable",
  "android") at run time [mem], or a copy of the Material alarm vector).
- OEM differences [tablet]: 4 sliders = AOSP result for a non-voice-capable device; "Sound enhancement" is vendor; stream max values are device specific.

## 2. STORAGE page

### 2.1 Page frame
- Fragment settings10/src/com/android/settings/deviceinfo/StorageDashboardFragment.java, XML settings10/res/xml/storage_dashboard_fragment.xml (key
  storage_dashboard_screen, title storage_settings = "Storage", orderingFromXml=false so android:order decides). Home row (Settings home): key top_level_storage, title
  "Storage", icon ic_homepage_storage, summary storage_summary = "<percent> used - <free> free" built by TopLevelStoragePreferenceController: NumberFormat.
  getPercentInstance().format(used/total) + Formatter.formatFileSize(ctx, free) (public API, SI units, two decimals: "95% used - 1.57 GB free" [tablet]).
- Toolbar: onViewCreated runs EntityHeaderController.styleActionBar(activity): ActionBar background = ColorDrawable(colorPrimaryDark = #E0E0E0), elevation 0
  (settings10/src/com/android/settings/widget/EntityHeaderController.java). So on this page the toolbar is #E0E0E0 (same as the status bar, no shadow) instead of
  the usual #F5F5F5 + shadow: Tj's "gray header". NOT FOUND: any background on the donut block itself (storage_summary_donut.xml sets none, so it sits on #FAFAFA).
- No options menu on the internal volume ("Migrate data" menu item exists but is invisible unless another private volume is the current storage).
- Rows in order: pref_summary (0), toggle_asm (1), pref_photos_videos (2), pref_music_audio (3), pref_games (4), pref_movies (5), pref_other_apps (6), pref_files (7),
  pref_system (100), category pref_secondary_users (200, title storage_other_users "Other users", only with more users). Divider above Photos
  (settings:allowDividerAbove=true) and above/below toggle_asm.
- Loading: onResume restarts two loaders (app sizes: StorageAsyncLoader; volume sizes: VolumeSizesLoader). Until sizes arrive every row summary is
  memory_calculating_size "Calculating..." (U+2026) and the bars are uninitialised; cached values (shared prefs "CachedStorageValues", valid 5 minutes =
  Settings.Global STORAGE_SETTINGS_CLOBBER_THRESHOLD default, same user) are shown first; a full-screen loading container is shown while quota stats are supported.

### 2.2 Header: donut block (settings10/res/layout-sw360dp/storage_summary_donut.xml; layout-land is identical)
- The tablet (sw 564.7 dp >= 360, < 600) takes layout-sw360dp + values-sw360dp. The default settings10/res/layout/storage_summary_donut.xml is a VERTICAL phone layout
  (not used here). Preference class StorageSummaryDonutPreference (androidx Preference, not clickable, layout storage_summary_donut), key pref_summary.
- Root LinearLayout horizontal, center_vertical, wrap_content height. Left LinearLayout (weight 1, vertical, center_vertical): paddingStart
  @dimen/preference_no_icon_padding_start = 72dp (77 px; fwbase/packages/SettingsLib/res/values/styles_support_preference.xml), paddingEnd
  @dimen/storage_summary_padding_end = 16dp (17), paddingTop 32dp (34), paddingBottom 32dp (34). Children:
  - @android:id/title TextView: match_parent, singleLine, ellipsize marquee, fadingEdge horizontal, fontFamily @*android:string/config_headlineFontFamily (empty =
    Roboto), textAppearance DeviceDefault.Display1 (textColorSecondary #8A000000), textSize 36sp (46 px, line 62 px).
  - @android:id/summary TextView: match_parent, marginStart 4dp, marginEnd 4dp, maxLines 10, paddingBottom 20dp (21 px), DeviceDefault.Body1 (14sp = 18 px, line 25 px),
    textColorPrimary (#DE000000).
  - Button @+id/deletion_helper_button: text storage_menu_free "Free up space", style @style/ActionPrimaryButton (= android:Widget.DeviceDefault.Button.Colored),
    wrap_content. Click = startActivity(new Intent(StorageManager.ACTION_MANAGE_STORAGE)) ("android.os.storage.action.MANAGE_STORAGE", public API 26). [Tj: REMOVE]
    Text block height without the button [calc]: 34+62+25+21+34 = 176 px.
- Right: com.android.settings.widget.DonutView @+id/donut 112dp x 112dp (119 px), layout_marginEnd 32dp (34 px), gravity end|center_vertical, minWidth 58dp,
  paddingStart/End = listPreferredItemPaddingStart/End (16dp; ignored by onDraw, which uses getWidth()/getHeight()). View spans x 447-566.
- Title text = StorageSummaryDonutPreferenceController.convertUsedBytesToFormattedText: Formatter.formatBytes(res, usedBytes, flags 0) (hidden) -> BytesResult
  value/units, expanded into storage_size_large_alternate = "^1<small> <font size="20">^2</font></small>" (number, then a space inside <small> = RelativeSizeSpan 0.8, then
  the unit inside <font size="20"> = AbsoluteSizeSpan(20, dip=true) [mem], i.e. 20 dip = 21.25 px NOT scaled by font scale; the enclosing RelativeSizeSpan 0.8 may be
  overridden by it depending on span order: measure the "GB" glyph height on the tablet). Example [tablet] "30.28" + "GB".
- Summary text = getString(storage_volume_total) = "Used of %1$s" with Formatter.formatShortFileSize(ctx, totalBytes) (public API, SI, shorter) = "Used of 32 GB" [tablet].
- DonutView (settings10/src/com/android/settings/widget/DonutView.java): arc rect = (stroke, stroke, width-stroke, height-stroke), BUTT caps, STROKE style, antialias.
  stroke: the constructor reads storage_donut_thickness = 4dp (settings10/res/values/dimens.xml l.319) as the float 4.25, but the inflate path then calls
  styledAttrs.getDimensionPixelSize(DonutView_thickness, (int) 4.25) and the attribute is not set in the layout, so the stroke is the INTEGER 4 px [calc].
  Background circle: 360 degrees from -90 (12 o'clock); filled arc: sweep 360 x percent from -90 clockwise. Both Paints get PorterDuffColorFilter(colorAccent
  #008577, SRC_IN): filled arc = meter_consumed_color (colorForeground, alpha 1.0) -> opaque teal; track = meter_background_color (colorForeground alpha 0.3) ->
  teal at 30% alpha (fwbase/packages/SettingsLib/res/color/meter_*_color.xml). Outer ring diameter [calc] 119 - 4 = 115 px (= Tj's measured 115), centreline 111, inner 107.
  percent = used/total (double). Inner text, centred in the view: line 1 = Utils.formatPercentage(percent) = NumberFormat.getPercentInstance().format(percent) ("95%";
  HALF_EVEN, 0 decimals) in typeface Typeface.create(config_headlineFontFamily = "") = Roboto Regular, colour accent, size storage_donut_view_percent_text_size; the
  localized percent sign gets RelativeSizeSpan(storage_donut_view_percent_sign_size / percent_text_size); line 2 = storage_percent_full "used", accent, size
  storage_donut_view_label_text_size (TextPaint.setTextSize with the float px; shrinks to 10sp if the string is longer than 10 chars; content description
  join_two_unrelated_items). Block height = textHeight(label)+textHeight(big) via descent-ascent, translated to be centred; line 2 baseline = centerY + total/2 - descent.
  Sizes by qualifier: default (phone) percent 45sp / label 21sp / sign 20sp; values-sw360dp AND values-land: percent 30sp (38.25 px), label 14sp (17.85 px), sign 20sp
  (25.5 px, ratio 0.667). The tablet gets the sw360dp values. (dimens: settings10/res/values/dimens.xml l.308-319, values-sw360dp/dimens.xml l.28-31.)
- Data (StorageDashboardFragment.onReceivedSizes): used = totalBytes - freeBytes; updateBytes(used, total) -> invalidateData -> updateState (title, summary, percent).

### 2.3 Row template (settings10/res/layout/storage_item.xml, class settings10/src/com/android/settings/deviceinfo/StorageItemPreference.java)
- Root LinearLayout vertical, minHeight ?listPreferredItemHeightSmall (51 px), gravity center_vertical, paddingEnd listPreferredItemPaddingEnd (17), paddingTop 14dp,
  paddingBottom 14dp (15 px each), background ?selectableItemBackground (ripple; the row is clickable), clipToPadding false. Inside: horizontal LinearLayout
  paddingStart listPreferredItemPaddingStart (17), center_vertical:
  - icon_frame: minWidth 56dp (60 px), start|center_vertical, paddingTop/Bottom 4dp; icon PreferenceImageView wrap_content, maxWidth/maxHeight 24dp (26 px).
  - RelativeLayout (match_parent x wrap_content, center_vertical): title TextView (@android:id/title) singleLine, DeviceDefault.Subhead (16sp = 20 px, line 28),
    textColorPrimary, ellipsize marquee, fadingEdge horizontal; summary TextView (@android:id/summary) DeviceDefault.Body1 14sp, textColor ?textColorSecondary
    (#8A000000), maxLines 10, layout_alignParentEnd (the size is right aligned at the title's top); ProgressBar (@android:id/progress) match_parent x 8dp (9 px),
    layout_marginTop 12dp (13), marginBottom 4dp (4), max 100, below the title, alignStart title, style ?progressBarStyleHorizontal.
- ProgressBar style = Widget.Material.Light.ProgressBar.Horizontal (fwbase styles_material.xml l.774: progressDrawable @drawable/progress_horizontal_material,
  minHeight/maxHeight 16dip). progress_horizontal_material (and its track tint, progress_bar_height_material 4dp, progress_bar_corner_material 0dp = square ends,
  config_progressBarCornerRadius -> 0dp) : drawable NOT FOUND [mem: bar 4dp tall centred in the 8dp view, fill colorControlActivated = accent, track
  colorControlNormal-tinted light gray]. [tablet] visible bar about 5 px (4dp = 4.25 px), teal over light gray, x 77-583, no rounded ends.
- Row height [calc] = 15+15 + 28 (title line) + 13 + 9 + 4 = 84 px = Tj's measured storage row pitch 84 px (the earlier 79 px estimate missed the font padding and the
  rounding). Geometry: icon x 17-43 (centre 30), title x = 17+60 = 77, bar x 77 to 600-17 = 583 (matches [tablet]).
- Bar value: setStorageSize(size, total): progress = (total == 0) ? 0 : (int)(size * 100 / total) (integer truncation, ProgressBar max 100; no clamp).
- Size text: FileSizeFormatter.formatFileSize(ctx, size, suffix = identifier of android:string/gigabyteShort ("GB"), mult = 1,000,000,000) (settings10/src/com/android/
  settings/utils/FileSizeFormatter.java): ALWAYS gigabytes, DECIMAL: value = size / 1e9 as float; value < 1 -> "%.2f"; value < 10 -> "%.1f"; else "%.0f"; then
  fileSizeSuffix = "%1$s %2$s" (plain space) wrapped in BidiFormatter.unicodeWrap. Results [tablet]: "3.3 GB", "0.54 GB", "3.0 GB", "0.00 GB", "3.7 GB", "15 GB", "4.3 GB".
- Icons (24dp vectors, viewport 24, tint ?android:attr/colorControlNormal; also re-tinted in code by StorageItemPreferenceController.applyTint with colorControlNormal):
  settings10/res/drawable/ic_photo_library, ic_media_stream, ic_videogame_vd_theme_24, ic_local_movies, ic_storage_apps, ic_folder_vd_theme_24 and
  fwbase/packages/SettingsLib/res/drawable/ic_system_update (System row), ic_storage (Storage manager row), ic_homepage_storage (Settings home row; coloured tile).

### 2.4 The seven rows (settings10/src/com/android/settings/deviceinfo/storage/StorageItemPreferenceController.java and StorageAsyncLoader.java)
  key (order)           title resource -> English                       icon                          size shown = data source
  pref_photos_videos(2) storage_photos_videos "Photos & videos"         ic_photo_library              photosAppsSize + externalStats.imageBytes + externalStats.videoBytes
  pref_music_audio (3)  storage_music_audio "Music & audio"             ic_media_stream               musicAppsSize + externalStats.audioBytes
  pref_games (4)        storage_games "Games"                           ic_videogame_vd_theme_24      gamesSize
  pref_movies (5)       storage_movies_tv "Movie & TV apps"             ic_local_movies               videoAppsSize
  pref_other_apps (6)   storage_other_apps "Other apps"                  ic_storage_apps               otherAppsSize
  pref_files (7)        storage_files "Files"                           ic_folder_vd_theme_24         externalStats.totalBytes - audioBytes - videoBytes - imageBytes - appBytes
  pref_system (100)     storage_detail_system "System"                  ic_system_update              max(TrafficStats.GB_IN_BYTES, usedBytes - attributed), attributed = for EVERY user (games+music+video+photos+otherApps sizes) + (externalStats.totalBytes - externalStats.appBytes)
  The Files row is added only if StorageManager.findEmulatedForPrivate(volume) is non-null and isMountedReadable(), else removed. TrafficStats.GB_IN_BYTES =
  1,073,741,824 (binary) [mem; class not in checkout]: the System row never shows less than 1.07 GB (displayed with the decimal formatter: "1.1 GB").
- App attribution (StorageAsyncLoader.getStorageResultForUser): for every installed ApplicationInfo of each user (PackageManager.getInstalledApplicationsAsUser(0, user)):
  stats = StorageStatsManager.queryStatsForPackage(uuid, packageName, user); blamed = dataBytes; if cacheQuota(uuid, uid) < cacheBytes then blamed = blamed - cacheBytes +
  cacheQuota; the first time a package name is seen also += codeBytes (StorageStats code bytes; public getter is getAppBytes() [mem]); then by ApplicationInfo.category:
  CATEGORY_GAME -> games; AUDIO -> music; VIDEO -> video (Movie & TV); IMAGE -> photos; default: FLAG_IS_GAME -> games, else other apps. External (shared storage) stats =
  StorageStatsManager.queryExternalStatsForUser(uuid, user) (totalBytes, audioBytes, videoBytes, imageBytes, appBytes). Calls via settings10 StorageStatsSource
  (fwbase/packages/SettingsLib/src/com/android/settingslib/applications/StorageStatsSource.java l.42-59).
- Click actions (handlePreferenceTreeClick; every intent gets Intent.EXTRA_USER_ID, launched with startActivityForResult(AsUser) request code 0; ActivityNotFoundException is
  only logged):
  - Photos & videos -> SubSettingLauncher(ManageApplications) title storage_photos_videos, EXTRA_CLASSNAME Settings.PhotosStorageActivity, EXTRA_STORAGE_TYPE =
    STORAGE_TYPE_PHOTOS_VIDEOS (3), list type LIST_TYPE_PHOTOGRAPHY, filter ApplicationsState.FILTER_PHOTOS. That list's header row (PhotosViewHolderController): icon
    ic_photo_library, title storage_detail_images "Images", size = imageBytes + videoBytes, click ACTION_VIEW type "image/*" with FLAG_ACTIVITY_NEW_DOCUMENT and
    EXTRA_FROM_STORAGE=true.
  - Music & audio -> ManageApplications title storage_music_audio, class Settings.StorageUseActivity, EXTRA_STORAGE_TYPE = STORAGE_TYPE_MUSIC (1), EXTRA_VOLUME_UUID/NAME,
    filter FILTER_AUDIO. Header row (MusicViewHolderController): icon ic_headset_24dp, title audio_files_title "Audio files", size = audioBytes, click ACTION_VIEW on
    DocumentsContract.buildRootUri("com.android.providers.media.documents", "audio_root") with type DocumentsContract.Root.MIME_TYPE_ITEM, CATEGORY_DEFAULT.
  - Games -> ManageApplications title game_storage_settings "Games", class Settings.GamesStorageActivity (LIST_TYPE_GAMES, FILTER_GAMES).
  - Movie & TV apps -> ManageApplications title storage_movies_tv, class Settings.MoviesStorageActivity (LIST_TYPE_MOVIES, FILTER_MOVIES).
  - Other apps -> ManageApplications title apps_storage "Apps storage", class Settings.StorageUseActivity, volume uuid/name (LIST_TYPE_STORAGE, FILTER_OTHER_APPS);
    does nothing if the volume is null.
  - Files -> sharedVolume.buildBrowseIntent() (VolumeInfo, hidden; [mem] ACTION_BROWSE / documents root "primary" of com.android.externalstorage.documents).
  - System -> AlertDialog (PrivateVolumeSettings.SystemInfoFragment): message storage_detail_dialog_system = "System includes files used to run Android version
    <Build.VERSION.RELEASE>", positive button android.R.string.ok "OK", no title.
  - Storage manager row (toggle_asm, "Storage manager", MasterSwitchPreference, ic_storage, fragment AutomaticStorageManagerSettings): isAvailable =
    !ActivityManager.isLowRamDeviceStatic() -> HIDDEN on Android Go (matches [tablet], no such row).

### 2.5 Total / free / "32 GB" (where the numbers come from)
- StorageManagerVolumeProvider (SettingsLib): total = StorageStatsManager.getTotalBytes(volume.getFsUuid()), free = StorageStatsManager.getFreeBytes(fsUuid);
  VolumeSizesLoader.getVolumeSize -> PrivateStorageInfo(free, total); StorageDashboardFragment uses used = total - free. The Settings home summary uses
  PrivateStorageInfo.getPrivateStorageInfo, which SUMS total and free over all TYPE_PRIVATE mounted-readable volumes.
- [mem] StorageStatsService.getTotalBytes for the default (internal) volume returns StorageManager.getPrimaryStorageSize() = FileUtils.roundStorageSize(total space of
  the data partition): rounded UP to the next value of {1,2,4,...,512} x 1000^n bytes (decimal), e.g. 28.4e9 -> 32,000,000,000. That is why the page says "32 GB".
  getFreeBytes = usable space of /data PLUS the cache the system may clear (cache above its reserved size) when quotas are supported [mem], so "used" excludes
  clearable cache.
- Unit consistency check [calc, matches [tablet]]: used 30.28e9 / total 32e9 = 0.94625 -> "95%"; the header number 30.28 is therefore decimal (SI) too:
  Formatter.formatBytes(res, bytes, 0) in this release counts 1000 per step (FLAG_IEC_UNITS absent) [mem]. Rules [mem, android.text.format.Formatter, not in checkout]:
  pick the first unit where the value is <= 900 (B, kB, MB, GB, TB); value >= 100 or unit bytes -> "%.0f"; value < 1 -> "%.2f"; 1 <= value < 10 -> "%.2f" without
  FLAG_SHORTER ("%.1f" with it); 10 <= value < 100 -> "%.2f" without FLAG_SHORTER ("%.0f" with it). So the header ("30.28") uses flags 0 (two decimals), the total ("32 GB")
  and the Settings-home free ("1.57 GB" via formatFileSize, no SHORTER) / total via formatShortFileSize (SHORTER) differ, and the rows use FileSizeFormatter (always GB,
  1 decimal below 10, none from 10). Strings (fwbase core/res/res/values/strings.xml l.27-39): megabyteShort "MB", gigabyteShort "GB", terabyteShort "TB", fileSizeSuffix
  "%1$s %2$s".
- Secondary users: SecondaryUserController adds one StorageItemPreference per extra user under "Other users"; none on a single-user tablet.

### 2.6 Gotchas for a non-system app (Storage)
- Public (API 26): StorageStatsManager.getTotalBytes(UUID) / getFreeBytes(UUID) (use StorageManager.UUID_DEFAULT; no permission [mem]) give the same rounded total and
  free-with-clearable-cache as Settings. queryStatsForPackage / queryStatsForUid / queryExternalStatsForUser / getCacheQuotaBytes need PACKAGE_USAGE_STATS (appop
  GET_USAGE_STATS; the adb grant is enough) [mem]. Do NOT use File.getTotalSpace() for the total (28.x GB, not 32 GB).
- Hidden: StorageManager.getPrimaryStorageSize, VolumeInfo.buildBrowseIntent / findEmulatedForPrivate, UserManager.getUsers (MANAGE_USERS), Formatter.formatBytes,
  TrafficStats.GB_IN_BYTES, android:string/gigabyteShort (non-public string; hard-code "GB"), SubSettingLauncher/ManageApplications and the Settings.*StorageActivity
  classes. Public: Formatter.formatFileSize / formatShortFileSize (SI), ApplicationInfo.category (API 26) and FLAG_IS_GAME, StorageManager.ACTION_MANAGE_STORAGE,
  DocumentsContract.buildRootUri, Intent.ACTION_VIEW.
- Multi-user attribution cannot be reproduced exactly without MANAGE_USERS (only the own user's apps are visible); the code bytes are counted once per package name.
- Row intents that leave the app: Files / Music / Photos headers use documents-UI roots; on an Android Go build the handler may be Files Go instead of DocumentsUI:
  NOT verifiable here. The second-level lists (ManageApplications) are Settings-internal activities and cannot be started by a third-party app.
- The first paint of the stock page shows cached numbers (<= 5 min old) and then "Calculating..." rows; a full app-size scan (one queryStatsForPackage per installed
  package) can take seconds: run it off the main thread.
- DonutView colours come from PorterDuff SRC_IN over the accent: reproduce as accent at alpha 255 (arc) and alpha 77 (0.3) (track); text accent.

## 3. NOT FOUND in the checkout (do not guess)
- fwbase core/res/res/drawable and core/res/res/color: seekbar_track_material, seekbar_thumb_material_anim, control_background_32dp_material,
  progress_horizontal_material (and its tint), switch_thumb/track drawables, ic_audio_alarm, ic_audio_alarm_mute, text_color_secondary state list.
- androidx.preference layouts/styles (preference_material, preference_category_material, the divider decoration, the "Advanced" expand row).
- android.preference.SeekBarVolumizer, AudioService (stream limits, setStreamVolume checks), RingtoneManager, Ringtone.getTitle, Formatter, FileUtils.roundStorageSize,
  StorageStatsService, StorageManager, VolumeInfo, TrafficStats, android.provider.Settings key constants and literals, MediaProvider RingtonePickerActivity UI.
- Tablet-only: the OEM "Sound enhancement" row, the real value of the 8 px source of the extra row spacing (none needed: 84 px is reproduced), any gray background of the
  Storage donut block (the gray in Tj's screenshot is the status bar + restyled toolbar, to be confirmed against the screenshot).
