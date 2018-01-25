package com.miittech.you.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.util.SimpleArrayMap;
import android.text.TextUtils;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.utils.DistanceUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.miittech.you.App;
import com.miittech.you.ble.BleClient;
import com.miittech.you.ble.BleWriteCallback;
import com.miittech.you.ble.GattCallback;
import com.miittech.you.ble.ScanResult;
import com.miittech.you.ble.ScanResultCallback;
import com.miittech.you.global.BleUUIDS;
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
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import static com.miittech.you.global.BleUUIDS.characteristicUUID;
import static com.miittech.you.global.BleUUIDS.serviceUUID;
import static com.miittech.you.global.BleUUIDS.userCharacteristicLogUUID;
import static com.miittech.you.global.BleUUIDS.userServiceUUID;

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
    private SimpleArrayMap<String,Boolean> mNotFirstDisConnect = new SimpleArrayMap<>();
    private List<String> isConnecttingMacs = new ArrayList<>();
    private boolean isBind = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        new Thread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                BleClient.getInstance().cancelScan();
                e.printStackTrace();
            }
        });
        checkLocationService();
        cmdReceiver = new CmdReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(IntentExtras.ACTION.ACTION_BLE_COMMAND);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        getApplicationContext().registerReceiver(cmdReceiver, filter);
        LogUtils.d("bleService-onStartCommand()-new Thread");
        scanDevice();
    }

    private synchronized void checkLocationService() {
        if(mLocationClient==null) {
            mLocationClient = new LocationClient(getApplicationContext());
            mLocationClient.registerLocationListener(myListener);
            LocationClientOption option = new LocationClientOption();
            option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
            option.setCoorType("bd09ll");
            option.setScanSpan(5000);
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
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.d("bleService","bleService-onStartCommand()");
        AlarmManager aManager=(AlarmManager)getSystemService(Service.ALARM_SERVICE);
        Intent intent1 = new Intent(IntentExtras.ACTION.ACTION_TASK_SEND);
        PendingIntent pi=PendingIntent.getBroadcast(this, 0, intent1, PendingIntent.FLAG_CANCEL_CURRENT);
        aManager.setWindow(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+10000,5000, pi);
//        aManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,System.currentTimeMillis()+5000,5000,pi);
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtils.d("bleService-onDestroy()");
        this.unregisterReceiver(cmdReceiver);
        BleClient.getInstance().cancelScan();
        BleClient.getInstance().disconnectAllDevice();

        AlarmManager aManager=(AlarmManager)getSystemService(Service.ALARM_SERVICE);
        Intent intent1 = new Intent(IntentExtras.ACTION.ACTION_TASK_SEND);
        PendingIntent pi=PendingIntent.getBroadcast(this, 0, intent1, PendingIntent.FLAG_CANCEL_CURRENT);
        aManager.cancel(pi);

        if(mLocationClient!=null){
            mLocationClient.stop();
            mLocationClient=null;
        }

        mDeviceMap.clear();
        mapRssi.clear();
        mapBattery.clear();
        mBindMap.clear();
        mLinkLoseMap.clear();
        isConnecttingMacs.clear();
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
                            connectDevice(mDeviceMap.get(intent.getStringExtra("address")));
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
                            connectDevice(bleDevice);
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
//                        case IntentExtras.CMD.CMD_TASK_EXCE:
//                            stringBuilder.append("CMD_TASK_EXCE");
//                            exceTask();
//                            break;
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
                            sendBroadcast(bleOnIntent);
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
                            sendBroadcast(bleOffIntent);
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
        if(BleClient.getInstance().isScaning()) {
            if (lastUnScanning != 0 && TimeUtils.getTimeSpanByNow(lastUnScanning, TimeConstants.MIN) > 10) {
                lastUnScanning = 0;
                BleClient.getInstance().cancelScan();
                LogUtils.d("bleservice","exceCheckScaning()-->cancelScan()");
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
                connectDevice(bleDevice);
            }
        }
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
        sendBroadcast(intent);
        lastUnScanning = TimeUtils.getNowMills();
        BleClient.getInstance().startScan(new ScanResultCallback(){
            @Override
            public synchronized void onScaning(ScanResult scanResult) {
                lastUnScanning = TimeUtils.getNowMills();
                if(scanResult==null||TextUtils.isEmpty(scanResult.getName())||!scanResult.getName().contains("yoowoo")){
                    return;
                }
//                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
//                intent.putExtra("ret",IntentExtras.RET.RET_BLE_SCANING);
//                intent.putExtra("address",scanResult.getMac());
//                sendBroadcast(intent);
                if(!mDeviceMap.containsKey(scanResult.getMac())){
                    if(isBind&&scanResult.getRssi()>-50){
                        if(mBindMap.size()==0) {
                            mBindMap.put(scanResult.getMac(),scanResult.getDevice());
                            LogUtils.d("bleService", "扫描到并开始绑定贴片----->" + scanResult.getMac());
                            Intent intent1 = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                            intent1.putExtra("ret", IntentExtras.RET.RET_BLE_FIND_BIND_DEVICE);
                            intent1.putExtra("address", scanResult.getMac());
                            sendBroadcast(intent1);
                        }
                        return;
                    }
                }else {
                    LogUtils.d("bleService","扫描到并开始连接贴片----->"+scanResult.getMac());
                    mDeviceMap.put(scanResult.getMac(), scanResult.getDevice());
                    Intent intent2 = new Intent(IntentExtras.ACTION.ACTION_BLE_COMMAND);
                    intent2.putExtra("cmd", IntentExtras.CMD.CMD_DEVICE_SCANING);
                    intent2.putExtra("address", scanResult.getMac());
                    sendBroadcast(intent2);
                }
            }
        });
    }
    public synchronized void connectDevice(BluetoothDevice bleDevice){
        synchronized (isConnecttingMacs) {
            if (TextUtils.isEmpty(Common.getTocken())) {
                    return;
            }
            if (bleDevice == null || (!mDeviceMap.containsKey(bleDevice.getAddress())
                    && !mBindMap.containsKey(bleDevice.getAddress())
                    && !isConnecttingMacs.contains(bleDevice.getAddress()))) {
                return;
            }
            if (BleClient.getInstance().getConnectState(bleDevice) == BluetoothGatt.STATE_DISCONNECTED) {
                BleClient.getInstance().connectDevice(bleDevice, new GattCallback() {
                    @Override
                    public synchronized boolean onStartConnect(String mac) {
                        LogUtils.d("bleService", "贴片开始连接----->" + mac);
                        Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                        intent.putExtra("ret", IntentExtras.RET.RET_BLE_CONNECT_START);
                        intent.putExtra("address", mac);
                        sendBroadcast(intent);
                        if(isConnecttingMacs.contains(mac)){
                            return false;
                        }else {
                            isConnecttingMacs.add(mac);
                            return true;
                        }
                    }

                    @Override
                    public synchronized void onConnectFail(String mac) {
                        LogUtils.d("bleService", "贴片连接失败----->" + mac);
                        if(isConnecttingMacs.contains(mac)){
                            isConnecttingMacs.remove(mac);
                        }
                        Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                        intent.putExtra("ret", IntentExtras.RET.RET_BLE_CONNECT_FAILED);
                        intent.putExtra("address", mac);
                        sendBroadcast(intent);
                    }

                    @Override
                    public synchronized void onConnectSuccess(String mac,int status) {
                        LogUtils.d("bleService", "贴片连接成功----->" + mac);
                        Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                        intent.putExtra("ret", IntentExtras.RET.RET_BLE_CONNECT_SUCCESS);
                        intent.putExtra("address", mac);
                        sendBroadcast(intent);
                        if (mBindMap.containsKey(mac)) {
                            setBindMode(mac);
                        } else if (mDeviceMap.containsKey(mac)) {
                            setWorkMode(mac);
                        }
                    }

                    @Override
                    public synchronized void onDisConnected(boolean isActiveDisConnected, final String mac, int status) {
                        LogUtils.d("bleService", "贴片连接断开----->" + mac + "   isActiveDisConnected--->" + isActiveDisConnected);
                        Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                        intent.putExtra("ret", IntentExtras.RET.RET_BLE_DISCONNECT);
                        intent.putExtra("address", mac);
                        sendBroadcast(intent);
                        if (isConnecttingMacs.contains(mac)) {
                            isConnecttingMacs.remove(mac);
                        }
                        if (mLinkLoseMap.containsKey(mac)) {
                            mLinkLoseMap.remove(mac);
                        }
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
                            if (isIgnoreEvents.containsKey(mac) && isIgnoreEvents.get(mac)) {
                                isIgnoreEvents.remove(mac);
                            } else {
                                Common.doCommitEvents(App.getInstance(), mac, Params.EVENT_TYPE.DEVICE_LOSE);
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
                        sendBroadcast(intent);
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
                        if (characteristic != null && characteristic.getUuid().toString().equals(BleUUIDS.userCharactButtonStateUUID)) {
                            byte[] data = characteristic.getValue();
                            LogUtils.d("bleService", "监测到" + mac + "点击事件(" + data[0] + ")--->报警广播数据");
                            if (data[0] == 02) {
                                if (!Common.isBell()) {
                                    LogUtils.d("贴片在勿扰范围内,报警忽略!!");
                                    return;
                                }
                                DeviceInfo deviceInfo = (DeviceInfo) SPUtils.getInstance().readObject(mac);
                                if (deviceInfo != null) {
                                    if (Common.isBell()) {
                                        doPlay(deviceInfo);
                                    }
                                }

                            }
                        } else if (characteristic != null && characteristic.getUuid().toString().equals(BleUUIDS.batCharacteristicUUID)) {
                            byte[] data = characteristic.getValue();
                            LogUtils.d("bleService", "监测到" + mac + "电池电量" + data[0] + "%");
                            Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                            intent.putExtra("ret", IntentExtras.RET.RET_BLE_READ_BATTERY);
                            intent.putExtra("address", mac);
                            intent.putExtra("battery", data[0] + "");
                            sendBroadcast(intent);
                            mapBattery.put(mac, data[0] + "");

                        }
                    }

                    @Override
                    public void onCharacteristicWrite(String mac, BluetoothGattCharacteristic characteristic, int status) {
                        super.onCharacteristicWrite(mac, characteristic, status);
                        LogUtils.d("bleService", "onCharacteristicWrite:" + mac + "    status:" + status);
                    }
                });
            }
        }

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
                sendBroadcast(intent);
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
        isConnecttingMacs.clear();
        isBind=false;
    }
    private synchronized void diableBluetooth(){
        LogUtils.d("bleService","diableBluetooth");
        BleClient.getInstance().diableBluetooth();
        mDeviceMap.clear();
        mBindMap.clear();
        isConnecttingMacs.clear();
        isBind=false;
    }
    private synchronized void doLogOut(){
        LogUtils.d("bleService","doLogOut");
        BleClient.getInstance().cancelScan();
        byte[] data = new byte[]{0x00};
        BleClient.getInstance().disconnectAllWithLinklose(data);
        mDeviceMap.clear();
        mBindMap.clear();
        isConnecttingMacs.clear();
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
                sendBroadcast(intent);
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
                sendBroadcast(intent);
            }

            @Override
            public void onWriteFialed(BluetoothDevice device) {
                LogUtils.d("bleService","贴片结束报警--->失败了----->"+device.getAddress());
            }
        });
    }

    public synchronized void setWorkMode(final String mac){
            if(TextUtils.isEmpty(mac)||!BleClient.getInstance().isConnected(mac)||!mDeviceMap.containsKey(mac)){
                return;
            }
            byte[] dataWork = Common.formatBleMsg(Params.BLEMODE.MODE_WORK, Common.getUserId());
            BleClient.getInstance().write(mac, userServiceUUID, userCharacteristicLogUUID, dataWork, new BleWriteCallback() {
                @Override
                public void onWriteSuccess(final BluetoothDevice device) {
                    LogUtils.d("bleService","贴片设置工作模式成功(isNeedAlerts-->"+mNotFirstConnect.get(device.getAddress())+")----->"+device.getAddress());
                    Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                    intent.putExtra("ret", IntentExtras.RET.RET_BLE_MODE_WORK_SUCCESS);
                    intent.putExtra("address", device.getAddress());
                    sendBroadcast(intent);
                    if(!mDeviceMap.containsKey(device.getAddress())) {
                        return;
                    }
                    final DeviceInfo deviceInfo = (DeviceInfo) SPUtils.getInstance().readObject(device.getAddress());
                    if (deviceInfo == null||deviceInfo.getAlertinfo()==null) {
                        return;
                    }
                    DeviceInfo.AlertinfoBean alertinfoBean = deviceInfo.getAlertinfo();
                    if(mNotFirstConnect.containsKey(device.getAddress())&&mNotFirstConnect.get(device.getAddress())) {

                        if(alertinfoBean.getIsReconnect() == 1 && Common.isBell()){
                            BingGoPlayUtils.playBingGo();
                        }
                        Common.doCommitEvents(App.getInstance(), mac, Params.EVENT_TYPE.DEVICE_REDISCOVER);
                    }else{
                        mNotFirstConnect.put(device.getAddress(),true);
                        BingGoPlayUtils.playBingGo();
                        mLocationClient.requestLocation();
                        Common.doCommitEvents(App.getInstance(), mac, Params.EVENT_TYPE.DEVICE_CONNECT);
                    }
                }

                @Override
                public void onWriteFialed(BluetoothDevice device) {
                    LogUtils.d("bleService","贴片设置工作模式失败----->"+device.getAddress());
                    Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                    intent.putExtra("ret", IntentExtras.RET.RET_BLE_MODE_WORK_FAIL);
                    intent.putExtra("address", device.getAddress());
                    sendBroadcast(intent);
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

    public synchronized void setBindMode(String mac){
        if(TextUtils.isEmpty(mac)||!BleClient.getInstance().isConnected(mac)
                ||!mBindMap.containsKey(mac)){
            return;
        }
        byte[] bind = Common.formatBleMsg(Params.BLEMODE.MODE_BIND,Common.getUserId());
        BleClient.getInstance().write(mac, BleUUIDS.userServiceUUID, BleUUIDS.userCharacteristicLogUUID, bind, new BleWriteCallback() {
            @Override
            public void onWriteSuccess(BluetoothDevice device) {
                LogUtils.d("bleService","贴片设置绑定模式成功----->"+device.getAddress());
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret", IntentExtras.RET.RET_BLE_MODE_BIND_SUCCESS);
                intent.putExtra("address", device.getAddress());
                sendBroadcast(intent);
                mBindMap.clear();
                isBind =false;
                if(!mDeviceMap.containsKey(device.getAddress())){
                    mDeviceMap.put(device.getAddress(),device);
                }
                mNotFirstConnect.put(device.getAddress(),true);
            }

            @Override
            public void onWriteFialed(BluetoothDevice device) {
                LogUtils.d("bleService","贴片设置绑定模式失败----->"+device.getAddress());
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret", IntentExtras.RET.RET_BLE_MODE_BIND_FAIL);
                intent.putExtra("address", device.getAddress());
                sendBroadcast(intent);
            }
        });
    }

    public class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location){
            LogUtils.d("bleService","接收到定位信息----->"+location.getLatitude()+","+location.getLongitude());
//            if(!TextUtils.isEmpty(location.getAddrStr())){
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
            sendBroadcast(intent);

//            Intent task= new Intent(IntentExtras.ACTION.ACTION_BLE_COMMAND);
//            task.putExtra("cmd",IntentExtras.CMD.CMD_TASK_EXCE);
//            sendBroadcast(task);
            exceTask();

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