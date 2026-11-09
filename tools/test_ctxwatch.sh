#!/usr/bin/env bash
# test_ctxwatch.sh - hermetic self-test of tools/ctxwatch.py, the context-size watchdog (TASKS BM1d, ~3 s).
# It runs the script in a temp directory with its own .git/ (state and log live there) against synthetic transcripts.
# With CLAUDE_AUTOCOMPACT_PCT_OVERRIDE=40 the auto-compact point is T = 392,000 tokens:
#   level 1 from est >= 235,200 | level 2 from est >= 333,200 | level 3 when the LAST CALL's prompt > 422,000 | level 4 from est >= 879,300
#   bash tools/test_ctxwatch.sh        # exit 0 = all green
set -uo pipefail
D="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
T="$(mktemp -d)"; trap 'rm -rf "$T"' EXIT
R="$T/r"; mkdir -p "$R/.git"
PASS=0; FAIL=0
ok()  { PASS=$((PASS+1)); }
bad() { FAIL=$((FAIL+1)); echo "  FAIL  $1"; }
check() { if eval "$2" >/dev/null 2>&1; then ok; else bad "$1"; fi; }

# mk PROMPT [EXTRA_CHARS] : a transcript whose last real model call had PROMPT tokens, followed by one tool result of EXTRA_CHARS characters
mk() {
  python3 -I - "$T/t.jsonl" "$1" "${2:-0}" "${3:-}" <<'PY'
import json, sys
path, prompt, extra, kind = sys.argv[1], int(sys.argv[2]), int(sys.argv[3]), sys.argv[4]
def asst(mid, p, side=False, model="claude-x", tool=True):
    c = [{"type": "text", "text": "ok"}]
    if tool:
        c.append({"type": "tool_use", "id": "t" + mid, "name": "Bash", "input": {"command": "echo " + mid}})
    return {"type": "assistant", "isSidechain": side, "message": {"id": mid, "model": model, "content": c,
            "usage": {"input_tokens": 2, "cache_read_input_tokens": p - 2, "cache_creation_input_tokens": 0, "output_tokens": 5}}}
rows = [{"type": "user", "message": {"role": "user", "content": "hi"}}]
if kind == "boundary":
    rows.append({"type": "system", "subtype": "compact_boundary", "compactMetadata": {"trigger": "auto", "preTokens": 391000}, "timestamp": "2026-11-09T01:00:00Z"})
if kind == "farboundary":      # the marker, then 600 KB of re-injected files, then the first model call after the compaction
    rows.append({"type": "system", "subtype": "compact_boundary", "compactMetadata": {"trigger": "auto", "preTokens": 392147}, "timestamp": "2026-11-09T02:00:00Z"})
    for i in range(6):
        rows.append({"type": "attachment", "attachment": {"content": "y" * 100000}})
rows.append(asst("m1", prompt))
if kind == "stale":            # a transcript that lags: the last model call is the one BEFORE the compaction, which is already in the file
    rows.append({"type": "system", "subtype": "compact_boundary", "compactMetadata": {"trigger": "auto", "preTokens": 440000}, "timestamp": "2026-11-09T03:00:00Z"})
    rows.append({"type": "user", "message": {"role": "user", "content": "SUMMARY " + "s" * 20000}})
    for i in range(3):
        rows.append({"type": "attachment", "attachment": {"content": "z" * 100000}})
if kind == "sidechain":
    rows.append(asst("m2", 900000, side=True))
    rows.append(asst("m3", 900000, model="<synthetic>"))
if extra:
    rows.append({"type": "user", "message": {"role": "user", "content": [{"type": "tool_result", "tool_use_id": "tm1", "content": "x" * extra}]}})
with open(path, "w") as f:
    for r in rows:
        f.write(json.dumps(r) + "\n")
PY
}
# hook [ENV=VAL ...] : one PostToolUse hook call inside the temp repo; prints what the hook printed
hook() {
  ( cd "$R" && printf '{"hook_event_name":"PostToolUse","transcript_path":"%s"}' "$T/t.jsonl" \
      | env -u CLAUDE_CODE_AUTO_COMPACT_WINDOW CLAUDE_AUTOCOMPACT_PCT_OVERRIDE=40 "$@" python3 -I "$D/tools/ctxwatch.py" )
}
field() { python3 -I -c 'import json,sys; d=json.loads(sys.stdin.read()); print(d.get(sys.argv[1]) if sys.argv[1]=="systemMessage" else d["hookSpecificOutput"][sys.argv[1]])' "$1"; }

mk 100000;                       O="$(hook)"; check "100K (26 % of T): silent" '[ -z "$O" ]'
mk 250000;                       O="$(hook)"
check "250K (64 %): level 1 info, valid hook JSON for PostToolUse" 'printf "%s" "$O" | field hookEventName | grep -qx PostToolUse && printf "%s" "$O" | field additionalContext | grep -q "CONTEXT WATCH (info)"'
check "level 1 is not pushed at the user (no systemMessage)" '! printf "%s" "$O" | grep -q systemMessage'
O="$(hook)";                     check "the same level is spoken only once" '[ -z "$O" ]'
mk 340000;                       O="$(hook)"
check "340K (87 %): level 2 'due soon', tells Claude to ckpt, shown to the user too" 'printf "%s" "$O" | field additionalContext | grep -q "due soon" && printf "%s" "$O" | field additionalContext | grep -q "tools/ckpt.sh" && printf "%s" "$O" | field systemMessage | grep -q "due soon"'
mk 440000;                       O="$(hook)"
check "440K last call (> T + 30K) seen ONCE is not an alarm yet (one big tool result right before a compaction looks the same)" '! printf "%s" "$O" | grep -q "CONTEXT ALARM"'
mk 445000;                       O="$(hook)"
check "a second, different model call above T + 30K (445K): level 3 ALARM names the cause and compact_guard" 'printf "%s" "$O" | field additionalContext | grep -q "CONTEXT ALARM" && printf "%s" "$O" | field additionalContext | grep -q "compact_guard.sh" && printf "%s" "$O" | field additionalContext | grep -q "did NOT fire"'
N=0; for i in $(seq 1 12); do [ -n "$(hook)" ] && N=$((N+1)); done
check "the alarm repeats, but only once in 12 calls (not on every tool call)" '[ "$N" = 1 ]'
mk 920000;                       O="$(hook)"
check "920K: level 4 CRITICAL tells Claude to stop and Tj to start a new session" 'printf "%s" "$O" | field additionalContext | grep -q "CONTEXT CRITICAL" && printf "%s" "$O" | field additionalContext | grep -q "start a new session"'
mk 100000;                       O="$(hook)"
check "context falls back to 100K (a compaction happened): silent, level reset" '[ -z "$O" ]'
mk 250000;                       O="$(hook)"
check "after the reset the next climb speaks again at level 1" 'printf "%s" "$O" | field additionalContext | grep -q "CONTEXT WATCH (info)"'

# the estimate counts tool results that are not in any usage yet
rm -f "$R/.git/ctxwatch.state"
mk 150000 400000;                O="$(hook)"
check "150K call + a 400,000-char tool result (about 118K tokens) reaches level 1 (est, not just usage)" 'printf "%s" "$O" | field additionalContext | grep -q "CONTEXT WATCH (info)"'
# subagent (isSidechain) and <synthetic> entries are not the main thread's prompt
rm -f "$R/.git/ctxwatch.state"
mk 100000 0 sidechain;           O="$(hook)"
check "sidechain + synthetic entries with 900K are ignored (main thread is 100K): silent" '[ -z "$O" ]'
# a compaction in the transcript is logged (proof that compaction worked)
mk 100000 0 boundary; hook >/dev/null
check "a compact_boundary entry is logged as COMPACTION SEEN with its size" 'grep -q "COMPACTION SEEN trigger=auto pre=391000" "$R/.git/ctxwatch.log"'
# a transcript that lags: the compaction marker is the newest thing in it, the last model call (440K) is from before it
rm -f "$R/.git/ctxwatch.state" "$R/.git/ctxwatch.log"
mk 440000 0 stale;               O="$(hook)"
check "compaction newer than the last model call (stale 440K call): silent, no false alarm" '[ -z "$O" ]'
check "... and it is logged as a fresh compaction" 'grep -q "COMPACTION SEEN trigger=auto pre=440000" "$R/.git/ctxwatch.log"'
O="$(cd "$R" && env CLAUDE_AUTOCOMPACT_PCT_OVERRIDE=40 python3 -I "$D/tools/ctxwatch.py" --report --transcript "$T/t.jsonl")"
check "--report says the session was just compacted (estimate from what follows the marker, not 440K)" 'printf "%s" "$O" | grep -q "just compacted" && printf "%s" "$O" | grep -q "auto at 2026-11-09T03:00:00Z (was 440000)" && ! printf "%s" "$O" | grep -q "~44[0-9]K"'
# the marker is further back than the 400 KB window (a compaction re-injects hundreds of KB): found when the context falls, and by --report
rm -f "$R/.git/ctxwatch.state" "$R/.git/ctxwatch.log"
mk 400000; hook >/dev/null
mk 120000 0 farboundary; hook >/dev/null
check "context fell 400K -> 120K, marker 600 KB back: COMPACTION SEEN is still logged (look-back beyond the window)" 'grep -q "COMPACTION SEEN trigger=auto pre=392147" "$R/.git/ctxwatch.log"'
O="$(cd "$R" && env CLAUDE_AUTOCOMPACT_PCT_OVERRIDE=40 python3 -I "$D/tools/ctxwatch.py" --report --transcript "$T/t.jsonl")"
check "--report finds a marker beyond the window" 'printf "%s" "$O" | grep -q "auto at 2026-11-09T02:00:00Z (was 392147)"'
# a smaller auto-compact window moves T (500,000 x 40 % = 200,000)
rm -f "$R/.git/ctxwatch.state"
mk 250000;                       O="$(hook CLAUDE_CODE_AUTO_COMPACT_WINDOW=500000)"
check "CLAUDE_CODE_AUTO_COMPACT_WINDOW=500000 moves the point to 200K: 250K is an ALARM there" 'printf "%s" "$O" | field additionalContext | grep -q "CONTEXT ALARM" && printf "%s" "$O" | field additionalContext | grep -q "~200K"'

# robustness: never fails, never prints on bad input
printf 'not json at all\n\x00\x01\x02\n' > "$T/t.jsonl"; O="$(hook)"; RC=$?
check "garbage transcript: silent, exit 0" '[ -z "$O" ] && [ "$RC" = 0 ]'
: > "$T/t.jsonl"; O="$(hook)"; RC=$?
check "empty transcript: silent, exit 0" '[ -z "$O" ] && [ "$RC" = 0 ]'
rm -f "$T/t.jsonl"; O="$(hook)"; RC=$?
check "missing transcript: silent, exit 0" '[ -z "$O" ] && [ "$RC" = 0 ]'
O="$(cd "$R" && printf 'not json' | python3 -I "$D/tools/ctxwatch.py")"; RC=$?
check "invalid hook JSON on stdin: silent, exit 0" '[ -z "$O" ] && [ "$RC" = 0 ]'

# the human views
mk 250000
O="$(cd "$R" && env CLAUDE_AUTOCOMPACT_PCT_OVERRIDE=40 python3 -I "$D/tools/ctxwatch.py" --report --transcript "$T/t.jsonl")"
check "--report says where auto-compact fires and how full the session is" 'printf "%s" "$O" | grep -q "auto-compact at ~392K" && printf "%s" "$O" | grep -q "~250K tokens"'
O="$(cd "$R" && python3 -I "$D/tools/ctxwatch.py" --top --transcript "$T/t.jsonl")"
check "--top lists the tools the context growth belongs to" 'printf "%s" "$O" | grep -q "first prompt" && printf "%s" "$O" | grep -q "tool results by tool"'

echo "test_ctxwatch: $PASS passed, $FAIL failed"
[ "$FAIL" = 0 ]
