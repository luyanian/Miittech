package com.miittech.you.net;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.miittech.you.R;
import com.miittech.you.global.HttpUrl;
import com.ryon.mutils.NetworkUtils;
import com.ryon.mutils.ToastUtils;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

/**
 * Created by Administrator on 2017/2/14.
 */

public class ApiServiceManager {
    private static ApiServiceManager sigleInstance = new ApiServiceManager();
    private static ApiService apiService;
    public static ApiServiceManager getInstance(){
        return sigleInstance;
    }
    public synchronized ApiService buildApiService(Context context){
        if(!NetworkUtils.isConnected()){
            ToastUtils.showShort(R.string.msg_net_unavailable);
        }
        if(apiService==null){
            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();
            Retrofit retrofit = new Retrofit.Builder()
                .client(getClient(context))
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addConverterFactory(ScalarsConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())//新的配置
                .baseUrl(HttpUrl.URL)
                .build();
            apiService = retrofit.create(ApiService.class);
        }
        return apiService;
    }
//    public synchronized ApiService buildApiService(Context context, String baseUrl){
//        Gson gson = new GsonBuilder()
//                .setLenient()
//                .create();
//        Retrofit retrofit = new Retrofit.Builder()
//                .client(getClient(context))
//                .addConverterFactory(GsonConverterFactory.create(gson))
//                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())//新的配置
//                .baseUrl(baseUrl)
//                .build();
//
//        return retrofit.create(ApiService.class);
//    }

    /**
     * 获取 OkHttpClient
     *
     * @return OkHttpClient
     */
    private static OkHttpClient getClient(Context context) {
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
            @Override
            public void log(String message) {
                Log.d("retrofit-log",message);
            }
        });
        HttpLoggingInterceptor.Level level= HttpLoggingInterceptor.Level.BODY;
        httpLoggingInterceptor.setLevel(level);
        return new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .cookieJar(new CookieManger(context))
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .addInterceptor(httpLoggingInterceptor)
                .build();
    }
}
