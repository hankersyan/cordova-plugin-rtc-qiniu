package cordova.plugin.qnrtc.ui;

import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.qiniu.droid.rtc.QNRTCEngine;
import com.qiniu.droid.rtc.QNSurfaceView;
import com.qiniu.droid.rtc.QNTrackInfo;
import com.qiniu.droid.rtc.QNTrackKind;

import org.json.JSONObject;
import org.webrtc.RendererCommon;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.net.ssl.HttpsURLConnection;

import cordova.plugin.qnrtc.QNRtc;

public class UserTrackView extends FrameLayout {

    private static final String TAG = "UserTrackView";
    private static final boolean PRINT_DEBUG_LOG = false;

    private static final boolean DISPLAY_LARGE_VIDEO_TRACK = true;
    private static final boolean DISPLAY_SMALL_VIDEO_TRACK = true;

    public static final String TAG_CAMERA = "camera";
    public static final String TAG_SCREEN = "screen";

    private boolean inited = false;
    private ViewGroup mVideoViewLargeParent;
    private QNSurfaceView mSurfaceViewLarge;

    private ViewGroup mVideoViewSmallParent;
    private QNSurfaceView mSurfaceViewSmall;

    private ImageView mMicrophoneStateView;
    private TextView mAudioView;

    private QNRTCEngine mQNRTCEngine;
    private String mUserId;
    private QNTrackInfo mQNAudioTrackInfo;
    private List<QNTrackInfo> mQNVideoTrackInfos = new ArrayList<>();

    private QNTrackInfo mTrackInfoDisplayInLargeView = null;
    private QNTrackInfo mTrackInfoDisplayInSmallView = null;
    private int mMicrophoneViewVisibility = -1;
    private int mPos = -1;

    private List<JSONObject> _usersInfo = new ArrayList<>();

    public UserTrackView(@NonNull Context context) {
        super(context);
        init();
    }

    public UserTrackView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        if (inited) {
            return;
        }
        inited = true;
        LayoutInflater.from(getContext()).inflate(getLayout(), this, true);
    }

    protected int getLayout() {
        return QNRtc.getResourceId("user_tracks_view", "layout");
    }

    public String getUserId() {
        return mUserId;
    }

    public boolean isTaken() {
        return !TextUtils.isEmpty(getUserId());
    }

    public List<QNTrackInfo> getTrackInfos() {
        List<QNTrackInfo> trackInfos = new ArrayList<>();
        if (mQNAudioTrackInfo != null) {
            trackInfos.add(mQNAudioTrackInfo);
        }
        trackInfos.addAll(mQNVideoTrackInfos);
        return trackInfos;
    }


    public void setUserTrackInfo(QNRTCEngine engine, String userId, List<QNTrackInfo> trackInfos) {
        setUserTrackInfo(engine, userId, trackInfos, View.VISIBLE);
    }

    public void setUserTrackInfo(QNRTCEngine engine, String userId, List<QNTrackInfo> trackInfos, int microphoneViewVisibility) {
        LogD(TAG, "setUserTrackInfo() userId: " + userId);
        mQNRTCEngine = engine;
        mUserId = userId;
        mQNAudioTrackInfo = null;
        mQNVideoTrackInfos.clear();

        if (TextUtils.isEmpty(mUserId)) {
            return;
        }
        setMicrophoneStateVisibility(microphoneViewVisibility);
        mAudioView.setText(mUserId);
        onAddTrackInfo(trackInfos);

        String apiUrl = QNRtc.getUserInfoUrl();
        if (apiUrl != null && !apiUrl.isEmpty()) {
            JSONObject found = findUser(mUserId);
            if (found != null) {
                try {
                    mAudioView.setText(found.getString("name"));
                } catch (Exception ex) {
                    Log.i(TAG, ex.getMessage());
                }
            } else {
                new RequestNameTask().execute(QNRtc.getUserInfoUrl().replace("<USER_ID>", mUserId));
            }
        }
    }

    public void unSetUserTrackInfo() {
        if (mQNRTCEngine != null) {
            if (mTrackInfoDisplayInLargeView != null) {
                mQNRTCEngine.setRenderWindow(mTrackInfoDisplayInLargeView, null);
            }
            if (mTrackInfoDisplayInSmallView != null) {
                mQNRTCEngine.setRenderWindow(mTrackInfoDisplayInSmallView, null);
            }
        }
        reset();
    }

    public void reset() {
        LogD(TAG, "reset()");
        mQNRTCEngine = null;
        mUserId = null;
        mQNAudioTrackInfo = null;
        mQNVideoTrackInfos.clear();
        mPos = -1;

        mSurfaceViewLarge.setVisibility(View.GONE);
        mVideoViewLargeParent.setVisibility(View.GONE);
        mSurfaceViewSmall.setVisibility(View.GONE);
        mVideoViewSmallParent.setVisibility(View.GONE);
        mAudioView.setText("");
        mAudioView.setVisibility(View.GONE);
        mTrackInfoDisplayInLargeView = null;
        mTrackInfoDisplayInSmallView = null;
    }

    public void dispose() {
        LogD(TAG, "dispose()");
        mSurfaceViewLarge.release();
        mSurfaceViewSmall.release();
    }

    public void onAddTrackInfo(List<QNTrackInfo> trackInfos) {
        LogD(TAG, "onAddTrackInfo()");
        for (QNTrackInfo item : trackInfos) {
            onAddTrackInfo(item, false);
        }
        onTrackInfoChanged();
    }

    @SuppressWarnings("unused")
    public void onAddTrackInfo(QNTrackInfo trackInfo) {
        onAddTrackInfo(trackInfo, true);
    }

    private void onAddTrackInfo(QNTrackInfo trackInfo, boolean notify) {
        if (QNTrackKind.AUDIO.equals(trackInfo.getTrackKind())) {
            mQNAudioTrackInfo = trackInfo;
        } else {
            mQNVideoTrackInfos.add(trackInfo);
        }
        if (notify) {
            onTrackInfoChanged();
        }
    }

    public boolean onRemoveTrackInfo(List<QNTrackInfo> trackInfos) {
        LogD(TAG, "onRemoveTrackInfo()");
        for (QNTrackInfo item : trackInfos) {
            onRemoveTrackInfo(item, false);
        }
        onTrackInfoChanged();
        return mQNAudioTrackInfo != null || !mQNVideoTrackInfos.isEmpty();
    }

    @SuppressWarnings("unused")
    public void onRemoveTrackInfo(QNTrackInfo trackInfo) {
        onRemoveTrackInfo(trackInfo, true);
    }

    public void onRemoveTrackInfo(QNTrackInfo trackInfo, boolean notify) {
        if (QNTrackKind.AUDIO.equals(trackInfo.getTrackKind())) {
            mQNAudioTrackInfo = null;
        } else {
            mQNVideoTrackInfos.remove(trackInfo);
        }
        if (notify) {
            onTrackInfoChanged();
        }
    }

    public void onTracksMuteChanged() {
        LogD(TAG, "onTracksMuteChanged()");
        // audio track
        if (mQNAudioTrackInfo != null) {
            setMicrophoneStateVisibilityInner(View.VISIBLE);
            updateMicrophoneStateView(mQNAudioTrackInfo.isMuted());
        } else {
            setMicrophoneStateVisibilityInner(View.INVISIBLE);
        }

        // video tracks
        boolean hideAudioView = containsUnMutedVideoTracks(2);
        setAudioViewStateVisibility(hideAudioView ? View.GONE : View.VISIBLE);
        // note : mSurfaceViewSmall is on top, so set visibility
        if (mTrackInfoDisplayInSmallView != null) {
            mSurfaceViewSmall.setVisibility(hideAudioView ? View.VISIBLE : View.GONE);
        }
    }

    private void onTrackInfoChanged() {
        onTracksMuteChanged();

        QNTrackInfo cameraTrackInfo = findVideoTrack(TAG_CAMERA);
        QNTrackInfo screenTrackInfo = findVideoTrack(TAG_SCREEN);

        // in case, camera has no tag.
        if (cameraTrackInfo == null && !mQNVideoTrackInfos.isEmpty()) {
            List<QNTrackInfo> trackInfosExcludeScreenTrack = new ArrayList<>(mQNVideoTrackInfos);
            trackInfosExcludeScreenTrack.remove(screenTrackInfo);
            if (!trackInfosExcludeScreenTrack.isEmpty()) {
                cameraTrackInfo = trackInfosExcludeScreenTrack.get(0);
            }
        }

        QNTrackInfo trackInfoDisplayInLargeView = null;
        QNTrackInfo trackInfoDisplayInSmallView = null;
        if (cameraTrackInfo != null && screenTrackInfo != null) {
            LogD(TAG, "contains camera and screen track info");
            trackInfoDisplayInLargeView = screenTrackInfo;
            trackInfoDisplayInSmallView = cameraTrackInfo;
        } else {
            if (cameraTrackInfo != null) {
                LogD(TAG, "just contains camera track info");
                trackInfoDisplayInLargeView = cameraTrackInfo;
            }
            if (screenTrackInfo != null) {
                LogD(TAG, "just contains screen track info");
                trackInfoDisplayInLargeView = screenTrackInfo;
            }
        }
        updateTrackInfoInLargeView(trackInfoDisplayInLargeView);
        updateTrackInfoInSmallView(trackInfoDisplayInSmallView);
    }

    private QNTrackInfo findVideoTrack(String tag) {
        QNTrackInfo found = null;
        for (QNTrackInfo item : mQNVideoTrackInfos) {
            if (tag.equals(item.getTag())) {
                found = item;
                break;
            }
        }
        return found;
    }

    private void updateTrackInfoInLargeView(QNTrackInfo trackInfoDisplayInLargeView) {
        if (mTrackInfoDisplayInLargeView != null && mTrackInfoDisplayInLargeView == trackInfoDisplayInLargeView) {
            LogD(TAG, "skip updateTrackInfoInLargeView, same track");
            return;
        }
        mTrackInfoDisplayInLargeView = trackInfoDisplayInLargeView;
        if (mTrackInfoDisplayInLargeView != null) {
            if (DISPLAY_LARGE_VIDEO_TRACK) {
                mSurfaceViewLarge.setVisibility(View.VISIBLE);
                mQNRTCEngine.setRenderWindow(mTrackInfoDisplayInLargeView, mSurfaceViewLarge);
                if (TAG_SCREEN.equals(mTrackInfoDisplayInLargeView.getTag())) {
                    mSurfaceViewLarge.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
                } else {
                    mSurfaceViewLarge.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
                }
            } else {
                mSurfaceViewLarge.setVisibility(View.GONE);
                mVideoViewLargeParent.setBackgroundColor(getTargetColor(new Random().nextInt(6)));
            }
            mVideoViewLargeParent.setVisibility(View.VISIBLE);
        } else {
            mSurfaceViewLarge.setVisibility(View.GONE);
            mVideoViewLargeParent.setVisibility(View.GONE);
        }
    }

    private void updateTrackInfoInSmallView(QNTrackInfo trackInfoDisplayInSmallView) {
        if (mTrackInfoDisplayInSmallView != null && mTrackInfoDisplayInSmallView == trackInfoDisplayInSmallView) {
            LogD(TAG, "skip updateTrackInfoInSmallView, same track");
            return;
        }
        mTrackInfoDisplayInSmallView = trackInfoDisplayInSmallView;
        if (mTrackInfoDisplayInSmallView != null) {
            if (DISPLAY_SMALL_VIDEO_TRACK) {
                mSurfaceViewSmall.setVisibility(View.VISIBLE);
                mQNRTCEngine.setRenderWindow(mTrackInfoDisplayInSmallView, mSurfaceViewSmall);
            } else {
                mSurfaceViewSmall.setVisibility(View.GONE);
                mVideoViewSmallParent.setBackgroundColor(getTargetColor(new Random().nextInt(6)));
            }
            mVideoViewSmallParent.setVisibility(View.VISIBLE);
        } else {
            mSurfaceViewSmall.setVisibility(View.GONE);
            mVideoViewSmallParent.setVisibility(View.GONE);
        }
    }

    @SuppressWarnings("all")
    private boolean containsUnMutedVideoTracks(int count) {
        boolean unMuted = false;
        for (int i = 0; i < mQNVideoTrackInfos.size() && i < count; i++) {
            QNTrackInfo item = mQNVideoTrackInfos.get(i);
            if (!item.isMuted()) {
                unMuted = true;
                break;
            }
        }
        return unMuted;
    }


    public void changeViewBackgroundByPos(int pos) {
        LogD(TAG, "changeViewBackgroundByPos() " + pos);
        mPos = pos;
        if (mPos != -1) {
            mAudioView.setBackgroundColor(getTargetColor(pos));
        }
    }

    private void setAudioViewStateVisibility(int visibility) {
        mAudioView.setVisibility(visibility);
    }

    public void setMicrophoneStateVisibility(int visibility) {
        mMicrophoneViewVisibility = visibility;
        mMicrophoneStateView.setVisibility(visibility);
    }

    private void setMicrophoneStateVisibilityInner(int visibility) {
        if (mMicrophoneViewVisibility == -1 || mMicrophoneViewVisibility == View.VISIBLE) {
            mMicrophoneStateView.setVisibility(visibility);
        }
    }

    private void updateMicrophoneStateView(boolean isMute) {
        mMicrophoneStateView.setImageResource(isMute ? QNRtc.getResourceId("microphone_disable", "mipmap") : QNRtc.getResourceId("microphone_state_enable", "drawable"));
    }

    @SuppressWarnings("all")
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mVideoViewLargeParent = findViewById(QNRtc.getResourceId("qn_surface_view_large_parent", "id"));
        mSurfaceViewLarge = (QNSurfaceView) findViewById(QNRtc.getResourceId("qn_surface_view_large", "id"));

        mVideoViewSmallParent = findViewById(QNRtc.getResourceId("qn_surface_view_small_parent", "id"));
        mSurfaceViewSmall = (QNSurfaceView) findViewById(QNRtc.getResourceId("qn_surface_view_small", "id"));

        mMicrophoneStateView = (ImageView) findViewById(QNRtc.getResourceId("microphone_state_view", "id"));
        mAudioView = (TextView) findViewById(QNRtc.getResourceId("qn_audio_view", "id"));
    }

    @Override
    public void setVisibility(int visibility) {
        if (visibility == View.GONE) {
            mSurfaceViewLarge.setVisibility(visibility);
            mSurfaceViewSmall.setVisibility(visibility);
        } else {
            if (mTrackInfoDisplayInLargeView != null) {
                mSurfaceViewLarge.setVisibility(visibility);
            }
            if (mTrackInfoDisplayInSmallView != null) {
                mSurfaceViewSmall.setVisibility(visibility);
            }
        }
        super.setVisibility(visibility);
    }

    private int getTargetColor(int pos) {
        int[] customizedColors = getContext().getResources().getIntArray(QNRtc.getResourceId("audioBackgroundColors", "array"));
        return customizedColors[pos % 6];
    }

    public void setZOrderMediaOverlay(boolean isMediaOverlay, boolean onTop) {
        if (DISPLAY_LARGE_VIDEO_TRACK) {
            mSurfaceViewLarge.setZOrderMediaOverlay(isMediaOverlay);
        }
        if (DISPLAY_SMALL_VIDEO_TRACK) {
            mSurfaceViewSmall.setZOrderMediaOverlay(isMediaOverlay);
            mSurfaceViewSmall.setZOrderOnTop(onTop);
        }
    }

    public static void swap(QNRTCEngine engine, UserTrackView trackViewFirst, UserTrackView trackViewSecond) {
        String userIdFirst = trackViewFirst.getUserId();
        List<QNTrackInfo> trackInfosFirst = trackViewFirst.getTrackInfos();
        int postFirst = trackViewFirst.mPos;

        String userIdSecond = trackViewSecond.getUserId();
        List<QNTrackInfo> trackInfosSecond = trackViewSecond.getTrackInfos();
        int postSecond = trackViewSecond.mPos;

        trackViewFirst.setUserTrackInfo(engine, userIdSecond, trackInfosSecond, trackViewFirst.mMicrophoneViewVisibility);
        trackViewFirst.changeViewBackgroundByPos(postSecond);

        trackViewSecond.setUserTrackInfo(engine, userIdFirst, trackInfosFirst, trackViewSecond.mMicrophoneViewVisibility);
        trackViewSecond.changeViewBackgroundByPos(postFirst);
    }

    public String getResourceName() {
        try {
            return this.getResources().getResourceEntryName(this.getId());
        } catch (Resources.NotFoundException var2) {
            return "";
        }
    }


    private void LogD(String tag, String message) {
        if (PRINT_DEBUG_LOG) {
            Log.d(tag + " " + getResourceName(), message);
        }
    }

    class RequestNameTask extends AsyncTask<String, String, String>{

        @Override
        protected String doInBackground(String... uri) {
            String responseString = null;
            try {
                String urlStr = uri[0];
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                if(conn.getResponseCode() == HttpsURLConnection.HTTP_OK){
                    InputStream in = new BufferedInputStream(conn.getInputStream());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    String line;
                    StringBuilder result = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    responseString = result.toString();
                } else {
                    //response = "FAILED"; // See documentation for more info on response handling
                }
                conn.disconnect();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            try {
                final JSONObject jObject = new JSONObject(result);

                _usersInfo.add(0, jObject);

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        // UI thread
                        try {
                            mAudioView.setText(jObject.getString("name"));
                        } catch (Exception ex) {
                            Log.e(TAG, ex.getMessage());
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    private JSONObject findUser(String uid) {
        for (int i = 0; i < _usersInfo.size() - 1; i++) {
            try {
                if (_usersInfo.get(i).getString("id").equalsIgnoreCase(uid)) {
                    return _usersInfo.get(i);
                }
            } catch (Exception ex) {
                Log.i(TAG, ex.getMessage());
            }
        }
        return null;
    }
}
