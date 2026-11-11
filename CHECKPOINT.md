# CHECKPOINT 1159 — read me first, then TASKS.md

**Written:** 2026-11-11T16:26:21Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-3e66a8c0-ruc8aa` · **builds on:** `4d5ec186` (this checkpoint is the commit after it)

## Just done
BV1d: eleven module builds (AGP 8.13.2, debug APK 8.3 MB) after ONE port fix (R.attr to androidx.appcompat.R.attr in PopupMenuHelper). BV1f: custom drawer is a WHITELIST (new installs hidden until Tj allows), so AppVisibility.AUTO_ALLOWED_PACKAGES + HiddenApps.autoAllowIfDue (once, level-triggered) + 4 JVM tests written, NOT yet run

## Do this next
1) run bash tools/gradle.sh :app:testDebugUnitTest --tests com.tj.launcher.AppVisibilityTest; 2) CUT THE ART per research/ELEVEN_PORT_2026-11-11.md section 4 in eleven/ (slim ImageWorker + ImageFetcher, delete ImageCache/BitmapWorkerTask/Blur/Simple/PlaylistWorkerTask/disklrucache/LruCache?/PlaylistArtworkStore/PhotoSelectionDialog, patch callers: HomeActivity photo result, SettingsActivity delete_cache pref + USE_BLUR clearCaches, PopupMenuHelper FETCH_ALBUM_ART, Album/ArtistPopupMenuHelper, DeleteDialog, MusicUtils.removeFromCache, ElevenUtils.getImageFetcher, MusicPlaybackService setImageCache, MusicDB, ElevenApplication), then CHANGES.md + guard test. Baseline commit db65f0240c22 = pristine upstream. Heartbeat trig_01GVcBRvTAzqf97UhMTJszab armed (rule 4: continue from here); never call update_trigger or delete_trigger

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  db65f024 ckpt 1158: BV1: eleven/ vendored PRISTINE from upstream ca22308 (426 files, only paths 
  11bc1a01 ckpt 1157: BV1a/b audit written: research/ELEVEN_PORT_2026-11-11.md (pin ca22308cfe54, 
  0b81f71b ckpt 1156: BU1c ticked; Tj 16:10Z asked for Eleven (org.lineageos.eleven, lineage-17.1)
  917bb443 ckpt 1155: Merged main (other session's 14:35Z OUTBOX heartbeat notes, clean) into the 
  2e29f290 ckpt 1154: BU1a/b done: Eleven (org.lineageos.eleven) branch lineage-17.1 verified, def
  21fda685 ckpt 1153: BU1 written in TASKS (Tj 15:48Z: find the stock lineage music app source for
  4425299c ckpt 1152: 14:18Z heartbeat fired (rules 1-5 done): hooks ok; newest main commit 697aa1
  f6a84b4d ckpt 1151: Merged main into the branch (d8c02111): the account-1 session sent the BT1 a
  b9051ceb ckpt 1150: 12:18Z heartbeat fired again (rules 1-4 ok, no other session). update_trigge
  4ea7eb30 ckpt 1150: 11:18Z: other session inactive since its ckpt 1149 (10:15Z, over 60 min, no 
```

(8 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
