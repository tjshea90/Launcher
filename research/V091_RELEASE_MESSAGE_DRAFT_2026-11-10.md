# DRAFT of the answer to Tj for v0.9.1 (TASKS BP1b / BP1d / BP2b)

Written 2026-11-10 01:45 UTC, BEFORE the release exists, so a fresh account can send it without re-deriving anything.
**Before sending:** (1) the Release must exist (`get_release_by_tag v0.9.1`) and be recorded; (2) replace `<LINK>` (alone on its own line, plain text, no code block); (3) if the gate changed anything (a check removed, a fix added, a section that stayed red), correct the matching sentence; (4) plain words, no internal labels in the text Tj reads.
Everything below the line is the message.

---

v0.9.1 is released. It passed the unit tests (911) and the emulator gate.

<LINK>

**What changed**

- **Settings home page:** Sound now says only "Volume", System only "Languages", and Display says "Brightness, sleep, font size".
- **Settings polish:** the Battery line no longer shows "Not charging" for a moment when the page opens, and the Storage and Battery rows no longer change height while loading. The brightness percentage now uses your tablet's real range.
- **White box on the lock screen after sliding the panel back up:** new attempt. The white cover over the pencil now disappears as soon as the pencil starts moving up, and stays away until the panel has stopped. Once a finger touches the screen the app checks the pencil every 25 ms instead of every 100 to 150 ms, so it notices the slide within about two frames. This is not proven on your tablet. While the panel is sliding you may see the real pencil for a moment.
- **Tablet report:** ordinary Quickstep use no longer floods the lock log, so a report now keeps the events of the custom session before it.

**Not fixed yet (I need a report from the tablet for each)**

- **Screenshot button on the first side-key press, then a visible patch a couple of times:** I have no log of it. Your last report only held "app opened in Quickstep" lines, which had pushed everything else out. v0.9.1 fixes that part.
- **Quickstep apps in the custom Recents list:** your report showed why: the clean-up found neither a "Clear all" button nor a closable card on your tablet's Recents screen within 2.5 seconds, and the shell helper (the other way to clear them) was not running. v0.9.1 records what that screen contains when this happens: counts only, no app names or titles.

**Your tablet report of 10 November**

- **Battery page:** it can only show the percentage because the app has no usage access on the tablet. That is expected, not a bug. To get the numbers: start the shell helper, open Launcher control, tap "Do everything that is missing", then open Settings > Battery once in the custom launcher. With the cable instead:

```bash
adb shell pm grant com.tj.launcher android.permission.PACKAGE_USAGE_STATS
```

- **Storage page:** works on the tablet (scan took 0.66 s, 96 % used).
- **No crash** anywhere in the report.
- **Shell helper:** it was not running. Installing an update stops it, so it has to be started again after each install.

**What I need from you**

1. Install v0.9.1.
2. If the white box, the Screenshot button on the first press, or Quickstep apps in Recents happens again: open Launcher control and send the report right away, before opening other apps.
3. For the Battery numbers: the grant above, then a new report.

**Saving and handing over**

- Everything is saved on GitHub and `main` is the real state. A different Claude account can continue with this one line: "Resume Launcher: run bash tools/install-hooks.sh, then bash tools/resume.sh, read HANDOFF.md block 0p and continue from CHECKPOINT.md Do this next."
- For about ten minutes two sessions worked at once: the old account's session finished its review and fixed three findings while this one had already started. It then stopped by itself. I merged its work; nothing was lost. One of its fixes matters to you: the new Recents diagnostic would have copied the titles of your Recents cards into the public report, and now records counts only.
- The auto-resume timer of this session is on (every 2 hours). The old account's timer cannot be seen from here; it only wakes the old session.
