#!/usr/bin/env bash
# adb-grant.sh — OPTIONAL extra permissions for features that need adb. Nothing in the launcher needs this to run; each gated feature just stays
# unavailable (and says so) until its grant is given. Revoke with:  adb shell pm revoke com.tj.launcher android.permission.WRITE_SECURE_SETTINGS
#   bash tools/adb-grant.sh keyboard     # lets the launcher switch the default keyboard (stock <-> Gboard) by itself   [needs release with that feature]
#   bash tools/adb-grant.sh tiles        # lets it keep the quick settings at Auto-rotate only                              [needs release with that feature]
#   bash tools/adb-grant.sh notifications  # notification access (clear all notifications for the kill switch) — can also be done in Settings
set -u
PKG=com.tj.launcher
adb get-state >/dev/null 2>&1 || { echo "No device (USB debugging on?)"; exit 1; }
case "${1:-}" in
  keyboard|tiles) adb shell pm grant "$PKG" android.permission.WRITE_SECURE_SETTINGS && echo "OK  WRITE_SECURE_SETTINGS granted (revoke: pm revoke ...)";;
  notifications)  adb shell cmd notification allow_listener "$PKG/com.android.launcher3.notification.NotificationListener" && echo "OK  notification access granted";;
  *) echo "usage: $0 keyboard|tiles|notifications"; exit 2;;
esac
