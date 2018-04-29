package com.example.piotr.camera2;

import android.media.ImageReader;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

public class ImageCapturer {

    private static final String TAG = "ImageCapturer";
    public final int IMAGE_FORMAT;

    private ImageReader imageReader;

    private BackgroundThread bgThread;

    public ImageCapturer(final int format) {
        IMAGE_FORMAT = format;

        bgThread = new BackgroundThread(TAG);
    }

    public void start(ImageReader.OnImageAvailableListener listener, Size imageSize) {
        bgThread.start();

        imageReader = ImageReader.newInstance(imageSize.getWidth(), imageSize.getHeight(), IMAGE_FORMAT, 2);
        imageReader.setOnImageAvailableListener(listener, bgThread.getHandler());
        Log.i(TAG, "Image capturer configured");
    }

    public void stop() {
        try {
            bgThread.stop();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }

    public Surface getSurface() {
        if(imageReader != null)
            return imageReader.getSurface();
        else{
            throw new NullPointerException("You didn't configured ImageCapturer!");
        }
    }
}
