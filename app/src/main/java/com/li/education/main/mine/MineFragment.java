package com.li.education.main.mine;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.li.education.MainActivity;
import com.li.education.R;
import com.li.education.base.AppData;
import com.li.education.base.BaseActivity;
import com.li.education.base.BaseFragment;
import com.li.education.base.bean.BaseResult;
import com.li.education.base.bean.HomeResult;
import com.li.education.base.bean.InfoResult;
import com.li.education.base.bean.vo.AreaVO;
import com.li.education.base.bean.vo.CityVO;
import com.li.education.base.common.BaseSubscriber;
import com.li.education.base.common.TokenManager;
import com.li.education.base.http.HttpService;
import com.li.education.base.http.RetrofitUtil;
import com.li.education.main.identify.IdentifyActivity;
import com.li.education.main.identify.ScanActivity;
import com.li.education.main.mine.adapter.AdapterVO;
import com.li.education.main.study.PlayActivity;
import com.li.education.util.UtilBitmap;
import com.li.education.util.UtilGlide;
import com.li.education.util.UtilIntent;
import com.li.education.util.UtilMD5Encryption;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import rx.Subscriber;

import static rx.android.schedulers.AndroidSchedulers.mainThread;
import static rx.schedulers.Schedulers.io;

/**
 * Created by liu on 2017/6/3.
 */

public class MineFragment extends BaseFragment implements View.OnClickListener {
    private View mView;
    private CircleImageView mHeadImg;
    private TextView mTvLoginRegister;
    private RelativeLayout mRlInfo;
    private RelativeLayout mRlModify;
    private RelativeLayout mRlExam;
    private RelativeLayout mRlExamRecord;
    private RelativeLayout mRlStudy;
    private RelativeLayout mRlFeed;
    private RelativeLayout mRlCall;
    private RelativeLayout mRlAbout;
    private RelativeLayout mRlExit;

    private MainActivity mActivity;
    private LinearLayout mLlSignout;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_mine, container, false);
        return mView;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mActivity = (MainActivity) getActivity();
        initView();
        getInfo();
    }



    private void initView() {
        mHeadImg = (CircleImageView) mView.findViewById(R.id.cv_img);
        mHeadImg.setOnClickListener(this);
        mLlSignout = (LinearLayout) mView.findViewById(R.id.ll_signout);
        mTvLoginRegister = (TextView) mView.findViewById(R.id.tv_login_register);
        mTvLoginRegister.setOnClickListener(this);
        mRlInfo = (RelativeLayout) mView.findViewById(R.id.rl_info);
        mRlModify = (RelativeLayout) mView.findViewById(R.id.rl_modify);
        mRlExam = (RelativeLayout) mView.findViewById(R.id.rl_exam);
        mRlExamRecord = (RelativeLayout) mView.findViewById(R.id.rl_exam_record);
        mRlStudy = (RelativeLayout) mView.findViewById(R.id.rl_study);
        mRlFeed = (RelativeLayout) mView.findViewById(R.id.rl_feed);
        mRlCall = (RelativeLayout) mView.findViewById(R.id.rl_call);
        mRlAbout = (RelativeLayout) mView.findViewById(R.id.rl_about);
        mRlExit = (RelativeLayout) mView.findViewById(R.id.rl_exit);
        mRlInfo.setOnClickListener(this);
        mRlModify.setOnClickListener(this);
        mRlExam.setOnClickListener(this);
        mRlExamRecord.setOnClickListener(this);
        mRlStudy.setOnClickListener(this);
        mRlFeed.setOnClickListener(this);
        mRlCall.setOnClickListener(this);
        mRlAbout.setOnClickListener(this);
        mRlExit.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cv_img:
                ChoosePicDialog dialog = new ChoosePicDialog((BaseActivity) getActivity());
                dialog.show();
//                UtilIntent.intentDIYLeftToRight(getActivity(), ScanActivity.class);
                break;

            case R.id.rl_exam:
                if(TextUtils.isEmpty(AppData.token)){
                    UtilIntent.intentDIYLeftToRight(getActivity(), LoginActivity.class);
                }else {
                    if(!TextUtils.isEmpty(AppData.examYN) && AppData.examYN.equals("Y")) {
                        UtilIntent.intentDIYLeftToRight(getActivity(), ExamActivity.class);
                    }else{
                        mActivity.showToast("请学习完相关课程再进行考试");
                    }
                }
                break;

            case R.id.rl_exam_record:
                if(TextUtils.isEmpty(AppData.token)) {
                    UtilIntent.intentDIYLeftToRight(getActivity(), LoginActivity.class);
                }else{
                    UtilIntent.intentDIYLeftToRight(getActivity(), ExamRecordActivity.class);
                }
                break;

            case R.id.rl_modify:
                if(TextUtils.isEmpty(AppData.token)){
                    UtilIntent.intentDIYLeftToRight(getActivity(), LoginActivity.class);
                }else {
                    UtilIntent.intentDIYLeftToRight(getActivity(), ModifyActivity.class);
                }
                break;

            case R.id.rl_info:
                if(TextUtils.isEmpty(AppData.token)){
                    UtilIntent.intentDIYLeftToRight(getActivity(), LoginActivity.class);
                }else {
                    UtilIntent.intentDIYLeftToRight(getActivity(), InfoActivity.class);
                }
                break;

            case R.id.rl_feed:
                if(TextUtils.isEmpty(AppData.token)){
                    UtilIntent.intentDIYLeftToRight(getActivity(), LoginActivity.class);
                }else {
                    UtilIntent.intentDIYLeftToRight(getActivity(), FeedBackActivity.class);
                }
                break;
            case R.id.rl_study:
                if (TextUtils.isEmpty(AppData.token)) {
                    UtilIntent.intentDIYLeftToRight(getActivity(), LoginActivity.class);
                } else {
                    UtilIntent.intentDIYLeftToRight(getActivity(), StudyRecordActivity.class);
                }
                break;
            case R.id.rl_call:
                UtilIntent.intentDIYLeftToRight(getActivity(), LocationActivity.class);
                break;
            case R.id.rl_about:
                UtilIntent.intentDIYLeftToRight(getActivity(), AboutActivity.class);
                break;
            case R.id.rl_exit:
                if(!TextUtils.isEmpty(AppData.token)){
                    signout();
                }else{
                    mActivity.showToast("请先登录");
                }
                break;

            case R.id.tv_login_register:
                UtilIntent.intentDIYLeftToRight(getActivity(), LoginActivity.class);
//                UtilIntent.intentDIYLeftToRight(getActivity(), ScanActivity.class);
                break;

        }
    }

    private void signout() {
        RetrofitUtil.getInstance().create(HttpService.class).signout(AppData.token).subscribeOn(io()).observeOn(mainThread()).subscribe(new Subscriber<BaseResult>() {

            @Override
            public void onStart() {
                super.onStart();
                mActivity.showProgressDialog();
            }

            @Override
            public void onCompleted() {
                mActivity.removeProgressDialog();
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
                mActivity.removeProgressDialog();
                mActivity.showToast("服务器出错");
            }

            @Override
            public void onNext(BaseResult result) {
                if(result.isStatus()){
                    AppData.token = "";
                    TokenManager.signout(getActivity());
                    loginState();
                }else{
                    if(result.getMessage().equals("99")){
                        AppData.token = "";
                        TokenManager.signout(getActivity());
                        loginState();
                    }else{
                        mActivity.showToast(result.getMessage());
                    }
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loginState();
        if(!TextUtils.isEmpty(AppData.url) && mHeadImg != null){
            Bitmap bitmap = UtilBitmap.base64ToBitmap(AppData.url);
            mHeadImg.setImageBitmap(bitmap);
        }
    }

    private void loginState() {
        if (!TokenManager.isLogin(getActivity())) {
            mHeadImg.setVisibility(View.GONE);
            mTvLoginRegister.setVisibility(View.VISIBLE);
            mLlSignout.setVisibility(View.GONE);
        } else {
            mHeadImg.setVisibility(View.VISIBLE);
            mTvLoginRegister.setVisibility(View.GONE);
            mLlSignout.setVisibility(View.VISIBLE);
        }
    }

    public void setHeadImg(Bitmap bitmap){
        mHeadImg.setImageBitmap(bitmap);
        File file = UtilBitmap.compressBmpToFile(bitmap, AppData.PATH + "touxiang.jpg", 600);
        modifyHeadImg(file);
    }

    private void modifyHeadImg(File file){
        Map<String, RequestBody> map = new HashMap<>();
        RequestBody requestBody = RequestBody.create(MediaType.parse("image/jpeg"), file);
        map.put("file\"; filename=\"touxiang.jpg", requestBody);
        RequestBody bodyToken = RequestBody.create(MediaType.parse("text/plain"), AppData.token);
        map.put("token", bodyToken);
        RetrofitUtil.getInstance().create(HttpService.class).updUserImg(map).subscribeOn(io()).observeOn(mainThread()).subscribe(new Subscriber<BaseResult>() {
            @Override
            public void onStart() {
                super.onStart();
                mActivity.showProgressDialog();
            }

            @Override
            public void onCompleted() {
                mActivity.removeProgressDialog();
            }

            @Override
            public void onError(Throwable e) {
                mActivity.removeProgressDialog();
                e.printStackTrace();
            }

            @Override
            public void onNext(BaseResult result) {
            }
        });
    }

    private void getInfo(){
        if(!TokenManager.isLogin(mActivity)){
            return;
        }
        RetrofitUtil.getInstance().create(HttpService.class).getUserInfo(AppData.token).subscribeOn(io()).observeOn(mainThread()).subscribe(new BaseSubscriber<InfoResult>((BaseActivity) getActivity()) {

            @Override
            public void onStart() {
                super.onStart();
                mActivity.showProgressDialog();
            }

            @Override
            public void onCompleted() {
                mActivity.removeProgressDialog();
            }

            @Override
            public void onError(Throwable e) {
                mActivity.removeProgressDialog();
                e.printStackTrace();
            }


            @Override
            public void success(InfoResult result) {
                TokenManager.setUserInfo(result.getData());
                AppData.examYN = result.getData().getExamYN();
                AppData.url = result.getData().getFacefirsturl();
                Bitmap bitmap = UtilBitmap.base64ToBitmap(result.getData().getFacefirsturl());
                mHeadImg.setImageBitmap(bitmap);
            }
        });
    }
}
