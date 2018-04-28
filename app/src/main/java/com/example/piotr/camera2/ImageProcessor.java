package com.example.piotr.camera2;

import android.content.Context;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ImageProcessor implements OpenCVManager.InitializedCallback{

    private static final String TAG = "ImageProcessor";

    private Context context;

    private Handler openCVHandler;
    private HandlerThread openCVThread;

    private boolean isOpenCVInitialized = false;

    //for threshold calculations
    private static Mat otsu;

    public ImageProcessor(Context context) {
        this.context = context;
    }

    public void start() {
        Log.i(TAG, "Image processor start");
        startOpenCVThread();
        new OpenCVManager(context, this).init();
    }

    public void stop() {
        stopOpenCVThread();
    }

    @Override
    public void onOpenCVInitialized() {
        isOpenCVInitialized = true;
    }

    public boolean isOpenCVInitialized() { return isOpenCVInitialized; }

    public Optional<List<Point>> computeBestContours(Mat rgba){

        Mat v = ImageProcessor.gray(rgba);
        Imgproc.adaptiveThreshold(v, v, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 115, 4);
        Imgproc.medianBlur(v, v, 15);
        Imgproc.erode(v, v, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new org.opencv.core.Size(11, 11)));
        canny(v, v);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(v, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        final int imageArea = rgba.rows() * rgba.cols();
        final int thresholdMinArea = imageArea / 18;

        return chooseBestContours(contours, thresholdMinArea);
    }

    public static Optional<List<Point>> chooseBestContours(List<MatOfPoint> contours, final int thresholdMinArea) {
        List<Point> bestContours = null;

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

            Log.i(TAG, "s " + maxArea);

            if(maxArea > thresholdMinArea) {
                bestContours = contours.get(index).toList();
            }
        }

        return Optional.ofNullable(bestContours);
    }

    private void startOpenCVThread() {
        openCVThread = new HandlerThread("Open CV Thread");
        openCVThread.start();
        openCVHandler = new Handler(openCVThread.getLooper());
    }

    private void stopOpenCVThread() {
        if(openCVThread != null) {
            openCVThread.quit();
            try {
                openCVThread.join();
                openCVThread = null;
                openCVHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void post(Runnable r) {
        openCVHandler.removeCallbacksAndMessages(null);
        openCVHandler.post(r);
    }

    public static Mat gray(Mat rgba) {
        Mat gray = new Mat();
        Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY, 4);
        return gray;
    }

    public static double otsuThreshold(Mat singleChannel) {
        if(otsu == null)
            otsu = new Mat();
        return Imgproc.threshold(singleChannel, otsu, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
    }

    public static void canny(Mat src, Mat dst) {
        double highThreshold = otsuThreshold(src);
        double lowThreshold = highThreshold / 3;
        Imgproc.Canny(src, dst, lowThreshold, highThreshold);
    }

    public static List<Mat> getHSVChannels(Mat im) {
        List<Mat> hsv = new ArrayList<>(3);
        Core.split(im, hsv);
        return hsv;
    }

    public static Mat rgbaToHsv(Mat rgba) {
        Mat im = new Mat();
        Imgproc.cvtColor(rgba, im, Imgproc.COLOR_RGBA2RGB);
        Imgproc.cvtColor(im, im, Imgproc.COLOR_RGB2HSV);
        return im;
    }

    public static Mat withCanny(Mat rgba) {
        Mat im = new Mat();
        canny(rgba, im);
        Imgproc.cvtColor(im, im, Imgproc.COLOR_GRAY2RGBA);
        Core.bitwise_or(rgba, im, im);
        return im;
    }
}
