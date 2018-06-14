package com.example.piotr.camera2.scanning;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.*;
import android.hardware.camera2.CameraAccessException;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import com.example.piotr.camera2.R;
import com.example.piotr.camera2.editing.EditingActivity;
import com.example.piotr.camera2.utils.GlobalVars;
import com.example.piotr.camera2.utils.OpenCVHelperFuncs;
import com.example.piotr.camera2.utils.OpenCVInitializer;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.core.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//TODO MOTION DETECTION
public class ScanningActivity extends AppCompatActivity implements ImageReader.OnImageAvailableListener, QuadrilateralCallback{

    public static final String EXTRA_CONTOURS = "com.example.piotr.camera2.scanning.CONTOURS";
    public static final String EXTRA_ROTATE90FIX = "com.example.piotr.camera2.scanning.ROTATE90FIX";

    private static final String TAG = "ScanningActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private static final int IMAGE_FORMAT = PixelFormat.RGBA_8888;

    private CameraManager cameraManager;
    private Size cameraOutputSize;
    private ImageCapturer imageCapturer;

    private QuadrilateralComputingTask quadrilateralComputingTask;
    private final int blurSize = 5;
    private final double sizeThreshold = 1.0/18;

    private final CachedBitmap finalBitmap = new CachedBitmap();

    private ScanningPreviewView previewView;

    private boolean rotate90fix;

    private boolean editingActivityStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanning);

        requestCameraPermissions();

        previewView = findViewById(R.id.previewView);
        //camera rotation fix
        rotate90fix = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        previewView.rotate90fix(rotate90fix);

        cameraManager = new CameraManager(this);
        imageCapturer = new ImageCapturer(IMAGE_FORMAT);

        try{
            cameraManager.setCameraId(cameraManager.getCameraIds()[0]);
            cameraOutputSize = cameraManager.getOutputSizes()[0];
            Log.i(TAG, "Avaiable sizes\n" + Arrays.deepToString(cameraManager.getOutputSizes()));
        } catch(CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //TODO REMOVE DEBUG
    private void debug() {
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.debug).copy(Bitmap.Config.ARGB_8888, false);

        final Mat rgba = new Mat(bmp.getWidth(), bmp.getHeight(), CvType.CV_8UC4);
        Utils.bitmapToMat(bmp, rgba);

        previewView.setNewImage(rgba);
        previewView.redraw();

        checkAndExecuteQuadrilateralComputingTask(rgba);

        bmp.recycle();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        forceFullscreen();

        OpenCVInitializer.init();
        debug(); //TODO REMOVE DEUBG
        startImageProcessing();

        editingActivityStarted = false;
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
        }

        image.close();
    }

    private void drawPreviewAndScanOpenCV(final int width, final int height, byte[] rgbaImageBytes) {
        final Mat rgba = ImageConverter.RGBA_8888toMat(width, height, rgbaImageBytes);

        previewView.setNewImage(rgba);
        previewView.redraw();

        if (!cameraManager.getAutoFocusState().isPresent()) {
            Log.i(TAG, "autofocus error");
            checkAutofocusAndRestartIfNeeded();
            return;
        }

        if (!cameraManager.isAutoFocusLockedCorrectly())
            return;

        checkAndExecuteQuadrilateralComputingTask(rgba);
    }

    private void checkAutofocusAndRestartIfNeeded() {
        if(!cameraManager.getAutoFocusState().isPresent()) {
            stopImageProcessing();
            runOnUiThread(() -> buildAlertDialogAutofocusError().show());
            Log.e(TAG, "Autofocus error dialog");
        }
    }

    private void checkAndExecuteQuadrilateralComputingTask(final Mat rgba) {
        if (quadrilateralComputingTask == null || quadrilateralComputingTask.getStatus() != AsyncTask.Status.RUNNING){
            quadrilateralComputingTask = new QuadrilateralComputingTask(this, blurSize, sizeThreshold);
            quadrilateralComputingTask.execute(rgba);
        }
    }

    public void onObtained(Mat rgba, List<Point> quad) {
        orientationCorrectionRGBA(rgba);
        finalBitmap.setFromMat(rgba);
        if(!finalBitmap.getBitmap().isPresent())
            return;

        ArrayList<PointF> quadF = OpenCVHelperFuncs.convertListOfPoints(quad);
        orientationCorrectionQuad(quadF, finalBitmap.getBitmap().get().getWidth());

        //Orientation corrected so rotate90fix no needed later
        startEditingActivity(finalBitmap.getBitmap().get(), quadF, false);
    }

    private void orientationCorrectionRGBA(final Mat rgba) {
        if(rotate90fix)
            Core.rotate(rgba, rgba, Core.ROTATE_90_CLOCKWISE);
    }

    private void orientationCorrectionQuad(final List<PointF> quadF, final int bitmapNewWidth) {
        if(rotate90fix) {
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            matrix.postTranslate(bitmapNewWidth, 0);

            final float[] pointTmp = new float[2];
            for(PointF p : quadF) {
                pointTmp[0] = p.x;
                pointTmp[1] = p.y;
                matrix.mapPoints(pointTmp);
                p.x = pointTmp[0];
                p.y = pointTmp[1];
            }
        }
    }

    private void startEditingActivity(Bitmap bitmap, ArrayList<PointF> quadF, final boolean rotate90fix) {
        Intent intent = new Intent(this, EditingActivity.class);
        GlobalVars.bitmap = bitmap;
        intent.putExtra(EXTRA_CONTOURS, quadF);
        intent.putExtra(EXTRA_ROTATE90FIX, rotate90fix);

        synchronized (this) {
            if(!editingActivityStarted) {
                editingActivityStarted = true;
                if(quadrilateralComputingTask != null && quadrilateralComputingTask.getStatus() == AsyncTask.Status.RUNNING) {
                    quadrilateralComputingTask.cancel(true);
                }
                startActivity(intent);
            }
        }
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

    private AlertDialog buildAlertDialogAutofocusError() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        return builder.setTitle("Auto-focus error")
                .setMessage("Camera has to be restarted")
                .setPositiveButton("Restart", (dialog, which) -> {
                    startImageProcessing();
                    checkAutofocusAndRestartIfNeeded();
                })
                .setNegativeButton("Exit", (dialog, which) -> finish())
                .create();
    }
}
