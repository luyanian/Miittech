package com.miittech.you.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.text.TextUtils;

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
import com.inuker.bluetooth.library.connect.response.BleNotifyResponse;
import com.inuker.bluetooth.library.connect.response.BleReadResponse;
import com.inuker.bluetooth.library.connect.response.BleReadRssiResponse;
import com.inuker.bluetooth.library.connect.response.BleUnnotifyResponse;
import com.inuker.bluetooth.library.connect.response.BleWriteResponse;
import com.inuker.bluetooth.library.model.BleGattProfile;
import com.miittech.you.App;
import com.miittech.you.common.BleCommon;
import com.miittech.you.common.Common;
import com.miittech.you.common.SoundPlayUtils;
import com.miittech.you.entity.Locinfo;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.global.SPConst;
import com.miittech.you.manager.BLEClientManager;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.DeviceInfoResponse;
import com.miittech.you.net.response.FriendsResponse;
import com.miittech.you.net.response.UserInfoResponse;
import com.ryon.constant.TimeConstants;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.NetworkUtils;
import com.ryon.mutils.SPUtils;
import com.ryon.mutils.TimeUtils;
import com.ryon.mutils.ToastUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;
import java.util.function.Predicate;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;

import static com.inuker.bluetooth.library.Constants.REQUEST_SUCCESS;
import static com.miittech.you.common.BleCommon.characteristicUUID;
import static com.miittech.you.common.BleCommon.serviceUUID;
import static com.miittech.you.common.BleCommon.userCharacteristicLogUUID;
import static com.miittech.you.common.BleCommon.userServiceUUID;

public  class BleService extends Service {
    public LocationClient mLocationClient = null;
    private MyLocationListener myListener = new MyLocationListener();
    private long lastMillins=0;
    CmdReceiver cmdReceiver;
    private List<String> mConnectedList = new ArrayList<>();
    private Map<String, Integer> mapRssi = new HashMap<String, Integer>();
    private Map<String,String> mapBattery = new HashMap<>();
    private BDLocation lastLocation;
    private List<String> mMacList = new ArrayList<>();

    @Override  
    public IBinder onBind(Intent intent) {
        return null;  
    }  
  
    @Override  
    public void onCreate() {
        super.onCreate();
        BluetoothContext.set(getApplicationContext());
        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(myListener);
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setCoorType("bd09ll");
        option.setScanSpan(60000);
        option.setOpenGps(true);
        option.setIsNeedAddress(true);
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
        AlarmManager aManager=(AlarmManager)getSystemService(Service.ALARM_SERVICE);
        Intent intent1 = new Intent(IntentExtras.ACTION.ACTION_TASK_SEND);
        PendingIntent pi=PendingIntent.getBroadcast(this, 0, intent1, PendingIntent.FLAG_CANCEL_CURRENT);
        aManager.setWindow(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+10000,5000, pi);
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


    class CmdReceiver extends BroadcastReceiver {
        @Override
        public synchronized void onReceive(Context context, Intent intent) {
            LogUtils.d("CommandReceiver","收到广播："+intent.getAction()+"----->"+intent.getIntExtra("cmd", -1));
            if(intent.getAction().equals(IntentExtras.ACTION.ACTION_BLE_COMMAND)){
                int cmd = intent.getIntExtra("cmd", -1);//获取Extra信息
                switch (cmd) {
                    case IntentExtras.CMD.CMD_DEVICE_LIST_ADD:
                        addDeviceList(intent.getStringArrayListExtra("macList"));
                        break;
                    case IntentExtras.CMD.CMD_DEVICE_CONNECT_BIND:
                        bindDevice(intent.getStringExtra("address"));
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
                    case IntentExtras.CMD.CMD_DEVICE_LIST_CLEAR:
                        clearAllConnect();
                        break;
                    case IntentExtras.CMD.CMD_TASK_EXCE:
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                exceTask();
                            }
                        }).start();
                        break;
                }
            }
        }
    }

    private synchronized void exceTask() {
        exceReportSubmit();
        if(mMacList.size()<=0){
            return;
        }
        for(final String mac : mMacList){
            if(BLEClientManager.getClient().getConnectStatus(mac)==Constants.STATUS_DEVICE_CONNECTED){
                BLEClientManager.getClient().readRssi(mac, new BleReadRssiResponse() {
                    @Override
                    public void onResponse(int code, Integer data) {
                        if(code==Constants.REQUEST_SUCCESS) {
                            LogUtils.d("readRssi",mac+">>>"+data);
                            Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                            intent.putExtra("ret", IntentExtras.RET.RET_DEVICE_READ_RSSI);
                            intent.putExtra("address", mac);
                            intent.putExtra("rssi", data);
                            sendBroadcast(intent);
                            mapRssi.put(mac,data);
                        }
                    }
                });
                DeviceInfoResponse response = (DeviceInfoResponse) SPUtils.getInstance().readObject(mac);
                if(response!=null) {
                    DeviceInfoResponse.UserinfoBean.DevinfoBean.AlertinfoBean alertinfoBean = response.getUserinfo().getDevinfo().getAlertinfo();
                    if (alertinfoBean != null) {
                        byte[] data = new byte[1];
                        if (alertinfoBean.getIsRepeat() == 0 || Common.isIgnoreBell()) {
                           data[0] = 0x00;

                        }else{
                            data[0] = 0x02;
                        }
//                        BLEClientManager.getClient().write(mac,BleCommon.linkLossUUID,BleCommon.characteristicUUID,data,new BleWriteResponse(){
//                            @Override
//                            public void onResponse(int code) {
//                                if(code == REQUEST_SUCCESS){
//
//                                }
//                            }
//                        });
                    }
                }
            }else{
                if(BLEClientManager.getClient().getConnectStatus(mac)==Constants.STATUS_DEVICE_DISCONNECTED){
                    connectDevice(mac);
                }
            }
        }
    }

    public synchronized void addDeviceList(ArrayList<String> macList){
        this.mMacList.clear();
        this.mMacList.addAll(macList);

        for(String mac:macList){
            connectDevice(mac);
        }
        for (String conMac : mConnectedList){
            if(!macList.contains(conMac)){
                BLEClientManager.getClient().unregisterConnectStatusListener(conMac, new BleConnectStatusListener() {
                    @Override
                    public void onConnectStatusChanged(String mac, int status) {

                    }
                });
                BLEClientManager.getClient().unnotify(conMac, BleCommon.userServiceUUID, BleCommon.userCharactButtonStateUUID, new BleUnnotifyResponse() {
                    @Override
                    public void onResponse(int code) {

                    }
                });
                BLEClientManager.getClient().disconnect(conMac);
                mConnectedList.remove(conMac);
            }
        }
    }

    public synchronized void connectDevice(final String mac){
        if(mConnectedList.contains(mac)){
            return;
        }
        mConnectedList.add(mac);
        if(BLEClientManager.getClient().getConnectStatus(mac)!=Constants.STATUS_DEVICE_DISCONNECTED){
            return;
        }
        BleConnectOptions options = new BleConnectOptions.Builder()
                .setConnectRetry(5)   // 连接如果失败重试3次
                .setConnectTimeout(30000)   // 连接超时30s
                .setServiceDiscoverRetry(3)  // 发现服务如果失败重试3次
                .setServiceDiscoverTimeout(20000)  // 发现服务超时20s
                .build();
        BLEClientManager.getClient().connect(mac,options, new BleConnectResponse() {
            @Override
            public void onResponse(int code, BleGattProfile data) {
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                if(code==Constants.REQUEST_SUCCESS){
                    LogUtils.d("bleResponse","贴片连接成功----->"+mac);
                    intent.putExtra("ret",IntentExtras.RET.RET_DEVICE_CONNECT_SUCCESS);
                    sendBroadcast(intent);
                    setWorkMode(mac);
                }else{
                    if(mConnectedList.contains(mac)){
                        mConnectedList.remove(mac);
                    }
                    LogUtils.d("bleResponse","贴片连接失败----->"+mac);
                    intent.putExtra("ret",IntentExtras.RET.RET_DEVICE_CONNECT_FAILED);
                    sendBroadcast(intent);
                }
            }
        });
    }

    public synchronized void bindDevice(final String mac){
        if(mConnectedList.contains(mac)){
            return;
        }
        mConnectedList.add(mac);
        BleConnectOptions options = new BleConnectOptions.Builder()
                .setConnectRetry(5)   // 连接如果失败重试3次
                .setConnectTimeout(30000)   // 连接超时30s
                .setServiceDiscoverRetry(3)  // 发现服务如果失败重试3次
                .setServiceDiscoverTimeout(20000)  // 发现服务超时20s
                .build();
        BLEClientManager.getClient().connect(mac,options, new BleConnectResponse() {
            @Override
            public void onResponse(int code, BleGattProfile data) {
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                if(code==Constants.REQUEST_SUCCESS) {
                    LogUtils.d("bleResponse","贴片连接成功----->"+mac);
                    intent.putExtra("ret", IntentExtras.RET.RET_DEVICE_CONNECT_SUCCESS);
                    sendBroadcast(intent);
                    setBindMode(mac);
                }else{
                    if(mConnectedList.contains(mac)){
                        mConnectedList.remove(mac);
                    }
                    LogUtils.d("bleResponse","贴片连接失败----->"+mac);
                    intent.putExtra("ret", IntentExtras.RET.RET_DEVICE_CONNECT_FAILED);
                    sendBroadcast(intent);
                }
            }
        });
    }

    private synchronized void unbindDevice(final String address) {
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
                        if(mConnectedList.contains(address)){
                            mConnectedList.remove(address);
                        }
                        LogUtils.d("bleResponse","贴片绑定成功----->"+address);
                        if(mConnectedList.contains(address)){
                            mConnectedList.remove(address);
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

    private synchronized void clearAllConnect(){
        for(String mac : mConnectedList){
            BLEClientManager.getClient().unregisterConnectStatusListener(mac, new BleConnectStatusListener() {
                @Override
                public void onConnectStatusChanged(String mac, int status) {

                }
            });
            BLEClientManager.getClient().unnotify(mac, BleCommon.userServiceUUID, BleCommon.userCharactButtonStateUUID, new BleUnnotifyResponse() {
                @Override
                public void onResponse(int code) {

                }
            });
            BLEClientManager.getClient().disconnect(mac);
            mConnectedList.remove(mac);
        }
    }

    private synchronized void startAlert(final String address) {
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
    private synchronized void stopAlert(final String address) {
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
                            LogUtils.d("bleResponse","贴片结束报警----->"+address);
                            intent.putExtra("ret", IntentExtras.RET.RET_DEVICE_CONNECT_ALERT_STOP_SUCCESS);
                            sendBroadcast(intent);
                        }
                    }
                });
            }

        }

    public synchronized void setWorkMode(final String mac){
        byte[] dataWork = Common.formatBleMsg(Params.BLEMODE.MODE_WORK, App.getInstance().getUserId());
        BLEClientManager.getClient().write(mac, userServiceUUID, userCharacteristicLogUUID, dataWork, new BleWriteResponse() {
            @Override
            public void onResponse(int code) {
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                if(code==Constants.REQUEST_SUCCESS) {
                    LogUtils.d("bleResponse","贴片设置工作模式成功----->"+mac);
                    intent.putExtra("ret", IntentExtras.RET.RET_DEVICE_CONNECT_WORK_SUCCESS);
                    sendBroadcast(intent);
                    if(SPUtils.getInstance().getBoolean(SPConst.IS_DEVICE_REDISCOVER)) {
                        Common.doCommitEvents(App.getInstance(), Common.formatMac2DevId(mac), Params.EVENT_TYPE.DEVICE_REDISCOVER, null);
                        final DeviceInfoResponse response = (DeviceInfoResponse) SPUtils.getInstance().readObject(mac);
                        if (response != null) {
                            DeviceInfoResponse.UserinfoBean.DevinfoBean.AlertinfoBean alertinfoBean = response.getUserinfo().getDevinfo().getAlertinfo();
                            if (alertinfoBean != null) {
                                byte[] data = new byte[1];
                                if (alertinfoBean.getIsReconnect()==1&&!Common.isIgnoreBell()) {
                                    new Thread(new Runnable(){
                                        public void run(){
                                            try {
                                                Thread.sleep(3000);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                           if(BLEClientManager.getClient().getConnectStatus(mac)==Constants.STATUS_DEVICE_CONNECTED){
                                               doPlay(response);
                                           }
                                        }
                                    }).start();
                                }
                            }
                        }
                    }else{
                        Common.doCommitEvents(App.getInstance(),Common.formatMac2DevId(mac),Params.EVENT_TYPE.DEVICE_CONNECT,null);
                        SPUtils.getInstance().put(SPConst.IS_DEVICE_REDISCOVER,true);
                    }
                    registAndNotify(mac);
                }else{
                    if(mConnectedList.contains(mac)){
                        mConnectedList.remove(mac);
                    }
                    LogUtils.d("bleResponse","贴片设置工作模式失败----->"+mac);
                    intent.putExtra("ret", IntentExtras.RET.RET_DEVICE_CONNECT_WORK_FAILED);
                    sendBroadcast(intent);
                }
            }
        });
    }
    public synchronized void registAndNotify(final String mac){
        BLEClientManager.getClient().registerConnectStatusListener(mac, new BleConnectStatusListener() {
            long temp=0;
            @Override
            public void onConnectStatusChanged(String mac, int status) {
                if (status == Constants.STATUS_CONNECTED) {
                    LogUtils.d("bleResponse",mac+">>>贴片连接状态改变>>已连接");
                    if(!mConnectedList.contains(mac)){
                        mConnectedList.add(mac);
                    }
                    temp = TimeUtils.getNowMills();
                } else if (status == Constants.STATUS_DISCONNECTED) {
                    LogUtils.d("bleResponse",mac+">>>贴片连接状态改变>>已断开");
                    BLEClientManager.getClient().disconnect(mac);
                    DeviceInfoResponse response = (DeviceInfoResponse) SPUtils.getInstance().readObject(mac);
                    if(response!=null){
                        DeviceInfoResponse.UserinfoBean.DevinfoBean.AlertinfoBean alertinfoBean = response.getUserinfo().getDevinfo().getAlertinfo();
                        if(alertinfoBean!=null){
                            if(alertinfoBean.getIsRepeat()==0||Common.isIgnoreBell()){
                                return;
                            }
                        }
                        if(temp!=0&&TimeUtils.getTimeSpan(temp,TimeUtils.getNowMills(),TimeConstants.SEC)>20){
                            doPlay(response);
                        };
                    }
                    Common.doCommitEvents(App.getInstance(),Common.formatMac2DevId(mac),Params.EVENT_TYPE.DEVICE_LOSE,null);
                    if(mConnectedList.contains(mac)){
                        mConnectedList.remove(mac);
                    }
                    Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                    intent.putExtra("ret", IntentExtras.RET.RET_DEVICE_CONNECT_FAILED);
                    intent.putExtra("address", mac);
                    sendBroadcast(intent);
                }
            }
        });

        BLEClientManager.getClient().notify(mac, BleCommon.userServiceUUID, BleCommon.userCharactButtonStateUUID, new BleNotifyResponse() {
            @Override
            public void onNotify(UUID service, UUID character, byte[] value) {
                LogUtils.d("接收到蓝牙发送广播》》》"+value);
                if(value[0]==02){
                    if(Common.isIgnoreBell()){
                        LogUtils.d("贴片在勿扰范围内,报警忽略！");
                        return;
                    }
                    DeviceInfoResponse response = (DeviceInfoResponse) SPUtils.getInstance().readObject(mac);
                    if(response!=null){
                        doPlay(response);
                    }

                }
            }

            @Override
            public void onResponse(int code) {

            }
        });
        BLEClientManager.getClient().notify(mac, BleCommon.batServiceUUID, BleCommon.batCharacteristicUUID, new BleNotifyResponse() {
            @Override
            public void onNotify(UUID service, UUID character, byte[] value) {
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret", IntentExtras.RET.RET_DEVICE_READ_BATTERY);
                intent.putExtra("address", mac);
                intent.putExtra("battery", value[0]+"");
                sendBroadcast(intent);
                mapBattery.put(mac,value[0]+"");
            }

            @Override
            public void onResponse(int code) {
                if(code==REQUEST_SUCCESS){

                }
            }
        });
    }

    private void doPlay(DeviceInfoResponse response) {
        String url = response.getUserinfo().getDevinfo().getAlertinfo().getUrlX();
        boolean isRepeat = (response.getUserinfo().getDevinfo().getAlertinfo().getIsRepeat()==1)?true:false;
        boolean isShake = (response.getUserinfo().getDevinfo().getAlertinfo().getIsShake()==1)?true:false;
        int duration = response.getUserinfo().getDevinfo().getAlertinfo().getDuration();
        if(url.contains("bluesforslim")){
            SoundPlayUtils.play(1,isRepeat,duration);
        }else if(url.contains("countryfair")){
            SoundPlayUtils.play(2,isRepeat,duration);
        }else if(url.contains("theclassiccall")){
            SoundPlayUtils.play(4,isRepeat,duration);
        }else{
            SoundPlayUtils.play(3,isRepeat,duration);
        }
    }

    public synchronized void setBindMode(final String mac){
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
                    setWorkMode(mac);
                }else{
                    if(mConnectedList.contains(mac)){
                        mConnectedList.remove(mac);
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
            if(!TextUtils.isEmpty(location.getAddrStr())){
                Locinfo locinfo = new Locinfo();
                locinfo.setAddr(location.getAddrStr());
                locinfo.setLat(location.getLatitude());
                locinfo.setLng(location.getLongitude());
                SPUtils.getInstance().remove(SPConst.LOC_INFO);
                SPUtils.getInstance().saveObject(SPConst.LOC_INFO,locinfo);
            }
            LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
            long curMillis = TimeUtils.getNowDate().getTime();
            if(lastLocation!=null) {
                LatLng last = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                if(DistanceUtil.getDistance(last, current)>200||TimeUtils.getTimeSpan(curMillis,lastMillins, TimeConstants.MIN)>30){
                    reportUserLocation(curMillis,location);
                }
            }else{
                reportUserLocation(curMillis,location);
            }
        }
    }

    private synchronized void reportUserLocation(final long millis, final BDLocation location) {
        if(mConnectedList.size()<=0){
            return;
        }
        final Map user_loc = new HashMap();
        user_loc.put("lat", location.getLatitude());
        user_loc.put("lng", location.getLongitude());
        user_loc.put("addr", Common.encodeBase64(location.getAddrStr()));
        final List<Map> devlist = new ArrayList<>();
        for (final String mac : mConnectedList) {
            if (mapBattery.containsKey(mac) && mapRssi.containsKey(mac)) {
                final Map devItem = new HashMap();
                devItem.put("devid", Common.formatMac2DevId(mac));
                devItem.put("devbattery", mapBattery.get(mac));
                int rssi = mapRssi.get(mac);
                if (rssi < -85) {
                    devItem.put("devposstate", 3);
                } else if (rssi > -85 && rssi < -70) {
                    devItem.put("devposstate", 2);
                } else if (rssi > -70) {
                    devItem.put("devposstate", 1);
                }
                devItem.put("devstate", 1);
                devItem.put("usedstate", 1);
                devItem.put("bindstate", 1);
                devlist.add(devItem);
            }
        }
        if(devlist.size()<=0){
            return;
        }
        Map repdata = new HashMap();
        repdata.put("reptime", TimeUtils.millis2String(millis, new SimpleDateFormat("yyyyMMddhhmmss")));
        repdata.put("user_loc", user_loc);
        repdata.put("devlist", devlist);
        Map param = new HashMap();
        param.put("method", 1);
        param.put("repdata", repdata);
        String json = new Gson().toJson(param);
        if(NetworkUtils.isConnected()){
            doReport(json);
        }else {
            Set<String> reportList = SPUtils.getInstance().getStringSet("reportList");
            if (reportList == null) {
                reportList = new LinkedHashSet<>();
            }
            reportList.add(json);
            SPUtils.getInstance().put("reportList", reportList);
        }
        lastLocation = location;
        lastMillins = millis;
    }
    public synchronized void exceReportSubmit(){
        if(!NetworkUtils.isConnected()){
            return;
        }
        final Set<String> reportList = SPUtils.getInstance().getStringSet("reportList");
        if(reportList==null||reportList.size()<=0){
            return;
        }

        final Iterator iterator = reportList.iterator();//先迭代出来
        while(iterator.hasNext()){//遍历
            doReport((String) iterator.next());
        }
    }

    private void doReport(final String json) {
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
                    public synchronized void accept(FriendsResponse response) throws Exception {
                        if (response.isSuccessful()) {
                            final Set<String> reportList = SPUtils.getInstance().getStringSet("reportList");
                            if(reportList!=null||reportList.contains(json)){
                                reportList.remove(json);
                                SPUtils.getInstance().remove("reportList");
                                SPUtils.getInstance().put("reportList",reportList);
                            }

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