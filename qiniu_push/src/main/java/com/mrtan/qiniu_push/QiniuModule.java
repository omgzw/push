package com.mrtan.qiniu_push;

import android.app.Activity;
import android.content.Intent;

import com.alibaba.fastjson.JSONObject;
import com.mrtan.qiniu_push.activity.AVStreamingActivity;
import com.mrtan.qiniu_push.activity.StreamingBaseActivity;
import com.taobao.weex.annotation.JSMethod;
import com.taobao.weex.bridge.JSCallback;
import com.taobao.weex.common.WXModule;

public class QiniuModule extends WXModule {

    private QiniuProxy mProxy = new QiniuProxy();

    @JSMethod(uiThread = true)
    public void testAsyncFunc(JSONObject options, JSCallback callback) {
        if(mProxy != null) {
            mProxy.testAsyncFunc(options, callback);
        }
    }

    //run JS thread
    @JSMethod(uiThread = false)
    public JSONObject testSyncFunc() {
        return mProxy.testSyncFunc();
    }

    /**
     * 退流状态回调
     *
     * @param callback
     */
    @JSMethod(uiThread = false)
    public void setStreamingStateChangedListener(JSCallback callback) {
        mProxy.setStreamingStateChangedListener(callback);
    }

    /**
     * 录制按钮状态回调
     *
     * @param callback
     */
    @JSMethod(uiThread = false)
    public void setShutterStateCallback(JSCallback callback) {
        mProxy.setShutterStateCallback(callback);
    }

    /**
     * 开始退流
     */
    @JSMethod(uiThread = true)
    public void startStream() {
        mProxy.startStream();
    }

    /**
     * 停止退流
     */
    @JSMethod(uiThread = true)
    public void stopStream() {
        mProxy.stopStream();
    }

    /**
     * 初始化
     */
    @JSMethod(uiThread = true)
    public JSONObject init(JSONObject options) {
        return mProxy.init(options, mWXSDKInstance);
    }

    @JSMethod (uiThread = true)
    public void gotoNativePage(JSONObject options){
        String url = options.getString(QiniuProxy.KEY_URL);
        if (url != null) {
            if (mWXSDKInstance != null && mWXSDKInstance.getContext() instanceof Activity) {
                Intent intent = new Intent(mWXSDKInstance.getContext(), AVStreamingActivity.class);
                intent.putExtra(StreamingBaseActivity.INPUT_TEXT, url);
                ((Activity) mWXSDKInstance.getContext()).startActivity(intent);
            }
        }
    }
}
