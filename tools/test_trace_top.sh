#!/usr/bin/env bash
# test_trace_top.sh - self-test of tools/trace_top.py (runs inside tools/test_handoff.sh; < 1 s, no network, no device).
#   bash tools/test_trace_top.sh      # exit 0 = green
# Pins the parser's reading of an ART method trace (header text + SLOW binary records, versions 2 and 3), the self / inclusive arithmetic and the two
# /proc snapshot diff used by the emulator section `pr` (DA1). The format itself is only proven by a real trace from the CI emulator.
set -uo pipefail
D="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
python3 -I "$D/tools/trace_top.py" --selftest
