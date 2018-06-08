package com.example.piotr.camera2.editing;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import com.example.piotr.camera2.R;
import com.example.piotr.camera2.scanning.ScanningActivity;
import com.example.piotr.camera2.utils.GlobalBitmap;

import java.util.ArrayList;
import java.util.List;

public class EditingActivity extends AppCompatActivity {

    EditingView editingView;

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
        final Bitmap bmp = GlobalBitmap.bitmap;
        GlobalBitmap.bitmap = null;

        final List<PointF> quadF = intent.getParcelableArrayListExtra(ScanningActivity.EXTRA_CONTOURS);
        if(quadF == null)
            return;

        final boolean rotate90fix = intent.getBooleanExtra(ScanningActivity.EXTRA_ROTATE90FIX, false);

        editingView.setRotate90Fix(rotate90fix);
        editingView.setNewImageWithContours(bmp, quadF);
    }
}
