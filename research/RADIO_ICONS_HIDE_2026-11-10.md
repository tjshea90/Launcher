# BS1 + BS2: keep Wi-Fi and Bluetooth ON on the lock screen and in the custom launcher, but hide their symbols (Tj, 2026-11-10 23:27 and 23:55 UTC)

Tj (verbatim, BS1): "Is it possible to keep wifi on when I turn the screen off and it goes to screen lock, but hide the wifi symbol in the lock screen and custom launcher"

Tj (verbatim, BS2, a follow-up the same night): "Make it also keep Bluetooth on but hide Bluetooth the same way it keeps wifi on but hides the symbol. Only keep them on if they are already on. Don't turn them on automatically. They should never, at any time, show up on the lock screen or in the custom launcher"

## Verdict from the AOSP 10 source (read 2026-11-10, branch `android10-release`, files under `frameworks/base/packages/SystemUI/src/com/android/systemui/statusbar/phone/` unless noted)

**Yes, on stock Android 10 SystemUI.** The secure setting `icon_blacklist` (a comma-separated list of status-bar slot names) hides the Wi-Fi symbol everywhere the status icons are drawn, while the radio stays on:

- `StatusBarIconController.ICON_BLACKLIST = "icon_blacklist"`; `getIconBlacklist(String)` splits the value at commas. **When the setting is missing the code uses `"rotate,headset"`** (so a write of just `wifi` would also bring the rotate and headset icons back: our value must be the default list PLUS `wifi`).
- `StatusBarSignalPolicy.onTuningChanged`: `blockWifi = blockList.contains(mSlotWifi)` (`mSlotWifi` = framework string `status_bar_wifi` = `wifi`); on a change `mBlockWifi = blockWifi || mForceBlockWifi` and the network callbacks are re-registered; `setWifiIndicators`: `boolean visible = statusIcon.visible && !mBlockWifi;` = the icon (and its in/out arrows) is simply not shown. `mForceBlockWifi` is never assigned in this file.
- `StatusBarIconControllerImpl` registers `addTunable(this, ICON_BLACKLIST)`; `onTuningChanged` clears and re-adds every holder of every slot with the new blocklist, so the change is LIVE (no restart).
- **One controller feeds all the status-icon containers:** `KeyguardStatusBarView.onAttachedToWindow` does `mIconManager = new TintedIconManager(findViewById(R.id.statusIcons)); Dependency.get(StatusBarIconController.class).addIconGroup(mIconManager);` = the LOCK-SCREEN status bar uses the same blocklist; `QuickStatusBarHeader` does the same for the notification panel's header icons. `addIconGroup` hands each group `blocked = mIconBlacklist.contains(slot.getName())`.
- Slot names (core `config_statusBarIcons`): alarm_clock, rotate, headset, data_saver, ime, sync_failing, sync_active, nfc, tty, speakerphone, cdma_eri, data_connection, phone_evdo_signal, phone_signal, secure, managed_profile, cast, vpn, bluetooth, camera, microphone, location, mute, volume, zen, ethernet, **wifi**, hotspot, mobile, airplane, battery, sensors_off.

## What the source does NOT tell us (unproven until a tablet report)

- Tj's tablet runs the OEM's SystemUI (MediaTek keyguard, see TASKS X1 / the number overlay). `icon_blacklist` is AOSP's mechanism; the OEM build may still follow it (it is code from the same tree) but nothing here proves it. Proof on the CI emulator is stock SystemUI only.
- The Wi-Fi sleep policy: `Settings.Global.WIFI_SLEEP_POLICY = "wifi_sleep_policy"` (0 default, 1 never while plugged, 2 never) exists; the Android 10 Wi-Fi service files (`WifiController`, `ActiveModeWarden`, ...) could not be fetched (404 on the mirror), so whether this ROM disconnects Wi-Fi on screen-off by itself is NOT verified. This feature only stops OUR code from switching the radio off; it does not touch the sleep policy.
- Hiding the symbol hides nothing else: the Wi-Fi tile in the notification panel stays, and the Settings look-alike / real Settings still show Wi-Fi.

## Design (TASKS BS1b + BS2a; code: `RadioIconHold` pure rules, `RadioIconHide` Android side; renamed from WifiIcon* in BS2a)

- One switch "Keep Wi-Fi and Bluetooth on, hide their symbols" (`keep_radios_hide_icons`, formerly `keep_wifi_hide_icon`; release default ON, debug OFF like `quiet_banners`). Needs the `WRITE_SECURE_SETTINGS` adb grant the launcher already holds; without it nothing changes (the Wi-Fi guards keep switching Wi-Fi off exactly as before, because a visible symbol on the lock screen is worse than today).
- WHEN the symbol is hidden = the same world as the pop-up banner quiet (`NotifQuietPolicy.why`): the custom launcher, a dark or locked screen in ANY launcher, a plain PIN still being decided. An unlocked, decided Quickstep session gets the original `icon_blacklist` back (Tj's "lock screen and custom launcher" only).
- WHAT is written: `Settings.Secure icon_blacklist` = the original list (a missing setting counts as `rotate,headset`) + `wifi`; the original is saved FIRST in `Settings.Global tj_radio_icons_original` (survives "Clear data", like `tj_banners_original`), and put back only while the value is still ours; a list somebody changed meanwhile only loses our `wifi` token.
- WHAT stops being switched off: the Wi-Fi half of AK1 (`LockQuiet.onLock`, the lock-screen re-check) and of the custom-launcher radio guard / `ReturnReset.radiosOff` (one choke point). Bluetooth rules are unchanged (Tj did not mention Bluetooth). Condition `WifiIconHold.keepsWifiOn(switchOn, grant, automationOff)`: switch on AND grant AND automation not paused (breaker / Restore everything).

## Emulator proof of the symbol (run #519, id 42926361302, main@8babd57, section `wf`, Wi-Fi only; pictures from branch ci-shots, RUN.txt named the run)

Status-bar pictures at 600x1024: the Wi-Fi fan icon sits at x 545-560, y 5-17. Pure-white pixels in the box x 543-563, y 3-20: unlocked Quickstep **102**, custom launcher **0**, Quickstep again **102**, custom again **0**. The lock screen (a PIN set, Wi-Fi on, hold on) shows only the signal triangle, the battery and "100%" (no Wi-Fi fan); the lock-screen control (no hold) is added to the section in run #520. So on STOCK Android 10 SystemUI the list hides the symbol in the status bar and in the lock-screen status bar, live, with the radio on (`wifi_on=1`, `mNetworkInfo` CONNECTED not printed fully).
The uiautomator dump is useless for this: it showed 0 SystemUI nodes in the unlocked states (the status bar is not part of the active window's tree) and none mentioning Wi-Fi on the lock screen. Pixels it is.

## BS2: Bluetooth

- Slot name: `bluetooth` (`status_bar_bluetooth`, in the framework `config_statusBarIcons` list read for BS1). The icon is drawn by `PhoneStatusBarPolicy.updateBluetooth` through the same `StatusBarIconController` (see the second source read below), so the same list hides it on the status bar, the lock screen and the panel header.
- The emulator has NO Bluetooth hardware (the existing scenario says so at the tile checks): Bluetooth is proven by the JVM tests (list helpers for two slots, the restore rule) and by the list value `rotate,headset,wifi,bluetooth` on the emulator, never by a picture.
- "Only keep them on if they are already on. Don't turn them on automatically": the feature only STOPS switching radios off; nothing in it turns a radio on (the hold writes `icon_blacklist`, `RadioIconHide.keepsRadiosOn` only makes the switch-OFF paths skip). The one older path that turns a radio on is AK1's put-back after a gesture unlock (`LockQuiet.onUnlock`). With the feature active nothing is switched off at a lock (`lq_prev` = w0b0), so it has nothing to put back; it only restores a radio AK1 itself switched off in that same lock, i.e. a lock screen that came up before the helper could hide the symbols (right after a boot in Quickstep mode: `keepsRadiosOn` is false there, the radios go off like v0.9.3, and come back at a gesture unlock). Skipping the put-back while the feature is on was considered and DROPPED: it would leave the radios off after every restart that began in Quickstep mode. Emulator phase N (WF12a / WF12 / WF12b) proves Wi-Fi OFF stays OFF after taking the hold, after a gesture unlock and after a plain PIN.
- Known gap, said plainly: the first seconds after a reboot that started from an UNLOCKED Quickstep session (the hold was off at shutdown, the process starts a few seconds after the lock screen first draws). Closing it needs an ACTION_SHUTDOWN hook that only a real reboot can prove; not built.
- Uninstalling the launcher while the hold is on leaves the two symbols hidden (the list stays in Settings); "Restore everything" first, or `adb shell settings delete secure icon_blacklist`.

## Moments walked (TASKS BS2c: "They should never, at any time, show up on the lock screen or in the custom launcher")

The hold is on whenever `NotifQuietPolicy.why` says quiet; the radios are kept only where `RadioIconHold.keepsRadiosOn` (= the same rule on a lock screen) says so. Moment by moment:

| Moment | Symbols | Radios | Covered? |
| :- | :- | :- | :- |
| In the custom launcher (any time) | list hidden (custom mode needs no helper) | kept (guard skips) | yes |
| Screen off from the custom launcher | already hidden before the lock screen draws | kept | yes |
| Screen off from an unlocked Quickstep session | the dark screen is a hold moment: the list is written at the screen-off sync (and by the 2 Hz tick), before the wake draws the lock screen | kept (helper is running there) | yes (emulator picture of the lock screen, WF6b) |
| Lock screen, then a plain PIN (decision pending) | stays hidden (`unlockUndecided`) | kept | yes |
| Gesture unlock into Quickstep | list put back exactly as found (by design: Quickstep shows the stock look) | kept | by design |
| A radio turned on while locked (e.g. from a lock-screen tile) | already hidden | stays on | yes |
| Process start (HOME app, boot) with the stored mode CUSTOM | hold taken by the first sync (`assumeHelper`) | kept | yes, except the flash below |
| Lock screen already up before the helper binds, stored mode QUICKSTEP (right after a boot) | not hidden (helper rule of the policy) | switched OFF like v0.9.3 (nothing to show); a gesture unlock puts back what AK1 switched off | yes (this is the review's finding 1; JVM test `radiosStayOnOnlyWhileTheSymbolsCanReallyBeHidden`) |
| Quick-settings tiles on the lock screen / custom launcher | the tile profile already leaves only the rotation tile there | n/a | yes (existing rule) |

NOT covered or unproven, said plainly: (1) a flash of the symbols on the boot lock screen before the app process has started (needs a real reboot to measure; an ACTION_SHUTDOWN hook is not built); (2) the tablet's own MediaTek SystemUI honouring `icon_blacklist` on the lock screen (only a tablet report proves it); (3) Bluetooth on real hardware (the emulator has none; JVM tests + AOSP source only; the slot name `bluetooth` is the framework's `status_bar_bluetooth`); (4) uninstalling the app while the hold is on (the symbols stay hidden everywhere; `adb shell settings delete secure icon_blacklist`, or install the app again: the first sync puts the list back); (5) the Wi-Fi sleep policy (does Android keep the connection with the screen off) was not verified.
