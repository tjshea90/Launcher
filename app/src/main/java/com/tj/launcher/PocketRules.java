package com.tj.launcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * CH1 (Tj, 2026-11-18): "Pocket return". With the switch ON, the "Back to custom" quick-settings tile and drawer icon do MORE than the plain return: LinkMeter
 * (same tablet) puts the linked phone to sleep, the tablet's Wi-Fi and VPN go off, every open app is closed, the custom launcher and the lock screen are
 * prepared ahead of time. Design: {@code research/CH1_POCKET_RETURN_DESIGN_2026-11-18.md}; the other half: LinkMeter {@code docs/LAUNCHER_BRIDGE.md}.
 *
 * <p>Everything that DECIDES is in this class (pure, JVM-tested in {@code PocketRulesTest}); {@link PocketReturn} is the glue that does it. The one rule
 * every old code path relies on: {@link #active} is false with the switch OFF, so each new branch in old code is dead then and nothing changes
 * (Tj: "If it is toggle off, there should be no changes to how the launcher currently behaves").
 */
public final class PocketRules {
    private PocketRules() {}

    /** The switch in Launcher control. Default OFF in release AND debug (a new behaviour that closes apps and switches a phone: only on Tj's word). */
    public static final String KEY = "pocket_return";
    public static boolean defaultOn() { return false; }

    /** How the return was asked for. Only these two count (Tj: "either the quick settings tile or the app drawer button"). */
    public static final String VIA_TILE = "tile", VIA_ICON = "icon";

    /**
     * Pure: does the extra pocket work run for this return? Only with the switch ON and only for the tile / the icon. The double Home press, the volume / back /
     * flurry / shake / knock methods and a plain PIN unlock are NOT pocket returns (Tj: "the double press on the home button ... should remain as is").
     */
    public static boolean active(boolean switchOn, String via) {
        return switchOn && (VIA_TILE.equals(via) || VIA_ICON.equals(via));
    }

    // ------------------------------------------------------------------------------------------------ lock preparation (step 10)

    /** The delays (ms after SCREEN_OFF) at which the lock preparation runs today. */
    private static final long[] LOCK_PREP_NORMAL = { 700L, 6500L };
    /** With the switch ON the lock preparation also runs at the side-key press itself and 250 ms later ("immediately"), before the old 700 ms. */
    private static final long[] LOCK_PREP_POCKET = { 0L, 250L, 700L, 6500L };

    /** Pure: when the lock preparation runs after the screen went off. Switch OFF = exactly the old two times. A copy: callers may not change the table. */
    public static long[] lockPrepDelaysMs(boolean switchOn) {
        return (switchOn ? LOCK_PREP_POCKET : LOCK_PREP_NORMAL).clone();
    }

    /**
     * Pure: at the lock, in the custom launcher, close the apps that have a notification (force-stop: their notifications go with them)? Only with the
     * switch ON, a custom session (the Quickstep case has its own clean-up), the keyguard up, no handshake armed, the helper there, and once per lock.
     */
    public static boolean lockNotificationClean(boolean switchOn, boolean customMode, boolean automationOff, boolean keyguardLocked, boolean handshakeArmed, boolean helperReady, boolean alreadyDone) {
        return switchOn && customMode && !automationOff && keyguardLocked && !handshakeArmed && helperReady && !alreadyDone;
    }

    // ------------------------------------------------------------------------------------------------ which apps may be force-stopped (step 6)

    public static final String LINKMETER_PKG = "com.tj.linkmeter";

    /**
     * Pure: packages the pocket clean-up never stops although the old rules ({@link ResetPolicy#shouldKill}) would allow it: LinkMeter (it carries the phone steps and
     * the Bluetooth link; a force-stopped app also stays "stopped" until opened), and the keyboards Tj uses (Gboard, SwiftKey, Swoop: the stock keyboard is the
     * system's own and the old rules keep the current one).
     */
    public static boolean keepPackage(String pkg) {
        if (pkg == null) return false;
        return LINKMETER_PKG.equals(pkg) || KeyboardPolicy.isGboard(pkg) || KeyboardPolicy.isSwoop(pkg) || KeyboardPolicy.isSwiftKey(pkg, null);
    }

    /** What the real device is asked per package (the glue backs it with PackageManager); a fake in the tests. */
    public interface Facts {
        /** The old rule: us, the stock launcher, the current keyboard and the core system parts are never closed. */
        boolean mayClose(String pkg);
        /** A system app (FLAG_SYSTEM) that no update replaced: never force-stopped by the pocket clean-up ("or system apps or launcher related"). */
        boolean isSystemApp(String pkg);
    }

    /**
     * Pure: the packages the pocket return force-stops. {@code origin} = what the old clean-up already decided (apps Quickstep opened); {@code taskPackages} =
     * the apps that have a card in Recents right now (they are OPEN); {@code notified} = apps with a notification in the shade. Every list may be null (the helper
     * could not answer). Order: origin first, then the cards, then the notifications; no duplicates. The apps that are only INSTALLED are never added: a force-stop puts
     * an app into Android's "stopped" state, which silences its pushes until it is opened again (ReturnReset.runInvisible), so only what is open or notifying is stopped.
     */
    public static List<String> stopList(Collection<String> origin, Collection<String> taskPackages, Collection<String> notified, Facts facts) {
        Set<String> out = new LinkedHashSet<>();
        add(out, origin, facts);
        add(out, taskPackages, facts);
        add(out, notified, facts);
        return new ArrayList<>(out);
    }

    private static void add(Set<String> out, Collection<String> in, Facts facts) {
        if (in == null) return;
        for (String p : in) {
            if (p == null || p.isEmpty() || out.contains(p)) continue;
            if (keepPackage(p) || !facts.mayClose(p) || facts.isSystemApp(p)) continue;
            out.add(p);
        }
    }

    // ------------------------------------------------------------------------------------------------ the bridge to LinkMeter (steps 1-3 and when step 4 may run)

    /** The answer to {@code hello}; {@code null} everywhere a field is unknown. {@code answered} false = the call failed (not installed, process died, security refusal). */
    public static final class Hello {
        public final boolean answered, ok, tablet, linked;
        public final String why;
        public Hello(boolean answered, boolean ok, boolean tablet, boolean linked, String why) {
            this.answered = answered; this.ok = ok; this.tablet = tablet; this.linked = linked; this.why = why == null ? "" : why;
        }
        public static Hello none() { return new Hello(false, false, false, false, ""); }
    }

    /** What to do about the phone. */
    public static final class Verdict {
        public final boolean start;
        public final String text;
        Verdict(boolean start, String text) { this.start = start; this.text = text; }
    }

    /** Pure: may the phone job be started? Anything short of a clear yes skips the PHONE steps only; the tablet steps never wait for it. */
    public static Verdict helloVerdict(Hello h) {
        if (h == null || !h.answered) return new Verdict(false, "LinkMeter did not answer (not installed, or an older LinkMeter without the Launcher bridge): the phone steps are skipped");
        if (!h.ok) return new Verdict(false, "LinkMeter refused the Launcher (it does not know this app's signing key): the phone steps are skipped");
        if (!h.tablet) return new Verdict(false, "this LinkMeter is not set up as the tablet: the phone steps are skipped");
        if (!h.linked) return new Verdict(false, "LinkMeter has no phone linked" + (h.why.isEmpty() ? "" : " (" + h.why + ")") + ": the phone steps are skipped");
        return new Verdict(true, "LinkMeter is ready");
    }

    public static final long SENT_CAP_MS = 25_000L;       // the tablet's Wi-Fi goes off at the latest this long after the phone job started
    public static final long TOTAL_CAP_MS = 120_000L;      // the Launcher stops watching the job after this long
    public static final long FAST_POLL_MS = 1_000L, SLOW_POLL_MS = 5_000L;

    /** One look at {@code pocket_state}; {@code answered} false = the call failed. {@code state}: IDLE / RUNNING / SENT / DONE / FAILED (anything else is not understood). */
    public static final class Look {
        public final boolean answered, ok, sent;
        public final String state, text;
        public Look(boolean answered, boolean ok, String state, boolean sent, String text) {
            this.answered = answered; this.ok = ok; this.state = state == null ? "" : state; this.sent = sent; this.text = text == null ? "" : text;
        }
        public static Look none() { return new Look(false, false, "", false, ""); }
    }

    /** What the Launcher does after a look. */
    public static final class Step {
        /** The tablet's Wi-Fi goes off now (never true twice: the caller says whether it already is). */
        public final boolean wifiOffNow;
        /** Stop watching. */
        public final boolean finished;
        /** When to look again (ms from now), if not finished. */
        public final long nextMs;
        /** One short reason for the lock log. */
        public final String why;
        Step(boolean wifiOffNow, boolean finished, long nextMs, String why) { this.wifiOffNow = wifiOffNow; this.finished = finished; this.nextMs = nextMs; this.why = why; }
    }

    /**
     * Pure: the watcher's one decision after each look. The tablet's Wi-Fi is the way to the phone (its hotspot) until the phone has ACCEPTED its sleep order
     * ({@code sent}; Bluetooth carries the rest), so it goes off when the job says so, when the job ended, when LinkMeter does not answer, or at the latest
     * {@link #SENT_CAP_MS} after the start. The deadline is checked FIRST (a watcher that waits on an answer must end by itself).
     */
    public static Step next(Look look, boolean wifiAlreadyOff, long elapsedMs) {
        if (elapsedMs >= TOTAL_CAP_MS) return new Step(!wifiAlreadyOff, true, 0, "stopped watching after " + (TOTAL_CAP_MS / 1000) + " s");
        if (look == null || !look.answered) return new Step(!wifiAlreadyOff, true, 0, "LinkMeter did not answer");
        if (!look.ok) return new Step(!wifiAlreadyOff, true, 0, "LinkMeter refused");
        switch (look.state) {
            case "DONE": return new Step(!wifiAlreadyOff, true, 0, "the phone job is done");
            case "FAILED": return new Step(!wifiAlreadyOff, true, 0, "the phone job failed");
            case "SENT": return new Step(!wifiAlreadyOff, false, SLOW_POLL_MS, "the phone accepted its sleep order");
            case "RUNNING":
                if (look.sent) return new Step(!wifiAlreadyOff, false, SLOW_POLL_MS, "the phone accepted its sleep order");
                if (elapsedMs >= SENT_CAP_MS) return new Step(!wifiAlreadyOff, false, SLOW_POLL_MS, "no sleep order accepted after " + (SENT_CAP_MS / 1000) + " s: the Wi-Fi goes off anyway");
                return new Step(false, false, FAST_POLL_MS, "waiting for the phone's sleep order");
            default:   // IDLE after a start (the job vanished) or a word we do not know
                return new Step(!wifiAlreadyOff, true, 0, "the phone job is not running (" + (look.state.isEmpty() ? "no state" : look.state) + ")");
        }
    }

    /** Pure: a one-line, plain summary for the Launcher control screen and the tablet report. */
    public static String summary(String at, String verdictText, String lastLook, boolean wifiWasOff) {
        StringBuilder b = new StringBuilder();
        b.append(at == null || at.isEmpty() ? "no pocket return yet" : "last pocket return " + at);
        if (verdictText != null && !verdictText.isEmpty()) b.append("; ").append(verdictText);
        if (lastLook != null && !lastLook.isEmpty()) b.append("; phone: ").append(lastLook);
        b.append(wifiWasOff ? "; tablet Wi-Fi off" : "; tablet Wi-Fi left as it was");
        return b.toString();
    }
}
