# CB1: the "Device connected to <VPN>" row in the quick-settings panel (Tj, 2026-11-13)

Verdict in one line: **it cannot be removed from the panel by any means open to the launcher (no root); it can only be COVERED**, and a cover leaves an empty strip where the row was (the gear row and the notifications do not move up) and cannot follow a moving panel without a short flash.

Sources read 2026-11-13 by a web-fetch subagent (verbatim excerpts in its report; AOSP `android10-release`, GitHub mirror, raw URLs). VERIFIED = read in the source; NOT VERIFIED = reasoned or from memory.

## What the row is (VERIFIED, AOSP 10)
- `QSSecurityFooter` (SystemUI, `qs/QSSecurityFooter.java`) inflates `layout/quick_settings_footer.xml`: a full-width, clickable horizontal `LinearLayout` (NO id), background `?android:attr/colorPrimary` (the panel colour), padding `qs_tile_padding_top` above and below, `qs_footer_padding_start/end` at the sides; children `footer_text` (TextView, weight 1, `TextAppearance.QS.TileLabel`, `textColorSecondary`) and `footer_icon` (ImageView, `qs_footer_icon_size`, tinted `textColorSecondary`) at the RIGHT.
- Visible when `(isDeviceManaged && !isDemoDevice) || hasCACerts || hasCACertsInWorkProfile || vpnName != null || vpnNameWorkProfile != null`, else GONE. **While it is visible the QS divider line above it is GONE** (`mDivider.setVisibility(mIsVisible ? GONE : VISIBLE)`): before the VPN the panel had a thin divider line at that place, now it has the row.
- Text for one third-party VPN: `quick_settings_disclosure_named_vpn` = "Device connected to %1$s" (%1$s = the VPN app's LABEL, `VpnConfig.getVpnLabel`). Icon: `stat_sys_vpn_ic` (the key; `stat_sys_branded_vpn` only for a SYSTEM app with metadata `com.android.systemui.IS_BRANDED`).
- `onClick`: collapses the panel and opens SystemUI's "Network monitoring" dialog (its VPN part links to the VPN settings).
- Place: the LAST child of `QSPanel` (brightness, tile pages, divider, footer); the pencil / gear bar (`QSFooterImpl`) is below the QSPanel. So the row sits between the tiles and the pencil / gear bar, in the EXPANDED panel only (not in the first pull's row of 6 tiles).
- Animation: `QSAnimator` fades it in with the tiles (alpha 0 -> 1, start delay 0.86 of the expansion), first page only; on the keyguard `setPosition()` returns early (no fancy animation there).
- Detection: `SecurityControllerImpl.updateState()` reads `getVpnConfig(user.id)` for every user on each network callback; nothing an app can change.

## Levers, one by one
| # | Lever | Verdict |
| :- | :- | :- |
| 1 | A setting (Settings.Secure/Global, `icon_blacklist`, demo mode, tuner) | **None exists.** The visibility rule above reads only the VPN / management / CA-certificate state. `icon_blacklist` (BZ1) hides the status-bar key only; `device_demo_mode` changes only the MANAGED-device part of the rule. |
| 2 | Runtime resource overlay (RRO) installed by adb (layout height 0, or empty string + clear icon) | **Impossible for a sideloaded APK (VERIFIED).** `IdmapManager.enforceOverlayable`: always enforced for overlays targeting Q+; a pre-Q overlay must be preinstalled or platform-signed. `calculateFulfilledPolicies` gives a /data overlay only PUBLIC (+ SIGNATURE if signed with the TARGET's key). idmap2 `CheckOverlayable`: a target that declares NO `<overlayable>` (SystemUI on android10-release has no `res/values/overlayable.xml`: 404) accepts an overlay only with ODM / OEM / SYSTEM / VENDOR / PRODUCT / SIGNATURE, never PUBLIC: "overlay must be preinstalled or signed with the same signature as the target". SystemUI is signed with the tablet maker's platform key. This also closes lever 3 of `research/PENCIL_REMOVAL_2026-11-10.md` (the pencil) for good. NOT VERIFIED: PackageManagerService's own install-time overlay checks (the 1.1 MB file was not read); they can only make it stricter. |
| 3 | The VPN app | Nothing: the row is SystemUI's, shown for ANY app that uses Android's VPN service; only its name comes from the app's label. |
| 4 | Accessibility (what the launcher has) | Can read the row (its text) and draw windows over it; **cannot hide, remove or resize SystemUI's views.** A cover in the panel colour hides the text and the key. It cannot close the gap: the strip stays as tall as the row (about one text line plus top and bottom padding; NOT MEASURED), the gear row and the notifications stay where they are now. The thin divider line of the old look could be drawn on the cover. |

## What a cover would look and feel like (reasoned from the pencil cover's history, BN4 / BN8b / BQ1b)
- At rest (panel open and still): the row is gone, a plain strip in the panel colour is there instead (optionally with the old thin line).
- While a finger moves the panel: the pencil cover's rules take the cover away at finger down and bring it back when SystemUI is quiet (otherwise it trails the panel as a coloured bar over the lock screen / notifications, Tj's "white box", BN4). The row would therefore SHOW for a moment while pulling the panel down or pushing it up, and on the first open until the panel is still.
- A tap on the strip: a touchable cover swallows it (the "Network monitoring" dialog no longer opens); a drag that starts on the strip is swallowed too.
- Places: the lock screen, the custom launcher AND Quickstep (Tj names all three; today's pencil cover runs only in the custom world + lock screen).
- Dark theme (battery saver in a launcher): the cover must take the panel colour of the current theme (the pencil cover's colour rule).

## Tj's answer (CB2, 2026-11-13): "If it can't be removed, make the launcher automatically turn off my VPN any time the screen turns off or the lock screen is on"
No cover. The VPN itself goes off while the tablet is dark or locked.

## How an app turns ANOTHER app's VPN off on Android 10 (VERIFIED, AOSP android10-release, web-fetch subagent 2026-11-13)
- `VpnService.prepare(ctx)` = `IConnectivityManager.prepareVpn(callerPkg, null, user)`; null result = prepared.
- `ConnectivityService.prepareVpn`: `enforceCrossUserPermission(user)`, then `throwIfLockdownEnabled()` (system LEGACY lockdown VPN: `IllegalStateException("Unavailable in lockdown mode")`), then `Vpn.prepare(old, new)`.
- `Vpn.prepare(oldPackage = us, newPackage = null)`: (1) `if (mAlwaysOn && !isCurrentPreparedPackage(us)) return false;` = an Always-on VPN can never be dethroned; (2) not the current package and `isVpnUserPreConsented(us)` (app-op `OP_ACTIVATE_VPN` == MODE_ALLOWED for the CALLING uid) -> `prepareInternal(us)`, return true. No CONTROL_VPN check on this path (it is only on the newPackage path used by the consent dialog).
- `prepareInternal(us)`: tears the interface down (agentDisconnect), sends `LAST_CALL_TRANSACTION` to the old app's VpnService (= its `onRevoke()`), unbinds it, or `mLegacyVpnRunner.exit()` for a built-in (L2TP / IPsec) VPN; then `mPackage = us`, state IDLE.
- Consent: default mode of ACTIVATE_VPN is MODE_IGNORED. Two ways to allow it: Android's own `ConfirmDialog` (started with startActivityForResult from the Intent prepare() returns; OK calls `prepareVpn(null, us)` = revokes the running VPN ONCE, then `setVpnPackageAuthorization(us, true)`; Cancel does nothing; it closes at once if another app is Always-on) or adb `appops set com.tj.launcher ACTIVATE_VPN allow` (the shell command's name for the op: NOT read in source; the CI run proves it).
- After the launcher took the VPN over it is the "prepared" package with NO interface; Tj's VPN app, already consented, takes it back silently the next time it connects (no dialog).
- Cost: one binder call per trigger, only when a VPN network is up (`ConnectivityManager.getAllNetworks()` + TRANSPORT_VPN, which lists every network).

## CE1 (2026-11-15): the VPN row on the lock screen came back; LinkMeter and VpnOff fought, now ONE rule
**What Tj saw:** "Device connected to ..." on the lock-screen panel again. **Cause (read in both repos, not seen on a device):** LinkMeter v0.2.183 put its VPN back 2 s after
every revoke (at most 3 in 10 min); `VpnOff` revoked it at every screen-off; after three stops `VpnOffRules.GAVE_UP` left it, so the row stayed. `VpnOff` also never acted in
the unlocked custom launcher, where Tj says the row must never show either.
**New rule (Tj's four sentences, 2026-11-15):** a VPN may run ONLY in an unlocked, decided Quickstep session; it is off at screen off, on the lock screen (any launcher), in
the custom launcher and while a plain PIN is undecided. That is exactly `NotifQuietPolicy`'s "quiet" world (the banner switch BH1 and the radio symbols BS1 use it), so
`VpnOff` is one more consumer of `AnimationSpeedup.syncNow` (level-triggered, same triggers) instead of reading its own facts at its own call sites.
**The contract with LinkMeter:** the Launcher PUBLISHES the world as `Settings.Global tj_launcher_world` = `quickstep` | `quiet:<custom launcher|screen off|lock screen|unlock
not decided yet>` (`VpnOffRules.worldValue`; written on the worker thread, only when it changes, needs the WRITE_SECURE_SETTINGS grant; removed when the switch is off or
on Restore everything). The word is written BEFORE the VPN is revoked, so LinkMeter reads "quiet" when `onRevoke` arrives and PAUSES (holds its VPN down, keeps Tj's choice,
never calls `VpnService.prepare`, does not count pauses as revokes) instead of putting the VPN back. It resumes by itself when the word turns `quickstep` (observer + screen /
unlock receiver + 30 s level look). Missing word (no grant, Launcher not running) = LinkMeter uses its own screen + keyguard facts; unknown word = may run (fail open).
**What stays the same:** the revoke mechanism (`VpnService.prepare` with the launcher's own app-op, above), the cap (3 stops per 10 min, `GAVE_UP`; it now counts only stops
in a quiet world), the switch (Launcher control > Lock screen, renamed "No VPN on the lock screen or in the custom launcher"). **Limits:** a third-party VPN app that
reconnects by itself forever still wins after 3 stops (nothing else can be done on Android 10); with no adb grant the custom launcher cannot signal LinkMeter, but the Launcher
still revokes there and LinkMeter then pauses on its first revoke only if it sees quiet facts (it does not in the custom launcher without the word: it would put the VPN back, up
to 3 times).
**Proof:** JVM `VpnOffRulesTest` (13 tests incl. the world word and every situation), emulator section `vo` (phases F / H extended; CE1c).

### CE1 read-only review of the diff (2026-11-15, neutral brief, raw findings; verdicts are written below as each is handled)
1 MED: Launcher control's VPN toggle does not call syncNow; switch OFF in a quiet world leaves the word `quiet:*` (LinkMeter stays paused); poll dies on !switchOn with quietWorld still true; poll armed only once at `began`, so tapping "Allow" later gives no poll.
2 MED: helper missing / automationOff = "not quiet" -> word flips to `quickstep` on a lock screen when the accessibility service unbinds, or at a process restart between the assumeHelper sync and the +2.5 s sync; endQuiet resets stop counters in between.
3 MED: MAX_STOPS / GAVE_UP and `refusedThisLock` were made for a lock lasting minutes; the custom-launcher world lasts all day (give-up then row shows ~10 min; one refusal blocks the whole day).
4 LOW-MED: `run()` acts on a world snapshot taken on the pipeline thread; a gesture unlock in the gap stops Tj's Quickstep VPN. Re-read the world on the worker right before `prepare`.
5 LOW: syncSoon coalescing keeps the first trigger ("window change" suppresses check): a poll / "a VPN connected" look can be swallowed.
6 LOW: Restore everything can leave a stale `quiet:*` word (in-flight sync publishes after retract; retractChecked already true).
7 LOW: a failed word write does not stop the revoke that follows (LinkMeter reads the old `quickstep` and reconnects at once, burning stops).
8 LOW: `worker()` and `watch()` share the VpnOff class monitor; `registerNetworkCallback` binder call under it can block the main thread; failed registration retried at every sync.
