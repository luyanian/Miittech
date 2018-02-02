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
import android.support.v4.util.SimpleArrayMap;
import android.text.TextUtils;
import android.util.Log;
import com.miittech.you.App;
import com.ryon.mutils.LogUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Administrator on 2018/1/10.
 */

public class BleClient {
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
//    private BluetoothLeScanner bluetoothLeScanner;
    private BleScanCallback bleScanCallback;
    private BleLeScanCallback bleLeScanCallback;
    private GattCallback mGattCallback;
    private Context context;
    private SimpleArrayMap<String,Boolean> isActivityDisConnects = new SimpleArrayMap();
    private SimpleArrayMap<String,BluetoothGatt> bluetoothGatts = new SimpleArrayMap<>();
    private SimpleArrayMap<String,Boolean> isDisConnectMaps = new SimpleArrayMap<>();
    private SimpleArrayMap<String,Boolean> isEffectiveOption = new SimpleArrayMap<>();
    private List<String> mConnecttingList=new ArrayList<>();
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
                        isScaning = false;
                        mBluetoothAdapter.stopLeScan(bleLeScanCallback);
                        bleLeScanCallback=null;
                    } else{
                        isScaning = true;
                        bleLeScanCallback = new BleLeScanCallback(scanResultCallback);
                        boolean isStart = mBluetoothAdapter.startLeScan(bleLeScanCallback);
                        LogUtils.d("bleservice", "mBluetoothAdapter.startLeScan-->" + isStart);
                    }
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
    public synchronized void connectDevice(BluetoothDevice mDevice, final GattCallback mGattCallback){
        this.mGattCallback = mGattCallback;
        synchronized (mConnecttingList) {
            if (mConnecttingList.contains(mDevice.getAddress())) {
                return;
            }
            mConnecttingList.add(mDevice.getAddress());
            if(!mGattCallback.onStartConnect(mDevice.getAddress())){
                return;
            }

            if(bluetoothGatts.containsKey(mDevice.getAddress())){
                mGattCallback.onConnectFail(mDevice.getAddress());
                if (mConnecttingList.contains(mDevice.getAddress())) {
                    mConnecttingList.remove(mDevice.getAddress());
                }
                return;
            }
            if (mDevice == null) {
                mGattCallback.onConnectFail(mDevice.getAddress());
                if (mConnecttingList.contains(mDevice.getAddress())) {
                    mConnecttingList.remove(mDevice.getAddress());
                }
                return;
            }
            mDevice.connectGatt(context, false, new BluetoothGattCallback() {
                @Override
                public synchronized void onConnectionStateChange(final BluetoothGatt gatt, int status, final int newState) {
                    super.onConnectionStateChange(gatt, status, newState);
                    synchronized (mConnecttingList) {
                        if (status==BluetoothGatt.GATT_SUCCESS&&newState == BluetoothProfile.STATE_CONNECTED) {
                            gatt.discoverServices();
                        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                            if(mBluetoothAdapter.isEnabled()) {
                                if (gatt != null&&gatt.getDevice()!=null&&!TextUtils.isEmpty(gatt.getDevice().getAddress())) {
                                    final String mac = gatt.getDevice().getAddress();
                                    if (mConnecttingList.contains(gatt.getDevice().getAddress())
                                            && isActivityDisConnects.containsKey(mac) && !isConnected(gatt.getDevice().getAddress())
                                            && isDisConnectMaps.containsKey(mac) && isDisConnectMaps.get(mac)) {
                                        mGattCallback.onDisConnected(isActivityDisConnects.get(mac), mac, newState);
                                        isEffectiveOption.put(mac,false);
                                        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
                                        executorService.schedule(new Runnable() {
                                            @Override
                                            public void run() {
//                                                mGattCallback.onDisConnected(isActivityDisConnects.get(mac), mac, newState);
                                                if(isEffectiveOption.containsKey(mac)&&!isEffectiveOption.get(mac)) {
                                                    LogUtils.d("bleService","onDisConnected  isEffectiveOption-->false");
                                                    mGattCallback.onEffectDisConnected(isActivityDisConnects.get(mac), mac, newState);
                                                    isEffectiveOption.put(mac,true);
                                                }else{
                                                    LogUtils.d("bleService","onDisConnected  isEffectiveOption-->true");
                                                }
//                                              executorService.shutdown();
                                            }
                                        },5,TimeUnit.SECONDS);
                                        isDisConnectMaps.put(gatt.getDevice().getAddress(), false);
                                        if (bluetoothGatts.containsKey(gatt.getDevice().getAddress())) {
                                            BluetoothGatt bluetoothGatt = bluetoothGatts.get(gatt.getDevice().getAddress());
                                            if (bluetoothGatt != null) {
                                                refresh(bluetoothGatt);
                                                bluetoothGatt.disconnect();
                                                bluetoothGatt.close();
                                            } else {
                                                refresh(gatt);
                                                gatt.disconnect();
                                                gatt.close();
                                            }
                                            bluetoothGatts.remove(gatt.getDevice().getAddress());
                                        }
                                    } else if (gatt != null) {
                                        mGattCallback.onConnectFail(mac);
                                        refresh(gatt);
                                        gatt.disconnect();
                                        gatt.close();
                                    }
                                }
                            }
                            if (mConnecttingList.contains(gatt.getDevice().getAddress())) {
                                mConnecttingList.remove(gatt.getDevice().getAddress());
                            }
                        }
                    }
                }

                @Override
                public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                    super.onReadRemoteRssi(gatt, rssi, status);
                    if(gatt!=null&&gatt.getDevice()!=null&&!TextUtils.isEmpty(gatt.getDevice().getAddress())) {
                        mGattCallback.onReadRemoteRssi(gatt.getDevice().getAddress(), rssi, status);
                    }
                }

                @Override
                public synchronized void onServicesDiscovered(final BluetoothGatt gatt, int status) {
                    super.onServicesDiscovered(gatt, status);
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        if (gatt.getServices() != null && gatt.getServices().size() > 0) {
                            if (bluetoothGatts.containsKey(gatt.getDevice().getAddress())) {
                                BluetoothGatt bluetoothGatt = bluetoothGatts.get(gatt.getDevice().getAddress());
                                if (bluetoothGatt != null) {
                                    refresh(bluetoothGatt);
                                    bluetoothGatt.disconnect();
                                    bluetoothGatt.close();
                                }
                            }
                            bluetoothGatts.put(gatt.getDevice().getAddress(), gatt);
                            isActivityDisConnects.put(gatt.getDevice().getAddress(), false);
                            isDisConnectMaps.put(gatt.getDevice().getAddress(), true);
                            if (isEffectiveOption.containsKey(gatt.getDevice().getAddress())&&!isEffectiveOption.get(gatt.getDevice().getAddress())){
                                LogUtils.d("bleService","onServicesDiscovered  isEffectiveOption-->false");
                                mGattCallback.onConnectSuccess(gatt.getDevice().getAddress(), status);
                                isEffectiveOption.put(gatt.getDevice().getAddress(),true);
                            }else{
                                LogUtils.d("bleService","onServicesDiscovered  isEffectiveOption-->true");
                                mGattCallback.onEffectConnectSuccess(gatt.getDevice().getAddress(), status);
                            }
                            setNotify(gatt);
                        }
                    } else {
                        gatt.discoverServices();
                    }
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    super.onCharacteristicChanged(gatt, characteristic);
                    if(gatt!=null&&gatt.getDevice()!=null&&!TextUtils.isEmpty(gatt.getDevice().getAddress())) {
                        mGattCallback.onCharacteristicChanged(gatt.getDevice().getAddress(), characteristic);
                    }
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicWrite(gatt, characteristic, status);
                    if(gatt!=null&&gatt.getDevice()!=null&&!TextUtils.isEmpty(gatt.getDevice().getAddress())) {
                        mGattCallback.onCharacteristicWrite(gatt.getDevice().getAddress(), characteristic, status);
                    }
                }
            });
        }
    }
    public synchronized void write(final String mac,
                                      final UUID uuid_service,
                                      final UUID uuid_write,
                                      final byte[] data,
                                      final BleWriteCallback bleWriteCallback) {
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
    public synchronized void read(final String mac,
                                   final String uuid_service,
                                   final String uuid_write,
                                   final BleReadCallback bleReadCallback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(bluetoothGatts.containsKey(mac)&&bluetoothGatts.get(mac)!=null) {
                    BluetoothGatt gatt = bluetoothGatts.get(mac);
                    if(gatt!=null&&gatt.getDevice()!=null) {
                        BluetoothGattService bluetoothGattServer = gatt.getService(UUID.fromString(uuid_service));
                        if(bluetoothGattServer!=null) {
                            BluetoothGattCharacteristic gattCharacteristic = bluetoothGattServer.getCharacteristic(UUID.fromString(uuid_write));
                            if (gattCharacteristic != null&&bleReadCallback!=null) {
                                bleReadCallback.onReadResponse(gattCharacteristic.getValue());
                            }
                        }
                    }
                }
            }
        }).start();
    }
    public synchronized void readBleVertion(final String mac,final BleReadCallback bleReadCallback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(bluetoothGatts.containsKey(mac)&&bluetoothGatts.get(mac)!=null) {
                    BluetoothGatt gatt = bluetoothGatts.get(mac);
                    if(gatt!=null&&gatt.getDevice()!=null) {
                        BluetoothGattService bluetoothGattServer = gatt.getService(BleUUIDS.versionServiceUUID);
                        if(bluetoothGattServer!=null) {
                            BluetoothGattCharacteristic firmwareVertionCharacteristic = bluetoothGattServer.getCharacteristic(BleUUIDS.firmwareVertionCharacteristicUUID);
                            BluetoothGattCharacteristic softwareVertionCharacteristic = bluetoothGattServer.getCharacteristic(BleUUIDS.softwareVertionCharacteristicUUID);
                            byte[] firmwareVertion = null;
                            byte[] softwareVertion = null;
                            if (firmwareVertionCharacteristic != null) {
                                firmwareVertion = firmwareVertionCharacteristic.getValue();
                            }
                            if (softwareVertionCharacteristic != null) {
                                softwareVertion = softwareVertionCharacteristic.getValue();
                            }
                            if(bleReadCallback!=null&&firmwareVertion!=null&&softwareVertion!=null){
                                bleReadCallback.onReadBleVertion(firmwareVertion,softwareVertion);
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
        new Thread(new Runnable() {
            @Override
            public synchronized void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (gatt != null) {
                    BluetoothGattService buttonGattServer = gatt.getService(BleUUIDS.userServiceUUID);
                    if (buttonGattServer != null) {
                        BluetoothGattCharacteristic buttonCharacteristic = buttonGattServer.getCharacteristic(BleUUIDS.userCharactButtonStateUUID);
                        if(buttonCharacteristic!=null) {
                            final int properties = buttonCharacteristic.getProperties();
                            if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0) {

                            } else {
                                boolean success1 = gatt.setCharacteristicNotification(buttonCharacteristic, true);
                                LogUtils.d("bleService", gatt.getDevice().getAddress() + "--->setCharacteristicNotification = " + success1);
                                try {
                                    Thread.sleep(300);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                final BluetoothGattDescriptor descriptor = buttonCharacteristic.getDescriptor(BleUUIDS.CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
                                if (descriptor != null) {
                                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                    boolean success2 = gatt.writeDescriptor(descriptor);
                                    LogUtils.d("bleService", gatt.getDevice().getAddress() + "--->writeDescriptor:NOTIFICATION_VALUE = " + success2);
                                }
                            }
                        }

                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    BluetoothGattService battaryGattServer = gatt.getService(BleUUIDS.batServiceUUID);
                    BluetoothGattCharacteristic batCharacteristic = battaryGattServer.getCharacteristic(BleUUIDS.batCharacteristicUUID);
                    if(batCharacteristic!=null) {
                        // Check characteristic property
                        final int properties = batCharacteristic.getProperties();
                        if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0){

                        }else{
                            boolean success1 = gatt.setCharacteristicNotification(batCharacteristic, true);
                            LogUtils.d("bleService", gatt.getDevice().getAddress() + "--->setCharacteristicNotification = " + success1);
                            try {
                                Thread.sleep(300);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            final BluetoothGattDescriptor descriptor = batCharacteristic.getDescriptor(BleUUIDS.CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
                            if (descriptor != null) {
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                boolean success2 = gatt.writeDescriptor(descriptor);
                                LogUtils.d("bleService", gatt.getDevice().getAddress() + "--->writeDescriptor:NOTIFICATION_VALUE = " + success2);
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
    public synchronized int getConnectState(BluetoothDevice device){
        if(bluetoothManager!=null) {            ;
            return bluetoothManager.getConnectionState(device,BluetoothGatt.GATT);
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
            if(mConnecttingList.contains(mac)){
                mConnecttingList.remove(mac);
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
            mConnecttingList.clear();
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
        mConnecttingList.clear();
        isDisConnectMaps.clear();
    }

    public void enaableBluetooth() {
        if(mBluetoothAdapter!=null){
            mBluetoothAdapter.enable();
        }
    }
    public static boolean refresh(BluetoothGatt gatt) {
        try {
            Log.d("bleService", "refresh device cache");
            Method localMethod = gatt.getClass().getMethod("refresh", (Class[]) null);
            if (localMethod != null) {
                boolean result = (Boolean) localMethod.invoke(gatt, (Object[]) null);
                if (!result)
                    Log.d("bleService", "refresh failed");
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("bleService", "An exception occurred while refreshing device cache");
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
                    bluetoothGatt.disconnect();
                    bluetoothGatt.close();
                    if (mGattCallback!=null) {
                        mGattCallback.onEffectDisConnected(false, mac, BluetoothGatt.STATE_DISCONNECTED);
                    }
                }
            }
            bluetoothGatts.clear();
            mConnecttingList.clear();
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
}
