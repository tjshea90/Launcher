#!/usr/bin/env bash
# ci_publish_shots.sh - run by .github/workflows/emulator.yml (input shots=true), NEVER by hand and never locally.
#
# WHY (TASKS BN1, 2026-11-09): Claude cannot download workflow artifacts (a hook blocks it), so it could only read the compact RESULTS
# text of an emulator run and never SEE a screen. A "looks identical to the stock Settings" job cannot be judged from numbers alone
# (a missing icon, a wrong glyph, an off-by-one gap). This script force-pushes the run's PNG screenshots to the orphan branch
# `ci-shots` (one commit, replaced by every run). Claude then does `git fetch origin ci-shots`, `git show origin/ci-shots:<file> >
# <scratchpad>/<file>` and looks at the picture with the Read tool.
#
# Safe by construction: the emulator is not Tj's tablet (no personal data on it); only *.png from the output folder are published,
# at most MAX files; the branch shares no history with main (resume.sh already ignores orphan branches); a failure here never fails
# the run (the workflow step is continue-on-error and this script always exits 0).
set -uo pipefail
SRC="${1:-emulator-out}"
MAX="${SHOTS_MAX:-60}"
GLOB="${SHOTS_GLOB:-*.png}"
[ -d "$SRC" ] || { echo "ci_publish_shots: no $SRC folder, nothing to publish"; exit 0; }
[ -n "${GITHUB_TOKEN:-}" ] && [ -n "${GITHUB_REPOSITORY:-}" ] || { echo "ci_publish_shots: no token / repository (not on Actions), nothing published"; exit 0; }

T="$(mktemp -d)"
n=0
# newest first, so a cap keeps the latest pictures
while IFS= read -r f; do
  [ "$n" -ge "$MAX" ] && break
  cp "$f" "$T/$(basename "$f")" && n=$((n+1))
done < <(find "$SRC" -maxdepth 2 -type f -name "$GLOB" -printf '%T@ %p\n' 2>/dev/null | sort -rn | cut -d' ' -f2-)
[ "$n" -gt 0 ] || { echo "ci_publish_shots: no $GLOB files in $SRC"; exit 0; }
printf 'run %s on %s\nsections: %s\nfiles: %s\n' "${GITHUB_RUN_ID:-?}" "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "${SECTIONS:-?}" "$n" > "$T/RUN.txt"

cd "$T" || exit 0
git init -q -b ci-shots . || exit 0
git config user.name "github-actions" && git config user.email "actions@users.noreply.github.com"
git add -A && git commit -q -m "ci screenshots: run ${GITHUB_RUN_ID:-?} sections ${SECTIONS:-?}" || exit 0
git remote add origin "https://x-access-token:${GITHUB_TOKEN}@github.com/${GITHUB_REPOSITORY}.git"
if git push -q -f origin ci-shots 2>/tmp/ci_publish_shots.err; then
  echo "ci_publish_shots: $n screenshot(s) pushed to branch ci-shots (run ${GITHUB_RUN_ID:-?})"
else
  echo "ci_publish_shots: push failed (screenshots stay in the artifact): $(sed 's/x-access-token:[^@]*@/x-access-token:***@/' /tmp/ci_publish_shots.err | tail -2)"
fi
exit 0
