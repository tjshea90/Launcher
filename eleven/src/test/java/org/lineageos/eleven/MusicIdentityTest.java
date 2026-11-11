package org.lineageos.eleven;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

/**
 * TASKS BW1 (Tj, 2026-11-11): "The music app you are building must be called 'Music' with that exact icon" (the blue speaker of the ATG music player on his tablet).
 *
 * The name: Android shows the app under the label of its launcher activity. It must be the plain string resource {@code app_name}, that resource must say "Music" and NO translation may
 * override it (upstream translated it: "Musique", "Musica", ...), so the app is called Music in every language.
 * The icon: a LEGACY icon (one PNG per density, drawn by tools/make_music_icon.py from the speaker of Tj's screenshots) and no adaptive icon. Launcher3 puts a legacy icon on its white plate
 * exactly as it does for his existing Music, which an adaptive icon (upstream's pink note, background + foreground layers) would not.
 * Runs in the unit-test step of every release and locally with: bash tools/gradle.sh :eleven:testDebugUnitTest
 */
public class MusicIdentityTest {

    private static File module() {
        if (new File("src/main/java").isDirectory()) return new File(".");
        if (new File("eleven/src/main/java").isDirectory()) return new File("eleven");
        throw new AssertionError("cannot find the eleven module from " + new File(".").getAbsolutePath());
    }

    private static String read(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    @Test
    public void theAppNameStringIsMusic() throws IOException {
        String s = read(new File(module(), "src/main/res/values/strings.xml"));
        Matcher m = Pattern.compile("<string name=\"app_name\"[^>]*>([^<]*)</string>").matcher(s);
        assertTrue("values/strings.xml has no app_name", m.find());
        assertEquals("Music", m.group(1));
    }

    @Test
    public void noTranslationRenamesTheApp() throws IOException {
        List<String> offenders = new ArrayList<>();
        File[] dirs = new File(module(), "src/main/res").listFiles();
        assertNotNull(dirs);
        for (File d : dirs) {
            if (!d.isDirectory() || !d.getName().startsWith("values")) continue;
            File[] files = d.listFiles();
            if (files == null) continue;
            for (File f : files) {
                if (!f.getName().endsWith(".xml")) continue;
                if (d.getName().equals("values") && f.getName().equals("strings.xml")) continue;   // the one allowed definition
                if (Pattern.compile("name=\"app_name\"").matcher(read(f)).find()) offenders.add(d.getName() + "/" + f.getName());
            }
        }
        assertTrue("translations that rename the app (it is called Music in every language): " + offenders, offenders.isEmpty());
    }

    @Test
    public void theLauncherActivityAndTheApplicationUseTheAppNameString() throws IOException {
        String manifest = read(new File(module(), "src/main/AndroidManifest.xml"));
        // the application label
        Matcher app = Pattern.compile("<application\\b[^>]*>", Pattern.DOTALL).matcher(manifest);
        assertTrue(app.find());
        assertTrue("the application label must be @string/app_name", app.group().contains("android:label=\"@string/app_name\""));
        assertTrue("the application icon must be @mipmap/ic_launcher", app.group().contains("android:icon=\"@mipmap/ic_launcher\""));
        assertFalse("no round icon (it would bring back an adaptive one)", manifest.contains("android:roundIcon"));
        // the activity with the launcher intent filter: its own label, if it has one, must be the same string
        Matcher act = Pattern.compile("<activity\\b[^>]*>(?:(?!</activity>).)*?android.intent.category.LAUNCHER(?:(?!</activity>).)*?</activity>", Pattern.DOTALL).matcher(manifest);
        assertTrue("no launcher activity in the manifest", act.find());
        Matcher label = Pattern.compile("android:label=\"([^\"]*)\"").matcher(act.group().substring(0, act.group().indexOf('>') + 1));
        if (label.find()) assertEquals("@string/app_name", label.group(1));
        // no hard-coded label anywhere in the manifest that names the app
        assertFalse(manifest.contains("android:label=\"Eleven\"") || manifest.contains("android:label=\"Music\""));
    }

    /**
     * The icon is ADAPTIVE (CI run #554: Launcher3 shrinks a round LEGACY icon to 62 % of its white plate, Tj's speaker is 71.5 %; an adaptive icon is not shrunk): a pure white background layer
     * and a foreground layer that holds only the speaker. No second source (no legacy ic_launcher.png, no upstream pink note layers).
     */
    @Test
    public void theIconIsAnAdaptiveIconWithAPureWhiteBackground() throws IOException {
        File res = new File(module(), "src/main/res");
        String xml = read(new File(res, "mipmap-anydpi/ic_launcher.xml"));
        assertTrue("not an adaptive icon", xml.contains("<adaptive-icon"));
        assertTrue("the background layer must be the white colour", xml.contains("<background android:drawable=\"@color/ic_launcher_background\""));
        assertTrue("the foreground layer must be the speaker", xml.contains("<foreground android:drawable=\"@mipmap/ic_launcher_foreground\""));
        assertFalse("the icon has more layers than background + foreground", xml.contains("<monochrome") || xml.contains("<inset"));
        Matcher c = Pattern.compile("<color name=\"ic_launcher_background\">#([0-9A-Fa-f]{6,8})</color>").matcher(read(new File(res, "values/ic_launcher_background.xml")));
        assertTrue("values/ic_launcher_background.xml has no ic_launcher_background colour", c.find());
        assertTrue("the plate behind the speaker is pure white, as in Tj's drawer (found #" + c.group(1) + ")", c.group(1).equalsIgnoreCase("FFFFFF") || c.group(1).equalsIgnoreCase("FFFFFFFF"));
        File[] dirs = res.listFiles();
        assertNotNull(dirs);
        for (File d : dirs) {
            if (!d.getName().startsWith("mipmap")) continue;
            File[] files = d.listFiles();
            if (files == null) continue;
            for (File f : files) {
                if (d.getName().equals("mipmap-anydpi")) assertEquals("only the adaptive xml may be in mipmap-anydpi", "ic_launcher.xml", f.getName());
                else assertEquals("only the speaker foreground may be in " + d.getName() + ", found " + f.getName(), "ic_launcher_foreground.png", f.getName());
            }
        }
        assertFalse(new File(res, "drawable/ic_launcher.xml").exists());
    }

    /**
     * Every density's foreground layer is 108 dp; Launcher3 shows its middle 72 dp (the white plate) and Tj's speaker is 39.34 of the plate's 55 px = 71.5 % wide, so the speaker is 0.4769 of the layer
     * (tools/make_music_icon.py FOREGROUND_RADIUS). Nothing but the speaker is drawn: the corners and the ring just outside it are transparent.
     */
    @Test
    public void everyDensityHasTheSpeakerAtTheRightSize() throws Exception {
        String[][] sizes = {{"mdpi", "108"}, {"hdpi", "162"}, {"xhdpi", "216"}, {"xxhdpi", "324"}, {"xxxhdpi", "432"}};
        for (String[] s : sizes) {
            File f = new File(module(), "src/main/res/mipmap-" + s[0] + "/ic_launcher_foreground.png");
            assertTrue("missing " + f, f.isFile());
            Png img = Png.read(f);
            int n = Integer.parseInt(s[1]);
            assertEquals(f + " width", n, img.w);
            assertEquals(f + " height", n, img.h);
            int c = n / 2;
            // the speaker's width along the middle row: 0.4769 of the layer (71.5 % of the plate), +- 0.012
            int first = -1, last = -1;
            for (int x = 0; x < n; x++) {
                if (img.alpha(x, c) >= 128) {
                    if (first < 0) first = x;
                    last = x;
                }
            }
            assertTrue("no speaker in " + f, first >= 0);
            double width = (last - first + 1) / (double) n;
            assertEquals("the speaker of " + f + " is 0.4769 of the layer (71.5 % of the plate)", 0.4769, width, 0.012);
            // the four corners and the ring just outside the speaker are transparent: the plate is the background layer's, not the foreground's
            assertEquals("corner of " + f, 0, img.alpha(0, 0));
            assertEquals("corner of " + f, 0, img.alpha(n - 1, n - 1));
            double rad = n * 0.2384;
            for (int sign : new int[] {-1, 1}) {
                assertEquals("outside the speaker of " + f, 0, img.alpha((int) Math.round(n / 2.0 - 0.5 + sign * rad * 1.08), c));
            }
            // the dust cap in the middle: dark grey, opaque
            assertEquals(255, img.alpha(c, c));
            assertTrue("the dust cap of " + f + " is dark", lum(img.argb(c, c)) < 110);
            // the blue cone: at 55 % of the artwork radius to the left and to the right, blue clearly beats red, and it is opaque
            for (int sign : new int[] {-1, 1}) {
                int px = img.argb((int) Math.round(n / 2.0 - 0.5 + sign * rad * 0.55), c);
                assertEquals(255, (px >>> 24) & 0xff);
                int r = (px >> 16) & 0xff, b = px & 0xff;
                assertTrue("the cone of " + f + " is blue (r=" + r + ", b=" + b + ")", b - r >= 60 && b >= 110);
            }
            // the bezel: a neutral grey ring (no colour cast) near the edge, opaque
            int ring = img.argb(c, (int) Math.round(n / 2.0 - 0.5 - rad * 0.93));
            assertEquals(255, (ring >>> 24) & 0xff);
            int rr = (ring >> 16) & 0xff, gg = (ring >> 8) & 0xff, bb = ring & 0xff;
            assertTrue("the bezel of " + f + " is grey", Math.abs(rr - gg) <= 8 && Math.abs(gg - bb) <= 8 && rr > 40);
        }
    }

    /** A minimal PNG reader (8-bit RGBA, not interlaced: what tools/make_music_icon.py writes): the unit tests compile against android.jar, which has no javax.imageio. */
    private static final class Png {
        int w, h;
        int[] px;

        int argb(int x, int y) { return px[y * w + x]; }

        int alpha(int x, int y) { return (px[y * w + x] >>> 24) & 0xff; }

        static int be32(byte[] d, int p) {
            return ((d[p] & 0xff) << 24) | ((d[p + 1] & 0xff) << 16) | ((d[p + 2] & 0xff) << 8) | (d[p + 3] & 0xff);
        }

        static Png read(File f) throws IOException, DataFormatException {
            byte[] d = Files.readAllBytes(f.toPath());
            byte[] sig = {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a};
            for (int i = 0; i < 8; i++) assertEquals(f + " is not a PNG", sig[i], d[i]);
            Png o = new Png();
            int depth = 0, ctype = -1, interlace = -1;
            ByteArrayOutputStream idat = new ByteArrayOutputStream();
            for (int p = 8; p + 8 <= d.length; ) {
                int len = be32(d, p);
                String type = new String(d, p + 4, 4, StandardCharsets.US_ASCII);
                int body = p + 8;
                if (type.equals("IHDR")) {
                    o.w = be32(d, body); o.h = be32(d, body + 4);
                    depth = d[body + 8] & 0xff; ctype = d[body + 9] & 0xff; interlace = d[body + 12] & 0xff;
                } else if (type.equals("IDAT")) {
                    idat.write(d, body, len);
                } else if (type.equals("IEND")) {
                    break;
                }
                p = body + len + 4;
            }
            assertEquals(f + ": 8 bits per channel", 8, depth);
            assertEquals(f + ": colour type 6 (RGBA)", 6, ctype);
            assertEquals(f + ": not interlaced", 0, interlace);
            int stride = o.w * 4;
            byte[] raw = new byte[(stride + 1) * o.h];
            Inflater inf = new Inflater();
            inf.setInput(idat.toByteArray());
            int off = 0;
            while (off < raw.length) {
                int n = inf.inflate(raw, off, raw.length - off);
                if (n == 0 && (inf.finished() || inf.needsInput())) break;
                off += n;
            }
            inf.end();
            assertEquals(f + ": the picture data is complete", raw.length, off);
            byte[] img = new byte[stride * o.h];
            for (int y = 0; y < o.h; y++) {
                int ft = raw[y * (stride + 1)] & 0xff;
                for (int x = 0; x < stride; x++) {
                    int cur = raw[y * (stride + 1) + 1 + x] & 0xff;
                    int a = x >= 4 ? img[y * stride + x - 4] & 0xff : 0;
                    int b = y > 0 ? img[(y - 1) * stride + x] & 0xff : 0;
                    int c = (x >= 4 && y > 0) ? img[(y - 1) * stride + x - 4] & 0xff : 0;
                    int v;
                    switch (ft) {
                        case 0: v = cur; break;
                        case 1: v = cur + a; break;
                        case 2: v = cur + b; break;
                        case 3: v = cur + ((a + b) >> 1); break;
                        case 4: {
                            int pp = a + b - c, pa = Math.abs(pp - a), pb = Math.abs(pp - b), pc = Math.abs(pp - c);
                            v = cur + ((pa <= pb && pa <= pc) ? a : (pb <= pc ? b : c));
                            break;
                        }
                        default: throw new AssertionError(f + ": unknown PNG filter " + ft);
                    }
                    img[y * stride + x] = (byte) v;
                }
            }
            o.px = new int[o.w * o.h];
            for (int i = 0; i < o.px.length; i++) {
                int r = img[i * 4] & 0xff, g = img[i * 4 + 1] & 0xff, bl = img[i * 4 + 2] & 0xff, al = img[i * 4 + 3] & 0xff;
                o.px[i] = (al << 24) | (r << 16) | (g << 8) | bl;
            }
            return o;
        }
    }

    private static int lum(int argb) {
        int r = (argb >> 16) & 0xff, g = (argb >> 8) & 0xff, b = argb & 0xff;
        return (r * 299 + g * 587 + b * 114) / 1000;
    }
}
