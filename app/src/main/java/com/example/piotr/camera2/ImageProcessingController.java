package com.example.piotr.camera2;

import android.content.Context;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ImageProcessingController implements ImageReader.OnImageAvailableListener, OpenCVManager.InitializedCallback{

    private static final String TAG = "ImageProcessingController";

    private Context context;

    private DocumentScanningPreviewView previewView;

    private Handler readerHandler;
    private HandlerThread readerThread;

    private Handler openCVHandler;
    private HandlerThread openCVThread;

    private ImageReader imageReader;
    private CachedBitmap cachedBitmap;

    private boolean isOpenCVInitialized = false;

    private Mat rgba;

    public ImageProcessingController(Context context) {
        this.context = context;
        cachedBitmap = new CachedBitmap();
    }

    public void setPreviewView(DocumentScanningPreviewView previewView) {
        this.previewView = previewView;
    }

    public void configure(CameraManager cameraManager, Size imageSize) {
        startReaderThread();
        startOpenCVThread();

        imageReader = ImageReader.newInstance(imageSize.getWidth(), imageSize.getHeight(), PixelFormat.RGBA_8888, 2);
        imageReader.setOnImageAvailableListener(this, readerHandler);

        cameraManager.setTargetSurface(imageReader.getSurface());

        new OpenCVManager(context, this).init();
    }

    public void stop() {
        stopReaderThread();
        stopOpenCVThread();
    }

    @Override
    public void onOpenCVInitialized() {
        isOpenCVInitialized = true;
    }

    @Override
    public void onImageAvailable(ImageReader reader) {

        final Image image = imageReader.acquireLatestImage();

        if (image == null)
            return;

        int width = image.getWidth();
        int height = image.getHeight();

        byte[] bytesImage = ImageDecoder.getRGBA_8888(image);
        if(!isOpenCVInitialized) {
            ByteBuffer buffer = ByteBuffer.wrap(bytesImage);
            cachedBitmap.setFromByteBuffer(width, height, buffer);
        } else {
            rgba = new Mat(height, width, CvType.CV_8UC4);
            rgba.put(0, 0, bytesImage);

            cachedBitmap.setFromMat(rgba);

            openCVHandler.post(openCVComputing);
        }

        previewView.drawBitmap(cachedBitmap.getBitmap());
        image.close();
    }

    private final Runnable openCVComputing = new Runnable() {
        @Override
        public void run() {
            Mat gray = new Mat();
            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY, 4);

            Mat intermediate = new Mat();

            Imgproc.GaussianBlur(gray, intermediate, new org.opencv.core.Size(5,5), 0);
            Imgproc.Canny(intermediate, intermediate, 60, 90);
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(intermediate, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            if(contours.size() > 0) {
                int index = 0;
                double maxArea = Imgproc.contourArea(contours.get(0));

                for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {

                    double tempArea = Imgproc.contourArea(contours.get(contourIdx));
                    if(tempArea > maxArea) {
                        maxArea = tempArea;
                        index = contourIdx;
                    }
                }

                final int imageArea = rgba.rows() * rgba.cols();
                final int thresholdMinArea = imageArea / 18;

                if(maxArea > thresholdMinArea) {
                    previewView.drawContours(contours.get(index).toList());
                }
            }

        }
    };

    private void startReaderThread() {
        readerThread = new HandlerThread("Image processing");
        readerThread.start();
        readerHandler = new Handler(readerThread.getLooper());
    }

    private void stopReaderThread() {
        if(readerThread != null) {
            readerThread.quitSafely();
            try {
                readerThread.join();
                readerThread = null;
                readerHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void startOpenCVThread() {
        openCVThread = new HandlerThread("Open CV Thread");
        openCVThread.start();
        openCVHandler = new Handler(openCVThread.getLooper());
    }

    private void stopOpenCVThread() {
        if(openCVThread != null) {
            openCVThread.quitSafely();
            try {
                openCVThread.join();
                openCVThread = null;
                openCVHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
