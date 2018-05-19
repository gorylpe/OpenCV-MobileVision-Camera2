package com.example.piotr.camera2.scanning;

import android.content.Context;
import android.graphics.*;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import com.example.piotr.camera2.utils.OpenCVHelperFuncs;
import com.example.piotr.camera2.utils.StopWatch;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScanningPreviewView extends SurfaceView {

    public static final String TAG = "ScanningPreviewView";

    private boolean rotate90;
    private AtomicBoolean editMode;

    private CachedBitmap cachedBitmap;

    private List<PointF> contours;
    private final float[] tmpContourPointCoords = new float[2];
    //TODO percentage value of width in onLayout
    private final int contourPointRadius = 40;
    private Paint contoursPointPaint;
    private Paint contoursStrokePaint;
    private Paint contoursFillPaint;
    private Path contoursPath;
    private Path contoursPathTransformed;

    private Matrix bitmapToCanvasMatrix;
    private int lastBitmapWidth = 0;
    private int lastBitmapHeight = 0;
    private int lastCanvasWidth = 0;
    private int lastCanvasHeight = 0;

    private Matrix viewToBitmapMatrix;

    private boolean movingContourPoint = false;
    private int touchedContourPoint;

    //debug
    private long framesCount = 0;
    private long timeDrawing = 1;

    public ScanningPreviewView(Context context) {
        super(context);
        init();
    }

    public ScanningPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setWillNotDraw(false);

        editMode = new AtomicBoolean();
        editMode.set(false);
        rotate90 = false;

        cachedBitmap = new CachedBitmap();

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

        bitmapToCanvasMatrix = new Matrix();

        viewToBitmapMatrix = new Matrix();

        StopWatch.start();
    }

    public void rotate90(boolean rotate) {
        this.rotate90 = rotate;
    }

    public void setEditMode() {
        editMode.set(true);
    }

    public void setPreviewMode() {
        editMode.set(false);
    }

    private void setNewImage(@NonNull Mat rgba) {
        cachedBitmap.setFromMat(rgba);
        final Bitmap bmp = cachedBitmap.getBitmap().get();
        calculateViewToBitmapMatrix(bmp.getWidth(), bmp.getHeight());
    }

    public synchronized void setNewImage(@NonNull Mat rgba, boolean setToEditMode) {
        if(!editMode.get()) {
            if(setToEditMode)
                setEditMode();

            setNewImage(rgba);
        }
    }

    public synchronized void setNewImageWithContours(@NonNull Mat rgba, @NonNull List<org.opencv.core.Point> contours, boolean setToEditMode) {
        if(!editMode.get()) {
            if(setToEditMode)
                setEditMode();

            setNewImage(rgba);

            this.contours = OpenCVHelperFuncs.convertListOfPoints(contours);
            updateContoursPath();
        }
    }

    public void calculateViewToBitmapMatrix(final int bitmapWidth, final int bitmapHeight) {
        final int width = getWidth();
        final int height = getHeight();

        calculateRect1ToRect2FillMatrix(viewToBitmapMatrix, bitmapWidth, bitmapHeight, width, height);
        viewToBitmapMatrix.invert(viewToBitmapMatrix);
    }

    public void redraw() {
        if(Looper.myLooper() == Looper.getMainLooper())
            invalidate();
        else
            postInvalidate();
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
    protected void onDraw(Canvas canvas) {
        //TODO remove debug fps
        timeDrawing += StopWatch.stop();
        StopWatch.start();
        framesCount++;
        Log.i("FPS", ((double)1000000000 * framesCount / timeDrawing) + " fps");
        //end debug

        if (canvas != null && cachedBitmap.getBitmap().isPresent()) {
            final Bitmap bitmap = cachedBitmap.getBitmap().get();

            canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);

            final int bitmapWidth = bitmap.getWidth();
            final int bitmapHeight = bitmap.getHeight();
            final int canvasWidth = canvas.getWidth();
            final int canvasHeight = canvas.getHeight();

            if(sizesDifferFromLast(bitmapWidth, bitmapHeight, canvasWidth, canvasHeight)) {
                setNewSizes(bitmapWidth, bitmapHeight, canvasWidth, canvasHeight);
                calculateBitmapToCanvasMatrix(bitmapWidth, bitmapHeight, canvasWidth, canvasHeight);
            }

            canvas.drawBitmap(bitmap, bitmapToCanvasMatrix, null);

            if(contoursPath != null) {
                contoursPath.transform(bitmapToCanvasMatrix, contoursPathTransformed);
                canvas.drawPath(contoursPathTransformed, contoursFillPaint);
                canvas.drawPath(contoursPathTransformed, contoursStrokePaint);
            }
            if(contours != null) {
                for(PointF p : contours) {
                    tmpContourPointCoords[0] = p.x;
                    tmpContourPointCoords[1] = p.y;
                    bitmapToCanvasMatrix.mapPoints(tmpContourPointCoords);
                    canvas.drawCircle(tmpContourPointCoords[0], tmpContourPointCoords[1], contourPointRadius, contoursPointPaint);
                }
            }
        }
    }

    private boolean sizesDifferFromLast(final int bitmapWidth, final int bitmapHeight, final int canvasWidth, final int canvasHeight) {
        return (bitmapWidth != lastBitmapWidth
                || bitmapHeight != lastBitmapHeight
                || canvasWidth != lastCanvasWidth
                || canvasHeight != lastCanvasHeight);
    }

    private void setNewSizes(final int bitmapWidth, final int bitmapHeight, final int canvasWidth, final int canvasHeight) {
        lastBitmapWidth = bitmapWidth;
        lastBitmapHeight = bitmapHeight;
        lastCanvasWidth = canvasWidth;
        lastCanvasHeight = canvasHeight;
    }

    private void calculateBitmapToCanvasMatrix(final int bitmapWidth, final int bitmapHeight, final int canvasWidth, final int canvasHeight) {
        calculateRect1ToRect2FillMatrix(bitmapToCanvasMatrix, bitmapWidth, bitmapHeight, canvasWidth, canvasHeight);
    }

    private void calculateRect1ToRect2FillMatrix(final Matrix matrix, final int rect1Width, final int rect1Height, final int rect2Width, final int rect2Height) {
        matrix.reset();

        final int dcenterx = (rect2Width - rect1Width) / 2;
        final int dcentery = (rect2Height - rect1Height) / 2;

        //move to center
        matrix.postTranslate(dcenterx, dcentery);

        float scale;
        //fix for wrong camera image
        if(rotate90){
            //rotate 90 degrees relative to center
            matrix.postRotate(90, rect2Width / 2, rect2Height / 2);
            scale = Math.max((float)rect2Width / rect1Height, (float)rect2Height / rect1Width);
        } else {
            scale = Math.max((float)rect2Height / rect1Height, (float)rect2Width / rect1Width);
        }

        //scale to full screen
        matrix.postScale(scale, scale, rect2Width / 2, rect2Height / 2);
    }
}
