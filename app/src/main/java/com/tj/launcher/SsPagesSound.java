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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * BN1: the Sound page, built from Tj's screenshot of the stock page (Android 10 Settings, SoundSettings / sound_settings.xml) minus what he removed (Do Not Disturb, "Shortcut to prevent ringing",
 * the words "and vibrations" of the last heading, "Sound enhancement"): four volume sliders (Media, Call, Alarm, Notification), a divider, "Default notification sound" and "Default alarm sound", a
 * divider, the heading OTHER SOUNDS and the switches Screen locking sounds, Charging sounds and vibration, Touch sounds. No search anywhere. Every control changes the real system value:
 * a slider calls AudioManager.setStreamVolume on a worker thread and plays the stock release sample (the code follows the platform's SeekBarVolumizer, android-10.0.0_r47, line by line); a sound row opens
 * the system picker through {@link SsRingtoneFragment}; a switch writes the same Settings key as AOSP's SettingPref. The page follows changes made elsewhere (hardware keys, other apps, adb) while it is
 * visible: a receiver for the volume / mute / ringer / Do Not Disturb broadcasts plus one observer on the system settings, both removed in stop(). Rules (pure Java, JVM-tested): {@link SsSoundRules}.
 * The row layout is AOSP's preference_volume_slider.xml in code: row padding 16 dp, icon frame min 44 dp (icon 24 dp, end padding 12 dp, 4 dp above and below), a column with 8 dp above and below holding
 * the title (16 sp, 12 dp start padding) and a 48 dp SeekBar (12 dp start padding), which makes a 97 px row at 170 dpi, as measured on Tj's screenshot. Every system call is in a try / catch: a missing
 * service or permission leaves a slider that cannot move, never a crash.
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
    // audio thread: MSG_SET_VOLUME + the row index is the volume write of that row (coalesced), the others are the sample machinery of SeekBarVolumizer
    private static final int MSG_REQUEST_SAMPLE = 1, MSG_START_SAMPLE = 2, MSG_STOP_SAMPLE = 3, MSG_INIT_SAMPLE = 4, MSG_SYNC = 5, MSG_SET_VOLUME = 100;
    private static final int MSG_TITLES = 1;   // io thread
    private static final long TOAST_GAP_MS = 3000, REFRESH_DELAY_MS = 50;
    private static final String CANT_CHANGE = "This setting can't be changed from here.", NEEDS_DND = "This needs Do Not Disturb access.";

    /** What a refresh reads once for all rows: the facts that are not per stream. */
    private static final class Facts {
        boolean scoOn;
        int ringerMode = SsSoundRules.RINGER_NORMAL;
        int filter = SsSoundRules.FILTER_ALL;
        boolean allowAlarms = true, allowMedia = true, allowRinger = true;
    }

    /** One stream as the audio service reports it. */
    private static final class Level {
        int min, max, volume, lastAudible;
        boolean muted;
    }

    /**
     * Two hidden AudioManager methods the stock slider relies on; both are on the greylist (@UnsupportedAppUsage in Android 10), so reflection works for this app, and both fall back to the public
     * twin (the current volume, the public ringer mode) when this build refuses them.
     */
    private static final class Hidden {
        static final Method LAST_AUDIBLE = find("getLastAudibleStreamVolume", int.class);
        static final Method RINGER_INTERNAL = find("getRingerModeInternal");

        private static Method find(String name, Class<?>... args) {
            try { return AudioManager.class.getMethod(name, args); } catch (Throwable t) { return null; }
        }

        /** The volume the stream had before it was muted (a stream silenced by Do Not Disturb reports 0 itself, and the stock slider shows this one). */
        static int lastAudible(AudioManager am, int stream, int fallback) {
            try { if (LAST_AUDIBLE != null) return (Integer) LAST_AUDIBLE.invoke(am, stream); } catch (Throwable ignored) { }
            return fallback;
        }

        /** The ringer mode the user chose, which Do Not Disturb does not overwrite (the public getRingerMode answers silent while Do Not Disturb silences everything). */
        static int ringerMode(AudioManager am) {
            try { if (RINGER_INTERNAL != null) return (Integer) RINGER_INTERNAL.invoke(am); } catch (Throwable ignored) { }
            return am.getRingerMode();
        }
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
        private long lastToast;            // main thread only
        private boolean refreshPending;    // main thread only

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
            Audio au = new Audio();
            audio = au;
            ioThread = new HandlerThread("ss-sound-io");
            ioThread.start();
            io = new Handler(ioThread.getLooper(), m -> {
                if (m.what != MSG_TITLES) return false;
                loadTitle(SsSoundRules.TYPE_NOTIFICATION, ringNotification);
                loadTitle(SsSoundRules.TYPE_ALARM, ringAlarm);
                return true;
            });
            IntentFilter filter = new IntentFilter();
            for (String action : SsSoundRules.WATCHED_ACTIONS) filter.addAction(action);
            try { a.registerReceiver(receiver, filter); } catch (Throwable ignored) { }
            ContentResolver cr = a.getContentResolver();
            try { cr.registerContentObserver(Settings.System.CONTENT_URI, true, observer); } catch (Throwable ignored) { }   // every volume is stored per output device: watch the table, filter by name
            try { cr.registerContentObserver(Settings.Secure.getUriFor(SsSoundRules.KEY_CHARGING_SOUNDS), false, observer); } catch (Throwable ignored) { }
            refreshNow();
            for (Row r : rows) au.prepareSample(r.index, r.stream);   // SeekBarVolumizer.start: MSG_INIT_SAMPLE, so the first release plays at once
            requestTitles();
        }

        @Override public void stop() {
            if (!started) return;
            started = false;
            main.removeCallbacks(refresh);
            refreshPending = false;
            try { a.unregisterReceiver(receiver); } catch (Throwable ignored) { }
            try { a.getContentResolver().unregisterContentObserver(observer); } catch (Throwable ignored) { }
            for (Row r : rows) r.tracking = false;
            Audio au = audio;
            audio = null;
            if (au != null) au.shutdown();   // stops a playing sample (SoundSettings.onPause), still applies the volume writes already queued
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

        /** Reads everything again now (also what the audio thread asks for after a write); it replaces a refresh that was still waiting. */
        private final Runnable refresh = new Runnable() {
            @Override public void run() {
                main.removeCallbacks(this);
                refreshPending = false;
                if (started) refreshNow();
            }
        };

        /**
         * Coalesces a burst of broadcasts and setting changes (every step of a drag fires several) into one read, and does nothing while a finger is on a slider: the release asks for a read
         * after the last write (MSG_SYNC), and the slider under the finger is not moved by it anyway.
         */
        private void refreshSoon() {
            if (!started || refreshPending) return;
            for (Row r : rows) if (r.tracking) return;
            refreshPending = true;
            main.postDelayed(refresh, REFRESH_DELAY_MS);
        }

        private void refreshNow() {
            Facts f = facts();
            for (Row r : rows) r.sync(f);
            syncSwitches();
        }

        private Facts facts() {
            Facts f = new Facts();
            try { if (am != null) f.scoOn = am.isBluetoothScoOn(); } catch (Throwable ignored) { }
            try { if (am != null) f.ringerMode = Hidden.ringerMode(am); } catch (Throwable ignored) { }
            try {
                if (nm != null) {
                    f.filter = nm.getCurrentInterruptionFilter();
                    NotificationManager.Policy p = nm.getNotificationPolicy();
                    if (p != null) {
                        f.allowAlarms = SsSoundRules.allowsAlarms(p.priorityCategories);
                        f.allowMedia = SsSoundRules.allowsMedia(p.priorityCategories);
                        f.allowRinger = SsSoundRules.allowsRinger(p.priorityCategories);
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
                toast(CANT_CHANGE);
                syncSwitches();
            }
            return ok;
        }

        /**
         * TouchSoundPreferenceController: after the setting, the sound effects are loaded (on) or unloaded (off) off the main thread, so the next touch already has (or no longer holds) its sounds.
         * The page may be gone by then, so this thread belongs to no lifecycle.
         */
        private void loadEffects(final boolean on) {
            new Thread(() -> {
                try { if (on) am.loadSoundEffects(); else am.unloadSoundEffects(); } catch (Throwable ignored) { }
            }, "ss-sound-effects").start();
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
                final Context app = a.getApplicationContext();   // a background thread must not hold the activity
                Uri uri = RingtoneManager.getActualDefaultRingtoneUri(app, type);
                String title = null;
                if (uri != null) {
                    Ringtone r = RingtoneManager.getRingtone(app, uri);
                    if (r != null) {
                        try { title = r.getTitle(app); } finally { r.stop(); }   // getRingtone prepares a player: release it at once
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
            int stream = -1;
            int iconKind = -1;
            boolean tracking;   // main thread only: a finger is on the slider

            Row(int index) {
                this.index = index;
                String title = SsSoundRules.TITLES[index];
                view = new LinearLayout(a);
                view.setOrientation(LinearLayout.HORIZONTAL);
                view.setGravity(Gravity.CENTER_VERTICAL);
                view.setPadding(dp(16), 0, dp(16), 0);   // listPreferredItemPaddingStart / End
                view.setMinimumHeight(dp(48));           // listPreferredItemHeightSmall; the row itself is not clickable (android:clickable="false", SeekBarPreference is not selectable)

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
                t.setPaddingRelative(dp(12), 0, 0, 0);   // the stock widget_frame next to the title is GONE (the volume preference has no widget layout)
                t.setText(title);
                body.addView(t, new LinearLayout.LayoutParams(MATCH, WRAP));

                bar = new SeekBar(a);   // the theme's seekBarStyle: Material track (2 dp), teal thumb, same as the stock row
                bar.setPaddingRelative(dp(12), bar.getPaddingTop(), bar.getPaddingEnd(), bar.getPaddingBottom());
                bar.setContentDescription(title);
                bar.setOnSeekBarChangeListener(this);
                LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(MATCH, dp(48));
                slp.gravity = Gravity.CENTER_VERTICAL;
                body.addView(bar, slp);
            }

            /** Reads the stream and shows it: range, position, glyph, enabled. The position is left alone while the finger is on the slider. */
            void sync(Facts f) {
                int s = SsSoundRules.streamFor(index, f.scoOn);
                Level l = read(s);
                if (l == null && index == SsSoundRules.ROW_CALL && s != SsSoundRules.STREAM_VOICE_CALL) {   // the public API refuses the Bluetooth SCO stream: the call row falls back to the voice call stream
                    s = SsSoundRules.STREAM_VOICE_CALL;
                    l = read(s);
                }
                stream = s;
                if (l == null) {   // no audio service: a slider that cannot move
                    bar.setEnabled(false);
                    setIcon(SsSoundRules.ICON_NORMAL);
                    return;
                }
                boolean zen = SsSoundRules.zenMuted(f.filter, s, f.allowAlarms, f.allowMedia, f.allowRinger);
                setIcon(SsSoundRules.iconKind(index, f.ringerMode, l.muted, zen));
                bar.setEnabled(!zen);
                if (tracking) return;
                if (bar.getMax() != l.max) bar.setMax(l.max);   // the maximum first: a new minimum above the old maximum would be clamped
                if (bar.getMin() != l.min) bar.setMin(l.min);
                int p = SsSoundRules.shownVolume(s, l.volume, l.lastAudible, l.muted, zen, f.ringerMode, l.min, l.max);
                if (bar.getProgress() != p) {
                    if (bar.isAttachedToWindow()) bar.setProgress(p, true); else bar.setProgress(p);   // the stock slider animates to a volume changed elsewhere
                }
            }

            private Level read(int s) {
                if (am == null) return null;
                try {
                    Level l = new Level();
                    l.min = am.getStreamMinVolume(s);   // IllegalArgumentException for a stream that is not public (Bluetooth SCO)
                    l.max = Math.max(l.min, am.getStreamMaxVolume(s));
                    l.volume = am.getStreamVolume(s);
                    l.muted = am.isStreamMute(s);
                    l.lastAudible = Hidden.lastAudible(am, s, l.volume);
                    return l;
                } catch (Throwable t) {
                    return null;
                }
            }

            private void setIcon(int kind) {
                if (kind == iconKind) return;
                iconKind = kind;
                int res;
                switch (index) {
                    case SsSoundRules.ROW_MEDIA:
                        res = kind == SsSoundRules.ICON_MUTED ? R.drawable.ss_ic_media_stream_off : R.drawable.ss_ic_media_stream;
                        break;
                    case SsSoundRules.ROW_CALL:
                        res = R.drawable.ss_ic_local_phone;   // the call volume can never be muted (lowest volume 1): its glyph never changes
                        break;
                    case SsSoundRules.ROW_ALARM:
                        res = kind == SsSoundRules.ICON_MUTED ? R.drawable.ss_ic_audio_alarm_off : R.drawable.ss_ic_audio_alarm;
                        break;
                    default:
                        res = kind == SsSoundRules.ICON_RINGER_VIBRATE ? R.drawable.ss_ic_volume_ringer_vibrate
                                : kind == SsSoundRules.ICON_RINGER_SILENT ? R.drawable.ss_ic_notifications_off : R.drawable.ss_ic_notifications;
                }
                icon.setImageDrawable(a.getDrawable(res));
            }

            /** The progress of this slider IS the stream volume (the bar runs from the stream's lowest to its highest volume). */
            @Override public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                if (!fromUser) return;   // our own setProgress, or the system moving the slider
                Audio au = audio;
                if (au != null) au.setVolume(index, stream, progress);
            }

            @Override public void onStartTrackingTouch(SeekBar s) { tracking = true; }

            /** The finger lifts: play the stock sample (after the last write, same queue), then read the real volume back (SeekBarVolumizer.onStopTrackingTouch -> postStartSample). */
            @Override public void onStopTrackingTouch(SeekBar s) {
                tracking = false;
                Audio au = audio;
                if (au == null) return;
                au.requestSample(index, stream);
                au.sync();
            }
        }

        // ------------------------------------------------------------------ the audio worker: volume writes and release samples
        /**
         * One per start(): a thread for AudioManager.setStreamVolume and the release samples (SeekBarVolumizer's callback handler), so a slow audio service never stalls the page. Everything below the
         * constructor runs on that thread.
         */
        private final class Audio {
            private final HandlerThread thread = new HandlerThread("ss-sound-audio");
            final Handler handler;
            private final Context app = a.getApplicationContext();   // the samples must not hold the activity
            private final Ringtone[] samples = new Ringtone[SsSoundRules.TITLES.length];   // one prepared sample per row
            private final int[] sampleStream = new int[SsSoundRules.TITLES.length];
            private int current = -1;   // the row whose sample started last (SoundSettings.VolumePreferenceCallback.mCurrent)

            Audio() {
                Arrays.fill(sampleStream, -1);
                thread.start();
                handler = new Handler(thread.getLooper(), this::handle);
            }

            // ---- called from the main thread
            /** The latest value of a slider; an older one of the same row that has not been written yet is dropped (SeekBarVolumizer.postSetVolume). */
            void setVolume(int row, int stream, int progress) {
                handler.removeMessages(MSG_SET_VOLUME + row);
                handler.sendMessage(handler.obtainMessage(MSG_SET_VOLUME + row, stream, progress));
            }

            void requestSample(int row, int stream) { handler.sendMessage(handler.obtainMessage(MSG_REQUEST_SAMPLE, row, stream)); }

            void prepareSample(int row, int stream) { handler.sendMessage(handler.obtainMessage(MSG_INIT_SAMPLE, row, stream)); }

            void sync() { handler.sendEmptyMessage(MSG_SYNC); }

            /** The page leaves the screen: the writes already queued are still applied, then every sample is stopped and released; the delayed messages (cut-off, a second try) are dropped by quitSafely. */
            void shutdown() {
                handler.post(this::releaseAll);
                thread.quitSafely();
            }

            // ---- the audio thread
            private boolean handle(Message m) {
                if (m.what >= MSG_SET_VOLUME) {
                    writeVolume(m.arg1, m.arg2);
                    return true;
                }
                switch (m.what) {
                    case MSG_INIT_SAMPLE: sample(m.arg1, m.arg2); return true;
                    case MSG_REQUEST_SAMPLE: requestSampleNow(m.arg1, m.arg2); return true;
                    case MSG_START_SAMPLE: startSample(m.arg1, m.arg2); return true;
                    case MSG_STOP_SAMPLE: stopSampleOf(current); return true;
                    case MSG_SYNC: main.post(refresh); return true;
                    default: return false;
                }
            }

            /**
             * SeekBarVolumizer MSG_SET_STREAM_VOLUME: unmute a muted stream that is dragged above 0, set the volume, mute an unmuted stream that was dragged to 0. (The stock order is mute first; here the mute
             * comes after the set, so a refused set leaves nothing half done.) A SecurityException is Android's "Not allowed to change Do Not Disturb state": on a tablet without vibrator a ring-type stream
             * (Notification aliases Ring there) that reaches 0 switches the ringer to silent, which an app may do only with Do Not Disturb access. The slider then snaps back to the real value.
             */
            @SuppressLint("WrongConstant")   // FLAG_SHOW_UI_WARNINGS is hidden, so the SDK's flag list for setStreamVolume does not know it
            private void writeVolume(int stream, int progress) {
                try {
                    int adjust = SsSoundRules.muteAdjustment(am.isStreamMute(stream), progress);
                    if (adjust == SsSoundRules.ADJUST_UNMUTE) am.adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE, 0);
                    am.setStreamVolume(stream, progress, FLAG_SHOW_UI_WARNINGS);
                    if (adjust == SsSoundRules.ADJUST_MUTE) am.adjustStreamVolume(stream, AudioManager.ADJUST_MUTE, 0);
                    if (current >= 0) armCutoff();   // SoundSettings.onStreamValueChanged: a change while a sample is current restarts the 2 s timer
                } catch (SecurityException e) {
                    final String msg = e.getMessage() != null && e.getMessage().contains("Do Not Disturb") ? NEEDS_DND : CANT_CHANGE;
                    main.post(() -> toastRateLimited(msg));
                    main.post(refresh);
                } catch (Throwable t) {
                    main.post(refresh);
                }
            }

            private void armCutoff() {
                handler.removeMessages(MSG_STOP_SAMPLE);
                handler.sendEmptyMessageDelayed(MSG_STOP_SAMPLE, SsSoundRules.SAMPLE_CUTOFF_MS);
            }

            /** SeekBarVolumizer.postStartSample: at once, or after a second when the sample of this row still plays (and then it starts only if it has stopped by then). */
            private void requestSampleNow(int row, int stream) {
                handler.removeMessages(MSG_START_SAMPLE);
                int delay = SsSoundRules.sampleStartDelayMs(playing(row));
                if (delay == 0) startSample(row, stream);
                else handler.sendMessageDelayed(handler.obtainMessage(MSG_START_SAMPLE, row, stream), delay);
            }

            /** SeekBarVolumizer.onStartSample + SoundSettings.onSampleStarting: only when none plays; another row's sample is stopped first; the 2 s cut-off starts. */
            private void startSample(int row, int stream) {
                Ringtone r = sample(row, stream);
                if (r == null || playing(row)) return;
                if (current >= 0 && current != row) stopSampleOf(current);
                current = row;
                armCutoff();
                try { r.play(); } catch (Throwable ignored) { }
            }

            /**
             * The prepared sample of a row: Ringtone.setStreamType is what SeekBarVolumizer.onInitSample calls, so the sample plays on the slider's own stream (and its volume is the slider's). A Ringtone
             * that was stopped is gone (stop() destroys its player), so a new one is made when needed. The bypass-mute flag the stock adds is left out: it needs a privilege this app does not have.
             */
            @SuppressWarnings("deprecation")
            private Ringtone sample(int row, int stream) {
                Ringtone r = samples[row];
                if (r != null && sampleStream[row] == stream) return r;
                release(row);
                r = null;
                try {
                    r = RingtoneManager.getRingtone(app, sampleUri(stream));
                    if (r != null) r.setStreamType(stream);
                } catch (Throwable t) {
                    if (r != null) { try { r.stop(); } catch (Throwable ignored) { } }   // do not leave a prepared player behind
                    r = null;
                }
                samples[row] = r;
                sampleStream[row] = r == null ? -1 : stream;
                return r;
            }

            private boolean playing(int row) {
                Ringtone r = row < 0 ? null : samples[row];
                try { return r != null && r.isPlaying(); } catch (Throwable t) { return false; }
            }

            /** Cuts a sample that is still playing and prepares the next one (stop() destroys the player). A sample that has ended by itself is left alone. */
            private void stopSampleOf(int row) {
                if (row < 0 || !playing(row)) return;
                int stream = sampleStream[row];
                release(row);
                sample(row, stream);
            }

            private void release(int row) {
                Ringtone r = samples[row];
                samples[row] = null;
                sampleStream[row] = -1;
                if (r != null) { try { r.stop(); } catch (Throwable ignored) { } }
            }

            private void releaseAll() {
                for (int i = 0; i < samples.length; i++) release(i);
                current = -1;
            }

            /** Media: Settings' own media_volume.ogg (shipped as res/raw/ss_media_volume, 0.49 s mono Vorbis); the others: the default sound of their kind. */
            private Uri sampleUri(int stream) {
                switch (SsSoundRules.sampleSource(stream)) {
                    case SsSoundRules.SAMPLE_BUNDLED: return Uri.parse("android.resource://" + app.getPackageName() + "/" + R.raw.ss_media_volume);
                    case SsSoundRules.SAMPLE_RINGTONE: return Settings.System.DEFAULT_RINGTONE_URI;
                    case SsSoundRules.SAMPLE_NOTIFICATION: return Settings.System.DEFAULT_NOTIFICATION_URI;
                    default: return Settings.System.DEFAULT_ALARM_ALERT_URI;
                }
            }
        }
    }
}
