package com.miittech.you.ble;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Build;

import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Administrator on 2018/1/10.
 */

public class BleClient {

    private BluetoothAdapter mBluetoothAdapter;
    private Application application;
    private ScanResultCallback scanResultCallback;
    private boolean isActivityDisConnect = false;
    private Map<String,BluetoothGatt> bluetoothGatts = new HashMap<>();
    public void init(Application application){
        this.application = application;
        final BluetoothManager bluetoothManager =(BluetoothManager) application.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }
    public void startScan(ScanResultCallback scanResultCallback){
        if(scanResultCallback!=null) {
            if(Build.VERSION.SDK_INT> 24){
                mBluetoothAdapter.getBluetoothLeScanner().startScan(scanResultCallback);
            }else {
                mBluetoothAdapter.startLeScan(scanResultCallback);
            }
        }
    }

    public void connectDevice(final BluetoothDevice device, final GattCallback mGattCallback){
        BluetoothGatt mBluetoothGatt = device.connectGatt(application, false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if(newState == BluetoothGatt.STATE_CONNECTED){
                    mGattCallback.onConnectSuccess(device,gatt,newState);
                }else if(newState == BluetoothGatt.STATE_DISCONNECTED){
                    mGattCallback.onDisConnected(isActivityDisConnect,device,gatt,newState);
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
            }
        });
        bluetoothGatts.put(device.getAddress(),mBluetoothGatt);
    }
    public boolean write(String mac,
                      String uuid_service,
                      String uuid_write,
                      byte[] data,
                      BleWriteCallback callback) {
        BluetoothGatt gatt = bluetoothGatts.get(mac);
        BluetoothGattCharacteristic gattCharacteristic = gatt.getService(UUID.fromString(uuid_service)).getCharacteristic(UUID.fromString(uuid_write));
        gattCharacteristic.setValue(data);
        return gatt.writeCharacteristic(gattCharacteristic);
    }

    public boolean isConnected(String mac){
        if(bluetoothGatts.containsKey(mac)) {
            BluetoothGatt mBluetoothGatt = bluetoothGatts.get(mac);
            return mBluetoothGatt.getConnectionState(mBluetoothGatt.getDevice())==BluetoothGatt.STATE_CONNECTED;
        }
        return false;
    }

    public void disConnect(String mac){
        if(bluetoothGatts.containsKey(mac)){
            BluetoothGatt mBluetoothGatt = bluetoothGatts.get(mac);
            if(mBluetoothGatt!=null){
                isActivityDisConnect=true;
                mBluetoothGatt.disconnect();
                mBluetoothGatt.close();
            }
        }
    }


}
