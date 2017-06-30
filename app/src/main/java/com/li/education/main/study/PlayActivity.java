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

import com.bumptech.glide.Glide;
import com.li.education.R;
import com.li.education.base.AppData;
import com.li.education.base.BaseActivity;
import com.li.education.base.bean.BaseResult;
import com.li.education.base.bean.HomeResult;
import com.li.education.base.bean.vo.ChapterListVO;
import com.li.education.base.bean.vo.ChapterVO;
import com.li.education.base.bean.vo.PlayUploadVO;
import com.li.education.base.http.HttpService;
import com.li.education.base.http.RetrofitUtil;
import com.li.education.main.identify.IdentifyActivity;
import com.li.education.util.UtilData;
import com.li.education.util.UtilDate;
import com.li.education.util.UtilGson;
import com.li.education.util.UtilIntent;
import com.li.education.util.UtilSPutil;
import com.pnikosis.materialishprogress.ProgressWheel;

import java.util.HashMap;
import java.util.Timer;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.utils.UtilPixelTransfrom;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;
import rx.Subscriber;

import static io.vov.vitamio.MediaPlayer.MEDIA_INFO_BUFFERING_END;
import static io.vov.vitamio.MediaPlayer.MEDIA_INFO_BUFFERING_START;
import static rx.android.schedulers.AndroidSchedulers.mainThread;
import static rx.schedulers.Schedulers.io;

/**
 * Created by liu on 2017/6/12.
 */

public class PlayActivity extends BaseActivity implements MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener, View.OnClickListener, MediaController.PlayStateCallBackListener, MediaPlayer.OnCompletionListener {
    private Uri uri;
    private VideoView mVideoView;
    private ImageView mIvPlay;
    private TextView mTvTime;
    private ProgressWheel mProgressWheel;
    private MediaController mMediaController;
    private RelativeLayout mRlTitleLayout;
    private FrameLayout mFlVideoLayout;
    private LinearLayout mLlLayout;
    private RelativeLayout mRlPlayNext;
    private TextView mTvPlayNext;
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
        faceHandler.sendEmptyMessageDelayed(0,1000);
    }

    private Handler faceHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            face();
        }
    };
    //X分钟弹出人脸识别
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(faceTime == 0){
                face();
            }else{
                faceTime--;
                Log.d("face", faceTime + "");
                mHandler.sendEmptyMessageDelayed(0,1000);
            }
        }
    };
    //暂停x分钟自动退出学习
    private Handler pauseHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(pauseTime >= 60 * pause){
                showToast("暂停时间太长自动退出学习状态");
                finish();
            }else{
                pauseTime++;
                pauseHandler.sendEmptyMessageDelayed(0,1000);
            }
        }
    };

    private void initView() {
        mTvTime = (TextView) findViewById(R.id.tv_time);
        mTvTime.setText("有效学时：" + data.getLongtime() / 60);
        mVideoView = (VideoView) findViewById(R.id.buffer);
        uri = Uri.parse(currVO.getPic());
        mVideoView.setVideoURI(uri);
        mMediaController = new MediaController(this);
        mMediaController.setFullClickListener(this);
        mMediaController.setPlayStateCallBackListener(this);
        mVideoView.setMediaController(mMediaController);
        mVideoView.requestFocus();
        mVideoView.setOnInfoListener(this);
        mVideoView.setOnBufferingUpdateListener(this);
        mVideoView.setOnCompletionListener(this);
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                // optional need Vitamio 4.0
                mediaPlayer.setPlaybackSpeed(1.0f);
                mProgressWheel.setVisibility(View.GONE);
                mIvPlay.setVisibility(View.VISIBLE);
            }
        });
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mVideoView != null && mVideoView.isPlaying()) {
            currTime = (int) mVideoView.getCurrentPosition();
            mMediaController.pause();
        }
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MEDIA_INFO_BUFFERING_START:
                if (mVideoView.isPlaying()) {
                    mProgressWheel.setVisibility(View.VISIBLE);
                }
                break;

            case MEDIA_INFO_BUFFERING_END:
                mProgressWheel.setVisibility(View.GONE);
                break;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_play:
                mIvPlay.setVisibility(View.GONE);
                if (currTime > 0) {
                    mVideoView.seekTo(currTime);
                }
                mMediaController.start();
                break;

            case R.id.iv_full:
                if (!isFull) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    mMediaController.hide();
                    isFull = true;
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    mMediaController.hide();
                    isFull = false;
                }
                break;

            case R.id.iv_back:
                finishAnimator();
                break;

            case R.id.tv_play_next:
                mRlPlayNext.setVisibility(View.GONE);
                currPosition++;
                currVO = data.getSysEduTypeList().get(currPosition);
                uri = Uri.parse(currVO.getPic());
                mVideoView.setVideoURI(uri);
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
            mMediaController.hide();
            isFull = false;
        } else {
            finishAnimator();
        }
    }

    @Override
    public void start() {
        mIvPlay.setVisibility(View.GONE);
        mHandler.sendEmptyMessage(0);
        pauseHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void pause() {
        mIvPlay.setVisibility(View.VISIBLE);
        if (mVideoView != null) {
            currTime = (int) mVideoView.getCurrentPosition();
        }
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
                uploadVO.setStudyTime(String.valueOf(mVideoView.getCurrentPosition() - studyTime));
                uploadVO.setFinishyn(isfinish);
                uploadVO.setStopWatchTime(String.valueOf(mVideoView.getCurrentPosition()));
                AppData.data.add(uploadVO);
            } else {
                tempVO.setUpdTime(endTime);
                tempVO.setStopWatchTime(String.valueOf(mVideoView.getCurrentPosition()));
                tempVO.setStudyTime(String.valueOf(mVideoView.getCurrentPosition() - studyTime));
            }
        }
        if (isValid()) {
            String jsonStr = UtilGson.toJson(AppData.data);
            upload(jsonStr);
        }
    }


    private void upload(String eduJsonStr) {
        RetrofitUtil.getInstance().create(HttpService.class).insEduRecord(AppData.token, eduJsonStr).subscribeOn(io()).observeOn(mainThread()).subscribe(new Subscriber<BaseResult>() {

            @Override
            public void onStart() {
                super.onStart();
            }

            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }

            @Override
            public void onNext(BaseResult result) {
                if (result.isStatus()) {
                    AppData.data.clear();
                    currVO.setVideoTime(String.valueOf(currTime));
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
        if(AppData.cycle_code.equals("1") || AppData.cycle_code.equals("2")){
            if(data.getLongtime() > 690 * 60){
                return true;
            }else if(totalTime >= 1800){
                return true;
            }
        }else{
            if(data.getLongtime() > 1410 * 60){
                return true;
            }else if(totalTime >= 1800){
                return true;
            }
        }
        return isValid;
    }

    private void face() {
        UtilIntent.intentResultDIYLeftToRight(this, IdentifyActivity.class, 999);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (resultCode){
            case 0:
                AppData.data.clear();
                finish();
                break;

            case 1:
                initFaceTime();
                mHandler.sendEmptyMessageDelayed(0, 1000);
                break;
        }
    }

    private void initFaceTime() {
        int faceRandom = UtilData.getRandom(1,3);
        switch (faceRandom){
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
    public void onCompletion(MediaPlayer mp) {
        mRlPlayNext.setVisibility(View.VISIBLE);
        if(data.getSysEduTypeList() != null) {
            if(data.getSysEduTypeList().size() - 1 == currPosition){
                mTvPlayNext.setText("全部播放完成");
                mTvPlayNext.setOnClickListener(null);
            }else{
                mTvPlayNext.setText("播放下一个");
                mTvPlayNext.setOnClickListener(this);
            }
            uploadData("Y");
        }else{
            mTvPlayNext.setText("全部播放完成");
            mTvPlayNext.setOnClickListener(null);
        }
    }

}
