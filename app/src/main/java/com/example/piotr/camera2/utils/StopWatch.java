package com.example.piotr.camera2.utils;

import android.util.Log;

public class StopWatch {
    private static long lastTime = 0;

    public static void start() {
        lastTime = System.nanoTime();
    }

    public static long stop() {
        return System.nanoTime() - lastTime;
    }

    public static long stop(final String TAG) {
        long diff = stop();
        Log.i("d", "Drawing time " + ((double)diff / 1000000) + " ms");
        return diff;
    }
}
