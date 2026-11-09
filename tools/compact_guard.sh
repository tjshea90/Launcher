#!/usr/bin/env bash
# compact_guard.sh - can any PreCompact hook veto a compaction right now? (TASKS BM1c; why: tools/compact_guard.py)
#   bash tools/compact_guard.sh [--quiet]     exit 0 = nothing vetoes, 1 = something does (the line says what and how to fix it)
set -uo pipefail
D="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
command -v python3 >/dev/null 2>&1 || { echo "compaction guard: skipped (no python3)"; exit 0; }
COMPACT_GUARD_REPO="${COMPACT_GUARD_REPO:-$D}" python3 -I "$D/tools/compact_guard.py" "$@"
