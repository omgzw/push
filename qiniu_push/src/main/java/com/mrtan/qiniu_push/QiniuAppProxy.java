package com.mrtan.qiniu_push;

import android.app.Application;
import android.util.Log;

import com.qiniu.pili.droid.streaming.StreamingEnv;
import com.taobao.weex.WXSDKEngine;

import io.dcloud.weex.AppHookProxy;

public class QiniuAppProxy implements AppHookProxy {
    @Override
    public void onCreate(Application application) {
        //可写初始化触发逻辑
        Log.i("StreamAppProxy","StreamAppProxy");
        try {
            StreamingEnv.init(application);
            WXSDKEngine.registerComponent("cameraPush", QiniuCameraPushComponent.class);
        } catch (Throwable t){
            t.printStackTrace();
        }
    }
}
