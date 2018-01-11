package com.miittech.you.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;

/**
 * Created by Administrator on 2018/1/10.
 */

public abstract class GattCallback extends BluetoothGattCallback {
    public abstract void onStartConnect(BluetoothDevice device);

    public abstract void onConnectFail(BluetoothDevice device);

    public abstract void onConnectSuccess(BluetoothDevice device, BluetoothGatt gatt, int status);

    public abstract void onDisConnected(boolean isActiveDisConnected,BluetoothDevice device, BluetoothGatt gatt, int status);

    public abstract void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status);
}
