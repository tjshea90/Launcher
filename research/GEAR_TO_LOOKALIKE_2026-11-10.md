# BR1: the settings gear in the notification panel opens the Settings look-alike (Tj, 2026-11-10 19:27 UTC)

Status: facts read from AOSP `android10-release` (raw.githubusercontent.com/aosp-mirror/platform_frameworks_base, 2026-11-10); design below; result and proof are appended at the bottom as they arrive.

## What the gear is (AOSP 10 SystemUI, verbatim facts)
- `packages/SystemUI/res/layout/qs_footer_impl.xml`: the footer is ONE row (`QSFooterImpl`, height `qs_footer_height`). Two weighted containers:
  - LEFT (`qs_footer_actions_edit_container`, gravity start): the pencil `@android:id/edit` (`AlphaOptimizedImageView`, `qs_footer_action_button_size` square, contentDescription "Edit order of settings.").
  - RIGHT (`qs_footer_actions_container`, gravity end): `multi_user_switch`, then `settings_button_container` (a `qs_footer_action_button_size` square) holding `com.android.systemui:id/settings_button` (`SettingsButton`, contentDescription **"Open settings."**) and an invisible `tuner_icon`.
  - So the pencil and the gear are NOT neighbours: the pencil is at the left end of the footer, the gear at the right end. Both are vertically centred in the same row and have the same size, so their offset is constant (dx depends on the footer width = rotation / density; dy = 0 in theory, measured anyway).
- `QSFooterImpl.updateVisibilities()`: `mEditContainer.setVisibility(isDemo || !mExpanded ? INVISIBLE : VISIBLE)` and `mSettingsButton.setVisibility(isDemo || !mExpanded ? INVISIBLE : VISIBLE)`: the gear and the pencil appear and disappear TOGETHER (same condition), and **`device_demo_mode` removes both** (this matters for BR2: the demo flag would also remove the gear that BR1 redirects). `mSettingsContainer` is GONE when quick settings are disabled by policy.
- `onClick`: `if (!mExpanded) return;` then, when the user is set up, `startSettingsActivity()` = `mActivityStarter.startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS), true /* dismissShade */)`. Long press: `SettingsButton.TUNER_ENABLE_AVAILABLE = false` in Android 10, so a long press does nothing special.
- The stock path collapses the shade itself (`dismissShade = true`); an activity started from outside does not.

## Why not the other ways
- **A manifest handler for `android.settings.SETTINGS`**: stock Settings' filter has priority 1; non-privileged apps are clamped to priority 0, so the resolver always picks stock Settings and never shows a chooser. Dead on arrival.
- **Redirect when the stock Settings window appears**: a flash of the real Settings, a loop risk (other flows open it legitimately) and no way to tell the gear from a notification action. Rejected.
- **Accessibility cannot change another app's click handler**; it can only click, cover or overlay. So: a transparent touchable overlay over the gear.

## Design (v0.9.3)
- Switch `gear_to_lookalike` (release ON, debug OFF like every new Tj-facing behaviour; Launcher control entry in the "Quick settings" group).
- Wanted only when: switch on, custom mode, tablet UNLOCKED (the lock screen keeps the stock gear: the look-alike must never open over the lock screen without the PIN), the Settings look-alike switch on (`SettingsSpoof`, whose alias exists only in the custom mode).
- The overlay rides on the pencil cover (same find / follow / guards: `PencilRules.SlideGuard`, `FingerGuard`, stale looks): it is shown exactly while the cover is shown, hidden (untouchable, kept warm) exactly when the cover is hidden. Position = pencil bounds + a learned offset (one gear-only search the first time, again after a rotation / density change). Consequence: **it needs the pencil cover switch to be on** (documented in the Launcher control text).
- On a tap (DOWN and UP inside, finger moved less than the touch slop): collapse the shade (`StatusBarManager.collapsePanels()` through reflection; the manifest already holds the normal permission EXPAND_STATUS_BAR), then start the look-alike with the same intent the drawer uses (`ACTION_MAIN` + `CATEGORY_LAUNCHER` + the `SettingsAlias` component, `NEW_TASK | RESET_TASK_IF_NEEDED`). If that throws (alias not enabled yet), start the real Settings (`Settings.ACTION_SETTINGS`), i.e. what the gear would have done.
- Known gaps (said to Tj): a tap within roughly half a second after pulling the panel or after touching it goes to the real gear (the cover logic keeps the overlay away while the panel may move); the lock-screen panel keeps the stock gear.
