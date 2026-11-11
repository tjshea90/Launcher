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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.widget.ImageView;

import org.lineageos.eleven.widgets.AlbumScrimImage;
import org.lineageos.eleven.widgets.LetterTileDrawable;

/**
 * MODIFIED for the Launcher build (TASKS BV1; see CHANGES.md). Tj: "never at any time show album art on any tab or anywhere in the app. It should never load or display album
 * album art, ever."
 *
 * Upstream's ImageWorker looked for album, artist and playlist pictures (memory and disk cache, the MediaStore album-art store, embedded pictures, background tasks, a blur of
 * the picture). ALL OF THAT IS GONE. This class only draws the app's own placeholders, the very ones upstream draws while it has found nothing: a {@link LetterTileDrawable}
 * (artists: the first letters of the name on a coloured tile; albums, songs and playlists: the music-note tile). It has no cache, no worker task and reads no file, no
 * content provider and no network. A guard test (src/test, NoAlbumArtGuardTest) fails if any of that comes back.
 */
public abstract class ImageWorker {

    /**
     * Default transition drawable fade time
     */
    public static final int FADE_IN_TIME = 200;

    /**
     * Default transition drawable fade time slow
     */
    public static final int FADE_IN_TIME_SLOW = 1000;

    /**
     * The Context to use
     */
    protected final Context mContext;

    /**
     * Constructor of <code>ImageWorker</code>
     *
     * @param context The {@link Context} to use
     */
    protected ImageWorker(final Context context) {
        mContext = context.getApplicationContext();
    }

    /**
     * @return A new drawable of the default artwork
     */
    public Drawable getNewDrawable(ImageType imageType, String name,
                                   String identifier) {
        LetterTileDrawable letterTileDrawable = new LetterTileDrawable(mContext);
        letterTileDrawable.setTileDetails(name, identifier, imageType);
        letterTileDrawable.setIsCircular(false);
        return letterTileDrawable;
    }

    /**
     * Parses the drawable for instances of TransitionDrawable and breaks them open until it finds
     * a drawable that isn't a transition drawable
     *
     * @param drawable to parse
     * @return the target drawable that isn't a TransitionDrawable
     */
    public static Drawable getTopDrawable(final Drawable drawable) {
        if (drawable == null) {
            return null;
        }

        Drawable retDrawable = drawable;
        while (retDrawable instanceof TransitionDrawable) {
            TransitionDrawable transition = (TransitionDrawable) retDrawable;
            retDrawable = transition.getDrawable(transition.getNumberOfLayers() - 1);
        }

        return retDrawable;
    }

    /**
     * Creates a transition drawable to Bitmap with params. In this build the only caller is
     * {@link AlbumScrimImage}, which passes the flat default colour, never a picture.
     *
     * @param resources    Android Resources!
     * @param fromDrawable the drawable to transition from
     * @param bitmap       the bitmap to transition to
     * @param fadeTime     the fade time in MS to fade in
     * @param force        force create a transition even if bitmap == null (fade to transparent)
     * @return the drawable if created, null otherwise
     */
    public static TransitionDrawable createImageTransitionDrawable(final Resources resources,
                                                                   final Drawable fromDrawable,
                                                                   final Bitmap bitmap,
                                                                   final int fadeTime,
                                                                   final boolean force) {
        if (bitmap != null || force) {
            final Drawable[] arrayDrawable = new Drawable[2];
            arrayDrawable[0] = getTopDrawable(fromDrawable);

            // Add the transition to drawable
            Drawable layerTwo;
            if (bitmap != null) {
                layerTwo = new BitmapDrawable(resources, bitmap);
                layerTwo.setFilterBitmap(false);
            } else {
                // if no bitmap (forced) then transition to transparent
                layerTwo = new ColorDrawable(Color.TRANSPARENT);
            }

            arrayDrawable[1] = layerTwo;

            // Finally, return the image
            final TransitionDrawable result = new TransitionDrawable(arrayDrawable);
            result.setCrossFadeEnabled(true);
            result.startTransition(fadeTime);
            return result;
        }

        return null;
    }

    /**
     * This will create the palette transition from the original color to the new one
     *
     * @param scrimImage the container to change the color for
     * @param color      the color to transition to
     * @return the transition to run
     */
    public static TransitionDrawable createPaletteTransition(AlbumScrimImage scrimImage,
                                                             int color) {
        final Drawable[] arrayDrawable = new Drawable[2];
        arrayDrawable[0] = getTopDrawable(scrimImage.getBackground());

        if (arrayDrawable[0] == null) {
            arrayDrawable[0] = new ColorDrawable(Color.TRANSPARENT);
        }

        arrayDrawable[1] = new ColorDrawable(color);

        // create the transition
        final TransitionDrawable result = new TransitionDrawable(arrayDrawable);
        result.setCrossFadeEnabled(true);
        result.startTransition(FADE_IN_TIME_SLOW);
        return result;
    }

    /**
     * Loads the default image into the image view given the image type
     *
     * @param imageView The {@link ImageView}
     * @param imageType The type of image
     */
    public void loadDefaultImage(final ImageView imageView, final ImageType imageType,
                                 final String name, final String identifier) {
        if (imageView != null) {
            // if an existing letter drawable exists, re-use it
            Drawable existingDrawable = imageView.getDrawable();
            if (existingDrawable instanceof LetterTileDrawable) {
                ((LetterTileDrawable) existingDrawable).setTileDetails(name, identifier, imageType);
            } else {
                imageView.setImageDrawable(getNewDrawable(imageType, name,
                        identifier));
            }
        }
    }

    /**
     * Shows the placeholder for an artist, album or playlist. Upstream then went looking for a picture; this build never does.
     *
     * @param key        The unique identifier for the image.
     * @param artistName The artist name (the letters of an artist tile).
     * @param albumName  Unused here (kept so the callers stay as upstream wrote them).
     * @param albumId    The album id (the key of an album tile).
     * @param imageView  The {@link ImageView} that gets the placeholder.
     * @param imageType  Artist, album or playlist.
     */
    protected void loadImage(final String key, final String artistName,
                             final String albumName, final long albumId,
                             final ImageView imageView, final ImageType imageType) {

        loadImage(key, artistName, albumName, albumId, imageView, imageType, false);
    }

    /**
     * Same as above; {@code scaleImgToView} only mattered for a real picture and is ignored.
     */
    protected void loadImage(final String key, final String artistName, final String albumName,
                             final long albumId, final ImageView imageView,
                             final ImageType imageType, final boolean scaleImgToView) {

        if (key == null || imageView == null) {
            return;
        }

        if (imageType == ImageType.ARTIST) {
            loadDefaultImage(imageView, imageType, artistName, key);
        } else if (imageType == ImageType.ALBUM) {
            // don't show letters for albums so pass in null as the display string
            // because an album could have multiple artists, use the album id as the key here
            loadDefaultImage(imageView, imageType, null, String.valueOf(albumId));
        } else {
            // don't show letters for playlists so pass in null as the display string
            loadDefaultImage(imageView, imageType, null, key);
        }
    }

    /**
     * Shows the playlist placeholder (no letters). Upstream built a cover from the top songs' pictures; this build never does.
     *
     * @param playlistId playlist identifier
     * @param imageView  to set the placeholder on
     */
    public void loadPlaylistImage(final long playlistId, final ImageView imageView) {
        if (imageView == null) {
            return;
        }

        loadDefaultImage(imageView, ImageType.PLAYLIST, null, String.valueOf(playlistId));
    }

    /**
     * The blurred "scrim" behind the playing screen. Upstream blurred the album picture; with no picture it ends in the default state, which is all this build ever does.
     *
     * @param key             The unique identifier for the image.
     * @param albumScrimImage The {@link AlbumScrimImage} that gets the default state.
     */
    protected void loadBlurImage(final String key, final String artistName, final String albumName,
                                 final long albumId, final AlbumScrimImage albumScrimImage) {
        if (key == null || albumScrimImage == null) {
            return;
        }

        albumScrimImage.transitionToDefaultState();
    }

    /**
     * Used to define what type of image URL to fetch for, artist or album.
     */
    public enum ImageType {
        ARTIST,
        ALBUM,
        PLAYLIST
    }
}
