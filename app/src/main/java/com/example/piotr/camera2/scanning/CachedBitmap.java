package com.example.piotr.camera2.scanning;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.util.Log;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.nio.ByteBuffer;
import java.util.Optional;

public class CachedBitmap {
    private Bitmap bitmap;
    private int lastWidth = 0;
    private int lastHeight = 0;

    public void setFromByteBuffer(final int width, final int height, ByteBuffer buffer) {
        checkAndInitBitmap(width, height);
        bitmap.copyPixelsFromBuffer(buffer);
    }

    public void setFromBitmap(@NonNull Bitmap bmp) {
        ByteBuffer buffer = ByteBuffer.allocate(bmp.getByteCount());
        bmp.copyPixelsToBuffer(buffer);
        buffer.rewind();
        setFromByteBuffer(bmp.getWidth(), bmp.getHeight(), buffer);
    }

    public void setFromMat(Mat mat) {
        final int width = mat.cols();
        final int height = mat.rows();
        checkAndInitBitmap(width, height);
        Utils.matToBitmap(mat, bitmap);
    }

    public Optional<Bitmap> getBitmap() {
        return Optional.ofNullable(bitmap);
    }

    private void checkAndInitBitmap(final int width, final int height) {
        if(sizesDiffersFromLastOrNoBitmap(width, height)) {
            initBitmap(width, height);
        }
    }

    private boolean sizesDiffersFromLastOrNoBitmap(final int width, final int height) {
        return (bitmap == null || lastWidth != width || lastHeight == height);
    }

    private void initBitmap(int width, int height) {
        if(bitmap == null)
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        else
            bitmap.reconfigure(width, height, bitmap.getConfig());
        lastWidth = width;
        lastHeight = height;
    }
}
