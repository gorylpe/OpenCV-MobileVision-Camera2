package com.example.piotr.camera2.utils;

import android.graphics.Matrix;

public class BitmapDrawingUtils {

    public enum ScaleType {
        FIT,
        FILL
    }

    public static void calculateRectToRectScaleFillMatrix(final Matrix matrix,
                                                          final int rect1W,
                                                          final int rect1H,
                                                          final int rect2W,
                                                          final int rect2H,
                                                          final boolean rotateRect1By90DegreesClockwise) {
        calculateRectToRectMatrix(matrix, rect1W, rect1H, rect2W, rect2H, rotateRect1By90DegreesClockwise, ScaleType.FILL);
    }


    public static void calculateRectToRectScaleFitMatrix(final Matrix matrix,
                                                          final int rect1W,
                                                          final int rect1H,
                                                          final int rect2W,
                                                          final int rect2H,
                                                          final boolean rotateRect1By90DegreesClockwise) {
        calculateRectToRectMatrix(matrix, rect1W, rect1H, rect2W, rect2H, rotateRect1By90DegreesClockwise, ScaleType.FIT);
    }

    public static void calculateRectToRectMatrix(final Matrix matrix,
                                                 final int rect1W,
                                                 final int rect1H,
                                                 final int rect2W,
                                                 final int rect2H,
                                                 final boolean rotateRect1By90DegreesClockwise,
                                                 final ScaleType scaleType) {
        matrix.reset();

        final int dcenterx = (rect2W - rect1W) / 2;
        final int dcentery = (rect2H - rect1H) / 2;

        //move to center
        matrix.postTranslate(dcenterx, dcentery);

        float scale;
        //fix for wrong camera image
        if(rotateRect1By90DegreesClockwise){
            switch (scaleType) {
                case FILL:
                    scale = Math.max((float)rect2W / rect1H, (float)rect2H / rect1W);
                    break;
                case FIT:
                    scale = Math.min((float)rect2W / rect1H, (float)rect2H / rect1W);
                    break;
                default:
                    scale = 1.0f;
            }
        } else {
            switch (scaleType) {
                case FILL:
                    scale = Math.max((float)rect2H / rect1H, (float)rect2W / rect1W);
                    break;
                case FIT:
                    scale = Math.min((float)rect2H / rect1H, (float)rect2W / rect1W);
                    break;
                default:
                    scale = 1.0f;
            }
        }

        //scale to full screen
        matrix.postScale(scale, scale, rect2W / 2, rect2H / 2);

        if(rotateRect1By90DegreesClockwise) {
            //rotate 90 degrees relative to center
            matrix.postRotate(90, rect2W / 2, rect2H / 2);
        }
    }
}
