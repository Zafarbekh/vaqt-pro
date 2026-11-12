package uz.vaqt.namoz;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        String items = c.getSharedPreferences("vaqt", 0).getString("items", null);
        if (items != null) AlarmReceiver.scheduleAll(c, items);
    }
}
