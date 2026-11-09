package com.tj.launcher;

/**
 * BN1: every page of the Settings look-alike (one activity class, {@link SsPageActivity}, shows the page named in its intent). {@code id} is the intent extra, {@code title} the toolbar title (the real strings
 * of Android 10 Settings, or Tj's screenshots where the tablet differs). HOME has no toolbar (the stock home page has the search card where it would be, and Tj wants no search).
 */
public enum SsPage {
    HOME("home", "Settings"),
    BATTERY("battery", "Battery"),
    BATTERY_USAGE("battery_usage", "Battery usage"),
    DISPLAY("display", "Display"),
    FONT_SIZE("font_size", "Font size"),
    DISPLAY_SIZE("display_size", "Display size"),
    SOUND("sound", "Sound"),
    STORAGE("storage", "Storage"),
    ACCESSIBILITY("accessibility", "Accessibility"),
    TTS("tts", "Text-to-speech output"),
    MAGNIFICATION("magnification", "Magnification"),
    SYSTEM("system", "System"),
    LANGUAGES("languages", "Languages\u00a0& input")   /* AOSP language_settings: a no-break space before the ampersand */,
    ABOUT("about", "About tablet"),
    LEGAL("legal", "Legal information");

    public final String id;
    public final String title;

    SsPage(String id, String title) { this.id = id; this.title = title; }

    /** The page for an intent extra; anything unknown or missing is the home page (never a crash on a stale or malformed intent). */
    public static SsPage fromId(String id) {
        if (id != null) for (SsPage p : values()) if (p.id.equals(id)) return p;
        return HOME;
    }

    /** The seven entries of the home page, in Tj's order (his message of 2026-11-09 02:06 UTC: Battery, Display, Sound, Storage, Accessibility, System, About tablet). */
    public static SsPage[] homeEntries() { return new SsPage[] { BATTERY, DISPLAY, SOUND, STORAGE, ACCESSIBILITY, SYSTEM, ABOUT }; }
}
