package cordova.plugin.qnrtc.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.pili.pldroid.player.PLOnErrorListener;
import com.pili.pldroid.player.PLOnInfoListener;
import com.pili.pldroid.player.widget.PLVideoView;
import com.qiniu.droid.rtc.QNCustomMessage;
import com.qiniu.droid.rtc.QNRTCEngine;
import com.qiniu.droid.rtc.QNRTCEngineEventListener;
import com.qiniu.droid.rtc.QNRoomState;
import com.qiniu.droid.rtc.QNStatisticsReport;
import com.qiniu.droid.rtc.QNTrackInfo;
import com.qiniu.droid.rtc.model.QNAudioDevice;
import com.qiniu.droid.rtc.model.QNMergeTrackOption;

import java.util.ArrayList;
import java.util.List;

import cordova.plugin.qnrtc.QNRtc;
import cordova.plugin.qnrtc.model.RemoteTrack;
import cordova.plugin.qnrtc.model.RemoteUser;
import cordova.plugin.qnrtc.model.RemoteUserList;
import cordova.plugin.qnrtc.ui.CircleTextView;
import cordova.plugin.qnrtc.ui.MergeLayoutConfigView;
import cordova.plugin.qnrtc.utils.QNAppServer;
import cordova.plugin.qnrtc.utils.SplitUtils;
import cordova.plugin.qnrtc.utils.ToastUtils;

public class LiveRoomActivity extends Activity implements QNRTCEngineEventListener {
    private static final String TAG = "LiveRoomActivity";
    public static final String EXTRA_ROOM_ID = "ROOM_ID";
    public static final String EXTRA_ROOM_TOKEN = "ROOM_TOKEN";
    public static final String EXTRA_USER_ID = "USER_ID";
    private static final String BASE_URL = "rtmp://pili-rtmp.qnsdk.com/sdk-live/";

    private static final String[] MANDATORY_PERMISSIONS = {
            "android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.RECORD_AUDIO",
            "android.permission.INTERNET",
            "android.permission.CAMERA"
    };

    private PLVideoView mVideoView;
    private LinearLayout mLogView;
    private TextView mAudioBitrateText;
    private TextView mAudioFpsText;
    private TextView mVideoBitrateText;
    private TextView mVideoFpsText;
    private TextView mRemoteTextView;
    private Chronometer mTimer;
    private PopupWindow mPopWindow;
    private Toast mLogToast;

    private QNRTCEngine mQNRTCEngine;
    private StringBuffer mRemoteLogText;
    private RemoteUserList mRemoteUserList;
    private String mToken;
    private MergeLayoutConfigView mMergeLayoutConfigView;
    private UserListAdapter mUserListAdapter;
    private RemoteUser mChooseUser;
    private boolean mIsJoinedRoom = false;
    private String mUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(getSystemUiVisibility());
        setContentView(QNRtc.getResourceId("activity_live_room", "layout"));

        Intent intent = getIntent();
        String mRtmpUrl = BASE_URL + intent.getStringExtra(EXTRA_ROOM_ID);
        mToken = intent.getStringExtra(EXTRA_ROOM_TOKEN);
        mUserName = intent.getStringExtra(EXTRA_USER_ID);

        mTimer = (Chronometer) findViewById(QNRtc.getResourceId("timer", "id"));
        mLogView = (LinearLayout) findViewById(QNRtc.getResourceId("log_text", "id"));
        ImageButton mLogButton = (ImageButton) findViewById(QNRtc.getResourceId("log_shown_button", "id"));
        mLogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLogView.setVisibility(mLogView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            }
        });
        mAudioBitrateText = (TextView) findViewById(QNRtc.getResourceId("audio_bitrate_log_text", "id"));
        mAudioFpsText = (TextView) findViewById(QNRtc.getResourceId("audio_fps_log_text", "id"));
        mVideoBitrateText = (TextView) findViewById(QNRtc.getResourceId("video_bitrate_log_text", "id"));
        mVideoFpsText = (TextView) findViewById(QNRtc.getResourceId("video_fps_log_text", "id"));
        mRemoteTextView = (TextView) findViewById(QNRtc.getResourceId("remote_log_text", "id"));
        mRemoteTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
        mVideoView = (PLVideoView) findViewById(QNRtc.getResourceId("PLVideoView", "id"));

        mRemoteUserList = new RemoteUserList();

        //权限校验
        for (String permission : MANDATORY_PERMISSIONS) {
            if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                logAndToast("Permission " + permission + " is not granted");
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
        }

        //初始化RTC
        mQNRTCEngine = QNRTCEngine.createEngine(this, this);
        mQNRTCEngine.setAutoSubscribe(false);

        //播放器相关
        mVideoView.setVideoPath(mRtmpUrl);
        mVideoView.setOnErrorListener(new PLOnErrorListener() {
            @Override
            public boolean onError(int errorCode) {
                switch (errorCode) {
                    case ERROR_CODE_OPEN_FAILED:
                        logAndToast("播放器打开失败");
                        break;
                    case ERROR_CODE_IO_ERROR:
                        logAndToast("网络异常");
                        break;
                    default:
                        logAndToast("PlayerError Code: " + errorCode);
                        break;
                }
                return false;
            }
        });
        mVideoView.setOnInfoListener(new PLOnInfoListener() {
            @Override
            public void onInfo(int what, int extra) {
                switch (what) {
                    case MEDIA_INFO_VIDEO_RENDERING_START:
                        mTimer.setBase(SystemClock.elapsedRealtime());
                        mTimer.start();
                        break;
                    case MEDIA_INFO_VIDEO_BITRATE:
                        mVideoBitrateText.setText("VideoBitrate: " + extra / 1000 + " kb/s");
                        break;
                    case MEDIA_INFO_VIDEO_FPS:
                        mVideoFpsText.setText("VideoFps: " + extra);
                        break;
                    case MEDIA_INFO_AUDIO_BITRATE:
                        mAudioBitrateText.setText("AudioBitrate: " + extra / 1000 + " kb/s");
                        break;
                    case MEDIA_INFO_AUDIO_FPS:
                        mAudioFpsText.setText("AudioFps: " + extra);
                        break;
                }
            }
        });

        mMergeLayoutConfigView = new MergeLayoutConfigView(this);
        mUserListAdapter = new UserListAdapter();
        mMergeLayoutConfigView.getUserListView().setAdapter(mUserListAdapter);
        mMergeLayoutConfigView.getBtnConfirm().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<RemoteTrack> remoteTracks = mMergeLayoutConfigView.updateMergeOptions();
                List<QNMergeTrackOption> addedTrackOptions = new ArrayList<>();
                List<QNMergeTrackOption> removedTrackOptions = new ArrayList<>();
                for (RemoteTrack item : remoteTracks) {
                    if (item.isTrackInclude()) {
                        addedTrackOptions.add(item.getQNMergeTrackOption());
                    } else {
                        removedTrackOptions.add(item.getQNMergeTrackOption());
                    }
                }
                if (!addedTrackOptions.isEmpty()) {
                    mQNRTCEngine.setMergeStreamLayouts(addedTrackOptions, null);
                }
                if (!removedTrackOptions.isEmpty()) {
                    mQNRTCEngine.removeMergeStreamLayouts(removedTrackOptions, null);
                }
                if (mPopWindow != null) {
                    mPopWindow.dismiss();
                }
                ToastUtils.s(LiveRoomActivity.this, "已发送合流配置，请等待合流画面生效");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCall();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mVideoView.stopPlayback();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mPopWindow != null && mPopWindow.isShowing()) {
            mPopWindow.dismiss();
        }
        mPopWindow = null;
    }

    @Override
    public void onBackPressed() {
        disconnect();
        super.onBackPressed();
    }

    private void startCall() {
        if (mQNRTCEngine == null) {
            return;
        }
        logAndToast("正在连接");
        mQNRTCEngine.joinRoom(mToken);
    }

    private void disconnect() {
        if (mLogToast != null) {
            mLogToast.cancel();
        }
        if (mQNRTCEngine != null) {
            if (isAdmin(mUserName)) {
                mQNRTCEngine.stopMergeStream(null);
            }
            mQNRTCEngine.destroy();
        }
        mIsJoinedRoom = false;
    }

    private void logAndToast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, msg);
                if (mLogToast != null) {
                    mLogToast.cancel();
                }
                mLogToast = Toast.makeText(LiveRoomActivity.this, msg, Toast.LENGTH_SHORT);
                mLogToast.show();
            }
        });
    }

    public void onClickConfig(View view) {
        if (mIsJoinedRoom && mRemoteUserList.size() != 0) {
            if (!isAdmin(mUserName)) {
                ToastUtils.s(LiveRoomActivity.this, "您不是admin用户，无法进行合流操作");
            } else {
                //配置页
                mChooseUser = mRemoteUserList.getRemoteUserByPosition(0);
                mMergeLayoutConfigView.updateConfigInfo(mChooseUser);
                mUserListAdapter.notifyDataSetChanged();

                mPopWindow = new PopupWindow(mMergeLayoutConfigView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
                mPopWindow.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, QNRtc.getResourceId("popupWindowBackground", "color"))));
                mPopWindow.showAtLocation(getWindow().getDecorView().getRootView(), Gravity.BOTTOM, 0, 0);
            }
        } else {
            logAndToast("会议房间目前没有用户");
        }
    }

    @TargetApi(19)
    private static int getSystemUiVisibility() {
        int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        return flags;
    }

    public void logAndUpdateRemoteLogText(final String logText) {
        Log.i(TAG, logText);
        if (mRemoteLogText == null) {
            mRemoteLogText = new StringBuffer();
        }
        if (mLogView != null && mLogView.getVisibility() == View.VISIBLE) {
            mRemoteTextView.setText(mRemoteLogText.append(logText + "\n"));
        }
    }

    /// implement QNRTCEngineEventListener
    @Override
    public void onRoomStateChanged(QNRoomState state) {
        if (state == QNRoomState.CONNECTED) {
            mIsJoinedRoom = true;
            if (mRemoteUserList.size() == 0) {
                showErrorDialog("找不到会议直播，请确认该房间是否有其他用户发布流。");
            } else {
                mVideoView.start();
                logAndToast(getString(QNRtc.getResourceId("connected_to_room", "string")));
            }
        }
    }

    @Override
    public void onRoomLeft() {

    }

    @Override
    public void onRemoteUserJoined(String remoteUserId, String userData) {
        logAndUpdateRemoteLogText("onRemoteUserJoined : " + remoteUserId);
        mRemoteUserList.onUserJoined(remoteUserId, userData);
        if (mUserListAdapter != null) {
            mUserListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onRemoteUserLeft(String remoteUserId) {
        logAndUpdateRemoteLogText("onRemoteUserJoined : " + remoteUserId);
        mRemoteUserList.onUserLeft(remoteUserId);
        if (mUserListAdapter != null) {
            mUserListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onMessageReceived(QNCustomMessage message) {
    }

    @Override
    public void onLocalPublished(List<QNTrackInfo> trackInfoList) {
        Log.i(TAG, "onLocalPublished");
    }

    @Override
    public void onRemotePublished(String remoteUserId, List<QNTrackInfo> trackInfoList) {
        logAndUpdateRemoteLogText("onRemotePublished : " + remoteUserId);
        mRemoteUserList.onTracksPublished(remoteUserId, trackInfoList);
        if (isAdmin(mUserName)) {
            resetMergeStream();
        }
    }

    @Override
    public void onRemoteUnpublished(String remoteUserId, List<QNTrackInfo> trackInfoList) {
        logAndUpdateRemoteLogText("onRemoteUnpublished : " + remoteUserId);
        mRemoteUserList.onTracksUnPublished(remoteUserId, trackInfoList);
        if (isAdmin(mUserName)) {
            resetMergeStream();
        }
    }

    @Override
    public void onRemoteUserMuted(String remoteUserId, List<QNTrackInfo> trackInfoList) {
        logAndUpdateRemoteLogText("onRemoteUserMuted : " + remoteUserId);
    }

    @Override
    public void onSubscribed(String remoteUserId, List<QNTrackInfo> trackInfoList) {
        logAndUpdateRemoteLogText("onSubscribed : " + remoteUserId);
    }

    @Override
    public void onKickedOut(String userId) {
        logAndUpdateRemoteLogText("onUserKickedOut : " + userId);
    }

    @Override
    public void onError(int i, final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToastUtils.s(LiveRoomActivity.this, s);
            }
        });
        mIsJoinedRoom = false;
        disconnect();
        finish();
    }

    @Override
    public void onStatisticsUpdated(QNStatisticsReport report) {
        Log.i(TAG, "onStatisticsUpdated");
    }

    @Override
    public void onRemoteStatisticsUpdated(List<QNStatisticsReport> list) {

    }

    @Override
    public void onAudioRouteChanged(QNAudioDevice routing) {
        Log.i(TAG, "onAudioRouteChanged: " + routing.value());
    }

    @Override
    public void onCreateMergeJobSuccess(String mergeJobId) {
        Log.i(TAG, "onCreateMergeJobSuccess: " + mergeJobId);
    }

    private class UserListAdapter extends RecyclerView.Adapter<ViewHolder> {

        int[] mColor = {
                Color.parseColor("#588CEE"),
                Color.parseColor("#F8CF5F"),
                Color.parseColor("#4D9F67"),
                Color.parseColor("#F23A48")
        };

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(getApplicationContext()).inflate(QNRtc.getResourceId("item_user", "layout"), parent, false));
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            RemoteUser remoteUser = mRemoteUserList.getRemoteUserByPosition(position);
            String userId = remoteUser.getUserId();
            holder.username.setText(userId);
            holder.username.setCircleColor(mColor[position % 4]);
            if (mChooseUser != null && mChooseUser.getUserId().equals(userId)) {
                holder.itemView.setBackground(getResources().getDrawable(QNRtc.getResourceId("white_background", "drawable")));
            } else {
                holder.itemView.setBackgroundResource(0);
            }
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mChooseUser = mRemoteUserList.getRemoteUserByPosition(holder.getAdapterPosition());
                    mMergeLayoutConfigView.updateConfigInfo(mChooseUser);
                    notifyDataSetChanged();
                }
            });
        }

        @Override
        public int getItemCount() {
            return mRemoteUserList.size();
        }
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        CircleTextView username;

        private ViewHolder(View itemView) {
            super(itemView);
            username = (CircleTextView) itemView.findViewById(QNRtc.getResourceId("user_name_text", "id"));
        }
    }

    private void resetMergeStream() {
        Log.d(TAG, "resetMergeStream()");
        List<QNMergeTrackOption> configuredMergeTracksOptions = new ArrayList<>();

        // video tracks merge layout options.
        List<RemoteTrack> remoteVideoTrackInfoList = mRemoteUserList.getRemoteVideoTracks();
        if (!remoteVideoTrackInfoList.isEmpty()) {
            List<QNMergeTrackOption> mergeTrackOptions = SplitUtils.split(remoteVideoTrackInfoList.size(),
                    QNAppServer.STREAMING_WIDTH, QNAppServer.STREAMING_HEIGHT);
            if (mergeTrackOptions.size() != remoteVideoTrackInfoList.size()) {
                Log.e(TAG, "split option error.");
                return;
            }

            for (int i = 0; i < mergeTrackOptions.size(); i++) {
                RemoteTrack remoteTrack = remoteVideoTrackInfoList.get(i);

                if (!remoteTrack.isTrackInclude()) {
                    continue;
                }
                QNMergeTrackOption item = mergeTrackOptions.get(i);
                remoteTrack.updateQNMergeTrackOption(item);
                configuredMergeTracksOptions.add(remoteTrack.getQNMergeTrackOption());
            }
        }

        // audio tracks merge layout options
        List<RemoteTrack> remoteAudioTrackInfoList = mRemoteUserList.getRemoteAudioTracks();
        if (!remoteAudioTrackInfoList.isEmpty()) {
            for (RemoteTrack remoteTrack : remoteAudioTrackInfoList) {
                if (!remoteTrack.isTrackInclude()) {
                    continue;
                }
                configuredMergeTracksOptions.add(remoteTrack.getQNMergeTrackOption());
            }
        }

        mQNRTCEngine.setMergeStreamLayouts(configuredMergeTracksOptions, null);
    }

    private boolean isAdmin(String username) {
        return username.trim().equals(QNAppServer.ADMIN_USER);
    }

    private void showErrorDialog(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(LiveRoomActivity.this)
                        .setTitle(getText(QNRtc.getResourceId("channel_error_title", "string")))
                        .setMessage(message)
                        .setCancelable(false)
                        .setNeutralButton(QNRtc.getResourceId("positive_dialog_tips", "string"),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                        disconnect();
                                        finish();
                                    }
                                })
                        .create()
                        .show();
            }
        });
    }
}
