package com.example.piotr.camera2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.hardware.camera2.CameraAccessException;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.util.Size;
import android.view.View;
import org.opencv.core.*;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class MainActivity extends AppCompatActivity implements ImageReader.OnImageAvailableListener{

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private static final int IMAGE_FORMAT = PixelFormat.RGBA_8888;

    private CameraManager cameraManager;
    private ImageCapturer imageCapturer;

    private QuadrilateralComputingTask quadrilateralComputingTask;
    private final int blurSize = 5;
    private final double sizeThreshold = 1.0/18;

    private DocumentScanningPreviewView previewView;
    private CachedBitmap cachedBitmap;

    private Size cameraOutputSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestCameraPermissions();

        previewView = findViewById(R.id.previewView);

        cameraManager = new CameraManager(this);
        imageCapturer = new ImageCapturer(IMAGE_FORMAT);

        cachedBitmap = new CachedBitmap();

        try{
            cameraManager.setCameraId(cameraManager.getCameraIds()[0]);
            cameraOutputSize = cameraManager.getOutputSizes()[0];
            Log.i(TAG, "Avaiable sizes\n" + Arrays.deepToString(cameraManager.getOutputSizes()));
        } catch(CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        forceFullscreen();

        OpenCVInitializer.init();
        startImageProcessing();
    }

    private void forceFullscreen() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private void startImageProcessing() {
        try{
            if(cameraOutputSize == null)
                return;

            imageCapturer.start(this, cameraOutputSize);
            cameraManager.setTargetSurface(imageCapturer.getSurface());
            cameraManager.startCamera();
        } catch(CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        final Image image = reader.acquireLatestImage();
        if (image == null)
            return;

        final int width = image.getWidth();
        final int height = image.getHeight();

        byte[] bytesImage = ImageConverter.getRGBA_8888(image);

        if(OpenCVInitializer.initialized) {
            drawPreviewAndScanOpenCV(width, height, bytesImage);
        } else {
            drawPreview(width, height, bytesImage);
        }

        image.close();
    }

    private void drawPreview(final int width, final int height, byte[] rgbaImageBytes) {
        ByteBuffer buffer = ByteBuffer.wrap(rgbaImageBytes);
        cachedBitmap.setFromByteBuffer(width, height, buffer);

        previewView.drawBitmap(cachedBitmap.getBitmap());
    }

    private void drawPreviewAndScanOpenCV(final int width, final int height, byte[] rgbaImageBytes) {
        final Mat rgba = ImageConverter.RGBA_8888toMat(width, height, rgbaImageBytes);

        final Mat rgbaWithCanny = new Mat();
        ImageProcessor.withCanny(rgba, rgbaWithCanny, blurSize);

        cachedBitmap.setFromMat(rgbaWithCanny);
        previewView.drawBitmap(cachedBitmap.getBitmap());

        if (!cameraManager.getAutoFocusState().isPresent()) {
            Log.i(TAG, "autofocus error");
            return;
        }

        if (!cameraManager.isAutoFocusLockedCorrectly())
            return;

        checkAndExecuteQuadrilateralComputingTask(rgba);
    }

    private void checkAndExecuteQuadrilateralComputingTask(final Mat rgba) {
        if (quadrilateralComputingTask == null || quadrilateralComputingTask.getStatus() != AsyncTask.Status.RUNNING){
            quadrilateralComputingTask = new QuadrilateralComputingTask(this, blurSize, sizeThreshold);
            quadrilateralComputingTask.execute(rgba);
        }
    }

    public static class QuadrilateralComputingTask extends AsyncTask<Mat, Void, Pair<Mat, List<Point>>> {

        private WeakReference<MainActivity> activityReference;
        private final int blurSize;
        private final double sizeThreshold;

        public QuadrilateralComputingTask(MainActivity context, final int blurSize, final double sizeThreshold) {
            activityReference = new WeakReference<>(context);
            this.blurSize = blurSize;
            this.sizeThreshold = sizeThreshold;
        }

        @Override
        protected Pair<Mat, List<Point>> doInBackground(Mat... mats) {
            final Mat rgba = mats[0];
            Pair<Mat, List<Point>> result = null;

            Optional<MatOfPoint> bestContours = ImageProcessor.computeBestContours(rgba, blurSize, sizeThreshold);
            if(bestContours.isPresent()) {
                MatOfPoint2f approx = ImageProcessor.approxPolyDP(bestContours.get());
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
                MainActivity activity = activityReference.get();
                if (activity == null || activity.isFinishing()) return;

                activity.onQuadrilateralObtained(result.first, result.second);
            }
        }
    }

    private void onQuadrilateralObtained(Mat rgba, List<Point> quad) {
        cachedBitmap.setFromMat(rgba);
        previewView.drawBitmap(cachedBitmap.getBitmap());
        previewView.drawContours(quad);

        stopImageProcessing();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "onPause");

        stopImageProcessing();
    }

    private void stopImageProcessing() {
        Log.e(TAG, "Stopping image processing");
        cameraManager.stopCamera();
        Log.e(TAG, "Stopped camera");
        imageCapturer.stop();
        Log.e(TAG, "Stopped image capturer");
        Log.e(TAG, "Stopped image processing");
    }

    private void changeOutputSize(Size newSize) {
        cameraOutputSize = newSize;
        stopImageProcessing();
        startImageProcessing();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                finishAffinity();
            }
        }
    }

    private void requestCameraPermissions() {
        if (!checkCameraPermissions()) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    private boolean checkCameraPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }
}
