package com.tj.launcher;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import com.android.launcher3.R;

/**
 * BN1: the one activity class of the Settings look-alike; the intent extra {@link #EXTRA_PAGE} names the {@link SsPage} to show (a sub page is another instance of this class on top of the same task, so
 * Back works like in the real Settings). Reached from the custom drawer through the manifest alias {@link SettingsSpoof#ALIAS}; not exported itself. The chrome is the platform's own: the theme
 * Theme.DeviceDefault.Settings gives the solid toolbar, the back arrow and the status bar; the home page has no toolbar (no search card either: Tj wants no search anywhere).
 * An open page closes itself on resume when the look-alike is not wanted any more (Quickstep session, switch off): a stale task must not show our pages next to the real Settings.
 */
public class SsPageActivity extends Activity {
    public static final String EXTRA_PAGE = "ss_page";
    private SsPage page = SsPage.HOME;

    public static Intent intent(Context c, SsPage p) { return new Intent(c, SsPageActivity.class).putExtra(EXTRA_PAGE, p.id); }

    @Override protected void onCreate(Bundle b) {
        Intent in = getIntent();
        page = SsPage.fromId(in == null ? null : in.getStringExtra(EXTRA_PAGE));   // no extra = the entry through the alias = the home page
        setTheme(page == SsPage.HOME ? R.style.SsTheme_Home : R.style.SsTheme);   // before super.onCreate: the window decor is not installed yet
        super.onCreate(b);
        View content = SsPages.build(this, page);
        setContentView(content);
        ActionBar bar = getActionBar();
        if (bar != null && page != SsPage.HOME) {
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setTitle(page.title);
        }
        setTitle(page.title);
    }

    @Override protected void onResume() {
        super.onResume();
        if (!SettingsSpoof.pageAllowed(this)) finish();
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
