package com.example.piotr.camera2.editing;

import android.content.Context;
import android.graphics.*;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.ViewTreeObserver;
import com.example.piotr.camera2.utils.BitmapDrawingUtils;

import java.util.List;

public class EditingView extends SurfaceView {

    private static final String TAG = "EditingView";

    private Bitmap bmp;
    private List<PointF> contours;

    private boolean rotate90fix;

    private final float[] tmpContourPointCoords = new float[2];
    //TODO percentage value of width in onLayout
    private final int contourPointRadius = 40;
    private Paint contoursPointPaint;
    private Paint contoursStrokePaint;
    private Paint contoursFillPaint;
    private Path contoursPath;
    private Path contoursPathTransformed;

    private Matrix bitmapToCanvasMatrix;
    private int lastBmpW = 0;
    private int lastBmpH = 0;
    private int lastCW = 0;
    private int lastCH = 0;

    private Matrix viewToBitmapMatrix;

    private PointF lastTouchBmpCords;
    private boolean movingContourPoint = false;
    private int movingContourPointIndex = 0;

    public EditingView(Context context) {
        super(context);
        init();
    }

    public EditingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setWillNotDraw(false);

        bitmapToCanvasMatrix = new Matrix();
        viewToBitmapMatrix = new Matrix();

        contoursPointPaint = new Paint();
        contoursPointPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        contoursPointPaint.setColor(Color.argb(255, 88, 42, 114));
        contoursPointPaint.setStrokeWidth(1.0f);
        contoursPointPaint.setShadowLayer(5.5f, 6.0f, 6.0f, Color.BLACK);

        contoursStrokePaint = new Paint();
        contoursStrokePaint.setStyle(Paint.Style.STROKE);
        contoursStrokePaint.setColor(Color.argb(255, 88, 42, 114));
        contoursStrokePaint.setStrokeWidth(7.0f);

        contoursFillPaint = new Paint();
        contoursFillPaint.setStyle(Paint.Style.FILL);
        contoursFillPaint.setColor(Color.argb(128, 122, 159, 53));

        contoursPath = new Path();
        contoursPathTransformed = new Path();

        //onTouchEvent won't work if bmp set before View measurement, so recalculate View to Bitmap matrix after View measurement
        getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if(bmp != null) {
                calculateViewToBitmapMatrix(bmp.getWidth(), bmp.getHeight());
            }
        });
    }

    public void redraw() {
        if(Looper.myLooper() == Looper.getMainLooper())
            invalidate();
        else
            postInvalidate();
    }

    public void setRotate90Fix(final boolean rotate90Fix) {
        this.rotate90fix = rotate90Fix;
    }

    public void setNewImageWithContours(@NonNull Bitmap bmp, @NonNull List<PointF> contours) {
        this.bmp = bmp;
        this.contours = contours;
        updateContoursPath();

        calculateViewToBitmapMatrix(bmp.getWidth(), bmp.getHeight());
    }

    public void calculateViewToBitmapMatrix(final int bmpW, final int bmpH) {
        final int width = getWidth();
        final int height = getHeight();

        BitmapDrawingUtils.calculateRectToRectScaleFillMatrix(viewToBitmapMatrix, bmpW, bmpH, width, height, rotate90fix);
        viewToBitmapMatrix.invert(viewToBitmapMatrix);

        Log.i(TAG, viewToBitmapMatrix.toString());
    }

    private void updateContoursPath() {
        contoursPath.reset();

        PointF p = contours.get(0);
        contoursPath.moveTo(p.x, p.y);

        for(int i = 1; i < contours.size(); ++i) {
            p = contours.get(i);
            contoursPath.lineTo(p.x, p.y);
        }
        contoursPath.close();
    }

    @Override
    protected void onDraw(Canvas c) {
        if(c != null && bmp != null) {
            c.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);

            final int bmpW = bmp.getWidth();
            final int bmpH = bmp.getHeight();
            final int cW = c.getWidth();
            final int cH = c.getHeight();

            if(sizesDifferFromLast(bmpW, bmpH, cW, cH)) {
                setNewSizes(bmpW, bmpH, cW, cH);
                calculateBitmapToCanvasMatrix(bmpW, bmpH, cW, cH);
            }

            if(contoursPath != null) {
                contoursPath.transform(bitmapToCanvasMatrix, contoursPathTransformed);
                Log.i(TAG, contoursPathTransformed.isEmpty() + "");
                c.drawPath(contoursPathTransformed, contoursFillPaint);
                c.drawPath(contoursPathTransformed, contoursStrokePaint);
            }

            c.drawBitmap(bmp, bitmapToCanvasMatrix, null);

            if(contours != null) {
                for(PointF p : contours) {
                    tmpContourPointCoords[0] = p.x;
                    tmpContourPointCoords[1] = p.y;
                    bitmapToCanvasMatrix.mapPoints(tmpContourPointCoords);
                    c.drawCircle(tmpContourPointCoords[0], tmpContourPointCoords[1], contourPointRadius, contoursPointPaint);
                }
            }
        }
    }

    private boolean sizesDifferFromLast(final int bmpW, final int bmpH, final int cW, final int cH) {
        return (bmpW != lastBmpW
                || bmpH != lastBmpH
                || cW != lastCW
                || cH != lastCH);
    }

    private void setNewSizes(final int bmpW, final int bmpH, final int cW, final int cH) {
        lastBmpW = bmpW;
        lastBmpH = bmpH;
        lastCW = cW;
        lastCH = cH;
    }

    private void calculateBitmapToCanvasMatrix(final int bmpW, final int bmpH, final int cW, final int cH) {
        BitmapDrawingUtils.calculateRectToRectScaleFillMatrix(bitmapToCanvasMatrix, bmpW, bmpH, cW, cH, rotate90fix);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        //no bitmap drawn
        if(bitmapToCanvasMatrix == null)
            return true;
        //dont edit contours when in edit mode
        float[] touchCoords = new float[2];
        touchCoords[0] = e.getX();
        touchCoords[1] = e.getY();

        //translate
        viewToBitmapMatrix.mapPoints(touchCoords);

        PointF touchBmpCoords = new PointF(touchCoords[0], touchCoords[1]);

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                for (int i = 0; i < contours.size(); ++i) {
                    PointF contoursPoint = contours.get(i);
                    //euclidean distance between points, touch is near contour point
                    if (Math.hypot(contoursPoint.x - touchBmpCoords.x, contoursPoint.y - touchBmpCoords.y) <= contourPointRadius) {
                        movingContourPoint = true;
                        movingContourPointIndex = i;
                        lastTouchBmpCords = touchBmpCoords;
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if(movingContourPoint && lastTouchBmpCords != null) {
                    float dx = touchBmpCoords.x - lastTouchBmpCords.x;
                    float dy = touchBmpCoords.y - lastTouchBmpCords.y;

                    PointF contourPoint = contours.get(movingContourPointIndex);
                    contourPoint.x += dx;
                    contourPoint.y += dy;

                    lastTouchBmpCords = touchBmpCoords;

                    redraw();
                }
                break;
            case MotionEvent.ACTION_UP:
                movingContourPoint = false;
                break;
        }

        return true;
    }
}
