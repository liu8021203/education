package com.li.education.main.mine;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.aspsine.swipetoloadlayout.OnLoadMoreListener;
import com.aspsine.swipetoloadlayout.OnRefreshListener;
import com.aspsine.swipetoloadlayout.SwipeToLoadLayout;
import com.li.education.R;
import com.li.education.base.AppData;
import com.li.education.base.BaseActivity;
import com.li.education.base.bean.BaseResult;
import com.li.education.base.bean.HomeResult;
import com.li.education.base.http.HttpService;
import com.li.education.base.http.RetrofitUtil;
import com.li.education.main.home.CustomItemDecoration;
import com.li.education.main.mine.adapter.StudyRecordAdapter;

import rx.Subscriber;

import static rx.android.schedulers.AndroidSchedulers.mainThread;
import static rx.schedulers.Schedulers.io;

/**
 * Created by liu on 2017/6/19.
 */

public class StudyRecordActivity extends BaseActivity implements View.OnClickListener, OnRefreshListener, OnLoadMoreListener{
    private ImageView mIvBack;
    private TextView mTvTime;
    private RecyclerView mRecyclerView;
    private StudyRecordAdapter mAdapter;
    private SwipeToLoadLayout mSwipeToLoadLayout;
    private int currIndex = 1;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_record);
        initView();
        getData(String.valueOf(currIndex));
    }

    private void initView() {
        mIvBack = (ImageView) findViewById(R.id.iv_back);
        mIvBack.setOnClickListener(this);
        mTvTime = (TextView) findViewById(R.id.tv_time);
        mRecyclerView = (RecyclerView) findViewById(R.id.swipe_target);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(manager);
        mRecyclerView.addItemDecoration(new CustomItemDecoration(1, 0xffe9edf8));
        mAdapter = new StudyRecordAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mSwipeToLoadLayout = (SwipeToLoadLayout) findViewById(R.id.swipe_to_load_layout);
        mSwipeToLoadLayout.setOnRefreshListener(this);
        mSwipeToLoadLayout.setOnLoadMoreListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.iv_back:
                onBackPressed();
                break;
        }
    }


    private void getData(String pagenum){
        RetrofitUtil.getInstance().create(HttpService.class).getLearnrecordList(AppData.token, pagenum).subscribeOn(io()).observeOn(mainThread()).subscribe(new Subscriber<BaseResult>() {

            @Override
            public void onStart() {
                super.onStart();
            }

            @Override
            public void onCompleted() {
                mSwipeToLoadLayout.setRefreshing(false);
            }

            @Override
            public void onError(Throwable e) {
                mSwipeToLoadLayout.setRefreshing(false);
//                mSwipeToLoadLayout.isLoadingMore()
                e.printStackTrace();
            }

            @Override
            public void onNext(BaseResult result) {
                if(result.isStatus()){
                }
            }
        });
    }



    @Override
    public void onLoadMore() {

    }

    @Override
    public void onRefresh() {

    }
}
