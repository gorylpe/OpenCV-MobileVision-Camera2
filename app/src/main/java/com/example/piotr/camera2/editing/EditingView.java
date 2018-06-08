package com.example.piotr.camera2.editing;

import android.content.Context;
import android.graphics.*;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.SurfaceView;
import com.example.piotr.camera2.utils.BitmapDrawingUtils;
import com.example.piotr.camera2.utils.OpenCVHelperFuncs;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.util.List;

public class EditingView extends SurfaceView {

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
    }

    public void setRotate90Fix(final boolean rotate90Fix) {
        this.rotate90fix = rotate90Fix;
    }

    public void setNewImageWithContours(@NonNull Bitmap bmp, @NonNull List<PointF> contours) {
        this.bmp = bmp;
        this.contours = contours;
        updateContoursPath();
    }

    public void calculateViewToBitmapMatrix(final int bitmapWidth, final int bitmapHeight) {
        final int width = getWidth();
        final int height = getHeight();

        BitmapDrawingUtils.calculateRectToRectScaleFillMatrix(viewToBitmapMatrix, bitmapWidth, bitmapHeight, width, height, rotate90fix);
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

    /*@Override
    public boolean onTouchEvent(MotionEvent e) {
        //no bitmap drawn
        if(bitmapToCanvasMatrix == null)
            return true;
        //dont edit contours when in edit mode
        if(editMode.get()) {
            float[] touchCoords = new float[2];
            touchCoords[0] = e.getX();
            touchCoords[1] = e.getY();

            //translate
            viewToBitmapMatrix.mapPoints(touchCoords);

            PointF touchPoint = new PointF(touchCoords[0], touchCoords[1]);


            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    for(PointF contoursPoint : contours) {
                        //euclidean distance between points, touch is near contour point
                        if(Math.hypot(contoursPoint.x - touchPoint.x, contoursPoint.y - touchPoint.y) <= contourPointRadius) {
                            Log.i("xd", "Touched point x:" + contoursPoint.x + " y:" + contoursPoint.y);
                        }
                    }
                    break;
            }
        }

        return true;
    }*/
}
