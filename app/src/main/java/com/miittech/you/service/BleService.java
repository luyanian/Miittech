package com.miittech.you.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;
import com.google.gson.Gson;
import com.inuker.bluetooth.library.BluetoothContext;
import com.inuker.bluetooth.library.Constants;
import com.inuker.bluetooth.library.connect.listener.BleConnectStatusListener;
import com.inuker.bluetooth.library.connect.options.BleConnectOptions;
import com.inuker.bluetooth.library.connect.response.BleConnectResponse;
import com.inuker.bluetooth.library.connect.response.BleReadResponse;
import com.inuker.bluetooth.library.connect.response.BleReadRssiResponse;
import com.inuker.bluetooth.library.connect.response.BleWriteResponse;
import com.inuker.bluetooth.library.model.BleGattProfile;
import com.miittech.you.App;
import com.miittech.you.common.BleCommon;
import com.miittech.you.common.Common;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.manager.BLEClientManager;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.FriendsResponse;
import com.ryon.constant.TimeConstants;
import com.ryon.mutils.ConvertUtils;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.TimeUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;

import static com.inuker.bluetooth.library.Constants.REQUEST_SUCCESS;
import static com.inuker.bluetooth.library.Constants.STATUS_DEVICE_CONNECTING;
import static com.miittech.you.common.BleCommon.characteristicUUID;
import static com.miittech.you.common.BleCommon.serviceUUID;
import static com.miittech.you.common.BleCommon.userCharacteristicLogUUID;
import static com.miittech.you.common.BleCommon.userServiceUUID;

public class BleService extends Service {
    public LocationClient mLocationClient = null;
    private MyLocationListener myListener = new MyLocationListener();
    private long lastMillins=0;
    CmdReceiver cmdReceiver;
    private List<String> mMacList = new ArrayList<>();
    private BDLocation lastLocation;

      
    @Override  
    public IBinder onBind(Intent intent) {
        return null;  
    }  
  
    @Override  
    public void onCreate() {
        super.onCreate();
        android.os.Debug.waitForDebugger();
        BluetoothContext.set(getApplicationContext());
        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(myListener);
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setCoorType("bd09ll");
        option.setScanSpan(60000);
        option.setOpenGps(true);
        option.setIgnoreKillProcess(false);
        option.setWifiCacheTimeOut(5*60*1000);
        mLocationClient.setLocOption(option);
        mLocationClient.start();

        cmdReceiver = new CmdReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(IntentExtras.ACTION.ACTION_BLE_COMMAND);
        getApplicationContext().registerReceiver(cmdReceiver, filter);

        LogUtils.d("BleService-OnCreate()");
    }
      

    @Override  
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.d("BleService-onStartCommand()");
        return START_REDELIVER_INTENT;
    }
  
    @Override  
    public void onDestroy() {
        super.onDestroy();
        LogUtils.d("BleService-onDestroy()");
        this.unregisterReceiver(cmdReceiver);
        if(mLocationClient!=null){
            mLocationClient.stop();
        }
    }

    public  void connectDevice(){
        for(final String mac:mMacList){
            int status = BLEClientManager.getClient().getConnectStatus(mac);
            if(status== Constants.STATUS_DEVICE_CONNECTED||status== STATUS_DEVICE_CONNECTING){
                return;
            }else{
                BleConnectOptions options = new BleConnectOptions.Builder()
                        .setConnectRetry(3)   // 连接如果失败重试3次
                        .setConnectTimeout(30000)   // 连接超时30s
                        .setServiceDiscoverRetry(3)  // 发现服务如果失败重试3次
                        .setServiceDiscoverTimeout(20000)  // 发现服务超时20s
                        .build();
                BLEClientManager.getClient().connect(mac,options, new BleConnectResponse() {
                    @Override
                    public void onResponse(int code, BleGattProfile data) {
                        if(code==Constants.REQUEST_SUCCESS) {
                            setWorkMode(mac);
                        }else{
                            if(mMacList.contains(mac)){
                                mMacList.remove(mac);
                            }
                        }
                    }
                });
            }
        }
    }

    class CmdReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            LogUtils.d("CommandReceiver","收到广播："+intent.getAction()+"----->"+intent.getIntExtra("cmd", -1));
            if(intent.getAction().equals(IntentExtras.ACTION.ACTION_BLE_COMMAND)){
                int cmd = intent.getIntExtra("cmd", -1);//获取Extra信息
                switch (cmd) {
                    case IntentExtras.CMD.CMD_DEVICE_CONNECT_BIND:
                        bindDevice(intent.getStringExtra("address"));
                        break;
                    case IntentExtras.CMD.CMD_DEVICE_CONNECT_WORK:
                        connectDevice(intent.getStringExtra("address"));
                        break;
                    case IntentExtras.CMD.CMD_DEVICE_ALERT_START:
                        startAlert(intent.getStringExtra("address"));
                        break;
                    case IntentExtras.CMD.CMD_DEVICE_ALERT_STOP:
                        stopAlert(intent.getStringExtra("address"));
                        break;
                    case IntentExtras.CMD.CMD_DEVICE_UNBIND:
                        unbindDevice(intent.getStringExtra("address"));
                        break;
                }
            }
        }
    }

    public  void connectDevice(final String mac){
        if(mMacList.contains(mac)){
           return;
        }
        mMacList.add(mac);
        BleConnectOptions options = new BleConnectOptions.Builder()
                .setConnectRetry(3)   // 连接如果失败重试3次
                .setConnectTimeout(30000)   // 连接超时30s
                .setServiceDiscoverRetry(3)  // 发现服务如果失败重试3次
                .setServiceDiscoverTimeout(20000)  // 发现服务超时20s
                .build();
        BLEClientManager.getClient().refreshCache(mac);
        BLEClientManager.getClient().connect(mac,options, new BleConnectResponse() {
            @Override
            public void onResponse(int code, BleGattProfile data) {
                Intent intent = new Intent();
                intent.putExtra("action",IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                if(code==Constants.REQUEST_SUCCESS){
                    LogUtils.d("bleResponse","贴片连接成功----->"+mac);
                    intent.putExtra("ret",IntentExtras.RET.RET_DEVICE_CONNECT_SUCCESS);
                    sendBroadcast(intent);
                    setWorkMode(mac);
                }else{
                    if(mMacList.contains(mac)){
                        mMacList.remove(mac);
                    }
                    LogUtils.d("bleResponse","贴片连接失败----->"+mac);
                    intent.putExtra("ret",IntentExtras.RET.RET_DEVICE_CONNECT_FAILED);
                    sendBroadcast(intent);
                }
            }
        });

        BLEClientManager.getClient().registerConnectStatusListener(mac, new BleConnectStatusListener() {
            @Override
            public void onConnectStatusChanged(String mac, int status) {
                if (status == Constants.STATUS_CONNECTED) {
                    if(!mMacList.contains(mac)){
                        mMacList.add(mac);
                    }
                } else if (status == Constants.STATUS_DISCONNECTED) {
                    if(mMacList.contains(mac)){
                        mMacList.remove(mac);
                    }
                }
            }
        });

    }

    public  void bindDevice(final String mac){
        if(mMacList.contains(mac)){
            return;
        }
        mMacList.add(mac);

        BLEClientManager.getClient().connect(mac, new BleConnectResponse() {
            @Override
            public void onResponse(int code, BleGattProfile data) {
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                if(code==Constants.REQUEST_SUCCESS) {
                    LogUtils.d("bleResponse","贴片连接成功----->"+mac);
                    intent.putExtra("ret", IntentExtras.RET.RET_DEVICE_CONNECT_SUCCESS);
                    sendBroadcast(intent);
                    setBindMode(mac);
                }else{
                    if(mMacList.contains(mac)){
                        mMacList.remove(mac);
                    }
                    LogUtils.d("bleResponse","贴片连接失败----->"+mac);
                    intent.putExtra("ret", IntentExtras.RET.RET_DEVICE_CONNECT_FAILED);
                    sendBroadcast(intent);
                }
            }
        });
    }

    private void unbindDevice(final String address) {
        if(BLEClientManager.getClient().getConnectStatus(address)==Constants.STATUS_DEVICE_DISCONNECTED){
            connectDevice(address);
            return;
        }
        if(BLEClientManager.getClient().getConnectStatus(address)== Constants.STATUS_DEVICE_CONNECTED){
            byte[] dataWork = Common.formatBleMsg(Params.BLEMODE.MODE_UNBIND, App.getInstance().getUserId());
            BLEClientManager.getClient().write(address, userServiceUUID, userCharacteristicLogUUID, dataWork, new BleWriteResponse() {
                @Override
                public void onResponse(int code) {
                    Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                    if (code == REQUEST_SUCCESS) {
                        if(mMacList.contains(address)){
                            mMacList.remove(address);
                        }
                        LogUtils.d("bleResponse","贴片绑定成功----->"+address);
                        if(mMacList.contains(address)){
                            mMacList.remove(address);
                        }
                        intent.putExtra("ret", IntentExtras.RET.RET_DEVICE_UNBIND_SUCCESS);
                        sendBroadcast(intent);
                    }else{
                        LogUtils.d("bleResponse","贴片绑定失败----->"+address);
                    }
                }
            });
        }

    }

    private void startAlert(final String address) {
        byte[] options = new byte[]{0x02};
        int status = BLEClientManager.getClient().getConnectStatus(address);
        if(status==Constants.STATUS_DEVICE_DISCONNECTED){
            connectDevice(address);
            return;
        }
        if(status==Constants.STATUS_DEVICE_CONNECTED){
            BLEClientManager.getClient().write(address, serviceUUID, characteristicUUID, options, new BleWriteResponse() {
                @Override
                public void onResponse(int code) {
                    Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                    if(code==Constants.REQUEST_SUCCESS) {
                        LogUtils.d("bleResponse","贴片开始报警----->"+address);
                        intent.putExtra("ret", IntentExtras.RET.RET_DEVICE_CONNECT_ALERT_START_SUCCESS);
                        sendBroadcast(intent);
                    }
                }
            });
        }

    }
    private void stopAlert(final String address) {
            byte[] options = new byte[]{0x00};
            int status = BLEClientManager.getClient().getConnectStatus(address);
            if(status==Constants.STATUS_DEVICE_DISCONNECTED){
                connectDevice(address);
                return;
            }
            if(status==Constants.STATUS_DEVICE_CONNECTED){
                BLEClientManager.getClient().write(address, serviceUUID, characteristicUUID, options, new BleWriteResponse() {
                    @Override
                    public void onResponse(int code) {
                        Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                        if(code==Constants.REQUEST_SUCCESS) {
                            LogUtils.d("bleResponse","贴片开始结束----->"+address);
                            intent.putExtra("ret", IntentExtras.RET.RET_DEVICE_CONNECT_ALERT_STOP_SUCCESS);
                            sendBroadcast(intent);
                        }
                    }
                });
            }

        }

    public void setWorkMode(final String mac){
        byte[] dataWork = Common.formatBleMsg(Params.BLEMODE.MODE_WORK, App.getInstance().getUserId());
        BLEClientManager.getClient().write(mac, userServiceUUID, userCharacteristicLogUUID, dataWork, new BleWriteResponse() {
            @Override
            public void onResponse(int code) {
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                if(code==Constants.REQUEST_SUCCESS) {
                    LogUtils.d("bleResponse","贴片设置工作模式成功----->"+mac);
                    intent.putExtra("ret", IntentExtras.RET.RET_DEVICE_CONNECT_WORK_SUCCESS);
                    sendBroadcast(intent);
                }else{
                    if(mMacList.contains(mac)){
                        mMacList.remove(mac);
                    }
                    LogUtils.d("bleResponse","贴片设置工作模式失败----->"+mac);
                    intent.putExtra("ret", IntentExtras.RET.RET_DEVICE_CONNECT_WORK_FAILED);
                    sendBroadcast(intent);
                }
            }
        });
    }
    public void setBindMode(final String mac){
        byte[] bind = Common.formatBleMsg(Params.BLEMODE.MODE_BIND,App.getInstance().getUserId());
        BLEClientManager.getClient().write(mac, BleCommon.userServiceUUID, BleCommon.userCharacteristicLogUUID, bind, new BleWriteResponse() {
            @Override
            public void onResponse(int code) {
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                if(code==Constants.REQUEST_SUCCESS) {
                    LogUtils.d("bleResponse","贴片设置绑定模式成功----->"+mac);
                    intent.putExtra("ret", IntentExtras.RET.RET_DEVICE_CONNECT_BIND_SUCCESS);
                    intent.putExtra("address", mac);
                    sendBroadcast(intent);
                }else{
                    if(mMacList.contains(mac)){
                        mMacList.remove(mac);
                    }
                    LogUtils.d("bleResponse","贴片设置绑定模式失败----->"+mac);
                    intent.putExtra("ret", IntentExtras.RET.RET_DEVICE_CONNECT_BIND_FAIL);
                    sendBroadcast(intent);
                }
            }
        });
    }

    public class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location){
            LogUtils.d("bleResponse","接收到定位信息----->"+location.getLatitude()+","+location.getLongitude());
            LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
            long curMillis = TimeUtils.getNowDate().getTime();
            if(lastLocation!=null) {
                LatLng last = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                if(DistanceUtil. getDistance(last, current)>200||TimeUtils.getTimeSpan(curMillis,lastMillins, TimeConstants.MIN)>30){
                    reportUserLocation(curMillis,location);
                }
            }else{
                reportUserLocation(curMillis,location);
            }
            lastLocation = location;
        }
    }

    private void reportUserLocation(final long millis, final BDLocation location) {
        final Map user_loc = new HashMap();
        user_loc.put("lat", location.getLatitude());
        user_loc.put("lng", location.getLongitude());
        user_loc.put("addr", Common.encodeBase64(location.getAddrStr()));
        final List<Map> devlist = new ArrayList<>();
        for (final String mac : mMacList) {
            final Map devItem = new HashMap();
            devItem.put("devid", Common.formatMac2DevId(mac));
            BLEClientManager.getClient().read(mac, BleCommon.batServiceUUID, BleCommon.batCharacteristicUUID, new BleReadResponse() {
                @Override
                public void onResponse(int code, byte[] data) {
                    if (code == Constants.REQUEST_SUCCESS) {
                        devItem.put("devbattery", ConvertUtils.bytes2HexString(data));
                        BLEClientManager.getClient().readRssi(mac, new BleReadRssiResponse() {
                            @Override
                            public void onResponse(int code, Integer data) {
                                if (code == Constants.REQUEST_SUCCESS) {
                                    if (data < -85) {
                                        devItem.put("devposstate", 3);
                                    }
                                    if (data > -85 && data < -70) {
                                        devItem.put("devposstate", 2);
                                    }
                                    if (data > -70) {
                                        devItem.put("devposstate", 1);
                                    }
                                    devItem.put("devstate", 1);
                                    devItem.put("usedstate", 1);
                                    devItem.put("bindstate", 1);
                                    devlist.add(devItem);

                                    Map repdata = new HashMap();
                                    repdata.put("reptime", TimeUtils.millis2String(millis, new SimpleDateFormat("yyyymmddhhmmss")));
                                    repdata.put("user_loc", user_loc);
                                    repdata.put("devlist", devlist);
                                    Map param = new HashMap();
                                    param.put("method", 1);
                                    param.put("repdata", repdata);
                                    String json = new Gson().toJson(param);
                                    PubParam pubParam = new PubParam(App.getInstance().getUserId());
                                    String sign_unSha1 = pubParam.toValueString() + json + App.getInstance().getTocken();
                                    LogUtils.d("sign_unsha1", sign_unSha1);
                                    String sign = EncryptUtils.encryptSHA1ToString(sign_unSha1).toLowerCase();
                                    LogUtils.d("sign_sha1", sign);
                                    String path = HttpUrl.Api + "userreport/" + pubParam.toUrlParam(sign);
                                    final RequestBody requestBody = RequestBody.create(MediaType.parse(HttpUrl.MediaType_Json), json);

                                    ApiServiceManager.getInstance().buildApiService(App.getInstance().getApplicationContext()).postToGetFriendList(path, requestBody)
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe(new Consumer<FriendsResponse>() {
                                                @Override
                                                public void accept(FriendsResponse response) throws Exception {
                                                    if (response.isSuccessful()) {

                                                    }
                                                }
                                            }, new Consumer<Throwable>() {
                                                @Override
                                                public void accept(Throwable throwable) throws Exception {
                                                    throwable.printStackTrace();
                                                }
                                            });
                                }
                            }
                        });
                    }
                }
            });
        }
    }
}