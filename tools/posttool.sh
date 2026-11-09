#!/usr/bin/env bash
# posttool.sh - PostToolUse hook for EVERY tool (matcher "*"): the light layer around autosave.sh (TASKS BM1).
#
# WHY: autosave.sh is registered for Edit|Write|NotebookEdit|Bash only. Claude Code's matcher is a list of EXACT tool
# names, so every GitHub MCP call, Agent, Read, Grep and Glob skipped the whole save ladder: the gate triage of the
# session that died on 2026-11-08 (8 hp failures, the cs flake, the solo reruns it had dispatched) was written between
# MCP calls, never reached OUTBOX.md, and died with the session. The same gap let the account plugin's compaction veto
# go unseen. This runs after ANY tool and does the cheap things:
#   1. keepalive --once : HANDOFF.md mtime fresh (the plugin's 600 s veto test, tools/keepalive.sh)
#   2. ctxwatch         : context-size watchdog; prints hook JSON (additionalContext) only from 60 % of the compaction point up
#   3. for tools autosave.sh does NOT cover: capture Claude's words (200+ chars) into OUTBOX.md and, only if that wrote
#      something, commit + push it through autosave.sh (no git work on a plain read)
# ALWAYS exits 0, never blocks. Whatever it prints is read as hook JSON, so only ctxwatch prints.
set -uo pipefail
D="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"; cd "$D" || exit 0
[ -e .git ] || exit 0
IN=""; [ ! -t 0 ] && IN="$(timeout 2 cat 2>/dev/null || true)"

bash tools/keepalive.sh --once >/dev/null 2>&1 || true
printf '%s' "$IN" | bash tools/ctxwatch.sh 2>/dev/null || true

TOOL="$(printf '%s' "$IN" | grep -o '"tool_name": *"[^"]*"' | head -1 | sed 's/.*: *"\(.*\)"/\1/')"
case "$TOOL" in
  Edit|Write|NotebookEdit|Bash|"") ;;         # autosave.sh handles these (and an unparsable payload is not worth guessing at)
  *)
    S0="$(stat -c %s OUTBOX.md 2>/dev/null || echo 0)"
    printf '%s' "$IN" | OUTBOX_MIN=200 bash tools/capture_outbox.sh >/dev/null 2>&1 || true
    S1="$(stat -c %s OUTBOX.md 2>/dev/null || echo 0)"
    [ "$S0" != "$S1" ] && { bash tools/autosave.sh --text </dev/null >/dev/null 2>&1 || true; }
    ;;
esac
exit 0
