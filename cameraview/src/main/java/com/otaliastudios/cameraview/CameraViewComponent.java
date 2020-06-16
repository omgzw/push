package com.otaliastudios.cameraview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.otaliastudios.cameraview.controls.Engine;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.controls.Mode;
import com.otaliastudios.cameraview.gesture.Gesture;
import com.otaliastudios.cameraview.gesture.GestureAction;
import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.annotation.JSMethod;
import com.taobao.weex.ui.action.BasicComponentData;
import com.taobao.weex.ui.component.WXComponent;
import com.taobao.weex.ui.component.WXVContainer;

public class CameraViewComponent extends WXComponent<CameraView> {
    final String TAG = "CameraViewComponent";

    public CameraViewComponent(WXSDKInstance instance, WXVContainer parent, BasicComponentData basicComponentData) {
        super(instance, parent, basicComponentData);
        Log.i(TAG, "CameraViewComponent");
    }

//    app:cameraGestureTap="autoFocus"
//    app:cameraGestureLongTap="none"
//    app:cameraGesturePinch="zoom"
//    app:cameraGestureScrollHorizontal="filterControl1"
//    app:cameraGestureScrollVertical="exposureCorrection"
//    app:cameraMode="picture"
//    app:cameraAutoFocusMarker="@string/cameraview_default_autofocus_marker">

    @Override
    protected CameraView initComponentHostView(@NonNull Context context) {
        Log.i(TAG, "initComponentHostView");
        try {
            CameraView view = new CameraView(context);
            view.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            view.setKeepScreenOn(true);
            view.setExperimental(true);
            view.setEngine(Engine.CAMERA2);
            view.setPlaySounds(true);
            view.setFlash(Flash.OFF);
            view.mapGesture(Gesture.TAP, GestureAction.AUTO_FOCUS);
            view.mapGesture(Gesture.PINCH, GestureAction.ZOOM);
            view.mapGesture(Gesture.SCROLL_HORIZONTAL, GestureAction.FILTER_CONTROL_1);
            view.mapGesture(Gesture.SCROLL_VERTICAL, GestureAction.EXPOSURE_CORRECTION);
            view.setMode(Mode.VIDEO);
            Log.i(TAG, "initComponentHostView end");
            return view;
        } catch (Throwable t){
            Log.i(TAG, "Throwable");
            t.printStackTrace();
            Log.i(TAG, "Throwable end");
            throw t;
        }
    }

    @JSMethod
    public void open() {
        getHostView().open();
    }

    @JSMethod
    public void close() {
        getHostView().close();
    }

    @Override
    public void destroy() {
        super.destroy();
        getHostView().close();
        getHostView().destroy();
    }
}
