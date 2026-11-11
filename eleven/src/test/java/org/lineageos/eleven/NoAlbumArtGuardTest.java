package org.lineageos.eleven;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.junit.Test;

/**
 * GUARD for TASKS BV1 (Tj, 2026-11-11): "never at any time show album art on any tab or anywhere in the app. It should never load or display album art, ever."
 *
 * Eleven's source is scanned (comments removed, string literals checked separately). The test fails when ANY way of reading, decoding, caching, fetching or choosing a picture
 * is back in the module, or when the placeholders are no longer what the app draws. Adding an exception is a decision: write it in TASKS.md first, then here.
 * Runs in the unit-test step of every release (android.yml) and locally with: bash tools/gradle.sh :eleven:testDebugUnitTest
 */
public class NoAlbumArtGuardTest {

    // ---- what must not appear in code (comments and string contents removed) ----
    private static final String[][] FORBIDDEN_CODE = {
            {"MediaStore album-art column", "\\bALBUM_ART\\b"},
            {"embedded picture", "getEmbeddedPicture"},
            {"media metadata retriever", "MediaMetadataRetriever"},
            {"bitmap decoding", "BitmapFactory|ImageDecoder|decodeStream|decodeFile|decodeResource"},
            {"thumbnails", "loadThumbnail|ThumbnailUtils|MediaStore\\.Images"},
            {"opening files / providers (artwork readers)", "openInputStream|openFileDescriptor|openAssetFileDescriptor|ParcelFileDescriptor"},
            {"image cache / disk cache / workers", "DiskLruCache|ImageCache|BitmapWorkerTask|PlaylistWorkerTask|PlaylistArtworkStore|addBitmapToCache|getCachedBitmap|getArtworkFromFile"},
            {"blur (RenderScript) of a picture", "RenderScript|ScriptIntrinsicBlur"},
            {"choosing or fetching a picture", "PhotoSelection|selectNewPhoto|CHANGE_IMAGE|FETCH_ALBUM_ART|FETCH_ARTIST_IMAGE|ACTION_GET_CONTENT|ACTION_PICK|ACTION_OPEN_DOCUMENT|ACTION_IMAGE_CAPTURE"},
            {"network", "HttpURLConnection|HttpsURLConnection|URLConnection|okhttp|java\\.net\\.URL\\b|WebView|java\\.net\\.Socket"},
    };

    // ---- what must not appear inside a string literal ----
    // "content://media/external/audio/albumart", "albumart/12", "album_art": yes. A class name such as "...AlbumArtPagerAdapter.AlbumArtFragment.ID" (a fragment argument key): no.
    private static final String ALBUM_ART_LITERAL = "(?i)\\balbum_?arts?\\b";
    private static final String[][] FORBIDDEN_LITERALS = {
            {"album-art content URI or column", ALBUM_ART_LITERAL},
            {"image mime type (picture picker)", "(?i)^image/"},
            {"web address", "(?i)^https?://"},
    };

    // ---- the ONLY places that may hand a bitmap to the system or a widget, and where it must come from ----
    private static final Set<String> SYSTEM_BITMAP_FILES = new HashSet<>(Arrays.asList("MusicPlaybackService.java"));
    private static final Set<String> WIDGET_BITMAP_FILES = new HashSet<>(Arrays.asList(
            "AppWidgetSmall.java", "AppWidgetLarge.java", "AppWidgetLargeAlternate.java"));

    private static File module() {
        if (new File("src/main/java").isDirectory()) return new File(".");
        if (new File("eleven/src/main/java").isDirectory()) return new File("eleven");
        throw new AssertionError("cannot find the eleven module from " + new File(".").getAbsolutePath());
    }

    private static List<File> javaFiles() throws IOException {
        List<File> out = new ArrayList<>();
        walk(new File(module(), "src/main/java"), out);
        assertTrue("no Java files found: the guard would pass on nothing", out.size() > 100);
        return out;
    }

    private static void walk(File dir, List<File> out) {
        File[] kids = dir.listFiles();
        if (kids == null) return;
        Arrays.sort(kids);
        for (File k : kids) {
            if (k.isDirectory()) walk(k, out);
            else if (k.getName().endsWith(".java")) out.add(k);
        }
    }

    private static String read(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    /** Source split into code (comments removed, string contents emptied) and the contents of its string literals. */
    static final class Lexed {
        final String code;
        final List<String> literals;
        Lexed(String code, List<String> literals) { this.code = code; this.literals = literals; }
    }

    static Lexed lex(String s) {
        StringBuilder code = new StringBuilder();
        List<String> lits = new ArrayList<>();
        int i = 0, n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (c == '/' && i + 1 < n && s.charAt(i + 1) == '/') {
                while (i < n && s.charAt(i) != '\n') i++;
            } else if (c == '/' && i + 1 < n && s.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < n && !(s.charAt(i) == '*' && s.charAt(i + 1) == '/')) i++;
                i += 2;
            } else if (c == '"') {
                StringBuilder lit = new StringBuilder();
                code.append('"');
                i++;
                while (i < n && s.charAt(i) != '"') {
                    if (s.charAt(i) == '\\' && i + 1 < n) { lit.append(s.charAt(i + 1)); i += 2; }
                    else { lit.append(s.charAt(i)); i++; }
                }
                i++;
                code.append('"');
                lits.add(lit.toString());
            } else if (c == '\'') {
                code.append(c);
                i++;
                while (i < n && s.charAt(i) != '\'') { if (s.charAt(i) == '\\') i++; i++; }
                code.append('\'');
                i++;
            } else {
                code.append(c);
                i++;
            }
        }
        return new Lexed(code.toString(), lits);
    }

    @Test public void theLexerRemovesCommentsAndKeepsLiteralsApart() {
        Lexed l = lex("int a; // getEmbeddedPicture\n/* BitmapFactory */ String u = \"content://x/albumart\"; char q = '\"';");
        assertTrue(l.code, !l.code.contains("getEmbeddedPicture") && !l.code.contains("BitmapFactory"));
        assertEquals(Arrays.asList("content://x/albumart"), l.literals);   // the self-test of the guard: a token inside a URL string is not hidden by a "//"
    }

    /** One real-world example per code rule (same order of names): a rule with a typo that matches nothing would let the guard pass for ever. */
    private static final String[][] EXAMPLES = {
            {"MediaStore album-art column", "String c = MediaStore.Audio.AlbumColumns.ALBUM_ART;"},
            {"embedded picture", "byte[] b = retriever.getEmbeddedPicture();"},
            {"media metadata retriever", "MediaMetadataRetriever r = new MediaMetadataRetriever();"},
            {"bitmap decoding", "Bitmap b = BitmapFactory.decodeStream(in);"},
            {"thumbnails", "Bitmap b = ThumbnailUtils.createAudioThumbnail(f, size, null);"},
            {"opening files / providers (artwork readers)", "ParcelFileDescriptor p = resolver.openFileDescriptor(uri, \"r\");"},
            {"image cache / disk cache / workers", "DiskLruCache cache = DiskLruCache.open(dir, 1, 1, size);"},
            {"blur (RenderScript) of a picture", "RenderScript rs = RenderScript.create(context);"},
            {"choosing or fetching a picture", "Intent i = new Intent(Intent.ACTION_GET_CONTENT);"},
            {"network", "HttpURLConnection c = (HttpURLConnection) url.openConnection();"},
    };

    @Test public void everyCodeRuleCatchesItsOwnExample() {
        assertEquals("one example per rule", FORBIDDEN_CODE.length, EXAMPLES.length);
        for (int i = 0; i < FORBIDDEN_CODE.length; i++) {
            assertEquals(FORBIDDEN_CODE[i][0], EXAMPLES[i][0]);
            Lexed l = lex(EXAMPLES[i][1]);
            assertTrue("rule '" + FORBIDDEN_CODE[i][0] + "' does not catch: " + EXAMPLES[i][1], Pattern.compile(FORBIDDEN_CODE[i][1]).matcher(l.code).find());
        }
        // the same words inside a comment or a string are NOT code (the lexer empties them): a comment that explains the removal must not trip the guard
        assertTrue(!Pattern.compile(FORBIDDEN_CODE[3][1]).matcher(lex("// BitmapFactory is gone\nString s = \"BitmapFactory\";").code).find());
    }

    @Test public void theLiteralRuleCatchesTheRealUriAndColumnButNotAClassName() {
        Pattern p = Pattern.compile(ALBUM_ART_LITERAL);
        assertTrue(p.matcher("content://media/external/audio/albumart").find());
        assertTrue(p.matcher("content://media/external/audio/albumart/12").find());
        assertTrue(p.matcher("album_art").find());
        assertTrue(p.matcher("Albumart").find());
        assertTrue(!p.matcher(".adapters.AlbumArtPagerAdapter.AlbumArtFragment.ID").find());
        assertTrue(!p.matcher("content://media/external/audio/media").find());
    }

    @Test public void noWayToReadDecodeCacheFetchOrChooseAPictureExistsInTheCode() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (File f : javaFiles()) {
            Lexed l = lex(read(f));
            for (String[] rule : FORBIDDEN_CODE) {
                if (Pattern.compile(rule[1]).matcher(l.code).find()) offenders.add(f.getName() + ": " + rule[0] + " (" + rule[1] + ")");
            }
            for (String[] rule : FORBIDDEN_LITERALS) {
                for (String lit : l.literals) {
                    if (Pattern.compile(rule[1]).matcher(lit).find()) offenders.add(f.getName() + ": " + rule[0] + " in the string \"" + lit + "\"");
                }
            }
        }
        assertTrue("album art / picture code is back (TASKS BV1): " + offenders, offenders.isEmpty());
    }

    @Test public void onlyTheServiceHandsABitmapToTheSystemAndOnlyFromGetAlbumArt() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (File f : javaFiles()) {
            String code = lex(read(f)).code;
            boolean system = Pattern.compile("setLargeIcon|METADATA_KEY_ALBUM_ART|METADATA_KEY_ART\\b|METADATA_KEY_DISPLAY_ICON|putBitmap").matcher(code).find();
            if (system && !SYSTEM_BITMAP_FILES.contains(f.getName())) offenders.add(f.getName() + " puts a bitmap into the notification / media session");
            if (system && SYSTEM_BITMAP_FILES.contains(f.getName()) && !code.contains("getAlbumArt(")) offenders.add(f.getName() + " does not take that bitmap from getAlbumArt()");
            boolean widget = code.contains("setImageViewBitmap");
            if (widget && !WIDGET_BITMAP_FILES.contains(f.getName())) offenders.add(f.getName() + " puts a bitmap into a widget");
            if (widget && !code.contains("getAlbumArt(true)")) offenders.add(f.getName() + " does not take the widget bitmap from getAlbumArt(true)");
        }
        assertTrue(offenders.toString(), offenders.isEmpty());
    }

    @Test public void thePaletteOnlyRunsInBitmapWithColorsOnThePlaceholder() throws IOException {
        List<String> users = new ArrayList<>();
        for (File f : javaFiles()) {
            if (Pattern.compile("Palette\\s*\\.\\s*from").matcher(lex(read(f)).code).find()) users.add(f.getName());
        }
        assertEquals(Arrays.asList("BitmapWithColors.java"), users);
    }

    @Test public void theCachePackageHoldsOnlyThePlaceholderClasses() {
        File[] files = new File(module(), "src/main/java/org/lineageos/eleven/cache").listFiles();
        Set<String> names = new TreeSet<>();
        if (files != null) for (File f : files) names.add(f.getName());
        assertEquals(new TreeSet<>(Arrays.asList("ImageFetcher.java", "ImageWorker.java", "LruCache.java")), names);
    }

    @Test public void theProviderPackageHasNoArtworkStore() {
        File[] files = new File(module(), "src/main/java/org/lineageos/eleven/provider").listFiles();
        assertTrue(files != null && files.length > 0);
        for (File f : files) assertTrue(f.getName(), !f.getName().toLowerCase().contains("artwork") && !f.getName().toLowerCase().contains("image"));
    }

    @Test public void theManifestAsksForNoInternet() throws IOException {
        String manifest = read(new File(module(), "src/main/AndroidManifest.xml"));
        assertTrue(!manifest.contains("android.permission.INTERNET"));
    }

    @Test public void theBuildHasNoRenderScriptAndKeepsTheRealPackageName() throws IOException {
        String gradle = read(new File(module(), "build.gradle.kts"));
        assertTrue(!gradle.toLowerCase().contains("renderscript") || gradle.contains("//"));   // only mentioned in a comment, never switched on
        assertTrue(gradle.contains("applicationId = \"org.lineageos.eleven\""));
    }

    @Test public void thePlaceholdersAreStillWhatTheAppDraws() throws IOException {
        File base = new File(module(), "src/main/java/org/lineageos/eleven");
        String worker = lex(read(new File(base, "cache/ImageWorker.java"))).code;
        String fetcher = lex(read(new File(base, "cache/ImageFetcher.java"))).code;
        // lists, grids, headers and the playing screen: the placeholder tile, per type (artists: letters; albums, songs, playlists: the note tile)
        assertTrue(worker.contains("loadDefaultImage(imageView, imageType, artistName, key)"));
        assertTrue(worker.contains("loadDefaultImage(imageView, imageType, null, String.valueOf(albumId))"));
        assertTrue(worker.contains("loadDefaultImage(imageView, ImageType.PLAYLIST, null, String.valueOf(playlistId))"));
        assertTrue(worker.contains("albumScrimImage.transitionToDefaultState()"));
        // notification, lock screen, widgets and the colours of the playing screen: the placeholder bitmap
        assertTrue(fetcher.contains("LetterTileDrawable.createDefaultBitmap(mContext, key, ImageType.ALBUM, false"));
        assertEquals("getArtwork must have exactly one way out", 1, fetcher.split("return LetterTileDrawable\\.createDefaultBitmap", -1).length - 1);
    }
}
