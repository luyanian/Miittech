package com.miittech.you.ble;

import com.inuker.bluetooth.library.BluetoothClient;
import com.inuker.bluetooth.library.Constants;
import com.inuker.bluetooth.library.connect.listener.BleConnectStatusListener;
import com.inuker.bluetooth.library.connect.options.BleConnectOptions;
import com.inuker.bluetooth.library.connect.response.BleConnectResponse;
import com.inuker.bluetooth.library.connect.response.BleReadResponse;
import com.inuker.bluetooth.library.connect.response.BleReadRssiResponse;
import com.inuker.bluetooth.library.connect.response.BleWriteResponse;
import com.inuker.bluetooth.library.model.BleGattProfile;
import com.inuker.bluetooth.library.search.SearchRequest;
import com.inuker.bluetooth.library.search.response.SearchResponse;
import com.miittech.you.App;
import com.miittech.you.common.BleCommon;
import com.miittech.you.common.Common;
import com.miittech.you.global.Params;
import com.ryon.mutils.StringUtils;

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

    private BluetoothClient getClient() {
        if (mClient == null) {
            synchronized (ClientManager.class) {
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

    private boolean isConnect(String mac){
        if(StringUtils.isEmpty(mac)){
            return false;
        }
        if(!mMacList.contains(mac)) {
            mMacList.add(mac);
            return true;
        }
        return false;
    }

    public void delDevice(String mac){
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
    public void connectDevice(String address,BleConnectResponse response) {
        int status = getClient().getConnectStatus(address);
        if(status==Constants.STATUS_DEVICE_CONNECTED||status==Constants.STATUS_DEVICE_CONNECTING){
            return;
        }
        BleConnectOptions options = new BleConnectOptions.Builder()
                .setConnectRetry(3)   // 连接如果失败重试3次
                .setConnectTimeout(30000)   // 连接超时30s
                .setServiceDiscoverRetry(3)  // 发现服务如果失败重试3次
                .setServiceDiscoverTimeout(20000)  // 发现服务超时20s
                .build();
        getClient().connect(address, options,response);
    }
    public void connectDevice(final String mac) {
        if(!isConnect(mac)){
            return;
        }
        int status = getClient().getConnectStatus(mac);
        if(status==Constants.STATUS_DEVICE_CONNECTED||status==Constants.STATUS_DEVICE_CONNECTING){
            return;
        }
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
        getClient().registerConnectStatusListener(mac, new BleConnectStatusListener() {
            @Override
            public void onConnectStatusChanged(String mac, int status) {
                if (status == STATUS_CONNECTED) {
                    setWorkMode(mac);
                } else if (status == STATUS_DISCONNECTED) {

                }
            }
        });
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
        if(getClient().getConnectStatus(mac)!=Constants.STATUS_DEVICE_CONNECTED){
            return;
        }
        byte[] bind = Common.formatBleMsg(Params.BLEMODE.MODE_BIND,App.getInstance().getUserId());
        getClient().write(mac, BleCommon.userServiceUUID, BleCommon.userCharacteristicLogUUID, bind, response);
    }

    public void doFindOrBell(String mac,byte[] options,BleWriteResponse response) {
        if(getClient().getConnectStatus(mac)!=Constants.STATUS_DEVICE_CONNECTED){
            connectDevice(mac);
            return;
        }
        if(getClient().getConnectStatus(mac)==Constants.STATUS_DEVICE_CONNECTED){
            getClient().write(mac, serviceUUID, characteristicUUID, options, response);
        }
    }

    public void search(SearchRequest request, SearchResponse searchResponse) {
        getClient().search(request,searchResponse);
    }

    public void closeBluetooth() {
        getClient().closeBluetooth();
    }

    public void readRssi(String mac, BleReadRssiResponse bleReadRssiResponse) {
        if(getClient().getConnectStatus(mac)!=Constants.STATUS_DEVICE_CONNECTED){
            connectDevice(mac);
            return;
        }
        getClient().readRssi(mac,bleReadRssiResponse);
    }

    public void read(String mac, UUID batServiceUUID, UUID batCharacteristicUUID, BleReadResponse bleReadResponse) {
        if(getClient().getConnectStatus(mac)!=Constants.STATUS_DEVICE_CONNECTED){
            connectDevice(mac);
            return;
        }
        getClient().read(mac,batServiceUUID,batCharacteristicUUID,bleReadResponse);
    }
}