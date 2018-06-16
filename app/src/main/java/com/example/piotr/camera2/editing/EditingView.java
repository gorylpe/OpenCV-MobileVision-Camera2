package com.example.piotr.camera2.editing;

import android.content.Context;
import android.graphics.*;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import com.example.piotr.camera2.scanning.CachedBitmap;
import com.example.piotr.camera2.utils.DrawingUtils;
import org.opencv.core.Mat;

import java.util.List;

public class EditingView extends SurfaceView {

    private static final String TAG = "EditingView";

    private CachedBitmap cachedBmp;
    private List<PointF> contours;

    private DrawingUtils.Scale scale;

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

        cachedBmp = new CachedBitmap();

        scale = DrawingUtils.Scale.FILL;

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
            if(cachedBmp.getBitmap().isPresent()) {
                Bitmap bmp = cachedBmp.getBitmap().get();
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

    public void setNewImageWithContours(@NonNull Mat mat, @NonNull List<PointF> contours) {
        this.cachedBmp.setFromMat(mat);
        this.contours = contours;
        updateContoursPath();

        calculateViewToBitmapMatrix(mat.width(), mat.height());

        redraw();
    }

    public void setScale(DrawingUtils.Scale scale) {
        this.scale = scale;
    }

    public void calculateViewToBitmapMatrix(final int bmpW, final int bmpH) {
        final int width = getWidth();
        final int height = getHeight();

        DrawingUtils.calculateRectToRectMatrix(viewToBitmapMatrix, bmpW, bmpH, width, height, rotate90fix, scale);
        viewToBitmapMatrix.invert(viewToBitmapMatrix);
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
        if(c != null && cachedBmp.getBitmap().isPresent()) {
            final Bitmap bmp = cachedBmp.getBitmap().get();

            c.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);

            final int bmpW = bmp.getWidth();
            final int bmpH = bmp.getHeight();
            final int cW = c.getWidth();
            final int cH = c.getHeight();

            //if last bitmap and canvas sizes differs
            if(!DrawingUtils.equalSizes(lastBmpW, lastBmpH, bmpW, bmpH)
            || !DrawingUtils.equalSizes(lastCW, lastCH, cW, cH)) {
                //save new sizes
                lastBmpW = bmpW; lastBmpH = bmpH; lastCW = cW; lastCH = cH;
                //recalculate drawing bitmap to canvas transform matrix
                DrawingUtils.calculateRectToRectMatrix(bitmapToCanvasMatrix, bmpW, bmpH, cW, cH, rotate90fix, scale);
            }

            c.drawBitmap(bmp, bitmapToCanvasMatrix, null);

            //draw contours
            if(contoursPath != null) {
                contoursPath.transform(bitmapToCanvasMatrix, contoursPathTransformed);
                c.drawPath(contoursPathTransformed, contoursFillPaint);
                c.drawPath(contoursPathTransformed, contoursStrokePaint);
            }

            //draw contours corners as circles
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

                    updateContoursPath();
                    redraw();
                }
                break;
            case MotionEvent.ACTION_UP:
                updateContoursPath();
                redraw();
                movingContourPoint = false;
                break;
        }

        return true;
    }
}
