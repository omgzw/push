package com.mrtan.qiniu_push;

import android.app.Application;
import android.util.Log;

import com.qiniu.pili.droid.streaming.StreamingEnv;

import io.dcloud.weex.AppHookProxy;

public class QiniuAppProxy implements AppHookProxy {
    @Override
    public void onCreate(Application application) {
        //可写初始化触发逻辑
        Log.i("StreamAppProxy","StreamAppProxy");
        try {
            StreamingEnv.init(application);
        } catch (Throwable t){
            t.printStackTrace();
        }
    }
}
