package com.li.education;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.li.education.base.AppData;
import com.li.education.base.BaseActivity;
import com.li.education.main.home.HomeFragment;
import com.li.education.main.mine.LoginActivity;
import com.li.education.main.mine.MineFragment;
import com.li.education.main.study.StudyFragment;
import com.li.education.util.UtilBitmap;
import com.li.education.util.UtilIntent;

import java.io.File;

public class MainActivity extends BaseActivity implements View.OnClickListener {
    private RelativeLayout mRlHome;
    private RelativeLayout mRlStudy;
    private RelativeLayout mRlMine;
    private TextView mTvHome;
    private TextView mTvStudy;
    private TextView mTvMine;

    private HomeFragment mHomeFragment;
    private StudyFragment mStudyFragment;
    private MineFragment mMineFragment;

    private FragmentManager mManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        mManager = getSupportFragmentManager();
        selectTab(0);
    }

    private void initView() {
        mRlHome = (RelativeLayout) findViewById(R.id.rl_home);
        mRlStudy = (RelativeLayout) findViewById(R.id.rl_study);
        mRlMine = (RelativeLayout) findViewById(R.id.rl_mine);
        mRlHome.setOnClickListener(this);
        mRlStudy.setOnClickListener(this);
        mRlMine.setOnClickListener(this);
        mTvHome = (TextView) findViewById(R.id.tv_home);
        mTvStudy = (TextView) findViewById(R.id.tv_study);
        mTvMine = (TextView) findViewById(R.id.tv_mine);

    }


    private void selectTab(int index) {
        FragmentTransaction transaction = mManager.beginTransaction();
        clearState();
        hideFragments(transaction);
        switch (index) {
            case 0:
                mTvHome.setTextColor(0xff000000);
                mTvHome.setCompoundDrawablesWithIntrinsicBounds(0, R.mipmap.main_home_select, 0, 0);
                mRlHome.setEnabled(false);
                mRlStudy.setEnabled(true);
                mRlMine.setEnabled(true);
                if (mHomeFragment == null) {
                    mHomeFragment = new HomeFragment();
                    transaction.add(R.id.frame_layout, mHomeFragment);
                } else {
                    transaction.show(mHomeFragment);
                }
                break;

            case 1:
                mTvStudy.setTextColor(0xff000000);
                mTvStudy.setCompoundDrawablesWithIntrinsicBounds(0, R.mipmap.main_study_select, 0, 0);
                mRlHome.setEnabled(true);
                mRlStudy.setEnabled(false);
                mRlMine.setEnabled(true);
                if (mStudyFragment == null) {
                    mStudyFragment = new StudyFragment();
                    transaction.add(R.id.frame_layout, mStudyFragment);
                } else {
                    transaction.show(mStudyFragment);
                }

                break;

            case 2:
                mTvMine.setTextColor(0xff000000);
                mTvMine.setCompoundDrawablesWithIntrinsicBounds(0, R.mipmap.main_mine_select, 0, 0);
                mRlHome.setEnabled(true);
                mRlStudy.setEnabled(true);
                mRlMine.setEnabled(false);
                if (mMineFragment == null) {
                    mMineFragment = new MineFragment();
                    transaction.add(R.id.frame_layout, mMineFragment);
                } else {
                    transaction.show(mMineFragment);
                }
                break;
        }
        transaction.commit();
    }


    private void clearState() {
        mTvHome.setTextColor(0xffa0a0a0);
        mTvStudy.setTextColor(0xffa0a0a0);
        mTvMine.setTextColor(0xffa0a0a0);
        mTvHome.setCompoundDrawablesWithIntrinsicBounds(0, R.mipmap.main_home_unselect, 0, 0);
        mTvStudy.setCompoundDrawablesWithIntrinsicBounds(0, R.mipmap.main_study_unselect, 0, 0);
        mTvMine.setCompoundDrawablesWithIntrinsicBounds(0, R.mipmap.main_mine_unselect, 0, 0);
    }

    private void hideFragments(FragmentTransaction transaction) {
        if (mHomeFragment != null) {
            transaction.hide(mHomeFragment);
        }
        if (mStudyFragment != null) {
            transaction.hide(mStudyFragment);
        }
        if (mMineFragment != null) {
            transaction.hide(mMineFragment);
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rl_home:
                selectTab(0);
                break;

            case R.id.rl_study:
                if (!TextUtils.isEmpty(AppData.token)) {
                    selectTab(1);
                } else {
                    UtilIntent.intentDIYLeftToRight(MainActivity.this, LoginActivity.class);
                }

                break;

            case R.id.rl_mine:
                selectTab(2);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 0:
                File file = new File(AppData.PATH);
                if(!file.exists()){
                    file.mkdirs();
                }
                File fileImg = new File(AppData.PATH, "touxiang.jpg");
                if(fileImg.exists()) {
//                    startPhotoZoom(Uri.fromFile(new File(AppData.PATH, "touxiang.jpg")));
                    setPicToView(Uri.fromFile(new File(AppData.PATH, "touxiang.jpg")));
                }
                break;
            case 1:
                if(data != null) {
//                    startPhotoZoom(data.getData());
                    setPicToView(data.getData());
                }
                break;
            default:
                break;
        }
    }


    private void setPicToView(Uri uri) {
        ContentResolver resolver = getContentResolver();
        Cursor cursor = resolver.query(uri, null, null, null, null);// 根据Uri从数据库中找
        if (cursor != null) {
            cursor.moveToFirst();// 把游标移动到首位，因为这里的Uri是包含ID的所以是唯一的不需要循环找指向第一个就是了
            String filePath = cursor.getString(cursor.getColumnIndex("_data"));// 获取图片路
            String orientation = cursor.getString(cursor
                    .getColumnIndex("orientation"));// 获取旋转的角度
            cursor.close();
            Bitmap bitmap = BitmapFactory.decodeFile(filePath);//根据Path读取资源图片
            int angle = 0;
            if (orientation != null && !"".equals(orientation)) {
                angle = Integer.parseInt(orientation);
            }
            if (angle != 0) {
                // 下面的方法主要作用是把图片转一个角度，也可以放大缩小等
                Matrix m = new Matrix();
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                m.setRotate(angle); // 旋转angle度
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height,
                        m, true);// 从新生成图片

            }
            if(mMineFragment != null) {
                mMineFragment.setHeadImg(bitmap);
            }
        }else {
            int rotate = UtilBitmap.getBitmapDegree(uri.getPath());
            Bitmap bitmap = BitmapFactory.decodeFile(uri.getPath());//根据Path读取资源图片
            Bitmap bitmap1 = UtilBitmap.rotateBitmapByDegree(bitmap, rotate);
            if(mMineFragment != null) {
                mMineFragment.setHeadImg(bitmap1);
            }

        }
    }
}
