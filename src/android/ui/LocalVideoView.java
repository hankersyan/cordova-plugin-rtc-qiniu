package cordova.plugin.qnrtc.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import com.qiniu.droid.rtc.QNLocalSurfaceView;
import com.qiniu.droid.rtc.QNLocalVideoCallback;

import org.webrtc.VideoFrame;

import cordova.plugin.qnrtc.QNRtc;

public class LocalVideoView extends RTCVideoView implements QNLocalVideoCallback {

    public LocalVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(mContext).inflate(QNRtc.getResourceId("local_video_view", "layout"), this, true);//R.layout.local_video_view
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mLocalSurfaceView = (QNLocalSurfaceView) findViewById(QNRtc.getResourceId("local_surface_view", "id"));//R.id.local_surface_view
        mLocalSurfaceView.setLocalVideoCallback(this);
    }

    @Override
    public int onRenderingFrame(int textureId, int width, int height, VideoFrame.TextureBuffer.Type type, long timestampNs) {
        return textureId;
    }

    @Override
    public void onPreviewFrame(byte[] data, int width, int height, int rotation, int fmt, long timestampNs) {
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
