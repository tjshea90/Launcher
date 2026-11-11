# Eleven art cut: read-only audit (2026-11-11, TASKS BV1) + first emulator picture round

## Audit (read-only subagent, neutral brief, diff `db65f0240c22..HEAD -- eleven`)
Verdict: no HIGH, no crash-class defect, no remaining way to load a picture (every bitmap/drawable comes from `ImageWorker.loadDefaultImage` and `ImageFetcher.getArtwork`, i.e. `LetterTileDrawable`; 1115 XML/Java references resolved, none dangling). Findings and decisions:
1. MEDIUM, owner decision (NOT changed): the "Blur background" switch still exists and blurs nothing (with no cover there is nothing to blur; same as upstream with art-less music). Ask Tj in the answer; deleting it = remove the `SwitchPreference` at `res/xml/settings.xml` (default is off).
2. LOW, tell Tj: the PLACEHOLDER tile still goes to SystemUI (notification large icon, MediaSession `METADATA_KEY_ALBUM_ART`); Android 10 may show it as a lock-screen / shade backdrop while music plays. Upstream does the same for art-less tracks. Removing it = drop `.putBitmap` (MusicPlaybackService ~1568) and `.setLargeIcon` (~1647); the guard still passes. Not checked on the MediaTek build.
3. LOW, TO DO: dead 2-argument constructor `BitmapWithColors(Bitmap,int)` leads to `Palette.from(mBitmap)`: delete it (no caller).
4. LOW, TO DO: guard blind spots: add tokens `setImageURI|setImageIcon|Icon.createWith(Bitmap|ContentUri|FilePath|AdaptiveBitmap)|setImageViewUri|setImageViewIcon|createFromPath|createFromStream|BitmapRegionDecoder|BigPictureStyle|bigPicture|Glide|Picasso|METADATA_KEY_(ALBUM_)?ART_URI|METADATA_KEY_DISPLAY_ICON_URI` (each with an EXAMPLES entry), scan `.kt` too, check dependencies.
5. LOW, TO DO: `cache/LruCache.java` is unused: delete, guard allows only ImageFetcher + ImageWorker.
6. LOW: stale `DeleteDialog` javadoc / `cachekey` argument (no effect). 7. LOW: unused imports/fields (no effect). 8. LOW: orphan string `settings_storage_category`. (6-8: left as is, they keep the diff against upstream small.)

## First emulator picture round (run 544, id 43063412826, ci-shots)
- Eleven starts on the SONGS tab; the tabs are ARTISTS | ALBUMS | SONGS | PLAYLISTS (4, not 5); PLAYLISTS holds Last added / Recently played / My top tracks.
- The song row shows the placeholder: a GREEN square with a white music note (the "note in a coloured box" Tj described). The bottom bar shows a small note icon + play button.
- **`uiautomator dump` returned nothing useful while Eleven was in front** (the fallback taps of `mutap` ran, BACK left Eleven for the Launcher and later taps opened Files / Contacts / Gallery): MU-8, MU-17, MU-18 failed for that reason, not because of the app. Navigate Eleven by FIXED COORDINATES (600x1024: tabs at y=120, x = 80 / 228 / 372 / 524; first list row y about 200, its overflow button x=584; bottom bar centre (300, 957)) and assert `resumed` still names Eleven after every step.
- MU-2 control (Android serves the cover through content://media/external/audio/albumart/<id>) found NO cover on API 29 (`content read` gave nothing): find another control (see TASKS BV1e).
