package com.example.piotr.camera2;

import android.content.Context;
import android.util.Log;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

public class OpenCVController {

    private static final String TAG = "OpenCVController";

    private Context context;
    private BaseLoaderCallback loaderCallback;
    private final InitializedCallback initializedCallback;

    public OpenCVController(Context context, final InitializedCallback initializedCallback) {
        this.context = context;
        this.initializedCallback = initializedCallback;
        this.loaderCallback = new BaseLoaderCallback(context) {
            @Override
            public void onManagerConnected(int status) {
                switch (status) {
                    case LoaderCallbackInterface.SUCCESS:
                        Log.i(TAG, "OpenCV loaded successfully");
                        // Load native library after(!) OpenCV initialization
                        System.loadLibrary("opencv_java3");

                        OpenCVController.this.initializedCallback.onOpenCVInitialized();
                        break;
                    default:
                        super.onManagerConnected(status);
                        break;
                }
            }
        };
    }

    public void initOnResume() {
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this.context, loaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public interface InitializedCallback {
        void onOpenCVInitialized();
    }
}
