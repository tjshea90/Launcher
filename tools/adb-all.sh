#!/usr/bin/env bash
# adb-all.sh — EVERYTHING in ONE adb session (Tj, 2026-11-04: "Make it so i can do everything in one session with adb").
#   bash tools/adb-all.sh                      # normal: permissions, helper, probe, pull the tablet's Settings app, screenshots of the stock Settings pages
#   bash tools/adb-all.sh Launcher-v0.5.0.apk  # same, but install (update) the launcher APK first
#   bash tools/adb-all.sh --browser-off        # also disable the old Browser app (com.android.browser) — reversible
#   bash tools/adb-all.sh --status             # only show what is granted / running, change nothing
#   bash tools/adb-all.sh --no-pull            # skip the big file copies and the screenshots
# Plug the tablet in, USB debugging ON, tap Allow. Everything lands in ./probe-out : send me that folder (zip it) or paste probe-out/launcher-probe.txt and probe-out/status.txt.
# Safe: every permission step is reversible (research/ADB_TODO.md has the UNDO block); the probe only READS (names of settings, no accounts / serial / Wi-Fi names).
set -u
PKG=com.tj.launcher
A11Y="$PKG/$PKG.TjAccessibilityService"
D="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT=probe-out; mkdir -p "$OUT/shots" "$OUT/apk"
STATUS="$OUT/status.txt"; REPORT="$OUT/launcher-probe.txt"; : > "$STATUS"; : > "$REPORT"
APK=""; BROWSER=0; STATUS_ONLY=0; PULL=1
for a in "$@"; do case "$a" in --browser-off) BROWSER=1;; --status) STATUS_ONLY=1;; --no-pull) PULL=0;; -*) echo "unknown option $a"; exit 1;; *) APK="$a";; esac; done
say(){ echo "$*" | tee -a "$STATUS"; }
ok(){ say "  OK    $*"; }
bad(){ say "  FAIL  $*"; }
adbsh(){ adb shell "$@" 2>&1 | tr -d '\r'; }

command -v adb >/dev/null 2>&1 || { echo "adb is not installed / not on PATH (install 'Android platform-tools')."; exit 1; }
adb get-state >/dev/null 2>&1 || { echo "No tablet found: USB debugging ON, cable in, tap ALLOW on the tablet, then run again."; exit 1; }
say "== adb-all $(date '+%Y-%m-%d %H:%M')  tablet: $(adbsh getprop ro.product.model) / Android $(adbsh getprop ro.build.version.release) / $(adbsh getprop ro.build.display.id)"

if [ -n "$APK" ]; then
  [ -f "$APK" ] || { echo "APK not found: $APK"; exit 1; }
  say "== 0. install $APK"; if adb install -r "$APK" >/dev/null 2>&1; then ok "installed"; else bad "install failed (a different signature needs an uninstall first)"; fi
fi
adbsh pm path "$PKG" | grep -q package: || { echo "The launcher ($PKG) is not installed. Install it first: bash tools/adb-all.sh Launcher-vX.apk"; exit 1; }

if [ "$STATUS_ONLY" = 0 ]; then
  say "== 1. permissions (each survives updates; undo: research/ADB_TODO.md)"
  for p in WRITE_SECURE_SETTINGS DUMP CHANGE_CONFIGURATION READ_EXTERNAL_STORAGE PACKAGE_USAGE_STATS; do
    r="$(adbsh pm grant "$PKG" android.permission.$p)"; if [ -z "$r" ]; then ok "granted $p"; else bad "$p: $r"; fi
  done
  for op in GET_USAGE_STATS WRITE_SETTINGS RUN_IN_BACKGROUND RUN_ANY_IN_BACKGROUND; do
    r="$(adbsh cmd appops set "$PKG" $op allow)"; if [ -z "$r" ]; then ok "appop $op allow"; else bad "appop $op: $r"; fi
  done
  adbsh dumpsys deviceidle whitelist +"$PKG" | grep -q Added && ok "battery: not frozen while the screen is off" || ok "battery whitelist (already set or no message)"
  say "== 1b. speed: compile the launcher ahead of time (about a minute; sideloaded apps are otherwise only half-optimised, which slows the first start after an unlock)"
  r="$(adbsh cmd package compile -m speed -f "$PKG")"; echo "$r" | grep -qi success && ok "compiled ($r)" || bad "compile: $r"
  say "== 2. accessibility helper"
  cur="$(adbsh settings get secure enabled_accessibility_services)"
  if echo "$cur" | grep -q "$A11Y"; then ok "already on"; else
    if [ -z "$cur" ] || [ "$cur" = null ]; then new="$A11Y"; else new="$cur:$A11Y"; fi
    adbsh settings put secure enabled_accessibility_services "$new" >/dev/null; adbsh settings put secure accessibility_enabled 1 >/dev/null
    echo "$(adbsh settings get secure enabled_accessibility_services)" | grep -q "$A11Y" && ok "turned on (was: $cur)" || bad "could not turn it on: do it in Settings > Accessibility"
  fi
  say "== 3. the invisible clean-up helper (lasts until the next restart: run this script again then)"
  apkpath="$(adbsh pm path "$PKG" | head -1 | cut -d: -f2)"
  adb shell "CLASSPATH=$apkpath setsid nohup app_process /system/bin com.tj.launcher.ShellServer >/dev/null 2>&1 &" >/dev/null 2>&1
  sleep 2
  n="$(adbsh "ps -A -o ARGS | grep -c '[S]hellServer'")"; [ "${n:-0}" -ge 1 ] 2>/dev/null && ok "helper running" || bad "helper did not start"
  if [ "$BROWSER" = 1 ]; then
    say "== 4. Browser app off"; r="$(adbsh pm disable-user --user 0 com.android.browser)"; echo "$r" | grep -qi disabled && ok "$r  (undo: adb shell pm enable com.android.browser)" || bad "$r"
  else say "== 4. Browser app: skipped (add --browser-off to disable it; undo: adb shell pm enable com.android.browser)"; fi
fi

say "== 5. status of every permission command (what the tablet reports)"
adbsh dumpsys package "$PKG" | grep -E 'granted=(true|false)' | grep -E 'WRITE_SECURE_SETTINGS|DUMP|CHANGE_CONFIGURATION|READ_EXTERNAL_STORAGE|PACKAGE_USAGE_STATS|WRITE_SETTINGS' | sed 's/^ */  /' | tee -a "$STATUS"
adbsh cmd appops get "$PKG" | grep -E 'GET_USAGE_STATS|WRITE_SETTINGS|RUN_IN_BACKGROUND|RUN_ANY_IN_BACKGROUND' | sed 's/^/  appop /' | tee -a "$STATUS"
say "  accessibility: $(adbsh settings get secure enabled_accessibility_services)"
say "  helper processes: $(adbsh "ps -A -o ARGS | grep -c '[S]hellServer'")"
say "  max users: $(adbsh pm get-max-users)   users: $(adbsh pm list users | tr '\n' ' ')"
say "  crypto: $(adbsh getprop ro.crypto.type) / $(adbsh getprop ro.crypto.state)   adb tcp: '$(adbsh getprop service.adb.tcp.port)' / persist '$(adbsh getprop persist.adb.tcp.port)'"
say "  power-menu setting: '$(adbsh settings get secure power_menu_actions)'   lineage: '$(adbsh getprop ro.lineage.version)'"

say "== 6. read-only probe -> $REPORT"
while IFS= read -r line; do
  case "$line" in "## "*) { echo; echo "===== ${line#\#\# }"; } >> "$REPORT";; "") ;; *) { echo "\$ adb shell $line"; adb shell "$line" < /dev/null 2>&1 | tr -d '\r'; } >> "$REPORT";; esac
done < "$D/../app/src/main/resources/probe-commands.txt"
ok "$(wc -l < "$REPORT") lines"

if [ "$STATUS_ONLY" = 0 ] && [ "$PULL" = 1 ]; then
  say "== 7. copy the tablet's own system apps (for the Settings clone + the stock look)"
  for pkg in com.android.settings com.android.systemui com.android.launcher3 android; do
    for apk in $(adbsh pm path $pkg | sed 's/^package://'); do
      if adb pull "$apk" "$OUT/apk/${pkg}_$(basename "$apk")" >/dev/null 2>&1; then ok "pulled $pkg: $(basename "$apk")"; else bad "could not pull $apk"; fi
    done
  done
  say "== 8. screenshots + screen structure of the stock Settings pages (touch nothing on the tablet for ~1 minute)"
  for page in SETTINGS WIFI_SETTINGS BLUETOOTH_SETTINGS DISPLAY_SETTINGS SOUND_SETTINGS APPLICATION_SETTINGS INTERNAL_STORAGE_SETTINGS BATTERY_SAVER_SETTINGS DATE_SETTINGS LOCALE_SETTINGS ACCESSIBILITY_SETTINGS SECURITY_SETTINGS PRIVACY_SETTINGS DEVICE_INFO_SETTINGS INPUT_METHOD_SETTINGS; do
    adb shell am start -a android.settings.$page >/dev/null 2>&1; sleep 2.5
    adb exec-out screencap -p > "$OUT/shots/$page.png" 2>/dev/null
    adb shell uiautomator dump /sdcard/tjui.xml >/dev/null 2>&1 && adb pull /sdcard/tjui.xml "$OUT/shots/$page.xml" >/dev/null 2>&1
  done
  adb shell input keyevent KEYCODE_HOME >/dev/null 2>&1
  ok "$(ls "$OUT/shots"/*.png 2>/dev/null | wc -l) screenshots in $OUT/shots"
fi
say ""
say "DONE. Send me the folder $OUT (zip it), or at least paste $STATUS and $REPORT. A FAIL line above tells you what did not work."
