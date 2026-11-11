package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

/**
 * BT1f (Tj, 2026-11-11: "in the custom launcher recents list, make the name of the spoof settings "settings" and not "launcher settings""). Android 10's Recents titles a task
 * "<application label> <activity label>" unless the two are equal (ActivityManagerWrapper.getBadgedContentDescription, AOSP android10-release), so the look-alike (activity label "Settings")
 * showed as "Launcher Settings". These checks pin the three facts that make the card read "Settings": the application label equals the look-alike's label, and the components that must keep the
 * name "Launcher" (the HOME router, the Launcher activity) carry their own explicit label so they do not inherit "Settings".
 */
public class AppLabelTest {
    private static File module() {
        for (String p : new String[]{"src/main", "app/src/main"}) { File f = new File(p); if (new File(f, "AndroidManifest.xml").isFile()) return f; }
        throw new AssertionError("app/src/main not found from " + new File(".").getAbsolutePath());
    }
    private static String read(File f) throws Exception { return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8); }
    private static String string(String name) throws Exception {
        Matcher m = Pattern.compile("<string name=\"" + name + "\"[^>]*>([^<]*)</string>").matcher(read(new File(module(), "res/values/ss_strings.xml")));
        assertTrue(name + " must be defined in ss_strings.xml", m.find());
        return m.group(1);
    }
    /** The opening tag (attributes) of the manifest element whose android:name is {@code name}. */
    private static String tagOf(String manifest, String element, String name) {
        Matcher m = Pattern.compile("<" + element + "\\b[^>]*android:name=\"" + Pattern.quote(name) + "\"[^>]*>", Pattern.DOTALL).matcher(manifest);
        assertTrue(element + " " + name + " must be in the manifest", m.find());
        return m.group();
    }
    private static String label(String tag) {
        Matcher m = Pattern.compile("android:label=\"([^\"]*)\"").matcher(tag);
        return m.find() ? m.group(1) : null;
    }

    @Test public void theApplicationLabelEqualsTheLookAlikeLabelSoTheRecentsCardReadsJustSettings() throws Exception {
        String manifest = read(new File(module(), "AndroidManifest.xml"));
        assertEquals("@string/tj_app_label", label(tagOf(manifest, "application", "com.tj.launcher.TjApplication")));
        assertEquals("@string/ss_label", label(tagOf(manifest, "activity", "com.tj.launcher.SsPageActivity")));
        assertEquals("@string/ss_label", label(tagOf(manifest, "activity-alias", "com.tj.launcher.SettingsAlias")));
        assertEquals("Settings", string("ss_label"));
        assertEquals(string("ss_label"), string("tj_app_label"));
        assertEquals("the closer's exemption for the look-alike's own card (RecentsFilter) must name what the resources name", SettingsSpoof.LABEL, string("ss_label"));   // review C #1 (BT1)
    }

    /** Review C #2 (BT1): a component WITHOUT its own label inherits the application label, which is now "Settings". The ones a person can meet by name (activities, aliases, services) all say what they are. */
    @Test public void everyActivityAliasAndServiceCarriesItsOwnLabel() throws Exception {
        String manifest = read(new File(module(), "AndroidManifest.xml")).replaceAll("(?s)<!--.*?-->", "");
        Matcher m = Pattern.compile("<(activity-alias|activity|service)\\b([^>]*)>", Pattern.DOTALL).matcher(manifest);
        int seen = 0;
        while (m.find()) {
            seen++;
            Matcher n = Pattern.compile("android:name=\"([^\"]*)\"").matcher(m.group(2));
            assertTrue("a component without a name", n.find());
            String name = n.group(1);
            if (name.endsWith("ColorExtractionService")) continue;   // an upstream bound service nobody can reach by name
            assertTrue(m.group(1) + " " + name + " has no android:label and would read \"Settings\"", label(m.group()) != null);
        }
        assertTrue("the manifest scan found the components", seen >= 15);
    }

    @Test public void theHomeRouterAndTheLauncherActivityKeepTheNameLauncher() throws Exception {
        String manifest = read(new File(module(), "AndroidManifest.xml"));
        assertEquals("@string/derived_app_name", label(tagOf(manifest, "activity", "com.tj.launcher.RouterActivity")));
        assertEquals("@string/derived_app_name", label(tagOf(manifest, "activity", "com.android.launcher3.Launcher")));
    }
}
