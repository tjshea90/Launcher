# v0.9.18 (CW1) — the answer to Tj, draft (2026-11-23)

Written BEFORE the release and checked against the code and the run list (TASKS CW1c / CW1e). Claim only what a run or a JVM test proved; "reasoned only" and "only the tablet can show" stay apart.

## Release link
(filled in after the build: `https://github.com/tjshea90/Launcher/releases/tag/v0.9.18`, plain text on its own line)

## What I built (all without taking anything away: no feature, speed or look was given up)
Speed
- **The switch between the custom and the Quickstep side counts as ready earlier (A1).** Before, it waited for the whole load of the launcher (icon cache refresh, deep shortcuts, widgets, three idle waits), none of which you can see. Now it is ready as soon as the home and the app list are on screen. JVM tests (`Cw1RulesTest`, `UnifiedRules`) + the emulator sections `ul`, `ul2` pass. How many milliseconds it saves: only the tablet can show (the emulator is too noisy for a number).
- **The unlock / handshake waits less (A4, A6, A7):** the wait loop looks at the cheap ready flag before it fetches the accessibility tree every 40 ms; no stock-launcher lookup before it is needed; one preference write instead of three. JVM tests.
- **Cached lookups (A9, A10):** the stock-launcher lookup, the per-app filter preferences. JVM tests.

Screenshot-button hider (home screen)
- **The first picture of the patch is built sooner (B1a):** after 100 ms of quiet instead of 250 ms, so the first power-menu press after an unlock, a return or a double Home finds it ready more often. A late draw still marks it stale, so a wrong picture is never shown. JVM tests; emulator `hf` green (solo, three runs), `hp` green.
- **No pencil search in front of the dialog's own event (B3):** the quick-settings pencil search no longer runs before the power dialog's first event on the home screen (it averaged 33 ms and took up to 1305 ms on your tablet, and it delayed the start of the fade). JVM tests; `hf` green.

RAM / CPU / battery
- Idle worker threads time out (C6); icons of hidden apps are no longer kept in memory (C8); the stock-launcher bookkeeping no longer spawns commands that the app is not allowed to run anyway (D7) and uses one delayed post instead of two sleeping threads (D11).

Stock launcher
- **It cannot be put to sleep (D2).** SystemUI binds its touch service for good; a kill is re-bound within seconds (runs #714 / #715). Nothing may disable its HOME activity or that service. Written into HANDOFF so nobody tries again. What can be done, I did: with the one launcher it no longer counts a Quickstep session or a battery-saver flip as "pinned memory" (D3), only the screens that really pin; one cleanup after the switch to the one launcher (D5); the Recents clear-all waits from the stock launcher's first window instead of a fixed time, hard cap 6 s (D4). JVM tests + emulator `sf`, `cs`, `rg` green.

## What I did NOT build, and why
- The hider on the drawer, apps in front and an unready home (B2), the card made touchable later (B4), the page-step window kept (B9) and the other B items: they touch the card and lock-patch paths and need your tablet numbers first. The pure rules for B2 / B4 / B9 are written and tested, NOT wired.
- The VPN check (C4): the 30-second poll IS the function on Android 10 (its callback is blind to a VPN that excludes the launcher).
- A2, A3, A5, C3, D6, D8: deferred until a tablet number says they matter.
- **The lock screen's hider was not changed:** the audit found it already well prepared; the gaps were on the home screen.

## What the tests proved, and what they did not
- Unit tests (full suite + lint + handoff self-test) green. Emulator: every section the diff touches passed, some only after a solo rerun (three failures were test premises of the old two-launcher world, the rest known emulator crashes / stalls; the same checks fail the same way on the v0.9.17 tag). Details: TASKS CW1e.
- NOT provable on the emulator: how much faster anything feels, the real RAM / battery effect, the home hider's first press on your tablet.

## Questions for you
1. B17: should the Screenshot-button hider also run in the unlocked (full-app) home? Today it does not, by the one-launcher design.
2. C1: keep the secret-code key filter on all day (it is the lock-out safety net, 0.1-0.3 ms per key press), or only while the screen is locked?
3. After a day of v0.9.18: Settings > Developer options > Memory > "Quickstep" (3 h and 1 day average), and whether the first power-menu press on the home screen still shows the Screenshot button even once.
