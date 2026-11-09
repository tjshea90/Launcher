package com.tj.launcher;

/**
 * BN1: the pure rules of the Sound page (JVM-tested: SsSoundRulesTest). Plain Java, no Android classes. Sources: AOSP 10 Settings (android-10.0.0_r47) {@code notification/*VolumePreferenceController},
 * {@code VolumeSeekBarPreference}, {@code SoundSettings}, {@code RingtonePreferenceControllerBase}, {@code SettingPref}; the platform's {@code SeekBarVolumizer} (hidden class, not in the checkout: its
 * rules are written from memory and marked "[mem]"); research/aosp10-settings/B_sound_storage.md sec. 1. The platform integers mirrored here (stream types, ringer modes, interruption filters,
 * ringtone types) are API constants that have not changed since they were introduced, so the tests can pin them.
 */
final class SsSoundRules {
    private SsSoundRules() {}

    // ------------------------------------------------------------------ the four slider rows
    static final int ROW_MEDIA = 0, ROW_CALL = 1, ROW_ALARM = 2, ROW_NOTIFICATION = 3;

    /** The row titles in Tj's order (his screenshot: Media, Call, Alarm, Notification; the strings are AOSP's media/call/alarm/notification_volume_option_title). No Ring row: the tablet is not voice capable. */
    static final String[] TITLES = { "Media volume", "Call volume", "Alarm volume", "Notification volume" };

    // android.media.AudioManager.STREAM_*
    static final int STREAM_VOICE_CALL = 0, STREAM_RING = 2, STREAM_MUSIC = 3, STREAM_ALARM = 4, STREAM_NOTIFICATION = 5, STREAM_BLUETOOTH_SCO = 6;
    private static final int[] STREAMS = { STREAM_MUSIC, STREAM_VOICE_CALL, STREAM_ALARM, STREAM_NOTIFICATION };

    /** The stream a row controls. Call volume follows the Bluetooth SCO link while a call is routed to it (CallVolumePreferenceController.getAudioStream). */
    static int streamFor(int row, boolean bluetoothScoOn) {
        if (row == ROW_CALL) return bluetoothScoOn ? STREAM_BLUETOOTH_SCO : STREAM_VOICE_CALL;
        return STREAMS[row];
    }

    /** The rows that are shown: all four, or only Media on a single-volume device (the call, alarm and notification controllers are available only when !isSingleVolume; media always). */
    static int[] visibleRows(boolean singleVolume) {
        return singleVolume ? new int[] { ROW_MEDIA } : new int[] { ROW_MEDIA, ROW_CALL, ROW_ALARM, ROW_NOTIFICATION };
    }

    static boolean isNotificationOrRing(int stream) { return stream == STREAM_NOTIFICATION || stream == STREAM_RING; }

    // ------------------------------------------------------------------ slider <-> stream volume
    // The stock slider is a SeekBar over [min .. max] (SeekBarPreference.setMin / setMax with AudioManager.getStreamMinVolume / getStreamMaxVolume). Ours runs over [0 .. max - min] with the
    // same thumb positions (the thumb sits at (volume - min) / (max - min) of the track): checked on Tj's screenshot, where Alarm (min 1, max 15) shows volume 12 at 11/14 of the track
    // (thumb centre 460 of a track from 77 to 566) and Notification (0 .. 15) shows volume 6 at 6/15.

    /** The SeekBar's max: the number of steps between the lowest and the highest volume (never negative, 0 = a fixed volume). */
    static int sliderMax(int min, int max) { return Math.max(0, max - min); }

    /** The slider position of a stream volume (clamped into the stream's range). */
    static int toProgress(int volume, int min, int max) { return clamp(volume, min, max) - min; }

    /** The stream volume of a slider position (position 0 = the lowest volume; a position outside the track is clamped). */
    static int toVolume(int progress, int min, int max) { return clamp(progress + min, min, max); }

    /**
     * What the slider shows, in the order of SeekBarVolumizer.updateSeekBar [mem]: a stream muted by Do Not Disturb shows its last audible volume (the slider is disabled); the Notification row in
     * vibrate mode shows the lowest position; a muted stream shows the lowest position; otherwise the real volume.
     */
    static int shownVolume(int row, int volume, boolean muted, boolean zenMuted, int ringerMode, int min, int max) {
        if (zenMuted) return clamp(volume, min, max);
        if (row == ROW_NOTIFICATION && ringerMode == RINGER_VIBRATE) return clamp(min, min, max);
        if (muted) return clamp(min, min, max);
        return clamp(volume, min, max);
    }

    private static int clamp(int v, int lo, int hi) {
        int top = Math.max(lo, hi);
        return v < lo ? lo : (v > top ? top : v);
    }

    // ------------------------------------------------------------------ icons
    static final int RINGER_SILENT = 0, RINGER_VIBRATE = 1, RINGER_NORMAL = 2;   // AudioManager.RINGER_MODE_*
    static final int ICON_NORMAL = 0, ICON_MUTED = 1, ICON_RINGER_VIBRATE = 2, ICON_RINGER_SILENT = 3;

    /**
     * Which glyph a row shows (VolumeSeekBarPreference.updateIconView: the controller's showIcon override first, then the mute icon when muted and not Do-Not-Disturb-muted, then the row's own icon).
     * The Notification row inherits RingVolumePreferenceController, which calls showIcon in EVERY ringer mode (bell, bell-off, vibrate): its "muted at volume 0" branch is never reached, only the ringer
     * mode changes the glyph. The other rows use the mute icon (Media: the crossed note; Alarm: the crossed clock; Call: the same phone).
     */
    static int iconKind(int row, int ringerMode, boolean muted, boolean zenMuted) {
        if (row == ROW_NOTIFICATION) {
            if (ringerMode == RINGER_VIBRATE) return ICON_RINGER_VIBRATE;
            if (ringerMode == RINGER_SILENT) return ICON_RINGER_SILENT;
            return ICON_NORMAL;
        }
        return muted && !zenMuted ? ICON_MUTED : ICON_NORMAL;
    }

    // ------------------------------------------------------------------ Do Not Disturb
    static final int FILTER_ALL = 1, FILTER_PRIORITY = 2, FILTER_NONE = 3, FILTER_ALARMS = 4;   // NotificationManager.INTERRUPTION_FILTER_*

    /**
     * Whether Do Not Disturb silences this stream, so its slider is disabled (SeekBarVolumizer.isZenMuted [mem]; the interruption filter is the public twin of Settings.Global.zen_mode: NONE = total
     * silence = ZEN_MODE_NO_INTERRUPTIONS mutes everything, ALARMS = alarms only mutes notification and ring, PRIORITY mutes the streams whose category the policy does not allow).
     */
    static boolean zenMuted(int filter, int stream, boolean allowAlarms, boolean allowMedia, boolean allowRinger) {
        if (filter == FILTER_NONE) return true;
        if (filter == FILTER_ALARMS) return isNotificationOrRing(stream);
        if (filter == FILTER_PRIORITY) {
            return (!allowAlarms && stream == STREAM_ALARM) || (!allowMedia && stream == STREAM_MUSIC) || (!allowRinger && isNotificationOrRing(stream));
        }
        return false;
    }

    // ------------------------------------------------------------------ ringtone rows
    /** The first summary of a ringtone row: AOSP's summary_placeholder (a no-break space) keeps the two-line height until the title is known. */
    static final String PLACEHOLDER = " ";
    static final String NONE = "None";          // framework ringtone_silent (the first entry of the picker)
    static final String UNKNOWN = "Unknown";    // framework ringtone_unknown (what Ringtone.getTitle answers for a sound it cannot name)

    /**
     * The summary of "Default notification sound" / "Default alarm sound": the title of the current sound. AOSP 10 leaves the placeholder when the sound is silent (RingtonePreferenceControllerBase
     * sets no summary for a null Uri, so after choosing None the row shows nothing or the stale title); the framework's own wording for that choice is "None", which is what we show.
     */
    static String ringtoneSummary(boolean hasSound, String title) {
        if (!hasSound) return NONE;
        if (title == null || title.trim().isEmpty()) return UNKNOWN;
        return title;
    }

    static final int TYPE_NOTIFICATION = 2, TYPE_ALARM = 4;   // RingtoneManager.TYPE_*
    static final String TITLE_NOTIFICATION_SOUND = "Default notification sound", TITLE_ALARM_SOUND = "Default alarm sound";
    private static final int REQUEST_BASE = 200;               // SoundSettings.REQUEST_CODE

    /** The request code of a picker for a ringtone type; the type is part of it so the result needs no remembered state (the page may be recreated while the picker is open). */
    static int requestCode(int type) { return REQUEST_BASE + type; }

    /** The ringtone type of a request code, or -1 when the code is not one of ours. */
    static int typeForRequest(int requestCode) {
        int t = requestCode - REQUEST_BASE;
        return t == TYPE_NOTIFICATION || t == TYPE_ALARM ? t : -1;
    }

    // ------------------------------------------------------------------ "Other sounds" switches
    // SettingPref(type, key, setting, DEFAULT_ON): state = getInt(setting, 1) != 0 (a missing value means on); change = putInt(setting, checked ? 1 : 0).
    static final String KEY_LOCKSCREEN_SOUNDS = "lockscreen_sounds_enabled";   // Settings.System.LOCKSCREEN_SOUNDS_ENABLED (hidden constant)
    static final String KEY_CHARGING_SOUNDS = "charging_sounds_enabled";       // Settings.Secure.CHARGING_SOUNDS_ENABLED (hidden constant)
    static final String KEY_TOUCH_SOUNDS = "sound_effects_enabled";            // Settings.System.SOUND_EFFECTS_ENABLED
    static final int DEFAULT_ON = 1;

    static boolean isOn(int value) { return value != 0; }

    static int valueFor(boolean on) { return on ? 1 : 0; }

    // ------------------------------------------------------------------ what to watch
    static final String KEY_NOTIFICATION_SOUND = "notification_sound", KEY_ALARM_ALERT = "alarm_alert";   // Settings.System.NOTIFICATION_SOUND / ALARM_ALERT
    static final int CHANGE_VOLUME = 1, CHANGE_RINGTONE = 2, CHANGE_SWITCH = 4;

    /**
     * What a change of a system setting means for the page (a bit mask, 0 = nothing the page shows). AudioService stores every volume per output device ("volume_music_speaker",
     * "volume_alarm_headset" ...), so any name that starts with "volume_" is a volume. {@code null}: the system did not say which setting changed, so everything is read again.
     */
    static int changeKind(String settingName) {
        if (settingName == null) return CHANGE_VOLUME | CHANGE_RINGTONE | CHANGE_SWITCH;
        if (settingName.startsWith("volume_")) return CHANGE_VOLUME;
        if (settingName.equals(KEY_NOTIFICATION_SOUND) || settingName.equals(KEY_ALARM_ALERT)) return CHANGE_RINGTONE;
        if (settingName.equals(KEY_LOCKSCREEN_SOUNDS) || settingName.equals(KEY_CHARGING_SOUNDS) || settingName.equals(KEY_TOUCH_SOUNDS)) return CHANGE_SWITCH;
        return 0;
    }

    /**
     * The broadcasts that tell the sliders to read the system again: what SeekBarVolumizer's receiver [mem] and AdjustVolumeRestrictedPreferenceController.getIntentFilter listen to (volume, mute,
     * master mute, ringer mode, the output device of a stream) plus the Do Not Disturb changes that disable a slider. Several are hidden AudioManager constants: the literal action strings.
     */
    static final String[] WATCHED_ACTIONS = {
        "android.media.VOLUME_CHANGED_ACTION",
        "android.media.STREAM_MUTE_CHANGED_ACTION",
        "android.media.MASTER_MUTE_CHANGED_ACTION",
        "android.media.RINGER_MODE_CHANGED",
        "android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION",
        "android.media.STREAM_DEVICES_CHANGED_ACTION",
        "android.app.action.INTERRUPTION_FILTER_CHANGED",
        "android.app.action.NOTIFICATION_POLICY_CHANGED",
    };

    // ------------------------------------------------------------------ release sample
    static final int SAMPLE_BUNDLED = 0, SAMPLE_RINGTONE = 1, SAMPLE_NOTIFICATION = 2, SAMPLE_ALARM = 3;
    /** SoundSettings.SAMPLE_CUTOFF: "manually cap sample playback at 2 seconds". */
    static final int SAMPLE_CUTOFF_MS = 2000;

    /**
     * Which sound a slider plays when the finger lifts: Media plays the short sample that Settings ships (media_volume.ogg); the others play the default sound of their kind
     * (VolumeSeekBarPreference passes a sample Uri only for STREAM_MUSIC; SeekBarVolumizer's constructor [mem] picks ring -> ringtone, notification -> notification sound, anything else -> alarm sound).
     */
    static int sampleSource(int stream) {
        if (stream == STREAM_MUSIC) return SAMPLE_BUNDLED;
        if (stream == STREAM_RING) return SAMPLE_RINGTONE;
        if (stream == STREAM_NOTIFICATION) return SAMPLE_NOTIFICATION;
        return SAMPLE_ALARM;
    }
}
