package com.mrtan.qiniu_push;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.controls.Engine;

public class PreviewActivity extends Activity {

    private CameraView camera;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        camera = new CameraView(this);
        camera.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        camera.setEngine(Engine.CAMERA2);
        ((FrameLayout)findViewById(R.id.root)).addView(camera);
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