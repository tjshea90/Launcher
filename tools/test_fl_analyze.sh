#!/usr/bin/env bash
# test_fl_analyze.sh - pins the two analyzer bugs found in run #563 (BY1b, 2026-11-12) with a synthetic screenrecord-like film:
#   (1) screenrecord writes a frame only when the screen CHANGES, so a film ends at its last change: "--ref last" must compare with
#       the SETTLED end state (fl_analyze pads the end with the last frame), not with the middle of the last animation;
#   (2) the boxes have odd sizes (core 99 x 83): cropping the yuv420p video rounded them to the chroma grid and the raw stream was cut
#       at the wrong places (fl_analyze converts to grey BEFORE cropping).
# The film: grey 100 everywhere; the two RING boxes go dark (32) from 1.0 to 2.0 s, then 64 / 80 / 90 for 0.1 s each, 100 at 2.3 s,
# where the film ENDS (mpdecimate drops repeated frames, like screenrecord). The CORE never changes = the patch "over an open panel".
# Expected: flashA = 78 frames (1.00-2.30 s: ring 68, 36, 20 levels away; the 90 step is too small for mpdecimate to keep), no flashB,
# 139 film frames. Needs ffmpeg + libx264; prints SKIP without them. Writes only into a fresh temporary directory.
set -uo pipefail
cd "$(dirname "$0")/.." || exit 1
command -v ffmpeg >/dev/null 2>&1 || { echo "test_fl_analyze: SKIP (no ffmpeg)"; exit 0; }
python3 -I tools/fl_analyze.py --selftest || exit 1
T="$(mktemp -d)"
vf=""
for spec in "0x202020:1.0:2.0" "0x404040:2.0:2.1" "0x505050:2.1:2.2" "0x5a5a5a:2.2:2.3"; do
  c=${spec%%:*}; r=${spec#*:}; a=${r%%:*}; b=${r#*:}
  for box in 435:547:30:95 485:687:111:15; do
    IFS=: read -r x y w h <<< "$box"
    vf="$vf,drawbox=x=$x:y=$y:w=$w:h=$h:color=$c:t=fill:enable='between(t,$a,$b-0.001)'"
  done
done
if ! ffmpeg -v error -y -f lavfi -i "color=c=0x646464:s=600x1024:r=60:d=2.4" -vf "format=yuv420p${vf},mpdecimate" -fps_mode vfr -c:v libx264 -crf 18 "$T/v.mp4" 2>"$T/err"; then
  echo "test_fl_analyze: SKIP (ffmpeg could not encode: $(tail -1 "$T/err"))"; exit 0
fi
out="$(python3 -I tools/fl_analyze.py "$T/v.mp4" syn --core 491,553,590,636 --ring "435,547,465,642;485,687,596,702" --mode flash --ref last \
  --sheet "$T/sheet.png" --sheetbox 425,329,600,712 | grep '^FLMETRIC')"
echo "$out"
fail=0
for want in "frames=139" "flashA=78" "flashB=0" "spans=A1.00-2.30"; do
  echo "$out" | grep -q " $want\b" || { echo "test_fl_analyze: FAIL (expected $want)"; fail=1; }
done
[ -s "$T/sheet.png" ] || { echo "test_fl_analyze: FAIL (no contact sheet)"; fail=1; }
[ "$fail" = 0 ] && echo "test_fl_analyze: OK"
exit "$fail"
