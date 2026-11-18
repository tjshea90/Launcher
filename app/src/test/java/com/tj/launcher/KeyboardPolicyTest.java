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
    @Test public void theStatusLineWhenNothingCanBeChosenNamesGboardAndWhatIsEnabled() {
        assertEquals("Gboard is not an enabled keyboard (enabled: com.android.inputmethod.latin, com.touchtype.swiftkey): left as is. Turn it on under Virtual keyboard > Manage keyboards.",
                KeyboardPolicy.noTargetStatus(Mode.QUICKSTEP, Arrays.asList(LATIN, SWIFT)));
        assertEquals("Gboard is not an enabled keyboard (enabled: none): left as is. Turn it on under Virtual keyboard > Manage keyboards.", KeyboardPolicy.noTargetStatus(Mode.QUICKSTEP, Arrays.<KeyboardPolicy.Ime>asList()));
        assertTrue(KeyboardPolicy.noTargetStatus(Mode.QUICKSTEP, null).contains("enabled: none"));
        assertEquals("no enabled stock keyboard found: left as is", KeyboardPolicy.noTargetStatus(Mode.CUSTOM, Arrays.asList(OTHER)));   // the custom launcher's line is unchanged
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
    // ---- BX1: Gboard is the Quickstep side's keyboard now, so the look-alike leaves it out like SwiftKey, by package, unless it is the only keyboard listed
    @Test public void theLookAlikeNeverNamesGboardWhileAStockKeyboardIsListed() {
        List<String> shown = KeyboardPolicy.visibleLabels(
                Arrays.asList("com.android.inputmethod.latin", "com.google.android.inputmethod.latin", "com.touchtype.swiftkey"),
                Arrays.asList("Android keyboard (AOSP)", "Gboard", "Microsoft SwiftKey keyboard"));
        assertEquals(Arrays.asList("Android keyboard (AOSP)"), shown);
        assertEquals("Android keyboard (AOSP)", SsFormat.joinAnd(shown));   // not "Android keyboard (AOSP) and Gboard"
        assertEquals(Arrays.asList("Android keyboard (AOSP)"), KeyboardPolicy.visibleLabels(
                Arrays.asList("com.google.android.inputmethod.latin", "com.android.inputmethod.latin"), Arrays.asList("Gboard", "Android keyboard (AOSP)")));   // the order of the list does not matter
    }
    @Test public void gboardIsStillNamedWhenItIsTheOnlyKeyboardSoTheRowNeverGoesBlank() {
        assertEquals(Arrays.asList("Gboard"), KeyboardPolicy.visibleLabels(Arrays.asList("com.google.android.inputmethod.latin"), Arrays.asList("Gboard")));
        assertEquals(Arrays.asList("Gboard"), KeyboardPolicy.visibleLabels(Arrays.asList("com.google.android.inputmethod.latin", "com.touchtype.swiftkey"), Arrays.asList("Gboard", "Microsoft SwiftKey keyboard")));
    }
    @Test public void aStockKeyboardThatMerelyCarriesTheLabelGboardIsNotHidden() {
        // matched by package, never by label: the AOSP keyboard stays whatever it is called
        assertEquals(Arrays.asList("Gboard"), KeyboardPolicy.visibleLabels(Arrays.asList("com.android.inputmethod.latin", "com.google.android.inputmethod.latin"), Arrays.asList("Gboard", "Gboard")));
    }
    @Test public void theQuickstepSideKeyboardsAreSwiftKeyAndGboardOnly() {
        assertTrue(KeyboardPolicy.isQuickstepSide("com.google.android.inputmethod.latin", "Gboard"));
        assertTrue(KeyboardPolicy.isQuickstepSide("com.touchtype.swiftkey", "Microsoft SwiftKey keyboard"));
        assertTrue(KeyboardPolicy.isQuickstepSide("com.other", "SwiftKey"));
        assertFalse(KeyboardPolicy.isQuickstepSide("com.android.inputmethod.latin", "Android keyboard (AOSP)"));
        assertFalse(KeyboardPolicy.isQuickstepSide("com.android.inputmethod.latin", "Gboard"));
        assertFalse(KeyboardPolicy.isQuickstepSide(null, null));
    }
    @Test public void aMissingPackageListOrBlankLabelsAreHandled() {
        assertEquals(Arrays.asList("Gboard"), KeyboardPolicy.visibleLabels(null, Arrays.asList("Gboard", "Microsoft SwiftKey keyboard", " ", null)));
        assertEquals(0, KeyboardPolicy.visibleLabels(Arrays.asList("x"), null).size());
    }

    // ---- CH1e (Tj, 2026-11-18): "Make an option in the launcher settings to choose between the stock Android keyboard, gboard, or swoop for the quickstep launcher"
    private static final KeyboardPolicy.Ime SWOOP = new KeyboardPolicy.Ime("app.swoop.keyboard/.KeysService", "app.swoop.keyboard", false);
    private static final KeyboardPolicy.Ime SWOOP_DEBUG = new KeyboardPolicy.Ime("app.swoop.keyboard.debug/app.swoop.keyboard.KeysService", "app.swoop.keyboard.debug", false);

    @Test public void theDefaultChoiceIsGboardAndTheOldRuleIsUnchanged() {
        assertEquals(KeyboardPolicy.CHOICE_GBOARD, KeyboardPolicy.normalizeChoice(null));
        assertEquals(KeyboardPolicy.CHOICE_GBOARD, KeyboardPolicy.normalizeChoice(""));
        assertEquals(KeyboardPolicy.CHOICE_GBOARD, KeyboardPolicy.normalizeChoice("SWOOP"));   // a damaged value changes nothing
        assertEquals(KeyboardPolicy.CHOICE_STOCK, KeyboardPolicy.normalizeChoice("stock"));
        assertEquals(KeyboardPolicy.CHOICE_SWOOP, KeyboardPolicy.normalizeChoice("swoop"));
        List<KeyboardPolicy.Ime> all = Arrays.asList(LATIN, GBOARD_APP, SWOOP, SWIFT);
        for (String orig : new String[] { null, LATIN.id, SWIFT.id, "gone/.Ime" }) {
            assertEquals(KeyboardPolicy.target(Mode.QUICKSTEP, all, orig), KeyboardPolicy.target(Mode.QUICKSTEP, all, orig, KeyboardPolicy.CHOICE_GBOARD));
            assertEquals(KeyboardPolicy.target(Mode.QUICKSTEP, all, orig), KeyboardPolicy.target(Mode.QUICKSTEP, all, orig, "garbage"));
        }
    }
    @Test public void quickstepWithTheStockChoiceUsesTheStockKeyboard() {
        assertEquals(LATIN.id, KeyboardPolicy.target(Mode.QUICKSTEP, Arrays.asList(GBOARD_APP, SWOOP, LATIN), GBOARD_APP.id, KeyboardPolicy.CHOICE_STOCK));
        assertNull("no enabled stock keyboard: left as is, not Gboard", KeyboardPolicy.target(Mode.QUICKSTEP, Arrays.asList(GBOARD_APP, SWOOP), GBOARD_APP.id, KeyboardPolicy.CHOICE_STOCK));
    }
    @Test public void quickstepWithTheSwoopChoiceUsesSwoopAndNothingElse() {
        assertEquals(SWOOP.id, KeyboardPolicy.target(Mode.QUICKSTEP, Arrays.asList(LATIN, GBOARD_APP, SWOOP), LATIN.id, KeyboardPolicy.CHOICE_SWOOP));
        assertEquals("the release build wins over a debug build", SWOOP.id, KeyboardPolicy.target(Mode.QUICKSTEP, Arrays.asList(SWOOP_DEBUG, SWOOP), null, KeyboardPolicy.CHOICE_SWOOP));
        assertEquals("a debug build alone is used", SWOOP_DEBUG.id, KeyboardPolicy.target(Mode.QUICKSTEP, Arrays.asList(LATIN, SWOOP_DEBUG), null, KeyboardPolicy.CHOICE_SWOOP));
        assertNull("Swoop not enabled: left as is (never silently Gboard or the original)", KeyboardPolicy.target(Mode.QUICKSTEP, Arrays.asList(LATIN, GBOARD_APP), LATIN.id, KeyboardPolicy.CHOICE_SWOOP));
        assertEquals(SWOOP.id, KeyboardPolicy.pickSwoop(Arrays.asList(OTHER, SWOOP)));
        assertNull(KeyboardPolicy.pickSwoop(Arrays.<KeyboardPolicy.Ime>asList()));
    }
    @Test public void theCustomLauncherKeepsTheStockKeyboardWhateverTheChoice() {
        List<KeyboardPolicy.Ime> all = Arrays.asList(SWOOP, GBOARD_APP, LATIN);
        for (String choice : new String[] { KeyboardPolicy.CHOICE_STOCK, KeyboardPolicy.CHOICE_GBOARD, KeyboardPolicy.CHOICE_SWOOP, null, "x" })
            assertEquals(LATIN.id, KeyboardPolicy.target(Mode.CUSTOM, all, SWOOP.id, choice));
    }
    @Test public void swoopIsRecognisedByItsPackageOnly() {
        assertTrue(KeyboardPolicy.isSwoop("app.swoop.keyboard"));
        assertTrue(KeyboardPolicy.isSwoop("app.swoop.keyboard.debug"));
        assertFalse(KeyboardPolicy.isSwoop("app.swoop.keyboard.other"));
        assertFalse(KeyboardPolicy.isSwoop("com.swoop.keyboard"));
        assertFalse(KeyboardPolicy.isSwoop(null));
        assertFalse(KeyboardPolicy.isSwoop("com.google.android.inputmethod.latin"));
    }
    @Test public void theStatusLineNamesTheKeyboardThatWasAskedFor() {
        assertEquals("Swoop is not an enabled keyboard (enabled: com.android.inputmethod.latin): left as is. Turn it on under Virtual keyboard > Manage keyboards.",
                KeyboardPolicy.noTargetStatus(Mode.QUICKSTEP, Arrays.asList(LATIN), KeyboardPolicy.CHOICE_SWOOP));
        assertEquals("no enabled stock keyboard found: left as is", KeyboardPolicy.noTargetStatus(Mode.QUICKSTEP, Arrays.asList(SWOOP), KeyboardPolicy.CHOICE_STOCK));
        assertEquals("no enabled stock keyboard found: left as is", KeyboardPolicy.noTargetStatus(Mode.CUSTOM, Arrays.asList(SWOOP), KeyboardPolicy.CHOICE_SWOOP));
        assertEquals(KeyboardPolicy.noTargetStatus(Mode.QUICKSTEP, Arrays.asList(LATIN)), KeyboardPolicy.noTargetStatus(Mode.QUICKSTEP, Arrays.asList(LATIN), KeyboardPolicy.CHOICE_GBOARD));
        assertEquals("Gboard", KeyboardPolicy.choiceName(null));
        assertEquals("Swoop", KeyboardPolicy.choiceName("swoop"));
        assertEquals("the stock keyboard", KeyboardPolicy.choiceName("stock"));
    }
    @Test public void theSettingsLookAlikeDoesNotNameSwoopEither() {
        assertTrue(KeyboardPolicy.isQuickstepSide("app.swoop.keyboard", "Swoop"));
        assertTrue(KeyboardPolicy.isQuickstepSide("app.swoop.keyboard.debug", "Swoop"));
        assertFalse(KeyboardPolicy.isQuickstepSide("com.android.inputmethod.latin", "Android keyboard (AOSP)"));
        assertEquals(Arrays.asList("Android keyboard (AOSP)"), KeyboardPolicy.visibleLabels(Arrays.asList("app.swoop.keyboard", "com.android.inputmethod.latin"), Arrays.asList("Swoop", "Android keyboard (AOSP)")));
        assertEquals("when Swoop is the one and only keyboard the row names it instead of going blank", Arrays.asList("Swoop"),
                KeyboardPolicy.visibleLabels(Arrays.asList("app.swoop.keyboard"), Arrays.asList("Swoop")));
    }
}
