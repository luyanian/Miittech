package com.miittech.you;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import com.baidu.mapapi.SDKInitializer;
import com.inuker.bluetooth.library.Code;
import com.inuker.bluetooth.library.Constants;
import com.inuker.bluetooth.library.connect.listener.BleConnectStatusListener;
import com.inuker.bluetooth.library.connect.options.BleConnectOptions;
import com.inuker.bluetooth.library.connect.response.BleConnectResponse;
import com.inuker.bluetooth.library.connect.response.BleWriteResponse;
import com.inuker.bluetooth.library.model.BleGattProfile;
import com.miittech.you.ble.ClientManager;
import com.miittech.you.common.BleCommon;
import com.miittech.you.common.Common;
import com.miittech.you.global.Params;
import com.miittech.you.global.SPConst;
import com.miittech.you.receiver.BluetoothReceiver;
import com.mob.MobApplication;
import com.ryon.mutils.ActivityPools;
import com.ryon.mutils.SPUtils;
import com.ryon.mutils.StringUtils;
import com.ryon.mutils.Utils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.inuker.bluetooth.library.Constants.REQUEST_SUCCESS;
import static com.inuker.bluetooth.library.Constants.STATUS_CONNECTED;
import static com.inuker.bluetooth.library.Constants.STATUS_DISCONNECTED;
import static com.miittech.you.common.BleCommon.characteristicUUID;
import static com.miittech.you.common.BleCommon.serviceUUID;
import static com.miittech.you.common.BleCommon.userCharacteristicLogUUID;
import static com.miittech.you.common.BleCommon.userServiceUUID;

/**
 * Created by Administrator on 2017/9/7.
 */

public class App extends MobApplication {
    private static App instance;
    private List<String> mMacList = new ArrayList<>();
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        SDKInitializer.initialize(getApplicationContext());
        Utils.init(getApplicationContext());
        registerActivityListener();
        registReciver();
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

    public void addMac(String mac){
        if(StringUtils.isEmpty(mac)){
            return;
        }
        if(!mMacList.contains(mac)) {
            mMacList.add(mac);
            connectDevice(mac);
        }
    }
    public void addMacSetWork(String mac) {
        if(StringUtils.isEmpty(mac)){
            return;
        }
        if(!mMacList.contains(mac)) {
            mMacList.add(mac);
            setWorkMode(mac);
        }
    }

    public void delMac(String mac){
        if(ClientManager.getClient().getConnectStatus(mac)==Constants.STATUS_DEVICE_CONNECTED){
            byte[] dataWork = Common.formatBleMsg(Params.BLEMODE.MODE_UNBIND, App.getInstance().getUserId());
            ClientManager.getClient().write(mac, userServiceUUID, userCharacteristicLogUUID, dataWork, new BleWriteResponse() {
                @Override
                public void onResponse(int code) {
                    if (code == REQUEST_SUCCESS) {

                    }
                }
            });
        }
        if(mMacList.contains(mac)){
            mMacList.remove(mac);
        }
    }

    private void connectDevice(final String mac) {
        BleConnectOptions options = new BleConnectOptions.Builder()
                .setConnectRetry(3)   // 连接如果失败重试3次
                .setConnectTimeout(30000)   // 连接超时30s
                .setServiceDiscoverRetry(3)  // 发现服务如果失败重试3次
                .setServiceDiscoverTimeout(20000)  // 发现服务超时20s
                .build();
        ClientManager.getClient().connect(mac, options, new BleConnectResponse() {
            @Override
            public void onResponse(int code, BleGattProfile data) {
                if(code== Constants.CODE_CONNECT){

                }
            }
        });

        BleConnectStatusListener mBleConnectStatusListener = new BleConnectStatusListener() {

            @Override
            public void onConnectStatusChanged(String mac, int status) {
                if (status == STATUS_CONNECTED) {
                    setWorkMode(mac);
                } else if (status == STATUS_DISCONNECTED) {

                }
            }
        };
        ClientManager.getClient().registerConnectStatusListener(mac, mBleConnectStatusListener);
    }
    public void setWorkMode(String mac){
        if(ClientManager.getClient().getConnectStatus(mac)!=Constants.STATUS_DEVICE_CONNECTED){
            return;
        }
        byte[] dataWork = Common.formatBleMsg(Params.BLEMODE.MODE_WORK, App.getInstance().getUserId());
        ClientManager.getClient().write(mac, userServiceUUID, userCharacteristicLogUUID, dataWork, new BleWriteResponse() {
            @Override
            public void onResponse(int code) {
                if (code == REQUEST_SUCCESS) {

                }
            }
        });
    }
    public void setBindMode(String mac,BleWriteResponse response){
        byte[] bind = Common.formatBleMsg(Params.BLEMODE.MODE_BIND,App.getInstance().getUserId());
        ClientManager.getClient().write(mac, BleCommon.userServiceUUID, BleCommon.userCharacteristicLogUUID, bind, response);
    }

    public void doFindOrBell(String mac,byte[] options,BleWriteResponse response) {
        if(ClientManager.getClient().getConnectStatus(mac)==Constants.STATUS_DEVICE_CONNECTED){
            ClientManager.getClient().write(mac, serviceUUID, characteristicUUID, options, response);
        }
    }


    public void connectMac(String address,BleConnectResponse response) {
        BleConnectOptions options = new BleConnectOptions.Builder()
                .setConnectRetry(3)   // 连接如果失败重试3次
                .setConnectTimeout(30000)   // 连接超时30s
                .setServiceDiscoverRetry(3)  // 发现服务如果失败重试3次
                .setServiceDiscoverTimeout(20000)  // 发现服务超时20s
                .build();
        ClientManager.getClient().connect(address, options,response);
    }
}
