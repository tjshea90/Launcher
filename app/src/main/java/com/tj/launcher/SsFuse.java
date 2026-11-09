package com.tj.launcher;

import android.content.Context;

/**
 * BN1, review C finding 3 (2026-11-09): about 7,000 lines of look-alike page code run in the MAIN process, which also hosts the custom Home and the accessibility service, so an uncaught exception on any
 * Settings page counted toward the crash-loop breaker ({@link CrashGuard}: 3 crashes within 2 minutes = back to Quickstep and every switch wiped). The same remedy as KA1's keep-alive fuse: the FIRST crash
 * that began in the look-alike's own code only switches the look-alike OFF (and remembers why) and is NOT counted; a repeat while the fuse is already blown counts like any crash. Tj's own flip of the switch
 * in Launcher control is his explicit retry and re-arms the fuse. Pure decisions here (JVM-tested: SsFuseTest); {@link #blowFuse} runs in the dying process and must never throw.
 */
final class SsFuse {
    /** True after the fuse blew (and until Tj switches the look-alike on again). */
    static final String BLOCKED_KEY = "ss_blocked";
    /** When and why (the exception's class and message), one flat line, or "". */
    static final String WHY_KEY = "ss_blocked_why";
    private static final String PKG = "com.tj.launcher.";

    private SsFuse() {}

    /** Pure: is this class part of the look-alike? The Ss* classes (pages, rows, views, rules, receivers; nested classes too) and {@link SettingsSpoof}. */
    static boolean isLookAlikeClass(String className) {
        if (className == null || !className.startsWith(PKG)) return false;
        String simple = className.substring(PKG.length());
        return simple.startsWith("Ss") || simple.startsWith("SettingsSpoof");
    }

    /**
     * Pure: did this crash begin in the look-alike? The FIRST frame of our own code (package com.tj.launcher) on the exception's stack decides, looking at the exception first and then at its causes
     * (outermost first, at most 8 deep): a crash in Home, the accessibility service or Launcher3 has a different first frame (or none of ours at all) and stays the breaker's.
     */
    static boolean isLookAlikeCrash(Throwable e) {
        int depth = 0;
        for (Throwable t = e; t != null && depth < 8; t = t.getCause(), depth++) {
            StackTraceElement[] frames = t.getStackTrace();
            if (frames == null) continue;
            for (StackTraceElement f : frames) {
                String cn = f.getClassName();
                if (cn != null && cn.startsWith(PKG)) return isLookAlikeClass(cn);
            }
        }
        return false;
    }

    /** Pure: the fuse takes this crash (the first one only; a repeat while it is already blown goes to the breaker like any crash). */
    static boolean fuseHandles(Throwable e, boolean alreadyBlown) { return !alreadyBlown && isLookAlikeCrash(e); }

    static boolean blocked(Context c) { return Features.get(c, BLOCKED_KEY, false); }

    static String blockedWhy(Context c) { return Features.getString(c, WHY_KEY, ""); }

    /** Called from the crash handler on the main thread of the dying process: the look-alike goes OFF, synchronously (the process is about to die). The next process start puts the alias back to off. */
    static void blowFuse(Context c, Throwable e) {
        try {
            Features.set(c, SettingsSpoof.KEY, false);
            Features.set(c, BLOCKED_KEY, true);
            String m = e == null ? "" : e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage()).replaceAll("\\s+", " ");
            Features.setString(c, WHY_KEY, new java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.US).format(new java.util.Date()) + "  " + (m.length() > 140 ? m.substring(0, 140) : m));
        } catch (Throwable ignored) { }
    }

    /** Tj switched the look-alike on in Launcher control: his retry re-arms the fuse. */
    static void rearm(Context c) {
        Features.set(c, BLOCKED_KEY, false);
        Features.setString(c, WHY_KEY, "");
    }
}
