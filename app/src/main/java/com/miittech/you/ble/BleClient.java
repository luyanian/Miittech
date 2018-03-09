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
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.SimpleArrayMap;
import android.text.TextUtils;
import android.util.Log;
import com.miittech.you.App;
import com.miittech.you.ble.gatt.BaseOptionCallback;
import com.miittech.you.ble.gatt.BleNotifyCallback;
import com.miittech.you.ble.gatt.BleReadCallback;
import com.miittech.you.ble.gatt.BleWriteCallback;
import com.miittech.you.ble.gatt.GattCallback;
import com.miittech.you.ble.scan.BleLeScanCallback;
import com.miittech.you.ble.scan.BleScanCallback;
import com.miittech.you.ble.scan.ScanResultCallback;
import com.miittech.you.entity.DeviceInfo;
import com.miittech.you.fragment.ListFragment;
import com.miittech.you.global.IntentExtras;
import com.miittech.you.global.Params;
import com.miittech.you.utils.BingGoPlayUtils;
import com.miittech.you.utils.Common;
import com.ryon.mutils.LogUtils;
import com.ryon.mutils.SPUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
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
    private SimpleArrayMap<UUID,BleReadCallback> bleReadCallbacks = new SimpleArrayMap<>();
    private SimpleArrayMap<String, Boolean> isBinds = new SimpleArrayMap<String, Boolean>();
    private SimpleArrayMap<String, Boolean> isEffectConnectSuccess = new SimpleArrayMap<String, Boolean>();
    private SimpleArrayMap<UUID,BleWriteCallback> bleWriteCallbacks = new SimpleArrayMap<>();
    private SimpleArrayMap<UUID,BleNotifyCallback> bleNotifyCallbacks = new SimpleArrayMap<>();
    private Context context;
    private SimpleArrayMap<String,Boolean> isActivityDisConnects = new SimpleArrayMap();
    private SimpleArrayMap<String,BluetoothGatt> bluetoothGatts = new SimpleArrayMap<>();
    private SimpleArrayMap<String,Boolean> isDisConnectMaps = new SimpleArrayMap<>();
    private SimpleArrayMap<String,Boolean> isEffectiveOption = new SimpleArrayMap<>();
    private boolean isScaning = false;
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
    public synchronized void connectDevice(BluetoothDevice mDevice, GattCallback mGattCallback){
//        if (mDevice == null) {
//            mGattCallback.onConnectFail(mDevice.getAddress());
//            return;
//        }
//        if(mGattCallbacks.containsKey(mDevice.getAddress())&&mGattCallbacks.get(mDevice.getAddress())!=null){
//            LogUtils.d("bleService", mDevice.getAddress()+" has another connectting options,cancle current option");
//            return;
//        }

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
                                boolean isBind = mGattCallbacks.get(gatt.getDevice().getAddress()).onEffectConnectSuccess(gatt.getDevice().getAddress(), status);
                                isEffectConnectSuccess.put(gatt.getDevice().getAddress(),true);
                                isBinds.put(gatt.getDevice().getAddress(),isBind);
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
                            ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
                            executorService.schedule(new Runnable() {
                                @Override
                                public void run() {
                                    LogUtils.d("bleService", mac+" in not reconnect in 5s isEffectiveOption--->"+isEffectiveOption.get(mac));
//                                    synchronized (isEffectiveOption) {
    //                                                mGattCallback.onDisConnected(isActivityDisConnects.get(mac), mac, newState);
                                        if (isEffectiveOption.containsKey(mac) && !isEffectiveOption.get(mac)) {
                                            if (mGattCallbacks.containsKey(mac) && mGattCallbacks.get(mac) != null) {
                                                mGattCallbacks.get(mac).onEffectDisConnected(isActivityDisConnects.get(mac), mac, newState);
                                            }else{
                                                LogUtils.d("bleService", "gattcallback is not exsist or is null  "+mGattCallbacks);
                                            }
                                            isEffectiveOption.put(mac, true);

//                                            if(mGattCallbacks.containsKey(gatt.getDevice().getAddress())){
//                                                mGattCallbacks.remove(gatt.getDevice().getAddress());
//                                            }
                                        } else {
                                            LogUtils.d("bleService", "device reconnected in short time  isEffectiveOption-->true"+"    "+gatt.getDevice().getAddress());
                                        }
//                                }
//                                              executorService.shutdown();
                                }
                            },5, TimeUnit.SECONDS);
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
                }
            }

            @Override
            public synchronized void onServicesDiscovered(final BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (gatt.getServices() != null && gatt.getServices().size() > 0) {
                        if (isBinds.containsKey(gatt.getDevice().getAddress())&&isBinds.get(gatt.getDevice().getAddress())) {
                            setBindMode(gatt);
                        } else {
                            setWorkMode(gatt);
                        }
//                      mGattCallback.onEffectConnectSuccess(gatt.getDevice().getAddress(), status);
                    }
                } else {
//                  gatt.discoverServices();
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                if(bleNotifyCallbacks.containsKey(characteristic.getUuid())){
                    BleNotifyCallback bleNotifyCallback = bleNotifyCallbacks.get(characteristic.getUuid());
                    if(bleNotifyCallback!=null){
                        bleNotifyCallback.notifyDate(gatt, characteristic);
                    }
                }
                if(gatt!=null&&gatt.getDevice()!=null&&!TextUtils.isEmpty(gatt.getDevice().getAddress())) {
                    String mac = gatt.getDevice().getAddress();
                    if(mGattCallbacks.containsKey(mac)&&mGattCallbacks.get(mac)!=null) {
                        mGattCallbacks.get(mac).onCharacteristicChanged(mac, characteristic);
                    }
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
                if(gatt!=null&&gatt.getDevice()!=null&&!TextUtils.isEmpty(gatt.getDevice().getAddress())) {
                    if(bleReadCallbacks.containsKey(characteristic.getUuid())){
                        BleReadCallback bleReadCallback = bleReadCallbacks.get(characteristic.getUuid());
                        if(bleReadCallback!=null){
                            bleReadCallback.onReadResponse(characteristic.getValue());
                        }
                    }
                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
                if(bleWriteCallbacks.containsKey(characteristic.getUuid())){
                    BleWriteCallback bleWriteCallback = bleWriteCallbacks.get(characteristic.getUuid());
                    if(bleWriteCallback!=null){
                        bleWriteCallback.onOptionSucess();
                    }
                }
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorWrite(gatt, descriptor, status);
                if(bleNotifyCallbacks.containsKey(descriptor.getCharacteristic().getUuid())){
                    BleNotifyCallback baseBleCallback = bleNotifyCallbacks.get(descriptor.getCharacteristic().getUuid());
                    if(baseBleCallback!=null){
                        baseBleCallback.onOptionSucess();
                    }
                }
            }
        });
    }

    public synchronized void setWorkMode(final BluetoothGatt gatt){
        String mac = gatt.getDevice().getAddress();
        if(TextUtils.isEmpty(mac)||!BleClient.getInstance().isConnected(mac)){
            return;
        }
        byte[] dataWork = Common.formatBleMsg(Params.BLEMODE.MODE_WORK, Common.getUserId());
        BleClient.getInstance().write(mac, userServiceUUID, userCharacteristicLogUUID, dataWork, new BleWriteCallback(){
            @Override
            public void onWriteSuccess(final BluetoothDevice device) {
                if(isEffectConnectSuccess.containsKey(device.getAddress())&&isEffectConnectSuccess.get(device.getAddress())&&mGattCallbacks.containsKey(device.getAddress())&&mGattCallbacks.get(device.getAddress())!=null){
                    GattCallback gattCallback = mGattCallbacks.get(device.getAddress());
                    gattCallback.onWorkModeSuccess(device);
                    isEffectConnectSuccess.remove(device.getAddress());
                }
                setNotify(gatt);
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
        BleClient.getInstance().write(mac, BleUUIDS.userServiceUUID, BleUUIDS.userCharacteristicLogUUID, bind, new BleWriteCallback() {
            @Override
            public void onWriteSuccess(BluetoothDevice device) {
                if(mGattCallbacks.containsKey(device.getAddress())&&mGattCallbacks.get(device.getAddress())!=null){
                    GattCallback gattCallback = mGattCallbacks.get(device.getAddress());
                    gattCallback.onBindModeSuccess(device);
                }
                isBinds.remove(mac);
                setNotify(gatt);
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
                isBinds.remove(mac);
            }
        });
    }

    public synchronized void write(final String mac,
                                      final UUID uuid_service,
                                      final UUID uuid_write,
                                      final byte[] data,
                                      final BleWriteCallback bleWriteCallback) {
        bleWriteCallbacks.put(uuid_write,bleWriteCallback);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(bluetoothGatts.containsKey(mac)&&bluetoothGatts.get(mac)!=null) {
                    BluetoothGatt gatt = bluetoothGatts.get(mac);
                    if(gatt!=null&&gatt.getDevice()!=null) {
                        BluetoothGattService bluetoothGattServer = gatt.getService(uuid_service);
                        if(bluetoothGattServer!=null) {
                            BluetoothGattCharacteristic gattCharacteristic = bluetoothGattServer.getCharacteristic(uuid_write);
                            if (gattCharacteristic != null) {
                                gattCharacteristic.setValue(data);
                                if(gatt.writeCharacteristic(gattCharacteristic)){
                                    if(bleWriteCallback!=null){
                                        bleWriteCallback.onWriteSuccess(gatt.getDevice());
                                    }
                                }else{
                                    if(bleWriteCallback!=null){
                                        bleWriteCallback.onWriteFialed(gatt.getDevice());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }).start();
    }
    public void write(final String mac,
                      final UUID spotaServiceUuid,
                      final UUID spotaMemDevUuid,
                      final int memType,
                      final int formatUint32,
                      final int offset,
                      final BleWriteCallback bleWriteCallback) {
        bleWriteCallbacks.put(spotaMemDevUuid,bleWriteCallback);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(bluetoothGatts.containsKey(mac)&&bluetoothGatts.get(mac)!=null) {
                    BluetoothGatt gatt = bluetoothGatts.get(mac);
                    if(gatt!=null&&gatt.getDevice()!=null) {
                        BluetoothGattService bluetoothGattServer = gatt.getService(spotaServiceUuid);
                        if(bluetoothGattServer!=null) {
                            BluetoothGattCharacteristic gattCharacteristic = bluetoothGattServer.getCharacteristic(spotaMemDevUuid);
                            if (gattCharacteristic != null) {
                                gattCharacteristic.setValue(memType,formatUint32,offset);
                                if(gatt.writeCharacteristic(gattCharacteristic)){
                                    if(bleWriteCallback!=null){
                                        bleWriteCallback.onWriteSuccess(gatt.getDevice());
                                    }
                                }else{
                                    if(bleWriteCallback!=null){
                                        bleWriteCallback.onWriteFialed(gatt.getDevice());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }).start();
    }
    public synchronized void writeNonThread(final String mac,
                                   final UUID uuid_service,
                                   final UUID uuid_write,
                                   final byte[] data,
                                   final BleWriteCallback bleWriteCallback) {
        bleWriteCallbacks.put(uuid_write,bleWriteCallback);
        if(bluetoothGatts.containsKey(mac)&&bluetoothGatts.get(mac)!=null) {
            BluetoothGatt gatt = bluetoothGatts.get(mac);
            if(gatt!=null&&gatt.getDevice()!=null) {
                BluetoothGattService bluetoothGattServer = gatt.getService(uuid_service);
                if(bluetoothGattServer!=null) {
                    BluetoothGattCharacteristic gattCharacteristic = bluetoothGattServer.getCharacteristic(uuid_write);
                    if (gattCharacteristic != null) {
                        gattCharacteristic.setValue(data);
                        if(gatt.writeCharacteristic(gattCharacteristic)){
                            if(bleWriteCallback!=null){
                                bleWriteCallback.onWriteSuccess(gatt.getDevice());
                            }
                        }else{
                            if(bleWriteCallback!=null){
                                bleWriteCallback.onWriteFialed(gatt.getDevice());
                            }
                        }
                    }
                }
            }
        }
    }
    public void writeNonThread(final String mac,
                      final UUID spotaServiceUuid,
                      final UUID spotaMemDevUuid,
                      final int memType,
                      final int formatUint32,
                      final int offset,
                      final BleWriteCallback bleWriteCallback) {
        bleWriteCallbacks.put(spotaMemDevUuid,bleWriteCallback);
        if(bluetoothGatts.containsKey(mac)&&bluetoothGatts.get(mac)!=null) {
            BluetoothGatt gatt = bluetoothGatts.get(mac);
            if(gatt!=null&&gatt.getDevice()!=null) {
                BluetoothGattService bluetoothGattServer = gatt.getService(spotaServiceUuid);
                if(bluetoothGattServer!=null) {
                    BluetoothGattCharacteristic gattCharacteristic = bluetoothGattServer.getCharacteristic(spotaMemDevUuid);
                    if (gattCharacteristic != null) {
                        gattCharacteristic.setValue(memType,formatUint32,offset);
                        if(gatt.writeCharacteristic(gattCharacteristic)){
                            if(bleWriteCallback!=null){
                                bleWriteCallback.onWriteSuccess(gatt.getDevice());
                            }
                        }else{
                            if(bleWriteCallback!=null){
                                bleWriteCallback.onWriteFialed(gatt.getDevice());
                            }
                        }
                    }
                }
            }
        }
    }
    public synchronized void read(final String mac,
                                   final UUID uuid_service,
                                   final UUID uuid_characristic,BleReadCallback bleReadCallback) {
        this.bleReadCallbacks.put(uuid_characristic,bleReadCallback);
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(bluetoothGatts.containsKey(mac)&&bluetoothGatts.get(mac)!=null) {
                    BluetoothGatt gatt = bluetoothGatts.get(mac);
                    if(gatt!=null&&gatt.getDevice()!=null) {
                        BluetoothGattService bluetoothGattServer = gatt.getService(uuid_service);
                        if(bluetoothGattServer!=null) {
                            BluetoothGattCharacteristic gattCharacteristic = bluetoothGattServer.getCharacteristic(uuid_characristic);
                            if (gattCharacteristic != null){
                                gatt.readCharacteristic(gattCharacteristic);
                            }
                        }
                    }
                }
            }
        }).start();
    }
    public synchronized boolean readRssi(String mac){
        if(bluetoothGatts.containsKey(mac)&&bluetoothGatts.get(mac)!=null) {
            BluetoothGatt gatt = bluetoothGatts.get(mac);
            return gatt.readRemoteRssi();
        }
        return false;
    }

    public synchronized void setNotify(final BluetoothGatt gatt){
        gatt.readRemoteRssi();
        if (gatt != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (gatt) {
                        List<BluetoothGattService> services = gatt.getServices();
                        if (services == null) {
                            return;
                        }
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        for (BluetoothGattService service : services) {
                            List<BluetoothGattCharacteristic> bluetoothGattCharacteristics = service.getCharacteristics();
                            if (bluetoothGattCharacteristics != null&&bluetoothGattCharacteristics.size()>0) {
                                for (BluetoothGattCharacteristic bluetoothGattCharacteristic : bluetoothGattCharacteristics) {
                                    if (bluetoothGattCharacteristic != null) {
                                        int properties = bluetoothGattCharacteristic.getProperties();
                                        if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0) {

                                        } else {
                                            try {
                                                Thread.sleep(300);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                            boolean success1 = gatt.setCharacteristicNotification(bluetoothGattCharacteristic, true);
                                            LogUtils.d("bleService", gatt.getDevice().getAddress() + "--->setCharacteristicNotification = " + success1);
                                            if(success1) {
                                                final BluetoothGattDescriptor descriptor = bluetoothGattCharacteristic.getDescriptor(BleUUIDS.CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
                                                if(descriptor != null) {
                                                    try {
                                                        Thread.sleep(300);
                                                    } catch (InterruptedException e) {
                                                        e.printStackTrace();
                                                    }
                                                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                                    boolean success2 = gatt.writeDescriptor(descriptor);
                                                    LogUtils.d("bleService", gatt.getDevice().getAddress() + "--->writeDescriptor:NOTIFICATION_VALUE = " + success2);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }).start();
        }

//        new Thread(new Runnable() {
//            @Override
//            public synchronized void run() {
//                try {
//                    Thread.sleep(500);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//
//                if (gatt != null) {
//                    BluetoothGattService buttonGattServer = gatt.getService(BleUUIDS.userServiceUUID);
//                    if (buttonGattServer != null) {
//                        BluetoothGattCharacteristic buttonCharacteristic = buttonGattServer.getCharacteristic(BleUUIDS.userCharactButtonStateUUID);
//                        if(buttonCharacteristic!=null) {
//                            final int properties = buttonCharacteristic.getProperties();
//                            if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0) {
//
//                            } else {
//                                boolean success1 = gatt.setCharacteristicNotification(buttonCharacteristic, true);
//                                LogUtils.d("bleService", gatt.getDevice().getAddress() + "--->setCharacteristicNotification = " + success1);
//                                try {
//                                    Thread.sleep(300);
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
//                                final BluetoothGattDescriptor descriptor = buttonCharacteristic.getDescriptor(BleUUIDS.CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
//                                if (descriptor != null) {
//                                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//                                    boolean success2 = gatt.writeDescriptor(descriptor);
//                                    LogUtils.d("bleService", gatt.getDevice().getAddress() + "--->writeDescriptor:NOTIFICATION_VALUE = " + success2);
//                                }
//                            }
//                        }
//
//                    }
//                    try {
//                        Thread.sleep(500);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    BluetoothGattService battaryGattServer = gatt.getService(BleUUIDS.batServiceUUID);
//                    BluetoothGattCharacteristic batCharacteristic = battaryGattServer.getCharacteristic(BleUUIDS.batCharacteristicUUID);
//                    if(batCharacteristic!=null) {
//                        // Check characteristic property
//                        final int properties = batCharacteristic.getProperties();
//                        if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0){
//
//                        }else{
//                            boolean success1 = gatt.setCharacteristicNotification(batCharacteristic, true);
//                            LogUtils.d("bleService", gatt.getDevice().getAddress() + "--->setCharacteristicNotification = " + success1);
//                            try {
//                                Thread.sleep(300);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                            final BluetoothGattDescriptor descriptor = batCharacteristic.getDescriptor(BleUUIDS.CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
//                            if (descriptor != null) {
//                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//                                boolean success2 = gatt.writeDescriptor(descriptor);
//                                LogUtils.d("bleclient", gatt.getDevice().getAddress() + "--->writeDescriptor:NOTIFICATION_VALUE = " + success2);
//                            }
//                        }
//                    }
//                }
//            }
//        }).start();
    }

    public void notify(final String mac, final UUID charastic_uuid, final BleNotifyCallback bleNotifyCallback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                LogUtils.d("bleclient", "- Enable notifications for SPOTA_SERV_STATUS characteristic");
                if(bluetoothGatts.containsKey(mac)&&bluetoothGatts.get(mac)!=null) {
                    BluetoothGatt gatt = bluetoothGatts.get(mac);
                    // Get the service status UUID from the gatt and enable notifications
                    List<BluetoothGattService> services = gatt.getServices();
                    for (BluetoothGattService service : services) {
                        LogUtils.d("bleclient", "  Found service: " + service.getUuid().toString());
                        List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                        for (BluetoothGattCharacteristic characteristic : characteristics) {
                            LogUtils.d("bleclient", "  Found characteristic: " + characteristic.getUuid().toString());
                            if (characteristic.getUuid().equals(charastic_uuid)) {
                                if(bleNotifyCallback!=null){
                                    bleNotifyCallbacks.put(charastic_uuid,bleNotifyCallback);
                                    bleNotifyCallbacks.put(BleUUIDS.SPOTA_DESCRIPTOR_UUID,bleNotifyCallback);
                                }
                                LogUtils.d("bleclient", "*** Found SUOTA service");
                                gatt.setCharacteristicNotification(characteristic, true);
                                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                                        BleUUIDS.SPOTA_DESCRIPTOR_UUID);
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                gatt.writeDescriptor(descriptor);
                            }
                        }
                    }
                }
            }
        }).start();
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
                        BleClient.getInstance().write(bluetoothGatt.getDevice().getAddress(), BleUUIDS.linkLossUUID, BleUUIDS.characteristicUUID, data, new BleWriteCallback() {
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
}
