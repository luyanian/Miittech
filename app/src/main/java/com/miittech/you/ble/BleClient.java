package com.miittech.you.ble;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.os.Build;
import com.miittech.you.global.BleUUIDS;
import com.ryon.mutils.LogUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Administrator on 2018/1/10.
 */

public class BleClient {
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private ScanResultCallback scanResultCallback;
    private Context context;
    private Map<String,Boolean> isActivityDisConnects = new HashMap();
    private Map<String,BluetoothGatt> bluetoothGatts = new HashMap<>();

    private boolean isScaning = false;
    static BleClient bleClient;
    public synchronized static BleClient getInstance(){
        if(bleClient==null){
            synchronized (BleClient.class){
                bleClient = new BleClient();
            }
        }
        return bleClient;
    }
    public void init(Context context){
        this.context = context;
        bluetoothManager =(BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if(mBluetoothAdapter!=null&&Build.VERSION.SDK_INT> 24){
            bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        }
    }
    public synchronized void startScan(final ScanResultCallback scanResultCallback){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(scanResultCallback!=null&&mBluetoothAdapter!=null&&mBluetoothAdapter.isEnabled()) {
                    BleClient.this.scanResultCallback = scanResultCallback;
                    isScaning = true;
                    if(Build.VERSION.SDK_INT> 24&&bluetoothLeScanner!=null){
                        bluetoothLeScanner.startScan(scanResultCallback);
                    }else {
                        mBluetoothAdapter.startLeScan(scanResultCallback);
                    }
                }
            }
        }).start();

    }

    public synchronized void connectDevice(final BluetoothDevice device, final GattCallback mGattCallback){
        mGattCallback.onStartConnect(device);
        if(device==null){
            mGattCallback.onConnectFail(device);
            return;
        }
        device.connectGatt(context, false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if(newState == BluetoothGatt.STATE_CONNECTED){
                    gatt.discoverServices();
                }else if(newState == BluetoothGatt.STATE_DISCONNECTED){
                    if(gatt.getServices()!=null&&gatt.getServices().size()>0) {
                        mGattCallback.onDisConnected(isActivityDisConnects.get(device.getAddress()), device, gatt, newState);
                    }else{
                        mGattCallback.onConnectFail(device);
                    }
                    gatt.disconnect();
                    gatt.close();
                    if(bluetoothGatts.containsKey(device.getAddress())) {
                        bluetoothGatts.remove(device.getAddress());
                    }
                }

            }

            @Override
            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                super.onReadRemoteRssi(gatt, rssi, status);
                mGattCallback.onReadRemoteRssi(gatt,rssi,status);
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    bluetoothGatts.put(device.getAddress(),gatt);
                    isActivityDisConnects.put(device.getAddress(),false);
                    mGattCallback.onConnectSuccess(device,gatt,status);
                    gatt.readRemoteRssi();
                    setNotify(gatt);
                }else{
                    mGattCallback.onConnectFail(device);
                    gatt.disconnect();
                    gatt.close();
                    if(bluetoothGatts.containsKey(device.getAddress())) {
                        bluetoothGatts.remove(device.getAddress());
                    }
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                mGattCallback.onCharacteristicChanged(gatt, characteristic);
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
                mGattCallback.onCharacteristicWrite(gatt, characteristic, status);
            }

        });
    }
    public synchronized boolean write(String mac,
                      String uuid_service,
                      String uuid_write,
                      byte[] data) {
        if(bluetoothGatts.containsKey(mac)&&bluetoothGatts.get(mac)!=null) {
            BluetoothGatt gatt = bluetoothGatts.get(mac);
            if(gatt!=null) {
                BluetoothGattService bluetoothGattServer = gatt.getService(UUID.fromString(uuid_service));
                if(bluetoothGattServer!=null) {
                    BluetoothGattCharacteristic gattCharacteristic = bluetoothGattServer.getCharacteristic(UUID.fromString(uuid_write));
                    if (gattCharacteristic != null) {
                        gattCharacteristic.setValue(data);
                        return gatt.writeCharacteristic(gattCharacteristic);
                    }
                }
            }
        }
        return false;
    }
    public synchronized boolean readRssi(String mac){
        if(bluetoothGatts.containsKey(mac)&&bluetoothGatts.get(mac)!=null) {
            BluetoothGatt gatt = bluetoothGatts.get(mac);
            return gatt.readRemoteRssi();
        }
        return false;
    }

    public synchronized void setNotify(BluetoothGatt gatt){
        if (gatt != null) {
            BluetoothGattService buttonGattServer = gatt.getService(UUID.fromString(BleUUIDS.userServiceUUID));
            if (buttonGattServer != null) {
                BluetoothGattCharacteristic buttonCharacteristic = buttonGattServer.getCharacteristic(UUID.fromString(BleUUIDS.userCharactButtonStateUUID));
                if (buttonCharacteristic != null) {
                    boolean success1 = gatt.setCharacteristicNotification(buttonCharacteristic, true);
                    LogUtils.d("bleService", gatt.getDevice().getAddress() + "--->setCharacteristicNotification = " + success1);
                    if (success1) {
                        BluetoothGattDescriptor descriptor = buttonCharacteristic.getDescriptor(UUID.fromString(BleUUIDS.UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR));
                        if (descriptor != null) {
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            success1 = gatt.writeDescriptor(descriptor);
                            LogUtils.d("bleService", gatt.getDevice().getAddress() + "--->writeDescriptor:NOTIFICATION_VALUE = " + success1);
                        }
                    }
                }
//                BluetoothGattService battaryGattServer = gatt.getService(UUID.fromString(BleUUIDS.batServiceUUID));
//                if (battaryGattServer != null) {
//                    BluetoothGattCharacteristic batteryCharacteristic = battaryGattServer.getCharacteristic(UUID.fromString(BleUUIDS.batCharacteristicUUID));
//                    if (batteryCharacteristic != null) {
//                        try {
//                            Thread.sleep(200);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                        boolean success1 = gatt.setCharacteristicNotification(batteryCharacteristic, true);
//                        LogUtils.d("bleService", gatt.getDevice().getAddress() + "--->setCharacteristicNotification = " + success1);
//                        if (success1) {
//                            List<BluetoothGattDescriptor> descriptors = batteryCharacteristic.getDescriptors();
//                            for(BluetoothGattDescriptor bluetoothGattDescriptor : descriptors){
//                                if (bluetoothGattDescriptor != null) {
//                                    bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//                                    try {
//                                        Thread.sleep(200);
//                                    } catch (InterruptedException e) {
//                                        e.printStackTrace();
//                                    }
//                                    success1 = gatt.writeDescriptor(bluetoothGattDescriptor);
//                                    LogUtils.d("bleService", gatt.getDevice().getAddress() + "--->writeDescriptor:NOTIFICATION_VALUE = " + success1);
//                                }
//                            }
//                        }
//                    }
//                }
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
        if(bluetoothGatts.containsKey(mac)&&bluetoothGatts.get(mac)!=null){
            BluetoothGatt mBluetoothGatt = bluetoothGatts.get(mac);
            isActivityDisConnects.put(mac,true);
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
        }
    }


    public synchronized void cancelScan() {
        if(scanResultCallback!=null) {
            isScaning = false;
            if(Build.VERSION.SDK_INT> 24&&mBluetoothAdapter!=null&&bluetoothLeScanner!=null){
                if(mBluetoothAdapter.isEnabled()) {
                    bluetoothLeScanner.stopScan(scanResultCallback);
                }
            }else if(mBluetoothAdapter!=null) {
                mBluetoothAdapter.stopLeScan(scanResultCallback);
            }
        }
    }

    public synchronized void disconnectAllDevice() {
        Set<Map.Entry<String,BluetoothGatt>> set = bluetoothGatts.entrySet();
        for(Map.Entry<String,BluetoothGatt> entry:set){
            BluetoothGatt bluetoothGatt = entry.getValue();
            if(bluetoothGatt!=null){
                isActivityDisConnects.put(bluetoothGatt.getDevice().getAddress(),true);
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
            }
        }
    }


    public boolean isEnabled() {
        return mBluetoothAdapter!=null&&mBluetoothAdapter.isEnabled();
    }

    public boolean disableBluetooth() {
        return mBluetoothAdapter!=null&&mBluetoothAdapter.disable();
    }

    public void disconnectAllWithLinklose(byte[] data) {
        Set<Map.Entry<String,BluetoothGatt>> set = bluetoothGatts.entrySet();
        for(Map.Entry<String,BluetoothGatt> entry:set){
            BluetoothGatt bluetoothGatt = entry.getValue();
            if(bluetoothGatt!=null){
                BleClient.getInstance().write(bluetoothGatt.getDevice().getAddress(), BleUUIDS.linkLossUUID, BleUUIDS.characteristicUUID, data);
                isActivityDisConnects.put(bluetoothGatt.getDevice().getAddress(),true);
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
            }
        }
    }

    public void enaableBluetooth() {
        if(mBluetoothAdapter!=null){
            mBluetoothAdapter.enable();
        }
    }
}
