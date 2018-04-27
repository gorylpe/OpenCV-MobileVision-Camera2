package com.example.piotr.camera2;

import android.content.Context;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CameraManager {

    private static final String TAG = "CameraManager";

    private android.hardware.camera2.CameraManager manager;
    private String cameraId;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;

    private ReadWriteLock captureResultLock = new ReentrantReadWriteLock();
    private CaptureResult captureResult;

    private Handler cameraBackgroundHandler;
    private HandlerThread cameraBackgroundThread;

    private Surface targetSurface;

    public CameraManager(Context context) {
        manager = (android.hardware.camera2.CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
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
        Log.i(TAG, "openCamera");
        try {
            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    Log.e(TAG, "onOpened");
                    cameraDevice = camera;
                    createCaptureSession();
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

    private void createCaptureSession() {
        try {
            cameraDevice.createCaptureSession(Arrays.asList(targetSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    cameraCaptureSession = session;
                    startPreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {}
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startPreview() {
        try {
            CaptureRequest captureRequest = createPreviewCameraRequest();
            if(captureRequest != null) {
                cameraCaptureSession.prepare(targetSurface);
                cameraCaptureSession.setRepeatingRequest(captureRequest, captureCallback, cameraBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CaptureRequest createPreviewCameraRequest() {
        try {
            CaptureRequest.Builder captureRequestBuilder;

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(targetSurface);

            return captureRequestBuilder.build();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *  Object used to acquire last capture result
     */
    private final CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);

            captureResultLock.writeLock().lock();
            captureResult = result;
            captureResultLock.writeLock().unlock();
        }
    };

    public Optional<Integer> getAutoFocusState() {
        captureResultLock.readLock().lock();
        if(captureResult == null)
            return Optional.empty();
        captureResultLock.readLock().unlock();
        return Optional.ofNullable(captureResult.get(CaptureResult.CONTROL_AF_STATE));
    }

    public boolean isAutoFocusLockedCorrectly() {
        boolean result = false;
        if(getAutoFocusState().isPresent()) {
            int state = getAutoFocusState().get();
            result = state == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
                    || state == CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED;
        }
        return result;
    }

    private void startBackgroundThread() {
        if(cameraBackgroundThread == null) {
            cameraBackgroundThread = new HandlerThread("Camera Background");
            cameraBackgroundThread.start();
            cameraBackgroundHandler = new Handler(cameraBackgroundThread.getLooper());
        }
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
