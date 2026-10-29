package zone.zexn.edgerotate;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private TextView status;
    private TextView overlayStatus;
    private TextView settingsStatus;
    private Button overlayButton;
    private Button settingsButton;
    private Button toggleButton;

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(250,250,252));
        getWindow().setNavigationBarColor(Color.rgb(250,250,252));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(250,250,252));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(25), dp(35), dp(25), dp(30));
        root.setFitsSystemWindows(true);
        scroll.addView(root);
        setContentView(scroll);

        TextView title = text("侧边旋转", 28, 0xFF243044, true);
        root.addView(title);
        TextView desc = text("小按钮，单击锁定方向；再次单击恢复自动旋转。", 14,
                0xFF657084, false);
        LinearLayout.LayoutParams descLp = lp(0, 0, 18);
        root.addView(desc, descLp);

        LinearLayout card = new LinearLayout(this);
        card.setPadding(dp(18), dp(18), dp(18), dp(16));
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), 0xFFE7EAF0);
        card.setBackground(bg);
        root.addView(card, lp(0, 0, 16));

        status = text("", 18, 0xFF26364B, true);
        card.addView(status);
        TextView note = text("悬浮按钮会靠边显示。长按拖动到需要的位置，松手自动贴边。", 13,
                0xFF6C7889, false);
        card.addView(note, lp(0, 12, 14));

        overlayStatus = text("", 14, 0xFF3D5065, false);
        card.addView(overlayStatus, lp(0, 4, 6));
        overlayButton = button("允许显示悬浮窗", false);
        card.addView(overlayButton, lp(0, 8, 10));
        overlayButton.setOnClickListener(v -> openSetting(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION));

        settingsStatus = text("", 14, 0xFF3D5065, false);
        card.addView(settingsStatus, lp(0, 4, 6));
        settingsButton = button("允许修改系统设置", false);
        card.addView(settingsButton, lp(0, 8, 10));
        settingsButton.setOnClickListener(v -> openSetting(
                Settings.ACTION_MANAGE_WRITE_SETTINGS));

        toggleButton = button("开启侧边按钮", true);
        root.addView(toggleButton, lp(0, 4, 18));
        toggleButton.setOnClickListener(v -> toggle());

        TextView subTitle = text("关于后台常驻", 16, 0xFF293549, true);
        root.addView(subTitle, lp(0, 14, 10));
        TextView tips = text("已使用前台服务 + 开机自启。仍建议在澎湃 OS 中开启后台自启动、设置电池使用为无限制，并在最近任务中锁定本应用。系统的省电策略仍可能终止服务。", 13,
                0xFF657084, false);
        root.addView(tips);
        Button infoButton = button("打开应用详情（设置后台权限）", false);
        root.addView(infoButton, lp(0, 10, 12));
        infoButton.setOnClickListener(v -> {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
        });
    }

    private LinearLayout.LayoutParams lp(int left, int top, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(dp(left), dp(top), 0, dp(bottom));
        return p;
    }

    private TextView text(String str, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(str);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setLineSpacing(dp(2), 1.08f);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }

    private Button button(String label, boolean primary) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setMinHeight(dp(48));
        b.setPadding(dp(16), dp(10), dp(16), dp(10));
        b.setTextColor(primary ? Color.WHITE : 0xFF364961);
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(13));
        shape.setColor(primary ? 0xFF425D81 : 0xFFF0F3F7);
        b.setBackground(shape);
        return b;
    }

    private void openSetting(String action) {
        try {
            Intent intent = new Intent(action,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception ignored) {
                Toast.makeText(this, "请在系统设置中手动授予权限", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void toggle() {
        if (Prefs.isEnabled(this)) {
            Prefs.setEnabled(this, false);
            stopService(new Intent(this, OverlayService.class));
        } else {
            if (!Settings.canDrawOverlays(this)) {
                openSetting(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                return;
            }
            if (!RotationController.canControl(this)) {
                openSetting(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                return;
            }
            Prefs.setEnabled(this, true);
            try {
                Intent service = new Intent(this, OverlayService.class);
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(service);
                else startService(service);
            } catch (RuntimeException e) {
                Prefs.setEnabled(this, false);
                Toast.makeText(this, "系统阻止启动悬浮服务，请检查后台权限", Toast.LENGTH_LONG).show();
            }
        }
        refresh();
    }

    @Override protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        if (status == null) return;
        boolean overlay = Settings.canDrawOverlays(this);
        boolean write = RotationController.canControl(this);
        boolean enabled = Prefs.isEnabled(this);
        status.setText(enabled ? "状态：侧边按钮已开启" : "状态：侧边按钮已关闭");
        overlayStatus.setText((overlay ? "✓ " : "○ ") + "显示悬浮窗权限" + (overlay ? "：已授予" : "：未授予"));
        settingsStatus.setText((write ? "✓ " : "○ ") + "修改系统设置权限" + (write ? "：已授予" : "：未授予"));
        overlayButton.setVisibility(overlay ? View.GONE : View.VISIBLE);
        settingsButton.setVisibility(write ? View.GONE : View.VISIBLE);
        toggleButton.setText(enabled ? "关闭侧边按钮" : "开启侧边按钮");
    }
}
