package com.example.piotr.camera2.utils;

import android.graphics.Matrix;
import android.support.annotation.NonNull;

public class DrawingUtils {

    public enum Scale {
        FIT,
        FILL
    }

    public static void calculateRectToRectMatrix(final @NonNull Matrix matrix,
                                                 final int rect1W,
                                                 final int rect1H,
                                                 final int rect2W,
                                                 final int rect2H,
                                                 final boolean rotateRect1By90DegreesClockwise,
                                                 final Scale scaleType) {
        matrix.reset();

        final int dcenterx = (rect2W - rect1W) / 2;
        final int dcentery = (rect2H - rect1H) / 2;

        //move to center
        matrix.postTranslate(dcenterx, dcentery);

        float scale;
        //fix for wrong camera image
        switch (scaleType) {
            case FILL:
                if(rotateRect1By90DegreesClockwise)
                    scale = Math.max((float)rect2W / rect1H, (float)rect2H / rect1W);
                else
                    scale = Math.max((float)rect2H / rect1H, (float)rect2W / rect1W);
                break;
            case FIT:
                if(rotateRect1By90DegreesClockwise)
                    scale = Math.min((float)rect2W / rect1H, (float)rect2H / rect1W);
                else
                    scale = Math.min((float)rect2H / rect1H, (float)rect2W / rect1W);
                break;
            default:
                scale = 1.0f;
        }

        //scale to full screen
        matrix.postScale(scale, scale, rect2W / 2, rect2H / 2);

        if(rotateRect1By90DegreesClockwise) {
            //rotate 90 degrees relative to center
            matrix.postRotate(90, rect2W / 2, rect2H / 2);
        }
    }

    public static boolean equalSizes(int w1, int h1, int w2, int h2) {
        return w1 == w2 && h1 == h2;
    }
}
