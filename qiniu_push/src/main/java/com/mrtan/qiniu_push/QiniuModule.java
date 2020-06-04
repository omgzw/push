package com.mrtan.qiniu_push;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;
import com.taobao.weex.annotation.JSMethod;
import com.taobao.weex.bridge.JSCallback;
import com.taobao.weex.common.WXModule;
import com.qiniu.android.dns.DnsManager;
import com.qiniu.android.dns.IResolver;
import com.qiniu.android.dns.NetworkInfo;
import com.qiniu.android.dns.http.DnspodFree;
import com.qiniu.android.dns.local.AndroidDnsServer;
import com.qiniu.android.dns.local.Resolver;
import com.qiniu.pili.droid.streaming.ScreenSetting;
import com.qiniu.pili.droid.streaming.ScreenStreamingManager;
import com.qiniu.pili.droid.streaming.StreamStatusCallback;
import com.qiniu.pili.droid.streaming.StreamingProfile;
import com.qiniu.pili.droid.streaming.StreamingState;
import com.qiniu.pili.droid.streaming.StreamingStateChangedListener;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;

public class QiniuModule extends WXModule {

    String TAG = "QiniuModule";
    public static int REQUEST_CODE = 1000;

    //run ui thread
    @JSMethod(uiThread = true)
    public void testAsyncFunc(JSONObject options, JSCallback callback) {
        Log.e(TAG, "testAsyncFunc--"+options);
        if(callback != null) {
            JSONObject data = new JSONObject();
            data.put("code", "success");
            callback.invoke(data);
            //callback.invokeAndKeepAlive(data);
        }
    }

    //run JS thread
    @JSMethod (uiThread = false)
    public JSONObject testSyncFunc(){
        JSONObject data = new JSONObject();
        data.put("code", "success");
        return data;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_CODE && data.hasExtra("respond")) {
            Log.e("TestModule", "原生页面返回----"+data.getStringExtra("respond"));
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private JSCallback mStreamingStateChangedListener;
    private JSCallback mShutterStateCallback;
    private StreamingProfile mProfile = new StreamingProfile();
    protected boolean mShutterButtonPressed = false;
    public static final String KEY_URL = "url";
    private ScreenStreamingManager mScreenStreamingManager;

    /**
     * 退流状态回调
     * @param callback
     */
    @JSMethod(uiThread = false)
    public void setStreamingStateChangedListener(JSCallback callback){
        mStreamingStateChangedListener = callback;
    }

    /**
     * 录制按钮状态回调
     * @param callback
     */
    @JSMethod(uiThread = false)
    public void setShutterStateCallback(JSCallback callback){
        mShutterStateCallback = callback;
    }

    /**
     * 开始退流
     */
    @JSMethod(uiThread = true)
    public void startStream(){
        startStreamingInternal();
    }

    /**
     * 停止退流
     */
    @JSMethod(uiThread = true)
    public void stopStream(){
        stopStreamingInternal();
    }

    /**
     * 初始化
     * @param options
     * @return
     */
    @JSMethod(uiThread = false)
    public JSONObject init(JSONObject options) {
        String url = options.getString(KEY_URL);
        JSONObject data = new JSONObject();
        // 弱网推流
//        mProfile.setQuicEnable(quicEnable);
        Log.i(TAG, url);
        if (url != null) {
            try {
                mProfile.setPublishUrl(url);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                data.put("state", "fail");
                data.put("message", e.getMessage());
                return data;
            }
        } else {
            data.put("state", "fail");
            data.put("message", "字段 url 不能为空");
            return data;
        }
        initEncodingProfile();
        initView();
        //mProfile
        initStreamingManager();
        data.put("state", "success");
        data.put("message", "ok");
        return data;
    }

    private void initEncodingProfile() {
        //视频质量
        //VIDEO_QUALITY_LOW1
        //VIDEO_QUALITY_LOW2
        //VIDEO_QUALITY_LOW3
        //VIDEO_QUALITY_MEDIUM1
        //VIDEO_QUALITY_MEDIUM2
        //VIDEO_QUALITY_MEDIUM3
        //VIDEO_QUALITY_HIGH1
        //VIDEO_QUALITY_HIGH2
        //VIDEO_QUALITY_HIGH3
        mProfile.setVideoQuality(StreamingProfile.VIDEO_QUALITY_MEDIUM2);
        //视频大小
        //VIDEO_ENCODING_HEIGHT_240
        //VIDEO_ENCODING_HEIGHT_480
        //VIDEO_ENCODING_HEIGHT_544
        //VIDEO_ENCODING_HEIGHT_720
        //VIDEO_ENCODING_HEIGHT_1088
        mProfile.setEncodingSizeLevel(StreamingProfile.VIDEO_ENCODING_HEIGHT_480);
        //音频质量
        //AUDIO_QUALITY_LOW1
        //AUDIO_QUALITY_LOW2
        //AUDIO_QUALITY_MEDIUM1
        //AUDIO_QUALITY_MEDIUM2
        //AUDIO_QUALITY_HIGH1
        //AUDIO_QUALITY_HIGH2
        mProfile.setAudioQuality(StreamingProfile.AUDIO_QUALITY_MEDIUM2);
        // 横竖屏幕
        mProfile.setEncodingOrientation(StreamingProfile.ENCODING_ORIENTATION.PORT);
        //Rate control
        mProfile.setEncoderRCMode(StreamingProfile.EncoderRCModes.QUALITY_PRIORITY);
        //Bitrate Control
        //BitrateAdjustMode.Auto : StreamingProfile.BitrateAdjustMode.Manual : StreamingProfile.BitrateAdjustMode.Disable
        mProfile.setBitrateAdjustMode(StreamingProfile.BitrateAdjustMode.Auto);
        //FPS control
        mProfile.setFpsControllerEnable(true);
        mProfile.setYuvFilterMode(StreamingProfile.YuvFilterMode.Linear);
        mProfile.setVideoAdaptiveBitrateRange(150 * 1024, 800 * 1024);
        mProfile.setDnsManager(getMyDnsManager())
                .setStreamStatusConfig(new StreamingProfile.StreamStatusConfig(3))
                .setSendingBufferProfile(new StreamingProfile.SendingBufferProfile(0.2f, 0.8f, 3.0f, 20 * 1000));
    }

    private void initView() {

    }

    private void initStreamingManager() {
        // In screen streaming, screen size normally should equals to encoding size
        ScreenSetting screenSetting = new ScreenSetting();
        //todo 输入
        screenSetting.setSize(480, 848);
        screenSetting.setDpi(1);

        mScreenStreamingManager = new ScreenStreamingManager();
//        mScreenStreamingManager.setStreamingSessionListener(this);
        mScreenStreamingManager.setStreamingStateListener(mStreamingStateChanged);
        mScreenStreamingManager.setStreamStatusCallback(new StreamStatusCallback() {
            @Override
            public void notifyStreamStatusChanged(StreamingProfile.StreamStatus streamStatus) {
                Log.i(TAG, "bitrate:" + streamStatus.totalAVBitrate / 1024 + " kbps"
                        + "\naudio:" + streamStatus.audioFps + " fps"
                        + "\nvideo:" + streamStatus.videoFps + " fps");
            }
        });
        mScreenStreamingManager.prepare(mWXSDKInstance.getContext(), screenSetting, null, mProfile);
//        mScreenStreamingManager.setStreamingSessionListener(this);
        mScreenStreamingManager.setNativeLoggingEnabled(false);
    }

    protected void startStreamingInternal() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                QiniuModule.this.setShutterButtonEnabled(false);
                boolean res = QiniuModule.this.startStreaming();
                mShutterButtonPressed = true;
                if (!res) {
                    mShutterButtonPressed = false;
                    QiniuModule.this.setShutterButtonEnabled(true);
                }
                QiniuModule.this.setShutterButtonPressed(mShutterButtonPressed);
            }
        }).start();
    }

    protected void stopStreamingInternal() {
        if (mShutterButtonPressed) {
            // disable the shutter button before stopStreaming
            setShutterButtonEnabled(false);
            boolean res = stopStreaming();
            if (!res) {
                mShutterButtonPressed = true;
                setShutterButtonEnabled(true);
            }
            setShutterButtonPressed(mShutterButtonPressed);
        }
    }

    private boolean startStreaming() {
        return mScreenStreamingManager.startStreaming();
    }

    private boolean stopStreaming() {
        return mScreenStreamingManager.stopStreaming();
    }

    private static DnsManager getMyDnsManager() {
        IResolver r0 = null;
        IResolver r1 = new DnspodFree();
        IResolver r2 = AndroidDnsServer.defaultResolver();
        try {
            r0 = new Resolver(InetAddress.getByName("119.29.29.29"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return new DnsManager(NetworkInfo.normal, new IResolver[]{r0, r1, r2});
    }

    private StreamingStateChangedListener mStreamingStateChanged =  new StreamingStateChangedListener() {
        public void onStateChanged(StreamingState streamingState, Object extra) {
            Log.i(TAG, "StreamingState streamingState:" + streamingState + ",extra:" + extra);
            JSONObject data = new JSONObject();
            data.put("state", streamingState.toString());
            if (extra != null) {
                data.put("ext", extra.toString());
            }
            switch (streamingState) {
                case PREPARING:
//                mStatusMsgContent = getString(R.string.string_state_preparing);
                    break;
                case READY:
//                mIsReady = true;
//                mStatusMsgContent = getString(R.string.string_state_ready);
                    // Start streaming when `READY`
                    startStreamingInternal();
                    break;
                case CONNECTING:
//                mStatusMsgContent = getString(R.string.string_state_connecting);
                    break;
                case STREAMING:
//                mStatusMsgContent = getString(R.string.string_state_streaming);
                    setShutterButtonEnabled(true);
                    setShutterButtonPressed(true);
                    break;
                case SHUTDOWN:
//                mStatusMsgContent = getString(R.string.string_state_ready);
                    setShutterButtonEnabled(true);
                    setShutterButtonPressed(false);
                    break;
                case IOERROR:
                    //Network-connection is unavailable when `startStreaming`.
                    //You can `startStreaming` later or just finish the streaming
//                mLogContent += "IOERROR\n";
//                mStatusMsgContent = getString(R.string.string_state_ready);
                    setShutterButtonEnabled(true);
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            QiniuModule.this.startStreamingInternal();
                        }
                    }, 2000);
                    break;
                case DISCONNECTED:
                    // Network-connection is broken when streaming
                    // You can do reconnecting in `onRestartStreamingHandled`
//                mLogContent += "DISCONNECTED\n";
                    break;
                case UNKNOWN:
//                mStatusMsgContent = getString(R.string.string_state_ready);
                    break;
                case SENDING_BUFFER_EMPTY:
                    break;
                case SENDING_BUFFER_FULL:
                    break;
                case AUDIO_RECORDING_FAIL:
                    break;
                case INVALID_STREAMING_URL:
                    Log.e(TAG, "Invalid streaming url:" + extra);
                    break;
                case UNAUTHORIZED_STREAMING_URL:
                    Log.e(TAG, "Unauthorized streaming url:" + extra);
//                mLogContent += "Unauthorized Url\n";
                    break;
                case UNAUTHORIZED_PACKAGE:
//                mLogContent += "Unauthorized package\n";
                    break;
            }
            if (mStreamingStateChangedListener != null) {
                mStreamingStateChangedListener.invoke(data);
            }
        }
    };

    protected void setShutterButtonEnabled(final boolean enable) {
        if (mShutterStateCallback != null){
            JSONObject data = new JSONObject();
            data.put("isEnable", enable);
            mShutterStateCallback.invoke(data);
        }
    }

    protected void setShutterButtonPressed(final boolean pressed) {
        if (mShutterStateCallback != null){
            JSONObject data = new JSONObject();
            data.put("pressed", pressed);
            mShutterStateCallback.invoke(data);
        }
    }

//    @Override
//    public void notifyStreamStatusChanged(final StreamingProfile.StreamStatus streamStatus) {
//        Log.i(TAG, "bitrate:" + streamStatus.totalAVBitrate / 1024 + " kbps"
//                + "\naudio:" + streamStatus.audioFps + " fps"
//                + "\nvideo:" + streamStatus.videoFps + " fps");
//    }
}
