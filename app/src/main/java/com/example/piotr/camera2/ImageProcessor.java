package com.example.piotr.camera2;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ImageProcessor{

    private static final String TAG = "ImageProcessor";

    private final static double approxPolyDPEpsConst = 0.02;

    //resized width for contours finding
    private final static int fixedWidth = 500;

    //for threshold calculations
    private static Mat otsu;

    public static Optional<MatOfPoint> computeBestContours(Mat rgba, final int blurSize, final double sizeThreshold){
        Mat resized = new Mat();

        final int oldWidth = rgba.cols();
        final int oldHeight = rgba.rows();

        final double ratio = oldWidth / fixedWidth;
        final int width = Double.valueOf(oldWidth / ratio).intValue();
        final int height = Double.valueOf(oldHeight / ratio).intValue();

        final int imageArea = width * height;
        final int thresholdMinArea = (int)(imageArea * sizeThreshold);
        final Point center = new Point(width / 2, height / 2);

        ImageProcessor.resize(rgba, resized, width, height);

        Mat v = new Mat();
        ImageProcessor.canny(resized, v, blurSize);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(v, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        resized.release();
        v.release();
        hierarchy.release();

        Optional<MatOfPoint> bestContours = chooseBestContoursWithPointInside(contours, thresholdMinArea, center);
        bestContours.ifPresent(c -> ImageProcessor.multiplyPointsByScalar(c, ratio));
        return bestContours;
    }

    public static Optional<MatOfPoint> chooseBestContoursWithPointInside(List<MatOfPoint> contours, final int thresholdMinArea, Point pointInside) {
        MatOfPoint bestContours = null;

        if(contours.size() > 0) {
            int index = 0;
            double maxArea = 0;

            for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
                double tempArea = Imgproc.contourArea(contours.get(contourIdx));
                if(tempArea > maxArea) {
                    maxArea = tempArea;
                    index = contourIdx;
                }
            }

            MatOfPoint2f contourToTest = new MatOfPoint2f();
            contours.get(index).convertTo(contourToTest, CvType.CV_32F);
            final double hasPoint = Imgproc.pointPolygonTest(contourToTest, pointInside, false);
            if(hasPoint >= 0.0 && maxArea > thresholdMinArea) {
                bestContours = contours.get(index);
            }
        }

        return Optional.ofNullable(bestContours);
    }

    public static void multiplyPointsByScalar(MatOfPoint points, final double scalar) {
        Core.multiply(points, Scalar.all(scalar), points);
    }

    public static MatOfPoint2f approxPolyDP(MatOfPoint contours) {
        MatOfPoint2f c2f = new MatOfPoint2f(contours.toArray());
        double peri = Imgproc.arcLength(c2f, true);
        MatOfPoint2f approx = new MatOfPoint2f();
        Imgproc.approxPolyDP(c2f, approx, approxPolyDPEpsConst * peri, true);
        c2f.release();
        return approx;
    }

    public static void resize(Mat src, Mat dst, final int newWidth, final int newHeight) {
        Size newSize = new Size(newWidth, newHeight);
        Imgproc.resize(src, dst, newSize);
    }

    public static void gray(Mat src, Mat dst) {
        Imgproc.cvtColor(src, dst, Imgproc.COLOR_RGBA2GRAY, 4);
    }

    public static double otsuThreshold(Mat singleChannel) {
        if(otsu == null)
            otsu = new Mat();
        return Imgproc.threshold(singleChannel, otsu, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
    }

    public static void canny(Mat rgbaSrc, Mat dst, final int blurSize) {
        ImageProcessor.gray(rgbaSrc, dst);
        Imgproc.GaussianBlur(dst, dst, new org.opencv.core.Size(blurSize, blurSize), 0);
        double highThreshold = otsuThreshold(dst);
        double lowThreshold = highThreshold / 3;
        Imgproc.Canny(dst, dst, lowThreshold, highThreshold);
    }

    public static Mat withCanny(Mat rgbaSrc, Mat dst, final int blurSize) {
        final int oldWidth = rgbaSrc.cols();
        final int oldHeight = rgbaSrc.rows();

        final double ratio = oldWidth / fixedWidth;
        final int width = Double.valueOf(oldWidth / ratio).intValue();
        final int height = Double.valueOf(oldHeight / ratio).intValue();

        ImageProcessor.resize(rgbaSrc, dst, width, height);

        canny(dst, dst, blurSize);

        ImageProcessor.resize(dst, dst, oldWidth, oldHeight);

        Imgproc.cvtColor(dst, dst, Imgproc.COLOR_GRAY2RGBA);
        Core.bitwise_or(rgbaSrc, dst, dst);

        return dst;
    }
}
