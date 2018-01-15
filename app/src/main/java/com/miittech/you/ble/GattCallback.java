package com.miittech.you.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by Administrator on 2018/1/10.
 */

public abstract class GattCallback{
    public abstract boolean onStartConnect(String mac);
    public abstract void onConnectFail(String mac);

    public abstract void onConnectSuccess(String mac, int status);

    public abstract void onDisConnected(boolean isActiveDisConnected,String mac,int status);

    public abstract void onReadRemoteRssi(String mac, int rssi, int status);
    public void onCharacteristicChanged(String mac, BluetoothGattCharacteristic characteristic){};
    public void onCharacteristicWrite(String mac, BluetoothGattCharacteristic characteristic, int status){};
}
