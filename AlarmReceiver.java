package uz.vaqt.namoz;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.core.content.ContextCompat;
import org.json.JSONArray;
import org.json.JSONObject;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        Intent s = new Intent(c, AlarmService.class);
        s.putExtras(i);
        ContextCompat.startForegroundService(c, s);
    }

    static void scheduleAll(Context c, String json) {
        try {
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            SharedPreferences p = c.getSharedPreferences("vaqt", 0);
            int flags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
            int old = p.getInt("n", 0);
            for (int k = 0; k < old; k++) {
                am.cancel(PendingIntent.getBroadcast(c, k, new Intent(c, AlarmReceiver.class), flags));
            }
            JSONArray a = new JSONArray(json);
            PendingIntent show = PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
            long now = System.currentTimeMillis();
            for (int k = 0; k < a.length(); k++) {
                JSONObject o = a.getJSONObject(k);
                long at = o.getLong("at");
                if (at <= now) continue;
                Intent in = new Intent(c, AlarmReceiver.class);
                in.putExtra("kind", o.optString("kind", "azan"));
                in.putExtra("snd", o.optString("snd", "sys"));
                in.putExtra("label", o.optString("label", ""));
                in.putExtra("time", o.optString("time", ""));
                in.putExtra("vol", (float) o.optDouble("vol", 1.0));
                in.putExtra("loop", o.optBoolean("loop", false));
                in.putExtra("ui", o.optBoolean("ui", false));
                in.putExtra("cam", o.optBoolean("cam", false));
                in.putExtra("maxMs", o.optLong("maxMs", 0));
                PendingIntent pi = PendingIntent.getBroadcast(c, k, in, flags);
                am.setAlarmClock(new AlarmManager.AlarmClockInfo(at, show), pi);
            }
            p.edit().putInt("n", a.length()).putString("items", json).apply();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
