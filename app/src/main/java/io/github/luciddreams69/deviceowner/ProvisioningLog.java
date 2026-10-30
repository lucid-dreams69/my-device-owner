package io.github.luciddreams69.deviceowner;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.PersistableBundle;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TreeSet;

/** Records each enrollment callback so the app can show the order they happened in. */
final class ProvisioningLog {

    private static final String PREFS = "provisioning_log";
    private static final String KEY_EVENTS = "events";

    private ProvisioningLog() {
    }

    static void add(Context context, String event, PersistableBundle extras) {
        String line = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date())
                + "  " + event;
        if (extras != null && !extras.isEmpty()) {
            line += "\n    QR extras: " + describe(extras);
        }
        SharedPreferences prefs = prefs(context);
        String old = prefs.getString(KEY_EVENTS, "");
        prefs.edit().putString(KEY_EVENTS, old.isEmpty() ? line : old + "\n" + line).apply();
    }

    static String read(Context context) {
        return prefs(context).getString(KEY_EVENTS, "");
    }

    /** The PROVISIONING_ADMIN_EXTRAS_BUNDLE from the QR code, if any. */
    @SuppressWarnings("deprecation")
    static PersistableBundle adminExtras(Intent intent) {
        String key = DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE;
        if (Build.VERSION.SDK_INT >= 33) {
            return intent.getParcelableExtra(key, PersistableBundle.class);
        }
        return intent.getParcelableExtra(key);
    }

    @SuppressWarnings("deprecation")
    private static String describe(PersistableBundle bundle) {
        StringBuilder out = new StringBuilder();
        for (String key : new TreeSet<>(bundle.keySet())) {
            if (out.length() > 0) out.append(", ");
            out.append(key).append('=').append(bundle.get(key));
        }
        return out.toString();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
