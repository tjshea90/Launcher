package com.tj.launcher;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import android.util.Log;
import android.view.PixelCopy;
import android.view.Window;

/**
 * BI1: the LAUNCHER's half of the real-pixel patch on the unlocked home screen. The home screen is the system wallpaper (behind the Launcher's transparent window) plus what the Launcher draws on top (icons,
 * labels, folders). The wallpaper half is a calibrated crop ({@link HomeShot}); this class copies exactly what the Launcher's window shows in the same screen rectangle with {@link PixelCopy} (the Launcher is in
 * this process; GPU-drawn text and shadows come out as they are on screen, with their alpha) and lays it over the wallpaper crop the way SurfaceFlinger does (SRC_OVER, premultiplied): the result is what the
 * screen shows in that rectangle.
 */
final class HomeLayer {
    private static final String TAG = "TjHomeLayer";

    private HomeLayer() {}

    /** Called on the handler's thread with the copied layer (alpha kept) or null and the reason. */
    interface Done { void done(Bitmap layer, String problem); }

    /** Copies the Launcher window's pixels in {@code src} (window = screen coordinates: the Launcher is full screen). Never throws; {@code cb} is always called, on {@code h}. */
    static void copy(final Rect src, final Handler h, final Done cb) {
        try {
            final com.android.launcher3.Launcher l = TjHooks.launcher();
            if (l == null) { post(h, cb, null, "no launcher instance"); return; }
            final Window w = l.getWindow();
            if (w == null || w.peekDecorView() == null || !w.peekDecorView().isAttachedToWindow()) { post(h, cb, null, "the launcher window is not attached"); return; }
            if (src.width() <= 0 || src.height() <= 0) { post(h, cb, null, "empty rectangle"); return; }
            final Bitmap b = Bitmap.createBitmap(src.width(), src.height(), Bitmap.Config.ARGB_8888);
            PixelCopy.request(w, new Rect(src), b, new PixelCopy.OnPixelCopyFinishedListener() {
                @Override public void onPixelCopyFinished(int result) {
                    if (result == PixelCopy.SUCCESS) cb.done(b, null);
                    else { b.recycle(); cb.done(null, "PixelCopy result " + result); }
                }
            }, h);
        } catch (Throwable t) {
            Log.e(TAG, "copy failed", t);
            post(h, cb, null, "" + t);
        }
    }

    private static void post(Handler h, final Done cb, final Bitmap b, final String why) {
        h.post(new Runnable() { @Override public void run() { cb.done(b, why); } });
    }

    /**
     * BL1 (the home curtain): {@code layer} drawn straight onto the MUTABLE {@code base} at ({@code left}, {@code top}), SRC_OVER like {@link #compose}, and {@code base} is returned (no copy: one full-screen bitmap
     * fewer alive at once on a low-RAM tablet). The layer may be SMALLER than the base (the Launcher window can leave a system bar out; everything outside the layer stays the wallpaper picture). Null on any failure,
     * and when the layer does not fit inside the base or the base is not mutable.
     */
    static Bitmap composeInPlace(Bitmap base, Bitmap layer, int left, int top) {
        try {
            if (base == null || layer == null || !base.isMutable() || left < 0 || top < 0 || left + layer.getWidth() > base.getWidth() || top + layer.getHeight() > base.getHeight()) return null;
            new Canvas(base).drawBitmap(layer, left, top, null);
            return base;
        } catch (Throwable t) { Log.e(TAG, "composeInPlace failed", t); return null; }
    }

    /** {@code layer} over a copy of {@code base} (both the same size, base is the opaque wallpaper crop): what the screen shows. Returns null on any failure. */
    static Bitmap compose(Bitmap base, Bitmap layer) {
        try {
            if (base == null || layer == null || base.getWidth() != layer.getWidth() || base.getHeight() != layer.getHeight()) return null;
            Bitmap out = base.copy(Bitmap.Config.ARGB_8888, true);
            if (out == null) return null;
            new Canvas(out).drawBitmap(layer, 0, 0, null);
            return out;
        } catch (Throwable t) { Log.e(TAG, "compose failed", t); return null; }
    }
}
