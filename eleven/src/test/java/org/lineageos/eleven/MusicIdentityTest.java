package org.lineageos.eleven;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

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

    @Test
    public void theIconIsALegacyIconWithoutAnAdaptiveOne() {
        File res = new File(module(), "src/main/res");
        File[] dirs = res.listFiles();
        assertNotNull(dirs);
        for (File d : dirs) {
            if (!d.getName().startsWith("mipmap")) continue;
            assertFalse("an adaptive icon (mipmap-anydpi / -v26) would replace the speaker: " + d.getName(), d.getName().contains("anydpi") || d.getName().contains("-v26"));
            File[] files = d.listFiles();
            if (files == null) continue;
            for (File f : files) {
                assertTrue("only ic_launcher.png may be in " + d.getName() + ", found " + f.getName(), f.getName().equals("ic_launcher.png"));
            }
        }
        assertFalse(new File(res, "drawable/ic_launcher.xml").exists());
    }

    @Test
    public void everyDensityHasTheSpeakerAtTheRightSize() throws IOException {
        String[][] sizes = {{"mdpi", "48"}, {"hdpi", "72"}, {"xhdpi", "96"}, {"xxhdpi", "144"}, {"xxxhdpi", "192"}};
        for (String[] s : sizes) {
            File f = new File(module(), "src/main/res/mipmap-" + s[0] + "/ic_launcher.png");
            assertTrue("missing " + f, f.isFile());
            BufferedImage img = ImageIO.read(f);
            assertNotNull(f + " is not a readable PNG", img);
            int n = Integer.parseInt(s[1]);
            assertEquals(f + " width", n, img.getWidth());
            assertEquals(f + " height", n, img.getHeight());
            assertTrue(f + " has no alpha channel", img.getColorModel().hasAlpha());
            double c = (n - 1) / 2.0;
            // the four corners are transparent (a round artwork, no plate of its own: Launcher3 adds the white plate)
            assertEquals("corner of " + f, 0, alpha(img, 0, 0));
            assertEquals("corner of " + f, 0, alpha(img, n - 1, n - 1));
            // the dust cap in the middle: dark grey, opaque
            int mid = img.getRGB((int) c, (int) c);
            assertEquals(255, (mid >>> 24) & 0xff);
            assertTrue("the dust cap of " + f + " is dark", lum(mid) < 110);
            // the blue cone: at 55 % of the artwork radius to the left and to the right, blue clearly beats red, and it is opaque
            double rad = n * 0.46;
            for (int sign : new int[] {-1, 1}) {
                int px = img.getRGB((int) Math.round(c + sign * rad * 0.55), (int) Math.round(c));
                assertEquals(255, (px >>> 24) & 0xff);
                int r = (px >> 16) & 0xff, b = px & 0xff;
                assertTrue("the cone of " + f + " is blue (r=" + r + ", b=" + b + ")", b - r >= 60 && b >= 110);
            }
            // the bezel: a neutral grey ring (no colour cast) near the edge, opaque
            int ring = img.getRGB((int) Math.round(c), (int) Math.round(c - rad * 0.93));
            assertEquals(255, (ring >>> 24) & 0xff);
            int rr = (ring >> 16) & 0xff, gg = (ring >> 8) & 0xff, bb = ring & 0xff;
            assertTrue("the bezel of " + f + " is grey", Math.abs(rr - gg) <= 8 && Math.abs(gg - bb) <= 8 && rr > 40);
        }
    }

    private static int alpha(BufferedImage img, int x, int y) {
        return (img.getRGB(x, y) >>> 24) & 0xff;
    }

    private static int lum(int argb) {
        int r = (argb >> 16) & 0xff, g = (argb >> 8) & 0xff, b = argb & 0xff;
        return (r * 299 + g * 587 + b * 114) / 1000;
    }
}
