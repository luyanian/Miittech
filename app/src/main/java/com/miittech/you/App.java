package com.miittech.you;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import com.baidu.mapapi.SDKInitializer;
import com.miittech.you.global.SPConst;
import com.miittech.you.receiver.BluetoothReceiver;
import com.miittech.you.service.BleOptionstService;
import com.miittech.you.service.BluetoothService;
import com.miittech.you.service.ReportService;
import com.mob.MobApplication;
import com.ryon.mutils.ActivityPools;
import com.ryon.mutils.SPUtils;
import com.ryon.mutils.Utils;

import cn.jpush.android.api.JPushInterface;

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
        JPushInterface.setDebugMode(true);
        JPushInterface.init(getApplicationContext());
        Utils.init(getApplicationContext());
        registerActivityListener();
        registReciver();
//        startService(new Intent(this, BluetoothService.class));
//        startService(new Intent(this, com.inuker.bluetooth.library.BluetoothService.class));
        startService(new Intent(this, BleOptionstService.class));
        startService(new Intent(this, ReportService.class));
    }


    public static App getInstance() {
        return instance;
    }


    public String getTocken(){
        return SPUtils.getInstance(SPConst.USER.SP_NAME).getString(SPConst.USER.KEY_TOCKEN);
    }
    public String getUserId(){
        return SPUtils.getInstance(SPConst.USER.SP_NAME).getString(SPConst.USER.KEY_USERID);
    }
    public String getUserName(){
        return SPUtils.getInstance(SPConst.USER.SP_NAME).getString(SPConst.USER.KEY_UNAME);
    }
    public int getAlerStatus(){
        return SPUtils.getInstance(SPConst.ALET_STATUE.SP_NAME).getInt(SPConst.ALET_STATUE.KEY_STATUS,SPConst.ALET_STATUE.STATUS_UNBELL);
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
