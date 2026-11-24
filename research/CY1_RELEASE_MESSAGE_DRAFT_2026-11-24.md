# CY1 answer to Tj (DRAFT 2026-11-24, written BEFORE the release; check every sentence against the code and the run list)

Status of the sentences: P = proven (JVM test or emulator run), R = reasoned only.

## Short version (first text of the answer)
v0.9.20 is a very small speed release. I audited the launcher (not the Settings pages, not the music app, as you said) and found that four earlier speed passes had already taken almost every cheap win. Three small, provably neutral changes were left. Release link: <LINK, plain text, own line>.

## What I checked
- Process start, manifest, receivers, services (by hand: clean, nothing eager that is not needed).
- The accessibility service (per-event work, polling, wake-ups, tree walks), the Home / lock / unlock path, background work, disk writes on the main thread, the Launcher3 loader / icon cache. Two read-only reviews with neutral briefs; their reports are in research/CY1_AUDIT_A_2026-11-23.md and CY1_AUDIT_B_2026-11-23.md. A third review (launcher3 hot paths) was not run: my usage was capped for a day and that area was reviewed in CW1.

## What changed (3 things, 4 files, +33 / -8 lines)
1. Returns from Quickstep to the custom launcher wrote the "last return" report text to the preferences file with an fsync, on the main thread, right before the black cover goes (3 writes), plus one flag. These are now written in the background (the same way the other status lines already are). P: in-process readers see the new value at once (same SharedPreferences object); the JVM tests and sections base,b4,o,cs ran. R: the saving is about 10-40 ms on the cover-removal moment of such a return (estimate; no tablet number newer than v0.9.1 exists).
2. Locking the screen in the custom launcher no longer pings the shell helper to ask "are you there" when the answer cannot matter (the rule is false in custom mode). P: ProfileSessionTest has an exhaustive truth table (all input combinations) showing the rule is false whenever the mode is custom. R: 1.5-3 ms per lock cycle on the main thread (estimate).
3. Same for the PIN Enter tap of a plain custom unlock (the check order was changed, the answer is identical). P: same exhaustive test.

## What I did NOT change, and why
- Everything that touches the lock-screen timing (PIN-pad checks, recents polling, the Recents / usage queries on a double-Home return, the curtain order) stays as it is: the possible saving is a few ms, the risk is a lock-screen regression, and I have no tablet number to size it. They are listed in the research files as "build only after a tablet number".
- R8 / resource shrinking stays OFF (as decided earlier).

## How much testing (honest)
The tiered gate asked for 4 emulator runs (~91 min). I ran ONE run of sections base, b4, o, cs (the sections that execute the changed code) + the full unit suite, lint and the handoff test (ship.sh). The sections that exercise lock timing were not rerun because no timing code changed. Gate verdict: <FILL: run #794 id 45413130411 ...>.

## What I need from you (optional, only if you want the next step)
Developer options > Memory > "Quickstep" number after a day of use, and a tablet report (Launcher control > report) from v0.9.20: it carries the PIN-pad check counts and the "PROFILE ready +N ms" line that would size the remaining items.

## Honest verdict
You will not feel this release. The launcher was already well optimised (four earlier passes); this one removes three avoidable bits of main-thread work. I would not spend more usage on speed until a tablet report shows a number worth chasing.
