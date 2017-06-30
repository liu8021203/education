package com.li.education.main.mine;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;

import com.li.education.R;
import com.li.education.base.BaseActivity;

/**
 * Created by liu on 2017/6/18.
 */

public class AboutActivity extends BaseActivity{
    private ImageView mIvBack;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        mIvBack = (ImageView) findViewById(R.id.iv_back);
        mIvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }
}
