# CW1 findings and fix list (Tj, 2026-11-22 18:01; written 2026-11-22 after the four audits)

Inputs: `research/CW1_AUDIT_A_SPEED_*`, `_B_PATCH_*`, `_C_RESOURCES_*`, `_D_STOCK_SLEEP_*` (all 2026-11-22, read-only reviews, every cost marked [V] code / [I] reasoned / [T] only the tablet can show).
Rule from Tj: nothing may lose speed or function. A fix with a real risk to the lock paths is built only with its emulator section; a fix that needs a tablet number first is NOT built.

## What the audits found, in one paragraph each
- **Speed (A):** no big defect; the felt costs are (A1) a profile flip counts as "ready" only after the whole LoaderTask (icon cache, deep shortcuts, widgets, idle waits), so the cover and the unlock wait for invisible work; (A4) `settleHome` fetches the accessibility root every 40 ms for up to 4.5 s before it looks at the cheap ready flag; (A2/A3) double Home and the return clean-up do their bookkeeping on the main thread before / right after the Launcher starts; small waste on the handshake path (A6, A7), uncached stock lookups (A9), per-app preference chains (A10).
- **Screenshot-button hider (B):** the lock screen is well prepared; the HOME is not: (B1) the first press after an unlock, a return, a double Home or a page step finds a stale or unbuilt composite (the composite is built only after 250 ms of quiet; the prime runs only after a tile / drawer-icon return); (B3) the home flavour runs the quick-settings pencil search SYNCHRONOUSLY on the first event of the burst, before the dialog's own event (avg 33 ms, max 1305 ms on the tablet) and delays the fade anchor; (B2) the drawer, apps in front and an unready home have no real-pixel patch, so the slow path (scan 28-563 ms) runs with the stock entry visible; (B4) the card is made touchable in the same message as its first visible frame; (B8) the open look is drawn cold at the first fade; (B6/B7/B9) rotation, service restart and a mid-scroll wallpaper step leave the window missing or cold.
- **RAM / CPU / battery (C):** no HIGH finding. The steady state is event-driven: no AlarmManager, JobScheduler, WakeLock or Timer; 2 free-running timers (VpnOff 30 s, helper BEAT 30 s), ~55 receivers / observers / listeners, 1 foreground service (off by default). Smaller items: C4 VpnOff poll, C6 idle threads never time out, C8 hidden apps' icons kept in memory, C5 binder calls per layer refresh.
- **Stock launcher (D):** it CANNOT be put to sleep: SystemUI binds its TouchInteractionService for good (adj 100, never cached, a kill is re-bound in seconds, runs #714 / #715). The achievable goal is the smallest footprint and the fewest wake-ups. CV1 already removed the main cause (the stock HOME activity is not started any more). What is left is a stale trigger: the Settings Force stop (SR2) debt model still counts every ended Quickstep session and every battery-saver flip as in the two-launcher world (D3).

## Decisions
| Id | What | Decision | Package |
| :- | :- | :- | :- |
| A1 | "profile ready" = apps bound, not the whole load | DO (pure rule + `ul`/`ul2`) | X |
| A4 | check the ready flag before the 40 ms root fetch | DO | X |
| A6, A7 | handshake: no eager stock lookup in the unified world; one commit instead of three | DO | X |
| A9, A10, A11 | cache the stock lookup, per-load filter prefs, per-press fixed cost | DO | X |
| A2, A3 | double Home / return clean-up bookkeeping off the main thread | DEFER: order of side effects on a lock-adjacent path; needs tablet timing lines "HOME custom home in front +N ms" first | - |
| A5 | plain PIN: start the reload at the PIN submit | DEFER: lock-safety, needs design (MED-HIGH risk) | - |
| A8 | unlocked profile runs custom-only patch work per draw | DO only the guard that is free (skip two keyguard binders when no patch world) | X |
| B3 | no synchronous pencil search before the dialog's own event | DO | Y |
| B1 a/b/c | build the home composite earlier, keep it per page step, prime after a plain unlock | DO | Y |
| B2 | fast card on the dialog's own event (drawer, apps, unready home) | DO | Y |
| B4, B10, B11, B13 | card touchable later, logs after the frame, cache the open-look filter, cache wallpaper id | DO | Y |
| B6, B7, B8, B9 | rotation refresh, dark-settle on restart, warm first draw, keep the home window across a step | DO if the section can prove it, else note | Y |
| B5 | lock without a crop: calibrate at the first quiet moment | DO (a) only | Y |
| B12, B14, B15, B16 | | skip (bounded, rare, or the helper is off) | - |
| B17 | hider in the UNLOCKED profile (shows stock Screenshot entry by design, CV1 "no patches") | ASK Tj | - |
| C1 | Konami key filter on all day (0.1-0.3 ms per input event [I]; it is the lock-out failsafe) | ASK Tj | - |
| C2 | narrow the a11y event types | NO (the patches depend on them) | - |
| C3 | profile flip = full reload (0.3-1 s [I]) | MEASURE on the tablet first | - |
| C4 | VpnOff: use the existing NetworkCallback + 5 min backstop | DO | X |
| C6 | idle executor threads time out | DO | X |
| C8 | do not cache icons of hidden apps in memory | DO | X |
| C5, C7, C9, C10 | | skip (small / risky / not the app's process) | - |
| D3 | SR2 debt only counts what can pin; "stock window seen" flag; configuration signature | DO | Z |
| D5 | one seeded cleanup after the switch to the one launcher | DO | Z |
| D4 | Recents clear-all clock starts at the first stock window; "cold start +N ms" in the log | DO | Z |
| D7 | no refused dumpsys spawns without the helper | DO | Z |
| D11 | one handler post instead of two sleeping threads | DO | Z |
| D2 | never disable the stock HOME activity or TouchInteractionService (written into HANDOFF) | DO (text only) | Z |
| D6, D8 | late stock pre-arm; alias flip | DEFER until a tablet number says it matters | - |
| D1, D9, D10 | expectation / report line / manual memory number | tell Tj; ask for the Developer options memory number | - |

Packages: X = speed + resources (several files), Y = the hider (TjAccessibilityService patch / home code), Z = stock-launcher sleep (StockForceStop, StockRefresh, QsMemory, Recents clear-all). Each is built with JVM tests where a rule is pure, reviewed read-only on its delta, then gated with the emulator sections its diff executes (lock core when lock / patch code changed). One release.

## Questions for Tj (answer when the release is announced; they change nothing that is built now)
1. B17: should the Screenshot-button hider also run in the UNLOCKED home (the full-app mode)? Today it does not, by the CV1 design.
2. C1: keep the secret-code key filter on all day (it is the lock-out safety net) or only while locked?
3. Tablet numbers: Settings > Developer options > Memory > "Quickstep" (3 h and 1 day average) after a day of v0.9.17; the newest tablet report is v0.9.1.
