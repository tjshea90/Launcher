# The auto-resume heartbeat prompt (verbatim; TASKS BO1 / BP2c)

Create it with `mcp__claude-code-remote__create_trigger`: name `Launcher auto-resume heartbeat`, cron `0 */2 * * *`, `initiation` `human_request`, no session id (it binds to the calling session).
Put the returned id into every `ckpt` "next" while it lives. A trigger of ANOTHER account cannot be read or deleted from yours.

```
Launcher auto-resume heartbeat (Tj's standing order, TASKS BO1 / CLAUDE.md "Auto-resume"). Six rules, in this order:
1. Run `bash tools/install-hooks.sh`, then `bash tools/resume.sh`, and read CHECKPOINT.md "Do this next". This trigger's own id is in that text.
2. If another session's commit on origin is under 60 minutes old (a different branch is ahead of yours, or `git log -1 origin/main` is not yours): do NOT work. Disable this trigger with update_trigger enabled=false, ckpt one line saying so, stop.
3. If INBOX.md holds a message from Tj that TASKS.md lacks or that has no answer yet: write it into TASKS.md in his words, ckpt, and handle it first.
4. If you were in the middle of a job (gate runs, a release, a fix): list the emulator.yml / android.yml runs BEFORE dispatching anything, then continue exactly from "Do this next". Checkpoint after every step with `bash tools/ckpt.sh "did" "next"` and keep this trigger's id in "next".
5. If the only open items are waiting on Tj (a question, a tablet report, a grant): say so in one short line and stop; do not invent work.
6. If the job is shipped and recorded and nothing Claude-owned is left: delete this trigger with delete_trigger, write that in the ckpt, stop.
```

History: `trig_01DhC2W8FUPFeqdGFQCt7yWe` (old account, armed 2026-11-09 17:27 UTC, every 2 h at :27); `trig_01NG4acJZ3CVuQZutyT6rsCC` (new account, armed 2026-11-10 01:11 UTC, every 2 h at :11).
