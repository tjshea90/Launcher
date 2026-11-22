# CV1: ONE launcher (ours) for both worlds: design (Tj, 2026-11-22)

Tj (verbatim, TASKS CV1): "Build it all in one release. Work carefully. Look at what the custom launcher currently does and what quickstep does. The goal is for me to notice no difference in functionality, only it is one launcher instead of quickstep. Everything must behave and work exactly as it already does, but using only the custom launcher."
Inputs: `research/CV1_AUDIT_A_*` (every Mode read), `_B_` (every stock-launcher use), `_C_` (app filter, reload, port gaps). Tags: [V] verified in code, [I] reasoned, [T] only Tj's tablet can show.

## 1. Idea in one paragraph
`Mode` stays the ONE state variable (CUSTOM = locked down, QUICKSTEP = unlocked). What changes is WHICH HOME is in front in QUICKSTEP mode: with switch `unified_launcher` ON (release default ON, debug default OFF, like `clean_start`) the Router starts OUR Launcher3 for both modes, and the Launcher runs in one of two PROFILES that follow the mode:
- **CUSTOM profile** = today's custom launcher, byte for byte: `launcher.db`, only the allowed apps, Settings look-alike, Solitaire stand-in, patches.
- **UNLOCKED profile** = what Quickstep is today: EVERY launchable app (also Launcher control, Back to custom, Setup, Adb guide; real Settings, no look-alike), its OWN icon layout (`launcher_unlocked.db`), no hidden-app logic, no patches, no Solitaire.
The stock launcher stays installed and is used only (a) as Android's Recents (it always was: `RecentsActivity`, [V] audit B (3)), (b) as the EMERGENCY exit (below).

## 2. Decisions (and why)
D1 **Separate layout DB per profile.** The workspace is not filtered by `HiddenApps` [V C-1]: one shared layout would put a hidden app's icon (placed in the unlocked home) on the custom home, or leave holes in a layout arranged for the other profile. `LauncherProvider` gets `METHOD_SET_PROFILE_DB`; the loader calls it as the FIRST step of every load, on the model worker thread (the same looper as `ModelWriter` [V], so no write is in flight). Each profile has its own `DatabaseHelper` (ids per DB). First creation of the unlocked DB is SEEDED by copying `favorites` rows of the custom DB (ATTACH + INSERT outside any transaction); any failure = Launcher3's default layout. **The stock home's own arrangement (folders "Connect", "Games") cannot be read (private to com.android.launcher3, [V C-4]): the one unavoidable visible difference; disclosed to Tj.**
D2 **The profile is level-triggered.** `HomeProfile.wanted(c)` is a pure function of (switch, emergency, mode). `HomeProfile.loaded` (volatile, set by the loader at its start) is what the model really holds. `HomeProfile.syncNow(c, why)` reloads (`HiddenApps.reloadLauncher` = `forceReload`) when they differ. Triggers: `Mode.set`, Launcher resume (`TjHooks.onLauncherResumed`), process start, switch / emergency change. A reload that is not needed is never started.
D3 **A flip is hidden.** Custom -> unlocked at a handshake: the reload starts the moment the handshake is accepted, behind the keyguard (PIN typing takes seconds); if the model is not ready at the unlock the existing cover waits (cap 3 s). Unlocked -> custom (lock, plain PIN, double Home, tile): the cover stays up until `HomeProfile.ready` (`loaded == wanted` and `model.isModelLoaded()`); `settleHome` removes it only then. Never a visible moment of the unlocked home in custom mode.
D4 **Filter in the unlocked profile = everything the stock drawer shows**: `AppVisibility.isShownUnlocked` (all packages, all own LAUNCHER components except the Settings alias). One place (`HiddenApps.isShown` / `isPackageShown`) feeds AppFilter, SessionCommitReceiver (new apps get a home icon like stock), sweeps, notification sightings [V C-3]. `RecentsSweep` is already a no-op in QUICKSTEP.
D5 **Emergency exit = switch OFF behaviour.** `Unified.active(c) = switchOn && !emergency`. Emergency is set by the crash breaker (third crash in 120 s: Mode QUICKSTEP + RestoreStore + `emergency=true`), by a failed start of our own launcher in QUICKSTEP, and by nothing else; cleared by `CrashGuard.clearIfNewer` (new version), the Control switch (any change) and a new boot is NOT enough. While emergency, EVERY rule below behaves exactly like v0.9.16 (stock home visible in QUICKSTEP, both stray guards, `startStock`). OS rungs (Safe mode, Default home app, uninstall/disable) unchanged [V B-1.5].
D6 **Recents stays Android's own** (stock process, [V B-3]); its opening animation may differ slightly from the integrated one (disclosed, [T]).
D7 **No new permission, no helper work, no new adb grant.**

## 3. What changes, file by file (each needs a JVM test where the rule is pure)
| Area | Change |
| :- | :- |
| `Unified` (new) | `switchOn`, `emergency`, `active`, `setEmergency`, `defaultOn(debuggable)`. Pure `UnifiedRules`: `homeFor(mode, active)` (CUSTOM->OURS; QS->OURS if active else STOCK), `profile(mode, active)`, `strayCustomApplies(active)`, `verifyFront(...)`. |
| `HomeProfile` (new) | `wanted`, `loaded`, `ready`, `syncNow`, DB name per profile, loader hook `onLoadStart`. |
| `RouterActivity` | QS-world start = `startQuickstepHome()`: ours when `Unified.active`, else `startStock()`; failure of ours -> `startStock()` + `Mode.markFailOpen` + `Unified.setEmergency(true)`. Double Home / L10 / prearm logic unchanged. |
| `TjHooks.onLauncherResumed` | `customHomeAppeared` (stray custom guard) only when `!Unified.active`; syncNow. |
| `TjAccessibilityService` | `guardStrayHome`: also in QS when active (stock HOME window = stray; Recents is not a HOME window, as today); `startQuickstepFront`, the handshake front check (:1996-1998) and `verifyQuickstepFront` (:2062) judge "our launcher in front" when active; `settleHome` waits for `HomeProfile.ready`. |
| `LoaderTask` / `LauncherProvider` / `LauncherSettings` | profile DB switch (D1), `volatile mOpenHelper`. |
| `HiddenApps`, `AppVisibility` | unlocked rule (D4). `HiddenAppsActivity` skips the reload while the unlocked profile is loaded (the filter does not depend on its choice). |
| `RecentsPolicy.apply` | no `AppOrigin.markCustom` while the unlocked profile is wanted (apps opened there belong to the Quickstep session and are cleaned at the return). |
| `CrashGuard`, `Mode`, `ControlActivity`, `ReportState` | emergency set / clear, switch row, report lines (switch, emergency, wanted/loaded profile, db sizes, last switch). |
| Scenario / gate | new sections `ul` (unified path), `ul2` (emergency + crash), core re-run with `unified` token. |

## 4. Not changed on purpose
Hidden-app Recents sweeps, home patch, lock patch, Settings look-alike, keyboard / tiles / immersive / notifications / radios / battery saver / animations (all keyed on `Mode`, [V A-4/A-5]); Recents scans (`com.android.launcher3` windows) and the stock force-stop / refresh (the stock process still hosts Recents and leaks); handshake, return methods, failsafes (FailsafeKeys), boot rule, UsageGap.

## 5. Differences Tj may notice (to tell him plainly)
1. The unlocked home starts as a COPY of the custom home's layout, not as his old stock arrangement (cannot be imported).
2. Recents opening animation may differ [T].
3. "Home settings" page: the stock popup also shows only Wallpapers (AE7, photo 7), so nothing is lost.
4. First load of the unlocked profile after a handshake may need a moment (reload); the cover bridges it [T].

## 6. Test plan
JVM: `UnifiedRulesTest`, `HomeProfileTest` (wanted / ready / reload decision / db names), `AppVisibilityTest` (+unlocked), router decision, crash -> emergency, report lines. Emulator `ul`: switch ON, handshake -> our Launcher in front, drawer has Launcher control + Settings (real) + all apps, Control opens, no look-alike, `launcher_unlocked.db` exists, layout independent (item added in unlocked home absent from custom), double Home -> custom, custom drawer hides apps again (cover until ready), lock/unlock cycle, Recents key opens `com.android.launcher3` RecentsActivity, no crash. `ul2`: emergency path = stock home, crash breaker. Lock core `base,n,o,ac,e1,x1,rb,last` run BOTH with the switch off (debug default) and once with `unified` ON where meaningful. Review: read-only subagents, delta only, neutral briefs.
