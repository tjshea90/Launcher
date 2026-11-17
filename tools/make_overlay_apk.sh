#!/usr/bin/env bash
# make_overlay_apk.sh — build a tiny NON-static runtime resource overlay (RRO) APK. Used by the emulator research
# section `ro` (TASKS CG1e: can the quick-settings pencil be changed without root?). Nothing here runs on the tablet.
#
#   bash tools/make_overlay_apk.sh <out.apk> <overlay-package> <target-package> <res-dir> [priority]
#
# <res-dir> holds ordinary resource folders (drawable/, values/, layout/ ...) whose resource NAMES match the target's.
# Needs the SDK build-tools (aapt2, zipalign, apksigner) + a platform jar + keytool. The APK is signed with a
# throw-away key made next to the scratch files and never kept: an overlay signed like this is exactly what a
# user-installed (`pm install` as the shell user) overlay looks like to Android.
set -euo pipefail
OUT="${1:?out.apk}"; PKGN="${2:?overlay package}"; TARGET="${3:?target package}"; RES="${4:?res dir}"; PRIO="${5:-1}"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/opt/android-sdk}}"
BT="$(ls -d "$SDK"/build-tools/*/ | sort -V | tail -1)"
JAR="$(ls -d "$SDK"/platforms/android-*/android.jar | sort -V | tail -1)"
W="$(mktemp -d)"; trap 'rm -rf "$W"' EXIT
cat > "$W/AndroidManifest.xml" <<EOF
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="$PKGN">
    <uses-sdk android:minSdkVersion="29" android:targetSdkVersion="29" />
    <overlay android:targetPackage="$TARGET" android:priority="$PRIO" android:isStatic="false" />
    <application android:hasCode="false" android:label="overlay test" />
</manifest>
EOF
"${BT}aapt2" compile --dir "$RES" -o "$W/compiled.zip"
"${BT}aapt2" link -o "$W/unsigned.apk" -I "$JAR" --manifest "$W/AndroidManifest.xml" \
  --min-sdk-version 29 --target-sdk-version 29 "$W/compiled.zip"
"${BT}zipalign" -f 4 "$W/unsigned.apk" "$W/aligned.apk"
keytool -genkeypair -keystore "$W/k.jks" -storepass pass1234 -keypass pass1234 -alias k -keyalg RSA -keysize 2048 \
  -validity 3650 -dname "CN=overlay throwaway" >/dev/null 2>&1
mkdir -p "$(dirname "$OUT")"; rm -f "$OUT"
"${BT}apksigner" sign --ks "$W/k.jks" --ks-pass pass:pass1234 --key-pass pass:pass1234 --out "$OUT" "$W/aligned.apk"
"${BT}apksigner" verify "$OUT" >/dev/null
echo "built $OUT ($(stat -c %s "$OUT") bytes): $PKGN -> $TARGET, priority $PRIO, non-static"
