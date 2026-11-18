package com.tj.launcher;

import android.app.Activity;
import android.os.Bundle;

/** N4: the "Back to custom" icon (an app icon that acts as the kill switch) and the target of the quick-settings tile. Invisible: switches and finishes. Does nothing in the custom launcher. */
public class BackToCustomActivity extends Activity {
    /** CH1: set by the quick-settings tile ("tile"); the drawer icon starts this activity without it. */
    public static final String EXTRA_VIA = "via";

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        try {
            if (!Mode.isCustom(this) && ReturnMethods.on(this, ReturnMethods.Method.ICON)) {
                String via = PocketRules.VIA_TILE.equals(getIntent().getStringExtra(EXTRA_VIA)) ? PocketRules.VIA_TILE : PocketRules.VIA_ICON;   // CH1: the tile says so; the drawer icon has no extra
                ProfileSession.returnToCustom(this, "icon", via);
            }
        } catch (Throwable t) { android.util.Log.e("TjBack", "return failed", t); }
        finish();
        overridePendingTransition(0, 0);
    }
}
