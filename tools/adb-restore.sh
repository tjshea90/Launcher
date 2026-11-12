#!/usr/bin/env bash
# adb-restore.sh — put the tablet back to the stock launcher from a PC, even if the launcher's own screens are unusable.
#   adb devices            # tablet listed? (USB debugging on)
#   bash tools/adb-restore.sh
# What it does, in order (each step independent; a failure does not stop the next):
#  1. disables our app for the user -> Android immediately falls back to the stock Quickstep (this alone fixes a broken Home screen)
#  2. wipes our app's saved state (modes, switches, snapshots)
#  3. restores the settings Tj had before (overridable): keyboard + quick-settings tiles
#  4. re-enables nothing: to use the launcher again, run:  adb shell pm enable com.tj.launcher
# Fully removing it:  adb uninstall com.tj.launcher   (or Settings > Apps). Safe mode (hold power-off, long-press) also disables it.
set -u
PKG=com.tj.launcher
IME_DEFAULT="${IME_DEFAULT:-com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME}"   # Gboard (Tj's usual since 2026-11-12, SwiftKey before); set "" to leave the keyboard alone
TILES_DEFAULT="${TILES_DEFAULT:-rotation}"                                            # Tj's current quick settings: only Auto-rotate; set "" to leave alone
adb get-state >/dev/null 2>&1 || { echo "No device. Plug the tablet in with USB debugging enabled."; exit 1; }
adb shell pm disable-user --user 0 "$PKG" && echo "OK  launcher disabled -> stock launcher is Home again"
adb shell pm clear "$PKG" >/dev/null 2>&1 && echo "OK  launcher state wiped"
if [ -n "$IME_DEFAULT" ] && adb shell ime list -s | grep -q "$IME_DEFAULT"; then
  adb shell ime enable "$IME_DEFAULT" >/dev/null 2>&1; adb shell ime set "$IME_DEFAULT" && echo "OK  keyboard -> $IME_DEFAULT"
fi
[ -n "$TILES_DEFAULT" ] && adb shell settings put secure sysui_qs_tiles "$TILES_DEFAULT" && echo "OK  quick settings tiles -> $TILES_DEFAULT"
adb shell input keyevent KEYCODE_HOME
echo "Done. If anything still looks wrong: adb uninstall $PKG   and/or reboot."
