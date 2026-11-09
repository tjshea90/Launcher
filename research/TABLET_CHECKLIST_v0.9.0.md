# Tablet checklist for v0.9.0, the Settings look-alike (BN1). DRAFT written 2026-11-09 before the release; NOTHING in it has run on the real tablet yet, only JVM tests and the CI emulator

Your words (2026-11-09): "The settings app does not need to work perfectly, it only needs to look identical. For any settings that are hard to build or won't function, don't build it. Simply make it look like the setting is there." So the LOOK was built from your screenshots; the controls change the real setting where the app is allowed to, and stay silent (no message, no snap back) where it is not.

Install: Release page, `Launcher-v0.9.0.apk`, over the old one (same key: permissions, Home choice, accessibility stay). The adb helper does not survive an install: start it again, so it knows the new `SETSYS` verb (Mono audio, Audio balance). One NEW adb permission for the Battery page ("Last full charge", "Screen usage since full charge"): `adb shell pm grant com.tj.launcher android.permission.PACKAGE_USAGE_STATS` (the setup scripts and the "Do everything" list in the app have it).

## What it is
In the CUSTOM launcher the drawer has a "Settings" icon (the stock gear on teal) that opens the look-alike: Battery, Display, Sound, Storage, Accessibility, System, About tablet, in that order, no search bar or icon anywhere. The real Settings is kept out of the custom drawer; the Quickstep drawer always keeps it, so you can put the two side by side. Launcher control > "Settings look-alike in the custom launcher" switches it off (the real Settings comes back).

## Do this
**A. Open it and compare each page with the real Settings (Quickstep mode).** Row heights, text sizes, icons, summaries, spacing. Tell me every difference, with a screenshot of both (your photos stay out of the repo).
**B. Battery.** Header (percentage, charging state, "Last full charge", "Screen usage since full charge"), the overflow menu has only "Battery usage", which opens a page with ONE permanent Solitaire row.
**C. Display.** Brightness level (the system's own slider dialog), Screen timeout (list), Auto-rotate screen, Font size and Display size pages with their slider and preview. Font size: Small / Default / Large / Largest = 1.0 / 1.2 / 1.3 / 1.4. Display size: the app cannot run `wm density` itself, so the slider may move without changing the screen size. That is expected; say if it looks wrong.
**D. Sound.** Four volume sliders, the two ringtone rows (system picker), Other sounds switches. No Do Not Disturb row.
**E. Storage.** The donut, "Used of ...", the categories and System. Tapping System opens a one-sentence dialog (my default; tell me if you want it inert).
**F. Accessibility and System.** Headings and rows as in your screenshots, Text-to-speech output page, Touch & hold delay dialog, Magnification page, Languages & input (last row Text-to-speech output), Legal information.
**G. About tablet.** No owner area; Custom build version `vnd_tb8168p1_bsp 1779111601`.

## Questions for you (built with a default, not blocking)
1. Should the Solitaire row in Battery usage show a percentage? (built without)
2. Storage > System: a dialog, or nothing? (built: dialog)
3. Storage numbers in GB with two decimals, as on the real page? (built that way)

## Send back
* The list from A with screenshots; the tablet report (Launcher control > report) if a page crashes or the Settings icon is missing.

## If something is wrong
* Launcher control > "Settings look-alike in the custom launcher" off, or "Restore everything". The previous release is v0.8.13 (Releases).
* Details and every decision: `research/SETTINGS_BUILD_LOG_2026-11-09.md`; research: `research/aosp10-settings/` and `research/SETTINGS_SPOOF_2026-11-09.md`.
