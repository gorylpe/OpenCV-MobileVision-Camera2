package com.example.piotr.camera2.editing;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import com.example.piotr.camera2.R;
import com.example.piotr.camera2.scanning.ScanningActivity;
import com.example.piotr.camera2.utils.BitmapDrawingUtils;
import com.example.piotr.camera2.utils.GlobalBitmap;
import com.example.piotr.camera2.utils.OpenCVInitializer;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EditingActivity extends AppCompatActivity {

    private static final String TAG = "EditingActivity";

    public static final String EXTRA_CONTOURS = "com.example.piotr.camera2.editing.CONTOURS";

    private EditingView editingView;

    private ArrayList<PointF> quadF;
    private Bitmap bmp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editing);

        Intent intent = getIntent();
        if(intent == null) {
            finish();
            return;
        }

        editingView = findViewById(R.id.editing_view);

        if(GlobalBitmap.bitmap == null)
            return;
        bmp = GlobalBitmap.bitmap;

        quadF = intent.getParcelableArrayListExtra(ScanningActivity.EXTRA_CONTOURS);
        if(quadF == null)
            return;

        final boolean rotate90fix = intent.getBooleanExtra(ScanningActivity.EXTRA_ROTATE90FIX, false);

        editingView.setRotate90Fix(rotate90fix);
        editingView.setNewImageWithContours(bmp, quadF);
    }

    @Override
    protected void onResume() {
        super.onResume();
        OpenCVInitializer.init();
    }

    private void transformBitmap() {

        ArrayList<Point> sourcePoints = new ArrayList<>(4);
        PointF topleft = quadF.stream().min((o1, o2) -> Float.compare(o1.x + o1.y, o2.x + o2.y)).orElseGet(PointF::new);
        sourcePoints.add(new Point(topleft.x, topleft.y));

        PointF topright = quadF.stream().max((o1, o2) -> Float.compare(o1.x - o1.y, o2.x - o2.y)).orElseGet(PointF::new);
        sourcePoints.add(new Point(topright.x, topright.y));

        PointF bottomright = quadF.stream().max((o1, o2) -> Float.compare(o1.x + o1.y, o2.x + o2.y)).orElseGet(PointF::new);
        sourcePoints.add(new Point(bottomright.x, bottomright.y));

        PointF bottomleft = quadF.stream().min((o1, o2) -> Float.compare(o1.x - o1.y, o2.x - o2.y)).orElseGet(PointF::new);
        sourcePoints.add(new Point(bottomleft.x, bottomleft.y));

        MatOfPoint2f sourceMat = new MatOfPoint2f();
        sourceMat.fromList(sourcePoints);

        RotatedRect boundingBox = Imgproc.minAreaRect(sourceMat);
        int width = (int)boundingBox.size.width;
        int height = (int)boundingBox.size.height;

        ArrayList<Point> targetPoints = new ArrayList<>(4);
        targetPoints.add(new Point(0.0, 0.0));
        targetPoints.add(new Point(width - 1, 0.0));
        targetPoints.add(new Point(width - 1, height - 1));
        targetPoints.add(new Point(0.0, height - 1));

        MatOfPoint2f targetMat = new MatOfPoint2f();
        targetMat.fromList(targetPoints);

        Mat transformation = Imgproc.getPerspectiveTransform(sourceMat, targetMat);

        Mat source = new Mat(bmp.getWidth(), bmp.getHeight(), CvType.CV_8UC4);
        Utils.bitmapToMat(bmp, source);

        Mat transformed = new Mat(width, height, CvType.CV_8UC4);
        Imgproc.warpPerspective(source, transformed, transformation, new Size(width, height));

        Bitmap newBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Utils.matToBitmap(transformed, newBmp);

        quadF = new ArrayList<>(4);
        for(Point p : targetMat.toList()) {
            quadF.add(new PointF((float)p.x, (float)p.y));
        }

        editingView.setScaleType(BitmapDrawingUtils.ScaleType.FIT);
        editingView.setNewImageWithContours(newBmp, quadF);

        Log.i(TAG, sourcePoints.toString());
        Log.i(TAG, targetPoints.toString());
    }

    public void btnDone(View view) {
        transformBitmap();
    }
}
