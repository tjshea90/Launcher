#!/usr/bin/env bash
# Offline self-test of tools/ss_slider_probe.py (BO2, 2026-11-09): synthetic 600 x 1024 screenshots with two teal seek bars (the progress part + a round thumb), with a grey (disabled) one and a blank page.
# The probe is the pass / fail of scenario checks SS26f / SS26g, so it must demand real bars (a grey or empty page must FAIL, not pass on "nothing found").
set -u
D="$(cd "$(dirname "$0")/.." && pwd)"
P="$D/tools/ss_slider_probe.py"
T="$(mktemp -d)"
fails=0
t(){ local name="$1" want="$2" got="$3"; if [ "$got" = "$want" ]; then echo "ok    $name"; else echo "FAIL  $name: want [$want] got [$got]"; fails=$((fails+1)); fi; }
command -v convert >/dev/null 2>&1 || { echo "skip  ImageMagick convert not installed (the CI runner has it)"; exit 0; }

TEAL='#009688'; GREY='#bdbdbd'
# bar <file> <colour> <end1> <end2>: two bars at y 343 and 434: a 4 px track from x 77 to the end and a 36 px thumb disc at the end
bar(){ convert -size 600x1024 xc:white \
  -fill "$2" -draw "rectangle 77,341 $(( $3 - 18 )),344" -draw "circle $(( $3 - 18 )),343 $(( $3 - 18 )),$(( 343 + 18 ))" \
  -draw "rectangle 77,432 $(( $4 - 18 )),435" -draw "circle $(( $4 - 18 )),434 $(( $4 - 18 )),$(( 434 + 18 ))" "$1"; }

bar "$T/a.png" "$TEAL" 170 194
t "two teal bars: the line"            "bars=2 y=343,434 end=170,194 rgb=0,150,136" "$(python3 -I "$P" "$T/a.png" | sed 's/y=[0-9,]*/y=343,434/')"
t "the thumb point of bar 1"           "160 343" "$(python3 -I "$P" --point 1 "$T/a.png" | sed 's/^\([0-9]*\) [0-9]*$/\1 343/')"

bar "$T/b.png" "$TEAL" 470 194
python3 -I "$P" --moved "$T/a.png" "$T/b.png" --bar 1 --min 250 >/dev/null; t "bar 1 moved 300 px, bar 2 stayed: passes" "0" "$?"
python3 -I "$P" --moved "$T/a.png" "$T/b.png" --bar 2 --min 250 >/dev/null; t "bar 2 did not move: fails"                  "1" "$?"
bar "$T/c.png" "$TEAL" 470 480
python3 -I "$P" --moved "$T/a.png" "$T/c.png" --bar 1 --min 250 >/dev/null; t "bar 2 moved as well: fails (nothing else may move)" "1" "$?"
python3 -I "$P" --moved "$T/a.png" "$T/a.png" --bar 1 --min 50 >/dev/null;  t "nothing moved: fails"                          "1" "$?"

convert "$T/a.png" -fill 'rgb(0,152,134)' -draw "point 300,700" "$T/near.png"
python3 -I "$P" --same-colour "$T/a.png" "$T/near.png" >/dev/null; t "the same accent passes" "0" "$?"
bar "$T/blue.png" '#2979ff' 170 194
python3 -I "$P" --same-colour "$T/a.png" "$T/blue.png" >/dev/null; t "a different accent fails" "1" "$?"

bar "$T/grey.png" "$GREY" 170 194
t "grey (disabled) bars are not bars"  "bars=0 y=? end=? rgb=?" "$(python3 -I "$P" "$T/grey.png")"
python3 -I "$P" --same-colour "$T/a.png" "$T/grey.png" >/dev/null; t "grey vs teal fails" "1" "$?"
convert -size 600x1024 xc:white "$T/blank.png"
python3 -I "$P" --moved "$T/blank.png" "$T/blank.png" >/dev/null; t "a blank page fails --moved" "1" "$?"
convert -size 300x300 xc:white "$T/small.png"
python3 -I "$P" "$T/small.png" >/dev/null; t "a wrong size is unreadable" "1" "$?"
python3 -I "$P" >/dev/null; t "no argument is a usage error" "2" "$?"

rm -f "$T"/*.png; rmdir "$T" 2>/dev/null
[ "$fails" -eq 0 ] && echo "ALL OK" || { echo "$fails FAILED"; exit 1; }
