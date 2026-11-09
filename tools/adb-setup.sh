#!/usr/bin/env bash
# adb-setup.sh — runs the SAFE adb lines from research/ADB_COMMANDS.txt (sections 2, 3, 4, 6) in one go. Every line is reversible (section 9 of that file).
#   bash tools/adb-setup.sh          # grant everything the launcher can use
#   bash tools/adb-setup.sh --check  # only show what is granted
# Nothing here installs software, touches the lock screen or makes the launcher a device admin.
set -u
PKG=com.tj.launcher
adb get-state >/dev/null 2>&1 || { echo "No device found: is USB debugging on and the tablet plugged in? (research/ADB_COMMANDS.txt section 0)"; exit 1; }
# the invisible clean-up helper (section 11): lasts until the tablet restarts, so it has its own switch:  bash tools/adb-setup.sh --helper
start_helper(){
  APK="$(adb shell pm path "$PKG" | head -1 | cut -d: -f2 | tr -d '\r')"
  [ -n "$APK" ] || { echo "launcher not installed"; exit 1; }
  adb shell "CLASSPATH=$APK setsid nohup app_process /system/bin com.tj.launcher.ShellServer >/dev/null 2>&1 &"
  sleep 1; adb shell "ps -A | grep -c ShellServer" | tr -d '\r' | sed 's/^/helper processes running: /'
}
if [ "${1:-}" = "--helper" ]; then start_helper; exit 0; fi
if [ "${1:-}" = "--check" ]; then
  adb shell dumpsys package "$PKG" | grep -E "WRITE_SECURE_SETTINGS|READ_EXTERNAL_STORAGE|android.permission.DUMP|CHANGE_CONFIGURATION" | sort -u
  exit 0
fi
run(){ echo "+ $*"; "$@" >/dev/null 2>&1 && echo "  ok" || echo "  skipped (older release without this permission, or already set)"; }
run adb shell pm grant "$PKG" android.permission.WRITE_SECURE_SETTINGS
run adb shell pm grant "$PKG" android.permission.READ_EXTERNAL_STORAGE
run adb shell dumpsys deviceidle whitelist +"$PKG"
run adb shell cmd appops set "$PKG" RUN_IN_BACKGROUND allow
run adb shell cmd appops set "$PKG" RUN_ANY_IN_BACKGROUND allow
run adb shell pm grant "$PKG" android.permission.DUMP
run adb shell pm grant "$PKG" android.permission.CHANGE_CONFIGURATION
run adb shell pm grant "$PKG" android.permission.PACKAGE_USAGE_STATS
run adb shell cmd appops set "$PKG" GET_USAGE_STATS allow
run adb shell cmd appops set "$PKG" WRITE_SETTINGS allow
echo; echo "The invisible clean-up helper is separate (it must be started again after every restart):  bash tools/adb-setup.sh --helper"
echo "Done. Check with: bash tools/adb-setup.sh --check"
