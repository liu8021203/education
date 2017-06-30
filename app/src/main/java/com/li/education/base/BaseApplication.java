package com.li.education.base;

import android.app.Application;

import com.li.education.util.UtilSPutil;

/**
 * Created by liu on 2017/6/20.
 */

public class BaseApplication extends Application{

    @Override
    public void onCreate() {
        super.onCreate();
        AppData.token = UtilSPutil.getInstance(this).getString("token", "");
    }
}
