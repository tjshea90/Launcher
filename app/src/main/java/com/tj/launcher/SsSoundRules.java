package com.tj.launcher;

/**
 * BN1: the pure rules of the Sound page (JVM-tested: SsSoundRulesTest). Plain Java, no Android classes. Sources: AOSP 10 (android-10.0.0_r47) Settings {@code notification/*VolumePreferenceController},
 * {@code VolumeSeekBarPreference}, {@code SoundSettings}, {@code RingtonePreferenceControllerBase}, {@code SettingPref}, and the platform's {@code android.preference.SeekBarVolumizer} (core/java), whose
 * code the slider rows follow line by line; research/aosp10-settings/B_sound_storage.md sec. 1. The platform integers mirrored here (stream types, ringer modes, interruption filters, policy categories,
 * ringtone types, mute adjustments) are API constants that have not changed since they were introduced, so the tests can pin them.
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

    // ------------------------------------------------------------------ slider position
    // The stock slider is a SeekBar over the ABSOLUTE volumes [min .. max]: Settings' SeekBarPreference.onBindViewHolder calls setMax and setMin (AudioManager.getStreamMaxVolume / getStreamMinVolume),
    // SeekBarVolumizer.setSeekBar sets the same max again and shows the stream volume itself as the progress. The thumb sits at (volume - min) / (max - min) of the track. Checked on Tj's screenshot:
    // Alarm (min 1, max 15) shows volume 12 at 11/14 of the track (thumb centre x = 460 on a track whose thumb centre runs from 77 to 566) and Notification (0 .. 15) shows volume 6 at 6/15 (x = 272).

    /** A volume clamped into the stream's range (what ProgressBar does with any progress it is given, so 0 is the lowest position of a stream whose minimum is 1). */
    static int clampVolume(int volume, int min, int max) {
        int top = Math.max(min, max);
        return volume < min ? min : (volume > top ? top : volume);
    }

    /**
     * What the slider shows: SeekBarVolumizer.updateSeekBar, in its order. A stream muted by Do Not Disturb shows its LAST AUDIBLE volume (the slider is disabled); the Notification and Ring streams in vibrate
     * mode show 0; a muted stream shows 0; otherwise the volume. The answer is clamped into [min .. max] like the progress bar does.
     */
    static int shownVolume(int stream, int volume, int lastAudible, boolean muted, boolean zenMuted, int ringerMode, int min, int max) {
        if (zenMuted) return clampVolume(lastAudible, min, max);
        if (isNotificationOrRing(stream) && ringerMode == RINGER_VIBRATE) return clampVolume(0, min, max);
        if (muted) return clampVolume(0, min, max);
        return clampVolume(volume, min, max);
    }

    // ------------------------------------------------------------------ writing a volume
    static final int ADJUST_MUTE = -100, ADJUST_SAME = 0, ADJUST_UNMUTE = 100;   // AudioManager.ADJUST_*

    /**
     * What to tell the audio service around the slider's new volume (SeekBarVolumizer.handleMessage, MSG_SET_STREAM_VOLUME): a MUTED stream dragged above 0 is unmuted first; an UNMUTED stream dragged to 0
     * is muted (that is what makes the Media glyph turn into the crossed note and gives the stream a "last audible" volume). Streams whose lowest volume is 1 (Call, Alarm) never reach 0, so never get muted here.
     */
    static int muteAdjustment(boolean muted, int progress) {
        if (muted && progress > 0) return ADJUST_UNMUTE;
        if (!muted && progress == 0) return ADJUST_MUTE;
        return ADJUST_SAME;
    }

    // ------------------------------------------------------------------ icons
    static final int RINGER_SILENT = 0, RINGER_VIBRATE = 1, RINGER_NORMAL = 2;   // AudioManager.RINGER_MODE_*
    static final int ICON_NORMAL = 0, ICON_MUTED = 1, ICON_RINGER_VIBRATE = 2, ICON_RINGER_SILENT = 3;

    /**
     * Which glyph a row shows (VolumeSeekBarPreference.updateIconView: the controller's showIcon override first, then the mute icon when muted and not Do-Not-Disturb-muted, then the row's own icon).
     * The Notification row inherits RingVolumePreferenceController, which calls showIcon in EVERY ringer mode (bell, bell-off, vibrate): its "muted at volume 0" branch is never reached, only the ringer
     * mode changes the glyph. The other rows use the mute icon (Media: the crossed note; Alarm: the crossed clock; Call: the same phone). {@code muted} is AudioManager.isStreamMute, as at the first bind of
     * the stock page (a Media stream at volume 0 WITHOUT the mute flag keeps the plain note, as on Tj's screenshot).
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

    // NotificationManager.Policy.PRIORITY_CATEGORY_* (bit mask in Policy.priorityCategories)
    static final int CATEGORY_REMINDERS = 1, CATEGORY_EVENTS = 1 << 1, CATEGORY_MESSAGES = 1 << 2, CATEGORY_CALLS = 1 << 3, CATEGORY_REPEAT_CALLERS = 1 << 4, CATEGORY_ALARMS = 1 << 5, CATEGORY_MEDIA = 1 << 6;

    static boolean allowsAlarms(int priorityCategories) { return (priorityCategories & CATEGORY_ALARMS) != 0; }

    static boolean allowsMedia(int priorityCategories) { return (priorityCategories & CATEGORY_MEDIA) != 0; }

    /**
     * SeekBarVolumizer's mAllowRinger = !ZenModeConfig.areAllPriorityOnlyNotificationZenSoundsMuted(policy): the ring and notification streams stay audible in "priority only" when reminders, events, messages,
     * calls or repeat callers are allowed. (AOSP also counts "channels that bypass Do Not Disturb"; that bit of Policy.state is a hidden field an app cannot read, so it is left out.)
     */
    static boolean allowsRinger(int priorityCategories) {
        return (priorityCategories & (CATEGORY_REMINDERS | CATEGORY_EVENTS | CATEGORY_MESSAGES | CATEGORY_CALLS | CATEGORY_REPEAT_CALLERS)) != 0;
    }

    /**
     * Whether Do Not Disturb silences this stream, so its slider is disabled (SeekBarVolumizer.isZenMuted; the interruption filter is the public twin of Settings.Global.zen_mode: NONE = total silence =
     * ZEN_MODE_NO_INTERRUPTIONS mutes everything, ALARMS = alarms only mutes notification and ring, PRIORITY = important interruptions mutes the streams whose category the policy does not allow).
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
     * The summary of "Default notification sound" / "Default alarm sound": the title of the current sound. AOSP 10 leaves the summary alone when the sound is silent (RingtonePreferenceControllerBase.updateSummary
     * sets none for a null Uri, so a freshly opened page shows the placeholder and the page that has just changed to None keeps the old title until it is reopened); the framework's own word for that choice is
     * "None" (Ringtone.getTitle of a null Uri), which is what we show.
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
     * The broadcasts that tell the sliders to read the system again: the five SeekBarVolumizer's receiver listens to (volume, internal ringer mode, interruption filter, notification policy, the output device
     * of a stream) plus the stream mute, master mute and public ringer mode changes (a slider dragged to 0 mutes its stream). Several are hidden AudioManager constants: the literal action strings.
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
    /** SeekBarVolumizer.CHECK_RINGTONE_PLAYBACK_DELAY_MS: a release while the sample still plays asks again after this long. */
    static final int SAMPLE_RECHECK_MS = 1000;

    /** SeekBarVolumizer.postStartSample: start at once, or one second later when a sample is playing right now (and then only if it has stopped by then). */
    static int sampleStartDelayMs(boolean samplePlaying) { return samplePlaying ? SAMPLE_RECHECK_MS : 0; }

    /**
     * Which sound a slider plays when the finger lifts: Media plays the short sample that Settings ships (media_volume.ogg; VolumeSeekBarPreference passes a sample Uri only for STREAM_MUSIC); the others
     * play the default sound of their kind (SeekBarVolumizer's constructor: ring -> ringtone, notification -> notification sound, anything else, Call included -> alarm sound).
     */
    static int sampleSource(int stream) {
        if (stream == STREAM_MUSIC) return SAMPLE_BUNDLED;
        if (stream == STREAM_RING) return SAMPLE_RINGTONE;
        if (stream == STREAM_NOTIFICATION) return SAMPLE_NOTIFICATION;
        return SAMPLE_ALARM;
    }
}
