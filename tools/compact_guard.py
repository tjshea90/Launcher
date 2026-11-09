#!/usr/bin/env python3
"""compact_guard.py - would ANY PreCompact hook veto a compaction right now? (TASKS BM1c, 2026-11-09)

WHY: an account plugin (`session-handoff`) ships a PreCompact command hook that prints {"decision":"block"} unless a
HANDOFF.md was modified in the last 600 s. Claude Code skips a vetoed AUTOMATIC compaction silently, the context grew to the
977K blocking limit and four sessions died with "Prompt is too long" (research/COMPACTION_BLOCKED_2026-11-09.md). Nothing
checked for it. This does: it collects every PreCompact command hook (enabled plugins + settings files), statically scans the
script for veto patterns, and for each suspect RUNS it the way the CLI would (cwd = repo root, CLAUDE_PROJECT_DIR = repo root,
settings env applied, synthetic hook JSON on stdin, 10 s timeout) and reads the answer. Only suspects are run; our own hooks
(this repo's tools/) are scanned but never run (toobig.sh commits and pushes).

Exit 0 = nothing vetoes now, 1 = at least one hook vetoes. Prints one line (or nothing with --quiet when OK), plus a fix hint.
Env overrides (tests): COMPACT_GUARD_REPO, COMPACT_GUARD_PLUGINS.  HOME decides which user settings are read.
"""
import glob
import json
import os
import re
import shlex
import subprocess
import sys

REPO = os.path.abspath(os.environ.get("COMPACT_GUARD_REPO") or os.getcwd())
PLUGINS = os.environ.get("COMPACT_GUARD_PLUGINS") or os.path.expanduser("~/.claude/plugins")
VETO = [re.compile(p) for p in (r'"decision"\s*:\s*"block"', r'\\"decision\\"\s*:\s*\\"block\\"', r"\bexit\s+2\b",
                                r'"continue"\s*:\s*false', r'\\"continue\\"\s*:\s*false')]


def settings_low_to_high():
    cands = [os.path.expanduser("~/.claude/settings.json"),
             os.path.join(os.path.dirname(REPO), ".claude", "settings.json"),
             os.path.join(REPO, ".claude", "settings.json"),
             os.path.join(REPO, ".claude", "settings.local.json")]
    return [p for p in dict.fromkeys(cands) if os.path.isfile(p)]


def load_json(p):
    try:
        with open(p) as f:
            d = json.load(f)
        return d if isinstance(d, dict) else {}
    except (OSError, ValueError):
        return {}


def settings_env():
    env = {}
    for p in settings_low_to_high():
        e = load_json(p).get("env")
        if isinstance(e, dict):
            env.update({str(k): str(v) for k, v in e.items()})
    return env


def precompact_entries(data):
    h = data.get("hooks", data)
    out = []
    if isinstance(h, dict):
        for blk in h.get("PreCompact") or []:
            if isinstance(blk, dict):
                out.extend(x for x in blk.get("hooks", []) if isinstance(x, dict))
    return out


def collect():
    found = []
    for p in settings_low_to_high():
        for hh in precompact_entries(load_json(p)):
            found.append((p, os.path.dirname(os.path.dirname(p)), hh))
    for p in glob.glob(os.path.join(PLUGINS, "**", "hooks.json"), recursive=True):
        root = os.path.dirname(os.path.dirname(p)) if os.path.basename(os.path.dirname(p)) == "hooks" else os.path.dirname(p)
        for hh in precompact_entries(load_json(p)):
            found.append((p, root, hh))
    return found


def script_of(cmd):
    try:
        toks = shlex.split(cmd)
    except ValueError:
        return ""
    for t in toks:
        t = os.path.expanduser(t)
        if "/" in t and os.path.isfile(t):
            return t
    return ""


def name_of(script, cmd):
    if script:
        return "%s/%s" % (os.path.basename(os.path.dirname(os.path.dirname(script))) or "", os.path.basename(script))
    return cmd[:60]


def probe(cmd, root, extra_env):
    env = dict(os.environ)
    env.update(extra_env)
    env["CLAUDE_PLUGIN_ROOT"] = root
    env["CLAUDE_PROJECT_DIR"] = REPO
    hook_in = json.dumps({"session_id": "compact-guard", "transcript_path": "", "cwd": REPO,
                          "hook_event_name": "PreCompact", "trigger": "auto", "custom_instructions": ""})
    try:
        r = subprocess.run(cmd, shell=True, cwd=REPO, input=hook_in, capture_output=True, text=True, timeout=10, env=env)
    except subprocess.TimeoutExpired:
        return False, "timed out after 10 s (a slow hook is not a veto)"
    except OSError as e:
        return False, "could not run: %s" % e
    veto, reason = r.returncode == 2, (r.stderr or "").strip()[:200]
    out = (r.stdout or "").strip()
    if out:
        try:
            j = json.JSONDecoder().raw_decode(out[out.find("{"):])[0]
            if isinstance(j, dict) and (j.get("decision") == "block" or j.get("continue") is False):
                veto, reason = True, str(j.get("reason") or j.get("stopReason") or "blocked")
        except ValueError:
            pass
    return veto, reason


def main():
    quiet = "--quiet" in sys.argv
    env_extra = settings_env()
    seen, checked, suspects, vetoes = set(), 0, [], []
    for src, root, hh in collect():
        if hh.get("type", "command") != "command":
            continue
        cmd = (hh.get("command") or "").replace("${CLAUDE_PLUGIN_ROOT}", root).replace("$CLAUDE_PLUGIN_ROOT", root)
        cmd = cmd.replace("${CLAUDE_PROJECT_DIR}", REPO)
        if not cmd or (cmd, root) in seen:
            continue
        seen.add((cmd, root))
        checked += 1
        script = script_of(cmd)
        text = cmd
        if script:
            try:
                with open(script, errors="replace") as f:
                    text += "\n" + f.read(300000)
            except OSError:
                pass
        ours = "launcher-checkpoint-hooks" in cmd or (script and script.startswith(os.path.join(REPO, "tools") + os.sep)) \
            or "/tools/toobig.sh" in cmd
        if ours or not any(r.search(text) for r in VETO):
            continue
        suspects.append(name_of(script, cmd))
        v, why = probe(cmd, root, env_extra)
        if v:
            vetoes.append((name_of(script, cmd), why))
    if vetoes:
        n, why = vetoes[0]
        print("compaction guard: VETO - %s would block a compaction right now: %s | Fix: bash tools/keepalive.sh --start "
              "(and check HANDOFF_STALE_SECONDS in .claude/settings.json env), or disable that plugin." % (n, why[:160]))
        sys.exit(1)
    if not quiet:
        print("compaction guard: OK - %d PreCompact command hook(s) checked, %d could veto (%s), none does now."
              % (checked, len(suspects), ", ".join(suspects) or "none"))
    sys.exit(0)


if __name__ == "__main__":
    main()
