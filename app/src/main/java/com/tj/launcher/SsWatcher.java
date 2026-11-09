package com.tj.launcher;

import android.app.Activity;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

/**
 * BN1: watches system settings while a Settings look-alike page is visible, so a value changed elsewhere (the real quick tiles, an adb command, the hardware keys) shows at once. One ContentObserver for
 * all the watched settings; registered in onStart and removed in onStop by the activity ({@link SsPages.Live}), so nothing outlives the screen. {@code onChange} runs once at start (initial refresh).
 */
final class SsWatcher implements SsPages.Live {
    private final Activity a;
    private final Runnable onChange;
    private final Uri[] uris;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ContentObserver obs = new ContentObserver(main) {
        @Override public void onChange(boolean selfChange) { SsWatcher.this.onChange.run(); }
    };

    SsWatcher(Activity a, Runnable onChange, Uri... uris) { this.a = a; this.onChange = onChange; this.uris = uris; }

    static Uri sys(String name) { return Settings.System.getUriFor(name); }
    static Uri sec(String name) { return Settings.Secure.getUriFor(name); }

    @Override public void start() {
        for (Uri u : uris) {
            try { if (u != null) a.getContentResolver().registerContentObserver(u, false, obs); } catch (Throwable ignored) { }
        }
        onChange.run();
    }

    @Override public void stop() { try { a.getContentResolver().unregisterContentObserver(obs); } catch (Throwable ignored) { } }
}
