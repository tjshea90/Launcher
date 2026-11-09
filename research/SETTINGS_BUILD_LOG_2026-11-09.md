# BN1 build log (Settings look-alike) — newest facts first; read with SETTINGS_BUILD_PLAN_2026-11-09.md

## Where things are (so a cold session does not search)
- Code (all `app/src/main/java/com/tj/launcher/`): `SettingsSpoof` (switch `settings_spoof`, alias gating, `syncSoon` / `syncNow`, real Settings kept out of the custom drawer), `SsPage` (enum of pages),
  `SsPageActivity` (ONE activity, extra `ss_page`), `SsPages` (page builders; M0: home only, others say "not built yet"), `SsRows` (row / tile / divider factory). Resources: `res/values/ss_styles.xml`
  (theme `SsTheme`, `SsTheme.Home`), `ss_strings.xml`, `res/drawable/ss_ic_launcher.xml` (adaptive icon) + `res/mipmap-*/ss_ic_launcher_foreground.png` (AOSP gear PNGs, unchanged), `ss_glyph_*.xml` (the seven
  home glyphs), `ss_ic_arrow_back.xml`. Manifest: `SsPageActivity` + `activity-alias com.tj.launcher.SettingsAlias` (DISABLED in the manifest; the code enables it). Wiring: `AppVisibility.ALWAYS_SHOWN_OWN_COMPONENTS`,
  `HiddenApps.isShown/isPackageShown` (real Settings hidden), `Mode.set`, `TjApplication.onCreate`, `RestoreStore`, `RecentsGuard` (the look-alike's task stays in Recents), `ControlActivity` (switch row), `ReportState`.
- Tests: `SettingsSpoofTest` (10 JVM tests: gating, entry set, page table). Emulator section `ss` (scenario + `tools/gate-map.txt`): SS1-SS7 = alias on/off, ONE Settings in the drawer, opens `SsPageActivity`, seven rows in order,
  no search. First CI run of `ss` with `shots=true`: emulator.yml run #451 (id 42565458939, head 2ba5988).
- The switch is WORK IN PROGRESS: default OFF everywhere (`SettingsSpoof.defaultOn` returns false; `SettingsSpoofTest.wipDefaultIsOff` pins it). The finishing commit makes release builds ON (`return !debuggable`) and changes the test.

## Tj's screenshots (still in `/root/.claude/uploads/<session>/`; NEVER copy into the repo). File name prefix -> page
07401db6 Battery 84% | d6bbbc77 Battery 86% | e895669e Battery 86% | 0b8dab8c Sound | 4cd04033 Sound (scrolled) | 1eeec128 Display | 20aea9d7 Display | 2a288d48 Accessibility (top) | 3c150c6a Accessibility (middle) |
3d280fbe Accessibility (lower) | 52ef06ee Storage | 623c7986 Languages & input | 64660f88 About tablet (top, Owner) | a39abad9 About tablet (scrolled) | 8052f043 Touch & hold delay dialog |
8d14938a Text-to-speech output | aa9db61d Legal information | d9d18032 Magnification | cc4fb931 System | be456e72 Settings home (top) | df7cd971 Settings home (bottom) | afaadb68, bd381c3e = chat screenshots (not Settings).
A contact sheet of all of them can be made in a minute with PIL (scratchpad only).

## Decisions taken while building (Claude; Tj can veto) — rule: ANYTHING not in Tj's list of pages or screenshots does nothing when tapped (he said "only the following settings pages available"; "Model & hardware" is inert on the stock tablet too)
1. Inert rows: Languages & input -> Languages, Virtual keyboard, Physical keyboard, Spell checker (their pages are not in his list); About -> Model & hardware, Android version (its page shows the Build number), Custom build version;
   Battery usage -> the Solitaire row. Controls that live ON an allowed page work (e.g. Pointer speed dialog, brightness, timeout, volumes).
2. Legal information rows open the real read-only licence screens through the public intents (`android.settings.LICENSE` etc.) when a handler exists.
3. Sound ringtone rows use `RingtoneManager.ACTION_RINGTONE_PICKER` (public; what the real Settings does).

## Open questions for Tj (ask ONCE, batched, only if his answer changes the work)
- none yet (collect here; ask with the question tool at a natural pause)
