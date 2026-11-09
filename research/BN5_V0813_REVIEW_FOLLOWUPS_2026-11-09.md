# BN5: follow-ups from the second review of the v0.8.13 code (2026-11-09)

Source: a `/code-review high` pass started while BN1 (the Settings look-alike) was being finished; it read the v0.8.13 BN2 / BN3 / BN4 code (home patch, lock patch, pencil cover), NOT the Ss* files.
These are NOT part of BN1 and nothing here was changed by the settings work. Before fixing one, compare with `research/BN2_BN3_BN4_REVIEW_2026-11-09.md` (the first review: 6 fixed, 4 accepted with reasons): a finding may be one of the accepted four.
Each item: file:line, the defect, the failure scenario. Priority guess (Claude): items 1, 8, 10 touch the Screenshot-button cover Tj cares about (BN2); 2 and 4 are test / CI hygiene.


## 1. `app/src/main/java/com/tj/launcher/TjAccessibilityService.java:719`

The BN2 unlock exemption depends on `lastKeyguardUpMs`, which is only refreshed by incidental `keyguardLockedFast()` callers and at USER_PRESENT, so it may be older than 3 s when the keyguard-exit event arrives.

**Failure scenario:** The keyguard's FrameLayout window event comes at the START of the exit, before USER_PRESENT. `keyguardLockedFast()` is not polled on a timer, and `updateLockNumber` skips it when LockNumber is enabled. If the user spent more than UNLOCK_GRACE_MS (3 s) on the PIN pad after the last positive `keyguardLockedFast()` call, `since(lastKeyguardUpMs)` is >= 3000. `foreignTreatment` then returns FOREIGN_NOW, the unconditional 2.5 s guard starts, and the first side-key press after unlock still shows the stock Screenshot entry. The fix then works only for quick unlocks. `isKeyguardLocked()` cannot be asked at event time, because it is already false once the keyguard is going away.

## 2. `tools/emulator_scenario.sh:3045`

HP14 passes vacuously: when no `PATCH window created: resting=false` line exists, `hp-f-after-unlock.txt` is empty, and `! grep -q` on an empty file succeeds.

**Failure scenario:** The awk keeps lines only from the last 'PATCH window created: resting=false' onward (`if (n)`). If that marker is missing or its wording changes, or the log was rotated, the output file is empty and the 'no systemui guard after the unlock' check is green even if v0.8.12's bug is back. Add a precondition that the file is non-empty, or a positive check such as 'unlock window ignored' >= 1. The section's own INFO line shows the count only as information.

## 3. `tools/gate.sh:135`

The new SOLO handling adds the solo runs on top of the requested group count, so `--groups N` yields more than N runs.

**Failure scenario:** With `--groups 2` (the documented option for when Actions minutes are scarce) and `pt` selected, `nb = min(max(2, 2-1), len(units)) = 2` bins plus the solo `pt` run gives 3 runs. With `pt` and `rg` and `--groups 3` it gives 4 runs. The minute-saving option now spends more minutes than before, and nothing says so.

## 4. `tools/ci_publish_shots.sh:35`

Every run force-pushes a fresh single-commit orphan `ci-shots` branch, so parallel runs overwrite each other's screenshots.

**Failure scenario:** CLAUDE.md says gate groups run side by side. Two or three runs with shots=true finish within minutes of each other and each does `git push -f origin ci-shots`. Only the last finisher's PNGs survive, and RUN.txt names only that run. Claude fetches the branch expecting the screens of the run it dispatched and reads another run's pictures, or finds the ones it needed gone. Use a per-run branch or a per-run subdirectory, or fetch and merge instead of forcing.

## 5. `.github/workflows/emulator.yml:32`

`contents: write` is granted to the whole emulator job, so every run (shots=false included) and every step gets a write-capable GITHUB_TOKEN, although only the optional last step needs it.

**Failure scenario:** All steps (the checkout, the local `./.github/actions/results` action, `tools/emulator_scenario.sh` and third-party actions) run with a token that can push to the repo, including `main` if branch protection is off. The comment says 'nothing else writes', but nothing enforces it. A compromised or buggy step, or a scenario script running arbitrary adb and shell, could push. Put the publish step in a separate job with `needs: emulator`, and give only that job write permission.

## 6. `app/src/main/java/com/tj/launcher/TjAccessibilityService.java:3829`

BN3 lengthens the lock-screen finger-away window from 0.9 s to 1.5 s, which widens the window in which a side-key press hits the 'menu opened while the patch was away' path that previously caused the Screenshot flash.

**Failure scenario:** The BE1 comment at line 145 says the 'patch was away' fast path is 'the case behind a Screenshot flash'. After a tap on the lock screen, a side-key press at 0.9-1.5 s used to find the patch back at rest. It now finds it hidden and depends on the dialog's own event to restore it in time. The trade (fewer pin-pad pull flashes, more exposure to this one) was neither measured nor covered by a check. A dedicated emulator check for a tap followed by a side-key press at about 1.2 s is missing.

## 7. `app/src/main/java/com/tj/launcher/TjAccessibilityService.java:2376`

`penConfirmedMs` is never reset, so `hidePencil` can report a stale 'pencil last seen N ms ago' age and write a misleading lock-log line.

**Failure scenario:** `penConfirmedMs` is set only when a pencil is found and never cleared on removal or hide. If the cover is hidden by a path that did not just see the pencil (a `penNode == null` hide from penWatch, or 'no pencil found' after a search, long after the last sighting), `age` is large. On the locked screen `age >= 150` writes a 'cover hidden 40000 ms after the pencil was last seen' LockLog entry, one per second at most. That reads like a white-box bug in the tablet report. Reset `penConfirmedMs = 0` in `hidePencil`/`removePencil`, or log only when the cover was actually up and followed.

## 8. `app/src/main/java/com/tj/launcher/TjAccessibilityService.java:732`

`homeForeignRecheck` returns early when `overlayShown` is true or the patch window is gone, leaving `homeForeignProvisionalUntilMs` set and the real foreign window uncounted.

**Failure scenario:** A provisional guard is armed at an unlock-time SystemUI event, then within 400 ms the power menu opens (overlayShown) or the patch window is rebuilt. The recheck returns without lifting, counting or clearing `homeForeignProvisionalUntilMs`. If the event was a real shade, the 'foreign windows seen' counter in the tablet report (which BI1 uses for diagnosis) misses it. The stale provisional value also lets a later `homeFocusChanged(true)` lift a guard that this event no longer owns, if `homeForeignUntilMs` still equals it. Clear the provisional mark (or count the window) on these early exits.

## 9. `app/src/main/java/com/tj/launcher/TjAccessibilityService.java:3112`

The dying-dialog suppression is armed only by the close-system-dialogs broadcast while `overlayShown`, so a menu closed by Back or by tapping outside, and any scan that races the reopening, are not covered.

**Failure scenario:** `menuClosedAtMs` is set only in the broadcast receiver, and Back or an outside tap on the stock menu sends no such broadcast. The tall card can then still flash from a scan that lists the fading dialog window 100-250 ms after the close. In the other direction, if a new opening's ActionsDialog event is declined or arrives late, a scan inside the 450 ms window is ignored and the card or patch for the genuine reopening is delayed. The mark is cleared only in `fastMenuOpen`, not when a scan sees fresh menu text. The fix is narrower than the symptom it names.

## 10. `app/src/main/java/com/tj/launcher/HomePatchRules.java:32`

`foreignTreatment` treats every SystemUI FrameLayout event within 3 s of the keyguard as the keyguard, and the focus recheck lifts the guard as soon as the Launcher has focus, so a real shade pulled right after an unlock can end up uncovered.

**Failure scenario:** The shade window is also announced as `com.android.systemui android.widget.FrameLayout`. If the user pulls the notification shade within 3 s of unlocking, the event is provisional. If the Launcher still reports `hasWindowFocus()` when the 400 ms recheck runs (the shade is only partly open and has not taken focus yet), `homeForeignLift()` clears the guard and calls `patchShow(false)`. The opaque home patch, a copy of the old Launcher pixels, is then drawn over the shade as it opens. The comment 'nothing can paint over a real window meanwhile' holds only until the lift. The recheck should also require that no foreign window is currently present (for example via `getWindows()`).
