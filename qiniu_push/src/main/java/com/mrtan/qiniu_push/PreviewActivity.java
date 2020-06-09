package com.mrtan.qiniu_push;

import android.app.Activity;
import android.os.Bundle;

import com.otaliastudios.cameraview.CameraView;

public class PreviewActivity extends Activity {

    private CameraView camera;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        camera = findViewById(R.id.camera);
    }

    @Override
    protected void onResume() {
        super.onResume();
        camera.open();
    }

    @Override
    protected void onPause() {
        super.onPause();
        camera.close();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        camera.destroy();
    }
}