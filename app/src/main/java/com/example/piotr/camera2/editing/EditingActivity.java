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

public class EditingActivity extends AppCompatActivity {

    Bitmap bitmap;
    ArrayList<PointF> quadF;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editing);

        Intent intent = getIntent();
        if(intent == null) {
            finish();
            return;
        }


        if(GlobalBitmap.bitmap == null)
            return;
        bitmap = GlobalBitmap.bitmap;
        GlobalBitmap.bitmap = null;

        quadF = intent.getParcelableArrayListExtra(ScanningActivity.EXTRA_CONTOURS);
        if(quadF == null)
            return;

        Log.i("asd", bitmap.getWidth() + "");
    }
}
