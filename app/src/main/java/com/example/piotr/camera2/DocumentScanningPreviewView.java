package com.example.piotr.camera2;

import android.content.Context;
import android.graphics.*;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.SurfaceView;
import org.opencv.core.Point;

import java.util.List;

public class DocumentScanningPreviewView extends SurfaceView {
    private Matrix bitmapToScreenMatrix;
    private Bitmap bitmap;

    private int lastBitmapWidth = 0;
    private int lastBitmapHeight = 0;
    private int lastCanvasWidth = 0;
    private int lastCanvasHeight = 0;

    Paint contoursPaint;
    private List<Point> contours;
    Path contoursPath;


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
        bitmapToScreenMatrix = new Matrix();

        contoursPaint = new Paint();
        contoursPaint.setStyle(Paint.Style.FILL);
        contoursPaint.setColor(Color.GREEN);

        contoursPath = new Path();
    }

    public void drawBitmap(@NonNull  Bitmap bitmap) {
        this.bitmap = bitmap;
        postInvalidate();
    }

    public void drawContours(@NonNull  List<Point> contours) {
        this.contours = contours;
        postInvalidate();
    }

    public void removeContours() {
        this.contours = null;
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

            canvas.drawBitmap(bitmap, bitmapToScreenMatrix, null);


            if(contours != null) {
                contoursPath.reset();

                Point p = contours.get(0);
                contoursPath.moveTo((float)p.x, (float)p.y);

                for(int i = 1; i < contours.size(); ++i) {
                    p = contours.get(i);
                    contoursPath.lineTo((float)p.x, (float)p.y);
                }
                contoursPath.close();

                contoursPath.transform(bitmapToScreenMatrix);

                canvas.drawPath(contoursPath, contoursPaint);
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

    private void calculateMatrix(final int bitmapWidth, final int bitmapHeight, final int canvasWidth, final int canvasHeight) {
        bitmapToScreenMatrix.reset();

        final int dcenterx = (canvasWidth - bitmapWidth) / 2;
        final int dcentery = (canvasHeight - bitmapHeight) / 2;

        //move to center
        bitmapToScreenMatrix.postTranslate(dcenterx, dcentery);
        //rotate 90 degrees relative to center
        bitmapToScreenMatrix.postRotate(90, canvasWidth / 2, canvasHeight / 2);

        //scale to full screen
        final float scale = Math.max((float)canvasWidth / bitmapHeight, (float)canvasHeight / bitmapWidth);
        bitmapToScreenMatrix.postScale(scale, scale, canvasWidth / 2, canvasHeight / 2);
    }
}
