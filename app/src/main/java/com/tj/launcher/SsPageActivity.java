package com.tj.launcher;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import com.android.launcher3.R;
import java.util.ArrayList;
import java.util.List;

/**
 * BN1: the one activity class of the Settings look-alike; the intent extra {@link #EXTRA_PAGE} names the {@link SsPage} to show (a sub page is another instance of this class on top of the same task, so
 * Back works like in the real Settings). Reached from the custom drawer through the manifest alias {@link SettingsSpoof#ALIAS}; not exported itself. The chrome is the platform's own: the theme
 * Theme.DeviceDefault.Settings gives the solid toolbar, the back arrow and the status bar; the home page has no toolbar (no search card either: Tj wants no search anywhere).
 * An open page closes itself on resume when the look-alike is not wanted any more (Quickstep session, switch off): a stale task must not show our pages next to the real Settings.
 */
public class SsPageActivity extends Activity {
    public static final String EXTRA_PAGE = "ss_page";
    private SsPage page = SsPage.HOME;
    private final List<SsPages.Live> live = new ArrayList<>();
    private static final int MENU_BATTERY_USAGE = 1;

    public static Intent intent(Context c, SsPage p) { return new Intent(c, SsPageActivity.class).putExtra(EXTRA_PAGE, p.id); }

    @Override protected void onCreate(Bundle b) {
        Intent in = getIntent();
        String id = null;
        try { id = in == null ? null : in.getStringExtra(EXTRA_PAGE); } catch (RuntimeException ignored) { }   // review B #7: the alias is exported, so any app can send a bad Parcelable extra; that must show the home page, not blow SsFuse
        page = SsPage.fromId(id);   // no extra = the entry through the alias = the home page
        setTheme(page == SsPage.HOME ? R.style.SsTheme_Home : R.style.SsTheme);   // before super.onCreate: the window decor is not installed yet
        super.onCreate(b);
        if (!SettingsSpoof.pageAllowed(this)) { finish(); return; }   // review C #4: a tap on the alias while it is still enabled in a Quickstep session must not build the page first (onResume checks again)
        View content = SsPages.build(this, page, live);
        setContentView(content);
        ActionBar bar = getActionBar();
        if (bar != null && page != SsPage.HOME) {
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setTitle(page.title);
            if (page == SsPage.BATTERY) {   // the Battery page's toolbar is the header's gray and flat (AOSP: colorPrimaryDark, no elevation)
                bar.setBackgroundDrawable(new ColorDrawable(SsRows.themeColor(this, android.R.attr.colorPrimaryDark, 0xFFE0E0E0)));
                bar.setElevation(0);
            }
        }
        setTitle(page.title);
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        if (page == SsPage.BATTERY) menu.add(Menu.NONE, MENU_BATTERY_USAGE, Menu.NONE, "Battery usage").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);   // the only item, as in Tj's list
        return true;
    }

    @Override protected void onStart() {
        super.onStart();
        for (SsPages.Live l : live) l.start();
    }

    @Override protected void onStop() {
        for (SsPages.Live l : live) l.stop();
        super.onStop();
    }

    @Override protected void onResume() {
        super.onResume();
        if (!SettingsSpoof.pageAllowed(this)) finish();
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        if (item.getItemId() == MENU_BATTERY_USAGE) { startActivity(intent(this, SsPage.BATTERY_USAGE)); return true; }
        return super.onOptionsItemSelected(item);
    }
}
