package com.example.piotr.camera2.scanning;

import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Pair;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Optional;

public class QuadrilateralComputingTask extends AsyncTask<Mat, Void, Pair<Mat, List<Point>>> {

    private WeakReference<QuadrilateralCallback> callbackReference;
    private final int blurSize;
    private final double sizeThreshold;

    public QuadrilateralComputingTask(ScanningActivity context, final int blurSize, final double sizeThreshold) {
        callbackReference = new WeakReference<>(context);
        this.blurSize = blurSize;
        this.sizeThreshold = sizeThreshold;
    }

    @Override
    protected Pair<Mat, List<Point>> doInBackground(Mat... mats) {
        final Mat rgba = mats[0];
        Pair<Mat, List<Point>> result = null;

        Optional<MatOfPoint> bestContours = ImageContoursProcessor.computeBestContours(rgba, blurSize, sizeThreshold);
        if(bestContours.isPresent()) {
            MatOfPoint2f approx = ImageContoursProcessor.approxPolyDP(bestContours.get());
            List<Point> approxList = approx.toList();
            approx.release();

            //Quad
            if(approxList.size() == 4) {
                result = new Pair<>(rgba, approxList);
            }
        }

        return result;
    }

    @Override
    protected void onPostExecute(@Nullable Pair<Mat, List<Point>> result) {
        if(result != null) {
            QuadrilateralCallback callback = callbackReference.get();
            if (callback == null) return;

            callback.onObtained(result.first, result.second);
        }
    }
}

