package zone.zexn.edgerotate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) return;
        if (!Prefs.isEnabled(context)
                || !Settings.canDrawOverlays(context)
                || !RotationController.canControl(context)) return;
        try {
            Intent service = new Intent(context, OverlayService.class);
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(service);
            else context.startService(service);
        } catch (RuntimeException e) {
            // Xiaomi's background/autostart rules may block a boot-time launch.
            Log.w("EdgeRotate", "Boot-time restart denied by system", e);
        }
    }
}
