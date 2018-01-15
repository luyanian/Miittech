package com.miittech.you.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

/**
 * Created by ryon on 2018/1/13.
 */

public class BleWriteCallback {
    public synchronized void onWriteSuccess(BluetoothDevice device){};
    public synchronized void onWriteFialed(BluetoothDevice device){};
}
