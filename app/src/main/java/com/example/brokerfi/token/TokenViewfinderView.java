package com.example.brokerfi.token;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;

import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.ViewfinderView;

/**
 * Custom scan overlay with square corner brackets and without the default ZXing result dots.
 */
public class TokenViewfinderView extends ViewfinderView {

    private static final int CORNER_LENGTH_DP = 22;
    private static final int CORNER_STROKE_DP = 3;

    private final int cornerLengthPx;
    private final int cornerStrokePx;
    private final Paint cornerPaint;

    public TokenViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        float density = context.getResources().getDisplayMetrics().density;
        cornerLengthPx = (int) (CORNER_LENGTH_DP * density);
        cornerStrokePx = (int) (CORNER_STROKE_DP * density);

        cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cornerPaint.setColor(0xFF1A73E8);
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(cornerStrokePx);
        cornerPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    public void addPossibleResultPoint(ResultPoint point) {
        // Suppress the default ZXing animated result dots.
    }

    @Override
    public void onDraw(Canvas canvas) {
        refreshSizes();
        if (framingRect == null || previewSize == null) {
            return;
        }

        final Rect frame = framingRect;
        final int width = getWidth();
        final int height = getHeight();

        paint.setColor(resultBitmap != null ? resultColor : maskColor);
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);

        if (resultBitmap != null) {
            paint.setAlpha(CURRENT_POINT_OPACITY);
            canvas.drawBitmap(resultBitmap, null, frame, paint);
            return;
        }

        drawCornerBrackets(canvas, frame);
        possibleResultPoints.clear();
        lastPossibleResultPoints.clear();
    }

    private void drawCornerBrackets(Canvas canvas, Rect frame) {
        int left = frame.left;
        int top = frame.top;
        int right = frame.right;
        int bottom = frame.bottom;
        int len = cornerLengthPx;

        canvas.drawLine(left, top, left + len, top, cornerPaint);
        canvas.drawLine(left, top, left, top + len, cornerPaint);

        canvas.drawLine(right, top, right - len, top, cornerPaint);
        canvas.drawLine(right, top, right, top + len, cornerPaint);

        canvas.drawLine(left, bottom, left + len, bottom, cornerPaint);
        canvas.drawLine(left, bottom, left, bottom - len, cornerPaint);

        canvas.drawLine(right, bottom, right - len, bottom, cornerPaint);
        canvas.drawLine(right, bottom, right, bottom - len, cornerPaint);
    }
}
