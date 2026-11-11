# What was changed against upstream (Apache License 2.0, section 4(b))

Upstream: LineageOS Eleven, branch `lineage-17.1`, commit `ca22308cfe54b3c4d7200441a3f951f4831fb035` (see `UPSTREAM.md`).
The baseline commit in this repo ("ckpt 1158", `db65f0240c22`) is that source unchanged, only moved to the Gradle layout. Everything after it is ours: `git diff db65f0240c22..HEAD -- eleven`.
Reason (TASKS.md BV1, Tj 2026-11-11): the music app must look and work as upstream wrote it, but must NEVER load or show album art, anywhere.

## 1. Port to this project's toolchain
- `build.gradle.kts` (new; replaces upstream's `build.gradle`): AGP 8.13.2, `namespace` and `applicationId` `org.lineageos.eleven`, compileSdk 36, minSdk 28 and targetSdk 30 (upstream's), AIDL and `BuildConfig` switched on (AGP 8 does not generate them by default), no `.dev` suffix on the debug build, R8 off (like the launcher), signed with the launcher's key when it is present, upstream's dependency versions unchanged.
- `utils/PopupMenuHelper.java`: `androidx.appcompat.R.attr.actionOverflowMenuStyle` instead of `R.attr...` (this project builds with `android.nonTransitiveRClass=true`).

## 2. No album art, ever
Upstream looked for album, artist and playlist pictures in many places. All of them are gone; what the app draws instead is the placeholder it already drew while it found nothing: a coloured tile with the first letters (artists) or the music-note tile (albums, songs, playlists, the playing screen), and the same placeholder bitmap in the notification, on the lock screen, in the widgets and as the source of the playing screen's colours.
- DELETED: `cache/ImageCache`, `BitmapWorkerTask`, `BlurBitmapWorkerTask`, `SimpleBitmapWorkerTask`, `PlaylistWorkerTask`, `cache/disklrucache/*` (memory + disk caches, the MediaStore album-art reader, background tasks, the RenderScript blur), `provider/PlaylistArtworkStore` (timestamps of playlist covers), `menu/PhotoSelectionDialog` (choose a photo / use the default), `utils/IoUtils` (closed the cache's streams and imported `java.net.Socket`; nothing else used it). Also removed after the read-only audit (2026-11-11): `cache/LruCache` (unused), the second constructor of `utils/colors/BitmapWithColors` (it ran Palette over any picture; the one left takes the colours of the placeholder) and the Palette call behind it.
- REWRITTEN, same public names so the callers did not change: `cache/ImageWorker` and `cache/ImageFetcher` now only draw the placeholders (`loadImage`, `loadPlaylistImage`, `loadBlurImage` = default state of the scrim, `getArtwork` = the placeholder bitmap). No cache, no worker task, no file, content-provider or network access.
- PATCHED to match: `ElevenApplication` (no cache to trim), `utils/ElevenUtils.getImageFetcher`, `MusicPlaybackService` (no cache to set), `ui/activities/HomeActivity` (the gallery photo picker and its result handler), `ui/activities/SettingsActivity` (the "Delete cache" preference and the cache clearing on the blur switch), `menu/DeleteDialog` (no cache to clean), `utils/MusicUtils` (`removeFromCache`, `selectOldPhoto`), `utils/PopupMenuHelper` + `menu/FragmentMenuItems` (the "Change image" menu entry and the unused "fetch" ids), `utils/AlbumPopupMenuHelper` + `ArtistPopupMenuHelper` (the click handler that opened the photo dialog), `provider/MusicDB` (the playlist-artwork table), `utils/ImageUtils` (picture scaling), `ui/fragments/AudioPlayerFragment`, `ArtistFragment`, `AlbumFragment` and the Artist, Album and ArtistDetailAlbum adapters (the disk-cache `flush()` calls and the `onPause` overrides that only did that).
- RESOURCES: `res/xml/settings.xml` loses the "Storage" category (it only held "Delete cache"); the strings of the removed items are removed from `values/strings.xml` only (the translations keep them unused).
- KEPT ON PURPOSE: `LetterTileDrawable` and the `default_artwork` drawable (the placeholders), `BitmapWithColors` + `ColorExtractor` + Palette (they colour the PLACEHOLDER), `AlbumScrimImage` (flat default colour), `MusicPlaybackService.getAlbumArt` and its users (notification, media session, three widgets), which now only ever receive the placeholder.
- The manifest has no INTERNET permission (upstream neither), so nothing can be fetched over the network.

## 3. What Tj will notice (everything else is upstream)
- No cover pictures anywhere; artists show coloured letter tiles, songs and albums the note tile.
- No "Change image" entry on artists and albums, no photo picker, no "Delete cache" setting.

## 4. The guard
`src/test/java/org/lineageos/eleven/NoAlbumArtGuardTest.java` scans this module's source and fails when a way to read, decode, cache, fetch or choose a picture comes back, or when the placeholders are no longer what the app draws. It runs in the unit-test step of every release.
