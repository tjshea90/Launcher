package com.tj.launcher;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import java.util.Objects;

/**
 * BN1: opens the system ringtone picker for the Sound page's two sound rows and applies its answer. A headless platform Fragment, because {@link SsPageActivity} has no onActivityResult and the
 * platform routes a fragment's startActivityForResult back to that fragment (Activity.dispatchActivityResult finds it by its "who"), so the activity needs no change. The fragment keeps no state:
 * the ringtone type is part of the request code ({@link SsSoundRules#requestCode}) and the page learns about the new sound from its observer on the system setting, so a recreated activity
 * (rotation, process death while the picker is open) still gets the answer applied. Public class with a public no-argument constructor: the FragmentManager re-creates it by name.
 * What AOSP 10 does (SoundSettings.onPreferenceTreeClick -> DefaultRingtonePreference.onPrepareRingtonePickerIntent / onActivityResult -> onSaveRingtone): the picker is the public
 * ACTION_RINGTONE_PICKER (com.android.providers.media), the current sound is preselected, "None" is offered, the "Default" entry is not; the answer is written by
 * RingtoneManager.setActualDefaultRingtoneUri (needs WRITE_SETTINGS or the adb-granted WRITE_SECURE_SETTINGS, like every system setting the look-alike changes).
 */
public final class SsRingtoneFragment extends Fragment {
    static final String TAG = "ss_ringtone_result";

    /** RingtoneManager.EXTRA_RINGTONE_AUDIO_ATTRIBUTES_FLAGS (hidden constant): lets the picker's preview sound through Do Not Disturb, as it does for Settings. */
    private static final String EXTRA_AUDIO_ATTRIBUTES_FLAGS = "android.intent.extra.ringtone.AUDIO_ATTRIBUTES_FLAGS";
    private static final int FLAG_BYPASS_INTERRUPTION_POLICY = 1 << 6;   // AudioAttributes.FLAG_BYPASS_INTERRUPTION_POLICY (hidden)

    /** Adds the fragment to the activity once; a recreated activity gets the old instance back from its FragmentManager. Never throws. */
    static void attach(Activity a) {
        try {
            FragmentManager fm = a.getFragmentManager();
            if (fm.findFragmentByTag(TAG) == null) fm.beginTransaction().add(new SsRingtoneFragment(), TAG).commitAllowingStateLoss();
        } catch (Throwable ignored) { }
    }

    /** Opens the picker for a ringtone type ({@code RingtoneManager.TYPE_NOTIFICATION} or {@code TYPE_ALARM}); false when it cannot be opened (no picker on this device, no fragment). Call on the main thread. */
    static boolean pick(Activity a, int type, String title) {
        try {
            Uri existing = null;
            try { existing = RingtoneManager.getActualDefaultRingtoneUri(a, type); } catch (Throwable ignored) { }   // the row is preselected; null = "None" is
            FragmentManager fm = a.getFragmentManager();
            Fragment f = fm.findFragmentByTag(TAG);
            if (f == null) {
                f = new SsRingtoneFragment();
                fm.beginTransaction().add(f, TAG).commitAllowingStateLoss();
                fm.executePendingTransactions();
            } else if (!f.isAdded()) {
                fm.executePendingTransactions();
            }
            f.startActivityForResult(pickerIntent(type, title, existing), SsSoundRules.requestCode(type));
            return true;
        } catch (Throwable t) {   // ActivityNotFoundException (no picker), IllegalStateException (fragment not attached)
            return false;
        }
    }

    /** The extras of DefaultRingtonePreference.onPrepareRingtonePickerIntent, in the same order and with the same values (SHOW_DEFAULT is switched off after the default Uri was added). */
    private static Intent pickerIntent(int type, String title, Uri existing) {
        Intent i = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(type));
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, type);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, title);
        i.putExtra(EXTRA_AUDIO_ATTRIBUTES_FLAGS, FLAG_BYPASS_INTERRUPTION_POLICY);
        return i;
    }

    /**
     * The picker's answer. Cancel / Back: nothing changes (the picker answers RESULT_CANCELED without data). OK: the picked Uri, or none for the "None" entry, becomes the default sound. AOSP does not
     * look at the result code, only at "data != null"; requiring RESULT_OK as well can only make a stray answer harmless. The write goes through a short worker thread: the setting is stored first, then
     * RingtoneManager copies the sound into the settings cache, which is file work.
     */
    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        final int type = SsSoundRules.typeForRequest(requestCode);
        if (type < 0 || resultCode != Activity.RESULT_OK || data == null) return;
        Activity a = getActivity();
        if (a == null) return;
        final Context app = a.getApplicationContext();
        final Uri picked;
        try {
            picked = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        } catch (RuntimeException e) {
            return;   // review A #4: a third-party picker that answers with something that is not a Uri must not take the shared launcher process down
        }
        Thread t = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    RingtoneManager.setActualDefaultRingtoneUri(app, type, picked);
                } catch (Throwable e) {
                    // BN1l (Tj: "it only needs to look identical"): a refused write is silent, the page just shows what the system holds when the row is read again.
                    // (The setting is written first and the sound is copied into the settings cache afterwards: a refusal of the copy still leaves the new sound in place.)
                }
            }
        }, "ss-ringtone-save");
        t.start();
    }
}
