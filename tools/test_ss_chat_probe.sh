#!/usr/bin/env bash
# Offline self-test of tools/ss_chat_probe.py (BN1l, 2026-11-09): synthetic 600 x 1024 screenshots with the chat of Tj's Default shot, of run #458 (bubbles 8 px too tall), and broken ones.
# The probe is the pass / fail of scenario check SS28f, so it must demand real numbers (a blank or half-drawn screen must FAIL, not pass on "nothing found").
set -u
D="$(cd "$(dirname "$0")/.." && pwd)"
P="$D/tools/ss_chat_probe.py"
T="$(mktemp -d)"
fails=0
t(){ local name="$1" want="$2" got="$3"; if [ "$got" = "$want" ]; then echo "ok    $name"; else echo "FAIL  $name: want [$want] got [$got]"; fails=$((fails+1)); fi; }
command -v convert >/dev/null 2>&1 || { echo "skip  ImageMagick convert not installed (the CI runner has it)"; exit 0; }

BLUE='#4285F4'; GREEN='#689F38'
# draw <file> <blue1> <blue2> <green1> <green2> <bubble1 bottom> <bubble2 bottom>: discs 56 px tall, bubbles start at the green disc tops
draw(){ convert -size 600x1024 xc:white \
  -fill "$BLUE" -draw "rectangle 524,$2 580,$(( $2 + 55 ))" -draw "rectangle 524,$3 580,$(( $3 + 55 ))" \
  -fill "$GREEN" -draw "rectangle 20,$4 76,$(( $4 + 55 ))" -draw "rectangle 20,$5 76,$(( $5 + 55 ))" \
  -draw "rectangle 88,$4 511,$6" -draw "rectangle 88,$5 196,$7" "$1"; }

draw "$T/tj.png" 125 313 219 431 293 505
t "Tj's Default shot: the line"            "blue_tops=125,313 green_tops=219,431 bubble_h=75,75" "$(python3 -I "$P" "$T/tj.png")"
python3 -I "$P" --check "$T/tj.png" >/dev/null; t "Tj's Default shot passes --check" "0" "$?"

draw "$T/458.png" 125 329 227 455 309 537
t "run #458 (font padding): the line"      "blue_tops=125,329 green_tops=227,455 bubble_h=83,83" "$(python3 -I "$P" "$T/458.png")"
python3 -I "$P" --check "$T/458.png" >/dev/null; t "run #458 fails --check (bubbles 8 px too tall)" "1" "$?"

draw "$T/near.png" 126 314 220 432 294 506
python3 -I "$P" --check "$T/near.png" >/dev/null; t "one pixel off still passes" "0" "$?"

draw "$T/off3.png" 125 313 219 431 296 505
python3 -I "$P" --check "$T/off3.png" >/dev/null; t "a bubble 3 px too tall fails" "1" "$?"

convert -size 600x1024 xc:white "$T/blank.png"
t "a blank screen prints question marks"   "blue_tops=? green_tops=? bubble_h=?" "$(python3 -I "$P" "$T/blank.png")"
python3 -I "$P" --check "$T/blank.png" >/dev/null; t "a blank screen fails --check" "1" "$?"

# only the first incoming message is drawn: the second bubble is missing
convert -size 600x1024 xc:white -fill "$BLUE" -draw "rectangle 524,125 580,180" -draw "rectangle 524,313 580,368" -fill "$GREEN" -draw "rectangle 20,219 76,274" -draw "rectangle 88,219 511,293" "$T/half.png"
python3 -I "$P" --check "$T/half.png" >/dev/null; t "a missing second bubble fails --check" "1" "$?"

convert -size 300x300 xc:white "$T/small.png"
python3 -I "$P" --check "$T/small.png" >/dev/null; t "a screenshot of the wrong size fails" "1" "$?"
python3 -I "$P" --check "$T/nothere.png" >/dev/null 2>&1; t "a missing file fails" "1" "$?"
python3 -I "$P" >/dev/null; t "no argument is a usage error" "2" "$?"

rm -f "$T"/*.png; rmdir "$T" 2>/dev/null
[ "$fails" -eq 0 ] && echo "ALL OK" || { echo "$fails FAILED"; exit 1; }
