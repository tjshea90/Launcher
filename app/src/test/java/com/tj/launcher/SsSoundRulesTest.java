package com.tj.launcher;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

/**
 * BN1: the Sound page rules. Every expected number is written down independently of the code: the stream numbers, ringer modes, filters, policy bits, ringtone types and request codes are the Android 10
 * API constants (AudioManager.STREAM_*, RINGER_MODE_*, ADJUST_*, NotificationManager.INTERRUPTION_FILTER_* / Policy.PRIORITY_CATEGORY_*, RingtoneManager.TYPE_*); the slider positions come from the
 * measurements on Tj's Sound screenshot (600 x 1024 px, the thumb centre of a slider runs from x = 77 to x = 566); the behaviours are those of the platform's SeekBarVolumizer (android-10.0.0_r47).
 */
public class SsSoundRulesTest {
    // AudioManager.STREAM_*
    private static final int VOICE_CALL = 0, RING = 2, MUSIC = 3, ALARM = 4, NOTIFICATION = 5, SCO = 6;
    // AudioManager.RINGER_MODE_*
    private static final int SILENT = 0, VIBRATE = 1, NORMAL = 2;
    // NotificationManager.INTERRUPTION_FILTER_*
    private static final int FILTER_UNKNOWN = 0, ALL = 1, PRIORITY = 2, NONE = 3, ALARMS = 4;

    // ------------------------------------------------------------------ rows
    @Test public void rowsAreTheFourOfTjsScreenshotInOrder() {
        assertArrayEquals(new String[] { "Media volume", "Call volume", "Alarm volume", "Notification volume" }, SsSoundRules.TITLES);
        assertEquals(0, SsSoundRules.ROW_MEDIA);
        assertEquals(1, SsSoundRules.ROW_CALL);
        assertEquals(2, SsSoundRules.ROW_ALARM);
        assertEquals(3, SsSoundRules.ROW_NOTIFICATION);
    }

    @Test public void eachRowControlsItsOwnStream() {
        assertEquals(MUSIC, SsSoundRules.streamFor(0, false));
        assertEquals(VOICE_CALL, SsSoundRules.streamFor(1, false));
        assertEquals(ALARM, SsSoundRules.streamFor(2, false));
        assertEquals(NOTIFICATION, SsSoundRules.streamFor(3, false));
    }

    @Test public void onlyTheCallRowFollowsTheBluetoothLink() {
        assertEquals(SCO, SsSoundRules.streamFor(1, true));
        assertEquals(MUSIC, SsSoundRules.streamFor(0, true));
        assertEquals(ALARM, SsSoundRules.streamFor(2, true));
        assertEquals(NOTIFICATION, SsSoundRules.streamFor(3, true));
    }

    @Test public void aSingleVolumeDeviceShowsOnlyMedia() {
        assertArrayEquals(new int[] { 0, 1, 2, 3 }, SsSoundRules.visibleRows(false));
        assertArrayEquals(new int[] { 0 }, SsSoundRules.visibleRows(true));
    }

    @Test public void ringAndNotificationAreTheRingTypeStreams() {
        assertTrue(SsSoundRules.isNotificationOrRing(RING));
        assertTrue(SsSoundRules.isNotificationOrRing(NOTIFICATION));
        assertFalse(SsSoundRules.isNotificationOrRing(MUSIC));
        assertFalse(SsSoundRules.isNotificationOrRing(ALARM));
        assertFalse(SsSoundRules.isNotificationOrRing(VOICE_CALL));
        assertFalse(SsSoundRules.isNotificationOrRing(SCO));
    }

    // ------------------------------------------------------------------ slider position
    @Test public void aVolumeIsClampedIntoTheStreamRange() {
        assertEquals(12, SsSoundRules.clampVolume(12, 1, 15));
        assertEquals(1, SsSoundRules.clampVolume(0, 1, 15));     // the alarm stream never goes below 1
        assertEquals(1, SsSoundRules.clampVolume(-4, 1, 15));
        assertEquals(15, SsSoundRules.clampVolume(16, 1, 15));
        assertEquals(0, SsSoundRules.clampVolume(0, 0, 15));
        assertEquals(15, SsSoundRules.clampVolume(99, 0, 15));
        assertEquals(7, SsSoundRules.clampVolume(5, 7, 3));      // a broken range (max below min) pins to the minimum
    }

    /** The thumb centre x of a slider whose bar runs from x = 77 (lowest volume) to x = 566 (highest): the numbers measured on the screenshot. */
    private static double thumbX(int shown, int min, int max) { return 77 + (shown - min) * (566.0 - 77.0) / (max - min); }

    @Test public void theThumbsLandWhereTheScreenshotShowsThem() {
        // Alarm: range 1..15, the thumb centre was measured at x = 460, which is volume 12 (11 of 14 steps)
        int alarm = SsSoundRules.shownVolume(ALARM, 12, 12, false, false, NORMAL, 1, 15);
        assertEquals(12, alarm);
        assertEquals(460, thumbX(alarm, 1, 15), 2.0);
        // Notification: range 0..15, centre at x = 272, which is volume 6 (6 of 15 steps)
        int notification = SsSoundRules.shownVolume(NOTIFICATION, 6, 6, false, false, NORMAL, 0, 15);
        assertEquals(6, notification);
        assertEquals(272, thumbX(notification, 0, 15), 2.0);
        // Media and Call sit at the far left (centre 77): media volume 0, call volume at its minimum 1
        assertEquals(77, thumbX(SsSoundRules.shownVolume(MUSIC, 0, 0, false, false, NORMAL, 0, 15), 0, 15), 0.5);
        assertEquals(77, thumbX(SsSoundRules.shownVolume(VOICE_CALL, 1, 1, false, false, NORMAL, 1, 5), 1, 5), 0.5);
    }

    @Test public void aPlainStreamShowsItsVolume() {
        assertEquals(9, SsSoundRules.shownVolume(MUSIC, 9, 9, false, false, NORMAL, 0, 15));
        assertEquals(3, SsSoundRules.shownVolume(ALARM, 3, 3, false, false, NORMAL, 1, 7));
    }

    @Test public void aMutedStreamShowsTheLowestPosition() {
        assertEquals(0, SsSoundRules.shownVolume(MUSIC, 9, 9, true, false, NORMAL, 0, 15));
        assertEquals(1, SsSoundRules.shownVolume(ALARM, 9, 9, true, false, NORMAL, 1, 15));   // 0 clamped to the stream's minimum
    }

    @Test public void vibrateShowsZeroOnTheRingTypeStreamsOnly() {
        assertEquals(0, SsSoundRules.shownVolume(NOTIFICATION, 6, 6, false, false, VIBRATE, 0, 15));
        assertEquals(0, SsSoundRules.shownVolume(RING, 6, 6, false, false, VIBRATE, 0, 7));
        assertEquals(6, SsSoundRules.shownVolume(MUSIC, 6, 6, false, false, VIBRATE, 0, 15));
        assertEquals(6, SsSoundRules.shownVolume(ALARM, 6, 6, false, false, VIBRATE, 1, 15));
        assertEquals(6, SsSoundRules.shownVolume(NOTIFICATION, 6, 6, false, false, SILENT, 0, 15));   // silent: no rule of its own, the stream's own mute decides
    }

    @Test public void doNotDisturbShowsTheLastAudibleVolumeAndBeatsEverythingElse() {
        // the audio service reports 0 for a silenced stream; the slider shows what the stream had before
        assertEquals(12, SsSoundRules.shownVolume(ALARM, 0, 12, true, true, NORMAL, 1, 15));
        assertEquals(6, SsSoundRules.shownVolume(NOTIFICATION, 0, 6, true, true, VIBRATE, 0, 15));
        assertEquals(15, SsSoundRules.shownVolume(MUSIC, 0, 40, false, true, NORMAL, 0, 15));    // clamped into the range
        assertEquals(1, SsSoundRules.shownVolume(ALARM, 0, 0, true, true, NORMAL, 1, 15));
    }

    // ------------------------------------------------------------------ writing a volume
    @Test public void theMuteAdjustmentsAreTheAudioManagerOnes() {
        assertEquals(-100, SsSoundRules.ADJUST_MUTE);
        assertEquals(0, SsSoundRules.ADJUST_SAME);
        assertEquals(100, SsSoundRules.ADJUST_UNMUTE);
    }

    @Test public void draggingToZeroMutesAndDraggingAboveZeroUnmutes() {
        assertEquals(SsSoundRules.ADJUST_MUTE, SsSoundRules.muteAdjustment(false, 0));
        assertEquals(SsSoundRules.ADJUST_UNMUTE, SsSoundRules.muteAdjustment(true, 1));
        assertEquals(SsSoundRules.ADJUST_UNMUTE, SsSoundRules.muteAdjustment(true, 15));
        assertEquals(SsSoundRules.ADJUST_SAME, SsSoundRules.muteAdjustment(true, 0));    // already muted at 0
        assertEquals(SsSoundRules.ADJUST_SAME, SsSoundRules.muteAdjustment(false, 7));   // an ordinary move
        assertEquals(SsSoundRules.ADJUST_SAME, SsSoundRules.muteAdjustment(false, 1));
    }

    // ------------------------------------------------------------------ icons
    @Test public void ringerModeNumbersAreTheAudioManagerOnes() {
        assertEquals(0, SsSoundRules.RINGER_SILENT);
        assertEquals(1, SsSoundRules.RINGER_VIBRATE);
        assertEquals(2, SsSoundRules.RINGER_NORMAL);
    }

    @Test public void mediaAlarmAndCallShowTheMuteGlyphWhenMuted() {
        for (int row : new int[] { SsSoundRules.ROW_MEDIA, SsSoundRules.ROW_CALL, SsSoundRules.ROW_ALARM }) {
            assertEquals(SsSoundRules.ICON_NORMAL, SsSoundRules.iconKind(row, NORMAL, false, false));
            assertEquals(SsSoundRules.ICON_MUTED, SsSoundRules.iconKind(row, NORMAL, true, false));
            assertEquals(SsSoundRules.ICON_NORMAL, SsSoundRules.iconKind(row, NORMAL, true, true));   // muted by Do Not Disturb: the plain glyph, the slider is just disabled
            assertEquals(SsSoundRules.ICON_NORMAL, SsSoundRules.iconKind(row, VIBRATE, false, false)); // the ringer mode is not theirs
        }
    }

    @Test public void theNotificationGlyphFollowsOnlyTheRingerMode() {
        assertEquals(SsSoundRules.ICON_NORMAL, SsSoundRules.iconKind(SsSoundRules.ROW_NOTIFICATION, NORMAL, false, false));
        assertEquals(SsSoundRules.ICON_RINGER_VIBRATE, SsSoundRules.iconKind(SsSoundRules.ROW_NOTIFICATION, VIBRATE, false, false));
        assertEquals(SsSoundRules.ICON_RINGER_SILENT, SsSoundRules.iconKind(SsSoundRules.ROW_NOTIFICATION, SILENT, false, false));
        assertEquals(SsSoundRules.ICON_NORMAL, SsSoundRules.iconKind(SsSoundRules.ROW_NOTIFICATION, NORMAL, true, false));   // a muted flag alone changes nothing
        assertEquals(SsSoundRules.ICON_NORMAL, SsSoundRules.iconKind(SsSoundRules.ROW_NOTIFICATION, NORMAL, true, true));
        assertEquals(SsSoundRules.ICON_RINGER_VIBRATE, SsSoundRules.iconKind(SsSoundRules.ROW_NOTIFICATION, VIBRATE, true, true));
    }

    // ------------------------------------------------------------------ Do Not Disturb
    @Test public void interruptionFilterNumbersAreTheNotificationManagerOnes() {
        assertEquals(1, SsSoundRules.FILTER_ALL);
        assertEquals(2, SsSoundRules.FILTER_PRIORITY);
        assertEquals(3, SsSoundRules.FILTER_NONE);
        assertEquals(4, SsSoundRules.FILTER_ALARMS);
    }

    @Test public void totalSilenceMutesEveryStream() {
        for (int s : new int[] { VOICE_CALL, RING, MUSIC, ALARM, NOTIFICATION, SCO }) assertTrue("stream " + s, SsSoundRules.zenMuted(NONE, s, true, true, true));
    }

    @Test public void alarmsOnlyMutesTheRingTypeStreams() {
        assertTrue(SsSoundRules.zenMuted(ALARMS, NOTIFICATION, true, true, true));
        assertTrue(SsSoundRules.zenMuted(ALARMS, RING, true, true, true));
        assertFalse(SsSoundRules.zenMuted(ALARMS, MUSIC, true, true, true));
        assertFalse(SsSoundRules.zenMuted(ALARMS, ALARM, true, true, true));
        assertFalse(SsSoundRules.zenMuted(ALARMS, VOICE_CALL, true, true, true));
    }

    @Test public void priorityOnlyMutesWhatThePolicyDoesNotAllow() {
        // alarms not allowed: only the alarm stream
        assertTrue(SsSoundRules.zenMuted(PRIORITY, ALARM, false, true, true));
        assertFalse(SsSoundRules.zenMuted(PRIORITY, MUSIC, false, true, true));
        assertFalse(SsSoundRules.zenMuted(PRIORITY, NOTIFICATION, false, true, true));
        // media not allowed: only the music stream
        assertTrue(SsSoundRules.zenMuted(PRIORITY, MUSIC, true, false, true));
        assertFalse(SsSoundRules.zenMuted(PRIORITY, ALARM, true, false, true));
        // nothing that rings allowed: notification and ring
        assertTrue(SsSoundRules.zenMuted(PRIORITY, NOTIFICATION, true, true, false));
        assertTrue(SsSoundRules.zenMuted(PRIORITY, RING, true, true, false));
        assertFalse(SsSoundRules.zenMuted(PRIORITY, MUSIC, true, true, false));
        // everything allowed: nothing
        for (int s : new int[] { VOICE_CALL, RING, MUSIC, ALARM, NOTIFICATION }) assertFalse("stream " + s, SsSoundRules.zenMuted(PRIORITY, s, true, true, true));
        // the call stream is never muted by priority-only
        assertFalse(SsSoundRules.zenMuted(PRIORITY, VOICE_CALL, false, false, false));
    }

    @Test public void withDoNotDisturbOffNothingIsMuted() {
        for (int s : new int[] { VOICE_CALL, RING, MUSIC, ALARM, NOTIFICATION }) {
            assertFalse(SsSoundRules.zenMuted(ALL, s, false, false, false));
            assertFalse(SsSoundRules.zenMuted(FILTER_UNKNOWN, s, false, false, false));
        }
    }

    @Test public void policyCategoriesAreTheNotificationManagerBits() {
        assertEquals(1, SsSoundRules.CATEGORY_REMINDERS);
        assertEquals(2, SsSoundRules.CATEGORY_EVENTS);
        assertEquals(4, SsSoundRules.CATEGORY_MESSAGES);
        assertEquals(8, SsSoundRules.CATEGORY_CALLS);
        assertEquals(16, SsSoundRules.CATEGORY_REPEAT_CALLERS);
        assertEquals(32, SsSoundRules.CATEGORY_ALARMS);
        assertEquals(64, SsSoundRules.CATEGORY_MEDIA);
    }

    @Test public void alarmsAndMediaAreAllowedByTheirOwnBitOnly() {
        assertTrue(SsSoundRules.allowsAlarms(32));
        assertTrue(SsSoundRules.allowsAlarms(32 | 64 | 1));
        assertFalse(SsSoundRules.allowsAlarms(31 | 64 | 128));
        assertFalse(SsSoundRules.allowsAlarms(0));
        assertTrue(SsSoundRules.allowsMedia(64));
        assertTrue(SsSoundRules.allowsMedia(64 | 32));
        assertFalse(SsSoundRules.allowsMedia(63 | 128));
        assertFalse(SsSoundRules.allowsMedia(0));
    }

    @Test public void theRingTypeStreamsStayAudibleWhenAnyOfTheFiveCategoriesIsAllowed() {
        for (int bit : new int[] { 1, 2, 4, 8, 16 }) assertTrue("bit " + bit, SsSoundRules.allowsRinger(bit));
        assertTrue(SsSoundRules.allowsRinger(31));
        assertFalse(SsSoundRules.allowsRinger(0));
        assertFalse(SsSoundRules.allowsRinger(32));            // alarms
        assertFalse(SsSoundRules.allowsRinger(64));            // media
        assertFalse(SsSoundRules.allowsRinger(128));           // system sounds
        assertFalse(SsSoundRules.allowsRinger(32 | 64 | 128));
    }

    // ------------------------------------------------------------------ ringtone rows
    @Test public void aRingtoneRowShowsTheTitleOfItsSound() {
        assertEquals("Cesium", SsSoundRules.ringtoneSummary(true, "Cesium"));
        assertEquals("On The Hunt", SsSoundRules.ringtoneSummary(true, "On The Hunt"));
    }

    @Test public void aSilentChoiceReadsNone() {
        assertEquals("None", SsSoundRules.ringtoneSummary(false, null));
        assertEquals("None", SsSoundRules.ringtoneSummary(false, "Cesium"));   // no sound wins over a stale title
    }

    @Test public void aSoundWithoutAnameReadsUnknown() {
        assertEquals("Unknown", SsSoundRules.ringtoneSummary(true, null));
        assertEquals("Unknown", SsSoundRules.ringtoneSummary(true, ""));
        assertEquals("Unknown", SsSoundRules.ringtoneSummary(true, "   "));
    }

    @Test public void thePlaceholderIsASingleNoBreakSpace() {
        assertEquals(1, SsSoundRules.PLACEHOLDER.length());
        assertEquals(0xA0, SsSoundRules.PLACEHOLDER.charAt(0));
    }

    @Test public void theRowTitlesAreAospsStrings() {
        assertEquals("Default notification sound", SsSoundRules.TITLE_NOTIFICATION_SOUND);
        assertEquals("Default alarm sound", SsSoundRules.TITLE_ALARM_SOUND);
    }

    @Test public void theRequestCodeCarriesTheRingtoneType() {
        assertEquals(2, SsSoundRules.TYPE_NOTIFICATION);   // RingtoneManager.TYPE_NOTIFICATION
        assertEquals(4, SsSoundRules.TYPE_ALARM);          // RingtoneManager.TYPE_ALARM
        assertEquals(202, SsSoundRules.requestCode(SsSoundRules.TYPE_NOTIFICATION));
        assertEquals(204, SsSoundRules.requestCode(SsSoundRules.TYPE_ALARM));
        assertEquals(2, SsSoundRules.typeForRequest(202));
        assertEquals(4, SsSoundRules.typeForRequest(204));
    }

    @Test public void aRequestCodeThatIsNotOursIsNotAType() {
        for (int code : new int[] { 0, 1, 200, 201, 203, 205, 1000, -1 }) assertEquals("code " + code, -1, SsSoundRules.typeForRequest(code));
    }

    // ------------------------------------------------------------------ switches
    @Test public void theSwitchesWriteAospsKeys() {
        assertEquals("lockscreen_sounds_enabled", SsSoundRules.KEY_LOCKSCREEN_SOUNDS);
        assertEquals("charging_sounds_enabled", SsSoundRules.KEY_CHARGING_SOUNDS);
        assertEquals("sound_effects_enabled", SsSoundRules.KEY_TOUCH_SOUNDS);
    }

    @Test public void aMissingSwitchValueMeansOn() {
        assertEquals(1, SsSoundRules.DEFAULT_ON);
        assertTrue(SsSoundRules.isOn(SsSoundRules.DEFAULT_ON));
    }

    @Test public void anySwitchValueButZeroIsOn() {
        assertFalse(SsSoundRules.isOn(0));
        assertTrue(SsSoundRules.isOn(1));
        assertTrue(SsSoundRules.isOn(2));
        assertTrue(SsSoundRules.isOn(-1));
        assertEquals(1, SsSoundRules.valueFor(true));
        assertEquals(0, SsSoundRules.valueFor(false));
    }

    // ------------------------------------------------------------------ what to watch
    @Test public void changeKindSaysWhatASettingMeansForThePage() {
        assertEquals(1 | 2 | 4, SsSoundRules.changeKind(null));                          // unknown: read everything
        assertEquals(SsSoundRules.CHANGE_VOLUME, SsSoundRules.changeKind("volume_music_speaker"));
        assertEquals(SsSoundRules.CHANGE_VOLUME, SsSoundRules.changeKind("volume_alarm_headset"));
        assertEquals(SsSoundRules.CHANGE_VOLUME, SsSoundRules.changeKind("volume_ring"));
        assertEquals(SsSoundRules.CHANGE_RINGTONE, SsSoundRules.changeKind("notification_sound"));
        assertEquals(SsSoundRules.CHANGE_RINGTONE, SsSoundRules.changeKind("alarm_alert"));
        assertEquals(SsSoundRules.CHANGE_SWITCH, SsSoundRules.changeKind("lockscreen_sounds_enabled"));
        assertEquals(SsSoundRules.CHANGE_SWITCH, SsSoundRules.changeKind("charging_sounds_enabled"));
        assertEquals(SsSoundRules.CHANGE_SWITCH, SsSoundRules.changeKind("sound_effects_enabled"));
    }

    @Test public void otherSettingsMeanNothing() {
        assertEquals(0, SsSoundRules.changeKind("screen_brightness"));
        assertEquals(0, SsSoundRules.changeKind("ringtone"));
        assertEquals(0, SsSoundRules.changeKind("volume"));        // not a per-device volume name
        assertEquals(0, SsSoundRules.changeKind("mode_ringer_streams_affected"));
        assertEquals(0, SsSoundRules.changeKind(""));
    }

    @Test public void theWatchedBroadcastsAreTheOnesThatMoveASlider() {
        Set<String> got = new HashSet<>(Arrays.asList(SsSoundRules.WATCHED_ACTIONS));
        assertEquals(SsSoundRules.WATCHED_ACTIONS.length, got.size());   // no duplicates
        Set<String> want = new HashSet<>(Arrays.asList(
                "android.media.VOLUME_CHANGED_ACTION",
                "android.media.STREAM_MUTE_CHANGED_ACTION",
                "android.media.MASTER_MUTE_CHANGED_ACTION",
                "android.media.RINGER_MODE_CHANGED",
                "android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION",
                "android.media.STREAM_DEVICES_CHANGED_ACTION",
                "android.app.action.INTERRUPTION_FILTER_CHANGED",
                "android.app.action.NOTIFICATION_POLICY_CHANGED"));
        assertEquals(want, got);
    }

    // ------------------------------------------------------------------ release sample
    @Test public void mediaPlaysTheBundledSampleAndTheOthersTheirDefaultSound() {
        assertEquals(SsSoundRules.SAMPLE_BUNDLED, SsSoundRules.sampleSource(MUSIC));
        assertEquals(SsSoundRules.SAMPLE_RINGTONE, SsSoundRules.sampleSource(RING));
        assertEquals(SsSoundRules.SAMPLE_NOTIFICATION, SsSoundRules.sampleSource(NOTIFICATION));
        assertEquals(SsSoundRules.SAMPLE_ALARM, SsSoundRules.sampleSource(ALARM));
        assertEquals(SsSoundRules.SAMPLE_ALARM, SsSoundRules.sampleSource(VOICE_CALL));   // the stock call slider plays the alarm sound
        assertEquals(SsSoundRules.SAMPLE_ALARM, SsSoundRules.sampleSource(SCO));
    }

    @Test public void theFourSampleKindsAreDistinct() {
        Set<Integer> kinds = new HashSet<>(Arrays.asList(SsSoundRules.SAMPLE_BUNDLED, SsSoundRules.SAMPLE_RINGTONE, SsSoundRules.SAMPLE_NOTIFICATION, SsSoundRules.SAMPLE_ALARM));
        assertEquals(4, kinds.size());
    }

    @Test public void aSampleIsCutOffAfterTwoSeconds() {
        assertEquals(2000, SsSoundRules.SAMPLE_CUTOFF_MS);
    }

    @Test public void aReleaseWhileTheSamplePlaysWaitsASecond() {
        assertEquals(1000, SsSoundRules.SAMPLE_RECHECK_MS);
        assertEquals(0, SsSoundRules.sampleStartDelayMs(false));
        assertEquals(1000, SsSoundRules.sampleStartDelayMs(true));
    }
}
