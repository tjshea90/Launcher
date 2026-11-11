# Eleven in the custom launcher, with NO album art ever (TASKS BV1; audit + plan, 2026-11-11)

Tj, 2026-11-11 ~16:10 UTC (full text in TASKS.md BV1): include the music app `org.lineageos.eleven` from the Android 10 branch in the custom launcher's drawer; same look and function as the code, but it must NEVER load or display album art anywhere; artists show coloured letter tiles, songs / now playing show the app's own music-note tile. A separate APK is fine.

## 1. The pin (BV1a)
- Repo https://github.com/LineageOS/android_packages_apps_Eleven , branch `lineage-17.1`, commit `ca22308cfe54b3c4d7200441a3f951f4831fb035` (2024-02-08, "Automatic translation import"). Licence Apache-2.0 (`NOTICE`: "Copyright (c) 2014, The CyanogenMod Project").
- Manifest has NO `INTERNET` permission (only `ACCESS_NETWORK_STATE`), so nothing in the app can fetch art over the network. Other permissions (storage, wake lock, foreground service, modify audio settings, ...) stay as upstream.
- Source: `src/` (150 Java files) and `src_aosp/` (1 file), `res/`, `AndroidManifest.xml`. Upstream builds with AGP 7.0.4 / compileSdk 30 / RenderScript (only the album-art blur uses it).
- Decision: ONE vendored Gradle module `eleven/` in this repo (pinned code, reproducible, nothing fetched at build time), applicationId `org.lineageos.eleven`, built and signed by our CI, shipped as its own APK. Every file we change gets a "modified" notice (Apache 2.0 section 4) and is listed in `eleven/CHANGES.md`.

## 2. Where album art enters the app (audit of the pinned commit; BV1b)
| # | Where | What it does |
| :- | :- | :- |
| 1 | `cache/ImageCache.java` | memory + disk (DiskLruCache) cache of bitmaps; `getCachedArtwork` / `getArtworkFromFile` read MediaStore `content://media/external/audio/albumart` (embedded and folder art); user-picked photos are stored here too |
| 2 | `cache/ImageWorker.java` | `loadImage` (every list, grid, header, now-playing image): shows the placeholder first, then `SimpleBitmapWorkerTask` swaps in a real bitmap if one exists; `loadPlaylistImage` (+ `PlaylistWorkerTask`: mosaic of the top songs' covers); `loadBlurImage` (+ `BlurBitmapWorkerTask`, RenderScript, runs only on a real bitmap) |
| 3 | `cache/ImageFetcher.java` | `getArtwork` / `getArtworkBitmap` (real bitmap, else the default tile bitmap) feed the notification, MediaSession, lock screen, widgets and `ColorExtractor`; `decodeSampledBitmapFromUri` decodes a photo the user picked |
| 4 | `MusicPlaybackService.java` | 1551-1571 MediaSession metadata (`METADATA_KEY_ALBUM_ART`), 1613-1650 notification (`setLargeIcon`), 2827-2850 `getAlbumArt`, 617 `setImageCache` |
| 5 | `appwidgets/AppWidgetLarge:118`, `AppWidgetLargeAlternate:123`, `AppWidgetSmall:119` | `service.getAlbumArt(true).getBitmap()` |
| 6 | `utils/colors/ColorExtractor.java:59`, `BitmapWithColors.java:183` | Palette colours of the `getArtwork` bitmap (UI colours of the playing screen) |
| 7 | callers of the image entry points | `adapters/` SongList, Playlist, AlbumArtPager:252, ArtistDetailSong, Artist, ProfileSong, QueueSong, ArtistDetailAlbum, Album; `ui/fragments/` ArtistDetail:203,280, PlaylistDetail:197, AlbumDetail:122,202; `BaseActivity:309`, `SlidingPanelActivity:288`, `HomeActivity:229` |
| 8 | user-supplied artwork | `menu/PhotoSelectionDialog`, `AlbumPopupMenuHelper:91`, `ArtistPopupMenuHelper:79`, `PopupMenuHelper:315` + `FragmentMenuItems:46` (`FETCH_ALBUM_ART`), `HomeActivity` `NEW_PHOTO` 81 / 341-365 |
| 9 | housekeeping | `ElevenApplication.onLowMemory` (`evictAll`), `SettingsActivity:93,168` (clear image cache), `DeleteDialog:98` + `MusicUtils:1617-1631` (`removeFromCache`), `provider/MusicDB` creates `PlaylistArtworkStore` (timestamps for playlist covers) |
| 10 | `widgets/AlbumScrimImage` | uses `ImageWorker.createImageTransitionDrawable` / `createPaletteTransition` (pure helpers) |

## 3. What the app draws when there is NO art (this is the look Tj wants everywhere)
- `ImageWorker.loadImage` always draws the placeholder first: ARTIST = `LetterTileDrawable` with the artist's initials (the "Ca", "Fl", "Ta" tiles of Tj's photo); ALBUM = the same drawable with no letters (the music-note tile; key = album id); PLAYLIST = no letters. If no bitmap is found the worker task ends with `imageView.setImageDrawable(mFromDrawable)`, the SAME placeholder. So the "no art" screens are exactly the placeholder code path that already exists.
- `ImageFetcher.getArtwork` returns `LetterTileDrawable.createDefaultBitmap(...)` when there is no art: that tile bitmap is what the notification, lock screen, widgets and the playing-screen colours already use for art-less music.
- `BlurBitmapWorkerTask` with no bitmap ends in `AlbumScrimImage.transitionToDefaultState()`; with blur OFF the scrim colours come from `ColorExtractor` on the default tile bitmap.

## 4. Plan (decided; keeps the no-art behaviour byte for byte, removes everything that could load art)
- DELETE: `cache/ImageCache`, `BitmapWorkerTask`, `BlurBitmapWorkerTask`, `SimpleBitmapWorkerTask`, `PlaylistWorkerTask`, `cache/disklrucache/*`, `cache/LruCache` (if nothing else uses it), `provider/PlaylistArtworkStore` (+ its two calls in `MusicDB`), `menu/PhotoSelectionDialog` and every menu entry / activity result that picks or fetches artwork, the image-cache setting. RenderScript is no longer needed.
- SLIM (placeholder only, same public API so the ~30 callers stay unchanged): `ImageWorker` (`ImageType`, `loadDefaultImage`, `getNewDrawable`, the two static transition helpers, `loadImage` / `loadPlaylistImage` = default drawable only, `loadBlurImage` = `transitionToDefaultState()`), `ImageFetcher` (`getArtwork` = default tile only, no bitmap decoding, no cache).
- KEEP: `LetterTileDrawable`, `BitmapWithColors` + `ColorExtractor` + Palette (they colour the PLACEHOLDER), `AlbumScrimImage`, the whole UI and playback code.
- The notification / MediaSession / widgets keep showing the default tile bitmap exactly as upstream does for art-less music (not album art).
- Guard (BV1c): a JVM test fails if any art API token (`ALBUM_ART`, `albumart`, `getEmbeddedPicture`, `MediaMetadataRetriever`, `BitmapFactory`, `openFileDescriptor`, `ImageCache`, `DiskLruCache`, `RenderScript`, `PhotoSelection`, `METADATA_KEY_ALBUM_ART` outside the one allowed placeholder call, ...) appears in `eleven/src`, with an explicit allowlist.
- Emulator proof (BV1e): an mp3 with an embedded flat-colour cover; that colour must never appear on any screen.

## 5. Open details to check while porting (not decisions for Tj)
- `HomeActivity:225-233` uses `getArtwork` in an AsyncTask (what for?); `SettingsActivity` clear-cache preference and its strings; `PopupMenuHelper:315` case body.
- Build files: upstream `build.gradle` (sourceSets, `src_aosp`, proguard, renderscript flags) must be ported to AGP 8.13.2; resources reference RenderScript nothing; `lint-baseline.xml` is upstream's.
- Upstream clones used for this audit lived in the session scratchpad only; re-clone with `git clone --branch lineage-17.1` and check out the pinned commit above (never commit a clone).
