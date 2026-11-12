package uz.vaqt.namoz;

import android.content.Context;
import android.content.Intent;
import android.util.Base64;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.io.File;
import java.io.FileOutputStream;
import org.json.JSONArray;

@CapacitorPlugin(name = "Alarm")
public class AlarmPlugin extends Plugin {
    static AlarmPlugin inst;

    @Override
    public void load() { inst = this; }

    static void notifyRing(String json) {
        try {
            if (inst != null) inst.notifyListeners("ring", new JSObject(json));
        } catch (Exception e) { e.printStackTrace(); }
    }

    @PluginMethod
    public void schedule(PluginCall call) {
        try {
            JSONArray arr = call.getData().getJSONArray("items");
            AlarmReceiver.scheduleAll(getContext(), arr.toString());
            call.resolve();
        } catch (Exception e) { call.reject(String.valueOf(e)); }
    }

    @PluginMethod
    public void saveSound(PluginCall call) {
        try {
            String id = call.getString("id");
            byte[] b = Base64.decode(call.getString("data"), Base64.DEFAULT);
            try (FileOutputStream f = new FileOutputStream(new File(getContext().getFilesDir(), "snd_" + id))) {
                f.write(b);
            }
            call.resolve();
        } catch (Exception e) { call.reject(String.valueOf(e)); }
    }

    @PluginMethod
    public void stop(PluginCall call) {
        Context c = getContext();
        c.startService(new Intent(c, AlarmService.class).setAction("STOP"));
        call.resolve();
    }

    @PluginMethod
    public void pending(PluginCall call) {
        try {
            String r = AlarmService.ringing;
            call.resolve(r == null ? new JSObject() : new JSObject(r));
        } catch (Exception e) { call.resolve(new JSObject()); }
    }
}
