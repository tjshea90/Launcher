# CH1 — "Pocket return" + Quickstep keyboard choice (Tj, 2026-11-18). Design, written BEFORE the code.

Tj's request is in `TASKS.md` CH1 (verbatim). This file is the contract between the two apps, so a session cut off in the middle can
continue from it. LinkMeter's half is described again in LinkMeter `docs/LAUNCHER_BRIDGE.md` (same contract).

## What the switch is
Launcher control, group "Quick settings & keyboard" (or a group of its own): **"Pocket return"**, key `pocket_return`, default OFF in release
AND debug. OFF = nothing below happens and no existing code path changes (a pure function `PocketRules.active(switchOn, method)` is the only gate;
every new branch in old code asks it first; JVM test pins `active(false, *) == false`).

ON, and the return to the custom launcher comes from the **"Back to custom" quick-settings tile** or the **"Back to custom" drawer icon**
(both start `BackToCustomActivity`; `ReturnTile` now passes `via=tile`, the icon is `via=icon`). NOT double Home (`RouterActivity`), NOT the volume /
back / flurry / shake / knock methods, NOT a plain PIN unlock. Tj: "the double press on the home button ... should remain as is".

## The ten steps and where each one lives
| Tj step | Who does it | How |
| :- | :- | :- |
| 1 phone hotspot + Wi-Fi off | LinkMeter (tablet app, job on the tablet, orders go to the phone) | phone actions `wifi_off`, then `hotspot_off_deep` (hotspot off, location off, airplane on **only while Bluetooth stays up**, screen off) |
| 2 phone airplane mode on | LinkMeter | inside `hotspot_off_deep` (DeepSleepRules: never leaves the phone in airplane mode with no way to wake it) |
| 3 phone battery saver on, screen off | LinkMeter | `eco_auto_on` (LinkMeter's saver) before; `eco_android_on` (Android's own Battery saver) AFTER the hotspot is down (SystemSaver puts it back if the hotspot dies with it); `screen_off` again at the end, checked |
| 4 tablet Wi-Fi off | Launcher `PocketReturn` | after LinkMeter says "orders sent" (the hotspot route is the way to the phone; Bluetooth carries the rest) or after a 25 s cap |
| 5 tablet VPN off | Launcher | level-triggered `VpnOff` pipeline (`AnimationSpeedup.syncNow`), already quiet-world; pocket forces a look |
| 6 close all apps, force-stop the unneeded | Launcher `ReturnReset` (invisible path with the shell helper) | pocket widens the target list to every launchable non-system app except the keep set (self, stock launcher, IME, **LinkMeter**, Gboard / Swoop); without the helper the existing on-screen Recents clear runs |
| 7 switch to the custom launcher | Launcher (existing `ProfileSession.returnToCustom`) | not delayed: the phone job runs IN PARALLEL, Tj sees the custom home at once |
| 8 stock keyboard | Launcher `KeyboardSwitch.apply(CUSTOM)` | already follows `Mode.set`; pocket calls it again explicitly (idempotent) |
| 9 preload the custom launcher | Launcher + accessibility service | `TjAccessibilityService.pocketPreload(why)`: patch window / pencil cover (`refreshLayers`), lock number, Settings look-alike (`SettingsSpoof.syncSoon`), notifications + Recents cleared (the reset itself), no scrim measurement (BQ2) |
| 10 at the side key | accessibility service | with the switch ON the lock preparation runs at 0 ms and 250 ms after SCREEN_OFF (today 700 ms and 6.5 s, both stay): patch window, pencil cover, lock number, notifications closed, Wi-Fi / Bluetooth symbols hidden, no VPN, custom home in front behind the keyguard; at lock in the custom mode the complete clean (apps + Recents) runs once per lock |

Order inside the Launcher: the phone job is started FIRST (step 1-3), the return to the custom launcher runs at once beside it (7, 6, 8, 5), the tablet
Wi-Fi goes off when the job reports `sent` (4). That is the order of Tj's list for everything that depends on something earlier; nothing waits on the phone
for longer than 25 s, and a failed or missing phone step never blocks a tablet step (each step is isolated and writes one `POCKET` line into the lock log).

## Contract Launcher <-> LinkMeter (tablet LinkMeter, same device)
* LinkMeter exports a **ContentProvider**, authority `com.tj.linkmeter.launcher` (class `LauncherBridge`). Only `call()` is served; `query / insert / update / delete / getType` do nothing.
* Identity (`LauncherIdentity`, pure + tested): the caller's uid must own the package `com.tj.launcher` AND that package's signing certificate SHA-256 must be
  `0D:86:C7:DD:08:FF:4D:B3:52:E5:4B:46:8D:05:14:21:15:BA:42:AC:03:7C:BD:42:15:7B:48:24:4B:04:E5:F4` (= `tools/keystore.fingerprint` of the Launcher repo).
  LinkMeter's own key is public (a debug keystore in its repo), the Launcher's is private: this pin is what makes the bridge safe. Debug builds of LinkMeter accept the package name alone.
* `call("hello")` -> `{ok, versionCode, tablet, linked}`: LinkMeter is installed, is the tablet side, has a phone paired.
* `call("pocket_start")` -> starts ONE job (a second start while one runs returns the running one) and answers at once `{ok, state}`.
* `call("pocket_state")` -> `{state: IDLE|RUNNING|SENT|DONE|FAILED, sent: bool, text: "<what happened, whole sentences>", steps: "..."}`.
  `sent` = the phone accepted its sleep order (`hotspot_off_deep` answered "Sent"): from then on the tablet's Wi-Fi may go off (Bluetooth carries the rest).
* The job runs on a LinkMeter thread with a partial wake lock (<= 150 s); the Launcher polls `pocket_state` every 1 s until `sent` (cap 25 s), then every 5 s until done (cap 120 s) so the provider's process keeps the Launcher's priority while it works.
* Nothing in the phone app changes: only existing `/act` names are used, so an older phone app still works (an unknown action is reported in `text`).

## Quickstep keyboard choice (CH1e)
Launcher control radio "Keyboard in Quickstep": `stock` / `gboard` (default = today) / `swoop`. Stored `quickstep_keyboard`. `KeyboardPolicy.target(mode, enabled, original, choice)`:
Quickstep + stock = `pickStock`; + gboard = `pickGboard` (else the original); + swoop = the enabled IME whose package is `app.swoop.keyboard` (the release id; the `.debug` build `app.swoop.keyboard.debug` also counts), else null with a status line that names what IS enabled.
The custom side keeps the stock keyboard. Swoop is the Quickstep side's keyboard when chosen, so the Settings look-alike hides it like Gboard / SwiftKey (`isQuickstepSide`, `visibleLabels`).
Facts about Swoop (repo tjshea90/Keyboard, read 2026-11-18): `applicationId app.swoop.keyboard`, service `app.swoop.keyboard.KeysService` (IME id `app.swoop.keyboard/.KeysService`), label "Swoop".

## What can and cannot be proven here
CI emulator: the Launcher half (switch OFF = unchanged, ON = the step lines in the right order, Wi-Fi off, stock keyboard, lock prep at 0 ms) with LinkMeter absent ("phone steps skipped"
line). The phone half (`hotspot_off_deep` etc.) was proven by the earlier LinkMeter work only as far as its own notes say ("NOT RUN ON A DEVICE"); the bridge itself is proven by JVM tests (identity, step order,
isolation, timeouts, fake phone) and, for the real provider, by an instrumented-free check: the manifest entry, the cert pin and `tools/test.sh`. The real run needs Tj's tablet + phone: TASKS CH1g.
