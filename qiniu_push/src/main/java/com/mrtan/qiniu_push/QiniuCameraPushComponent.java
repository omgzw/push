package com.mrtan.qiniu_push;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.alibaba.fastjson.JSONObject;
import com.mrtan.qiniu_push.ui.CameraPreviewFrameView;
import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.annotation.JSMethod;
import com.taobao.weex.bridge.JSCallback;
import com.taobao.weex.ui.action.BasicComponentData;
import com.taobao.weex.ui.component.WXComponent;
import com.taobao.weex.ui.component.WXComponentProp;
import com.taobao.weex.ui.component.WXVContainer;

public class QiniuCameraPushComponent extends WXComponent<CameraPreviewFrameView> {
    private final String TAG = "CameraPushComponent";
    private QiniuCameraProxy proxy;
    public QiniuCameraPushComponent(WXSDKInstance instance, WXVContainer parent, BasicComponentData basicComponentData) {
        super(instance, parent, basicComponentData);
    }

    public QiniuCameraPushComponent(WXSDKInstance instance, WXVContainer parent, int type, BasicComponentData basicComponentData) {
        super(instance, parent, type, basicComponentData);
    }

    @Override
    protected CameraPreviewFrameView initComponentHostView(@NonNull Context context) {
        proxy = new QiniuCameraProxy(context);
        CameraPreviewFrameView cameraPreviewFrameView = new CameraPreviewFrameView(context);
        cameraPreviewFrameView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (context instanceof Activity) {
            ((Activity) context).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        return cameraPreviewFrameView;
    }

    @WXComponentProp(name = "url")
    public void setUrl(String url) {
        Log.i(TAG, String.format("setUrl %s", url));
        proxy.onInit(url, getHostView());
        onShow();
    }

    @JSMethod
    public void startStream() {
        proxy.startStreamingInternal();
    }

    @JSMethod
    public void stopStream() {
        proxy.stopStreamingInternal();
    }

    @JSMethod
    public void setStateCallback(JSCallback callback){
        proxy.setStreamingStateChangedListener(callback);
    }

    @JSMethod
    public void setLogCallBack(JSCallback callBack){
        proxy.setLogListener(callBack);
    }

    @JSMethod
    public void switchCamera(){
        proxy.switchCamera();
    }

    @JSMethod
    public void switchFlash(){
        proxy.switchTorch();
    }

    @JSMethod
    public  void switchMute(){
        proxy.switchMute();
    }

    @JSMethod
    public void switchFaceBeauty(){
        proxy.switchFaceButty();
    }

    @JSMethod
    public void onShow(){
        proxy.onResume();
    }

    @JSMethod
    public void onHidden(){
        proxy.onPause();
    }

    @Override
    public void destroy() {
        proxy.onPause();
       proxy.switchCamera();
        proxy.onDestroy();
        super.destroy();
    }
}
