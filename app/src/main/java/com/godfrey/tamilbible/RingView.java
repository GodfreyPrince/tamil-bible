package com.godfrey.tamilbible;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/** A small circular progress ring used in book lists and the progress screen. */
public class RingView extends View {

    private float fraction = 0f;
    private int color = 0xFFE8A13D;
    private int trackColor = 0x22E8A13D;
    private boolean showCheck = false;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint checkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path check = new Path();

    public RingView(Context c) { super(c); }

    public RingView(Context c, AttributeSet attrs) { super(c, attrs); }

    public void set(float fraction, int color, int trackColor, boolean showCheck) {
        this.fraction = Math.max(0f, Math.min(1f, fraction));
        this.color = color;
        this.trackColor = trackColor;
        this.showCheck = showCheck;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        float cx = w / 2f, cy = h / 2f;
        float r = Math.min(w, h) / 2f - w * 0.10f;
        float stroke = Math.max(2f, w * 0.085f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(stroke);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(trackColor);
        canvas.drawCircle(cx, cy, r, paint);
        if (fraction > 0f) {
            paint.setColor(color);
            canvas.drawArc(cx - r, cy - r, cx + r, cy + r, -90, 360f * fraction, false, paint);
        }
        if (showCheck && fraction >= 1f) {
            checkPaint.setStyle(Paint.Style.STROKE);
            checkPaint.setStrokeWidth(stroke);
            checkPaint.setStrokeCap(Paint.Cap.ROUND);
            checkPaint.setStrokeJoin(Paint.Join.ROUND);
            checkPaint.setColor(color);
            float k = r * 0.42f;
            check.reset();
            check.moveTo(cx - k, cy);
            check.lineTo(cx - k * 0.25f, cy + k * 0.7f);
            check.lineTo(cx + k, cy - k * 0.6f);
            canvas.drawPath(check, checkPaint);
        }
    }
}
