package com.miittech.you.ble;

import com.inuker.bluetooth.library.BluetoothClient;
import com.inuker.bluetooth.library.Constants;
import com.inuker.bluetooth.library.connect.listener.BleConnectStatusListener;
import com.inuker.bluetooth.library.connect.options.BleConnectOptions;
import com.inuker.bluetooth.library.connect.response.BleConnectResponse;
import com.inuker.bluetooth.library.connect.response.BleWriteResponse;
import com.inuker.bluetooth.library.model.BleGattProfile;
import com.miittech.you.App;
import com.miittech.you.common.BleCommon;
import com.miittech.you.common.Common;
import com.miittech.you.global.Params;
import com.ryon.mutils.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static com.inuker.bluetooth.library.Constants.REQUEST_SUCCESS;
import static com.inuker.bluetooth.library.Constants.STATUS_CONNECTED;
import static com.inuker.bluetooth.library.Constants.STATUS_DISCONNECTED;
import static com.miittech.you.common.BleCommon.characteristicUUID;
import static com.miittech.you.common.BleCommon.serviceUUID;
import static com.miittech.you.common.BleCommon.userCharacteristicLogUUID;
import static com.miittech.you.common.BleCommon.userServiceUUID;

public class ClientManager {
    private static ClientManager clientManager;
    private static BluetoothClient mClient;
    private List<String> mMacList = new ArrayList<>();

    public static ClientManager getInstance(){
        if(clientManager==null){
            synchronized (ClientManager.class){
                clientManager = new ClientManager();
            }
        }
        return clientManager;
    }

    public BluetoothClient getClient() {
        if (mClient == null) {
            synchronized (BluetoothClient.class) {
                if (mClient == null) {
                    mClient = new BluetoothClient(App.getInstance());
                }
            }
        }
        return mClient;
    }

    public List<String> getMacList(){
        return mMacList;
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
        if(getClient().getConnectStatus(mac)== Constants.STATUS_DEVICE_CONNECTED){
            byte[] dataWork = Common.formatBleMsg(Params.BLEMODE.MODE_UNBIND, App.getInstance().getUserId());
            getClient().write(mac, userServiceUUID, userCharacteristicLogUUID, dataWork, new BleWriteResponse() {
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
        getClient().connect(mac, options, new BleConnectResponse() {
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
        getClient().registerConnectStatusListener(mac, mBleConnectStatusListener);
    }
    public void setWorkMode(String mac){
        if(getClient().getConnectStatus(mac)!=Constants.STATUS_DEVICE_CONNECTED){
            return;
        }
        byte[] dataWork = Common.formatBleMsg(Params.BLEMODE.MODE_WORK, App.getInstance().getUserId());
        getClient().write(mac, userServiceUUID, userCharacteristicLogUUID, dataWork, new BleWriteResponse() {
            @Override
            public void onResponse(int code) {
                if (code == REQUEST_SUCCESS) {

                }
            }
        });
    }
    public void setBindMode(String mac,BleWriteResponse response){
        byte[] bind = Common.formatBleMsg(Params.BLEMODE.MODE_BIND,App.getInstance().getUserId());
        getClient().write(mac, BleCommon.userServiceUUID, BleCommon.userCharacteristicLogUUID, bind, response);
    }

    public void doFindOrBell(String mac,byte[] options,BleWriteResponse response) {
        if(getClient().getConnectStatus(mac)==Constants.STATUS_DEVICE_CONNECTED){
            getClient().write(mac, serviceUUID, characteristicUUID, options, response);
        }
    }


    public void connectMac(String address,BleConnectResponse response) {
        BleConnectOptions options = new BleConnectOptions.Builder()
                .setConnectRetry(3)   // 连接如果失败重试3次
                .setConnectTimeout(30000)   // 连接超时30s
                .setServiceDiscoverRetry(3)  // 发现服务如果失败重试3次
                .setServiceDiscoverTimeout(20000)  // 发现服务超时20s
                .build();
        getClient().connect(address, options,response);
    }
}