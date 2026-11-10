#!/usr/bin/env python3
"""tablet_intake.py - save what Tj sends from the tablet INTO THE PROJECT and turn it into one facts file Claude reads first.

  python3 -I tools/tablet_intake.py <tablet-report.txt | folder | zip | -> [--label NAME] [--out research/tablet] [--date YYYY-MM-DD] [--keep-images]

Why (Tj, 2026-11-05: "make sure claude can save and use all necessary information from the tablet"): the tablet report is the only way Claude sees the real tablet. A report that stays in a chat
is lost with the session, and one that is committed raw could carry a serial number, an address or an e-mail address into a repository that may be public. So:
  1. read the input safely (a zip is never extracted to disk; names are never trusted; sizes are capped);
  2. keep only small TEXT files (the report, the PC route's launcher-probe.txt / status.txt, screen-structure .xml); images and system apps are NOT copied (their names, sizes and sha256 are listed);
  3. redact serial numbers, IMEI-like numbers, MAC and IP addresses, e-mail addresses and Wi-Fi names; refuse the whole intake if anything shaped like a live credential is left;
  4. write research/tablet/<date>/ (+ FACTS.md: red flags first, then the answers to the open questions) and refresh research/tablet/LATEST.md.
Pure Python 3 standard library. Run it with -I (isolated): the input is untrusted data.
"""
import argparse
import datetime
import hashlib
import os
import re
import shutil
import sys
import zipfile

MAX_TEXT = 12 * 1024 * 1024        # one text file
MAX_XML = 200 * 1024               # one screen-structure file
MAX_IMG = 300 * 1024               # only with --keep-images
MAX_IMG_TOTAL = 4 * 1024 * 1024

# ---------------------------------------------------------------- redaction
REDACTIONS = [
    ("email", re.compile(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}"), "<email>"),
    ("mac", re.compile(r"\b(?:[0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}\b"), "<mac>"),
    # IPv6: only the forms that cannot be mistaken for something else (8 groups, or a "::" with a hex group before it), so a time like 06:49:20, "::1" and "std::x" are left alone
    ("ipv6", re.compile(r"(?i)(?<![0-9a-z:])(?:(?:[0-9a-f]{1,4}:){7}[0-9a-f]{1,4}|(?:[0-9a-f]{1,4}:){1,6}(?::[0-9a-f]{1,4}){1,6}|(?:[0-9a-f]{1,4}:){1,7}:)(?![0-9a-z:])"), "<ipv6>"),
    ("ip", re.compile(r"\b(?!127\.0\.0\.1\b)(?!0\.0\.0\.0\b)(?:\d{1,3}\.){3}\d{1,3}\b"), lambda m: "<ip>" if all(int(x) <= 255 for x in m.group(0).split(".")) else m.group(0)),
    ("ssid", re.compile(r"(?i)(\bSSID\b\s*[:=]\s*)\"?[^\"\n]+\"?"), r"\1<redacted>"),
    # v0.7.5 AI1 diagnostics: the lock log lists Recents card descriptions ("; saw 2 card(s): <label + task title>"). A task title can be a page title or a conversation name, so it never reaches the public repo.
    ("cards", re.compile(r"(?i)(\bsaw\s+\d+\s+card\(s\):\s*)[^\n]*"), r"\1<card titles redacted>"),
    ("serial", re.compile(r"(?im)(\b(?:ro\.serialno|ro\.boot\.serialno|serialno|serial number|serial|imei|meid|android_id|bluetooth_address|bdaddr|wifi_mac)\b[\]\[: =]*)[^\s\]]+"), r"\1<redacted>"),
    ("digits", re.compile(r"\b\d{14,19}\b"), "<digits>"),
]
CREDENTIALS = re.compile(
    r"sk-ant-[A-Za-z0-9_-]{40,}|sk-[A-Za-z0-9_-]{32,}|ghp_[A-Za-z0-9]{30,}|gho_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{30,}|AIza[0-9A-Za-z_-]{30,}|AKIA[0-9A-Z]{16}|xox[baprs]-[A-Za-z0-9-]{10,}|-----BEGIN [A-Z ]*PRIVATE KEY-----")


def redact(text):
    """-> (text, {kind: count}). Order matters: e-mail and MAC before the number patterns."""
    counts = {}
    for kind, rx, repl in REDACTIONS:
        n = [0]

        def sub(m, repl=repl, n=n):
            out = repl(m) if callable(repl) else m.expand(repl)
            if out != m.group(0):
                n[0] += 1
            return out
        text = rx.sub(sub, text)
        if n[0]:
            counts[kind] = counts.get(kind, 0) + n[0]
    return text, counts


# ---------------------------------------------------------------- reading the input safely
def safe_name(rel):
    """A file name we are willing to write: the base name only (never a path), every character outside letters digits . _ - turned into _ ("tablet-report (1).txt" arrives like that), no leading dot; None if nothing is left."""
    base = os.path.basename(rel.replace("\\", "/"))
    base = re.sub(r"[^A-Za-z0-9._-]", "_", base).lstrip(".")[:120]
    if not base or not re.search(r"[A-Za-z0-9]", base):
        return None
    return base


# folder / zip members we read as the tablet's text: the report, the short summary, the PC route's probe and status (a browser may add " (1)" or a date to the name)
TEXT_RX = re.compile(r"^(tablet-report|tablet-summary|launcher-probe|status)[A-Za-z0-9._-]*\.txt$", re.I)


def entries(path):
    """Yield (relative path, size, reader) for every file; reader(limit) returns bytes. Never writes anything."""
    if path == "-":
        data = sys.stdin.buffer.read(MAX_TEXT + 1)
        yield "tablet-report.txt", len(data), (lambda limit, data=data: data[:limit])
    elif os.path.isdir(path):
        for root, dirs, files in os.walk(path, followlinks=False):
            dirs[:] = [d for d in dirs if not os.path.islink(os.path.join(root, d))]
            for f in sorted(files):
                full = os.path.join(root, f)
                if os.path.islink(full):
                    continue
                rel = os.path.relpath(full, path).replace(os.sep, "/")
                yield rel, os.path.getsize(full), (lambda limit, full=full: open(full, "rb").read(limit))
    elif zipfile.is_zipfile(path):
        with zipfile.ZipFile(path) as z:
            for info in z.infolist():
                if info.is_dir():
                    continue
                name = info.filename.replace("\\", "/")
                if name.startswith("/") or ".." in name.split("/"):
                    yield "UNSAFE:" + name, info.file_size, None
                    continue
                yield name, info.file_size, (lambda limit, info=info: z.open(info).read(limit))
    else:
        yield os.path.basename(path), os.path.getsize(path), (lambda limit, path=path: open(path, "rb").read(limit))


def sha256_of(reader_path, rel, path):
    """sha256 of one entry without writing it anywhere."""
    h = hashlib.sha256()
    if path == "-":
        return h.hexdigest()
    if os.path.isdir(path):
        with open(os.path.join(path, rel), "rb") as f:
            for chunk in iter(lambda: f.read(1 << 20), b""):
                h.update(chunk)
    elif zipfile.is_zipfile(path):
        with zipfile.ZipFile(path) as z, z.open(rel) as f:
            for chunk in iter(lambda: f.read(1 << 20), b""):
                h.update(chunk)
    else:
        with open(path, "rb") as f:
            for chunk in iter(lambda: f.read(1 << 20), b""):
                h.update(chunk)
    return h.hexdigest()


# ---------------------------------------------------------------- understanding the report
class Report:
    """The tablet report (or the PC route's launcher-probe.txt), parsed just enough to answer questions."""

    def __init__(self, text):
        self.text = text
        self.kv = {}
        self.probe = []          # [(command, output)] in order
        self.sections = {}       # title -> lines
        title, cmd, buf = None, None, []
        for line in text.splitlines():
            if line.startswith("===== "):
                if cmd is not None:
                    self.probe.append((cmd, "\n".join(buf).strip("\n")))
                    cmd, buf = None, []
                title = line[6:].strip()
                self.sections.setdefault(title, [])
                continue
            if title is not None:
                self.sections[title].append(line)
            if line.startswith("$ "):
                if cmd is not None:
                    self.probe.append((cmd, "\n".join(buf).strip("\n")))
                cmd = re.sub(r"^adb shell ", "", line[2:].strip())
                buf = []
            elif cmd is not None:
                buf.append(line)
            else:
                m = re.match(r"^([A-Za-z][A-Za-z0-9_ ().,'/=-]{0,70}?):\s(.*)$", line)
                if m and m.group(1) not in self.kv:
                    self.kv[m.group(1).strip()] = m.group(2).strip()
        if cmd is not None:
            self.probe.append((cmd, "\n".join(buf).strip("\n")))

    def get(self, *needles):
        """The output of the first probe command that contains every needle, or None."""
        for cmd, out in self.probe:
            if all(n in cmd for n in needles):
                return out
        return None

    def first_line(self, *needles):
        out = self.get(*needles)
        if out is None:
            return None
        for ln in out.splitlines():
            if ln.strip():
                return ln.strip()
        return ""

    def section(self, startswith):
        for t, lines in self.sections.items():
            if t.startswith(startswith):
                return lines
        return None


def power_key_device(rep):
    out = rep.get("getevent -pl")
    if not out:
        return None
    dev = name = None
    found = []
    for ln in out.splitlines():
        t = ln.strip()
        if t.startswith("add device"):
            dev, name = t.split(":", 1)[-1].strip(), None
        elif t.startswith("name:"):
            name = t.split(":", 1)[-1].strip().strip('"')
        elif "KEY_POWER" in t and dev:
            found.append("%s (%s)" % (dev, name or "?"))
    return ", ".join(found) if found else "no input device lists KEY_POWER"


def facts_markdown(rep, source, when):
    kv = rep.kv
    flags, rows = [], []

    def row(q, a, src=""):
        rows.append((q, a if a not in (None, "") else "(not in the report)", src))

    helper = kv.get("shell helper reachable", "").lower()
    if helper == "no":
        flags.append("The shell helper was NOT running when this report was written: most of the probe below is empty. Ask Tj to start it (PART 2 of the phone guide) and collect again.")
    if kv.get("accessibility helper bound", "").lower() == "no":
        flags.append("The accessibility helper is not bound: the power-menu cover, lock-screen features and the clean-up do nothing until it is switched on.")
    if kv.get("crash breaker tripped", "").lower() == "yes":
        flags.append("The crash breaker TRIPPED: the launcher crashed repeatedly and fell back to the stock launcher. Read the crash and log sections first.")
    demo = (kv.get("retail-demo flag (must be 0 or not set)") or rep.first_line("device_demo_mode") or "").strip()
    if demo and demo not in ("0", "null", "(not set)"):
        flags.append("device_demo_mode = %s. It must be 0 or not set (retail demo mode can create a demo user at boot). Tell Tj to run the guide's UNDO card for it." % demo)
    users = rep.get("pm list users")
    nusers = len(re.findall(r"UserInfo\{", users)) if users else None
    if nusers and nusers > 1:
        flags.append("%d Android users exist (expected 1). Find out which and why before anything else." % nusers)
    perms = rep.section("this launcher's permissions") or []
    missing = [ln.split(":")[0] for ln in perms if "NOT granted" in ln and ln.split(":")[0] not in ("INTERNET",)]
    if missing:
        flags.append("Permissions NOT granted: %s. 'Do everything that is missing' (or the guide's FALLBACK) fixes that." % ", ".join(missing))
    sdk = kv.get("android", "")
    if sdk and not sdk.startswith("10 "):
        flags.append("Android is not 10 (%s): every Android-10 assumption in the project needs a second look." % sdk)
    crash = rep.get("logcat -b crash")
    if crash and crash.strip():
        flags.append("The crash log has %d line(s) about com.tj.launcher: read them." % len(crash.strip().splitlines()))
    sc_tasks = None
    m = re.search(r"Recents tasks parsed: (\d+)", rep.text)
    if m:
        sc_tasks = int(m.group(1))
    sample = rep.get("dumpsys activity recents", "Recent #")
    if sc_tasks == 0 and sample and "Recent #" in sample:
        flags.append("PARSER MISMATCH: the Recents sample shows tasks but the launcher's own reader parsed 0. The task-precise clean-up does NOT work on this ROM; the dump's shape differs from Android 10 AOSP.")
    nsample = rep.get("NotificationRecord(")
    mn = re.search(r"Apps with a notification now \(as the clean-up sees them\): (.*)", rep.text)
    if mn and nsample and "NotificationRecord(" in nsample and mn.group(1).strip() in ("(none)", "[]"):
        # The launcher's reader (ShellProtocol.parseNotifPackages) counts only an ACTIVE record of user 0 whose package has a dotted name. The system's own "pkg=android" (for example "USB debugging connected")
        # is not an app the clean-up can close, so a sample of only those is no mismatch (a stock emulator shows exactly that, run 119). A record shape the loose pattern cannot read at all still is one.
        recs = re.findall(r"NotificationRecord\(0x[0-9a-fA-F]+: pkg=(\S+) user=UserHandle\{(-?\d+)\}", nsample)   # -1 = all users: the system\'s own "pkg=android" records on a stock Android 10 (PG3, v0.7.6 gate)
        if not recs or any("." in p and u == "0" for p, u in recs):
            flags.append("PARSER MISMATCH: the notification sample shows records but the launcher's own reader found no app. The 'shared app with a notification' rule does not work on this ROM.")
    # The launcher stores a one-line verdict ("keytest_summary") computed from the WHOLE helper answer; the raw answer is cut by the report, so it is only a fallback for older reports.
    ksum = kv.get("keytest_summary", "")
    kout = kv.get("keytest_output", "")
    key = ksum or kout
    if key:
        seen = ("WAS seen" in ksum) if ksum else ("KEY_POWER" in kout)
        refused = ("REFUSED" in ksum or "no answer about switching" in ksum) if ksum else ("REFUSED" in kout)
        if "could NOT read the input devices" in ksum:
            flags.append("Side-key test: the shell could NOT read the input devices at all (getevent printed nothing): Power menu v2 through getevent is not possible on this tablet as it stands.")
        elif not seen:
            flags.append("Side-key test: the side key was NOT seen by the helper (not pressed in time, or the key is not on a device the shell can read). Repeat it once with the key held 1.5 s inside the 8 seconds before deciding.")
        if refused:
            flags.append("Side-key test: the shell did not get a clean OK when it tried to switch the normal power menu off.")
        if kv.get("keytest_menu") == "shown":
            flags.append("Side-key test: the NORMAL power menu still appeared while the flag was held: on this tablet the flag does not keep it away (Power menu v2 as designed will not work).")

    row("Launcher version / mode", "%s / %s" % (kv.get("launcher version", "?"), kv.get("mode", "?")), "state section")
    row("Tablet", "%s; Android %s" % (kv.get("model", "?"), kv.get("android", "?")), "app side")
    row("Build", rep.first_line("ro.build.fingerprint") or kv.get("fingerprint"), "getprop ro.build.fingerprint")
    row("LineageOS?", rep.first_line("ro.lineage.version") or "no (property empty)", "getprop ro.lineage.version")
    row("Low-RAM (Go) device", rep.first_line("ro.config.low_ram") or kv.get("low-RAM (Go) device"), "")
    mem = rep.get("head -3 /proc/meminfo")
    row("Memory", " | ".join(" ".join(x.split()) for x in mem.splitlines()[:3]) if mem else None, "/proc/meminfo")
    row("A real second Android user possible?", (rep.first_line("pm get-max-users") or "") + ("; users now: %d" % nusers if nusers else ""), "pm get-max-users, pm list users")
    row("Encryption (helper before first unlock?)", "type=%s state=%s" % (rep.first_line("ro.crypto.type") or "?", rep.first_line("ro.crypto.state") or "?"), "getprop ro.crypto.*")
    listening = rep.first_line("netstat", "grep -c")
    row("ADB over the network kept across restarts?", "service.adb.tcp.port='%s' persist.adb.tcp.port='%s'; listening on :555x now: %s" % (rep.first_line("service.adb.tcp.port") or "", rep.first_line("persist.adb.tcp.port") or "", listening if listening is not None else "?"), "getprop, netstat count")
    pma = rep.first_line("power_menu_actions")
    lin = rep.get("content://lineagesettings/secure")
    if lin is None:
        lin_txt = "(not in the report)"
    elif not lin.strip() or "Error" in lin or "No result" in lin:
        lin_txt = "no LineageOS settings provider answered (this is not LineageOS, or nothing matched)"
    else:
        lin_txt = "%d line(s) of LineageOS power/button settings" % len(lin.splitlines())
    row("ROM's own power-menu setting (hide Screenshot natively?)", "power_menu_actions=%s; %s" % (pma or "null", lin_txt), "settings get secure power_menu_actions")
    row("Side key: which input device", power_key_device(rep), "getevent -pl")
    if key:
        row("Side-key test (Power menu v2)", "%s; normal menu appeared while the flag was held: %s" % (key[:300], kv.get("keytest_menu", "(not answered)")), "KEYTEST + Tj's answer")
    else:
        row("Side-key test (Power menu v2)", "not done (phone guide PART 3, button 3)", "")
    row("Status bar customised? (policy_control)", rep.first_line("settings get global policy_control") or kv.get("status-bar hiding"), "settings get global policy_control")
    row("Recents format check", ("the launcher's reader parsed %d task(s)" % sc_tasks) if sc_tasks is not None else None, "PARSER SELF-CHECK")
    row("Notification format check", mn.group(1).strip() if mn else None, "PARSER SELF-CHECK")
    row("Keep-alive", "running=%s; services: %s" % (kv.get("keep-alive service running", "?"), "; ".join(" ".join(x.split()) for x in (rep.get("dumpsys activity services com.tj.launcher") or "").splitlines()[:3])), "state + dumpsys")
    cost = " | ".join(kv[k] for k in ("process memory (AD8)", "start-up (AD8)", "accessibility helper load (AD8)") if kv.get(k))
    row("Launcher cost on this tablet (AD8: memory, start-up, helper load)", cost or None, "state section")
    adj = rep.get("dumpsys activity processes")
    row("Launcher process priority", "; ".join(x.strip() for x in (adj or "").splitlines()[:3]) if adj else None, "dumpsys activity processes")
    row("Compiled ahead of time?", "; ".join(x.strip() for x in (rep.get("dexopt") or "").splitlines()[:3]), "dumpsys package ... dexopt")
    row("Quick-settings tiles", "setting=%s; SystemUI has: %s" % (kv.get("quick-settings tiles setting now", "?"), " ".join((rep.get("SystemUIService") or "").split())), "state + SystemUIService")
    row("Last unlock", kv.get("unlock_speed") or kv.get("unlock_pick_last"), "state section (allow-listed)")
    row("Last unlock, start to finish (BK1c)", kv.get("unlock_sum"), "state section (allow-listed): Enter tap -> custom home on screen, with the cover and the Launcher resume")
    row("Last clean-up", kv.get("reset_last"), "state section")
    logt = rep.get("logcat -d -v time")
    if logt is not None:
        lines = [l for l in logt.splitlines() if l.strip()]
        errs = [l for l in lines if re.search(r"\sE[/\s]|FATAL|Exception", l)]   # "-v time" writes the level as "E/Tag(", "-v threadtime" as " E Tag:" (code review, 2026-11-05: the first form was never matched)
        locks = [l for l in lines if "AT LOCK" in l]
        row("Launcher log (tail)", "%d line(s); %d error-looking; last clean-up: %s" % (len(lines), len(errs), (locks[-1][:200] if locks else "none in the tail")), "logcat -d -s Tj*")
        if errs:
            flags.append("The launcher's log tail has %d error-looking line(s), first: %s" % (len(errs), errs[0][:200]))

    if "TABLET SUMMARY (short" in rep.text:
        flags_note = "This was the SHORT summary (shared as text): it has no shell probe, so every row marked (not in the report) is just not covered. Ask Tj for tablet-report.txt when one of those rows matters."
    else:
        flags_note = None
    md = ["# Tablet facts", "",
          "Generated by `tools/tablet_intake.py` on %s from `%s`. **Read this first, then the raw report next to it.** Private details were redacted before this was saved." % (when, source), "",
          "## Red flags (read first)", ""]
    md += ["- " + f for f in flags] if flags else ["- none found by the automatic checks (still read the table)."]
    if flags_note:
        md += ["", "> " + flags_note]
    md += ["", "## Answers to the open questions", "", "| Question | Answer | Source |", "| :- | :- | :- |"]
    for q, a, s in rows:
        md.append("| %s | %s | %s |" % (q, str(a).replace("|", "/").replace("\n", " "), s))
    md.append("")
    return "\n".join(md), flags


# ---------------------------------------------------------------- the intake
def next_dir(base, date):
    d = os.path.join(base, date)
    n = 2
    while os.path.exists(d):
        d = os.path.join(base, "%s-%d" % (date, n))
        n += 1
    return d


def run(src, out_base, label, date, keep_images):
    if src != "-" and not os.path.exists(src):
        print("not found: %s" % src)
        return 2
    notes, kept, skipped, apks = [], [], [], []
    report_text = None
    staged = {}                      # file name -> redacted text
    images = {}
    redactions = {}
    img_total = 0
    single = src == "-" or not (os.path.isdir(src) or zipfile.is_zipfile(src))
    for rel, size, reader in entries(src):
        if rel.startswith("UNSAFE:"):
            skipped.append("%s (unsafe path, ignored)" % rel[7:])
            continue
        name = safe_name(rel)
        low = (name or "").lower()
        if single:
            # the caller named ONE file on purpose: it is the report, whatever it is called, as long as it is text
            if size > MAX_TEXT:
                skipped.append("%s (%d bytes, over the text limit)" % (rel, size))
                continue
            raw = reader(MAX_TEXT + 1)
            if b"\x00" in raw[:4096]:
                skipped.append("%s (not a text file)" % rel)
                continue
            text = raw.decode("utf-8", "replace")
            name = "tablet-summary.txt" if "TABLET SUMMARY (short" in text[:300] else "tablet-report.txt"
            text, c = redact(text)
            for k, v in c.items():
                redactions[k] = redactions.get(k, 0) + v
            staged[name] = text
        elif name is None:
            skipped.append("%s (name not allowed)" % rel)
        elif TEXT_RX.match(name):
            if size > MAX_TEXT:
                skipped.append("%s (%d bytes, over the text limit)" % (rel, size))
                continue
            text = reader(MAX_TEXT + 1).decode("utf-8", "replace")
            text, c = redact(text)
            for k, v in c.items():
                redactions[k] = redactions.get(k, 0) + v
            staged[name] = text
        elif low.endswith(".xml") and "/shots/" in "/" + rel and size <= MAX_XML:
            text = reader(MAX_XML + 1).decode("utf-8", "replace")
            text, c = redact(text)
            for k, v in c.items():
                redactions[k] = redactions.get(k, 0) + v
            staged["shots-" + name] = text
        elif low.endswith(".apk"):
            apks.append((name, size, sha256_of(reader, rel, src)))
        elif low.endswith(".png") and keep_images and size <= MAX_IMG and img_total + size <= MAX_IMG_TOTAL:
            images["shots-" + name] = reader(MAX_IMG + 1)
            img_total += size
        else:
            skipped.append("%s (%d bytes: not a text report; images and system apps are not copied)" % (rel, size))
    if not staged:
        print("no tablet-report / tablet-summary / launcher-probe / status .txt file (a text file) found in %s" % src)
        return 3
    for name, text in staged.items():
        if CREDENTIALS.search(text):
            print("REFUSED: %s contains something shaped like a live credential. Nothing was saved. Remove it and run again." % name)
            return 4
    def newest(prefix):
        """The staged file that starts with prefix and is the NEWEST: a browser calls a second download 'name (1)' and a third 'name (2)' (safe_name turns that into name__1_), so the highest number wins and no number is the oldest."""
        names = [n for n in staged if n.lower().startswith(prefix)]
        def rank(n):
            m = re.search(r"__(\d+)_(?:\.|$)", n)
            return (int(m.group(1)) if m else 0, n)
        return max(names, key=rank) if names else None
    # the full report first, then the PC route's probe, then the short summary (status.txt alone has no facts to parse)
    picked = newest("tablet-report") or newest("launcher-probe") or newest("tablet-summary")
    report_text = staged[picked] if picked else "\n".join(staged.values())
    reading = "FACTS.md was written from %s" % (picked or "all the text files together")
    if picked and sum(1 for n in staged if n.lower().startswith("tablet-report")) > 1 and picked.lower().startswith("tablet-report"):
        reading += " (the newest of %d tablet reports, by the ' (N)' number a browser adds to a later download; the others are saved beside it)" % sum(1 for n in staged if n.lower().startswith("tablet-report"))
    dest = next_dir(out_base, date)
    os.makedirs(dest, exist_ok=True)
    for name, text in staged.items():
        with open(os.path.join(dest, name), "w", encoding="utf-8") as f:
            f.write(text)
        kept.append(name)
    for name, data in images.items():
        with open(os.path.join(dest, name), "wb") as f:
            f.write(data)
        kept.append(name)
    if apks:
        with open(os.path.join(dest, "apk-list.txt"), "w", encoding="utf-8") as f:
            for name, size, digest in apks:
                f.write("%s  %d bytes  sha256 %s\n" % (name, size, digest))
        kept.append("apk-list.txt")
    rep = Report(report_text)
    src_name = "pasted text" if src == "-" else os.path.basename(src.rstrip("/"))
    md, flags = facts_markdown(rep, label or src_name, date)
    with open(os.path.join(dest, "FACTS.md"), "w", encoding="utf-8") as f:
        f.write(md)
    with open(os.path.join(dest, "INTAKE.txt"), "w", encoding="utf-8") as f:
        f.write("source: %s\nlabel: %s\ndate: %s\nkept: %s\nread: %s\nredactions: %s\nskipped:\n%s\n" % (
            src_name, label, date, ", ".join(kept), reading, redactions or "none", "\n".join("  " + s for s in skipped) or "  (nothing)"))
    latest = os.path.join(out_base, "LATEST.md")
    with open(latest, "w", encoding="utf-8") as f:
        f.write("# Latest tablet intake: %s\n\nSaved in the folder `%s/` next to this file (its raw report and screen files sit beside its FACTS.md). Older intakes stay in their own folders.\n\n" % (os.path.basename(dest), os.path.basename(dest)))
        f.write(md.split("\n", 1)[1])
    print("saved %s  (kept: %s; redactions: %s; %d red flag(s))" % (dest, ", ".join(kept), redactions or "none", len(flags)))
    if "newest of" in reading:
        print("  note: " + reading)
    for fl in flags:
        print("  RED FLAG: " + fl)
    print("next: read %s, then `git add research/tablet && bash tools/ckpt.sh \"tablet intake %s\" \"...\"`" % (os.path.join(dest, "FACTS.md"), date))
    return 0


def main(argv):
    ap = argparse.ArgumentParser(description="Save and summarise what Tj sends from the tablet.")
    ap.add_argument("src")
    ap.add_argument("--label", default="")
    ap.add_argument("--out", default=os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "research", "tablet"))
    ap.add_argument("--date", default=datetime.date.today().isoformat())
    ap.add_argument("--keep-images", action="store_true", help="also keep small PNGs (only when Tj agrees: a screenshot cannot be redacted)")
    a = ap.parse_args(argv)
    out = os.path.abspath(a.out)
    os.makedirs(out, exist_ok=True)
    return run(a.src, out, a.label, a.date, a.keep_images)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
