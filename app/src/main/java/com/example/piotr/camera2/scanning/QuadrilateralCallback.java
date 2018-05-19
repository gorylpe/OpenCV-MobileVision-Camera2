package com.example.piotr.camera2.scanning;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.util.List;

public interface QuadrilateralCallback {
    void onObtained(Mat rgba, List<Point> quad);
}
