package zone.zexn.edgerotate;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.content.pm.ServiceInfo;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.Toast;
import android.util.Log;

public final class OverlayService extends Service {
    static final String ACTION_STOP = "zone.zexn.edgerotate.STOP";
    private static final String CHANNEL_ID = "edge_rotate_persistent";
    private static final int NOTIFICATION_ID = 101;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private WindowManager windowManager;
    private WindowManager.LayoutParams params;
    private BubbleView bubble;
    private ContentObserver rotationObserver;
    private boolean shown;

    @Override public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            Prefs.setEnabled(this, false);
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!Prefs.isEnabled(this) || !Settings.canDrawOverlays(this) ||
                !RotationController.canControl(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        // A real foreground service, not a silent hidden keepalive service.
        createNotificationChannel();
        Notification notification = makeNotification();
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } catch (RuntimeException ex) {
            Log.w("EdgeRotate", "Foreground service was not allowed", ex);
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!shown) {
            try {
                showBubble();
            } catch (RuntimeException e) {
                Log.e("EdgeRotate", "Could not create overlay", e);
                Toast.makeText(this, "无法显示悬浮按钮，请检查悬浮窗权限", Toast.LENGTH_LONG).show();
                stopSelf();
                return START_NOT_STICKY;
            }
        }
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "侧边旋转常驻服务", NotificationManager.IMPORTANCE_MIN);
            channel.setDescription("保持侧边方向锁定按钮可用");
            channel.setShowBadge(false);
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.createNotificationChannel(channel);
        }
    }

    private Notification makeNotification() {
        Intent launchIntent = new Intent(this, MainActivity.class);
        PendingIntent open = PendingIntent.getActivity(this, 1, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent stopIntent = new Intent(this, OverlayService.class).setAction(ACTION_STOP);
        PendingIntent stop = PendingIntent.getService(this, 2, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return b.setSmallIcon(R.drawable.ic_app)
                .setContentTitle("侧边旋转已开启")
                .setContentText("点击悬浮按钮切换方向锁定")
                .setContentIntent(open)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setShowWhen(false)
                .addAction(new Notification.Action.Builder(null, "关闭按钮", stop).build())
                .build();
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @SuppressWarnings("deprecation")
    private Point screenSize() {
        Point p = new Point();
        windowManager.getDefaultDisplay().getSize(p);
        return p;
    }

    private int clampedY(int proposed) {
        int h = screenSize().y;
        int minY = dp(48);
        int maxY = Math.max(minY, h - dp(108));
        return Math.max(minY, Math.min(proposed, maxY));
    }

    private void restorePosition() {
        Point screen = screenSize();
        params.x = Prefs.isRight(this) ? Math.max(0, screen.x - params.width) : 0;
        int available = Math.max(1, screen.y - params.height);
        params.y = clampedY(Math.round(Prefs.getYFraction(this) * available));
    }

    private void savePosition() {
        Point screen = screenSize();
        boolean right = params.x + params.width / 2 >= screen.x / 2;
        float y = (float) params.y / Math.max(1, screen.y - params.height);
        Prefs.savePosition(this, right, y);
    }

    private void snapToSide() {
        Point screen = screenSize();
        params.x = params.x + params.width / 2 >= screen.x / 2
                ? Math.max(0, screen.x - params.width) : 0;
        params.y = clampedY(params.y);
        updateWindow();
        savePosition();
    }

    private void updateWindow() {
        if (shown && bubble != null) {
            try {
                windowManager.updateViewLayout(bubble, params);
            } catch (IllegalArgumentException ignored) { }
        }
    }

    private void showBubble() {
        bubble = new BubbleView(this);
        int size = dp(52); // Touch target 52dp; visible circle only 32dp.
        params = new WindowManager.LayoutParams(
                size, size,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.setTitle("侧边旋转按钮");
        restorePosition();
        bubble.setOnTouchListener(new BubbleTouch());
        windowManager.addView(bubble, params);
        shown = true;
        rotationObserver = new ContentObserver(mainHandler) {
            @Override public void onChange(boolean selfChange) {
                if (bubble != null) bubble.updateState();
            }
        };
        getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION),
                false, rotationObserver);
    }

    private final class BubbleTouch implements View.OnTouchListener {
        private float downX, downY;
        private int startX, startY;
        private boolean dragging;
        private final int slop = ViewConfiguration.get(OverlayService.this).getScaledTouchSlop();

        @Override public boolean onTouch(View view, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getRawX();
                    downY = event.getRawY();
                    startX = params.x;
                    startY = params.y;
                    dragging = false;
                    bubble.setPressedVisual(true);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - downX;
                    float dy = event.getRawY() - downY;
                    if (!dragging && Math.hypot(dx, dy) > slop) dragging = true;
                    if (dragging) {
                        Point size = screenSize();
                        params.x = Math.max(0, Math.min(
                                startX + Math.round(dx), size.x - params.width));
                        params.y = clampedY(startY + Math.round(dy));
                        updateWindow();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    bubble.setPressedVisual(false);
                    if (dragging) {
                        snapToSide();
                    } else if (RotationController.canControl(OverlayService.this)) {
                        boolean ok = RotationController.toggle(OverlayService.this);
                        bubble.updateState();
                        if (!ok) Toast.makeText(OverlayService.this,
                                "系统未允许更改旋转状态，请检查权限", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(OverlayService.this,
                                "请在侧边旋转 App 中授权修改系统设置", Toast.LENGTH_LONG).show();
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    bubble.setPressedVisual(false);
                    if (dragging) snapToSide();
                    return true;
                default:
                    return false;
            }
        }
    }

    @Override public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (shown) {
            restorePosition();
            updateWindow();
            bubble.updateState();
        }
    }

    @Override public void onDestroy() {
        if (rotationObserver != null) {
            getContentResolver().unregisterContentObserver(rotationObserver);
            rotationObserver = null;
        }
        if (shown && bubble != null) {
            try { windowManager.removeView(bubble); }
            catch (IllegalArgumentException ignored) { }
        }
        shown = false;
        bubble = null;
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) {
        return null;
    }
}
