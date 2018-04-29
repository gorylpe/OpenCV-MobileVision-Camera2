package com.example.piotr.camera2;

import android.os.Handler;
import android.os.HandlerThread;

public class BackgroundThread {
    private final String name;
    private Handler handler;
    private HandlerThread thread;

    public BackgroundThread(String name) {
        this.name = name;
    }

    public void start() {
        if(thread == null) {
            thread = new HandlerThread(name);
            thread.start();
            handler = new Handler(thread.getLooper());
        }
    }

    public void stop() throws InterruptedException {
        if (thread != null) {
            thread.quit();
            thread.join();
            thread = null;
            handler = null;
        }
    }

    public Handler getHandler() {
        if(handler == null) {
            throw new NullPointerException("BackgroundThread not started");
        }
        return handler;
    }
}
