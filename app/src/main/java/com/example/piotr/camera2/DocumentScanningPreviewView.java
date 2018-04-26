package com.example.piotr.camera2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.SurfaceView;

public class DocumentScanningPreviewView extends SurfaceView {
    private Matrix matrix;
    private Bitmap bitmap;
    private int lastBitmapWidth = 0;
    private int lastBitmapHeight = 0;
    private int lastCanvasWidth = 0;
    private int lastCanvasHeight = 0;

    public DocumentScanningPreviewView(Context context) {
        super(context);
        init();
    }

    public DocumentScanningPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        matrix = new Matrix();
    }

    public void drawBitmap(@NonNull  Bitmap bitmap) {
        this.bitmap = bitmap;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (canvas != null && bitmap != null) {
            canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);

            final int bitmapWidth = bitmap.getWidth();
            final int bitmapHeight = bitmap.getHeight();
            final int canvasWidth = canvas.getWidth();
            final int canvasHeight = canvas.getHeight();

            if(sizesDifferFromLast(bitmapWidth, bitmapHeight, canvasWidth, canvasHeight)) {
                setNewSizes(bitmapWidth, bitmapHeight, canvasWidth, canvasHeight);
                calculateMatrix(bitmapWidth, bitmapHeight, canvasWidth, canvasHeight);
            }

            canvas.drawBitmap(bitmap, matrix, null);
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

    private void calculateMatrix(final int bitmapWidth, final int bitmapHeight, final int canvasWidth, final int canvasHeight) {
        matrix.reset();

        final int dcenterx = (canvasWidth - bitmapWidth) / 2;
        final int dcentery = (canvasHeight - bitmapHeight) / 2;

        //move to center
        matrix.postTranslate(dcenterx, dcentery);
        //rotate 90 degrees relative to center
        matrix.postRotate(90, canvasWidth / 2, canvasHeight / 2);

        //scale to full screen
        final float scale = Math.max((float)canvasWidth / bitmapHeight, (float)canvasHeight / bitmapWidth);
        matrix.postScale(scale, scale, canvasWidth / 2, canvasHeight / 2);
    }
}
