#!/usr/bin/env bash
# Watch ANOTHER session's branch WITHOUT touching it (Tj, 2026-11-17, CG2/CG3: "without breaking or interrupting any other
# session", "monitor the other session and make sure to finish and build the release").
# Read-only: only `git ls-remote` (bounded by timeout), no fetch, no push, no CI call (the CI API cannot be read from bash).
#
#   bash tools/watch_branch.sh <branch> [quiet_min=30] [digest_min=45] [poll_s=60]
#
# Run it with Bash `run_in_background` (the script itself, never `( cmd ) &`): the one line it prints when it exits is the
# notification. It exits at the FIRST of
#   WATCH-EVENT QUIET    the branch tip did not change for quiet_min minutes (the other session may be dead or waiting on CI:
#                        look at the CI run list BEFORE you decide it is dead)
#   WATCH-EVENT TAG      a tag appeared or went away on origin (a release was made)
#   WATCH-EVENT GONE     the branch no longer exists on origin
#   WATCH-EVENT ERROR    ls-remote failed 10 times in a row (network)
#   WATCH-EVENT DIGEST   digest_min minutes passed (periodic check-in; re-arm it)
# The deadline is checked FIRST in every round (repo rule: every polling loop checks its deadline first).
set -u
BR="${1:?usage: watch_branch.sh <branch> [quiet_min] [digest_min] [poll_s]}"
QUIET_MIN="${2:-30}"; DIGEST_MIN="${3:-45}"; POLL="${4:-60}"
now() { date -u +%s; }
START="$(now)"; DEADLINE=$(( START + DIGEST_MIN * 60 )); LAST_CHANGE="$START"; CHANGES=0; FAILS=0; ROUND=0
tip_out="$(timeout 30 git ls-remote origin "refs/heads/$BR" 2>/dev/null)"; tip_rc=$?
T0="$(echo "$tip_out" | cut -c1-12)"
tags_now() { timeout 30 git ls-remote --tags origin 2>/dev/null | sed 's#.*refs/tags/##' | grep -v '\^{}' | sort; }
G0="$(tags_now)"
[ "$tip_rc" -eq 0 ] || { echo "WATCH-EVENT ERROR: first ls-remote failed (rc $tip_rc)"; exit 0; }
[ -n "$T0" ] || { echo "WATCH-EVENT GONE: branch $BR does not exist on origin"; exit 0; }
while :; do
  N="$(now)"
  if [ "$N" -ge "$DEADLINE" ]; then
    echo "WATCH-EVENT DIGEST after ${DIGEST_MIN} min: $BR tip $T0, changed $CHANGES time(s), last change $(( (N - LAST_CHANGE) / 60 )) min ago"; exit 0
  fi
  sleep "$POLL"
  ROUND=$(( ROUND + 1 )); N="$(now)"
  out="$(timeout 30 git ls-remote origin "refs/heads/$BR" 2>/dev/null)"; rc=$?
  if [ "$rc" -ne 0 ]; then
    FAILS=$(( FAILS + 1 ))
    [ "$FAILS" -ge 10 ] && { echo "WATCH-EVENT ERROR: ls-remote failed $FAILS times in a row (last rc $rc)"; exit 0; }
    continue
  fi
  FAILS=0; T1="$(echo "$out" | cut -c1-12)"
  [ -z "$T1" ] && { echo "WATCH-EVENT GONE: branch $BR vanished from origin (tip was $T0)"; exit 0; }
  if [ "$T1" != "$T0" ]; then T0="$T1"; LAST_CHANGE="$N"; CHANGES=$(( CHANGES + 1 )); fi
  if [ $(( N - LAST_CHANGE )) -ge $(( QUIET_MIN * 60 )) ]; then
    echo "WATCH-EVENT QUIET: $BR tip $T0 unchanged for $(( (N - LAST_CHANGE) / 60 )) min (changed $CHANGES time(s) while watching)"; exit 0
  fi
  if [ $(( ROUND % 3 )) -eq 0 ]; then   # tags only every third round: cheap
    G1="$(tags_now)"
    if [ -n "$G1" ] && [ "$G1" != "$G0" ]; then
      echo "WATCH-EVENT TAG: tags changed on origin: $(diff <(echo "$G0") <(echo "$G1") | grep '^[<>]' | tr '\n' ' ' | cut -c1-120)"; exit 0
    fi
  fi
done
