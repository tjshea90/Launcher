# CD2a emulator growth test: does a Quickstep session make the stock launcher keep growing? (2026-11-14)

Runs: **cd2** = run 43553084459 (#604, job 132319727143, helper OFF, 733 s, 5 PASS 0 FAIL) and **cd2h** = run 43553087209 (#605, job 132319735153, helper ON, 612 s, 6 PASS 0 FAIL), both on main head aae08790, 2026-11-14 03:43Z.
Method (tools/emulator_scenario.sh sections `cd2`, `cd2h`; helpers `cdmem` / `cdreturns` hoisted): the stock launcher process is killed once, three apps are started, then a Quickstep session is STARTED AND ENDED 24 times (`cdreturns`: mode Quickstep, Home, double Home back to the custom launcher, the return reset runs), `dumpsys meminfo com.android.launcher3` after 0 / 6 / 12 / 18 / 24 sessions (cd2 then 30 more Recents open / close cycles). The process id stays the same: the stock process was NOT restarted during the 24 sessions.
Tj's question (03:34Z): "would [starting and ending a Quickstep session] cause quickstep to keep growing".

## Raw numbers (PSS KB; Objects; RSS; system counts; two controls)

| step | TOTAL | Java | Native | System | Priv.Other | Views | ViewRootImpl | Activities | RSS | stock tasks | stock activity records | ours (PSS) | SystemUI (PSS) |
| :- | -: | -: | -: | -: | -: | -: | -: | -: | -: | -: | -: | -: | -: |
| s0 (no helper, fresh) | 16789 | 1352 | 7440 | 5001 | 988 | 0 | 0 | 0 | 103676 | 1 | 0 | 45738 | 57753 |
| s6 | 42061 | 6100 | 21000 | 11253 | 2232 | 143 | 2 | 2 | 148440 | 3 | 2 | 38066 | 60780 |
| s12 | 42436 | 6260 | 21096 | 11312 | 2288 | 143 | 2 | 2 | 148480 | 3 | 2 | 40508 | 60791 |
| s18 | 41296 | 6144 | 19848 | 11536 | 2288 | 143 | 2 | 2 | 146692 | 3 | 2 | 42195 | 63487 |
| s24 | 41492 | 6212 | 19920 | 11564 | 2316 | 143 | 2 | 2 | 147220 | 3 | 2 | 44364 | 62260 |
| sR30 (+30 Recents cycles) | 42144 | 6096 | 20656 | 11600 | 2372 | 143 | 2 | 2 | 148548 | 3 | 2 | 69575 | 62711 |
| h0 (helper, fresh) | 17328 | 3136 | 6796 | 5116 | 980 | 0 | 0 | 0 | 105600 | 1 | 0 | 39748 | 56605 |
| h6 | 36371 | 5920 | 15640 | 11227 | 2264 | 97 | 1 | 1 | 142220 | 2 | 1 | 44271 | 62306 |
| h12 | 36437 | 5928 | 15680 | 11205 | 2268 | 97 | 1 | 1 | 141988 | 2 | 1 | 44592 | 62434 |
| h18 | 35645 | 5808 | 15004 | 11225 | 2280 | 97 | 1 | 1 | 141308 | 2 | 1 | 44269 | 59761 |
| h24 | 37062 | 5856 | 16304 | 11242 | 2304 | 97 | 1 | 1 | 142296 | 2 | 1 | 44223 | 60630 |

Graphics is 0 in every row (software GL on the CI emulator: GPU memory is NOT measurable here).

## Reading

- **No growth per session.** After the first six sessions the stock process is FLAT: no helper 42.1 -> 42.4 -> 41.3 -> 41.5 MB (sessions 6 / 12 / 18 / 24), helper 36.4 -> 36.4 -> 35.6 -> 37.1 MB. The drift over 18 sessions is -0.6 MB (no helper) and +0.7 MB (helper): noise, not a slope (a leak of even 0.1 MB per session would show as 2.4 MB over 24).
- **Counts are constant:** Views 143 / 97, ViewRootImpl 2 / 1, Activities 2 / 1, the system's stock tasks 3 / 2 and activity records 2 / 1 at every step. Nothing is piling up per session, neither inside the process nor in the system's task list.
- **The one-time cost** is the first sessions: 17 MB (fresh process, nothing created yet) -> 36-42 MB. That is the stock home activity + the Recents screen being created once, then reused. (With the helper the stock side is lighter: one activity less, no on-screen Recents screen.)
- 30 more Recents open / close cycles on top: 41.5 -> 42.1 MB (flat).
- Side observation about OUR process (control, not the question): our own PSS stays 38-45 MB during the sessions but read **69.6 MB after the 30 Recents cycles** (the accessibility service scans the Recents windows each time). Not the stock launcher's memory; may be GC timing; noted for the RAM work (TASKS CA1), not investigated here.

## Together with Tj's own tablet data
The tablet's Memory screen showed Quickstep at a flat 414-450 MB with the 1-DAY average (445) at least as big as the 3-HOUR one (414): a process that leaks per session would show a LOWER old average than the recent one. So a per-session leak does not fit either.

## Limits (say them every time)
Plain AOSP Android 10 Quickstep, not MediaTek's Go build; software GL (Graphics row reads 0); 24 sessions, not months; no dark-mode flips. The answer is "the Quickstep session itself does not make it grow", not "nothing of ours can".
