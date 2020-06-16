package com.otaliastudios.cameraview;

import android.app.Application;
import android.util.Log;

import com.otaliastudios.cameraview.engine.CameraEngine;
import com.taobao.weex.WXSDKEngine;
import com.taobao.weex.common.WXException;

import io.dcloud.weex.AppHookProxy;

public class CameraAppProxy implements AppHookProxy {
    @Override
    public void onCreate(Application application) {
        Log.i("CameraProxy","onCreate");
        try {
            WXSDKEngine.registerComponent("mrtan-camera", CameraViewComponent.class);
            Log.i("CameraProxy","mrtan-camera init success");
        } catch (WXException e){
            Log.i("CameraProxy","crash");
            e.printStackTrace();
            Log.i("CameraProxy","end");
        }
    }
}
