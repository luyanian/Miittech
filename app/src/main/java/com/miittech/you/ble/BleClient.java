package com.miittech.you.ble;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.os.Build;

import com.miittech.you.global.BleUUIDS;
import com.ryon.mutils.LogUtils;

import java.util.HashMap;
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
    private boolean isActivityDisConnect = false;
    private Map<String,BluetoothGatt> bluetoothGatts = new HashMap<>();
    private static final String UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR = "00002902-0000-1000-8000-00805f9b34fb";
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
        if(device==null){
            return;
        }

        mGattCallback.onStartConnect(device);
        BluetoothGatt mBluetoothGatt = device.connectGatt(context, false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if(newState == BluetoothGatt.STATE_CONNECTED){
                    gatt.discoverServices();
                }else if(newState == BluetoothGatt.STATE_DISCONNECTED){
                    if(bluetoothGatts.containsKey(device.getAddress())){
                        BluetoothGatt bluetoothGatt = bluetoothGatts.get(device.getAddress());
                        if(bluetoothGatt!=null){
                            if(bluetoothGatt.getServices()!=null&&bluetoothGatt.getServices().size()>0) {
                                mGattCallback.onDisConnected(isActivityDisConnect, device, gatt, newState);
                                isActivityDisConnect=false;
                            }else{
                                mGattCallback.onConnectFail(device);
                            }
                            bluetoothGatt.disconnect();
                            bluetoothGatt.close();
                        }else{
                            mGattCallback.onConnectFail(device);
                        }
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
                    mGattCallback.onConnectSuccess(device,gatt,status);
                    gatt.readRemoteRssi();
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

    public synchronized boolean notify(final String mac, final String uuid_service, final String uuid_notify){
        if (bluetoothGatts.containsKey(mac) && bluetoothGatts.get(mac) != null) {
            BluetoothGatt gatt = bluetoothGatts.get(mac);
            if (gatt != null) {
                BluetoothGattService bluetoothGattServer = gatt.getService(UUID.fromString(uuid_service));
                if (bluetoothGattServer != null) {
                    BluetoothGattCharacteristic gattCharacteristic = bluetoothGattServer.getCharacteristic(UUID.fromString(uuid_notify));
                    if (gattCharacteristic != null) {
                        boolean success1 = gatt.setCharacteristicNotification(gattCharacteristic, true);
                        LogUtils.d("bleService",gatt.getDevice().getAddress()+"--->setCharacteristicNotification = "+success1);
                        if(success1){
                            BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(UUID.fromString(UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR));
                            if (descriptor != null) {
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                boolean success2 = gatt.writeDescriptor(descriptor);
                                LogUtils.d("bleService",gatt.getDevice().getAddress()+"--->writeDescriptor:NOTIFICATION_VALUE = "+success2);
                                return success2;
                            }
                        }else{
                            return success1;
                        }
                    }
                }
            }
        }
        return  false;
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
            isActivityDisConnect=true;
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
                isActivityDisConnect=true;
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
                isActivityDisConnect=true;
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
            }
        }
    }
}
