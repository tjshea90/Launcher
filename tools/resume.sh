#!/usr/bin/env bash
# resume.sh — the cold-start handoff. Runs from the SessionStart hook (and by hand:
# `bash tools/resume.sh`, if a session did not get the briefing).
#
# A session can die at any point with no warning. The next one — another account, another
# device, hours later — opens this repo COLD. Everything it needs must already be on disk and
# must arrive without Tj explaining anything. This prints: whether the checkout is current
# with GitHub, whether the last session was cut off mid-step, any other branch holding
# unlanded work, an unrecorded release, CI state hints, then CHECKPOINT.md, the OPEN items
# of TASKS.md, the INBOX tail, and the rules.
#
# BUDGET: Claude Code caps hook output at 10,000 characters (more is cut to a 2,000-char
# preview — the briefing is silently lost). tools/test_handoff.sh checks the size. Keep this
# short; narrative belongs in CHECKPOINT.md / TASKS.md / CLAUDE.md.
set -uo pipefail
D="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"; cd "$D" || exit 0

TEXT_MODE=0
for a in "$@"; do [ "$a" = "--text" ] && TEXT_MODE=1; done

# Self-repair the safety net (idempotent, silent when correct).
bash tools/install-hooks.sh --quiet >/dev/null 2>&1 || true
# 2026-11-09 (TASKS BM1): an account plugin vetoes every compaction unless HANDOFF.md was touched in the last 600 s, which killed
# four sessions with "Prompt is too long". Keep the file fresh (idempotent background daemon, dies with the Claude process).
bash tools/keepalive.sh --start >/dev/null 2>&1 || true

BRIEF="$(
  echo "=== RESUMING LAUNCHER — Android 10 Go launcher. A previous session may have been cut off. ==="
  echo "=== Do NOT re-plan or re-read finished work: continue from 'Do this next' below.        ==="
  echo

  if [ -e .git ]; then
    find .git -name '*.lock' -mmin +2 -delete 2>/dev/null || true
    timeout 25 git fetch -q --tags origin >/dev/null 2>&1 || echo "  NOTE  could not reach GitHub — working from the local checkout only."

    DIRTY="$(git status --porcelain 2>/dev/null)"
    CURBR="$(git branch --show-current 2>/dev/null || true)"
    UP="$(git rev-parse --abbrev-ref '@{u}' 2>/dev/null || true)"
    [ -z "$UP" ] && [ -n "$CURBR" ] && UP="origin/$CURBR"
    BEHIND=0; AHEAD=0
    if [ -n "$UP" ] && git rev-parse --verify -q "$UP" >/dev/null 2>&1; then
      BEHIND="$(git rev-list --count "HEAD..$UP" 2>/dev/null || echo 0)"
      AHEAD="$(git rev-list --count "$UP..HEAD" 2>/dev/null || echo 0)"
    elif [ -n "$CURBR" ]; then
      echo "  WARN  branch $CURBR is not on GitHub yet — nothing on it is backed up until the next checkpoint pushes it."
    fi
    # Fast-forward only, from a clean tree (a merge could conflict on a half-finished change).
    if [ -z "$DIRTY" ] && [ "$BEHIND" -gt 0 ] && [ "$AHEAD" -eq 0 ]; then
      timeout 25 git merge -q --ff-only "$UP" >/dev/null 2>&1 \
        && echo "  OK    pulled $BEHIND newer commit(s) from GitHub — checkout is current."
    elif [ "$BEHIND" -gt 0 ] && [ "$AHEAD" -gt 0 ]; then
      echo "  WARN  branch DIVERGED from GitHub ($AHEAD local, $BEHIND remote) — two sessions at once? Reconcile before working."
    elif [ "$BEHIND" -gt 0 ]; then
      echo "  WARN  $BEHIND commit(s) behind GitHub and the tree is dirty — reconcile before new work."
    fi
    [ "$AHEAD" -gt 0 ] && echo "  NOTE  $AHEAD commit(s) not yet pushed — 'git push origin HEAD'."

    # OTHER BRANCHES WITH WORK THIS ONE LACKS. Claude Code gives every session its own branch,
    # so a cut-off session's work can sit on a branch nobody opens (Portfolio: 565 commits
    # stranded for a whole migration). Name each one and the command that brings it in.
    for RB in $(git for-each-ref --format='%(refname:short)' refs/remotes/origin 2>/dev/null | grep -v -e '^origin$' -e '^origin/HEAD$' -e "^origin/${CURBR}$" | head -12); do
      N="$(git rev-list --count "HEAD..$RB" 2>/dev/null || echo 0)"
      [ "$N" -gt 0 ] || continue
      # Dependabot branches and orphan histories (no common ancestor with this branch: the repo was re-rooted on 2026-11-02) are not stranded
      # session work, and merging them would drag in an unrelated root commit. Say so once, quietly, instead of a false alarm every session.
      case "$RB" in origin/dependabot/*) continue ;; esac
      if ! git merge-base HEAD "$RB" >/dev/null 2>&1; then echo "  note  ${RB#origin/} shares no history with this branch (orphan): ignored."; continue; fi
      echo "  !!    ${RB#origin/} has $N commit(s) this branch lacks (last: $(git log -1 --format='%s' "$RB" | cut -c1-70); newest commit $(git log -1 --format=%cr "$RB"))."
      echo "        Look first:  git log --oneline HEAD..$RB | head   — then:  git merge $RB   (tell Tj if it contradicts this branch)"
      SIBLING=1
    done
    # Another session / another Claude account? Its newest commit is its heartbeat (HANDOFF.md 0d "TAKEOVER").
    [ -n "${SIBLING:-}" ] && echo "  NOTE  Another session's work is ahead of you: newest commit under ~20 min old = it MAY still be running (do not touch main, releases or its CI runs; ask Tj); older with no CI run of its in flight = cut off: TAKE OVER, steps in HANDOFF.md 0d."

    LASTAUTO="$(git log -1 --format=%cr --grep='^auto-checkpoint:' 2>/dev/null || true)"
    if [ -n "$LASTAUTO" ]; then
      echo "  OK    auto-checkpointing is live ($(git log --oneline --grep='^auto-checkpoint:' | wc -l | tr -d ' ') so far, latest $LASTAUTO)."
    else
      echo "  NOTE  no auto-checkpoint commits yet. If edits go by and none appear over a few tool calls,"
      echo "        the hooks are not firing: run 'bash tools/install-hooks.sh', say so, and use 'bash tools/ckpt.sh' by hand."
    fi

    # A release GitHub built that nobody recorded would let the next build reuse a versionCode
    # already on the phone (Android then refuses the install).
    VN="$(grep -m1 -oE 'versionName *= *"[^"]+"' app/build.gradle.kts 2>/dev/null | grep -oE '"[^"]+"' | tr -d '"' || true)"
    if [ -n "$VN" ] && git rev-parse -q --verify "refs/tags/v$VN" >/dev/null 2>&1 && ! grep -q "^| v${VN} |" BUILDLOG.md 2>/dev/null; then
      echo "  !!    v$VN WAS RELEASED BUT NEVER RECORDED — run:  bash tools/record-release.sh v$VN \"what it changed\""
    fi

    # Cut off mid-change? Commits after the newest deliberate checkpoint = an unfinished step.
    LASTCKPT="$(git log -1 --format=%H --extended-regexp --grep='^(ckpt [0-9]+:|ship v)' 2>/dev/null || true)"
    if [ -n "$LASTCKPT" ]; then
      SINCE="$(git rev-list --count "$LASTCKPT"..HEAD 2>/dev/null || echo 0)"
      if [ "$SINCE" -gt 0 ]; then
        echo
        echo "  !!    THE LAST SESSION WAS INTERRUPTED MID-CHANGE: $SINCE automatic checkpoint(s) after the last"
        echo "        deliberate one. CHECKPOINT.md below describes the state BEFORE them. Read the change first:"
        echo "          git diff $(git rev-parse --short "$LASTCKPT")..HEAD"
        git diff --stat "$LASTCKPT"..HEAD 2>/dev/null | tail -12 | sed 's/^/          /'
        echo "        Finish it, then checkpoint with tools/ckpt.sh before starting anything new."
      fi
    fi
    if [ -n "$DIRTY" ]; then
      echo
      echo "  !!    UNCOMMITTED WORK IS PRESENT (not even autosave got to it). 'git diff' is what was in flight:"
      printf '%s\n' "$DIRTY" | head -15 | sed 's/^/          /'
    fi
    echo
  fi

  # can any PreCompact hook veto a compaction right now? (one line; a VETO line is the first thing to fix)
  bash tools/compact_guard.sh 2>/dev/null | sed "s/^/  /" | cut -c1-210
  echo "-- toolchain --"
  command -v java >/dev/null 2>&1 && echo "  OK    java" || echo "  WARN  no java — gradlew cannot run"
  bash tools/ensure-build-env.sh --check --quiet >/dev/null 2>&1 \
    && echo "  OK    android sdk ready" \
    || echo "  note  no android sdk yet — tools/gradle.sh installs it on the first build (~5 min, automatic)"
  bash tools/checkkeystore.sh 2>&1 | head -2 | sed 's/^  note /  note /'
  echo

  if [ -f CHECKPOINT.md ]; then
    echo "######## CHECKPOINT.md — where the last session stopped. START HERE. ########"
    cat CHECKPOINT.md
    echo
  fi
  if [ -f TASKS.md ]; then
    echo "######## TASKS.md — the 18 NEWEST open items (line numbers; older ones: grep -n '\- \[ \]' TASKS.md | head -40) ########"
    grep -n '^\s*- \[ \]' TASKS.md | cut -c1-130 | tail -18     # newest last: new requests are appended, the old head-30 hid every request of the last days (2026-11-06) and overflowed the 10,000-char hook cap
    echo "  ($(grep -c '^\s*- \[x\]' TASKS.md) done — not shown)"
    echo
  fi
  if [ -f INBOX.md ]; then
    echo "######## INBOX.md tail — Tj's latest raw messages; anything TASKS.md lacks is an unwritten request ########"
    tail -c 700 INBOX.md
    echo
  fi
  if [ -f OUTBOX.md ]; then   # 2026-11-06 (Tj: "why are my prompts and Claude findings not being saved?"): what Claude LAST told Tj, saved by tools/capture_outbox.sh on every Stop
    echo "######## OUTBOX.md tail — what Claude last told Tj (answers, findings, decisions); the whole text is in the file ########"
    tail -c 550 OUTBOX.md | grep -v '^<!-- sha:'
    echo
  fi
  echo "######## STOCK LOOK REFERENCE (Tj: remember at all times): research/reference/stock/INDEX.md — stock-home.png, stock-drawer.png; 5 cols, 8 rows, NO dock, scrim 50% white ########"
  echo "######## RULES (full text: CLAUDE.md, BRIEF.md; NEW to this project with no context: HANDOFF.md) ########"
  cat <<'SHORT'
- Checkpoint after EVERY step:  bash tools/ckpt.sh "did" "next"   (no gate; "next" is what no diff can recover).
- New request from Tj -> TASKS.md as [ ] boxes in his words FIRST, ckpt, then code.
- GitHub Actions builds every APK (android.yml); ship at versions with  bash ship.sh "note"  then trigger the
  workflow with mcp__github__actions_run_trigger, then  bash tools/record-release.sh.
- Never commit a key, the keystore (app/sideload.jks) or local.properties. Never regenerate the keystore.
- Build traps (JAVA_TOOL_OPTIONS, one Gradle at a time, Maven 429): BRIEF.md.
SHORT
)"

if [ "$TEXT_MODE" -eq 1 ]; then
  printf '%s\n' "$BRIEF"
elif command -v python3 >/dev/null 2>&1; then
  printf '%s' "$BRIEF" | python3 -I -c '
import json, sys
print(json.dumps({"hookSpecificOutput": {"hookEventName": "SessionStart", "additionalContext": sys.stdin.read()}}))
' 2>/dev/null || printf '%s\n' "$BRIEF"
else
  printf '%s\n' "$BRIEF"
fi
exit 0
