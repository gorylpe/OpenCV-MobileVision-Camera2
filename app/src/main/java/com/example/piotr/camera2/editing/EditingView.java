package com.example.piotr.camera2.editing;

import android.content.Context;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;

public class EditingView extends SurfaceView {
    public EditingView(Context context) {
        super(context);
    }

    public EditingView(Context context, AttributeSet attrs) {
        super(context, attrs);
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
