package com.example.piotr.camera2;

import android.graphics.PixelFormat;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import java.util.Optional;

public class ImageCapturer {

    private static final String TAG = "ImageCapturer";

    public static final int format = PixelFormat.RGBA_8888;
    private ImageReader imageReader;

    private Handler readerHandler;
    private HandlerThread readerThread;

    public void configure(ImageReader.OnImageAvailableListener listener, Size imageSize) {
        startReaderThread();

        imageReader = ImageReader.newInstance(imageSize.getWidth(), imageSize.getHeight(), format, 2);
        imageReader.setOnImageAvailableListener(listener, readerHandler);
        Log.i(TAG, "Image capturer configured");
    }

    public void stop() {
        stopReaderThread();
        imageReader.close();
        imageReader = null;
    }

    public Surface getSurface() {
        if(imageReader != null)
            return imageReader.getSurface();
        else{
            throw new NullPointerException("You didn't configured ImageCapturer!");
        }
    }

    private void startReaderThread() {
        readerThread = new HandlerThread("Image processing");
        readerThread.start();
        readerHandler = new Handler(readerThread.getLooper());
    }

    private void stopReaderThread() {
        if (readerThread != null) {
            readerThread.quit();
            try {
                Log.i(TAG, "stopping image reader thread");
                readerThread.join();
                Log.i(TAG, "stopped image reader thread");
                readerThread = null;
                readerHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
