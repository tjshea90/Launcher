package com.tj.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Chooses which apps the CUSTOM launcher shows. Reachable only while the stock launcher (Quickstep) is on (Tj: "settings ... only visible and accessible when the quickstep launcher is on").
 * Checked = shown in the custom drawer and its search. Apps installed after the first run start unchecked ("new") until Tj checks them.
 */
public class HiddenAppsActivity extends Activity {
    private static final class Row { String pkg; String label; Drawable icon; }

    private final List<Row> rows = new ArrayList<>();
    private Set<String> shown = new HashSet<>();

    private String summary() {
        return "Apps shown in the custom launcher\nSwitch ON = shown in the drawer and its search (" + shown.size() + " of " + rows.size() + " shown). Newly installed apps start OFF until you switch them on.";
    }

    private void setAll(boolean on) {
        for (Row r : rows) {
            if (on == shown.contains(r.pkg)) continue;
            if (on) shown.add(r.pkg); else shown.remove(r.pkg);
            HiddenApps.setShown(this, r.pkg, on);
        }
        if (!Unified.unlockedHome(this)) HiddenApps.reloadLauncher(this);   // CV1: the unlocked profile shows every app whatever is chosen here; the custom profile is rebuilt when the mode flips back
    }

    @Override protected void onResume() { super.onResume(); RecentsGuard.excludeOurTasks(this); RecentsGuard.describeAsTarget(this); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Mode.isCustom(this)) { finish(); return; }   // locked into the custom launcher: this screen only exists in Quickstep mode
        PackageManager pm = getPackageManager();
        Set<String> seen = new HashSet<>();
        for (ResolveInfo ri : pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(getPackageName()) || !seen.add(pkg)) continue;
            Row r = new Row();
            r.pkg = pkg; r.label = String.valueOf(ri.loadLabel(pm)); r.icon = ri.loadIcon(pm);
            rows.add(r);
        }
        Collections.sort(rows, new Comparator<Row>() { @Override public int compare(Row a, Row c) { return a.label.compareToIgnoreCase(c.label); } });
        for (Row r : rows) if (HiddenApps.isPackageShownInCustom(this, r.pkg)) shown.add(r.pkg);   // CV1 review: the custom choice, not the unlocked profile's "everything"

        final float dp = getResources().getDisplayMetrics().density;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        final TextView head = new TextView(this);
        head.setTextColor(Color.DKGRAY);
        head.setTextSize(14);
        head.setPadding((int) (16 * dp), (int) (28 * dp), (int) (16 * dp), (int) (8 * dp));
        root.addView(head);
        // O2 (Tj 2026-11-04): "kind of hard to press the check boxes ... a larger toggle that is easy to press next to each app": the WHOLE row is the button, with a big switch at the right.
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding((int) (12 * dp), 0, (int) (12 * dp), (int) (8 * dp));
        android.widget.Button all = new android.widget.Button(this);
        all.setText("Show all"); all.setAllCaps(false);
        android.widget.Button none = new android.widget.Button(this);
        none.setText("Hide all"); none.setAllCaps(false);
        bar.addView(all, new LinearLayout.LayoutParams(0, (int) (52 * dp), 1f));
        bar.addView(none, new LinearLayout.LayoutParams(0, (int) (52 * dp), 1f));
        root.addView(bar);
        final ListView list = new ListView(this);
        final BaseAdapter adapter = new BaseAdapter() {
            @Override public int getCount() { return rows.size(); }
            @Override public Object getItem(int i) { return rows.get(i); }
            @Override public long getItemId(int i) { return i; }
            @Override public View getView(int i, View v, ViewGroup parent) {
                final Row r = rows.get(i);
                final LinearLayout row = new LinearLayout(HiddenAppsActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setMinimumHeight((int) (76 * dp));
                row.setPadding((int) (16 * dp), (int) (8 * dp), (int) (16 * dp), (int) (8 * dp));
                ImageView iv = new ImageView(HiddenAppsActivity.this);
                iv.setImageDrawable(r.icon);
                row.addView(iv, new LinearLayout.LayoutParams((int) (44 * dp), (int) (44 * dp)));
                TextView tv = new TextView(HiddenAppsActivity.this);
                tv.setText(r.label);
                tv.setTextSize(18);
                tv.setTextColor(Color.BLACK);
                tv.setPadding((int) (16 * dp), 0, (int) (8 * dp), 0);
                row.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                final android.widget.Switch sw = new android.widget.Switch(HiddenAppsActivity.this);
                sw.setChecked(shown.contains(r.pkg));
                sw.setScaleX(1.6f); sw.setScaleY(1.6f);          // a big, easy target
                sw.setClickable(false);                           // the whole row takes the tap
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.rightMargin = (int) (14 * dp);
                row.addView(sw, lp);
                row.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        boolean on = !shown.contains(r.pkg);
                        if (on) shown.add(r.pkg); else shown.remove(r.pkg);
                        sw.setChecked(on);
                        HiddenApps.setShown(HiddenAppsActivity.this, r.pkg, on);
                        if (!Unified.unlockedHome(HiddenAppsActivity.this)) HiddenApps.reloadLauncher(HiddenAppsActivity.this);   // CV1
                        head.setText(summary());
                    }
                });
                return row;
            }
        };
        list.setAdapter(adapter);
        head.setText(summary());
        all.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { setAll(true); adapter.notifyDataSetChanged(); head.setText(summary()); } });
        none.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { setAll(false); adapter.notifyDataSetChanged(); head.setText(summary()); } });
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }
}
