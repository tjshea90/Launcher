#!/usr/bin/env bash
# gate.sh - which emulator sections does THIS change need, and how to split them into short parallel runs?
# (Tj, 2026-11-06: "Only keep reasonable tests from now on and make them as fast and efficient as possible".)
#
#   bash tools/gate.sh [<base-rev>] [--full] [--groups N] [--list]
#
#   <base-rev>   what the change is measured against (default: origin/main; for a release: the previous release tag or `main`)
#   --full       every section of the gate (core + all opt): a milestone, or lock / unlock / power-menu code changed a lot
#   --groups N   split into at most N parallel runs (default 3: about 10 minutes each; use 2 if Actions minutes are scarce)
#   --list       print the table (tools/gate-map.txt) and exit
#   --check      verify that the table and tools/emulator_scenario.sh agree (the handoff self-test runs it)
#
# It only READS git and tools/gate-map.txt; it dispatches nothing (dispatch with the GitHub tool: emulator.yml, ref = the branch, inputs {"sections": "<group>"}).
# Tier 0 (every release, no emulator): unit tests + lint + the handoff self-test: `bash tools/gradle.sh testDebugUnitTest :app:lintDebug`, `bash tools/test_handoff.sh` (android.yml re-runs them).
set -uo pipefail
D="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"; cd "$D" || exit 1
exec python3 -I - "$D" "$@" <<'PY'
import re, subprocess, sys

root = sys.argv[1]
args = sys.argv[2:]
full = "--full" in args
listing = "--list" in args
groups = 3
base = None
i = 0
while i < len(args):
    a = args[i]
    if a == "--groups" and i + 1 < len(args):
        groups = max(1, int(args[i + 1])); i += 2; continue
    if not a.startswith("--") and base is None:
        base = a
    i += 1
base = base or "origin/main"

def git(*a):
    return subprocess.run(["git", "-C", root] + list(a), capture_output=True, text=True).stdout

# ---- the table
table = []
for line in open(root + "/tools/gate-map.txt", encoding="utf-8"):
    line = line.rstrip("\n")
    if not line.strip() or line.lstrip().startswith("#"):
        continue
    f = [x.strip() for x in line.split("::")]
    if len(f) < 6:
        print("WARN bad line in gate-map.txt:", line[:80]); continue
    table.append({"name": f[0], "wait": int(f[1]), "tier": f[2], "path": f[3], "content": f[4], "why": f[5]})

if listing:
    print(f'{"section":7} {"waits":>6} {"tier":5} protects')
    for t in table:
        print(f'{t["name"]:7} {t["wait"]:5d}s {t["tier"]:5} {t["why"]}')
    sys.exit(0)

# scenario order (sections run in this order inside one run)
order = re.findall(r'^if want ([a-z0-9]+); then', open(root + "/tools/emulator_scenario.sh", encoding="utf-8").read(), re.M)
known = set(order)
if "--check" in args:   # used by tools/test_handoff.sh: the table and the scenario must agree
    missing = [t["name"] for t in table if t["name"] not in known]
    dup = sorted({t["name"] for t in table if [x["name"] for x in table].count(t["name"]) > 1})
    if missing or dup:
        print("gate-map.txt problem: missing in the scenario:", missing, "duplicated:", dup); sys.exit(1)
    print("gate-map.txt agrees with tools/emulator_scenario.sh (%d sections)" % len(table)); sys.exit(0)
for t in table:
    if t["name"] not in known:
        print(f'WARN section {t["name"]} is in gate-map.txt but not in tools/emulator_scenario.sh')

# ---- the change
changed = [p for p in git("diff", "--name-only", base).split("\n") if p]
lines = [l[1:] for l in git("diff", "-U0", base, "--", "app", "launcher3", "phoneguide").split("\n")
         if l and l[0] in "+-" and not l.startswith("+++") and not l.startswith("---")]
product = [p for p in changed if p.startswith(("app/", "launcher3/", "phoneguide/"))]
harness = [p for p in changed if p in ("tools/emulator_scenario.sh", ".github/workflows/emulator.yml", "tools/gate-map.txt")]

print(f"change vs {base}: {len(changed)} file(s), {len(product)} product file(s), {len(lines)} changed product line(s)")
if not product and not harness and not full:
    print("TIER 0 ONLY: nothing but docs / tools changed: unit tests + lint + handoff self-test, NO emulator run.")
    sys.exit(0)

chosen, reasons = [], {}
for t in table:
    why = None
    if full:
        why = "full gate"
    elif t["tier"] == "core":
        why = "core (lock-safety)"
    else:
        if t["path"] != "-":
            rx = re.compile(t["path"])
            hit = next((p for p in changed if rx.search(p)), None)
            if hit: why = "path " + hit.split("/")[-1]
        if not why and t["content"] != "-":
            rx = re.compile(t["content"])
            hit = next((l for l in lines if rx.search(l)), None)
            if hit: why = "changed line mentions " + rx.search(hit).group(0)
    if why:
        chosen.append(t); reasons[t["name"]] = why

rank = {n: k for k, n in enumerate(order)}
# sections that read the output files of ANOTHER section (v0.7.6 gate: `b4` greps y2-log.txt, so b4 in a run without y2 fails for nothing): the needed one is added and both stay in ONE run
# `legacy` needs `base` in the SAME run ahead of it (RS1: `reset2` and others only pass after `base`; v0.9.0 gate 2026-11-09: legacy in a run without base failed reset2 on the head AND on the v0.8.13 tag)
BIND = {"b4": "y2", "legacy": "base"}
for need_by, need in BIND.items():
    if need_by in {t["name"] for t in chosen} and need not in {t["name"] for t in chosen}:
        for t in table:
            if t["name"] == need:
                chosen.append(t); reasons[need] = "needed by " + need_by
chosen.sort(key=lambda t: rank.get(t["name"], 999))
print("sections:", ",".join(t["name"] for t in chosen))
for t in chosen:
    print(f'  {t["name"]:7} {t["wait"]:4d}s  {reasons[t["name"]]}')

# ---- balanced split (longest first into the lightest group), each group in scenario order
units = []   # a unit = sections that must share a run
by_name = {t["name"]: t for t in chosen}
used = set()
for t in chosen:
    if t["name"] in used: continue
    unit = [t]; used.add(t["name"])
    for a, b in BIND.items():
        if a in by_name and b in by_name and t["name"] in (a, b):
            for other in (by_name[a], by_name[b]):
                if other["name"] not in used: unit.append(other); used.add(other["name"])
    units.append(unit)
# SOLO sections always get a run of their own (2026-11-09, gate of v0.8.13: `pt` after x1,v failed with the SAME four checks as in the v0.8.4 gate, BC2 closetap + three PB, because the emulator's stock scrim
# was 0.75 when phase C measured it and 0.80 again in phase D; `pt` alone passes; `rg` is order-sensitive too, TASKS AT2). Chaining them cost a rerun every time.
SOLO = {"pt", "rg"}
solo_units = [u for u in units if all(t["name"] in SOLO for t in u)]
units = [u for u in units if u not in solo_units]
def balance(n):   # longest unit first into the lightest of n bins
    bins = [{"w": 0, "s": []} for _ in range(n)]
    for unit in sorted(units, key=lambda u: -sum(t["wait"] for t in u)):
        lo = min(bins, key=lambda x: x["w"]); lo["s"].extend(unit); lo["w"] += sum(t["wait"] for t in unit)
    return bins
nb = min(max(2, groups - len(solo_units)), len(units)) if units else 0
g = balance(nb)
while units and nb < len(units) and max(3 + 2.5 * x["w"] / 60.0 for x in g) > 40:   # a run of more than ~40 min would meet the 45 min step limit: more bins
    nb += 1; g = balance(nb)
for unit in solo_units:
    g.append({"w": sum(t["wait"] for t in unit), "s": list(unit)})
g = g or [{"w": 0, "s": []}]
print(f"\nSPLIT into {len(g)} parallel run(s) (wall time about 3 min boot + 2.5 x waits):")
tot = 0
for k, x in enumerate(sorted(g, key=lambda x: -x["w"]), 1):
    x["s"].sort(key=lambda t: rank.get(t["name"], 999))
    est = 3 + 2.5 * x["w"] / 60.0
    tot += est
    print(f'  run {k}: ~{est:.0f} min   {{"sections": "{",".join(t["name"] for t in x["s"])}"}}')
print(f"runner minutes about {tot:.0f}; wall time about the longest run.")
print("Dispatch: emulator.yml, ref = the branch, one run per line above. Read each: list_workflow_jobs -> get_job_logs (tail 60).")
PY
