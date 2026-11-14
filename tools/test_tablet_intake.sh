#!/usr/bin/env bash
# test_tablet_intake.sh — self-test of tools/tablet_intake.py (runs inside tools/test_handoff.sh; ~2 s, no network, writes only to a temp dir).
# Pins the rules that keep a tablet report SAFE to commit and USEFUL to read:
#   private strings are redacted, a credential makes the intake refuse, a zip is never extracted (zip-slip names are ignored), sizes are capped,
#   system apps / screenshots are listed but not copied, every red flag the report can show is raised, and nothing is written outside --out.
#   bash tools/test_tablet_intake.sh        # exit 0 = all green
set -uo pipefail
D="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
T="$(mktemp -d)"; trap 'rm -rf "$T"' EXIT
PASS=0; FAIL=0
ok(){ PASS=$((PASS+1)); }
bad(){ FAIL=$((FAIL+1)); echo "  FAIL  $1"; }
check(){ if eval "$2" >/dev/null 2>&1; then ok; else bad "$1"; fi; }
SAMPLE="$D/tools/testdata/tablet-sample/tablet-report.txt"
INTAKE="bash $D/tools/tablet-intake.sh"
PRIVATE='R52N90ABCDE|192\.168\.1\.77|aa:bb:cc:dd:ee:ff|a@example\.com|356938035643809|HomeNet-5G'

check "the sample report exists and holds all six planted private strings" '[ "$(grep -oE "$PRIVATE" "$SAMPLE" | sort -u | wc -l)" = 6 ]'

# ---------------------------------------------------------------- a folder (the PC route / the Downloads folder): text kept + redacted, apk and png listed or dropped
mkdir -p "$T/in/shots" "$T/in/apk"
cp "$SAMPLE" "$T/in/tablet-report.txt"
echo "== adb-all 2026-11-05  tablet: Acme Tab10" > "$T/in/status.txt"
head -c 4000 /dev/urandom > "$T/in/apk/com.android.settings.apk"
printf '\x89PNG\r\n\x1a\n' > "$T/in/shots/SETTINGS.png"; head -c 2000 /dev/urandom >> "$T/in/shots/SETTINGS.png"
echo '<hierarchy><node text="Wi-Fi" /></hierarchy>' > "$T/in/shots/SETTINGS.xml"
ln -s /etc/passwd "$T/in/passwd-link.txt"
SUM="$(sha256sum "$T/in/apk/com.android.settings.apk" | cut -d' ' -f1)"
OUT1="$($INTAKE "$T/in" --out "$T/out" --date 2026-11-05 --label folder 2>&1)"; RC1=$?
check "folder: exit 0"                                   '[ "$RC1" = 0 ]'
check "folder: FACTS.md, INTAKE.txt, report, status, LATEST.md exist" '[ -f "$T/out/2026-11-05/FACTS.md" ] && [ -f "$T/out/2026-11-05/INTAKE.txt" ] && [ -f "$T/out/2026-11-05/tablet-report.txt" ] && [ -f "$T/out/2026-11-05/status.txt" ] && [ -f "$T/out/LATEST.md" ]'
check "folder: NO planted private string survives anywhere in the output" '! grep -rE "$PRIVATE" "$T/out"'
check "folder: the redaction markers are there (and the facts are still readable)" 'grep -q "<email>" "$T/out/2026-11-05/tablet-report.txt" && grep -q "<ip>" "$T/out/2026-11-05/tablet-report.txt" && grep -q "<mac>" "$T/out/2026-11-05/tablet-report.txt" && grep -q "Acme Tab10" "$T/out/2026-11-05/FACTS.md"'
check "folder: the apk is listed with its sha256 but NOT copied" 'grep -q "$SUM" "$T/out/2026-11-05/apk-list.txt" && [ -z "$(find "$T/out" -name "*.apk")" ]'
check "folder: the png is NOT copied by default"           '[ -z "$(find "$T/out" -name "*.png")" ]'
check "folder: the screen-structure xml is kept"           '[ -f "$T/out/2026-11-05/shots-SETTINGS.xml" ]'
check "folder: a symlink inside the folder is not followed" '! grep -rq "root:" "$T/out"'
check "folder: a clean sample shows no red flag"           'grep -q "none found by the automatic checks" "$T/out/2026-11-05/FACTS.md"'
check "folder: the table answers the open questions"       'grep -q "Side-key test (Power menu v2) | the side key WAS seen" "$T/out/2026-11-05/FACTS.md" && grep -q "Recents format check | the launcher.s reader parsed 2" "$T/out/2026-11-05/FACTS.md" && grep -q "Encryption" "$T/out/2026-11-05/FACTS.md"'
check "folder: CD1 the stock launcher's memory row reads the dump (PSS, parts, activities, procstats, RSS, biggest) and a normal size raises NO flag" 'grep -q "Stock launcher (Quickstep) memory (CD1) | PSS now 117 MB (java heap 20, native heap 29, code 40, graphics 17" "$T/out/2026-11-05/FACTS.md" && grep -q "3 activities, 412 views" "$T/out/2026-11-05/FACTS.md" && grep -q "RSS 126 MB" "$T/out/2026-11-05/FACTS.md" && grep -q "biggest now: 190,000K: com.android.systemui" "$T/out/2026-11-05/FACTS.md" && ! grep -q "STOCK launcher (Quickstep, com.android.launcher3) holds" "$T/out/2026-11-05/FACTS.md"'
check "folder: LATEST.md is the newest FACTS.md"           'grep -q "Latest tablet intake: 2026-11-05" "$T/out/LATEST.md" && grep -q "Red flags" "$T/out/LATEST.md"'
OUT2="$($INTAKE "$T/in" --out "$T/out" --date 2026-11-05 2>&1)"
check "a second intake on the same day gets its own folder (nothing is overwritten)" '[ -d "$T/out/2026-11-05-2" ] && [ -f "$T/out/2026-11-05/FACTS.md" ] && grep -q "2026-11-05-2" "$T/out/LATEST.md"'

# ---------------------------------------------------------------- CD1: a stock launcher at 450 MB raises a red flag (and a report without the dump just has an empty row)
mkdir -p "$T/big"; sed 's/TOTAL:       120000/TOTAL:       460000/' "$SAMPLE" > "$T/big/tablet-report.txt"
$INTAKE "$T/big" --out "$T/outbig" --date 2026-11-07 >/dev/null 2>&1
check "CD1: a stock launcher at 450 MB raises the red flag with its size" 'grep -q "STOCK launcher (Quickstep, com.android.launcher3) holds 449 MB" "$T/outbig/2026-11-07/FACTS.md"'
mkdir -p "$T/nodump"; grep -v 'launcher3' "$SAMPLE" > "$T/nodump/tablet-report.txt"
$INTAKE "$T/nodump" --out "$T/outnd" --date 2026-11-08 >/dev/null 2>&1; RCND=$?
check "CD1: a report without the dump still runs and shows an empty (not made-up) answer" '[ "$RCND" = 0 ] && grep -q "Stock launcher (Quickstep) memory (CD1) | (not in the report)" "$T/outnd/2026-11-08/FACTS.md"'

# ---------------------------------------------------------------- --keep-images keeps ONLY a small png
head -c 400000 /dev/urandom > "$T/in/shots/BIG.png"
$INTAKE "$T/in" --out "$T/out2" --date 2026-11-06 --keep-images >/dev/null 2>&1
check "--keep-images keeps the small png and drops the big one" '[ -f "$T/out2/2026-11-06/shots-SETTINGS.png" ] && [ ! -f "$T/out2/2026-11-06/shots-BIG.png" ]'

# ---------------------------------------------------------------- a ZIP: never extracted; zip-slip and absolute names ignored; the real report still read
python3 -I - "$T" "$SAMPLE" <<'PY'
import sys, zipfile
t, sample = sys.argv[1], sys.argv[2]
with zipfile.ZipFile(t + "/in.zip", "w") as z:
    z.write(sample, "launcher-probe/tablet-report.txt")
    z.writestr("../evil.txt", "owned")
    z.writestr("/abs-evil.txt", "owned")
    z.writestr("launcher-probe/../../evil2.txt", "owned")
    z.writestr("launcher-probe/apk/x.apk", b"APKDATA" * 100)
PY
$INTAKE "$T/in.zip" --out "$T/outz/sub" --date 2026-11-05 >/dev/null 2>&1; RCZ=$?
check "zip: exit 0 and the report was read"             '[ "$RCZ" = 0 ] && [ -f "$T/outz/sub/2026-11-05/FACTS.md" ]'
check "zip: nothing was written outside --out (zip-slip)" '[ ! -e "$T/outz/evil.txt" ] && [ ! -e "$T/evil.txt" ] && [ ! -e "$T/outz/evil2.txt" ] && [ ! -e /abs-evil.txt ] && [ "$(find "$T" -name "*evil*" | wc -l)" = 0 ]'
check "zip: unsafe member names are listed as ignored"   'grep -q "unsafe path" "$T/outz/sub/2026-11-05/INTAKE.txt"'
check "zip: nothing from the zip was extracted to disk"  '[ -z "$(find "$T/outz" -name "*.apk")" ] && grep -q "x.apk" "$T/outz/sub/2026-11-05/apk-list.txt"'

# ---------------------------------------------------------------- a single file, and the report text on standard input (Tj pasted it into the chat)
$INTAKE "$SAMPLE" --out "$T/outf" --date 2026-11-05 >/dev/null 2>&1
check "a single file works"                              '[ -f "$T/outf/2026-11-05/FACTS.md" ]'
cp "$SAMPLE" "$T/emulator report (1).txt"
$INTAKE "$T/emulator report (1).txt" --out "$T/outw" --date 2026-11-05 >/dev/null 2>&1; RCW=$?
check "a single file works under ANY name (a browser adds ' (1)'; the emulator test passes x-report.txt)" '[ "$RCW" = 0 ] && [ -f "$T/outw/2026-11-05/tablet-report.txt" ] && grep -q "Acme Tab10" "$T/outw/2026-11-05/FACTS.md"'
head -c 3000 /dev/urandom > "$T/binary.bin"
$INTAKE "$T/binary.bin" --out "$T/outbin" --date 2026-11-05 >/dev/null 2>&1; RCBIN=$?
check "a single binary file is refused as not text (exit 3, nothing saved)" '[ "$RCBIN" = 3 ] && [ -z "$(find "$T/outbin" -type f 2>/dev/null)" ]'
mkdir -p "$T/dl"; cp "$SAMPLE" "$T/dl/tablet-report (1).txt"; echo "== adb-all" > "$T/dl/status 2026-11-05.txt"; echo "x" > "$T/dl/notes.txt"
$INTAKE "$T/dl" --out "$T/outdl" --date 2026-11-05 >/dev/null 2>&1
check "a folder member named 'tablet-report (1).txt' is read (saved with a safe name); unrelated .txt files are skipped" '[ -f "$T/outdl/2026-11-05/tablet-report__1_.txt" ] && [ -f "$T/outdl/2026-11-05/status_2026-11-05.txt" ] && [ ! -f "$T/outdl/2026-11-05/notes.txt" ] && grep -q "Acme Tab10" "$T/outdl/2026-11-05/FACTS.md"'
$INTAKE - --out "$T/outs" --date 2026-11-05 < "$SAMPLE" >/dev/null 2>&1
check "standard input works and says where it came from" '[ -f "$T/outs/2026-11-05/FACTS.md" ] && grep -q "pasted text" "$T/outs/2026-11-05/FACTS.md" && ! grep -rE "$PRIVATE" "$T/outs"'

# ---------------------------------------------------------------- the SHORT summary Tj can share as text (no shell probe): the state rows still work, and FACTS.md says what is missing
sed -n '1,/^===== SHELL PROBE/p' "$SAMPLE" | sed '$d' | sed '1s/.*/===== TABLET SUMMARY (short: the full report is the file tablet-report.txt) Thu Nov 05 06:10:00 GMT 2026/' > "$T/summary.txt"
$INTAKE - --out "$T/outsum" --date 2026-11-05 --label summary < "$T/summary.txt" >/dev/null 2>&1; RCS=$?
FS="$T/outsum/2026-11-05/FACTS.md"
check "the short summary is read (exit 0)"                '[ "$RCS" = 0 ] && [ -f "$FS" ]'
check "the short summary: FACTS.md says it has no probe"  'grep -q "SHORT summary" "$FS" && grep -q "(not in the report)" "$FS"'
check "the short summary: the launcher state rows are filled in" 'grep -q "Launcher version / mode | 0.7.0 (code 17) / custom" "$FS" && grep -q "Side-key test (Power menu v2) | the side key WAS seen" "$FS" && grep -q "Recents format check | the launcher.s reader parsed 2" "$FS"'
check "the short summary: private strings are redacted here too" '! grep -rE "$PRIVATE" "$T/outsum"'

# ---------------------------------------------------------------- a credential: refuse the WHOLE intake, save nothing
mkdir -p "$T/cred"
{ cat "$SAMPLE"; printf 'leaked: ghp_%s\n' "$(printf 'k%.0s' $(seq 1 36))"; } > "$T/cred/tablet-report.txt"
OUTC="$($INTAKE "$T/cred" --out "$T/outc" --date 2026-11-05 2>&1)"; RCC=$?
check "credential: refused (exit 4) and says so"         '[ "$RCC" = 4 ] && printf "%s" "$OUTC" | grep -q REFUSED'
check "credential: NOTHING was saved"                    '[ -z "$(find "$T/outc" -type f 2>/dev/null)" ]'

# ---------------------------------------------------------------- caps and wrong input
mkdir -p "$T/big"; head -c 13000000 /dev/zero > "$T/big/tablet-report.txt"
OUTB="$($INTAKE "$T/big" --out "$T/outb" --date 2026-11-05 2>&1)"; RCB=$?
check "a report over the size cap is not read (exit 3, nothing saved)" '[ "$RCB" = 3 ] && [ -z "$(find "$T/outb" -type f 2>/dev/null)" ]'
OUTN="$($INTAKE "$T/does-not-exist" --out "$T/outn" 2>&1)"; RCN=$?
check "a missing input is reported (exit 2)"             '[ "$RCN" = 2 ] && printf "%s" "$OUTN" | grep -q "not found"'
mkdir -p "$T/empty"; echo hi > "$T/empty/readme.txt"
$INTAKE "$T/empty" --out "$T/oute" >/dev/null 2>&1; RCE=$?
check "a folder with no report is reported (exit 3)"     '[ "$RCE" = 3 ]'

# ---------------------------------------------------------------- every red flag the report can show is raised (one bad tablet, all at once)
mkdir -p "$T/badin"
python3 -I - "$SAMPLE" "$T/badin/tablet-report.txt" <<'PY'
import sys
s = open(sys.argv[1]).read()
def rep(a, b):
    global s
    assert a in s, a
    s = s.replace(a, b)
rep("shell helper reachable: yes", "shell helper reachable: no")
rep("accessibility helper bound: yes", "accessibility helper bound: no")
rep("crash breaker tripped: no", "crash breaker tripped: yes")
rep("retail-demo flag (must be 0 or not set): (not set)", "retail-demo flag (must be 0 or not set): 1")
rep("DUMP: granted", "DUMP: NOT granted")
rep("android: 10 (API 29)", "android: 11 (API 30)")
rep("\tUserInfo{0:Owner:13} running", "\tUserInfo{0:Owner:13} running\n\tUserInfo{10:Guest:4} running")
rep("Recents tasks parsed: 2", "Recents tasks parsed: 0")
rep("Apps with a notification now (as the clean-up sees them): [com.android.systemui, com.whatsapp]", "Apps with a notification now (as the clean-up sees them): (none)")
rep("keytest_summary: the side key WAS seen (held 1.5 s); the shell MAY switch the normal power menu off", "keytest_summary: the side key was NOT seen (no KEY_POWER event in 9 s); the shell was REFUSED when it tried to switch the normal power menu off")
rep("keytest_menu: not_shown", "keytest_menu: shown")
rep("$ logcat -b crash -d -v time | grep -A12 'com.tj.launcher' | head -60\n", "$ logcat -b crash -d -v time | grep -A12 'com.tj.launcher' | head -60\nFATAL EXCEPTION: main\n  Process: com.tj.launcher, PID: 4242\n")
open(sys.argv[2], "w").write(s)
PY
$INTAKE "$T/badin" --out "$T/outbad" --date 2026-11-05 >/dev/null 2>&1
F="$T/outbad/2026-11-05/FACTS.md"
check "red flag: helper not running"                      'grep -q "shell helper was NOT running" "$F"'
check "red flag: accessibility helper not bound"          'grep -q "accessibility helper is not bound" "$F"'
check "red flag: crash breaker tripped"                   'grep -q "crash breaker TRIPPED" "$F"'
check "red flag: retail-demo flag"                        'grep -q "device_demo_mode = 1" "$F"'
check "red flag: a permission is missing"                 'grep -q "Permissions NOT granted: DUMP" "$F"'
check "red flag: not Android 10"                          'grep -q "Android is not 10" "$F"'
check "red flag: more than one Android user"              'grep -q "2 Android users" "$F"'
check "red flag: Recents reader mismatch"                 'grep -q "PARSER MISMATCH: the Recents sample" "$F"'
check "red flag: notification reader mismatch"            'grep -q "PARSER MISMATCH: the notification sample" "$F"'
check "red flag: side key not seen"                       'grep -q "side key was NOT seen" "$F"'
check "red flag: shell refused to switch the menu off"    'grep -q "did not get a clean OK" "$F"'
check "red flag: normal menu still appeared"              'grep -q "NORMAL power menu still appeared" "$F"'
check "red flag: a crash log line"                        'grep -q "crash log has" "$F"'
# the shell could not read the input devices at all (getevent printed nothing): a different, stronger flag than "not pressed"
python3 -I - "$SAMPLE" "$T/badin/tablet-report.txt" <<'PY'
import sys
s = open(sys.argv[1]).read()
a = "keytest_summary: the side key WAS seen (held 1.5 s); the shell MAY switch the normal power menu off"
assert a in s
s = s.replace(a, "keytest_summary: the shell could NOT read the input devices (getevent printed nothing), so this test cannot say whether the side key is readable; the shell MAY switch the normal power menu off")
open(sys.argv[2], "w").write(s)
PY
$INTAKE "$T/badin" --out "$T/outbad2" --date 2026-11-05 >/dev/null 2>&1
check "red flag: the shell could not read the input devices at all" 'grep -q "could NOT read the input devices at all" "$T/outbad2/2026-11-05/FACTS.md" && ! grep -q "side key was NOT seen by the helper" "$T/outbad2/2026-11-05/FACTS.md"'
# the launcher's reader skips the system's own "pkg=android" records (not an app it can close): a sample of ONLY those is no mismatch (a stock emulator shows exactly that, run 119 of 2026-11-05);
# a record shape the reader cannot parse at all still is one
python3 -I - "$SAMPLE" "$T/sysin/tablet-report.txt" "$T/shapein/tablet-report.txt" "$T/sysall/tablet-report.txt" <<'PY'
import os, sys
s = open(sys.argv[1]).read()
a = "Apps with a notification now (as the clean-up sees them): [com.android.systemui, com.whatsapp]"
assert a in s
s = s.replace(a, "Apps with a notification now (as the clean-up sees them): (none)")
w, u = "pkg=com.whatsapp user=UserHandle{0}", "pkg=com.android.systemui user=UserHandle{0}"
assert w in s and u in s
variants = ((sys.argv[2], s.replace(w, "pkg=android user=UserHandle{0}").replace(u, "pkg=android user=UserHandle{0}")),
            (sys.argv[3], s.replace(w, "package com.whatsapp userId 0").replace(u, "package com.android.systemui userId 0")),
            (sys.argv[4], s.replace(w, "pkg=android user=UserHandle{-1}").replace(u, "pkg=android user=UserHandle{-1}")))   # v0.7.6 gate: a stock emulator lists its own records for ALL users (-1)
for path, text in variants:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, "w").write(text)
PY
$INTAKE "$T/sysin" --out "$T/outsys" --date 2026-11-05 >/dev/null 2>&1
$INTAKE "$T/shapein" --out "$T/outshape" --date 2026-11-05 >/dev/null 2>&1
$INTAKE "$T/sysall" --out "$T/outsysall" --date 2026-11-05 >/dev/null 2>&1
check "no notification mismatch flag when the sample holds only the system's pkg=android records" '[ -f "$T/outsys/2026-11-05/FACTS.md" ] && ! grep -q "PARSER MISMATCH: the notification sample" "$T/outsys/2026-11-05/FACTS.md"'
check "no notification mismatch flag when the system records are for ALL users (UserHandle{-1})" '[ -f "$T/outsysall/2026-11-05/FACTS.md" ] && ! grep -q "PARSER MISMATCH: the notification sample" "$T/outsysall/2026-11-05/FACTS.md"'
check "a notification record shape the reader cannot parse is still a mismatch" 'grep -q "PARSER MISMATCH: the notification sample" "$T/outshape/2026-11-05/FACTS.md"'

# code review of 2026-11-05: (1) logcat "-v time" writes the level as "E/Tag(", which the error detector used to miss
python3 -I - "$SAMPLE" "$T/logerr/tablet-report.txt" <<'PY'
import os, sys
s = open(sys.argv[1]).read()
a = "11-05 06:01:11.201 I/TjReset( 4242): AT LOCK: cleaned 6 apps in 412 ms"
assert a in s
os.makedirs(os.path.dirname(sys.argv[2]), exist_ok=True)
open(sys.argv[2], "w").write(s.replace(a, a + "\n11-05 06:02:00.000 E/TjReset( 4242): boom"))
PY
$INTAKE "$T/logerr" --out "$T/outlogerr" --date 2026-11-05 >/dev/null 2>&1
check "an E/ line in the launcher's log (logcat -v time) is counted as an error and flagged" 'grep -q "log tail has 1 error-looking line" "$T/outlogerr/2026-11-05/FACTS.md"'
check "the unchanged sample log has no error-looking line" 'grep -q "0 error-looking" "$T/outf/2026-11-05/FACTS.md"'
# (2) IPv6 addresses are redacted; things that only look like one (::1, std::string, a time of day) are left alone
{ cat "$SAMPLE"; printf '%s\n' "link: fe80::a1b2:c3d4:e5f6:7a8b%wlan0 and 2001:db8:85a3::8a2e:370:7334 and 2001:0db8:0000:0000:0000:ff00:0042:8329 end" "loopback ::1 and std::string and the time 06:49:20 end"; } > "$T/v6.txt"
$INTAKE "$T/v6.txt" --out "$T/outv6" --date 2026-11-05 >/dev/null 2>&1
V6="$T/outv6/2026-11-05/tablet-report.txt"
check "IPv6 addresses are redacted (three of them, nothing left of the digits)" '[ -f "$V6" ] && ! grep -qE "fe80::a1b2|2001:db8:85a3|2001:0db8:0000" "$V6" && [ "$(grep -o "<ipv6>" "$V6" | wc -l)" = 3 ]'
check "what only looks like an address is left alone (::1, std::string, a time of day)" 'grep -q "loopback ::1 and std::string and the time 06:49:20 end" "$V6"'
# (3) two reports sent together (a browser numbers the second download " (1)"): the newest is read, both are kept, and INTAKE.txt says so
mkdir -p "$T/two"
sed 's/Acme Tab10/OldTabX/g' "$SAMPLE" > "$T/two/tablet-report.txt"
sed 's/Acme Tab10/NewTabY/g' "$SAMPLE" > "$T/two/tablet-report (1).txt"
$INTAKE "$T/two" --out "$T/outtwo" --date 2026-11-05 >/dev/null 2>&1
check "with two reports FACTS.md comes from the newest (' (1)'), not the alphabetically first" 'grep -q "NewTabY" "$T/outtwo/2026-11-05/FACTS.md" && ! grep -q "OldTabX" "$T/outtwo/2026-11-05/FACTS.md"'
check "both reports are kept and INTAKE.txt names the one that was read" '[ -f "$T/outtwo/2026-11-05/tablet-report.txt" ] && [ -f "$T/outtwo/2026-11-05/tablet-report__1_.txt" ] && grep -q "read: FACTS.md was written from tablet-report__1_.txt (the newest of 2" "$T/outtwo/2026-11-05/INTAKE.txt"'
check "the red flags come BEFORE the table"               '[ "$(grep -n "## Red flags" "$F" | cut -d: -f1)" -lt "$(grep -n "## Answers" "$F" | cut -d: -f1)" ]'

# ---------------------------------------------------------------- the guide tells Tj to send the file this tool reads
check "the phone guide names tablet-report.txt as the file to send" 'grep -q "tablet-report.txt" "$D/phoneguide/src/main/assets/steps.txt"'
check "research/tablet/README.md explains the flow"      '[ -f "$D/research/tablet/README.md" ] && grep -q "tablet-intake.sh" "$D/research/tablet/README.md"'

# ---------------------------------------------------------------- Recents card titles (v0.7.5 AI1 diagnostics) never reach the repo
mkdir -p "$T/cards"
printf '%s\n' '== lock log' '06:01:02 RESET recents: closed 1 card(s) of Portfolio; saw 2 card(s): Portfolio - Quarterly Secret Page Title | Solitaire' > "$T/cards/tablet-report.txt"
$INTAKE "$T/cards" --out "$T/outcards" --date 2026-11-06 --label cards >/dev/null 2>&1; RCC=$?
check "Recents card titles: the intake still succeeds"            '[ "$RCC" = 0 ]'
check "Recents card titles: the title text is gone from every output file" '! grep -rq "Quarterly Secret Page Title" "$T/outcards"'
check "Recents card titles: the marker and the harmless label list stay" 'grep -q "<card titles redacted>" "$T/outcards/2026-11-06/tablet-report.txt" && grep -q "closed 1 card(s) of Portfolio" "$T/outcards/2026-11-06/tablet-report.txt"'

echo "tablet intake self-test: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ]
