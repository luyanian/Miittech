package com.miittech.you.ble.gatt;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by Administrator on 2018/1/10.
 */

public abstract class GattCallback{
    public abstract void onStartConnect(String mac);
    public abstract void onConnectFail(String mac);

    public abstract void onConnectSuccess(String mac, int status);
    public abstract boolean onEffectConnectSuccess(String mac, int status);
    public abstract void onDisConnected(boolean isActiveDisConnected,String mac,int status);
    public abstract void onEffectDisConnected(boolean isActiveDisConnected,String mac,int status);

    public abstract void onReadRemoteRssi(String mac, int rssi, int status);
    public void onCharacteristicChanged(String mac, BluetoothGattCharacteristic characteristic){};

    public abstract void onBindModeSuccess(BluetoothDevice device);
    public abstract void onBindModeFaild(BluetoothDevice device);
    public abstract void onWorkModeSuccess(BluetoothDevice device);
    public abstract void onWorkModeFaild(BluetoothDevice device);


}
