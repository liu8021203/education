package com.li.education.base.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by liu on 2017/6/7.
 */

public class RetrofitUtil {
    public static Retrofit retrofit = null;
    public static Retrofit faceRetrofit = null;

    public static Retrofit getInstance() {
        if (retrofit == null) {
//            Gson gson = new GsonBuilder()
//                    .setDateFormat("yyyy-MM-dd")
//                    .create();
            retrofit = new Retrofit.Builder().baseUrl("http://59.110.242.72:8080/yitongeduapp/").
                    addConverterFactory(GsonConverterFactory.create()).
                    addCallAdapterFactory(RxJavaCallAdapterFactory.create()).build();
        }
        return retrofit;
    }


    public static Retrofit getFaceInstance() {
        if (faceRetrofit == null) {
            faceRetrofit = new Retrofit.Builder().baseUrl("http://60.222.232.22:8090/").
                    addConverterFactory(GsonConverterFactory.create()).
                    addCallAdapterFactory(RxJavaCallAdapterFactory.create()).build();
        }
        return faceRetrofit;
    }
}
