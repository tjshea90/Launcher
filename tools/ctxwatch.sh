#!/usr/bin/env bash
# ctxwatch.sh - hook wrapper for tools/ctxwatch.py (context-size watchdog, TASKS BM1d). See that file for the why.
# Reads the hook JSON from stdin (bounded: a caller with nothing on stdin must never hang this), always exits 0.
#   bash tools/ctxwatch.sh --report     # one line: how full is this session, where does auto-compact fire
set -uo pipefail
D="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"; cd "$D" || exit 0
command -v python3 >/dev/null 2>&1 || exit 0
if [ "${1:-}" = "--report" ]; then python3 -I "$D/tools/ctxwatch.py" "$@" 2>/dev/null; exit 0; fi
IN=""; [ ! -t 0 ] && IN="$(timeout 2 cat 2>/dev/null || true)"
printf '%s' "$IN" | timeout 10 python3 -I "$D/tools/ctxwatch.py" "$@" 2>/dev/null
exit 0
