package com.example.piotr.camera2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.media.ImageReader;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

public class MainActivity extends AppCompatActivity implements OpenCVController.InitializedCallback {

    private static final String TAG = "CameraTEST";
    public static final int REQUEST_CAMERA_PERMISSION = 200;

    private TextureView textureView;

    private Size imageDimension;
    private ImageReader imageReader;

    private CameraController cameraController;
    private OpenCVController openCVController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        requestCameraPermissions();

        textureView = findViewById(R.id.texture);

        cameraController = new CameraController(this);
        try{
            cameraController.setCameraId(0);
            cameraController.setOutputSize(0);
        } catch(CameraAccessException e) {
            e.printStackTrace();
        }

        openCVController = new OpenCVController(this, this);
        Log.e(TAG, "onCreateEnd");

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");

        openCVController.initOnResume();

        if(textureView.isAvailable()) {
            try{
                cameraController.setTargetSurface(new Surface(textureView.getSurfaceTexture()));
                cameraController.startCamera();
            } catch(CameraAccessException e) {
                e.printStackTrace();
            }
        } else {
            textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    try {
                        cameraController.setTargetSurface(new Surface(textureView.getSurfaceTexture()));
                        cameraController.startCamera();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {

                }
            });
        }
    }


    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        cameraController.stopCamera();
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
