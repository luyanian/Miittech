package com.miittech.you;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;

import com.baidu.mapapi.SDKInitializer;
import com.mob.MobApplication;
import com.ryon.mutils.ActivityPools;
import com.ryon.mutils.SPUtils;
import com.ryon.mutils.Utils;

/**
 * Created by Administrator on 2017/9/7.
 */

public class App extends MobApplication {
    @Override
    public void onCreate() {
        super.onCreate();
        SDKInitializer.initialize(getApplicationContext());
        Utils.init(getApplicationContext());
        registerActivityListener();
    }

    public static final String SESSION="session";
    public static final String TOCKEN="tocken";
    public static final String USERID="userId";
    public static final String UNAME="uname";
    public static String getTocken(){
        return SPUtils.getInstance(SESSION).getString(TOCKEN);
    }
    public static String getUserId(){
        return SPUtils.getInstance(SESSION).getString(USERID);
    }
    public static String getUserName(){
        return SPUtils.getInstance(SESSION).getString(UNAME);
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

}
