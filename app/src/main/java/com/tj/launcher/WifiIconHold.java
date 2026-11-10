package com.tj.launcher;

import java.util.ArrayList;
import java.util.List;

/**
 * BS1 (Tj, 2026-11-10 23:27 UTC: "Is it possible to keep wifi on when I turn the screen off and it goes to screen lock, but hide the wifi symbol in the lock screen and custom launcher"). The pure rules
 * (JVM-tested, no Android classes) of the Wi-Fi SYMBOL hold; {@link WifiIconHide} is the Android side and {@link NotifQuietPolicy#why} decides WHEN (the same world as the pop-up banners: the custom launcher,
 * a dark or locked screen in any launcher, a plain PIN still being decided; an unlocked, decided Quickstep session shows the symbol as usual).
 *
 * HOW (verified in the AOSP 10 sources, research/WIFI_ICON_HIDE_2026-11-10.md): the secure setting {@link #SETTING} is a comma-separated list of status-bar slot names that SystemUI hides; one shared icon
 * controller feeds the status bar, the LOCK SCREEN's status bar and the notification panel's header, and a change is live. A MISSING setting means "rotate,headset" in SystemUI's code, so the value we write is
 * the original list (or that default) PLUS {@link #SLOT}, never just "wifi".
 *
 * The hold bookkeeping is {@link NotifQuietHold}'s, for a list instead of a zero: the original goes to disk BEFORE the first write (a missing setting is a real original: {@code null}); a list that already hides
 * the symbol (Tj's own, or ours still there) is left alone and nothing is held; ending the hold puts the exact original back only while the value is still exactly ours, and takes out only our token from a list
 * somebody changed meanwhile; the stored hold is cleared only AFTER that write; without the permission a hold is neither taken nor ended (the saved original waits for the next start with it); a write that fails
 * while TAKING the hold is undone, one that fails while ENDING it leaves the hold so the next trigger tries again.
 *
 * Keeping the radio ON is the other half ({@link #keepsWifiOn}): AK1's lock-screen rule and the custom launcher's radio guard leave Wi-Fi alone only while the symbol can really be hidden.
 */
public final class WifiIconHold {
    private WifiIconHold() {}

    /** The switch in Launcher control (shipped default ON, debug builds OFF: the older emulator sections keep their premises, like quiet_banners and clean_start). */
    public static final String KEY = "keep_wifi_hide_icon";
    /** Settings.Secure: the hidden status-bar slots, comma-separated. */
    public static final String SETTING = "icon_blacklist";
    /** The slot name of the Wi-Fi symbol (framework string status_bar_wifi). */
    public static final String SLOT = "wifi";
    /** What SystemUI hides when the setting is missing (StatusBarIconController.getIconBlacklist(null) in Android 10). */
    public static final String SYSTEM_DEFAULT = "rotate,headset";

    public static boolean defaultOn(boolean debuggable) { return !debuggable; }

    /**
     * Pure: do the Wi-Fi switch-offs (AK1 lock-screen rule, the custom launcher's radio guard, the session reset) leave Wi-Fi ON? Only while the symbol can really be hidden: the switch is on, the adb permission is
     * there (without it the symbol would show on the lock screen: today's behaviour, Wi-Fi off, is the safer one) and automation is not paused ("Restore everything" / the crash breaker).
     */
    public static boolean keepsWifiOn(boolean switchOn, boolean grant, boolean automationOff) { return switchOn && grant && !automationOff; }

    // ---------------------------------------------------------------- the list, parsed exactly as SystemUI parses it (split at ',', empty entries dropped, no trimming, exact compare)

    private static List<String> slots(String list) {
        List<String> out = new ArrayList<>();
        for (String s : (list == null ? SYSTEM_DEFAULT : list).split(",")) if (!s.isEmpty()) out.add(s);
        return out;
    }

    private static String join(List<String> parts) {
        StringBuilder b = new StringBuilder();
        for (String s : parts) { if (b.length() > 0) b.append(','); b.append(s); }
        return b.toString();
    }

    /** Does SystemUI hide this slot with this stored value? ({@code null} = the setting is missing = the system default list.) */
    public static boolean hides(String list, String slot) { return slots(list).contains(slot); }

    /** The stored value that hides {@code slot} in addition to what {@code list} hides (a missing setting starts from the system default). */
    public static String with(String list, String slot) {
        List<String> parts = slots(list);
        if (!parts.contains(slot)) parts.add(slot);
        return join(parts);
    }

    /** {@code list} without {@code slot}. A missing setting stays missing (nothing of ours can be in it). */
    public static String without(String list, String slot) {
        if (list == null) return null;
        List<String> parts = slots(list);
        while (parts.remove(slot)) { /* every copy */ }
        return join(parts);
    }

    /** No write is wanted. */
    public static final String NOTHING = "\u0000nothing";

    /**
     * What to write when the hold ends: the exact original (null = delete the setting) while the value is still exactly ours; our token taken out of a list somebody changed meanwhile; {@link #NOTHING} when the
     * symbol is not hidden any more (a reset, Tj's own edit).
     */
    public static String restoreValue(String current, String original) {
        if (!hides(current, SLOT)) return NOTHING;
        if (current != null && current.equals(with(original, SLOT))) return original;
        return without(current, SLOT);
    }

    // ---------------------------------------------------------------- the saved original (a Settings.Global string, so it survives "Clear data")

    /** The marker value: "n" = the setting was missing, "v" + list = the stored list (it may be empty). */
    public static String encodeMarker(String original) { return original == null ? "n" : "v" + original; }

    /** The original a marker stands for. An unreadable marker counts as "missing" (the system default is the safe side). */
    public static String decodeMarker(String marker) {
        if (marker == null || marker.isEmpty() || marker.charAt(0) != 'v') return null;
        return marker.substring(1);
    }

    // ---------------------------------------------------------------- the hold

    /** What survives a process death. */
    public interface State {
        boolean held();
        /** The value to put back (null = the setting was missing); only meaningful while {@link #held}. */
        String original();
        /** Stores both in ONE write. Must throw when the write did not happen. */
        void save(boolean held, String original);
    }

    /** The one Settings.Secure value. */
    public interface Setting {
        String get();
        /** {@code null} deletes the setting. Must throw when the write did not happen. */
        void put(String value);
    }

    public interface Sink { void log(String message); }

    /** Brings the setting in line with {@code want} and returns whether the hold is on afterwards. Never throws for an ordinary failed write (it is logged and the stored state stays consistent). */
    public static boolean set(State st, Setting s, boolean permitted, boolean want, Sink log) {
        final boolean held = st.held();
        if (want) {
            if (!permitted) return held;
            final String cur = s.get();
            if (hides(cur, SLOT)) return held;   // already hidden (by us, or Tj's own list): nothing to write, nothing new to remember
            try {
                st.save(true, cur);   // the original (the newest value without our token) on disk BEFORE the change
                s.put(with(cur, SLOT));
                return true;
            } catch (RuntimeException e) {
                log.log("could not hide the Wi-Fi symbol: " + e);
                if (!held) { try { st.save(false, null); } catch (RuntimeException ignored) { } }
                return held;
            }
        }
        if (!held) return false;
        if (!permitted) return true;   // cannot put it back now: the hold and the saved original stay
        try {
            final String w = restoreValue(s.get(), st.original());
            if (!NOTHING.equals(w)) { s.put(w); log.log("Wi-Fi symbol list put back to " + (w == null ? "(system default)" : "\"" + w + "\"")); }
            st.save(false, null);   // cleared only AFTER the write
            return false;
        } catch (RuntimeException e) {
            log.log("could not put the Wi-Fi symbol list back: " + e);
            return true;
        }
    }
}
