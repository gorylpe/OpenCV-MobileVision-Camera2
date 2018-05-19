package com.example.piotr.camera2.utils;

import android.graphics.PointF;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;

public class OpenCVHelperFuncs {

    public static ArrayList<PointF> convertListOfPoints(List<Point> points) {
        ArrayList<PointF> androidPoints = new ArrayList<>(points.size());
        for (org.opencv.core.Point p : points) {
            androidPoints.add(new PointF((float) p.x, (float) p.y));
        }
        return androidPoints;
    }
}
