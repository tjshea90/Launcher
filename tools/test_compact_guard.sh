#!/usr/bin/env bash
# test_compact_guard.sh - hermetic self-test of tools/keepalive.sh + tools/compact_guard.sh (TASKS BM1, ~5 s).
# Pins the 2026-11-09 failure: a PreCompact hook that vetoes compaction unless HANDOFF.md is fresh killed four sessions.
# Uses a REPLICA of the plugin's veto logic in a fake user directory, and ALSO the real plugin script when this container has it.
#   bash tools/test_compact_guard.sh        # exit 0 = all green
set -uo pipefail
D="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
T="$(mktemp -d)"
cleanup() { KEEPALIVE_PIDFILE="$T/ka.pid" bash "$D/tools/keepalive.sh" --stop >/dev/null 2>&1; rm -rf "$T"; }
trap cleanup EXIT
PASS=0; FAIL=0
ok()  { PASS=$((PASS+1)); }
bad() { FAIL=$((FAIL+1)); echo "  FAIL  $1"; }
check() { if eval "$2" >/dev/null 2>&1; then ok; else bad "$1"; fi; }
age() { echo $(( $(date +%s) - $(stat -c %Y "$1" 2>/dev/null || echo 0) )); }

FH="$T/fh"; PL="$FH/.claude/plugins/synced/p/hooks"; REPO="$T/repo"
mkdir -p "$PL" "$REPO/.claude"
cat > "$PL/precompact-handoff.sh" <<'EOS'
#!/usr/bin/env bash
# replica of the account plugin's veto logic (session-handoff v0.1.0 precompact-handoff.sh)
STALE=${HANDOFF_STALE_SECONDS:-600}; P="${CLAUDE_PROJECT_DIR:-$PWD}"; newest=0
for f in "$P"/HANDOFF.md "$P"/HANDOFF.*.md; do [ -f "$f" ] || continue; m=$(stat -c %Y "$f"); [ "$m" -gt "$newest" ] && newest=$m; done
if [ "$newest" -eq 0 ]; then printf '{"decision":"block","reason":"No HANDOFF.md found."}\n'
elif [ $(( $(date +%s) - newest )) -gt "$STALE" ]; then printf '{"decision":"block","reason":"Freshest handoff is stale."}\n'; fi
exit 0
EOS
cat > "$PL/hooks.json" <<'EOJ'
{"hooks":{"PreCompact":[{"matcher":"*","hooks":[{"type":"command","command":"\"${CLAUDE_PLUGIN_ROOT}/hooks/precompact-handoff.sh\""}]}]}}
EOJ
chmod +x "$PL/precompact-handoff.sh"
guard() { HOME="$FH" COMPACT_GUARD_REPO="$REPO" python3 -I "$D/tools/compact_guard.py" "$@"; }

# 1. the 2026-11-09 failure, reproduced: HANDOFF.md two hours old -> the guard must call it a VETO (exit 1)
echo "# handoff" > "$REPO/HANDOFF.md"; touch -d '2 hours ago' "$REPO/HANDOFF.md"
OUT1="$(guard 2>&1)"; RC1=$?
check "stale HANDOFF.md -> guard exits 1 (the veto that killed four sessions)" '[ "$RC1" = 1 ]'
check "the VETO line names the hook and the fix" 'printf "%s" "$OUT1" | grep -q "VETO" && printf "%s" "$OUT1" | grep -q "precompact-handoff.sh" && printf "%s" "$OUT1" | grep -q "keepalive"'

# 2. keepalive --once (layer 2) clears it
KEEPALIVE_FILE="$REPO/HANDOFF.md" bash "$D/tools/keepalive.sh" --once
check "keepalive --once makes the mtime fresh" '[ "$(age "$REPO/HANDOFF.md")" -lt 5 ]'
OUT2="$(guard 2>&1)"; RC2=$?
check "after keepalive --once the guard is OK (exit 0) and says how many hooks could veto" '[ "$RC2" = 0 ] && printf "%s" "$OUT2" | grep -q "OK" && printf "%s" "$OUT2" | grep -q "1 could veto"'
check "--quiet prints nothing when OK" '[ -z "$(guard --quiet 2>&1)" ]'
check "keepalive changed only the mtime, not the content" '[ "$(cat "$REPO/HANDOFF.md")" = "# handoff" ]'

# 3. the settings env knob (layer 3) clears it too, and removing it brings the veto back (control)
touch -d '2 hours ago' "$REPO/HANDOFF.md"
printf '{"env":{"HANDOFF_STALE_SECONDS":"315360000"}}\n' > "$REPO/.claude/settings.json"
guard >/dev/null 2>&1; RC3=$?
check "stale file but HANDOFF_STALE_SECONDS in settings env -> guard OK" '[ "$RC3" = 0 ]'
rm -f "$REPO/.claude/settings.json"
guard >/dev/null 2>&1; RC3b=$?
check "control: without that env the same stale file vetoes again" '[ "$RC3b" = 1 ]'

# 4. a MISSING HANDOFF.md is vetoed too (the plugin blocks on 'No HANDOFF.md found')
rm -f "$REPO/HANDOFF.md"
guard >/dev/null 2>&1; RC4=$?
check "missing HANDOFF.md -> guard exits 1" '[ "$RC4" = 1 ]'

# 5. a hook that never vetoes is reported as 'none could veto'; prompt/agent hooks are ignored (docs: not run for PreCompact)
printf '#!/usr/bin/env bash\nexit 0\n' > "$PL/precompact-handoff.sh"
mkdir -p "$FH/.claude/plugins/synced/q/hooks"
printf '{"hooks":{"PreCompact":[{"hooks":[{"type":"prompt","prompt":"Evaluate whether compaction should proceed {\\"ok\\": false}"}]}]}}\n' > "$FH/.claude/plugins/synced/q/hooks/hooks.json"
OUT5="$(guard 2>&1)"; RC5=$?
check "harmless hook + a prompt-type hook -> OK, 0 could veto, no crash" '[ "$RC5" = 0 ] && printf "%s" "$OUT5" | grep -q "0 could veto"'

# 6. the daemon (layer 1): the loop itself keeps the file fresh, and stops on --stop
export KEEPALIVE_FILE="$REPO/HANDOFF.md" KEEPALIVE_PIDFILE="$T/ka.pid" KEEPALIVE_INTERVAL=1 KEEPALIVE_MAX_SECONDS=60
echo "# handoff" > "$KEEPALIVE_FILE"
bash "$D/tools/keepalive.sh" --start >/dev/null 2>&1
check "daemon reports running" 'bash "$D/tools/keepalive.sh" --status'
touch -d '2 hours ago' "$KEEPALIVE_FILE"
DL=$(( $(date +%s) + 6 ))
while [ "$(date +%s)" -lt "$DL" ] && [ "$(age "$KEEPALIVE_FILE")" -gt 5 ]; do sleep 0.3; done
check "daemon touched a file that was two hours old, by itself" '[ "$(age "$KEEPALIVE_FILE")" -lt 5 ]'
check "--start twice does not start a second daemon" '[ "$(bash "$D/tools/keepalive.sh" --start 2>&1 | grep -c "already running")" = 1 ]'
bash "$D/tools/keepalive.sh" --stop >/dev/null 2>&1
check "daemon stops on --stop" '! bash "$D/tools/keepalive.sh" --status'
unset KEEPALIVE_FILE KEEPALIVE_PIDFILE KEEPALIVE_INTERVAL KEEPALIVE_MAX_SECONDS

# 7. keepalive restores a TRACKED HANDOFF.md that was deleted, and never invents an untracked one (fixtures and other repos stay clean)
G="$T/g"; mkdir -p "$G/tools"; cp "$D/tools/keepalive.sh" "$G/tools/"
( cd "$G" && git init -q . && git config user.email t@t && git config user.name t && echo "# tracked" > HANDOFF.md && git add -A && git commit -q -m init && rm HANDOFF.md \
  && bash tools/keepalive.sh --once )
check "deleted but tracked HANDOFF.md is restored from git" '[ "$(cat "$G/HANDOFF.md" 2>/dev/null)" = "# tracked" ]'
G2="$T/g2"; mkdir -p "$G2/tools"; cp "$D/tools/keepalive.sh" "$G2/tools/"
( cd "$G2" && git init -q . && bash tools/keepalive.sh --once )
check "no HANDOFF.md anywhere -> none is invented (the tree stays clean)" '[ ! -e "$G2/HANDOFF.md" ]'

# 8. the REAL plugin script, when this container has it: the guard must see its veto, and keepalive must clear it
REALH="$(ls "$HOME"/.claude/plugins/synced/*/*/hooks/precompact-handoff.sh 2>/dev/null | head -1)"
if [ -n "$REALH" ]; then
  R="$T/real"; mkdir -p "$R" "$T/eh"; echo "# h" > "$R/HANDOFF.md"; touch -d '2 hours ago' "$R/HANDOFF.md"
  RG() { HOME="$T/eh" COMPACT_GUARD_PLUGINS="$HOME/.claude/plugins" COMPACT_GUARD_REPO="$R" python3 -I "$D/tools/compact_guard.py" "$@"; }
  RG >/dev/null 2>&1; RR1=$?
  check "REAL session-handoff hook: a stale HANDOFF.md is seen as a veto" '[ "$RR1" = 1 ]'
  KEEPALIVE_FILE="$R/HANDOFF.md" bash "$D/tools/keepalive.sh" --once
  RG >/dev/null 2>&1; RR2=$?
  check "REAL session-handoff hook: after keepalive it no longer vetoes" '[ "$RR2" = 0 ]'
else
  echo "  note  real session-handoff plugin not installed here - replica-only run"
fi

echo "test_compact_guard: $PASS passed, $FAIL failed"
[ "$FAIL" = 0 ]
