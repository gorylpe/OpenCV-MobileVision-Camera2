package com.example.piotr.camera2;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity  {

    private static final String TAG = "CameraTEST";
    public static final int REQUEST_CAMERA_PERMISSION = 200;

    private DocumentScanningPreviewView previewView;

    private CameraManager cameraManager;
    private ImageProcessingController imageProcessingController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        forceFullscreen();

        requestCameraPermissions();

        previewView = findViewById(R.id.previewView);

        cameraManager = new CameraManager(this);
        imageProcessingController = new ImageProcessingController(this);
        imageProcessingController.setPreviewView(previewView);

        try{
            cameraManager.setCameraId(0);
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
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");

        try{
            Size imageSize = cameraManager.getOutputSizes()[0];
            imageProcessingController.configure(cameraManager, imageSize);
            cameraManager.startCamera();
        } catch(CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        imageProcessingController.stop();
        cameraManager.stopCamera();
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
}
