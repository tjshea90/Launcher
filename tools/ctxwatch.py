#!/usr/bin/env python3
"""ctxwatch.py - context-size watchdog (TASKS BM1d, 2026-11-09). Called by tools/ctxwatch.sh from hooks.

WHY: four sessions in a row grew to 969K-978K tokens and died with "Prompt is too long" because an account
plugin silently vetoed every auto-compaction (research/COMPACTION_BLOCKED_2026-11-09.md). The veto is fixed
(keepalive / env / compact_guard), but compaction can fail for another reason tomorrow. This makes that LOUD
at 40 % of the window instead of at 98 %: nothing else in the system measured the context.

HOW: the transcript's last main-thread assistant entry carries the real `usage` of the last model call:
prompt tokens = input_tokens + cache_read_input_tokens + cache_creation_input_tokens. Everything written to the
transcript after that entry (tool results) is added as chars / 3.4. No network, no API, one tail read.

LEVELS (T = the auto-compact point = CLAUDE_AUTOCOMPACT_PCT_OVERRIDE % of 980,000, or of CLAUDE_CODE_AUTO_COMPACT_WINDOW):
  1 info      >= 60 % of T
  2 soon      >= 85 % of T
  3 ALARM     the LAST MODEL CALL already had more than T + 30K tokens: compaction should have run before it and did not
  4 CRITICAL  >= 90 % of the hard stop (977K): every further message will fail
Each level is spoken once, then repeated every 12 hook calls for levels 3 and 4. A drop of the context (a compaction
happened) resets the level, and the compaction is logged (".git/ctxwatch.log"). Output: hook JSON with
hookSpecificOutput.additionalContext (+ systemMessage from level 2 up) on stdout, nothing at all below level 1.

Modes:  (stdin = hook JSON)   default       one hook call
        --report [--transcript PATH]          one human line (used by ckpt / by hand), always exits 0
ALWAYS exits 0 and never blocks anything.
"""
import glob
import json
import os
import sys
import time

HARD = int(os.environ.get("CTXWATCH_HARD", "977000"))        # the CLI's own blocking limit (window 1M - 20K - 3K)
BASE = 980000                                                # effective window the percentage is applied to
CHARS_PER_TOKEN = 3.4
ALARM_SLACK = 30000
REPEAT_EVERY = 12


def threshold():
    try:
        base = int(os.environ.get("CLAUDE_CODE_AUTO_COMPACT_WINDOW", "") or BASE)
    except ValueError:
        base = BASE
    base = max(100000, min(base, BASE))
    try:
        pct = float(os.environ.get("CLAUDE_AUTOCOMPACT_PCT_OVERRIDE", "") or 98.7)
    except ValueError:
        pct = 98.7
    if not 1 <= pct <= 100:
        pct = 98.7
    return int(base * pct / 100)


def find_transcript(hook):
    tp = (hook or {}).get("transcript_path") or ""
    if tp and os.path.isfile(tp):
        return tp
    cands = glob.glob(os.path.expanduser("~/.claude/projects/*/*.jsonl"))
    slug = os.getcwd().replace("/", "-")
    mine = [c for c in cands if os.path.basename(os.path.dirname(c)) == slug] or cands
    return max(mine, key=os.path.getmtime) if mine else ""


def read_tail(path, nbytes):
    size = os.path.getsize(path)
    with open(path, "rb") as f:
        f.seek(max(0, size - nbytes))
        raw = f.read()
    lines = raw.split(b"\n")
    if size > nbytes:
        lines = lines[1:]            # first line is probably cut in half
    return lines


def measure(path):
    """-> dict(prompt=int, est=int, compaction=dict|None) or None. prompt = tokens of the last model call."""
    size = os.path.getsize(path)
    for window in (400000, 3000000, 30000000):
        try:
            lines = read_tail(path, window)
        except OSError:
            return None
        after, found, compaction = 0, None, None
        for raw in reversed(lines):
            if not raw.strip():
                continue
            if found is not None:
                # usage entry already found: older lines matter only for the most recent compaction marker
                if b"compact_boundary" not in raw:
                    continue
            try:
                o = json.loads(raw)
            except ValueError:
                if found is None:
                    after += len(raw)
                continue
            if not isinstance(o, dict):
                if found is None:
                    after += len(raw)
                continue
            if compaction is None and (o.get("subtype") == "compact_boundary" or o.get("type") == "compact_boundary"):
                md = o.get("compactMetadata") or o.get("compact_metadata") or {}
                compaction = {"trigger": md.get("trigger"), "pre": md.get("preTokens") or md.get("pre_tokens"),
                              "at": o.get("timestamp")}
                if found is not None:
                    break
            if found is None and o.get("type") == "assistant" and not o.get("isSidechain"):
                msg = o.get("message") or {}
                u = msg.get("usage")
                if isinstance(u, dict) and msg.get("model") != "<synthetic>":
                    prompt = int(u.get("input_tokens") or 0) + int(u.get("cache_read_input_tokens") or 0) \
                        + int(u.get("cache_creation_input_tokens") or 0)
                    if prompt > 0:
                        found = {"prompt": prompt, "est": prompt + int(u.get("output_tokens") or 0) + int(after / CHARS_PER_TOKEN)}
                        continue
            if found is None:
                after += len(raw)
        if found is not None:
            found["compaction"] = compaction
            return found
        if window >= size:
            break                                   # the whole file was covered and holds no usage entry
    return None


def level_of(m, t):
    if m["est"] >= 0.90 * HARD:
        return 4
    if m["prompt"] > t + ALARM_SLACK:
        return 3
    if m["est"] >= 0.85 * t:
        return 2
    if m["est"] >= 0.60 * t:
        return 1
    return 0


def k(n):
    return "%dK" % round(n / 1000.0)


def message(level, m, t):
    now = "~%s tokens in use" % k(m["est"])
    if level == 1:
        return ("CONTEXT WATCH (info): %s; auto-compact is due at ~%s (hard stop ~%s). Nothing to do yet. Keep big reads "
                "(CI logs, run lists, photos, whole files) out of this chat: tail_lines, grep, or a read-only subagent "
                "with a neutral brief." % (now, k(t), k(HARD)), False)
    if level == 2:
        return ("CONTEXT WATCH: %s; auto-compact is due soon, at ~%s. Run `bash tools/ckpt.sh \"did\" \"next\"` NOW with an exact "
                "'next' (file, function, command, CI run ids) so the compaction summary and the cold-resume briefing agree."
                % (now, k(t)), True)
    if level == 3:
        return ("CONTEXT ALARM: the last model call already had %s tokens, ABOVE the auto-compact point (~%s): auto-compact did NOT "
                "fire, something is blocking it. (1) `bash tools/compact_guard.sh` names any PreCompact hook that vetoes. (2) `bash "
                "tools/ckpt.sh` with an exact next. (3) Tell Tj plainly: 'Auto-compact is not firing, context is %s of ~%s; start a "
                "new session now or send /compact'. At ~%s every message fails with Prompt is too long."
                % (k(m["prompt"]), k(t), k(m["est"]), k(HARD), k(HARD)), True)
    return ("CONTEXT CRITICAL: %s of a ~%s hard stop. Run `bash tools/ckpt.sh` NOW (one step), then STOP all other work and tell Tj: "
            "'Handoff ready: start a new session, it resumes from CHECKPOINT.md.' The next messages will fail."
            % (now, k(HARD)), True)


def brief(inp):
    try:
        s = json.dumps(inp, ensure_ascii=False)
    except (TypeError, ValueError):
        s = str(inp)
    return s[:70]


def top_report(path, n=8):
    """Where did the context go? Growth between consecutive model calls, minus the model's own output, is the size of the
    tool results that arrived in between; attribute it to the tools of the earlier call."""
    cur, order = {}, []
    with open(path, "rb") as f:
        for raw in f:
            try:
                o = json.loads(raw)
            except ValueError:
                continue
            if not isinstance(o, dict) or o.get("type") != "assistant" or o.get("isSidechain"):
                continue
            msg = o.get("message") or {}
            u, mid = msg.get("usage"), msg.get("id")
            if not isinstance(u, dict) or msg.get("model") == "<synthetic>" or not mid:
                continue
            p = int(u.get("input_tokens") or 0) + int(u.get("cache_read_input_tokens") or 0) + int(u.get("cache_creation_input_tokens") or 0)
            if mid not in cur:
                order.append(mid)
                cur[mid] = {"prompt": 0, "out": 0, "tools": []}
            cur[mid]["prompt"], cur[mid]["out"] = p, int(u.get("output_tokens") or 0)
            for b in msg.get("content") or []:
                if isinstance(b, dict) and b.get("type") == "tool_use" and (b.get("name"), brief(b.get("input"))) not in cur[mid]["tools"]:
                    cur[mid]["tools"].append((b.get("name"), brief(b.get("input"))))
    if not order:
        print("no model calls with usage found")
        return
    first, last = cur[order[0]]["prompt"], cur[order[-1]]["prompt"]
    by_tool, jumps, out_total = {}, [], 0
    for a, b in zip(order, order[1:]):
        pa, pb = cur[a], cur[b]
        out_total += pa["out"]
        grow = pb["prompt"] - pa["prompt"] - pa["out"]
        if grow < -1000 or not pa["tools"]:
            continue                                   # a compaction (context fell) or a turn without tools
        for name, inp in pa["tools"]:
            by_tool[name] = by_tool.get(name, 0) + grow // len(pa["tools"])
        jumps.append((grow, pa["tools"][0][0], pa["tools"][0][1], len(pa["tools"])))
    print("calls %d | first prompt %s (= system + tools + CLAUDE.md + briefing) | now %s | model output kept in context ~%s"
          % (len(order), k(first), k(last), k(out_total)))
    print("tool results by tool (tokens): " + ", ".join("%s %s" % (a, k(b)) for a, b in sorted(by_tool.items(), key=lambda x: -x[1])[:n]))
    print("biggest single jumps:")
    for grow, name, inp, cnt in sorted(jumps, reverse=True)[:n]:
        print("  %7s  %s%s %s" % (k(grow), name, ("(+%d more in parallel)" % (cnt - 1)) if cnt > 1 else "", inp))


def logline(text):
    try:
        p = os.path.join(".git", "ctxwatch.log")
        if os.path.isdir(".git"):
            if os.path.exists(p) and os.path.getsize(p) > 200000:
                os.replace(p, p + ".1")
            with open(p, "a") as f:
                f.write("%s %s\n" % (time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()), text))
    except OSError:
        pass


def main():
    argv = sys.argv[1:]
    hook = {}
    if "--report" not in argv and "--top" not in argv and not sys.stdin.isatty():
        try:
            hook = json.loads(sys.stdin.read() or "{}")
        except ValueError:
            hook = {}
    path = ""
    if "--transcript" in argv:
        path = argv[argv.index("--transcript") + 1]
    path = path or find_transcript(hook)
    t = threshold()
    if "--top" in argv:
        if path and os.path.isfile(path):
            top_report(path)
        else:
            print("no transcript found")
        return
    m = measure(path) if path and os.path.isfile(path) else None
    if "--report" in argv:
        if not m:
            print("context: unknown (no transcript with usage found)")
        else:
            c = m["compaction"]
            print("context: ~%s tokens (last call %s); auto-compact at ~%s = %d%%; hard stop ~%s; last compaction: %s"
                  % (k(m["est"]), k(m["prompt"]), k(t), round(100.0 * m["est"] / t),
                     k(HARD), ("%s at %s (was %s)" % (c["trigger"], c["at"], c["pre"])) if c else "none seen in the recent transcript"))
        return
    if not m:
        return
    state_path = os.path.join(".git", "ctxwatch.state")
    st = {"level": 0, "calls": 0, "comp": ""}
    try:
        with open(state_path) as f:
            st.update(json.load(f))
    except (OSError, ValueError):
        pass
    st["calls"] = int(st.get("calls", 0)) + 1
    c = m["compaction"]
    if c and str(c.get("at")) != st.get("comp"):
        st["comp"] = str(c.get("at"))
        logline("COMPACTION SEEN trigger=%s pre=%s at=%s" % (c.get("trigger"), c.get("pre"), c.get("at")))
    level = level_of(m, t)
    speak = False
    if level > st["level"]:
        speak = True
    elif level >= 3 and st["calls"] % REPEAT_EVERY == 0:
        speak = True
    if level != st["level"]:
        logline("level %d -> %d est=%s last_call=%s T=%s" % (st["level"], level, m["est"], m["prompt"], t))
    elif st["calls"] % 25 == 0:
        logline("tick level=%d est=%s last_call=%s T=%s" % (level, m["est"], m["prompt"], t))
    st["level"] = level
    try:
        if os.path.isdir(".git"):
            with open(state_path, "w") as f:
                json.dump(st, f)
    except OSError:
        pass
    if speak and level >= 1:
        text, show_user = message(level, m, t)
        ev = hook.get("hook_event_name") or "PostToolUse"
        out = {"hookSpecificOutput": {"hookEventName": ev, "additionalContext": text}}
        if show_user:
            out["systemMessage"] = text
        sys.stdout.write(json.dumps(out) + "\n")


if __name__ == "__main__":
    try:
        main()
    except Exception:            # a watchdog must never break the session it watches
        pass
    sys.exit(0)
