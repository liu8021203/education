package com.li.education.main.mine;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.li.education.R;
import com.li.education.base.BaseActivity;

/**
 * Created by liu on 2017/6/19.
 */

public class StudyRecordDetailsActivity extends BaseActivity{
    private ImageView mIvBack;
    private TextView mTvName;
    private TextView mTvTimeNum;
    private TextView mTvStart;
    private TextView mTvEnd;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_details);
        initView();
    }

    private void initView() {
        mIvBack = (ImageView) findViewById(R.id.iv_back);
        mIvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        mTvName = (TextView) findViewById(R.id.tv_name);
        mTvTimeNum = (TextView) findViewById(R.id.tv_time);
        mTvStart = (TextView) findViewById(R.id.tv_start);
        mTvEnd = (TextView) findViewById(R.id.tv_end);
    }
}
