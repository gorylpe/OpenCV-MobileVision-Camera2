package com.example.piotr.camera2;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.media.Image;
import android.media.ImageReader;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import org.opencv.core.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ImageReader.OnImageAvailableListener{

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 200;

    private DocumentScanningPreviewView previewView;

    private CameraManager cameraManager;
    private ImageCapturer imageCapturer;
    private ImageProcessor imageProcessor;

    private CachedBitmap cachedBitmap;

    private Size cameraOutputSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        requestCameraPermissions();

        previewView = findViewById(R.id.previewView);

        cameraManager = new CameraManager(this);
        imageCapturer = new ImageCapturer();
        imageProcessor = new ImageProcessor(this);

        cachedBitmap = new CachedBitmap();

        try{
            cameraManager.setCameraId(0);
            cameraOutputSize = cameraManager.getOutputSizes()[0];
            Log.i(TAG, "Avaiable sizes\n" + Arrays.deepToString(cameraManager.getOutputSizes()));
        } catch(CameraAccessException e) {
            e.printStackTrace();
        }
        //timer();
        Log.e(TAG, "onCreateEnd");
    }

    private void forceFullscreen() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    @Override
    public void onImageAvailable(ImageReader reader) {

        final Image image = reader.acquireLatestImage();
        if (image == null)
            return;

        int width = image.getWidth();
        int height = image.getHeight();

        byte[] bytesImage = ImageConverter.getRGBA_8888(image);

        drawPreview(width, height, bytesImage);
        if(imageProcessor.isOpenCVInitialized()) {
            scan(width, height, bytesImage);
        }

        image.close();
    }

    private void drawPreview(final int width, final int height, byte[] rgbaImageBytes) {
        ByteBuffer buffer = ByteBuffer.wrap(rgbaImageBytes);
        cachedBitmap.setFromByteBuffer(width, height, buffer);

        previewView.drawBitmap(cachedBitmap.getBitmap());
    }

    private void scan(final int width, final int height, byte[] rgbaImageBytes) {
        Mat rgba = ImageConverter.RGBA_8888toMat(width, height, rgbaImageBytes);

        if(!cameraManager.getAutoFocusState().isPresent()) {
            Log.i(TAG, "autofocus error");
        }
        imageProcessor.post(() -> {
            if(cameraManager.isAutoFocusLockedCorrectly()) {
                imageProcessor.computeBestContours(rgba, 5, 1.0/18)
                .ifPresent(bestContours -> {
                    MainActivity.this.onContoursObtained(rgba, bestContours);
                });
            }
        });
    }

    private void onContoursObtained(Mat rgba, MatOfPoint contours) {
        MatOfPoint2f approx = ImageProcessor.approxPolyDP(contours);
        List<Point> approxList = approx.toList();
        approx.release();

        //Quad
        if(approxList.size() == 4) {
            onQuadrilateralObtained(rgba, approxList);
        }
    }

    private void onQuadrilateralObtained(Mat rgba, List<Point> quad) {
        cachedBitmap.setFromMat(rgba);
        previewView.drawBitmap(cachedBitmap.getBitmap());
        previewView.drawContours(quad);

        stopImageProcessing();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        forceFullscreen();

        startImageProcessing();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "onPause");

        stopImageProcessing();
    }

    private void startImageProcessing() {
        try{
            if(cameraOutputSize == null)
                return;

            imageProcessor.start();
            imageCapturer.configure(this, cameraOutputSize);
            Surface targetSurface = imageCapturer.getSurface();
            cameraManager.setTargetSurface(targetSurface);
            cameraManager.startCamera();
        } catch(CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void stopImageProcessing() {
        Log.e(TAG, "Stopping image processing");
        imageCapturer.stop();
        Log.e(TAG, "Stopped image capturer");
        imageProcessor.stop();
        Log.e(TAG, "Stopped image processor");
        cameraManager.stopCamera();
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
