package com.miittech.you.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
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
import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
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
    private Map<String,BleDevice> mDeviceMap = new HashMap<>();
    private Map<String,BleDevice> mBindMap = new HashMap<>();
    private Map<String,Boolean> mAlertingMap = new HashMap<>();
    private Map<String,Byte> mLinkLoseMap = new HashMap<>();
    private List<String> mConnectMac=new ArrayList<>();
    private boolean isBind = false;
    private Map<String,Boolean> isNeedAlerts = new HashMap<>();
    private long lastScanningTime=TimeUtils.getNowMills();

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
        option.setScanSpan(5000);
        option.setOpenGps(true);
        option.setIsNeedAddress(true);
        option.setIgnoreKillProcess(true);
        option.setWifiCacheTimeOut(5*60*1000);
        mLocationClient.setLocOption(option);
        mLocationClient.start();

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
        BleManager.getInstance().cancelScan();
        BleManager.getInstance().disconnectAllDevice();
        mConnectMac.clear();
        mDeviceMap.clear();
        mapRssi.clear();
        mapBattery.clear();
        mBindMap.clear();
        mLinkLoseMap.clear();
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
                        BleDevice bleDevice = mBindMap.get(intent.getStringExtra("address"));
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
                        clearAllConnect(false);
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
                        BleManager.getInstance().cancelScan();
                        clearAllConnect(true);
                        break;
                }
            }
            LogUtils.d("bleService",stringBuilder.toString());
        }
    }

    private synchronized void exceTask() {
        exceReportSubmit();
        exceCalibrationDevice();
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
                        LogUtils.d("bleService",device.getMac()+">>>"+rssi);
                        Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                        intent.putExtra("ret", IntentExtras.RET.RET_BLE_READ_RSSI);
                        intent.putExtra("address", device.getMac());
                        intent.putExtra("rssi", rssi);
                        sendBroadcast(intent);
                        mapRssi.put(device.getMac(),rssi);
                    }
                });

                DeviceInfo deviceInfo = (DeviceInfo) SPUtils.getInstance().readObject(bleDevice.getMac());
                if(deviceInfo!=null) {
                    if(mAlertingMap.containsKey(Common.formatDevId2Mac(deviceInfo.getDevidX()))&&mAlertingMap.get(Common.formatDevId2Mac(deviceInfo.getDevidX()))){
                        return;
                    }
                    DeviceInfo.AlertinfoBean alertinfoBean = deviceInfo.getAlertinfo();
                    if (alertinfoBean != null) {
                        final byte[] data = new byte[1];
                        if (!Common.isBell()) {
                           data[0] = 0x00;
                        }else{
                            data[0] = 0x02;
                        }
                        if(!mLinkLoseMap.containsKey(Common.formatDevId2Mac(deviceInfo.getDevidX()))||data[0]!=mLinkLoseMap.get(Common.formatDevId2Mac(deviceInfo.getDevidX()))) {
                            BleManager.getInstance().write(bleDevice, BleUUIDS.linkLossUUID, BleUUIDS.characteristicUUID, data, new BleWriteCallback() {

                                @Override
                                public void onWriteSuccess(BleDevice bleDevice) {
                                    mLinkLoseMap.put(bleDevice.getMac(), data[0]);
                                }

                                @Override
                                public void onWriteFailure(BleDevice bleDevice, BleException exception) {

                                }
                            });
                        }
                    }
                }
            }else{
                if(BleManager.getInstance().getConnectState(bleDevice)== BleConnectState.CONNECT_DISCONNECT){
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
                        BleDevice bleDevice = mDeviceMap.get(mac);
                        if(bleDevice!=null&&!BleManager.getInstance().isConnected(bleDevice)){
                            if (mConnectMac.contains(mac)) {
                                mConnectMac.remove(mac);
                            }
                            BleManager.getInstance().disconnect(bleDevice);
                        }
                    }
                    if(TimeUtils.getTimeSpan(lastScanningTime,TimeUtils.getNowMills(),TimeConstants.MIN)>5){
                        BleManager.getInstance().cancelScan();
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
            if(!isNeedAlerts.containsKey(mac)){
                isNeedAlerts.put(mac,false);
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
    }



    public synchronized  void scanDevice(){
        if(TextUtils.isEmpty(Common.getTocken())||BleManager.getInstance().getScanSate()==BleScanState.STATE_SCANNING){
            return;
        }
        BleScanRuleConfig scanRuleConfig = new BleScanRuleConfig.Builder()
                .setDeviceName(false,"yoowoo")
                .setAutoConnect(false)
                .setScanTimeOut(0)
                .build();
        BleManager.getInstance().initScanRule(scanRuleConfig);
        BleManager.getInstance().scan(new BleScanCallback() {
            @Override
            public void onScanStarted(boolean success) {
                lastScanningTime = TimeUtils.getNowMills();
                LogUtils.d("bleService","贴片扫描开始----->");
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret",IntentExtras.RET.RET_BLE_SCAN_START);
                sendBroadcast(intent);
            }

            @Override
            public void onScanning(BleDevice result) {
                lastScanningTime = TimeUtils.getNowMills();
                LogUtils.d("bleService","扫描到有效贴片----->"+result.getMac());
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret",IntentExtras.RET.RET_BLE_SCANING);
                intent.putExtra("address",result.getMac());
                sendBroadcast(intent);
                if(!mDeviceMap.containsKey((result.getMac()))){
                    if(isBind&&result.getRssi()>-50){
                        if(mBindMap.size()==0) {
                            mBindMap.put(result.getMac(), result);
                            LogUtils.d("bleService", "开始绑定贴片----->" + result.getMac());
                            Intent intent1 = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                            intent1.putExtra("ret", IntentExtras.RET.RET_BLE_FIND_BIND_DEVICE);
                            intent1.putExtra("address", result.getMac());
                            sendBroadcast(intent1);
                        }
                    }
                    return;
                }
                mDeviceMap.put(result.getMac(),result);
                Intent intent2= new Intent(IntentExtras.ACTION.ACTION_BLE_COMMAND);
                intent2.putExtra("cmd",IntentExtras.CMD.CMD_DEVICE_SCANING);
                intent2.putExtra("address",result.getMac());
                sendBroadcast(intent2);
            }

            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
                LogUtils.d("bleService","贴片扫描结束----->");
            }
        });
    }
    long lastConnectTime =0;
    public synchronized void connectDevice(final BleDevice bleDevice){
        if(TextUtils.isEmpty(Common.getTocken())){
            return;
        }
        if(bleDevice==null||mConnectMac.contains(bleDevice.getMac())||(!mDeviceMap.containsKey(bleDevice.getMac())&&!mBindMap.containsKey(bleDevice.getMac()))){
            return;
        }
        if(BleManager.getInstance().getConnectState(bleDevice)==BleConnectState.CONNECT_IDLE
                ||BleManager.getInstance().getConnectState(bleDevice)==BleConnectState.CONNECT_DISCONNECT
                ||BleManager.getInstance().getConnectState(bleDevice)==BleConnectState.CONNECT_TIMEOUT) {

            long currentConnectTime = TimeUtils.getNowMills();
            if (currentConnectTime - lastConnectTime < 300) {
                return;
            }
            lastConnectTime = currentConnectTime;
            BleManager.getInstance().connect(bleDevice, new BleGattCallback() {
                @Override
                public void onStartConnect(BleDevice device) {
                    LogUtils.d("bleService", "贴片开始连接----->" + device.getMac());
                    Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                    intent.putExtra("ret", IntentExtras.RET.RET_BLE_CONNECT_START);
                    intent.putExtra("address", device.getMac());
                    sendBroadcast(intent);
                    if(!mConnectMac.contains(device.getMac())){
                        mConnectMac.add(device.getMac());
                    }
                }

                @Override
                public void onConnectFail(BleDevice device, BleException exception) {
                    LogUtils.d("bleService", "贴片连接失败----->" + device.getMac());
                    Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                    intent.putExtra("ret", IntentExtras.RET.RET_BLE_CONNECT_FAILED);
                    intent.putExtra("address", device.getMac());
                    sendBroadcast(intent);
                    if(mConnectMac.contains(device.getMac())){
                        mConnectMac.remove(device.getMac());
                    }
                }

                @Override
                public void onConnectSuccess(BleDevice device, BluetoothGatt gatt, int status) {
                    LogUtils.d("bleService", "贴片连接成功----->" + device.getMac());
                    Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                    intent.putExtra("ret", IntentExtras.RET.RET_BLE_CONNECT_SUCCESS);
                    intent.putExtra("address", device.getMac());
                    sendBroadcast(intent);
                    if(mBindMap.containsKey(device.getMac())){
                        setBindMode(device);
                    }else if(mDeviceMap.containsKey(device.getMac())) {
                        setWorkMode(device);
                    }
                }

                @Override
                public void onDisConnected(boolean isActiveDisConnected, BleDevice device, BluetoothGatt gatt, int status) {
                    LogUtils.d("bleService", "贴片连接断开----->" + device.getMac()+"   isActiveDisConnected--->"+isActiveDisConnected);
                    Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                    intent.putExtra("ret", IntentExtras.RET.RET_BLE_DISCONNECT);
                    intent.putExtra("address", device.getMac());
                    sendBroadcast(intent);
                    if(mConnectMac.contains(device.getMac())){
                        mConnectMac.remove(device.getMac());
                    }
                    if(mLinkLoseMap.containsKey(device.getMac())){
                        mLinkLoseMap.remove(device.getMac());
                    }
                    if(!isActiveDisConnected) {
                        Common.doCommitEvents(App.getInstance(), Common.formatMac2DevId(device.getMac()), Params.EVENT_TYPE.DEVICE_LOSE, null);
                        DeviceInfo deviceInfo = (DeviceInfo) SPUtils.getInstance().readObject(device.getMac());
                        if (deviceInfo != null) {
                            DeviceInfo.AlertinfoBean alertinfoBean = deviceInfo.getAlertinfo();
                            if (alertinfoBean != null) {
                                if (alertinfoBean.getIsRepeat() == 1 && Common.isBell()) {
                                    doPlay(deviceInfo);
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    private synchronized void unbindDevice(String address) {
        BleDevice bleDevice = mDeviceMap.get(address);
        if(bleDevice==null||!BleManager.getInstance().isConnected(bleDevice)){
            return;
        }
        byte[] dataWork = Common.formatBleMsg(Params.BLEMODE.MODE_UNBIND, Common.getUserId());
        BleManager.getInstance().write(bleDevice, userServiceUUID, userCharacteristicLogUUID, dataWork, new BleWriteCallback() {
            @Override
            public void onWriteSuccess(BleDevice device) {
                LogUtils.d("bleService","贴片解绑成功----->"+device.getMac());
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret", IntentExtras.RET.RET_BLE_UNBIND_COMPLETE);
                intent.putExtra("address", device.getMac());
                sendBroadcast(intent);
                BleManager.getInstance().disconnect(device);
                if(mDeviceMap.containsKey(device.getMac())){
                    mDeviceMap.remove(device.getMac());
                }
                if(mConnectMac.contains(device.getMac())){
                    mConnectMac.remove(device.getMac());
                }
                if(mLinkLoseMap.containsKey(device.getMac())){
                    mLinkLoseMap.remove(device.getMac());
                }
                mBindMap.clear();
                isBind=false;
            }

            @Override
            public void onWriteFailure(BleDevice device,BleException exception) {
                LogUtils.d("bleService","贴片解绑失败----->"+device.getMac());
            }
        });
    }

    private synchronized void clearAllConnect(boolean isBleClose){
        LogUtils.d("bleService","clearAllConnect");
        mDeviceMap.clear();
        mBindMap.clear();
        if(!isBleClose) {
            isNeedAlerts.clear();
        }
        isBind=false;
        BleManager.getInstance().disconnectAllDevice();
    }

    private synchronized void startAlert(String address) {
        BleDevice bleDevice = mDeviceMap.get(address);
        if(bleDevice==null||!BleManager.getInstance().isConnected(bleDevice)){
            return;
        }
        byte[] options = new byte[]{0x02};
        BleManager.getInstance().write(bleDevice, serviceUUID, characteristicUUID, options, new BleWriteCallback() {
            @Override
            public void onWriteSuccess(BleDevice device) {
                mAlertingMap.put(device.getMac(),true);
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                LogUtils.d("bleService","贴片开始报警----->"+device.getMac());
                intent.putExtra("ret", IntentExtras.RET.RET_BLE_ALERT_STARTED);
                intent.putExtra("address", device.getMac());
                sendBroadcast(intent);
            }

            @Override
            public void onWriteFailure(BleDevice device,BleException exception) {
                LogUtils.d("bleService","贴片开始报警---->失败了----->"+device.getMac());
            }
        });

    }
    private synchronized void stopAlert(String address) {
        BleDevice bleDevice = mDeviceMap.get(address);
        if(bleDevice==null||!BleManager.getInstance().isConnected(bleDevice)){
            return;
        }
        byte[] options = new byte[]{0x00};
        BleManager.getInstance().write(bleDevice, serviceUUID, characteristicUUID, options, new BleWriteCallback() {
            @Override
            public void onWriteSuccess(BleDevice device) {
                mAlertingMap.put(device.getMac(),false);
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                LogUtils.d("bleService","贴片结束报警----->"+device.getMac());
                intent.putExtra("ret", IntentExtras.RET.RET_BLE_ALERT_STOPED);
                intent.putExtra("address", device.getMac());
                sendBroadcast(intent);
            }

            @Override
            public void onWriteFailure(BleDevice device,BleException exception) {
                LogUtils.d("bleService","贴片结束报警--->失败了----->"+device.getMac());
            }
        });
    }

    public synchronized void setWorkMode(BleDevice bleDevice){
        if(bleDevice==null||!BleManager.getInstance().isConnected(bleDevice)||!mDeviceMap.containsKey(bleDevice.getMac())){
            return;
        }
        byte[] dataWork = Common.formatBleMsg(Params.BLEMODE.MODE_WORK, Common.getUserId());
        BleManager.getInstance().write(bleDevice, userServiceUUID, userCharacteristicLogUUID, dataWork, new BleWriteCallback() {
            @Override
            public void onWriteSuccess(final BleDevice device) {
                LogUtils.d("bleService","贴片设置工作模式成功(isNeedAlerts-->"+isNeedAlerts.get(device.getMac())+")----->"+device.getMac());
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret", IntentExtras.RET.RET_BLE_MODE_WORK_SUCCESS);
                intent.putExtra("address", device.getMac());
                sendBroadcast(intent);
                if(!mDeviceMap.containsKey(device.getMac())) {
                    return;
                }
                if(isNeedAlerts.containsKey(device.getMac())&&isNeedAlerts.get(device.getMac())) {
                    Common.doCommitEvents(App.getInstance(), Common.formatMac2DevId(device.getMac()), Params.EVENT_TYPE.DEVICE_REDISCOVER, null);
                    final DeviceInfo deviceInfo = (DeviceInfo) SPUtils.getInstance().readObject(device.getMac());
                    if (deviceInfo != null) {
                        DeviceInfo.AlertinfoBean alertinfoBean = deviceInfo.getAlertinfo();
                        if (alertinfoBean != null) {
                            if (alertinfoBean.getIsReconnect() == 1 && Common.isBell()) {
                                doPlay(deviceInfo);
                            }
                        }
                    }
                }else{
                    BingGoPlayUtils.playBingGo();
                    Common.doCommitEvents(App.getInstance(),Common.formatMac2DevId(device.getMac()),Params.EVENT_TYPE.DEVICE_CONNECT,null);
                    isNeedAlerts.put(device.getMac(),true);
                }
                readRssi(device);
            }
            @Override
            public void onWriteFailure(BleDevice device,BleException exception) {
                LogUtils.d("bleService","贴片设置工作模式失败----->"+device.getMac());
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret", IntentExtras.RET.RET_BLE_MODE_WORK_FAIL);
                intent.putExtra("address", device.getMac());
                sendBroadcast(intent);
            }
        });
    }

    private void readRssi(BleDevice bleDevice) {
        if(bleDevice!=null&&mDeviceMap.containsKey(bleDevice.getMac())&&BleManager.getInstance().isConnected(bleDevice)) {
            BleManager.getInstance().readRssi(bleDevice, new BleRssiCallback() {
                @Override
                public void onRssiFailure(BleDevice device, BleException exception) {

                }

                @Override
                public void onRssiSuccess(BleDevice device, int rssi) {
                    LogUtils.d("bleService", device.getMac() + ">>>" + rssi);
                    Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                    intent.putExtra("ret", IntentExtras.RET.RET_BLE_READ_RSSI);
                    intent.putExtra("address", device.getMac());
                    intent.putExtra("rssi", rssi);
                    sendBroadcast(intent);
                    mapRssi.put(device.getMac(), rssi);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    notfyAlert(device);
                }
            });
        }
    }

    public synchronized void notfyAlert(final BleDevice bleDevice) {
        BleManager.getInstance().notify(bleDevice, BleUUIDS.userServiceUUID, BleUUIDS.userCharactButtonStateUUID, new BleNotifyCallback() {
            @Override
            public void onNotifySuccess() {
                LogUtils.d("bleService", "设置监测贴片按钮事件成功");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                notifyBattery(bleDevice);
            }

            @Override
            public void onNotifyFailure(BleException exception) {
                LogUtils.d("bleService", exception.getDescription());
            }

            @Override
            public void onCharacteristicChanged(BleDevice device, byte[] data) {
                LogUtils.d("bleService", "监测到" + device.getMac() + "双击事件("+data[0]+")--->报警广播数据");
                if (data[0] == 02) {
                    if (!Common.isBell()) {
                        LogUtils.d("贴片在勿扰范围内,报警忽略！");
                        return;
                    }
                    DeviceInfo deviceInfo = (DeviceInfo) SPUtils.getInstance().readObject(device.getMac());
                    if (deviceInfo != null) {
                        if(Common.isBell()) {
                            doPlay(deviceInfo);
                        }
                    }else{

                    }

                }
            }
        });

    }
    public synchronized void notifyBattery(final BleDevice bleDevice){
        BleManager.getInstance().notify(bleDevice, BleUUIDS.batServiceUUID, BleUUIDS.batCharacteristicUUID, new BleNotifyCallback() {
            @Override
            public void onNotifySuccess() {
                LogUtils.d("bleService", "设置监测电量广播成功");
            }

            @Override
            public void onNotifyFailure(BleException exception) {
                LogUtils.d("bleService", exception.getDescription());
            }

            @Override
            public void onCharacteristicChanged(BleDevice device, byte[] data) {
                LogUtils.d("bleService", "监测到" + device.getMac() + "电池电量" + data[0]+"%");
                if(BleManager.getInstance().isConnected(device)) {
                    Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                    intent.putExtra("ret", IntentExtras.RET.RET_BLE_READ_BATTERY);
                    intent.putExtra("address", device.getMac());
                    intent.putExtra("battery", data[0] + "");
                    sendBroadcast(intent);
                    mapBattery.put(device.getMac(), data[0] + "");
                }
            }
        });
    }

    private void doPlay(DeviceInfo deviceInfo) {
        String url = deviceInfo.getAlertinfo().getUrlX();
        boolean isShake = (deviceInfo.getAlertinfo().getIsShake()==1)?true:false;
        int duration = deviceInfo.getAlertinfo().getDuration();
        duration*=1000;
        if(url.contains("bluesforslim")){
            SoundPlayUtils.play(3,duration,isShake);
        }else if(url.contains("countryfair")){
            SoundPlayUtils.play(4,duration,isShake);
        }else if(url.contains("theclassiccall")){
            SoundPlayUtils.play(2,duration,isShake);
        }else{
            SoundPlayUtils.play(1,duration,isShake);
        }
    }

    public synchronized void setBindMode(BleDevice bleDevice){
        if(bleDevice==null||!BleManager.getInstance().isConnected(bleDevice)||!mBindMap.containsKey(bleDevice.getMac())){
            return;
        }
        byte[] bind = Common.formatBleMsg(Params.BLEMODE.MODE_BIND,Common.getUserId());
        BleManager.getInstance().write(bleDevice, BleUUIDS.userServiceUUID, BleUUIDS.userCharacteristicLogUUID, bind, new BleWriteCallback() {
            @Override
            public void onWriteSuccess(BleDevice device) {
                LogUtils.d("bleService","贴片设置绑定模式成功----->"+device.getMac());
                Intent intent = new Intent(IntentExtras.ACTION.ACTION_CMD_RESPONSE);
                intent.putExtra("ret", IntentExtras.RET.RET_BLE_MODE_BIND_SUCCESS);
                intent.putExtra("address", device.getMac());
                sendBroadcast(intent);
                mBindMap.clear();
                isBind =false;
                if(!mDeviceMap.containsKey(device.getMac())){
                    mDeviceMap.put(device.getMac(),device);
                }
                isNeedAlerts.put(device.getMac(),true);
                readRssi(device);
            }

            @Override
            public void onWriteFailure(BleDevice device,BleException exception) {
                LogUtils.d("bleService","贴片设置绑定模式失败----->"+device.getMac());
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