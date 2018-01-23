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
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.os.Build;
import android.support.v4.util.SimpleArrayMap;
import android.text.TextUtils;
import android.util.Log;
import com.miittech.you.App;
import com.miittech.you.global.BleUUIDS;
import com.ryon.mutils.LogUtils;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Administrator on 2018/1/10.
 */

public class BleClient {
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BleScanCallback bleScanCallback;
    private BleLeScanCallback bleLeScanCallback;
    private GattCallback mGattCallback;
    private Context context;
    private SimpleArrayMap<String,Boolean> isActivityDisConnects = new SimpleArrayMap();
    private SimpleArrayMap<String,BluetoothGatt> bluetoothGatts = new SimpleArrayMap<>();
    private SimpleArrayMap<String,Boolean> isDisConnectMaps = new SimpleArrayMap<>();
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
        if(mBluetoothAdapter!=null&&Build.VERSION.SDK_INT> 21){
            bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        }
    }
    public synchronized void startScan(final ScanResultCallback scanResultCallback){
        if(scanResultCallback!=null&&mBluetoothAdapter!=null&&mBluetoothAdapter.isEnabled()) {
            isScaning = true;
            if(Build.VERSION.SDK_INT> 21&&bluetoothLeScanner!=null){
                bleScanCallback = new BleScanCallback(scanResultCallback);
                bluetoothLeScanner.startScan(bleScanCallback);
            }else {
                bleLeScanCallback = new BleLeScanCallback(scanResultCallback);
                mBluetoothAdapter.startLeScan(bleLeScanCallback);
            }
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
                return;
            }
            if (mDevice == null) {
                mGattCallback.onConnectFail(mDevice.getAddress());
                return;
            }
            mDevice.connectGatt(context, false, new BluetoothGattCallback() {
                @Override
                public synchronized void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    super.onConnectionStateChange(gatt, status, newState);
                    synchronized (mConnecttingList) {
                        if (status==BluetoothGatt.GATT_SUCCESS&&newState == BluetoothProfile.STATE_CONNECTED) {
                            gatt.discoverServices();
                        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                            if(mBluetoothAdapter.isEnabled()) {
                                if (gatt != null&&gatt.getDevice()!=null&&!TextUtils.isEmpty(gatt.getDevice().getAddress())) {
                                    String mac = gatt.getDevice().getAddress();
                                    if (mConnecttingList.contains(gatt.getDevice().getAddress())
                                            && isActivityDisConnects.containsKey(mac) && !isConnected(gatt.getDevice().getAddress())
                                            && isDisConnectMaps.containsKey(mac) && isDisConnectMaps.get(mac)) {
                                        mGattCallback.onDisConnected(isActivityDisConnects.get(mac), mac, newState);
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
                            mGattCallback.onConnectSuccess(gatt.getDevice().getAddress(), status);
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
                                      final String uuid_service,
                                      final String uuid_write,
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
                        BluetoothGattService bluetoothGattServer = gatt.getService(UUID.fromString(uuid_service));
                        if(bluetoothGattServer!=null) {
                            BluetoothGattCharacteristic gattCharacteristic = bluetoothGattServer.getCharacteristic(UUID.fromString(uuid_write));
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
                    BluetoothGattService buttonGattServer = gatt.getService(UUID.fromString(BleUUIDS.userServiceUUID));
                    if (buttonGattServer != null) {
                        BluetoothGattCharacteristic buttonCharacteristic = buttonGattServer.getCharacteristic(UUID.fromString(BleUUIDS.userCharactButtonStateUUID));
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
                                final BluetoothGattDescriptor descriptor = buttonCharacteristic.getDescriptor(UUID.fromString(BleUUIDS.CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID));
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
                    BluetoothGattService battaryGattServer = gatt.getService(UUID.fromString(BleUUIDS.batServiceUUID));
                    BluetoothGattCharacteristic batCharacteristic = battaryGattServer.getCharacteristic(UUID.fromString(BleUUIDS.batCharacteristicUUID));
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
                            final BluetoothGattDescriptor descriptor = batCharacteristic.getDescriptor(UUID.fromString(BleUUIDS.CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID));
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
    public synchronized void cancelScan() {
        isScaning = false;
        if(Build.VERSION.SDK_INT> 21&&bluetoothLeScanner!=null&&bleScanCallback!=null){
            bluetoothLeScanner.stopScan(bleScanCallback);
            bleScanCallback=null;
        }else if(mBluetoothAdapter!=null&&bleLeScanCallback != null) {
            mBluetoothAdapter.stopLeScan(bleLeScanCallback);
            bleLeScanCallback=null;
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
                        mGattCallback.onDisConnected(false, mac, BluetoothGatt.STATE_DISCONNECTED);
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
