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
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;
import android.view.View;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements OpenCVController.InitializedCallback {

    private static final String TAG = "CameraTEST";
    public static final int REQUEST_CAMERA_PERMISSION = 200;

    private TextureView textureView;

    private Size imageDimension;
    private ImageReader imageReader;
    private Bitmap cacheBitmap;
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

        textureView = findViewById(R.id.texture);

        openCVController = new OpenCVController(this, this);
        cameraController = new CameraController(this);
        try{
            cameraController.setCameraId(0);
            cameraController.setOutputSize(0);
            Log.i(TAG, "Avaiable sizes\n" + Arrays.deepToString(cameraController.getOutputSizes()));
        } catch(CameraAccessException e) {
            e.printStackTrace();
        }

        imageDimension = cameraController.getOutputSize();
        imageReader = ImageReader.newInstance(imageDimension.getWidth(), imageDimension.getHeight(), PixelFormat.RGBA_8888, 2);

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

    public static byte[] imageRGBAToByteArray(Image image) {
        ByteBuffer buffer;
        int rowStride;
        int pixelStride;
        int width = image.getWidth();
        int height = image.getHeight();
        int offset = 0;

        Image.Plane[] planes = image.getPlanes();
        buffer = planes[0].getBuffer();
        rowStride = planes[0].getRowStride();
        pixelStride = planes[0].getPixelStride();

        byte[] data = new byte[image.getWidth() * image.getHeight() * 4];

        final int rowLength = width * pixelStride;

        for (int row = 0; row < height; row++) {
            buffer.get(data, offset, rowLength);

            // Advance buffer the remainder of the row stride, unless on the last row.
            // Otherwise, this will throw an IllegalArgumentException because the buffer
            // doesn't include the last padding.
            if (row != height - 1) {
                buffer.position(buffer.position() + rowStride - rowLength);
            }
            offset += rowLength;
        }

        return data;
    }

    private void timer() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Log.e(TAG, "FPS: " + frames);
                synchronized (framesLock){
                    frames = 0;
                }
            }
        }, 1000, 1000);
    }

    private void drawOnTexture(Bitmap bitmap) {
        synchronized (framesLock){
            frames++;
        }

        Canvas canvas = textureView.lockCanvas();
        if (canvas != null) {
            canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);

            Matrix mat = new Matrix();

            final int bitmapWidth = bitmap.getWidth();
            final int bitmapHeight = bitmap.getHeight();
            final int canvasWidth = canvas.getWidth();
            final int canvasHeight = canvas.getHeight();

            final int dcenterx = (canvasWidth - bitmapWidth) / 2;
            final int dcentery = (canvasHeight - bitmapHeight) / 2;

            //move to center
            mat.postTranslate(dcenterx, dcentery);
            //rotate 90 degrees relative to center
            mat.postRotate(90, canvasWidth / 2, canvasHeight / 2);

            //scale to full screen
            final float scale = Math.max((float)canvasWidth / bitmapHeight, (float)canvasHeight / bitmapWidth);
            mat.postScale(scale, scale, canvasWidth / 2, canvasHeight / 2);

            canvas.drawBitmap(bitmap, mat, null);

            textureView.unlockCanvasAndPost(canvas);
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
                long startTime = System.nanoTime();
                long difference = 0;
                long lastTime = startTime;

                final Image image = imageReader.acquireLatestImage();

                int bitmapWidth = imageDimension.getWidth();
                int bitmapHeight = imageDimension.getHeight();

                if(cacheBitmap == null || bitmapWidth != cacheBitmap.getWidth() || bitmapHeight != cacheBitmap.getHeight() ) {
                    cacheBitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
                }

                difference = System.nanoTime() - lastTime;
                lastTime = System.nanoTime();
                Log.i(TAG, "Creating bitmap: " + (double)difference / 1000000 + "ms");

                byte[] bytes = imageRGBAToByteArray(image);

                difference = System.nanoTime() - lastTime;
                lastTime = System.nanoTime();
                Log.i(TAG, "Image to byte array: " + (double)difference / 1000000 + "ms");

                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                cacheBitmap.copyPixelsFromBuffer(buffer);

                lastTime = System.nanoTime();

                drawOnTexture(cacheBitmap);

                difference = System.nanoTime() - lastTime;
                Log.i(TAG, "Draw on texture: " + (double)difference / 1000000 + "ms");
                image.close();

                difference = System.nanoTime() - startTime;
                Log.i(TAG, "TOTAL: " + (double)difference / 1000000 + "ms");
            }
        }, imageProcessingHandler);


        try{
            cameraController.startCamera();
        } catch(CameraAccessException e) {
            e.printStackTrace();
        }
        openCVController.initOnResume();

        /*if(textureView.isAvailable()) {
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
        }*/
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
