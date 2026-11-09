# Why sessions died with "Prompt is too long" (found 2026-11-09, TASKS BM1)

Tj, 2026-11-09 00:17 UTC: "Continue this session and try to find a way to stop this problem" (screenshot: the v0.8.12 gate session ending in
"Prompt is too long"). This file is the evidence. The short version is in `TASKS.md` BM1 and `CLAUDE.md`.

## 1. What happens (one paragraph)

The context window of these sessions is **1,000,000 tokens**; auto-compact is set to fire at **901,600** (92 % of an effective 980,000). A
plugin on Tj's claude.ai account, `session-handoff`, is synced into every cloud session and ships a **PreCompact hook that VETOES compaction**
unless a `HANDOFF.md` / `HANDOFF.<session>.md` in the project root was modified in the last **600 s**. Our `HANDOFF.md` is written only at
releases, so the veto fired at every auto-compaction. Claude Code (docs: "If compaction was triggered proactively before the context limit,
Claude Code skips it and the conversation continues uncompacted") tried three times, stopped, the context kept growing to the **977K blocking
limit**, and the CLI printed its own synthetic message "Prompt is too long". Nothing was wrong with the work, the tools or the size of any one
result: it is a time bomb that fires at the first auto-compaction of every long session.

## 2. Evidence

| Session (id) | used tokens at death | list-price cost | cache-read tokens |
| :- | -: | -: | -: |
| `session_01UWi8ZxEmqCcL83UHEaqj9K` (the screenshot; v0.8.12 gate) | 978,004 | $32.66 | 183 M |
| `session_01UWi8ZzQcUmy1dHAGkxFQdX` (home screen dimming patch) | 975,725 | $32.33 | 178 M |
| `session_01UWi8ZvrV7FDzZMv6rWApDX` (Quickstep animation toggle) | 975,990 | $30.70 | 185 M |
| `session_01UWi8Zxcazcaa4QPhF7UPYs` (home patch review) | 969,296 | $33.33 | 177 M |

All four: `autocompact_state` = enabled, enforced, threshold 901,600; `status_detail` = "Prompt is too long"; none ever compacted (a successful
compaction would have reset the context). Tj's own BH2 ("Compact this session when possible because context window is over 90 %") hit the same wall.

Timeline of the screenshot session (events from the session record, UTC): at 22:44:56 the failing turn starts with a prompt of **961,018 tokens**
(959,358 cache read + 1,658 cache write + 2); 22:44:58.284 `status: compacting` ... 22:44:58.353 status cleared, `compact_output_chars: 0`
(69 ms, no model call); 22:45:10.778 the same (0 ms); 22:45:34.848 the same (79 ms); 22:45:34.993 `result`: `is_error: true`,
`terminal_reason: blocking_limit`, `result: "Prompt is too long"`. The error message event is `model: <synthetic>`, `is_api_error_message: true`: the
CLI refused locally, the API never saw an over-long request. 69-79 ms is exactly the time a tiny command hook needs. After a worker restart
(00:16:36) the same: one `compacting` attempt, 84 ms, "Prompt is too long".

The hook (plugin `session-handoff` v0.1.0, `hooks/precompact-handoff.sh`, registered with `"matcher": "*"`): looks for `HANDOFF.md` and
`HANDOFF.<session>.md` in `${CLAUDE_PROJECT_DIR:-$PWD}`, takes the freshest mtime, and if older than `${HANDOFF_STALE_SECONDS:-600}` (or
missing) prints `{"decision":"block","reason":"... Run the /handoff slash command ..."}`. The plugin manifest says so itself: "Ships a PreCompact
hook that blocks context summarization until a fresh handoff exists." Reproduced in this container 2026-11-09 with the real script:
`HANDOFF.md` 811 s old -> block JSON; `HANDOFF_STALE_SECONDS=999999999` -> no output; after `touch HANDOFF.md` -> no output; run from the parent
directory (no HANDOFF.md there) -> block. A second plugin (`context-handoff`) has a `type: prompt` PreCompact hook that can answer
`{"ok":false}`: per the docs `prompt`/`agent` hooks are NOT run for PreCompact (only command / http / mcp_tool), so it is inert.

Why it was invisible: the veto is silent for an AUTOMATIC compaction (the reason is shown only for a manual /compact), the model is never told,
and the CLI's retry stops after three tries.

## 3. Facts measured or confirmed on 2026-11-09 (so nobody re-derives them)

- Docs (code.claude.com): exit code 2 or `"decision":"block"` blocks compaction; `prompt` / `agent` hooks do not run for PreCompact;
  `CLAUDE_AUTOCOMPACT_PCT_OVERRIDE` (1-100) can only LOWER the threshold; settings `env` values are applied to a running session when the file is
  saved; settings hooks are reloaded when the file changes; a matcher `"*"` / empty / omitted matches every tool, `Edit|Write|Bash` matches ONLY those
  three (MCP tools, Agent, Read, Grep are skipped); PostToolUse can return `additionalContext` and `updatedToolOutput`; caps: `MAX_MCP_OUTPUT_TOKENS`
  25,000, `BASH_MAX_OUTPUT_LENGTH` 30,000 chars, `CLAUDE_CODE_FILE_READ_MAX_OUTPUT_TOKENS` 25,000.
- Measured here: after writing `env` into the repo's `.claude/settings.json`, a NEW Bash call saw the new values within ~3 s, and a HOOK process saw them
  too (`HANDOFF_STALE_SECONDS=315360000`, `CLAUDE_AUTOCOMPACT_PCT_OVERRIDE=40`, overriding the harness's 92). A hook process has `CLAUDE_PROJECT_DIR` =
  the repo root and `CLAUDE_PID` = the Claude process. The harness env var was `CLAUDE_AUTOCOMPACT_PCT_OVERRIDE=92`.
- `get_session` shows `autocompact_state.threshold` and `context_usage.used_tokens` for ANY session id: the cheapest way to see how full a session is.
  `list_events` with `kinds ["result","system"]` shows the compaction attempts (`status: compacting`) and the final `result` (`terminal_reason`).
- The compaction itself was never the slow or failing part; at 901K a successful compaction would also have cost a 900K-token summarisation call.

## 4. The fix (layers; any one stops the veto, the others catch what it misses)

1. `tools/keepalive.sh` keeps `HANDOFF.md`'s mtime fresh (daemon started by `resume.sh`; also `--once` from autosave / capture_inbox / posttool).
2. `HANDOFF_STALE_SECONDS=315360000` in `.claude/settings.json` env (and written by `tools/install-hooks.sh` into the session-root settings).
3. `tools/compact_guard.sh` RUNS the real veto hooks as the CLI would and fails loudly (one line in the `resume.sh` briefing; `tools/test_compact_guard.sh`).
4. Compact EARLY: `CLAUDE_AUTOCOMPACT_PCT_OVERRIDE=40` (about 392K) instead of 92 (901K). Safer (a summary of 390K, not 900K) and cheaper: each turn re-reads
   the whole prompt, the dead sessions averaged ~500K and cost ~$30 each; the average context halves.
5. `tools/ctxwatch.sh` (watchdog): reads the real prompt size from the transcript's last `usage`, warns at 60 / 85 % of the compaction point and
   raises an ALARM when the context is ABOVE it (= auto-compact did not fire, for ANY reason). `tools/test_ctxwatch.sh`.
6. The PostToolUse matcher covers EVERY tool (`tools/posttool.sh`): text written between GitHub MCP calls now reaches `OUTBOX.md` (before, the whole gate
   triage of the dead session died with it).

What only Tj can do: disable the `session-handoff` plugin in his claude.ai plugin list. It vetoes compaction in his OTHER projects too (novig, Mirage ...)
and a repo cannot change an account plugin.

## 5. Status (updated as the work lands; see TASKS BM1b-BM1g)

See `CHECKPOINT.md` for the live state. Open after the first write of this file: compact_guard, ctxwatch, posttool, install-hooks env merge, resume.sh
integration, the live compaction test (this very session crossed 306K tokens at 00:35 UTC with the 40 % point at about 392K), tests, CLAUDE.md note.
