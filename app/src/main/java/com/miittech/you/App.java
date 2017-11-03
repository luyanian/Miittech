package com.miittech.you;

import android.*;
import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;

import com.baidu.mapapi.SDKInitializer;
import com.luck.picture.lib.permissions.RxPermissions;
import com.miittech.you.ble.common.BluetoothDeviceManager;
import com.miittech.you.receiver.BluetoothReceiver;
import com.mob.MobApplication;
import com.ryon.mutils.ActivityPools;
import com.ryon.mutils.SPUtils;
import com.ryon.mutils.Utils;
import com.vise.baseble.ViseBle;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Administrator on 2017/9/7.
 */

public class App extends MobApplication {
    private static App instance;
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        SDKInitializer.initialize(getApplicationContext());
        Utils.init(getApplicationContext());
        registerActivityListener();
        registReciver();
        BluetoothDeviceManager.getInstance().init(this);
        ViseBle.getInstance().disconnect();
        ViseBle.getInstance().clear();
    }


    public static Application getInstance() {
        return instance;
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

    public void registReciver(){
        BluetoothReceiver bluetoothReceiver = new BluetoothReceiver();
        IntentFilter intent = new IntentFilter();
        intent.addAction (BluetoothDevice.ACTION_PAIRING_REQUEST);
        intent.addAction(BluetoothDevice.ACTION_FOUND);
        intent.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intent.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        intent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intent.setPriority(Integer.MAX_VALUE);
        registerReceiver(bluetoothReceiver, intent);
    }
}
