package zone.zexn.edgerotate;

import android.content.Context;
import android.content.ContentResolver;
import android.provider.Settings;
import android.view.Display;
import android.view.WindowManager;

final class RotationController {
    private RotationController() {}

    static boolean canControl(Context c) {
        return Settings.System.canWrite(c);
    }

    static boolean isAutoRotateOn(Context c) {
        try {
            return Settings.System.getInt(c.getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION, 1) == 1;
        } catch (SecurityException e) {
            return false;
        }
    }

    // Returns true only if the requested state was actually persisted.
    static boolean setAutoRotate(Context c, boolean enabled) {
        if (!canControl(c)) return false;
        ContentResolver resolver = c.getContentResolver();
        try {
            if (!enabled) {
                // Preserve the current display orientation when entering rotation lock.
                WindowManager windowManager = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
                Display display = windowManager.getDefaultDisplay();
                Settings.System.putInt(resolver, Settings.System.USER_ROTATION, display.getRotation());
            }
            boolean wrote = Settings.System.putInt(resolver,
                    Settings.System.ACCELEROMETER_ROTATION, enabled ? 1 : 0);
            return wrote && (isAutoRotateOn(c) == enabled);
        } catch (SecurityException | IllegalArgumentException e) {
            return false;
        }
    }

    static boolean toggle(Context c) {
        return setAutoRotate(c, !isAutoRotateOn(c));
    }
}
