package zone.zexn.edgerotate;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

final class BubbleView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean locked;
    private boolean pressed;

    BubbleView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        updateState();
    }

    void updateState() {
        locked = !RotationController.isAutoRotateOn(getContext());
        setContentDescription(locked ? "方向已锁定，点击开启自动旋转" : "自动旋转开启，点击锁定方向");
        invalidate();
    }

    void setPressedVisual(boolean value) {
        pressed = value;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = 16f * density;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(locked ? 0xB33F4654 : 0xB34D667E);
        paint.setAlpha(pressed ? 235 : 143);  // low-key translucent edge bubble
        canvas.drawCircle(cx, cy, radius, paint);

        paint.setColor(0xFFFFFFFF);
        paint.setAlpha(pressed ? 255 : 220);
        paint.setStrokeWidth(1.7f * density);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        if (locked) {
            // Padlock.
            canvas.drawRoundRect(cx - 7f*density, cy - 1f*density,
                    cx + 7f*density, cy + 9f*density,
                    2f*density, 2f*density, paint);
            Path top = new Path();
            top.moveTo(cx - 4.5f*density, cy - 1f*density);
            top.lineTo(cx - 4.5f*density, cy - 5f*density);
            top.cubicTo(cx - 4.5f*density, cy - 11f*density, cx + 4.5f*density,
                    cy - 11f*density, cx + 4.5f*density, cy - 5f*density);
            top.lineTo(cx + 4.5f*density, cy - 1f*density);
            canvas.drawPath(top, paint);
        } else {
            // Circular rotate arrow.
            canvas.drawArc(cx - 9f*density, cy - 9f*density,
                    cx + 9f*density, cy + 9f*density,
                    -70f, 285f, false, paint);
            Path tip = new Path();
            tip.moveTo(cx + 9f*density, cy - 7f*density);
            tip.lineTo(cx + 9f*density, cy - 1f*density);
            tip.lineTo(cx + 3f*density, cy - 3f*density);
            canvas.drawPath(tip, paint);
        }
    }
}
