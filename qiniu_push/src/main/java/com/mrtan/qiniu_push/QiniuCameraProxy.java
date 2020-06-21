package com.mrtan.qiniu_push;

import android.content.Context;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.alibaba.fastjson.JSONObject;
import com.mrtan.qiniu_push.gles.FBO;
import com.mrtan.qiniu_push.plain.CameraConfig;
import com.mrtan.qiniu_push.ui.CameraPreviewFrameView;
import com.qiniu.pili.droid.streaming.AVCodecType;
import com.qiniu.pili.droid.streaming.AudioSourceCallback;
import com.qiniu.pili.droid.streaming.CameraStreamingSetting;
import com.qiniu.pili.droid.streaming.MediaStreamingManager;
import com.qiniu.pili.droid.streaming.MicrophoneStreamingSetting;
import com.qiniu.pili.droid.streaming.StreamStatusCallback;
import com.qiniu.pili.droid.streaming.StreamingPreviewCallback;
import com.qiniu.pili.droid.streaming.StreamingProfile;
import com.qiniu.pili.droid.streaming.StreamingSessionListener;
import com.qiniu.pili.droid.streaming.StreamingState;
import com.qiniu.pili.droid.streaming.StreamingStateChangedListener;
import com.qiniu.pili.droid.streaming.SurfaceTextureCallback;
import com.qiniu.pili.droid.streaming.av.common.PLFourCC;
import com.taobao.weex.bridge.JSCallback;

import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.List;

import static com.mrtan.qiniu_push.QiniuProxy.KEY_URL;
import static com.mrtan.qiniu_push.QiniuProxy.getMyDnsManager;

public class QiniuCameraProxy implements
        StreamingPreviewCallback,
        CameraPreviewFrameView.Listener,
        SurfaceTextureCallback,
        StreamingSessionListener,
        StreamStatusCallback,
        StreamingStateChangedListener,
        AudioSourceCallback {
    private final String TAG = "QiniuCameraProxy";
    private boolean mIsTorchOn = false;
    private boolean mIsNeedMute = false;
    private boolean mIsNeedFB = false;
    private int mMaxZoom = 0;
    private boolean mOrientationChanged = false;
    private int mCurrentCamFacingIndex;

    private boolean mShutterButtonPressed = false;
    private boolean mIsReady;
    private String mStatusMsgContent;
    private String mLogContent = "\n";
    private StreamingProfile mProfile = new StreamingProfile();
    private boolean mAudioStereoEnable = false;
//    private static final String TAG2 = "StreamingProfile";
    private JSCallback mStreamingStateChangedListener;
    private JSCallback mLogListener;

    private FBO mFBO = new FBO();

    private Switcher mSwitcher = new Switcher();
    //推流设置
    private MediaStreamingManager mMediaStreamingManager;
    //摄像头参数配置
    private CameraStreamingSetting mCameraStreamingSetting;
    private CameraConfig mCameraConfig;
    //    private AudioMixer mAudioMixer;
    private boolean mIsPictureStreaming = false;
    private Context mContext;
    private View mHostView;

    public QiniuCameraProxy(Context context) {
        mContext = context;
    }

    protected void setStreamingStateChangedListener(JSCallback callback){
        mStreamingStateChangedListener = callback;
    }

    protected void setLogListener(JSCallback callback){
        mLogListener = callback;
    }

    protected void onResume() {
        Log.i(TAG, "onResume");
        mShutterButtonPressed = false;
        mMediaStreamingManager.resume();
    }

    protected void onPause() {
        Log.i(TAG, "onPause");
        normalPause();
    }

    private void normalPause() {
        mIsReady = false;
        mShutterButtonPressed = false;
        mIsPictureStreaming = false;
        mMediaStreamingManager.pause();
    }

    public void onDestroy() {
        if (mMediaStreamingManager != null) {
            mMediaStreamingManager.destroy();
        }
    }

    protected void initStreamingManager(Context context, CameraPreviewFrameView cameraPreviewFrameView) {
        mHostView = cameraPreviewFrameView;
        mMediaStreamingManager = new MediaStreamingManager(context, cameraPreviewFrameView, AVCodecType.SW_VIDEO_WITH_SW_AUDIO_CODEC);
        MicrophoneStreamingSetting microphoneStreamingSetting = null;
        if (mAudioStereoEnable) {
            microphoneStreamingSetting = new MicrophoneStreamingSetting();
            microphoneStreamingSetting.setChannelConfig(AudioFormat.CHANNEL_IN_STEREO);
        }
        mMediaStreamingManager.prepare(mCameraStreamingSetting, microphoneStreamingSetting, null, mProfile);
        mMediaStreamingManager.setAutoRefreshOverlay(true);
        if (mCameraConfig.mIsCustomFaceBeauty) {
            mMediaStreamingManager.setSurfaceTextureCallback(this);
        }
        cameraPreviewFrameView.setListener(this);
        mMediaStreamingManager.setStreamingSessionListener(this);
        mMediaStreamingManager.setStreamStatusCallback(this);
        mMediaStreamingManager.setAudioSourceCallback(this);
        mMediaStreamingManager.setStreamingStateListener(this);

    }

    //开始推流
    private boolean startStreaming() {
        return mMediaStreamingManager.startStreaming();
    }

    //结束推流
    private boolean stopStreaming() {
        return mMediaStreamingManager.stopStreaming();
    }

    //前后摄像头切换
    private class Switcher implements Runnable {
        @Override
        public void run() {
            mCurrentCamFacingIndex = (mCurrentCamFacingIndex + 1) % CameraStreamingSetting.getNumberOfCameras();
            CameraStreamingSetting.CAMERA_FACING_ID facingId;
            if (mCurrentCamFacingIndex == CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_BACK.ordinal()) {
                facingId = CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_BACK;
            } else if (mCurrentCamFacingIndex == CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_FRONT.ordinal()) {
                facingId = CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_FRONT;
            } else {
                facingId = CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_3RD;
            }
            Log.i(TAG, "switchCamera:" + facingId);
            mMediaStreamingManager.switchCamera(facingId);
        }
    }

    private boolean isPictureStreaming() {
        return mIsPictureStreaming;
    }

    private CameraStreamingSetting buildCameraStreamingSetting() {
        /*
         * CameraConfig: mFrontFacing false
         * CameraConfig: mSizeLevel MEDIUM
         * CameraConfig: mSizeRatio RATIO_16_9
         * CameraConfig: mIsFaceBeautyEnabled true
         * CameraConfig: mIsCustomFaceBeauty false
         * CameraConfig: mContinuousAutoFocus true
         * CameraConfig: mPreviewMirror false
         * CameraConfig: mEncodingMirror false
         */
        mCameraConfig = new CameraConfig();
        mCameraConfig.mFrontFacing = false;
        mCameraConfig.mSizeLevel = CameraStreamingSetting.PREVIEW_SIZE_LEVEL.MEDIUM;
        mCameraConfig.mSizeRatio = CameraStreamingSetting.PREVIEW_SIZE_RATIO.RATIO_16_9;
        mCameraConfig.mIsFaceBeautyEnabled = true;
        mCameraConfig.mIsCustomFaceBeauty = false;
        mCameraConfig.mContinuousAutoFocus = true;
        mCameraConfig.mPreviewMirror = false;
        mCameraConfig.mEncodingMirror = false;

        CameraStreamingSetting cameraStreamingSetting = new CameraStreamingSetting();
        cameraStreamingSetting.setCameraId(Camera.CameraInfo.CAMERA_FACING_BACK)
                .setCameraPrvSizeLevel(mCameraConfig.mSizeLevel)
                .setCameraPrvSizeRatio(mCameraConfig.mSizeRatio)
                .setFocusMode(mCameraConfig.mFocusMode)
                .setContinuousFocusModeEnabled(mCameraConfig.mContinuousAutoFocus)
                .setFrontCameraPreviewMirror(mCameraConfig.mPreviewMirror)
                .setFrontCameraMirror(mCameraConfig.mEncodingMirror).setRecordingHint(false)
                .setResetTouchFocusDelayInMs(3000)
                .setBuiltInFaceBeautyEnabled(!mCameraConfig.mIsCustomFaceBeauty)
                .setFaceBeautySetting(new CameraStreamingSetting.FaceBeautySetting(1.0f, 1.0f, 0.8f));

        if (mCameraConfig.mIsFaceBeautyEnabled) {
            cameraStreamingSetting.setVideoFilter(CameraStreamingSetting.VIDEO_FILTER_TYPE.VIDEO_FILTER_BEAUTY);
        } else {
            cameraStreamingSetting.setVideoFilter(CameraStreamingSetting.VIDEO_FILTER_TYPE.VIDEO_FILTER_NONE);
        }

        return cameraStreamingSetting;
    }

    public Camera.Size onPreviewSizeSelected(List<Camera.Size> list) {
        return null;
    }

    public void initView() {
        // 初始化摄像头设置
        mCameraStreamingSetting = buildCameraStreamingSetting();
        //是否开启美颜
        mIsNeedFB = mCameraConfig.mIsFaceBeautyEnabled;
        //是否开启镜像映射
//        mIsPreviewMirror = mCameraConfig.mPreviewMirror;
        //动态改变推流镜像
//        mIsEncodingMirror = mCameraConfig.mEncodingMirror;
        //前置摄像头还是后置摄像头
        mCurrentCamFacingIndex = mCameraConfig.mFrontFacing ? 1 : 0;
        initButtonText();
    }

    /**
     * 切换美颜
     */
    public void switchFaceButty() {
        mIsNeedFB = !mIsNeedFB;
        //是否开启美颜
        mMediaStreamingManager.setVideoFilterType(mIsNeedFB ?
                CameraStreamingSetting.VIDEO_FILTER_TYPE.VIDEO_FILTER_BEAUTY
                : CameraStreamingSetting.VIDEO_FILTER_TYPE.VIDEO_FILTER_NONE);
//        updateFBButtonText();
    }

    /**
     * 切换静音
     */
    public void switchMute() {
        mIsNeedMute = !mIsNeedMute;
        //静音
        mMediaStreamingManager.mute(mIsNeedMute);
//        updateMuteButtonText();
    }

    /**
     * 切换摄像头
     */
    public void switchTorch() {
        if (isPictureStreaming()) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mIsTorchOn) {
                    mMediaStreamingManager.turnLightOff();
                } else {
                    mMediaStreamingManager.turnLightOn();
                }
                mIsTorchOn = !mIsTorchOn;
//                setTorchEnabled(mIsTorchOn);
            }
        }).start();
    }

    /**
     * 切换摄像头
     */
    public void switchCamera() {
        if (isPictureStreaming()) {
            return;
        }
        mHostView.removeCallbacks(mSwitcher);
        mHostView.postDelayed(mSwitcher, 100);
    }

    private void initButtonText() {
//        updateFBButtonText();
//        updateCameraSwitcherButtonText(mCameraStreamingSetting.getReqCameraId());
//        updateFBButtonText();
//        updateMuteButtonText();
//        updateOrientationBtnText();
    }

    @Override
    public void onStateChanged(StreamingState streamingState, Object extra) {
        // general states are handled in the `StreamingBaseActivity`
        Log.i(TAG, String.format("onStateChanged %s", streamingState.toString()));
        superOnStateChanged(streamingState, extra);
        switch (streamingState) {
            case READY:
                mMaxZoom = mMediaStreamingManager.getMaxZoom();
                break;
            case SHUTDOWN:
                if (mOrientationChanged) {
                    mOrientationChanged = false;
                    startStreamingInternal();
                }
                break;
            case OPEN_CAMERA_FAIL:
                Log.e(TAG, "Open Camera Fail. id:" + extra);
                break;
            case CAMERA_SWITCHED:
                Log.i(TAG, "camera switched");
                break;
            case TORCH_INFO:
                if (extra != null) {
                    final boolean isSupportedTorch = (Boolean) extra;
                    Log.i(TAG, "isSupportedTorch=" + isSupportedTorch);
                }
                break;
        }
    }

//    private void setFocusAreaIndicator() {
//        if (mRotateLayout == null) {
//            mRotateLayout = findViewById(R.id.focus_indicator_rotate_layout);
//            mMediaStreamingManager.setFocusAreaIndicator(mRotateLayout,
//                    mRotateLayout.findViewById(R.id.focus_indicator));
//        }
//    }

//    private void setTorchEnabled(final boolean enabled) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                String flashlight = enabled ? getString(R.string.flash_light_off) : getString(R.string.flash_light_on);
//                mTorchBtn.setText(flashlight);
//            }
//        });
//    }

//    private void updateOrientationBtnText() {
//    }

//    private void updateFBButtonText() {
//        if (mFaceBeautyBtn != null) {
//            mFaceBeautyBtn.setText(mIsNeedFB ? "FB Off" : "FB On");
//        }
//    }

//    private void updateMuteButtonText() {
//        if (mMuteButton != null) {
//            mMuteButton.setText(mIsNeedMute ? "Unmute" : "Mute");
//        }
//    }

//    private void updateCameraSwitcherButtonText(int camId) {
//        if (mCameraSwitchBtn == null) {
//            return;
//        }
//        if (camId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//            mCameraSwitchBtn.setText(R.string.camera_back);
//        } else {
//            mCameraSwitchBtn.setText(R.string.camera_font);
//        }
//    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        Log.i(TAG, "onSingleTapUp X:" + e.getX() + ",Y:" + e.getY());
        if (mIsReady) {
//            setFocusAreaIndicator();
            mMediaStreamingManager.doSingleTapUp((int) e.getX(), (int) e.getY());
            return true;
        }
        return false;
    }

    @Override
    public boolean onZoomValueChanged(float factor) {
        if (mIsReady && mMediaStreamingManager.isZoomSupported()) {
            int mCurrentZoom = (int) (mMaxZoom * factor);
            mCurrentZoom = Math.min(mCurrentZoom, mMaxZoom);
            mCurrentZoom = Math.max(0, mCurrentZoom);
            Log.d(TAG, "zoom ongoing, scale: " + mCurrentZoom + ",factor:" + factor + ",maxZoom:" + mMaxZoom);
            mMediaStreamingManager.setZoomValue(mCurrentZoom);
        }
        return false;
    }

    @Override
    public void onSurfaceCreated() {
        Log.i(TAG, "onSurfaceCreated");
        // 美颜 only used in custom beauty algorithm case
        mFBO.initialize(mContext);
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        Log.i(TAG, "onSurfaceChanged width:" + width + ",height:" + height);
        mFBO.updateSurfaceSize(width, height);
    }

    @Override
    public void onSurfaceDestroyed() {
        Log.i(TAG, "onSurfaceDestroyed");
        // 自定义美颜 only used in custom beauty algorithm case
        mFBO.release();
    }

    @Override
    public int onDrawFrame(int texId, int texWidth, int texHeight, float[] transformMatrix) {
        // 自定义美颜 When using custom beauty algorithm, you should return a new texId from the SurfaceTexture.
        // newTexId should not equal with texId, Otherwise, there is no filter effect.
        return mFBO.drawFrame(texId, texWidth, texHeight);
    }

    @Override
    public boolean onPreviewFrame(byte[] bytes, int width, int height, int rotation, int fmt, long tsInNanoTime) {
        Log.i(TAG, "onPreviewFrame " + width + "x" + height + ",fmt:" + (fmt == PLFourCC.FOURCC_I420 ? "I420" : "NV21") + ",ts:" + tsInNanoTime + ",rotation:" + rotation);
        return true;
    }

    protected void onInit(String url, CameraPreviewFrameView cameraPreviewFrameView) {
        initEncodingProfile();
        initView();
        // 弱网推流
        mProfile.setQuicEnable(false);
        try {
            mProfile.setPublishUrl(url);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        mAudioStereoEnable = false;
        initStreamingManager(mContext, cameraPreviewFrameView);
    }

    private void superOnStateChanged(StreamingState streamingState, Object extra) {
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
                mIsReady = true;
//                mStatusMsgContent = getString(R.string.string_state_ready);
                /*
                 * Start streaming when `READY`
                 */
                startStreamingInternal();
                break;
            case CONNECTING:
//                mStatusMsgContent = getString(R.string.string_state_connecting);
                break;
            case STREAMING:
//                mStatusMsgContent = getString(R.string.string_state_streaming);
//                setShutterButtonEnabled(true);
//                setShutterButtonPressed(true);
                break;
            case SHUTDOWN:
//                mStatusMsgContent = getString(R.string.string_state_ready);
//                setShutterButtonEnabled(true);
//                setShutterButtonPressed(false);
                break;
            case IOERROR:
                /*
                 * Network-connection is unavailable when `startStreaming`.
                 * You can `startStreaming` later or just finish the streaming
                 */
                mLogContent += "IOERROR\n";
                mStatusMsgContent = mContext.getString(R.string.string_state_ready);
//                setShutterButtonEnabled(true);
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startStreamingInternal();
                    }
                }, 2000);
                break;
            case DISCONNECTED:
                /*
                 * Network-connection is broken when streaming
                 * You can do reconnecting in `onRestartStreamingHandled`
                 */
                mLogContent += "DISCONNECTED\n";
                break;
            case UNKNOWN:
                mStatusMsgContent = mContext.getString(R.string.string_state_ready);
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
                mLogContent += "Unauthorized Url\n";
                break;
            case UNAUTHORIZED_PACKAGE:
                mLogContent += "Unauthorized package\n";
                break;
        }
//        Log.i(TAG, mStatusMsgContent + streamingState.toString());
//        Log.i(TAG, mLogContent + streamingState.toString());
        data.put("content", mStatusMsgContent);
        if (mStreamingStateChangedListener != null){
            mStreamingStateChangedListener.invokeAndKeepAlive(data);
        }
        if (mLogListener != null){
            mLogListener.invokeAndKeepAlive(mLogContent);
        }
        mLogContent = "";
        mStatusMsgContent = "";
    }

    @Override
    public boolean onRecordAudioFailedHandled(int code) {
        Log.i(TAG, "onRecordAudioFailedHandled");
        return false;
    }

    @Override
    public boolean onRestartStreamingHandled(int code) {
        Log.i(TAG, "onRestartStreamingHandled");
        startStreamingInternal();
        return true;
    }

    @Override
    public void onAudioSourceAvailable(ByteBuffer byteBuffer, int i, long l, boolean b) {
    }

    @Override
    public int onPreviewFpsSelected(List<int[]> list) {
        return -1;
    }


    @Override
    public void notifyStreamStatusChanged(final StreamingProfile.StreamStatus streamStatus) {
        if(mStreamingStateChangedListener != null) {
            JSONObject data = new JSONObject();
            data.put("", "bitrate:" + streamStatus.totalAVBitrate / 1024 + " kbps"
                    + "\naudio:" + streamStatus.audioFps + " fps"
                    + "\nvideo:" + streamStatus.videoFps + " fps");
            mStreamingStateChangedListener.invokeAndKeepAlive(data);
        }
    }

    protected void startStreamingInternal() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mShutterButtonPressed = startStreaming();
            }
        }).start();
    }

    protected void stopStreamingInternal() {
        if (mShutterButtonPressed) {
            boolean res = stopStreaming();
            if (!res) {
                mShutterButtonPressed = true;
            }
        }
    }

    private void initEncodingProfile() {
        mProfile.setVideoQuality(11);
        mProfile.setEncodingSizeLevel(1);
        // video misc
        mProfile.setEncodingOrientation(StreamingProfile.ENCODING_ORIENTATION.PORT);
        mProfile.setEncoderRCMode(StreamingProfile.EncoderRCModes.QUALITY_PRIORITY);
        mProfile.setBitrateAdjustMode(StreamingProfile.BitrateAdjustMode.Auto);
        mProfile.setFpsControllerEnable(true);
        mProfile.setYuvFilterMode(StreamingProfile.YuvFilterMode.Linear);
        mProfile.setVideoAdaptiveBitrateRange(153600, 819200);
        mProfile.setAudioQuality(11);
        mProfile.setDnsManager(getMyDnsManager())
                .setStreamStatusConfig(new StreamingProfile.StreamStatusConfig(3))
                .setSendingBufferProfile(new StreamingProfile.SendingBufferProfile(0.2f, 0.8f, 3.0f, 20 * 1000));
    }

}
