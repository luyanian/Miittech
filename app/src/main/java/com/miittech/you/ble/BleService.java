package com.miittech.you.ble;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.v4.util.SimpleArrayMap;
import android.text.TextUtils;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.miittech.you.App;
import com.miittech.you.ble.gatt.BleWriteCallback;
import com.miittech.you.ble.gatt.GattCallback;
import com.miittech.you.ble.scan.ScanResult;
import com.miittech.you.ble.scan.ScanResultCallback;
import com.miittech.you.task.ConnectDeviceTask;
import com.miittech.you.task.Priority;
import com.miittech.you.task.TaskQueue;
import com.miittech.you.utils.BingGoPlayUtils;
import com.miittech.you.utils.Common;
import com.miittech.you.utils.SoundPlayUtils;
import com.miittech.you.entity.DeviceInfo;
import com.miittech.you.entity.Locinfo;
import com.miittech.you.global.HttpUrl;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.global.Params;
import com.miittech.you.global.PubParam;
import com.miittech.you.global.SPConst;
import com.miittech.you.net.ApiServiceManager;
import com.miittech.you.net.response.DeviceListResponse;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import static com.miittech.you.ble.BleUUIDS.characteristicUUID;
import static com.miittech.you.ble.BleUUIDS.serviceUUID;
import static com.miittech.you.ble.BleUUIDS.userCharacteristicLogUUID;
import static com.miittech.you.ble.BleUUIDS.userServiceUUID;

public  class BleService extends Service {
    public LocationClient mLocationClient = null;
    private MyLocationListener myListener = new MyLocationListener();
    private long lastMillins=0;
    private long lastUnScanning = 0;
    CmdReceiver cmdReceiver;
    private SimpleArrayMap<String, Integer> mapRssi = new SimpleArrayMap<String, Integer>();
    private SimpleArrayMap<String,String> mapBattery = new SimpleArrayMap<>();
    private BDLocation lastLocation;
    private SimpleArrayMap<String,BluetoothDevice> mDeviceMap = new SimpleArrayMap<>();
    private SimpleArrayMap<String,BluetoothDevice> mBindMap = new SimpleArrayMap<>();
    private SimpleArrayMap<String,Boolean> mAlertingMap = new SimpleArrayMap<>();
    private SimpleArrayMap<String,Byte> mLinkLoseMap = new SimpleArrayMap<>();
    private SimpleArrayMap<String,Boolean> isIgnoreEvents = new SimpleArrayMap<>();
    private SimpleArrayMap<String,Boolean> mNotFirstConnect = new SimpleArrayMap<>();
    private SimpleArrayMap<String,Boolean> isDevicesNeedAlert = new SimpleArrayMap<>();
    private SimpleArrayMap<String,Boolean> mNotFirstDisConnect = new SimpleArrayMap<>();
    private boolean isBind = false;
    private static ScheduledExecutorService executorService = null;
    private TaskQueue taskQueue;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                BleClient.getInstance().cancelScan();
                LogUtils.d(e.getMessage());
                e.printStackTrace();
            }
        });
        checkLocationService();
        cmdReceiver = new CmdReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(IntentExtras.ACTION.ACTION_BLE_COMMAND);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        App.getInstance().getLocalBroadCastManager().registerReceiver(cmdReceiver, filter);
        LogUtils.d("bleService-onStartCommand()-new Thread");
        scanDevice();
        taskQueue = new TaskQueue(1);
        taskQueue.start();
    }

    private synchronized void checkLocationService() {
        if(mLocationClient==null) {
            mLocationClient = new LocationClient(getApplicationContext());
            mLocationClient.registerLocationListener(myListener);
            LocationClientOption option = new LocationClientOption();
            option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
            option.setCoorType("bd09ll");
            option.setScanSpan(30000);
            option.setOpenGps(true);
            option.setIsNeedAddress(true);
            option.setIgnoreKillProcess(true);
            option.setWifiCacheTimeOut(5 * 60 * 1000);
            mLocationClient.setLocOption(option);
        }
        if(!mLocationClient.isStarted()) {
            LogUtils.d("bleService","检测百度定位服务--->false");
            mLocationClient.start();
            mLocationClient.requestLocation();
        }else{
            LogUtils.d("bleService","检测百度定位服务--->true");
        }
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        LogUtils.d("bleService","bleService-onStartCommand()");
        if(executorService!=null){
            executorService.shutdownNow();
            executorService=null;
        }
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                Intent intent1 = new Intent(IntentExtras.ACTION.ACTION_TASK_SEND);
                App.getInstance().sendBroadcast(intent1);
            }
        }, 5, 30, TimeUnit.SECONDS);

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtils.d("bleService-onDestroy()");
        App.getInstance().getLocalBroadCastManager().unregisterReceiver(cmdReceiver);
        BleClient.getInstance().cancelScan();
        BleClient.getInstance().disconnectAllDevice();

        if(taskQueue!=null){
            taskQueue.stop();
        }
        if(executorService!=null) {
            executorService.shutdownNow();
            executorService=null;
        }

        if(mLocationClient!=null){
            mLocationClient.stop();
            mLocationClient=null;
        }

        mDeviceMap.clear();
        mapRssi.clear();
        mapBattery.clear();
        mBindMap.clear();
        mLinkLoseMap.clear();
        mNotFirstConnect.clear();
        mNotFirstDisConnect.clear();

    }

    StringBuilder stringBuilder = new StringBuilder();
    class CmdReceiver extends BroadcastReceiver {
        @Override
        public synchronized void onReceive(Context context, Intent intent) {
            synchronized (this) {
                stringBuilder.delete(0, stringBuilder.length());
                stringBuilder.append("收到广播 cmd==>");
                if (intent.getAction().equals(IntentExtras.ACTION.ACTION_BLE_COMMAND)) {
                    int cmd = intent.getIntExtra("cmd", -1);//获取Extra信息
                    switch (cmd) {
                        case IntentExtras.CMD.CMD_DEVICE_LIST_ADD:
                            stringBuilder.append("CMD_DEVICE_LIST_ADD   macList==>" + intent.getStringArrayListExtra("macList"));
                            addDeviceList(intent.getStringArrayListExtra("macList"));
                            break;
                        case IntentExtras.CMD.CMD_DEVICE_SCANING:
                            stringBuilder.append("CMD_DEVICE_SCANING   mac==>" + intent.getStringExtra("address"));
                            connectDevice(mDeviceMap.get(intent.getStringExtra("address")),false);
                            break;
                        case IntentExtras.CMD.CMD_DEVICE_BIND_SCAN:
                            stringBuilder.append("CMD_DEVICE_BIND_SCAN");
                            isBind = true;
                            mBindMap.clear();
                            break;
                        case IntentExtras.CMD.CMD_DEVICE_UNBIND_ERROR:
                            stringBuilder.append("CMD_DEVICE_UNBIND_ERROR   mac==>" + intent.getStringExtra("address"));
                            isBind = false;
                            String address = intent.getStringExtra("address");
                            if (mDeviceMap.containsKey(address)) {
                                mDeviceMap.remove(address);
                            }
                            if (mBindMap.containsKey(address)) {
                                mBindMap.remove(address);
                            }
                            break;
                        case IntentExtras.CMD.CMD_DEVICE_CONNECT_BIND:
                            stringBuilder.append("CMD_DEVICE_CONNECT_BIND   mac==>" + intent.getStringExtra("address"));
                            BluetoothDevice bleDevice = mBindMap.get(intent.getStringExtra("address"));
                            connectDevice(bleDevice,true);
                            break;
                        case IntentExtras.CMD.CMD_DEVICE_ALERT_START:
                            stringBuilder.append("CMD_DEVICE_ALERT_START   mac==>" + intent.getStringExtra("address"));
                            startAlert(intent.getStringExtra("address"));
                            break;
                        case IntentExtras.CMD.CMD_DEVICE_ALERT_STOP:
                            stringBuilder.append("CMD_DEVICE_ALERT_STOP   mac==>" + intent.getStringExtra("address"));
                            stopAlert(intent.getStringExtra("address"));
                            break;
                        case IntentExtras.CMD.CMD_DEVICE_UNBIND:
                            stringBuilder.append("CMD_DEVICE_UNBIND   mac==>" + intent.getStringExtra("address"));
                            unbindDevice(intent.getStringExtra("address"));
                            break;
                        case IntentExtras.CMD.CMD_DEVICE_LIST_CLEAR:
                            stringBuilder.append("CMD_DEVICE_LIST_CLEAR");
                            doLogOut();
                            break;
                        case IntentExtras.CMD.CMD_TASK_EXCE:
                            stringBuilder.append("CMD_TASK_EXCE");
                            exceTask();
                            break;
                    }
                } else if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                    switch (blueState) {
                        case BluetoothAdapter.STATE_TURNING_ON:
                            stringBuilder.append("STATE_TURNING_ON");
                            break;
                        case BluetoothAdapter.STATE_ON:
                            stringBuilder.append("STATE_ON");
                            Intent bleOnIntent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                            bleOnIntent.putExtra("ret", IntentExtras.RET.RET_BLE_STATE_ON);
                            App.getInstance().getLocalBroadCastManager().sendBroadcast(bleOnIntent);
                            scanDevice();
                            exceCalibrationDevice();
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            stringBuilder.append("STATE_TURNING_OFF");
                            break;
                        case BluetoothAdapter.STATE_OFF:
                            stringBuilder.append("STATE_OFF");
                            diableBluetooth();
                            Intent bleOffIntent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                            bleOffIntent.putExtra("ret", IntentExtras.RET.RET_BLE_STATE_OFF);
                            App.getInstance().getLocalBroadCastManager().sendBroadcast(bleOffIntent);
                            break;
                    }
                }
                LogUtils.d("bleService", stringBuilder.toString());
            }
        }
    }

    private synchronized void exceTask() {
        checkLocationService();
        exceCheckScaning();
        exceReportSubmit();
        exceCalibrationDevice();
        exceSetLinkLose();
    }
    private void exceCheckScaning() {
        LogUtils.d("bleservice","exceCheckScaning()-->"+BleClient.getInstance().isScaning());
        if(BleClient.getInstance().isScaning()) {
            if (lastUnScanning != 0 && TimeUtils.getTimeSpanByNow(lastUnScanning, TimeConstants.MIN) > 1) {
                lastUnScanning = 0;
                BleClient.getInstance().cancelScan();
            }
        }else{
            scanDevice();
        }
    }

    private void exceSetLinkLose() {
        for (int i=0;i<mDeviceMap.size();i++){
            BluetoothDevice bleDevice = mDeviceMap.valueAt(i);
            if(bleDevice==null){
                continue;
            }
            if(BleClient.getInstance().isConnected(bleDevice.getAddress())){
                BleClient.getInstance().readRssi(bleDevice.getAddress());
                DeviceInfo deviceInfo = (DeviceInfo) SPUtils.getInstance().readObject(bleDevice.getAddress());
                if(deviceInfo!=null) {
                    if(mAlertingMap.containsKey(Common.formatDevId2Mac(deviceInfo.getDevidX()))&&mAlertingMap.get(Common.formatDevId2Mac(deviceInfo.getDevidX()))){
                        continue;
                    }
                    final byte[] data = new byte[1];
                    if (!Common.isBell()) {
                        data[0] = 0x00;
                    }else{
                        data[0] = 0x02;
                    }
                    if(!mLinkLoseMap.containsKey(Common.formatDevId2Mac(deviceInfo.getDevidX()))||data[0]!=mLinkLoseMap.get(Common.formatDevId2Mac(deviceInfo.getDevidX()))) {
                        BleClient.getInstance().write(bleDevice.getAddress(),BleUUIDS.linkLossUUID, BleUUIDS.characteristicUUID, data, new BleWriteCallback() {
                            @Override
                            public void onWriteSuccess(BluetoothDevice device) {
                                mLinkLoseMap.put(device.getAddress(), data[0]);
                            }

                            @Override
                            public void onWriteFialed(BluetoothDevice device) {

                            }
                        });
                    }
                }
            }else {
                connectDevice(bleDevice,false);
            }
        }
    }

    private void connectDevice(BluetoothDevice bleDevice, final boolean isBind) {
        ConnectDeviceTask connectDeviceTask = new ConnectDeviceTask(bleDevice, isBind, new GattCallback() {
            @Override
            public synchronized void onStartConnect(String mac) {
                LogUtils.d("bleService", "贴片开始连接----->" + mac);
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret", IntentExtras.RET.RET_BLE_CONNECT_START);
                intent.putExtra("address", mac);
                App.getInstance().getLocalBroadCastManager().sendBroadcast(intent);
            }

            @Override
            public synchronized void onConnectFail(String mac) {
                LogUtils.d("bleService", "贴片连接失败----->" + mac);
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret", IntentExtras.RET.RET_BLE_CONNECT_FAILED);
                intent.putExtra("address", mac);
                App.getInstance().getLocalBroadCastManager().sendBroadcast(intent);
            }

            @Override
            public synchronized void onConnectSuccess(String mac, int status) {
                LogUtils.d("bleService", "贴片连接成功----->" + mac);
            }

            @Override
            public synchronized boolean onEffectConnectSuccess(String mac, int status) {
                LogUtils.d("bleService", "贴片连接成功----->" + mac);
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret", IntentExtras.RET.RET_BLE_CONNECT_SUCCESS);
                intent.putExtra("address", mac);
                App.getInstance().getLocalBroadCastManager().sendBroadcast(intent);
                return mBindMap.containsKey(mac);
            }

            @Override
            public synchronized void onDisConnected(boolean isActiveDisConnected, final String mac, int status) {
                if (mLinkLoseMap.containsKey(mac)) {
                    mLinkLoseMap.remove(mac);
                }
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret", IntentExtras.RET.RET_BLE_DISCONNECT);
                intent.putExtra("address", mac);
                App.getInstance().getLocalBroadCastManager().sendBroadcast(intent);
            }

            @Override
            public synchronized void onEffectDisConnected(boolean isActiveDisConnected, final String mac, int status) {
                LogUtils.d("bleService", "贴片连接断开----->" + mac + "   isActiveDisConnected--->" + isActiveDisConnected);
                if (mLinkLoseMap.containsKey(mac)) {
                    mLinkLoseMap.remove(mac);
                }
                if (isIgnoreEvents.containsKey(mac) && isIgnoreEvents.get(mac)) {
                    isIgnoreEvents.remove(mac);
                } else {
                    Common.doCommitEvents(App.getInstance(), mac, Params.EVENT_TYPE.DEVICE_LOSE);
                }
                isDevicesNeedAlert.put(mac,true);

                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret", IntentExtras.RET.RET_BLE_DISCONNECT);
                intent.putExtra("address", mac);
                App.getInstance().getLocalBroadCastManager().sendBroadcast(intent);

                DeviceInfo deviceInfo = (DeviceInfo) SPUtils.getInstance().readObject(mac);
                if (deviceInfo == null || deviceInfo.getAlertinfo() == null) {
                    return;
                }
                if (!isActiveDisConnected) {
                    DeviceInfo.AlertinfoBean alertinfoBean = deviceInfo.getAlertinfo();
                    if (mNotFirstDisConnect.containsKey(mac) && mNotFirstDisConnect.get(mac)) {
                        if (alertinfoBean.getIsRepeat() == 1 && Common.isBell()) {
                            doPlay(deviceInfo);
                        }
                    } else {
                        mNotFirstDisConnect.put(mac, true);
                        if (Common.isBell()) {
                            doPlay(deviceInfo);
                        }
                    }
                }
            }

            @Override
            public void onReadRemoteRssi(String mac, int rssi, int status) {
                LogUtils.d("bleService", mac + ">>>" + rssi);
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret", IntentExtras.RET.RET_BLE_READ_RSSI);
                intent.putExtra("address", mac);
                intent.putExtra("rssi", rssi);
                App.getInstance().getLocalBroadCastManager().sendBroadcast(intent);
                mapRssi.put(mac, rssi);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onCharacteristicChanged(String mac, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(mac, characteristic);
                if (characteristic != null && characteristic.getUuid().equals(BleUUIDS.userCharactButtonStateUUID)) {
                    byte[] data = characteristic.getValue();
                    LogUtils.d("bleService", "监测到" + mac + "点击事件(" + data[0] + ")--->报警广播数据");
                    if (data[0] == 02) {
//                                if (!Common.isBell()) {
//                                    LogUtils.d("贴片在勿扰范围内,报警忽略!!");
//                                    return;
//                                }
                        DeviceInfo deviceInfo = (DeviceInfo) SPUtils.getInstance().readObject(mac);
                        if (deviceInfo != null) {
                            doPlay(deviceInfo);
                        }

                    }
                } else if (characteristic != null && characteristic.getUuid().equals(BleUUIDS.batCharacteristicUUID)) {
                    byte[] data = characteristic.getValue();
                    LogUtils.d("bleService", "监测到" + mac + "电池电量" + data[0] + "%");
                    Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                    intent.putExtra("ret", IntentExtras.RET.RET_BLE_READ_BATTERY);
                    intent.putExtra("address", mac);
                    intent.putExtra("battery", data[0] + "");
                    App.getInstance().getLocalBroadCastManager().sendBroadcast(intent);
                    mapBattery.put(mac, data[0] + "");

                }
            }

            @Override
            public void onBindModeSuccess(BluetoothDevice device) {
                LogUtils.d("bleService","贴片设置绑定模式成功----->"+device.getAddress());
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret", IntentExtras.RET.RET_BLE_MODE_BIND_SUCCESS);
                intent.putExtra("address", device.getAddress());
                App.getInstance().getLocalBroadCastManager().sendBroadcast(intent);
                mBindMap.clear();
                if(isBind) {
                    BleService.this.isBind = false;
                }
                if(!mDeviceMap.containsKey(device.getAddress())){
                    mDeviceMap.put(device.getAddress(),device);
                }
                mNotFirstConnect.put(device.getAddress(),true);
            }

            @Override
            public void onBindModeFaild(BluetoothDevice device) {
                LogUtils.d("bleService","贴片设置绑定模式失败----->"+device.getAddress());
                isIgnoreEvents.put(device.getAddress(),true);
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret", IntentExtras.RET.RET_BLE_MODE_BIND_FAIL);
                intent.putExtra("address", device.getAddress());
                App.getInstance().getLocalBroadCastManager().sendBroadcast(intent);
            }

            @Override
            public void onWorkModeSuccess(BluetoothDevice device) {
                LogUtils.d("bleService","贴片设置工作模式成功(isNeedAlerts-->"+mNotFirstConnect.get(device.getAddress())+")----->"+device.getAddress());
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret", IntentExtras.RET.RET_BLE_MODE_WORK_SUCCESS);
                intent.putExtra("address", device.getAddress());
                App.getInstance().getLocalBroadCastManager().sendBroadcast(intent);

                if(!mDeviceMap.containsKey(device.getAddress())) {
                    return;
                }
                final DeviceInfo deviceInfo = (DeviceInfo) SPUtils.getInstance().readObject(device.getAddress());
                if (deviceInfo == null||deviceInfo.getAlertinfo()==null) {
                    return;
                }
                DeviceInfo.AlertinfoBean alertinfoBean = deviceInfo.getAlertinfo();
                if(mNotFirstConnect.containsKey(device.getAddress())&&mNotFirstConnect.get(device.getAddress())) {
                    if(isDevicesNeedAlert.containsKey(device.getAddress())&&isDevicesNeedAlert.get(device.getAddress())) {
                        if(alertinfoBean.getIsReconnect() == 1 && Common.isBell()){
                            BingGoPlayUtils.playBingGo();
                        }
                        Common.doCommitEvents(App.getInstance(), device.getAddress(), Params.EVENT_TYPE.DEVICE_REDISCOVER);
                    }

                }else{
                    mNotFirstConnect.put(device.getAddress(),true);
                    BingGoPlayUtils.playBingGo();
                    mLocationClient.requestLocation();
                    Common.doCommitEvents(App.getInstance(), device.getAddress(), Params.EVENT_TYPE.DEVICE_CONNECT);
                }
            }

            @Override
            public void onWorkModeFaild(BluetoothDevice device) {
                LogUtils.d("bleService","贴片设置工作模式失败----->"+device.getAddress());
                isIgnoreEvents.put(device.getAddress(),true);
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret", IntentExtras.RET.RET_BLE_MODE_WORK_FAIL);
                intent.putExtra("address", device.getAddress());
                App.getInstance().getLocalBroadCastManager().sendBroadcast(intent);
            }

            @Override
            public void onCancelAlert(String mac) {
                isDevicesNeedAlert.put(mac,false);
            }
        });
        if(isBind){
            connectDeviceTask.setPriority(Priority.Immediately);
        }else{
            connectDeviceTask.setPriority(Priority.LOW);
        }
        taskQueue.add(connectDeviceTask);
    }

    private void exceCalibrationDevice() {
        if(TextUtils.isEmpty(Common.getTocken())){
            return;
        }
        DeviceListResponse deviceResponse = (DeviceListResponse) SPUtils.getInstance().readObject(SPConst.DATA.DEVICELIST);
        if(deviceResponse!=null&&deviceResponse.getDevlist()!=null){
            for (DeviceInfo devlistBean : deviceResponse.getDevlist()){
                if(TextUtils.isEmpty(devlistBean.getFriendname())) {
                    String mac = Common.formatDevId2Mac(devlistBean.getDevidX());
                    if (!mDeviceMap.containsKey(mac)) {
                        mDeviceMap.put(mac, null);
                    }else{
                        BluetoothDevice bleDevice = mDeviceMap.get(mac);
                        if(bleDevice!=null&&!BleClient.getInstance().isConnected(bleDevice.getAddress())){
                            BleClient.getInstance().disConnect(bleDevice.getAddress());
                        }
                    }
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
        SimpleArrayMap<String,BluetoothDevice> tempMap = new SimpleArrayMap<>();
        tempMap.putAll(mDeviceMap);
        for (int i=0;i<tempMap.size();i++) {
            if(!macList.contains(tempMap.keyAt(i))){
                mDeviceMap.remove(tempMap.keyAt(i));
                if(tempMap.valueAt(i)!=null) {
                    BleClient.getInstance().disConnect(tempMap.valueAt(i).getAddress());
                }
            }
        }
    }

    public synchronized  void scanDevice(){
        if(TextUtils.isEmpty(Common.getTocken())||BleClient.getInstance().isScaning()){
            return;
        }
        LogUtils.d("bleService","贴片扫描开始----->");
        Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
        intent.putExtra("ret",IntentExtras.RET.RET_BLE_SCAN_START);
        App.getInstance().getLocalBroadCastManager().sendBroadcast(intent);
        lastUnScanning = TimeUtils.getNowMills();
        BleClient.getInstance().startScan(new ScanResultCallback(){
            @Override
            public synchronized void onScaning(ScanResult scanResult) {
                lastUnScanning = TimeUtils.getNowMills();
                if(scanResult==null||TextUtils.isEmpty(scanResult.getName())||!scanResult.getName().contains("yoowoo")){
                    return;
                }
                LogUtils.d("bleService", "扫描有物贴片----->" + scanResult.getMac() +"  rssi:"+scanResult.getRssi());
                BleClient.getInstance().scanning(scanResult.getMac());
                if(!mDeviceMap.containsKey(scanResult.getMac())){
                    if(isBind&&scanResult.getRssi()>-50){
                        if(mBindMap.size()==0) {
                            mBindMap.put(scanResult.getMac(),scanResult.getDevice());
                            LogUtils.d("bleService", "扫描到并开始绑定贴片----->" + scanResult.getMac());
                            Intent intent1 = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                            intent1.putExtra("ret", IntentExtras.RET.RET_BLE_FIND_BIND_DEVICE);
                            intent1.putExtra("address", scanResult.getMac());
                            App.getInstance().getLocalBroadCastManager().sendBroadcast(intent1);
                        }
                        return;
                    }
                }else {
                    LogUtils.d("bleService", "扫描到并开始连接贴片----->" + scanResult.getMac()+"   rssi:"+scanResult.getRssi());
                    if(scanResult.getRssi()>-90){
                        mDeviceMap.put(scanResult.getMac(), scanResult.getDevice());
                        Intent intent2 = new Intent(IntentExtras.ACTION.ACTION_BLE_COMMAND);
                        intent2.putExtra("cmd", IntentExtras.CMD.CMD_DEVICE_SCANING);
                        intent2.putExtra("address", scanResult.getMac());
                        App.getInstance().getLocalBroadCastManager().sendBroadcast(intent2);
                    }
                }
            }
        });
    }

    private synchronized void unbindDevice(final String address) {
        if(TextUtils.isEmpty(address)||!BleClient.getInstance().isConnected(address)){
            return;
        }
        byte[] dataWork = Common.formatBleMsg(Params.BLEMODE.MODE_UNBIND, Common.getUserId());
        BleClient.getInstance().write(address, userServiceUUID, userCharacteristicLogUUID, dataWork, new BleWriteCallback() {
            @Override
            public void onWriteSuccess(BluetoothDevice device) {
                LogUtils.d("bleService","贴片解绑成功----->"+device.getAddress());
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret", IntentExtras.RET.RET_BLE_UNBIND_COMPLETE);
                intent.putExtra("address", device.getAddress());
                App.getInstance().getLocalBroadCastManager().sendBroadcast(intent);
                isIgnoreEvents.put(device.getAddress(),true);
                BleClient.getInstance().disConnect(device.getAddress());
                if(mDeviceMap.containsKey(device.getAddress())){
                    mDeviceMap.remove(device.getAddress());
                }
                if(mLinkLoseMap.containsKey(device.getAddress())){
                    mLinkLoseMap.remove(device.getAddress());
                }
                mBindMap.clear();
                isBind=false;
            }

            @Override
            public void onWriteFialed(BluetoothDevice device) {
                LogUtils.d("bleService","贴片解绑失败----->"+device.getAddress());
                unbindDevice(device.getAddress());
            }
        });
    }
    private synchronized void clearAllConnect(){
        LogUtils.d("bleService","clearAllConnect");
        BleClient.getInstance().disconnectAllDevice();
        mDeviceMap.clear();
        mBindMap.clear();
        isBind=false;
    }
    private synchronized void diableBluetooth(){
        LogUtils.d("bleService","diableBluetooth");
        BleClient.getInstance().diableBluetooth();
        mDeviceMap.clear();
        mBindMap.clear();
        isBind=false;
    }
    private synchronized void doLogOut(){
        LogUtils.d("bleService","doLogOut");
        BleClient.getInstance().cancelScan();
        byte[] data = new byte[]{0x00};
        BleClient.getInstance().disconnectAllWithLinklose(data);
        mDeviceMap.clear();
        mBindMap.clear();
        mNotFirstConnect.clear();
        mNotFirstDisConnect.clear();
        isBind=false;
    }

    private synchronized void startAlert(String address) {
        BluetoothDevice bleDevice = mDeviceMap.get(address);
        if(bleDevice==null||!BleClient.getInstance().isConnected(bleDevice.getAddress())){
            return;
        }
        byte[] options = new byte[]{0x02};
        BleClient.getInstance().write(bleDevice.getAddress(), serviceUUID, characteristicUUID, options, new BleWriteCallback() {
            @Override
            public void onWriteSuccess(BluetoothDevice device) {
                mAlertingMap.put(device.getAddress(),true);
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                LogUtils.d("bleService","贴片开始报警----->"+device.getAddress());
                intent.putExtra("ret", IntentExtras.RET.RET_BLE_ALERT_STARTED);
                intent.putExtra("address", device.getAddress());
                App.getInstance().getLocalBroadCastManager().sendBroadcast(intent);
            }

            @Override
            public void onWriteFialed(BluetoothDevice device) {
                LogUtils.d("bleService","贴片开始报警---->失败了----->"+device.getAddress());
            }
        });
    }
    private synchronized void stopAlert(String address) {
        BluetoothDevice bleDevice = mDeviceMap.get(address);
        if(bleDevice==null||!BleClient.getInstance().isConnected(bleDevice.getAddress())){
            return;
        }
        byte[] options = new byte[]{0x00};
        BleClient.getInstance().write(bleDevice.getAddress(), serviceUUID, characteristicUUID, options, new BleWriteCallback() {
            @Override
            public void onWriteSuccess(BluetoothDevice device) {
                mAlertingMap.put(device.getAddress(),false);
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                LogUtils.d("bleService","贴片结束报警----->"+device.getAddress());
                intent.putExtra("ret", IntentExtras.RET.RET_BLE_ALERT_STOPED);
                intent.putExtra("address", device.getAddress());
                App.getInstance().getLocalBroadCastManager().sendBroadcast(intent);
            }

            @Override
            public void onWriteFialed(BluetoothDevice device) {
                LogUtils.d("bleService","贴片结束报警--->失败了----->"+device.getAddress());
            }
        });
    }

    private void doPlay(DeviceInfo deviceInfo) {
        String url = deviceInfo.getAlertinfo().getUrlX();
        boolean isShake = (deviceInfo.getAlertinfo().getIsShake()==1)?true:false;
        int duration = deviceInfo.getAlertinfo().getDuration();
        duration*=1000;
        if(url.contains("bluesforslim")){
            SoundPlayUtils.play(3,duration,isShake,Common.decodeBase64(deviceInfo.getDevname()));
        }else if(url.contains("countryfair")){
            SoundPlayUtils.play(4,duration,isShake,Common.decodeBase64(deviceInfo.getDevname()));
        }else if(url.contains("theclassiccall")){
            SoundPlayUtils.play(2,duration,isShake,Common.decodeBase64(deviceInfo.getDevname()));
        }else{
            SoundPlayUtils.play(1,duration,isShake,Common.decodeBase64(deviceInfo.getDevname()));
        }
    }

    public class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location){
            LogUtils.d("bleService","接收到定位信息----->"+location.getLatitude()+","+location.getLongitude());
            Locinfo locinfo = new Locinfo();
            locinfo.setCity(location.getCity());
            locinfo.setAddr(location.getAddrStr());
            locinfo.setLat(location.getLatitude());
            locinfo.setLng(location.getLongitude());

            SPUtils.getInstance().remove(SPConst.LOC_INFO);
            SPUtils.getInstance().saveObject(SPConst.LOC_INFO,locinfo);

            Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
            intent.putExtra("ret", IntentExtras.RET.LOCATION);
            intent.putExtra("data", locinfo);
            App.getInstance().getLocalBroadCastManager().sendBroadcast(intent);

//            Intent task= new Intent(IntentExtras.ACTION.ACTION_BLE_COMMAND);
//            task.putExtra("cmd",IntentExtras.CMD.CMD_TASK_EXCE);
//            sendBroadcast(task);
//            exceTask();
//            }
            LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
            long curMillis = TimeUtils.getNowMills();
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
        DeviceListResponse response = (DeviceListResponse) SPUtils.getInstance().readObject(SPConst.DATA.DEVICELIST);
        if(response==null){
            return;
        }
        List<DeviceInfo> mlist = response.getDevlist();
        if(mlist==null||mlist.size()<=0){
            return;
        }
        final List<Map> devlist = new ArrayList<>();
        for (DeviceInfo devlistBean : mlist){
            String mac = Common.formatDevId2Mac(devlistBean.getDevidX());
            final Map devItem = new HashMap();
            devItem.put("devid", devlistBean.getDevidX());
            devItem.put("usedstate", devlistBean.getUsedstate());
            devItem.put("bindstate", 1);
            if(!TextUtils.isEmpty(devlistBean.getFriendname())){
                devItem.put("sourceid", 0);
                devItem.put("devbattery",0);
                devItem.put("devposstate", 0);
                devItem.put("devstate", 0);
            }else{
                devItem.put("sourceid", devlistBean.getFriendid());
                if(mDeviceMap.containsKey(mac)&&mDeviceMap.get(mac)!=null) {
                    BluetoothDevice bleDevice = mDeviceMap.get(mac);
                    if (BleClient.getInstance().isConnected(bleDevice.getAddress())) {
                        devItem.put("devstate", 1);
                        if (mapBattery.containsKey(mac)) {
                            devItem.put("devbattery", mapBattery.get(mac));
                        } else {
                            continue;
                        }
                        if (mapRssi.containsKey(mac)) {
                            int rssi = mapRssi.get(mac);
                            if (rssi < -85) {
                                devItem.put("devposstate", 3);
                            } else if (rssi > -85 && rssi < -70) {
                                devItem.put("devposstate", 2);
                            } else if (rssi > -70) {
                                devItem.put("devposstate", 1);
                            }
                        } else {
                            continue;
                        }
                    } else {
                        devItem.put("devbattery", 0);
                        devItem.put("devposstate", 100);
                        devItem.put("devstate", -99);
                    }
                }else{
                    continue;
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
        repdata.put("reptime", TimeUtils.millis2String(millis, new SimpleDateFormat("yyyyMMddHHmmss")));
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
        if(!NetworkUtils.isConnected()){
            return;
        }
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