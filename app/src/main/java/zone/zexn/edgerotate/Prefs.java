package zone.zexn.edgerotate;

import android.content.Context;
import android.content.SharedPreferences;

final class Prefs {
    private static final String NAME = "edge_rotate";
    private static final String ENABLED = "enabled";
    private static final String SIDE = "right_side";
    private static final String Y_FRACTION = "y_fraction";

    private Prefs() {}

    static SharedPreferences get(Context context) {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    static boolean isEnabled(Context c) {
        return get(c).getBoolean(ENABLED, false);
    }

    static void setEnabled(Context c, boolean enabled) {
        // commit() so a service-start/reboot immediately sees the requested state.
        get(c).edit().putBoolean(ENABLED, enabled).commit();
    }

    static boolean isRight(Context c) {
        return get(c).getBoolean(SIDE, true);
    }

    static float getYFraction(Context c) {
        return Math.min(0.92f, Math.max(0.08f, get(c).getFloat(Y_FRACTION, 0.52f)));
    }

    static void savePosition(Context c, boolean right, float fraction) {
        get(c).edit()
                .putBoolean(SIDE, right)
                .putFloat(Y_FRACTION, Math.min(0.92f, Math.max(0.08f, fraction)))
                .apply();
    }
}
