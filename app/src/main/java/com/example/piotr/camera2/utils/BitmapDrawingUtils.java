package com.example.piotr.camera2.utils;

import android.graphics.Matrix;

public class BitmapDrawingUtils {

    public static void calculateRectToRectScaleFillMatrix(final Matrix matrix,
                                                          final int rect1W,
                                                          final int rect1H,
                                                          final int rect2W,
                                                          final int rect2H,
                                                          final boolean rotateRect1By90DegreesClockwise) {
        matrix.reset();

        final int dcenterx = (rect2W - rect1W) / 2;
        final int dcentery = (rect2H - rect1H) / 2;

        //move to center
        matrix.postTranslate(dcenterx, dcentery);

        float scale;
        //fix for wrong camera image
        if(rotateRect1By90DegreesClockwise){
            scale = Math.max((float)rect2W / rect1H, (float)rect2H / rect1W);
        } else {
            scale = Math.max((float)rect2H / rect1H, (float)rect2W / rect1W);
        }

        //scale to full screen
        matrix.postScale(scale, scale, rect2W / 2, rect2H / 2);

        if(rotateRect1By90DegreesClockwise) {
            //rotate 90 degrees relative to center
            matrix.postRotate(90, rect2W / 2, rect2H / 2);
        }
    }
}
