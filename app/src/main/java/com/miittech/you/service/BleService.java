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
import android.os.IBinder;
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
import com.miittech.you.ble.BleClient;
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
    CmdReceiver cmdReceiver;
    private Map<String, Integer> mapRssi = new HashMap<String, Integer>();
    private Map<String,String> mapBattery = new HashMap<>();
    private BDLocation lastLocation;
    private Map<String,BluetoothDevice> mDeviceMap = new HashMap<>();
    private Map<String,BluetoothDevice> mBindMap = new HashMap<>();
    private Map<String,Boolean> mAlertingMap = new HashMap<>();
    private Map<String,Byte> mLinkLoseMap = new HashMap<>();
    private List<String> mConnectMac=new ArrayList<>();
    private Map<String,Boolean> mNotFirstConnect = new HashMap<>();
    private Map<String,Boolean> mNotFirstDisConnect = new HashMap<>();
    private boolean isBind = false;

    @Override  
    public IBinder onBind(Intent intent) {
        return null;
    }  
  
    @Override  
    public void onCreate() {
        super.onCreate();
        BleClient.getInstance().init(App.getInstance().getApplicationContext());
        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(myListener);
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setCoorType("bd09ll");
        option.setScanSpan(5000);
        option.setOpenGps(true);
        option.setIsNeedAddress(true);
        option.setIgnoreKillProcess(true);
        option.setWifiCacheTimeOut(5*60*1000);
        mLocationClient.setLocOption(option);
        mLocationClient.start();
        mLocationClient.requestLocation();

        cmdReceiver = new CmdReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(IntentExtras.ACTION.ACTION_BLE_COMMAND);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        getApplicationContext().registerReceiver(cmdReceiver, filter);
        LogUtils.d("bleService-OnCreate()");
        scanDevice();
    }
      

    @Override  
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.d("bleService-onStartCommand()");
        scanDevice();
        AlarmManager aManager=(AlarmManager)getSystemService(Service.ALARM_SERVICE);
        Intent intent1 = new Intent(IntentExtras.ACTION.ACTION_TASK_SEND);
        PendingIntent pi=PendingIntent.getBroadcast(this, 0, intent1, PendingIntent.FLAG_CANCEL_CURRENT);
        aManager.setWindow(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+10000,5000, pi);
        return START_REDELIVER_INTENT;
    }
  
    @Override  
    public void onDestroy() {
        super.onDestroy();
        LogUtils.d("bleService-onDestroy()");
        this.unregisterReceiver(cmdReceiver);
        BleClient.getInstance().cancelScan();
        BleClient.getInstance().disconnectAllDevice();
        mConnectMac.clear();
        mDeviceMap.clear();
        mapRssi.clear();
        mapBattery.clear();
        mBindMap.clear();
        mLinkLoseMap.clear();
        mNotFirstConnect.clear();
        mNotFirstDisConnect.clear();
        if(mLocationClient!=null){
            mLocationClient.stop();
        }
    }

    StringBuilder stringBuilder = new StringBuilder();
    class CmdReceiver extends BroadcastReceiver {
        @Override
        public synchronized void onReceive(Context context, Intent intent) {
            stringBuilder.delete(0,stringBuilder.length());
            stringBuilder.append("收到广播 cmd==>");
            if(intent.getAction().equals(IntentExtras.ACTION.ACTION_BLE_COMMAND)){
                int cmd = intent.getIntExtra("cmd", -1);//获取Extra信息
                switch (cmd) {
                    case IntentExtras.CMD.CMD_DEVICE_LIST_ADD:
                        stringBuilder.append("CMD_DEVICE_LIST_ADD   macList==>"+intent.getStringArrayListExtra("macList"));
                        addDeviceList(intent.getStringArrayListExtra("macList"));
                        break;
                    case IntentExtras.CMD.CMD_DEVICE_SCANING:
                        stringBuilder.append("CMD_DEVICE_SCANING   mac==>"+intent.getStringExtra("address"));
                        connectDevice(mDeviceMap.get(intent.getStringExtra("address")));
                        break;
                    case IntentExtras.CMD.CMD_DEVICE_BIND_SCAN:
                        stringBuilder.append("CMD_DEVICE_BIND_SCAN");
                        isBind = true;
                        mBindMap.clear();
                        break;
                    case IntentExtras.CMD.CMD_DEVICE_UNBIND_ERROR:
                        stringBuilder.append("CMD_DEVICE_UNBIND_ERROR   mac==>"+intent.getStringExtra("address"));
                        isBind=false;
                        String address = intent.getStringExtra("address");
                        if(mDeviceMap.containsKey(address)){
                            mDeviceMap.remove(address);
                        }
                        if(mBindMap.containsKey(address)){
                            mBindMap.remove(address);
                        }
                        break;
                    case IntentExtras.CMD.CMD_DEVICE_CONNECT_BIND:
                        stringBuilder.append("CMD_DEVICE_CONNECT_BIND   mac==>"+intent.getStringExtra("address"));
                        BluetoothDevice bleDevice = mBindMap.get(intent.getStringExtra("address"));
                        connectDevice(bleDevice);
                        break;
                    case IntentExtras.CMD.CMD_DEVICE_ALERT_START:
                        stringBuilder.append("CMD_DEVICE_ALERT_START   mac==>"+intent.getStringExtra("address"));
                        startAlert(intent.getStringExtra("address"));
                        break;
                    case IntentExtras.CMD.CMD_DEVICE_ALERT_STOP:
                        stringBuilder.append("CMD_DEVICE_ALERT_STOP   mac==>"+intent.getStringExtra("address"));
                        stopAlert(intent.getStringExtra("address"));
                        break;
                    case IntentExtras.CMD.CMD_DEVICE_UNBIND:
                        stringBuilder.append("CMD_DEVICE_UNBIND   mac==>"+intent.getStringExtra("address"));
                        unbindDevice(intent.getStringExtra("address"));
                        break;
                    case IntentExtras.CMD.CMD_DEVICE_LIST_CLEAR:
                        stringBuilder.append("CMD_DEVICE_LIST_CLEAR");
                        doLogOut();
                        break;
                    case IntentExtras.CMD.CMD_TASK_EXCE:
                        stringBuilder.append("CMD_TASK_EXCE");
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                exceTask();
                            }
                        }).start();
                        break;
                }
            }else if(intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
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
                        Intent bleOffIntent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                        bleOffIntent.putExtra("ret", IntentExtras.RET.RET_BLE_STATE_OFF);
                        sendBroadcast(bleOffIntent);
                        BleClient.getInstance().cancelScan();
                        clearAllConnect();
                        break;
                }
            }
            LogUtils.d("bleService",stringBuilder.toString());
        }
    }

    private synchronized void exceTask() {
        exceReportSubmit();
        exceCalibrationDevice();
        for (Map.Entry<String,BluetoothDevice> entry:mDeviceMap.entrySet()){
            BluetoothDevice bleDevice = entry.getValue();
            if(bleDevice==null){
                continue;
            }
            if(BleClient.getInstance().isConnected(bleDevice.getAddress())){
                BleClient.getInstance().readRssi(bleDevice.getAddress());
                DeviceInfo deviceInfo = (DeviceInfo) SPUtils.getInstance().readObject(bleDevice.getAddress());
                if(deviceInfo!=null) {
                    if(mAlertingMap.containsKey(Common.formatDevId2Mac(deviceInfo.getDevidX()))&&mAlertingMap.get(Common.formatDevId2Mac(deviceInfo.getDevidX()))){
                        return;
                    }
                    final byte[] data = new byte[1];
                    if (!Common.isBell()) {
                       data[0] = 0x00;
                    }else{
                        data[0] = 0x02;
                    }
                    if(!mLinkLoseMap.containsKey(Common.formatDevId2Mac(deviceInfo.getDevidX()))||data[0]!=mLinkLoseMap.get(Common.formatDevId2Mac(deviceInfo.getDevidX()))) {
                        if(BleClient.getInstance().write(bleDevice.getAddress(), BleUUIDS.linkLossUUID, BleUUIDS.characteristicUUID, data)){
                            mLinkLoseMap.put(bleDevice.getAddress(), data[0]);
                        }
                    }
                }
            }else{
                if(BleClient.getInstance().getConnectState(bleDevice)==BluetoothGatt.STATE_DISCONNECTED){
                    connectDevice(bleDevice);
                }
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
                        if (mConnectMac.contains(mac)) {
                            mConnectMac.remove(mac);
                        }
                    }else{
                        BluetoothDevice bleDevice = mDeviceMap.get(mac);
                        if(bleDevice!=null&&!BleClient.getInstance().isConnected(bleDevice.getAddress())){
                            if (mConnectMac.contains(mac)) {
                                mConnectMac.remove(mac);
                            }
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
        Map<String,BluetoothDevice> tempMap = new HashMap<>();
        tempMap.putAll(mDeviceMap);
        for (Map.Entry<String, BluetoothDevice> entry : tempMap.entrySet()) {
            if(!macList.contains(entry.getKey())){
                mDeviceMap.remove(entry.getKey());
                BleClient.getInstance().disConnect(entry.getValue().getAddress());
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
        BleClient.getInstance().startScan(new ScanResultCallback(){
            @Override
            public void onScaning(ScanResult scanResult) {
                if(scanResult==null||TextUtils.isEmpty(scanResult.getName())||!scanResult.getName().contains("yoowoo")){
                    return;
                }
                LogUtils.d("bleService","扫描到有效贴片----->"+scanResult.getMac());
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret",IntentExtras.RET.RET_BLE_SCANING);
                intent.putExtra("address",scanResult.getMac());
                sendBroadcast(intent);
                if(!mDeviceMap.containsKey(scanResult.getMac())){
                    if(isBind&&scanResult.getRssi()>-50){
                        if(mBindMap.size()==0) {
                            mBindMap.put(scanResult.getMac(),scanResult.getDevice());
                            LogUtils.d("bleService", "开始绑定贴片----->" + scanResult.getMac());
                            Intent intent1 = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                            intent1.putExtra("ret", IntentExtras.RET.RET_BLE_FIND_BIND_DEVICE);
                            intent1.putExtra("address", scanResult.getMac());
                            sendBroadcast(intent1);
                        }
                        return;
                    }
                }
                mDeviceMap.put(scanResult.getMac(),scanResult.getDevice());
                Intent intent2= new Intent(IntentExtras.ACTION.ACTION_BLE_COMMAND);
                intent2.putExtra("cmd",IntentExtras.CMD.CMD_DEVICE_SCANING);
                intent2.putExtra("address",scanResult.getMac());
                sendBroadcast(intent2);

            }
        });
    }
    public synchronized void connectDevice(final BluetoothDevice bleDevice){
        if(TextUtils.isEmpty(Common.getTocken())){
            return;
        }
        if(bleDevice==null||mConnectMac.contains(bleDevice.getAddress())||(!mDeviceMap.containsKey(bleDevice.getAddress())&&!mBindMap.containsKey(bleDevice.getAddress()))){
            return;
        }
        if(BleClient.getInstance().getConnectState(bleDevice)==BluetoothGatt.STATE_DISCONNECTED){
            BleClient.getInstance().connectDevice(bleDevice, new GattCallback() {
                @Override
                public void onStartConnect(BluetoothDevice device) {
                    LogUtils.d("bleService", "贴片开始连接----->" + device.getAddress());
                    Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                    intent.putExtra("ret", IntentExtras.RET.RET_BLE_CONNECT_START);
                    intent.putExtra("address", device.getAddress());
                    sendBroadcast(intent);
                    if(!mConnectMac.contains(device.getAddress())){
                        mConnectMac.add(device.getAddress());
                    }
                }
                @Override
                public void onConnectFail(BluetoothDevice device) {
                    LogUtils.d("bleService", "贴片连接失败----->" + device.getAddress());
                    Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                    intent.putExtra("ret", IntentExtras.RET.RET_BLE_CONNECT_FAILED);
                    intent.putExtra("address", device.getAddress());
                    sendBroadcast(intent);
                    if(mConnectMac.contains(device.getAddress())){
                        mConnectMac.remove(device.getAddress());
                    }
                }

                @Override
                public void onConnectSuccess(BluetoothDevice device, BluetoothGatt gatt, int status) {
                    LogUtils.d("bleService", "贴片连接成功----->" + device.getAddress());
                    Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                    intent.putExtra("ret", IntentExtras.RET.RET_BLE_CONNECT_SUCCESS);
                    intent.putExtra("address", device.getAddress());
                    sendBroadcast(intent);
                    if(mBindMap.containsKey(device.getAddress())){
                        setBindMode(device);
                    }else if(mDeviceMap.containsKey(device.getAddress())) {
                        setWorkMode(device);
                    }
                }

                @Override
                public void onDisConnected(boolean isActiveDisConnected, BluetoothDevice device, BluetoothGatt gatt, int status) {
                    LogUtils.d("bleService", "贴片连接断开----->" + device.getAddress()+"   isActiveDisConnected--->"+isActiveDisConnected);
                    Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                    intent.putExtra("ret", IntentExtras.RET.RET_BLE_DISCONNECT);
                    intent.putExtra("address", device.getAddress());
                    sendBroadcast(intent);
                    if(mConnectMac.contains(device.getAddress())){
                        mConnectMac.remove(device.getAddress());
                    }
                    if(mLinkLoseMap.containsKey(device.getAddress())){
                        mLinkLoseMap.remove(device.getAddress());
                    }
                    final String mac = Common.formatMac2DevId(device.getAddress());
                    DeviceInfo deviceInfo = (DeviceInfo) SPUtils.getInstance().readObject(device.getAddress());
                    if (deviceInfo == null || deviceInfo.getAlertinfo()==null) {
                        return;
                    }
                    if(!isActiveDisConnected) {
                        DeviceInfo.AlertinfoBean alertinfoBean = deviceInfo.getAlertinfo();
                        if(mNotFirstDisConnect.containsKey(mac)&&mNotFirstDisConnect.get(mac)){
                            if (alertinfoBean.getIsRepeat() == 1 && Common.isBell()) {
                                doPlay(deviceInfo);
                            }
                        }else{
                            mNotFirstDisConnect.put(mac,true);
                            if (Common.isBell()) {
                                doPlay(deviceInfo);
                            }
                        }
                        Common.doCommitEvents(App.getInstance(), mac, Params.EVENT_TYPE.DEVICE_LOSE, null);
                    }
                }

                @Override
                public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                    LogUtils.d("bleService", gatt.getDevice().getAddress() + ">>>" + rssi);
                    Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                    intent.putExtra("ret", IntentExtras.RET.RET_BLE_READ_RSSI);
                    intent.putExtra("address", gatt.getDevice().getAddress());
                    intent.putExtra("rssi", rssi);
                    sendBroadcast(intent);
                    mapRssi.put(gatt.getDevice().getAddress(), rssi);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    super.onCharacteristicChanged(gatt, characteristic);
                    if (characteristic!=null&&characteristic.getUuid().toString().equals(BleUUIDS.userCharactButtonStateUUID)) {
                        byte[] data = characteristic.getValue();
                        LogUtils.d("bleService", "监测到" + gatt.getDevice().getAddress() + "双击事件("+data[0]+")--->报警广播数据");
                        if (data[0] == 02) {
                            if (!Common.isBell()) {
                                LogUtils.d("贴片在勿扰范围内,报警忽略!!");
                                return;
                            }
                            DeviceInfo deviceInfo = (DeviceInfo) SPUtils.getInstance().readObject(gatt.getDevice().getAddress());
                            if (deviceInfo != null) {
                                if(Common.isBell()) {
                                    doPlay(deviceInfo);
                                }
                            }

                        }
                    }else if(characteristic!=null&&characteristic.getUuid().toString().equals(BleUUIDS.batCharacteristicUUID)){
                        byte[] data = characteristic.getValue();
                        LogUtils.d("bleService", "监测到" + gatt.getDevice().getAddress() + "电池电量" + data[0]+"%");
                        Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                        intent.putExtra("ret", IntentExtras.RET.RET_BLE_READ_BATTERY);
                        intent.putExtra("address", gatt.getDevice().getAddress());
                        intent.putExtra("battery", data[0] + "");
                        sendBroadcast(intent);
                        mapBattery.put(gatt.getDevice().getAddress(), data[0] + "");

                    }
                }
                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicWrite(gatt, characteristic, status);
                    LogUtils.d("bleService", "onCharacteristicWrite:" + gatt.getDevice().getAddress() + "    status:" + status);
                }
            });
        }
    }

    private synchronized void unbindDevice(String address) {
        BluetoothDevice bleDevice = mDeviceMap.get(address);
        if(bleDevice==null||!BleClient.getInstance().isConnected(bleDevice.getAddress())){
            return;
        }
        byte[] dataWork = Common.formatBleMsg(Params.BLEMODE.MODE_UNBIND, Common.getUserId());
        if(BleClient.getInstance().write(bleDevice.getAddress(), userServiceUUID, userCharacteristicLogUUID, dataWork)){
            LogUtils.d("bleService","贴片解绑成功----->"+bleDevice.getAddress());
            Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
            intent.putExtra("ret", IntentExtras.RET.RET_BLE_UNBIND_COMPLETE);
            intent.putExtra("address", bleDevice.getAddress());
            sendBroadcast(intent);
            BleClient.getInstance().disConnect(bleDevice.getAddress());
            if(mDeviceMap.containsKey(bleDevice.getAddress())){
                mDeviceMap.remove(bleDevice.getAddress());
            }
            if(mConnectMac.contains(bleDevice.getAddress())){
                mConnectMac.remove(bleDevice.getAddress());
            }
            if(mLinkLoseMap.containsKey(bleDevice.getAddress())){
                mLinkLoseMap.remove(bleDevice.getAddress());
            }
            mBindMap.clear();
            isBind=false;
        }else{
            LogUtils.d("bleService","贴片解绑失败----->"+bleDevice.getAddress());
        }
    }

    private synchronized void clearAllConnect(){
        LogUtils.d("bleService","clearAllConnect");
        mDeviceMap.clear();
        mBindMap.clear();
        isBind=false;
        BleClient.getInstance().disconnectAllDevice();
    }
    private synchronized void doLogOut(){
        LogUtils.d("bleService","doLogOut");
        final byte[] data = new byte[]{0x00};
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
        if(BleClient.getInstance().write(bleDevice.getAddress(), serviceUUID, characteristicUUID, options)){
            mAlertingMap.put(address,true);
            Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
            LogUtils.d("bleService","贴片开始报警----->"+address);
            intent.putExtra("ret", IntentExtras.RET.RET_BLE_ALERT_STARTED);
            intent.putExtra("address", address);
            sendBroadcast(intent);
        }else{
            LogUtils.d("bleService","贴片开始报警---->失败了----->"+address);
        }
    }
    private synchronized void stopAlert(String address) {
        BluetoothDevice bleDevice = mDeviceMap.get(address);
        if(bleDevice==null||!BleClient.getInstance().isConnected(bleDevice.getAddress())){
            return;
        }
        byte[] options = new byte[]{0x00};
        if(BleClient.getInstance().write(bleDevice.getAddress(), serviceUUID, characteristicUUID, options)){
            mAlertingMap.put(bleDevice.getAddress(),false);
            Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
            LogUtils.d("bleService","贴片结束报警----->"+bleDevice.getAddress());
            intent.putExtra("ret", IntentExtras.RET.RET_BLE_ALERT_STOPED);
            intent.putExtra("address", bleDevice.getAddress());
            sendBroadcast(intent);
        }else{
            LogUtils.d("bleService","贴片结束报警--->失败了----->"+bleDevice.getAddress());
        }
    }

    public synchronized void setWorkMode(BluetoothDevice bleDevice){
        if(bleDevice==null||!BleClient.getInstance().isConnected(bleDevice.getAddress())||!mDeviceMap.containsKey(bleDevice.getAddress())){
            return;
        }
        byte[] dataWork = Common.formatBleMsg(Params.BLEMODE.MODE_WORK, Common.getUserId());
        if(BleClient.getInstance().write(bleDevice.getAddress(), userServiceUUID, userCharacteristicLogUUID, dataWork)){
            LogUtils.d("bleService","贴片设置工作模式成功(isNeedAlerts-->"+mNotFirstConnect.get(bleDevice.getAddress())+")----->"+bleDevice.getAddress());
            Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
            intent.putExtra("ret", IntentExtras.RET.RET_BLE_MODE_WORK_SUCCESS);
            intent.putExtra("address", bleDevice.getAddress());
            sendBroadcast(intent);
            if(!mDeviceMap.containsKey(bleDevice.getAddress())) {
                return;
            }
            final DeviceInfo deviceInfo = (DeviceInfo) SPUtils.getInstance().readObject(bleDevice.getAddress());
            if (deviceInfo == null||deviceInfo.getAlertinfo()==null) {
                return;
            }
            DeviceInfo.AlertinfoBean alertinfoBean = deviceInfo.getAlertinfo();
            if(mNotFirstConnect.containsKey(bleDevice.getAddress())&&mNotFirstConnect.get(bleDevice.getAddress())) {
                Common.doCommitEvents(App.getInstance(), Common.formatMac2DevId(bleDevice.getAddress()), Params.EVENT_TYPE.DEVICE_REDISCOVER, null);
                if(alertinfoBean.getIsReconnect() == 1 && Common.isBell()){
                    BingGoPlayUtils.playBingGo();
                }
            }else{
                mNotFirstConnect.put(bleDevice.getAddress(),true);
                BingGoPlayUtils.playBingGo();
                Common.doCommitEvents(App.getInstance(),Common.formatMac2DevId(bleDevice.getAddress()),Params.EVENT_TYPE.DEVICE_CONNECT,null);
            }
        }else{
            LogUtils.d("bleService","贴片设置工作模式失败----->"+bleDevice.getAddress());
            Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
            intent.putExtra("ret", IntentExtras.RET.RET_BLE_MODE_WORK_FAIL);
            intent.putExtra("address", bleDevice.getAddress());
            sendBroadcast(intent);
        }
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

    public synchronized void setBindMode(BluetoothDevice bleDevice){
        if(bleDevice==null||!BleClient.getInstance().isConnected(bleDevice.getAddress())
                ||!mBindMap.containsKey(bleDevice.getAddress())){
            return;
        }
        byte[] bind = Common.formatBleMsg(Params.BLEMODE.MODE_BIND,Common.getUserId());
        if(BleClient.getInstance().write(bleDevice.getAddress(), BleUUIDS.userServiceUUID, BleUUIDS.userCharacteristicLogUUID, bind)){
            LogUtils.d("bleService","贴片设置绑定模式成功----->"+bleDevice.getAddress());
            Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
            intent.putExtra("ret", IntentExtras.RET.RET_BLE_MODE_BIND_SUCCESS);
            intent.putExtra("address", bleDevice.getAddress());
            sendBroadcast(intent);
            mBindMap.clear();
            isBind =false;
            if(!mDeviceMap.containsKey(bleDevice.getAddress())){
                mDeviceMap.put(bleDevice.getAddress(),bleDevice);
            }
            mNotFirstConnect.put(bleDevice.getAddress(),true);
        }else{
            LogUtils.d("bleService","贴片设置绑定模式失败----->"+bleDevice.getAddress());
            Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
            intent.putExtra("ret", IntentExtras.RET.RET_BLE_MODE_BIND_FAIL);
            intent.putExtra("address", bleDevice.getAddress());
            sendBroadcast(intent);
        }
    }

    public class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location){
            LogUtils.d("bleService","接收到定位信息----->"+location.getLatitude()+","+location.getLongitude());
            if(!TextUtils.isEmpty(location.getAddrStr())){
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
            }
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
            if(TextUtils.isEmpty(devlistBean.getFriendname())){
                devItem.put("sourceid", 0);
                devItem.put("devbattery",0);
                devItem.put("devposstate", 0);
                devItem.put("devstate", 0);
            }else{
                devItem.put("sourceid", devlistBean.getFriendid());
                BluetoothDevice bleDevice = mDeviceMap.get(mac);
                if(BleClient.getInstance().isConnected(bleDevice.getAddress())){
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