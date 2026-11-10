# BS1: keep Wi-Fi ON on the lock screen and in the custom launcher, but hide its symbol (Tj, 2026-11-10 23:27 UTC)

Tj (verbatim): "Is it possible to keep wifi on when I turn the screen off and it goes to screen lock, but hide the wifi symbol in the lock screen and custom launcher"

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

## Design (TASKS BS1b; code: `WifiIconHold` pure rules, `WifiIconHide` Android side)

- One switch "Keep Wi-Fi on, hide its symbol" (`keep_wifi_hide_icon`; release default ON, debug OFF like `quiet_banners`). Needs the `WRITE_SECURE_SETTINGS` adb grant the launcher already holds; without it nothing changes (the Wi-Fi guards keep switching Wi-Fi off exactly as before, because a visible symbol on the lock screen is worse than today).
- WHEN the symbol is hidden = the same world as the pop-up banner quiet (`NotifQuietPolicy.why`): the custom launcher, a dark or locked screen in ANY launcher, a plain PIN still being decided. An unlocked, decided Quickstep session gets the original `icon_blacklist` back (Tj's "lock screen and custom launcher" only).
- WHAT is written: `Settings.Secure icon_blacklist` = the original list (a missing setting counts as `rotate,headset`) + `wifi`; the original is saved FIRST in `Settings.Global tj_wifi_icon_original` (survives "Clear data", like `tj_banners_original`), and put back only while the value is still ours; a list somebody changed meanwhile only loses our `wifi` token.
- WHAT stops being switched off: the Wi-Fi half of AK1 (`LockQuiet.onLock`, the lock-screen re-check) and of the custom-launcher radio guard / `ReturnReset.radiosOff` (one choke point). Bluetooth rules are unchanged (Tj did not mention Bluetooth). Condition `WifiIconHold.keepsWifiOn(switchOn, grant, automationOff)`: switch on AND grant AND automation not paused (breaker / Restore everything).
