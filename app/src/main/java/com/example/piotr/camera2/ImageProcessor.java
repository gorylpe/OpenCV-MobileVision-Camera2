package com.example.piotr.camera2;

import android.content.Context;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ImageProcessor implements OpenCVManager.InitializedCallback{

    private static final String TAG = "ImageProcessor";

    private Context context;

    private Handler openCVHandler;
    private HandlerThread openCVThread;

    private boolean isOpenCVInitialized = false;

    public ImageProcessor(Context context) {
        this.context = context;
    }

    public void start() {
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
        List<Point> bestContours = null;

        Mat gray = new Mat();
        Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY, 4);

        Mat intermediate = new Mat();

        Imgproc.GaussianBlur(gray, intermediate, new org.opencv.core.Size(5,5), 0);
        Imgproc.Canny(intermediate, intermediate, 60, 90);
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(intermediate, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        if(contours.size() > 0) {
            int index = 0;
            double maxArea = Imgproc.contourArea(contours.get(0));

            for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {

                double tempArea = Imgproc.contourArea(contours.get(contourIdx));
                if(tempArea > maxArea) {
                    maxArea = tempArea;
                    index = contourIdx;
                }
            }

            final int imageArea = rgba.rows() * rgba.cols();
            final int thresholdMinArea = imageArea / 18;

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
            openCVThread.quitSafely();
            try {
                openCVThread.join();
                openCVThread = null;
                openCVHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
