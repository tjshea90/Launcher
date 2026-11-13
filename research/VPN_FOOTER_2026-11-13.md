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

## Decision needed from Tj (asked 2026-11-13)
Cover it (the strip, the short flash while the panel moves, taps on the strip do nothing) or leave it as Android draws it.
