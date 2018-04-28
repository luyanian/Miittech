package com.miittech.you.ble;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.support.v4.util.SimpleArrayMap;
import android.text.TextUtils;
import android.util.Log;
import com.miittech.you.App;
import com.miittech.you.ble.gatt.BleNotifyCallback;
import com.miittech.you.ble.gatt.BleReadCallback;
import com.miittech.you.ble.gatt.BleWriteCallback;
import com.miittech.you.ble.gatt.GattCallback;
import com.miittech.you.ble.scan.BleLeScanCallback;
import com.miittech.you.ble.scan.ScanResultCallback;
import com.miittech.you.ble.task.trans.BleNotifyTask;
import com.miittech.you.ble.task.trans.BleReadRemoteRssiTask;
import com.miittech.you.ble.task.trans.BleReadTask;
import com.miittech.you.ble.task.trans.BleTransTaskQueue;
import com.miittech.you.ble.task.trans.BleWriteOffsetTask;
import com.miittech.you.ble.task.trans.BleWriteTask;
import com.miittech.you.global.Params;
import com.miittech.you.utils.Common;
import com.ryon.constant.TimeConstants;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.TimeUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.miittech.you.ble.BleUUIDS.userCharacteristicLogUUID;
import static com.miittech.you.ble.BleUUIDS.userServiceUUID;

/**
 * Created by Administrator on 2018/1/10.
 */

public class BleClient {
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
//    private BluetoothLeScanner bluetoothLeScanner;
//    private BleScanCallback bleScanCallback;
    private BleLeScanCallback bleLeScanCallback;
    private SimpleArrayMap<String,GattCallback> mGattCallbacks = new SimpleArrayMap<>();
    private SimpleArrayMap<String, Boolean> isEffectConnectSuccess = new SimpleArrayMap<String, Boolean>();
    private SimpleArrayMap<String,SimpleArrayMap> deviceReadCallbacks = new SimpleArrayMap<>();
    private SimpleArrayMap<String,SimpleArrayMap> deviceWriteCallbacks = new SimpleArrayMap<>();
    private SimpleArrayMap<String,SimpleArrayMap> deviceNotifyCallbacks = new SimpleArrayMap<>();
    private SimpleArrayMap<String,Long> bleLastScanTime = new SimpleArrayMap<>();
    private Context context;
    private SimpleArrayMap<String,Boolean> isActivityDisConnects = new SimpleArrayMap();
    private SimpleArrayMap<String,BluetoothGatt> bluetoothGatts = new SimpleArrayMap<>();
    private SimpleArrayMap<String,Boolean> isDisConnectMaps = new SimpleArrayMap<>();
    private SimpleArrayMap<String,Boolean> isEffectiveOption = new SimpleArrayMap<>();
    private boolean isScaning = false;
    private BleTransTaskQueue bleTransTaskQueue;
    static BleClient bleClient;
    public synchronized static BleClient getInstance(){
        if(bleClient==null){
            synchronized (BleClient.class){
                bleClient = new BleClient();
                bleClient.initContext();
            }
        }
        return bleClient;
    }

    public BleClient() {
        bleTransTaskQueue = new BleTransTaskQueue();
    }

    public void destroy(){
        bleTransTaskQueue.stop();
    }

    public void initContext(){
        this.context = App.getInstance();
        bluetoothManager =(BluetoothManager) App.getInstance().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
//        if(mBluetoothAdapter!=null&&Build.VERSION.SDK_INT> 21){
//            bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
//        }
    }
    public synchronized void startScan(final ScanResultCallback scanResultCallback){
//            if(Build.VERSION.SDK_INT> 21&&bluetoothLeScanner!=null){
//                bleScanCallback = new BleScanCallback(scanResultCallback);
//                bluetoothLeScanner.startScan(bleScanCallback);
//            }else {
//                bleLeScanCallback = new BleLeScanCallback(scanResultCallback);
//                mBluetoothAdapter.startLeScan(bleLeScanCallback);
//            }
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(scanResultCallback!=null&&mBluetoothAdapter!=null&&mBluetoothAdapter.isEnabled()) {
                    if (bleLeScanCallback != null) {
                        mBluetoothAdapter.stopLeScan(bleLeScanCallback);
                        bleLeScanCallback=null;
                    }

                    isScaning = true;
                    bleLeScanCallback = new BleLeScanCallback(scanResultCallback);
                    boolean isStart = mBluetoothAdapter.startLeScan(bleLeScanCallback);
                    LogUtils.d("bleservice", "mBluetoothAdapter.startLeScan-->" + isStart);

                }
            }
        }).start();
    }
    public synchronized void cancelScan() {
        isScaning = false;
//        if(Build.VERSION.SDK_INT> 21&&bluetoothLeScanner!=null&&bleScanCallback!=null){
//            bluetoothLeScanner.stopScan(bleScanCallback);
//            bleScanCallback=null;
//        }else if(mBluetoothAdapter!=null&&bleLeScanCallback != null) {
//            mBluetoothAdapter.stopLeScan(bleLeScanCallback);
//            bleLeScanCallback=null;
//        }
        if(mBluetoothAdapter!=null&&bleLeScanCallback!=null) {
            mBluetoothAdapter.stopLeScan(bleLeScanCallback);
            bleLeScanCallback = null;
        }
    }
    public void scanning(String mac){
        bleLastScanTime.put(mac, TimeUtils.getNowMills());
    }
    public synchronized void connectDevice(BluetoothDevice mDevice, GattCallback mGattCallback){
        mGattCallback.onStartConnect(mDevice.getAddress());
        mGattCallbacks.put(mDevice.getAddress(),mGattCallback);
        mDevice.connectGatt(context, false, new BluetoothGattCallback() {
            @Override
            public synchronized void onConnectionStateChange(final BluetoothGatt gatt, int status, final int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (status==BluetoothGatt.GATT_SUCCESS&&newState == BluetoothProfile.STATE_CONNECTED) {
                    bluetoothGatts.put(gatt.getDevice().getAddress(), gatt);
                    isActivityDisConnects.put(gatt.getDevice().getAddress(), false);
                    synchronized (isEffectiveOption){
                        isDisConnectMaps.put(gatt.getDevice().getAddress(), true);
                        if (isEffectiveOption.containsKey(gatt.getDevice().getAddress()) && !isEffectiveOption.get(gatt.getDevice().getAddress())) {
                            LogUtils.d("bleService", "onServicesDiscovered  isEffectiveOption-->false"+"    isActivityDisConnects"+isActivityDisConnects+"     "+gatt.getDevice().getAddress());
                            if (mGattCallbacks.containsKey(gatt.getDevice().getAddress()) && mGattCallbacks.get(gatt.getDevice().getAddress()) != null) {
                                mGattCallbacks.get(gatt.getDevice().getAddress()).onConnectSuccess(gatt.getDevice().getAddress(), status);
                            }
                            isEffectiveOption.put(gatt.getDevice().getAddress(), true);
                        } else {
                            LogUtils.d("bleService", "onServicesDiscovered  isEffectiveOption-->true"+"    isActivityDisConnects"+isActivityDisConnects+"    "+gatt.getDevice().getAddress());
                            if (mGattCallbacks.containsKey(gatt.getDevice().getAddress()) && mGattCallbacks.get(gatt.getDevice().getAddress()) != null) {
                                mGattCallbacks.get(gatt.getDevice().getAddress()).onEffectConnectSuccess(gatt.getDevice().getAddress(), status);
                                isEffectConnectSuccess.put(gatt.getDevice().getAddress(),true);

                            }
                        }
                    }
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    if (gatt != null&&gatt.getDevice()!=null&&!TextUtils.isEmpty(gatt.getDevice().getAddress())) {
                        final String mac = gatt.getDevice().getAddress();
                        if (mGattCallbacks.containsKey(gatt.getDevice().getAddress())
                                && isActivityDisConnects.containsKey(mac) && !isConnected(gatt.getDevice().getAddress())
                                && isDisConnectMaps.containsKey(mac) && isDisConnectMaps.get(mac)) {
                            if(mGattCallbacks.containsKey(mac)&&mGattCallbacks.get(mac)!=null) {
                                LogUtils.d("bleService", "onDisConnected    "+gatt.getDevice().getAddress());
                                mGattCallbacks.get(mac).onDisConnected(isActivityDisConnects.get(mac), mac, newState);
                            }
//                                        mGattCallback.onEffectDisConnected(isActivityDisConnects.get(mac), mac, newState);
                            isEffectiveOption.put(mac,false);
                            isEffectConnectSuccess.put(gatt.getDevice().getAddress(),false);
                            bleLastScanTime.put(mac,TimeUtils.getNowMills());
                            final ScheduledExecutorService executorService1 = Executors.newSingleThreadScheduledExecutor();
                            executorService1.scheduleAtFixedRate(new Runnable() {
                                @Override
                                public void run() {
                                    if (isEffectiveOption.containsKey(mac) && !isEffectiveOption.get(mac)) {
                                        if(!isReciveScanning(mac)) {
                                            LogUtils.d("bleService", "not find this device in 5s  " + mac);
                                            if (mGattCallbacks.containsKey(mac) && mGattCallbacks.get(mac) != null) {
                                                mGattCallbacks.get(mac).onEffectDisConnected(isActivityDisConnects.get(mac), mac, newState);
                                            } else {
                                                LogUtils.d("bleService", "gattcallback is not exsist or is null  " + mGattCallbacks);
                                            }
                                            isEffectiveOption.put(mac, true);
                                            executorService1.shutdownNow();
                                        }else{
                                            if (mGattCallbacks.containsKey(mac) && mGattCallbacks.get(mac) != null) {
                                                mGattCallbacks.get(mac).onCancelAlert(mac);
                                            }
                                            LogUtils.d("bleService", "sanning this device in 5s  "+mac);
                                        }
                                    }else{
                                        LogUtils.d("bleService", "this device already be option  "+mac);
                                        executorService1.shutdownNow();
                                    }
                                }
                            }, 1, 1, TimeUnit.SECONDS);
                            isDisConnectMaps.put(gatt.getDevice().getAddress(), false);
                            if (bluetoothGatts.containsKey(gatt.getDevice().getAddress())) {
                                BluetoothGatt bluetoothGatt = bluetoothGatts.get(gatt.getDevice().getAddress());
                                if (bluetoothGatt != null) {
                                    bluetoothGatt.disconnect();
                                    bluetoothGatt.close();
                                } else {
                                    gatt.disconnect();
                                    gatt.close();
                                }
                                bluetoothGatts.remove(gatt.getDevice().getAddress());
                            }
                        } else if (gatt != null) {
                            if(mGattCallbacks.containsKey(mac)&&mGattCallbacks.get(mac)!=null) {
                                mGattCallbacks.get(mac).onConnectFail(mac);
                            }
                            gatt.disconnect();
                            gatt.close();
                        }
                    }
                }
            }

            @Override
            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                super.onReadRemoteRssi(gatt, rssi, status);
                if(gatt!=null&&gatt.getDevice()!=null&&!TextUtils.isEmpty(gatt.getDevice().getAddress())) {
                    String mac = gatt.getDevice().getAddress();
                    if(mGattCallbacks.containsKey(mac)&&mGattCallbacks.get(mac)!=null) {
                        mGattCallbacks.get(mac).onReadRemoteRssi(mac, rssi, status);
                    }
                    bleLastScanTime.put(mac,TimeUtils.getNowMills());
                }
            }

            @Override
            public synchronized void onServicesDiscovered(final BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (gatt.getServices() != null && gatt.getServices().size() > 0) {
                        String mac = gatt.getDevice().getAddress();
                        if(mGattCallbacks.containsKey(mac)&&mGattCallbacks.get(mac)!=null) {
                            mGattCallbacks.get(mac).onServicesDiscovered(gatt, status);
                        }
                    }
                } else {
//                  gatt.discoverServices();
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                if(gatt==null||gatt.getDevice()==null||TextUtils.isEmpty(gatt.getDevice().getAddress())||characteristic==null){
                    return;
                }
                String mac = gatt.getDevice().getAddress();
                if(deviceNotifyCallbacks.containsKey(mac)&&deviceNotifyCallbacks.get(mac)!=null) {
                    SimpleArrayMap<UUID,BleNotifyCallback> bleNotifyCallBacks = deviceNotifyCallbacks.get(mac);
                    if (bleNotifyCallBacks.containsKey(characteristic.getUuid())) {
                        BleNotifyCallback bleNotifyCallback = bleNotifyCallBacks.get(characteristic.getUuid());
                        if (bleNotifyCallback != null) {
                            bleNotifyCallback.notifyDate(gatt, characteristic);
                        }
                    }
                }

                if(mGattCallbacks.containsKey(mac)&&mGattCallbacks.get(mac)!=null) {
                    mGattCallbacks.get(mac).onCharacteristicChanged(mac, characteristic);
                }

            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
                if(gatt!=null&&gatt.getDevice()!=null&&!TextUtils.isEmpty(gatt.getDevice().getAddress())&&characteristic!=null) {
                    String mac = gatt.getDevice().getAddress();
                    if(deviceReadCallbacks.containsKey(mac)&&deviceReadCallbacks.get(mac)!=null) {
                        SimpleArrayMap<UUID,BleReadCallback> bleReadCallbacks = deviceReadCallbacks.get(mac);
                        if (bleReadCallbacks.containsKey(characteristic.getUuid())) {
                            BleReadCallback bleReadCallback = bleReadCallbacks.get(characteristic.getUuid());
                            if (bleReadCallback != null) {
                                bleReadCallback.onReadResponse(gatt.getDevice(), characteristic, characteristic.getValue());
                            }
                        }
                    }
                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
                if(gatt==null||gatt.getDevice()==null||TextUtils.isEmpty(gatt.getDevice().getAddress())||characteristic==null){
                    return;
                }
                String mac = gatt.getDevice().getAddress();
                if(deviceWriteCallbacks.containsKey(mac)&&deviceWriteCallbacks.get(mac)!=null) {
                    SimpleArrayMap<UUID,BleWriteCallback> bleWriteCallbacks = deviceWriteCallbacks.get(mac);
                    if (bleWriteCallbacks.containsKey(characteristic.getUuid())) {
                        BleWriteCallback bleWriteCallback = bleWriteCallbacks.get(characteristic.getUuid());
                        if (bleWriteCallback != null) {
                            bleWriteCallback.onOptionSucess();
                        }
                    }
                }
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorWrite(gatt, descriptor, status);
                if(gatt==null||gatt.getDevice()==null||TextUtils.isEmpty(gatt.getDevice().getAddress())||descriptor==null){
                    return;
                }
                String mac = gatt.getDevice().getAddress();
                if(deviceNotifyCallbacks.containsKey(mac)&&deviceNotifyCallbacks.get(mac)!=null) {
                    SimpleArrayMap<UUID, BleNotifyCallback> bleNotifyCallbacks = deviceNotifyCallbacks.get(mac);
                    if(descriptor.getCharacteristic()!=null) {
                        if (bleNotifyCallbacks.containsKey(descriptor.getCharacteristic().getUuid())) {
                            BleNotifyCallback baseBleCallback = bleNotifyCallbacks.get(descriptor.getCharacteristic().getUuid());
                            if (baseBleCallback != null) {
                                baseBleCallback.onOptionSucess();
                            }
                        }
                    }
                }
            }
        });
    }

    private boolean isReciveScanning(String mac) {
        if(!bleLastScanTime.containsKey(mac)){
            return false;
        }else{
            return  bleLastScanTime.containsKey(mac)&&(TimeUtils.getTimeSpan(bleLastScanTime.get(mac),TimeUtils.getNowMills(), TimeConstants.SEC)<5);
        }
    }

    public synchronized void setWorkMode(final BluetoothGatt gatt){
        String mac = gatt.getDevice().getAddress();
        if(TextUtils.isEmpty(mac)||!BleClient.getInstance().isConnected(mac)){
            return;
        }
        byte[] dataWork = Common.formatBleMsg(Params.BLEMODE.MODE_WORK, Common.getUserId());
        write(mac, userServiceUUID, userCharacteristicLogUUID, dataWork, new BleWriteCallback(){
            @Override
            public void onWriteSuccess(final BluetoothDevice device) {
                if(isEffectConnectSuccess.containsKey(device.getAddress())&&isEffectConnectSuccess.get(device.getAddress())&&mGattCallbacks.containsKey(device.getAddress())&&mGattCallbacks.get(device.getAddress())!=null){
                    GattCallback gattCallback = mGattCallbacks.get(device.getAddress());
                    gattCallback.onWorkModeSuccess(device);
                    isEffectConnectSuccess.remove(device.getAddress());
                }
            }

            @Override
            public void onWriteFialed(BluetoothDevice device) {
                if(isEffectConnectSuccess.containsKey(device.getAddress())&&isEffectConnectSuccess.get(device.getAddress())&&mGattCallbacks.containsKey(device.getAddress())&&mGattCallbacks.get(device.getAddress())!=null){
                    GattCallback gattCallback = mGattCallbacks.get(device.getAddress());
                    gattCallback.onWorkModeFaild(device);
                    isEffectConnectSuccess.remove(device.getAddress());
                }
                if(mGattCallbacks.containsKey(gatt.getDevice().getAddress())){
                    mGattCallbacks.remove(gatt.getDevice().getAddress());
                }
            }
        });
    }
    public synchronized void setBindMode(final BluetoothGatt gatt){
        final String mac = gatt.getDevice().getAddress();
        if(TextUtils.isEmpty(mac)||!BleClient.getInstance().isConnected(mac)){
            return;
        }
        byte[] bind = Common.formatBleMsg(Params.BLEMODE.MODE_BIND,Common.getUserId());
        write(mac, BleUUIDS.userServiceUUID, BleUUIDS.userCharacteristicLogUUID, bind, new BleWriteCallback() {
            @Override
            public void onWriteSuccess(BluetoothDevice device) {
                if(mGattCallbacks.containsKey(device.getAddress())&&mGattCallbacks.get(device.getAddress())!=null){
                    GattCallback gattCallback = mGattCallbacks.get(device.getAddress());
                    gattCallback.onBindModeSuccess(device);
                }
            }

            @Override
            public void onWriteFialed(BluetoothDevice device) {
                if(mGattCallbacks.containsKey(device.getAddress())&&mGattCallbacks.get(device.getAddress())!=null){
                    GattCallback gattCallback = mGattCallbacks.get(device.getAddress());
                    gattCallback.onBindModeFaild(device);
                }
                if(mGattCallbacks.containsKey(gatt.getDevice().getAddress())){
                    mGattCallbacks.remove(gatt.getDevice().getAddress());
                }
            }
        });
    }

    public synchronized void write(final String mac,
                                   final UUID uuid_service,
                                   final UUID uuid_write,
                                   final byte[] data,
                                   final BleWriteCallback bleWriteCallback){
        BleWriteTask bleWriteTask = new BleWriteTask(mac,uuid_service,uuid_write,data,bleWriteCallback);
        bleTransTaskQueue.add(bleWriteTask);
    }

    public synchronized void write(final String mac,
                                     final UUID uuid_service,
                                     final UUID uuid_write,
                                     final byte[] data,
                                     final boolean isUpdate,
                                     final BleWriteCallback bleWriteCallback){
//        if(isUpdate){
//            writeData(mac,uuid_service,uuid_write,data,bleWriteCallback);
//        }else {
            BleWriteTask bleWriteTask = new BleWriteTask(mac, uuid_service, uuid_write, data, bleWriteCallback);
            bleWriteTask.setIsUpdate(isUpdate);
            bleTransTaskQueue.add(bleWriteTask);
//        }
    }
    public synchronized void writeOffset(final String mac,
                                         final UUID uuid_service,
                                         final UUID uuid_write,
                                         final int memType,
                                         final int formatUint32,
                                         final int offset,
                                         final BleWriteCallback bleWriteCallback){
        BleWriteOffsetTask bleWriteTask = new BleWriteOffsetTask(mac,uuid_service,uuid_write,memType,formatUint32,offset,bleWriteCallback);
        bleTransTaskQueue.add(bleWriteTask);
    }
    public synchronized void writeOffset(final String mac,
                                   final UUID uuid_service,
                                   final UUID uuid_write,
                                   final int memType,
                                   final int formatUint32,
                                   final int offset,
                                   final boolean isUpdate,
                                   final BleWriteCallback bleWriteCallback){
//        if(isUpdate){
//            writeOffsetData(mac,uuid_service,uuid_write,memType,formatUint32,offset,bleWriteCallback);
//        }else {
            BleWriteOffsetTask bleWriteTask = new BleWriteOffsetTask(mac, uuid_service, uuid_write, memType, formatUint32, offset, bleWriteCallback);
            bleWriteTask.setIsUpdate(isUpdate);
            bleTransTaskQueue.add(bleWriteTask);
//        }
    }

    public synchronized void writeData(final String mac,
                                      final UUID uuid_service,
                                      final UUID uuid_write,
                                      final byte[] data,
                                      final BleWriteCallback bleWriteCallback) {
        boolean sucessfull = false;
        if(deviceWriteCallbacks.containsKey(mac)&&deviceWriteCallbacks.get(mac)!=null) {
            SimpleArrayMap<UUID, BleWriteCallback> bleWriteCallbacks = deviceWriteCallbacks.get(mac);
            bleWriteCallbacks.put(uuid_write,bleWriteCallback);
        }else {
            SimpleArrayMap<UUID, BleWriteCallback> bleWriteCallbacks = new SimpleArrayMap<>();
            bleWriteCallbacks.put(uuid_write,bleWriteCallback);
            deviceWriteCallbacks.put(mac,bleWriteCallbacks);
        }
        if(bluetoothGatts.containsKey(mac)&&bluetoothGatts.get(mac)!=null) {
            BluetoothGatt gatt = bluetoothGatts.get(mac);
            if(gatt!=null&&gatt.getDevice()!=null) {
                BluetoothGattService service = gatt.getService(uuid_service);
                if(service!=null) {
                    BluetoothGattCharacteristic gattCharacteristic = service.getCharacteristic(uuid_write);
                    if (gattCharacteristic != null) {
                        gattCharacteristic.setValue(data);
                        sucessfull = gatt.writeCharacteristic(gattCharacteristic);
                    }
                }
            }
            if(sucessfull){
                if(bleWriteCallback!=null){
                    bleWriteCallback.onWriteSuccess(gatt.getDevice());
                }
            }else{
                if(bleWriteCallback!=null){
                    write(mac,uuid_service,uuid_write,data,bleWriteCallback);
                    bleWriteCallback.onWriteFialed(gatt.getDevice());
                }
            }
        }
    }
    public void writeOffsetData(final String mac,
                      final UUID spotaServiceUuid,
                      final UUID spotaMemDevUuid,
                      final int memType,
                      final int formatUint32,
                      final int offset,
                      final BleWriteCallback bleWriteCallback) {
        boolean sucessful = false;
        if(deviceWriteCallbacks.containsKey(mac)&&deviceWriteCallbacks.get(mac)!=null) {
            SimpleArrayMap<UUID, BleWriteCallback> bleNotifyCallbacks = deviceWriteCallbacks.get(mac);
            bleNotifyCallbacks.put(spotaMemDevUuid,bleWriteCallback);
        }else {
            SimpleArrayMap<UUID, BleWriteCallback> bleNotifyCallbacks = new SimpleArrayMap<>();
            bleNotifyCallbacks.put(spotaMemDevUuid,bleWriteCallback);
            deviceWriteCallbacks.put(mac,bleNotifyCallbacks);
        }
        if(bluetoothGatts.containsKey(mac)&&bluetoothGatts.get(mac)!=null) {
            BluetoothGatt gatt = bluetoothGatts.get(mac);
            if(gatt!=null&&gatt.getDevice()!=null) {
                BluetoothGattService service = gatt.getService(spotaServiceUuid);
                if(service!=null) {
                    BluetoothGattCharacteristic gattCharacteristic = service.getCharacteristic(spotaMemDevUuid);
                    if (gattCharacteristic != null) {
                        gattCharacteristic.setValue(memType,formatUint32,offset);
                        sucessful = gatt.writeCharacteristic(gattCharacteristic);
                    }
                }
            }
            if(sucessful){
                if(bleWriteCallback!=null){
                    bleWriteCallback.onWriteSuccess(gatt.getDevice());
                }
            }else{
                if(bleWriteCallback!=null){
                    writeOffset(mac,spotaServiceUuid,spotaMemDevUuid,memType,formatUint32,offset,bleWriteCallback);
                    bleWriteCallback.onWriteFialed(gatt.getDevice());
                }
            }
        }
    }

    public synchronized void read(final String mac,
                                  final UUID uuid_service,
                                  final UUID uuid_characristic,BleReadCallback bleReadCallback) {
        BleReadTask bleReadTask = new BleReadTask(mac,uuid_service,uuid_characristic,bleReadCallback);
        bleTransTaskQueue.add(bleReadTask);
    }

    public synchronized void read(final String mac,
                                    final UUID uuid_service,
                                    final UUID uuid_characristic,
                                    final boolean isUpdate,
                                    BleReadCallback bleReadCallback) {
        BleReadTask bleReadTask = new BleReadTask(mac,uuid_service,uuid_characristic,bleReadCallback);
        bleReadTask.setIsUpdate(isUpdate);
        bleTransTaskQueue.add(bleReadTask);
    }
    public synchronized void readData(final String mac,
                                   final UUID uuid_service,
                                   final UUID uuid_characristic,BleReadCallback bleReadCallback) {
        if(deviceReadCallbacks.containsKey(mac)&&deviceReadCallbacks.get(mac)!=null){
            SimpleArrayMap<UUID,BleReadCallback> bleReadCallBacks = deviceReadCallbacks.get(mac);
            bleReadCallBacks.put(uuid_characristic,bleReadCallback);
        }else{
            SimpleArrayMap<UUID,BleReadCallback> bleReadCallBacks = new SimpleArrayMap<>();
            bleReadCallBacks.put(uuid_characristic,bleReadCallback);
            deviceReadCallbacks.put(mac,bleReadCallBacks);
        }

        if(bluetoothGatts.containsKey(mac)&&bluetoothGatts.get(mac)!=null) {
            BluetoothGatt gatt = bluetoothGatts.get(mac);
            if(gatt!=null&&gatt.getDevice()!=null) {
                BluetoothGattService service = gatt.getService(uuid_service);
                if(service!=null) {
                    BluetoothGattCharacteristic gattCharacteristic = service.getCharacteristic(uuid_characristic);
                    if (gattCharacteristic != null){
                        gatt.readCharacteristic(gattCharacteristic);
                    }
                }
            }
        }
    }
    public synchronized void readRemoteRssi(String mac){
        BleReadRemoteRssiTask bleReadRemoteRssiTask = new BleReadRemoteRssiTask(mac);
        bleTransTaskQueue.add(bleReadRemoteRssiTask);
    }
    public synchronized void readRemoteRssiData(String mac){
        if(bluetoothGatts.containsKey(mac)&&bluetoothGatts.get(mac)!=null) {
            BluetoothGatt gatt = bluetoothGatts.get(mac);
            gatt.readRemoteRssi();
        }
    }
    public synchronized void enableAllNotify(BluetoothDevice device){
        if(device==null||TextUtils.isEmpty(device.getAddress())) {
            return;
        }
        BleNotifyTask bleNotifyTask1 = new BleNotifyTask(device.getAddress(), BleUUIDS.userServiceUUID,BleUUIDS.userCharacteristicLogUUID);
        bleTransTaskQueue.add(bleNotifyTask1);
        BleNotifyTask bleNotifyTask2 = new BleNotifyTask(device.getAddress(), BleUUIDS.userServiceUUID,BleUUIDS.userCharactButtonStateUUID);
        bleTransTaskQueue.add(bleNotifyTask2);
        BleNotifyTask bleNotifyTask3 = new BleNotifyTask(device.getAddress(), BleUUIDS.batServiceUUID,BleUUIDS.batCharacteristicUUID);
        bleTransTaskQueue.add(bleNotifyTask3);

//        String mac = device.getAddress();
//        if(bluetoothGatts.containsKey(mac)&&bluetoothGatts.get(mac)!=null) {
//            BluetoothGatt gatt = bluetoothGatts.get(mac);
//            List<BluetoothGattService> services = gatt.getServices();
//            if (services != null) {
//                for (BluetoothGattService service : services) {
//                    if(service!=null) {
//                        List<BluetoothGattCharacteristic> bluetoothGattCharacteristics = service.getCharacteristics();
//                        if (bluetoothGattCharacteristics != null && bluetoothGattCharacteristics.size() > 0) {
//                            for (BluetoothGattCharacteristic bluetoothGattCharacteristic : bluetoothGattCharacteristics) {
//                                if (bluetoothGattCharacteristic != null) {
//                                    int properties = bluetoothGattCharacteristic.getProperties();
//                                    if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0) {
//                                    } else {
//                                        BleNotifyTask bleNotifyTask = new BleNotifyTask(device.getAddress(), service.getUuid(), bluetoothGattCharacteristic.getUuid());
//                                        bleTransTaskQueue.add(bleNotifyTask);
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
    }
    public void onNotifyListener(String mac,UUID charastic_uuid,BleNotifyCallback bleNotifyCallback){
        if(bleNotifyCallback!=null){
            if(deviceNotifyCallbacks.containsKey(mac)&&deviceNotifyCallbacks.get(mac)!=null){
                SimpleArrayMap<UUID,BleNotifyCallback> bleNotifyCallBacks = deviceNotifyCallbacks.get(mac);
                bleNotifyCallBacks.put(charastic_uuid,bleNotifyCallback);
            }else{
                SimpleArrayMap<UUID,BleNotifyCallback> bleNotifyCallBacks = new SimpleArrayMap<>();
                bleNotifyCallBacks.put(charastic_uuid,bleNotifyCallback);
                deviceNotifyCallbacks.put(mac,bleNotifyCallBacks);
            }
        }
    }
    public void notify(String mac,UUID service_uuid,UUID charistic_uuid){
        BleNotifyTask bleNotifyTask = new BleNotifyTask(mac,service_uuid,charistic_uuid);
        bleTransTaskQueue.add(bleNotifyTask);
    }
    public void notify(String mac,UUID service_uuid,UUID charistic_uuid,boolean isUpdate){
//        if(isUpdate) {
//            notifyData(mac,service_uuid,charistic_uuid);
//        }else{
            BleNotifyTask bleNotifyTask = new BleNotifyTask(mac, service_uuid, charistic_uuid);
            bleNotifyTask.setIsUpdate(isUpdate);
            bleTransTaskQueue.add(bleNotifyTask);
//        }
    }
    public synchronized void notifyData(final String mac, final UUID service_uuid, final UUID charastic_uuid) {
        if(bluetoothGatts.containsKey(mac)&&bluetoothGatts.get(mac)!=null) {
            BluetoothGatt gatt = bluetoothGatts.get(mac);
            BluetoothGattService service = gatt.getService(service_uuid);
            if (service != null) {
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(charastic_uuid);
                if(characteristic!=null) {
                    gatt.setCharacteristicNotification(characteristic, true);
                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(BleUUIDS.SPOTA_DESCRIPTOR_UUID);
                    if (descriptor != null) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    }
                }
            }
        }
    }

    public synchronized boolean isConnected(String mac){
        if(bluetoothGatts.containsKey(mac)&&bluetoothGatts.get(mac)!=null){
            BluetoothDevice bluetoothDevice = bluetoothGatts.get(mac).getDevice();
            if(bluetoothManager!=null&&bluetoothDevice!=null) {            ;
                return bluetoothManager.getConnectionState(bluetoothDevice,BluetoothGatt.GATT)==BluetoothGatt.STATE_CONNECTED;
            }
        }
        return false;
    }
    public synchronized int getConnectState(String mac){
        if(bluetoothGatts.containsKey(mac)&&bluetoothGatts.get(mac)!=null) {
            BluetoothDevice bluetoothDevice = bluetoothGatts.get(mac).getDevice();
            if (bluetoothManager != null&&bluetoothDevice!=null) {                ;
                return bluetoothManager.getConnectionState(bluetoothDevice, BluetoothGatt.GATT);
            }
        }
        return BluetoothGatt.STATE_DISCONNECTED;
    }

    public synchronized boolean isScaning(){
        return  isScaning;
    }

    public synchronized void disConnect(String mac){
        synchronized (bluetoothGatts) {
            if (bluetoothGatts.containsKey(mac) && bluetoothGatts.get(mac) != null) {
                BluetoothGatt mBluetoothGatt = bluetoothGatts.get(mac);
                isActivityDisConnects.put(mac, true);
                mBluetoothGatt.disconnect();
                mBluetoothGatt.close();
                bluetoothGatts.remove(mac);
            }
        }
    }

    public synchronized void disconnectAllDevice() {
        synchronized (bluetoothGatts) {
            for (int i=0;i<bluetoothGatts.size();i++) {
                BluetoothGatt bluetoothGatt = bluetoothGatts.valueAt(i);
                if (bluetoothGatt != null) {
                    isActivityDisConnects.put(bluetoothGatt.getDevice().getAddress(), true);
                    bluetoothGatt.disconnect();
                    bluetoothGatt.close();
                }
            }
            bluetoothGatts.clear();
            mGattCallbacks.clear();
            isDisConnectMaps.clear();
        }
    }


    public boolean isEnabled() {
        return mBluetoothAdapter!=null&&mBluetoothAdapter.isEnabled();
    }

    public boolean disableBluetooth() {
        return mBluetoothAdapter!=null&&mBluetoothAdapter.disable();
    }

    public void disconnectAllWithLinklose(byte[] data) {
        for(int i=0;i<bluetoothGatts.size();i++){
            final BluetoothGatt bluetoothGatt = bluetoothGatts.valueAt(i);
            if(bluetoothGatt!=null){
                if(bluetoothGatt.getDevice()!=null){
                    if(isConnected(bluetoothGatt.getDevice().getAddress())){
                        write(bluetoothGatt.getDevice().getAddress(), BleUUIDS.linkLossUUID, BleUUIDS.characteristicUUID, data, new BleWriteCallback() {
                            @Override
                            public void onWriteSuccess(BluetoothDevice device) {
                                isActivityDisConnects.put(device.getAddress(),true);
                                bluetoothGatt.disconnect();
                                bluetoothGatt.close();
                            }

                            @Override
                            public void onWriteFialed(BluetoothDevice device) {
                                isActivityDisConnects.put(bluetoothGatt.getDevice().getAddress(),true);
                                bluetoothGatt.disconnect();
                                bluetoothGatt.close();
                            }
                        });
                    }else{
                        isActivityDisConnects.put(bluetoothGatt.getDevice().getAddress(),true);
                        bluetoothGatt.disconnect();
                        bluetoothGatt.close();
                    }
                }else{
                    isActivityDisConnects.put(bluetoothGatt.getDevice().getAddress(),true);
                    bluetoothGatt.disconnect();
                    bluetoothGatt.close();
                }
            }
        }
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        bluetoothGatts.clear();
        mGattCallbacks.clear();
        isDisConnectMaps.clear();
    }

    public void enaableBluetooth() {
        if(mBluetoothAdapter!=null){
            mBluetoothAdapter.enable();
        }
    }
    public static boolean refresh(BluetoothGatt gatt) {
        try {
            Log.d("bleclient", "refresh device cache");
            Method localMethod = gatt.getClass().getMethod("refresh", (Class[]) null);
            if (localMethod != null) {
                boolean result = (Boolean) localMethod.invoke(gatt, (Object[]) null);
                if (!result)
                    Log.d("bleclient", "refresh failed");
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("bleclient", "An exception occurred while refreshing device cache");
        }
        return false;
    }

    public void diableBluetooth() {
        synchronized (bluetoothGatts) {
            for (int i=0;i<bluetoothGatts.size();i++) {
                BluetoothGatt bluetoothGatt = bluetoothGatts.valueAt(i);
                if (bluetoothGatt != null) {
                    String mac = "";
                    if(bluetoothGatt.getDevice()!=null){
                        mac = bluetoothGatt.getDevice().getAddress();
                    }
                    isActivityDisConnects.put(mac, true);
//                    if(mGattCallbacks.containsKey(mac)&&mGattCallbacks.get(mac)!=null) {
//                        mGattCallbacks.get(mac).onEffectDisConnected(false, mac, BluetoothGatt.STATE_DISCONNECTED);
//                    }
                    bluetoothGatt.disconnect();
                    bluetoothGatt.close();
                }
            }
            bluetoothGatts.clear();
            mGattCallbacks.clear();
            isDisConnectMaps.clear();
        }
    }

    public void restartBle() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(mBluetoothAdapter!=null){
                    mBluetoothAdapter.disable();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    while (mBluetoothAdapter.getState()==BluetoothAdapter.STATE_OFF){
                        mBluetoothAdapter.enable();
                        continue;
                    }
                }
            }
        }).start();

    }

    public BluetoothGatt getBluetoothGatt(String mac) {
        if(bluetoothGatts.containsKey(mac)){
            return bluetoothGatts.get(mac);
        }
        return null;
    }

    public void requestConnectionPriority(String mac,int priority) {
        if (Build.VERSION.SDK_INT >= 21) {
            if (bluetoothGatts.containsKey(mac) && bluetoothGatts.get(mac) != null) {
                BluetoothGatt gatt = bluetoothGatts.get(mac);
                gatt.requestConnectionPriority(priority);
            }
        }

    }
}
