package com.example.piotr.camera2.scanning;

import android.content.Context;
import android.graphics.*;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.SurfaceView;
import com.example.piotr.camera2.utils.BitmapDrawingUtils;
import org.opencv.core.Mat;

public class ScanningPreviewView extends SurfaceView {

    public static final String TAG = "ScanningPreviewView";

    private boolean rotate90fix;

    private CachedBitmap cachedBmp;

    private Matrix bitmapToCanvasMatrix;
    private int lastBmpW = 0;
    private int lastBmpH = 0;
    private int lastCW = 0;
    private int lastCH = 0;

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

        rotate90fix = false;

        cachedBmp = new CachedBitmap();

        bitmapToCanvasMatrix = new Matrix();
    }

    public void rotate90fix(boolean fix) {
        this.rotate90fix = fix;
    }

    public void setNewImage(@NonNull Mat rgba) {
        cachedBmp.setFromMat(rgba);
    }

    public void redraw() {
        if(Looper.myLooper() == Looper.getMainLooper())
            invalidate();
        else
            postInvalidate();
    }

    @Override
    protected void onDraw(Canvas c) {

        if (c != null && cachedBmp.getBitmap().isPresent()) {
            final Bitmap bmp = cachedBmp.getBitmap().get();

            c.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);

            final int bmpW = bmp.getWidth();
            final int bmpH = bmp.getHeight();
            final int cW = c.getWidth();
            final int cH = c.getHeight();

            if(sizesDifferFromLast(bmpW, bmpH, cW, cH)) {
                setNewSizes(bmpW, bmpH, cW, cH);
                calculateBitmapToCanvasMatrix(bmpW, bmpH, cW, cH);
            }

            c.drawBitmap(bmp, bitmapToCanvasMatrix, null);
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
}
