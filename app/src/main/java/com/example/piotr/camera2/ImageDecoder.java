package com.example.piotr.camera2;

import android.media.Image;

import java.nio.ByteBuffer;

public class ImageDecoder {

    public static byte[] getRGBA_8888(Image image) {
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
}
