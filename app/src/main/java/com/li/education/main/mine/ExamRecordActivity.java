package com.li.education.main.mine;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.aspsine.swipetoloadlayout.OnLoadMoreListener;
import com.aspsine.swipetoloadlayout.SwipeToLoadLayout;
import com.li.education.R;
import com.li.education.base.AppData;
import com.li.education.base.BaseActivity;
import com.li.education.base.bean.ExamRecordResult;
import com.li.education.base.bean.HomeResult;
import com.li.education.base.common.BaseSubscriber;
import com.li.education.base.http.HttpService;
import com.li.education.base.http.RetrofitUtil;
import com.li.education.main.home.CustomItemDecoration;
import com.li.education.main.mine.adapter.ExamRecordAdapter;

import rx.Subscriber;

import static rx.android.schedulers.AndroidSchedulers.mainThread;
import static rx.schedulers.Schedulers.io;

/**
 * Created by liu on 2017/6/30.
 */

public class ExamRecordActivity extends BaseActivity implements OnLoadMoreListener{
    private RecyclerView mRecyclerView;
    private SwipeToLoadLayout mSwipeToLoadLayout;
    private ExamRecordAdapter mAdapter;
    private int page = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam_record);
        initView();
        getData("1");
    }

    private void initView() {
        mRecyclerView = (RecyclerView) findViewById(R.id.swipe_target);
        mSwipeToLoadLayout = (SwipeToLoadLayout) findViewById(R.id.swipe_to_load_layout);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        CustomItemDecoration decoration = new CustomItemDecoration(1, 0xffd9e0f2);
        mRecyclerView.addItemDecoration(decoration);
        mRecyclerView.setLayoutManager(manager);
        mSwipeToLoadLayout.setOnLoadMoreListener(this);
    }


    private void getData(String page){
        RetrofitUtil.getInstance().create(HttpService.class).getExamrecordList(AppData.token, page).subscribeOn(io()).observeOn(mainThread()).subscribe(new BaseSubscriber<ExamRecordResult>(ExamRecordActivity.this) {

            @Override
            public void onStart() {
            }

            @Override
            public void onCompleted() {
                mSwipeToLoadLayout.setLoadingMore(false);
            }

            @Override
            public void onError(Throwable e) {
                mSwipeToLoadLayout.setLoadingMore(false);
                e.printStackTrace();
            }

            @Override
            public void onNext(ExamRecordResult result) {
                if(result.isStatus()){
                    if(mAdapter == null){
                        mAdapter = new ExamRecordAdapter(ExamRecordActivity.this);
                        mAdapter.setData(result.getData().getList());
                        mRecyclerView.setAdapter(mAdapter);
                    }else{
                        mAdapter.setData(result.getData().getList());
                        mAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void success(ExamRecordResult result) {

            }
        });
    }

    @Override
    public void onLoadMore() {
        page++;
        getData(String.valueOf(page));
    }
}
