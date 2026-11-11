#!/usr/bin/env bash
# test_music_icon_check.sh — self-test of tools/music_icon_check.py, the check behind emulator check MU-21 (TASKS BW1d). ~4 s, no network, writes only to a temp dir.
# Pins: the speaker the generator draws passes on plates of 52 / 55 / 56 px; the SIZE criterion fails the first, legacy icon (Launcher3 shrank it to 0.87, CI run #554) and a speaker that is too big;
# a plate with nothing on it, a pink disc of the right size (the colours) and a missing entry are not the speaker either.
# Needs python3 with numpy + PIL (tools/make_music_icon.py) and ImageMagick; prints SKIP and exits 0 where one is missing (the runner of android.yml has no numpy).
#   bash tools/test_music_icon_check.sh        # exit 0 = all green
set -uo pipefail
D="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
python3 -I -c 'import numpy, PIL' >/dev/null 2>&1 || { echo "SKIP: python3 with numpy and PIL is missing"; exit 0; }
command -v identify >/dev/null 2>&1 && command -v convert >/dev/null 2>&1 || { echo "SKIP: ImageMagick is missing"; exit 0; }
T="$(mktemp -d)"; trap 'rm -rf "$T"' EXIT
PASS=0; FAIL=0
printf '<node text="Music" bounds="[129,213][243,333]"/>\n' > "$T/dump.txt"
chk(){ # chk <name> <want exit code> <picture> [text the output must contain] [label]
  local name="$1" want="$2" pic="$3" has="${4:-}" label="${5:-Music}" out rc
  out="$(python3 -I "$D/tools/music_icon_check.py" "$pic" "$T/dump.txt" "$label" 2>&1)"; rc=$?
  if [ "$rc" = "$want" ] && { [ -z "$has" ] || printf '%s' "$out" | grep -q "$has"; }; then PASS=$((PASS+1)); else FAIL=$((FAIL+1)); echo "  FAIL  $name: want exit $want${has:+ and '$has'}, got exit $rc: $out"; fi
}
sim(){ python3 -I "$D/tools/make_music_icon.py" --drawer-sim "$@" >/dev/null; }

sim "$T/ok52.png" 52;            chk "the generated speaker on the emulator's 52 px plate is the speaker" 0 "$T/ok52.png" "-> SPEAKER"
sim "$T/ok55.png" 55;            chk "... on Tj's 55 px plate" 0 "$T/ok55.png" "-> SPEAKER"
sim "$T/ok56.png" 56;            chk "... on a 56 px plate" 0 "$T/ok56.png" "-> SPEAKER"
sim "$T/small.png" 52 0.87;      chk "NEGATIVE: a speaker at 0.87 of that size (what Launcher3 made of the legacy icon) fails on the size" 1 "$T/small.png" "of the plate, Tj's is"
sim "$T/big.png" 52 1.15;        chk "NEGATIVE: a speaker 15 % too big fails on the size" 1 "$T/big.png" "of the plate, Tj's is"
sim "$T/none.png" 52 0.02;       chk "NEGATIVE: a plate with nothing on it is not the speaker" 1 "$T/none.png" "nothing but white"
chk "NEGATIVE: an entry that is not in the dump cannot be measured (exit 2)" 2 "$T/ok52.png" "no entry" "Nope"

# a pink disc of exactly the right size (upstream's pink note is an adaptive icon without any white plate, a different failure): the colours must fail it
python3 -I - "$T/pink.png" <<'EOF'
import sys
from PIL import Image, ImageDraw
img = Image.new("RGB", (600, 1024), (169, 160, 163))
d = ImageDraw.Draw(img)
d.rounded_rectangle((160, 230, 211, 281), radius=4, fill=(255, 255, 255))
cx, cy, r = 186, 256, 18.6
d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=(233, 30, 99))
img.save(sys.argv[1])
EOF
chk "NEGATIVE: a pink disc of the right size fails on the colours" 1 "$T/pink.png" "NOT THE SPEAKER"

if [ "$FAIL" = 0 ]; then echo "music icon check self-test: $PASS checks passed"; exit 0; fi
echo "music icon check self-test: $FAIL FAILED, $PASS passed"; exit 1
