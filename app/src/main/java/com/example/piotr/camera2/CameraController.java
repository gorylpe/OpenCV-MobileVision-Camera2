package com.example.piotr.camera2;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.net.wifi.aware.Characteristics;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;

public class CameraController {

    private static final String TAG = "CameraController";

    private Context context;

    private CameraManager manager;
    private String cameraId;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuilder;

    private Handler cameraBackgroundHandler;
    private HandlerThread cameraBackgroundThread;

    private Surface targetSurface;
    private Size outputSize;

    public CameraController(Context context) {
        this.context = context;
        manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    public void setCameraId(int i) throws ArrayIndexOutOfBoundsException {
        this.cameraId = getCameraIds()[i];
    }

    public void setCameraId(String cameraId) throws CameraAccessException {
        boolean contains = Arrays.asList(getCameraIds()).contains(cameraId);
        if(!contains) {
            throw new CameraAccessException(CameraAccessException.CAMERA_DISCONNECTED);
        }
        this.cameraId = cameraId;
    }

    public String[] getCameraIds() {
        try {
            return manager.getCameraIdList();
        } catch(CameraAccessException e) {
            e.printStackTrace();
        }
        return new String[0];
    }

    public Size[] getOutputSizes() throws CameraAccessException {
        CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if(map == null) {
            throw new NullPointerException("StreamConfigurationMap is null");
        }
        return map.getOutputSizes(ImageReader.class);
    }

    public void setOutputSize(int i) throws CameraAccessException, ArrayIndexOutOfBoundsException {
        outputSize = getOutputSizes()[i];
    }

    public Size getOutputSize() {
        return outputSize;
    }

    public void setTargetSurface(Surface targetSurface) {
        this.targetSurface = targetSurface;
    }

    public void startCamera() throws CameraAccessException, NullPointerException {
        startBackgroundThread();
        if(targetSurface == null) {
            throw new NullPointerException("Target surface is null");
        }
        if(cameraId == null) {
            throw new NullPointerException("Camera id is null");
        }
        if(outputSize == null) {
            throw new NullPointerException("Output size is null");
        }
        openCamera();
    }

    public void stopCamera() {
        stopBackgroundThread();
        closeCamera();
    }



    private void closeCamera() {
        if(null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    private void openCamera() throws CameraAccessException {
        Log.e(TAG, "openCamera");
        try {
            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    Log.e(TAG, "onOpened");
                    cameraDevice = camera;
                    createCameraPreview();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    cameraDevice.close();
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    cameraDevice.close();
                    cameraDevice = null;
                }
            }, null);
        } catch(SecurityException e) {}
    }

    private void createCameraPreview() {
        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            captureRequestBuilder.addTarget(targetSurface);

            cameraDevice.createCaptureSession(Collections.singletonList(targetSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    // Camera already closed
                    if(null == cameraDevice) {
                        return;
                    }

                    cameraCaptureSession = session;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {}
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if(null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
            return;
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_OFF);
        try {
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, cameraBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startBackgroundThread() {
        cameraBackgroundThread = new HandlerThread("Camera Background");
        cameraBackgroundThread.start();
        cameraBackgroundHandler = new Handler(cameraBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if(cameraBackgroundThread != null) {
            cameraBackgroundThread.quitSafely();
            try {
                cameraBackgroundThread.join();
                cameraBackgroundThread = null;
                cameraBackgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
