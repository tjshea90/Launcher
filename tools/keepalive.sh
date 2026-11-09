#!/usr/bin/env bash
# keepalive.sh — keep HANDOFF.md's MODIFICATION TIME fresh so that an account plugin can never veto a
# compaction (TASKS BM1, 2026-11-09; full evidence: research/COMPACTION_BLOCKED_2026-11-09.md).
#
# WHY THIS EXISTS
# ---------------
# Tj's claude.ai account carries the plugin `session-handoff`, which is synced into EVERY cloud
# session. It ships a PreCompact hook (precompact-handoff.sh) that answers {"decision":"block"}
# unless a HANDOFF.md / HANDOFF.<session>.md in the project root was modified in the last 600 s
# (env knob HANDOFF_STALE_SECONDS). Our HANDOFF.md is edited only at releases, so EVERY auto-compaction
# (set at 901,600 tokens of a 1,000,000 window) was vetoed: the CLI retried 3 times, gave up, the context
# grew to the 977K blocking limit and the session died with "Prompt is too long". Four sessions in a row
# (969K-978K tokens each, ~$30 each) died that way; none had ever compacted.
#
# WHAT THIS DOES (and does not)
# -----------------------------
# Only the file's mtime changes, never its content, so git sees nothing and no commit is made. The real
# handoff is CHECKPOINT.md (committed after every step); HANDOFF.md is the static orientation page.
# It never creates a file that is not already tracked (a fixture / other repo stays clean); a tracked
# HANDOFF.md that was deleted is restored from git, because a MISSING file is vetoed too.
#
# LAYERS (any one is enough; all four exist on purpose):
#   1. this daemon: `--start` (resume.sh at session start) touches the file every 60 s
#   2. `--once` from autosave.sh / capture_inbox.sh / posttool.sh (every tool call, every prompt)
#   3. HANDOFF_STALE_SECONDS=315360000 in .claude/settings.json env (the plugin's own knob)
#   4. tools/compact_guard.sh runs the real veto hooks and says so if one still blocks
#
#   bash tools/keepalive.sh            # = --once
#   bash tools/keepalive.sh --start    # idempotent background daemon (dies with the Claude process / after 24 h)
#   bash tools/keepalive.sh --stop | --status
#
# ALWAYS exits 0 except --status (0 = daemon running, 1 = not). Env overrides are for tools/test_compact_guard.sh.
set -uo pipefail
D="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"; cd "$D" || exit 0
FILE="${KEEPALIVE_FILE:-$D/HANDOFF.md}"
PIDF="${KEEPALIVE_PIDFILE:-$D/.git/keepalive.pid}"
INTERVAL="${KEEPALIVE_INTERVAL:-60}"
MAXLIFE="${KEEPALIVE_MAX_SECONDS:-86400}"

touch_once() {
  if [ ! -f "$FILE" ] && [ -z "${KEEPALIVE_FILE:-}" ] && [ -e .git ] && git cat-file -e HEAD:HANDOFF.md 2>/dev/null; then
    git show HEAD:HANDOFF.md > "$FILE" 2>/dev/null || true
  fi
  [ -f "$FILE" ] && touch "$FILE" 2>/dev/null
  return 0
}

alive() {   # pid of a running keepalive daemon, else nothing
  local p; p="$(cat "$PIDF" 2>/dev/null || true)"
  [ -n "$p" ] && kill -0 "$p" 2>/dev/null && tr '\0' ' ' < "/proc/$p/cmdline" 2>/dev/null | grep -q 'keepalive' && echo "$p"
}

case "${1:---once}" in
  --once) touch_once ;;
  --run)
    mkdir -p "$(dirname "$PIDF")" 2>/dev/null
    echo "$$" > "$PIDF"
    END=$(( $(date +%s) + MAXLIFE ))
    # deadline FIRST (CLAUDE.md: every polling loop checks its deadline first)
    while [ "$(date +%s)" -lt "$END" ]; do
      touch_once
      # stop when the Claude process that started us is gone, or when the pid file no longer names us
      if [ -n "${CLAUDE_PID:-}" ] && ! kill -0 "$CLAUDE_PID" 2>/dev/null; then break; fi
      [ "$(cat "$PIDF" 2>/dev/null || true)" = "$$" ] || break
      sleep "$INTERVAL"
    done
    [ "$(cat "$PIDF" 2>/dev/null || true)" = "$$" ] && rm -f "$PIDF"
    ;;
  --start)
    touch_once
    P="$(alive || true)"
    if [ -n "$P" ]; then echo "keepalive already running (pid $P)"; exit 0; fi
    # fully detached, every pipe closed: a hook that leaves a pipe open never finishes
    setsid nohup bash "$D/tools/keepalive.sh" --run </dev/null >/dev/null 2>&1 &
    for _ in 1 2 3 4 5 6 7 8 9 10; do [ -n "$(alive || true)" ] && break; sleep 0.2; done
    P="$(alive || true)"
    [ -n "$P" ] && echo "keepalive started (pid $P)" || echo "keepalive could not start (layers 2-4 still protect)"
    ;;
  --stop)
    P="$(alive || true)"
    if [ -n "$P" ]; then kill "$P" 2>/dev/null; rm -f "$PIDF"; echo "keepalive stopped"; else echo "keepalive not running"; fi
    ;;
  --status)
    P="$(alive || true)"
    AGE="?"; [ -f "$FILE" ] && AGE=$(( $(date +%s) - $(stat -c %Y "$FILE" 2>/dev/null || echo 0) ))
    if [ -n "$P" ]; then echo "keepalive running (pid $P), $(basename "$FILE") age ${AGE}s"; exit 0; fi
    echo "keepalive NOT running, $(basename "$FILE") age ${AGE}s"; exit 1 ;;
  *) echo "usage: keepalive.sh [--once|--start|--stop|--status]"; ;;
esac
exit 0
