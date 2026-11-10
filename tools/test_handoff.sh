#!/usr/bin/env bash
# test_handoff.sh — hermetic self-test of the handoff/checkpoint system itself (~seconds).
# Runs as a fast check inside ckpt.sh, as a ship.sh gate, and as the first gate of both
# workflows. It builds a throwaway clone and never touches the real settings file or remote.
#   bash tools/test_handoff.sh        # exit 0 = all green
set -uo pipefail
D="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
T="$(mktemp -d)"; trap 'rm -rf "$T"' EXIT
PASS=0; FAIL=0
ok(){ PASS=$((PASS+1)); }
bad(){ FAIL=$((FAIL+1)); echo "  FAIL  $1"; }
check(){ if eval "$2" >/dev/null 2>&1; then ok; else bad "$1"; fi; }

# ---------------------------------------------------------------- the tiered emulator gate (AJ1, Tj 2026-11-06): the table and the scenario must agree
check "tools/gate.sh --check: gate-map.txt agrees with the scenario" "bash '$D/tools/gate.sh' --check"
check "tools/gate.sh --list prints the gate table" "bash '$D/tools/gate.sh' --list"

# ---------------------------------------------------------------- workflow minute rules
for W in "$D"/.github/workflows/*.yml; do
  # No `push:` trigger that is not tags-only (autosave pushes after every tool call).
  if python3 -I - "$W" <<'PY'
import sys, re
t = open(sys.argv[1]).read()
m = re.search(r'^on:\n((?:[ \t]+.*\n|\n)+)', t, re.M)
block = m.group(1) if m else ''
bad = False
lines = block.splitlines()
for i, l in enumerate(lines):
    if re.match(r'^  push:\s*$', l):
        nxt = lines[i+1] if i+1 < len(lines) else ''
        if 'branches' in nxt or 'paths' in nxt or not nxt.strip().startswith('tags'):
            bad = True
    if re.match(r'^  push:\s*\{', l):
        bad = True
sys.exit(1 if bad else 0)
PY
  then ok; else bad "$(basename "$W") has a non-tag push trigger (would burn Actions minutes)"; fi
done

# A workflow file that does not parse silently LOSES its triggers (GitHub then says "Workflow does not have 'workflow_dispatch' trigger"): 2026-11-05 a colon inside a step name did exactly that.
if python3 -I -c "import yaml" >/dev/null 2>&1; then
  for W in "$D"/.github/workflows/*.yml; do
    if python3 -I - "$W" <<'PY'
import sys, yaml
d = yaml.safe_load(open(sys.argv[1]))
on = d.get(True, d.get('on')) or {}
name = sys.argv[1].rsplit('/', 1)[-1]
if name in ('emulator.yml', 'android.yml') and 'workflow_dispatch' not in on:
    sys.exit(1)
sys.exit(0)
PY
    then ok; else bad "$(basename "$W") does not parse as YAML or lost its workflow_dispatch trigger (a colon inside a step name does that)"; fi
  done
fi

# ---------------------------------------------------------------- fixture repo + fake remote
git init -q --bare "$T/remote.git"
git clone -q "$T/remote.git" "$T/work" 2>/dev/null
mkdir -p "$T/work/tools" "$T/work/app"
cp "$D"/tools/*.sh "$D"/tools/session-root-hooks.json "$D"/tools/MIRROR_MAIN "$T/work/tools/" 2>/dev/null
echo off > "$T/work/tools/MIRROR_MAIN"
cp "$D"/ship.sh "$T/work/" 2>/dev/null
cd "$T/work" || exit 1
rm -f tools/test_handoff.sh   # the fixture must not recurse into this test via ckpt.sh
git config user.email t@t; git config user.name t
cat > app/build.gradle.kts <<'G'
        versionCode = 1
        versionName = "0.1.0"
G
printf '# BUILDLOG\n' > BUILDLOG.md
printf '# TASKS\n- [ ] first task\n' > TASKS.md
git checkout -q -b sess1
export CLAUDE_HOOK_SETTINGS="$T/settings.json" CLAUDE_REPO_ROOT="$T"   # never the real file

# ---------------------------------------------------------------- hooks installer
echo '{"permissions":{"allow":["Bash"]},"hooks":{"Stop":[{"hooks":[{"type":"command","command":"echo other # novig-checkpoint-hooks"}]}]}}' > "$CLAUDE_HOOK_SETTINGS"
check "install-hooks reports MISSING before install" '! bash tools/install-hooks.sh --check'
check "install-hooks installs"                      'bash tools/install-hooks.sh --quiet && bash tools/install-hooks.sh --check'
check "install-hooks is idempotent (no change)"     'B=$(cat "$CLAUDE_HOOK_SETTINGS"); bash tools/install-hooks.sh --quiet; [ "$B" = "$(cat "$CLAUDE_HOOK_SETTINGS")" ]'
check "install-hooks keeps permissions + novig hook" 'python3 -I -c "
import json,os
d=json.load(open(os.environ[\"CLAUDE_HOOK_SETTINGS\"]))
assert d[\"permissions\"][\"allow\"]==[\"Bash\"]
assert any(\"novig-checkpoint-hooks\" in h[\"command\"] for e in d[\"hooks\"][\"Stop\"] for h in e[\"hooks\"])
assert any(\"launcher-checkpoint-hooks\" in h[\"command\"] for e in d[\"hooks\"][\"Stop\"] for h in e[\"hooks\"])
"'
check "hooks point only at this repo's tools/" 'python3 -I -c "
import json,os
d=json.load(open(os.environ[\"CLAUDE_HOOK_SETTINGS\"]))
cmds=[h[\"command\"] for ev in d[\"hooks\"].values() for e in ev for h in e[\"hooks\"] if \"launcher-checkpoint-hooks\" in h[\"command\"]]
assert cmds and all(\"$PWD/tools/\" in c and \"*\" not in c for c in cmds), cmds
"'

# ---------------------------------------------------------------- secret scan
printf '%s\n' "token=ghp_$(printf 'a%.0s' $(seq 1 36))" > leak.txt
git add -A >/dev/null 2>&1
check "secretscan blocks a GitHub token" '! bash tools/secretscan.sh'
git reset -q; rm -f leak.txt

# ---------------------------------------------------------------- autosave + push
echo hi > a.txt
check "autosave commits and pushes"  'bash tools/autosave.sh --text >/dev/null; git log -1 --format=%s | grep -q "^auto-checkpoint:" && [ "$(bash tools/unpushed.sh)" = 0 ]'
check "branch reached the remote"    'git ls-remote --heads origin sess1 | grep -q sess1'
check "main is NOT touched while MIRROR_MAIN=off" '! git ls-remote --heads origin main | grep -q main'
printf 'ghp_%s\n' "$(printf 'b%.0s' $(seq 1 36))" > leak2.txt
check "autosave refuses a credential"  'bash tools/autosave.sh --text | grep -q "AUTOSAVE BLOCKED"; ! git log --format=%s | head -1 | grep -q leak; ! git ls-tree -r HEAD --name-only | grep -q leak2'
rm -f leak2.txt
# a merge with unresolved conflicts is never auto-committed (2026-11-10, TASKS BP1e: `git add -A` would mark the files resolved and publish the conflict markers)
CONF_BR="$(git rev-parse --abbrev-ref HEAD)"
git checkout -q -b conf_other; echo other > conf.txt; git add conf.txt; git commit -q -m "conf other"; git checkout -q "$CONF_BR"
echo mine > conf.txt; git add conf.txt; git commit -q -m "conf mine"
git merge conf_other >/dev/null 2>&1
CONF_HEAD="$(git rev-parse HEAD)"
check "autosave does NOT commit a merge with unresolved conflicts" 'bash tools/autosave.sh --text | grep -q "AUTOSAVE PAUSED" && [ "$(git rev-parse HEAD)" = "$CONF_HEAD" ] && [ -n "$(git diff --name-only --diff-filter=U)" ]'
check "ckpt does NOT commit a merge with unresolved conflicts either" 'bash tools/ckpt.sh "x" "y" 2>&1 | grep -q "ckpt NOT committed" && [ "$(git rev-parse HEAD)" = "$CONF_HEAD" ] && [ -n "$(git diff --name-only --diff-filter=U)" ]'
echo resolved > conf.txt; git add -A >/dev/null 2>&1; git commit -q --no-edit >/dev/null 2>&1
echo more > a2.txt
check "autosave saves again once the conflict is resolved and committed" 'bash tools/autosave.sh --text >/dev/null; git log -1 --format=%s | grep -q "^auto-checkpoint:" && [ "$(bash tools/unpushed.sh)" = 0 ]'

# ---------------------------------------------------------------- ckpt numbering + CHECKPOINT.md
bash tools/ckpt.sh "did one" "do two" >/dev/null 2>&1
N1="$(sed -n '1s/^# CHECKPOINT \([0-9]*\).*/\1/p' CHECKPOINT.md)"
check "ckpt writes CHECKPOINT.md with did/next" '[ -n "$N1" ] && grep -q "did one" CHECKPOINT.md && grep -q "do two" CHECKPOINT.md'
bash tools/ckpt.sh "did two" "do three" >/dev/null 2>&1
N2="$(sed -n '1s/^# CHECKPOINT \([0-9]*\).*/\1/p' CHECKPOINT.md)"
check "ckpt numbers only go up"  '[ "${N2:-0}" -gt "${N1:-99}" ]'
check "ckpt pushed everything"   '[ "$(bash tools/unpushed.sh)" = 0 ]'

# ---------------------------------------------------------------- resume: briefing, interruption, stranded branch
OUT="$(bash tools/resume.sh --text 2>&1)"
check "resume prints CHECKPOINT + open task" 'printf "%s" "$OUT" | grep -q "do three" && printf "%s" "$OUT" | grep -q "first task"'
check "resume reports a clean state (no interruption warning)" '! printf "%s" "$OUT" | grep -q "INTERRUPTED MID-CHANGE"'
echo wip > b.txt; bash tools/autosave.sh --text >/dev/null
OUT="$(bash tools/resume.sh --text 2>&1)"
check "resume flags an interrupted session"  'printf "%s" "$OUT" | grep -q "INTERRUPTED MID-CHANGE"'
bash tools/ckpt.sh "finished wip" "next" >/dev/null 2>&1
git checkout -q -b sess2 2>/dev/null; echo more > c.txt; bash tools/ckpt.sh "other session work" "continue" >/dev/null 2>&1
git checkout -q sess1 2>/dev/null
OUT="$(bash tools/resume.sh --text 2>&1)"
check "resume names another branch's unlanded work" 'printf "%s" "$OUT" | grep -q "sess2 has"'
check "resume briefing fits the 10,000-char hook cap (with a long INBOX + TASKS)" '
  for i in $(seq 1 80); do echo "- [ ] open task number $i with a reasonably long description of what must be done" >> TASKS.md; done
  head -c 6000 /dev/zero | tr "\0" "x" >> INBOX.md
  [ "$(bash tools/resume.sh --text | wc -c)" -lt 10000 ]'

# ---------------------------------------------------------------- the phone guide list has ONE source (the app's asset); the research copy must be identical
check "phone-guide steps.txt equals research/ADB_PHONE_COMMANDS.txt (edit the asset, then cp it)" 'cmp -s "$D/phoneguide/src/main/assets/steps.txt" "$D/research/ADB_PHONE_COMMANDS.txt"'
check "no phone-guide command starts with adb (the OTG shell takes the bare command)" '! grep -E "^(cmd|alt):[[:space:]]*adb( |$)" "$D/phoneguide/src/main/assets/steps.txt"'
check "no phone-guide command is destructive" '! grep -E "^(cmd|alt):.*(pm uninstall|pm clear|wipe|factory|rm -|reboot|setprop|locksettings|pm hide|pm suspend|svc wifi|svc data|settings put global (airplane|adb_enabled|development))" "$D/phoneguide/src/main/assets/steps.txt"'

# ---------------------------------------------------------------- the tablet intake: what Tj sends from the tablet is saved redacted + summarised (its own self-test, ~2 s)
if TI_OUT="$(bash "$D/tools/test_tablet_intake.sh" 2>&1)"; then ok; else bad "tablet intake self-test failed (bash tools/test_tablet_intake.sh)"; printf '%s\n' "$TI_OUT" | grep FAIL | head -10; fi

# ---------------------------------------------------------------- the node finder the emulator scenario's phone-guide checks use (tools/uinode.py; its own self-test, ~1 s)
if UN_OUT="$(bash "$D/tools/test_uinode.sh" 2>&1)"; then ok; else bad "uinode self-test failed (bash tools/test_uinode.sh)"; printf '%s\n' "$UN_OUT" | grep FAIL | head -10; fi

# ---------------------------------------------------------------- the chat probe of scenario check SS28f (tools/ss_chat_probe.py; its own self-test, ~1 s)
if SC_OUT="$(bash "$D/tools/test_ss_chat_probe.sh" 2>&1)"; then ok; else bad "chat probe self-test failed (bash tools/test_ss_chat_probe.sh)"; printf '%s\n' "$SC_OUT" | grep FAIL | head -10; fi
if SL_OUT="$(bash "$D/tools/test_ss_slider_probe.sh" 2>&1)"; then ok; else bad "slider probe self-test failed (bash tools/test_ss_slider_probe.sh)"; printf '%s\n' "$SL_OUT" | grep FAIL | head -10; fi

# ---------------------------------------------------------------- compaction must never be vetoed (TASKS BM1, 2026-11-09: an account plugin's PreCompact hook killed four sessions with "Prompt is too long"; own self-tests, ~5 s)
if CG_OUT="$(bash "$D/tools/test_compact_guard.sh" 2>&1)"; then ok; else bad "compact guard / keepalive self-test failed (bash tools/test_compact_guard.sh)"; printf '%s\n' "$CG_OUT" | grep FAIL | head -10; fi
if CW_OUT="$(bash "$D/tools/test_ctxwatch.sh" 2>&1)"; then ok; else bad "context watchdog self-test failed (bash tools/test_ctxwatch.sh)"; printf '%s\n' "$CW_OUT" | grep FAIL | head -10; fi
check "the project's own settings carry the compaction env (veto switched off, compaction early)" 'grep -q HANDOFF_STALE_SECONDS "$D/.claude/settings.json" && grep -q CLAUDE_AUTOCOMPACT_PCT_OVERRIDE "$D/.claude/settings.json" && grep -q HANDOFF_STALE_SECONDS "$D/tools/session-root-hooks.json"'
check "a PostToolUse hook with matcher * (every tool, MCP included) is registered in settings and template" 'python3 -c "import json,sys; [sys.exit(0 if any(e.get(\"matcher\")==\"*\" and \"posttool.sh\" in json.dumps(e) for e in json.load(open(p))[\"hooks\"][\"PostToolUse\"]) else 1) for p in sys.argv[1:]]" "$D/.claude/settings.json" "$D/tools/session-root-hooks.json"'
check "HANDOFF.md exists (a missing file is vetoed by the account plugin too)" '[ -f "$D/HANDOFF.md" ]'

# ---------------------------------------------------------------- inbox capture
printf '{"prompt":"please build X, my key is ghp_%s ok"}' "$(printf 'c%.0s' $(seq 1 36))" | bash tools/capture_inbox.sh
check "inbox captures the message, redacts the key, and commits it" 'grep -q "please build X" INBOX.md && grep -q "REDACTED-KEY" INBOX.md && ! grep -q "ghp_c" INBOX.md && [ "$(bash tools/unpushed.sh)" = 0 ]'

# ---------------------------------------------------------------- outbox capture (Tj, 2026-11-06: "why are my prompts and Claude findings not being saved?")
OB_T="$T/ob.jsonl"
printf '%s\n' '{"type":"user","message":{"role":"user","content":"hi"}}' "{\"type\":\"assistant\",\"message\":{\"role\":\"assistant\",\"content\":[{\"type\":\"text\",\"text\":\"Finding: the answer is 42, my key is ghp_$(printf 'c%.0s' $(seq 1 36))\"}]}}" > "$OB_T"
printf '{"hook_event_name":"Stop","transcript_path":"%s"}' "$OB_T" | bash tools/autosave.sh >/dev/null 2>&1
check "outbox captures Claude's last reply on Stop, quoted, key redacted, and commits it" 'grep -q "^> Finding: the answer is 42" OUTBOX.md && grep -q "REDACTED-KEY" OUTBOX.md && ! grep -q "ghp_c" OUTBOX.md && [ -z "$(git status --porcelain OUTBOX.md)" ]'
printf '{"hook_event_name":"Stop","transcript_path":"%s"}' "$OB_T" | bash tools/capture_outbox.sh
check "outbox does not repeat an identical reply" '[ "$(grep -c "^## 20" OUTBOX.md)" = 1 ]'
printf '{"hook_event_name":"PostToolUse","transcript_path":"%s"}' "$OB_T" | bash tools/autosave.sh >/dev/null 2>&1
check "outbox: a tool-call hook adds nothing for the same short reply" '[ "$(grep -c "^## 20" OUTBOX.md)" = 1 ]'
printf '%s\n' "{\"type\":\"assistant\",\"message\":{\"role\":\"assistant\",\"content\":[{\"type\":\"text\",\"text\":\"Short narration\"}]}}" > "$OB_T"
printf '{"hook_event_name":"PostToolUse","transcript_path":"%s"}' "$OB_T" | bash tools/autosave.sh >/dev/null 2>&1
check "outbox: a tool-call hook skips short narration (under 200 characters)" '[ "$(grep -c "^## 20" OUTBOX.md)" = 1 ]'
printf '%s\n' "{\"type\":\"assistant\",\"message\":{\"role\":\"assistant\",\"content\":[{\"type\":\"text\",\"text\":\"Finding two, mid-turn: $(printf 'z%.0s' $(seq 1 260))\"}]}}" > "$OB_T"
printf '{"hook_event_name":"PostToolUse","transcript_path":"%s"}' "$OB_T" | bash tools/autosave.sh >/dev/null 2>&1
check "outbox: a tool-call hook keeps substantial text, so a chat that dies mid-turn still leaves its findings" '[ "$(grep -c "^## 20" OUTBOX.md)" = 2 ]'

# ---------------------------------------------------------------- ship gate: version must increase
echo '| v0.1.0 | code 1 | 2026-01-01T00:00Z | first' >> BUILDLOG.md
SHIPOUT="$(bash ship.sh "x" 2>&1)"
check "ship refuses a non-increased versionCode" 'printf "%s" "$SHIPOUT" | grep -q "not higher" && ! printf "%s" "$SHIPOUT" | grep -q "ready for GitHub"'

echo "handoff self-test: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ]
