package com.example.piotr.camera2;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.hardware.camera2.CameraAccessException;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements OpenCVController.InitializedCallback {

    private static final String TAG = "CameraTEST";
    public static final int REQUEST_CAMERA_PERMISSION = 200;

    private DocumentScanningPreviewView previewView;

    private ImageReader imageReader;
    private CachedBitmap cachedBitmap;

    private Handler imageProcessingHandler;
    private HandlerThread imageProcessingThread;

    private CameraController cameraController;
    private OpenCVController openCVController;

    final private Object framesLock = new Object();
    private int frames = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate");
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        requestCameraPermissions();

        previewView = findViewById(R.id.previewView);

        openCVController = new OpenCVController(this, this);
        cameraController = new CameraController(this);
        try{
            cameraController.setCameraId(0);
            cameraController.setOutputSize(0);
            Log.i(TAG, "Avaiable sizes\n" + Arrays.deepToString(cameraController.getOutputSizes()));
        } catch(CameraAccessException e) {
            e.printStackTrace();
        }

        Size imageDimension = cameraController.getOutputSize();
        imageReader = ImageReader.newInstance(imageDimension.getWidth(), imageDimension.getHeight(), PixelFormat.RGBA_8888, 2);

        cachedBitmap = new CachedBitmap();

        cameraController.setTargetSurface(imageReader.getSurface());
        //timer();
        Log.e(TAG, "onCreateEnd");
    }

    private void startBackgroundThread() {
        imageProcessingThread = new HandlerThread("Image processing");
        imageProcessingThread.start();
        imageProcessingHandler = new Handler(imageProcessingThread.getLooper());
    }

    private void stopBackgroundThread() {
        if(imageProcessingThread != null) {
            imageProcessingThread.quitSafely();
            try {
                imageProcessingThread.join();
                imageProcessingThread = null;
                imageProcessingHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        startBackgroundThread();

        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {

                final Image image = imageReader.acquireLatestImage();

                int width = image.getWidth();
                int height = image.getHeight();

                ByteBuffer buffer = ByteBuffer.wrap(ImageDecoder.getRGBA_8888(image));
                cachedBitmap.setFromByteBuffer(width, height, buffer);
                previewView.drawBitmap(cachedBitmap.getBitmap());

                image.close();
            }
        }, imageProcessingHandler);


        try{
            cameraController.startCamera();
        } catch(CameraAccessException e) {
            e.printStackTrace();
        }
        openCVController.initOnResume();
    }


    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        cameraController.stopCamera();
        stopBackgroundThread();
        super.onPause();
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

    @Override
    public void onOpenCVInitialized() {

    }
}
