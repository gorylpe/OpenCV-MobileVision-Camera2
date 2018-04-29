package com.example.piotr.camera2.utils;

import android.support.annotation.Nullable;
import android.util.Log;
import org.opencv.android.OpenCVLoader;

public class OpenCVInitializer {

    private static final String TAG = "OpenCVInitializer";

    public static volatile boolean initialized = false;

    public static void init() {
        init(null);
    }

    public static void init(@Nullable final InitializedCallback initializedCallback) {
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "Internal OpenCV library not found.");
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            System.loadLibrary("opencv_java3");

            if(initializedCallback != null)
                initializedCallback.onOpenCVInitialized();

            initialized = true;
        }
    }

    public interface InitializedCallback {
        void onOpenCVInitialized();
    }
}
