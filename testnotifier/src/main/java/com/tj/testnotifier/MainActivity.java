package com.tj.testnotifier;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Bundle;
import android.widget.TextView;

/** CI-only: posts one notification each time it is opened. Its title carries the package name, so a screen dump shows which copy (quick / ctl) it came from. */
public class MainActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        TextView t = new TextView(this);
        t.setText("Notifier test app " + getPackageName());
        t.setTextSize(20);
        setContentView(t);
        if (getIntent().getBooleanExtra("vpn", false)) {   // CB2 (section vo): start the test VPN (prepare() is null once the scenario allowed ACTIVATE_VPN for this app) and post nothing
            android.content.Intent need = android.net.VpnService.prepare(this);
            android.util.Log.i("TJVPN", need == null ? "prepared, starting" : "NOT prepared (appops ACTIVATE_VPN missing)");
            if (need == null) startService(new android.content.Intent(this, TestVpnService.class));
            finish();
            return;
        }
        if (getIntent().getBooleanExtra("quiet", false)) return;   // the task-clean-up test (section rt) needs a shared app that opens WITHOUT a notification
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        boolean high = getIntent().getBooleanExtra("high", false);   // section nq (BH1): only a HIGH-importance notification pops up as a banner; every other section keeps the default channel
        String ch = high ? "h" : "t";
        nm.createNotificationChannel(new NotificationChannel(ch, high ? "Test pop-up" : "Test", high ? NotificationManager.IMPORTANCE_HIGH : NotificationManager.IMPORTANCE_DEFAULT));
        nm.notify(1, new Notification.Builder(this, ch)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("NOTIFIER " + getPackageName())
                .setContentText("posted when the app was opened")
                .build());
    }
}
