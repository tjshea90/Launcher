# CG1a: every path that shows the custom home while Tj was in Quickstep

Read-only investigation of 2026-11-17 (no code, no gradle, no CI, no git writes except this file).
Question (TASKS.md CG1 / CG1a; INBOX.md:1620, 2026-11-17 18:47 UTC), verbatim:
"Sometimes the launcher randomly shows the custom launcher, even though i was in quickstep. It isn't a big deal but a little annoying."

Evidence tags used on every statement:
- **[V]** verified by reading the cited file:line (code, manifest, scenario, TASKS / BUILDLOG, or a saved tablet report line).
- **[I]** inferred: Android framework behaviour that is not in this repo, or a judgement about how often something happens.

Paths are relative to the repo root. `TAS` = `app/src/main/java/com/tj/launcher/TjAccessibilityService.java`. All other Java files are in `app/src/main/java/com/tj/launcher/`. Line numbers are those of the working tree on 2026-11-17.

---

## 1. Result in one screen

1. **There is no guard for "custom home in front while the stored mode is Quickstep".** The only stray-home guard works in the other direction and returns at once unless the mode is custom (`TAS:3064`) [V]. No other window check on our Launcher exists (every `Mode.isCustom` use was read) [V].
2. **Nothing is logged when the custom Launcher comes to the front.** `TjHooks.onLauncherResumed` (`TjHooks.java:84-102`) writes no line; `TjHooks.onLauncherCreated` (`TjHooks.java:167-171`) writes "created (locked=..., process up ... ms)" without the mode [V]. So the paths of table B are silent, and the four saved tablet reports cannot contain a real instance (section 5) [V].
3. **`Mode.set` never logs the flip itself.** Of its 15 call sites (appendix A) the ones in the lock / unlock code and the two RECONCILE paths write a lock-log line next to the flip; the flips caused by the router's double Home (A1), a new boot (A4), every return method (A6), the crash breaker and the Control screen are SILENT (at most a text in a preference such as `failsafe_last`) [V].
4. **Most likely cause of "sometimes, randomly, mild" (B1, [I] for the framework half):** after a lock in a Quickstep session with an app open, the lock-screen preparation puts the custom home on top of the HOME stack; the gesture unlock then resumes the APP above it (AD5), nothing raises the stock home again; leaving the app with Back (or when it ends) makes Android resume the top HOME task = the custom home, with no Home intent, no router, no mode change, no log.
5. **Second: double-Home by lag (A1):** the router counts two Home intents as a double when they are PROCESSED less than 900 ms apart (`RouterActivity.java:56-58, 65`, `HomeDoublePress.java:5-10`); a main-thread stall or a cold process makes two taps 1-2 s apart look like a double. There is no switch to turn it off (`ControlActivity.java:316`) [V]. This one also closes Quickstep's apps.
6. **Third: RECONCILE (A2/A3) became live on 2026-11-13 ~17:00 UTC**, when PACKAGE_USAGE_STATS was granted on the tablet (TASKS.md:1296-1297); before that `UsageGap` was inert there (reports 2026-11-10 and 2026-11-10-2 line 612: "PACKAGE_USAGE_STATS: NOT granted"). Tj's message is 4 days later [V for the dates, I for the link].
7. The saved reports contain **no instance** of the symptom. All four were taken in Quickstep mode (`mode: quickstep`, line 618/619) and all predate the usage-access grant [V].

---

## 2. Ranking (likelihood that it is the reported symptom) with the smallest fix and the proof line

| Rank | Id | Where | Trigger | Likelihood | Smallest fix | Log line that proves it on the next tablet report |
| :- | :- | :- | :- | :- | :- | :- |
| 1 | B1 | `TAS:1869`, `3238-3261`, `2044` | lock in Quickstep with an app open, gesture unlock (AD5 resume), then leave the app with Back / it finishes | occasional (every such session that is left by Back) [I] | bounded reverse guard `STRAYCUSTOM` (twin of `guardStrayHome`) that sends the stock home back; root fix later: router starts stock home THEN the app | `LAUNCHER resumed while mode quickstep (locked=false, last unlock N ms, fg before=<pkg>)` then `STRAYCUSTOM ...` |
| 2 | A1 | `RouterActivity.java:65-69` | two Home intents processed < 900 ms apart (stall, cold process, impatient re-press) | occasional [I] | first only a log line; then a lag-aware rule decided with data | `HOME2 double Home -> custom: gap N ms, process up P ms` |
| 3 | A2 / A3 | `RouterActivity.java:71-75`; `TAS:328, 2011-2022`; `UsageGap.java:61-79` | the helper was not connected at an unlock, then a Home press or the helper's reconnect finds KEYGUARD_SHOWN newer than the session start; live since 2026-11-13 | rare to occasional, high impact (apps are closed) | RECONCILE only when the helper was NOT connected when the keyguard was shown; add the three times to the log line | `RECONCILE ...: shown=HH:mm:ss.SSS ref=HH:mm:ss.SSS helperUp=HH:mm:ss.SSS` |
| 4 | A6 | `BackToCustomActivity.java:11`, `ReturnTile.java:21-27`, `ProfileSession.java:127-136` | accidental tap of the "Back to custom" quick-settings tile (one of his three Quickstep tiles) or icon | occasional [I] | pass `via=tile|icon`, write a lock-log line | `RETURN via tile` / `RETURN via icon` |
| 5 | A5 | `TAS:1981-1988` | handshake not recognised at the Enter tap (pad events late or dropped) -> plain unlock ends the session | rare | none (already logged); widen only if the next report shows it | `ENTER lag=..ms lead=.. handshake=none` + `UNLOCK plain -> custom (a Quickstep session ended)` (exist today) |
| 6 | B3 / B2 | `TAS:1966-1975, 2032-2047`; `ProfileSession.java:74` | cold or killed stock launcher at the gesture unlock, or the pre-arm router start arriving late | rare | add a "gave up" line after the last VERIFY | `VERIFY attempt N: the custom launcher is in front after a Quickstep choice ...` (exists) |
| 7 | B4 | framework | stock launcher force-stopped / crashed while in front, Recents Back | rare, deterministic | covered by the B1 guard | `LAUNCHER resumed while mode quickstep` |
| 8 | A4, A7, A8, A9 | see 3 | new boot; lock-time clean-up flip; unreadable preferences; withdrawn handshake | rare / by design | the `MODE` line below | `MODE quickstep -> custom by <caller>` |

Why this order [I]: Tj's wording is mild ("shows the custom launcher", "not a big deal"). Every A-path except A4 runs `ReturnReset.run` (apps closed, notifications cleared, radios off; `RouterActivity.java:95-96`, `ProfileSession.java:135`, `TAS:2019`, `TAS:1985`), which he would probably have mentioned. The B-paths only show a home screen. B1 also fits "sometimes": it needs a lock cycle with an app open AND leaving that app by Back instead of Home, and it is silent, which is why no earlier report shows it.

**Cross-cutting change that answers CG1a's own request ("a lock-log line that says WHY"), about 25 lines, no behaviour change:**
1. `Mode.set` (`Mode.java:67-86`): after the commit, when the value changed, `LockLog.addAsync(c, "MODE", from + " -> " + to + " by " + caller)`, and keep the last six flips in a Features key `mode_flips` (the tablet report prints every Features key: `failsafe_last`, `guard_count`, `qs_tiles_quickstep` etc. at 2026-11-10-2 lines 663-705 [V], so a flip survives the 100-line lock-log window). `caller` = first stack frame outside `Mode` (readable: R8 is off, `app/build.gradle.kts:42` `isMinifyEnabled = false` [V]); or add a `why` argument to the 15 call sites.
2. `TjHooks.onLauncherResumed` (`TjHooks.java:84`): when `!Mode.isCustom(c)`, `LockLog.addAsync(c, "LAUNCHER", "resumed while mode quickstep, locked=<km>, last unlock <ms> ms ago, fg before=<pkg>")`; and add the mode to the "created" line (`TjHooks.java:170`).

---

## 3. Facts the analysis rests on

### 3.1 Modes and writers
- Stored in `tj_mode` preferences, key `mode`, default CUSTOM when absent (`Mode.java:59-62`) [V]. `Mode.set` uses `commit()` and always rewrites `changedAt`, even when the value is unchanged (`Mode.java:67-86`) [V]. No other class writes `tj_mode` (grep for `tj_mode` / `"mode"`: only `Mode.java` and the helper's change listener `TAS:587-596, 625`) [V].
- Session start reference for the safety net: `Mode.sessionRefMs` = max(`changedAt`, `qsConfirmedAt`) (`Mode.java:40-45`); `confirmQuickstep` is called at the accepted handshake (`TAS:1800`) and at the handshake unlock (`TAS:1971`) [V].

### 3.2 Who starts what
- `RouterActivity` is the ONLY HOME activity: `singleInstance`, `noHistory`, `taskAffinity=""`, Theme.NoDisplay (`AndroidManifest.xml:233-248`) [V]. A second Home press that arrives while the invisible instance is alive is delivered to `onNewIntent` and handled the same way (`RouterActivity.java:27-33`) [V].
- The custom `Launcher` activity: `singleTask`, `clearTaskOnLaunch`, `stateNotNeeded`, `taskAffinity=""`, `configChanges` WITHOUT uiMode / fontScale / density / locale (`AndroidManifest.xml:199-212`), filters MAIN+LAUNCHER_APP and MAIN+SECONDARY_HOME (`:213-226`); no HOME category, so Android never picks it as the default home [V]. The router starts it with an implicit MAIN+SECONDARY_HOME intent (a HOME-type task) when `home_type_launcher` is on (default; tablet: `home_type_launcher: true`, 2026-11-10-2:671) (`RouterActivity.java:76-99`) [V].
- Every start of the router or the Launcher from our code (complete list, grep): `TAS:1819` (`startRouter`, callers `1829` prearm, `1984` plain unlock, `2018` reconcile, `3260` stock front), `ProfileSession.java:133` (`returnToCustom`, prearm "custom"), `:143` (`goToQuickstep`, prearm "stock"), `ReturnReset.java:247` (`settleHome` without the helper, prearm "custom"), `ControlActivity.java:622` (plain Home intent after "Switch to the custom launcher"), and `performGlobalAction(GLOBAL_ACTION_HOME)` at `TAS:2330` (`goHome`, used by `settleHome` `2349` and `ReturnReset.java:451`) and `TAS:3082` (`guardStrayHome`) [V]. Nothing else starts our Launcher; no notification content intent opens it (no `PendingIntent` in `KeepAliveService`, `NotifQuiet`, `VpnOff`) [V].

### 3.3 The lock / unlock flow in a Quickstep session (what leaves the custom home on top)
1. SCREEN_OFF posts `lockPrepare` at +700 ms and +6500 ms (`TAS:2911-2923`); SCREEN_ON on a locked tablet runs `lockPrepare("woke locked")` (`TAS:1874`) [V].
2. `lockPrepare` (`TAS:1844-1871`) puts the custom home in front behind the keyguard when `ProfileSession.prearmCustomStackOnLockedWake(prearm, locked, armed, alreadyDone, paused)` (`TAS:1869`, `ProfileSession.java:34-36`). `prearm` defaults ON (`ProfileSession.java:25`; tablet `profile_prearm: true`, 2026-11-10-2:699). The stored mode STAYS Quickstep (`TAS:1825` comment: "Mode is left alone: the unlock decides") [V]. The lock-time pre-clean that would flip the mode is off on the tablet (`profile_preclean: false`, 2026-11-10-2:700), so Quickstep's apps stay open: lock log "LOCKPREP no shell helper: Quickstep's apps stay until the unlock ..." (2026-11-10-2:725, 795) [V].
3. A gesture ("handshake") accepted on the pad: `secretAccepted` (`TAS:1792-1802`) calls `startQuickstepFront("handshake")` (`TAS:1796`). With `resume_qs_app` (default ON, `ResumeTarget.java:9`, 6 h memory `:11`) and a remembered app, `startQuickstepFront` starts THE APP with its launch intent and returns (`TAS:3240-3251`); only without a remembered app does it start the stock home through the router (`TAS:3254-3260`) [V].
4. At USER_PRESENT `applyUnlockChoiceBody` (`TAS:1947-2004`) counts the resumed app as "Quickstep in front" (`TAS:1967`, so no second start), then `verifyQuickstepFront(0)` (`TAS:1975`, `2032-2047`) looks once at +600 ms and, only after a restart, again 900 ms later (`ProfileSession.java:74`; at most two restarts, the second is not checked) and acts ONLY when OUR launcher or the Solitaire stand-in is in front (`TAS:2039-2040`); with an app in front it logs "attempt N: Quickstep (or another app) is in front, nothing to do" (`TAS:2044`) [V].
5. A plain PIN: `Mode.set(CUSTOM)` + `startRouter("custom")` + `ReturnReset.run` (`TAS:1981-1988`), by design [V].

### 3.4 Android's HOME stack (the framework half of B1, [I])
Both home tasks (the stock launcher's and our SECONDARY_HOME task) are HOME-type tasks in the one home stack. The repo already records the behaviour for the other direction: "Once the stock launcher's home task exists ... closing the last app makes Android resume THAT task instead of asking the router" (`StrayHomeGuard.java:4-8`) [V for the text, I for its application here]. The consequence for B1 (order of events) [I]:

```
before the lock   : app X on top; home stack = [stock home (top), custom home]
lock + prearm     : custom home brought to the top of the home stack (behind the keyguard)
gesture + AD5     : X started on top again; home stack unchanged = [custom home (top), stock home]
leave X (Back)    : Android resumes the top HOME task = the custom home; no HOME intent, no router
```

### 3.5 Timeline [V]
- AD5 (resume the app that was open) shipped in v0.7.4, 2026-11-06T05:50Z (`BUILDLOG.md:32`).
- `UsageGap` / AC1 (reconcile after a gap) exists since 2026-11-05 (`UsageGap.java:8`), inert without usage access (`UsageGap.java:44-46, 54`).
- Reports 2026-11-10 and 2026-11-10-2: `PACKAGE_USAGE_STATS: NOT granted` (line 612). TASKS.md:1294: "Last tablet report (2026-11-10-2): everything granted except PACKAGE_USAGE_STATS". Grant done 2026-11-13 ~17:00 Z (TASKS.md:1296 CC1b; check "5 x granted=true" CC1c, `:1297`).
- Current release v0.9.11, code 50, 2026-11-15T23:08Z (`BUILDLOG.md:61`). The report of the symptom: 2026-11-17 18:47 UTC (`INBOX.md:1620`).
- Process life on the tablet (our process restarts often; `keep_alive: false`, 2026-11-10-2:672): 2026-11-09 line 629 "process starts recorded: 6, in the last 24 h: 6"; 2026-11-10-2 line 630 "process starts recorded: 10, in the last 24 h: 2". Main-thread load: 2026-11-09 line 628 (power-menu scans max 647 ms, pencil tree searches max 514 ms), 2026-11-10-2 line 629 (max 563 ms, 1305 ms) [V].

---

## 4. Table A: the stored mode flips Quickstep -> custom without Tj choosing it

### A1. Double Home (impatient or lagged) [rank 2]
- **Where:** `RouterActivity.java:65-69` (`HomeDoublePress.isDouble(last, now)` -> `CrashGuard.clearTripped`, `Mode.set(CUSTOM)`, `returnedByDoubleHome = true`, then `ReturnReset.run` at `:96`); presses recorded in memory at `:56-58` (`Mode.java:18-31`); window `HomeDoublePress.java:5, 8-10` = 900 ms [V].
- **Trigger:** two Home intents handled by the router (`pre == null`, so not a prearm start) within 900 ms of each other, stored mode Quickstep [V]. The timestamps are PROCESSING times (`System.currentTimeMillis()` at `RouterActivity.java:56`), not tap times [V].
- **Preconditions / sources of Home intents:** Tj's Home key; the system when it needs a home and has none [I]; the stock launcher's own `startHome()` e.g. after Recents "Clear all" or Back [I]; our own `performGlobalAction(HOME)` only runs in custom mode (`TAS:3064` guard, `2330`) [V].
- **Why it can look random [V + I]:** router start latency on the tablet is 0.13 s to 2.44 s (lock log START -> ROUTER: 2026-11-10-2 `03:10:43.741` -> `03:10:46.183`; `03:11:40.404` -> `03:11:41.486`; 2026-11-09 `21:59:11.123` -> `21:59:11.250`) and the main thread stalls 0.5-1.3 s (section 3.5). A first press delayed by such a stall and a second press processed right after it are "a double" for the router even when Tj tapped 1-2 s apart. A user who sees nothing happen presses Home again [I].
- **No switch:** the Control screen says "Press Home twice ... (always on, nothing to switch)" (`ControlActivity.java:316`) [V].
- **How common:** occasional [I].
- **What Tj sees:** the Quickstep home does not appear; the custom home appears after a black cover; Quickstep's apps are closed, notifications cleared, radios off (`RouterActivity.java:95-96`) [V].
- **Log today:** `ROUTER home press, mode quickstep` (`RouterActivity.java:51`), twice, written BEFORE the flip; the flip is silent; later `RESET plan: ...` (`ReturnReset.java:212`) and `HOME custom home in front +N ms after the clean-up` (`TAS:2346`) [V]. No line says "double Home". Real sample of two presses (91 s apart, so no double): 2026-11-10 lines 747 and 758 (`19:32:49.842`, `19:34:20.486`).

### A2. RECONCILE on a Home press [rank 3]
- **Where:** `RouterActivity.java:71-75` -> `UsageGap.sessionEndedWhileAway` (`UsageGap.java:61-79`) -> `Mode.set(CUSTOM)` `:73`, `ReturnReset.run` via `returnedByDoubleHome` `:74, 96` [V].
- **Trigger:** a Home press in Quickstep, tablet unlocked and interactive, and the newest KEYGUARD_SHOWN usage event (type 17) is newer than `Mode.sessionRefMs` (`UsageGap.java:25-35, 61-79`) [V].
- **Preconditions:** usage access granted (else 0, `UsageGap.java:44-46, 54`); a lock since the session's last handshake confirmation without a confirmed handshake. With the helper alive every handshake unlock confirms (`TAS:1800, 1971`), so a false positive needs the helper away at the unlock (killed, restarting, force-stopped, accessibility toggled) [V code, I frequency]. Tj force-stops our app and Quickstep during his RAM experiments (TASKS.md:1316 CD1j: "I forced stopped quickstep several times, ... force stopped the custom launcher") [V], which makes "helper away at an unlock" more likely [I]. Possible clock-base mismatch between `System.currentTimeMillis()` and usage events after a clock jump [I, rare].
- **How common:** rare to occasional; live since 2026-11-13 [V date, I frequency].
- **What Tj sees:** after a Home press the custom home plus a reset (apps closed).
- **Log today:** `RECONCILE Home press: the keyguard was shown after the Quickstep session began and no handshake was confirmed: ending the session like a plain unlock` (`RouterActivity.java:72`) after `ROUTER home press, mode quickstep` (`:51`) [V]. The line carries no times, so "helper away" cannot be told from "false positive".

### A3. RECONCILE when the helper (re)connects [rank 3]
- **Where:** `TAS:328` (`reconcileAfterGap("the helper (re)connected")`, 1500 ms after `onServiceConnected`; `TAS:325` writes `SERVICE connected, mode <mode>`) -> `TAS:2011-2022` (`Mode.set(CUSTOM)` `:2017`, `startRouter("custom")` `:2018`, `ReturnReset.run` `:2019`) [V].
- **Trigger / preconditions:** as A2, evaluated at every (re)connect (2-10 process starts in the report windows, section 3.5) with the tablet unlocked and awake [V]. At a reconnect while the tablet is locked it returns false (`UsageGap.java:69`) [V].
- **How common / sees:** as A2; can fire with no user action at all (the custom home appears by itself a second or two after the helper comes back) [I].
- **Log today:** `RECONCILE the helper (re)connected: the keyguard was shown after the Quickstep session began and no handshake was confirmed: ending the session like a plain unlock` (`TAS:2015`) [V].

### A4. A new boot ends the session (by design; silent)
- `ProfileSession.checkBoot` (`ProfileSession.java:109-123`, called at `RouterActivity.java:47`): first Home after a boot count change -> `Mode.set(CUSTOM)` `:118`. Text only in a preference (`ProfileSession.java:120`), no lock-log line [V]. Theoretical edge: `Settings.Global.BOOT_COUNT` unreadable (-1) stores `boot_count_seen=-1`; a later real count makes `bootEndsQuickstep(-1, n)` true (`ProfileSession.java:58-60`) [V code]; the tablet reads it fine (`boot_count_seen: 19`, 2026-11-10 line 649) [V]. Rare.

### A5. Plain unlock because the handshake was not recognised
- **Where:** `TAS:1981-1988` (`Mode.set(CUSTOM)` `:1983`); the recogniser `trackUnlockKey` (`TAS:1686-1713`, 3 s tolerance for a late Enter `:1691-1694`); a withdrawn handshake `secretWithdrawn` (`TAS:1805-1813`) [V].
- **Trigger:** pad events late or dropped (Enter lag of 2164 ms is on record: 2026-11-10-2 line 729 `ENTER lag=2164ms lead=NN handshake=none`, a plain PIN), or the handshake timed out before the Enter [V for the lag, I for a missed gesture].
- **How common:** rare. Tj would probably phrase it "my gesture did not work".
- **Log today (good):** `ENTER lag=...ms lead=.. handshake=none` (`TAS:1709`), `UNLOCK plain -> custom (a Quickstep session ended), Enter to unlock N ms` (`TAS:1988`), `UNLOCKSUM custom home on screen N ms after the Enter tap ... stored mode ...` (`TAS:3182`); sample 2026-11-10-2 lines 729, 735, 738 [V]. A withdrawn handshake: `HANDSHAKE withdrawn (screen off or a plain retry too late), mode custom` (`TAS:1811`) [V].

### A6. A return method used by accident [rank 4]
- **Where:** `BackToCustomActivity.java:10-12` (also the target of the tile), `ReturnTile.java:21-27`, `ProfileSession.java:127-136` (`Mode.set(CUSTOM)` `:131`, prearm "custom" router start `:133-134`, `ReturnReset.run` `:135`); other methods: `TAS:1752` (key codes), `:1919` (side-key flurry), `:3409` (shake / knock) [V].
- **Defaults:** ICON on, TILE on, VOLCODE on (Tj stored `return_m_volcode: false`, 2026-11-10-2:705), BACKS, VOLBOTH, FLURRY, SHAKE, KNOCK off (`ReturnMethods.java:21-29`) [V]. The tile needs the TILE and the ICON method on (`ReturnTile.java:26` and `BackToCustomActivity.java:11`) [V].
- **The tile is one of his three Quickstep tiles:** `qs_tiles_quickstep: wifi,battery,custom(com.tj.launcher/.ReturnTile)` (2026-11-09:702, 2026-11-10:703, 2026-11-10-2:703) [V]: a mis-tap next to the Wi-Fi tile ends the session.
- **Proof it fired on the tablet at least once:** `failsafe_last: back to custom via icon at 16:20:24` (2026-11-10 line 666; the text says "icon" for the icon AND the tile because both end in `BackToCustomActivity`) [V]. Whether it was deliberate is unknown.
- **How common:** occasional [I]. **Sees:** custom home + reset (apps closed).
- **Log today:** only the preference text `failsafe_last` (`ProfileSession.java:132`); the lock log shows `ROUTER prepare custom, mode custom` because `Mode.set` ran first (`RouterActivity.java:51`) [V].

### A7. Lock-time clean-up flips the mode (needs the shell helper)
- `TAS:1854-1860` (`Mode.set(CUSTOM)` `:1858`) when `profile_preclean` is on, the helper is ready and no handshake is armed. Off on the tablet (`profile_preclean: false`, 2026-11-10-2:700) [V]. By design; lock-log `CLEANLOCK ...` (`ReturnReset.java:182`) [V].

### A8. Preferences unreadable or cleared
- `Mode.get` returns CUSTOM when the key is missing (`Mode.java:59-62`): Clear data, a corrupt `tj_mode.xml`. Silent [V]. Rare.

### A9. A withdrawn handshake reverts its own flip (by design)
- `TAS:1805-1813`: only when the lock began in custom and the handshake flipped the mode (`flippedByHandshake`, `TAS:1795`); returns at `:1806` for a session that was already Quickstep [V]. Logged (`TAS:1811`).

---

## 5. Table B: the custom home becomes visible while the stored mode stays Quickstep

### B1. Prearmed custom home under the AD5-resumed app [rank 1]
- **Where:** `TAS:1869` + `1826-1833` (prearm), `TAS:1796` + `3238-3261` (resume starts the app and returns at `:3250`), `TAS:1966-1968` (resumed app counts as "front"), `TAS:2039-2044` (verify does nothing when an app is in front) [V].
- **Trigger:** Quickstep session; an app open at the lock (remembered by `rememberQuickstepApp`, `TAS:3210-3220`, within 6 h); gesture unlock (AD5 resume); later the app is left by Back, finishes, crashes, or Back from Recents [V for the chain in the code, I for what Android resumes].
- **Preconditions on the tablet [V]:** `profile_prearm: true`, `profile_preclean: false`, `resume_qs_app` not stored = default ON, `startup_open: false` (2026-11-10-2:699, 700, 709); in release no Solitaire stand-in is started (`ReturnReset.java:80`).
- **Framework half [I]:** section 3.4. Same mechanism the repo already documented for W5 (`StrayHomeGuard.java:4-8`).
- **How common:** occasional [I]: needs a lock cycle with an app open since the last Home press, and leaving the app by Back; a Home press (router -> stock home) resets the order, which is why it looks random.
- **What Tj sees:** the custom home screen in the middle of a Quickstep session, nothing closed. One Home press brings the stock home back (single press = Quickstep) [V code `RouterActivity.java:105`]; a quick second press makes it a double Home (A1).
- **Log today:** silent. Related lines that do exist: `START resume <pkg> (handshake)` (`TAS:3249`) and `UNLOCK handshake <id> -> Quickstep, stock in front=true, Enter to unlock N ms` (`TAS:1972`). Nothing is written when the custom Launcher resumes (`TjHooks.java:84-102`) [V].
- **Test gap [V]:** the AD5 emulator section proves the app comes forward (`tools/emulator_scenario.sh:1834-1835`) but never presses Back from it or looks at what is underneath; there is no emulator check anywhere for "custom home in front in Quickstep" (the W5 test at `:405-414` covers only the opposite direction).

### B2. The pre-arm router start lands late
- Prearm = `startRouter("custom")` (`TAS:1815-1823, 1829`); the ROUTER line follows 0.13-2.44 s later (section A1 data). A gesture unlock that arrives inside that gap lets the late custom start end up above the stock / app start [I]. The verify only repairs it if the custom launcher or Solitaire is in front at +600 / +900 ms, at most two restarts, then it stops silently (`ProfileSession.java:69-74`, `TAS:2033`) [V]. In the saved data the wake -> gesture gap was 7 s (`03:11:40.234` wake, `03:11:48.545` handshake), so the prearm had landed [V].
- **How common:** rare. **Log today:** `VERIFY attempt N: the custom launcher is in front after a Quickstep choice: starting Quickstep's home again` (`TAS:2041`); no "gave up" line [V].

### B3. Cold or killed stock launcher at the gesture unlock
- `startRouter("stock")` -> `RouterActivity.startStock` (`RouterActivity.java:108-120`) takes seconds when the stock process is gone (Quickstep grows to 280-450 MB on this 2 GB tablet, TASKS CD1; LMK kills cached processes) [V for the numbers in TASKS, I for the kill]. Until it is up the custom home is the visible home task; `applyUnlockChoiceBody` sees `stock in front=false` (`TAS:1967`) and starts it again (`:1968`), the verify gives up after two restarts [V].
- **How common:** rare to occasional. **Log today:** `UNLOCK handshake ... stock in front=false`, `VERIFY ...`, `UNLOCKSUM custom home on screen N ms after the Enter tap ... stored mode ...` (`TAS:1972, 2041, 3182`) [V]. None occurs in the saved reports (all say "stock in front=true" and "nothing to do": 2026-11-09 lines 750, 753; 2026-11-10-2 lines 780, 786, 811, 817).

### B4. Android resumes the HOME stack top after something ends
- Stock launcher force-stopped or crashing while in front (Tj does force-stop it: CD1j), Back from the Recents screen, an app crash or ANR close: the remaining top HOME task is our custom home [I]. No log. Rare; it is the same missing guard as B1.

---

## 6. Checked and ruled out

| Candidate | Result |
| :- | :- |
| Notification taps | no `PendingIntent` that opens our Launcher or router (`KeepAliveService`, `NotifQuiet`, `VpnOff` grepped) [V] |
| Battery-saver / dark-theme, font, density, locale, wallpaper changes | `configChanges` omits uiMode / fontScale / density / locale (`AndroidManifest.xml:208`), so a RUNNING Launcher is recreated, but a stopped one is not brought to the front by a configuration change [I]. `Mode.set` side effects do not restart anything: the Settings alias toggle uses `DONT_KILL_APP` (`SettingsSpoof.java:119`) [V] |
| Crash-loop breaker | goes the other way: `Mode.set(QUICKSTEP)` (`CrashGuard.java:65`); `markFailOpen` (`RouterActivity.java:102`) only after a failed custom start [V] |
| Stray-home guard | custom mode only (`TAS:3064`) [V] |
| `onNewIntent` of the Launcher (`launcher3/src/com/android/launcher3/Launcher.java:1356-1410`) | handles `EXTRA_HOME_WHILE_ON_HOME` only; no mode check, no start of anything [V: contents read earlier in this investigation; the hooks are called from `Launcher.java:325` (created) and `:949` (resumed)] |
| Process death of our app | the mode survives (`commit()`, `Mode.java:66`); after the restart the router reads it; the cold start only feeds A1 [V + I] |
| Entrances / Konami / Solitaire hotspot | custom -> Quickstep only (`ProfileSession.java:139-145`; callers `TAS:1743, 3267`, `EntranceWatcher.java:109`) [V] |
| `ControlActivity` | `:123` "Switch to the custom launcher" is deliberate; `:395` Restore everything goes to Quickstep; its Quickstep-only screens finish themselves when the mode becomes custom [V] |
| Default-home chooser after a crash | the chooser lists the router and the stock launcher; our Launcher has no HOME category (`AndroidManifest.xml:213-226`), so it cannot be chosen directly [V] |
| Cast / second display | the SECONDARY_HOME filter lets Android start our Launcher as the home of a secondary display in any mode [I]; not the tablet's own screen |
| Lock-time `RecentsSweep` | its lock-time call is custom-mode only (`TAS:1864`) [V] |

---

## 7. Scan of the saved tablet reports (`research/tablet/*/tablet-report.txt`)

Question: is there a sequence where the custom Launcher was created or resumed while the stored mode was Quickstep, outside the designed pre-arm behind the keyguard?

**Answer: no.** Counts per report (grep): `LAUNCHER created` 0, `RECONCILE` 0, a VERIFY line saying "starting Quickstep's home again" 0, `STRAY` 0 (the guard writes only logcat) in all four reports (2026-11-05, 2026-11-09, 2026-11-10, 2026-11-10-2) [V]. All four were taken while the stored mode was Quickstep (`mode: quickstep`, 2026-11-09:618, 2026-11-10:619, 2026-11-10-2:619) [V]. The lock-log window is only the last 40 lines (2026-11-09: lines 714-753) or 100 lines (2026-11-10-2: lines 718-817), so absence proves little [V]. The 2026-11-05 report has no lock log. The four reports are v0.7.1 (code 18), v0.8.12 (code 37), v0.9.0 (code 39) and v0.9.1 (code 40) (their FACTS.md); the first predates AD5 (v0.7.4), all predate the usage-access grant and v0.9.2 - v0.9.11 [V].

Quotable designed behaviour (pre-arm while the mode is Quickstep, behind the keyguard):
- 2026-11-10-2:723 `03:10:45.680 LOCKPREP woke locked: locked=true armed=false mode=quickstep`; :728 `03:10:46.183 ROUTER prepare custom, mode quickstep` (the START at :719 `03:10:43.741` = 2.44 s earlier).
- 2026-11-10-2:792-799 `03:11:40.234 LOCKPREP woke locked ... mode=quickstep`, `03:11:40.404 START router custom`, `03:11:41.486 ROUTER prepare custom, mode quickstep`.

Quotable gesture unlocks (no AD5 resume: `qsApp` was null in every sample, so B1 was never exercised):
- 2026-11-10-2:773-780 `03:11:31.568 START stock home, no resume: no Quickstep app is remembered [qsApp=null fg=com.tj.launcher mode=quickstep]`, `HANDSHAKE erase2 accepted, mode now quickstep`, `UNLOCK handshake erase2 -> Quickstep, stock in front=true, Enter to unlock 1397 ms`, :786 `VERIFY attempt 1: Quickstep (or another app) is in front, nothing to do`. Same at 03:11:48-03:11:50 (:804-817) and in 2026-11-09 lines 743-753.

Quotable plain unlock that ended a session (by design): 2026-11-10-2:729 `03:10:49.952 ENTER lag=2164ms lead=NN handshake=none`, :735 `03:10:51.432 UNLOCK plain -> custom (a Quickstep session ended), Enter to unlock 1174 ms`.

Other quotable lines: 2026-11-10:666 `failsafe_last: back to custom via icon at 16:20:24` (A6 fired once on the tablet); 2026-11-09:665 `failsafe_last: Quickstep via secret entrance: Page flick at 19:36:30`; `guard_count: 11`, `guard_last: 15:25:03` (the custom-direction guard redirected the stock home 11 times, 2026-11-10-2:667-668); 2026-11-10:747 and :758 two plain Home presses in Quickstep 91 s apart (no double).

---

## 8. Smallest change and proof, per top path

### P0. Cross-cutting instrumentation (do first; no behaviour change)
See section 2: `MODE from -> to by <caller>` in `Mode.set` plus the `mode_flips` Features key, and `LAUNCHER resumed while mode quickstep` in `TjHooks.onLauncherResumed`, mode added to the "created" line. With these two the next tablet report names the cause of any flip or any custom-home-in-Quickstep, whatever path it is (A1-A9 and B1-B4) [I: design, V: the report prints Features keys and the lock-log tail].

### P1. B1 (and B2-B4 at once): bounded reverse guard
- Pure rule next to `StrayHomeGuard.shouldRedirect` (`StrayHomeGuard.java:26-31`), e.g. `StrayCustomGuard.shouldRedirect(enabled, quickstepMode, keyguardLocked, customHomeShown, now, failOpenUntil, standDownUntil, recent[])` with the same MAX 4 per 8 s and 30 s cool-down, plus JVM tests.
- Service side: a twin of `guardStrayHome` (`TAS:3060-3083`) called from the same place; fires on `TYPE_WINDOW_STATE_CHANGED` of OUR package with class `com.android.launcher3.Launcher` (not Control / Setup screens) while the stored mode is Quickstep, unlocked, `now - lastUnlockAt > ProfileSession.UNLOCK_QUIET_MS` (800 ms, `ProfileSession.java:77`), `now - Mode.changedAt > 2500` (as `TAS:3066`), automation not paused, `Mode.failOpenUntil` passed. Action: `LockLog.add("STRAYCUSTOM", "custom home in front in Quickstep (last unlock N ms ago, fg before=<pkg>)")` and `startRouter("stock")` (the router with prearm "stock" starts the stock home, `RouterActivity.java:52`; not counted as a Home press, so it can never make a double).
- Root cause, optional and larger: `RouterActivity` takes a new extra with the app to resume and starts the stock home FIRST and the app second from the same activity (the order cannot flip; same technique as `startTargetBehindLock`, `RouterActivity.java:97-98`); `startQuickstepFront` (`TAS:3238-3261`) passes the extra instead of `startActivity(li)`. The stock home then sits under the app and Back reveals it.
- Proof before the tablet (CI, about 1 minute added to section `ad`): after `tools/emulator_scenario.sh:1835` press Back and assert that the resumed activity is not `com.tj.launcher/com.android.launcher3.Launcher` (`resumed > "$OUT/ad-r1-back.txt"` then a `check` with `! grep -q`). Expected red today if B1 is real; green with the guard.
- Proof on the tablet: `LAUNCHER resumed while mode quickstep ... fg before=<app>` within seconds of a `START resume <app> (handshake)` line, then `STRAYCUSTOM`.

### P2. A1: log first, rule second
- `RouterActivity.java:65-69`: add `LockLog.add(this, "HOME2", "double Home -> custom: gap " + (now - last) + " ms, process up " + (SystemClock.uptimeMillis() - Process.getStartUptimeMillis()) + " ms")` (the same expression is used at `TjHooks.java:170`).
- Decide a rule only after a report shows the gap and the process age: for example ignore a "double" whose first press was handled in a process younger than about 5 s, or whose gap is under about 150 ms (not a human double press) [I]. Changing the 900 ms feature without data would break a feature Tj asked for (TASKS.md:108).

### P3. A2 / A3: only when the helper really was away
- `UsageGap.sessionEndedWhileAway` returns the facts it used (`shown`, `ref`, which of changed / confirmed set `ref`); the two RECONCILE lines (`RouterActivity.java:72`, `TAS:2015`) append `shown=HH:mm:ss.SSS ref=HH:mm:ss.SSS helperUp=HH:mm:ss.SSS`.
- New rule: RECONCILE only if the helper was NOT connected at `shown` (a new `TjAccessibilityService.connectedSinceMs`, set in `onServiceConnected` next to `TAS:325`; no service now = away). A helper that was connected through the whole lock decided the unlock itself (`applyUnlockChoiceBody`), so a later RECONCILE can only be a false positive.
- Proof line: the `RECONCILE` line with the three times; `shown` earlier than `helperUp` = a real gap, `helperUp` earlier than `shown` = false positive.

### P4. A6
- `ReturnTile.java:26`: `.putExtra("via", "tile")`; `BackToCustomActivity.java:11`: pass it to `ProfileSession.returnToCustom(this, via)`; inside `returnToCustom` (`ProfileSession.java:131`) `LockLog.add(app, "RETURN", why)`. Proof: `RETURN tile`.

### P5. B2 / B3
- A last lock-log line when `verifyQuickstepFront` stops after its second restart (`TAS:2033` returns silently): `VERIFY gave up: <ours|Solitaire> still in front after 2 restarts`. Proof: that line together with `UNLOCKSUM custom home on screen ...`.

### A5
- No change; both lines exist. Ask for a fresh tablet report right after the next occurrence (the lock-log window is 100 lines).

---

## 9. The cheapest discriminators from Tj (optional; two questions)
1. When it happened, did Quickstep's apps close (the black cover, notifications cleared)? Yes = A1 / A2 / A3 / A6 / A5 (a reset ran); no = B1 / B3 / B4.
2. What was the last thing before it: leaving an app with Back, unlocking with the gesture, pressing Home, or nothing at all (it came by itself 1-2 s after something restarted)?

---

## Appendix A: every `Mode.set` call site (grep, complete)

| File:line | New mode | Trigger | Lock-log line for the flip |
| :- | :- | :- | :- |
| `RouterActivity.java:62` | QUICKSTEP | three Home presses with the helper off (L10) | none (toast) |
| `RouterActivity.java:67` | CUSTOM | double Home (A1) | none |
| `RouterActivity.java:73` | CUSTOM | RECONCILE on Home press (A2) | `RECONCILE Home press: ...` |
| `ProfileSession.java:118` | CUSTOM | new boot (A4) | none |
| `ProfileSession.java:131` | CUSTOM | return methods icon / tile / key code / flurry / shake / knock (A6) | none (`failsafe_last` text) |
| `ProfileSession.java:141` | QUICKSTEP | entrances, Konami, Solitaire hotspot | none (`failsafe_last` text) |
| `TAS:1795` | QUICKSTEP | handshake accepted while the lock began in custom | `HANDSHAKE <id> accepted, mode now quickstep` |
| `TAS:1809` | CUSTOM | handshake withdrawn (A9) | `HANDSHAKE withdrawn ...` |
| `TAS:1858` | CUSTOM | lock-time clean-up (A7) | `CLEANLOCK ...` (`ReturnReset.java:182`) |
| `TAS:1964` | QUICKSTEP | handshake unlock while the mode was custom | `UNLOCK handshake ... -> Quickstep ...` |
| `TAS:1983` | CUSTOM | plain unlock (A5) | `UNLOCK plain -> custom (a Quickstep session ended) ...` |
| `TAS:2017` | CUSTOM | RECONCILE at helper connect (A3) | `RECONCILE <why>: ...` |
| `CrashGuard.java:65` | QUICKSTEP | crash breaker (3 crashes in 2 min) | none |
| `ControlActivity.java:123` | CUSTOM | button "Switch to the custom launcher" | none |
| `ControlActivity.java:395` | QUICKSTEP | "Restore everything" | none |

## Appendix B: lock-log lines that already mention the mode
`SCREEN_OFF` / `SCREEN_ON` / `USER_PRESENT` `locked=.. mode=..` (`TAS:2910`); `LOCKPREP <why>: locked=.. armed=.. mode=..` (`TAS:1850`); `ROUTER home press|prepare <x>, mode <mode>` (`RouterActivity.java:51`); `SERVICE connected, mode <mode>` (`TAS:325`); `HANDSHAKE <id> accepted, mode now <mode>` (`TAS:1801`); `START stock home, no resume: ... [qsApp=.. fg=.. mode=..]` (`TAS:3257`); `UNLOCKSUM ... stored mode ...` (`TAS:3176-3182`). The Launcher's own lifecycle lines carry no mode (`LAUNCHER created ...`, `TjHooks.java:170`).
