package uz.vaqt.namoz;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import androidx.core.app.NotificationCompat;
import java.io.File;
import org.json.JSONObject;

public class AlarmService extends Service {
    static String ringing = null;
    private static final String CH = "vaqt_alarm";
    private MediaPlayer mp;
    private int oldVol = -1;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final Runnable stopper = this::stopAll;

    @Override
    public IBinder onBind(Intent i) { return null; }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        if (i == null || "STOP".equals(i.getAction())) {
            stopAll();
            return START_NOT_STICKY;
        }
        String kind = i.getStringExtra("kind");
        String snd = i.getStringExtra("snd");
        String label = i.getStringExtra("label");
        String time = i.getStringExtra("time");
        float vol = i.getFloatExtra("vol", 1f);
        boolean loop = i.getBooleanExtra("loop", false);
        boolean ui = i.getBooleanExtra("ui", false);
        boolean cam = i.getBooleanExtra("cam", false);
        long maxMs = i.getLongExtra("maxMs", 0);

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CH, "Namoz va budilnik", NotificationManager.IMPORTANCE_HIGH);
            ch.setSound(null, null);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
        PendingIntent open = PendingIntent.getActivity(this, 1,
                new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stopPi = PendingIntent.getService(this, 2,
                new Intent(this, AlarmService.class).setAction("STOP"), PendingIntent.FLAG_IMMUTABLE);
        Notification n = new NotificationCompat.Builder(this, CH)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(label == null || label.isEmpty() ? "Vaqt" : label)
                .setContentText(ui ? "Budilnik" : "Namoz vaqti")
                .setContentIntent(open)
                .setFullScreenIntent(open, true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(true)
                .addAction(0, "To'xtatish", stopPi)
                .build();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(77, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(77, n);
        }

        h.removeCallbacks(stopper);
        release();
        try {
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (oldVol < 0) oldVol = am.getStreamVolume(AudioManager.STREAM_ALARM);
            int max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM);
            try { am.setStreamVolume(AudioManager.STREAM_ALARM, Math.max(1, Math.round(max * vol)), 0); } catch (Exception e) { }

            if ("vib".equals(kind)) {
                vibrate(false);
                h.postDelayed(stopper, 3000);
            } else {
                mp = new MediaPlayer();
                mp.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
                if ("beep".equals(kind)) {
                    mp.setDataSource(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
                    h.postDelayed(stopper, maxMs > 0 ? maxMs : 4000);
                } else {
                    File f = new File(getFilesDir(), "snd_" + snd);
                    if (snd != null && f.exists()) {
                        mp.setDataSource(f.getAbsolutePath());
                    } else {
                        mp.setDataSource(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
                    }
                    mp.setLooping(loop);
                    mp.setOnCompletionListener(x -> stopAll());
                    if (maxMs > 0) h.postDelayed(stopper, maxMs);
                }
                mp.prepare();
                mp.start();
                if (ui) vibrate(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            stopAll();
            return START_NOT_STICKY;
        }

        if (ui) {
            try {
                JSONObject o = new JSONObject();
                o.put("time", time);
                o.put("name", label);
                o.put("cam", cam);
                ringing = o.toString();
                AlarmPlugin.notifyRing(ringing);
            } catch (Exception e) { }
        }
        return START_NOT_STICKY;
    }

    private void vibrate(boolean repeat) {
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            long[] p = {0, 600, 400, 600, 400, 600};
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createWaveform(p, repeat ? 0 : -1));
            else v.vibrate(p, repeat ? 0 : -1);
        } catch (Exception e) { }
    }

    private void release() {
        try { if (mp != null) { mp.stop(); } } catch (Exception e) { }
        try { if (mp != null) { mp.release(); } } catch (Exception e) { }
        mp = null;
    }

    private void stopAll() {
        h.removeCallbacks(stopper);
        release();
        try {
            ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).cancel();
            if (oldVol >= 0) {
                ((AudioManager) getSystemService(Context.AUDIO_SERVICE)).setStreamVolume(AudioManager.STREAM_ALARM, oldVol, 0);
            }
        } catch (Exception e) { }
        oldVol = -1;
        ringing = null;
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        release();
        super.onDestroy();
    }
}
