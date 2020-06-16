package com.mrtan.qiniu_push.activity;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.qiniu.android.dns.DnsManager;
import com.qiniu.android.dns.IResolver;
import com.qiniu.android.dns.NetworkInfo;
import com.qiniu.android.dns.http.DnspodFree;
import com.qiniu.android.dns.local.AndroidDnsServer;
import com.qiniu.android.dns.local.Resolver;
import com.qiniu.pili.droid.streaming.AudioSourceCallback;
import com.qiniu.pili.droid.streaming.StreamStatusCallback;
import com.qiniu.pili.droid.streaming.StreamingProfile;
import com.qiniu.pili.droid.streaming.StreamingSessionListener;
import com.qiniu.pili.droid.streaming.StreamingState;
import com.qiniu.pili.droid.streaming.StreamingStateChangedListener;
import com.mrtan.qiniu_push.R;
import com.mrtan.qiniu_push.plain.EncodingConfig;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.List;

public abstract class StreamingBaseActivity extends Activity implements
        StreamingSessionListener,
        StreamStatusCallback,
        StreamingStateChangedListener,
        AudioSourceCallback {
    private static final String TAG = "StreamingBaseActivity";
    private static final String TAG2 = "StreamingProfile";

    public static final String INPUT_TEXT = "INPUT_TEXT";
    public static final String AUDIO_CHANNEL_STEREO = "AUDIO_CHANNEL_STEREO";
    public static final String TRANSFER_MODE_QUIC = "TRANSFER_MODE_QUIC";

    protected boolean mShutterButtonPressed = false;
    protected boolean mIsEncOrientationPort = true;
    protected boolean mIsReady;
    private String mStatusMsgContent;
    private String mLogContent = "\n";

    protected TextView mLogTextView;
    protected TextView mStatusTextView;
    protected TextView mStatView;

    protected Button mShutterButton;

    protected StreamingProfile mProfile = new StreamingProfile();

    protected abstract void initView();

    protected abstract void initStreamingManager();

    protected abstract boolean startStreaming();

    protected abstract boolean stopStreaming();

    protected boolean mAudioStereoEnable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initEncodingProfile();
        initView();

        mLogTextView = (TextView) findViewById(R.id.log_info);
        mStatusTextView = (TextView) findViewById(R.id.streamingStatus);
        mStatView = (TextView) findViewById(R.id.stream_status);
        mShutterButton = (Button) findViewById(R.id.toggleRecording_button);

        mShutterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mShutterButtonPressed) {
                    stopStreamingInternal();
                } else {
                    startStreamingInternal();
                }
            }
        });
        Intent intent = getIntent();
        String inputText = intent.getStringExtra(INPUT_TEXT);
        boolean quicEnable = intent.getBooleanExtra(TRANSFER_MODE_QUIC, false);
        mProfile.setQuicEnable(quicEnable);
        logProfile("setQuicEnable", String.valueOf(quicEnable));

        // publish url rtmp://pili-publish.qnsdk.com/sdk-live/ff5afd6d-bcb2-4671-b5cd-965a2d57eb92?e=1592307049&token=QxZugR8TAhI38AiJ_cptTl3RbzLyca3t-AAiH-Hh:KlKtpmX8Q36Eepjc2tewYU1Fx-8=
        try {
            //推流 Url
            mProfile.setPublishUrl(inputText);
            logProfile("setPublishUrl", inputText);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        mAudioStereoEnable = intent.getBooleanExtra(AUDIO_CHANNEL_STEREO, false);

        initStreamingManager();
    }

    protected void logProfile(String method, String args) {
        Log.i(TAG2, String.format("method: %s, args: %s", method, args));
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mShutterButtonPressed = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onStateChanged(StreamingState streamingState, Object extra) {
        Log.i(TAG, "StreamingState streamingState:" + streamingState + ",extra:" + extra);
        switch (streamingState) {
            case PREPARING:
                mStatusMsgContent = getString(R.string.string_state_preparing);
                break;
            case READY:
                mIsReady = true;
                mStatusMsgContent = getString(R.string.string_state_ready);
                /**
                 * Start streaming when `READY`
                 */
                startStreamingInternal();
                break;
            case CONNECTING:
                mStatusMsgContent = getString(R.string.string_state_connecting);
                break;
            case STREAMING:
                mStatusMsgContent = getString(R.string.string_state_streaming);
                setShutterButtonEnabled(true);
                setShutterButtonPressed(true);
                break;
            case SHUTDOWN:
                mStatusMsgContent = getString(R.string.string_state_ready);
                setShutterButtonEnabled(true);
                setShutterButtonPressed(false);
                break;
            case IOERROR:
                /**
                 * Network-connection is unavailable when `startStreaming`.
                 * You can `startStreaming` later or just finish the streaming
                 */
                mLogContent += "IOERROR\n";
                mStatusMsgContent = getString(R.string.string_state_ready);
                setShutterButtonEnabled(true);
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startStreamingInternal();
                    }
                }, 2000);
                break;
            case DISCONNECTED:
                /**
                 * Network-connection is broken when streaming
                 * You can do reconnecting in `onRestartStreamingHandled`
                 */
                mLogContent += "DISCONNECTED\n";
                break;
            case UNKNOWN:
                mStatusMsgContent = getString(R.string.string_state_ready);
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mLogTextView != null) {
                    mLogTextView.setText(mLogContent);
                }
                mStatusTextView.setText(mStatusMsgContent);
            }
        });
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
    public Camera.Size onPreviewSizeSelected(List<Camera.Size> list) {
        return null;
    }

    @Override
    public int onPreviewFpsSelected(List<int[]> list) {
        return -1;
    }

    protected void setShutterButtonEnabled(final boolean enable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mShutterButton.setFocusable(enable);
                mShutterButton.setClickable(enable);
                mShutterButton.setEnabled(enable);
            }
        });
    }

    protected void setShutterButtonPressed(final boolean pressed) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mShutterButtonPressed = pressed;
                mShutterButton.setPressed(pressed);
            }
        });
    }

    @Override
    public void notifyStreamStatusChanged(final StreamingProfile.StreamStatus streamStatus) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStatView.setText("bitrate:" + streamStatus.totalAVBitrate / 1024 + " kbps"
                        + "\naudio:" + streamStatus.audioFps + " fps"
                        + "\nvideo:" + streamStatus.videoFps + " fps");
            }
        });
    }

    protected void startStreamingInternal() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                setShutterButtonEnabled(false);
                boolean res = startStreaming();
                mShutterButtonPressed = true;
                if (!res) {
                    mShutterButtonPressed = false;
                    setShutterButtonEnabled(true);
                }
                setShutterButtonPressed(mShutterButtonPressed);
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

    /**
     * If you want to use a custom DNS server, config it
     * Not required.
     */
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
}
