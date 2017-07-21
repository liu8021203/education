package com.li.education.main.study;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.li.education.R;
import com.li.education.base.AppData;
import com.li.education.base.BaseActivity;
import com.li.education.base.bean.BaseResult;
import com.li.education.base.bean.HomeResult;
import com.li.education.base.bean.UploadStudyResult;
import com.li.education.base.bean.vo.ChapterListVO;
import com.li.education.base.bean.vo.ChapterVO;
import com.li.education.base.bean.vo.PlayUploadVO;
import com.li.education.base.http.HttpService;
import com.li.education.base.http.RetrofitUtil;
import com.li.education.main.identify.IdentifyActivity;
import com.li.education.main.identify.ScanActivity;
import com.li.education.util.UtilData;
import com.li.education.util.UtilDate;
import com.li.education.util.UtilGson;
import com.li.education.util.UtilIntent;
import com.li.education.util.UtilPixelTransfrom;
import com.li.education.util.UtilSPutil;
import com.li.education.view.LfSeekBar;
import com.pnikosis.materialishprogress.ProgressWheel;
import com.tencent.rtmp.ITXLivePlayListener;
import com.tencent.rtmp.TXLiveConstants;
import com.tencent.rtmp.TXLivePlayer;
import com.tencent.rtmp.ui.TXCloudVideoView;

import rx.Subscriber;

import static rx.android.schedulers.AndroidSchedulers.mainThread;
import static rx.schedulers.Schedulers.io;

/**
 * Created by liu on 2017/6/12.
 */

public class PlayActivity extends BaseActivity implements View.OnClickListener, ITXLivePlayListener {
    private Uri uri;
    private TXCloudVideoView mVideoView;
    private TXLivePlayer mLivePlayer;
    private ImageView mIvPlay;
    private TextView mTvTime;
    private ProgressWheel mProgressWheel;
    private RelativeLayout mRlTitleLayout;
    private FrameLayout mFlVideoLayout;
    private LinearLayout mLlLayout;
    private RelativeLayout mRlPlayNext;
    private TextView mTvPlayNext;
    private RelativeLayout rlController;
    private ImageView ivPlay;
    private TextView tvStart;
    private TextView tvTotal;
    private LfSeekBar mLfSeekBar;
    private ImageView ivFull;
    private boolean isFull = false;
    private int currTime = -1;
    private int currPosition = -1;

    private ChapterListVO data;
    private ChapterVO currVO;
    //开始时间
    private String startTime = "";
    private int faceTime = -1;

    private int pauseTime = 0;
    private int pause = 8;
    //循环上传
    private int n = 1;
    //首次启动
    private boolean isFirst = true;
    //是否播放
    private boolean isPlay = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 检测Vitamio是否解压解码包
        setContentView(R.layout.activity_play);
        Bundle bundle = getIntent().getExtras();
        currPosition = bundle.getInt("position", -1);
        if (currPosition == -1) {
            showToast("数据有误，请联系客服");
            return;
        } else {
            data = (ChapterListVO) bundle.getSerializable("data");
            currVO = data.getSysEduTypeList().get(currPosition);
            //判断是否学习完成
            if (currVO.getFinished().equals("N")) {
                //判断是否学习中
                if (currVO.getFinishing().equals("Y")) {
                    currTime = Integer.valueOf(currVO.getVideoTime());
                }
            }
        }
        pause = UtilSPutil.getInstance(this).getInt("pause", 8);
        initView();
        startTime = UtilDate.format(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss");
    }

    private Handler playHander = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            rlController.setVisibility(View.GONE);
        }
    };

    private Handler faceHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            face();
        }
    };
    //X分钟弹出人脸识别
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (faceTime == 0) {
                face();
                mHandler.removeCallbacksAndMessages(null);
            } else {
                faceTime--;
                Log.d("face", faceTime + "");
                mHandler.sendEmptyMessageDelayed(0, 1000);
            }
        }
    };
    //暂停x分钟自动退出学习
    private Handler pauseHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (pauseTime >= 60 * pause) {
                AppData.isValid = false;
                AppData.data.clear();
                pauseTime = 0;
                pauseHandler.removeCallbacksAndMessages(null);
                showToast("暂停时间太长自动退出学习状态");
                UtilSPutil.getInstance(PlayActivity.this).setLong("background", 0);
                finish();
            } else {
                Log.d("a", pauseTime + "");
                pauseTime++;
                pauseHandler.sendEmptyMessageDelayed(0, 1000);
            }
        }
    };

    private void initView() {
        mTvTime = (TextView) findViewById(R.id.tv_time);
        mVideoView = (TXCloudVideoView) findViewById(R.id.buffer);
        mVideoView.setOnClickListener(this);
        rlController = (RelativeLayout) findViewById(R.id.rl_controller);
        rlController.setVisibility(View.GONE);
        rlController.setEnabled(false);
        ivPlay = (ImageView) findViewById(R.id.iv_play_pause);
        ivPlay.setOnClickListener(this);
        tvStart = (TextView) findViewById(R.id.mediacontroller_time_current);
        tvTotal = (TextView) findViewById(R.id.mediacontroller_time_total);
        ivFull = (ImageView) findViewById(R.id.iv_full);
        ivFull.setOnClickListener(this);
        mLfSeekBar = (LfSeekBar) findViewById(R.id.mediacontroller_seekbar);
        mLivePlayer = new TXLivePlayer(this);
        uri = Uri.parse(currVO.getPic());
        mLivePlayer.setPlayerView(mVideoView);
        mLivePlayer.setPlayListener(this);
        mIvPlay = (ImageView) findViewById(R.id.iv_play);
        mIvPlay.setOnClickListener(this);
        mProgressWheel = (ProgressWheel) findViewById(R.id.progress_wheel);
        mFlVideoLayout = (FrameLayout) findViewById(R.id.fl_video_layout);
        mRlTitleLayout = (RelativeLayout) findViewById(R.id.rl_title_layout);
        mLlLayout = (LinearLayout) findViewById(R.id.ll_layout);
        mRlPlayNext = (RelativeLayout) findViewById(R.id.rl_play_next);
        mTvPlayNext = (TextView) findViewById(R.id.tv_play_next);
        mTvPlayNext.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        long timeTmp = UtilSPutil.getInstance(this).getLong("background", 0);
        if (timeTmp != 0 && System.currentTimeMillis() - timeTmp > this.pause * 60 * 1000L) {
            AppData.data.clear();
            AppData.isValid = false;
            UtilSPutil.getInstance(this).setLong("background", 0);
            showToast("超过8分钟，自动退出学习");
            finish();
        } else {
            if (isFirst) {
                isFirst = false;
                faceHandler.sendEmptyMessageDelayed(0, 1000);
            }else{
//                if(mLivePlayer != null){
//                    start();
//                }
                if(mVideoView != null){
                    mVideoView.onResume();
                }
            }

        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mLivePlayer != null && mLivePlayer.isPlaying()) {
            pause();
        }
        if(mVideoView != null){
            mVideoView.onPause();
        }
        mHandler.removeCallbacksAndMessages(null);
        UtilSPutil.getInstance(this).setLong("background", System.currentTimeMillis());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mLivePlayer != null) {
            mLivePlayer.stopPlay(true);
            mLivePlayer = null;
        }
        if (mVideoView != null){
            mVideoView.onDestroy();
            mVideoView = null;
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_play:
                mIvPlay.setVisibility(View.GONE);
                if(!isPlay) {
                    mProgressWheel.setVisibility(View.VISIBLE);
                    mLivePlayer.startPlay(currVO.getPic(), TXLivePlayer.PLAY_TYPE_VOD_MP4);
                }else{
                    start();
                }
                break;

            case R.id.iv_full:
                if (!isFull) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    isFull = true;
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    isFull = false;
                }
                break;

            case R.id.iv_back:
                finishAnimator();
                break;

            case R.id.tv_play_next:
                mRlPlayNext.setVisibility(View.GONE);
                mProgressWheel.setVisibility(View.VISIBLE);
                currPosition++;
                currVO = data.getSysEduTypeList().get(currPosition);
                currTime = Integer.valueOf(currVO.getVideoTime());
                startTime = UtilDate.format(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss");
                mLivePlayer.startPlay(currVO.getPic(), TXLivePlayer.PLAY_TYPE_VOD_MP4);
                break;

            case R.id.buffer:
                rlController.setVisibility(View.VISIBLE);
                playHander.sendEmptyMessageDelayed(0, 5000);
                break;
            case R.id.iv_play_pause:
                if(mLivePlayer.isPlaying()){
                    pause();
                }else{
                    start();
                }
                break;
        }
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            mRlTitleLayout.setVisibility(View.VISIBLE);
            mLlLayout.setVisibility(View.VISIBLE);
            LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, UtilPixelTransfrom.dip2px(PlayActivity.this, 220));
            mFlVideoLayout.setLayoutParams(params1);
            mFlVideoLayout.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

        } else {
            LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            mFlVideoLayout.setLayoutParams(params1);
            mFlVideoLayout.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
            mRlTitleLayout.setVisibility(View.GONE);
            mLlLayout.setVisibility(View.GONE);
        }
    }


    @Override
    public void onBackPressed() {
        if (isFull) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            isFull = false;
        } else {
            finishAnimator();
        }
    }

    private void start() {
        mLivePlayer.resume();
        ivPlay.setImageResource(R.mipmap.mediacontroller_pause);
        mIvPlay.setVisibility(View.GONE);
        mHandler.removeCallbacksAndMessages(null);
        mHandler.sendEmptyMessageDelayed(0,1000);
        pauseHandler.removeCallbacksAndMessages(null);
        pauseTime = 0;
    }

    private void pause() {
        mLivePlayer.pause();
        ivPlay.setImageResource(R.mipmap.mediacontroller_play);
        mIvPlay.setVisibility(View.VISIBLE);
        uploadData("N");
        mHandler.removeCallbacksAndMessages(null);
        pauseHandler.sendEmptyMessage(0);
    }

    private void uploadData(String isfinish) {

        PlayUploadVO tempVO = null;
        for (int i = 0; i < AppData.data.size(); i++) {
            if (AppData.data.get(i).getCode2().equals(currVO.getCode2())) {
                tempVO = AppData.data.get(i);
            }
        }
        if (currVO.getFinished().equals("N")) {
            String endTime = UtilDate.format(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss");
            int studyTime = Integer.valueOf(currVO.getVideoTime());
            if (tempVO == null) {
                PlayUploadVO uploadVO = new PlayUploadVO();
                uploadVO.setCode1(currVO.getCode1());
                uploadVO.setCode2(currVO.getCode2());
                uploadVO.setCrtTime(startTime);
                uploadVO.setUpdTime(endTime);
                uploadVO.setStudyTime(String.valueOf(currTime - studyTime));
                uploadVO.setFinishyn(isfinish);
                uploadVO.setStopWatchTime(String.valueOf(currTime));
                AppData.data.add(uploadVO);
            } else {
                tempVO.setUpdTime(endTime);
                tempVO.setFinishyn(isfinish);
                tempVO.setStudyTime(String.valueOf(currTime - studyTime));
                tempVO.setStopWatchTime(String.valueOf(currTime));
            }
        }
        if (isValid() || AppData.isValid) {
            mTvTime.setVisibility(View.GONE);
            String jsonStr = UtilGson.toJson(AppData.data);
            upload(jsonStr);
        }
    }


    private void upload(String eduJsonStr) {
        RetrofitUtil.getInstance().create(HttpService.class).insEduRecord(AppData.token, eduJsonStr, AppData.sessionid).subscribeOn(io()).observeOn(mainThread()).subscribe(new Subscriber<UploadStudyResult>() {

            @Override
            public void onStart() {
                super.onStart();
                Log.d("www", "onStart");
            }

            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                Log.d("www", "onError");
                e.printStackTrace();
                if (n <= 3) {
                    n++;
                    String jsonStr = UtilGson.toJson(AppData.data);
                    upload(jsonStr);
                }
            }

            @Override
            public void onNext(UploadStudyResult result) {
                Log.d("www", "onNext");
                if (result.isStatus()) {
                    AppData.isValid = true;
                    if (result.getData() != null && result.getData().getSessionid() != null) {
                        AppData.sessionid = result.getData().getSessionid();
                    }
                    AppData.data.clear();
                    currVO.setVideoTime(String.valueOf(currTime));
                    startTime = UtilDate.format(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss");
                } else {
                    if (n <= 3) {
                        n++;
                        String jsonStr = UtilGson.toJson(AppData.data);
                        upload(jsonStr);
                    }
                }
            }
        });
    }

    /**
     * 判断是否有效学时
     *
     * @return
     */
    public boolean isValid() {
        //无效学时
        boolean isValid = false;
        long totalTime = 0;
        for (int i = 0; i < AppData.data.size(); i++) {
            PlayUploadVO vo = AppData.data.get(i);
            long time = Integer.valueOf(vo.getStudyTime());
            totalTime += time;
        }
        if (AppData.cycle_code.equals("1") || AppData.cycle_code.equals("2")) {
            if (data.getLongtime() > 690 * 60) {
                return true;
            } else if (totalTime >= 30 * 60) {
                return true;
            }
        } else {
            if (data.getLongtime() > 1410 * 60) {
                return true;
            } else if (totalTime >= 30 * 60) {
                return true;
            }
        }
        return isValid;
    }

    private void face() {
        UtilIntent.intentResultDIYLeftToRight(this, ScanActivity.class, 999);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (resultCode) {
            case 0:
                AppData.isValid = false;
                AppData.data.clear();
                finish();
                break;

            case 1:
                initFaceTime();
                break;
        }
    }

    private void initFaceTime() {
        int faceRandom = UtilData.getRandom(1, 3);
        switch (faceRandom) {
            case 1:
                faceTime = 60 * 8;
                break;

            case 2:
                faceTime = 60 * 9;
                break;

            case 3:
                faceTime = 60 * 10;
                break;
        }
    }

    @Override
    public void onPlayEvent(int i, Bundle bundle) {
        //视频播放开始
        if (i == TXLiveConstants.PLAY_EVT_PLAY_BEGIN) {
            if(!isPlay) {
                if(currTime > 0) {
                    mLivePlayer.seek(currTime);
                }
                mHandler.removeCallbacksAndMessages(null);
                mHandler.sendEmptyMessageDelayed(0,1000);
                isPlay = true;
            }
            mProgressWheel.setVisibility(View.GONE);
            mIvPlay.setVisibility(View.GONE);
            ivPlay.setImageResource(R.mipmap.mediacontroller_pause);
        } else if (i == TXLiveConstants.PLAY_EVT_PLAY_PROGRESS) { //视频播放进度
            ivPlay.setImageResource(R.mipmap.mediacontroller_pause);
            int progress = bundle.getInt(TXLiveConstants.EVT_PLAY_PROGRESS);
            int duration = bundle.getInt(TXLiveConstants.EVT_PLAY_DURATION);
            currTime = duration;
            if (mLfSeekBar != null) {
                mLfSeekBar.setProgress(progress);
            }
            if (tvStart != null) {
                tvStart.setText(String.format("%02d:%02d", progress / 60, progress % 60));
            }
            if (tvTotal != null) {
                tvTotal.setText(String.format("%02d:%02d", duration / 60, duration % 60));
            }
            if (mLfSeekBar != null) {
                mLfSeekBar.setMax(duration);
            }
            return;
        } else if (i == TXLiveConstants.PLAY_EVT_PLAY_END) {
            isPlay = false;
            mRlPlayNext.setVisibility(View.VISIBLE);
            uploadData("Y");
            if(data.getSysEduTypeList() != null) {
                if(data.getSysEduTypeList().size() - 1 == currPosition){
                    mTvPlayNext.setText("全部播放完成");
                    mTvPlayNext.setOnClickListener(null);
                }else{
                    mTvPlayNext.setText("播放下一个");
                    mTvPlayNext.setOnClickListener(this);
                }
            }else{
                mTvPlayNext.setText("全部播放完成");
                mTvPlayNext.setOnClickListener(null);
            }
            if (tvStart != null) {
                tvStart.setText("00:00");
            }
            if (mLfSeekBar != null) {
                mLfSeekBar.setProgress(0);
            }
        } else if (i == TXLiveConstants.PLAY_EVT_PLAY_LOADING) { //视频播放加载中
//            mProgressWheel.setVisibility(View.VISIBLE);
//            mIvPlay.setVisibility(View.GONE);
        } else if (i == TXLiveConstants.PLAY_EVT_PLAY_BEGIN) {
//            mProgressWheel.setVisibility(View.GONE);
//            mIvPlay.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onNetStatus(Bundle bundle) {

    }


}
