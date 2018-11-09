package cordova.plugin.qnrtc.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import com.qiniu.droid.rtc.QNRemoteSurfaceView;
import com.qiniu.droid.rtc.QNRemoteVideoCallback;

import org.webrtc.VideoFrame;

import cordova.plugin.qnrtc.QNRtc;

public class RemoteVideoView extends RTCVideoView implements QNRemoteVideoCallback {

    public RemoteVideoView(Context context) {
        super(context);
        mContext = context;
    }

    public RemoteVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(mContext).inflate(QNRtc.getResourceId("remote_video_view", "layout"), this, true);//R.layout.remote_video_view
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mRemoteSurfaceView = (QNRemoteSurfaceView) findViewById(QNRtc.getResourceId("remote_surface_view", "id"));//R.id.remote_surface_view
        mRemoteSurfaceView.setRemoteVideoCallback(this);
    }

    @Override
    public void onRenderingFrame(VideoFrame frame) {

    }

    @Override
    public void onSurfaceCreated() {

    }

    @Override
    public void onSurfaceChanged(int i, int i1) {

    }

    @Override
    public void onSurfaceDestroyed() {

    }
}
