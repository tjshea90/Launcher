# The auto-resume heartbeat prompt (TASKS BO1 / BP2c; rule 2 corrected 2026-11-10, TASKS BP2g)

Create it with `mcp__claude-code-remote__create_trigger`: name `Launcher auto-resume heartbeat`, cron `0 */2 * * *`, `initiation` `human_request`, no session id (it binds to the calling session).
Put the returned id into every `ckpt` "next" while it lives. A trigger of ANOTHER account cannot be read or deleted from yours.

```
Launcher auto-resume heartbeat (Tj's standing order, TASKS BO1 / CLAUDE.md "Auto-resume"). Six rules, in this order:
1. Run `bash tools/install-hooks.sh`, then `bash tools/resume.sh`, and read CHECKPOINT.md "Do this next". This trigger's own id is in that text.
2. Is another session really working? Only REAL work counts: a deliberate `ckpt N:` commit, or a commit that changes anything except INBOX.md / OUTBOX.md / CHECKPOINT.md, made by a different branch or account and under 60 minutes old (look: `git log -6 --format='%h %cr %s' origin/main` and `git show --stat` of the newest one). A commit called `auto-checkpoint:` that only adds a worker-restart notice (or any other automatic line) to INBOX.md / OUTBOX.md is NOT work: a stalled session makes one every few hours, and two heartbeats on two accounts would each see the other's notice, stand down and disable themselves (2026-11-10: the new account sat idle from 01:52 to 04:28 UTC while its restart notices kept `main` looking fresh). If real work is under 60 minutes old: do NOT work in this firing, ckpt one line saying so, stop; do NOT disable this trigger (the next firing looks again; a disabled heartbeat is a session nobody wakes). Otherwise go on with rule 3.
3. If INBOX.md holds a message from Tj that TASKS.md lacks or that has no answer yet: write it into TASKS.md in his words, ckpt, and handle it first.
4. If you were in the middle of a job (gate runs, a release, a fix): list the emulator.yml / android.yml runs BEFORE dispatching anything, then continue exactly from "Do this next". Checkpoint after every step with `bash tools/ckpt.sh "did" "next"` and keep this trigger's id in "next".
5. If the only open items are waiting on Tj (a question, a tablet report, a grant): say so in one short line and stop; do not invent work.
6. If the job is shipped and recorded and nothing Claude-owned is left: delete this trigger with delete_trigger, write that in the ckpt, stop.
```

History: `trig_01DhC2W8FUPFeqdGFQCt7yWe` (old account, armed 2026-11-09 17:27 UTC, every 2 h at :27); `trig_01NG4acJZ3CVuQZutyT6rsCC` (new account, armed 2026-11-10 01:11 UTC, every 2 h at :11).

Update 2026-11-10 (corrected 06:40 UTC): `trig_01DhC2W8FUPFeqdGFQCt7yWe` (old account) was NOT deleted: its delete_trigger call waits for Tj's approval in the old account's chat (that session's ckpt 868). `trig_01NG4acJZ3CVuQZutyT6rsCC` (new account) was created with the OLD wording of rule 2; its owner updates it with `update_trigger` (prompt = the block above) or deletes it with `delete_trigger` once the job is shipped.

Update 2026-11-10 06:34 UTC: `trig_01NG4acJZ3CVuQZutyT6rsCC` (new account) DELETED by its owner after v0.9.1 shipped. The old account's one is still armed (see above).
