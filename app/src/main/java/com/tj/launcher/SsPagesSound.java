package com.tj.launcher;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.android.launcher3.R;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * BN1: the Sound page, built from Tj's screenshot of the stock page (Android 10 Settings, SoundSettings / sound_settings.xml) minus what he removed (Do Not Disturb, "Shortcut to prevent ringing",
 * the words "and vibrations" of the last heading, "Sound enhancement"): four volume sliders (Media, Call, Alarm, Notification), a divider, "Default notification sound" and "Default alarm sound", a
 * divider, the heading OTHER SOUNDS and the switches Screen locking sounds, Charging sounds and vibration, Touch sounds. No search anywhere. Every control changes the real system value:
 * a slider calls AudioManager.setStreamVolume (a worker thread, like SeekBarVolumizer) and plays the stock release sample; a sound row opens the system picker through {@link SsRingtoneFragment};
 * a switch writes the same Settings key as AOSP's SettingPref. The page follows changes made elsewhere (hardware keys, other apps, adb) while it is visible: a receiver for the volume / mute /
 * ringer / Do Not Disturb broadcasts plus one observer on the system settings, both removed in stop(). Rules (pure Java, JVM-tested): {@link SsSoundRules}.
 * The row layout is AOSP's preference_volume_slider.xml in code: row padding 16 dp, icon frame min 44 dp (icon 24 dp, end padding 12 dp, 4 dp above and below), a column with 8 dp above and below holding
 * the title (16 sp, 12 dp start padding) and a 48 dp SeekBar (12 dp start padding), which makes a 97 px row at 170 dpi, as measured on Tj's screenshot.
 */
final class SsPagesSound {
    private SsPagesSound() {}

    static void sound(final Activity a, LinearLayout list, List<SsPages.Live> live) {
        Page page = new Page(a);
        page.build(list);
        live.add(page);
        SsRingtoneFragment.attach(a);   // receives the answer of the sound picker (the activity itself has no onActivityResult)
    }

    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT, MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int FLAG_SHOW_UI_WARNINGS = 1 << 10;   // AudioManager.FLAG_SHOW_UI_WARNINGS (hidden): what SeekBarVolumizer passes to setStreamVolume
    private static final int MSG_START_SAMPLE = 1, MSG_STOP_SAMPLE = 2, MSG_SYNC = 3, MSG_SET_VOLUME = 100;   // audio thread (MSG_SET_VOLUME + row index)
    private static final int MSG_TITLES = 1;                                                                   // io thread
    private static final long TOAST_GAP_MS = 3000;

    /** What a refresh reads once for all rows: the facts that are not per stream. */
    private static final class Facts {
        boolean scoOn;
        int ringerMode = SsSoundRules.RINGER_NORMAL;
        int filter = SsSoundRules.FILTER_ALL;
        boolean allowAlarms = true, allowMedia = true, allowRinger = true;
    }

    private static final class Page implements SsPages.Live {
        private final Activity a;
        private final AudioManager am;
        private final NotificationManager nm;
        private final Handler main = new Handler(Looper.getMainLooper());
        private final List<Row> rows = new ArrayList<>();
        private View ringNotification, ringAlarm;
        private Switch swLock, swCharging, swTouch;
        private volatile boolean started;
        private volatile Audio audio;
        private HandlerThread ioThread;
        private volatile Handler io;
        private long lastToast;   // main thread only

        Page(Activity a) {
            this.a = a;
            this.am = (AudioManager) a.getSystemService(Context.AUDIO_SERVICE);
            this.nm = (NotificationManager) a.getSystemService(Context.NOTIFICATION_SERVICE);
        }

        // ------------------------------------------------------------------ building
        void build(LinearLayout list) {
            for (int r : SsSoundRules.visibleRows(singleVolume())) {
                Row row = new Row(r);
                rows.add(row);
                list.addView(row.view);
            }
            list.addView(SsRows.divider(a));   // AOSP: allowDividerAbove on the Do Not Disturb row; Tj removed that row, the line between the volumes and the sounds stays
            ringNotification = SsRows.row(a, null, SsSoundRules.TITLE_NOTIFICATION_SOUND, SsSoundRules.PLACEHOLDER,
                    v -> pick(SsSoundRules.TYPE_NOTIFICATION, SsSoundRules.TITLE_NOTIFICATION_SOUND));
            ringAlarm = SsRows.row(a, null, SsSoundRules.TITLE_ALARM_SOUND, SsSoundRules.PLACEHOLDER,
                    v -> pick(SsSoundRules.TYPE_ALARM, SsSoundRules.TITLE_ALARM_SOUND));
            list.addView(ringNotification);
            list.addView(ringAlarm);
            list.addView(SsRows.divider(a));
            list.addView(SsRows.heading(a, "Other sounds"));   // the stock text is "Other sounds and vibrations"; Tj: without "and vibrations"
            swLock = new Switch(a);
            swCharging = new Switch(a);
            swTouch = new Switch(a);
            list.addView(SsRows.switchRow(a, "Screen locking sounds", null, swLock, on -> setSwitch(false, SsSoundRules.KEY_LOCKSCREEN_SOUNDS, on)));
            list.addView(SsRows.switchRow(a, "Charging sounds and vibration", null, swCharging, on -> setSwitch(true, SsSoundRules.KEY_CHARGING_SOUNDS, on)));
            list.addView(SsRows.switchRow(a, "Touch sounds", null, swTouch, on -> {
                if (setSwitch(false, SsSoundRules.KEY_TOUCH_SOUNDS, on)) loadEffects(on);
            }));
            refreshNow();   // the first frame already shows the real values
        }

        /** AudioSystem.isSingleVolume (hidden): a television, or config_single_volume. Only Media is shown then. */
        private boolean singleVolume() {
            try {
                if (a.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK)) return true;
                Resources sys = Resources.getSystem();
                int id = sys.getIdentifier("config_single_volume", "bool", "android");
                return id != 0 && sys.getBoolean(id);
            } catch (Throwable t) {
                return false;
            }
        }

        // ------------------------------------------------------------------ lifecycle (SsPages.Live)
        @Override public void start() {
            if (started) return;
            started = true;
            audio = new Audio();
            ioThread = new HandlerThread("ss-sound-io");
            ioThread.start();
            io = new Handler(ioThread.getLooper(), m -> {
                if (m.what == MSG_TITLES) {
                    loadTitle(SsSoundRules.TYPE_NOTIFICATION, ringNotification);
                    loadTitle(SsSoundRules.TYPE_ALARM, ringAlarm);
                    return true;
                }
                return false;
            });
            IntentFilter filter = new IntentFilter();
            for (String action : SsSoundRules.WATCHED_ACTIONS) filter.addAction(action);
            try { a.registerReceiver(receiver, filter); } catch (Throwable ignored) { }
            ContentResolver cr = a.getContentResolver();
            try { cr.registerContentObserver(Settings.System.CONTENT_URI, true, observer); } catch (Throwable ignored) { }   // every volume is stored per output device: watch the table, filter by name
            try { cr.registerContentObserver(Settings.Secure.getUriFor(SsSoundRules.KEY_CHARGING_SOUNDS), false, observer); } catch (Throwable ignored) { }
            refreshNow();
            requestTitles();
        }

        @Override public void stop() {
            if (!started) return;
            started = false;
            main.removeCallbacks(refresh);
            try { a.unregisterReceiver(receiver); } catch (Throwable ignored) { }
            try { a.getContentResolver().unregisterContentObserver(observer); } catch (Throwable ignored) { }
            for (Row r : rows) r.tracking = false;
            Audio au = audio;
            audio = null;
            if (au != null) au.shutdown();   // stops a playing sample (SoundSettings.onPause), finishes the writes already queued
            HandlerThread t = ioThread;
            ioThread = null;
            io = null;
            if (t != null) t.quitSafely();
        }

        // ------------------------------------------------------------------ following the system
        private final BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) { refreshSoon(); }
        };

        private final ContentObserver observer = new ContentObserver(main) {
            @Override public void onChange(boolean selfChange) { changed(null); }
            @Override public void onChange(boolean selfChange, Uri uri) { changed(uri == null ? null : uri.getLastPathSegment()); }
        };

        private void changed(String settingName) {
            int kind = SsSoundRules.changeKind(settingName);
            if ((kind & (SsSoundRules.CHANGE_VOLUME | SsSoundRules.CHANGE_SWITCH)) != 0) refreshSoon();
            if ((kind & SsSoundRules.CHANGE_RINGTONE) != 0) requestTitles();
        }

        private final Runnable refresh = new Runnable() {
            @Override public void run() { if (started) refreshNow(); }
        };

        /** Coalesces a burst of broadcasts / setting changes (a drag fires many) into one read. */
        private void refreshSoon() {
            main.removeCallbacks(refresh);
            main.post(refresh);
        }

        private void refreshNow() {
            Facts f = facts();
            for (Row r : rows) r.sync(f);
            syncSwitches();
        }

        private Facts facts() {
            Facts f = new Facts();
            try { if (am != null) f.scoOn = am.isBluetoothScoOn(); } catch (Throwable ignored) { }
            try { if (am != null) f.ringerMode = am.getRingerMode(); } catch (Throwable ignored) { }
            try {
                if (nm != null) {
                    f.filter = nm.getCurrentInterruptionFilter();
                    NotificationManager.Policy p = nm.getNotificationPolicy();
                    if (p != null) {
                        int c = p.priorityCategories;
                        f.allowAlarms = (c & NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS) != 0;
                        f.allowMedia = (c & NotificationManager.Policy.PRIORITY_CATEGORY_MEDIA) != 0;
                        f.allowRinger = (c & (NotificationManager.Policy.PRIORITY_CATEGORY_REMINDERS | NotificationManager.Policy.PRIORITY_CATEGORY_EVENTS
                                | NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES | NotificationManager.Policy.PRIORITY_CATEGORY_CALLS
                                | NotificationManager.Policy.PRIORITY_CATEGORY_REPEAT_CALLERS)) != 0;   // ZenModeConfig.areAllPriorityOnlyNotificationZenSoundsMuted, negated [mem]
                    }
                }
            } catch (Throwable ignored) { }
            return f;
        }

        // ------------------------------------------------------------------ switches
        private void syncSwitches() {
            ContentResolver cr = a.getContentResolver();
            try { show(swLock, Settings.System.getInt(cr, SsSoundRules.KEY_LOCKSCREEN_SOUNDS, SsSoundRules.DEFAULT_ON)); } catch (Throwable ignored) { }
            try { show(swCharging, Settings.Secure.getInt(cr, SsSoundRules.KEY_CHARGING_SOUNDS, SsSoundRules.DEFAULT_ON)); } catch (Throwable ignored) { }
            try { show(swTouch, Settings.System.getInt(cr, SsSoundRules.KEY_TOUCH_SOUNDS, SsSoundRules.DEFAULT_ON)); } catch (Throwable ignored) { }
        }

        private void show(Switch s, int value) {
            boolean on = SsSoundRules.isOn(value);
            if (s != null && s.isChecked() != on) s.setChecked(on);
        }

        /** SettingPref.setSetting: putInt(key, checked ? 1 : 0) in Settings.System, or Settings.Secure for charging sounds. On a refusal the user is told and the switch shows the real value again. */
        private boolean setSwitch(boolean secure, String key, boolean on) {
            final ContentResolver cr = a.getContentResolver();
            final int value = SsSoundRules.valueFor(on);
            boolean ok = false;
            try {
                ok = secure ? Settings.Secure.putInt(cr, key, value) : Settings.System.putInt(cr, key, value);
            } catch (RuntimeException ignored) { }   // SecurityException without WRITE_SETTINGS / WRITE_SECURE_SETTINGS
            if (!ok) {
                toast("This setting can't be changed from here.");
                syncSwitches();
            }
            return ok;
        }

        /**
         * TouchSoundPreferenceController: after the setting, the sound effects are loaded (on) or unloaded (off) off the main thread. Both calls are hidden AudioManager methods, so by reflection and best
         * effort: the setting alone already decides whether a touch plays a sound.
         */
        private void loadEffects(final boolean on) {
            Handler h = io;
            if (h == null) return;
            h.post(() -> {
                try { AudioManager.class.getMethod(on ? "loadSoundEffects" : "unloadSoundEffects").invoke(am); } catch (Throwable ignored) { }
            });
        }

        // ------------------------------------------------------------------ sound rows
        private void pick(int type, String title) {
            if (!SsRingtoneFragment.pick(a, type, title)) toast("Can't open the ringtone picker.");
        }

        private void requestTitles() {
            Handler h = io;
            if (h == null) return;
            h.removeMessages(MSG_TITLES);
            h.sendEmptyMessage(MSG_TITLES);
        }

        /** RingtonePreferenceControllerBase.updateSummary on a worker thread: the title of the current default sound (a MediaStore lookup), posted to the row. */
        private void loadTitle(int type, final View row) {
            String text = null;
            try {
                Uri uri = RingtoneManager.getActualDefaultRingtoneUri(a, type);
                String title = null;
                if (uri != null) {
                    Ringtone r = RingtoneManager.getRingtone(a, uri);
                    if (r != null) {
                        try { title = r.getTitle(a); } finally { r.stop(); }   // getRingtone prepares a player: release it at once
                    }
                }
                text = SsSoundRules.ringtoneSummary(uri != null, title);
            } catch (Throwable ignored) { }
            if (text == null) return;
            final String shown = text;
            main.post(() -> { if (started) SsRows.setSummary(row, shown); });
        }

        // ------------------------------------------------------------------ small helpers
        private void toast(String text) {
            Toast.makeText(a, text, Toast.LENGTH_SHORT).show();
        }

        /** At most one toast every few seconds: a refused drag would otherwise fire it on every step. */
        private void toastRateLimited(String text) {
            long now = SystemClock.elapsedRealtime();
            if (now - lastToast < TOAST_GAP_MS) return;
            lastToast = now;
            toast(text);
        }

        private int dp(float v) { return SsRows.dp(a, v); }

        // ------------------------------------------------------------------ one volume row
        private final class Row implements SeekBar.OnSeekBarChangeListener {
            final int index;
            final LinearLayout view;
            final ImageView icon;
            final SeekBar bar;
            int stream = -1, min = 0, max = 0;
            int iconKind = -1;
            boolean tracking;

            Row(int index) {
                this.index = index;
                String title = SsSoundRules.TITLES[index];
                view = new LinearLayout(a);
                view.setOrientation(LinearLayout.HORIZONTAL);
                view.setGravity(Gravity.CENTER_VERTICAL);
                view.setPadding(dp(16), 0, dp(16), 0);   // listPreferredItemPaddingStart / End
                view.setMinimumHeight(dp(48));           // listPreferredItemHeightSmall; the row itself is not clickable (SeekBarPreference: selectable = false)

                LinearLayout frame = new LinearLayout(a);   // icon_frame
                frame.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
                frame.setMinimumWidth(dp(44));
                frame.setPadding(0, dp(4), dp(12), dp(4));
                icon = new ImageView(a);
                frame.addView(icon, new LinearLayout.LayoutParams(dp(24), dp(24)));
                view.addView(frame, new LinearLayout.LayoutParams(WRAP, WRAP));

                LinearLayout body = new LinearLayout(a);
                body.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(0, WRAP, 1f);
                blp.topMargin = dp(8);
                blp.bottomMargin = dp(8);
                view.addView(body, blp);

                TextView t = new TextView(a);
                SsRows.appearance(t, android.R.attr.textAppearanceListItem);   // TextAppearance.DeviceDefault.Subhead: 16 sp, textColorPrimary
                t.setSingleLine(true);
                t.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                t.setHorizontalFadingEdgeEnabled(true);
                t.setPaddingRelative(dp(12), 0, dp(12), 0);   // the end padding is the (empty) widget_frame of the stock layout
                t.setText(title);
                body.addView(t, new LinearLayout.LayoutParams(MATCH, WRAP));

                bar = new SeekBar(a);   // the theme's seekBarStyle: Material track (2 dp), teal thumb, same as the stock row
                bar.setPaddingRelative(dp(12), bar.getPaddingTop(), bar.getPaddingEnd(), bar.getPaddingBottom());
                bar.setContentDescription(title);   // SeekBarPreference: the row title
                bar.setOnSeekBarChangeListener(this);
                LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(MATCH, dp(48));
                slp.gravity = Gravity.CENTER_VERTICAL;
                body.addView(bar, slp);
            }

            /** Reads the stream and shows it: range, position, glyph, enabled. Not the position while the finger is on the slider. */
            void sync(Facts f) {
                int s = SsSoundRules.streamFor(index, f.scoOn);
                int[] st = read(s);
                if (st == null && index == SsSoundRules.ROW_CALL && s != SsSoundRules.STREAM_VOICE_CALL) {   // AudioHelper.getMinVolume: fall back to the voice call stream
                    s = SsSoundRules.STREAM_VOICE_CALL;
                    st = read(s);
                }
                stream = s;
                if (st == null) {   // no audio service: a slider that cannot move
                    min = 0;
                    max = 0;
                    bar.setEnabled(false);
                    bar.setMax(0);
                    bar.setProgress(0);
                    setIcon(SsSoundRules.ICON_NORMAL);
                    return;
                }
                min = st[0];
                max = Math.max(st[0], st[1]);
                boolean muted = st[3] != 0;
                boolean zen = SsSoundRules.zenMuted(f.filter, s, f.allowAlarms, f.allowMedia, f.allowRinger);
                setIcon(SsSoundRules.iconKind(index, f.ringerMode, muted, zen));
                bar.setEnabled(!zen);
                if (tracking) return;
                int range = SsSoundRules.sliderMax(min, max);
                if (bar.getMax() != range) bar.setMax(range);
                int p = SsSoundRules.toProgress(SsSoundRules.shownVolume(index, st[2], muted, zen, f.ringerMode, min, max), min, max);
                if (bar.getProgress() != p) {
                    if (bar.isAttachedToWindow()) bar.setProgress(p, true); else bar.setProgress(p);   // the stock slider animates to a volume changed elsewhere
                }
            }

            private int[] read(int s) {
                if (am == null) return null;
                try { return new int[] { am.getStreamMinVolume(s), am.getStreamMaxVolume(s), am.getStreamVolume(s), am.isStreamMute(s) ? 1 : 0 }; }
                catch (Throwable t) { return null; }
            }

            private void setIcon(int kind) {
                if (kind == iconKind) return;
                iconKind = kind;
                Drawable d;
                switch (index) {
                    case SsSoundRules.ROW_MEDIA:
                        d = a.getDrawable(kind == SsSoundRules.ICON_MUTED ? R.drawable.ss_ic_media_stream_off : R.drawable.ss_ic_media_stream);
                        break;
                    case SsSoundRules.ROW_CALL:
                        d = a.getDrawable(R.drawable.ss_ic_local_phone);   // "the call volume can never be fully muted": the muted glyph is the same phone
                        break;
                    case SsSoundRules.ROW_ALARM:
                        d = kind == SsSoundRules.ICON_MUTED ? alarmMutedIcon() : a.getDrawable(R.drawable.ss_ic_audio_alarm);
                        break;
                    default:
                        d = a.getDrawable(kind == SsSoundRules.ICON_RINGER_VIBRATE ? R.drawable.ss_ic_volume_ringer_vibrate
                                : kind == SsSoundRules.ICON_RINGER_SILENT ? R.drawable.ss_ic_notifications_off : R.drawable.ss_ic_notifications);
                }
                icon.setImageDrawable(d);
            }

            /** AlarmVolumePreferenceController.getMuteIcon is the framework's ic_audio_alarm_mute (not in the research checkout): use it when this build has it, else the Material "alarm off" glyph. */
            private Drawable alarmMutedIcon() {
                try {
                    int id = Resources.getSystem().getIdentifier("ic_audio_alarm_mute", "drawable", "android");
                    if (id != 0) {
                        Drawable d = a.getDrawable(id);
                        if (d != null) return d;
                    }
                } catch (Throwable ignored) { }
                return a.getDrawable(R.drawable.ss_ic_audio_alarm_off);
            }

            @Override public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                if (!fromUser) return;   // our own setProgress, or the system moving the slider
                postVolume(this, SsSoundRules.toVolume(progress, min, max));
            }

            @Override public void onStartTrackingTouch(SeekBar s) { tracking = true; }

            /** The finger lifts: play the stock sample (after the last write, same queue), then read the real volume back (SeekBarVolumizer.onStopTrackingTouch -> postStartSample). */
            @Override public void onStopTrackingTouch(SeekBar s) {
                tracking = false;
                Audio au = audio;
                if (au == null) return;
                au.handler.sendMessage(au.handler.obtainMessage(MSG_START_SAMPLE, index, stream));
                au.handler.sendEmptyMessage(MSG_SYNC);
            }
        }

        /** The latest value of a slider goes to the audio thread; an older one that has not been written yet is dropped. */
        private void postVolume(Row r, int volume) {
            Audio au = audio;
            if (au == null) return;
            au.handler.removeMessages(MSG_SET_VOLUME + r.index);
            au.handler.sendMessage(au.handler.obtainMessage(MSG_SET_VOLUME + r.index, r.stream, volume));
        }

        // ------------------------------------------------------------------ the audio worker: volume writes and release samples
        /** One per start(): a thread for AudioManager.setStreamVolume and the release sample, so a slow audio service never stalls the page; stopped with the page. */
        private final class Audio {
            final HandlerThread thread = new HandlerThread("ss-sound-audio");
            final Handler handler;
            private Ringtone sample;      // audio thread only
            private int sampleRow = -1;   // audio thread only

            Audio() {
                thread.start();
                handler = new Handler(thread.getLooper(), this::handle);
            }

            private boolean handle(Message m) {
                if (m.what >= MSG_SET_VOLUME && m.what < MSG_SET_VOLUME + SsSoundRules.TITLES.length) {
                    writeVolume(m.arg1, m.arg2);
                    return true;
                }
                switch (m.what) {
                    case MSG_START_SAMPLE: startSample(m.arg1, m.arg2); return true;
                    case MSG_STOP_SAMPLE: stopSample(); return true;
                    case MSG_SYNC: main.post(refresh); return true;
                    default: return false;
                }
            }

            void shutdown() {
                handler.post(this::stopSample);   // due at once, so quitSafely runs it; the delayed cutoff message is dropped with the queue
                thread.quitSafely();
            }

            private void writeVolume(int stream, int volume) {
                try {
                    am.setStreamVolume(stream, volume, FLAG_SHOW_UI_WARNINGS);
                    if (sample != null) armCutoff();   // SoundSettings.VolumePreferenceCallback.onStreamValueChanged: a change while a sample plays restarts the 2 s timer
                } catch (SecurityException e) {
                    // "Not allowed to change Do Not Disturb state": lowering a ring-type stream to 0 (or raising it from silent) flips the ringer mode, which a non-system app may do only with
                    // Do Not Disturb access. The slider snaps back to the real value.
                    main.post(() -> toastRateLimited("This setting can't be changed from here."));
                    main.post(refresh);
                } catch (Throwable t) {
                    main.post(refresh);
                }
            }

            private void armCutoff() {
                handler.removeMessages(MSG_STOP_SAMPLE);
                handler.sendEmptyMessageDelayed(MSG_STOP_SAMPLE, SsSoundRules.SAMPLE_CUTOFF_MS);
            }

            private void startSample(int row, int stream) {
                if (sample != null && sampleRow == row) {   // SeekBarVolumizer.onStartSample plays only when the sample is not already playing
                    boolean playing = false;
                    try { playing = sample.isPlaying(); } catch (Throwable ignored) { }
                    if (playing) {
                        armCutoff();
                        return;
                    }
                }
                stopSample();
                Ringtone r = null;
                try {
                    r = RingtoneManager.getRingtone(a, sampleUri(stream));
                    if (r == null) return;
                    r.setAudioAttributes(attributesFor(stream));
                    r.play();
                    sample = r;
                    sampleRow = row;
                    armCutoff();
                } catch (Throwable t) {
                    if (r != null) { try { r.stop(); } catch (Throwable ignored) { } }
                }
            }

            private void stopSample() {
                handler.removeMessages(MSG_STOP_SAMPLE);
                Ringtone r = sample;
                sample = null;
                sampleRow = -1;
                if (r != null) { try { r.stop(); } catch (Throwable ignored) { } }
            }

            /** Media: Settings' own media_volume.ogg (shipped as res/raw/ss_media_volume, 0.49 s mono Vorbis); the others: the default sound of their kind. */
            private Uri sampleUri(int stream) {
                switch (SsSoundRules.sampleSource(stream)) {
                    case SsSoundRules.SAMPLE_BUNDLED: return Uri.parse("android.resource://" + a.getPackageName() + "/" + R.raw.ss_media_volume);
                    case SsSoundRules.SAMPLE_RINGTONE: return Settings.System.DEFAULT_RINGTONE_URI;
                    case SsSoundRules.SAMPLE_NOTIFICATION: return Settings.System.DEFAULT_NOTIFICATION_URI;
                    default: return Settings.System.DEFAULT_ALARM_ALERT_URI;
                }
            }

            /** The playback attributes that make the sample follow the slider's stream volume (no bypass flags: an app that is not privileged may not request them). */
            private AudioAttributes attributesFor(int stream) {
                int usage, content;
                switch (stream) {
                    case SsSoundRules.STREAM_MUSIC:
                        usage = AudioAttributes.USAGE_MEDIA;
                        content = AudioAttributes.CONTENT_TYPE_MUSIC;
                        break;
                    case SsSoundRules.STREAM_VOICE_CALL:
                    case SsSoundRules.STREAM_BLUETOOTH_SCO:
                        usage = AudioAttributes.USAGE_VOICE_COMMUNICATION;
                        content = AudioAttributes.CONTENT_TYPE_SPEECH;
                        break;
                    case SsSoundRules.STREAM_ALARM:
                        usage = AudioAttributes.USAGE_ALARM;
                        content = AudioAttributes.CONTENT_TYPE_SONIFICATION;
                        break;
                    default:
                        usage = AudioAttributes.USAGE_NOTIFICATION;
                        content = AudioAttributes.CONTENT_TYPE_SONIFICATION;
                }
                return new AudioAttributes.Builder().setUsage(usage).setContentType(content).build();
            }
        }
    }
}
