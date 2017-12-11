package com.miittech.you.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothGatt;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.text.TextUtils;
import android.text.method.BaseKeyListener;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;
import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleIndicateCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleRssiCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleConnectState;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.data.BleScanState;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.BleScanRuleConfig;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.DeviceInfoResponse;
import com.miittech.you.net.response.DeviceResponse;
import com.miittech.you.net.response.FriendsResponse;
import com.ryon.constant.TimeConstants;
import com.ryon.mutils.EncryptUtils;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.NetworkUtils;
import com.ryon.mutils.SPUtils;
import com.ryon.mutils.TimeUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.sharesdk.framework.PlatformActionListener;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;
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
    private Map<String,BleDevice> mDeviceMap = new HashMap<>();
    private Map<String,BleDevice> mBindMap = new HashMap<>();
    private boolean isBind = false;

    @Override  
    public IBinder onBind(Intent intent) {
        return null;
    }  
  
    @Override  
    public void onCreate() {
        super.onCreate();
        BleManager.getInstance().init(getApplication());
        BleManager.getInstance().setMaxConnectCount(6);
        BleManager.getInstance().setOperateTimeout(10000);
        BleManager.getInstance().enableLog(true);
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
        scanDevice();
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
                    case IntentExtras.CMD.CMD_DEVICE_BIND_SCAN:
                        isBind = true;
                        mBindMap.clear();
                        break;
                    case IntentExtras.CMD.CMD_DEVICE_UNBIND_ERROR:
                        String address = intent.getStringExtra("address");
                        if(mDeviceMap.containsKey(address)){
                            mDeviceMap.remove(address);
                        }
                        break;
                    case IntentExtras.CMD.CMD_DEVICE_CONNECT_BIND:
                        BleDevice bleDevice = mBindMap.get(intent.getStringExtra("address"));
                        bindDevice(bleDevice);
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
        scanDevice();
        exceReportSubmit();
        List<BleDevice> list = BleManager.getInstance().getMultipleBluetoothController().getDeviceList();
        if(list.size()<=0){
            return;
        }
        for(final BleDevice bleDevice : list){
            if(BleManager.getInstance().getConnectState(bleDevice)== BleConnectState.CONNECT_CONNECTED){
                BleManager.getInstance().readRssi(bleDevice, new BleRssiCallback() {
                    @Override
                    public void onRssiFailure(BleDevice device,BleException exception) {

                    }

                    @Override
                    public void onRssiSuccess(BleDevice device,int rssi) {
                        LogUtils.d("readRssi",bleDevice.getMac()+">>>"+rssi);
                        Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                        intent.putExtra("ret", IntentExtras.RET.RET_BLE_READ_RSSI);
                        intent.putExtra("address", device.getMac());
                        intent.putExtra("rssi", rssi);
                        sendBroadcast(intent);
                        mapRssi.put(bleDevice.getMac(),rssi);
                    }
                });

                DeviceInfoResponse response = (DeviceInfoResponse) SPUtils.getInstance().readObject(bleDevice.getMac());
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
                if(BleManager.getInstance().getConnectState(bleDevice)== BleConnectState.CONNECT_DISCONNECT){
                    connectDevice(bleDevice);
                }
            }
        }
    }

    public synchronized void addDeviceList(ArrayList<String> macList){
        for(String mac:macList){
            if(!mDeviceMap.containsKey(mac)) {
                mDeviceMap.put(mac, null);
            }
        }
        Map<String,BleDevice> tempMap = new HashMap<>();
        tempMap.putAll(mDeviceMap);
        for (Map.Entry<String, BleDevice> entry : tempMap.entrySet()) {
            if(!macList.contains(entry.getKey())){
                mDeviceMap.remove(entry.getKey());
                BleManager.getInstance().disconnect(entry.getValue());
            }

        }
        scanDevice();
    }



    public synchronized  void scanDevice(){
        if(BleManager.getInstance().getScanSate()== BleScanState.STATE_SCANNING){
            return;
        }
        BleScanRuleConfig scanRuleConfig = new BleScanRuleConfig.Builder()
                .setDeviceName(false,"yoowoo")
                .setAutoConnect(true)
                .setScanTimeOut(0)
                .build();
        BleManager.getInstance().initScanRule(scanRuleConfig);
        BleManager.getInstance().scan(new BleScanCallback() {
            @Override
            public void onScanStarted(boolean success) {
                LogUtils.d("bleResponse","贴片扫描开始----->");
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret",IntentExtras.RET.RET_BLE_SCAN_START);
                sendBroadcast(intent);
            }

            @Override
            public void onScanning(BleDevice result) {
                LogUtils.d("bleResponse","扫描到有效贴片----->"+result.getMac());
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret",IntentExtras.RET.RET_BLE_SCANING);
                intent.putExtra("address",result.getMac());
                sendBroadcast(intent);
                if(!mDeviceMap.containsKey((result.getMac()))){
                    if(isBind&&result.getRssi()>-100){
                        if(!mBindMap.containsKey(result.getMac())) {
                            mBindMap.put(result.getMac(), result);
                            LogUtils.d("bleResponse", "开始绑定贴片----->" + result.getMac());
                            Intent intent1 = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                            intent1.putExtra("ret", IntentExtras.RET.RET_BLE_FIND_BIND_DEVICE);
                            intent1.putExtra("address", result.getMac());
                            sendBroadcast(intent1);
                        }
                        isBind = false;
                    }
                    return;
                }
                LogUtils.d("bleResponse","开始连接贴片----->"+result.getMac());
                mDeviceMap.put(result.getMac(),result);
                if(!BleManager.getInstance().isConnected(result)){
                    connectDevice(result);
                }
            }

            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {

            }
        });
    }
    public synchronized void connectDevice(BleDevice bleDevice){
        if(bleDevice==null){
            return;
        }
        if(BleManager.getInstance().getConnectState(bleDevice)==BleConnectState.CONNECT_CONNECTED
                ||BleManager.getInstance().getConnectState(bleDevice)==BleConnectState.CONNECT_CONNECTING){
            return;
        }
        BleManager.getInstance().connect(bleDevice, new BleGattCallback() {
            @Override
            public void onStartConnect(BleDevice device) {
                LogUtils.d("bleResponse","贴片开始连接----->"+device.getMac());
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret",IntentExtras.RET.RET_BLE_CONNECT_START);
                intent.putExtra("address",device.getMac());
                sendBroadcast(intent);
            }

            @Override
            public void onConnectFail(BleDevice device,BleException exception) {
                LogUtils.d("bleResponse","贴片连接失败----->"+device.getMac());
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret",IntentExtras.RET.RET_BLE_CONNECT_FAILED);
                intent.putExtra("address",device.getMac());
                sendBroadcast(intent);
            }

            @Override
            public void onConnectSuccess(BleDevice device, BluetoothGatt gatt, int status) {
                LogUtils.d("bleResponse","贴片连接成功----->"+device.getMac());
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret",IntentExtras.RET.RET_BLE_CONNECT_SUCCESS);
                intent.putExtra("address",device.getMac());
                sendBroadcast(intent);
                setWorkMode(device);
            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice device, BluetoothGatt gatt, int status) {
                LogUtils.d("bleResponse","贴片连接断开----->"+device.getMac());
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret",IntentExtras.RET.RET_BLE_DISCONNECT);
                intent.putExtra("address",device.getMac());
                sendBroadcast(intent);
                Common.doCommitEvents(App.getInstance(),Common.formatMac2DevId(device.getMac()),Params.EVENT_TYPE.DEVICE_LOSE,null);
                DeviceInfoResponse response = (DeviceInfoResponse) SPUtils.getInstance().readObject(device.getMac());
                if(response!=null){
                    DeviceInfoResponse.UserinfoBean.DevinfoBean.AlertinfoBean alertinfoBean = response.getUserinfo().getDevinfo().getAlertinfo();
                    if(alertinfoBean!=null){
                        if(alertinfoBean.getIsRepeat()==0||Common.isIgnoreBell()){
                            return;
                        }
                    }
                    doPlay(response);
                }
            }
        });
    }

    public synchronized void  bindDevice(BleDevice bleDevice){
        if(bleDevice==null){
            return;
        }
        BleManager.getInstance().connect(bleDevice,new BleGattCallback() {
            @Override
            public void onStartConnect(BleDevice bleDevice) {
                LogUtils.d("bleResponse","贴片开始连接----->"+bleDevice.getMac());
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret", IntentExtras.RET.RET_BLE_CONNECT_START);
                intent.putExtra("address", bleDevice.getMac());
                sendBroadcast(intent);
            }

            @Override
            public void onConnectFail(BleDevice bleDevice,BleException exception) {
                LogUtils.d("bleResponse","贴片连接失败----->"+bleDevice.getMac());
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret", IntentExtras.RET.RET_BLE_CONNECT_FAILED);
                intent.putExtra("address", bleDevice.getMac());
                sendBroadcast(intent);
            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                LogUtils.d("bleResponse","贴片连接成功----->"+bleDevice.getMac());
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret", IntentExtras.RET.RET_BLE_CONNECT_SUCCESS);
                intent.putExtra("address", bleDevice.getMac());
                sendBroadcast(intent);
                setBindMode(bleDevice);
            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice device, BluetoothGatt gatt, int status) {
                LogUtils.d("bleResponse","贴片连接断开----->"+device.getMac());
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret", IntentExtras.RET.RET_BLE_DISCONNECT);
                intent.putExtra("address", device.getMac());
                sendBroadcast(intent);
                Common.doCommitEvents(App.getInstance(),Common.formatMac2DevId(device.getMac()),Params.EVENT_TYPE.DEVICE_LOSE,null);
                DeviceInfoResponse response = (DeviceInfoResponse) SPUtils.getInstance().readObject(device.getMac());
                if(response!=null){
                    DeviceInfoResponse.UserinfoBean.DevinfoBean.AlertinfoBean alertinfoBean = response.getUserinfo().getDevinfo().getAlertinfo();
                    if(alertinfoBean!=null){
                        if(alertinfoBean.getIsRepeat()==0||Common.isIgnoreBell()){
                            return;
                        }
                    }
                    doPlay(response);
                }
            }
        });
    }

    private synchronized void unbindDevice(String address) {
        BleDevice bleDevice = mDeviceMap.get(address);
        if(bleDevice==null){
            return;
        }
        if(!BleManager.getInstance().isConnected(bleDevice)){
            connectDevice(bleDevice);
            return;
        }
        byte[] dataWork = Common.formatBleMsg(Params.BLEMODE.MODE_UNBIND, Common.getUserId());
        BleManager.getInstance().write(bleDevice, userServiceUUID, userCharacteristicLogUUID, dataWork, new BleWriteCallback() {
            @Override
            public void onWriteSuccess(BleDevice device) {
                LogUtils.d("bleResponse","贴片解绑成功----->"+device.getMac());
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret", IntentExtras.RET.RET_BLE_UNBIND_COMPLETE);
                intent.putExtra("address", device.getMac());
                sendBroadcast(intent);
            }

            @Override
            public void onWriteFailure(BleDevice device,BleException exception) {
                LogUtils.d("bleResponse","贴片绑定失败----->"+device.getMac());
            }
        });
    }

    private synchronized void clearAllConnect(){
        BleManager.getInstance().disconnectAllDevice();
        mConnectedList.clear();
        mBindMap.clear();
    }

    private synchronized void startAlert(String address) {
        BleDevice bleDevice = mDeviceMap.get(address);
        if(bleDevice==null){
            return;
        }
        if(!BleManager.getInstance().isConnected(bleDevice)){
            connectDevice(bleDevice);
            return;
        }
        byte[] options = new byte[]{0x02};
        BleManager.getInstance().write(bleDevice, serviceUUID, characteristicUUID, options, new BleWriteCallback() {
            @Override
            public void onWriteSuccess(BleDevice device) {
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                LogUtils.d("bleResponse","贴片开始报警----->"+device.getMac());
                intent.putExtra("ret", IntentExtras.RET.RET_BLE_ALERT_STARTED);
                intent.putExtra("address", device.getMac());
                sendBroadcast(intent);
            }

            @Override
            public void onWriteFailure(BleDevice device,BleException exception) {
                LogUtils.d("bleResponse","贴片开始报警---->失败了----->"+device.getMac());
            }
        });

    }
    private synchronized void stopAlert(String address) {
        BleDevice bleDevice = mDeviceMap.get(address);
        if(bleDevice==null){
            return;
        }
        if(!BleManager.getInstance().isConnected(bleDevice)){
            connectDevice(bleDevice);
            return;
        }
        byte[] options = new byte[]{0x00};
        BleManager.getInstance().write(bleDevice, serviceUUID, characteristicUUID, options, new BleWriteCallback() {
            @Override
            public void onWriteSuccess(BleDevice device) {
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                LogUtils.d("bleResponse","贴片结束报警----->"+device.getMac());
                intent.putExtra("ret", IntentExtras.RET.RET_BLE_ALERT_STOPED);
                intent.putExtra("address", device.getMac());
                sendBroadcast(intent);
            }

            @Override
            public void onWriteFailure(BleDevice device,BleException exception) {
                LogUtils.d("bleResponse","贴片结束报警--->失败了----->"+device.getMac());
            }
        });
    }

    public synchronized void setWorkMode(BleDevice bleDevice){
        if(bleDevice==null){
            return;
        }
        byte[] dataWork = Common.formatBleMsg(Params.BLEMODE.MODE_WORK, Common.getUserId());
        BleManager.getInstance().write(bleDevice, userServiceUUID, userCharacteristicLogUUID, dataWork, new BleWriteCallback() {
            @Override
            public void onWriteSuccess(final BleDevice device) {
                LogUtils.d("bleResponse","贴片设置工作模式成功----->"+device.getMac());
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret", IntentExtras.RET.RET_BLE_MODE_WORK_SUCCESS);
                intent.putExtra("address", device.getMac());
                sendBroadcast(intent);
                if(SPUtils.getInstance().getBoolean(SPConst.IS_DEVICE_REDISCOVER)) {
                    Common.doCommitEvents(App.getInstance(), Common.formatMac2DevId(device.getMac()), Params.EVENT_TYPE.DEVICE_REDISCOVER, null);
                    final DeviceInfoResponse response = (DeviceInfoResponse) SPUtils.getInstance().readObject(device.getMac());
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
                                        if(BleManager.getInstance().isConnected(device)){
                                            doPlay(response);
                                        }
                                    }
                                }).start();
                            }
                        }
                    }
                }else{
                    Common.doCommitEvents(App.getInstance(),Common.formatMac2DevId(device.getMac()),Params.EVENT_TYPE.DEVICE_CONNECT,null);
                    SPUtils.getInstance().put(SPConst.IS_DEVICE_REDISCOVER,true);
                }
                notfyAlert(device);
            }

            @Override
            public void onWriteFailure(BleDevice device,BleException exception) {
                LogUtils.d("bleResponse","贴片设置工作模式失败----->"+device.getMac());
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret", IntentExtras.RET.RET_BLE_MODE_WORK_FAIL);
                intent.putExtra("address", device.getMac());
                sendBroadcast(intent);
            }
        });
    }
    public synchronized void notfyAlert(final BleDevice bleDevice) {
        BleManager.getInstance().notify(bleDevice, BleCommon.userServiceUUID, BleCommon.userCharactButtonStateUUID, new BleNotifyCallback() {
            @Override
            public void onNotifySuccess() {
                LogUtils.d("bleResponse", "设置监测贴片按钮事件成功");
                notifyBattery(bleDevice);
            }

            @Override
            public void onNotifyFailure(BleException exception) {
                LogUtils.d("bleResponse", exception.getDescription());
            }

            @Override
            public void onCharacteristicChanged(BleDevice device, byte[] data) {
                LogUtils.d("bleResponse", "监测到" + device.getMac() + "双击事件("+data[0]+")--->报警广播数据");
                if (data[0] == 02) {
                    if (Common.isIgnoreBell()) {
                        LogUtils.d("贴片在勿扰范围内,报警忽略！");
                        return;
                    }
                    DeviceInfoResponse response = (DeviceInfoResponse) SPUtils.getInstance().readObject(device.getMac());
                    if (response != null) {
                        doPlay(response);
                    }

                }
            }
        });

    }
    public synchronized void notifyBattery(final BleDevice bleDevice){
        BleManager.getInstance().notify(bleDevice, BleCommon.batServiceUUID, BleCommon.batCharacteristicUUID, new BleNotifyCallback() {
            @Override
            public void onNotifySuccess() {
                LogUtils.d("bleResponse", "设置监测电量广播成功");
            }

            @Override
            public void onNotifyFailure(BleException exception) {
                LogUtils.d("bleResponse", exception.getDescription());
            }

            @Override
            public void onCharacteristicChanged(BleDevice device, byte[] data) {
                LogUtils.d("bleResponse", "监测到" + device.getMac() + "电池电量" + data[0]+"%");
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret", IntentExtras.RET.RET_BLE_READ_BATTERY);
                intent.putExtra("address", device.getMac());
                intent.putExtra("battery", data[0] + "");
                sendBroadcast(intent);
                mapBattery.put(device.getMac(), data[0] + "");
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

    public synchronized void setBindMode(BleDevice bleDevice){
        if(bleDevice==null){
            return;
        }
        byte[] bind = Common.formatBleMsg(Params.BLEMODE.MODE_BIND,Common.getUserId());
        BleManager.getInstance().write(bleDevice, BleCommon.userServiceUUID, BleCommon.userCharacteristicLogUUID, bind, new BleWriteCallback() {
            @Override
            public void onWriteSuccess(BleDevice device) {
                LogUtils.d("bleResponse","贴片设置绑定模式成功----->"+device.getMac());
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret", IntentExtras.RET.RET_BLE_MODE_BIND_SUCCESS);
                intent.putExtra("address", device.getMac());
                sendBroadcast(intent);
                if(mBindMap.containsKey(device.getMac())){
                    mBindMap.remove(device.getMac());
                }
                if(!mDeviceMap.containsKey(device.getMac())){
                    mDeviceMap.put(device.getMac(),device);
                }
                setWorkMode(device);
            }

            @Override
            public void onWriteFailure(BleDevice device,BleException exception) {
                LogUtils.d("bleResponse","贴片设置绑定模式失败----->"+device.getMac());
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret", IntentExtras.RET.RET_BLE_MODE_BIND_FAIL);
                intent.putExtra("address", device.getMac());
                sendBroadcast(intent);
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

                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret", IntentExtras.RET.LOCATION);
                intent.putExtra("data", locinfo);
                sendBroadcast(intent);
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
        DeviceResponse response = (DeviceResponse) SPUtils.getInstance().readObject(SPConst.DATA.DEVICELIST);
        if(response==null){
            return;
        }
        List<DeviceResponse.DevlistBean> mlist = response.getDevlist();
        if(mlist==null||mlist.size()<=0){
            return;
        }
        final List<Map> devlist = new ArrayList<>();
        for (DeviceResponse.DevlistBean devlistBean : mlist){
            String mac = Common.formatDevId2Mac(devlistBean.getDevidX());
            final Map devItem = new HashMap();
            devItem.put("devid", devlistBean.getDevidX());
            devItem.put("usedstate", devlistBean.getUsedstate());
            devItem.put("bindstate", 1);
            if(TextUtils.isEmpty(devlistBean.getFriendname())){
                devItem.put("sourceid", 0);
                devItem.put("devbattery",0);
                devItem.put("devposstate", 0);
                devItem.put("devstate", 0);
            }else{
                devItem.put("sourceid", devlistBean.getFriendid());
                BleDevice bleDevice = mDeviceMap.get(mac);
                if(BleManager.getInstance().isConnected(bleDevice)){
                    devItem.put("devstate", 1);
                    if (mapBattery.containsKey(mac)){
                        devItem.put("devbattery", mapBattery.get(mac));
                    }else{
                        return;
                    }
                    if(mapRssi.containsKey(mac)){
                        int rssi = mapRssi.get(mac);
                        if (rssi < -85) {
                            devItem.put("devposstate", 3);
                        } else if (rssi > -85 && rssi < -70) {
                            devItem.put("devposstate", 2);
                        } else if (rssi > -70) {
                            devItem.put("devposstate", 1);
                        }
                    }else{
                        return;
                    }
                }else {
                    devItem.put("devbattery",0);
                    devItem.put("devposstate", 100);
                    devItem.put("devstate", -99);
                }
            }
            devlist.add(devItem);
        }
        if(devlist.size()<=0){
            return;
        }
        final Map user_loc = new HashMap();
        user_loc.put("lat", location.getLatitude());
        user_loc.put("lng", location.getLongitude());
        user_loc.put("addr", Common.encodeBase64(location.getAddrStr()));
        Map repdata = new HashMap();
        repdata.put("reptime", TimeUtils.millis2String(millis, new SimpleDateFormat("yyyyMMddhhmmss")));
        repdata.put("user_loc", user_loc);
        repdata.put("devlist", devlist);
        String json = new Gson().toJson(repdata);
        if(NetworkUtils.isConnected()){
            JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();
            JsonArray jsonArray = new JsonArray();
            jsonArray.add(jsonObject);
            doReport(1,jsonArray);
        }else {
            Set<String> reportList = new LinkedHashSet<>();
            reportList.addAll(SPUtils.getInstance().getStringSet("reportList"));
            reportList.add(json);
            SPUtils.getInstance().remove("reportList");
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
        JsonArray jsonArray = new JsonArray();
        Iterator iterator = reportList.iterator();//先迭代出来
        while(iterator.hasNext()){//遍历
            JsonObject jsonObject = new JsonParser().parse((String)iterator.next()).getAsJsonObject();
            jsonArray.add(jsonObject);
            if(jsonArray.size()>50){
                break;
            }
        }
        doReport(2,jsonArray);
    }

    private void doReport(final int method, final JsonArray jsonArray) {
        Map param = new HashMap();
        param.put("method", method);
        param.put("repdata",jsonArray);
        String json = new Gson().toJson(param);
        if(TextUtils.isEmpty(Common.getUserId())||TextUtils.isEmpty(Common.getTocken())){
            return;
        }
        PubParam pubParam = new PubParam(Common.getUserId());
        String sign_unSha1 = pubParam.toValueString() + json + Common.getTocken();
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
                            final Set<String> reportList = new LinkedHashSet<>();
                            reportList.addAll(SPUtils.getInstance().getStringSet("reportList"));
                            if(reportList==null||reportList.size()<=0){
                                return;
                            }
                            for (JsonElement jsonElement : jsonArray){
                                String report = new Gson().toJson(jsonElement);
                                if(reportList.contains(report)){
                                    reportList.remove(report);
                                }
                            }
                            SPUtils.getInstance().remove("reportList");
                            SPUtils.getInstance().put("reportList",reportList);
                        }else{
                            response.onError(App.getInstance());
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