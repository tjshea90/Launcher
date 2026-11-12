package com.tj.launcher;

import java.util.ArrayList;
import java.util.List;

/**
 * BS1 + BS2 (Tj, 2026-11-10 23:27 UTC: "Is it possible to keep wifi on when I turn the screen off and it goes to screen lock, but hide the wifi symbol in the lock screen and custom launcher"; 23:55 UTC: "Make it
 * also keep Bluetooth on but hide Bluetooth the same way it keeps wifi on but hides the symbol. Only keep them on if they are already on. Don't turn them on automatically. They should never, at any time, show up
 * on the lock screen or in the custom launcher"). The pure rules (JVM-tested, no Android classes) of the Wi-Fi and Bluetooth SYMBOL hold; {@link RadioIconHide} is the Android side and
 * {@link NotifQuietPolicy#why} decides WHEN (the same world as the pop-up banners: the custom launcher, a dark or locked screen in any launcher, a plain PIN still being decided; an unlocked, decided Quickstep
 * session shows the symbols as usual). BZ1 (Tj, 2026-11-12): the same hold, by the same rule, hides the VPN key too ({@link #SLOT_VPN}); nothing here switches a VPN on or off.
 *
 * HOW (verified in the AOSP 10 sources, research/RADIO_ICONS_HIDE_2026-11-10.md): the secure setting {@link #SETTING} is a comma-separated list of status-bar slot names that SystemUI hides; one shared icon
 * controller feeds the status bar, the LOCK SCREEN's status bar and the notification panel's header, and a change is live. A MISSING setting means "rotate,headset" in SystemUI's code, so the value we write is
 * the original list (or that default) PLUS the slots {@link #SLOTS}, never just "wifi,bluetooth,vpn". The Wi-Fi slot goes through SystemUI's signal policy (hidden = not shown), the Bluetooth one and the VPN
 * key (StatusBarSignalPolicy.updateVpn) through the generic icon view (a blocked slot's view stays GONE however often the icon is updated).
 *
 * The hold bookkeeping is {@link NotifQuietHold}'s, for a list instead of a zero: the original goes to disk BEFORE the first write (a missing setting is a real original: {@code null}); a list that already hides
 * every slot of ours (Tj's own, or ours still there) is left alone and nothing is held; a hold that is already on keeps its ORIGINAL and only re-adds what is missing; ending the hold puts the exact original back
 * only while the value is still exactly ours, and otherwise takes out only the slots that our write added (a list somebody changed meanwhile keeps his edit); the stored hold is cleared only AFTER that write;
 * without the permission a hold is neither taken nor ended (the saved original waits for the next start with it); a write that fails while TAKING the hold is undone, one that fails while ENDING it leaves the
 * hold so the next trigger tries again.
 *
 * Keeping the radios ON is the other half ({@link #keepsRadiosOn}): AK1's lock-screen rule, the custom launcher's radio guard and the session reset leave BOTH radios alone only while the symbols can really be
 * hidden. The feature only stops things from switching a radio OFF: nothing in it turns a radio on (Tj: "Only keep them on if they are already on. Don't turn them on automatically.").
 */
public final class RadioIconHold {
    private RadioIconHold() {}

    /** The switch in Launcher control (shipped default ON, debug builds OFF: the older emulator sections keep their premises, like quiet_banners and clean_start). */
    public static final String KEY = "keep_radios_hide_icons";
    /** Settings.Secure: the hidden status-bar slots, comma-separated. */
    public static final String SETTING = "icon_blacklist";
    /** The slot names of the Wi-Fi and Bluetooth symbols (framework strings status_bar_wifi / status_bar_bluetooth). */
    public static final String SLOT_WIFI = "wifi";
    public static final String SLOT_BLUETOOTH = "bluetooth";
    /** The slot name of the VPN key (framework string status_bar_vpn; BZ1). Only its symbol is hidden: nothing switches a VPN on or off. */
    public static final String SLOT_VPN = "vpn";
    /** Every slot the hold hides, in the order missing ones are appended. */
    public static final String[] SLOTS = { SLOT_WIFI, SLOT_BLUETOOTH, SLOT_VPN };
    /** What SystemUI hides when the setting is missing (StatusBarIconController.getIconBlacklist(null) in Android 10). */
    public static final String SYSTEM_DEFAULT = "rotate,headset";

    public static boolean defaultOn(boolean debuggable) { return !debuggable; }

    /**
     * Pure: do the radio switch-offs (AK1 lock-screen rule, the custom launcher's radio guard, the session reset) leave Wi-Fi AND Bluetooth ON? Only where the symbols WILL be hidden: exactly when
     * {@link NotifQuietPolicy#why} wants the hold on a dark screen or a lock screen (the places those switch-offs run): the switch is on, the adb permission is there (without it the symbols would show on the lock
     * screen), automation is not paused ("Restore everything" / the crash breaker), and in a Quickstep mode the accessibility helper is running (a lock screen that is up BEFORE the helper binds, as right after a
     * boot, cannot have its symbols hidden yet: the radios go off like before, v0.9.3; review of BS1). One rule for both, so they can never disagree.
     */
    public static boolean keepsRadiosOn(boolean switchOn, boolean grant, boolean automationOff, boolean quickstepMode, boolean helperUp) {
        final NotifQuietPolicy.Facts f = new NotifQuietPolicy.Facts();
        f.switchOn = switchOn; f.grant = grant; f.automationOff = automationOff; f.quickstepMode = quickstepMode; f.helperUp = helperUp;
        f.screenOn = false;   // the switch-offs run on a lock screen or in the custom launcher; a dark screen is "quiet" in the policy's words in every mode
        return NotifQuietPolicy.wanted(f);
    }

    /**
     * Pure (review of BS2, finding 2): the hold has just ENDED with this reason from {@link NotifQuietPolicy#why}; must the radios that were being kept on go off NOW? Yes when the accessibility helper went away
     * while the screen is dark or the lock screen is up: the symbols would show with both radios still on, and the helper is what hears a radio come on (nothing else would switch them off). Never for the normal
     * end (an unlocked Quickstep session: the symbols are meant to show), a deliberate pause ("Restore everything", the crash breaker: the launcher does nothing then), a missing permission or the switch.
     */
    public static boolean radiosGoOffWhenHoldEnds(String why, boolean darkOrLocked) { return darkOrLocked && NotifQuietPolicy.WHY_NO_HELPER.equals(why); }

    /** What a trigger that would END the hold does (third review of BS2, finding 1): {@link #RELEASE} put the list back now, {@link #START_DEFERRAL} keep it and switch the radios off first, {@link #HOLD_BACK} keep it, a deferral is running. */
    public static final int RELEASE = 0, START_DEFERRAL = 1, HOLD_BACK = 2;
    /** The state of the deferral: nothing running, a worker is switching the radios off, that worker is doing its own final sync (which must release). */
    public static final int DEFER_IDLE = 0, DEFER_RUNNING = 1, DEFER_FINISHING = 2;

    /**
     * Pure (third review of BS2, finding 1): the symbols must not come back BEFORE the radios are off. A trigger wants to end the hold with reason {@code why}; only the helper's absence ({@link #radiosGoOffWhenHoldEnds})
     * leaves radios on that must go. The first such trigger STARTS a deferral; every further one while it runs (the helper's unbind is reported more than once within milliseconds, run #522: the second released the list
     * 6 ms after the first) is HELD BACK, the worker releases; the worker's own final sync ({@code deferState} FINISHING) releases at once, so a deferral can never repeat in a loop. A deferral that never finished
     * ({@code now} past {@code runningUntil}) blocks nothing for ever. Every other reason releases at once.
     */
    public static int releaseDecision(String why, boolean darkOrLocked, int deferState, long now, long runningUntil) {
        if (!NotifQuietPolicy.WHY_NO_HELPER.equals(why)) return RELEASE;
        if (deferState == DEFER_FINISHING) return RELEASE;
        if (deferState == DEFER_RUNNING && now < runningUntil) return HOLD_BACK;
        return radiosGoOffWhenHoldEnds(why, darkOrLocked) ? START_DEFERRAL : RELEASE;
    }

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

    /** Does the stored value already hide EVERY slot of ours? */
    public static boolean hidesAll(String list) { return missing(list).isEmpty(); }

    /** The slots of ours that this stored value does not hide yet, in {@link #SLOTS} order. */
    public static List<String> missing(String list) {
        List<String> have = slots(list), out = new ArrayList<>();
        for (String s : SLOTS) if (!have.contains(s)) out.add(s);
        return out;
    }

    /** The stored value that hides every slot of ours in addition to what {@code list} hides (a missing setting starts from the system default). */
    public static String with(String list) {
        List<String> parts = slots(list);
        for (String s : SLOTS) if (!parts.contains(s)) parts.add(s);
        return join(parts);
    }

    /** {@code list} without the given slots. A missing setting stays missing (nothing of ours can be in it). */
    public static String without(String list, List<String> remove) {
        if (list == null) return null;
        List<String> parts = slots(list);
        parts.removeAll(remove);
        return join(parts);
    }

    /** No write is wanted. */
    public static final String NOTHING = "\u0000nothing";

    /**
     * What to write when the hold ends: the exact original (null = delete the setting) while the value is still exactly ours; otherwise only the slots our write ADDED to that original are taken out (a list
     * somebody changed meanwhile keeps his edit, and a slot Tj had hidden himself stays hidden); {@link #NOTHING} when none of the added slots is hidden any more (a reset, Tj's own edit).
     */
    public static String restoreValue(String current, String original) {
        final List<String> added = missing(original);
        boolean any = false;
        for (String s : added) if (hides(current, s)) any = true;
        if (!any) return NOTHING;
        if (current != null && current.equals(with(original))) return original;
        return without(current, added);
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
            if (hidesAll(cur)) return held;   // everything we hide is hidden already (by us, or Tj's own list): nothing to write, nothing new to remember
            try {
                if (!held) st.save(true, cur);   // the original on disk BEFORE the first change; a hold that is already on keeps its original (the restore takes out only what we added)
                s.put(with(cur));
                return true;
            } catch (RuntimeException e) {
                log.log("could not hide the Wi-Fi / Bluetooth symbols: " + e);
                if (!held) { try { st.save(false, null); } catch (RuntimeException ignored) { } }
                return held;
            }
        }
        if (!held) return false;
        if (!permitted) return true;   // cannot put it back now: the hold and the saved original stay
        try {
            final String w = restoreValue(s.get(), st.original());
            if (!NOTHING.equals(w)) { s.put(w); log.log("symbol list put back to " + (w == null ? "(system default)" : "\"" + w + "\"")); }
            st.save(false, null);   // cleared only AFTER the write
            return false;
        } catch (RuntimeException e) {
            log.log("could not put the symbol list back: " + e);
            return true;
        }
    }
}
