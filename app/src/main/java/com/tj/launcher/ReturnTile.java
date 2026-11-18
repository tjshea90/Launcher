package com.tj.launcher;

import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/** N4: quick-settings tile "Back to custom". Tapping it (in Quickstep) switches to the custom launcher; it collapses the panel and does nothing in the custom launcher. */
public class ReturnTile extends TileService {
    @Override public void onStartListening() {
        try {
            Tile t = getQsTile();
            if (t == null) return;
            t.setState(Mode.isCustom(this) ? Tile.STATE_UNAVAILABLE : Tile.STATE_ACTIVE);
            t.setLabel("Back to custom");
            t.updateTile();
        } catch (Throwable ignored) { }
    }

    @Override public void onClick() {
        try {
            if (Mode.isCustom(this) || !ReturnMethods.on(this, ReturnMethods.Method.TILE)) return;
            startActivityAndCollapse(new Intent(this, BackToCustomActivity.class).putExtra(BackToCustomActivity.EXTRA_VIA, PocketRules.VIA_TILE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Throwable t) { android.util.Log.e("TjBack", "tile failed", t); }
    }
}
