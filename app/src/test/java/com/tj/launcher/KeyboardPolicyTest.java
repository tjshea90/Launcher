package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class KeyboardPolicyTest {
    private static final KeyboardPolicy.Ime LATIN = new KeyboardPolicy.Ime("com.android.inputmethod.latin/.LatinIME", "com.android.inputmethod.latin", true);
    private static final KeyboardPolicy.Ime GBOARD = new KeyboardPolicy.Ime("com.google.android.inputmethod.latin/x", "com.google.android.inputmethod.latin", true);
    private static final KeyboardPolicy.Ime SWIFT = new KeyboardPolicy.Ime("com.touchtype.swiftkey/com.touchtype.KeyboardService", "com.touchtype.swiftkey", false);
    private static final KeyboardPolicy.Ime OTHER = new KeyboardPolicy.Ime("com.other/.Ime", "com.other", false);
    /** Gboard as Tj has it: the Play Store app (a user app, not a system app), with its real input-method id. */
    private static final KeyboardPolicy.Ime GBOARD_APP = new KeyboardPolicy.Ime("com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME", "com.google.android.inputmethod.latin", false);
    /** Google's Indic keyboard: a Google input method that is NOT Gboard. */
    private static final KeyboardPolicy.Ime GOOGLE_INDIC = new KeyboardPolicy.Ime("com.google.android.apps.inputmethod.hindi/.HindiIme", "com.google.android.apps.inputmethod.hindi", true);

    @Test public void customModeUsesTheStockSystemKeyboard() {
        List<KeyboardPolicy.Ime> l = Arrays.asList(SWIFT, LATIN, OTHER);
        assertEquals(LATIN.id, KeyboardPolicy.target(Mode.CUSTOM, l, SWIFT.id));
    }

    // ---- BX1 (Tj, 2026-11-12): "I no longer use Microsoft Swiftkey. I use Google Gboard. Make the custom launcher open the same default keyboard it already opens, no changes needed, but make quickstep open Gboard"
    @Test public void quickstepModeUsesGboard() {
        assertEquals(GBOARD_APP.id, KeyboardPolicy.target(Mode.QUICKSTEP, Arrays.asList(LATIN, GBOARD_APP), LATIN.id));
        assertEquals("com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME", KeyboardPolicy.pickGboard(Arrays.asList(LATIN, GBOARD_APP)));
    }
    @Test public void quickstepPrefersGboardEvenWhenSwiftKeyIsStillInstalledListedFirstAndWasTheOriginalKeyboard() {
        assertEquals(GBOARD_APP.id, KeyboardPolicy.target(Mode.QUICKSTEP, Arrays.asList(SWIFT, LATIN, GBOARD_APP), SWIFT.id));
        assertEquals(GBOARD_APP.id, KeyboardPolicy.target(Mode.QUICKSTEP, Arrays.asList(SWIFT, GBOARD_APP), null));
    }
    @Test public void quickstepFindsGboardWhetherItIsAUserAppOrASystemApp() {
        assertEquals(GBOARD.id, KeyboardPolicy.target(Mode.QUICKSTEP, Arrays.asList(LATIN, GBOARD), LATIN.id));
        assertEquals(GBOARD_APP.id, KeyboardPolicy.target(Mode.QUICKSTEP, Arrays.asList(GBOARD_APP, LATIN), LATIN.id));
    }
    @Test public void quickstepNoLongerChoosesSwiftKeyJustBecauseItIsEnabled() {
        // SwiftKey enabled, Gboard not: the answer is the keyboard that was the default before we ever switched, not SwiftKey
        assertEquals(LATIN.id, KeyboardPolicy.target(Mode.QUICKSTEP, Arrays.asList(LATIN, SWIFT), LATIN.id));
        assertNull(KeyboardPolicy.target(Mode.QUICKSTEP, Arrays.asList(SWIFT), null));
    }
    @Test public void quickstepFallsBackToTheOriginalWhenGboardIsNotEnabled() {
        assertEquals(LATIN.id, KeyboardPolicy.target(Mode.QUICKSTEP, Arrays.asList(LATIN), LATIN.id));
        assertEquals(OTHER.id, KeyboardPolicy.target(Mode.QUICKSTEP, Arrays.asList(LATIN, OTHER), OTHER.id));
        assertNull("nothing usable: leave the keyboard alone", KeyboardPolicy.target(Mode.QUICKSTEP, Arrays.asList(LATIN), "gone/.Ime"));
    }
    @Test public void gboardIsRecognisedByItsPackageOnly() {
        assertTrue(KeyboardPolicy.isGboard("com.google.android.inputmethod.latin"));
        assertFalse(KeyboardPolicy.isGboard("com.android.inputmethod.latin"));          // the stock AOSP keyboard
        assertFalse(KeyboardPolicy.isGboard("com.google.android.apps.inputmethod.hindi"));
        assertFalse(KeyboardPolicy.isGboard("com.google.android.inputmethod.latin.beta"));
        assertFalse(KeyboardPolicy.isGboard(null));
        // another Google input method is not taken for Gboard
        assertNull(KeyboardPolicy.pickGboard(Arrays.asList(LATIN, GOOGLE_INDIC)));
        assertEquals(LATIN.id, KeyboardPolicy.target(Mode.QUICKSTEP, Arrays.asList(LATIN, GOOGLE_INDIC), LATIN.id));
    }
    @Test public void theCustomLauncherKeyboardIsUnchangedWhateverGboardDoes() {
        // Gboard enabled as a user app, or even as a system app: custom still opens the stock AOSP keyboard
        assertEquals(LATIN.id, KeyboardPolicy.target(Mode.CUSTOM, Arrays.asList(GBOARD_APP, LATIN), GBOARD_APP.id));
        assertEquals(LATIN.id, KeyboardPolicy.target(Mode.CUSTOM, Arrays.asList(LATIN, GBOARD_APP, SWIFT), SWIFT.id));
        assertEquals(LATIN.id, KeyboardPolicy.target(Mode.CUSTOM, Arrays.asList(GBOARD, LATIN), GBOARD.id));
        assertEquals(LATIN.id, KeyboardPolicy.target(Mode.CUSTOM, Arrays.asList(LATIN, GBOARD_APP), LATIN.id));
        // and a user-installed Gboard alone is never picked for custom (no system keyboard enabled: leave things alone)
        assertNull(KeyboardPolicy.target(Mode.CUSTOM, Arrays.asList(GBOARD_APP), GBOARD_APP.id));
    }
    @Test public void neverPicksAKeyboardThatIsNotEnabled() {
        assertNull(KeyboardPolicy.target(Mode.CUSTOM, Arrays.asList(SWIFT, OTHER), SWIFT.id));
        assertNull(KeyboardPolicy.target(Mode.QUICKSTEP, Arrays.asList(LATIN), "gone/.Ime"));
    }
    @Test public void prefersTheAospLatinKeyboard() {
        assertEquals(LATIN.id, KeyboardPolicy.pickStock(Arrays.asList(GBOARD, LATIN)));
    }

    // ---- BT1e (Tj, 2026-11-11): the Settings look-alike never names SwiftKey ("remove the text 'and Microsoft SwiftKey keyboard' even if that keyboard is installed")
    @Test public void swiftKeyIsRecognisedByPackageOrByLabelInAnyLetterCase() {
        assertTrue(KeyboardPolicy.isSwiftKey("com.touchtype.swiftkey", "Microsoft SwiftKey keyboard"));
        assertTrue(KeyboardPolicy.isSwiftKey("com.touchtype.swiftkey.beta", null));
        assertTrue(KeyboardPolicy.isSwiftKey(null, "Microsoft SWIFTKEY Keyboard"));
        assertTrue(KeyboardPolicy.isSwiftKey("com.other", "SwiftKey"));
        assertFalse(KeyboardPolicy.isSwiftKey("com.google.android.inputmethod.latin", "Gboard"));
        assertFalse(KeyboardPolicy.isSwiftKey(null, null));
    }
    @Test public void theVirtualKeyboardRowDropsSwiftKeyEvenWhenItIsEnabled() {
        List<String> shown = KeyboardPolicy.visibleLabels(Arrays.asList("com.google.android.inputmethod.latin", "com.touchtype.swiftkey"), Arrays.asList("Gboard", "Microsoft SwiftKey keyboard"));
        assertEquals(Arrays.asList("Gboard"), shown);
        assertEquals("Gboard", SsFormat.joinAnd(shown));   // the text Tj saw was "Gboard and Microsoft SwiftKey keyboard"
    }
    @Test public void otherKeyboardsStayInOrderAndASwiftKeyOnlyListIsEmpty() {
        assertEquals(Arrays.asList("Android keyboard (AOSP)", "Gboard"), KeyboardPolicy.visibleLabels(Arrays.asList("a", "com.touchtype.swiftkey", "b"), Arrays.asList("Android keyboard (AOSP)", "Microsoft SwiftKey keyboard", "Gboard")));
        assertEquals(0, KeyboardPolicy.visibleLabels(Arrays.asList("com.touchtype.swiftkey"), Arrays.asList("Microsoft SwiftKey keyboard")).size());
        assertEquals("", SsFormat.joinAnd(KeyboardPolicy.visibleLabels(Arrays.asList("com.touchtype.swiftkey"), Arrays.asList("Microsoft SwiftKey keyboard"))));
    }
    @Test public void aMissingPackageListOrBlankLabelsAreHandled() {
        assertEquals(Arrays.asList("Gboard"), KeyboardPolicy.visibleLabels(null, Arrays.asList("Gboard", "Microsoft SwiftKey keyboard", " ", null)));
        assertEquals(0, KeyboardPolicy.visibleLabels(Arrays.asList("x"), null).size());
    }
}
