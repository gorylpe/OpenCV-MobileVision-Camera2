package com.example.piotr.camera2.utils;

import android.util.Log;

public class StopWatch {
    private static long lastTime = 0;

    public static void start() {
        lastTime = System.nanoTime();
    }

    public static void stop(final String TAG) {
        long diff = System.nanoTime() - lastTime;
        Log.i("d", "Drawing time " + ((double)diff / 1000000) + " ms");
    }
}
