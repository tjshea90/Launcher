/*
 * Copyright (C) 2012 Andrew Neal
 * Copyright (C) 2014 The CyanogenMod Project
 * Copyright (C) 2019-2021 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lineageos.eleven.cache;

import android.content.Context;
import android.widget.ImageView;

import org.lineageos.eleven.Config;
import org.lineageos.eleven.utils.MusicUtils;
import org.lineageos.eleven.utils.PreferenceUtils;
import org.lineageos.eleven.utils.colors.BitmapWithColors;
import org.lineageos.eleven.utils.colors.ColorExtractor;
import org.lineageos.eleven.widgets.AlbumScrimImage;
import org.lineageos.eleven.widgets.LetterTileDrawable;

/**
 * MODIFIED for the Launcher build (TASKS BV1; see CHANGES.md): the entry points every screen calls to "load" an album, artist or playlist picture, with the same names and
 * signatures as upstream, but they only ever draw the app's own placeholder tiles (see {@link ImageWorker}). Nothing is fetched, decoded, cached or read from the media
 * library, a file or the network; the user can no longer pick or fetch a picture either (upstream's cache, photo picker and "fetch album art" are gone).
 */
public class ImageFetcher extends ImageWorker {

    private static ImageFetcher sInstance = null;
    private boolean mUseBlur;

    /**
     * Creates a new instance of {@link ImageFetcher}.
     *
     * @param context The {@link Context} to use.
     */
    public ImageFetcher(final Context context) {
        super(context);
        mUseBlur = PreferenceUtils.getInstance(context).getUseBlur();
    }

    /**
     * Used to create a singleton of the image fetcher
     *
     * @param context The {@link Context} to use
     * @return A new instance of this class.
     */
    public static ImageFetcher getInstance(final Context context) {
        if (sInstance == null) {
            sInstance = new ImageFetcher(context.getApplicationContext());
        }
        return sInstance;
    }

    public void setUseBlur(boolean useBlur) {
        mUseBlur = useBlur;
    }

    /**
     * Shows a playlist's placeholder (upstream: its most played song's artist image)
     *
     * @param playlistId id of the playlist
     * @param imageView  imageview to load into
     */
    public void loadPlaylistArtistImage(final long playlistId, final ImageView imageView) {
        loadPlaylistImage(playlistId, imageView);
    }

    /**
     * Shows a playlist's placeholder (upstream: a combined cover of its most played songs)
     *
     * @param playlistId id of the playlist
     * @param imageView  imageview to load into
     */
    public void loadPlaylistCoverArtImage(final long playlistId, final ImageView imageView) {
        loadPlaylistImage(playlistId, imageView);
    }

    /**
     * Used to show album placeholders.
     */
    public void loadAlbumImage(final String artistName, final String albumName, final long albumId,
                               final ImageView imageView) {
        loadImage(generateAlbumCacheKey(albumName, artistName), artistName, albumName, albumId,
                imageView, ImageType.ALBUM);
    }

    /**
     * Used to show the current artwork placeholder.
     */
    public void loadCurrentArtwork(final ImageView imageView) {
        loadImage(getCurrentCacheKey(), MusicUtils.getArtistName(), MusicUtils.getAlbumName(),
                MusicUtils.getCurrentAlbumId(), imageView, ImageType.ALBUM);
    }

    public void updateScrimImage(final AlbumScrimImage image,
                                 final ColorExtractor.Callback callback) {
        if (mUseBlur) {
            loadCurrentBlurredArtwork(image);
        } else {
            loadCurrentGradientArtwork(callback);
        }
    }

    /**
     * The scrim in its default state (upstream blurred the album picture here).
     */
    private void loadCurrentBlurredArtwork(final AlbumScrimImage image) {
        loadBlurImage(getCurrentCacheKey(), MusicUtils.getArtistName(), MusicUtils.getAlbumName(),
                MusicUtils.getCurrentAlbumId(), image);
    }

    private void loadCurrentGradientArtwork(final ColorExtractor.Callback callback) {
        final boolean isServiceUp = MusicUtils.isPlaybackServiceConnected();
        if (!isServiceUp) {
            return;
        }

        ColorExtractor.extractColors(this, callback);
    }

    public static String getCurrentCacheKey() {
        return generateAlbumCacheKey(MusicUtils.getAlbumName(), MusicUtils.getArtistName());
    }

    /**
     * Used to show artist placeholders (coloured tile with the first letters).
     */
    public void loadArtistImage(final String key, final ImageView imageView) {
        loadImage(key, key, null, -1, imageView, ImageType.ARTIST);
    }

    /**
     * Used to show artist placeholders. {@code scaleImgToView} only mattered for a real picture.
     */
    public void loadArtistImage(final String key, final ImageView imageView,
                                boolean scaleImgToView) {
        loadImage(key, key, null, -1, imageView, ImageType.ARTIST, scaleImgToView);
    }

    /**
     * The artwork of the current album, used by {@link org.lineageos.eleven.MusicPlaybackService}
     * for the notification, the lock screen and the widgets, and for the colours of the playing screen.
     * Upstream returned the album picture when it found one; this build returns ONLY the placeholder tile (the same bitmap upstream uses for music without a picture).
     *
     * @param albumName    Unused (kept as upstream's signature)
     * @param albumId      The ID of the current album (the key of the tile colour)
     * @param smallArtwork Get the small version of the placeholder
     * @return The placeholder tile as an {@link android.graphics.Bitmap} with its colours
     */
    public BitmapWithColors getArtwork(final String albumName, final long albumId,
                                       boolean smallArtwork) {
        final String key = String.valueOf(albumId);
        return LetterTileDrawable.createDefaultBitmap(mContext, key, ImageType.ALBUM, false,
                smallArtwork);
    }

    /**
     * Generates key used to tell albums apart. It needs both album name and artist name
     * to let to select correct tile for the case when there are two albums with the
     * same artist.
     *
     * @param albumName  The album name the cache key needs to be generated.
     * @param artistName The artist name the cache key needs to be generated.
     */
    public static String generateAlbumCacheKey(final String albumName, final String artistName) {
        if (albumName == null || artistName == null) {
            return null;
        }
        return albumName + "_" + artistName + "_" + Config.ALBUM_ART_SUFFIX;
    }
}
