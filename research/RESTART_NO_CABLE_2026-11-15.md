# What survives a tablet restart with NO adb cable (TASKS CF1a; read-only audit 2026-11-15, subagent with a neutral brief, 71 tool calls)
Tags: [code] read in source, [doc] a repo document says so, [platform] Android behaviour not proven by the repo. Newest REAL tablet restart proofs: 2026-11-05/06 (v0.7.x); newest tablet report v0.9.1 (before the VPN / Fast Wi-Fi world-word features).
Two meanings of "helper": the ACCESSIBILITY SERVICE (`TjAccessibilityService`, Android starts it) and the adb-started SHELL helper (`ShellServer`). Do not mix them.

## CONTINUES (no cable)
- Runtime grants WRITE_SECURE_SETTINGS, DUMP, CHANGE_CONFIGURATION, READ_EXTERNAL_STORAGE survived a real restart [doc TASKS.md:513-514, STEP 22; AC_DESIGN:10]; the v0.9.1 report still shows them [doc tablet-report 608-614].
- App-ops RUN_IN_BACKGROUND, RUN_ANY_IN_BACKGROUND, GET_USAGE_STATS, WRITE_SETTINGS and the battery whitelist stayed [doc TASKS.md:514].
- The accessibility service is bound by Android by itself after a real restart, about 1.5 min after boot [doc TASKS.md:491 STEP 15, HANDOFF.md:35; code TjApplication.java:29].
- The Launcher has no boot receiver; it runs as the Home app or when the service binds [code TjApplication.java, RouterActivity.java:47-49]. Default Home survived [doc AC_DESIGN:10].
- First Home after a new boot ends a Quickstep session and sets custom (not while the crash breaker / Restore everything paused automation) [code ProfileSession.java:109-120, CrashGuard.java:92-94].
- Holds (animations, banners, radio symbols, the VPN world word) are re-decided at process start (`reconcileAsync`) [code TjApplication.java:23, AnimationSpeedup.java:197-225]. Originals of banners / radio symbols live in Settings.Global and survive Clear data [code NotifQuiet.java:29, RadioIconHide.java:28-29].
- Lock-screen quieting (radios, tile list) runs from the app process before the service binds [code LockQuiet.java:130-146]. Screenshot patches use the accessibility service, not the shell helper [code TjAccessibilityService.java:4896, 5085].
- The tablet already runs WITHOUT a shell helper today (v0.9.1 report: "shell helper reachable: no", service bound 121 min, holds working) [doc tablet-report 620-634].

## STOPS or DEGRADES (fails safe)
- SHELL helper: nothing in the app starts it; it needs an `adb shell ... app_process` line; HANDOFF.md:267 "dies at reboot"; it was killed within ~15-27 min anyway on v0.7.x; the app cannot reach it today (HLP1/HLP2 open) [code ShellProtocol.java:352-362; doc TASKS.md:449-481].
  Costs: invisible sweeps by task id are skipped (unlock cleans on screen behind a cover), lock-screen model-fit calibration "wallpaper model used meanwhile", "Do everything that is missing" / in-app grants unavailable [code ReturnReset.java:89-94, RecentsSweep.java:142, TaskOrigin.java:146, TjAccessibilityService.java:1339, ShellProtocol.java:199-203].
- Until the service binds (~1.5 min): no secret handshake, lock-number overlay, power-menu patch, pencil cover; a plain PIN still unlocks (into custom); the three-Home failsafe works only while the service is NOT running [code RouterActivity.java:61]. A lock screen up before the service binds, in stored Quickstep mode, still switches radios off [doc RADIO_ICONS_HIDE:56].
- PACKAGE_USAGE_STATS is not granted today: the usage-based session safety net and the Settings look-alike's battery numbers are already empty and fail quiet [doc tablet-report 612; code UsageGap.java:40-54, QuickstepUsage.java:56-74].
- A reboot taken from a Quickstep session can leave the animation scales at 0, banners off, the radio-symbol list hidden or the lock-screen notification setting at 0 until the launcher process runs again (an ACTION_SHUTDOWN hook is "not built") [doc RADIO_ICONS_HIDE:40]. Nothing outside the app puts them back.
- Without the WRITE_SECURE_SETTINGS grant every hold and the world word stop; a stale word stays until the switch is turned off or Restore everything [code VpnOff.java].
- Battery saver restore is refused on a different boot epoch [code BatterySaverGuard.java:114, BatterySaverPolicy.java:143,194].
- LinkMeter Fast Wi-Fi after boot: its BootReceiver restarts it if wanted and Android's VPN consent holds; it pauses on its own screen + keyguard facts; a stale `quiet:*` word keeps it paused until the launcher republishes, a stale `quickstep` word does not pause it [code LinkMeter BootReceiver.java:34-35, FastVpn.java].

## NOT PROVEN
- ACTIVATE_VPN app-op across a reboot (belief: appops.xml persists) [platform]; timing of the first `tj_launcher_world` write after boot; the VPN-off and Fast Wi-Fi pause features on the real tablet (all "read in code, not seen on a device"); icon_blacklist on MediaTek's lock screen; Bluetooth hardware; whether the shell helper is reachable on v0.8+ (HLP1 open); a full-option reboot on the MT8168; the notification-access grant (STEP 17); self-adb (persist.adb.tcp.port is empty).
