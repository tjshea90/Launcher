#!/usr/bin/env bash
# Offline test of the b6 pencil helpers (pen_sample, pen_none, pen_all_shown, pen_control_ok) with a stubbed adb: bash tools/test_pen_sample.sh [scenario file] (2026-11-08, BK1b gate triage).
set -u
SCEN="${1:-tools/emulator_scenario.sh}"
OUT="$(mktemp -d)"
ex(){ # print the function text starting at "<name>(){" up to its closing brace (or the one-liner itself)
  local s; s=$(grep -n "^$1(){" "$SCEN" | head -1 | cut -d: -f1)
  if sed -n "${s}p" "$SCEN" | grep -q '}[[:space:]]*\(#.*\)\?$' && ! sed -n "${s}p" "$SCEN" | grep -q '{[[:space:]]*#'; then sed -n "${s}p" "$SCEN"; return; fi
  local e; e=$(awk -v s="$s" 'NR>s && /^}/ {print NR; exit}' "$SCEN"); sed -n "${s},${e}p" "$SCEN"
}
for f in kg_state pen_cover_of pen_sample pen_none pen_all_shown pen_control_ok bn8_shown_after_hide bq3_close_verdict bq3_closes_ok pen_tl pen_blocks; do ex "$f"; done > "$OUT/fns.sh"
FAKE_WIN=""; FAKE_POL=""; FAKE_PID=""; PKG=com.tj.launcher
adb(){ # adb shell dumpsys window windows|policy, pidof
  case "$*" in *"dumpsys window windows"*) printf '%s' "$FAKE_WIN";; *"dumpsys window policy"*) printf '%s' "$FAKE_POL";; *pidof*) [ -p /dev/stdin ] && cat > /dev/null; printf '%s' "$FAKE_PID";; esac   # a real `adb shell` SWALLOWS a piped stdin (run #513): the stub does too, so a pidof inside a pipeline is caught here
}
. "$OUT/fns.sh"
fails=0
t(){ local name="$1" want="$2" got="$3"; if [ "$got" = "$want" ]; then echo "ok    $name -> $got"; else echo "FAIL  $name: want [$want] got [$got]"; fails=$((fails+1)); fi; }

SB_CLOSED=$'  Window #5 Window{1a u0 StatusBar}:\n    mAttrs={(0,0)(fillx24) ty=STATUS_BAR}\n    Frames: containing=[0,0][600,1024] parent=[0,0][600,1024]\n    mFrame=[0,0][600,24] last=[0,0][600,24]\n'
SB_OPEN=$'  Window #5 Window{1a u0 StatusBar}:\n    mAttrs={(0,0)(fillxfill) ty=STATUS_BAR}\n    Frames: containing=[0,0][600,1024] parent=[0,0][600,1024]\n    mFrame=[0,0][600,1024] last=[0,0][600,1024]\n'
SB_LOWER=$'  Window #5 Window{1a u0 StatusBar}:\n    frame=[0,0][600,24]\n'
COV_SHOWN=$'  Window #9 Window{2b u0 TjPencilCover}:\n    mDisplayId=0\n    mAttrs={(4,564)(51x51) gr=TOP START ty=ACCESSIBILITY_OVERLAY fmt=TRANSLUCENT\n      fl=NOT_FOCUSABLE LAYOUT_IN_SCREEN LAYOUT_NO_LIMITS}\n    mFrame=[4,564][55,615]\n'
COV_HIDDEN=$'  Window #9 Window{2b u0 TjPencilCover}:\n    mDisplayId=0\n    mAttrs={(4,564)(51x51) gr=TOP START ty=ACCESSIBILITY_OVERLAY fmt=TRANSLUCENT\n      fl=NOT_FOCUSABLE NOT_TOUCHABLE LAYOUT_IN_SCREEN LAYOUT_NO_LIMITS}\n    mFrame=[4,564][55,615]\n'
POL_F=$'  mKeyguardDelegate: showing=false mOccluded=false\n'
POL_T=$'  mKeyguardDelegate: showing=true mOccluded=false\n'

FAKE_WIN="$SB_CLOSED"; FAKE_POL="$POL_F";            t "no cover, closed shade, unlocked"     "cover=none showing=false sb=[0,0][600,24]"   "$(pen_sample)"
FAKE_WIN="$SB_OPEN$COV_SHOWN"; FAKE_POL="$POL_T";    t "shown cover, open panel, locked"      "cover=shown showing=true sb=[0,0][600,1024]" "$(pen_sample)"
FAKE_WIN="$COV_HIDDEN$SB_CLOSED"; FAKE_POL="$POL_F"; t "hidden (NOT_TOUCHABLE) cover"         "cover=hidden showing=false sb=[0,0][600,24]" "$(pen_sample)"
FAKE_WIN="$SB_LOWER"; FAKE_POL="$POL_F";             t "frame printed in lower case"          "cover=none showing=false sb=[0,0][600,24]"   "$(pen_sample)"
FAKE_WIN=""; FAKE_POL="";                            t "empty dumps do not crash"             "cover=none  sb=?"                            "$(pen_sample)"
# run #512 (2026-11-10): the window of a killed process LINGERS in the dump (the service had restarted); only windows of a LIVE app process count
COV_LIVE=$'  Window #9 Window{2b u0 TjPencilCover}:\n    mDisplayId=0 stackId=1 mSession=Session{3f2a1b 4321:u0a105} mClient=android.os.BinderProxy@1\n    mAttrs={(4,564)(51x51) gr=TOP START ty=ACCESSIBILITY_OVERLAY fmt=TRANSLUCENT\n      fl=NOT_FOCUSABLE LAYOUT_IN_SCREEN LAYOUT_NO_LIMITS}\n    mFrame=[4,564][55,615]\n'
COV_LIVE_HIDDEN=$'  Window #9 Window{2b u0 TjPencilCover}:\n    mDisplayId=0 stackId=1 mSession=Session{3f2a1b 4321:u0a105} mClient=android.os.BinderProxy@1\n    mAttrs={(4,564)(51x51) gr=TOP START ty=ACCESSIBILITY_OVERLAY fmt=TRANSLUCENT\n      fl=NOT_FOCUSABLE NOT_TOUCHABLE LAYOUT_IN_SCREEN LAYOUT_NO_LIMITS}\n    mFrame=[4,564][55,615]\n'
COV_DEAD=$'  Window #8 Window{1f u0 TjPencilCover}:\n    mDisplayId=0 stackId=1 mSession=Session{9d8c7b 1111:u0a105} mClient=android.os.BinderProxy@2\n    mAttrs={(4,564)(51x51) gr=TOP START ty=ACCESSIBILITY_OVERLAY fmt=TRANSLUCENT\n      fl=NOT_FOCUSABLE LAYOUT_IN_SCREEN LAYOUT_NO_LIMITS}\n    mFrame=[4,564][55,615]\n'
FAKE_PID="4321"; FAKE_POL="$POL_T"
FAKE_WIN="$SB_OPEN$COV_LIVE";                        t "live process: touchable cover is shown"   "cover=shown showing=true sb=[0,0][600,1024]"  "$(pen_sample)"
FAKE_WIN="$SB_OPEN$COV_DEAD";                        t "dead process: lingering entry is no cover" "cover=none showing=true sb=[0,0][600,1024]"   "$(pen_sample)"
FAKE_WIN="$COV_DEAD$COV_LIVE_HIDDEN$SB_OPEN";        t "stale touchable entry + live hidden = hidden" "cover=hidden showing=true sb=[0,0][600,1024]" "$(pen_sample)"
FAKE_WIN="$COV_LIVE_HIDDEN$COV_DEAD$SB_OPEN";        t "live hidden + stale touchable (other order)" "cover=hidden showing=true sb=[0,0][600,1024]" "$(pen_sample)"
FAKE_PID="1111 4321"; FAKE_WIN="$SB_OPEN$COV_DEAD";  t "two pids: the older one is live too"      "cover=shown showing=true sb=[0,0][600,1024]"  "$(pen_sample)"
FAKE_PID="";          FAKE_WIN="$SB_OPEN$COV_DEAD";  t "no pid known: no filter (never a false none)" "cover=shown showing=true sb=[0,0][600,1024]" "$(pen_sample)"
FAKE_PID=""

pen_none "cover=none showing=false sb=x" "cover=hidden showing=true sb=y" && echo "ok    pen_none: none+hidden pass" || { echo "FAIL  pen_none none+hidden"; fails=$((fails+1)); }
pen_none "cover=none showing=false sb=x" "cover=shown showing=true sb=y" && { echo "FAIL  pen_none must flag shown"; fails=$((fails+1)); } || echo "ok    pen_none flags a shown cover"
pen_all_shown "cover=shown a" "cover=shown b" "cover=shown c" && echo "ok    pen_all_shown: 3 shown pass" || { echo "FAIL  pen_all_shown 3 shown"; fails=$((fails+1)); }
pen_all_shown "cover=shown a" "cover=none b" "cover=none c" && { echo "FAIL  pen_all_shown must flag none"; fails=$((fails+1)); } || echo "ok    pen_all_shown flags 'shown none none'"
pen_control_ok "cover=none showing=false sb=x" && echo "ok    control: unlocked + no cover passes" || { echo "FAIL  control ok"; fails=$((fails+1)); }
pen_control_ok "cover=shown showing=false sb=x" && { echo "FAIL  control must flag a shown cover"; fails=$((fails+1)); } || echo "ok    control flags a cover on the unlocked Quickstep panel"
pen_control_ok "cover=none showing=true sb=x" && { echo "FAIL  control must flag a still-locked device"; fails=$((fails+1)); } || echo "ok    control flags a device that is still locked"
pen_control_ok "cover=none showing=false sb=x" 0 && echo "ok    control: zero 'cover shown' log lines passes" || { echo "FAIL  control zero lines"; fails=$((fails+1)); }
pen_control_ok "cover=none showing=false sb=x" 2 && { echo "FAIL  control must flag cover-shown log lines"; fails=$((fails+1)); } || echo "ok    control flags 'PENCIL cover shown' log lines even when the window sample reads none"
# BN8b: the drag check counts 'cover shown' lines within a window after the FIRST hide (logcat -v time lines)
L1="$OUT/drag1.txt"; printf '%s\n' '11-10 01:00:00.000 I/TjA11y( 1): BN8b marker drag' '11-10 01:00:00.900 I/TjA11y( 1): PENCIL fast watch (a finger went down)' '11-10 01:00:01.010 I/TjA11y( 1): PENCIL cover hidden (kept warm; the panel is sliding up)' '11-10 01:00:01.800 I/TjA11y( 1): BN8b marker end' > "$L1"
t "drag: hidden once, never shown again"            "0"      "$(bn8_shown_after_hide "$L1" 480)"
L2="$OUT/drag2.txt"; printf '%s\n' '11-10 01:00:01.010 I/TjA11y( 1): PENCIL cover hidden (kept warm; the followed pencil is gone)' '11-10 01:00:01.050 I/TjA11y( 1): PENCIL cover shown [4,500][55,551]' '11-10 01:00:01.100 I/TjA11y( 1): PENCIL cover hidden (kept warm; the followed pencil is gone)' '11-10 01:00:01.140 I/TjA11y( 1): PENCIL cover shown [4,440][55,491]' > "$L2"
t "drag: the old flicker (shown twice in the slide)" "2"      "$(bn8_shown_after_hide "$L2" 480)"
L3="$OUT/drag3.txt"; printf '%s\n' '11-10 01:00:01.010 I/TjA11y( 1): PENCIL cover hidden (kept warm; the panel is sliding up)' '11-10 01:00:01.900 I/TjA11y( 1): PENCIL cover shown [4,300][55,351]' > "$L3"
t "drag: a cover that returns after the window is fine" "0"   "$(bn8_shown_after_hide "$L3" 480)"
L4="$OUT/drag4.txt"; printf '%s\n' '11-10 01:00:00.500 I/TjA11y( 1): PENCIL cover shown [4,564][55,615]' '11-10 01:00:01.000 I/TjA11y( 1): BN8b marker end' > "$L4"
t "drag: a cover that was never hidden is reported"  "nohide" "$(bn8_shown_after_hide "$L4" 480)"
L5="$OUT/drag5.txt"; printf '%s\n' '11-10 23:59:59.900 I/TjA11y( 1): PENCIL cover hidden (kept warm; the panel is sliding up)' '11-10 23:59:59.990 I/TjA11y( 1): PENCIL cover shown [4,300][55,351]' > "$L5"
t "drag: shown 90 ms after the hide counts"          "1"      "$(bn8_shown_after_hide "$L5" 480)"
# BQ3 (2026-11-10): the closings are judged by what the SERVICE logged after the BACK key (logcat -v time lines), not by a window sample
LB="$OUT/bq3a.txt"; printf '%s\n' '11-10 11:13:46.965 I/TjA11y( 1): BK1b marker expand' '11-10 11:13:47.025 I/TjA11y( 1): PENCIL cover shown [4,564][55,615]' \
  '11-10 11:13:50.350 I/TjA11y( 1): BQ3 marker close 1' '11-10 11:13:50.439 I/TjA11y( 1): PENCIL fast watch (SystemUI is redrawing)' '11-10 11:13:50.442 I/TjA11y( 1): PENCIL cover hidden (kept warm; the followed pencil is gone)' \
  '11-10 11:13:52.700 I/TjA11y( 1): BQ3 marker open 2' '11-10 11:13:52.757 I/TjA11y( 1): PENCIL cover shown [4,564][55,615]' \
  '11-10 11:13:55.850 I/TjA11y( 1): BQ3 marker close 2' '11-10 11:13:55.897 I/TjA11y( 1): PENCIL cover hidden (kept warm; the followed pencil is gone)' \
  '11-10 11:13:58.200 I/TjA11y( 1): BQ3 marker open 3' '11-10 11:13:58.260 I/TjA11y( 1): PENCIL cover shown [4,564][55,615]' > "$LB"
t "close 1: hidden 92 ms after the key"            "hidden-in-92"  "$(bq3_close_verdict "$LB" 1)"
t "close 2: hidden 47 ms after the key"            "hidden-in-47"  "$(bq3_close_verdict "$LB" 2)"
t "close 3: no marker in this file"                "no-marker"     "$(bq3_close_verdict "$LB" 3)"
LC="$OUT/bq3b.txt"; printf '%s\n' '11-10 11:14:00.000 I/TjA11y( 1): BQ3 marker close 3' '11-10 11:14:00.100 I/TjA11y( 1): PENCIL cover hidden (x)' '11-10 11:14:00.900 I/TjA11y( 1): PENCIL cover shown [4,564][55,615]' > "$LC"
t "a cover shown again after the hide is STUCK"    "STUCK-shown"   "$(bq3_close_verdict "$LC" 3)"
LD="$OUT/bq3c.txt"; printf '%s\n' '11-10 11:14:00.000 I/TjA11y( 1): BQ3 marker close 3' '11-10 11:14:00.100 I/TjA11y( 1): PENCIL fast watch (SystemUI is redrawing)' > "$LD"
t "no cover line at all"                           "no-cover-line" "$(bq3_close_verdict "$LD" 3)"
LE="$OUT/bq3d.txt"; printf '%s\n' '11-10 23:59:59.900 I/TjA11y( 1): BQ3 marker close 1' '11-10 23:59:59.990 I/TjA11y( 1): PENCIL cover hidden (x)' > "$LE"
t "hidden 90 ms after the key"                     "hidden-in-90"  "$(bq3_close_verdict "$LE" 1)"
bq3_closes_ok hidden-in-92 hidden-in-47 hidden-in-100 && echo "ok    closes ok: three hides pass" || { echo "FAIL  closes ok three hides"; fails=$((fails+1)); }
bq3_closes_ok hidden-in-92 hidden-in-47 no-cover-line && echo "ok    closes ok: the last one may find no cover" || { echo "FAIL  closes ok no cover last"; fails=$((fails+1)); }
bq3_closes_ok no-cover-line hidden-in-47 hidden-in-100 && { echo "FAIL  closes ok: the first must hide"; fails=$((fails+1)); } || echo "ok    closes ok flags a first closing without a hide"
bq3_closes_ok hidden-in-92 STUCK-shown hidden-in-100 && { echo "FAIL  closes ok must flag STUCK"; fails=$((fails+1)); } || echo "ok    closes ok flags a stuck cover"
bq3_closes_ok hidden-in-92 hidden-in-47 hidden-in-1600 && { echo "FAIL  closes ok must flag a late hide"; fails=$((fails+1)); } || echo "ok    closes ok flags a hide later than 1500 ms"
bq3_closes_ok hidden-in-92 no-marker hidden-in-100 && { echo "FAIL  closes ok must flag a missing marker"; fails=$((fails+1)); } || echo "ok    closes ok flags a missing marker"
# pen_tl (run #515): the cover's timeline, compact, from a start line
LT="$OUT/tl.txt"; printf '%s\n' '11-10 11:26:20.000 I/TjA11y( 1): BQ3 marker open 3' '11-10 11:26:20.050 I/TjA11y( 1): PENCIL cover shown [4,564][55,615]' '11-10 11:26:20.100 I/TjA11y( 1): PENCIL fast watch (SystemUI is redrawing)' \
  '11-10 11:26:27.079 I/TjA11y( 1): BN8b marker drag' '11-10 11:26:27.400 I/TjA11y( 1): PENCIL cover hidden (kept warm; a finger went down: the panel may be about to move, the pencil was last seen 12 ms ago)' \
  '11-10 11:26:29.300 I/TjA11y( 1): PENCIL cover shown [4,564][55,615]' '11-10 11:26:30.100 I/TjA11y( 1): BQ3 marker close 3' '11-10 11:26:30.230 I/TjA11y( 1): PENCIL cover hidden (kept warm; the followed pencil is gone)' > "$LT"
t "pen_tl: from the 3rd opening, 3 lines"          "20.000 BQ3 marker open 3|20.050 SHOWN [4,564][55,615]|27.079 BN8b marker drag|" "$(pen_tl "$LT" 'BQ3 marker open 3|BN8b marker drag' 0 3)"
t "pen_tl: skip 3, next 2 (hide with reason+age)"  "27.400 hid: a finger went down: the pane +12ms|29.300 SHOWN [4,564][55,615]|" "$(pen_tl "$LT" 'BQ3 marker open 3|BN8b marker drag' 3 2)"
t "pen_tl: a hide without an age"                  "30.230 hid: the followed pencil is gone|" "$(pen_tl "$LT" 'BQ3 marker open 3|BN8b marker drag' 6 3)"
t "pen_tl: the start line is not there"            "" "$(pen_tl "$LT" 'no such line' 0 5)"
FAKE_PID=""; FAKE_POL=""
FAKE_WIN="$COV_LIVE$COV_DEAD$SB_OPEN";               t "pen_blocks lists every block" "[pid=4321 touchable [4,564][55,615]] [pid=1111 touchable [4,564][55,615]] " "$(pen_blocks)"
FAKE_WIN="$COV_LIVE_HIDDEN$SB_OPEN";                 t "pen_blocks: a hidden block"   "[pid=4321 hidden [4,564][55,615]] " "$(pen_blocks)"
echo "failures: $fails"; exit $fails
