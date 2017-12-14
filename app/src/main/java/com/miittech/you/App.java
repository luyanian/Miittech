package com.miittech.you;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.multidex.MultiDex;

import com.baidu.mapapi.SDKInitializer;
import com.miittech.you.common.BingGoPlayUtils;
import com.miittech.you.common.SoundPlayUtils;
import com.miittech.you.global.SPConst;
import com.miittech.you.service.BleService;
import com.mob.MobApplication;
import com.ryon.mutils.ActivityPools;
import com.ryon.mutils.SPUtils;
import com.ryon.mutils.Utils;

import cn.jpush.android.api.JPushInterface;

/**
 * Created by Administrator on 2017/9/7.
 */

public class  App extends MobApplication {
    private static App instance;
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        SDKInitializer.initialize(getApplicationContext());
        JPushInterface.setDebugMode(true);
        JPushInterface.init(getApplicationContext());
        Utils.init(getApplicationContext());
        registerActivityListener();
        startService(new Intent(this,BleService.class));
        SoundPlayUtils.init(getApplicationContext());
        BingGoPlayUtils.init(App.getInstance());
    }


    public static App getInstance() {
        return instance;
    }





    private void registerActivityListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
                @Override
                public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                    /**
                     *  监听到 Activity创建事件 将该 Activity 加入list
                     */
                    ActivityPools.pushActivity(activity);

                }

                @Override
                public void onActivityStarted(Activity activity) {

                }

                @Override
                public void onActivityResumed(Activity activity) {

                }

                @Override
                public void onActivityPaused(Activity activity) {

                }

                @Override
                public void onActivityStopped(Activity activity) {

                }

                @Override
                public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

                }

                @Override
                public void onActivityDestroyed(Activity activity) {
                    ActivityPools.popActivity(activity);

                }
            });
        }
    }
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this) ;
    }
}
